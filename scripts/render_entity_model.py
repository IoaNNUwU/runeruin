#!/usr/bin/env python3
"""Render an entity model with a texture to a PNG: two isometric views, the front, a side and the top; or play
one of its animations into a GIF.

    python scripts/render_entity_model.py out.png snail runeruin:entity/snail/snail
    python scripts/render_entity_model.py out.png minecraft:minecart runeruin:entity/minecart/runic_minecart
    python scripts/render_entity_model.py out.png minecraft:creeper exports/new_charge.png --layer armor
    python scripts/render_entity_model.py out.png minecraft:cow entity/cow/cow_temperate --view 20,10 160,45 300,-60
    python scripts/render_entity_model.py out.png minecraft:cow entity/cow/cow_temperate --pose head.xRot=30 right_front_leg.xRot=-40
    python scripts/render_entity_model.py out.png minecraft:sheep entity/sheep/sheep --layer main wool=entity/sheep/sheep_wool
    python scripts/render_entity_model.py out.gif mossy_warrior runeruin:entity/mossy_warrior/mossy_warrior --animation punch
    python scripts/render_entity_model.py out.gif minecraft:camel entity/camel/camel --animation CamelAnimation.CAMEL_WALK

The game builds entity models in code, vanilla ones from Java and ours from the Blockbench projects in
assets/runeruin/models/entity, so the geometry comes from the game: `gradlew runData` writes every model layer
to build/entity_models/<namespace>/<model>/<layer>.json (EntityModelExport), the parts with their initial poses
and the faces of their boxes as ModelPart builds them. Run it again after a change to a model or its project.
A bare model name tries runeruin: then minecraft:. The texture is an id resolved through src/main and
.mc-sources/textures, or a path to a PNG. --layer draws several layers of the model together, such as a sheep and its
wool: <layer>=<texture> gives a layer a texture of its own, a layer without one takes the texture argument.

The model stands as the game draws it, turned over from the model space where Y points down; the front is -Z,
where a mob faces. --view replaces the five views with cameras at yaw,pitch in degrees: yaw goes around the model
from the front (0) over its right side (90), the back (180) and its left side (270), pitch is the height of the
camera, 90 straight above and -90 straight below. The default views are 45,30 225,-30 0,0 90,0 and 180,90.

Parts stand in their initial pose, without animation. --pose changes it as an animation would, by the fields of
ModelPart: <part>.<field>=<value> sets and += adds to x, y, z (1/16 block), xRot, yRot, zRot (degrees here,
radians in the game) or xScale, yScale, zScale. A part is its name or its path (body/head), the root is root; its
children move with it. An unknown part lists the parts of the model.

--animation plays an AnimationDefinition over that pose as KeyframeAnimation.apply does: the keyframes of its
rotation, position and scale channels with linear or Catmull-Rom interpolation, read from the Java source the game
loads. A bare name is a field of the class Blockbench exports for a project of ours (<model_identifier>Animation);
<Class>.<FIELD> finds any class in src/main/java or the Minecraft sources, vanilla ones in
net/minecraft/client/animation/definitions. Into a GIF it plays at --fps, 20 by default as the game ticks, and an
animation that does not loop rests a second on its last frame; into a PNG it is --frames still frames from the
first to the last, a row per view. Every frame of a view keeps one frame on the screen, so the model does not jump.
The views are iso front and side unless --view says otherwise, the scale 8. The animation plays at full strength:
the game scales a walk by the speed of the mob and turns the head to its target in code.

Faces turned away from the camera are dropped by the game's own normals; shade is by direction as in
render_model.py, not the game's entity lighting. Under the views every texture gets a row with its visible colors
from dark to light.
"""
import argparse
import json
import math
import re
import sys
from pathlib import Path

from PIL import Image

from render_model import BACKGROUND, NORMALS, ROOT, SHADE, add_palettes, apply, axis_rotation, bounds, cross, dot
from render_model import draw_view, face_texels, mat_mul, normalize, project, save_image, sheet

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
ANIMATION_VIEWS = {"iso front": (45, 30), "side": (90, 0)}
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
    roots = discover_assets_roots(None, [ROOT / "src/main/resources/assets", ROOT / ".mc-sources/textures/assets"])
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


def find_animation(identifier, name):
    """The AnimationDefinition <Class>.<FIELD>, or the field of the class that Blockbench exports for a project of
    ours: client/model/<model_identifier>Animation.java."""
    if "." in name:
        class_name, field = name.rsplit(".", 1)
    else:
        project = ROOT / f"src/main/resources/assets/runeruin/models/entity/{identifier.partition(':')[2]}.bbmodel"
        if not identifier.startswith("runeruin:") or not project.is_file():
            raise ModelError(f"--animation {name}: {identifier} has no Blockbench project; pass <Class>.<FIELD>")
        class_name = json.loads(project.read_text(encoding="utf-8"))["model_identifier"] + "Animation"
        field = name
    files = [file for folder in (ROOT / "src/main/java", ROOT / ".mc-sources/code/net/minecraft/client/animation")
             for file in folder.rglob(f"{class_name}.java")]
    if not files:
        raise ModelError(f"--animation {name}: no class {class_name} in src/main/java or the Minecraft sources")
    animations = read_animations(files[0].read_text(encoding="utf-8"))
    if field not in animations:
        raise ModelError(f"--animation {name}: {class_name} has no {field}; it has {', '.join(animations)}")
    return f"{class_name}.{field}", animations[field]


