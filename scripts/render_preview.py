#!/usr/bin/env python3
"""Render exports/preview_*.json (runeruin.region/1) to a PNG: front, side and top silhouettes.

    python scripts/render_preview.py out.png exports/preview_a.json [exports/preview_b.json ...] [--scale 10]

Shows the nearest non-air block along each axis, tinted by depth, colored by the block texture's average color.
Air and stone-like support blocks (stone, deepslate) are skipped so the subject stands out. The JSON and text
slices stay the exact source of truth; this is only for judging shape (bends, tilt, gaps) at a glance.
"""
import colorsys
import hashlib
import json
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEXTURE_DIRS = [
    ROOT / "src/main/resources/assets/runeruin/textures/block",
    ROOT / ".vanilla-textures/assets/minecraft/textures/block",
]
SKIPPED = {"stone", "deepslate", "air", "cave_air", "void_air"}
_cache = {}


def block_color(state):
    name = state.split("[")[0].split(":")[-1]
    if name not in _cache:
        _cache[name] = texture_color(name) or hash_color(name)
    return _cache[name]


def texture_color(name):
    for directory in TEXTURE_DIRS:
        for candidate in (name, name + "_top", name + "_side", name + "_0"):
            path = directory / (candidate + ".png")
            if path.exists():
                data = Image.open(path).convert("RGBA").tobytes()
                pixels = [data[i:i + 4] for i in range(0, len(data), 4) if data[i + 3] > 0]
                if pixels:
                    return tuple(sum(p[i] for p in pixels) // len(pixels) for i in range(3))
    return None


def hash_color(name):
    hue = int(hashlib.md5(name.encode()).hexdigest()[:4], 16) / 65535
    return tuple(int(c * 255) for c in colorsys.hsv_to_rgb(hue, 0.6, 0.9))


def load(path):
    data = json.load(open(path, encoding="utf-8"))
    palette = {k: block_color(v) for k, v in data["palette"].items() if v.split("[")[0].split(":")[-1] not in SKIPPED}
    return data["layers"], palette  # layers[y][z][x]


def render(width, height, cell, pick):
    image = Image.new("RGB", (width * cell, height * cell), (30, 30, 35))
    for row in range(height):
        for col in range(width):
            found = pick(row, col)
            if found is None:
                continue
            (r, g, b), depth = found
            shade = max(0.45, 1 - 0.035 * depth)
            fill, edge = tuple(int(c * shade) for c in (r, g, b)), tuple(int(c * shade * 0.8) for c in (r, g, b))
            for dy in range(cell):
                for dx in range(cell):
                    image.putpixel((col * cell + dx, row * cell + dy), edge if dx == 0 or dy == 0 else fill)
    return image


def views(layers, palette, cell):
    height, depth, width = len(layers), len(layers[0]), len(layers[0][0])

    def nearest(cells):
        for k, c in enumerate(cells):
            if c in palette:
                return palette[c], k
        return None

    front = render(width, height, cell, lambda row, x: nearest(layers[height - 1 - row][z][x] for z in range(depth)))
    side = render(depth, height, cell, lambda row, z: nearest(layers[height - 1 - row][z][x] for x in range(width)))
    top = render(width, depth, cell, lambda z, x: nearest(layers[y][z][x] for y in range(height - 1, -1, -1)))
    return [front, side, top]


def main(argv):
    scale = 10
    if "--scale" in argv:
        i = argv.index("--scale")
        scale = int(argv[i + 1])
        del argv[i:i + 2]
    if len(argv) < 2:
        sys.exit(__doc__)
    images = [view for path in argv[1:] for view in views(*load(path), scale)]
    gap = 10
    sheet = Image.new("RGB", (sum(i.width for i in images) + gap * len(images), max(i.height for i in images)), (20, 20, 25))
    x = 0
    for image in images:
        sheet.paste(image, (x, sheet.height - image.height))
        x += image.width + gap
    sheet.save(argv[0])


if __name__ == "__main__":
    main(sys.argv[1:])
