package dev.proceed.generation;

import dev.proceed.litematic.BlockState;
import dev.proceed.litematic.Schematic;
import dev.proceed.math.BlockPos;
import dev.proceed.math.BoundingBox;
import dev.proceed.math.Rotation;
import dev.proceed.piece.ConnectionPoint;
import dev.proceed.piece.PiecePool;
import dev.proceed.piece.StructurePiece;
import dev.proceed.placement.GenerationCondition;
import dev.proceed.placement.HeightProvider;
import dev.proceed.placement.PlacementContext;
import dev.proceed.placement.StructurePlacement;
import dev.proceed.processor.StructureProcessor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * The procedural generation engine.
 *
 * <p>Starting from a chosen start piece, it grows a structure by walking a frontier of open
 * {@link WorldConnection}s. For each open connection it looks for a compatible piece, works out
 * the unique rotation and offset that mate the two openings, rejects placements that collide with
 * existing pieces, leave the configured volume, or stray past {@code maxDistanceFromCenter}, and
 * repeats until it runs out of connections or hits a limit.
 *
 * <p>Two ways to run it:
 * <ul>
 *     <li>{@link #generate()} &mdash; grow a structure at the origin, ignoring world conditions.</li>
 *     <li>{@link #tryGenerate(PlacementContext)} &mdash; the vanilla-style path: evaluate
 *         placement conditions and grid spacing, resolve the world origin and start height, and
 *         return empty when the structure should not spawn here.</li>
 * </ul>
 *
 * <pre>{@code
 * Optional<GeneratedStructure> maybe = ProceedGenerator.create()
 *         .pool(pool)
 *         .condition(GenerationCondition.inBiomes("minecraft:desert").and(GenerationCondition.chance(0.4)))
 *         .placement(RandomSpreadPlacement.of(32, 8, 165745296))
 *         .startHeight(HeightProvider.fromTerrain(-1))     // sit one block into the ground
 *         .maxDistanceFromCenter(80)
 *         .processors(Processors.integrity(0.9))
 *         .maxPieces(40)
 *         .tryGenerate(context);
 * }</pre>
 *
 * <p>Everything is driven by a seeded {@link Random}, so the same configuration and seed always
 * produce the same structure &mdash; essential for reproducible worldgen.
 */
public final class ProceedGenerator {

    private static final long GROWTH_SALT = 0x7A9E14B3C6D28F01L;

    private PiecePool pool;
    private Long seed;
    private int maxPieces = 32;
    private int maxDepth = Integer.MAX_VALUE;
    private int spacing = 0;
    private int maxDistanceFromCenter = Integer.MAX_VALUE;
    private BoundingBox bounds;
    private boolean allowCollisions = false;
    private boolean sealOpenConnections = true;
    private boolean includeAir = false;
    private BlockState markerFill = Schematic.AIR;
    private Rotation startRotation = Rotation.NONE;

    private GenerationCondition condition;
    private StructurePlacement placement;
    private HeightProvider startHeight;
    private final List<StructureProcessor> processors = new ArrayList<>();

    private ProceedGenerator() {
    }

    public static ProceedGenerator create() {
        return new ProceedGenerator();
    }

    // ---- configuration --------------------------------------------------------------------

    /** The pieces to build from (required). */
    public ProceedGenerator pool(PiecePool pool) {
        this.pool = pool;
        return this;
    }

    /** Fixes the random seed for reproducible output. Defaults to a random seed each run. */
    public ProceedGenerator seed(long seed) {
        this.seed = seed;
        return this;
    }

    /** Hard cap on the number of pieces (including the start piece). Default {@code 32}. */
    public ProceedGenerator maxPieces(int maxPieces) {
        if (maxPieces < 1) {
            throw new IllegalArgumentException("maxPieces must be >= 1");
        }
        this.maxPieces = maxPieces;
        return this;
    }

    /** Maximum attachment distance from the start piece. Default: unlimited. */
    public ProceedGenerator maxDepth(int maxDepth) {
        if (maxDepth < 0) {
            throw new IllegalArgumentException("maxDepth must be >= 0");
        }
        this.maxDepth = maxDepth;
        return this;
    }

    /** Extra air blocks to leave between mated pieces. Default {@code 0} (pieces abut). */
    public ProceedGenerator spacing(int spacing) {
        if (spacing < 0) {
            throw new IllegalArgumentException("spacing must be >= 0");
        }
        this.spacing = spacing;
        return this;
    }

    /**
     * Clamps how far, in blocks on the X/Z plane, a piece may extend from the start piece's centre
     * &mdash; the analogue of jigsaw {@code max_distance_from_center} (vanilla uses 80/116).
     * Default: unlimited.
     */
    public ProceedGenerator maxDistanceFromCenter(int blocks) {
        if (blocks < 1) {
            throw new IllegalArgumentException("maxDistanceFromCenter must be >= 1");
        }
        this.maxDistanceFromCenter = blocks;
        return this;
    }

    /** Restricts placement to within this box (structure coordinates). Default: unlimited. */
    public ProceedGenerator bounds(BoundingBox bounds) {
        this.bounds = bounds;
        return this;
    }

    /** Allows pieces to overlap. Default {@code false} (collisions are rejected). */
    public ProceedGenerator allowCollisions(boolean allow) {
        this.allowCollisions = allow;
        return this;
    }

    /** Whether to seal leftover connections with cap pieces at the end. Default {@code true}. */
    public ProceedGenerator sealOpenConnections(boolean seal) {
        this.sealOpenConnections = seal;
        return this;
    }

    /** Whether the block output includes air cells (to clear space). Default {@code false}. */
    public ProceedGenerator includeAir(boolean includeAir) {
        this.includeAir = includeAir;
        return this;
    }

    /**
     * The block placed at marker cells on output. Defaults to air (markers become openings). Pass
     * {@code null} to leave marker cells untouched (emit nothing for them).
     */
    public ProceedGenerator replaceMarkersWith(BlockState fill) {
        this.markerFill = fill;
        return this;
    }

    /** The rotation applied to the start piece. Default {@link Rotation#NONE}. */
    public ProceedGenerator startRotation(Rotation rotation) {
        this.startRotation = rotation;
        return this;
    }

    /**
     * A condition that must pass for {@link #tryGenerate} to produce a structure (biome, dimension,
     * rarity, terrain height&hellip;). Default: none (always allowed).
     */
    public ProceedGenerator condition(GenerationCondition condition) {
        this.condition = condition;
        return this;
    }

    /**
     * The spacing/separation placement grid used by {@link #tryGenerate} to decide which chunks may
     * start a structure. Default: none (every queried chunk may start one).
     */
    public ProceedGenerator placement(StructurePlacement placement) {
        this.placement = placement;
        return this;
    }

    /** How {@link #tryGenerate} chooses the start Y (constant, uniform, terrain-projected&hellip;). */
    public ProceedGenerator startHeight(HeightProvider provider) {
        this.startHeight = provider;
        return this;
    }

    /** Adds a block post-processor (applied in order as blocks are emitted). */
    public ProceedGenerator processor(StructureProcessor processor) {
        this.processors.add(processor);
        return this;
    }

    /** Adds several block post-processors. */
    public ProceedGenerator processors(StructureProcessor... processors) {
        for (StructureProcessor p : processors) {
            this.processors.add(p);
        }
        return this;
    }

    // ---- generation -----------------------------------------------------------------------

    /** Grows a structure at the origin, ignoring world placement conditions. */
    public GeneratedStructure generate() {
        if (pool == null) {
            throw new IllegalStateException("a piece pool is required");
        }
        long actualSeed = seed != null ? seed : new Random().nextLong();
        Grown grown = grow(new Random(actualSeed));
        return new GeneratedStructure(grown.placed, grown.open, actualSeed,
                includeAir, markerFill, BlockPos.ORIGIN, processors);
    }

    /**
     * The vanilla-style path: check the configured {@link #condition} and {@link #placement}
     * against {@code ctx}, and if the structure may spawn here, grow it and resolve its world
     * origin (X/Z from placement, Y from {@link #startHeight}).
     *
     * @return the generated structure, or empty if it should not spawn in this context
     */
    public Optional<GeneratedStructure> tryGenerate(PlacementContext ctx) {
        if (pool == null) {
            throw new IllegalStateException("a piece pool is required");
        }
        if (condition != null && !condition.test(ctx)) {
            return Optional.empty();
        }

        int startX = ctx.blockX();
        int startZ = ctx.blockZ();
        if (placement != null) {
            var packed = placement.placementChunk(ctx);
            if (packed.isEmpty()) {
                return Optional.empty();
            }
            long p = packed.getAsLong();
            int cx = StructurePlacement.unpackChunkX(p);
            int cz = StructurePlacement.unpackChunkZ(p);
            if (cx != ctx.chunkX() || cz != ctx.chunkZ()) {
                return Optional.empty();
            }
            startX = (cx << 4) + 8;
            startZ = (cz << 4) + 8;
        }

        long growthSeed = seed != null ? seed : ctx.random(GROWTH_SALT).nextLong();
        Random rng = new Random(growthSeed);
        int startY = startHeight != null ? startHeight.sample(rng, ctx) : 0;

        Grown grown = grow(rng);
        BlockPos origin = new BlockPos(startX, startY, startZ);
        return Optional.of(new GeneratedStructure(grown.placed, grown.open, growthSeed,
                includeAir, markerFill, origin, processors));
    }

    // ---- core growth ----------------------------------------------------------------------

    private Grown grow(Random rng) {
        List<PlacedPiece> placed = new ArrayList<>();
        List<BoundingBox> boxes = new ArrayList<>();
        Map<StructurePiece, Integer> uses = new HashMap<>();
        List<WorldConnection> open = new ArrayList<>();

        StructurePiece startPiece = weightedPick(pool.startPieces(), rng);
        PlacedPiece start = new PlacedPiece(startPiece, startRotation, BlockPos.ORIGIN);
        placed.add(start);
        boxes.add(start.bounds());
        uses.merge(startPiece, 1, Integer::sum);

        BoundingBox startBox = start.bounds();
        int centerX = (startBox.minX() + startBox.maxX()) / 2;
        int centerZ = (startBox.minZ() + startBox.maxZ()) / 2;

        Deque<Frontier> frontier = new ArrayDeque<>();
        for (WorldConnection wc : start.worldConnections()) {
            frontier.add(new Frontier(wc, 0));
        }

        while (!frontier.isEmpty() && placed.size() < maxPieces) {
            Frontier entry = frontier.poll();
            if (entry.depth >= maxDepth) {
                open.add(entry.connection);
                continue;
            }
            Attachment attached = tryAttach(entry.connection, rng, boxes, uses, centerX, centerZ);
            if (attached == null) {
                open.add(entry.connection);
                continue;
            }
            placed.add(attached.piece);
            boxes.add(attached.piece.bounds());
            uses.merge(attached.piece.piece(), 1, Integer::sum);

            for (ConnectionPoint c : attached.piece.piece().connections()) {
                if (c == attached.consumed) {
                    continue;
                }
                frontier.add(new Frontier(attached.piece.worldConnection(c), entry.depth + 1));
            }
        }

        for (Frontier f : frontier) {
            open.add(f.connection);
        }

        if (sealOpenConnections) {
            open = sealWithCaps(open, placed, boxes, centerX, centerZ);
        }

        return new Grown(placed, open);
    }

    private Attachment tryAttach(WorldConnection open, Random rng, List<BoundingBox> boxes,
                                 Map<StructurePiece, Integer> uses, int centerX, int centerZ) {
        List<StructurePiece> candidates = new ArrayList<>();
        for (StructurePiece c : pool.candidatesFor(open.channel())) {
            if (c.maxUses() >= 0 && uses.getOrDefault(c, 0) >= c.maxUses()) {
                continue;
            }
            candidates.add(c);
        }
        for (StructurePiece candidate : weightedOrder(candidates, rng)) {
            Attachment attachment = tryPlaceCandidate(candidate, open, rng, boxes, centerX, centerZ);
            if (attachment != null) {
                return attachment;
            }
        }
        return null;
    }

    private Attachment tryPlaceCandidate(StructurePiece candidate, WorldConnection open, Random rng,
                                         List<BoundingBox> boxes, int centerX, int centerZ) {
        List<ConnectionPoint> mates = new ArrayList<>(candidate.connectionsOn(open.channel()));
        shuffle(mates, rng);
        BlockPos mateCell = open.mateCell(spacing);

        for (ConnectionPoint b : mates) {
            for (Rotation rot : matchingRotations(b, open, rng)) {
                BlockPos rotatedB = rot.rotate(b.position(), candidate.size());
                BlockPos offset = mateCell.subtract(rotatedB);
                BoundingBox box = BoundingBox.fromSize(offset, rot.rotate(candidate.size()));

                if (bounds != null && !bounds.contains(box)) {
                    continue;
                }
                if (exceedsMaxDistance(box, centerX, centerZ)) {
                    continue;
                }
                if (!allowCollisions && collides(box, boxes)) {
                    continue;
                }
                return new Attachment(new PlacedPiece(candidate, rot, offset), b);
            }
        }
        return null;
    }

    private boolean exceedsMaxDistance(BoundingBox box, int centerX, int centerZ) {
        if (maxDistanceFromCenter == Integer.MAX_VALUE) {
            return false;
        }
        return box.minX() < centerX - maxDistanceFromCenter
                || box.maxX() > centerX + maxDistanceFromCenter
                || box.minZ() < centerZ - maxDistanceFromCenter
                || box.maxZ() > centerZ + maxDistanceFromCenter;
    }

    private List<Rotation> matchingRotations(ConnectionPoint b, WorldConnection open, Random rng) {
        var target = open.facing().opposite();
        List<Rotation> out = new ArrayList<>(4);
        for (Rotation r : Rotation.ALL) {
            if (r.rotate(b.facing()) == target) {
                out.add(r);
            }
        }
        shuffle(out, rng);
        return out;
    }

    private boolean collides(BoundingBox box, List<BoundingBox> boxes) {
        for (BoundingBox b : boxes) {
            if (box.intersects(b)) {
                return true;
            }
        }
        return false;
    }

    private List<WorldConnection> sealWithCaps(List<WorldConnection> open, List<PlacedPiece> placed,
                                               List<BoundingBox> boxes, int centerX, int centerZ) {
        List<WorldConnection> stillOpen = new ArrayList<>();
        Random rng = new Random(0); // deterministic capping, independent of growth order
        for (WorldConnection wc : open) {
            StructurePiece cap = pool.capFor(wc.channel());
            if (cap == null) {
                stillOpen.add(wc);
                continue;
            }
            Attachment attachment = tryPlaceCandidate(cap, wc, rng, boxes, centerX, centerZ);
            if (attachment != null) {
                placed.add(attachment.piece);
                boxes.add(attachment.piece.bounds());
            } else {
                stillOpen.add(wc);
            }
        }
        return stillOpen;
    }

    // ---- weighted selection helpers -------------------------------------------------------

    private static StructurePiece weightedPick(List<StructurePiece> pieces, Random rng) {
        double total = 0;
        for (StructurePiece p : pieces) {
            total += p.weight();
        }
        double r = rng.nextDouble() * total;
        double acc = 0;
        for (StructurePiece p : pieces) {
            acc += p.weight();
            if (r < acc) {
                return p;
            }
        }
        return pieces.get(pieces.size() - 1);
    }

    private static List<StructurePiece> weightedOrder(List<StructurePiece> pieces, Random rng) {
        List<StructurePiece> remaining = new ArrayList<>(pieces);
        List<StructurePiece> ordered = new ArrayList<>(pieces.size());
        while (!remaining.isEmpty()) {
            StructurePiece pick = weightedPick(remaining, rng);
            ordered.add(pick);
            remaining.remove(pick);
        }
        return ordered;
    }

    private static <T> void shuffle(List<T> list, Random rng) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            T tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }

    private record Frontier(WorldConnection connection, int depth) {
    }

    /** A successful placement together with the local connection it consumed. */
    private record Attachment(PlacedPiece piece, ConnectionPoint consumed) {
    }

    /** The raw output of the growth loop. */
    private record Grown(List<PlacedPiece> placed, List<WorldConnection> open) {
    }
}
