package dev.proceed.example;

import dev.proceed.Proceed;
import dev.proceed.generation.GeneratedStructure;
import dev.proceed.generation.PlacedPiece;
import dev.proceed.litematic.Schematic;
import dev.proceed.litematic.SchematicBuilder;
import dev.proceed.math.BlockPos;
import dev.proceed.math.BoundingBox;
import dev.proceed.math.Direction;
import dev.proceed.piece.PiecePool;
import dev.proceed.piece.StructurePiece;

/**
 * A runnable, file-free demo: builds a few pieces in code, generates a layout, and prints a
 * top-down ASCII map. Run it with:
 *
 * <pre>{@code ./gradlew -q runDemo}</pre>
 *
 * or execute {@code main} directly from your IDE. Pass a seed as the first argument.
 */
public final class MazeDemo {

    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : System.nanoTime();

        // Two piece types, built entirely in code (no .litematic needed):
        // a straight corridor and a 4-way junction. Both connect on the "hall" channel.
        StructurePiece corridor = corridor();
        StructurePiece junction = junction();

        PiecePool pool = PiecePool.builder()
                .start(junction)
                .add(corridor, junction)
                .build();

        GeneratedStructure structure = Proceed.generator()
                .pool(pool)
                .seed(seed)
                .maxPieces(24)
                .bounds(BoundingBox.of(-30, 0, -30, 30, 4, 30))
                .sealOpenConnections(false)
                .generate();

        System.out.println(structure);
        System.out.println("seed = " + seed + "\n");
        printMap(structure);
    }

    /** A 5x3x5 corridor running north-south, with hall openings on the N and S faces. */
    private static StructurePiece corridor() {
        Schematic s = SchematicBuilder.of(5, 3, 5)
                .fill("minecraft:stone_bricks")
                .hollow("minecraft:air")
                .build();
        return StructurePiece.fromSchematic(s).name("corridor")
                .connection(new BlockPos(2, 1, 0), Direction.NORTH, "hall")
                .connection(new BlockPos(2, 1, 4), Direction.SOUTH, "hall")
                .build();
    }

    /** A 5x3x5 junction with hall openings on all four sides. */
    private static StructurePiece junction() {
        Schematic s = SchematicBuilder.of(5, 3, 5)
                .fill("minecraft:mossy_stone_bricks")
                .hollow("minecraft:air")
                .build();
        return StructurePiece.fromSchematic(s).name("junction")
                .weight(0.5) // junctions are rarer than corridors
                .connection(new BlockPos(2, 1, 0), Direction.NORTH, "hall")
                .connection(new BlockPos(2, 1, 4), Direction.SOUTH, "hall")
                .connection(new BlockPos(0, 1, 2), Direction.WEST, "hall")
                .connection(new BlockPos(4, 1, 2), Direction.EAST, "hall")
                .build();
    }

    private static void printMap(GeneratedStructure structure) {
        BoundingBox b = structure.bounds();
        if (b == null) {
            System.out.println("(empty)");
            return;
        }
        char[][] grid = new char[b.sizeZ()][b.sizeX()];
        for (char[] row : grid) {
            java.util.Arrays.fill(row, ' ');
        }
        for (PlacedPiece p : structure.placedPieces()) {
            BoundingBox box = p.bounds();
            char c = p.piece().name().startsWith("junction") ? '#' : '.';
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    grid[z - b.minZ()][x - b.minX()] = c;
                }
            }
        }
        System.out.println("top-down map ('#' junction, '.' corridor):");
        for (char[] row : grid) {
            System.out.println(new String(row));
        }
    }

    private MazeDemo() {
    }
}
