package dev.proceed.placement;

import java.util.Random;

/**
 * How structures are jittered within their spacing grid &mdash; matching Minecraft's
 * {@code RandomSpreadType}.
 */
public enum SpreadType {
    /** Uniform jitter across the cell. */
    LINEAR {
        @Override
        int sample(Random random, int bound) {
            return random.nextInt(bound);
        }
    },
    /** Triangular jitter, biased toward the cell centre (the vanilla default). */
    TRIANGULAR {
        @Override
        int sample(Random random, int bound) {
            return (random.nextInt(bound) + random.nextInt(bound)) / 2;
        }
    };

    abstract int sample(Random random, int bound);
}
