#!/usr/bin/env python3
"""Report and validate size, visible palette, alpha and the colors of each part of a Minecraft texture."""

from __future__ import annotations

import argparse
import sys
from collections import Counter
from pathlib import Path

from PIL import Image


def hex_color(color: tuple[int, ...]) -> str:
    return f"#{color[0]:02X}{color[1]:02X}{color[2]:02X}"


def color_limit(size: int, rough: bool) -> int:
    """Colors allowed for a part whose largest element has `size` pixels."""
    return (6 if rough else 4) if size > 6 else 3 if size > 3 else 2


def largest_element(pixels: set[tuple[int, int]]) -> int:
    """Pixel count of the largest 8-connected area."""
    left, largest = set(pixels), 0
    while left:
        stack, size = [left.pop()], 0
        while stack:
            x, y = stack.pop()
            size += 1
            for neighbor in ((x + dx, y + dy) for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                if neighbor in left:
                    left.remove(neighbor)
                    stack.append(neighbor)
        largest = max(largest, size)
    return largest


def check_parts(image: Image.Image, parts_path: Path, rough: set[str], errors: list[str]) -> None:
    """Each color of the parts map is one part; its elements share one set of colors."""
    with Image.open(parts_path) as source:
        parts = source.convert("RGBA")
    if parts.size != image.size:
        errors.append(f"parts map is {parts.width}x{parts.height}, texture is {image.width}x{image.height}")
        return
    by_part: dict[tuple[int, ...], set[tuple[int, int]]] = {}
    outside = []
    for y in range(image.height):
        for x in range(image.width):
            if image.getpixel((x, y))[3] == 0:
                continue
            part = parts.getpixel((x, y))
            if part[3] == 0:
                outside.append((x, y))
            else:
                by_part.setdefault(part[:3], set()).add((x, y))
    if outside:
        errors.append(f"{len(outside)} visible pixels have no part on the parts map, first at {outside[:4]}")

    budget = 0
    print(f"Parts: {len(by_part)}")
    for part, pixels in sorted(by_part.items(), key=lambda item: -len(item[1])):
        colors = sorted({image.getpixel(pixel)[:3] for pixel in pixels})
        largest = largest_element(pixels)
        name = hex_color(part) + (" (rough)" if hex_color(part) in rough else "")
        limit = color_limit(largest, hex_color(part) in rough)
        budget += limit
        print(f"  part {name}: {len(pixels)} px, largest element {largest} px, "
              f"{len(colors)}/{limit} colors {' '.join(hex_color(color) for color in colors)}")
        if len(colors) > limit:
            errors.append(f"part {hex_color(part)} has {len(colors)} colors, its largest element of "
                          f"{largest} px allows {limit}")
    for part in sorted(rough - {hex_color(part) for part in by_part}):
        errors.append(f"--rough {part} is not a color of the parts map")
    print(f"Parts allow up to {budget} colors in total")


def parse_size(value: str) -> tuple[int, int]:
    try:
        width, height = (int(part) for part in value.lower().split("x", 1))
    except (ValueError, TypeError) as exc:
        raise argparse.ArgumentTypeError("size must be written as WIDTHxHEIGHT, e.g. 16x16") from exc
    if width < 1 or height < 1:
        raise argparse.ArgumentTypeError("texture dimensions must be positive")
    return width, height


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("image", type=Path)
    parser.add_argument("--size", type=parse_size, default=(16, 16), help="expected dimensions, default: 16x16")
    parser.add_argument("--parts", type=Path, help="parts map of the same size: one flat color per part")
    parser.add_argument("--rough", nargs="+", default=[], metavar="RRGGBB",
                        help="parts map colors of rough parts, which may take 6 colors instead of 4")
    parser.add_argument("--max-colors", type=int, help="maximum distinct visible RGB colors")
    parser.add_argument("--min-colors", type=int, help="minimum distinct visible RGB colors")
    parser.add_argument("--require-opaque", action="store_true", help="fail if any pixel is not fully opaque")
    parser.add_argument("--allow-semitransparency", action="store_true", help="allow alpha values from 1 to 254")
    parser.add_argument("--histogram", type=int, default=8, help="number of most common colors to list, default: 8")
    args = parser.parse_args()
    if args.histogram < 0:
        parser.error("--histogram cannot be negative")

    with Image.open(args.image) as source:
        image = source.convert("RGBA")
    pixels = list(image.get_flattened_data())
    alpha_counts = Counter(pixel[3] for pixel in pixels)
    colors = Counter(pixel[:3] for pixel in pixels if pixel[3] > 0)
    partial_count = sum(count for alpha, count in alpha_counts.items() if 0 < alpha < 255)
    errors = []

    print(f"File: {args.image}")
    print(f"Size: {image.width}x{image.height} (expected {args.size[0]}x{args.size[1]})")
    print(f"Visible RGB colors: {len(colors)}")
    print(f"Alpha values: {dict(sorted(alpha_counts.items()))}")
    print(f"Semitransparent pixels: {partial_count}")

    if image.size != args.size:
        errors.append("unexpected image dimensions")
    if partial_count and not args.allow_semitransparency:
        errors.append("semitransparent pixels are not allowed by default")
    if args.require_opaque and any(alpha != 255 for alpha in alpha_counts):
        errors.append("texture is not fully opaque")
    if args.max_colors is not None and len(colors) > args.max_colors:
        errors.append(f"palette has {len(colors)} colors, maximum is {args.max_colors}")
    if args.min_colors is not None and len(colors) < args.min_colors:
        errors.append(f"palette has {len(colors)} colors, minimum is {args.min_colors}")

    for color, count in colors.most_common(args.histogram):
        print(f"{hex_color(color)}: {count} px")
    if args.parts is not None:
        check_parts(image, args.parts, {"#" + part.lstrip("#").upper() for part in args.rough}, errors)

    if errors:
        for error in errors:
            print(f"FAIL: {error}", file=sys.stderr)
        return 1
    print("PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
