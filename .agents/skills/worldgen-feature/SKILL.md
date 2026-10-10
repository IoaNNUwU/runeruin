---
name: worldgen-feature
description: >-
  Add or change a RuneRuin worldgen feature (decoration placed by biomes): Feature class, registration,
  configured and placed features, biome attachment, placement rules and preview. Use for new plants,
  rocks, vines, mushrooms, spikes or any Feature<?>; for large multi-chunk builds use worldgen-structure.
---

# Worldgen feature

Decoration runs after terrain, through the vanilla feature pipeline of each biome; terrain shape is `RRChunkGenerator` (AGENTS.md, "Worldgen").

| Step | File | What |
|------|------|------|
| 1 | `dimension/features/FooFeature.java` | `Feature` + config |
| 2 | `dimension/RRFeatures` | `REGISTRY.register(...)` (runtime type) |
| 3 | `dimension/RRConfiguredFeatures` | key + `bootstrap` entry: blocks and parameters |
| 4 | `dimension/RRPlacedFeatures` | key + placement, usually from the factories there: `floorPlacement` (scan down to ground), `ceilingPlacement` (scan up to a ceiling), `bandPlacement` (height band only); heights from `Const` and the band constants at the top of the class |
| 5 | `dimension/biomes/<layer>/Bar.java` | `generation.addFeature(<step>, RRPlacedFeatures.FOO)` |
| 6 | `RRGameTests.featureCases` | a row with the surface for the GameTests `feature_placement` and `feature_reach`; a preview job of its own in `preview/jobs/` only when the registry config is not enough (`headless-preview` skill) |

Then `compileJava`, `runData` (the configured, placed and biome JSON is generated) and the preview.

Rules:

- Vanilla features and placements are fine (`Feature.BLOCK_COLUMN`, `CavePlacements.*`). Custom placement modifiers live in `dimension/placements/` and are registered in `RRPlacementModifierTypes`.
- End every placement of a feature attached to a biome with `BiomeFilter.biome()`. Vanilla runs every feature of every biome in the surrounding 3×3 chunks at any height, and only this filter checks the biome at the placement position; without it the feature leaks into neighbouring biomes and other layers. Datagen only logs "is missing BiomeFilter.biome()", the GameTest `biome_registry_complete` fails on it. A feature placed from inside another feature needs no filter of its own.
- Biomes that share features must list them in the same relative order, or world loading fails in `FeatureSorter` with "Feature order cycle found". Biomes with pools get their water plants from `dimension/biomes/WaterDecorations.add(generation)`: add a new pool plant there, not to single biomes.
- Do not write outside the chunks a feature may touch: its own chunk and the eight around it, so a block at most 16 blocks from the origin along X and Z always fits. Size the feature so that it always fits, and do not check blocks one by one (vanilla `ensureCanWrite` also logs an error for every block outside). The GameTest `feature_reach` fails on a feature that writes further.
- Changing an existing feature only affects chunks generated afterwards.
