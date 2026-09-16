#!/usr/bin/env python3
"""Compare /rrexport dumps (runeruin.region/1 .txt / .json) and print geometry-oriented diffs."""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable

AIR_STATES = frozenset({"minecraft:air", "minecraft:cave_air", "minecraft:void_air"})
AIR_TOKENS = frozenset(". ,'")
SHOULD_NAME = re.compile(r"(should|expected|want|correct|target|built)", re.I)
STAMP_NAME = re.compile(r"region_(\d{8}_\d{6})", re.I)
REPLAY_NAME = re.compile(r"_(?:generated|generated_after_modify)(?:_(?:xy|xz|yz|info))?$", re.I)
ORIGIN_RE = re.compile(r"# origin \(inclusive min\): (-?\d+) (-?\d+) (-?\d+)")
SIZE_RE = re.compile(r"# size: (\d+) x (\d+) x (\d+)")
PROJ_RE = re.compile(r"# projection: (YZ|XY|XZ)\b")
LEGEND_RE = re.compile(r"#\s+(\S)\s+(\S+)\s+(\d+)\s*$")
GRID_RE = re.compile(r"^#\s+(?:[A-Za-z↑↓]\s+)?(\d+)\s{2}(.*)$")

DIFF_LIMIT = 80
ROW_LIMIT = 40


@dataclass
class Region:
    path: Path
    dimension: str
    origin: tuple[int, int, int]
    size: tuple[int, int, int]
    projection: str
    palette: dict[str, str]
    # world (x, y, z) -> block state id
    cells: dict[tuple[int, int, int], str] = field(default_factory=dict)

    @property
    def size_x(self) -> int:
        return self.size[0]

    @property
    def size_y(self) -> int:
        return self.size[1]

    @property
    def size_z(self) -> int:
        return self.size[2]


def is_air(state: str | None) -> bool:
    return state is None or state in AIR_STATES or state.split("[", 1)[0] in AIR_STATES


def block_id(state: str) -> str:
    return state.split("[", 1)[0]


def load(path: Path) -> Region:
    if path.suffix.lower() == ".json":
        return load_json(path)
    return load_txt(path)


def load_json(path: Path) -> Region:
    data = json.loads(path.read_text(encoding="utf-8"))
    ox, oy, oz = data["origin"]
    sx, sy, sz = data["size"]
    palette = {str(k): str(v) for k, v in data.get("palette", {}).items()}
    cells: dict[tuple[int, int, int], str] = {}
    layers = data.get("layers", [])
    for y, layer in enumerate(layers):
        for z, row in enumerate(layer):
            for x, token in enumerate(row):
                cells[(ox + x, oy + y, oz + z)] = palette.get(token, token)
    return Region(
        path=path,
        dimension=str(data.get("dimension", "")),
        origin=(ox, oy, oz),
        size=(sx, sy, sz),
        projection=str(data.get("projection", "xyz")),
        palette=palette,
        cells=cells,
    )


