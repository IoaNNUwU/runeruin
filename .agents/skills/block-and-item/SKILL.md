---
name: block-and-item
description: >-
  Checklist for adding, renaming or removing a RuneRuin block or item: registration, creative tab,
  blockstate and model datagen, loot, tags, recipes, client tints and renderers, translations and
  textures. Use whenever a block or item is created or its registration or datagen changes.
---

# Block or item

A new block touches 8–10 files. Copy the closest existing block (same shape, render layer, item form) instead of inventing a new pattern. Insert new entries next to related ones, not at the end of a file: parallel branches then merge without conflicts.

| Step | File | What |
|------|------|------|
| 1 | `blocks/RRBlocks.java` | Register with a local helper. `register(...)` also registers the `BlockItem`; `registerNoItem(...)` has none; `registerInWater`, `registerWaterLily`, `registerGlowingMoss*` cover those families. |
| 2 | `items/RRItems.java` | Standalone items only (`REGISTRY.registerItem`). |
| 3 | `creativetab/RRCreativeModeTabs.java` | Add the stack to the manual `displayItems` list (`Items` or `Blocks` section). |
| 4 | `datagen/DatagenModelProvider.java` | Blockstate, block model and item model in `registerModels`. The render layer comes from the model textures (e.g. `TextureMapping::forceAllTranslucent`); follow the most similar block. Hand-built models get their own `create…` method. |
| 5 | `datagen/DatagenBlockLootTableProvider.java` | A loot table for every block in `RRBlocks.REGISTRY`: `runData` fails on a missing one. |
| 6 | `datagen/DatagenBlockTagProvider.java`, `DatagenItemTagProvider.java` | Tool tags (`MINEABLE_WITH_*`, a `NEEDS_*_TOOL` tier if required) and vanilla families (`LOGS`, `LEAVES`, …). A missing tool tag fails silently: the block mines slowly and drops nothing if it requires the correct tool. |
| 7 | `datagen/DatagenRecipeProvider.java`, `DatagenDataMapProvider.java` | Recipes and data maps (e.g. compostables), if needed. |
| 8 | `client/RuneRuinClient.java` | Tint sources (`registerBlockTintSources`) and renderers, if needed. |
| 9 | `assets/runeruin/lang/en_us.json` and `ru_ru.json` | `block.runeruin.<id>` / `item.runeruin.<id>` in both files; they are hand-written. A missing key shows the raw key in game. |
| 10 | `assets/runeruin/textures/{block,item,entity}/` | PNGs, made with the `minecraft-pixel-texture-generation` skill. A missing texture only shows as magenta-black in game. |

Then run `compileJava` and `runData`. Datagen does not catch a missing texture, translation, wrong render layer, drops or tool, so list those under "Check in game" in the final report.
