package dev.proceed.litematic;

import dev.proceed.math.BlockPos;
import dev.proceed.nbt.NbtCompound;
import dev.proceed.nbt.NbtIo;
import dev.proceed.nbt.NbtList;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a Litematica {@code .litematic} file and flattens it into a single {@link Schematic}.
 *
 * <p>All the format's quirks are handled here:
 * <ul>
 *     <li>gzip-compressed NBT (auto-detected)</li>
 *     <li>the bit-packed {@code BlockStates} index array (see {@link LitematicaBitArray})</li>
 *     <li>multiple regions, merged into one coordinate space</li>
 *     <li>regions with negative sizes / offset positions, normalized to a 0-based grid</li>
 * </ul>
 *
 * <p>Typical use is just {@link #load(Path)} followed by {@link #toSchematic()} &mdash; or, more
 * commonly, letting {@code StructurePiece.fromFile(...)} do both for you.
 */
public final class Litematic {

    private final String name;
    private final String author;
    private final String description;
    private final int minecraftDataVersion;
    private final List<Region> regions;

    private Litematic(String name, String author, String description,
                      int minecraftDataVersion, List<Region> regions) {
        this.name = name;
        this.author = author;
        this.description = description;
        this.minecraftDataVersion = minecraftDataVersion;
        this.regions = regions;
    }

    public String name() { return name; }
    public String author() { return author; }
    public String description() { return description; }
    public int minecraftDataVersion() { return minecraftDataVersion; }
    public int regionCount() { return regions.size(); }

    /** Loads a {@code .litematic} from disk. */
    public static Litematic load(Path file) {
        try {
            return parse(NbtIo.read(file));
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read litematic: " + file, e);
        }
    }

    /** Loads a {@code .litematic} from a stream (e.g. a mod resource). */
    public static Litematic load(InputStream in) {
        try {
            return parse(NbtIo.read(in));
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read litematic stream", e);
        }
    }

    private static Litematic parse(NbtCompound root) {
        NbtCompound meta = root.contains("Metadata") ? root.getCompound("Metadata") : new NbtCompound();
        String name = meta.getString("Name", "");
        String author = meta.getString("Author", "");
        String description = meta.getString("Description", "");
        int dataVersion = root.getInt("MinecraftDataVersion", 0);

        NbtCompound regionsTag = root.getCompound("Regions");
        List<Region> regions = new ArrayList<>();
        for (String regionName : regionsTag.keys()) {
            regions.add(Region.parse(regionName, regionsTag.getCompound(regionName)));
        }
        if (regions.isEmpty()) {
            throw new IllegalStateException("litematic '" + name + "' contains no regions");
        }
        return new Litematic(name, author, description, dataVersion, regions);
    }

    /** Flattens every region into one normalized {@link Schematic} anchored at the origin. */
    public Schematic toSchematic() {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Region r : regions) {
            minX = Math.min(minX, r.worldMinX());
            minY = Math.min(minY, r.worldMinY());
            minZ = Math.min(minZ, r.worldMinZ());
            maxX = Math.max(maxX, r.worldMinX() + r.absX() - 1);
            maxY = Math.max(maxY, r.worldMinY() + r.absY() - 1);
            maxZ = Math.max(maxZ, r.worldMinZ() + r.absZ() - 1);
        }

        BlockPos size = new BlockPos(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
        BlockState[] grid = new BlockState[size.x() * size.y() * size.z()];

        for (Region r : regions) {
            r.writeInto(grid, size, minX, minY, minZ);
        }
        return new Schematic(size, grid);
    }

    /** A single named region within the file. */
    private static final class Region {
        final int posX, posY, posZ;
        final int sizeX, sizeY, sizeZ;
        final BlockState[] palette;
        final LitematicaBitArray blocks;

        Region(int posX, int posY, int posZ, int sizeX, int sizeY, int sizeZ,
               BlockState[] palette, LitematicaBitArray blocks) {
            this.posX = posX; this.posY = posY; this.posZ = posZ;
            this.sizeX = sizeX; this.sizeY = sizeY; this.sizeZ = sizeZ;
            this.palette = palette;
            this.blocks = blocks;
        }

        static Region parse(String name, NbtCompound tag) {
            NbtCompound pos = tag.getCompound("Position");
            NbtCompound size = tag.getCompound("Size");
            int px = pos.getInt("x"), py = pos.getInt("y"), pz = pos.getInt("z");
            int sx = size.getInt("x"), sy = size.getInt("y"), sz = size.getInt("z");

            NbtList paletteTag = tag.getList("BlockStatePalette");
            BlockState[] palette = new BlockState[paletteTag.size()];
            for (int i = 0; i < palette.length; i++) {
                palette[i] = BlockState.fromPalette(paletteTag.getCompound(i));
            }

            int absX = Math.abs(sx), absY = Math.abs(sy), absZ = Math.abs(sz);
            long volume = (long) absX * absY * absZ;
            int bits = LitematicaBitArray.bitsForPalette(palette.length);
            long[] states = tag.contains("BlockStates") ? tag.getLongArray("BlockStates") : new long[0];
            LitematicaBitArray blocks = new LitematicaBitArray(bits, volume, states);

            return new Region(px, py, pz, sx, sy, sz, palette, blocks);
        }

        int absX() { return Math.abs(sizeX); }
        int absY() { return Math.abs(sizeY); }
        int absZ() { return Math.abs(sizeZ); }

        // Minimum world corner, accounting for negative sizes that grow toward -axis.
        int worldMinX() { return posX + (sizeX < 0 ? sizeX + 1 : 0); }
        int worldMinY() { return posY + (sizeY < 0 ? sizeY + 1 : 0); }
        int worldMinZ() { return posZ + (sizeZ < 0 ? sizeZ + 1 : 0); }

        void writeInto(BlockState[] grid, BlockPos gridSize, int originX, int originY, int originZ) {
            int absX = absX(), absY = absY(), absZ = absZ();
            int signX = Integer.signum(sizeX == 0 ? 1 : sizeX);
            int signY = Integer.signum(sizeY == 0 ? 1 : sizeY);
            int signZ = Integer.signum(sizeZ == 0 ? 1 : sizeZ);

            for (int ay = 0; ay < absY; ay++) {
                for (int az = 0; az < absZ; az++) {
                    for (int ax = 0; ax < absX; ax++) {
                        int flat = (ay * absZ + az) * absX + ax;
                        int paletteIndex = blocks.get(flat);
                        if (paletteIndex >= palette.length) {
                            continue;
                        }
                        BlockState state = palette[paletteIndex];
                        if (state == null || state.isAir()) {
                            continue;
                        }
                        int worldX = posX + ax * signX;
                        int worldY = posY + ay * signY;
                        int worldZ = posZ + az * signZ;
                        int gx = worldX - originX;
                        int gy = worldY - originY;
                        int gz = worldZ - originZ;
                        int gi = (gy * gridSize.z() + gz) * gridSize.x() + gx;
                        grid[gi] = state;
                    }
                }
            }
        }
    }
}
