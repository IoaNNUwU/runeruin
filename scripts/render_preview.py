#!/usr/bin/env python3
"""Render exports/preview_*.json (runeruin.region/1) to a PNG sheet of orthographic 3D views.

    python scripts/render_preview.py out.png exports/preview_a.json [exports/preview_b.json ...] [options]

    --view ne:above,s:level,top   where the camera is: a side (n ne e se s sw w nw, or a compass bearing in degrees)
                                  and a height (above, level, below, top, bottom, or degrees above the horizon).
                                  Default: the four corners from above, and from below as well when the subject
                                  hangs from a ceiling the preview prepared.
    --box x1,y1,z1,x2,y2,z2       close-up: only this box of the export. Default: the subject with four blocks
                                  around it, so prepared ground does not set the scale; "--box all" shows everything.
    --cut x=12                    cut at that coordinate and look inside; "--cut x" cuts through the middle,
                                  where the text slice is. Also y and z. The half nearer to the camera is removed.
    --prepared faint|hide|solid   the ground, ceiling and pool the preview prepared (default: faint)
    --diff before.json            one picture of the changes: added blocks green, removed red, replaced orange
    --flat                        flat colours instead of textures: each block one colour (the average of its texture),
                                  every block a full cube. --diff always draws flat.
    --size 1500                   the longer side of the sheet in pixels; the scale is fitted to it
    --scale 12                    pixels per block instead of fitting

Several exports (also "exports/preview_boulder_s*.json") go on one sheet, one view of each with its seed below:
for comparing seeds, or a shape before and after a change.

Coordinates are local, as in the text slices: the edges of the box carry them, world = origin + local.
Faces are shaded by direction as in the game and darken with distance. Every block is drawn from its model with
its textures (blockstate variants and multiparts of src/main and src/generated, the model of the block's own name
for vanilla blocks), so slabs, plants and vines keep their shape; a block with no model found is a cube of the
average colour of its texture. Water is translucent, a cut face is pale and unshaded. With --flat blocks without
collision (plants, vines) are outlines. The JSON and the text slices stay the exact source of truth; pictures are
for judging shape: no lighting, and a tint (grass, vanilla leaves) is the plains colour.
"""
import argparse
import colorsys
import glob
import hashlib
import json
import math
import re
import sys
from array import array
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / ".agents/skills/minecraft-model-texture-analysis/scripts"))
from extract_model_faces import (  # noqa: E402
    ModelError,
    default_uv,
    load_model,
    numbers,
    parse_identifier,
    resolve_texture_file,
    resolve_texture_token,
)
from render_model import NORMALS, SHADE, VERTICES, apply, axis_rotation, element_transform, normalize  # noqa: E402

# Resolved: .mc-sources is a link, and the model loader accepts only files that stay under their root.
ASSET_ROOTS = [(ROOT / path).resolve() for path in ("src/main/resources/assets", "src/generated/resources/assets",
                                                    ".mc-sources/textures/assets")]
TEXTURE_DIRS = [
    ROOT / "src/main/resources/assets/runeruin/textures/block",
    ROOT / ".mc-sources/textures/assets/minecraft/textures/block",
]
# src/main first: the jar keeps the file a person keeps over a generated one.
MODEL_DIRS = [
    ROOT / "src/main/resources/assets/runeruin/models/block",
    ROOT / "src/generated/resources/assets/runeruin/models/block",
    ROOT / ".mc-sources/textures/assets/minecraft/models/block",
]
AIR = {"air", "cave_air", "void_air"}
WATER_BLOCKS = {"water", "bubble_column"}
UNDERWATER_PLANTS = {"kelp", "kelp_plant", "seagrass", "tall_seagrass"}
# Grey vanilla textures that the game tints by biome: the plains colours.
FOLIAGE = (119, 171, 47)
GRASS = (145, 189, 89)
WATER_RGB = (63, 118, 228)
TINTS = {"water": WATER_RGB, "vine": FOLIAGE, "lily_pad": (32, 128, 48)}
TINTS.update({name: GRASS for name in ("grass_block", "short_grass", "tall_grass", "fern", "large_fern")})

# How a block is drawn. Faces between two blocks of one group are not drawn: a pool is one volume.
EMPTY, CUT, SOLID, WATER, WET_PLANT, FAINT, GHOST, HOLLOW = range(8)
GROUP = [0, 0, 1, 2, 2, 3, 4, 5]
ALPHA = {WATER: 70, WET_PLANT: 70, FAINT: 40, GHOST: 110}
OUTLINED = (WET_PLANT, HOLLOW)
SUFFIX = {FAINT: " (prepared)", HOLLOW: " (outline)", WET_PLANT: " (outline)"}
ADDED, REMOVED, CHANGED, UNCHANGED = (60, 200, 80), (230, 60, 60), (240, 170, 40), (150, 150, 150)

