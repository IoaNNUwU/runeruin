---
name: minecraft-pixel-texture-generation
description: >-
  Create, edit, quantize, or validate Minecraft texture assets. Use whenever block, item, or entity
  textures are generated or recolored; default to 16x16 pixel art with no semitransparent pixels.
---

# Minecraft pixel texture generation

Use this skill whenever creating or modifying Minecraft texture PNGs in this project.

## Defaults

- Use pixel art with hard pixel edges. Default output size is 16x16 unless the model UV layout or user explicitly requires another size.
- Do not blur, anti-alias, smooth, or dither textures.
- Do not create semitransparent pixels unless the user explicitly asks. Alpha should be binary: pixels are either fully transparent (`A=0`) or fully opaque (`A=255`). For solid wood, bark, planks, and log ends, make every pixel fully opaque.
- Choose the smallest logical palette that keeps the texture readable. Use about 3 colors for a simple texture (base, shadow, highlight). For a texture with several visual components, use up to 8 colors total; share colors across components where possible. Example: bark (3), heartwood (3), and end-grain marks (2) can share one 8-color palette.
- Avoid near-duplicate colors that do not create a visible pixel-art distinction. Preserve a reference texture's palette when the user asks for visual consistency.

## Vanilla alternatives

When creating a custom alternative to a vanilla block or item (such as a log, planks, sign, door, or hanging sign), compare it with the equivalent from the project's pinned Minecraft version before editing:

- Inspect the vanilla blockstate, model geometry, parent model, UV mapping, and inventory model. Keep the vanilla shape, UV layout, and model behavior unless the user asks for a different design.
- Use the vanilla texture as the recoloring base when available. Change color values only in intended painted pixels; keep transparent pixels in the same positions and preserve the canvas dimensions. A 16x16 default does not override a larger vanilla texture layout: pass its exact dimensions to the conversion script.
- Compare the candidate texture's dimensions and alpha silhouette to vanilla. Do not resize the candidate to 16x16 for this comparison when vanilla uses another size. The default shape comparison checks transparent versus visible pixels; use exact-alpha comparison when the alpha values themselves must also match.
- If the vanilla source is unavailable, inspect an explicitly supplied reference or report that the shape comparison could not be completed. Do not substitute a different Minecraft version's asset.
- Use the available Minecraft model and texture analysis skill when model parents or UV regions are unclear.

## Convert and check

Use the bundled scripts rather than rewriting palette and alpha handling for each texture. They require Python 3 and Pillow.

`quantize_texture.py` resizes with nearest-neighbor to 16x16 by default, chooses a deterministic weighted palette with k-means when `--colors` is used, or accepts an explicit `--palette`, then maps pixels with no dithering. The default `--palette-match rgb` chooses the nearest color by RGB distance. Use `--palette-match luminance` when recoloring a vanilla texture to a new hue family: it preserves the source's light-to-shadow ordering while mapping across the target palette's brightness range. Pass the vanilla texture's exact dimensions with `--size` when it is not 16x16. Its default `--alpha-mode binary` preserves fully transparent pixels and promotes all visible pixels to fully opaque; use `--alpha-mode opaque` for solid textures. `--alpha-mode preserve` is an explicit opt-in for tasks that genuinely require alpha gradients.

```powershell
python .agents/skills/minecraft-pixel-texture-generation/scripts/quantize_texture.py input.png output.png --colors 3 --alpha-mode opaque
python .agents/skills/minecraft-pixel-texture-generation/scripts/quantize_texture.py input.png output.png --palette '#F7D171' '#E1AE3A' '#704B2D' --alpha-mode binary
python .agents/skills/minecraft-pixel-texture-generation/scripts/quantize_texture.py input.png output.png --palette '#684A2B' '#A47A35' '#D2A340' '#E1AE3A' '#F5CA62' '#F5E084' '#FAEDB4' '#FFF8E2' --palette-match luminance --size 32x32
```

`check_texture.py` reports dimensions, unique visible RGB colors, alpha values, and the most common colors. It fails by default if dimensions are not 16x16 or any pixel is semitransparent. Use `--max-colors` to enforce a palette limit and `--require-opaque` for fully solid textures.

```powershell
python .agents/skills/minecraft-pixel-texture-generation/scripts/check_texture.py output.png --max-colors 3 --require-opaque --histogram 3
python .agents/skills/minecraft-pixel-texture-generation/scripts/check_texture.py output.png --max-colors 8
```

`compare_texture_shape.py` compares a custom texture to its vanilla counterpart without changing either file. It fails on dimension or alpha-silhouette mismatches and reports the alpha histograms; `--exact-alpha` also requires every alpha value to match at each pixel.

```powershell
python .agents/skills/minecraft-pixel-texture-generation/scripts/compare_texture_shape.py vanilla.png custom.png
python .agents/skills/minecraft-pixel-texture-generation/scripts/compare_texture_shape.py vanilla.png custom.png --exact-alpha
```

Run the checker after conversion and the shape comparator for vanilla alternatives. Also ensure every texture identifier used by generated block and item models resolves to a PNG under the matching `textures/block/` or `textures/item/` directory. Inventory sprites need their own correctly named item texture when the model points to `textures/item/<item>.png`.
