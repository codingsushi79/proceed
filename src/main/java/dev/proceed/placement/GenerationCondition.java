package dev.proceed.placement;

import java.util.Arrays;
import java.util.Set;

/**
 * A predicate that decides whether a structure may start, given a {@link PlacementContext}.
 *
 * <p>This mirrors the checks Minecraft runs before a structure generates &mdash; biome allow-lists,
 * dimension, rarity rolls &mdash; but stays engine-agnostic: it only looks at what your
 * {@link PlacementContext} provides. Combine conditions with {@link #and}, {@link #or},
 * {@link #not}, {@link #all} and {@link #any}.
 *
 * <pre>{@code
 * GenerationCondition canSpawn = GenerationCondition.all(
 *         GenerationCondition.inDimension("minecraft:overworld"),
 *         GenerationCondition.inBiomes("minecraft:desert", "minecraft:badlands"),
 *         GenerationCondition.chance(0.25));
 * }</pre>
 */
@FunctionalInterface
public interface GenerationCondition {

    /** A salt so rarity rolls don't correlate with other random decisions. */
    long CHANCE_SALT = 0x5C4B1A2E7D3F09L;

    boolean test(PlacementContext ctx);

    default GenerationCondition and(GenerationCondition other) {
        return ctx -> this.test(ctx) && other.test(ctx);
    }

    default GenerationCondition or(GenerationCondition other) {
        return ctx -> this.test(ctx) || other.test(ctx);
    }

    static GenerationCondition not(GenerationCondition condition) {
        return ctx -> !condition.test(ctx);
    }

    static GenerationCondition always() {
        return ctx -> true;
    }

    static GenerationCondition never() {
        return ctx -> false;
    }

    /** True only if every condition passes (an empty list passes). */
    static GenerationCondition all(GenerationCondition... conditions) {
        GenerationCondition[] copy = conditions.clone();
        return ctx -> {
            for (GenerationCondition c : copy) {
                if (!c.test(ctx)) {
                    return false;
                }
            }
            return true;
        };
    }

    /** True if any condition passes (an empty list fails). */
    static GenerationCondition any(GenerationCondition... conditions) {
        GenerationCondition[] copy = conditions.clone();
        return ctx -> {
            for (GenerationCondition c : copy) {
                if (c.test(ctx)) {
                    return true;
                }
            }
            return false;
        };
    }

    /** Passes only in one of the listed biome ids. */
    static GenerationCondition inBiomes(String... biomes) {
        Set<String> set = Set.copyOf(Arrays.asList(biomes));
        return ctx -> set.contains(ctx.biome());
    }

    /** Passes in any biome except the listed ones. */
    static GenerationCondition notInBiomes(String... biomes) {
        Set<String> set = Set.copyOf(Arrays.asList(biomes));
        return ctx -> !set.contains(ctx.biome());
    }

    /** Passes only in the given dimension id. */
    static GenerationCondition inDimension(String dimension) {
        return ctx -> dimension.equals(ctx.dimension());
    }

    /** A deterministic rarity roll: passes with the given probability per chunk. */
    static GenerationCondition chance(double probability) {
        return ctx -> ctx.random(CHANCE_SALT).nextDouble() < probability;
    }

    /**
     * Passes only where the sampled terrain height at the anchor column falls within
     * {@code [minY, maxY]}. Requires a {@link HeightSampler} on the context; if none is set the
     * condition passes (there is nothing to test against).
     */
    static GenerationCondition terrainHeightBetween(int minY, int maxY) {
        return ctx -> {
            if (ctx.heightSampler() == null) {
                return true;
            }
            int h = ctx.terrainHeight(ctx.blockX(), ctx.blockZ(), minY);
            return h >= minY && h <= maxY;
        };
    }
}
