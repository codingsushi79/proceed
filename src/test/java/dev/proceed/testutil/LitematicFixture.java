package dev.proceed.testutil;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes tiny single-region {@code .litematic} files (plain, uncompressed NBT &mdash; the loader
 * auto-detects) so tests can exercise the real load path end-to-end.
 *
 * <p>The bit packing mirrors {@code LitematicaBitArray} exactly, including entries that span a
 * long boundary, so a successful round-trip proves the reader is correct.
 */
public final class LitematicFixture {

    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final String[] blocks; // block-state strings, null = air; index (y*sz+z)*sx+x

    public LitematicFixture(int sizeX, int sizeY, int sizeZ) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.blocks = new String[sizeX * sizeY * sizeZ];
    }

    private int index(int x, int y, int z) {
        return (y * sizeZ + z) * sizeX + x;
    }

    /** Sets a block; {@code state} may be a plain id or {@code id[prop=val,...]}. */
    public LitematicFixture set(int x, int y, int z, String state) {
        blocks[index(x, y, z)] = state;
        return this;
    }

    /** Fills the whole box with one block. */
    public LitematicFixture fill(String state) {
        for (int i = 0; i < blocks.length; i++) {
            blocks[i] = state;
        }
        return this;
    }

    /** Writes the fixture to {@code file} and returns it. */
    public Path writeTo(Path file) {
        try {
            Files.write(file, toBytes());
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private byte[] toBytes() throws IOException {
        // Build the palette: air is index 0.
        List<String> palette = new ArrayList<>();
        palette.add("minecraft:air");
        Map<String, Integer> paletteIndex = new LinkedHashMap<>();
        paletteIndex.put("minecraft:air", 0);
        for (String s : blocks) {
            String state = s == null ? "minecraft:air" : s;
            paletteIndex.computeIfAbsent(state, k -> {
                palette.add(k);
                return palette.size() - 1;
            });
        }

        int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(Math.max(1, palette.size() - 1)));
        long volume = (long) sizeX * sizeY * sizeZ;
        int longCount = (int) (((volume * bits) + 63) / 64);
        long[] packed = new long[longCount];
        for (int i = 0; i < blocks.length; i++) {
            String state = blocks[i] == null ? "minecraft:air" : blocks[i];
            setPacked(packed, bits, i, paletteIndex.get(state));
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);
        // Root compound.
        out.writeByte(10);
        out.writeUTF("");

        writeInt(out, "MinecraftDataVersion", 3465);
        writeInt(out, "Version", 6);

        // Metadata.
        out.writeByte(10);
        out.writeUTF("Metadata");
        writeString(out, "Name", "fixture");
        writeString(out, "Author", "test");
        writeString(out, "Description", "");
        writeInt(out, "RegionCount", 1);
        out.writeByte(0); // end Metadata

        // Regions -> "main".
        out.writeByte(10);
        out.writeUTF("Regions");
        out.writeByte(10);
        out.writeUTF("main");

        writeVec3i(out, "Position", 0, 0, 0);
        writeVec3i(out, "Size", sizeX, sizeY, sizeZ);

        // BlockStatePalette (list of compounds).
        out.writeByte(9);
        out.writeUTF("BlockStatePalette");
        out.writeByte(10); // element type compound
        out.writeInt(palette.size());
        for (String state : palette) {
            writePaletteEntry(out, state);
        }

        // BlockStates long array.
        out.writeByte(12);
        out.writeUTF("BlockStates");
        out.writeInt(packed.length);
        for (long l : packed) {
            out.writeLong(l);
        }

        out.writeByte(0); // end "main"
        out.writeByte(0); // end Regions
        out.writeByte(0); // end root
        out.flush();
        return bos.toByteArray();
    }

    private static void setPacked(long[] data, int bits, int index, int value) {
        long bitOffset = (long) index * bits;
        int startArr = (int) (bitOffset >> 6);
        int startBit = (int) (bitOffset & 63);
        int endArr = (int) (((long) (index + 1) * bits - 1) >> 6);
        long v = value & ((1L << bits) - 1);
        data[startArr] |= (v << startBit);
        if (startArr != endArr) {
            data[endArr] |= (v >>> (64 - startBit));
        }
    }

    private static void writePaletteEntry(DataOutputStream out, String state) throws IOException {
        String name = state;
        Map<String, String> props = new LinkedHashMap<>();
        int bracket = state.indexOf('[');
        if (bracket >= 0) {
            name = state.substring(0, bracket);
            String inner = state.substring(bracket + 1, state.length() - 1);
            for (String kv : inner.split(",")) {
                String[] parts = kv.split("=", 2);
                props.put(parts[0], parts[1]);
            }
        }
        writeString(out, "Name", name);
        if (!props.isEmpty()) {
            out.writeByte(10);
            out.writeUTF("Properties");
            for (Map.Entry<String, String> e : props.entrySet()) {
                writeString(out, e.getKey(), e.getValue());
            }
            out.writeByte(0);
        }
        out.writeByte(0); // end this palette compound
    }

    private static void writeInt(DataOutputStream out, String name, int value) throws IOException {
        out.writeByte(3);
        out.writeUTF(name);
        out.writeInt(value);
    }

    private static void writeString(DataOutputStream out, String name, String value) throws IOException {
        out.writeByte(8);
        out.writeUTF(name);
        out.writeUTF(value);
    }

    private static void writeVec3i(DataOutputStream out, String name, int x, int y, int z) throws IOException {
        out.writeByte(10);
        out.writeUTF(name);
        writeInt(out, "x", x);
        writeInt(out, "y", y);
        writeInt(out, "z", z);
        out.writeByte(0);
    }
}
