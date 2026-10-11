#!/usr/bin/env python3
"""Render an entity model with a texture to a PNG: two isometric views, the front, a side and the top.

    python scripts/render_entity_model.py out.png snail runeruin:entity/snail/snail
    python scripts/render_entity_model.py out.png minecraft:minecart runeruin:entity/minecart/runic_minecart
    python scripts/render_entity_model.py out.png minecraft:creeper exports/new_charge.png --layer armor

Entity models are built in Java, so the geometry comes from the game: `gradlew runData` writes every model layer,
vanilla and ours, to build/entity_models/<namespace>/<model>/<layer>.json (EntityModelExport), as faces already
built and posed by ModelPart. Run it again after a change to a model. A bare model name tries runeruin: then
minecraft:. The texture is an id resolved through src/main and .vanilla-textures, or a path to a PNG.

The model stands as the game draws it, turned over from the model space where Y points down; the front is -Z,
where a mob faces. Parts are in their initial pose, without animation. Faces turned away from the camera are
dropped by the game's own normals; shade is by direction as in render_model.py, not the game's entity lighting.
"""
import argparse
import json
import sys
from pathlib import Path

from PIL import Image

from render_model import NORMALS, ROOT, SHADE, dot, face_texels, render_view, save_sheet

sys.path.insert(0, str(ROOT / ".agents/skills/minecraft-model-texture-analysis/scripts"))
from extract_model_faces import ModelError, discover_assets_roots, resolve_texture_file  # noqa: E402

MODELS = ROOT / "build/entity_models"
# Camera directions (towards the viewer): +y up, -z the front of the model.
VIEWS = {
    "iso front": (1, 0.8165, -1),
    "iso back, from below": (-1, -0.8165, 1),
    "front": (0, 0, -1),
    "side": (1, 0, 0),
    "top": (0, 1, 0),
}


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


def texel_quads(model, texture):
    """One (corners, rgba, normal) per visible texel of every face, in model units (1/16 block)."""
    quads = []
    for quad in model["quads"]:
        # The renderers draw a model under scale(-1, -1, 1), which turns it over: Y points down in model space.
        corners = [(-x * 16, -y * 16, z * 16) for x, y, z, _, _ in quad["vertices"]]
        uv = [(u * 16, v * 16) for _, _, _, u, v in quad["vertices"]]
        normal = (-quad["normal"][0], -quad["normal"][1], quad["normal"][2])
        light = SHADE[max(NORMALS, key=lambda name: dot(NORMALS[name], normal))]
        quads.extend(face_texels(texture, corners, uv, normal, light, None))
    return quads


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("output", type=Path)
    parser.add_argument("model", help="model id (minecraft:minecart) or bare name")
    parser.add_argument("texture", help="texture id (runeruin:entity/snail/snail) or path to a PNG")
    parser.add_argument("--layer", default="main", help="model layer, default main")
    parser.add_argument("--scale", type=int, default=16, help="screen pixels per model unit (1/16 block), default 16")
    args = parser.parse_args()

    try:
        identifier, file = find_model(args.model, args.layer)
        texture_name, texture = load_texture(args.texture)
    except ModelError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    quads = texel_quads(json.loads(file.read_text(encoding="utf-8")), texture)
    panels = [(name, render_view(quads, camera, args.scale)) for name, camera in VIEWS.items()]
    save_sheet(args.output, f"{identifier}#{args.layer}  {texture_name}", panels)
    return 0


if __name__ == "__main__":
    sys.exit(main())
