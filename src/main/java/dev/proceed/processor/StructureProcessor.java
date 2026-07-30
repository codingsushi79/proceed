package dev.proceed.processor;

import dev.proceed.generation.BlockPlacement;

/**
 * Transforms the blocks of a generated structure as they are emitted &mdash; the engine-agnostic
 * analogue of Minecraft's {@code StructureProcessor}.
 *
 * <p>Processors run in order over every {@link BlockPlacement}. Return a replacement placement to
 * change the block (or its position), return the placement unchanged to keep it, or return
 * {@code null} to drop the block entirely (e.g. for a ruined, partially-collapsed look).
 *
 * <p>See {@link Processors} for ready-made processors: rule-based replacement, integrity/rot,
 * and simple swaps.
 */
@FunctionalInterface
public interface StructureProcessor {

    /**
     * @param placement the block about to be placed
     * @param ctx       shared processing context (deterministic RNG)
     * @return the transformed placement, or {@code null} to remove this block
     */
    BlockPlacement process(BlockPlacement placement, ProcessorContext ctx);
}
