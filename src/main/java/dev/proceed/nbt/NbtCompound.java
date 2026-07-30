package dev.proceed.nbt;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * An NBT compound tag: an ordered map of named values.
 *
 * <p>Values are stored as ordinary Java objects, keeping the reader tiny and dependency-free:
 * <ul>
 *     <li>{@code byte/short/int/long} &rarr; {@link Byte}, {@link Short}, {@link Integer}, {@link Long}</li>
 *     <li>{@code float/double} &rarr; {@link Float}, {@link Double}</li>
 *     <li>{@code string} &rarr; {@link String}</li>
 *     <li>{@code byte[]/int[]/long[]} &rarr; the matching Java array</li>
 *     <li>{@code list} &rarr; {@link NbtList}</li>
 *     <li>{@code compound} &rarr; {@link NbtCompound}</li>
 * </ul>
 * The typed getters throw a clear {@link IllegalStateException} on a type mismatch so malformed
 * schematics fail loudly rather than silently.
 */
public final class NbtCompound {

    private final Map<String, Object> values;

    NbtCompound(Map<String, Object> values) {
        this.values = values;
    }

    public NbtCompound() {
        this(new LinkedHashMap<>());
    }

    public boolean contains(String key) {
        return values.containsKey(key);
    }

    public Set<String> keys() {
        return values.keySet();
    }

    public Object get(String key) {
        return values.get(key);
    }

    void put(String key, Object value) {
        values.put(key, value);
    }

    private <T> T require(String key, Class<T> type) {
        Object v = values.get(key);
        if (v == null) {
            throw new IllegalStateException("missing NBT key '" + key + "'");
        }
        if (!type.isInstance(v)) {
            throw new IllegalStateException("NBT key '" + key + "' is a "
                    + v.getClass().getSimpleName() + ", expected " + type.getSimpleName());
        }
        return type.cast(v);
    }

    public byte getByte(String key) {
        return require(key, Number.class).byteValue();
    }

    public int getInt(String key) {
        return require(key, Number.class).intValue();
    }

    public int getInt(String key, int fallback) {
        Object v = values.get(key);
        return v instanceof Number n ? n.intValue() : fallback;
    }

    public long getLong(String key) {
        return require(key, Number.class).longValue();
    }

    public String getString(String key) {
        return require(key, String.class);
    }

    public String getString(String key, String fallback) {
        Object v = values.get(key);
        return v instanceof String s ? s : fallback;
    }

    public long[] getLongArray(String key) {
        return require(key, long[].class);
    }

    public int[] getIntArray(String key) {
        return require(key, int[].class);
    }

    public NbtCompound getCompound(String key) {
        return require(key, NbtCompound.class);
    }

    /** @return the named list, or an empty list if the key is absent. */
    public NbtList getList(String key) {
        Object v = values.get(key);
        if (v == null) {
            return new NbtList(NbtType.END, java.util.List.of());
        }
        if (v instanceof NbtList l) {
            return l;
        }
        throw new IllegalStateException("NBT key '" + key + "' is not a list");
    }
}
