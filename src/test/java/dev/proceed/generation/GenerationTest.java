package dev.proceed.generation;

import dev.proceed.Proceed;
import dev.proceed.litematic.Litematic;
import dev.proceed.math.BlockPos;
import dev.proceed.math.BoundingBox;
import dev.proceed.piece.Markers;
import dev.proceed.piece.PiecePool;
import dev.proceed.piece.StructurePiece;
import dev.proceed.testutil.LitematicFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerationTest {

    private static final String MARKER = "minecraft:purple_glazed_terracotta";

    /** A 3x3x3 corridor with "hall" openings on its north and south faces. */
    private StructurePiece corridor(Path dir) {
        Path file = new LitematicFixture(3, 3, 3)
                .fill("minecraft:stone")
                .set(1, 1, 0, MARKER + "[facing=north]")
                .set(1, 1, 2, MARKER + "[facing=south]")
                .writeTo(dir.resolve("corridor.litematic"));
        return StructurePiece.fromLitematic(Litematic.load(file))
                .name("corridor")
                .connectionsFromMarkers(Markers.byBlock(MARKER, "hall"))
                .build();
    }

    /** A 3x3x3 dead-end with a single "hall" opening on its north face. */
    private StructurePiece cap(Path dir) {
        Path file = new LitematicFixture(3, 3, 3)
                .fill("minecraft:cobblestone")
                .set(1, 1, 0, MARKER + "[facing=north]")
                .writeTo(dir.resolve("cap.litematic"));
        return StructurePiece.fromLitematic(Litematic.load(file))
                .name("cap")
                .connectionsFromMarkers(Markers.byBlock(MARKER, "hall"))
                .build();
    }

    @Test
    void growsAChainUpToTheLimit(@TempDir Path dir) {
        StructurePiece corridor = corridor(dir);
        PiecePool pool = PiecePool.builder().start(corridor).add(corridor).build();

        GeneratedStructure s = Proceed.generator()
                .pool(pool)
                .seed(1)
                .maxPieces(4)
                .sealOpenConnections(false)
                .generate();

        assertEquals(4, s.pieceCount());
        assertNoOverlaps(s);
    }

    @Test
    void piecesNeverOverlap(@TempDir Path dir) {
        StructurePiece corridor = corridor(dir);
        PiecePool pool = PiecePool.builder().start(corridor).add(corridor).build();

        // Try many seeds; the collision check must hold for all of them.
        for (long seed = 0; seed < 25; seed++) {
            GeneratedStructure s = Proceed.generator()
                    .pool(pool).seed(seed).maxPieces(12).generate();
            assertNoOverlaps(s);
        }
    }

    @Test
    void sameSeedIsReproducible(@TempDir Path dir) {
        StructurePiece corridor = corridor(dir);
        PiecePool pool = PiecePool.builder().start(corridor).add(corridor).build();

        GeneratedStructure a = Proceed.generator().pool(pool).seed(42).maxPieces(8).generate();
        GeneratedStructure b = Proceed.generator().pool(pool).seed(42).maxPieces(8).generate();

        assertEquals(a.pieceCount(), b.pieceCount());
        for (int i = 0; i < a.pieceCount(); i++) {
            assertEquals(a.placedPieces().get(i).offset(), b.placedPieces().get(i).offset());
            assertEquals(a.placedPieces().get(i).rotation(), b.placedPieces().get(i).rotation());
        }
    }

    @Test
    void capsSealOpenConnections(@TempDir Path dir) {
        StructurePiece corridor = corridor(dir);
        StructurePiece cap = cap(dir);
        PiecePool pool = PiecePool.builder()
                .start(corridor)
                .add(corridor)
                .cap("hall", cap)
                .build();

        // A short chain leaves open ends, which the caps should close.
        GeneratedStructure s = Proceed.generator()
                .pool(pool).seed(7).maxPieces(3).generate();

        assertTrue(s.openConnections().isEmpty(),
                "expected caps to seal all open connections, left: " + s.openConnections().size());
        assertNoOverlaps(s);
    }

    @Test
    void boundsRestrictGrowth(@TempDir Path dir) {
        StructurePiece corridor = corridor(dir);
        PiecePool pool = PiecePool.builder().start(corridor).add(corridor).build();

        // Only room for the start piece plus one on each side along Z.
        BoundingBox box = BoundingBox.of(0, 0, -3, 2, 2, 5);
        GeneratedStructure s = Proceed.generator()
                .pool(pool).seed(3).maxPieces(50).bounds(box)
                .sealOpenConnections(false)
                .generate();

        assertTrue(box.contains(s.bounds()), "structure escaped its bounds: " + s.bounds());
        assertTrue(s.pieceCount() <= 3);
    }

    @Test
    void markerCellsBecomeAirInOutput(@TempDir Path dir) {
        StructurePiece corridor = corridor(dir);
        PiecePool pool = PiecePool.builder().start(corridor).build();

        GeneratedStructure s = Proceed.generator()
                .pool(pool).seed(0).maxPieces(1).sealOpenConnections(false).generate();

        // The marker blocks must not survive as purple terracotta anywhere.
        boolean anyMarker = s.blocks().stream().anyMatch(b -> b.state().name().equals(MARKER));
        assertFalse(anyMarker, "marker block leaked into output");
    }

    private static void assertNoOverlaps(GeneratedStructure s) {
        // No two placed pieces share a cell.
        Set<BlockPos> occupied = new HashSet<>();
        for (BlockPlacement p : s.blocks()) {
            assertTrue(occupied.add(p.pos()),
                    "two pieces wrote to the same cell " + p.pos());
        }
    }
}
