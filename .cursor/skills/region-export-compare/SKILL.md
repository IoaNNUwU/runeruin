---
name: region-export-compare
description: >-
  Compares Minecraft region dumps against a corrected file or a verbal fix, then updates
  structure or feature geometry. Use when the user mentions /rrexport, rrexport,
  region_should_be, exports folder, comparing generated vs intended shape.
---

# Region export compare

Use this whenever the user is shaping a structure/feature from an in-game slice.
Do not eyeball large grids. Run the script first, then change generation math.

## Resolve inputs

Pick one, in this order:

1. **Two paths** — first is the `/rrexport` dump (actual), second is the corrected slice (expected). Typical names: `exports/region_YYYYMMDD_HHMMSS.txt` and `exports/region_should_be.txt`.
2. **One dump + a verbal fix** — profile the dump, then apply the description (e.g. "rim 1 thinner", "water one block lower").
3. **Nothing named** — look in `exports/`. Newest `region_YYYYMMDD_HHMMSS.txt` is actual (prefer `.txt` over the twin `.json`). A file whose name contains `should`, `expected`, `want`, or `correct` is expected.

`.txt` exists only when the selection is 1 block thick on some axis (a readable slice). Thick volumes are `.json` only.

## Run the script

From the repo root (Python stdlib only):

```powershell
python .cursor/skills/region-export-compare/scripts/compare_region.py
python .cursor/skills/region-export-compare/scripts/compare_region.py exports/actual.txt exports/region_should_be.txt
python .cursor/skills/region-export-compare/scripts/compare_region.py --profile --actual exports/actual.txt
```

Read the whole stdout. Overlay legend:

| mark | meaning |
|------|---------|
| `.` | same (air equals air) |
| `+` | extra in actual |
| `-` | missing from actual |
| `!` | both occupied, different block |

Compare by **block id**, not palette letter (tokens can differ between files). Align in **world** coordinates if origins differ.

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

**`.json` volume**: `origin`, `size`, `palette`, `layers[y][z]` = string of X tokens. Local `(0,0,0)` = origin; +X east, +Z south, +Y up.

## Example

`exports/region_20260829_235719.txt` vs `exports/region_should_be.txt` (YZ, 1×11×77):

- Y 10: `+.- ... -.+` — 2-wide rim cap shifted 1 outward on both +Z and −Z
- Y 8: `+ ... +` — same 1-block outset at the bowl wall
- diagnosis: outer silhouette 1 too large at the top, not a hole and not a water-fill bug
- fix: the formula that decides the outer radius / rim at those local Y values

## Checklist

- [ ] Inputs resolved (paths, auto `exports/`, or one file + words)
- [ ] Script ran; overlay read
- [ ] One geometric diagnosis written
- [ ] Generation math changed (not the dump)
- [ ] User asked to re-export a fresh area