# Normal, corners and the game's directional shade of the six faces.
FACES = [
    ((0, 1, 0), ((0, 1, 0), (1, 1, 0), (1, 1, 1), (0, 1, 1)), 1.0),
    ((0, -1, 0), ((0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)), 0.5),
    ((0, 0, -1), ((0, 0, 0), (1, 0, 0), (1, 1, 0), (0, 1, 0)), 0.8),
    ((0, 0, 1), ((0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)), 0.8),
    ((-1, 0, 0), ((0, 0, 0), (0, 0, 1), (0, 1, 1), (0, 1, 0)), 0.6),
    ((1, 0, 0), ((1, 0, 0), (1, 0, 1), (1, 1, 1), (1, 1, 0)), 0.6),
]
FAR_SHADE = 0.6  # the farthest corner of the box is this much darker than the nearest

SIDES = {"n": 0, "ne": 45, "e": 90, "se": 135, "s": 180, "sw": 225, "w": 270, "nw": 315}
HEIGHTS = {"above": 30, "level": 0, "below": -30, "top": 90, "bottom": -90}
CUT_VIEWS = {0: "e:level,ne:above,se:above", 1: "top,se:above,sw:above", 2: "s:level,se:above,sw:above"}

SHEET, PANEL, EDGE, AXIS, TEXT, DIM = (18, 18, 22), (30, 30, 36), (62, 62, 74), (150, 150, 165), (225, 225, 230), (150, 150, 160)
LEFT, TOP, RIGHT, BOTTOM = 46, 24, 10, 24  # panel margins: tick labels on the left and below, the view name above
GAP, LINE = 8, 18
MAX_SCALE = 24


def load_font():
    try:
        return ImageFont.load_default(13)
    except TypeError:  # Pillow before 10.1 has only the small bitmap font
        return ImageFont.load_default()


FONT = load_font()
_colors = {}
untextured = set()
unmodelled = set()


def block_name(state):
    return state.split("[")[0].split(":")[-1]


