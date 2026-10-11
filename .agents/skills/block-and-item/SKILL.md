---
name: block-and-item
description: >-
  Checklist for adding, renaming or removing a RuneRuin block or item: registration, creative tab,
  blockstate datagen, the model as a file a person finishes in Blockbench, loot, tags, recipes, client
  tints and renderers, translations and textures. Use whenever a block or item is created or its
  registration, model or datagen changes.
---

# Block or item

A new block touches 8–10 files. Copy the closest existing block (same shape, render layer, item form) instead of inventing a new pattern. Insert new entries next to related ones, not at the end of a file: parallel branches then merge without conflicts.

| Step | File | What |
|------|------|------|
| 1 | `blocks/RRBlocks.java` | Register with a local helper. `register(...)` also registers the `BlockItem`; `registerNoItem(...)` has none; `registerInWater`, `registerWaterLily`, `registerGlowingMoss*` cover those families. |
| 2 | `items/RRItems.java` | Standalone items only (`REGISTRY.registerItem`). |
| 3 | `creativetab/RRCreativeModeTabs.java` | Add the stack to the manual `displayItems` list (`Items` or `Blocks` section). |
| 4 | `datagen/DatagenModelProvider.java`, `datagen/models/` | Blockstate and item definition: one line per block in `registerModels`. A block of an existing family (trees, portals, lily pads) goes into that family's class in `datagen/models/`. A model on a vanilla parent (cube, cross, stairs) is generated there too. The render layer comes from the model textures (e.g. `TextureMapping::forceAllTranslucent`); follow the most similar block. |
| 4a | `assets/runeruin/models/{block,item}/` | A model with a geometry of its own is a file here, a draft that a person finishes in Blockbench (AGENTS.md, "What a person finishes"); the blockstate of step 4 refers to it by id. Write the JSON yourself, by hand or with a script you do not keep: `"format_version": "1.21.11"` first, a `name` on every element, and no faces of two elements in one plane, which flicker. A model that only moves such a file (lifted onto moss, sunk) is derived from it by `ModelJson.lifted`, never drawn a second time. |
| 5 | `datagen/DatagenBlockLootTableProvider.java` | A loot table for every block in `RRBlocks.REGISTRY`: `runData` fails on a missing one. |
| 6 | `datagen/DatagenBlockTagProvider.java`, `DatagenItemTagProvider.java` | Tool tags (`MINEABLE_WITH_*`, a `NEEDS_*_TOOL` tier if required) and vanilla families (`LOGS`, `LEAVES`, …). A missing tool tag fails silently: the block mines slowly and drops nothing if it requires the correct tool. |
| 7 | `datagen/DatagenRecipeProvider.java`, `DatagenDataMapProvider.java` | Recipes and data maps (e.g. compostables), if needed. |
| 8 | `client/RuneRuinClient.java` | Tint sources (`registerBlockTintSources`) and renderers, if needed. |
| 9 | `assets/runeruin/lang/en_us.json` and `ru_ru.json` | `block.runeruin.<id>` / `item.runeruin.<id>` in both files; they are hand-written. A missing key shows the raw key in game. |
| 10 | `assets/runeruin/textures/{block,item,entity}/` | PNGs, made with the `minecraft-pixel-texture-generation` skill. A missing texture only shows as magenta-black in game. |

Then run `compileJava`, `runData` and `python scripts/lint_assets.py`: the linter catches a missing model, texture or translation, a texture of the wrong size and a model file that Blockbench would save for another version of the game. Render the new model with `scripts/render_model.py`. Nothing catches a wrong render layer, drops or tool, so list those under "Check in game" in the final report.