def load_txt(path: Path) -> Region:
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines()
    origin = (0, 0, 0)
    size = (0, 0, 0)
    projection = ""
    dimension = ""
    palette: dict[str, str] = {}
    for line in lines:
        if line.startswith("# dimension:"):
            dimension = line.split(":", 1)[1].strip()
        m = ORIGIN_RE.match(line)
        if m:
            origin = (int(m.group(1)), int(m.group(2)), int(m.group(3)))
        m = SIZE_RE.match(line)
        if m:
            size = (int(m.group(1)), int(m.group(2)), int(m.group(3)))
        m = PROJ_RE.search(line)
        if m:
            projection = m.group(1).lower()
        m = LEGEND_RE.match(line)
        if m:
            palette[m.group(1)] = m.group(2)

    if not projection:
        if size[0] == 1:
            projection = "yz"
        elif size[2] == 1:
            projection = "xy"
        elif size[1] == 1:
            projection = "xz"
        else:
            projection = "xyz"

    rows: list[tuple[int, str]] = []
    for line in lines:
        if line.startswith("#    ") and ("→" in line or "->" in line):
            continue
        m = GRID_RE.match(line)
        if not m:
            continue
        token_line = m.group(2)
        if re.fullmatch(r"[0-9 ]+", token_line):
            continue
        rows.append((int(m.group(1)), token_line.rstrip("\n")))

    if size == (0, 0, 0) and rows:
        width = max(len(r[1]) for r in rows)
        if projection == "xz":
            size = (width, 1, len(rows))
        elif projection == "xy":
            size = (width, len(rows), 1)
        else:
            size = (1, len(rows), width)

    cells: dict[tuple[int, int, int], str] = {}
    ox, oy, oz = origin
    for local_v, raw in rows:
        for local_h, token in enumerate(raw):
            state = palette.get(token, token)
            if projection == "xz":
                cells[(ox + local_h, oy, oz + local_v)] = state
            elif projection == "xy":
                cells[(ox + local_h, oy + local_v, oz)] = state
            else:
                cells[(ox, oy + local_v, oz + local_h)] = state

    return Region(
        path=path,
        dimension=dimension,
        origin=origin,
        size=size,
        projection=projection,
        palette=palette,
        cells=cells,
    )


def discover(export_dir: Path) -> tuple[Path | None, Path | None, list[Path]]:
    files = [
        p for p in export_dir.iterdir()
        if p.is_file()
        and p.suffix.lower() in {".txt", ".json"}
        and not p.stem.lower().startswith("preview_")
        and not REPLAY_NAME.search(p.stem)
    ]
    stems: dict[str, list[Path]] = defaultdict(list)
    for p in files:
        stems[p.stem].append(p)

    def prefer(paths: Iterable[Path]) -> Path:
        ranked = sorted(paths, key=lambda p: (0 if p.suffix.lower() == ".txt" else 1, p.name))
        return ranked[0]

    unique = [prefer(group) for group in stems.values()]
    expected = [p for p in unique if SHOULD_NAME.search(p.stem)]
    actuals = [p for p in unique if p not in expected]

    def recency(path: Path) -> tuple[int, float]:
        m = STAMP_NAME.search(path.stem)
        stamp = int(m.group(1).replace("_", "")) if m else 0
        return stamp, path.stat().st_mtime

    actuals.sort(key=recency, reverse=True)
    expected.sort(key=lambda p: p.stat().st_mtime, reverse=True)
    return (
        actuals[0] if actuals else None,
        expected[0] if expected else None,
        unique,
    )


def token_for(region: Region, state: str) -> str:
    for token, mapped in region.palette.items():
        if mapped == state:
            return token
    if is_air(state):
        return "."
    return block_id(state)[-1:].upper() or "?"


def axis_names(projection: str) -> tuple[str, str, str]:
    if projection == "xz":
        return "X", "Z", "Y"
    if projection == "xy":
        return "X", "Y", "Z"
    return "Z", "Y", "X"


def slice_key(pos: tuple[int, int, int], projection: str) -> tuple[int, int]:
    x, y, z = pos
    if projection == "xz":
        return z, x
    if projection == "xy":
        return y, x
    return y, z


def world_fixed(region: Region) -> int:
    ox, oy, oz = region.origin
    if region.projection == "xz":
        return oy
    if region.projection == "xy":
        return oz
    return ox


def iter_sorted_cells(region: Region) -> Iterable[tuple[tuple[int, int, int], str]]:
    return sorted(region.cells.items())


