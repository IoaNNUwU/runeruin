---
name: headless-preview
description: >-
  Check the shape of a RuneRuin feature or structure without the game: runPreview jobs, parameters,
  exports/ outputs, render_preview.py, in-game /rrpreview, and adding a new PreviewJob. Use after
  changing a feature or structure, or when a new one needs a preview.
---

# Headless preview

`runPreview` builds one named job in an in-memory world without the client and writes `exports/preview_<job>.json`, the midplanes `_xy.txt` / `_xz.txt` / `_yz.txt` and `_info.txt` (seed, parameters, block counts). A failed job exits non-zero. Run it from the task worktree; it locks `build/` like any Gradle run.

```powershell
.\gradlew.bat runPreview -Ppreview=list
.\gradlew.bat runPreview -Ppreview=boulder -Pseed=3 -Pradius=10
python scripts/render_preview.py out.png exports/preview_boulder.json
```

Jobs: `giant_goblet` (default), `baobab`, `boulder`, `monolith`, `mini_volcano`, `glowing_ball`, `goblet_moss`, `glowing_mushroom`, `ashen_mushroom`, `cave_mushroom`, `water_lily`, `feature` (any configured feature, below) and `world_region` (terrain replay of a `/rrexport` region; follow the `region-export-compare` skill). `terrain_bench` is in the list too, but it measures speed, not shapes: follow the `benchmark` skill.

Parameters: `-Pseed`, `-Pheight`, `-Pradius`, `-PminRadius`, `-PmaxRadius`, `-Pstage`, `-Pregion=<export.json>`, `-PpreviewName=<name>`, or any `-Parg.<key>=<value>` (becomes `runeruin.preview.<key>`). In PowerShell quote the `-Parg.` ones: `'-Parg.id=runeruin:stone_spike'`; unquoted, PowerShell splits them at the dot and Gradle looks for a task.

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
| `cave` | floor y 53–56, ceiling y 72–75 | air at 0 64 0, for spikes that scan both ways |

`offset` shifts the origin from the surface block, like the scan of a placed feature: features rooted in the ceiling block itself (`inverted_tree`, `long_ceiling_block_vine`) need `offset=0`. `ground` defaults to stone. The GameTest `feature_placement` runs every configured feature without a job of its own this way (the table in `RRGameTests.featurePlacement`); add a row for a new feature that has no preview job.

`render_preview.py` (needs Pillow) draws front, side and top silhouettes of the full volume: use it to judge bends, tilt and gaps, since midplanes can miss the subject. The JSON and text files stay the source of truth. Previews skip placement modifiers and use their own random source: they reproduce a shape, not a real chunk.

In game: `/rrpreview list`, `/rrpreview <job> [seed] [key=value | name]…`. The client writes `/rrpreview` and `/rrexport` output to `~/.runeruin/game/runeruin-exports/`, not `exports/`.

## New preview job

Most features need no job: use `feature`. A job of its own is for parameters the registry does not hold or for structures: a class in `preview/jobs/` implementing `PreviewJob`, registered in `PreviewCatalog`. For write-only features build the config and call `PreviewJobs.placeFeature`; features that scan terrain need a floor or ceiling from `PreviewWorld.fillBox` first. Structures build their pieces directly (see `BaobabPreviewJob`, `GiantGobletPreviewJob`).

`PreviewWorld` answers block reads from its own map; a `WorldGenLevel` method it does not handle goes to the live server level and reads the real world. If a feature places nothing in the preview but works in game, look for such a method and add a case to `PreviewWorld.invoke` (as for `isStateAtPosition`).
