#!/usr/bin/env python3
"""Check the mod's client assets in seconds, without the game. Run after runData: models live in src/generated.

    python scripts/lint_assets.py

- blockstates, item definitions and models refer only to models that exist;
- every texture a model uses exists, vanilla ones in .vanilla-textures (gradlew extractVanillaTextures);
- en_us and ru_ru have the same keys, and every block and item has a name in both;
- block and item textures are 16x16, or 16 wide with a .mcmeta animation of whole frames, or listed in OTHER_SIZES.

Exits 1 and lists every problem when a check fails.
"""
import json
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
NAMESPACE = "runeruin"
ASSET_ROOTS = [ROOT / "src/main/resources/assets", ROOT / "src/generated/resources/assets"]
VANILLA = ROOT / ".vanilla-textures/assets"
LANGS = ("en_us", "ru_ru")
BUILTIN_PARENTS = {"builtin/generated", "builtin/entity"}
# Deliberate exceptions to 16x16, with their exact size: models with their own UV scale and the
# stem atlas that WaterLilyStemTexture slices at runtime.
OTHER_SIZES = {
    "block/elden_hanging_sign": (32, 32),
    "block/elden_sign": (32, 32),
    "block/firefly_butterfly": (8, 8),
    "block/firefly_jar_glass": (32, 32),
    "block/water_lily_stems": (800, 1280),
}


def split_id(value, default_namespace="minecraft"):
    namespace, _, path = value.rpartition(":")
    return namespace or default_namespace, path


def find_asset(namespace, kind, path, suffix):
    roots = ASSET_ROOTS if namespace == NAMESPACE else [VANILLA]
    for root in roots:
        candidate = root / namespace / kind / (path + suffix)
        if candidate.is_file():
            return candidate
    return None


def can_check(namespace, kind):
    return namespace == NAMESPACE or (VANILLA / namespace / kind).is_dir()


def asset_files(kind, pattern):
    for root in ASSET_ROOTS:
        yield from sorted((root / NAMESPACE / kind).rglob(pattern))


def read_json(path, problems):
    try:
        return json.loads(path.read_text(encoding="utf-8-sig"))
    except (OSError, ValueError) as error:
        problems.append(f"{rel(path)}: cannot read JSON: {error}")
        return None


def rel(path):
    return path.relative_to(ROOT).as_posix()


def model_refs(node):
    """Model ids under "model" keys of a blockstate or an item definition."""
    if isinstance(node, dict):
        for key, value in node.items():
            if key == "model" and isinstance(value, str):
                yield value
            else:
                yield from model_refs(value)
    elif isinstance(node, list):
        for value in node:
            yield from model_refs(value)


def check_ref(owner, kind, ref, suffix, problems, missing):
    namespace, path = split_id(ref)
    if kind == "models" and path in BUILTIN_PARENTS:
        return
    if not can_check(namespace, kind):
        missing.add(f"{namespace} {kind}")
    elif find_asset(namespace, kind, path, suffix) is None:
        problems.append(f"{rel(owner)}: {kind[:-1]} {namespace}:{path} does not exist")


def check_models(problems):
    missing = set()
    for path in asset_files("blockstates", "*.json"):
        for model_id in model_refs(read_json(path, problems)):
            check_ref(path, "models", model_id, ".json", problems, missing)
    for path in asset_files("items", "*.json"):
        for model_id in model_refs(read_json(path, problems)):
            check_ref(path, "models", model_id, ".json", problems, missing)
    for path in asset_files("models", "*.json"):
        model = read_json(path, problems)
        if not isinstance(model, dict):
            continue
        if isinstance(model.get("parent"), str):
            check_ref(path, "models", model["parent"], ".json", problems, missing)
        for texture in (model.get("textures") or {}).values():
            if isinstance(texture, str) and not texture.startswith("#"):
                check_ref(path, "textures", texture, ".png", problems, missing)
    # An old or missing .vanilla-textures must not turn unchecked references into "assets OK".
    for assets in sorted(missing):
        problems.append(f"{assets} not checked: missing in .vanilla-textures, run gradlew extractVanillaTextures")


def check_langs(problems):
    langs = {}
    for lang in LANGS:
        path = find_asset(NAMESPACE, "lang", lang, ".json")
        if path is None:
            problems.append(f"lang {lang}.json does not exist")
            return
        langs[lang] = read_json(path, problems) or {}
    keys = set().union(*langs.values())
    for lang, entries in langs.items():
        for key in sorted(keys - entries.keys()):
            problems.append(f"{lang}.json: missing {key} (present in another language)")
    names = {f"block.{NAMESPACE}.{path.stem}" for path in asset_files("blockstates", "*.json")}
    for path in asset_files("items", "*.json"):
        if f"block.{NAMESPACE}.{path.stem}" not in names:
            names.add(f"item.{NAMESPACE}.{path.stem}")
    for key in sorted(names - keys):
        problems.append(f"{key} has no name in {' or '.join(LANGS)}")


def png_size(path):
    with path.open("rb") as file:
        header = file.read(24)
    if len(header) < 24 or header[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    return struct.unpack(">II", header[16:24])


def check_textures(problems):
    for kind in ("block", "item"):
        for path in asset_files(f"textures/{kind}", "*.png"):
            size = png_size(path)
            if size is None:
                problems.append(f"{rel(path)}: not a PNG")
                continue
            width, height = size
            name = f"{kind}/{path.stem}"
            if name in OTHER_SIZES:
                if size != OTHER_SIZES[name]:
                    problems.append(f"{rel(path)}: {width}x{height}, OTHER_SIZES expects {OTHER_SIZES[name]}")
                continue
            animated = path.with_name(path.name + ".mcmeta").is_file()
            if width != 16 or height % 16 or (height != 16 and not animated):
                expected = "16x16 or 16x(16*frames) with .mcmeta" if animated or height != 16 else "16x16"
                problems.append(f"{rel(path)}: {width}x{height}, expected {expected}")


def main():
    problems = []
    check_models(problems)
    check_langs(problems)
    check_textures(problems)
    for problem in problems:
        print(f"FAIL: {problem}")
    print(f"{len(problems)} problem(s)" if problems else "assets OK")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