def block_color(name):
    if name not in _colors:
        rgb = texture_color(name)
        if rgb is None:
            untextured.add(name)
            rgb = hash_color(name)
        tint = TINTS.get(name) or (FOLIAGE if name.endswith("_leaves") and max(rgb) - min(rgb) < 16 else None)
        _colors[name] = tuple(c * t // 255 for c, t in zip(rgb, tint)) if tint else rgb
    return _colors[name]


def texture_color(name):
    """Average colour of the texture named after the block, else of its model's texture, else of any name_*.png."""
    textures = [name + suffix for suffix in ("", "_top", "_side", "_still")] + model_textures(name)
    paths = [directory / (texture + ".png") for texture in textures for directory in TEXTURE_DIRS]
    for path in paths + [path for directory in TEXTURE_DIRS for path in sorted(directory.glob(name + "_*.png"))]:
        if path.exists():
            data = Image.open(path).convert("RGBA").tobytes()
            pixels = [data[i:i + 4] for i in range(0, len(data), 4) if data[i + 3] > 0]
            if pixels:
                return tuple(sum(p[i] for p in pixels) // len(pixels) for i in range(3))
    return None


def model_textures(name):
    for directory in MODEL_DIRS:
        path = directory / (name + ".json")
        if path.exists():
            textures = json.loads(path.read_text(encoding="utf-8")).get("textures", {})
            # A texture is a string, or an object with "sprite" in 26.x models.
            textures = {key: value.get("sprite", "") if isinstance(value, dict) else value for key, value in textures.items()}
            order = sorted(textures, key=lambda key: ("all", "top", "end", "side").index(key) if key in ("all", "top", "end", "side") else 4)
            return [textures[key].split("/")[-1] for key in order if textures[key] and not textures[key].startswith("#")]
    return []


def hash_color(name):
    hue = int(hashlib.md5(name.encode()).hexdigest()[:4], 16) / 65535
    return tuple(int(c * 255) for c in colorsys.hsv_to_rgb(hue, 0.6, 0.9))


class Baked:
    """The faces of one block state from its model: corners in block units, texture, shade and what culls it."""

    def __init__(self):
        self.faces = []  # (corners, uv in texture pixels, texture, normal, shade, tinted, cullface, centre)
        self.full = False  # one unrotated 16x16x16 element: hides what it touches


_textures = {}
_models = {}


def texture_image(resource, namespace):
    key = (resource, namespace)
    if key not in _textures:
        image = Image.open(resource_path(resource, namespace)).convert("RGBA")
        if image.height > image.width and image.height % image.width == 0:  # an animation: its first frame
            image = image.crop((0, 0, image.width, image.width))
        _textures[key] = image
    return _textures[key]


def resource_path(resource, namespace):
    return resolve_texture_file(resource, namespace, [root for root in ASSET_ROOTS if root.is_dir()])[0]


def state_models(namespace, name, props):
    """(model, x, y) of a state: the variant of its blockstate file and every multipart that applies, or the
    model of the block's own name when there is no file (vanilla blocks)."""
    for root in ASSET_ROOTS:
        path = root / namespace / "blockstates" / (name + ".json")
        if path.is_file():
            break
    else:
        return [(parse_identifier(f"{namespace}:block/{name}"), 0, 0)]
    data = json.loads(path.read_text(encoding="utf-8-sig"))
    found = []

    def pick(entry):
        entry = entry[0] if isinstance(entry, list) else entry
        found.append((parse_identifier(entry["model"]), int(entry.get("x", 0)), int(entry.get("y", 0))))

    def matches(when):
        if "OR" in when:
            return any(matches(part) for part in when["OR"])
        if "AND" in when:
            return all(matches(part) for part in when["AND"])
        return all(props.get(key) in str(value).lower().split("|") for key, value in when.items())

    for key, entry in data.get("variants", {}).items():
        if all(props.get(k) == v for k, _, v in (pair.partition("=") for pair in key.split(",") if pair)):
            pick(entry)
            break
    for part in data.get("multipart", []):
        if "when" not in part or matches(part["when"]):
            pick(part["apply"])
    return found


def bake(state):
    """The model of a block state, or None when it has none we can draw (the block is then a flat cube)."""
    if state in _models:
        return _models[state]
    ident, _, rest = state.partition("[")
    namespace, _, name = ident.rpartition(":")
    props = dict(pair.split("=", 1) for pair in rest.rstrip("]").split(",") if "=" in pair)
    baked = Baked()
    try:
        roots = [root for root in ASSET_ROOTS if root.is_dir()]
        for ref, rx, ry in state_models(namespace or "minecraft", name, props):
            model = load_model(ref, roots)
            if not isinstance(model.get("elements"), list):
                raise ModelError("no elements")
            # A texture is a string, or an object with "sprite" in 26.x models.
            textures = {key: value.get("sprite", "") if isinstance(value, dict) else value
                        for key, value in model["textures"].items()}
            # A blockstate turns the model about the block's middle: x first, then y, as the game does.
            spin_x, spin_y = axis_rotation("x", -rx), axis_rotation("y", -ry)

            def spin(vector, centre=0):
                x, y, z = apply(spin_y, apply(spin_x, tuple(c - centre for c in vector)))
                return x + centre, y, z + centre

            for index, element in enumerate(model["elements"]):
                lo = numbers(element.get("from"), f"element {index} from", 3)
                hi = numbers(element.get("to"), f"element {index} to", 3)
                transform = element_transform(element.get("rotation"))
                baked.full |= lo == [0, 0, 0] and hi == [16, 16, 16] and not transform
                for direction, face in (element.get("faces") or {}).items():
                    if direction not in VERTICES:
                        continue
                    _, resource = resolve_texture_token(face.get("texture"), textures)
                    texture = texture_image(resource, ref.namespace)
                    u1, v1, u2, v2 = numbers(face["uv"], "uv", 4) if "uv" in face else default_uv(lo, hi, direction)
                    corners = [tuple((lo, hi)[pick == "t"][axis] for axis, pick in enumerate(code))
                               for code in VERTICES[direction]]
                    normal = NORMALS[direction]
                    if transform:
                        matrix, origin = transform
                        corners = [tuple(o + d for o, d in zip(origin, apply(matrix, tuple(c - o for c, o in zip(p, origin)))))
                                   for p in corners]
                        normal = normalize(apply(matrix, normal))
                    corners = [tuple(c / 16 for c in spin(p, 8)) for p in corners]
                    normal = spin(normal)
                    # Vertex k has u = u1 for k in {0, 1} and v = v1 for k in {0, 3}; a face rotation shifts k.
                    shift = (int(face.get("rotation", 0)) // 90) % 4
                    sx, sy = texture.width / 16, texture.height / 16
                    uv = [(((u1, u2)[(k + shift) % 4 in (2, 3)]) * sx, ((v1, v2)[(k + shift) % 4 in (1, 2)]) * sy)
                          for k in range(4)]
                    closest = max(NORMALS, key=lambda side: sum(a * b for a, b in zip(NORMALS[side], normal)))
                    cull = face.get("cullface")
                    cull = tuple(round(c) for c in spin(NORMALS[cull])) if cull in NORMALS else None
                    centre = tuple(sum(p[i] for p in corners) / 4 for i in range(3))
                    baked.faces.append((corners, uv, texture, normal, SHADE[closest] if element.get("shade", True) else 1.0,
                                        face.get("tintindex", -1) >= 0, cull, centre))
    except (ModelError, KeyError, OSError, ValueError):
        baked = None
    if baked is not None and not baked.faces:
        baked = None
    _models[state] = baked
    return baked


def block_tint(name, state):
    """The colour the game multiplies a tinted face by: the plains colours for vanilla blocks, none for ours."""
    if not state.partition("[")[0].startswith("minecraft:"):
        return TINTS.get(name, (255, 255, 255))
    return TINTS.get(name) or (FOLIAGE if name.endswith("_leaves") else GRASS)


def paint(image, corners, uv, texture, u, v, scale, view, color, opacity, pale):
    """One face of a model: its parallelogram on the sheet filled from the texture, multiplied by color
    (a pale unshaded cut face instead of it)."""
    points = [(u + dot(c, view.right) * scale, v - dot(c, view.up) * scale) for c in corners]
    left, top = math.floor(min(x for x, _ in points)), math.floor(min(y for _, y in points))
    width, height = math.ceil(max(x for x, _ in points)) - left + 1, math.ceil(max(y for _, y in points)) - top + 1
    local = [(x - left, y - top) for x, y in points]
    (x0, y0), (x1, y1), _, (x3, y3) = local
    # sheet = S0 + a (S3 - S0) + b (S1 - S0) and texture = T0 + a (T3 - T0) + b (T1 - T0): invert the first.
    ea, eb, fa, fb = x3 - x0, x1 - x0, y3 - y0, y1 - y0
    det = ea * fb - eb * fa
    if abs(det) < 1e-6:  # seen edge-on
        return
    (t0u, t0v), (t1u, t1v), _, (t3u, t3v) = uv
    coefficients = []
    for t0, t1, t3 in ((t0u, t1u, t3u), (t0v, t1v, t3v)):
        da, db = t3 - t0, t1 - t0
        cx, cy = (da * fb - db * fa) / det, (db * ea - da * eb) / det
        coefficients += [cx, cy, t0 - cx * x0 - cy * y0]
    tile = texture.transform((width, height), Image.AFFINE, coefficients, Image.NEAREST)
    mask = Image.new("L", (width, height), 0)
    ImageDraw.Draw(mask).polygon(local, fill=255, outline=255)  # the outline overlaps neighbours: no seams
    alpha = ImageChops.multiply(tile.getchannel("A"), mask)
    if opacity < 255:
        alpha = alpha.point(lambda a: a * opacity // 255)
    rgb = tile.convert("RGB")
    if pale:
        rgb = Image.blend(rgb, Image.new("RGB", rgb.size, (255, 255, 255)), 0.6)
    else:
        rgb = ImageChops.multiply(rgb, Image.new("RGB", rgb.size, color))
    image.paste(rgb, (left, top), alpha)


def scaled(rgb, factor):
    return tuple(min(255, int(c * factor)) for c in rgb)


def dot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


class View:
    """Where the camera is. It looks at the middle of the box; the projection is orthographic."""

    def __init__(self, text):
        side, _, height = text.strip().lower().partition(":")
        if side in ("top", "bottom") and not height:
            side, height = "s", side
        height = height or "above"
        try:
            yaw = SIDES[side] if side in SIDES else float(side)
            pitch = HEIGHTS[height] if height in HEIGHTS else float(height)
        except ValueError:
            sys.exit(f"--view {text}: expected <side>:<height>, e.g. ne:above, s:level, 30:45, top")
        compass = {bearing: name.upper() for name, bearing in SIDES.items()}
        side_name = compass.get(yaw % 360, f"{yaw:g} deg")
        if abs(pitch) == 90:
            # Seen from straight above, the side the camera stands on is at the bottom of the picture.
            up = compass.get((yaw + (180 if pitch > 0 else 0)) % 360, "?")
            self.label = f"{'top' if pitch > 0 else 'bottom'}, {up} up"
        else:
            self.label = f"from {side_name}, {height if height in HEIGHTS else f'{pitch:g} deg'}"
        yaw, pitch = math.radians(yaw), math.radians(pitch)
        clean = lambda vector: tuple(0.0 if abs(c) < 1e-9 else c for c in vector)
        self.to_camera = clean((math.sin(yaw) * math.cos(pitch), math.sin(pitch), -math.cos(yaw) * math.cos(pitch)))
        self.right = clean((-math.cos(yaw), 0.0, -math.sin(yaw)))
        c, r = self.to_camera, self.right
        self.up = clean((c[1] * r[2] - c[2] * r[1], c[2] * r[0] - c[0] * r[2], c[0] * r[1] - c[1] * r[0]))

    def extent(self, lo, hi):
        """Size of the projected box lo..hi in blocks: left, right, bottom, top."""
        corners = [(x, y, z) for x in (lo[0], hi[0] + 1) for y in (lo[1], hi[1] + 1) for z in (lo[2], hi[2] + 1)]
        across = [dot(corner, self.right) for corner in corners]
        along = [dot(corner, self.up) for corner in corners]
        return min(across), max(across), min(along), max(along)


class Scene:
    """The blocks of one export in a grid with one empty cell around it, so neighbours need no bounds check."""

    def __init__(self, name, seed, origin, size, textured=False):
        self.name, self.seed, self.origin, self.size = name, seed, tuple(origin), tuple(size)
        self.textured = textured
        sx, sy, sz = self.size
        self.steps = (1, (sx + 2) * (sz + 2), sx + 2)  # index step along x, y, z
        self.grid = array("H", bytes(2 * (sx + 2) * (sy + 2) * (sz + 2)))
        self.looks = [(EMPTY, None, None, None), (CUT, None, None, None)]  # grid value -> kind, colour, label, state
        self.baked = {}
        self.look_ids = {}
        self.counts = {}
        self.filled = []
        self.hanging = False
        self.clips = {}
        self.subject = None  # the box of everything the preview did not prepare: lo, hi

    def put(self, x, y, z, kind, rgb, label, prepared=False, state=None):
        if not prepared:
            lo, hi = self.subject or ((x, y, z), (x, y, z))
            self.subject = (min(lo[0], x), min(lo[1], y), min(lo[2], z)), (max(hi[0], x), max(hi[1], y), max(hi[2], z))
        key = (kind, rgb, label, state)
        look = self.look_ids.get(key)
        if look is None:
            look = self.look_ids[key] = len(self.looks)
            self.looks.append(key)
        index = x + 1 + self.steps[2] * (z + 1) + self.steps[1] * (y + 1)
        self.grid[index] = look
        self.filled.append((x, y, z, index))
        self.counts[look] = self.counts.get(look, 0) + 1

    def model(self, look):
        """The baked model of a look, or None: the block is drawn as a flat cube."""
        if look not in self.baked:
            kind, _, label, state = self.looks[look]
            self.baked[look] = bake(state) if self.textured and state and kind not in (EMPTY, CUT, WATER) else None
            if self.textured and self.baked[look] is None and state and kind not in (EMPTY, CUT, WATER):
                unmodelled.add(label)
        return self.baked[look]

    def hides(self, look):
        """Whether a block of this look hides the faces that touch it: a full cube of the solid kind."""
        model = self.model(look)
        return self.looks[look][0] == SOLID and (model.full if model else True)

    def clipped(self, lo, hi):
        """The grid with the blocks outside lo..hi cut away, and the blocks inside that can show a face."""
        key = (tuple(lo), tuple(hi))
        if key not in self.clips:
            grid = array("H", self.grid)
            inside = []
            for cell in self.filled:
                if all(lo[axis] <= cell[axis] <= hi[axis] for axis in range(3)):
                    inside.append(cell)
                else:
                    grid[cell[3]] = CUT
            kinds = [look[0] for look in self.looks]
            around = [sign * step for step in self.steps for sign in (1, -1)]
            self.clips[key] = grid, [c for c in inside if any(kinds[grid[c[3] + step]] != SOLID for step in around)]
        return self.clips[key]


def read(path):
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    if data.get("format") != "runeruin.region/1":
        sys.exit(f"{path} is not a runeruin.region/1 export")
    return data


def blocks(data):
    """Yields x, y, z, state, name, kind, prepared of every block except air, the top layer first."""
    hollow = set(data.get("noCollision", ()))
    palette = {}
    for token, state in data["palette"].items():
        name = block_name(state)
        if name in AIR:
            continue
        if name in WATER_BLOCKS:
            kind = WATER
        elif token in hollow:
            kind = WET_PLANT if name in UNDERWATER_PLANTS or "waterlogged=true" in state else HOLLOW
        else:
            kind = SOLID
        palette[token] = (state, name, kind)
    air = "".join(token for token in data["palette"] if token not in palette)
    layers, mask = data["layers"], data.get("prepared")
    for y in range(len(layers) - 1, -1, -1):
        for z, row in enumerate(layers[y]):
            if not row.strip(air):
                continue
            marks = mask[y][z] if mask else None
            for x, token in enumerate(row):
                if token in palette:
                    yield (x, y, z, *palette[token], marks is not None and marks[x] == "#")


def load_scene(path, prepared_mode, textured):
    data = read(path)
    scene = Scene(Path(path).stem, data.get("worldSeed"), data["origin"], data["size"], textured)
    covered = set()  # columns with prepared ground above the current layer
    subject = under_cover = 0
    for x, y, z, state, name, kind, prepared in blocks(data):
        if prepared:
            if kind != WATER:
                covered.add((x, z))
            if prepared_mode == "hide":
                continue
            if prepared_mode == "faint" and kind != WATER:
                kind = FAINT
        else:
            subject += 1
            under_cover += (x, z) in covered
        scene.put(x, y, z, kind, block_color(name), name, prepared, state)
    scene.hanging = under_cover * 2 > subject
    return scene


def load_diff(before_path, after_path, prepared_mode):
    """Both exports in one box, placed by their world origins. Prepared blocks are drawn as usual, not compared."""
    before, after = read(before_path), read(after_path)
    lo = [min(a, b) for a, b in zip(before["origin"], after["origin"])]
    hi = [max(a + s, b + t) for a, s, b, t in zip(before["origin"], before["size"], after["origin"], after["size"])]
    scene = Scene(f"{Path(after_path).stem} against {Path(before_path).stem}", after.get("worldSeed"), lo,
                  [h - l for h, l in zip(hi, lo)])
    shift = [o - l for o, l in zip(before["origin"], lo)]
    old = {}
    for x, y, z, state, _, kind, prepared in blocks(before):
        if not prepared:
            old[(x + shift[0], y + shift[1], z + shift[2])] = (state, kind)
    shift = [o - l for o, l in zip(after["origin"], lo)]
    for x, y, z, state, name, kind, prepared in blocks(after):
        pos = (x + shift[0], y + shift[1], z + shift[2])
        if prepared:
            if prepared_mode != "hide":
                faint = prepared_mode == "faint" and kind != WATER
                scene.put(*pos, FAINT if faint else kind, block_color(name), name, True)
            continue
        was = old.pop(pos, None)
        if was is None:
            scene.put(*pos, kind if kind in OUTLINED else SOLID, ADDED, "added")
        elif was[0] != state:
            scene.put(*pos, kind if kind in OUTLINED else SOLID, CHANGED, "replaced")
        elif kind == WATER:
            scene.put(*pos, kind, block_color(name), name)
        else:
            scene.put(*pos, kind, UNCHANGED, "unchanged")
    for pos in old:
        scene.put(*pos, GHOST, REMOVED, "removed")
    return scene


def tick_step(pixels_per_block, blocks):
    """Every 10 blocks as in the text slices; rarer when the labels would touch, every 5 on a short edge."""
    return next((step for step in (5, 10, 20, 50, 100) if step * pixels_per_block >= 34 and (step > 5 or blocks < 20)), 200)


def render(scene, view, lo, hi, scale):
    """One view of the box lo..hi (local block coordinates, inclusive)."""
    grid, surface = scene.clipped(lo, hi)
    looks = scene.looks
    kinds = [look[0] for look in looks]
    hides = [scene.hides(look) for look in range(len(looks))] if scene.textured else None
    left, right, bottom, top = view.extent(lo, hi)
    image = Image.new("RGB", (int((right - left) * scale) + LEFT + RIGHT, int((top - bottom) * scale) + TOP + BOTTOM), PANEL)
    draw = ImageDraw.Draw(image, "RGBA")
    camera = view.to_camera

    def pixel(point):
        return LEFT + (dot(point, view.right) - left) * scale, TOP + (top - dot(point, view.up)) * scale

    # The twelve edges of the box: axis, then the two fixed coordinates, each at lo or at hi + 1.
    edges = []
    for axis in range(3):
        b, c = [other for other in range(3) if other != axis]
        for at_b in (lo[b], hi[b] + 1):
            for at_c in (lo[c], hi[c] + 1):
                start, end, normal_b, normal_c = [0, 0, 0], [0, 0, 0], [0, 0, 0], [0, 0, 0]
                start[axis], end[axis] = lo[axis], hi[axis] + 1
                start[b] = end[b] = at_b
                start[c] = end[c] = at_c
                normal_b[b] = -1 if at_b == lo[b] else 1
                normal_c[c] = -1 if at_c == lo[c] else 1
                # An edge between two faces that look at the camera would run across the subject: left out.
                if not (dot(normal_b, camera) > 1e-6 and dot(normal_c, camera) > 1e-6):
                    draw.line([pixel(start), pixel(end)], fill=EDGE)
                edges.append((axis, start, end))

    depths = [dot((x, y, z), camera) for x in (lo[0], hi[0] + 1) for y in (lo[1], hi[1] + 1) for z in (lo[2], hi[2] + 1)]
    far, span = min(depths), (max(depths) - min(depths)) or 1
    faces = []
    for normal, corners, shade in FACES:
        if dot(normal, camera) > 1e-6:
            offsets = [(dot(corner, view.right) * scale, -dot(corner, view.up) * scale) for corner in corners]
            faces.append((dot(normal, scene.steps), offsets, shade))
    # A view that does not see the top would be dark: its brightest face gets the full colour instead.
    brightest = max(shade for _, _, shade in faces)
    faces = [(step, offsets, shade / brightest) for step, offsets, shade in faces]
    grid_lines = scale >= 6

    ordered = {}  # look -> the faces of its model that look towards the camera, far ones first

    # Far blocks first: the nearer ones cover them, the translucent ones blend over them.
    for x, y, z, index in sorted(surface, key=lambda cell: dot(cell, camera)):
        look = grid[index]
        kind, rgb, label, state = looks[look]
        model = scene.model(look) if scene.textured else None
        fog = FAR_SHADE + (1 - FAR_SHADE) * (dot((x, y, z), camera) - far) / span
        u, v = pixel((x, y, z))
        for step, offsets, shade in faces:
            if model is not None and kind != WET_PLANT:
                break
            other = kinds[grid[index + step]]
            if other == SOLID if hides is None else hides[grid[index + step]]:
                continue
            points = [(u + du, v + dv) for du, dv in offsets]
            if kind == SOLID:
                fill = tuple((c * 3 + 255 * 2) // 5 for c in rgb) if other == CUT else scaled(rgb, shade * fog)
                draw.polygon(points, fill=fill, outline=scaled(fill, 0.82) if grid_lines else fill)
                continue
            if kind != HOLLOW and GROUP[other] != GROUP[kind]:
                body = WATER_RGB if kind == WET_PLANT else rgb
                draw.polygon(points, fill=(*scaled(body, shade * fog), ALPHA[kind]))
            if kind in OUTLINED and other not in OUTLINED and model is None:
                draw.polygon(points, outline=tuple((c * 2 + 255) // 3 for c in rgb), width=2 if scale >= 14 else 1)
        if model is None:
            continue
        if look not in ordered:
            ordered[look] = sorted((face for face in model.faces if dot(face[3], camera) > 1e-6),
                                   key=lambda face: dot(face[7], camera))
        tint = block_tint(label, state)
        opacity = ALPHA[kind] if kind == FAINT else 255
        for corners, uv, texture, normal, shade, tinted, cull, _ in ordered[look]:
            near = grid[index + sum(c * step for c, step in zip(cull, scene.steps))] if cull else 0
            if hides[near]:
                continue
            light = shade / brightest * fog
            color = tuple(round(c * light) for c in (tint if tinted else (255, 255, 255)))
            paint(image, corners, uv, texture, u, v, scale, view, color, opacity, near == CUT)

    written = []

    def write(x, y, text):
        box = (x, y, x + draw.textlength(text, font=FONT), y + 13)
        if all(box[2] < o[0] or o[2] < box[0] or box[3] < o[1] or o[3] < box[1] for o in written):
            written.append(box)
            draw.text((x, y), text, fill=DIM, font=FONT)

    # Coordinates on one edge per axis: the lowest one when the axis runs across the picture, else the leftmost.
    for axis in range(3):
        du, dv = view.right[axis] * scale, -view.up[axis] * scale
        length = math.hypot(du, dv)
        if length * (hi[axis] + 1 - lo[axis]) < 12:
            continue
        across = abs(du) >= abs(dv)

        def outermost(edge):
            (u1, v1), (u2, v2) = pixel(edge[1]), pixel(edge[2])
            return (round(v1 + v2, 2), dot(edge[1], camera)) if across else (round(-u1 - u2, 2), dot(edge[1], camera))

        _, start, end = max((edge for edge in edges if edge[0] == axis), key=outermost)
        draw.line([pixel(start), pixel(end)], fill=AXIS)
        step = tick_step(length, hi[axis] + 1 - lo[axis])
        for value in range(-(-lo[axis] // step) * step, hi[axis] + 1, step):
            point = list(start)
            point[axis] = value + 0.5
            u, v = pixel(point)
            label = "xyz"[axis] + str(value)
            if across:
                draw.line([(u, v), (u, v + 4)], fill=AXIS)
                write(u - draw.textlength(label, font=FONT) / 2, v + 5, label)
            else:
                draw.line([(u - 4, v), (u, v)], fill=AXIS)
                write(u - 6 - draw.textlength(label, font=FONT), v - 8, label)
    draw.text((6, 4), view.label, fill=TEXT, font=FONT)
    return image


def clip(scene, view, box, cut):
    if box is None and scene.subject:
        box = [v - 4 for v in scene.subject[0]] + [v + 4 for v in scene.subject[1]]
    lo, hi = (list(box[:3]), list(box[3:])) if box else ([0, 0, 0], list(scene.size))
    lo, hi = [max(0, v) for v in lo], [min(s - 1, v) for s, v in zip(scene.size, hi)]
    if cut:
        axis, value = cut
        if value is None:
            value = int((2 * scene.origin[axis] + scene.size[axis] - 1) / 2) - scene.origin[axis]
        if view.to_camera[axis] >= 0:
            hi[axis] = min(hi[axis], value)
        else:
            lo[axis] = max(lo[axis], value)
    return lo, hi


def legend(scenes):
    """The most frequent looks over all scenes: colour, text."""
    totals = {}
    for scene in scenes:
        for look, count in scene.counts.items():
            kind, rgb, label, _ = scene.looks[look]
            key = (label + ({FAINT: SUFFIX[FAINT]} if scene.textured else SUFFIX).get(kind, ""), rgb)
            totals[key] = totals.get(key, 0) + count
    return [(rgb, f"{label} {count}") for (label, rgb), count in sorted(totals.items(), key=lambda item: -item[1])[:14]]


def sheet(panels, columns, header, captions, entries):
    """Panels (rows of images) under a header, with a caption below each and the legend at the bottom."""
    measure = ImageDraw.Draw(Image.new("RGB", (1, 1)))
    cell_w = max(image.width for image in panels)
    cell_h = max(image.height for image in panels) + (LINE if captions else 0)
    rows = -(-len(panels) // columns)
    width = max(columns * (cell_w + GAP) + GAP, int(measure.textlength(header, font=FONT)) + 2 * GAP)
    # Legend entries wrap to the width of the sheet.
    placed, x, y = [], GAP, 0
    for rgb, text in entries:
        entry_width = 18 + int(measure.textlength(text, font=FONT)) + 14
        if x + entry_width > width and x > GAP:
            x, y = GAP, y + LINE
        placed.append((x, y, rgb, text))
        x += entry_width
    legend_height = y + LINE if placed else 0
    image = Image.new("RGB", (width, LINE + GAP + rows * (cell_h + GAP) + legend_height + GAP), SHEET)
    draw = ImageDraw.Draw(image)
    draw.text((GAP, GAP // 2), header, fill=TEXT, font=FONT)
    for i, panel in enumerate(panels):
        x, y = GAP + (i % columns) * (cell_w + GAP), LINE + GAP + (i // columns) * (cell_h + GAP)
        image.paste(panel, (x, y))
        if captions:
            draw.text((x + 6, y + panel.height + 2), captions[i], fill=TEXT, font=FONT)
    top = LINE + GAP + rows * (cell_h + GAP)
    for x, y, rgb, text in placed:
        draw.rectangle([x, top + y + 2, x + 12, top + y + 14], fill=rgb, outline=EDGE)
        draw.text((x + 18, top + y), text, fill=DIM, font=FONT)
    return image


def fit(extents, columns, size, captions):
    """Pixels per block at which the panels fill a sheet of that size."""
    rows = -(-len(extents) // columns)
    wide = max(right - left for left, right, _, _ in extents)
    tall = max(top - bottom for _, _, bottom, top in extents)
    across = (size - GAP - columns * (LEFT + RIGHT + GAP)) / (columns * wide)
    down = (size - 3 * LINE - 2 * GAP - rows * (TOP + BOTTOM + GAP + (LINE if captions else 0))) / (rows * tall)
    return min(across, down)


def natural(path):
    return [int(part) if part.isdigit() else part for part in re.split(r"(\d+)", path)]


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("output")
    parser.add_argument("exports", nargs="+")
    parser.add_argument("--view")
    parser.add_argument("--box")
    parser.add_argument("--cut")
    parser.add_argument("--prepared", choices=("faint", "hide", "solid"), default="faint")
    parser.add_argument("--diff")
    parser.add_argument("--flat", action="store_true")
    parser.add_argument("--size", type=int, default=1500)
    parser.add_argument("--scale", type=float)
    args = parser.parse_args()

    # A pattern such as preview_boulder_s*.json also matches the midplane slices preview_boulder_s1_xy.json.
    paths = [path for pattern in args.exports for path in (sorted(glob.glob(pattern), key=natural) or [pattern])
             if path == pattern or not re.search(r"_(xy|xz|yz)\.json$", path)]
    if args.diff and len(paths) != 1:
        sys.exit("--diff compares one export with the one before: pass a single export")
    textured = not (args.flat or args.diff)
    scenes = [load_diff(args.diff, paths[0], args.prepared)] if args.diff else [load_scene(p, args.prepared, textured) for p in paths]
    box = None
    if args.box == "all":
        box = []
    elif args.box:
        box = [int(v) for v in args.box.split(",")]
        if len(box) != 6:
            sys.exit("--box needs x1,y1,z1,x2,y2,z2 or all")
        box = [min(box[i], box[i + 3]) for i in range(3)] + [max(box[i], box[i + 3]) for i in range(3)]
    cut = None
    if args.cut:
        axis, _, value = args.cut.lower().partition("=")
        if axis not in ("x", "y", "z"):
            sys.exit("--cut needs x, y or z, optionally with a coordinate: --cut x=12")
        cut = ("xyz".index(axis), int(value) if value else None)

    hanging = all(scene.hanging for scene in scenes)
    columns = None
    if args.view:
        views = args.view
    elif cut:
        views = CUT_VIEWS[cut[0]]
    elif len(scenes) > 1:
        views = "se:below" if hanging else "se:above"
    else:
        views = "ne:above,se:above,sw:above,nw:above" + (",ne:below,se:below,sw:below,nw:below" if hanging else "")
        columns = 4 if hanging else None
    views = [View(text) for text in views.split(",")]
    if len(scenes) > 1 and len(views) > 1:
        columns = len(views)

    jobs = [(scene, view, *clip(scene, view, box, cut)) for scene in scenes for view in views]
    extents = [view.extent(lo, hi) for _, view, lo, hi in jobs]
    captions = None
    if len(scenes) > 1:
        captions = [f"seed {scene.seed}  {scene.name}" for scene, _, _, _ in jobs]
    if columns is None:
        columns = max(range(1, len(jobs) + 1), key=lambda count: fit(extents, count, args.size, captions))
    scale = args.scale or max(1.0, min(MAX_SCALE, fit(extents, columns, args.size, captions)))

    panels = [render(scene, view, lo, hi, scale) for scene, view, lo, hi in jobs]
    first = scenes[0]
    header = [first.name, f"seed {first.seed}", "size " + " x ".join(map(str, first.size)),
              "origin " + " ".join(map(str, first.origin))] if len(scenes) == 1 else [f"{len(scenes)} exports"]
    header.append(f"{scale:.1f} px/block")
    if box:
        header.append("box " + args.box)
    if cut:
        header.append("cut " + args.cut)
    if args.prepared != "faint":
        header.append("prepared blocks: " + args.prepared)
    image = sheet(panels, columns, "   ".join(header), captions, legend(scenes))
    Path(args.output).parent.mkdir(parents=True, exist_ok=True)
    image.save(args.output)
    print(f"{args.output}: {image.width}x{image.height}, {scale:.1f} px/block, views: " + "; ".join(v.label for v in views))
    if unmodelled:
        print("no model found, drawn as a cube of the average colour: " + ", ".join(sorted(unmodelled)))
    if untextured and not textured:
        print("no texture found, colour picked from the name: " + ", ".join(sorted(untextured)))


if __name__ == "__main__":
    main()
