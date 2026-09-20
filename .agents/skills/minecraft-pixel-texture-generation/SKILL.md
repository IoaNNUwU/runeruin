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

## Convert and check

Use the bundled scripts rather than rewriting palette and alpha handling for each texture. They require Python 3 and Pillow.

`quantize_texture.py` resizes with nearest-neighbor to 16x16 by default, chooses a deterministic weighted palette with k-means when `--colors` is used, or accepts an explicit `--palette`, then maps each visible pixel to its nearest palette color with no dithering. Its default `--alpha-mode binary` preserves fully transparent pixels and promotes all visible pixels to fully opaque; use `--alpha-mode opaque` for solid textures. `--alpha-mode preserve` is an explicit opt-in for tasks that genuinely require alpha gradients.

```powershell
python .agents/skills/minecraft-pixel-texture-generation/scripts/quantize_texture.py input.png output.png --colors 3 --alpha-mode opaque
python .agents/skills/minecraft-pixel-texture-generation/scripts/quantize_texture.py input.png output.png --palette '#F7D171' '#E1AE3A' '#704B2D' --alpha-mode binary
```

`check_texture.py` reports dimensions, unique visible RGB colors, alpha values, and the most common colors. It fails by default if dimensions are not 16x16 or any pixel is semitransparent. Use `--max-colors` to enforce a palette limit and `--require-opaque` for fully solid textures.

```powershell
python .agents/skills/minecraft-pixel-texture-generation/scripts/check_texture.py output.png --max-colors 3 --require-opaque --histogram 3
python .agents/skills/minecraft-pixel-texture-generation/scripts/check_texture.py output.png --max-colors 8
```

Run the checker after conversion. Also ensure every texture identifier used by generated block and item models resolves to a PNG under the matching `textures/block/` or `textures/item/` directory. Inventory sprites need their own correctly named item texture when the model points to `textures/item/<item>.png`.