ANIMATION = re.compile(r"AnimationDefinition (\w+) = AnimationDefinition\.Builder\.withLength\(([\d.]+)F?\)(.*?)\.build\(\)",
                       re.S)
# In the order of the source: a bone, a channel of the bone, a keyframe of the channel.
ANIMATION_TOKEN = re.compile(r'addAnimation\(\s*"(\w+)"|Targets\.(\w+)|new Keyframe\(\s*([-\d.]+)F?,\s*'
                             r"KeyframeAnimations\.(\w+)\(\s*([-\d.]+)F?,\s*([-\d.]+)F?,\s*([-\d.]+)F?\s*\),\s*"
                             r"AnimationChannel\.Interpolations\.(\w+)\s*\)")
# What KeyframeAnimations makes of the numbers in the source, and the fields of ModelPart that the channel offsets.
VECTORS = {
    "degreeVec": lambda x, y, z: (math.radians(x), math.radians(y), math.radians(z)),
    "posVec": lambda x, y, z: (x, -y, z),
    "scaleVec": lambda x, y, z: (x - 1, y - 1, z - 1),
}
TARGETS = {"POSITION": 0, "ROTATION": 3, "SCALE": 6}


def read_animations(source):
    """Per field of an exported animation class: (length in seconds, whether it loops, [(bone, target, keyframes)])
    with keyframes as (timestamp, vector, interpolation)."""
    animations = {}
    for field, length, body in ANIMATION.findall(source):
        channels = []
        for bone, target, timestamp, vector, x, y, z, interpolation in ANIMATION_TOKEN.findall(body):
            if bone:
                current = bone
            elif target:
                channels.append((current, target, []))
            else:
                channels[-1][2].append((float(timestamp), VECTORS[vector](float(x), float(y), float(z)), interpolation))
        animations[field] = float(length), ".looping()" in body, channels
    return animations


def catmullrom(alpha, p0, p1, p2, p3):
    return 0.5 * (2 * p1 + (p2 - p0) * alpha + (2 * p0 - 5 * p1 + 4 * p2 - p3) * alpha * alpha
                  + (3 * p1 - p0 - 3 * p2 + p3) * alpha * alpha * alpha)


def sample(keyframes, seconds):
    """KeyframeAnimation.Entry.apply: the vector of a channel at a time."""
    prev = max(0, next((i for i, keyframe in enumerate(keyframes) if seconds <= keyframe[0]), len(keyframes)) - 1)
    following = min(len(keyframes) - 1, prev + 1)
    span = keyframes[following][0] - keyframes[prev][0]
    alpha = min(max((seconds - keyframes[prev][0]) / span, 0), 1) if following != prev else 0
    if keyframes[following][2] == "CATMULLROM":
        points = [keyframes[i][1] for i in (max(0, prev - 1), prev, following, min(len(keyframes) - 1, following + 1))]
        return tuple(catmullrom(alpha, *(point[axis] for point in points)) for axis in range(3))
    start, end = keyframes[prev][1], keyframes[following][1]
    return tuple(a + (b - a) * alpha for a, b in zip(start, end))


def animation_changes(parts, channels, seconds):
    """The offsets an animation adds to the parts at a time, in the form of pose_changes. ModelPart.createPartLookup
    finds a bone by its name, nearer the root first; root is the root."""
    changes = {}
    for bone, target, keyframes in channels:
        paths = [""] if bone == "root" else sorted((path for path in parts if path.endswith("/" + bone)),
                                                   key=lambda path: path.count("/"))
        if not paths:
            raise ModelError(f"the animation moves {bone}, which the model does not have")
        changes.setdefault(paths[0], []).extend(
            (TARGETS[target] + axis, True, value) for axis, value in enumerate(sample(keyframes, seconds)))
    return changes


