#!/usr/bin/env python3
"""Render a block or item model JSON to a PNG: two isometric views, the south face and the top.

    python scripts/render_model.py out.png runeruin:block/stone_lily [--scale 16] [--tint 91BD59]
    python scripts/render_model.py out.png src/generated/resources/assets/runeruin/models/block/x.json

A bare name (stone_lily) tries block/ then item/. Parents and textures resolve through src/main,
src/generated and .vanilla-textures (vanilla models included). Follows the game's baking: face vertex
order and UV rotation as in FaceBakery, element rotation with rescale, directional shade
(up 1.0, north/south 0.8, east/west 0.6, down 0.5). Faces with a tintindex are multiplied by --tint.
Item models built on item/generated show their layers as a flat sprite. Blockstate rotations, cullfaces,
emissive light and render types are ignored: the PNG is for judging geometry and UV placement.
"""
import argparse
import json
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / ".agents/skills/minecraft-model-texture-analysis/scripts"))
from extract_model_faces import (  # noqa: E402
    ModelError,
    default_uv,
    discover_assets_roots,
    load_model,
    model_ref_from_argument,
    numbers,
    parse_identifier,
    resolve_model_file,
    resolve_texture_file,
    resolve_texture_token,
)

BACKGROUND = (48, 48, 48)
LABEL_HEIGHT = 14
GAP = 8
SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}
NORMALS = {"up": (0, 1, 0), "down": (0, -1, 0), "north": (0, 0, -1), "south": (0, 0, 1),
           "west": (-1, 0, 0), "east": (1, 0, 0)}
# FaceInfo: per face, the corners of vertices 0..3 as (x, y, z) picks from (from, to).
VERTICES = {
    "down": ("ftt", "fff", "tff", "tft"),
    "up": ("ftf", "ftt", "ttt", "ttf"),
    "north": ("ttf", "tff", "fff", "ftf"),
    "south": ("ftt", "fft", "tft", "ttt"),
    "west": ("ftf", "fff", "fft", "ftt"),
    "east": ("ttt", "tft", "tff", "ttf"),
}
# Camera directions (towards the viewer): +x east, +y up, +z south.
VIEWS = {
    "iso south-east": (1, 0.8165, 1),
    "iso north-west": (-1, 0.8165, -1),
    "south": (0, 0, 1),
    "top": (0, 1, 0),
}


def normalize(v):
    length = math.sqrt(sum(c * c for c in v))
    return tuple(c / length for c in v)


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def dot(a, b):
    return sum(x * y for x, y in zip(a, b))


def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def apply(m, v):
    return tuple(sum(m[i][k] * v[k] for k in range(3)) for i in range(3))


def axis_rotation(axis, degrees):
    c, s = math.cos(math.radians(degrees)), math.sin(math.radians(degrees))
    if axis == "x":
        return [[1, 0, 0], [0, c, -s], [0, s, c]]
    if axis == "y":
        return [[c, 0, s], [0, 1, 0], [-s, 0, c]]
    return [[c, -s, 0], [s, c, 0], [0, 0, 1]]


def element_transform(rotation):
    """CuboidRotation: one axis and angle, or Euler x/y/z applied as rotationZYX; rescale stretches back."""
    if not isinstance(rotation, dict):
        return None
    if "axis" in rotation:
        matrix = axis_rotation(rotation["axis"], float(rotation.get("angle", 0)))
    else:
        matrix = mat_mul(mat_mul(axis_rotation("z", float(rotation.get("z", 0))),
                                 axis_rotation("y", float(rotation.get("y", 0)))),
                         axis_rotation("x", float(rotation.get("x", 0))))
    if rotation.get("rescale"):
        scale = [1 / max(abs(c) for c in apply(matrix, unit)) for unit in ((1, 0, 0), (0, 1, 0), (0, 0, 1))]
        matrix = [[matrix[i][j] * scale[j] for j in range(3)] for i in range(3)]
    origin = numbers(rotation.get("origin", [8, 8, 8]), "rotation origin", 3)
    return matrix, origin


def load_texture(resource, namespace, roots, cache):
    if resource not in cache:
        path, _ = resolve_texture_file(resource, namespace, roots)
        image = Image.open(path).convert("RGBA")
        if image.height > image.width and image.height % image.width == 0:
            image = image.crop((0, 0, image.width, image.width))
        cache[resource] = image
    return cache[resource]


