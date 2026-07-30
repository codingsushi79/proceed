package dev.proceed.placement;

import java.util.OptionalLong;
import java.util.Random;

/**
 * The standard spacing/separation grid, reproducing Minecraft's
 * {@code RandomSpreadStructurePlacement} exactly (same LCG, same salt maths), so structures spread
 * out the way players expect &mdash; roughly every {@code spacing} chunks, never closer than
 * {@code separation} chunks.
 *
 * <ul>
 *     <li><b>spacing</b> &mdash; the grid cell size in chunks (average distance between structures)</li>
 *     <li><b>separation</b> &mdash; the minimum distance in chunks; must be {@code < spacing}</li>
 *     <li><b>salt</b> &mdash; a per-structure constant so different structures don't overlap grids</li>
 *     <li><b>spreadType</b> &mdash; how the start chunk is jittered within its cell</li>
 * </ul>
 *
 * <p>For reference, vanilla villages use {@code spacing=34, separation=8}.
 */
public final class RandomSpreadPlacement implements StructurePlacement {

    private final int spacing;
    private final int separation;
    private final int salt;
    private final SpreadType spreadType;

    public RandomSpreadPlacement(int spacing, int separation, int salt, SpreadType spreadType) {
        if (spacing <= 0) {
            throw new IllegalArgumentException("spacing must be positive");
        }
        if (separation < 0 || separation >= spacing) {
            throw new IllegalArgumentException("separation must be in [0, spacing)");
        }
        this.spacing = spacing;
        this.separation = separation;
        this.salt = salt;
        this.spreadType = spreadType;
    }

    /** Convenience with the vanilla-default triangular spread. */
    public static RandomSpreadPlacement of(int spacing, int separation, int salt) {
        return new RandomSpreadPlacement(spacing, separation, salt, SpreadType.TRIANGULAR);
    }

    public int spacing() { return spacing; }
    public int separation() { return separation; }
    public int salt() { return salt; }
    public SpreadType spreadType() { return spreadType; }

    @Override
    public OptionalLong placementChunk(PlacementContext ctx) {
        int cellX = Math.floorDiv(ctx.chunkX(), spacing);
        int cellZ = Math.floorDiv(ctx.chunkZ(), spacing);

        // java.util.Random's constructor applies the same seed scramble as Minecraft's
        // LegacyRandomSource.setSeed, and its nextInt(bound) matches, so this is bit-for-bit
        // compatible with setLargeFeatureWithSalt(worldSeed, cellX, cellZ, salt).
        long featureSeed = (long) cellX * 341873128712L
                + (long) cellZ * 132897987541L
                + ctx.seed()
                + salt;
        Random random = new Random(featureSeed);

        int bound = spacing - separation;
        int offsetX = spreadType.sample(random, bound);
        int offsetZ = spreadType.sample(random, bound);

        int startChunkX = cellX * spacing + offsetX;
        int startChunkZ = cellZ * spacing + offsetZ;
        return OptionalLong.of(StructurePlacement.packChunk(startChunkX, startChunkZ));
    }
}
