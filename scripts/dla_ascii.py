# -*- coding: utf-8 -*-
"""Procedural ASCII dendrites in the style of the 64x64 reference PNG.

Usage:
    python scripts/dla_ascii.py
    python scripts/dla_ascii.py --seed 42 --size 64

Writes scripts/dla_variants.txt (all algorithms) and scripts/dla_variants/*.txt
"""
from __future__ import annotations

import argparse
import math
import random
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "scripts" / "dla_variants"
COMBINED = ROOT / "scripts" / "dla_variants.txt"
ORIGINAL = ROOT / "scripts" / "dla_original_ascii.txt"

N4 = ((-1, 0), (1, 0), (0, -1), (0, 1))
N8 = ((-1, -1), (-1, 0), (-1, 1), (0, -1), (0, 1), (1, -1), (1, 0), (1, 1))

# sc_v4_wiggly locked params (64x64). Linear sizes scale with grid; jitter stays in radians.
WIGGLY = dict(step=1.0, jitter=0.70, kill=1.42, influence=5.0)


def wiggly_kwargs(size: int) -> dict:
    s = size / 64.0
    return dict(
        **WIGGLY,
        n_attractors=int(round(680 * s * s)),
        radius=20.0 * s,
        target=int(round(387 * s * s)),
        center=(size / 2.0, size / 2.0 - 8.0 * s),
    )