def profile(region: Region) -> None:
    print(f"=== Profile ===")
    print(f"file:   {region.path}")
    print(f"dim:    {region.dimension or '?'}")
    print(f"origin: {region.origin[0]} {region.origin[1]} {region.origin[2]}")
    print(f"size:   {region.size_x} x {region.size_y} x {region.size_z}  (X Y Z)")
    print(f"proj:   {region.projection}")
    print("palette:")
    for token, state in region.palette.items():
        count = sum(1 for s in region.cells.values() if s == state)
        print(f"  {token}  {state}  {count}")

    h_name, v_name, fixed_name = axis_names(region.projection)
    fixed = world_fixed(region)
    print(f"fixed:  world {fixed_name} = {fixed}")

    by_row: dict[int, list[tuple[int, str]]] = defaultdict(list)
    for (x, y, z), state in region.cells.items():
        v, h = slice_key((x, y, z), region.projection)
        by_row[local_v_from_world(region.origin, v, region.projection)].append(
            (local_h_from_world(region.origin, h, region.projection), state)
        )

    print()
    print(f"=== Row spans (local {v_name} rows, local {h_name} ->) ===")
    for v in sorted(by_row, reverse=region.projection != "xz"):
        cols = sorted(by_row[v])
        groups: dict[str, list[tuple[int, int]]] = {}
        for state in {s for _, s in cols if not is_air(s)}:
            runs: list[tuple[int, int]] = []
            start = None
            prev = None
            for h, cell in cols:
                if cell == state:
                    if start is None:
                        start = h
                    prev = h
                elif start is not None:
                    runs.append((start, prev if prev is not None else start))
                    start = None
                    prev = None
            if start is not None and prev is not None:
                runs.append((start, prev))
            if runs:
                groups[state] = runs
        bits = []
        for state, runs in sorted(groups.items(), key=lambda kv: kv[0]):
            tok = token_for(region, state)
            span = " ".join(f"[{a}-{b}]" if a != b else f"[{a}]" for a, b in runs)
            bits.append(f"{tok} {span}")
        print(f"  {v_name} {v:>4}  " + ("  ".join(bits) if bits else "empty"))

    print()
    print(f"=== Slice ({v_name} up/down as in export) ===")
    print_slice(region, region.projection)


def print_slice(region: Region, projection: str, indent: str = "") -> None:
    h_name, v_name, _ = axis_names(projection)
    by_row: dict[int, dict[int, str]] = defaultdict(dict)
    for pos, state in region.cells.items():
        v, h = slice_key(pos, projection)
        by_row[local_v_from_world(region.origin, v, projection)][
            local_h_from_world(region.origin, h, projection)
        ] = token_for(region, state)
    if not by_row:
        print(f"{indent}(empty)")
        return
    hs = [h for row in by_row.values() for h in row]
    h0, h1 = min(hs), max(hs)
    v_values = sorted(by_row, reverse=projection != "xz")
    idx_w = max(len(str(abs(v))) + (1 if v < 0 else 0) for v in v_values)
    idx_w = max(idx_w, len(v_name))
    print(f"{indent}{v_name:>{idx_w}}  local {h_name} {h0}..{h1}")
    for v in v_values:
        row = []
        cells = by_row[v]
        for h in range(h0, h1 + 1):
            row.append(cells.get(h, " "))
        print(f"{indent}{v:>{idx_w}}  {''.join(row)}")


