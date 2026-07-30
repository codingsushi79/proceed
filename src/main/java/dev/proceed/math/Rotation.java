package dev.proceed.math;

/**
 * A yaw rotation around the vertical axis, in 90&deg; steps.
 *
 * <p>Minecraft structures are only ever rotated around Y (you never place a building on its
 * side), so these four values cover every orientation the generator produces.
 */
public enum Rotation {
    /** No rotation. */
    NONE,
    /** 90&deg; clockwise (viewed from above): {@code NORTH -> EAST}. */
    CLOCKWISE_90,
    /** 180&deg;. */
    CLOCKWISE_180,
    /** 270&deg; clockwise, i.e. 90&deg; counter-clockwise: {@code NORTH -> WEST}. */
    CLOCKWISE_270;

    /** All four rotations, handy for iterating candidate orientations. */
    public static final Rotation[] ALL = values();

    /** @return the direction after applying this rotation. */
    public Direction rotate(Direction dir) {
        return switch (this) {
            case NONE -> dir;
            case CLOCKWISE_90 -> dir.rotateYClockwise();
            case CLOCKWISE_180 -> dir.rotateYClockwise().rotateYClockwise();
            case CLOCKWISE_270 -> dir.rotateYCounterclockwise();
        };
    }

    /**
     * Rotates a local block position within a piece of the given size, keeping every
     * coordinate non-negative (the rotated piece still starts at the origin).
     *
     * @param pos  the local position, {@code 0 <= pos < size} on each axis
     * @param size the piece size <em>before</em> rotation
     * @return the position within the rotated piece
     */
    public BlockPos rotate(BlockPos pos, BlockPos size) {
        int x = pos.x(), y = pos.y(), z = pos.z();
        int sx = size.x(), sz = size.z();
        return switch (this) {
            case NONE -> pos;
            case CLOCKWISE_90 -> new BlockPos(sz - 1 - z, y, x);
            case CLOCKWISE_180 -> new BlockPos(sx - 1 - x, y, sz - 1 - z);
            case CLOCKWISE_270 -> new BlockPos(z, y, sx - 1 - x);
        };
    }

    /** @return the piece's bounding size after this rotation (X and Z swap for 90&deg;/270&deg;). */
    public BlockPos rotate(BlockPos size) {
        return switch (this) {
            case NONE, CLOCKWISE_180 -> size;
            case CLOCKWISE_90, CLOCKWISE_270 -> new BlockPos(size.z(), size.y(), size.x());
        };
    }

    /** @return the rotation that undoes this one. */
    public Rotation inverse() {
        return switch (this) {
            case NONE -> NONE;
            case CLOCKWISE_90 -> CLOCKWISE_270;
            case CLOCKWISE_180 -> CLOCKWISE_180;
            case CLOCKWISE_270 -> CLOCKWISE_90;
        };
    }

    /** @return the composition {@code this} then {@code then}. */
    public Rotation andThen(Rotation then) {
        int steps = (this.ordinal() + then.ordinal()) % 4;
        return ALL[steps];
    }
}