def texel_quads(model, ref, roots, tint):
    """One (corners, rgba, normal) per visible texel of every face, in model units 0..16."""
    quads, cache = [], {}
    for index, element in enumerate(model["elements"]):
        lo = numbers(element.get("from"), f"element {index} from", 3)
        hi = numbers(element.get("to"), f"element {index} to", 3)
        transform = element_transform(element.get("rotation"))
        shade = element.get("shade", True)
        for direction, face in (element.get("faces") or {}).items():
            if direction not in VERTICES:
                continue
            _, resource = resolve_texture_token(face.get("texture"), model["textures"])
            texture = load_texture(resource, ref.namespace, roots, cache)
            u1, v1, u2, v2 = numbers(face["uv"], "uv", 4) if "uv" in face else default_uv(lo, hi, direction)
            corners = [tuple((lo, hi)[pick == "t"][axis] for axis, pick in enumerate(code))
                       for code in VERTICES[direction]]
            normal = NORMALS[direction]
            if transform:
                matrix, origin = transform
                corners = [tuple(o + d for o, d in zip(origin, apply(matrix, tuple(c - o for c, o in zip(p, origin)))))
                           for p in corners]
                normal = normalize(apply(matrix, normal))
            # CuboidFace.UVs: vertex k has u = u1 for k in {0, 1}, v = v1 for k in {0, 3}; rotation shifts k.
            shift = (int(face.get("rotation", 0)) // 90) % 4
            uv = [((u1, u2)[(k + shift) % 4 in (2, 3)], (v1, v2)[(k + shift) % 4 in (1, 2)]) for k in range(4)]
            closest = max(NORMALS, key=lambda name: dot(NORMALS[name], normal))
            light = SHADE[closest] if shade else 1.0
            tinted = face.get("tintindex", -1) >= 0
            quads.extend(face_texels(texture, corners, uv, normal, light, tint if tinted else None))
    return quads


def face_texels(texture, corners, uv, normal, light, tint):
    """Split a face into texel quads. The face is a parallelogram: uv is affine in (a, b) along
    vertex 0 -> 3 and vertex 0 -> 1, so each texel's uv corners invert to points on the face."""
    p0, p1, _, p3 = corners
    (u0, v0), (u_b, v_b), _, (u_a, v_a) = uv
    du_a, dv_a, du_b, dv_b = u_a - u0, v_a - v0, u_b - u0, v_b - v0
    det = du_a * dv_b - du_b * dv_a
    if abs(det) < 1e-9:
        return []
    sx, sy = texture.width / 16, texture.height / 16
    us, vs = [u for u, _ in uv], [v for _, v in uv]
    left, right = math.floor(min(us) * sx + 1e-6), math.ceil(max(us) * sx - 1e-6)
    top, bottom = math.floor(min(vs) * sy + 1e-6), math.ceil(max(vs) * sy - 1e-6)

    def point(u, v):
        a = ((u - u0) * dv_b - (v - v0) * du_b) / det
        b = (du_a * (v - v0) - dv_a * (u - u0)) / det
        return tuple(o + a * (e3 - o) + b * (e1 - o) for o, e1, e3 in zip(p0, p1, p3))

    pixels = texture.load()
    texels = []
    for py in range(max(top, 0), min(bottom, texture.height)):
        for px in range(max(left, 0), min(right, texture.width)):
            r, g, b, alpha = pixels[px, py]
            if alpha == 0:
                continue
            if tint:
                r, g, b = (c * t // 255 for c, t in zip((r, g, b), tint))
            ua, ub = max(px / sx, min(us)), min((px + 1) / sx, max(us))
            va, vb = max(py / sy, min(vs)), min((py + 1) / sy, max(vs))
            quad = [point(ua, va), point(ub, va), point(ub, vb), point(ua, vb)]
            color = (round(r * light), round(g * light), round(b * light), alpha)
            texels.append((quad, color, normal))
    return texels


def render_view(quads, camera, scale):
    camera = normalize(camera)
    right = normalize(cross((0, 1, 0), camera)) if abs(camera[1]) < 0.999 else (1, 0, 0)
    up = cross(camera, right)
    visible = [(quad, color) for quad, color, normal in quads if dot(normal, camera) > 1e-6]
    projected = [([(dot(p, right) * scale, -dot(p, up) * scale) for p in quad], color,
                  sum(dot(p, camera) for p in quad) / 4) for quad, color in visible]
    # A view with no faces towards the camera (the top of a cross) keeps the size of one block face.
    xs = [x for points, _, _ in projected for x, _ in points] or [0, 16 * scale]
    ys = [y for points, _, _ in projected for _, y in points] or [0, 16 * scale]
    size = (math.ceil(max(xs) - min(xs)) + 2 * GAP, math.ceil(max(ys) - min(ys)) + 2 * GAP)
    image = Image.new("RGB", size, BACKGROUND)
    draw = ImageDraw.Draw(image, "RGBA")
    for points, color, _ in sorted(projected, key=lambda item: item[2]):
        polygon = [(x - min(xs) + GAP, y - min(ys) + GAP) for x, y in points]
        draw.polygon(polygon, fill=color, outline=color if color[3] == 255 else None)
    return image


def sprite_layers(ref, roots):
    """Layer textures when the parent chain ends in builtin/generated, else None."""
    textures, current = {}, ref
    while True:
        path = resolve_model_file(current, roots)
        if path is None:
            return None
        data = json.loads(path.read_text(encoding="utf-8-sig"))
        textures = {**data.get("textures", {}), **textures}
        parent = data.get("parent")
        if not isinstance(parent, str):
            return None
        if parse_identifier(parent).path == "builtin/generated":
            return [textures[f"layer{i}"] for i in range(5) if f"layer{i}" in textures]
        current = parse_identifier(parent)


def render_sprite(layers, ref, roots, scale):
    cache = {}
    images = [load_texture(layer, ref.namespace, roots, cache) for layer in layers]
    sprite = Image.new("RGBA", images[0].size, (0, 0, 0, 0))
    for image in images:
        sprite.alpha_composite(image.resize(sprite.size, Image.NEAREST))
    sprite = sprite.resize((sprite.width * scale, sprite.height * scale), Image.NEAREST)
    image = Image.new("RGB", (sprite.width + 2 * GAP, sprite.height + 2 * GAP), BACKGROUND)
    image.paste(sprite, (GAP, GAP), sprite)
    return [("sprite", image)]


def resolve_ref(name, roots):
    ref, file = model_ref_from_argument(name)
    if file is None and ":" not in name and "/" not in name:
        for prefix in ("block/", "item/"):
            candidate = parse_identifier(f"runeruin:{prefix}{name}")
            if resolve_model_file(candidate, roots):
                return candidate
        raise ModelError(f"No model runeruin:block/{name} or runeruin:item/{name}; pass a full id or a path")
    return ref


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("output", type=Path)
    parser.add_argument("model", help="model id (runeruin:block/x), bare name, or path to a model JSON")
    parser.add_argument("--scale", type=int, default=16, help="screen pixels per model unit (1/16 block), default 16")
    parser.add_argument("--tint", default="91BD59", help="RRGGBB for faces with a tintindex, default grass 91BD59")
    args = parser.parse_args()
    tint = tuple(int(args.tint[i:i + 2], 16) for i in (0, 2, 4))

    try:
        _, file = model_ref_from_argument(args.model)
        roots = discover_assets_roots(file, [ROOT / "src/generated/resources/assets", ROOT / "src/main/resources/assets",
                                             ROOT / ".vanilla-textures/assets"])
        ref = resolve_ref(args.model, roots)
        layers = sprite_layers(ref, roots)
        if layers:
            panels = render_sprite(layers, ref, roots, args.scale)
        else:
            model = load_model(ref, roots)
            if not isinstance(model.get("elements"), list):
                raise ModelError(f"{ref.identifier} has no elements and is not built on item/generated")
            quads = texel_quads(model, ref, roots, tint)
            panels = [(name, render_view(quads, camera, args.scale)) for name, camera in VIEWS.items()]
    except ModelError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1

    width = sum(image.width for _, image in panels) + GAP * (len(panels) + 1)
    height = max(image.height for _, image in panels) + LABEL_HEIGHT + GAP * 2 + 4
    sheet = Image.new("RGB", (width, height), BACKGROUND)
    draw = ImageDraw.Draw(sheet)
    draw.text((GAP, 2), ref.identifier, fill=(230, 230, 230))
    x = GAP
    for name, image in panels:
        draw.text((x, LABEL_HEIGHT), name, fill=(170, 170, 170))
        sheet.paste(image, (x, LABEL_HEIGHT + GAP + 4))
        x += image.width + GAP
    args.output.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(args.output)
    print(f"wrote {args.output} ({sheet.width}x{sheet.height})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
