package dev.proceed.litematic;

import dev.proceed.math.BlockPos;
import dev.proceed.testutil.LitematicFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LitematicLoadTest {

    @Test
    void loadsSizePaletteAndProperties(@TempDir Path dir) {
        Path file = new LitematicFixture(3, 3, 3)
                .fill("minecraft:stone")
                .set(2, 1, 1, "minecraft:oak_stairs[facing=east,half=bottom]")
                .set(0, 0, 0, null) // carve an air pocket
                .writeTo(dir.resolve("test.litematic"));

        Schematic s = Litematic.load(file).toSchematic();

        assertEquals(new BlockPos(3, 3, 3), s.size());
        assertEquals("minecraft:stone", s.getBlockState(1, 1, 1).name());
        assertTrue(s.isAir(0, 0, 0));

        BlockState stairs = s.getBlockState(2, 1, 1);
        assertEquals("minecraft:oak_stairs", stairs.name());
        assertEquals("east", stairs.property("facing"));
        assertEquals("bottom", stairs.property("half"));
    }

    @Test
    void bitPackingRoundTripsForManyPaletteEntries(@TempDir Path dir) {
        // 40 distinct blocks forces a 6-bit palette where entries span long boundaries.
        LitematicFixture fixture = new LitematicFixture(40, 1, 1);
        for (int x = 0; x < 40; x++) {
            fixture.set(x, 0, 0, "minecraft:stone_" + x);
        }
        Path file = fixture.writeTo(dir.resolve("packed.litematic"));

        Schematic s = Litematic.load(file).toSchematic();
        for (int x = 0; x < 40; x++) {
            assertEquals("minecraft:stone_" + x, s.getBlockState(x, 0, 0).name(),
                    "mismatch at x=" + x);
        }
    }

    @Test
    void airPaletteEntriesReadAsAir(@TempDir Path dir) {
        Path file = new LitematicFixture(2, 1, 1)
                .set(0, 0, 0, "minecraft:diamond_block")
                .set(1, 0, 0, null)
                .writeTo(dir.resolve("air.litematic"));

        Schematic s = Litematic.load(file).toSchematic();
        assertFalse(s.isAir(0, 0, 0));
        assertTrue(s.isAir(1, 0, 0));
    }
}
