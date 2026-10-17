---
name: minecraft-model-texture-analysis
description: >-
  Inspect Minecraft block, item, and mob model geometry and texture UVs. Use when a task asks
  which texture region a model element or face uses, or requests a visual check of Minecraft
  textures or model parts.
---

# Minecraft model and texture analysis

Use this skill to connect a rendered block or mob part to its model geometry and source texture. Keep source assets intact unless the user asks for an edit.

## Find the model and texture

For a NeoForge project, check these locations first:

- Models a person keeps: `src/main/resources/assets/<namespace>/models/{block,item}/`, every model with a geometry of its own.
- Datagen output: `src/generated/resources/assets/<namespace>/models/{block,item}/`, the models on vanilla parents and the ones derived from a kept model, and the related `blockstates/`.
- Authored textures: `src/main/resources/assets/<namespace>/textures/{block,item,entity}/`.
- Blockstate and derived-model generators: `src/main/java/**/datagen/`.
- Mob geometry: the Blockbench project `src/main/resources/assets/<namespace>/models/entity/<name>.bbmodel`, bones with cubes and a `uv_offset` per cube (box UV), and its animations, exported into `client/model/<Name>Animation.java`; the Java model in `client/model/` only plays them. A vanilla mob is Java: `texOffs(...)` and cube dimensions in its layer definition.

If a model JSON is absent, check whether datagen has run. Do not invent generated JSON. For model selection, follow blockstate variants or item model references until reaching the geometry-bearing model. For mobs, follow the renderer/model registration to its texture identifier and inspect both the UV declarations and the full atlas.

## Verify packaged resources

Use `build` only when the comparison needs the exact packaged resources. `runData` writes `src/generated/resources` but does not update an existing JAR. If generated model data changed, run datagen first and then package:

```powershell
.\gradlew.bat runData
.\gradlew.bat build
```

Inspect the resulting `build/libs/*.jar` as a zip to compare the shipped model or texture with its source. A normal UV extraction or `runClient` check does not require `build`.

## Extract JSON face UVs

The bundled script supports Minecraft cuboid/block model JSON with `elements`, `faces`, texture variables, project parent models, common vanilla cube parents (`cube_all`, `cube`, `cube_bottom_top`, vertical/horizontal `cube_column`, and orientable variants), and omitted `uv` values. It writes one PNG per selected face and a `manifest.json` that maps the crop back to the element, direction, texture resource, UV coordinates, source pixels, and face rotation. Other vanilla parents resolve from `.mc-sources/textures/assets/minecraft/models`, extracted by the setup script.

```powershell
python .agents/skills/minecraft-model-texture-analysis/scripts/extract_model_faces.py src/main/resources/assets/runeruin/models/block/firefly_in_a_jar.json --element jar_glass_body --face north
python .agents/skills/minecraft-model-texture-analysis/scripts/extract_model_faces.py runeruin:block/firefly_in_a_jar --element jar_glass_body
```

The output defaults to `exports/model_faces/<namespace>/<model path>/`. Read `manifest.json` before judging the image: the script crops the source UV rectangle and records rotation/mirroring without baking those transforms into the PNG. UVs outside the texture bounds are clipped and called out in the manifest and warnings. If Pillow is missing, install it in the active Python environment with `python -m pip install Pillow`.

Minecraft's cuboid element format has no standard semantic `name` field. The model files of this mod give every element a `name` (Blockbench writes it, and the `block-and-item` skill requires it); the Minecraft 26.2 model deserializer ignores it. If names are unavailable, select by array index with `--element '#0'`, then use `from`, `to`, face directions, and texture variables in the manifest to identify the part. Report ambiguity instead of treating array order as a stable semantic label.

## Render a model

`scripts/render_model.py` renders a block or item model to one PNG: isometric views from south-east and north-west, the south face and the top. It bakes like the game (vertex order and UV rotation as in `FaceBakery`, element rotation with `rescale`, directional shade) and multiplies faces with a `tintindex` by `--tint` (default grass). Item models on `item/generated` show their layers as a sprite. Use it to judge geometry and UV placement after a model or texture change instead of writing a renderer:

```powershell
python scripts/render_model.py exports/firefly_in_a_jar.png runeruin:block/firefly_in_a_jar
python scripts/render_model.py exports/model.png glowing_mushroom --scale 24
```

A bare name tries `runeruin:block/` then `runeruin:item/`. Blockstate rotations, cullfaces, emissive light and render types are not applied; transparency is blended in draw order.

## Render an entity model

`scripts/render_entity_model.py` renders a mob or any other entity model with a texture to one PNG: an isometric view of the front from above and of the back from below, the front, a side and the top. The game builds entity models in code, vanilla ones from Java and ours from the Blockbench projects in `assets/runeruin/models/entity/`, so the geometry comes from the game: `runData` writes every model layer, vanilla and ours, to `build/entity_models/<namespace>/<model>/<layer>.json`, the parts with their initial poses and the faces of their boxes as `ModelPart` builds them. Run `runData` again after a change to a model or its project; a change to a texture needs nothing. Use it for a new or changed entity model or texture instead of writing a renderer:

