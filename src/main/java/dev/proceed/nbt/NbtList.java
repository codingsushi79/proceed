package dev.proceed.nbt;

import java.util.ArrayList;
import java.util.List;

/**
 * An NBT list tag: an ordered sequence of values that all share one tag type.
 *
 * <p>Elements are stored as plain Java objects using the same mapping as {@link NbtCompound}
 * (e.g. {@link NbtCompound} for compound entries, {@link Integer} for ints).
 */
public final class NbtList {

    private final int elementTypeId;
    private final List<Object> items;

    NbtList(int elementTypeId, List<Object> items) {
        this.elementTypeId = elementTypeId;
        this.items = items;
    }

    /** @return the NBT type id shared by every element (see {@link NbtType}). */
    public int elementTypeId() {
        return elementTypeId;
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public Object get(int index) {
        return items.get(index);
    }

    /** @return the element at {@code index} as a compound, or throws if it is not one. */
    public NbtCompound getCompound(int index) {
        Object v = items.get(index);
        if (v instanceof NbtCompound c) {
            return c;
        }
        throw new IllegalStateException("list element " + index + " is not a compound");
    }

    /** @return an unmodifiable view of every compound in this list. */
    public List<NbtCompound> compounds() {
        List<NbtCompound> out = new ArrayList<>(items.size());
        for (Object v : items) {
            if (v instanceof NbtCompound c) {
                out.add(c);
            }
        }
        return out;
    }

    public List<Object> raw() {
        return items;
    }
}
