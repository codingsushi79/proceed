# Proceed

**Procedurally generate Minecraft structures from `.litematic` files, connected at relative entry/exit points.**

Proceed is a tiny, **dependency-free** Java library for mods. You design structure pieces in
[Litematica](https://github.com/maruohon/litematica) (a corridor, a room, a stairwell, a tower
segment…), mark where they connect, and Proceed stitches them together procedurally — picking
pieces, rotating them, aligning their openings, and rejecting collisions — until you have a
complete, unique structure to place in the world.

It has **no dependency on Minecraft, Forge, Fabric or NeoForge**. It reads `.litematic` files
itself and hands you a list of *"put this block state at this position"* placements. The final
"set the block" step stays in your mod, so Proceed works on any loader and any Minecraft version.

```
 .litematic files ──▶ StructurePiece ──▶ PiecePool ──▶ ProceedGenerator ──▶ GeneratedStructure
   (your builds)      (+ connections)     (the set)      (grows it)          (blocks to place)
```

---

## Quick start

```java
import dev.proceed.Proceed;
import dev.proceed.generation.*;
import dev.proceed.math.BlockPos;
import dev.proceed.piece.*;

// 1. Load pieces and describe how they connect.
//    Here connection points are auto-detected from a marker block: every purple glazed
//    terracotta becomes a "hall" connection, facing whichever way the block points.
MarkerScanner halls = Markers.byBlock("minecraft:purple_glazed_terracotta", "hall");

StructurePiece entrance = StructurePiece.fromFile("structures/entrance.litematic")
        .connectionsFromMarkers(halls)
        .build();

StructurePiece corridor = StructurePiece.fromFile("structures/corridor.litematic")
        .weight(3.0)                       // corridors show up 3x as often
        .connectionsFromMarkers(halls)
        .build();

StructurePiece treasureRoom = StructurePiece.fromFile("structures/treasure.litematic")
        .maxUses(1)                        // at most one treasure room per structure
        .connectionsFromMarkers(halls)
        .build();

StructurePiece deadEnd = StructurePiece.fromFile("structures/wall.litematic")
        .connectionsFromMarkers(halls)
        .build();

// 2. Gather them into a pool.
PiecePool pool = PiecePool.builder()
        .start(entrance)                   // generation begins here
        .add(corridor, treasureRoom)
        .cap("hall", deadEnd)              // seal any leftover openings with a wall
        .build();

// 3. Generate.
GeneratedStructure structure = Proceed.generator()
        .pool(pool)
        .seed(level.getSeed())             // reproducible per world/chunk
        .maxPieces(30)
        .maxDepth(8)
        .generate();

// 4. Place it in the world from your mod.
BlockPos origin = new BlockPos(x, y, z);
for (BlockPlacement p : structure.blocks(origin)) {
    setBlockInWorld(p.pos(), p.state());   // your loader-specific code — see below
}
```

That's the whole library. Everything else is optional configuration.

---

## Defining connection points

A **connection point** is a relative doorway on a piece. It has three things:

| | |
|---|---|
| **position** | where it is, in the piece's own local coordinates |
| **facing** | the outward direction a neighbour attaches from |
| **channel** | a label — two points only connect if their channels match |

Two points mate when their **channels are equal** and their **facings are opposite**: a corridor
exit facing `EAST` accepts a room entrance facing `WEST`.

### Option A — marker blocks (recommended)

Place a recognisable, *oriented* block wherever a piece should connect — a stair, observer, piston,
or a glazed terracotta — pointing **outward** through the opening. Proceed reads both the location
and the direction straight from your build:

```java
StructurePiece piece = StructurePiece.fromFile("corridor.litematic")
        .connectionsFromMarkers(Markers.byBlock("minecraft:observer", "corridor"))
        .build();
```

The marker block's `facing` property becomes the connection's direction. Marker cells are replaced
with air on output by default, so they form the actual opening (and never leave stray marker blocks
behind). If your marker block has no `facing` property, state the direction explicitly:

