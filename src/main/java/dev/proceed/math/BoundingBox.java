package dev.proceed.math;

/**
 * An axis-aligned box of block coordinates, inclusive on both corners.
 *
 * <p>The generator uses bounding boxes for fast collision checks between placed pieces &mdash;
 * the same approach Minecraft's own jigsaw structures use.
 */
public record BoundingBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public BoundingBox {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("min corner must not exceed max corner");
        }
    }

    /** Builds a box from two arbitrary corners (they are sorted for you). */
    public static BoundingBox of(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new BoundingBox(
                Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
    }

    /** A box that starts at {@code origin} and spans {@code size} blocks. */
    public static BoundingBox fromSize(BlockPos origin, BlockPos size) {
        return new BoundingBox(
                origin.x(), origin.y(), origin.z(),
                origin.x() + size.x() - 1,
                origin.y() + size.y() - 1,
                origin.z() + size.z() - 1);
    }

    public BlockPos min() { return new BlockPos(minX, minY, minZ); }
    public BlockPos max() { return new BlockPos(maxX, maxY, maxZ); }

    public int sizeX() { return maxX - minX + 1; }
    public int sizeY() { return maxY - minY + 1; }
    public int sizeZ() { return maxZ - minZ + 1; }

    public long volume() {
        return (long) sizeX() * sizeY() * sizeZ();
    }

    /** @return {@code true} if the two boxes share at least one block. */
    public boolean intersects(BoundingBox o) {
        return minX <= o.maxX && maxX >= o.minX
                && minY <= o.maxY && maxY >= o.minY
                && minZ <= o.maxZ && maxZ >= o.minZ;
    }

    /** @return {@code true} if {@code pos} lies inside this box. */
    public boolean contains(BlockPos pos) {
        return pos.x() >= minX && pos.x() <= maxX
                && pos.y() >= minY && pos.y() <= maxY
                && pos.z() >= minZ && pos.z() <= maxZ;
    }

    /** @return {@code true} if {@code o} is fully contained within this box. */
    public boolean contains(BoundingBox o) {
        return o.minX >= minX && o.maxX <= maxX
                && o.minY >= minY && o.maxY <= maxY
                && o.minZ >= minZ && o.maxZ <= maxZ;
    }

    /** @return this box translated by the given offset. */
    public BoundingBox offset(BlockPos delta) {
        return new BoundingBox(
                minX + delta.x(), minY + delta.y(), minZ + delta.z(),
                maxX + delta.x(), maxY + delta.y(), maxZ + delta.z());
    }

    /** @return the smallest box containing both this box and {@code o}. */
    public BoundingBox encapsulate(BoundingBox o) {
        return new BoundingBox(
                Math.min(minX, o.minX), Math.min(minY, o.minY), Math.min(minZ, o.minZ),
                Math.max(maxX, o.maxX), Math.max(maxY, o.maxY), Math.max(maxZ, o.maxZ));
    }
}
