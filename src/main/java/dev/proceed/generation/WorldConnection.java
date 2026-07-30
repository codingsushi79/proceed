package dev.proceed.generation;

import dev.proceed.math.BlockPos;
import dev.proceed.math.Direction;
import dev.proceed.piece.ConnectionPoint;

/**
 * A {@link ConnectionPoint} transformed into the generated structure's coordinate space.
 *
 * <p>During generation these live on the frontier: each one is an opening that either gets matched
 * with a new piece or, if nothing fits, is left open (and possibly capped). After generation,
 * {@link GeneratedStructure#openConnections()} exposes the ones that were never filled &mdash;
 * useful if your mod wants to attach the structure to the surrounding world.
 */
public record WorldConnection(BlockPos position,
                              Direction facing,
                              String channel,
                              PlacedPiece owner,
                              ConnectionPoint source) {

    /** @return the cell a mating piece's opening should occupy (one block outward), plus spacing. */
    public BlockPos mateCell(int spacing) {
        return position.offset(facing, 1 + spacing);
    }
}
