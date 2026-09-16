---
name: region-export-compare
description: >-
  Compares Minecraft region dumps against corrected exports or verbal fixes, then guides
  structure, feature, or seeded terrain-generation changes. Use for /rrexport, terrain_old,
  was/built exports, or generated-versus-expected region comparisons.
---

# Region export compare

Use this whenever the user is shaping a structure, feature, or terrain region from an in-game export.
Do not eyeball large grids. Run the script first, then change generation math.

## Resolve inputs

Pick one, in this order:

1. **Two paths** — first is the `/rrexport` dump (actual), second is the corrected slice (expected). Game exports may be under `run/runeruin-exports/` or the user data directory `%USERPROFILE%\.runeruin\game\runeruin-exports/` (equivalent to `~/.runeruin/game/runeruin-exports/`); project and preview exports are under `exports/`. Typical names: `region_YYYYMMDD_HHMMSS.txt` and `region_should_be.txt`.
2. **One dump + a verbal fix** — profile the dump, then apply the description (e.g. "rim 1 thinner", "water one block lower").
3. **Nothing named** — look in `run/runeruin-exports/`, `%USERPROFILE%\.runeruin\game\runeruin-exports/` (and, if needed, other subfolders under `%USERPROFILE%\.runeruin/`), and `exports/`. The first two hold game `/rrexport` output; `exports/` holds project/preview output. Across these folders, the newest `region_YYYYMMDD_HHMMSS.txt` is actual (prefer `.txt` over its twin `.json`). A file whose name contains `should`, `expected`, `want`, `correct`, or `built` is expected. Auto-discovery skips `preview_*` and `*_generated*` replay outputs. If exports are in different folders, pass both full paths to the compare script.

`.txt` exists only when the selection is 1 block thick on some axis (a readable slice). Thick volumes are `.json` only.

For user-named snapshots such as `was.json` and `built.json`, do not rely on automatic name guessing. Search the active project’s `exports/`, `runeruin-exports/`, and `run/runeruin-exports/` folders, plus the corresponding export folders of the known checkout that launched the client. Sort by modification time when the user says “the latest files”; ignore `preview_*` outputs unless named. Use the full `.json` volume for terrain replay. A quick PowerShell discovery pass is:

```powershell
$roots = @('exports', 'runeruin-exports', 'run/runeruin-exports', '<known-client-checkout>/exports', '<known-client-checkout>/run/runeruin-exports') | Where-Object { Test-Path -LiteralPath $_ }
$files = foreach ($root in $roots) { Get-ChildItem -LiteralPath $root -File -Filter '*.json' }
$files | Where-Object { $_.BaseName -notlike 'preview_*' -and $_.BaseName -notmatch '_(generated|generated_after_modify)(_(xy|xz|yz|info))?$' } | Sort-Object LastWriteTime -Descending | Select-Object -First 20 FullName, LastWriteTime
```

Replace `<known-client-checkout>` with a real path or remove those two entries. In PowerShell, parse metadata with `ConvertFrom-Json -AsHashtable`: palette tokens are case-sensitive (`D` and `d` are separate keys).

Check `format`, `dimension`, `worldSeed`, `origin`, and `size` before comparing. A seeded replay needs a `runeruin.region/1` JSON with `worldSeed`; if it is absent, ask the user to run `/rrexport` again with the updated mod. `was` and `built` should have the same dimension, seed, origin, and size. For a directional analysis, pass `was` as actual and `built` as expected: `missing` means blocks added in `built`, `extra` means blocks removed, and `mismatch` means a block-state replacement.

## Run the script

From the repo root (Python stdlib only):

```powershell
python .agents/skills/region-export-compare/scripts/compare_region.py
python .agents/skills/region-export-compare/scripts/compare_region.py exports/actual.txt exports/region_should_be.txt
python .agents/skills/region-export-compare/scripts/compare_region.py --profile --actual exports/actual.txt
python .agents/skills/region-export-compare/scripts/compare_region.py --dir run/runeruin-exports
python .agents/skills/region-export-compare/scripts/compare_region.py --dir "$env:USERPROFILE\.runeruin\game\runeruin-exports"
```

The script's default auto-pick folder is `exports/`. Use `--dir` with the folder containing both files, such as `run/runeruin-exports` or `"$env:USERPROFILE\.runeruin\game\runeruin-exports"`; use explicit paths when the actual and expected files are split between folders.

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

For **structure pieces / write-only features**, re-dump without the client (pick the job you are editing):

```
.\gradlew.bat runPreview -Ppreview=list
.\gradlew.bat runPreview -Ppreview=giant_goblet
.\gradlew.bat runPreview -Ppreview=boulder -Pradius=10
```

Then compare `exports/preview_<job>_yz.txt` (or the matching midplane) against the expected slice. Do not start or stop the user's `runClient`. If Gradle says `build/` is locked, ask them to close the game and run preview themselves.

### Seeded terrain workflow: prove `was`, then converge to `expected`

