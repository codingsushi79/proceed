package dev.proceed.litematic;

import dev.proceed.math.BlockPos;
import dev.proceed.math.BoundingBox;

import java.util.Map;

/**
 * A normalized, engine-agnostic block grid: the neutral form every {@code .litematic} is loaded
 * into before it becomes a structure piece.
 *
 * <p>The grid always starts at the origin {@code (0, 0, 0)} and spans {@link #size()} blocks. If
 * the source schematic used multiple regions or negative sizes, those quirks are resolved here so
 * the rest of the library only ever deals with a simple 0-based box.
 *
 * <p>Empty cells are represented by an air {@link BlockState}; there are no {@code null}s.
 */
public final class Schematic {

    /** The block returned for any empty cell. */
    public static final BlockState AIR = new BlockState("minecraft:air", Map.of());

    private final BlockPos size;
    private final BlockState[] blocks; // null entries mean air

    Schematic(BlockPos size, BlockState[] blocks) {
        this.size = size;
        this.blocks = blocks;
    }

    /** @return the grid dimensions in blocks. */
    public BlockPos size() {
        return size;
    }

    public int sizeX() { return size.x(); }
    public int sizeY() { return size.y(); }
    public int sizeZ() { return size.z(); }

    /** @return the total number of cells (including air). */
    public int volume() {
        return size.x() * size.y() * size.z();
    }

    /** @return the bounding box {@code (0,0,0)..(size-1)}. */
    public BoundingBox bounds() {
        return BoundingBox.fromSize(BlockPos.ORIGIN, size);
    }

    public boolean inBounds(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0
                && x < size.x() && y < size.y() && z < size.z();
    }

    private int index(int x, int y, int z) {
        return (y * size.z() + z) * size.x() + x;
    }

    /** @return the block at the given local position, or {@link #AIR} if empty/out of bounds. */
    public BlockState getBlockState(int x, int y, int z) {
        if (!inBounds(x, y, z)) {
            return AIR;
        }
        BlockState s = blocks[index(x, y, z)];
        return s == null ? AIR : s;
    }

    public BlockState getBlockState(BlockPos pos) {
        return getBlockState(pos.x(), pos.y(), pos.z());
    }

    public boolean isAir(int x, int y, int z) {
        return getBlockState(x, y, z).isAir();
    }
}
