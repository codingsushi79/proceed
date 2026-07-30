package dev.proceed.piece;

import dev.proceed.litematic.BlockState;
import dev.proceed.litematic.Schematic;
import dev.proceed.math.BlockPos;

/**
 * Turns marker blocks placed inside a schematic into {@link ConnectionPoint}s.
 *
 * <p>This is the intuitive authoring workflow: in your world editor you drop a recognisable block
 * (say purple wool, or an oriented block like a stair) wherever a piece should be able to connect,
 * then a scanner finds them at load time. See {@link Markers} for ready-made scanners.
 *
 * <p>Every cell of the schematic is offered to the scanner; return a {@link ConnectionPoint} for a
 * marker cell (its {@link ConnectionPoint#position()} <em>must</em> equal {@code pos}), or
 * {@code null} for an ordinary block. Marker cells are recorded so they can be replaced with air
 * (or anything else) when the structure is written to the world.
 */
@FunctionalInterface
public interface MarkerScanner {

    /**
     * @param schematic the piece being scanned
     * @param pos       the cell under inspection
     * @param state     the block at {@code pos}
     * @return a connection point anchored at {@code pos}, or {@code null} if this is not a marker
     */
    ConnectionPoint scan(Schematic schematic, BlockPos pos, BlockState state);
}