def view(text):
    try:
        yaw, pitch = (float(angle) for angle in text.split(","))
    except ValueError:
        raise argparse.ArgumentTypeError(f"{text}: expected yaw,pitch in degrees, such as 120,20") from None
    return yaw, pitch


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("output", type=Path, help="PNG, or GIF for an animation")
    parser.add_argument("model", help="model id (minecraft:minecart) or bare name")
    parser.add_argument("texture", help="texture id (runeruin:entity/snail/snail) or path to a PNG")
    parser.add_argument("--layer", nargs="+", default=["main"], metavar="LAYER[=TEXTURE]",
                        help="model layers drawn together, default main; a layer without a texture takes TEXTURE")
    parser.add_argument("--view", type=view, nargs="+", metavar="YAW,PITCH", help="cameras instead of the default views")
    parser.add_argument("--pose", nargs="+", default=[], metavar="PART.FIELD=VALUE", help="changes to the initial pose")
    parser.add_argument("--animation", metavar="NAME", help="animation of the model's exported class, or Class.FIELD")
    parser.add_argument("--frames", type=int, default=5, help="still frames of an animation in a PNG, default 5")
    parser.add_argument("--fps", type=int, default=20, help="frames per second of a GIF, default 20 as game ticks")
    parser.add_argument("--scale", type=int, help="screen pixels per model unit (1/16 block), default 16, 8 animated")
    args = parser.parse_args()
    scale = args.scale or (8 if args.animation else 16)

    try:
        layers = []
        for layer in args.layer:
            name, _, texture = layer.partition("=")
            identifier, file = find_model(args.model, name)
            parts = json.loads(file.read_text(encoding="utf-8"))["parts"]
            check_against_game(parts)
            layers.append((parts, pose_changes(parts, args.pose), load_texture(texture or args.texture)))
        animation, (length, looping, channels) = find_animation(identifier, args.animation) if args.animation \
            else (None, (0, False, []))
        if args.output.suffix.lower() == ".gif":
            if not animation:
                raise ModelError("a GIF needs --animation")
            # A loop ends where it starts, so its last frame is the first one of the next round.
            times = [i / args.fps for i in range(round(length * args.fps) + (0 if looping else 1))]
        else:
            steps = max(args.frames - (0 if looping else 1), 1)
            times = [length * i / steps for i in range(args.frames)] if animation else [0]

        def frame_quads(seconds):
            quads = []
            for parts, changes, (_, texture) in layers:
                moved = animation_changes(parts, channels, seconds)
                merged = {path: changes.get(path, []) + moved.get(path, []) for path in {*changes, *moved}}
                quads.extend(texel_quads(parts, posed_parts(parts, merged), texture))
            return quads

        frames = [frame_quads(seconds) for seconds in times]
    except ModelError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1

    default_views = ANIMATION_VIEWS if animation else VIEWS
    views = {f"yaw {yaw:g}, pitch {pitch:g}": (yaw, pitch) for yaw, pitch in args.view} if args.view else default_views
    # Per view the faces of every frame, drawn in one frame on the screen so that the model does not jump.
    projected = {}
    for name, (yaw, pitch) in views.items():
        yaw, pitch = math.radians(yaw), math.radians(pitch)
        camera = (math.sin(yaw) * math.cos(pitch), math.sin(pitch), -math.cos(yaw) * math.cos(pitch))
        projected[name] = [project(quads, camera, scale, (-math.cos(yaw), 0, -math.sin(yaw))) for quads in frames]
    boxes = {name: [f(edge) for f, edge in zip((min, min, max, max), zip(*(bounds(faces, scale) for faces in frames)))]
             for name, frames in projected.items()}

    # The textures would make the title of a small animated mob much wider than the mob.
    palettes = [(name, texture, None) for name, texture in dict(texture for _, _, texture in layers).items()]
    textures = "  ".join(name for name, _, _ in palettes)
    title = f"{identifier}#{'+'.join(layer.partition('=')[0] for layer in args.layer)}  {animation or textures}" \
            + (f"  pose: {' '.join(args.pose)}" if args.pose else "")
    if args.output.suffix.lower() == ".gif":
        sheets = [sheet(f"{title}  {seconds:.2f} / {length:g} s",
                        [(name, draw_view(faces[i], boxes[name])) for name, faces in projected.items()], palettes)
                  for i, seconds in enumerate(times)]
        # One size and one palette for all frames, so that nothing jumps or flickers between them.
        width, height = max(image.width for image in sheets), max(image.height for image in sheets)
        strip = Image.new("RGB", (width, height * len(sheets)), BACKGROUND)
        for i, image in enumerate(sheets):
            strip.paste(image, (0, i * height))
        sheets = [strip.crop((0, i * height, width, (i + 1) * height)) for i in range(len(sheets))]
        palette = strip.quantize(255, Image.Quantize.MEDIANCUT)
        gif = [image.quantize(palette=palette, dither=Image.Dither.NONE) for image in sheets]
        # An animation that does not loop rests on its last frame before it starts again.
        durations = [1000 // args.fps] * (len(gif) - 1) + [1000 // args.fps if looping else 1000]
        save_image(args.output, gif[0], save_all=True, append_images=gif[1:], duration=durations, loop=0)
    elif animation:
        rows = [sheet(f"{title}  {name}", [(f"{seconds:.2f} s", draw_view(faces[i], boxes[name]))
                                           for i, seconds in enumerate(times)]) for name, faces in projected.items()]
        result = Image.new("RGB", (max(row.width for row in rows), sum(row.height for row in rows)), BACKGROUND)
        for i, row in enumerate(rows):
            result.paste(row, (0, sum(previous.height for previous in rows[:i])))
        save_image(args.output, add_palettes(result, palettes))
    else:
        save_image(args.output, sheet(title, [(name, draw_view(faces[0], boxes[name]))
                                              for name, faces in projected.items()], palettes))
    return 0


if __name__ == "__main__":
    sys.exit(main())
