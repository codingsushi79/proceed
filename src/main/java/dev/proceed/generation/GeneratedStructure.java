package dev.proceed.generation;

import dev.proceed.litematic.BlockState;
import dev.proceed.math.BlockPos;
import dev.proceed.math.BoundingBox;
import dev.proceed.processor.ProcessorContext;
import dev.proceed.processor.StructureProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The result of a generation run: the pieces that were placed, any connections left open, and a
 * convenient way to enumerate every block to set in the world.
 *
 * <p>If the structure was produced from a {@link dev.proceed.placement.PlacementContext}, it also
 * carries the world {@link #origin()} that placement and the height provider resolved, so
 * {@link #blocks()} is already positioned. Otherwise the origin is {@code (0,0,0)} and you pass
 * your own to {@link #blocks(BlockPos)}.
 *
 * <p>Any {@link StructureProcessor}s configured on the generator are applied here, as blocks are
 * produced.
 */
public final class GeneratedStructure {

    private final List<PlacedPiece> placedPieces;
    private final List<WorldConnection> openConnections;
    private final long seed;
    private final boolean includeAir;
    private final BlockState markerFill;
    private final BlockPos origin;
    private final List<StructureProcessor> processors;

    GeneratedStructure(List<PlacedPiece> placedPieces,
                       List<WorldConnection> openConnections,
                       long seed,
                       boolean includeAir,
                       BlockState markerFill,
                       BlockPos origin,
                       List<StructureProcessor> processors) {
        this.placedPieces = List.copyOf(placedPieces);
        this.openConnections = List.copyOf(openConnections);
        this.seed = seed;
        this.includeAir = includeAir;
        this.markerFill = markerFill;
        this.origin = origin;
        this.processors = List.copyOf(processors);
    }

    /** @return every piece placed, in the order they were added (start piece first). */
    public List<PlacedPiece> placedPieces() {
        return placedPieces;
    }

    public int pieceCount() {
        return placedPieces.size();
    }

    /** @return connections that were never matched or capped &mdash; the structure's loose ends. */
    public List<WorldConnection> openConnections() {
        return openConnections;
    }

    /** @return the seed that produced this structure (re-run it to get the same result). */
    public long seed() {
        return seed;
    }

    /** @return the resolved world origin (placement + height), or {@code (0,0,0)} if none. */
    public BlockPos origin() {
        return origin;
    }

    /** @return {@code true} if nothing beyond the start piece could be placed. */
    public boolean isEmpty() {
        return placedPieces.isEmpty();
    }

    /** @return the overall bounding box in structure coordinates (before the origin shift). */
    public BoundingBox bounds() {
        BoundingBox box = null;
        for (PlacedPiece p : placedPieces) {
            box = box == null ? p.bounds() : box.encapsulate(p.bounds());
        }
        return box;
    }

    /** @return all block placements, positioned at the structure's resolved {@link #origin()}. */
    public List<BlockPlacement> blocks() {
        return assemble(origin);
    }

    /** @return all block placements, translated so the structure origin lands at {@code worldOrigin}. */
    public List<BlockPlacement> blocks(BlockPos worldOrigin) {
        return assemble(worldOrigin);
    }

    private List<BlockPlacement> assemble(BlockPos shift) {
        List<BlockPlacement> raw = new ArrayList<>();
        for (PlacedPiece p : placedPieces) {
            p.emit(raw, includeAir, markerFill);
        }

        boolean shifted = !shift.equals(BlockPos.ORIGIN);
        if (processors.isEmpty()) {
            if (!shifted) {
                return raw;
            }
            List<BlockPlacement> out = new ArrayList<>(raw.size());
            for (BlockPlacement p : raw) {
                out.add(new BlockPlacement(p.pos().add(shift), p.state()));
            }
            return out;
        }

        // Deterministic processing: seed the processor RNG from the structure seed.
        ProcessorContext ctx = new ProcessorContext(new Random(seed));
        List<BlockPlacement> out = new ArrayList<>(raw.size());
        for (BlockPlacement p : raw) {
            BlockPlacement placement = shifted
                    ? new BlockPlacement(p.pos().add(shift), p.state())
                    : p;
            for (StructureProcessor processor : processors) {
                placement = processor.process(placement, ctx);
                if (placement == null) {
                    break;
                }
            }
            if (placement != null) {
                out.add(placement);
            }
        }
        return out;
    }

    @Override
    public String toString() {
        return "GeneratedStructure[pieces=" + placedPieces.size()
                + ", open=" + openConnections.size()
                + ", origin=" + origin
                + ", bounds=" + bounds() + ", seed=" + seed + "]";
    }
}
