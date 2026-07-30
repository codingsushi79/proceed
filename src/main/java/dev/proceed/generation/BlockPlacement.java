package dev.proceed.generation;

import dev.proceed.litematic.BlockState;
import dev.proceed.math.BlockPos;

/**
 * A single block the generated structure wants placed in the world: a position and the block
 * state to set there.
 *
 * <p>Your mod consumes these &mdash; the library never touches the world itself, keeping it
 * engine-agnostic:
 *
 * <pre>{@code
 * for (BlockPlacement p : structure.blocks(worldOrigin)) {
 *     // translate p.state().toString() into your loader's BlockState and place it
 *     level.setBlock(toMc(p.pos()), parse(p.state()), 2);
 * }
 * }</pre>
 */
public record BlockPlacement(BlockPos pos, BlockState state) {
}
