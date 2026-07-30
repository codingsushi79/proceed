package dev.proceed.piece;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The set of pieces the generator may use, plus the rules for how it starts and finishes.
 *
 * <ul>
 *     <li><b>start</b> pieces &mdash; one is chosen (weighted) as the root the structure grows from</li>
 *     <li><b>pieces</b> &mdash; the pool of pieces that get attached as generation proceeds</li>
 *     <li><b>caps</b> &mdash; optional per-channel pieces used to seal any connection left open at
 *         the end (a wall, a dead-end), so the structure never has holes gaping into the world</li>
 * </ul>
 *
 * <pre>{@code
 * PiecePool pool = PiecePool.builder()
 *         .start(entrance)
 *         .add(corridor, tRoom, stairs, treasureRoom)
 *         .cap("corridor", wallCap)   // close leftover corridor connections with a wall
 *         .build();
 * }</pre>
 */
public final class PiecePool {

    private final List<StructurePiece> startPieces;
    private final List<StructurePiece> pieces;
    private final Map<String, StructurePiece> caps;
    private final Map<String, List<StructurePiece>> byChannel;

    private PiecePool(Builder b) {
        this.startPieces = List.copyOf(b.startPieces);
        this.pieces = List.copyOf(b.pieces);
        this.caps = Map.copyOf(b.caps);

        Map<String, List<StructurePiece>> index = new HashMap<>();
        for (StructurePiece piece : pieces) {
            for (ConnectionPoint c : piece.connections()) {
                index.computeIfAbsent(c.channel(), k -> new ArrayList<>()).add(piece);
            }
        }
        // De-duplicate: a piece with several connections on one channel should appear once.
        Map<String, List<StructurePiece>> deduped = new HashMap<>();
        index.forEach((channel, list) -> {
            List<StructurePiece> unique = new ArrayList<>();
            for (StructurePiece p : list) {
                if (!unique.contains(p)) {
                    unique.add(p);
                }
            }
            deduped.put(channel, List.copyOf(unique));
        });
        this.byChannel = Map.copyOf(deduped);
    }

    public List<StructurePiece> startPieces() { return startPieces; }
    public List<StructurePiece> pieces() { return pieces; }

    /** @return the pieces that have at least one connection on {@code channel}. */
    public List<StructurePiece> candidatesFor(String channel) {
        return byChannel.getOrDefault(channel, List.of());
    }

    /** @return the cap piece registered for {@code channel}, or {@code null} if none. */
    public StructurePiece capFor(String channel) {
        return caps.get(channel);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for {@link PiecePool}. */
    public static final class Builder {
        private final List<StructurePiece> startPieces = new ArrayList<>();
        private final List<StructurePiece> pieces = new ArrayList<>();
        private final Map<String, StructurePiece> caps = new HashMap<>();

        private Builder() {
        }

        /** Adds a candidate start piece (generation begins from one of these). */
        public Builder start(StructurePiece piece) {
            startPieces.add(piece);
            return this;
        }

        /** Adds an attachable piece to the pool. */
        public Builder add(StructurePiece piece) {
            pieces.add(piece);
            return this;
        }

        /** Adds several attachable pieces at once. */
        public Builder add(StructurePiece... pieces) {
            for (StructurePiece p : pieces) {
                this.pieces.add(p);
            }
            return this;
        }

        public Builder add(Iterable<StructurePiece> pieces) {
            for (StructurePiece p : pieces) {
                this.pieces.add(p);
            }
            return this;
        }

        /**
         * Registers a cap piece for {@code channel}. When generation finishes, every still-open
         * connection on that channel is sealed with this piece (if it fits). The cap should have a
         * single connection on the matching channel.
         */
        public Builder cap(String channel, StructurePiece piece) {
            caps.put(channel, piece);
            return this;
        }

        public PiecePool build() {
            if (startPieces.isEmpty()) {
                throw new IllegalStateException("a PiecePool needs at least one start piece");
            }
            return new PiecePool(this);
        }
    }
}
