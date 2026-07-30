package dev.proceed.litematic;

import dev.proceed.math.Rotation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockStateRotationTest {

    @Test
    void rotatesFacingProperty() {
        BlockState stairs = new BlockState("minecraft:oak_stairs", Map.of("facing", "north", "half", "bottom"));
        BlockState rotated = stairs.rotate(Rotation.CLOCKWISE_90);
        assertEquals("east", rotated.property("facing"));
        assertEquals("bottom", rotated.property("half")); // untouched
    }

    @Test
    void swapsPillarAxisOn90() {
        BlockState log = new BlockState("minecraft:oak_log", Map.of("axis", "x"));
        assertEquals("z", log.rotate(Rotation.CLOCKWISE_90).property("axis"));
        assertEquals("x", log.rotate(Rotation.CLOCKWISE_180).property("axis")); // 180 keeps axis
    }

    @Test
    void advancesSignRotation() {
        BlockState sign = new BlockState("minecraft:oak_sign", Map.of("rotation", "2"));
        assertEquals("6", sign.rotate(Rotation.CLOCKWISE_90).property("rotation"));
        assertEquals("14", sign.rotate(Rotation.CLOCKWISE_270).property("rotation"));
        // wraps around the 0-15 range
        assertEquals("2", new BlockState("minecraft:oak_sign", Map.of("rotation", "14"))
                .rotate(Rotation.CLOCKWISE_90).property("rotation"));
    }

    @Test
    void canonicalToString() {
        BlockState s = new BlockState("minecraft:oak_stairs", Map.of("half", "top", "facing", "west"));
        assertEquals("minecraft:oak_stairs[facing=west,half=top]", s.toString());
    }
}
