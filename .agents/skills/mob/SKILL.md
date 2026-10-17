---
name: mob
description: >-
  Checklist for adding or changing a RuneRuin mob: entity class and type, attributes, spawn rules and
  biome spawns, the Blockbench project and its exported animations, model, renderer and layers,
  textures, spawn egg, creative tab, translations, a GameTest, renders, and the hand-off of the model
  and textures to a person who finishes them. Use whenever a mob is created or its model, textures,
  animations or registration change.
---

# Mob

A new mob touches 12–15 files. Copy the closest existing mob instead of inventing a new pattern: Snail is a simple creature with one layer and no animation, Mossy Prophet and Mossy Warrior are monsters with a shared base class, moss of their own boxes and animations. Every path below is under `src/main/java/ioann/uwu/runeruin/` or `src/main/resources/assets/runeruin/`.

| Step | File | What |
|------|------|------|
| 1 | `entities/<Name>.java` | The mob: goals, `createAttributes()`, sounds (vanilla ones until it has its own, as `MossyRam` borrows the goat's). Mobs of one family share a base class with their goals and poses (`MossyRam`). State the client needs for a pose is synced with `SynchedEntityData` (`MossyRam.Action`) and its progress counted on the client, with the value of the previous tick for smooth frames. |
| 2 | `entities/RREntityTypes.java` | `REGISTRY.registerEntityType`: the category (`CREATURE`, `MONSTER`), `sized`, `eyeHeight`, `clientTrackingRange`, `passengerAttachments` for a mob that carries another. Every mob so far has `noLootTable()`: there is no entity loot provider yet, and the first mob with drops adds one to `datagen/`. |
| 3 | `RuneRuinMod.java` | A listener for `registerAttributes` (`EntityAttributeCreationEvent`); a family registers all its members in one method (`MossyRam.registerAttributes`). A mob without attributes crashes the game when it spawns. A mob that spawns naturally also registers `registerSpawnPlacements` (`Snail`). |
| 4 | `dimension/biomes/<layer>/<Biome>.java` | Natural spawns: `mobs.addSpawn(category, weight, new MobSpawnSettings.SpawnerData(type, min, max))`, as `JungleSwamp` and `StoneForest` spawn the snail; follow the `worldgen-biome` skill. A mob of a structure is spawned by its piece instead (`worldgen-structure` skill); one that another mob calls is created by it (`MossyProphet` summons warriors). |
| 5 | `models/entity/<name>.bbmodel` | The shape: a Blockbench project of the format Modded Entity with box UV. Write a new project by hand or with a script you do not keep, and copy the header of an existing one: `BlockbenchModel` reads only the format `modded_entity` saved by Blockbench 5 (`groups` and `outliner`). Every cube lies in a bone, bones have names in snake_case, and the animation finds them by name and place in the tree. `model_identifier` is the name of the exported animation class without `Animation`. Textures refer to their PNGs by `relative_path` (`../../textures/entity/<name>/<name>.png`) and have no `path`. |
| 6 | `client/model/<Name>Model.java` | `ModelLayerLocation` per layer and `createBodyLayer()` returning `BlockbenchModel.layer("<name>", <Name>Model::new)`. Moss, fur or a beard are cubes of the same project in the bones they grow on and on the same texture, as the `moss_*` cubes of `mossy_prophet.bbmodel`, not a second layer: a person sees and shapes them in Blockbench. The Java model only finds the parts it moves and plays the animations; it builds no geometry. |
| 7 | `client/model/<ModelIdentifier>Animation.java` | Animations are drawn in the `.bbmodel` (Animate mode) and exported with File → Export → Export Modded Entity Animations; the export replaces the class from `// Save this class` to the end, and the class is never edited by hand. An agent edits the animations in the project and exports them at web.blockbench.net, or writes the keyframes into the project and asks the person to export. A movement whose progress the mob counts shows the frame of that progress, 0 the first frame and 1 the last (`MossyRamModel.Movement`, the walk by `applyWalk`); a one-off event without such progress, such as a roar, plays in seconds from an `AnimationState` the mob starts (vanilla Warden). |
| 8 | `client/<Name>Renderer.java`, `client/<Name>RenderState.java` | A `MobRenderer` with the texture and the shadow radius (`MossyRamRenderer`). A render state of its own only when the model needs more than `LivingEntityRenderState`: `extractRenderState` copies the progress of the poses into it (`MossyRamRenderState`). Mobs of one family share a renderer with the layer location as a parameter. |
| 9 | `client/RuneRuinClient.java` | Every layer in `registerEntityLayers`, the renderer in `registerEntityRenderers`. |
| 10 | `textures/entity/<name>/` | `<name>.png` at the size of the project's `resolution`, one texture for all the cubes of the project, and a PNG per variant, made with the `minecraft-pixel-texture-generation` skill. The UV map of the project decides where every face lies (`minecraft-model-texture-analysis` skill). |
| 11 | `items/RRItems.java`, `datagen/DatagenModelProvider.java`, `textures/item/<name>_spawn_egg.png` | The spawn egg: `REGISTRY.registerItem("<name>_spawn_egg", SpawnEggItem::new, properties -> properties.spawnEgg(type))`, a flat item model (`generateFlatItem`) and its icon. |
| 12 | `creativetab/RRCreativeModeTabs.java` | The egg in the manual `displayItems` list, next to the other eggs. |
| 13 | `lang/en_us.json` and `ru_ru.json` | `entity.runeruin.<name>` and `item.runeruin.<name>_spawn_egg` in both files; they are hand-written. The linter checks only the egg: a missing entity name shows the raw key in game. |
| 14 | `preview/RRGameTests.java` | A GameTest for the behaviour that makes the mob what it is: an attack, a jump, a summon. Register the function in `TEST_FUNCTIONS` and the test at the end of `registerTests`: tests stand in a grid in that order, and one put in the middle moves the others into each other. A fight stands on `fightFloor` at a height no other test uses, against a `fightTarget` snail, and ends with `endFight`. |

**Renaming or removing.** Search the id everywhere first: Java, lang keys, the project, textures, the egg, biome spawns and structure templates. A renamed or removed entity disappears from existing worlds: keep the old id loading with `REGISTRY.addAlias(old, new)`, and propose a removal as a design first.

## Checks

Run `compileJava` and `runData`: data generation builds every layer from its project (`EntityModelExport` writes them to `build/entity_models`), so a broken project or a bone that an animation needs and the project lacks fails here with the name of the file. Then `python scripts/lint_assets.py`, which catches an absolute texture path in the project and a missing egg model, icon or name. Render the model with `scripts/render_entity_model.py` (bare name, e.g. `snail`, and the texture id `runeruin:entity/<name>/<name>`) and every new or changed animation into a GIF with `--animation <name>` (a PNG with `--animation` gives its still frames for a closer look); look at every picture. Run `runGameTestServer` and `build`.

Nothing catches a wrong shadow, lighting, spawning rate, or when an animation starts and how fast it plays: list them under "Check in game".

## Hand-off to a person

Required for every mob task that made or changed its model, animations or textures (`hand-off` skill). The files are the `.bbmodel`, every texture of the mob (further layers and variants included) and the spawn egg icon. With the project, list the bones that the animations and the Java model move, whose names and places in the tree must stay, and say that changed animations are exported again with File → Export → Export Modded Entity Animations into the `Animation` class. The checks afterwards are `runData`, the linter, the renders and `runGameTestServer`. If the person renamed or moved a bone that the code needs, report it and ask: change the code to the new name, not the file back.
