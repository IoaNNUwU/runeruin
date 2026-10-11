---
name: worldgen-structure
description: >-
  Add or change a RuneRuin structure (large multi-piece builds such as Giant Goblet or Baobab):
  structure and piece classes and types, datapack structure and structure set, biome tag, preview,
  and man-made builds from templates a person saves in the game. Use when something is too large for
  one feature or must span chunks.
---

# Structure

## Built by a person or by code

- A man-made build (a ship, a hut, a dungeon room, a ruin) is built by a person in the game and kept as templates in `data/runeruin/structure/<folder>/`. The structure adds a `TemplatePiece` and takes its template from `TemplatePiece.variants(templates, "<folder>")`, picked with the random of the structure; one more template in the folder is one more variant. Code decides where the build stands, how it is turned and what wear it gets on top (rotation, mirror and processors, as `StoneLilyFeature` does). Code does not shape the build.
- A shape of nature that differs with every seed (a tree, a goblet, a skeleton) is code, below.
- Not sure which of the two it is: ask the user before writing either.

A draft of a build for the person to start from is a generator with a preview job (`headless-preview` skill), shown as renders. The person runs `/rrpreview place <job> [seed]` to get it two blocks away in the game, rebuilds it, selects it with `/rrpos1` and `/rrpos2` and saves it with `/rrsave <folder>/<name>` (`entities` after the name keeps item frames and armor stands). A game started with `runClient` writes the template into `src/main/resources/data/runeruin/structure/` of its checkout; `/place template runeruin:<folder>/<name>` puts it back into a world. A structure void block in the build keeps the block of the world, air clears it. Once the templates are in, delete the generator: the build has one source. In the report, give the person the commands to type, with the real job and template names.

## In code

Copy Giant Goblet (`GiantGobletStructure`, `GiantGobletPiece`) or Baobab (`BaobabStructure`, `BaobabPiece`, `BaobabTreeGenerator`).

1. `dimension/structures/FooStructure.java` and `FooPiece.java`.
2. Types: `RRStructureTypes` and `RRStructurePieceTypes`.
3. Datapack: `RRStructures` (structure, biomes by tag) and `RRStructureSets` (spacing).
4. Biome tag: `RRBiomeTags.HAS_FOO` + `DatagenBiomeTagProvider`.
5. Preview job (`headless-preview` skill), then `compileJava` and `runData`.

Changing the algorithm of an existing structure leaves seams in structures that existing worlds have only half generated; say so in the report.
