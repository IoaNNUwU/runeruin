---
name: worldgen-structure
description: >-
  Add or change a RuneRuin structure (large multi-piece builds such as Giant Goblet or Baobab):
  structure and piece classes and types, datapack structure and structure set, biome tag, preview.
  Use when something is too large for one feature or must span chunks.
---

# Structure

Copy Giant Goblet (`GiantGobletStructure`, `GiantGobletPiece`) or Baobab (`BaobabStructure`, `BaobabPiece`, `BaobabTreeGenerator`).

1. `dimension/structures/FooStructure.java` and `FooPiece.java`.
2. Types: `RRStructureTypes` and `RRStructurePieceTypes`.
3. Datapack: `RRStructures` (structure, biomes by tag) and `RRStructureSets` (spacing).
4. Biome tag: `RRBiomeTags.HAS_FOO` + `DatagenBiomeTagProvider`.
5. Preview job (`headless-preview` skill), then `compileJava` and `runData`.

Changing the algorithm of an existing structure leaves seams in structures that existing worlds have only half generated; say so in the report.