```powershell
python scripts/render_entity_model.py exports/snail.png snail runeruin:entity/snail/snail
python scripts/render_entity_model.py exports/cart.png minecraft:minecart runeruin:entity/minecart/runic_minecart
python scripts/render_entity_model.py exports/charge.png minecraft:creeper exports/new_charge.png --layer armor
```

A bare model name tries `runeruin:` then `minecraft:`; the names are the `ModelLayerLocation`s (`ModelLayers` for vanilla). The texture is an id or a path to a PNG. The shade is by direction, not the game's entity lighting.

`--view` replaces the five views with cameras at `yaw,pitch` in degrees. Yaw goes around the model: 0 the front, 90 its right side, 180 the back, 270 its left side. Pitch is the height of the camera: 90 straight above, -90 straight below.

```powershell
python scripts/render_entity_model.py exports/cow.png minecraft:cow entity/cow/cow_temperate --view 20,10 160,45 300,-60
```

Parts stand in their initial pose: the script does not run `setupAnim`. `--pose` changes the pose as an animation would, by the fields of `ModelPart`: `<part>.<field>=<value>` sets and `+=` adds to `x`, `y`, `z` (1/16 block), `xRot`, `yRot`, `zRot` (degrees here, radians in the game) or `xScale`, `yScale`, `zScale`. A part is its name or its path (`body/head`), the root is `root`; its children move with it. An unknown part lists the parts of the model. Use it to check the pivots of the parts and the poses an animation is going to set:

```powershell
python scripts/render_entity_model.py exports/cow_walk.png minecraft:cow entity/cow/cow_temperate --pose head.xRot=25 right_front_leg.xRot=-35 left_front_leg.xRot=35
python scripts/render_entity_model.py exports/snail.png snail runeruin:entity/snail/snail --view 45,30 0,0 --pose left_antenna.zRot+=-30 head.y+=-2
```

## Assess model changes

The mobs of this mod keep their geometry and UVs in their Blockbench project (`assets/runeruin/models/entity/<name>.bbmodel`: the `uv_offset` and size of each cube, read by `client/model/BlockbenchModel`); look there first. For vanilla mobs in Java, trace the renderer/model registration to its atlas, then inspect `texOffs(...)`, box dimensions, texture scaling, and any per-face UV declarations in the model source. This project's Minecraft 26.2 source is `.mc-sources/code/net/minecraft/client/model/geom/ModelPart.java`; custom model code may define a different layout. For the standard `CubeListBuilder.texOffs(u, v).addBox(..., w, h, d)` layout, the source maps face rectangles as follows (coordinates are atlas pixels):

| Face | Rectangle `[left, top, right, bottom]` |
|------|-----------------------------------------|
| west | `[u, v+d, u+d, v+d+h]` |
| north | `[u+d, v+d, u+d+w, v+d+h]` |
| east | `[u+d+w, v+d, u+2d+w, v+d+h]` |
| south | `[u+2d+w, v+d, u+2d+2w, v+d+h]` |
| down | `[u+d, v, u+d+w, v+d]` |
| up | `[u+d+w, v+d, u+d+2w, v]` |

Check the model builder overload when it uses texture scaling, mirroring, deformation, or custom faces. Put the resulting **pixel coordinates** in a small analysis map and use the same script to crop the atlas:

```json
{
  "format": "minecraft-entity-uv-map/1",
  "texture": "example:entity/creature",
  "texture_size": [64, 64],
  "parts": [
    { "name": "body", "faces": { "north": [0, 0, 8, 8], "south": [8, 0, 16, 8] } }
  ]
}
```

The face rectangles are `[left, top, right, bottom]` in source-atlas pixels; each face may instead be an object such as `{ "uv": [0, 0, 8, 8], "rotation": 90 }`. Select a part by its name just like a block element:

```powershell
python .agents/skills/minecraft-model-texture-analysis/scripts/extract_model_faces.py --entity-uv-map exports/creature_uv_map.json --element body
```

This map is analysis input, not a Minecraft asset. Keep the source texture and model untouched unless the user asks for an edit. If a mob has neither a `.bbmodel` nor a Java model in this repository, report that and analyze only the available atlas rather than inventing part mappings.

When reviewing or changing an asset, inspect the full source texture alongside relevant crops. Verify transparency, UV bounds, face direction, texture-variable resolution, parent inheritance, model bounds, element rotation, and any animation metadata.

Describe findings using the element name (or index and bounds), face direction, texture id, and UV/pixel rectangle. If proposing a fix, name the file a person edits (the model JSON in `assets/runeruin/models/`, the mob's `.bbmodel` or the texture PNG; never a generator) and explain which faces or UV values would change before editing.
