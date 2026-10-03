---
name: worldgen-biome
description: >-
  Add, reweight, move or change a RuneRuin biome: biome class, RRBiomes key, the layer list in
  RRBiomeSource, terrain surface blocks, biome tags and attached features. Use for any biome work.
---

# Biome

Biomes do not shape the stacked caves (`RRChunkGenerator` does); they choose surface blocks, decoration, mobs and effects.

1. Class in `dimension/biomes/<layer>/` with `bootstrap(placedFeatures, carvers)` that builds the `Biome` (mobs, effects, `addFeature`). Layer folders: `toplayer`, `bloomingcaves` / `bloomingcavesceiling`, `deepcaves` / `deepcavesceiling`, `lostcaves` / `lostcavesceiling`. Copy the closest biome of the same layer.
2. Key + `ctx.register` in `RRBiomes`.
3. Entry in the layer list in `RRBiomeSource.newDefault` (also used by `RRDimension.bootstrapStem`), or it never generates. Weights are repeated entries.
4. Surface blocks in `dimension/chunkgenerator/RRTerrainSurfaces`, or it silently gets the default surface.
5. Tags, e.g. for structures: `RRBiomeTags` + `DatagenBiomeTagProvider`.
6. `compileJava`, `runData`, `runGameTestServer`.

`RRBiomeSource.getNoiseBiome` picks `list[size * noise]`. Adding, removing or reordering any entry of a layer list, weights included, moves every biome border of that layer in existing worlds, and the terrain surface moves with it through `RRTerrainSurfaces`. Propose such a change as a design first.

Features shared with other biomes must keep the same relative order: see the `worldgen-feature` skill.
