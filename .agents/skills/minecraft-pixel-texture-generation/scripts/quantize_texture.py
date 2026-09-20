#!/usr/bin/env python3
"""Resize and map a Minecraft texture to a small, deterministic RGB palette."""

from __future__ import annotations

import argparse
import random
from collections import Counter
from pathlib import Path
from typing import TypeAlias

from PIL import Image

RGB: TypeAlias = tuple[int, int, int]


def parse_size(value: str) -> tuple[int, int]:
    try:
        width, height = (int(part) for part in value.lower().split("x", 1))
    except (ValueError, TypeError) as exc:
        raise argparse.ArgumentTypeError("size must be written as WIDTHxHEIGHT, e.g. 16x16") from exc
    if width < 1 or height < 1:
        raise argparse.ArgumentTypeError("texture dimensions must be positive")
    return width, height


def parse_color(value: str) -> RGB:
    text = value.removeprefix("#")
    if len(text) != 6:
        raise argparse.ArgumentTypeError("palette colors must be #RRGGBB")
    try:
        return tuple(int(text[index : index + 2], 16) for index in (0, 2, 4))  # type: ignore[return-value]
    except ValueError as exc:
        raise argparse.ArgumentTypeError("palette colors must be #RRGGBB") from exc


def distance_sq(first: RGB | tuple[float, float, float], second: RGB | tuple[float, float, float]) -> float:
    return sum((a - b) ** 2 for a, b in zip(first, second))


def make_palette(pixels: list[RGB], color_count: int, seed: int) -> list[RGB]:
    weights = Counter(pixels)
    colors = sorted(weights)
    if color_count > len(colors):
        raise ValueError(f"requested {color_count} colors, but the image has only {len(colors)} visible colors")

    rng = random.Random(seed)
    centers: list[tuple[float, float, float]] = [tuple(map(float, rng.choices(colors, weights=[weights[c] for c in colors], k=1)[0]))]
    while len(centers) < color_count:
        distances = [min(distance_sq(color, center) for center in centers) for color in colors]
        selection_weights = [weights[color] * distance for color, distance in zip(colors, distances)]
        if sum(selection_weights) == 0:
            candidate = next(color for color in colors if tuple(map(float, color)) not in centers)
        else:
            candidate = rng.choices(colors, weights=selection_weights, k=1)[0]
        centers.append(tuple(map(float, candidate)))

    for _ in range(100):
        groups: list[list[RGB]] = [[] for _ in centers]
        for color in colors:
            nearest = min(range(color_count), key=lambda index: distance_sq(color, centers[index]))
            groups[nearest].append(color)

        updated = list(centers)
        reserved: set[RGB] = set()
        for index, group in enumerate(groups):
            if not group:
                candidate = max(
                    (color for color in colors if color not in reserved),
                    key=lambda color: weights[color] * min(distance_sq(color, center) for center in centers),
                    default=colors[0],
                )
                updated[index] = tuple(map(float, candidate))
                reserved.add(candidate)
                continue
            total = sum(weights[color] for color in group)
            updated[index] = tuple(
                sum(color[channel] * weights[color] for color in group) / total
                for channel in range(3)
            )

        converged = all(distance_sq(old, new) < 0.01 for old, new in zip(centers, updated))
        centers = updated
        if converged:
            break

    palette: list[RGB] = []
    for center in centers:
        color = tuple(round(channel) for channel in center)
        if color not in palette:
            palette.append(color)  # type: ignore[arg-type]
    if len(palette) != color_count:
        raise ValueError("palette clustering collapsed colors; retry with a different --seed")
    return palette


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    palette_group = parser.add_mutually_exclusive_group(required=True)
    palette_group.add_argument("--colors", type=int, help="number of colors to derive from the input")
    palette_group.add_argument("--palette", nargs="+", type=parse_color, help="explicit nearest-match palette in #RRGGBB form")
    parser.add_argument("--size", type=parse_size, default=(16, 16), help="output dimensions, default: 16x16")
    parser.add_argument("--alpha-mode", choices=("binary", "opaque", "preserve"), default="binary")
    parser.add_argument("--seed", type=int, default=42, help="deterministic palette seed")
    args = parser.parse_args()

    if args.colors is not None and args.colors < 1:
        parser.error("--colors must be at least 1")
    if args.palette is not None and not 1 <= len(set(args.palette)) <= 256:
        parser.error("--palette must contain between 1 and 256 distinct colors")

    with Image.open(args.input) as source:
        image = source.convert("RGBA")
    if image.size != args.size:
        image = image.resize(args.size, Image.Resampling.NEAREST)

    source_pixels = list(image.get_flattened_data())
    visible_colors = [pixel[:3] for pixel in source_pixels if args.alpha_mode == "opaque" or pixel[3] > 0]
    if not visible_colors:
        parser.error("input has no visible pixels to quantize")
    try:
        palette = args.palette or make_palette(visible_colors, args.colors, args.seed)
    except ValueError as exc:
        parser.error(str(exc))

    output_pixels = []
    for red, green, blue, alpha in source_pixels:
        if args.alpha_mode == "binary" and alpha == 0:
            output_pixels.append((0, 0, 0, 0))
            continue
        source_color = (red, green, blue)
        nearest = min(palette, key=lambda color: distance_sq(source_color, color))
        output_alpha = alpha if args.alpha_mode == "preserve" else 255
        output_pixels.append((*nearest, output_alpha))

    output = Image.new("RGBA", args.size)
    output.putdata(output_pixels)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    output.save(args.output, format="PNG", optimize=True)

    histogram = Counter(pixel[:3] for pixel in output_pixels if pixel[3] > 0)
    print(f"Saved {args.output} ({args.size[0]}x{args.size[1]}), {len(histogram)} visible colors, alpha mode {args.alpha_mode}.")
    for color, count in histogram.most_common():
        print(f"#{color[0]:02X}{color[1]:02X}{color[2]:02X}: {count} px")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
