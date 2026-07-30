package dev.proceed.placement;

import java.util.OptionalLong;

/**
 * Decides which chunks a structure may start in &mdash; the engine-agnostic analogue of
 * Minecraft's {@code StructurePlacement}.
 *
 * <p>The generator asks {@link #placementChunk} for a context's grid cell: an empty result means
 * "no structure starts near here", otherwise the returned packed chunk is the one chunk in that
 * cell allowed to start. When it equals the context's own chunk, generation proceeds.
 *
 * <p>See {@link RandomSpreadPlacement} for the standard spacing/separation grid.
 */
@FunctionalInterface
public interface StructurePlacement {

    /** Packs a chunk coordinate into a single long. */
    static long packChunk(int chunkX, int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | ((long) chunkZ << 32);
    }

    static int unpackChunkX(long packed) {
        return (int) (packed & 0xFFFFFFFFL);
    }

    static int unpackChunkZ(long packed) {
        return (int) (packed >> 32);
    }

    /**
     * @return the packed start chunk for the grid cell containing {@code ctx}, or empty if this
     * placement produces none there.
     */
    OptionalLong placementChunk(PlacementContext ctx);

    /** @return {@code true} if a structure should start in {@code ctx}'s own chunk. */
    default boolean shouldStartInChunk(PlacementContext ctx) {
        OptionalLong packed = placementChunk(ctx);
        if (packed.isEmpty()) {
            return false;
        }
        long p = packed.getAsLong();
        return unpackChunkX(p) == ctx.chunkX() && unpackChunkZ(p) == ctx.chunkZ();
    }
}