def compare(actual: Region, expected: Region) -> int:
    print("=== Inputs ===")
    print(f"actual:   {actual.path}")
    print(f"          {actual.size_x}x{actual.size_y}x{actual.size_z} {actual.projection} origin {actual.origin}")
    print(f"expected: {expected.path}")
    print(f"          {expected.size_x}x{expected.size_y}x{expected.size_z} {expected.projection} origin {expected.origin}")

    if actual.size != expected.size:
        print("SIZE MISMATCH — comparing overlapping world cells only")
    if actual.origin != expected.origin:
        print("ORIGIN MISMATCH — aligning in world coordinates")
    if actual.projection != expected.projection and actual.size == expected.size:
        print(f"projection labels differ ({actual.projection} vs {expected.projection})")

    print()
    print("=== Palette ===")
    states = sorted(set(actual.palette.values()) | set(expected.palette.values()))
    print(f"{'actual':<8} {'expected':<8} block")
    for state in states:
        a = next((t for t, s in actual.palette.items() if s == state), "-")
        e = next((t for t, s in expected.palette.items() if s == state), "-")
        print(f"{a:<8} {e:<8} {state}")

    all_pos = set(actual.cells) | set(expected.cells)
    extra: list[tuple[tuple[int, int, int], str]] = []
    missing: list[tuple[tuple[int, int, int], str]] = []
    mismatch: list[tuple[tuple[int, int, int], str, str]] = []
    match = 0
    for pos in all_pos:
        a = actual.cells.get(pos)
        e = expected.cells.get(pos)
        a_air, e_air = is_air(a), is_air(e)
        if a == e or (a_air and e_air):
            match += 1
            continue
        if a_air and not e_air:
            missing.append((pos, e or "?"))
        elif e_air and not a_air:
            extra.append((pos, a or "?"))
        else:
            mismatch.append((pos, a or "?", e or "?"))

    print()
    print("=== Diff counts ===")
    print(f"match:    {match}")
    print(f"extra:    {len(extra)}   (in actual, air/absent in expected)  {count_states(extra)}")
    print(f"missing:  {len(missing)}   (in expected, air/absent in actual)  {count_states(missing)}")
    print(f"mismatch: {len(mismatch)}   (both present, different block)  {count_pairs(mismatch)}")

    if not extra and not missing and not mismatch:
        print()
        print("IDENTICAL in overlapping cells.")
        return 0

    projection = expected.projection if expected.projection in {"yz", "xy", "xz"} else actual.projection
    h_name, v_name, fixed_name = axis_names(projection)
    print()
    print(f"=== By row (local {v_name}; world {fixed_name} = {world_fixed(expected)}) ===")
    row_stats = summarize_rows(actual, expected, extra, missing, mismatch, projection)
    printed = 0
    for v, line in row_stats:
        print(line)
        printed += 1
        if printed >= ROW_LIMIT:
            print(f"  ... {len(row_stats) - ROW_LIMIT} more rows")
            break

    print()
    print("=== Overlay (differing rows only, local coords as in the .txt) ===")
    print("  + extra in actual   - missing from actual   ! wrong block   . same")
    print_overlays(actual, expected, projection)

    print()
    print("=== Systematic read ===")
    print_systematic(actual, expected, extra, missing, mismatch, projection)

    print()
    print("=== First cells ===")
    listed = 0
    for pos, state in extra[:DIFF_LIMIT]:
        print(f"  extra    {fmt_pos(pos)}  {state}")
        listed += 1
    for pos, state in missing[: max(0, DIFF_LIMIT - listed)]:
        print(f"  missing  {fmt_pos(pos)}  {state}")
        listed += 1
    for pos, a, e in mismatch[: max(0, DIFF_LIMIT - listed)]:
        print(f"  mismatch {fmt_pos(pos)}  {a} -> {e}")
        listed += 1
    leftover = len(extra) + len(missing) + len(mismatch) - listed
    if leftover > 0:
        print(f"  ... {leftover} more cells")
    return 1


def compare_three(generated: Region, expected: Region, generated_after: Region) -> int:
    comparisons = (
        ("generated vs expected", generated, expected),
        ("generated_after_modify vs expected", generated_after, expected),
        ("generated vs generated_after_modify", generated, generated_after),
    )
    result = 0
    for title, actual, target in comparisons:
        print(f"\n{'=' * 12} {title} {'=' * 12}")
        result = max(result, compare(actual, target))
    return result


def count_states(items: list[tuple[tuple[int, int, int], str]]) -> str:
    c = Counter(block_id(s) for _, s in items)
    if not c:
        return ""
    return "[" + ", ".join(f"{k}:{v}" for k, v in c.most_common()) + "]"


def count_pairs(items: list[tuple[tuple[int, int, int], str, str]]) -> str:
    c = Counter(f"{block_id(a)}->{block_id(e)}" for _, a, e in items)
    if not c:
        return ""
    return "[" + ", ".join(f"{k}:{v}" for k, v in c.most_common()) + "]"


def fmt_pos(pos: tuple[int, int, int]) -> str:
    return f"world {pos[0]} {pos[1]} {pos[2]}"


