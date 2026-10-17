---
name: minecraft-pixel-texture-generation
description: >-
  Create, edit, quantize, or validate Minecraft texture assets. Use whenever block, item, or entity
  textures are generated or recolored; default to 16x16 pixel art with no semitransparent pixels.
---

# Minecraft pixel texture generation

A texture you make is a draft that goes to a person (`hand-off` skill). Once the person has redrawn a texture, do not regenerate or recolor it unless they ask for that texture: fit the model to it instead.

## Defaults

- Pixel art with hard pixel edges, 16x16 unless the model UV layout or the user requires another size.
- No blur, anti-aliasing, smoothing or dithering.
- No semitransparent pixels unless the user explicitly asks: every pixel is fully transparent (`A=0`) or fully opaque (`A=255`). Solid wood, bark, planks and log ends are fully opaque.

## Where the colors come from

When a part of the texture means the same as a block, draw it after that block: take its colors and its structure, such as the pattern, the direction of the grain and the size of the spots. Look for the block in this mod first (`src/main/resources/assets/runeruin/textures/block/`), then in vanilla (`.mc-sources/textures/assets/minecraft/textures/block/`). The wool of a new ram is drawn after the wool block that the ram drops, if the mod has one; without such a block, after vanilla `white_wool`. The moss on the back of the Mossy Prophet is drawn after the mod's `deep_moss` or `moss_light`, not after vanilla `moss_block`.

Keep the palette of the reference and bring it down to the limit of the part below by dropping shades the eye cannot tell apart: vanilla `white_wool` has 40 colors that look like 3. Name the reference block of each part in the report.

## Colors of each part

Colors are counted per part, not per texture. A part is everything in the texture that has one meaning: the bark of a log, the berries of a bush, the eyes of a mob. One berry is an element of the part "berries"; the size of the largest element sets the limit of the part. The limits follow vanilla blocks.

- A large part, whose largest element has more than 6 pixels, takes up to 4 colors: base, shadow, highlight and one more shade, such as a deep shadow in the cracks or a second highlight. Examples: the face of a smooth stone block, a leaf, a mushroom stem or cap, a stem, the body or the fur of a mob; vanilla `stone`, `oak_leaves` and `mushroom_stem` have 4.
- A large part with a rough or busy surface takes 5 or 6: moss, cobblestone, bark, gravel, sand, the side of sandstone; vanilla `moss_block`, `cobblestone`, `oak_log` and `sand` have 6, `sandstone_top` 5. Take more than 4 only when the reference block of the part has as many.
- A small detail of 4 to 6 pixels takes up to 3 colors: a berry, the flower head of a small plant, the eye of a mob, a knot in wood, a rune sign.
- A tiny detail of up to 3 pixels takes up to 2 colors: a spot on a mushroom cap, a glint on a crystal, a seed, a crack, a pupil.
- Elements with the same meaning belong to one part and take their colors from one set, wherever they are in the texture: every berry on a bush, every spot on a cap, both eyes and all legs of a mob, all boards of planks, the rings of a log end, every tuft of moss. A berry in one corner is never redder than a berry in the other; a smaller berry that shows only two shades takes them from the same set.
- The whole texture has as many colors as its parts together, with no fixed total. Parts share colors where they can: the darkest bark is the shadow of the heartwood, the highlight of a leaf is the base of its stem. Example: a log end with rough bark (6), heartwood (4) and rings (2) has at most 12 colors, fewer where they share; vanilla `oak_log_top` has 9.
- A color that the eye cannot tell from its neighbor in the part is not a new shade: drop it.

The checker counts the colors of each part from a parts map: a PNG of the texture's size where every visible pixel of the texture is painted with one flat color of its part, all berries with one color, the bark with another. Draw it in the same script that draws the texture and keep it in `exports/`, next to the pictures; it is a working file of the check and is never committed. A rough part is named to the checker by its color on the map with `--rough`.

## Vanilla alternatives

A custom alternative to a vanilla block or item (a log, planks, a sign, a door) starts from the vanilla one of the pinned version in `.mc-sources/textures`:

- Keep its blockstate, model geometry, parent, UV layout and inventory model unless the user asks for another design (`minecraft-model-texture-analysis` skill when parents or UVs are unclear).
- Recolor its texture: change only the painted pixels, keep the transparent ones where they are and keep its exact size, also when it is not 16x16. Compare the result with `compare_texture_shape.py`. Without the vanilla source, use a reference the user supplies or report that the comparison could not be done; never use another version's asset.

## Planes seen from both sides

A flat model element (a cross plant, a spike plane) has two faces, and each shows the texture unmirrored to whoever looks at it. From behind, a pixel therefore sits on the other half of the plane than from the front: a spike drawn on the left is on the viewer's left from both sides, which are two different places in the world.

When the pixels have to stay where they are (spikes, thorns, anything that must meet the rest of the model), mirror the UV of the back face: `[16, 0, 0, 16]` instead of `[0, 0, 16, 16]`, as the model file `assets/runeruin/models/block/deep_roots.json` does. Otherwise draw the texture left-right symmetric. Vanilla `block/cross` mirrors nothing, which is enough for a plant that only has to look right from each side on its own.

The two isometric views of `scripts/render_model.py` look at the model from opposite sides: compare a marked pixel in both.

## Convert and check

Use the bundled scripts in `.agents/skills/minecraft-pixel-texture-generation/scripts/` (Python 3, Pillow; options in `--help`) rather than rewriting palette and alpha handling:

- `quantize_texture.py` resizes with nearest-neighbor and maps to a palette without dithering. Pass the colors chosen for the parts with `--palette`: `--colors` picks one k-means palette for the whole texture and can give two elements of one part different shades. `--palette-match luminance` recolors a vanilla texture into another hue family and keeps its light-to-shadow order; `--size` keeps a vanilla size other than 16x16. `--alpha-mode binary` (default) keeps transparent pixels and makes the rest opaque, `opaque` fills everything; `preserve` only for alpha gradients the task requires.
- `check_texture.py`, run with the parts map on every texture you make, fails on a size other than 16x16 or a semitransparent pixel. With `--parts` it fails on a part with more colors than its largest element allows or a visible pixel without a part, and lists every part with its colors. `--require-opaque` for solid textures, `--max-colors` to keep the palette size of a recolored vanilla texture.
- `compare_texture_shape.py vanilla.png custom.png` fails on a size or alpha-silhouette mismatch; `--exact-alpha` also compares every alpha value.

```powershell
python .agents/skills/minecraft-pixel-texture-generation/scripts/quantize_texture.py input.png output.png --palette '#F7D171' '#E1AE3A' '#704B2D' --alpha-mode binary
python .agents/skills/minecraft-pixel-texture-generation/scripts/check_texture.py output.png --parts exports/output_parts.png --rough '#00C800'
```

Judge seams and repetition on a tiled 3×3 render, the way a wall of the block looks, never on the single 16×16 file. Pass texture ids or paths; several textures go side by side, e.g. the new one next to its vanilla counterpart:

```powershell
python scripts/render_texture_tile.py exports/tile.png runeruin:block/moss_light minecraft:block/moss_block --grid 3 --scale 8
```

Under the picture this script, `render_model.py` and `render_entity_model.py` draw a row per texture with its colors from dark to light, so whoever looks at the picture sees the palette without running the checker.

On a model, check the texture with `scripts/render_model.py` (the `minecraft-model-texture-analysis` skill). After `runData`, `python scripts/lint_assets.py` checks that every texture a model uses exists and that block and item textures are 16×16 (or animated 16-wide strips); a deliberate other size goes into its `OTHER_SIZES` with the reason.
