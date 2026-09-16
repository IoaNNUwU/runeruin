#!/usr/bin/env python3
"""Compare a terrain-only replay with a full world export, masking later features."""

from __future__ import annotations

import argparse
import fnmatch
import json
import sys
from collections import Counter
from pathlib import Path

from compare_region import Region, block_id, is_air, load

FORMAT = "runeruin.region/1"
POST_TERRAIN_PATTERNS = (
    "minecraft:*_leaves",
    "minecraft:*_log",
    "minecraft:*_wood",
    "minecraft:*_mushroom_block",
    "minecraft:*_vines*",
    "minecraft:*_ore",
    "minecraft:gravel",
    "minecraft:dead_bush",
    "minecraft:fern",
    "minecraft:large_fern",
    "minecraft:leaf_litter",
    "minecraft:short_grass",
    "minecraft:tall_grass",
    "minecraft:vine",
)


def read_metadata(path: Path) -> tuple[dict, Region]:
    if path.suffix.lower() != ".json":
        raise ValueError(f"Terrain replay comparison needs a full .json volume: {path}")
    data = json.loads(path.read_text(encoding="utf-8-sig"))
    if data.get("format") != FORMAT:
        raise ValueError(f"Unsupported region format in {path}: {data.get('format')!r}")
    if "worldSeed" not in data:
        raise ValueError(f"Missing worldSeed in {path}; re-export the region with the updated mod")
    return data, load(path)


def ignored(state: str | None, patterns: tuple[str, ...]) -> bool:
    if state is None:
        return False
    identifier = block_id(state)
    return any(fnmatch.fnmatchcase(identifier, pattern) for pattern in patterns)


def compare(
    generated_meta: dict,
    generated: Region,
    expected_meta: dict,
    expected: Region,
    patterns: tuple[str, ...],
) -> int:
    if generated_meta["worldSeed"] != expected_meta["worldSeed"]:
        raise ValueError(
            f"Seed mismatch: generated={generated_meta['worldSeed']}, expected={expected_meta['worldSeed']}"
        )
    if generated.dimension != expected.dimension:
        raise ValueError(f"Dimension mismatch: generated={generated.dimension!r}, expected={expected.dimension!r}")
    if generated.origin != expected.origin or generated.size != expected.size:
        raise ValueError(
            f"Bounds mismatch: generated origin/size={generated.origin}/{generated.size}, "
            f"expected={expected.origin}/{expected.size}"
        )

    all_positions = set(generated.cells) | set(expected.cells)
    ignored_pairs: Counter[tuple[str, str]] = Counter()
    extra: list[tuple[tuple[int, int, int], str]] = []
    missing: list[tuple[tuple[int, int, int], str]] = []
    mismatch: list[tuple[tuple[int, int, int], str, str]] = []
    exact = 0
    same_occupancy = 0
    excluded = 0

    for pos in all_positions:
        actual = generated.cells.get(pos)
        target = expected.cells.get(pos)
        if actual == target or (is_air(actual) and is_air(target)):
            exact += 1
            same_occupancy += 1
            continue
        if ignored(actual, patterns) or ignored(target, patterns):
            excluded += 1
            ignored_pairs[(block_id(actual) if actual else "air", block_id(target) if target else "air")] += 1
            continue
        actual_air, target_air = is_air(actual), is_air(target)
        if actual_air == target_air:
            same_occupancy += 1
        if actual_air:
            missing.append((pos, target or "?"))
        elif target_air:
            extra.append((pos, actual or "?"))
        else:
            mismatch.append((pos, actual or "?", target or "?"))

    eligible = len(all_positions) - excluded
    exact_rate = exact / eligible if eligible else 1.0
    shape_rate = same_occupancy / eligible if eligible else 1.0
    print("=== Terrain-only comparison ===")
    print(f"generated: {generated.path}")
    print(f"expected:  {expected.path}")
    print(f"dimension: {generated.dimension or '?'}")
    print(f"seed:      {generated_meta['worldSeed']}")
    print(f"origin:    {generated.origin}; size: {generated.size[0]} x {generated.size[1]} x {generated.size[2]}")
    print(f"cells: {len(all_positions)}; feature-affected cells excluded: {excluded}; compared: {eligible}")
    print(f"exact block match: {exact}/{eligible} ({exact_rate:.4%})")
    print(f"same solid/air shape: {same_occupancy}/{eligible} ({shape_rate:.4%})")
    print(f"terrain differences: extra={len(extra)}, missing={len(missing)}, block mismatch={len(mismatch)}")

    if ignored_pairs:
        print("excluded feature changes (block ids):")
        for (actual, target), count in ignored_pairs.most_common():
            print(f"  {actual} -> {target}: {count}")
    for label, items in (("extra", extra), ("missing", missing)):
        for pos, state in items[:20]:
            print(f"  {label} world {pos[0]} {pos[1]} {pos[2]}: {state}")
    for pos, actual, target in mismatch[:20]:
        print(f"  mismatch world {pos[0]} {pos[1]} {pos[2]}: {actual} -> {target}")
    listed = min(20, len(extra)) + min(20, len(missing)) + min(20, len(mismatch))
    remaining = len(extra) + len(missing) + len(mismatch) - listed
    if remaining > 0:
        print(f"  ... {remaining} more terrain differences")
    print("ignored block-id patterns:")
    for pattern in patterns:
        print(f"  {pattern}")
    return 1 if extra or missing or mismatch else 0


def main(argv: list[str]) -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    parser = argparse.ArgumentParser(description="Compare terrain replay with an exported world, excluding later features")
    parser.add_argument("generated", type=Path, help="Terrain-only world_region replay JSON")
    parser.add_argument("expected", type=Path, help="Full-volume /rrexport JSON from the live world")
    parser.add_argument(
        "--ignore-id",
        action="append",
        default=[],
        metavar="GLOB",
        help="Additional block-id glob to ignore, e.g. runeruin:glowing_moss_carpet",
    )
    args = parser.parse_args(argv)
    try:
        generated_meta, generated = read_metadata(args.generated)
        expected_meta, expected = read_metadata(args.expected)
        patterns = POST_TERRAIN_PATTERNS + tuple(args.ignore_id)
        return compare(generated_meta, generated, expected_meta, expected, patterns)
    except (OSError, ValueError, KeyError, TypeError, json.JSONDecodeError) as error:
        print(f"Error: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
