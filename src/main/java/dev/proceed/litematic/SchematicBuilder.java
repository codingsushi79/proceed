package dev.proceed.litematic;

import dev.proceed.math.BlockPos;

import java.util.Map;

/**
 * Builds a {@link Schematic} in code, for when you want structure pieces without authoring a
 * {@code .litematic} file (tests, generated shapes, quick prototypes).
 *
 * <pre>{@code
 * Schematic room = SchematicBuilder.of(5, 4, 5)
 *         .fill("minecraft:stone_bricks")   // solid block...
 *         .hollow("minecraft:air")          // ...then carve the interior
 *         .set(2, 1, 0, "minecraft:oak_door")
 *         .build();
 * }</pre>
 */
public final class SchematicBuilder {

    private final BlockPos size;
    private final BlockState[] blocks;

    private SchematicBuilder(int sizeX, int sizeY, int sizeZ) {
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            throw new IllegalArgumentException("schematic size must be positive");
        }
        this.size = new BlockPos(sizeX, sizeY, sizeZ);
        this.blocks = new BlockState[sizeX * sizeY * sizeZ];
    }

    public static SchematicBuilder of(int sizeX, int sizeY, int sizeZ) {
        return new SchematicBuilder(sizeX, sizeY, sizeZ);
    }

    public static SchematicBuilder of(BlockPos size) {
        return new SchematicBuilder(size.x(), size.y(), size.z());
    }

    private int index(int x, int y, int z) {
        return (y * size.z() + z) * size.x() + x;
    }

    /** Sets a cell from a block-state string, e.g. {@code "minecraft:oak_stairs[facing=east]"}. */
    public SchematicBuilder set(int x, int y, int z, String state) {
        return set(x, y, z, parse(state));
    }

    public SchematicBuilder set(int x, int y, int z, BlockState state) {
        blocks[index(x, y, z)] = state;
        return this;
    }

    public SchematicBuilder set(BlockPos pos, String state) {
        return set(pos.x(), pos.y(), pos.z(), state);
    }

    /** Fills every cell with one block. */
    public SchematicBuilder fill(String state) {
        BlockState s = parse(state);
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = s;
        }
        return this;
    }

    /** Replaces the interior (everything not on an outer face) with one block. */
    public SchematicBuilder hollow(String interior) {
        BlockState s = parse(interior);
        for (int y = 1; y < size.y() - 1; y++) {
            for (int z = 1; z < size.z() - 1; z++) {
                for (int x = 1; x < size.x() - 1; x++) {
                    blocks[index(x, y, z)] = s;
                }
            }
        }
        return this;
    }

    public Schematic build() {
        return new Schematic(size, blocks.clone());
    }

    private static BlockState parse(String state) {
        int bracket = state.indexOf('[');
        if (bracket < 0) {
            return new BlockState(state, Map.of());
        }
        String name = state.substring(0, bracket);
        String inner = state.substring(bracket + 1, state.length() - 1);
        java.util.Map<String, String> props = new java.util.LinkedHashMap<>();
        for (String kv : inner.split(",")) {
            if (kv.isBlank()) {
                continue;
            }
            String[] parts = kv.split("=", 2);
            props.put(parts[0].trim(), parts[1].trim());
        }
        return new BlockState(name, props);
    }
}
