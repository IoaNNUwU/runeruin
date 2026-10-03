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

Jobs: `giant_goblet` (default), `baobab`, `boulder`, `monolith`, `mini_volcano`, `glowing_ball`, `goblet_moss`, `glowing_mushroom`, `ashen_mushroom`, `cave_mushroom`, `water_lily`, and `world_region` (terrain replay of a `/rrexport` region; follow the `region-export-compare` skill).

Parameters: `-Pseed`, `-Pheight`, `-Pradius`, `-PminRadius`, `-PmaxRadius`, `-Pstage`, `-Pregion=<export.json>`, `-PpreviewName=<name>`, or any `-Parg.<key>=<value>` (becomes `runeruin.preview.<key>`).

`render_preview.py` (needs Pillow) draws front, side and top silhouettes of the full volume: use it to judge bends, tilt and gaps, since midplanes can miss the subject. The JSON and text files stay the source of truth. Previews skip placement modifiers and use their own random source: they reproduce a shape, not a real chunk.

In game: `/rrpreview list`, `/rrpreview <job> [seed] [key=value | name]…`. The client writes `/rrpreview` and `/rrexport` output to `~/.runeruin/game/runeruin-exports/`, not `exports/`.

## New preview job

A class in `preview/jobs/` implementing `PreviewJob`, registered in `PreviewCatalog`. For write-only features build the config and call `PreviewJobs.placeFeature`; features that scan terrain need a floor or ceiling from `PreviewWorld.fillBox` first. Structures build their pieces directly (see `BaobabPreviewJob`, `GiantGobletPreviewJob`).
