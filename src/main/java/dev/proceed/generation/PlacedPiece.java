package dev.proceed.generation;

import dev.proceed.litematic.BlockState;
import dev.proceed.litematic.Schematic;
import dev.proceed.math.BlockPos;
import dev.proceed.math.BoundingBox;
import dev.proceed.math.Direction;
import dev.proceed.math.Rotation;
import dev.proceed.piece.ConnectionPoint;
import dev.proceed.piece.StructurePiece;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link StructurePiece} fixed in the generated structure's coordinate space: it records the
 * piece, the {@link Rotation} applied to it, and the {@link BlockPos} offset of its (rotated)
 * origin.
 *
 * <p>All the local-to-world maths lives here, so the generator and the output code share one
 * consistent transform.
 */
public final class PlacedPiece {

    private final StructurePiece piece;
    private final Rotation rotation;
    private final BlockPos offset;
    private final BlockPos rotatedSize;

    public PlacedPiece(StructurePiece piece, Rotation rotation, BlockPos offset) {
        this.piece = piece;
        this.rotation = rotation;
        this.offset = offset;
        this.rotatedSize = rotation.rotate(piece.size());
    }

    public StructurePiece piece() { return piece; }
    public Rotation rotation() { return rotation; }
    public BlockPos offset() { return offset; }

    /** @return the world position of a local cell in this placement. */
    public BlockPos toWorld(BlockPos local) {
        return rotation.rotate(local, piece.size()).add(offset);
    }

    /** @return this placement's bounding box in structure coordinates. */
    public BoundingBox bounds() {
        return BoundingBox.fromSize(offset, rotatedSize);
    }

    /** @return this piece's connection points, transformed into structure coordinates. */
    public List<WorldConnection> worldConnections() {
        List<WorldConnection> out = new ArrayList<>(piece.connections().size());
        for (ConnectionPoint c : piece.connections()) {
            out.add(worldConnection(c));
        }
        return out;
    }

    /** @return one connection point transformed into structure coordinates. */
    public WorldConnection worldConnection(ConnectionPoint c) {
        BlockPos worldPos = toWorld(c.position());
        Direction worldFacing = rotation.rotate(c.facing());
        return new WorldConnection(worldPos, worldFacing, c.channel(), this, c);
    }

    /**
     * Emits the block placements for this piece into {@code out}.
     *
     * @param out            the collector
     * @param includeAir     if {@code true}, air cells are emitted too (useful for clearing space)
     * @param markerFill     the block to place at marker cells, or {@code null} to leave them empty
     */
    void emit(List<BlockPlacement> out, boolean includeAir, BlockState markerFill) {
        Schematic s = piece.schematic();
        var markers = piece.markerCells();
        for (int y = 0; y < s.sizeY(); y++) {
            for (int z = 0; z < s.sizeZ(); z++) {
                for (int x = 0; x < s.sizeX(); x++) {
                    BlockPos local = new BlockPos(x, y, z);
                    BlockPos world = toWorld(local);
                    if (markers.contains(local)) {
                        if (markerFill != null) {
                            out.add(new BlockPlacement(world, markerFill));
                        }
                        continue;
                    }
                    BlockState state = s.getBlockState(x, y, z);
                    if (state.isAir() && !includeAir) {
                        continue;
                    }
                    out.add(new BlockPlacement(world, state.rotate(rotation)));
                }
            }
        }
    }

    @Override
    public String toString() {
        return "PlacedPiece[" + piece.name() + ", rot=" + rotation + ", at=" + offset + "]";
    }
}
