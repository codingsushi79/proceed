package dev.proceed.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RotationTest {

    @Test
    void clockwise90RotatesDirectionsAroundCompass() {
        assertEquals(Direction.EAST, Rotation.CLOCKWISE_90.rotate(Direction.NORTH));
        assertEquals(Direction.SOUTH, Rotation.CLOCKWISE_90.rotate(Direction.EAST));
        assertEquals(Direction.WEST, Rotation.CLOCKWISE_90.rotate(Direction.SOUTH));
        assertEquals(Direction.NORTH, Rotation.CLOCKWISE_90.rotate(Direction.WEST));
    }

    @Test
    void verticalDirectionsAreUnaffectedByYawRotation() {
        for (Rotation r : Rotation.ALL) {
            assertEquals(Direction.UP, r.rotate(Direction.UP));
            assertEquals(Direction.DOWN, r.rotate(Direction.DOWN));
        }
    }

    @Test
    void positionRotationStaysWithinRotatedBox() {
        BlockPos size = new BlockPos(4, 1, 2); // x=4, z=2
        BlockPos rotatedSize = Rotation.CLOCKWISE_90.rotate(size);
        assertEquals(new BlockPos(2, 1, 4), rotatedSize);

        // A corner block should map to a corner of the rotated box.
        BlockPos corner = new BlockPos(3, 0, 0);
        BlockPos rotated = Rotation.CLOCKWISE_90.rotate(corner, size);
        assertEquals(new BlockPos(1, 0, 3), rotated); // (sz-1-z, y, x) = (2-1-0, 0, 3)
    }

    @Test
    void rotatingByFullTurnReturnsToStart() {
        BlockPos size = new BlockPos(5, 3, 2);
        BlockPos pos = new BlockPos(1, 2, 0);
        BlockPos once = Rotation.CLOCKWISE_90.rotate(pos, size);
        BlockPos twice = Rotation.CLOCKWISE_90.rotate(once, Rotation.CLOCKWISE_90.rotate(size));
        BlockPos thrice = Rotation.CLOCKWISE_90.rotate(twice, Rotation.CLOCKWISE_180.rotate(size));
        BlockPos back = Rotation.CLOCKWISE_90.rotate(thrice, Rotation.CLOCKWISE_270.rotate(size));
        assertEquals(pos, back);
    }

    @Test
    void inverseUndoesRotation() {
        assertEquals(Rotation.CLOCKWISE_270, Rotation.CLOCKWISE_90.inverse());
        assertEquals(Direction.NORTH,
                Rotation.CLOCKWISE_90.inverse().rotate(Rotation.CLOCKWISE_90.rotate(Direction.NORTH)));
    }
}
