#!/usr/bin/env python3
"""Render an entity model with a texture to a PNG: two isometric views, the front, a side and the top.

    python scripts/render_entity_model.py out.png snail runeruin:entity/snail/snail
    python scripts/render_entity_model.py out.png minecraft:minecart runeruin:entity/minecart/runic_minecart
    python scripts/render_entity_model.py out.png minecraft:creeper exports/new_charge.png --layer armor
    python scripts/render_entity_model.py out.png minecraft:cow entity/cow/cow_temperate --view 20,10 160,45 300,-60
    python scripts/render_entity_model.py out.png minecraft:cow entity/cow/cow_temperate --pose head.xRot=30 right_front_leg.xRot=-40

Entity models are built in Java, so the geometry comes from the game: `gradlew runData` writes every model layer,
vanilla and ours, to build/entity_models/<namespace>/<model>/<layer>.json (EntityModelExport), the parts with
their initial poses and the faces of their boxes as ModelPart builds them. Run it again after a change to a model.
A bare model name tries runeruin: then minecraft:. The texture is an id resolved through src/main and
.vanilla-textures, or a path to a PNG.

The model stands as the game draws it, turned over from the model space where Y points down; the front is -Z,
where a mob faces. --view replaces the five views with cameras at yaw,pitch in degrees: yaw goes around the model
from the front (0) over its right side (90), the back (180) and its left side (270), pitch is the height of the
camera, 90 straight above and -90 straight below. The default views are 45,30 225,-30 0,0 90,0 and 180,90.

Parts stand in their initial pose, without animation. --pose changes it as an animation would, by the fields of
ModelPart: <part>.<field>=<value> sets and += adds to x, y, z (1/16 block), xRot, yRot, zRot (degrees here,
radians in the game) or xScale, yScale, zScale. A part is its name or its path (body/head), the root is root; its
children move with it. An unknown part lists the parts of the model.

Faces turned away from the camera are dropped by the game's own normals; shade is by direction as in
render_model.py, not the game's entity lighting.
"""
import argparse
import json
import math
import re
import sys
from pathlib import Path

from PIL import Image

from render_model import NORMALS, ROOT, SHADE, apply, axis_rotation, cross, dot, face_texels, mat_mul, normalize
from render_model import render_view, save_sheet

sys.path.insert(0, str(ROOT / ".agents/skills/minecraft-model-texture-analysis/scripts"))
from extract_model_faces import ModelError, discover_assets_roots, resolve_texture_file  # noqa: E402

MODELS = ROOT / "build/entity_models"
# Cameras as (yaw, pitch) in degrees.
VIEWS = {
    "iso front": (45, 30),
    "iso back, from below": (225, -30),
    "front": (0, 0),
    "side": (90, 0),
    "top": (180, 90),
}
# ModelPart fields in the order of a part's "pose".
FIELDS = ("x", "y", "z", "xRot", "yRot", "zRot", "xScale", "yScale", "zScale")
POSE_CHANGE = re.compile(r"([\w/]+)\.(\w+)(\+?=)(-?\d+(?:\.\d+)?)")


def find_model(name, layer):
    for identifier in [name] if ":" in name else [f"runeruin:{name}", f"minecraft:{name}"]:
        directory = MODELS / identifier.replace(":", "/")
        if (directory / f"{layer}.json").is_file():
            return identifier, directory / f"{layer}.json"
        if directory.is_dir():
            layers = ", ".join(sorted(file.stem for file in directory.glob("*.json")))
            raise ModelError(f"{identifier} has no layer {layer}; its layers: {layers}")
    raise ModelError(f"No entity model {name} in {MODELS}; `gradlew runData` writes them")


def load_texture(name):
    if Path(name).is_file():
        return name, Image.open(name).convert("RGBA")
    roots = discover_assets_roots(None, [ROOT / "src/main/resources/assets", ROOT / ".vanilla-textures/assets"])
    path, identifier = resolve_texture_file(name, "minecraft", roots)
    return identifier, Image.open(path).convert("RGBA")


def pose_changes(parts, arguments):
    """Per part path, (index into the pose, whether to add, value) for every --pose change."""
    changes = {}
    for text in " ".join(arguments).replace(",", " ").split():
        match = POSE_CHANGE.fullmatch(text)
        if not match or match[2] not in FIELDS:
            raise ModelError(f"--pose {text}: expected <part>.<field>=<value> with a field of {', '.join(FIELDS)}")
        name, field, operator, value = match.groups()
        suffix = "/" + name.strip("/")
        paths = [""] if name == "root" else [path for path in parts
                                             if path == suffix or ("/" not in name and path.endswith(suffix))]
        if len(paths) != 1:
            known = ", ".join(path.strip("/") or "root" for path in parts)
            raise ModelError(f"--pose {text}: {len(paths)} parts are called {name}; the parts: {known}")
        value = math.radians(float(value)) if field.endswith("Rot") else float(value)
        changes.setdefault(paths[0], []).append((FIELDS.index(field), operator == "+=", value))
    return changes


