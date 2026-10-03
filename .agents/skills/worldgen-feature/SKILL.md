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
| 4 | `dimension/RRPlacedFeatures` | key + placement: count, height, scan; heights from `Const` |
| 5 | `dimension/biomes/<layer>/Bar.java` | `generation.addFeature(<step>, RRPlacedFeatures.FOO)` |
| 6 | `preview/jobs/` + `PreviewCatalog` | preview job, see the `headless-preview` skill |

Then `compileJava`, `runData` (the configured, placed and biome JSON is generated) and the preview.

Rules:

- Vanilla features and placements are fine (`Feature.BLOCK_COLUMN`, `CavePlacements.*`). Custom placement modifiers live in `dimension/placements/` and are registered in `RRPlacementModifierTypes`.
- Vanilla runs every feature of every biome in the surrounding 3×3 chunks at any height; only a `BiomeFilter` placement checks the biome at the placement position. Without it the feature leaks into neighbouring biomes and into other layers within its height range. A feature placed from inside another feature needs no filter of its own.
- Biomes that share features must list them in the same relative order, or world loading fails in `FeatureSorter` with "Feature order cycle found". Biomes with pools get their water plants from `dimension/biomes/WaterDecorations.add(generation)`: add a new pool plant there, not to single biomes.
- Do not write outside the chunks a feature may touch. Reuse an existing guard (`FeatureChunkBounds`, `ensureCanWrite` in `CaveMushroomFeature` / `GlowingMushroomFeature`) instead of adding a new one.
- Changing an existing feature only affects chunks generated afterwards.
