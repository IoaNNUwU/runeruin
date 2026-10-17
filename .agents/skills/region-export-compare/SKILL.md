---
name: region-export-compare
description: >-
  Compare a Minecraft region dump with a corrected export or a verbal fix, then change the
  structure, feature or seeded terrain generation to match. Use for /rrexport dumps, was/built
  exports and generated-versus-expected region comparisons.
---

# Region export compare

Do not eyeball large grids. Run the script first, then change generation math.

A man-made build (a ship, a hut, a dungeon room) is not turned into formulas. What the user built is the build: they save it with `/rrsave` and the structure places the template (`worldgen-structure` skill).

## Resolve inputs

Exports land in `%USERPROFILE%\.runeruin\game\runeruin-exports\` (a game started with `runClient`, whichever checkout started it) or in `exports/` of the checkout (server, preview and GameTest runs, terrain replays). Pick one, in this order:

1. **Two paths**: first the `/rrexport` dump (actual), then the corrected one (expected).
2. **One dump and a verbal fix**: profile the dump, then apply the description (e.g. "rim 1 thinner", "water one block lower").
3. **Nothing named**: in both folders, the newest `region_YYYYMMDD_HHMMSS.txt` is actual (prefer `.txt` over its twin `.json`), and a file whose name contains `should`, `expected`, `want`, `correct`, `target` or `built` is expected. The script finds them itself in `exports/`, or in `--dir`, skipping `preview_*` and `*_generated*` replays; when the two files are in different folders, pass both full paths.

Snapshots the user named, such as `was.json` and `built.json`: look for exactly those names in both folders, the newest first when the user says "the latest". A terrain replay needs the full `.json` volume.

`.txt` exists only when the selection is 1 block thick on some axis (a readable slice); thick volumes are `.json` only. Palette tokens are case-sensitive (`D` and `d` are different blocks): read the JSON with Python, not with Windows PowerShell's `ConvertFrom-Json`, which refuses keys that differ only in case. The format of both: [references/format.md](references/format.md).

Check `format`, `dimension`, `worldSeed`, `origin` and `size` before comparing: `was` and `built` must agree in all of them, and a seeded replay needs a `runeruin.region/1` JSON with `worldSeed`; if it is absent, ask the user to run `/rrexport` again with the updated mod. For a directional analysis pass `was` as actual and `built` as expected: `missing` means blocks added in `built`, `extra` blocks removed, `mismatch` a block-state replacement.

## Run the script

From the repo root (Python stdlib only):

```powershell
python .agents/skills/region-export-compare/scripts/compare_region.py
python .agents/skills/region-export-compare/scripts/compare_region.py exports/actual.txt exports/region_should_be.txt
python .agents/skills/region-export-compare/scripts/compare_region.py --profile --actual exports/actual.txt
python .agents/skills/region-export-compare/scripts/compare_region.py --dir "$env:USERPROFILE\.runeruin\game\runeruin-exports"
```

Read the whole stdout. Overlay legend:

| mark | meaning |
|------|---------|
| `.` | same (air equals air) |
| `+` | extra in actual |
| `-` | missing from actual |
| `!` | both occupied, different block |

Compare by **block state** (block id and properties), not palette letter (tokens can differ between files). Align in **world** coordinates if origins differ.

## Turn the diff into geometry

Write a short diagnosis before touching code. Prefer one systematic error:

- too wide / too narrow on both sides → outer radius, `insideSmooth`, rim thickness
- only one side → center, axis, or selection not through the midplane
- extra+missing paired on the outline → rim/outline **shifted**, not a random hole
- `!` only → fill vs water vs air, occupancy is already right
- wrong Y band → floor steps, lip height, water top, rim peak
- isolated cells → jitter, spillway, or a one-off `set(...)`, not a radius constant

Map overlay columns back to the `.txt` ruler (local axis). `world = origin + local`.

Do **not** patch individual cells or hand-edit `exports/`. Change the Java that places those blocks (usually `dimension/structures/*Piece.java` or `dimension/features/*`).

## After the code change

For **structure pieces / write-only features**, re-dump the job you are editing without the client (`headless-preview` skill), then compare `exports/preview_<job>_yz.txt` (or the matching midplane) against the expected slice.

### Seeded terrain: prove `was`, then converge to `expected`

Use this when the user provides `was` (the untouched world region) and `expected` (the same region after their desired edits), both full JSON exports that agree as above. Keep both source files unchanged.

1. **Understand the requested change.** Compare `was` with `expected`, with `was` as actual and `expected` as target. Explain the block additions, removals, material changes, and coordinates before editing generation code:

   ```powershell
   python .agents/skills/region-export-compare/scripts/compare_region.py '<path-to-was.json>' '<path-to-expected.json>'
   ```

2. **Prove the current algorithm reproduces `was`.** From the checkout whose `RRChunkGenerator` code will be edited, replay the exact `was.json` bounds and seed. Do this before changing the algorithm:

   ```powershell
   .\gradlew.bat runPreview -Ppreview=world_region '-Pregion=<absolute-path-to-was.json>' -Pstage=generated
   python .agents/skills/region-export-compare/scripts/compare_region.py exports/was_generated.json '<path-to-was.json>'
   python .agents/skills/region-export-compare/scripts/compare_terrain.py exports/was_generated.json '<path-to-was.json>'
   python .agents/skills/region-export-compare/scripts/compare_terrain.py exports/was_generated.json '<path-to-expected.json>'
   ```

   The replay is a valid baseline only when `generated` is nearly identical to `was` in the terrain-only comparison: near-total block/shape agreement with no systematic offsets. The comparison against `expected` records how far the current algorithm is from the target. If substantial terrain differences remain against `was`, investigate seed, dimension, bounds, code version, and generation stages before tuning toward `expected`.

3. **Change the algorithm toward `expected`.** Preserve `was_generated.json` as the before snapshot. Edit terrain-generation math, not either export. Replay the same immutable `was.json` after each meaningful code change:

   ```powershell
   .\gradlew.bat runPreview -Ppreview=world_region '-Pregion=<absolute-path-to-was.json>' -Pstage=generated_after_modify
   python .agents/skills/region-export-compare/scripts/compare_region.py exports/was_generated.json '<path-to-expected.json>' exports/was_generated_after_modify.json
   python .agents/skills/region-export-compare/scripts/compare_terrain.py exports/was_generated_after_modify.json '<path-to-expected.json>'
   python .agents/skills/region-export-compare/scripts/compare_terrain.py exports/was_generated_after_modify.json exports/was_generated.json
   ```

   Read the raw diff for every block difference and the terrain-only diff for geometry/material agreement. Repeat the edit → replay → compare loop until the remaining unmasked differences are very close to the user's target and their coordinates match the intended changes. Report both scores and the residual cells; do not declare success from a high percentage alone. `generated_after_modify` is overwritten on the next replay, so copy it outside `exports/` if an iteration needs to be retained.

`world_region` calls `RRChunkGenerator.fillFromNoise` on temporary in-memory chunks using the export's seed and bounds. It produces terrain and arcane pillars, but does not run carvers, biome features (including ores and vegetation), or structures. `compare_terrain.py` excludes changed cells matching its printed post-terrain block-id patterns, reports exact block and solid/air-shape match, and lists residual world-coordinate differences. Add a project-specific glob with `--ignore-id 'runeruin:some_feature*'` only when that block is known to come from a later feature stage. Always run and report the raw `compare_region.py` diff too: masking a feature cell can hide terrain that the feature replaced.

Replay output is written under the active code checkout's `exports/` as `<input-stem>_<stage>.json`; an absolute `-Pregion` path may point to an export from another checkout. Quote the whole `-Pregion=...` PowerShell argument. Replay is limited to Y=0..511, 64 chunks, and the export's 500,000-block volume limit.

Existing **world** chunks keep the old generation: an in-game `/rrexport` of an already generated structure shows the new math only in a fresh area, so ask the user to re-export a fresh area. For terrain, `/rrgenerate` rebuilds the selection in the game with the current code, the same way as `world_region` (terrain and pillars only).