```java
Markers.byBlock("minecraft:gold_block", "corridor", Direction.NORTH)
```

> **Tip:** put the marker on the **outermost layer** of the piece, in the cell that forms the
> opening. Proceed places neighbours so these opening cells sit flush against each other.

### Option B — declare them in code

Full control, no marker blocks needed:

```java
StructurePiece room = StructurePiece.fromFile("room.litematic")
        .name("room")
        .connection(new BlockPos(0, 1, 3), Direction.WEST, "corridor")
        .connection(new BlockPos(6, 1, 3), Direction.EAST, "corridor")
        .connection(new BlockPos(3, 1, 0), Direction.NORTH, "door")
        .build();
```

You can mix both styles on the same piece.

---

## Controlling generation

Every knob on `Proceed.generator()` is optional:

| Method | Default | What it does |
|---|---|---|
| `.pool(pool)` | *required* | the pieces to build from |
| `.seed(long)` | random | fix the seed for reproducible structures |
| `.maxPieces(int)` | `32` | hard cap on total pieces |
| `.maxDepth(int)` | unlimited | max attachment distance from the start piece |
| `.bounds(BoundingBox)` | unlimited | keep the whole structure inside a volume |
| `.spacing(int)` | `0` | air blocks left between mated pieces |
| `.allowCollisions(boolean)` | `false` | let pieces overlap |
| `.sealOpenConnections(boolean)` | `true` | close leftover openings with cap pieces |
| `.includeAir(boolean)` | `false` | emit air cells too (to clear space) |
| `.replaceMarkersWith(BlockState)` | air | what to put where marker blocks were |
| `.startRotation(Rotation)` | `NONE` | rotate the whole structure |

**Weights & limits** live on the piece: `.weight(double)` biases how often a piece is chosen, and
`.maxUses(int)` caps how many times it can appear.

---

## Vanilla-style placement, conditions & processors

Proceed mirrors Minecraft's own structure-generation layer so it can slot into real worldgen — but
it stays **engine-agnostic**: you feed in a `PlacementContext` describing the world (seed, chunk,
biome, dimension, a terrain height sampler), and Proceed decides whether and where to generate.

Use `tryGenerate(context)` instead of `generate()`. It returns `Optional.empty()` when the
structure shouldn't spawn there, and otherwise resolves the world origin for you:

```java
Optional<GeneratedStructure> maybe = Proceed.generator()
        .pool(pool)
        // WHERE it may spawn — biome/dimension/rarity conditions:
        .condition(GenerationCondition.all(
                GenerationCondition.inDimension("minecraft:overworld"),
                GenerationCondition.inBiomes("minecraft:desert", "minecraft:badlands"),
                GenerationCondition.chance(0.3)))
        // Spacing grid — exactly like RandomSpreadStructurePlacement (spacing, separation, salt):
        .placement(RandomSpreadPlacement.of(32, 8, 165745296))
        // Start height — constant / uniform / trapezoid / weighted / terrain-projected:
        .startHeight(HeightProvider.fromTerrain(-1))
        // Jigsaw size clamp, like max_distance_from_center:
        .maxDistanceFromCenter(80)
        // Post-processing, like StructureProcessor:
        .processors(
                Processors.integrity(0.9),                                  // 10% crumbles away
                Processors.replace("minecraft:cobblestone",
                        "minecraft:mossy_cobblestone", 0.4))
        .maxPieces(40)
        .tryGenerate(context);

maybe.ifPresent(structure -> {
    for (BlockPlacement p : structure.blocks()) {   // already positioned at the resolved origin
        level.setBlock(toMc(p.pos()), toMc(p.state()), 2);
    }
});
```

You build the context from whatever your generation callback has on hand:

```java
PlacementContext context = PlacementContext.builder(level.getSeed())
        .chunk(chunkPos.x, chunkPos.z)
        .biome(biomeId)
        .dimension(dimensionId)
        .heightSampler((x, z) -> chunkGenerator.getFirstFreeHeight(x, z, WORLD_SURFACE_WG, ...))
        .build();
```

