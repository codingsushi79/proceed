package dev.proceed.math;

/**
 * An immutable integer block coordinate.
 *
 * <p>Depending on context a {@code BlockPos} is either <em>local</em> to a piece
 * (0-based, within the piece's own size) or <em>world</em>-relative (offset applied by
 * the generator). The two never mix silently &mdash; the generator does the conversion.
 */
public record BlockPos(int x, int y, int z) {

    public static final BlockPos ORIGIN = new BlockPos(0, 0, 0);

    public BlockPos add(int dx, int dy, int dz) {
        return new BlockPos(x + dx, y + dy, z + dz);
    }

    public BlockPos add(BlockPos other) {
        return new BlockPos(x + other.x, y + other.y, z + other.z);
    }

    public BlockPos subtract(BlockPos other) {
        return new BlockPos(x - other.x, y - other.y, z - other.z);
    }

    /** @return this position moved one block in {@code dir}. */
    public BlockPos offset(Direction dir) {
        return add(dir.dx(), dir.dy(), dir.dz());
    }

    /** @return this position moved {@code amount} blocks in {@code dir}. */
    public BlockPos offset(Direction dir, int amount) {
        return add(dir.dx() * amount, dir.dy() * amount, dir.dz() * amount);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
