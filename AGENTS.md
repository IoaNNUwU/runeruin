# RuneRuin — agent map

NeoForge mod (`runeruin`), MC 26.2. Custom stacked-cave dimension. Entry: `RuneRuinMod` → registers DeferredRegisters; datapack registries come from `DatagenMain`.

Helpers: `RR.id` / `RR.resourceKey` / `RR.tagKey`. Dimension command: `/execute in runeruin:runeruin_dimension …`

## Packages

| Path | Role |
|------|------|
| `blocks/`, `items/`, `creativetab/` | Content registries |
| `datagen/` | Models, tags, recipes, loot + **worldgen bootstrap** |
| `dimension/` | Almost all worldgen |
| `dimension/biomes/<layer>/` | Biome definitions (one class per biome) |
| `dimension/features/` | Custom `Feature<?>` implementations |
| `dimension/chunkgenerator/` | Terrain fill (floors/ceilings/plates) |
| `dimension/structures/` | Structure + piece classes |
| `dimension/noise/` | Noise used by chunkgen + biome picking |
| `client/` | Clouds / renderers |
| `mixin/` | Vanilla tweaks |

Generated JSON lands under `src/generated/` (and mirrors in `bin/`); **edit Java bootstrap, then run datagen** — don’t hand-edit generated worldgen unless intentional.

## Vertical layers (`Const`)

Bottom → top (Y ≈): Void `0…50` → Lost caves → Deep caves → Blooming caves → Top layer (build limit 512).

Key Y constants: `LOST_CAVES_Y`, `LOST_CAVES_CEILING_Y`, `DEEP_CAVES_Y`, `DEEP_CAVES_CEILING_Y`, `BLOOMING_CAVES_Y`, `BLOOMING_CAVES_CEILING_Y`, `TOP_LAYER_Y`. Arcane stone plates sit between layers (`ArcaneStructureGen`).

## How generation splits

Two independent systems:

1. **Terrain shape** — `RRChunkGenerator.fillFromNoise` → `chunkgenerator/*Gen` (noise floors/ceilings) + `ArcaneStructureGen` (plates/pillars/runes). Biomes do **not** carve the stacked caves.
2. **Decoration** — vanilla feature pipeline after terrain: biome → placed features.

Biome pick by Y (+ noise within layer): `RRBiomeSource.getNoiseBiome`. Layer biome lists wired in `RRBiomeSource.newDefault` (also used by `RRDimension.bootstrapStem`).

```
RRDimension (stem + type)
    └─ RRChunkGenerator + RRBiomeSource
         ├─ terrain: TopLayerAndBloomingCavesGen / DeepCaves* / LostCaves* / VoidGen / ArcaneStructureGen
         └─ biomes: RRBiomes → biomes/<layer>/*.java → RRPlacedFeatures
```

## Feature pipeline (add decoration here)

```
Feature class          → register type     → configure (blocks/params) → place (count/height/scan) → attach to biome
dimension/features/X   RRFeatures          RRConfiguredFeatures          RRPlacedFeatures           biomes/... + RRBiomes
```

Order matters in datagen (`DatagenMain.DATAPACK_REGISTRY_BUILDER`): configured → placed → structures → biomes → dimension.

| Step | File | What to do |
|------|------|------------|
| 1 | `features/FooFeature.java` | Implement `Feature` + config |
| 2 | `RRFeatures` | `REGISTRY.register(...)` (runtime) |
| 3 | `RRConfiguredFeatures` | Key + `bootstrap` entry |
| 4 | `RRPlacedFeatures` | Key + placement; heights often use `Const.*` |
| 5 | `biomes/<layer>/Bar.java` | `generation.addFeature(..., RRPlacedFeatures.FOO)` |
| 6 | `RRBiomes` | Key + `ctx.register` if new biome |

Custom placement mods: `placements/` + `RRPlacementModifierTypes`.

Vanilla features OK in configured/placed (e.g. `Feature.BLOCK_COLUMN`, `CavePlacements.*`).

## Biomes (add biome here)

1. Class under `dimension/biomes/<layer>/` with `bootstrap(placedFeatures, carvers)` → builds `Biome` (mobs, effects, `addFeature`).
2. Key + register in `RRBiomes`.
3. Put holder into the right list in `RRBiomeSource.newDefault` or it never spawns.
4. Tags (structures etc.): `RRBiomeTags` + `DatagenBiomeTagProvider`.

Layer folders match vertical bands: `toplayer`, `bloomingcaves` / `bloomingcavesceiling`, `deepcaves` / `deepcavesceiling`, `lostcaves` / `lostcavesceiling`.

## Structures (separate from Features)

For large/jigsaw-style pieces (e.g. Giant Goblet):

- Type: `RRStructureTypes` + `structures/*Structure.java` / `*Piece.java` + `RRStructurePieceTypes`
- Datapack: `RRStructures` + `RRStructureSets`
- Biome filter via tag (`RRBiomeTags.HAS_GIANT_GOBLET`)

Note: there is also `GiantGobletFeature` / placed feature — feature vs structure are different paths; prefer structure for sparse large builds.

## Content outside worldgen

- Blocks/items: `RRBlocks`, `RRItems` (+ lang under `resources/assets/runeruin/lang/`)
- Models/loot/recipes/tags: `datagen/*`
- Teleport item: `items/RuneOfSpaceItem`

## Quick “where?”

| Want… | Open… |
|-------|--------|
| Layer Y heights | `Const` |
| Terrain look of a layer | matching `chunkgenerator/*Gen` |
| Which biome at Y | `RRBiomeSource` |
| Biome colors/mobs/features list | `biomes/<layer>/…` |
| New worldgen blob | `features/` → Features → Configured → Placed → biome |
| Dimension registration | `RRDimension` + `DatagenMain` |
| Pillars / arcane plates / runes | `ArcaneStructureGen`, `runes/Runes` |
