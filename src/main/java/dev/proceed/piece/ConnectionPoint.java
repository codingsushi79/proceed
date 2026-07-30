package dev.proceed.piece;

import dev.proceed.math.BlockPos;
import dev.proceed.math.Direction;

/**
 * A relative attachment point on a {@link StructurePiece} &mdash; the "entry/exit" doorways the
 * generator wires together.
 *
 * <p>A connection point carries three things:
 * <ul>
 *     <li>{@link #position()} &mdash; where it sits, in the piece's own local coordinates</li>
 *     <li>{@link #facing()} &mdash; the outward direction a neighbour attaches from</li>
 *     <li>{@link #channel()} &mdash; a label; two points only connect if their channels match
 *         (e.g. {@code "corridor"}, {@code "door"}, {@code "pipe"})</li>
 * </ul>
 *
 * <p>Two points mate when their channels are equal and their facings are opposite &mdash; a
 * corridor exit facing {@code EAST} accepts a room entrance facing {@code WEST}.
 */
public record ConnectionPoint(String id, BlockPos position, Direction facing, String channel) {

    public ConnectionPoint {
        if (position == null || facing == null || channel == null) {
            throw new IllegalArgumentException("connection point fields must be non-null");
        }
    }

    /** Convenience: an unnamed connection point (id derived from its channel and position). */
    public static ConnectionPoint of(BlockPos position, Direction facing, String channel) {
        String id = channel + "@" + position.x() + "," + position.y() + "," + position.z();
        return new ConnectionPoint(id, position, facing, channel);
    }

    /** @return {@code true} if a point on {@code channel} facing the opposite way could mate here. */
    public boolean canMateWith(ConnectionPoint other) {
        return channel.equals(other.channel) && facing == other.facing.opposite();
    }
}
