package dev.proceed;

import dev.proceed.generation.ProceedGenerator;

/**
 * Entry point for <b>Proceed</b> &mdash; a small, dependency-free library for procedurally
 * generating Minecraft structures from {@code .litematic} files with relative connection points.
 *
 * <p>The whole flow is three steps:
 * <ol>
 *     <li>Load {@code .litematic} files into
 *         {@link dev.proceed.piece.StructurePiece StructurePiece}s and declare their
 *         connection points (explicitly, or from marker blocks via
 *         {@link dev.proceed.piece.Markers Markers}).</li>
 *     <li>Collect them into a {@link dev.proceed.piece.PiecePool PiecePool}.</li>
 *     <li>Run {@link #generator()} to get a
 *         {@link dev.proceed.generation.GeneratedStructure GeneratedStructure}, then place its
 *         blocks in the world from your mod.</li>
 * </ol>
 *
 * <pre>{@code
 * StructurePiece entrance = StructurePiece.fromFile("entrance.litematic")
 *         .connectionsFromMarkers(Markers.byBlock("minecraft:purple_glazed_terracotta", "hall"))
 *         .build();
 * StructurePiece hall = StructurePiece.fromFile("hall.litematic")
 *         .connectionsFromMarkers(Markers.byBlock("minecraft:purple_glazed_terracotta", "hall"))
 *         .build();
 *
 * PiecePool pool = PiecePool.builder().start(entrance).add(hall).build();
 *
 * GeneratedStructure structure = Proceed.generator()
 *         .pool(pool)
 *         .seed(level.getSeed())
 *         .maxPieces(24)
 *         .generate();
 *
 * for (BlockPlacement p : structure.blocks(origin)) {
 *     // place p.state() at p.pos() using your mod loader's API
 * }
 * }</pre>
 *
 * <p>Proceed never touches Minecraft's classes itself, so it drops into any loader (Forge,
 * Fabric, NeoForge, Quilt) &mdash; you own the final "set this block" step.
 */
public final class Proceed {

    /** The library version. */
    public static final String VERSION = "0.1.0";

    private Proceed() {
    }

    /** @return a new, unconfigured {@link ProceedGenerator}. */
    public static ProceedGenerator generator() {
        return ProceedGenerator.create();
    }
}