def posed_parts(parts, changes):
    """ModelPart.translateAndRotate down the tree: per part path, the matrix and the offset that take the
    corners of its boxes into model space, in 1/16 block. Parents come before their children."""
    posed = {}
    for path, part in parts.items():
        pose = list(part["pose"])
        for index, add, value in changes.get(path, ()):
            pose[index] = value + (pose[index] if add else 0)
        matrix, offset = posed[path.rpartition("/")[0]] if path else ([[1, 0, 0], [0, 1, 0], [0, 0, 1]], (0, 0, 0))
        offset = tuple(o + d for o, d in zip(offset, apply(matrix, pose[:3])))
        x, y, z = (axis_rotation(axis, math.degrees(angle)) for axis, angle in zip("xyz", pose[3:6]))
        rotation = mat_mul(mat_mul(z, y), x)
        posed[path] = mat_mul(matrix, [[rotation[i][j] * pose[6 + j] for j in range(3)] for i in range(3)]), offset
    return posed


def check_against_game(parts):
    """The game wrote the matrix it gives every part in the initial pose: ours has to be the same."""
    for path, (matrix, offset) in posed_parts(parts, {}).items():
        ours = [value for row, o in zip(matrix, offset) for value in (*row, o / 16)]
        if any(abs(a - b) > 1e-4 for a, b in zip(ours, parts[path].get("matrix", ours))):
            raise ModelError(f"part {path.strip('/') or 'root'} is posed differently from the game: "
                             "posed_parts no longer follows ModelPart.translateAndRotate")


def texel_quads(parts, posed, texture):
    """One (corners, rgba, normal) per visible texel of every face, in model units (1/16 block)."""
    quads = []
    for path, part in parts.items():
        matrix, offset = posed[path]
        # Normals go through the inverse transpose, as in PoseStack: the cofactors with the sign of the determinant.
        cofactors = [cross(matrix[(i + 1) % 3], matrix[(i + 2) % 3]) for i in range(3)]
        determinant = dot(matrix[0], cofactors[0])
        if determinant == 0:
            continue
        for quad in part["quads"]:
            # The renderers draw a model under scale(-1, -1, 1), which turns it over: Y points down in model space.
            corners = [(-x, -y, z) for x, y, z in
                       (tuple(c + o for c, o in zip(apply(matrix, vertex[:3]), offset)) for vertex in quad["vertices"])]
            uv = [(u * 16, v * 16) for _, _, _, u, v in quad["vertices"]]
            x, y, z = normalize(apply(cofactors, quad["normal"]))
            normal = tuple(math.copysign(1, determinant) * c for c in (-x, -y, z))
            light = SHADE[max(NORMALS, key=lambda name: dot(NORMALS[name], normal))]
            quads.extend(face_texels(texture, corners, uv, normal, light, None))
    return quads


def view(text):
    try:
        yaw, pitch = (float(angle) for angle in text.split(","))
    except ValueError:
        raise argparse.ArgumentTypeError(f"{text}: expected yaw,pitch in degrees, such as 120,20") from None
    return yaw, pitch


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("output", type=Path)
    parser.add_argument("model", help="model id (minecraft:minecart) or bare name")
    parser.add_argument("texture", help="texture id (runeruin:entity/snail/snail) or path to a PNG")
    parser.add_argument("--layer", default="main", help="model layer, default main")
    parser.add_argument("--view", type=view, nargs="+", metavar="YAW,PITCH", help="cameras instead of the five views")
    parser.add_argument("--pose", nargs="+", default=[], metavar="PART.FIELD=VALUE", help="changes to the initial pose")
    parser.add_argument("--scale", type=int, default=16, help="screen pixels per model unit (1/16 block), default 16")
    args = parser.parse_args()

    try:
        identifier, file = find_model(args.model, args.layer)
        texture_name, texture = load_texture(args.texture)
        parts = json.loads(file.read_text(encoding="utf-8"))["parts"]
        check_against_game(parts)
        quads = texel_quads(parts, posed_parts(parts, pose_changes(parts, args.pose)), texture)
    except ModelError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    views = {f"yaw {yaw:g}, pitch {pitch:g}": (yaw, pitch) for yaw, pitch in args.view} if args.view else VIEWS
    panels = []
    for name, (yaw, pitch) in views.items():
        yaw, pitch = math.radians(yaw), math.radians(pitch)
        camera = (math.sin(yaw) * math.cos(pitch), math.sin(pitch), -math.cos(yaw) * math.cos(pitch))
        panels.append((name, render_view(quads, camera, args.scale, (-math.cos(yaw), 0, -math.sin(yaw)))))
    title = f"{identifier}#{args.layer}  {texture_name}" + (f"  pose: {' '.join(args.pose)}" if args.pose else "")
    save_sheet(args.output, title, panels)
    return 0


if __name__ == "__main__":
    sys.exit(main())
