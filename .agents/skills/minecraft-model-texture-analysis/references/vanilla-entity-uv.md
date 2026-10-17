# UVs of a vanilla entity model

Vanilla mobs build their models in Java: trace the renderer and model registration to the texture, then inspect `texOffs(...)`, box sizes, texture scaling and any per-face UV declarations in the model source (`.mc-sources/code/net/minecraft/client/model/geom/ModelPart.java` for this version; custom model code may use another layout). For the standard `CubeListBuilder.texOffs(u, v).addBox(..., w, h, d)` the faces lie at (atlas pixels):

| Face | Rectangle `[left, top, right, bottom]` |
|------|-----------------------------------------|
| west | `[u, v+d, u+d, v+d+h]` |
| north | `[u+d, v+d, u+d+w, v+d+h]` |
| east | `[u+d+w, v+d, u+2d+w, v+d+h]` |
| south | `[u+2d+w, v+d, u+2d+2w, v+d+h]` |
| down | `[u+d, v, u+d+w, v+d]` |
| up | `[u+d+w, v+d, u+d+2w, v]` |

Check the builder overload when it uses texture scaling, mirroring, deformation or custom faces. Put the resulting pixel coordinates into an analysis map and crop the atlas with the same script:

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

A face may instead be an object such as `{ "uv": [0, 0, 8, 8], "rotation": 90 }`. Select a part by its name, as a block element:

```powershell
python .agents/skills/minecraft-model-texture-analysis/scripts/extract_model_faces.py --entity-uv-map exports/creature_uv_map.json --element body
```

The map is analysis input, not a Minecraft asset; keep it in `exports/`. If a mob has neither a `.bbmodel` nor a Java model in this repository, report that and analyze only the atlas rather than inventing part mappings.
