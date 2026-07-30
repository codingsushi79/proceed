package dev.proceed.placement;

import java.util.List;
import java.util.Random;

/**
 * Chooses the Y the structure's start piece anchors to &mdash; the counterpart of Minecraft's
 * {@code HeightProvider} used by jigsaw {@code start_height}.
 *
 * <p>Factory methods cover the vanilla shapes: {@link #constant}, {@link #uniform},
 * {@link #trapezoid} and {@link #weighted}. {@link #fromTerrain} projects onto the terrain via the
 * context's {@link HeightSampler} (equivalent to {@code project_start_to_heightmap}).
 */
@FunctionalInterface
public interface HeightProvider {

    /** @return the chosen anchor Y. */
    int sample(Random random, PlacementContext ctx);

    /** Always the same Y. */
    static HeightProvider constant(int y) {
        return (random, ctx) -> y;
    }

    /** A uniformly random Y in {@code [minY, maxY]} (inclusive). */
    static HeightProvider uniform(int minY, int maxY) {
        if (maxY < minY) {
            throw new IllegalArgumentException("maxY must be >= minY");
        }
        return (random, ctx) -> minY + random.nextInt(maxY - minY + 1);
    }

    /**
     * A trapezoidal distribution over {@code [minY, maxY]} with a flat plateau of the given width,
     * biased toward the middle. A {@code plateau} of 0 gives a triangular distribution; matches
     * vanilla {@code TrapezoidHeight}.
     */
    static HeightProvider trapezoid(int minY, int maxY, int plateau) {
        if (maxY < minY) {
            throw new IllegalArgumentException("maxY must be >= minY");
        }
        int range = maxY - minY;
        return (random, ctx) -> {
            if (plateau >= range) {
                return minY + random.nextInt(range + 1);
            }
            int slope = (range - plateau) / 2;
            int top = range - slope;
            return minY + random.nextInt(top - slope + 1) + random.nextInt(slope + 1);
        };
    }

    /** Picks among weighted Y values. */
    static HeightProvider weighted(List<WeightedY> entries) {
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("weighted height needs at least one entry");
        }
        List<WeightedY> copy = List.copyOf(entries);
        double total = copy.stream().mapToDouble(WeightedY::weight).sum();
        return (random, ctx) -> {
            double r = random.nextDouble() * total;
            double acc = 0;
            for (WeightedY e : copy) {
                acc += e.weight();
                if (r < acc) {
                    return e.y();
                }
            }
            return copy.get(copy.size() - 1).y();
        };
    }

    /**
     * Projects onto the terrain: samples the context's {@link HeightSampler} at the anchor column
     * and adds {@code offset}. Falls back to {@code offset} if no sampler is present.
     */
    static HeightProvider fromTerrain(int offset) {
        return (random, ctx) -> ctx.terrainHeight(ctx.blockX(), ctx.blockZ(), 0) + offset;
    }

    /** One weighted Y value for {@link #weighted}. */
    record WeightedY(int y, double weight) {
    }
}