Use this when the user provides `was` (the untouched world region) and `expected` (the same region after their desired edits). Both must be full JSON exports from the same dimension, seed, origin, and size. Keep both source files unchanged. `//rrexport <name>` (or `/rrexport <name>`) records `worldSeed` in new exports; if it is missing, ask for a fresh export before replaying.

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

   Treat the replay as a valid baseline only when `generated` is nearly identical to `was` in the terrain-only comparison: near-total block/shape agreement with no systematic offsets. The comparison against `expected` records how far the current algorithm is from the target. If substantial terrain differences remain against `was`, investigate seed, dimension, bounds, code version, and generation stages before tuning toward `expected`; otherwise the baseline does not establish that this algorithm generated `was`.

3. **Change the algorithm toward `expected`.** Preserve `was_generated.json` as the before snapshot. Edit terrain-generation math, not either export. Replay the same immutable `was.json` after each meaningful code change:

   ```powershell
   .\gradlew.bat runPreview -Ppreview=world_region '-Pregion=<absolute-path-to-was.json>' -Pstage=generated_after_modify
   python .agents/skills/region-export-compare/scripts/compare_region.py exports/was_generated.json '<path-to-expected.json>' exports/was_generated_after_modify.json
   python .agents/skills/region-export-compare/scripts/compare_terrain.py exports/was_generated_after_modify.json '<path-to-expected.json>'
   python .agents/skills/region-export-compare/scripts/compare_terrain.py exports/was_generated_after_modify.json exports/was_generated.json
   ```

   Read the raw diff for every block difference and the terrain-only diff for geometry/material agreement. Repeat the edit → replay → compare loop until the remaining unmasked differences are very close to the user's target and their coordinates match the intended changes. Report both scores and the residual cells; do not declare success from a high percentage alone. `generated_after_modify` is overwritten on the next replay, so copy it outside `exports/` if an iteration needs to be retained.

`world_region` calls `RRChunkGenerator.fillFromNoise` on temporary in-memory chunks using the export's seed and bounds. It produces terrain and arcane plates, but does not run carvers, biome features (including ores and vegetation), or structures. `compare_terrain.py` excludes changed cells matching its printed post-terrain block-id patterns, reports exact block and solid/air-shape match, and lists residual world-coordinate differences. Add a project-specific glob with `--ignore-id 'runeruin:some_feature*'` only when that block is known to come from a later feature stage. Always run and report the raw `compare_region.py` diff too: masking a feature cell can hide terrain that the feature replaced. The terrain helper requires both JSONs to have matching seed, dimension, origin, and size.

Replay output is written under the active code checkout's `exports/` as `<input-stem>_<stage>.json`; an absolute `-Pregion` path may point to an export from another checkout. Quote the whole `-Pregion=...` PowerShell argument. Replay is limited to Y=0..511, 64 chunks, and the export's 500,000-block volume limit.

Existing **world** chunks keep the old piece. In-game `/rrexport` of a already-generated goblet will not show the new math until a fresh area is generated.

## Export format (`runeruin.region/1`)

Written by `region/RegionExport`. Commands: `/rrpos1` `/rrpos2` `/rrexport` `/rrclear`.

**`.txt` slice** (one axis size 1):

- Header: origin (inclusive min), size `X Y Z`, projection
- Legend: `token  blockstate  count` — `.` air, `,` cave_air, `W` water, letters from the block id (`G` = giant_goblet_piece)
- Grid: comment lines with a local index, then the tokens

| projection | when | right → | up the page | fixed |
|------------|------|---------|-------------|-------|
| `YZ` | size X = 1 | +Z | +Y | world X |
| `XY` | size Z = 1 | +X | +Y | world Z |
| `XZ` | size Y = 1 | +X | +Z down the page | world Y |

**`.json` volume**: `origin`, `size`, `worldSeed`, `palette`, `layers[y][z]` = string of X tokens. Local `(0,0,0)` = origin; +X east, +Z south, +Y up.

## Example

`exports/region_20260829_235719.txt` vs `exports/region_should_be.txt` (YZ, 1×11×77):

- Y 10: `+.- ... -.+` — 2-wide rim cap shifted 1 outward on both +Z and −Z
- Y 8: `+ ... +` — same 1-block outset at the bowl wall
- diagnosis: outer silhouette 1 too large at the top, not a hole and not a water-fill bug
- fix: the formula that decides the outer radius / rim at those local Y values

## Checklist

- [ ] Inputs resolved (paths, search game exports under `run/runeruin-exports/` and `%USERPROFILE%\.runeruin/` plus project `exports/`, or one file + words)
- [ ] Script ran; overlay read
- [ ] Terrain task: current generator replay agrees closely with `was` before edits
- [ ] Terrain task: each modified replay was compared with `expected` using raw and terrain-only diffs
- [ ] One geometric diagnosis written
- [ ] Generation math changed (not the dump)
- [ ] User asked to re-export a fresh area