def summarize_rows(
    actual: Region,
    expected: Region,
    extra: list[tuple[tuple[int, int, int], str]],
    missing: list[tuple[tuple[int, int, int], str]],
    mismatch: list[tuple[tuple[int, int, int], str, str]],
    projection: str,
) -> list[tuple[int, str]]:
    h_name, v_name, _ = axis_names(projection)
    ox, oy, oz = expected.origin
    grouped: dict[int, dict[str, list[int]]] = defaultdict(lambda: {"extra": [], "missing": [], "mismatch": []})

    def local_h(pos: tuple[int, int, int]) -> int:
        x, y, z = pos
        if projection == "xz":
            return x - ox
        if projection == "xy":
            return x - ox
        return z - oz

    def local_v(pos: tuple[int, int, int]) -> int:
        x, y, z = pos
        if projection == "xz":
            return z - oz
        if projection == "xy":
            return y - oy
        return y - oy

    for pos, _ in extra:
        grouped[local_v(pos)]["extra"].append(local_h(pos))
    for pos, _ in missing:
        grouped[local_v(pos)]["missing"].append(local_h(pos))
    for pos, _, _ in mismatch:
        grouped[local_v(pos)]["mismatch"].append(local_h(pos))

    lines: list[tuple[int, str]] = []
    for v in sorted(grouped, reverse=projection != "xz"):
        g = grouped[v]
        parts = []
        if g["extra"]:
            parts.append(f"extra {fmt_span(g['extra'])}")
        if g["missing"]:
            parts.append(f"missing {fmt_span(g['missing'])}")
        if g["mismatch"]:
            parts.append(f"mismatch {fmt_span(g['mismatch'])}")
        a_span = solid_span(actual, expected.origin, v, projection)
        e_span = solid_span(expected, expected.origin, v, projection)
        shape = ""
        if a_span and e_span:
            a0, a1 = a_span
            e0, e1 = e_span
            bits = []
            if a0 < e0:
                bits.append(f"left outset {e0 - a0}")
            elif a0 > e0:
                bits.append(f"left inset {a0 - e0}")
            if a1 > e1:
                bits.append(f"right outset {a1 - e1}")
            elif a1 < e1:
                bits.append(f"right inset {e1 - a1}")
            if bits:
                shape = f"; actual {h_name}[{a0}-{a1}] expected {h_name}[{e0}-{e1}]; " + ", ".join(bits)
        lines.append((v, f"  {v_name} {v:>4}  " + "; ".join(parts) + shape))
    return lines


def solid_span(
    region: Region, origin: tuple[int, int, int], local_v: int, projection: str
) -> tuple[int, int] | None:
    ox, oy, oz = origin
    hs: list[int] = []
    for (x, y, z), state in region.cells.items():
        if is_air(state):
            continue
        v, h = slice_key((x, y, z), projection)
        if projection == "xz":
            if v != oz + local_v:
                continue
            hs.append(h - ox)
        elif projection == "xy":
            if v != oy + local_v:
                continue
            hs.append(h - ox)
        else:
            if v != oy + local_v:
                continue
            hs.append(h - oz)
    if not hs:
        return None
    return min(hs), max(hs)


def fmt_span(values: list[int]) -> str:
    if not values:
        return ""
    vals = sorted(set(values))
    runs: list[str] = []
    start = prev = vals[0]
    for n in vals[1:]:
        if n == prev + 1:
            prev = n
            continue
        runs.append(f"{start}-{prev}" if start != prev else str(start))
        start = prev = n
    runs.append(f"{start}-{prev}" if start != prev else str(start))
    return ",".join(runs)


