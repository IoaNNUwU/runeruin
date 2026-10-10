---
name: headless-preview
description: >-
  Build a RuneRuin feature or structure without the game and look at it: runPreview jobs and
  parameters, several seeds in one run, render_preview.py pictures (3D views from any side, cuts,
  close-ups, before and after) to show in the chat, exports/ outputs, in-game /rrpreview, and adding a
  new PreviewJob. Use after every change to the shape of a feature or structure, or when a new one
  needs a preview.
---

# Headless preview

`runPreview` builds one named job in an in-memory world without the client and writes `exports/preview_<job>.json`, the midplanes `_xy.txt` / `_xz.txt` / `_yz.txt` and `_info.txt` (seed, parameters, block counts). A failed job exits non-zero. Run it from the task worktree; it locks `build/` like any Gradle run.

```powershell
.\gradlew.bat runPreview -Ppreview=list
.\gradlew.bat runPreview -Ppreview=boulder -Pseed=3 -Pradius=10
python scripts/render_preview.py exports/boulder.png exports/preview_boulder.json
```

Jobs: `giant_goblet` (default), `baobab`, `dinosaur_skeleton`, `hanging_tracks` (a whole network, or one piece with `'-Parg.piece=stairs'`; `'-Parg.terrain=real'` puts it under the Deep caves ceiling of the seed), `boulder`, `monolith`, `mini_volcano`, `glowing_ball`, `goblet_moss`, `glowing_mushroom`, `ashen_mushroom`, `cave_mushroom`, `water_lily`, `feature` (any configured feature, below) and `world_region` (terrain replay of a `/rrexport` region; follow the `region-export-compare` skill). `terrain_bench` is in the list too, but it measures speed, not shapes: follow the `benchmark` skill.

Parameters: `-Pseed`, `-Pseeds`, `-Pheight`, `-Pradius`, `-PminRadius`, `-PmaxRadius`, `-Pstage`, `-Pregion=<export.json>`, `-PpreviewName=<name>`, or any `-Parg.<key>=<value>` (becomes `runeruin.preview.<key>`). In PowerShell quote the `-Parg.` ones: `'-Parg.id=runeruin:stone_spike'`; unquoted, PowerShell splits them at the dot and Gradle looks for a task.

`-Pseeds=1-9` or `-Pseeds=1,4,7` (not negative) builds every seed in one run, at most 64, and names the files `preview_<job>_s<seed>.*`.

## Look at the result

Render after every change to a shape and look at the picture before you report: the text files tell where each block is, not what the thing looks like. `scripts/render_preview.py` (needs Pillow) draws the exported JSON, so another view takes about a second and no new preview run.

```powershell
python scripts/render_preview.py exports/goblet.png exports/preview_giant_goblet.json
python scripts/render_preview.py exports/goblet_cut.png exports/preview_giant_goblet.json --cut x
python scripts/render_preview.py exports/spikes.png 'exports/preview_stone_spike_s*.json'
```

Pick the view by the question:

| Question | View |
|----------|------|
| Overall shape | no options: the four corners from above; add `--view ne:above,sw:above` for two opposite corners at a larger scale |
| Something hanging from a ceiling | the default sheet adds the four corners from below when the preview prepared a ceiling; otherwise `--view se:below,nw:below` or `--view bottom` |
| Silhouette, proportions, tilt | straight views: `--view n:level,e:level` from the side, `--view top` from above |
| Wall thickness, hollows, what is inside | `--cut x`, `--cut y`, `--cut z` through the middle (the plane of the text slice), or `--cut x=12` |
| One detail | `--box x1,y1,z1,x2,y2,z2`, with `--view` if needed |
| How much the shape varies | `-Pseeds=1-9`, then all the exports on one sheet |
| What a change did | both exports on one sheet (before and after side by side), or the new export with `--diff exports/<before>.json` |
| Exact positions and counts | the text slices and `_info.txt`, not the picture |

`--view` takes `<side>:<height>` pairs separated by commas. The side is where the camera stands: `n ne e se s sw w nw` or a compass bearing in degrees. The height is `above` (30° up), `level`, `below`, `top`, `bottom` or degrees. Coordinates on the box edges, in `--box` and in `--cut` are local, the same as in the text slices; the header of the sheet gives the origin, world = origin + local.

