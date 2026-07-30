package dev.proceed.placement;

import java.util.Random;

/**
 * The world information a mod supplies when asking Proceed whether &mdash; and where &mdash; a
 * structure should generate. It is the engine-agnostic stand-in for Minecraft's
 * {@code GenerationContext}.
 *
 * <p>You fill in whatever your conditions and placement need: the world seed, the chunk being
 * populated, the biome and dimension ids, and optionally a {@link HeightSampler} so the structure
 * can be projected onto the terrain. Anything you leave out simply isn't tested.
 *
 * <pre>{@code
 * PlacementContext ctx = PlacementContext.builder(worldSeed)
 *         .chunk(chunkX, chunkZ)
 *         .biome("minecraft:desert")
 *         .dimension("minecraft:overworld")
 *         .heightSampler((x, z) -> chunkGen.getFirstFreeHeight(x, z, ...))
 *         .build();
 * }</pre>
 */
public final class PlacementContext {

    private final long seed;
    private final int chunkX;
    private final int chunkZ;
    private final int blockX;
    private final int blockZ;
    private final String biome;
    private final String dimension;
    private final HeightSampler heightSampler;

    private PlacementContext(Builder b) {
        this.seed = b.seed;
        this.chunkX = b.chunkX;
        this.chunkZ = b.chunkZ;
        this.blockX = b.blockX != null ? b.blockX : (b.chunkX << 4) + 8;
        this.blockZ = b.blockZ != null ? b.blockZ : (b.chunkZ << 4) + 8;
        this.biome = b.biome;
        this.dimension = b.dimension;
        this.heightSampler = b.heightSampler;
    }

    public long seed() { return seed; }
    public int chunkX() { return chunkX; }
    public int chunkZ() { return chunkZ; }

    /** @return the world X the structure anchors to (chunk centre unless set explicitly). */
    public int blockX() { return blockX; }

    /** @return the world Z the structure anchors to (chunk centre unless set explicitly). */
    public int blockZ() { return blockZ; }

    /** @return the biome id, or {@code ""} if none was supplied. */
    public String biome() { return biome; }

    /** @return the dimension id, or {@code ""} if none was supplied. */
    public String dimension() { return dimension; }

    /** @return the terrain height sampler, or {@code null} if none was supplied. */
    public HeightSampler heightSampler() { return heightSampler; }

    /** @return the terrain height at {@code (x, z)}, or {@code fallback} if no sampler is set. */
    public int terrainHeight(int x, int z, int fallback) {
        return heightSampler != null ? heightSampler.height(x, z) : fallback;
    }

    /**
     * @return a deterministic {@link Random} derived from the seed, chunk and {@code salt}. Use a
     * distinct salt per purpose so independent random decisions don't correlate.
     */
    public Random random(long salt) {
        long s = seed
                + (long) chunkX * 341873128712L
                + (long) chunkZ * 132897987541L
                + salt * 0x9E3779B97F4A7C15L;
        return new Random(s);
    }

    public Random random() {
        return random(0L);
    }

    public static Builder builder(long seed) {
        return new Builder(seed);
    }

    /** Fluent builder for {@link PlacementContext}. */
    public static final class Builder {
        private final long seed;
        private int chunkX;
        private int chunkZ;
        private Integer blockX;
        private Integer blockZ;
        private String biome = "";
        private String dimension = "";
        private HeightSampler heightSampler;

        private Builder(long seed) {
            this.seed = seed;
        }

        /** Sets the chunk; the anchor block defaults to its centre unless {@link #block} is set. */
        public Builder chunk(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            return this;
        }

        /** Sets an explicit anchor block position (and derives the chunk from it if unset). */
        public Builder block(int blockX, int blockZ) {
            this.blockX = blockX;
            this.blockZ = blockZ;
            this.chunkX = blockX >> 4;
            this.chunkZ = blockZ >> 4;
            return this;
        }

        public Builder biome(String biome) {
            this.biome = biome == null ? "" : biome;
            return this;
        }

        public Builder dimension(String dimension) {
            this.dimension = dimension == null ? "" : dimension;
            return this;
        }

        public Builder heightSampler(HeightSampler sampler) {
            this.heightSampler = sampler;
            return this;
        }

        public PlacementContext build() {
            return new PlacementContext(this);
        }
    }
}
