#!/usr/bin/env python3
"""Compare dimensions and alpha silhouette between vanilla and custom textures."""

from __future__ import annotations

import argparse
import sys
from collections import Counter
from pathlib import Path

from PIL import Image


def alpha_summary(image: Image.Image) -> tuple[Counter[int], list[int]]:
    alpha = [pixel[3] for pixel in image.get_flattened_data()]
    return Counter(alpha), alpha


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("reference", type=Path, help="vanilla or approved source texture")
    parser.add_argument("candidate", type=Path, help="custom recolored texture")
    parser.add_argument("--exact-alpha", action="store_true", help="compare exact alpha values instead of only the transparent/visible mask")
    args = parser.parse_args()

    with Image.open(args.reference) as source:
        reference = source.convert("RGBA")
    with Image.open(args.candidate) as source:
        candidate = source.convert("RGBA")

    print(f"Reference: {args.reference} ({reference.width}x{reference.height})")
    print(f"Candidate: {args.candidate} ({candidate.width}x{candidate.height})")
    reference_alpha, reference_values = alpha_summary(reference)
    candidate_alpha, candidate_values = alpha_summary(candidate)
    print(f"Reference alpha counts: {dict(sorted(reference_alpha.items()))}")
    print(f"Candidate alpha counts: {dict(sorted(candidate_alpha.items()))}")

    errors = []
    if reference.size != candidate.size:
        errors.append("texture dimensions differ")
    elif args.exact_alpha:
        mismatches = [index for index, (left, right) in enumerate(zip(reference_values, candidate_values)) if left != right]
        if mismatches:
            errors.append(f"exact alpha differs at {len(mismatches)} pixels")
    else:
        mismatches = [
            index
            for index, (left, right) in enumerate(zip(reference_values, candidate_values))
            if (left == 0) != (right == 0)
        ]
        if mismatches:
            errors.append(f"transparent/visible silhouette differs at {len(mismatches)} pixels")

    if errors:
        width = reference.width
        coords = [(index % width, index // width) for index in mismatches[:16]] if reference.size == candidate.size else []
        for error in errors:
            print(f"FAIL: {error}", file=sys.stderr)
        if coords:
            print(f"First mismatched coordinates: {coords}", file=sys.stderr)
        return 1

    mode = "exact alpha" if args.exact_alpha else "alpha silhouette"
    print(f"PASS: dimensions and {mode} match.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