How to read the picture:

- Faces are shaded by direction as in the game and darken with distance, so straight views keep depth.
- Faint blocks are what the preview prepared: ground, ceiling, pool. The view is framed on the subject with four blocks around it; `--box all` shows the whole export, `--prepared hide` or `--prepared solid` changes how the prepared blocks are drawn.
- Water is translucent. Blocks without collision (plants, vines, kelp) are outlines.
- A cut face is pale and unshaded. The half nearer to the camera is removed.
- Colours are the average of the block's texture. The script prints the blocks it found no texture for.

For before and after, export the old shape first under another name (`-PpreviewName=preview_boulder_before`, in the task worktree before your change or in the main checkout), then the new one. `--diff` draws added blocks green, removed ones red and replaced ones orange.

Then:

1. Write down what you see: the outline, proportions, what is hollow, what is attached to what, what looks wrong.
2. Compare it with the request. Change the code only for a difference you can name.
3. Show the final pictures in the chat, each with its job, seed and parameters; for a change to an existing shape, before and after. The pictures are in `exports/`, which git ignores: attach them, do not commit them.

Limits: a preview skips placement modifiers and real terrain and uses its own random source, so it reproduces a shape, not a real chunk. Every block is drawn as a full cube in one colour. Lighting, textures, transparency and render layers still need the game: list them under "Check in game".

## Any feature by id

The `feature` job places a configured feature from the registry on a prepared surface, four blocks thick, and fails when the feature changes no block:

```powershell
.\gradlew.bat runPreview -Ppreview=feature '-Parg.id=runeruin:inverted_tree' '-Parg.surface=ceiling' '-Parg.ground=minecraft:moss_block' '-Parg.offset=0'
```

| `surface` | Prepared blocks | Default origin |
|-----------|-----------------|----------------|
| `floor` | ground y 60–63 | air above it (`offset=1`) |
| `ceiling` | ground y 65–68 | air below it (`offset=-1`) |
| `water` | ground y 53–56, water 57–63 | air above the water |
| `underwater` | the same pool | water above the pool floor |
| `wall` | ground x 1–4, y 48–80 | the wall block 1 64 0 (`offset=0`), as wall placements expect |
| `cave` | floor y 32–35, ceiling y 93–96 | air at 0 64 0, for spikes that scan both ways |

`offset` shifts the origin from the surface block, like the scan of a placed feature: features rooted in the ceiling block itself (`inverted_tree`, `long_ceiling_block_vine`) need `offset=0`. `ground` defaults to stone. `x` and `z` (0–15) move the origin and its surface away from the chunk corner: giant spikes are wider in the middle of a chunk (`'-Parg.x=8' '-Parg.z=8'`). The GameTests `feature_placement` and `feature_reach` run every configured feature this way (the table in `RRGameTests.featureCases`); add a row for every new configured feature, `feature_reach` fails without it.

In game: `/rrpreview list`, `/rrpreview <job> [seed] [key=value | name]…`. The client writes `/rrpreview` and `/rrexport` output to `~/.runeruin/game/runeruin-exports/`, not `exports/`; `render_preview.py` draws those files too.

## New preview job

Most features need no job: use `feature`. A job of its own is for parameters the registry does not hold or for structures: a class in `preview/jobs/` implementing `PreviewJob`, registered in `PreviewCatalog`. For write-only features build the config and call `PreviewJobs.placeFeature`; features that scan terrain need a floor or ceiling first. Structures build their pieces directly (see `BaobabPreviewJob`, `GiantGobletPreviewJob`).

Build the ground, ceiling or pool with `PreviewWorld.fillBox`, and only those: the export marks the blocks it filled as prepared, and the renderer fades them and frames the picture on everything else. A block the feature replaces is no longer prepared.

`PreviewWorld` answers block reads from its own map; a `WorldGenLevel` method it does not handle goes to the live server level and reads the real world. If a feature places nothing in the preview but works in game, look for such a method and add a case to `PreviewWorld.invoke` (as for `isStateAtPosition`).
