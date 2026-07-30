package dev.proceed.processor;

import java.util.Random;

/**
 * The information a {@link StructureProcessor} may use while transforming a block.
 *
 * <p>The {@link #random()} here is seeded deterministically from the structure seed, so processed
 * output is reproducible for a given seed.
 */
public final class ProcessorContext {

    private final Random random;

    public ProcessorContext(Random random) {
        this.random = random;
    }

    /** @return the deterministic RNG for this generation run. */
    public Random random() {
        return random;
    }
}
