package dev.proceed.litematic;

import dev.proceed.math.Direction;
import dev.proceed.math.Rotation;
import dev.proceed.nbt.NbtCompound;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * A block together with its block-state properties, e.g. {@code minecraft:oak_stairs[facing=east,half=bottom]}.
 *
 * <p>Instances are immutable. {@link #rotate(Rotation)} returns a new state with common
 * directional properties ({@code facing}, {@code axis}, {@code rotation}) turned to match, so a
 * rotated staircase still faces sensibly. Properties the library does not recognise are copied
 * through unchanged.
 */
public final class BlockState {

    private final String name;
    private final Map<String, String> properties;

    public BlockState(String name, Map<String, String> properties) {
        this.name = name;
        // Sorted for a canonical, stable toString().
        this.properties = properties.isEmpty()
                ? Map.of()
                : new TreeMap<>(properties);
    }

    /** @return the block id, e.g. {@code "minecraft:stone"}. */
    public String name() {
        return name;
    }

    /** @return an unmodifiable, sorted view of the block-state properties. */
    public Map<String, String> properties() {
        return properties;
    }

    public String property(String key) {
        return properties.get(key);
    }

    /** @return {@code true} if this is air (or the litematica structure-void placeholder). */
    public boolean isAir() {
        return name.equals("minecraft:air")
                || name.equals("minecraft:cave_air")
                || name.equals("minecraft:void_air");
    }

    /** Reads a palette entry ({@code Name} plus optional {@code Properties} compound). */
    public static BlockState fromPalette(NbtCompound tag) {
        String name = tag.getString("Name");
        Map<String, String> props = new LinkedHashMap<>();
        if (tag.contains("Properties")) {
            NbtCompound p = tag.getCompound("Properties");
            for (String key : p.keys()) {
                props.put(key, String.valueOf(p.get(key)));
            }
        }
        return new BlockState(name, props);
    }

    /**
     * Returns this state rotated by {@code rotation}, adjusting the directional properties
     * Minecraft uses most often:
     * <ul>
     *     <li>{@code facing} &mdash; the compass facing of stairs, doors, furnaces, &hellip;</li>
     *     <li>{@code axis} &mdash; pillar/log orientation ({@code x}/{@code z} swap on 90&deg;)</li>
     *     <li>{@code rotation} &mdash; the 0&ndash;15 sign/banner rotation</li>
     * </ul>
     */
    public BlockState rotate(Rotation rotation) {
        if (rotation == Rotation.NONE || properties.isEmpty()) {
            return this;
        }
        Map<String, String> rotated = new LinkedHashMap<>(properties);

        String facing = properties.get("facing");
        if (facing != null) {
            try {
                rotated.put("facing", rotation.rotate(Direction.byName(facing)).blockStateName());
            } catch (IllegalArgumentException ignored) {
                // Non-directional "facing" value (unusual); leave it alone.
            }
        }

        String axis = properties.get("axis");
        if (axis != null && (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.CLOCKWISE_270)) {
            if (axis.equals("x")) {
                rotated.put("axis", "z");
            } else if (axis.equals("z")) {
                rotated.put("axis", "x");
            }
        }

        String signRotation = properties.get("rotation");
        if (signRotation != null) {
            try {
                int steps = switch (rotation) {
                    case CLOCKWISE_90 -> 4;
                    case CLOCKWISE_180 -> 8;
                    case CLOCKWISE_270 -> 12;
                    default -> 0;
                };
                int value = (Integer.parseInt(signRotation) + steps) & 15;
                rotated.put("rotation", Integer.toString(value));
            } catch (NumberFormatException ignored) {
                // Not a numeric rotation; leave it alone.
            }
        }

        return new BlockState(name, rotated);
    }

    /** @return the canonical string form, e.g. {@code minecraft:oak_stairs[facing=east,half=bottom]}. */
    @Override
    public String toString() {
        if (properties.isEmpty()) {
            return name;
        }
        StringBuilder sb = new StringBuilder(name).append('[');
        boolean first = true;
        for (Map.Entry<String, String> e : properties.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
            first = false;
        }
        return sb.append(']').toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof BlockState other
                && name.equals(other.name)
                && properties.equals(other.properties);
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 + properties.hashCode();
    }
}
