#!/usr/bin/env python3
"""Report and validate size, visible palette, and alpha of a Minecraft texture."""

from __future__ import annotations

import argparse
import sys
from collections import Counter
from pathlib import Path

from PIL import Image


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
        print(f"#{color[0]:02X}{color[1]:02X}{color[2]:02X}: {count} px")

    if errors:
        for error in errors:
            print(f"FAIL: {error}", file=sys.stderr)
        return 1
    print("PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
