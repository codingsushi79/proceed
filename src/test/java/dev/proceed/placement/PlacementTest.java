package dev.proceed.placement;

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementTest {

    private PlacementContext ctx(long seed, int chunkX, int chunkZ) {
        return PlacementContext.builder(seed).chunk(chunkX, chunkZ).build();
    }

    @Test
    void randomSpreadPicksExactlyOneStartPerCell() {
        RandomSpreadPlacement placement = RandomSpreadPlacement.of(8, 3, 12345);
        long seed = 987654321L;

        // Within one 8x8 grid cell exactly one chunk should be the designated start.
        int starts = 0;
        for (int cx = 0; cx < 8; cx++) {
            for (int cz = 0; cz < 8; cz++) {
                if (placement.shouldStartInChunk(ctx(seed, cx, cz))) {
                    starts++;
                }
            }
        }
        assertEquals(1, starts, "expected exactly one start chunk per grid cell");
    }

    @Test
    void randomSpreadIsDeterministicForSeed() {
        RandomSpreadPlacement placement = RandomSpreadPlacement.of(16, 4, 55);
        OptionalLong a = placement.placementChunk(ctx(42L, 100, -37));
        OptionalLong b = placement.placementChunk(ctx(42L, 100, -37));
        assertEquals(a, b);
    }

    @Test
    void startChunkStaysWithinItsCell() {
        int spacing = 10, separation = 2;
        RandomSpreadPlacement placement = RandomSpreadPlacement.of(spacing, separation, 7);
        for (int cx = -20; cx < 20; cx++) {
            OptionalLong packed = placement.placementChunk(ctx(1L, cx, cx));
            assertTrue(packed.isPresent());
            int startX = StructurePlacement.unpackChunkX(packed.getAsLong());
            int cell = Math.floorDiv(cx, spacing);
            int offset = startX - cell * spacing;
            assertTrue(offset >= 0 && offset < spacing - separation,
                    "start offset " + offset + " out of range for cx=" + cx);
        }
    }

    @Test
    void conditionsComposeAndGateContext() {
        GenerationCondition desert = GenerationCondition.inBiomes("minecraft:desert");
        GenerationCondition overworld = GenerationCondition.inDimension("minecraft:overworld");
        GenerationCondition both = desert.and(overworld);

        PlacementContext good = PlacementContext.builder(1)
                .biome("minecraft:desert").dimension("minecraft:overworld").build();
        PlacementContext wrongBiome = PlacementContext.builder(1)
                .biome("minecraft:plains").dimension("minecraft:overworld").build();

        assertTrue(both.test(good));
        assertFalse(both.test(wrongBiome));
        assertTrue(GenerationCondition.not(desert).test(wrongBiome));
        assertTrue(GenerationCondition.any(desert, overworld).test(wrongBiome)); // overworld still matches
    }

    @Test
    void chanceConditionIsDeterministicPerChunk() {
        GenerationCondition c = GenerationCondition.chance(0.5);
        boolean first = c.test(ctx(9L, 3, 4));
        for (int i = 0; i < 5; i++) {
            assertEquals(first, c.test(ctx(9L, 3, 4)), "chance roll must be stable for a chunk");
        }
    }

    @Test
    void heightProvidersRespectRanges() {
        var rng = new java.util.Random(0);
        PlacementContext ctx = ctx(0, 0, 0);

        assertEquals(64, HeightProvider.constant(64).sample(rng, ctx));

        for (int i = 0; i < 1000; i++) {
            int u = HeightProvider.uniform(10, 20).sample(rng, ctx);
            assertTrue(u >= 10 && u <= 20, "uniform out of range: " + u);
            int t = HeightProvider.trapezoid(0, 100, 20).sample(rng, ctx);
            assertTrue(t >= 0 && t <= 100, "trapezoid out of range: " + t);
        }
    }

    @Test
    void terrainProjectionUsesSampler() {
        PlacementContext ctx = PlacementContext.builder(0)
                .block(50, 60)
                .heightSampler((x, z) -> 72)
                .build();
        assertEquals(71, HeightProvider.fromTerrain(-1).sample(new java.util.Random(0), ctx));
    }
}