def print_overlays(actual: Region, expected: Region, projection: str) -> None:
    h_name, v_name, _ = axis_names(projection)
    a_rows: dict[int, dict[int, str]] = defaultdict(dict)
    e_rows: dict[int, dict[int, str]] = defaultdict(dict)
    for pos, state in actual.cells.items():
        v, h = slice_key(pos, projection)
        a_rows[v][h] = state
    for pos, state in expected.cells.items():
        v, h = slice_key(pos, projection)
        e_rows[v][h] = state
    rows = sorted(set(a_rows) | set(e_rows), reverse=projection != "xz")
    hs = [h for row in list(a_rows.values()) + list(e_rows.values()) for h in row]
    if not hs:
        return
    h0, h1 = min(hs), max(hs)
    shown = 0
    for v in rows:
        a_map = a_rows.get(v, {})
        e_map = e_rows.get(v, {})
        act, exp, dif = [], [], []
        changed = False
        for h in range(h0, h1 + 1):
            a = a_map.get(h)
            e = e_map.get(h)
            act.append(token_for(actual, a) if a else " ")
            exp.append(token_for(expected, e) if e else " ")
            a_air, e_air = is_air(a), is_air(e)
            if a == e or (a_air and e_air):
                dif.append(".")
            elif a_air and not e_air:
                dif.append("-")
                changed = True
            elif e_air and not a_air:
                dif.append("+")
                changed = True
            else:
                dif.append("!")
                changed = True
        if not changed:
            continue
        local_v = local_v_from_world(expected.origin, v, projection)
        print(f"  {v_name} {local_v}  (world {v_name}={v})")
        print(f"    act  {''.join(act)}")
        print(f"    exp  {''.join(exp)}")
        print(f"    dif  {''.join(dif)}")
        shown += 1
        if shown >= ROW_LIMIT:
            print("  ... more differing rows omitted")
            break


def print_systematic(
    actual: Region,
    expected: Region,
    extra: list[tuple[tuple[int, int, int], str]],
    missing: list[tuple[tuple[int, int, int], str]],
    mismatch: list[tuple[tuple[int, int, int], str, str]],
    projection: str,
) -> None:
    h_name, v_name, _ = axis_names(projection)
    left_out = right_out = left_in = right_in = 0
    rows = 0
    vs = set()
    for pos, _ in extra + missing:
        vs.add(slice_key(pos, projection)[0])
    for v in vs:
        a = solid_span(actual, expected.origin, local_v_from_world(expected.origin, v, projection), projection)
        e = solid_span(expected, expected.origin, local_v_from_world(expected.origin, v, projection), projection)
        if not a or not e:
            continue
        rows += 1
        if a[0] < e[0]:
            left_out += 1
        elif a[0] > e[0]:
            left_in += 1
        if a[1] > e[1]:
            right_out += 1
        elif a[1] < e[1]:
            right_in += 1

    hints: list[str] = []
    if left_out and right_out and left_out == right_out:
        hints.append(f"too wide on both {h_name} sides on {left_out} {v_name}-rows (outer radius / rim too large)")
    elif left_in and right_in and left_in == right_in:
        hints.append(f"too narrow on both {h_name} sides on {left_in} {v_name}-rows (outer radius / rim too small)")
    elif left_out or right_out or left_in or right_in:
        hints.append(
            f"asymmetric {h_name} error: left_outset={left_out} right_outset={right_out} "
            f"left_inset={left_in} right_inset={right_in}"
        )
    if extra and not missing and not mismatch:
        hints.append("actual only adds blocks — thickness or radius likely too large")
    if missing and not extra and not mismatch:
        hints.append("actual only lacks blocks — thickness or radius likely too small, or a hole/spill is too wide")
    if mismatch and not extra and not missing:
        hints.append("same occupancy, wrong block — fill/water/air assignment, not outline")
    extra_y = Counter(p[1] for p, _ in extra)
    miss_y = Counter(p[1] for p, _ in missing)
    if extra_y or miss_y:
        ys = sorted(set(extra_y) | set(miss_y))
        if ys:
            oy = expected.origin[1]
            locals_y = [y - oy for y in ys]
            hints.append(
                f"world Y {ys[0]}..{ys[-1]}  local Y {min(locals_y)}..{max(locals_y)}  ({len(ys)} levels)"
            )
    if extra and missing and not mismatch:
        hints.append(
            "paired extra+missing usually means an outline/rim shifted, not a random hole"
        )
    if not hints:
        hints.append("no single radius/thickness pattern; inspect overlays and first cells")
    for hint in hints:
        print(f"  - {hint}")