### How it maps to vanilla

| Minecraft concept | Proceed |
|---|---|
| `StructurePlacement` / `RandomSpreadStructurePlacement` | `StructurePlacement`, `RandomSpreadPlacement` (bit-for-bit same spacing maths) |
| `biome` / `dimension` filters, rarity | `GenerationCondition.inBiomes` / `inDimension` / `chance` / `terrainHeightBetween` |
| `HeightProvider` (`constant`/`uniform`/`trapezoid`/`weighted`) | `HeightProvider.constant` / `uniform` / `trapezoid` / `weighted` |
| `project_start_to_heightmap` | `HeightProvider.fromTerrain(offset)` + a `HeightSampler` |
| Jigsaw `max_depth` | `.maxDepth(int)` |
| Jigsaw `max_distance_from_center` | `.maxDistanceFromCenter(int)` |
| Template pools with weights, fallback/cap pools | `PiecePool` weights + `.cap(channel, piece)` |
| `StructureProcessor` (rule, integrity/rot, block-swap) | `StructureProcessor` + `Processors.rule…/integrity/replace/remove` |
| Rigid placement vs terrain-matching | `bounds` / `maxDistanceFromCenter` clamp + heightmap projection |

Conditions, placement, height and processors are all optional and independent — add only the ones
you need. Everything remains seeded and reproducible: the same context and configuration always
produce the same structure.

---

## Using the result

`GeneratedStructure` gives you:

- **`blocks(origin)`** — every `BlockPlacement` (a position + a `BlockState`), translated to your
  world origin. This is what you loop over to build the structure.
- **`placedPieces()`** — each `PlacedPiece` with its piece, rotation and offset, if you want to
  drive placement yourself (spawn entities, run block-entity setup, decorate rooms…).
- **`openConnections()`** — any openings left unsealed, handy for attaching the structure to the
  surrounding world (tunnels, cave mouths, etc.).
- **`bounds()`** — the overall bounding box.

### Placing blocks in your mod

Proceed emits block states as strings like `minecraft:oak_stairs[facing=east,half=bottom]`.
Convert them to your loader's block state once and cache it:

```java
// Fabric / NeoForge example (pseudocode)
net.minecraft.world.level.block.state.BlockState toMc(dev.proceed.litematic.BlockState s) {
    // Parse s.name() + s.properties() with BlockStateParser, or keep your own lookup table.
    return BlockStateParser.parseForBlock(registry, s.toString(), false).blockState();
}

for (BlockPlacement p : structure.blocks(origin)) {
    BlockPos mcPos = new BlockPos(p.pos().x(), p.pos().y(), p.pos().z());
    level.setBlock(mcPos, toMc(p.state()), Block.UPDATE_CLIENTS);
}
```

Rotations are already applied to both positions **and** directional block states (stairs, logs,
signs…), so a rotated staircase still faces the right way.

---

## How it works

1. A **start piece** is placed at the origin.
2. Its connection points form a **frontier** of open openings.
3. For each open opening, Proceed finds a compatible piece (matching channel), computes the single
   rotation + offset that makes the two openings meet face-to-face, and checks it against a
   **bounding-box collision test** and the optional volume bounds — the same fast approach vanilla
   jigsaw structures use.
4. Accepted pieces add their remaining openings to the frontier; generation continues until the
   frontier empties or a limit is hit.
5. Leftover openings are optionally **capped** so the structure never gapes into the world.

Everything is driven by a seeded `java.util.Random`, so the same pool + seed always produce the
same structure.

---

## Building

```bash
./gradlew build       # compile, test, and produce the jar
./gradlew test        # run the test suite
```

Requires JDK 17+. The library targets Java 17 bytecode for broad mod compatibility and pulls in
**zero runtime dependencies**.

Add it to your mod (once published):

```groovy
dependencies {
    implementation 'dev.proceed:proceed:0.1.0'
}
```

---

## License

MIT — see [LICENSE](LICENSE).
