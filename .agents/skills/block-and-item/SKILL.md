---
name: block-and-item
description: >-
  Checklist for adding, renaming or removing a RuneRuin block or item: registration, creative tab,
  blockstate datagen, the model as a file a person finishes in Blockbench, loot, tags, recipes, client
  tints and renderers, translations, textures, the outline that follows the model, and the hand-off of
  models and textures to a person who finishes them. Use whenever a block or item is created or its
  registration, model, outline or datagen changes.
---

# Block or item

A new block touches 8–10 files. Copy the closest existing block (same shape, render layer, item form) instead of inventing a new pattern.

| Step | File | What |
|------|------|------|
| 1 | `blocks/RRBlocks.java` | Register with a local helper. `register(...)` also registers the `BlockItem`; `registerNoItem(...)` has none (wall variants, potted plants); `registerInWater` and `registerWaterLily` cover those families. `Properties.ofFullCopy` of a vanilla wall sign or torch also copies the loot table and name that vanilla points at its standing block: use `wallVariant`, or the GameTest `blocks_own_loot_and_names` fails. |
| 2 | `items/RRItems.java` | Standalone items only (`REGISTRY.registerItem`). |
| 3 | `creativetab/RRCreativeModeTabs.java` | Add the stack to the manual `displayItems` list (`Items` or `Blocks` section). |
| 4 | `datagen/DatagenModelProvider.java`, `datagen/models/` | Blockstate and item definition: one line per block in `registerModels`. A block of an existing family (trees, portals, lily pads) goes into that family's class in `datagen/models/`. A model on a vanilla parent (cube, cross, stairs) is generated there too. The render layer comes from the model textures (e.g. `TextureMapping::forceAllTranslucent`); follow the most similar block. A model file of step 4a sets it in its own `textures`: a texture with half-transparent pixels, or `{"sprite": "runeruin:block/foo", "force_translucent": true}`. |
| 4a | `assets/runeruin/models/{block,item}/` | A model with a geometry of its own is a file here, which the blockstate of step 4 refers to by id. Write a new one by hand or with a script you do not keep. A new file has `"format_version": "1.21.11"` first, a `name` on every element, and no faces of two elements in one plane, which flicker. A model that only moves such a file (lifted onto moss, sunk) is derived from it by `ModelJson.lifted`, never drawn a second time. |
| 4b | the block class | A block with a model of step 4a always gets an outline that follows the model: `getShape`, the box the player sees and clicks. One `Block.box` per main part of the model (a stem, a crown, a body, a lid), none for small details (berries, petals, buds, thin twigs). A model turned by a blockstate property turns its outline the same way with `Shapes.rotateHorizontal`; a model turned at random (`createRotatedVariants`) picks the turn from the seed of the position, as `WispberryBushBlock` does. A shaped model never keeps a full block or a square column as its outline. |
| 5 | `datagen/DatagenBlockLootTableProvider.java` | A loot table for every block in `RRBlocks.REGISTRY`: `runData` fails on a missing one. |
| 6 | `datagen/DatagenBlockTagProvider.java`, `DatagenItemTagProvider.java` | Tool tags (`MINEABLE_WITH_*`, a `NEEDS_*_TOOL` tier if required) and vanilla families (`LOGS`, `LEAVES`, …). A missing tool tag fails silently: the block mines slowly and drops nothing if it requires the correct tool. |
| 7 | `datagen/DatagenRecipeProvider.java`, `DatagenDataMapProvider.java` | Recipes and data maps (e.g. compostables), if needed. |
| 8 | `client/RuneRuinClient.java` | Tint sources (`registerBlockTintSources`) and renderers, if needed. |
| 9 | `assets/runeruin/lang/en_us.json` and `ru_ru.json` | `block.runeruin.<id>` / `item.runeruin.<id>` in both files; they are hand-written. A missing key shows the raw key in game. |
| 10 | `assets/runeruin/textures/{block,item,entity}/` | PNGs, made with the `minecraft-pixel-texture-generation` skill. A missing texture only shows as magenta-black in game. |

**Renaming or removing.** Search the id everywhere first: Java, lang keys, model and blockstate files, textures, tags, recipes, the creative tab and the structure templates (`.nbt`) that contain the block. A renamed or removed id disappears from existing worlds: keep the old id loading with `REGISTRY.addAlias(old, new)` (as `RRItems` does for the wispberry), and propose a removal as a design first.

Then run `compileJava`, `runData` and `python scripts/lint_assets.py`: the linter catches a missing model, texture or translation, a texture of the wrong size and a model file that Blockbench would save for another version of the game. Render the new model with `scripts/render_model.py` twice and look at both pictures: without the outline, to judge the model, and with it, one `--outline x1,y1,z1,x2,y2,z2` per box of `getShape` (and `--y-rot` for a turned variant), to see that the outline covers the main parts and only them. Show both in the report. Nothing catches a wrong render layer, drops or tool, so list those under "Check in game" in the final report.

## Hand-off to a person

A new or changed model file of step 4a or texture goes to the person before the final report (`hand-off` skill); models derived by `ModelJson.lifted` need `runData` after it is saved. Then run `runData`, the linter and the renders again and bring the outline of step 4b in line with the finished model.
