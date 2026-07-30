package dev.proceed.processor;

import dev.proceed.generation.BlockPlacement;
import dev.proceed.litematic.BlockState;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Ready-made {@link StructureProcessor}s covering the common vanilla ones.
 *
 * <pre>{@code
 * StructureProcessor weathered = Processors.chain(
 *         Processors.integrity(0.85),                       // 15% of blocks crumble away
 *         Processors.replace("minecraft:cobblestone",       // some cobble turns mossy
 *                 "minecraft:mossy_cobblestone", 0.3));
 * }</pre>
 */
public final class Processors {

    private Processors() {
    }

    /** Runs several processors in order; a {@code null} from any one drops the block. */
    public static StructureProcessor chain(StructureProcessor... processors) {
        StructureProcessor[] copy = processors.clone();
        return (placement, ctx) -> {
            BlockPlacement current = placement;
            for (StructureProcessor p : copy) {
                if (current == null) {
                    return null;
                }
                current = p.process(current, ctx);
            }
            return current;
        };
    }

    /**
     * Keeps each block with probability {@code integrity}, dropping the rest &mdash; the classic
     * "ruined structure" effect (vanilla's {@code BlockRotProcessor}). {@code 1.0} keeps
     * everything, {@code 0.0} removes everything.
     */
    public static StructureProcessor integrity(double integrity) {
        return (placement, ctx) -> ctx.random().nextDouble() < integrity ? placement : null;
    }

    /** Unconditionally replaces one block id with another block state. */
    public static StructureProcessor replace(String fromBlockId, String toState) {
        BlockState to = parse(toState);
        return (placement, ctx) -> placement.state().name().equals(fromBlockId)
                ? new BlockPlacement(placement.pos(), to)
                : placement;
    }

    /** Replaces one block id with another, but only for a fraction {@code probability} of them. */
    public static StructureProcessor replace(String fromBlockId, String toState, double probability) {
        BlockState to = parse(toState);
        return (placement, ctx) -> {
            if (placement.state().name().equals(fromBlockId) && ctx.random().nextDouble() < probability) {
                return new BlockPlacement(placement.pos(), to);
            }
            return placement;
        };
    }

    /** Applies a table of {@code fromId -> toState} replacements in one pass. */
    public static StructureProcessor replaceAll(Map<String, String> replacements) {
        Map<String, BlockState> table = new java.util.HashMap<>();
        replacements.forEach((from, to) -> table.put(from, parse(to)));
        return (placement, ctx) -> {
            BlockState to = table.get(placement.state().name());
            return to == null ? placement : new BlockPlacement(placement.pos(), to);
        };
    }

    /** Removes every block matching {@code filter}. */
    public static StructureProcessor remove(Predicate<BlockState> filter) {
        return (placement, ctx) -> filter.test(placement.state()) ? null : placement;
    }

    /** Removes every block whose id is one of {@code blockIds}. */
    public static StructureProcessor removeBlocks(String... blockIds) {
        var ids = java.util.Set.copyOf(List.of(blockIds));
        return (placement, ctx) -> ids.contains(placement.state().name()) ? null : placement;
    }

    private static BlockState parse(String state) {
        int bracket = state.indexOf('[');
        if (bracket < 0) {
            return new BlockState(state, Map.of());
        }
        String name = state.substring(0, bracket);
        String inner = state.substring(bracket + 1, state.length() - 1);
        Map<String, String> props = new java.util.LinkedHashMap<>();
        for (String kv : inner.split(",")) {
            if (kv.isBlank()) {
                continue;
            }
            String[] parts = kv.split("=", 2);
            props.put(parts[0].trim(), parts[1].trim());
        }
        return new BlockState(name, props);
    }
}
