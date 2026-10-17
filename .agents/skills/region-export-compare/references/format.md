# Export format (`runeruin.region/1`)

Written by `region/RegionExport`. Commands: `/rrpos1` `/rrpos2` `/rrexport` `/rrgenerate` `/rrclear`.

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

## Example of a diagnosis

`exports/region_20260829_235719.txt` vs `exports/region_should_be.txt` (YZ, 1×11×77):

- Y 10: `+.- ... -.+` — 2-wide rim cap shifted 1 outward on both +Z and −Z
- Y 8: `+ ... +` — same 1-block outset at the bowl wall
- diagnosis: outer silhouette 1 too large at the top, not a hole and not a water-fill bug
- fix: the formula that decides the outer radius / rim at those local Y values