class _NodeIndex:
    """Uniform grid over nodes for radius queries."""

    def __init__(self, cell: float):
        self.cell = max(cell, 1.0)
        self.buckets: dict[tuple[int, int], list[int]] = {}

    def _key(self, x: float, y: float) -> tuple[int, int]:
        c = self.cell
        return (int(x // c), int(y // c))

    def add(self, i: int, x: float, y: float) -> None:
        k = self._key(x, y)
        self.buckets.setdefault(k, []).append(i)

    def query(self, x: float, y: float, radius: float) -> list[int]:
        c = self.cell
        gx, gy = self._key(x, y)
        r = int(radius / c) + 1
        ids: list[int] = []
        buckets = self.buckets
        for ix in range(gx - r, gx + r + 1):
            for iy in range(gy - r, gy + r + 1):
                ids.extend(buckets.get((ix, iy), ()))
        return ids


def grid_to_ascii(occ: set[tuple[int, int]], size: int, ch: str = "#") -> str:
    lines = []
    for y in range(size):
        row = [" "] * size
        for x in range(size):
            if (x, y) in occ:
                row[x] = ch
        lines.append("".join(row))
    return "\n".join(lines)


def stats(occ: set[tuple[int, int]], size: int) -> str:
    if not occ:
        return "n=0"
    xs = [p[0] for p in occ]
    ys = [p[1] for p in occ]
    cx = sum(xs) / len(occ)
    cy = sum(ys) / len(occ)
    rmax = max(math.hypot(x - cx, y - cy) for x, y in occ)
    return (
        f"n={len(occ)} occ={100 * len(occ) / (size * size):.1f}% "
        f"bbox=({min(xs)},{min(ys)})-({max(xs)},{max(ys)}) "
        f"com=({cx:.1f},{cy:.1f}) rmax={rmax:.1f}"
    )


def load_original() -> set[tuple[int, int]]:
    text = ORIGINAL.read_text(encoding="utf-8")
    occ: set[tuple[int, int]] = set()
    for y, line in enumerate(text.splitlines()):
        for x, ch in enumerate(line):
            if ch not in (" ",):
                occ.add((x, y))
    return occ


def dla(
    n_particles: int,
    size: int,
    seed: int,
    stick_neighbors=N4,
    stick_prob: float = 1.0,
    confine_r: float | None = None,
    center: tuple[int, int] | None = None,
) -> set[tuple[int, int]]:
    rng = random.Random(seed)
    cx, cy = center if center else (size // 2, size // 2)
    occupied = {(cx, cy)}
    r_max = 0.0
    neigh = stick_neighbors

    def near_cluster(x: int, y: int) -> bool:
        for dx, dy in neigh:
            if (x + dx, y + dy) in occupied:
                return True
        return False

    attempts = 0
    limit = n_particles * 6000
    while len(occupied) < n_particles and attempts < limit:
        attempts += 1
        launch_r = r_max + 4
        if confine_r is not None:
            launch_r = min(launch_r, max(confine_r - 1.0, 2.0))
        kill_r = (confine_r if confine_r is not None else launch_r + 14)
        ang = rng.random() * 2 * math.pi
        x = int(round(cx + launch_r * math.cos(ang)))
        y = int(round(cy + launch_r * math.sin(ang)))
        x = max(0, min(size - 1, x))
        y = max(0, min(size - 1, y))
        for _ in range(6000):
            dx, dy = rng.choice(N4)
            x += dx
            y += dy
            if not (0 <= x < size and 0 <= y < size):
                break
            dist = math.hypot(x - cx, y - cy)
            if dist > kill_r:
                break
            if near_cluster(x, y) and rng.random() < stick_prob:
                occupied.add((x, y))
                r_max = max(r_max, dist)
                break
    return occupied


def thicken(occ: set[tuple[int, int]], size: int, extra: int, seed: int) -> set[tuple[int, int]]:
    rng = random.Random(seed)
    occupied = set(occ)
    for _ in range(extra * 8):
        if len(occupied) >= len(occ) + extra:
            break
        x, y = rng.choice(tuple(occupied))
        dx, dy = rng.choice(N4)
        nx, ny = x + dx, y + dy
        if 0 <= nx < size and 0 <= ny < size:
            occupied.add((nx, ny))
    return occupied


def branching_walk(
    n_steps: int,
    size: int,
    seed: int,
    branch_p: float = 0.10,
    turn_p: float = 0.40,
) -> set[tuple[int, int]]:
    rng = random.Random(seed)
    cx = cy = size // 2
    occupied = {(cx, cy)}
    dirs = [(1, 0), (-1, 0), (0, 1), (0, -1)]
    tips = [(cx, cy, *rng.choice(dirs))]
    while len(occupied) < n_steps and tips:
        i = rng.randrange(len(tips))
        x, y, dx, dy = tips[i]
        if rng.random() < turn_p:
            ox, oy = x - cx, y - cy
            candidates = []
            for ndx, ndy in dirs:
                nx, ny = x + ndx, y + ndy
                if not (0 <= nx < size and 0 <= ny < size) or (nx, ny) in occupied:
                    continue
                outward = (nx - cx) * ox + (ny - cy) * oy
                w = 3.0 if outward >= 0 else 0.35
                if (ndx, ndy) == (dx, dy):
                    w *= 2.2
                if (ndx, ndy) == (-dx, -dy):
                    w *= 0.12
                candidates.append((w, ndx, ndy))
            if not candidates:
                tips.pop(i)
                continue
            total = sum(c[0] for c in candidates)
            r = rng.random() * total
            acc = 0.0
            ndx, ndy = candidates[0][1], candidates[0][2]
            for w, a, b in candidates:
                acc += w
                if r <= acc:
                    ndx, ndy = a, b
                    break
            dx, dy = ndx, ndy
        nx, ny = x + dx, y + dy
        if not (0 <= nx < size and 0 <= ny < size) or (nx, ny) in occupied:
            found = False
            for ndx, ndy in rng.sample(dirs, 4):
                tx, ty = x + ndx, y + ndy
                if 0 <= tx < size and 0 <= ty < size and (tx, ty) not in occupied:
                    nx, ny, dx, dy = tx, ty, ndx, ndy
                    found = True
                    break
            if not found:
                tips.pop(i)
                continue
        occupied.add((nx, ny))
        tips[i] = (nx, ny, dx, dy)
        if rng.random() < branch_p:
            for ndx, ndy in rng.sample(dirs, 4):
                tx, ty = nx + ndx, ny + ndy
                if 0 <= tx < size and 0 <= ty < size and (tx, ty) not in occupied:
                    occupied.add((tx, ty))
                    tips.append((tx, ty, ndx, ndy))
                    break
    return occupied


def dbm(
    n_particles: int,
    size: int,
    seed: int,
    eta: float,
    center: tuple[int, int] | None = None,
) -> set[tuple[int, int]]:
    """Dielectric-breakdown style: grow a 4-neighbor of the cluster with p ~ exposure^eta."""
    rng = random.Random(seed)
    cx, cy = center if center else (size // 2, size // 2)
    occupied = {(cx, cy)}

    def perimeter() -> list[tuple[int, int]]:
        s: set[tuple[int, int]] = set()
        for x, y in occupied:
            for dx, dy in N4:
                nx, ny = x + dx, y + dy
                if 0 <= nx < size and 0 <= ny < size and (nx, ny) not in occupied:
                    s.add((nx, ny))
        return list(s)

    while len(occupied) < n_particles:
        peri = perimeter()
        if not peri:
            break
        weights = []
        for x, y in peri:
            empty = 0
            for dx, dy in N4:
                nx, ny = x + dx, y + dy
                if not (0 <= nx < size and 0 <= ny < size) or (nx, ny) not in occupied:
                    empty += 1
            r = math.hypot(x - cx, y - cy)
            phi = (empty / 4.0) ** eta
            phi *= 0.35 + 0.65 * min(1.0, r / 8.0)
            weights.append(max(phi, 1e-6))
        total = sum(weights)
        pick = rng.random() * total
        acc = 0.0
        chosen = peri[0]
        for p, w in zip(peri, weights):
            acc += w
            if pick <= acc:
                chosen = p
                break
        occupied.add(chosen)
    return occupied


def bresenham(x0: int, y0: int, x1: int, y1: int) -> list[tuple[int, int]]:
    pts = []
    dx = abs(x1 - x0)
    dy = -abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx + dy
    x, y = x0, y0
    while True:
        pts.append((x, y))
        if x == x1 and y == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x += sx
        if e2 <= dx:
            err += dx
            y += sy
    return pts


def trim_to(occupied: set[tuple[int, int]], target: int, rng: random.Random) -> set[tuple[int, int]]:
    occ = set(occupied)
    while len(occ) > target:
        tips = []
        for x, y in occ:
            n_occ = sum(1 for dx, dy in N4 if (x + dx, y + dy) in occ)
            if n_occ <= 1:
                tips.append((x, y))
        if not tips:
            tips = list(occ)
        occ.remove(rng.choice(tips))
    return occ


def fatten_to(
    occupied: set[tuple[int, int]],
    target: int,
    size: int,
    cx: float,
    cy: float,
    rng: random.Random,
    max_r: float,
    core_boost: float = 1.55,
    rim_boost: float = 0.22,
    neighbor_pow: float = 2.0,
) -> set[tuple[int, int]]:
    """Fatten a 1-px skeleton toward `target` cells. Core and 2–3-neighbor notches first."""
    occ = set(occupied)
    peri: set[tuple[int, int]] = set()
    for x, y in occ:
        for dx, dy in N4:
            nx, ny = x + dx, y + dy
            if 0 <= nx < size and 0 <= ny < size and (nx, ny) not in occ:
                peri.add((nx, ny))
    while len(occ) < target and peri:
        weights: list[float] = []
        cells = list(peri)
        for x, y in cells:
            n_occ = 0
            for dx, dy in N4:
                if (x + dx, y + dy) in occ:
                    n_occ += 1
            r = math.hypot(x - cx, y - cy)
            if r > max_r:
                weights.append(0.0)
                continue
            t = min(1.0, r / max(max_r, 1.0))
            radial = core_boost * (1.0 - t) + rim_boost * t
            weights.append((0.28 + n_occ ** neighbor_pow) * radial)
        total = sum(weights)
        if total <= 0:
            break
        pick = rng.random() * total
        acc = 0.0
        chosen = cells[0]
        for p, w in zip(cells, weights):
            acc += w
            if pick <= acc:
                chosen = p
                break
        peri.remove(chosen)
        occ.add(chosen)
        x, y = chosen
        for dx, dy in N4:
            nx, ny = x + dx, y + dy
            if 0 <= nx < size and 0 <= ny < size and (nx, ny) not in occ:
                peri.add((nx, ny))
    return occ


def space_colonization(
    size: int,
    seed: int,
    n_attractors: int = 680,
    radius: float = 20.0,
    influence: float = 5.4,
    kill: float = 1.55,
    step: float = 1.05,
    jitter: float = 0.42,
    target: int = 387,
    center: tuple[float, float] | None = None,
    core_boost: float = 1.55,
    rim_boost: float = 0.22,
    neighbor_pow: float = 2.0,
    center_mix: float = 0.42,
) -> set[tuple[int, int]]:
    """Runions space colonization tuned toward the reference: dense core, wiggly 1–2 px veins."""
    rng = random.Random(seed)
    # Reference cluster sits in the upper half of the 64x64 frame (com y≈24).
    cx, cy = center if center is not None else (size / 2.0, size / 2.0 - 8.0)
    attractors: list[tuple[float, float]] = []
    while len(attractors) < n_attractors:
        ang = rng.random() * 2 * math.pi
        # Mix uniform disk (hairy rim) with extra central points (thick core).
        if rng.random() < center_mix:
            r = radius * (rng.random() ** 1.2)
        else:
            r = radius * math.sqrt(rng.random())
        attractors.append((cx + r * math.cos(ang), cy + r * math.sin(ang)))

    nodes: list[tuple[float, float]] = [(cx, cy)]
    occupied: set[tuple[int, int]] = {(int(round(cx)), int(round(cy)))}
    inf = influence
    index = _NodeIndex(max(inf, 2.0))
    index.add(0, cx, cy)

    def raster(x0: float, y0: float, x1: float, y1: float) -> None:
        for x, y in bresenham(int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1))):
            if 0 <= x < size and 0 <= y < size:
                occupied.add((x, y))

    def kill_near(points: list[tuple[float, float]], rad: float) -> None:
        nonlocal attractors
        if not points:
            return
        r2 = rad * rad
        keep = []
        for ax, ay in attractors:
            dead = False
            for px, py in points:
                dx = ax - px
                dy = ay - py
                if dx * dx + dy * dy <= r2:
                    dead = True
                    break
            if not dead:
                keep.append((ax, ay))
        attractors = keep

    kill_near(nodes, kill)

    max_iters = 1200 if size <= 64 else 2500
    for _ in range(max_iters):
        if not attractors or len(occupied) >= target:
            break
        assigned: list[list[tuple[float, float]]] = [[] for _ in nodes]
        for ax, ay in attractors:
            best_i = None
            best_d = inf
            for i in index.query(ax, ay, inf):
                nx, ny = nodes[i]
                d = math.hypot(ax - nx, ay - ny)
                if d < best_d:
                    best_d = d
                    best_i = i
            if best_i is not None:
                assigned[best_i].append((ax, ay))
        new_nodes: list[tuple[float, float]] = []
        hit_target = False
        for i, pts in enumerate(assigned):
            if not pts:
                continue
            nx, ny = nodes[i]
            vx = sum(p[0] - nx for p in pts) / len(pts)
            vy = sum(p[1] - ny for p in pts) / len(pts)
            ang = math.atan2(vy, vx) + rng.uniform(-jitter, jitter)
            nnx = nx + step * math.cos(ang)
            nny = ny + step * math.sin(ang)
            raster(nx, ny, nnx, nny)
            new_nodes.append((nnx, nny))
            if len(occupied) >= target:
                hit_target = True
                break
        if not new_nodes:
            inf = max(influence * 0.55, inf * 0.90)
            if inf <= influence * 0.56:
                break
            continue
        base = len(nodes)
        for k, (nnx, nny) in enumerate(new_nodes):
            index.add(base + k, nnx, nny)
        nodes.extend(new_nodes)
        kill_near(new_nodes, kill)
        if hit_target:
            break

    occ = fatten_to(
        occupied,
        target,
        size,
        cx,
        cy,
        rng,
        max_r=radius + 1.2,
        core_boost=core_boost,
        rim_boost=rim_boost,
        neighbor_pow=neighbor_pow,
    )
    return trim_to(occ, target, rng)


def eden(n_particles: int, size: int, seed: int) -> set[tuple[int, int]]:
    rng = random.Random(seed)
    cx = cy = size // 2
    occupied = {(cx, cy)}
    peri = {(cx + dx, cy + dy) for dx, dy in N4}
    while len(occupied) < n_particles and peri:
        x, y = rng.choice(tuple(peri))
        peri.remove((x, y))
        if not (0 <= x < size and 0 <= y < size) or (x, y) in occupied:
            continue
        occupied.add((x, y))
        for dx, dy in N4:
            nx, ny = x + dx, y + dy
            if 0 <= nx < size and 0 <= ny < size and (nx, ny) not in occupied:
                peri.add((nx, ny))
    return occupied


def lsystem_tree(size: int, seed: int, n_particles: int = 387) -> set[tuple[int, int]]:
    """Stochastic recursive branches (L-system-ish). Usually too 'tree-like' vs DLA."""
    rng = random.Random(seed)
    occupied: set[tuple[int, int]] = set()
    cx = cy = size // 2

    def stamp(x: float, y: float) -> None:
        ix, iy = int(round(x)), int(round(y))
        if 0 <= ix < size and 0 <= iy < size:
            occupied.add((ix, iy))

    def branch(x: float, y: float, ang: float, length: float, depth: int) -> None:
        if depth <= 0 or length < 1.2 or len(occupied) >= n_particles:
            return
        steps = max(2, int(length))
        for i in range(steps):
            t = (i + 1) / steps
            stamp(x + math.cos(ang) * length * t, y + math.sin(ang) * length * t)
        nx = x + math.cos(ang) * length
        ny = y + math.sin(ang) * length
        forks = 2 if rng.random() > 0.25 else 3
        for _ in range(forks):
            na = ang + rng.uniform(-0.9, 0.9)
            branch(nx, ny, na, length * rng.uniform(0.55, 0.78), depth - 1)

    for k in range(6):
        branch(cx, cy, k * math.pi / 3 + rng.uniform(-0.2, 0.2), rng.uniform(6.0, 9.0), 5)
    return occupied


VARIANTS = [
    (
        "01_dla4",
        "DLA 4-neighbor (classic Witten-Sander). Walkers stick on first edge contact.",
        lambda size, seed: dla(387, size, seed, stick_neighbors=N4),
    ),
    (
        "02_dla8",
        "DLA 8-neighbor. Diagonal stick allowed — thinner, more anisotropic arms.",
        lambda size, seed: dla(387, size, seed, stick_neighbors=N8),
    ),
    (
        "03_dla_confined",
        "DLA confined to r=21. Same sticking, but walkers cannot leave a circle — denser envelope.",
        lambda size, seed: dla(387, size, seed, stick_neighbors=N4, confine_r=21.0),
    ),
    (
        "04_dla_penetrating",
        "DLA stick_prob=0.25. Walkers often bounce off and crawl into fjords — thicker branches.",
        lambda size, seed: dla(387, size, seed, stick_neighbors=N4, stick_prob=0.25, confine_r=22.0),
    ),
    (
        "05_dla_thickened",
        "Classic DLA (~280 particles) then random 4-neighbor dilation up to 387. Fatter veins.",
        lambda size, seed: thicken(dla(280, size, seed, stick_neighbors=N4, confine_r=20.0), size, 107, seed + 99),
    ),
    (
        "06_dbm_eta05",
        "DBM eta=0.5 (dense branching). Tips preferred only weakly — compact circular cloud.",
        lambda size, seed: dbm(387, size, seed, eta=0.5),
    ),
    (
        "07_dbm_eta2",
        "DBM eta=2. Strong tip bias — spikier, more screened fjords.",
        lambda size, seed: dbm(387, size, seed, eta=2.0),
    ),
    (
        "08_branching_walk",
        "Outward biased self-avoiding walks that fork. Coral / lightning corridors.",
        lambda size, seed: branching_walk(387, size, seed),
    ),
    (
        "09_space_colonization",
        "Space colonization tuned to the reference: short wiggly internodes, dense core, fatten to 387.",
        lambda size, seed: space_colonization(size, seed),
    ),
    (
        "10_lsystem",
        "Stochastic recursive forks from the center. Regular tree, not a DLA cluster.",
        lambda size, seed: lsystem_tree(size, seed),
    ),
    (
        "11_eden",
        "Eden growth: uniform random perimeter. Compact blob, almost no branches.",
        lambda size, seed: eden(387, size, seed),
    ),
]


def write_block(title: str, note: str, occ: set[tuple[int, int]], size: int) -> str:
    return f"{title}\n{note}\n{stats(occ, size)}\n{grid_to_ascii(occ, size)}\n"


def occ_to_image(occ: set[tuple[int, int]], size: int, scale: int = 1):
    from PIL import Image

    buf = bytearray(size * size)
    for x, y in occ:
        if 0 <= x < size and 0 <= y < size:
            buf[y * size + x] = 255
    img = Image.frombytes("L", (size, size), bytes(buf))
    if scale != 1:
        img = img.resize((size * scale, size * scale), Image.NEAREST)
    return img.convert("RGB")


def save_png(occ: set[tuple[int, int]], size: int, path: Path, scale: int = 1) -> None:
    occ_to_image(occ, size, scale=scale).save(path)


def contact_sheet(images: list, cols: int = 4, pad: int = 8, label_h: int = 22):
    from PIL import Image, ImageDraw, ImageFont

    if not images:
        return None
    w, h = images[0].size
    rows = math.ceil(len(images) / cols)
    sheet = Image.new(
        "RGB",
        (cols * (w + pad) + pad, rows * (h + pad + label_h) + pad),
        (24, 24, 24),
    )
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.load_default()
    except OSError:
        font = None
    for i, img in enumerate(images):
        r, c = divmod(i, cols)
        x = pad + c * (w + pad)
        y = pad + r * (h + pad + label_h)
        sheet.paste(img, (x, y + label_h))
        draw.text((x, y), f"v{i + 1:02d}", fill=(220, 220, 220), font=font)
    return sheet


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate ASCII dendrite variants")
    parser.add_argument("--seed", type=int, default=7)
    parser.add_argument("--size", type=int, default=None)
    parser.add_argument(
        "--sc-tune",
        action="store_true",
        help="Write extra space-colonization parameter sweeps into dla_variants/",
    )
    parser.add_argument(
        "--wiggly-batch",
        action="store_true",
        help="Many sc_v4_wiggly instances at --size (default 128) as txt+png",
    )
    parser.add_argument("--count", type=int, default=20, help="How many wiggly seeds to write")
    args = parser.parse_args()
    size = args.size if args.size is not None else (128 if args.wiggly_batch else 64)
    seed = args.seed

    OUT_DIR.mkdir(parents=True, exist_ok=True)

    if args.wiggly_batch:
        out = OUT_DIR / f"wiggly_{size}"
        out.mkdir(parents=True, exist_ok=True)
        kwargs = wiggly_kwargs(size)
        prefer = [seed, 7, 11, 21, 3, 13, 17, 19, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101]
        seeds: list[int] = []
        for s in prefer:
            if s not in seeds:
                seeds.append(s)
            if len(seeds) >= args.count:
                break
        extra = 2
        while len(seeds) < args.count:
            if extra not in seeds:
                seeds.append(extra)
            extra += 1

        thumbs = []
        labels = []
        for i, s in enumerate(seeds, start=1):
            occ = space_colonization(size, s, **kwargs)
            name = f"v{i:02d}_seed{s:04d}"
            note = f"sc_v4_wiggly  {size}x{size}  seed={s}  {stats(occ, size)}"
            block = write_block(name, note, occ, size)
            (out / f"{name}.txt").write_text(block, encoding="utf-8")
            save_png(occ, size, out / f"{name}.png", scale=1)
            view = occ_to_image(occ, size, scale=4)
            view.save(out / f"{name}_x4.png")
            thumbs.append(occ_to_image(occ, size, scale=2))
            print(f"{name:20} {stats(occ, size)}")

        sheet = contact_sheet(thumbs, cols=4)
        if sheet is not None:
            sheet_path = out / "contact_sheet.png"
            sheet.save(sheet_path)
            print(f"wrote {sheet_path}")
        print(f"wrote {out} ({len(seeds)} versions)")
        return

    if args.sc_tune:
        sweeps: list[tuple[str, str, dict, int]] = [
            (
                "sc_v1_balanced",
                "База: короткие виггли-ветки, плотность как у оригинала.",
                {},
                seed,
            ),
            (
                "sc_v2_bushy",
                "Кустистее: больше коротких веточек по всему кругу.",
                dict(n_attractors=820, kill=1.32, influence=4.6, jitter=0.48, rim_boost=0.45),
                seed,
            ),
            (
                "sc_v3_thick_core",
                "Толстая сердцевина, тонкие кончики — ближе к пятнам оригинала.",
                dict(n_attractors=520, kill=1.85, influence=6.4, jitter=0.28, core_boost=2.4, rim_boost=0.12, neighbor_pow=2.4),
                seed,
            ),
            (
                "sc_v4_wiggly",
                "Сильный джиттер: ломаные ступеньки, меньше прямых лучей.",
                dict(step=1.0, jitter=0.70, kill=1.42, influence=5.0),
                seed,
            ),
            (
                "sc_v5_sparse_rim",
                "Редкий лохматый край, дырки и фьорды как у оригинала.",
                dict(n_attractors=560, kill=1.7, center_mix=0.55, core_boost=1.8, rim_boost=0.08, neighbor_pow=1.4),
                seed,
            ),
            (
                "sc_v6_filled",
                "Больше залитых жил (##/###), меньше одиночных пикселей.",
                dict(n_attractors=500, kill=1.9, influence=6.8, jitter=0.22, neighbor_pow=2.8, core_boost=1.7, rim_boost=0.35),
                seed,
            ),
            (
                "sc_v7_seed11",
                "Та же база, что v1, другой seed — другой экземпляр.",
                {},
                11,
            ),
            (
                "sc_v8_seed21",
                "Та же база, что v1, ещё один seed.",
                {},
                21,
            ),
        ]
        orig = load_original()
        orig_block = write_block(
            "00_original  (ASCII-слепок референса, 64x64)",
            "Не генерация — оригинал для сравнения.",
            orig,
            64,
        )
        (OUT_DIR / "00_original.txt").write_text(orig_block, encoding="utf-8")
        print("00_original", stats(orig, 64))
        chunks = [orig_block.rstrip() + "\n"]
        for name, note, kwargs, s in sweeps:
            occ = space_colonization(size, s, **kwargs)
            header = f"{name}  seed={s}  size={size}"
            block = write_block(header, note, occ, size)
            (OUT_DIR / f"{name}.txt").write_text(block, encoding="utf-8")
            chunks.append(block.rstrip() + "\n")
            print(f"{name:20} {stats(occ, size)}")
        compare_path = ROOT / "scripts" / "dla_sc_versions.txt"
        compare_path.write_text(
            "Space colonization vs original. Open files in scripts/dla_variants/sc_v*.txt\n"
            + ("\n" + "=" * 64 + "\n").join(chunks),
            encoding="utf-8",
        )
        print(f"wrote {compare_path}")
        return

    chunks: list[str] = []

    orig = load_original()
    orig_note = (
        "00_original  (ASCII trace of the reference PNG, 64x64, binary)\n"
        "Not generated — pixel snapshot so the other blocks can be compared to it."
    )
    orig_block = write_block(orig_note, "", orig, 64)
    # load_original is always 64 wide; if size!=64 still keep original as-is
    chunks.append(orig_block.rstrip() + "\n")
    (OUT_DIR / "00_original.txt").write_text(orig_block, encoding="utf-8")

    for name, note, fn in VARIANTS:
        occ = fn(size, seed)
        header = f"{name}  seed={seed}  size={size}"
        block = write_block(header, note, occ, size)
        chunks.append(block.rstrip() + "\n")
        (OUT_DIR / f"{name}.txt").write_text(block, encoding="utf-8")
        print(f"{name:20} {stats(occ, size)}")

    intro = (
        f"ASCII dendrite variants  size={size}  seed={seed}\n"
        f"Original occupancy was 387/4096 (9.4%), rmax~19.8, com~(32.8,24.2).\n"
        f"Each algorithm below targets ~387 cells on a {size}x{size} grid.\n"
        f"Re-run: python scripts/dla_ascii.py --seed {seed}\n"
        f"{'=' * 64}\n"
    )
    COMBINED.write_text(intro + ("\n" + "=" * 64 + "\n").join(chunks), encoding="utf-8")
    print(f"wrote {COMBINED}")
    print(f"wrote {OUT_DIR}")


if __name__ == "__main__":
    main()
