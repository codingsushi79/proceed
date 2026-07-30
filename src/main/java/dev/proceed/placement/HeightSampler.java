package dev.proceed.placement;

/**
 * Supplies the terrain surface height at a world column &mdash; your bridge to the world's
 * heightmap.
 *
 * <p>Proceed never reads the world itself, so when you want a structure to sit on the ground
 * (heightmap projection) or a condition to test terrain height, you hand it one of these. In a mod
 * it usually wraps {@code chunkGenerator.getFirstFreeHeight(x, z, ...)} or a
 * {@code Heightmap.Types.WORLD_SURFACE_WG} lookup.
 */
@FunctionalInterface
public interface HeightSampler {

    /** @return the surface Y at world column {@code (x, z)}. */
    int height(int x, int z);
}
