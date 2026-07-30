package dev.proceed.math;

/**
 * The six cardinal directions, matching Minecraft's block-facing conventions.
 *
 * <p>The coordinate system is the same one Minecraft uses:
 * <ul>
 *     <li>{@code +X} is east, {@code -X} is west</li>
 *     <li>{@code +Y} is up, {@code -Y} is down</li>
 *     <li>{@code +Z} is south, {@code -Z} is north</li>
 * </ul>
 *
 * <p>A connection point's direction always points <em>outward</em> from its piece &mdash;
 * the way a neighbouring piece should attach.
 */
public enum Direction {
    DOWN(0, -1, 0, Axis.Y),
    UP(0, 1, 0, Axis.Y),
    NORTH(0, 0, -1, Axis.Z),
    SOUTH(0, 0, 1, Axis.Z),
    WEST(-1, 0, 0, Axis.X),
    EAST(1, 0, 0, Axis.X);

    /** The axis a direction runs along. */
    public enum Axis { X, Y, Z }

    private final int dx;
    private final int dy;
    private final int dz;
    private final Axis axis;

    Direction(int dx, int dy, int dz, Axis axis) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.axis = axis;
    }

    public int dx() { return dx; }
    public int dy() { return dy; }
    public int dz() { return dz; }
    public Axis axis() { return axis; }

    /** @return {@code true} for {@link #UP} and {@link #DOWN}. */
    public boolean isVertical() {
        return axis == Axis.Y;
    }

    /** @return {@code true} for the four compass directions. */
    public boolean isHorizontal() {
        return axis != Axis.Y;
    }

    /** @return the direction pointing the opposite way. */
    public Direction opposite() {
        return switch (this) {
            case DOWN -> UP;
            case UP -> DOWN;
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case EAST -> WEST;
        };
    }

    /**
     * Rotates this direction 90&deg; clockwise around the vertical (Y) axis, following the
     * compass order {@code NORTH -> EAST -> SOUTH -> WEST}. Vertical directions are unchanged.
     */
    public Direction rotateYClockwise() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
            default -> this;
        };
    }

    /** Rotates this direction 90&deg; counter-clockwise around the vertical axis. */
    public Direction rotateYCounterclockwise() {
        return switch (this) {
            case NORTH -> WEST;
            case WEST -> SOUTH;
            case SOUTH -> EAST;
            case EAST -> NORTH;
            default -> this;
        };
    }

    /**
     * Parses a Minecraft direction name ({@code "north"}, {@code "up"}, &hellip;).
     *
     * @throws IllegalArgumentException if the name is not one of the six directions
     */
    public static Direction byName(String name) {
        return valueOf(name.trim().toUpperCase());
    }

    /** @return the lowercase name Minecraft uses in block states ({@code "north"} &hellip;). */
    public String blockStateName() {
        return name().toLowerCase();
    }
}
