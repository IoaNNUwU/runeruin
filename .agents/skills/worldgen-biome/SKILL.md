---
name: worldgen-biome
description: >-
  Add, reweight, move or change a RuneRuin biome: biome class, RRBiomes key, the layer list in
  RRBiomeSource, terrain surface blocks, biome tags and attached features. Use for any biome work.
---

# Biome

Biomes do not shape the stacked caves (`RRChunkGenerator` does); they choose surface blocks, decoration, mobs and effects.

1. Class in `dimension/biomes/<layer>/` with `bootstrap(placedFeatures, carvers)` that builds the `Biome` (mobs, effects, `addFeature`). Layer folders: `toplayer`, `bloomingcaves` / `bloomingcavesceiling`, `deepcaves` / `deepcavesceiling`, `lostcaves` / `lostcavesceiling`, `voidlayer`. Copy the closest biome of the same layer.
2. Key + `ctx.register` in `RRBiomes`.
3. Entry in the layer list in `RRBiomeSource.newDefault` (also used by `RRDimension.bootstrapStem`), or it never generates and the GameTest `biome_registry_complete` fails. A biome registered on purpose without generating goes into `PARKED_BIOMES` in `RRGameTests`. Top layer: a range of each climate parameter (`topLayerBiome`); other layers: `evenly`, where weights are repeated entries.
4. Surface blocks in `dimension/chunkgenerator/RRTerrainSurfaces`, or `biome_registry_complete` fails ("has no RRTerrainSurfaces entry").
5. Top layer: the land of the biome, a `TerrainProfile` in `DatagenDataMapProvider` (data map `runeruin:terrain_profile`); without one it gets `TerrainProfile.DEFAULT`. Look at it with the `top_layer_map` preview job and the `world_region` job (`headless-preview` skill).
6. Tags, e.g. for structures: `RRBiomeTags` + `DatagenBiomeTagProvider`.
7. `compileJava`, `runData`, `runGameTestServer`.

The top layer picks its biome like the overworld, from climate parameters between -1 and 1: temperature, humidity, the landform (erosion: steep land low, flat land high) and the distance to the edge of the island (continentalness, for biomes of the edge). Its land follows the profiles of the biomes around, blended over 16 blocks; near the edge of an island the island keeps its own shape and hanging soil. The other layers split their own noise evenly between the entries of their list. Changing a parameter range, or adding, removing or reordering any entry of a layer list, weights included, moves every biome border of that layer in existing worlds, and the terrain surface (and on the top layer the land) moves with it: design first (AGENTS.md).

Features shared with other biomes must keep the same relative order: see the `worldgen-feature` skill.
