package dev.proceed.piece;

import dev.proceed.litematic.Litematic;
import dev.proceed.litematic.Schematic;
import dev.proceed.math.BlockPos;
import dev.proceed.math.Direction;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A reusable building block for procedural generation: one schematic plus the
 * {@link ConnectionPoint}s that say how it links to its neighbours.
 *
 * <p>Build pieces with the fluent {@link #builder()} (or the {@code from*} shortcuts). Connection
 * points can be declared explicitly or, more conveniently, discovered from marker blocks placed in
 * the schematic (see {@link Markers}).
 *
 * <pre>{@code
 * // Explicit connections:
 * StructurePiece room = StructurePiece.fromFile("room.litematic")
 *         .name("room")
 *         .weight(2.0)
 *         .connection(new BlockPos(0, 1, 3), Direction.WEST, "corridor")
 *         .connection(new BlockPos(6, 1, 3), Direction.EAST, "corridor")
 *         .build();
 *
 * // Auto-detected from marker blocks:
 * StructurePiece corridor = StructurePiece.fromFile("corridor.litematic")
 *         .name("corridor")
 *         .connectionsFromMarkers(Markers.byBlock("minecraft:purple_glazed_terracotta", "corridor"))
 *         .build();
 * }</pre>
 *
 * <p>Instances are immutable and safe to share across generations.
 */
public final class StructurePiece {

    private final String name;
    private final Schematic schematic;
    private final List<ConnectionPoint> connections;
    private final Set<BlockPos> markerCells;
    private final double weight;
    private final int maxUses;

    private StructurePiece(Builder b) {
        this.name = b.name;
        this.schematic = b.schematic;
        this.connections = List.copyOf(b.connections);
        this.markerCells = Set.copyOf(b.markerCells);
        this.weight = b.weight;
        this.maxUses = b.maxUses;
    }

    public String name() { return name; }
    public Schematic schematic() { return schematic; }
    public BlockPos size() { return schematic.size(); }

    /** @return the piece's connection points, in declaration order. */
    public List<ConnectionPoint> connections() { return connections; }

    /** @return the local cells occupied by marker blocks, to be replaced on output. */
    public Set<BlockPos> markerCells() { return markerCells; }

    /** @return the relative likelihood of picking this piece (default {@code 1.0}). */
    public double weight() { return weight; }

    /** @return the maximum times this piece may appear in one structure, or {@code -1} for no limit. */
    public int maxUses() { return maxUses; }

    /** @return the connection points whose channel equals {@code channel}. */
    public List<ConnectionPoint> connectionsOn(String channel) {
        List<ConnectionPoint> out = new ArrayList<>();
        for (ConnectionPoint c : connections) {
            if (c.channel().equals(channel)) {
                out.add(c);
            }
        }
        return out;
    }

    @Override
    public String toString() {
        return "StructurePiece[" + name + ", size=" + size()
                + ", connections=" + connections.size() + "]";
    }

    // ---- construction ---------------------------------------------------------------------

    public static Builder builder() {
        return new Builder();
    }

    /** Loads a {@code .litematic} from disk and starts a builder for it. */
    public static Builder fromFile(String path) {
        return fromFile(Path.of(path));
    }

    /** Loads a {@code .litematic} from disk and starts a builder for it. */
    public static Builder fromFile(Path path) {
        return builder().schematic(Litematic.load(path).toSchematic()).name(fileStem(path));
    }

    /** Starts a builder from an already-loaded litematic. */
    public static Builder fromLitematic(Litematic litematic) {
        Builder b = builder().schematic(litematic.toSchematic());
        return litematic.name().isEmpty() ? b : b.name(litematic.name());
    }

    /** Starts a builder from a raw schematic (any source). */
    public static Builder fromSchematic(Schematic schematic) {
        return builder().schematic(schematic);
    }

    private static String fileStem(Path path) {
        String fn = path.getFileName().toString();
        int dot = fn.lastIndexOf('.');
        return dot > 0 ? fn.substring(0, dot) : fn;
    }

    /** Fluent builder for {@link StructurePiece}. */
    public static final class Builder {
        private String name = "piece";
        private Schematic schematic;
        private final List<ConnectionPoint> connections = new ArrayList<>();
        private final Set<BlockPos> markerCells = new HashSet<>();
        private double weight = 1.0;
        private int maxUses = -1;

        private Builder() {
        }

        public Builder schematic(Schematic schematic) {
            this.schematic = schematic;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /** Sets the relative selection weight (must be positive). */
        public Builder weight(double weight) {
            if (weight <= 0) {
                throw new IllegalArgumentException("weight must be positive");
            }
            this.weight = weight;
            return this;
        }

        /** Caps how many times this piece may be used in a single structure. */
        public Builder maxUses(int maxUses) {
            this.maxUses = maxUses;
            return this;
        }

        /** Adds an explicitly positioned connection point. */
        public Builder connection(String id, BlockPos position, Direction facing, String channel) {
            connections.add(new ConnectionPoint(id, position, facing, channel));
            return this;
        }

        /** Adds an explicitly positioned connection point (id derived automatically). */
        public Builder connection(BlockPos position, Direction facing, String channel) {
            connections.add(ConnectionPoint.of(position, facing, channel));
            return this;
        }

        /** Adds a pre-built connection point. */
        public Builder connection(ConnectionPoint point) {
            connections.add(point);
            return this;
        }

        /**
         * Scans the schematic with {@code scanner}, adding a connection point for every marker
         * block found and recording those cells so they can be replaced with air on output.
         *
         * @throws IllegalStateException if no schematic has been set yet
         */
        public Builder connectionsFromMarkers(MarkerScanner scanner) {
            if (schematic == null) {
                throw new IllegalStateException("set a schematic before scanning for markers");
            }
            for (int y = 0; y < schematic.sizeY(); y++) {
                for (int z = 0; z < schematic.sizeZ(); z++) {
                    for (int x = 0; x < schematic.sizeX(); x++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        ConnectionPoint point = scanner.scan(schematic, pos, schematic.getBlockState(x, y, z));
                        if (point != null) {
                            connections.add(point);
                            markerCells.add(point.position());
                        }
                    }
                }
            }
            return this;
        }

        /** Marks a cell to be replaced with air on output without adding a connection point. */
        public Builder stripCell(BlockPos cell) {
            markerCells.add(cell);
            return this;
        }

        public StructurePiece build() {
            if (schematic == null) {
                throw new IllegalStateException("a StructurePiece needs a schematic");
            }
            for (ConnectionPoint c : connections) {
                BlockPos p = c.position();
                if (!schematic.inBounds(p.x(), p.y(), p.z())) {
                    throw new IllegalStateException("connection '" + c.id() + "' at " + p
                            + " lies outside piece '" + name + "' of size " + schematic.size());
                }
            }
            if (connections.isEmpty()) {
                // Not fatal (a piece can be a terminal cap with a single connection, or a
                // standalone start), but a piece with zero connections can never be attached.
                Collections.emptyList();
            }
            return new StructurePiece(this);
        }
    }
}
