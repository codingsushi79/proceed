package dev.proceed.nbt;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Reads uncompressed or gzip-compressed NBT data into an {@link NbtCompound} tree.
 *
 * <p>{@code .litematic} files are gzip-compressed NBT, which this reader detects automatically
 * from the gzip magic bytes, so callers can pass a raw file stream without worrying about it.
 */
public final class NbtIo {

    private NbtIo() {
    }

    /** Reads the root compound from a file, transparently decompressing gzip. */
    public static NbtCompound read(Path file) throws IOException {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            return read(in);
        }
    }

    /** Reads the root compound from a stream, transparently decompressing gzip. */
    public static NbtCompound read(InputStream rawIn) throws IOException {
        InputStream in = rawIn.markSupported() ? rawIn : new BufferedInputStream(rawIn);
        in.mark(2);
        int b0 = in.read();
        int b1 = in.read();
        in.reset();
        boolean gzip = (b0 == 0x1f) && (b1 == 0x8b);

        DataInputStream data = new DataInputStream(gzip ? new GZIPInputStream(in) : in);
        int rootType = data.readByte();
        if (rootType != NbtType.COMPOUND) {
            throw new IOException("NBT root tag is not a compound (type " + rootType + ")");
        }
        data.readUTF(); // root name, conventionally empty
        return readCompound(data);
    }

    private static NbtCompound readCompound(DataInputStream in) throws IOException {
        Map<String, Object> values = new LinkedHashMap<>();
        while (true) {
            int type = in.readByte() & 0xFF;
            if (type == NbtType.END) {
                break;
            }
            String name = in.readUTF();
            values.put(name, readPayload(in, type));
        }
        return new NbtCompound(values);
    }

    private static Object readPayload(DataInputStream in, int type) throws IOException {
        switch (type) {
            case NbtType.BYTE:
                return in.readByte();
            case NbtType.SHORT:
                return in.readShort();
            case NbtType.INT:
                return in.readInt();
            case NbtType.LONG:
                return in.readLong();
            case NbtType.FLOAT:
                return in.readFloat();
            case NbtType.DOUBLE:
                return in.readDouble();
            case NbtType.BYTE_ARRAY: {
                int len = in.readInt();
                byte[] arr = new byte[len];
                in.readFully(arr);
                return arr;
            }
            case NbtType.STRING:
                return in.readUTF();
            case NbtType.LIST: {
                int elementType = in.readByte() & 0xFF;
                int len = in.readInt();
                List<Object> items = new ArrayList<>(Math.max(0, len));
                for (int i = 0; i < len; i++) {
                    items.add(readPayload(in, elementType));
                }
                return new NbtList(elementType, items);
            }
            case NbtType.COMPOUND:
                return readCompound(in);
            case NbtType.INT_ARRAY: {
                int len = in.readInt();
                int[] arr = new int[len];
                for (int i = 0; i < len; i++) {
                    arr[i] = in.readInt();
                }
                return arr;
            }
            case NbtType.LONG_ARRAY: {
                int len = in.readInt();
                long[] arr = new long[len];
                for (int i = 0; i < len; i++) {
                    arr[i] = in.readLong();
                }
                return arr;
            }
            default:
                throw new IOException("unknown NBT tag type: " + type);
        }
    }
}
