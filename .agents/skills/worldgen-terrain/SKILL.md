---
name: worldgen-terrain
description: >-
  Change the terrain of the RuneRuin dimension: layer heights, RRChunkGenerator and its layer generators,
  ceilings, pillars and runes, surface blocks, the noise of terrain and biome choice. Use for any change
  to the terrain or its noise.
---

# Terrain

## Layers

Bottom → top (`Const`): Void `0…50` → Lost caves `LOST_CAVES_Y` 56 … `LOST_CAVES_CEILING_Y` 131 → Deep caves 137 … 212 → Blooming caves 218 … 293 → Top layer from `TOP_LAYER_Y` 294 to the build limit 512. The 5 blocks under the Lost caves (51…55) and under the Blooming caves (213…217) are rock; the ceilings of the Void and of the Deep caves rise through it into the floor of the layer above, up to `VoidGen.roofY` and `DeepCavesGen.roofY`, and take their biomes along. Between the Lost and the Deep caves (132…136) there is no rock: the Deep caves floor rests on the line from which the Lost caves ceiling hangs (`DeepCavesAndLostCavesGen`).

## Two independent systems

1. **Terrain**: `RRChunkGenerator.fillFromNoise` → `TopLayerAndBloomingCavesGen`, `DeepCavesGen` (deep caves ceiling), `DeepCavesAndLostCavesGen`, `VoidGen`, all in `dimension/chunkgenerator/` with `HangingTerrainGenerator`; `TerracedCeiling` shapes the ceilings of the Void, the Deep and the Blooming caves; pillars and runes in `ArcaneStructureGen`, the rune patterns in `dimension/runes/`; surface blocks per biome in `RRTerrainSurfaces`. Biomes do not carve the caves.
2. **Decoration**: the vanilla feature pipeline per biome (`worldgen-feature` and `worldgen-biome` skills). `RRBiomeSource.getNoiseBiome` picks a biome by Y and noise from the lists in `RRBiomeSource.newDefault` (also used by `RRDimension.bootstrapStem`); weights are repeated entries.

## Noise

A new noise takes its name into the seed: `LazyNoise.single(name, frequency)` or `Noise.hashString(name + seed)` (`dimension/noise/`). A noise built from the bare world seed repeats every other such noise; the lost and blooming cave floors once had the same relief that way. Renaming a seed string changes existing worlds, as changing `RRTerrainSurfaces` does: design first (AGENTS.md).

## Check

The terrain of a region: replay it without the game with the `world_region` preview job and compare it with an export from the game (`region-export-compare` skill); in the game, `/rrgenerate` rebuilds a selection with the current code. A pure speed-up keeps the terrain checksum of `terrain_bench` (`benchmark` skill, only on the user's command).
