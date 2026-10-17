---
name: minecraft-model-texture-analysis
description: >-
  Inspect Minecraft block, item, and mob model geometry and texture UVs. Use when a task asks
  which texture region a model element or face uses, or requests a visual check of Minecraft
  textures, model parts or mob animations (rendered into a GIF).
---

# Minecraft model and texture analysis

Connect a rendered block or mob part to its model geometry and source texture. Keep source assets intact unless the user asks for an edit.

## Find the model and texture

- Models with a geometry of their own: `src/main/resources/assets/runeruin/models/{block,item}/`.
- Models on vanilla parents, the models derived from a kept one and the blockstates: `src/generated/resources/assets/runeruin/`, written by `runData` from `datagen/`. If one is absent, check whether datagen has run; never invent generated JSON.
- Textures: `src/main/resources/assets/runeruin/textures/{block,item,entity}/`; vanilla models and textures in `.mc-sources/textures/assets/minecraft/`.
- Mobs: the Blockbench project `assets/runeruin/models/entity/<name>.bbmodel`, bones with cubes and a `uv_offset` and size per cube (box UV, read by `client/model/BlockbenchModel`), and its animations, exported into `client/model/<Name>Animation.java`. A vanilla mob is Java: [vanilla entity UVs](references/vanilla-entity-uv.md).

Follow blockstate variants or item model references until the model that carries the geometry. For a mob, follow the renderer to its texture id and inspect both the UVs and the full atlas. Only when the shipped resources matter: `runData`, then `build`, and open `build/libs/*.jar` as a zip.

## Extract face UVs

`extract_model_faces.py` crops the texture region of each selected face of a block or item model (texture variables, project and vanilla parents, omitted `uv`) into a PNG, with a `manifest.json` that maps each crop back to the element, direction, texture, UV, source pixels and face rotation:

```powershell
python .agents/skills/minecraft-model-texture-analysis/scripts/extract_model_faces.py runeruin:block/firefly_in_a_jar --element jar_glass_body --face north
```

The output goes to `exports/model_faces/<namespace>/<model path>/`. Read `manifest.json` before judging a crop: rotation and mirroring are recorded, not baked into the PNG, and UVs outside the texture are clipped and reported. Every model file of this mod names its elements; without names select by index, `--element '#0'`, identify the part by `from`, `to`, faces and textures in the manifest, and report ambiguity instead of treating array order as a stable label.

## Render

Use these instead of writing a renderer; `--help` describes every option. Under the views each draws a row per texture with its colors.

- `scripts/render_model.py`: a block or item model, baked as the game does, in two isometric views, the south face and the top. `--y-rot` turns it as the `y` of a blockstate variant, `--outline x1,y1,z1,x2,y2,z2` (one per `Block.box` of `getShape`) draws the outline over it.

  ```powershell
  python scripts/render_model.py exports/bush.png wispberry_bush_stage3 --y-rot 90 --outline 7,0,7,9,6,9 --outline 3,5,3,13,16,13
  ```

- `scripts/render_entity_model.py`: a mob or any entity model with a texture. The geometry comes from `build/entity_models/`, which `runData` writes from the Java models and our projects: run `runData` after a change to a model or its project; a texture change needs nothing. A model is named by its `ModelLayerLocation` (`ModelLayers` for vanilla). `--layer` draws further layers, `--view yaw,pitch` sets the cameras, `--pose` moves parts as an animation would (to check pivots), `--animation` plays an animation into a GIF, or into still frames for a `.png`. The animation plays at full strength: the game scales a walk by the speed of the mob and turns the head in code. Every new or changed animation gets a GIF in the pull request.

  ```powershell
  python scripts/render_entity_model.py exports/snail.png snail runeruin:entity/snail/snail
  python scripts/render_entity_model.py exports/punch.gif mossy_warrior runeruin:entity/mossy_warrior/mossy_warrior --animation punch
  ```

## Report

Inspect the full source texture alongside the crops: transparency, UV bounds, face direction, texture variables, parent inheritance, model bounds, element rotation and animation metadata. Describe findings by the element name (or index and bounds), face direction, texture id and UV/pixel rectangle. A proposed fix names the file a person edits (the model JSON in `assets/runeruin/models/`, the mob's `.bbmodel` or the texture PNG; never a generator) and which faces or UV values would change, before editing.
