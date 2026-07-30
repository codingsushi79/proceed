package dev.proceed.piece;

import dev.proceed.math.Direction;

/**
 * Ready-made {@link MarkerScanner}s for the common ways of marking connection points.
 *
 * <p>The recommended workflow uses an <em>oriented</em> marker block &mdash; a stair, observer,
 * piston or any block with a {@code facing} property &mdash; so a single scanner reads both the
 * location and the outward direction straight from your build:
 *
 * <pre>{@code
 * // Every purple-glazed-terracotta block becomes a "corridor" connection point,
 * // facing whichever way the block's `facing` property points.
 * MarkerScanner corridors = Markers.byBlock("minecraft:purple_glazed_terracotta", "corridor");
 * }</pre>
 *
 * <p>If your marker block has no {@code facing} property, use
 * {@link #byBlock(String, String, Direction)} to state the direction explicitly.
 */
public final class Markers {

    private Markers() {
    }

    /**
     * Marks connection points on every block whose id equals {@code blockId}. The connection
     * channel is the block id itself and the facing is read from the block's {@code facing}
     * property.
     */
    public static MarkerScanner byBlock(String blockId) {
        return byBlock(blockId, blockId);
    }

    /**
     * Marks connection points on every block whose id equals {@code blockId}, assigning them the
     * given {@code channel}. The facing is read from the block's {@code facing} property.
     */
    public static MarkerScanner byBlock(String blockId, String channel) {
        return (schematic, pos, state) -> {
            if (!state.name().equals(blockId)) {
                return null;
            }
            String facing = state.property("facing");
            if (facing == null) {
                throw new IllegalStateException("marker block '" + blockId + "' at " + pos
                        + " has no 'facing' property; use Markers.byBlock(id, channel, direction) "
                        + "to set the direction explicitly");
            }
            return new ConnectionPoint(channel + "@" + pos.x() + "," + pos.y() + "," + pos.z(),
                    pos, Direction.byName(facing), channel);
        };
    }

    /**
     * Marks connection points on every block whose id equals {@code blockId}, with a fixed
     * {@code channel} and {@code facing} regardless of the block's own properties.
     */
    public static MarkerScanner byBlock(String blockId, String channel, Direction facing) {
        return (schematic, pos, state) -> {
            if (!state.name().equals(blockId)) {
                return null;
            }
            return new ConnectionPoint(channel + "@" + pos.x() + "," + pos.y() + "," + pos.z(),
                    pos, facing, channel);
        };
    }

    /** Combines several scanners; the first one to return a point for a cell wins. */
    public static MarkerScanner any(MarkerScanner... scanners) {
        return (schematic, pos, state) -> {
            for (MarkerScanner s : scanners) {
                ConnectionPoint p = s.scan(schematic, pos, state);
                if (p != null) {
                    return p;
                }
            }
            return null;
        };
    }
}