def local_v_from_world(origin: tuple[int, int, int], world_v: int, projection: str) -> int:
    ox, oy, oz = origin
    if projection == "xz":
        return world_v - oz
    return world_v - oy


def local_h_from_world(origin: tuple[int, int, int], world_h: int, projection: str) -> int:
    ox, oy, oz = origin
    if projection == "yz":
        return world_h - oz
    return world_h - ox


def parse_args(argv: list[str]) -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Compare /rrexport region dumps")
    p.add_argument("actual", nargs="?", help="Actual export (.txt or .json)")
    p.add_argument("expected", nargs="?", help="Corrected export or region_should_be.txt")
    p.add_argument("generated_after", nargs="?", help="Regenerated export after the generator change")
    p.add_argument("--dir", default="exports", help="Folder to auto-pick files from")
    p.add_argument("--actual", dest="actual_opt", help="Actual path (overrides positional)")
    p.add_argument("--expected", dest="expected_opt", help="Expected path (overrides positional)")
    p.add_argument("--generated-after", dest="generated_after_opt", help="Regenerated export after the generator change")
    p.add_argument("--profile", action="store_true", help="Measure one file only (no expected)")
    return p.parse_args(argv)


def resolve_paths(args: argparse.Namespace) -> tuple[Path | None, Path | None]:
    actual = Path(args.actual_opt or args.actual) if (args.actual_opt or args.actual) else None
    expected = Path(args.expected_opt or args.expected) if (args.expected_opt or args.expected) else None
    if actual and expected:
        return actual, expected
    export_dir = Path(args.dir)
    if not export_dir.is_dir():
        return actual, expected
    found_a, found_e, _ = discover(export_dir)
    return actual or found_a, expected or found_e


def main(argv: list[str]) -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    args = parse_args(argv)
    generated_after_path = Path(args.generated_after_opt or args.generated_after) if (args.generated_after_opt or args.generated_after) else None
    if generated_after_path:
        generated_path = Path(args.actual_opt or args.actual) if (args.actual_opt or args.actual) else None
        expected_path = Path(args.expected_opt or args.expected) if (args.expected_opt or args.expected) else None
        paths = (generated_path, expected_path, generated_after_path)
        if any(path is None or not path.is_file() for path in paths):
            print("Three-way compare needs existing generated, expected, and generated_after_modify files.", file=sys.stderr)
            for label, path in zip(("generated", "expected", "generated_after_modify"), paths):
                if path is None or not path.is_file():
                    print(f"Missing {label}: {path}", file=sys.stderr)
            print("Usage: compare_region.py <generated> <expected> <generated_after_modify>", file=sys.stderr)
            return 2
        return compare_three(load(generated_path), load(expected_path), load(generated_after_path))

    actual_path, expected_path = resolve_paths(args)

    if args.profile or (actual_path and not expected_path):
        if not actual_path:
            print("No export found. Pass a file or put region_*.txt in exports/", file=sys.stderr)
            return 2
        profile(load(actual_path))
        if not expected_path:
            print()
            print("No expected file. Apply the user's verbal fix to this profile.")
        return 0

    if not actual_path or not expected_path:
        export_dir = Path(args.dir)
        print("Could not resolve both files.", file=sys.stderr)
        if export_dir.is_dir():
            _, _, all_files = discover(export_dir)
            print("In exports/:", file=sys.stderr)
            for p in all_files:
                print(f"  {p}", file=sys.stderr)
        print("Usage: compare_region.py <actual> <expected>", file=sys.stderr)
        return 2

    if not actual_path.is_file():
        print(f"Missing actual: {actual_path}", file=sys.stderr)
        return 2
    if not expected_path.is_file():
        print(f"Missing expected: {expected_path}", file=sys.stderr)
        return 2

    return compare(load(actual_path), load(expected_path))


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
