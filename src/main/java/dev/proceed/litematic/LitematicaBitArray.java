package dev.proceed.litematic;

/**
 * Reader for Litematica's packed block-state index array.
 *
 * <p>Litematica stores each block as an index into the region palette, packed into a
 * {@code long[]} using a fixed bit width. Unlike vanilla's 1.16+ format, entries are packed
 * contiguously and <em>may span a long boundary</em>, so the extraction has to stitch bits from
 * two longs together.
 *
 * <p>The bit width is {@code max(2, ceil(log2(paletteSize)))}.
 */
final class LitematicaBitArray {

    private final long[] data;
    private final int bitsPerEntry;
    private final long maxEntryValue;
    private final long size;

    LitematicaBitArray(int bitsPerEntry, long size, long[] data) {
        this.bitsPerEntry = bitsPerEntry;
        this.size = size;
        this.maxEntryValue = (1L << bitsPerEntry) - 1L;
        this.data = data;
    }

    /** @return the minimum bits needed to index a palette of {@code paletteSize} entries. */
    static int bitsForPalette(int paletteSize) {
        int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(Math.max(1, paletteSize - 1)));
        return bits;
    }

    /** @return the palette index stored at the given flat block index. */
    int get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index + " out of range " + size);
        }
        long startOffset = (long) index * bitsPerEntry;
        int startArrIndex = (int) (startOffset >> 6);          // / 64
        int endArrIndex = (int) (((long) (index + 1) * bitsPerEntry - 1L) >> 6);
        int startBitOffset = (int) (startOffset & 0x3F);        // % 64

        if (startArrIndex == endArrIndex) {
            return (int) ((data[startArrIndex] >>> startBitOffset) & maxEntryValue);
        }
        int endOffset = 64 - startBitOffset;
        long low = data[startArrIndex] >>> startBitOffset;
        long high = data[endArrIndex] << endOffset;
        return (int) ((low | high) & maxEntryValue);
    }
}
