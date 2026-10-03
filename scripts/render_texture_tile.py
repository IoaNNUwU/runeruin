#!/usr/bin/env python3
"""Tile textures in a grid to judge seams and repetition, the way a wall of the block looks in game.

    python scripts/render_texture_tile.py out.png runeruin:block/moss_light [minecraft:block/stone path/to.png ...]
        [--grid 3] [--scale 8]

A texture is a file path or a texture id (namespace:path, e.g. runeruin:block/moss_light), looked up in
src/main/resources, src/generated/resources and .vanilla-textures. Animated textures show frame 0.
Each texture gets its own labeled panel, side by side.
"""
import argparse
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
ASSET_ROOTS = [
    ROOT / "src/main/resources/assets",
    ROOT / "src/generated/resources/assets",
    ROOT / ".vanilla-textures/assets",
]
BACKGROUND = (48, 48, 48)
LABEL_HEIGHT = 14
GAP = 8


def resolve(texture):
    path = Path(texture)
    if path.is_file():
        return path
    namespace, _, name = texture.rpartition(":")
    for root in ASSET_ROOTS:
        candidate = root / (namespace or "minecraft") / "textures" / (name.removesuffix(".png") + ".png")
        if candidate.is_file():
            return candidate
    raise SystemExit(f"Cannot find texture {texture} under {', '.join(str(root) for root in ASSET_ROOTS)}")


def first_frame(path):
    image = Image.open(path).convert("RGBA")
    animated = path.with_name(path.name + ".mcmeta").is_file()
    if animated and image.height > image.width and image.height % image.width == 0:
        return image.crop((0, 0, image.width, image.width))
    return image


def panel(path, grid, scale):
    texture = first_frame(path)
    tiled = Image.new("RGBA", (texture.width * grid, texture.height * grid), BACKGROUND + (255,))
    for y in range(grid):
        for x in range(grid):
            tiled.alpha_composite(texture, (x * texture.width, y * texture.height))
    return tiled.resize((tiled.width * scale, tiled.height * scale), Image.NEAREST)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("output", type=Path)
    parser.add_argument("textures", nargs="+")
    parser.add_argument("--grid", type=int, default=3, help="tiles per side, default 3")
    parser.add_argument("--scale", type=int, default=8, help="screen pixels per texture pixel, default 8")
    args = parser.parse_args()

    paths = [resolve(texture) for texture in args.textures]
    panels = [panel(path, args.grid, args.scale) for path in paths]
    width = sum(image.width for image in panels) + GAP * (len(panels) + 1)
    height = max(image.height for image in panels) + LABEL_HEIGHT + GAP * 2
    sheet = Image.new("RGB", (width, height), BACKGROUND)
    draw = ImageDraw.Draw(sheet)
    x = GAP
    for path, image, name in zip(paths, panels, args.textures):
        draw.text((x, GAP // 2), Path(name).stem if Path(name).is_file() else name, fill=(230, 230, 230))
        sheet.paste(image, (x, GAP + LABEL_HEIGHT), image)
        x += image.width + GAP
    args.output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(args.output)
    for path in paths:
        print(f"{path.relative_to(ROOT) if path.is_relative_to(ROOT) else path}")
    print(f"wrote {args.output} ({sheet.width}x{sheet.height})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
