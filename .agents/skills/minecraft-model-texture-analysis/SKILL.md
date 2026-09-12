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

- Datagen output: `src/generated/resources/assets/<namespace>/models/{block,item}/` and related `blockstates/`.
- Authored textures: `src/main/resources/assets/<namespace>/textures/{block,item,entity}/`.
- Model generators: usually `src/main/java/**/datagen/` or `data/` packages.
- Java mob geometry: entity model/layer definitions and renderers in source, often `client/`; mob UV layouts are commonly declared with `texOffs(...)` and cube dimensions rather than model JSON.

If a model JSON is absent, check whether datagen has run. Do not invent generated JSON. For model selection, follow blockstate variants or item model references until reaching the geometry-bearing model. For mobs, follow the renderer/model registration to its texture identifier and inspect both the UV declarations and the full atlas.

## Verify packaged resources

Use `build` only when the comparison needs the exact packaged resources. `runData` writes `src/generated/resources` but does not update an existing JAR. If generated model data changed, run datagen first and then package:

```powershell
.\gradlew.bat runData
.\gradlew.bat build
```

Inspect the resulting `build/libs/*.jar` as a zip to compare the shipped model or texture with its source. A normal UV extraction or `runClient` check does not require `build`.

## Extract JSON face UVs

The bundled script supports Minecraft cuboid/block model JSON with `elements`, `faces`, texture variables, project parent models, common vanilla cube parents (`cube_all`, `cube`, `cube_bottom_top`, vertical/horizontal `cube_column`, and orientable variants), and omitted `uv` values. It writes one PNG per selected face and a `manifest.json` that maps the crop back to the element, direction, texture resource, UV coordinates, source pixels, and face rotation. Other vanilla parents need their model assets available through `--assets-root`.

```powershell
python .agents/skills/minecraft-model-texture-analysis/scripts/extract_model_faces.py src/generated/resources/assets/runeruin/models/block/firefly_in_a_jar.json --element jar_glass_body --face north
python .agents/skills/minecraft-model-texture-analysis/scripts/extract_model_faces.py runeruin:block/firefly_in_a_jar --element jar_glass_body
```

The output defaults to `exports/model_faces/<namespace>/<model path>/`. Read `manifest.json` before judging the image: the script crops the source UV rectangle and records rotation/mirroring without baking those transforms into the PNG. UVs outside the texture bounds are clipped and called out in the manifest and warnings. If Pillow is missing, install it in the active Python environment with `python -m pip install Pillow`.

Minecraft's cuboid element format has no standard semantic `name` field. A project may add `name` to its generated element JSON for analysis selectors; the Minecraft 26.2 model deserializer ignores this extra field. If names are unavailable, select by array index with `--element '#0'`, then use `from`, `to`, face directions, and texture variables in the manifest to identify the part. Report ambiguity instead of treating array order as a stable semantic label.

## Assess model changes

For Java mob models, trace the renderer/model registration to its atlas, then inspect `texOffs(...)`, box dimensions, texture scaling, and any per-face UV declarations in the model source. This project's Minecraft 26.2 source is `.mc-sources/net/minecraft/client/model/geom/ModelPart.java`; custom model code may define a different layout. For the standard `CubeListBuilder.texOffs(u, v).addBox(..., w, h, d)` layout, the source maps face rectangles as follows (coordinates are atlas pixels):

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

This map is analysis input, not a Minecraft asset. Keep the source texture and model untouched unless the user asks for an edit. If a mob has no geometry definition in this repository, report that and analyze only the available atlas rather than inventing part mappings.

When reviewing or changing an asset, inspect the full source texture alongside relevant crops. Verify transparency, UV bounds, face direction, texture-variable resolution, parent inheritance, model bounds, element rotation, and any animation metadata.

Describe findings using the element name (or index and bounds), face direction, texture id, and UV/pixel rectangle. If proposing a fix, identify the authored source (model generator or source texture) and explain which faces or UV values would change before editing.
