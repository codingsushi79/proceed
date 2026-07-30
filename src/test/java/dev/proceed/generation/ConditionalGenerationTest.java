package dev.proceed.generation;

import dev.proceed.Proceed;
import dev.proceed.litematic.Schematic;
import dev.proceed.litematic.SchematicBuilder;
import dev.proceed.math.BlockPos;
import dev.proceed.math.Direction;
import dev.proceed.piece.PiecePool;
import dev.proceed.piece.StructurePiece;
import dev.proceed.placement.GenerationCondition;
import dev.proceed.placement.HeightProvider;
import dev.proceed.placement.PlacementContext;
import dev.proceed.placement.RandomSpreadPlacement;
import dev.proceed.processor.Processors;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionalGenerationTest {

    private StructurePiece corridor() {
        Schematic s = SchematicBuilder.of(3, 3, 3).fill("minecraft:stone").hollow("minecraft:air").build();
        return StructurePiece.fromSchematic(s).name("corridor")
                .connection(new BlockPos(1, 1, 0), Direction.NORTH, "hall")
                .connection(new BlockPos(1, 1, 2), Direction.SOUTH, "hall")
                .build();
    }

    private PiecePool pool() {
        StructurePiece c = corridor();
        return PiecePool.builder().start(c).add(c).build();
    }

    @Test
    void conditionBlocksGenerationInWrongBiome() {
        ProceedGenerator gen = Proceed.generator()
                .pool(pool())
                .condition(GenerationCondition.inBiomes("minecraft:desert"))
                .maxPieces(3);

        Optional<GeneratedStructure> desert = gen.tryGenerate(
                PlacementContext.builder(1).biome("minecraft:desert").build());
        Optional<GeneratedStructure> plains = gen.tryGenerate(
                PlacementContext.builder(1).biome("minecraft:plains").build());

        assertTrue(desert.isPresent());
        assertTrue(plains.isEmpty());
    }

    @Test
    void placementResolvesWorldOriginFromChunkAndHeight() {
        ProceedGenerator gen = Proceed.generator()
                .pool(pool())
                .placement(RandomSpreadPlacement.of(4, 1, 999))
                .startHeight(HeightProvider.constant(70))
                .maxPieces(3)
                .sealOpenConnections(false);

        long seed = 24680L;
        // Find the one start chunk in a cell and confirm the origin lands there.
        BlockPos found = null;
        for (int cx = 0; cx < 4 && found == null; cx++) {
            for (int cz = 0; cz < 4; cz++) {
                Optional<GeneratedStructure> s = gen.tryGenerate(
                        PlacementContext.builder(seed).chunk(cx, cz).build());
                if (s.isPresent()) {
                    found = s.get().origin();
                    break;
                }
            }
        }
        assertTrue(found != null, "expected a start chunk somewhere in the cell");
        assertEquals(70, found.y(), "start height should come from the height provider");
    }

    @Test
    void integrityProcessorRemovesBlocks() {
        GeneratedStructure full = Proceed.generator()
                .pool(pool()).seed(5).maxPieces(4).sealOpenConnections(false)
                .generate();
        GeneratedStructure eroded = Proceed.generator()
                .pool(pool()).seed(5).maxPieces(4).sealOpenConnections(false)
                .processor(Processors.integrity(0.5))
                .generate();

        assertTrue(eroded.blocks().size() < full.blocks().size(),
                "integrity < 1.0 should drop some blocks");
    }

    @Test
    void replaceProcessorSwapsBlocks() {
        GeneratedStructure s = Proceed.generator()
                .pool(pool()).seed(5).maxPieces(2).sealOpenConnections(false)
                .processor(Processors.replace("minecraft:stone", "minecraft:mossy_cobblestone"))
                .generate();

        boolean anyStone = s.blocks().stream().anyMatch(b -> b.state().name().equals("minecraft:stone"));
        boolean anyMossy = s.blocks().stream().anyMatch(b -> b.state().name().equals("minecraft:mossy_cobblestone"));
        assertFalse(anyStone, "all stone should have been replaced");
        assertTrue(anyMossy, "replacement block should be present");
    }

    @Test
    void maxDistanceFromCenterClampsGrowth() {
        GeneratedStructure s = Proceed.generator()
                .pool(pool()).seed(2).maxPieces(50)
                .maxDistanceFromCenter(5) // only room for the start piece plus neighbours within 5 blocks
                .sealOpenConnections(false)
                .generate();

        var b = s.bounds();
        int center = 1; // start piece 3-wide centred at 1
        assertTrue(b.minX() >= center - 5 && b.maxX() <= center + 5);
        assertTrue(b.minZ() >= center - 5 && b.maxZ() <= center + 5);
    }

    @Test
    void tryGenerateIsReproducible() {
        ProceedGenerator gen = Proceed.generator()
                .pool(pool())
                .startHeight(HeightProvider.uniform(60, 80))
                .maxPieces(6);
        PlacementContext ctx = PlacementContext.builder(77L).chunk(2, 2).build();

        GeneratedStructure a = gen.tryGenerate(ctx).orElseThrow();
        GeneratedStructure b = gen.tryGenerate(ctx).orElseThrow();
        assertEquals(a.origin(), b.origin());
        assertEquals(a.pieceCount(), b.pieceCount());
    }
}
