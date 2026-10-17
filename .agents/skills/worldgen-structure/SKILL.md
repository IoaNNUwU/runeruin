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

- A man-made build (a ship, a hut, a dungeon room, a ruin) is built by a person in the game and kept as templates in `src/main/resources/data/runeruin/structure/<folder>/<name>.snbt`: the text format the game keeps its own templates in, one line per block, which `runData` turns into the `.nbt` the game loads. The structure adds a `TemplatePiece` and takes its template from `TemplatePiece.variants(templates, "<folder>")`, picked with the random of the structure; one more template in the folder is one more variant. Code decides where the build stands, how it is turned and what wear it gets on top (rotation, mirror and processors; `StoneLilyFeature` shows them for a template that a feature places, `HangingPlatformPiece` for a piece that joins code). Code does not shape the build. The piece type exists already (`RRStructurePieceTypes.TEMPLATE`); the structure still needs steps 1 and 3–5 of "In code" below: its structure class and type, the datapack entries and the biome tag.
- A shape of nature that differs with every seed (a tree, a goblet, a skeleton) is code, below.
- Not sure which of the two it is: ask the user before writing either.

A draft of a build for the person to start from is a generator with a preview job (`headless-preview` skill), shown as renders. The person runs `/rrpreview place <job> [seed]` to get it two blocks away in the game, rebuilds it, selects it with `/rrpos1` and `/rrpos2` and saves it with `/rrsave <folder>/<name>` (`entities` after the name keeps item frames and armor stands). A game started with `runClient` writes the template as text into `src/main/resources/data/runeruin/structure/` of its checkout and reads it from there, so it needs no `runData` to see the latest save. Once the templates are in, delete the generator: the build has one source.

To rework a template, the person types `/rrload <folder>/<name> [corner]` (Tab lists the templates): it puts the template two blocks away, unturned, and selects it; they rebuild it, move the corners with `/rrpos1` and `/rrpos2` if it grew, and `/rrsave` without a name writes it back into the same file. A structure void block in the build keeps the block of the world, air clears it, and `/rrload` puts structure void blocks back where the template keeps the world. In the report, give the person the commands to type, with the real job and template names.

A template that joins code marks the joint with a jigsaw block, whose "Turns into" names the block that stays there. The platforms of the hanging tracks (`hanging_tracks/platform` where a track ends, `hanging_tracks/dungeon` beside the tracks) are built of oak with one jigsaw in the deck under the first rail, its arrow pointing out towards the track and "Turns into" `minecraft:oak_planks`; nothing of the build stands in front of it. `HangingPlatformPiece` turns the template so that the jigsaw meets the end of the track, makes its oak inverted tree in an inverted tree network, carries the chains that reach the top layer of the template on up to the ceiling and gives supplies to every empty chest and barrel; a container keeps the loot table it names (`/data merge block <pos> {LootTable:"runeruin:chests/hanging_tracks_treasure"}`).

## In code

Copy Giant Goblet (`GiantGobletStructure`, `GiantGobletPiece`) or Baobab (`BaobabStructure`, `BaobabPiece`, `BaobabTreeGenerator`).

1. `dimension/structures/FooStructure.java` and `FooPiece.java`.
2. Types: `RRStructureTypes` and `RRStructurePieceTypes`.
3. Datapack: `RRStructures` (structure, biomes by tag) and `RRStructureSets` (spacing).
4. Biome tag: `RRBiomeTags.HAS_FOO` + `DatagenBiomeTagProvider`.
5. Preview job (`headless-preview` skill), then `compileJava`, `runData` and `runGameTestServer`.

Changing the algorithm of an existing structure leaves seams in structures that existing worlds have only half generated; say so in the report.
