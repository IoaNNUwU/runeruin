#!/usr/bin/env python3
"""Extract the UV rectangle used by each face in a Minecraft cuboid model JSON."""

from __future__ import annotations

import argparse
import json
import math
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any

try:
    from PIL import Image
except ImportError:  # pragma: no cover - depends on the caller's Python environment
    Image = None  # type: ignore[assignment,misc]


FACES = ("down", "up", "north", "south", "west", "east")


class ModelError(Exception):
    """An actionable issue in the model, texture, or command-line input."""


@dataclass(frozen=True)
class ModelRef:
    namespace: str
    path: str
    file: Path | None = None

    @property
    def identifier(self) -> str:
        return f"{self.namespace}:{self.path}"


def parse_identifier(value: str, default_namespace: str = "minecraft") -> ModelRef:
    value = value.replace("\\", "/")
    if ":" in value:
        namespace, path = value.split(":", 1)
    else:
        namespace, path = default_namespace, value
    if path.startswith("models/"):
        path = path[len("models/") :]
    if path.endswith(".json"):
        path = path[:-5]
    if not namespace or not path or any(part == ".." for part in Path(path).parts):
        raise ModelError(f"Invalid model resource location: {value}")
    return ModelRef(namespace, path)


def assets_root_from_model_file(model_file: Path) -> Path | None:
    parts = model_file.resolve().parts
    for index in range(len(parts) - 1, -1, -1):
        if parts[index].lower() == "assets":
            return Path(*parts[: index + 1])
    return None


def discover_assets_roots(model_file: Path | None, requested: list[Path]) -> list[Path]:
    roots: list[Path] = []

    def add(path: Path) -> None:
        path = path.resolve()
        if path.is_dir() and path not in roots:
            roots.append(path)

    for path in requested:
        add(path)
    if model_file is not None:
        inferred = assets_root_from_model_file(model_file)
        if inferred is not None:
            add(inferred)

    search_bases = [Path.cwd()]
    if model_file is not None:
        search_bases.extend(model_file.resolve().parents)
    visited: set[Path] = set()
    for base in search_bases:
        for parent in (base, *base.parents):
            if parent in visited:
                continue
            visited.add(parent)
            add(parent / "src" / "generated" / "resources" / "assets")
            add(parent / "src" / "main" / "resources" / "assets")
    return roots


def resolve_model_file(ref: ModelRef, roots: list[Path]) -> Path | None:
    if ref.file is not None:
        return ref.file
    relative = Path(ref.namespace) / "models" / Path(ref.path + ".json")
    for root in roots:
        candidate = (root / relative).resolve()
        if candidate.is_relative_to(root) and candidate.is_file():
            return candidate
    return None


def builtin_vanilla_model(ref: ModelRef) -> dict[str, Any] | None:
    """Geometry for common vanilla parents whose JSON is packaged in Minecraft, not mod assets."""
    if ref.namespace != "minecraft" or not ref.path.startswith("block/"):
        return None
    model_name = ref.path.removeprefix("block/")
    face_textures: dict[str, str]
    texture_vars: dict[str, str]
    if model_name in {"cube_all", "cube_mirrored_all"}:
        face_textures = {face: "#all" for face in FACES}
        texture_vars = {"particle": "#all"}
    elif model_name == "cube":
        face_textures = {face: f"#{face}" for face in FACES}
        texture_vars = {}
    elif model_name == "cube_bottom_top":
        face_textures = {
            "down": "#bottom",
            "up": "#top",
            "north": "#side",
            "south": "#side",
            "west": "#side",
            "east": "#side",
        }
        texture_vars = {"particle": "#side"}
    elif model_name == "cube_column":
        face_textures = {
            "down": "#end",
            "up": "#end",
            "north": "#side",
            "south": "#side",
            "west": "#side",
            "east": "#side",
        }
        texture_vars = {"particle": "#side"}
    elif model_name == "cube_column_horizontal":
        face_textures = {
            "down": "#side",
            "up": "#side",
            "north": "#side",
            "south": "#side",
            "west": "#end",
            "east": "#end",
        }
        texture_vars = {"particle": "#side"}
    elif model_name == "orientable":
        face_textures = {
            "down": "#side",
            "up": "#top",
            "north": "#front",
            "south": "#side",
            "west": "#side",
            "east": "#side",
        }
        texture_vars = {"particle": "#side"}
    elif model_name == "orientable_with_bottom":
        face_textures = {
            "down": "#bottom",
            "up": "#top",
            "north": "#front",
            "south": "#side",
            "west": "#side",
            "east": "#side",
        }
        texture_vars = {"particle": "#side"}
    else:
        return None

    return {
        "textures": texture_vars,
        "elements": [
            {
                "name": f"vanilla_{model_name.replace('/', '_')}",
                "from": [0, 0, 0],
                "to": [16, 16, 16],
                "faces": {face: {"texture": texture} for face, texture in face_textures.items()},
            }
        ],
        "element_file": f"built-in geometry for {ref.identifier}",
        "files": [],
    }


def model_ref_from_argument(value: str) -> tuple[ModelRef, Path | None]:
    candidate = Path(value)
    if candidate.is_file():
        file = candidate.resolve()
        root = assets_root_from_model_file(file)
        if root is not None:
            try:
                relative = file.relative_to(root)
                if len(relative.parts) >= 3 and relative.parts[1] == "models":
                    namespace = relative.parts[0]
                    path = Path(*relative.parts[2:]).as_posix()
                    return parse_identifier(f"{namespace}:{path}"), file
            except ValueError:
                pass
        return ModelRef("minecraft", file.stem, file), file
    return parse_identifier(value), None


def load_model(ref: ModelRef, roots: list[Path], chain: tuple[str, ...] = ()) -> dict[str, Any]:
    if ref.identifier in chain:
        cycle = " -> ".join((*chain, ref.identifier))
        raise ModelError(f"Model parent cycle: {cycle}")
    model_file = resolve_model_file(ref, roots)
    if model_file is None:
        raise ModelError(f"Cannot find model JSON {ref.identifier} under: {format_roots(roots)}")
    try:
        data = json.loads(model_file.read_text(encoding="utf-8-sig"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ModelError(f"Cannot read model JSON {model_file}: {exc}") from exc
    if not isinstance(data, dict):
        raise ModelError(f"Model JSON root must be an object: {model_file}")

    parent_data: dict[str, Any] = {"textures": {}, "elements": None, "files": []}
    parent_value = data.get("parent")
    if isinstance(parent_value, str):
        parent_ref = parse_identifier(parent_value)
        try:
            parent_data = load_model(parent_ref, roots, (*chain, ref.identifier))
        except ModelError as exc:
            # Vanilla parent JSON lives in Minecraft's jar rather than a mod's asset roots.
            parent_data = builtin_vanilla_model(parent_ref) or parent_data
            if data.get("elements") is None and parent_data.get("elements") is None:
                raise ModelError(
                    f"Cannot resolve parent {parent_ref.identifier} for {ref.identifier}: {exc}"
                ) from exc

    parent_textures = parent_data.get("textures", {})
    own_textures = data.get("textures", {})
    if not isinstance(parent_textures, dict) or not isinstance(own_textures, dict):
        raise ModelError(f"Model textures must be a JSON object: {model_file}")
    textures = {**parent_textures, **own_textures}
    elements = data.get("elements")
    if elements is None:
        elements = parent_data.get("elements")
    element_file = model_file if data.get("elements") is not None else parent_data.get("element_file")
    return {
        "ref": ref,
        "file": model_file,
        "textures": textures,
        "elements": elements,
        "element_file": element_file,
        "files": [*parent_data.get("files", []), model_file],
    }


def format_roots(roots: list[Path]) -> str:
    return ", ".join(str(root) for root in roots) if roots else "no assets roots found"


def resolve_texture_token(token: Any, textures: dict[str, Any]) -> tuple[str, str]:
    if not isinstance(token, str) or not token:
        raise ModelError(f"Face texture must be a non-empty string, got {token!r}")
    original = token
    seen: set[str] = set()
    while token.startswith("#"):
        key = token[1:]
        if key in seen:
            raise ModelError(f"Texture variable cycle while resolving {original}: #{key}")
        seen.add(key)
        if key not in textures:
            raise ModelError(f"Texture variable {token} is not defined by the model or its parents")
        token = textures[key]
        if not isinstance(token, str):
            raise ModelError(f"Texture variable #{key} does not resolve to a string")
    return original, token


def resolve_texture_file(resource: str, namespace: str, roots: list[Path]) -> tuple[Path, str]:
    texture_ref = parse_identifier(resource, namespace)
    texture_path = texture_ref.path
    if texture_path.startswith("textures/"):
        texture_path = texture_path[len("textures/") :]
    if texture_path.endswith(".png"):
        texture_path = texture_path[:-4]
    relative = Path(texture_ref.namespace) / "textures" / Path(texture_path + ".png")
    for root in roots:
        candidate = (root / relative).resolve()
        if candidate.is_relative_to(root) and candidate.is_file():
            return candidate, texture_ref.identifier
    raise ModelError(
        f"Cannot find texture {texture_ref.identifier}.png under: {format_roots(roots)}"
    )


def default_uv(from_pos: list[float], to_pos: list[float], face: str) -> list[float]:
    fx, fy, fz = from_pos
    tx, ty, tz = to_pos
    values = {
        "down": [fx, 16.0 - tz, tx, 16.0 - fz],
        "up": [fx, fz, tx, tz],
        "north": [16.0 - tx, 16.0 - ty, 16.0 - fx, 16.0 - fy],
        "south": [fx, 16.0 - ty, tx, 16.0 - fy],
        "west": [fz, 16.0 - ty, tz, 16.0 - fy],
        "east": [16.0 - tz, 16.0 - ty, 16.0 - fz, 16.0 - fy],
    }
    return values[face]


def pixel_box(
    uv: list[float], width: int, height: int, *, block_uv_units: bool = True
) -> tuple[tuple[int, int, int, int], bool]:
    u1, v1, u2, v2 = uv
    scale_u = width / 16.0 if block_uv_units else 1.0
    scale_v = height / 16.0 if block_uv_units else 1.0
    raw_left = math.floor(min(u1, u2) * scale_u)
    raw_top = math.floor(min(v1, v2) * scale_v)
    raw_right = math.ceil(max(u1, u2) * scale_u)
    raw_bottom = math.ceil(max(v1, v2) * scale_v)
    clipped = raw_left < 0 or raw_top < 0 or raw_right > width or raw_bottom > height
    left = max(0, min(width, raw_left))
    top = max(0, min(height, raw_top))
    right = max(0, min(width, raw_right))
    bottom = max(0, min(height, raw_bottom))
    if right <= left or bottom <= top:
        raise ModelError(f"UV rectangle {uv} covers no pixels in a {width}x{height} texture")
    return (left, top, right, bottom), clipped


def numbers(value: Any, label: str, expected: int) -> list[float]:
    if not isinstance(value, list) or len(value) != expected:
        raise ModelError(f"{label} must contain {expected} numbers")
    try:
        result = [float(item) for item in value]
    except (TypeError, ValueError) as exc:
        raise ModelError(f"{label} must contain {expected} numbers") from exc
    if not all(math.isfinite(item) for item in result):
        raise ModelError(f"{label} contains a non-finite number")
    return result


def safe_name(value: str) -> str:
    slug = re.sub(r"[^A-Za-z0-9._-]+", "_", value).strip("._-")
    return slug or "element"


def selected_element(selector: str, element: dict[str, Any], index: int, name: str) -> bool:
    if selector.startswith("#"):
        return selector[1:].isdigit() and int(selector[1:]) == index
    return selector == name


def element_name(element: dict[str, Any], index: int) -> str:
    name = element.get("name")
    return name.strip() if isinstance(name, str) and name.strip() else f"element_{index:03d}"


def extract(args: argparse.Namespace) -> dict[str, Any]:
    if Image is None:
        raise ModelError(
            "PNG cropping requires Pillow. Install it with `python -m pip install Pillow`, then rerun."
        )

    ref, model_file = model_ref_from_argument(args.model_json)
    roots = discover_assets_roots(model_file, args.assets_root)
    if not roots:
        raise ModelError(
            "No assets roots found. Pass --assets-root pointing at the directory containing "
            "namespace folders (for example src/generated/resources/assets)."
        )
    model = load_model(ref, roots)
    elements = model.get("elements")
    if not isinstance(elements, list):
        raise ModelError(
            f"Model {ref.identifier} has no cuboid elements. Its geometry may be inherited from an "
            "unavailable vanilla parent or use a non-cuboid format."
        )

    selectors = args.element or []
    face_filter = set(args.face or [])
    out_dir = args.output_dir / Path(ref.namespace) / Path(ref.path)
    out_dir.mkdir(parents=True, exist_ok=True)
    warnings: list[str] = []
    manifest_elements: list[dict[str, Any]] = []
    element_names: list[str] = []
    extracted_count = 0

    for index, element in enumerate(elements):
        if not isinstance(element, dict):
            raise ModelError(f"Element {index} is not an object")
        name = element_name(element, index)
        element_names.append(f"{index}: {name}")
        if selectors and not any(selected_element(item, element, index, name) for item in selectors):
            continue

        from_pos = numbers(element.get("from"), f"element {index} from", 3)
        to_pos = numbers(element.get("to"), f"element {index} to", 3)
        faces = element.get("faces")
        if not isinstance(faces, dict):
            raise ModelError(f"Element {index} ({name}) has no faces object")
        manifest_face_list: list[dict[str, Any]] = []

        for direction, face_data in faces.items():
            if direction not in FACES:
                warnings.append(f"Element {index} ({name}) has unknown face direction {direction!r}; skipped")
                continue
            if face_filter and direction not in face_filter:
                continue
            if not isinstance(face_data, dict):
                raise ModelError(f"Face {direction} in element {index} must be an object")

            texture_variable, texture_resource = resolve_texture_token(face_data.get("texture"), model["textures"])
            try:
                texture_file, texture_id = resolve_texture_file(texture_resource, ref.namespace, roots)
            except ModelError as exc:
                warnings.append(f"Skipped element {index} ({name}) face {direction}: {exc}")
                continue
            try:
                with Image.open(texture_file) as opened:
                    frame_count = getattr(opened, "n_frames", 1)
                    if frame_count > 1:
                        warnings.append(f"{texture_id} is animated; extracted frame 0")
                    opened.seek(0)
                    texture = opened.convert("RGBA")
            except OSError as exc:
                raise ModelError(f"Cannot open texture {texture_file}: {exc}") from exc

            uv = numbers(face_data["uv"], f"element {index} {direction} uv", 4) if "uv" in face_data else default_uv(from_pos, to_pos, direction)
            box, clipped = pixel_box(uv, texture.width, texture.height)
            if clipped:
                warnings.append(
                    f"Element {index} ({name}) face {direction} UV {uv} extends outside "
                    f"the {texture.width}x{texture.height} texture; crop was clipped"
                )
            file_name = f"{index:03d}_{safe_name(name)}_{direction}.png"
            crop_path = out_dir / file_name
            texture.crop(box).save(crop_path, format="PNG")
            rotation = face_data.get("rotation", 0)
            face_manifest = {
                "direction": direction,
                "texture_variable": texture_variable,
                "texture": texture_id,
                "source_texture": str(texture_file),
                "source_size": [texture.width, texture.height],
                "uv_0_16": uv,
                "pixel_box_left_top_right_bottom": list(box),
                "uv_out_of_bounds": clipped,
                "uv_rotation_degrees": rotation,
                "uv_mirrored_u": uv[2] < uv[0],
                "uv_mirrored_v": uv[3] < uv[1],
                "output": file_name,
            }
            if "cullface" in face_data:
                face_manifest["cullface"] = face_data["cullface"]
            manifest_face_list.append(face_manifest)
            extracted_count += 1

        if manifest_face_list:
            manifest_elements.append(
                {
                    "index": index,
                    "name": name,
                    "from": from_pos,
                    "to": to_pos,
                    "element_rotation": element.get("rotation"),
                    "faces": manifest_face_list,
                }
            )

    if selectors:
        unmatched = [
            selector
            for selector in selectors
            if not any(selected_element(selector, element, i, element_name(element, i))
                       for i, element in enumerate(elements) if isinstance(element, dict))
        ]
        if unmatched:
            raise ModelError(
                f"Unknown element selector(s): {', '.join(unmatched)}. Available elements: "
                + ("; ".join(element_names) or "none")
            )
    if extracted_count == 0:
        if face_filter:
            raise ModelError(f"No faces matched the requested direction(s): {', '.join(sorted(face_filter))}")
        details = " " + " ".join(warnings) if warnings else ""
        raise ModelError(f"No extractable faces found in the selected elements.{details}")

    manifest = {
        "format": "minecraft-model-face-crops/1",
        "model": ref.identifier,
        "model_json": str(model["file"]),
        "element_source_json": str(model.get("element_file") or model["file"]),
        "assets_roots": [str(root) for root in roots],
        "note": "Crops show the source UV rectangles. Face UV rotation and mirroring are recorded, not applied to the pixels.",
        "elements": manifest_elements,
        "warnings": warnings,
    }
    manifest_path = out_dir / "manifest.json"
    manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return {"manifest": manifest_path, "count": extracted_count, "elements": manifest_elements, "warnings": warnings}


def extract_entity_uv_map(args: argparse.Namespace) -> dict[str, Any]:
    """Crop pixel-space face rectangles mapped from a Java entity model's atlas UVs."""
    if Image is None:
        raise ModelError(
            "PNG cropping requires Pillow. Install it with `python -m pip install Pillow`, then rerun."
        )
    map_file = args.entity_uv_map.resolve()
    try:
        data = json.loads(map_file.read_text(encoding="utf-8-sig"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ModelError(f"Cannot read entity UV map {map_file}: {exc}") from exc
    if not isinstance(data, dict) or data.get("format") != "minecraft-entity-uv-map/1":
        raise ModelError("Entity UV map must have format `minecraft-entity-uv-map/1`")
    parts = data.get("parts")
    if not isinstance(parts, list):
        raise ModelError("Entity UV map `parts` must be an array")
    texture_token = data.get("texture")
    if not isinstance(texture_token, str):
        raise ModelError("Entity UV map must provide a texture resource id")
    roots = discover_assets_roots(map_file, args.assets_root)
    if not roots:
        raise ModelError(
            "No assets roots found. Pass --assets-root pointing at the directory containing "
            "namespace folders (for example src/main/resources/assets)."
        )
    _, texture_resource = resolve_texture_token(texture_token, {})
    texture_file, texture_id = resolve_texture_file(texture_resource, "minecraft", roots)
    try:
        with Image.open(texture_file) as opened:
            frame_count = getattr(opened, "n_frames", 1)
            opened.seek(0)
            texture = opened.convert("RGBA")
    except OSError as exc:
        raise ModelError(f"Cannot open texture {texture_file}: {exc}") from exc

    warnings: list[str] = []
    expected_size = data.get("texture_size")
    if expected_size is not None:
        expected = numbers(expected_size, "texture_size", 2)
        if expected != [texture.width, texture.height]:
            warnings.append(
                f"Map declares texture_size {expected}, but source image is {texture.width}x{texture.height}"
            )
    if frame_count > 1:
        warnings.append(f"{texture_id} is animated; extracted frame 0")

    output_dir = args.output_dir / "entity" / safe_name(map_file.stem)
    output_dir.mkdir(parents=True, exist_ok=True)
    selectors = args.element or []
    face_filter = set(args.face or [])
    manifest_parts: list[dict[str, Any]] = []
    part_names: list[str] = []
    extracted_count = 0

    for index, part in enumerate(parts):
        if not isinstance(part, dict):
            raise ModelError(f"Entity part {index} is not an object")
        name_value = part.get("name")
        name = name_value.strip() if isinstance(name_value, str) and name_value.strip() else f"part_{index:03d}"
        part_names.append(f"{index}: {name}")
        if selectors and not any(selected_element(item, part, index, name) for item in selectors):
            continue
        faces = part.get("faces")
        if not isinstance(faces, dict):
            raise ModelError(f"Entity part {index} ({name}) has no faces object")

        manifest_faces: list[dict[str, Any]] = []
        for direction, face_value in faces.items():
            if direction not in FACES:
                warnings.append(f"Entity part {index} ({name}) has unknown face direction {direction!r}; skipped")
                continue
            if face_filter and direction not in face_filter:
                continue
            if isinstance(face_value, dict):
                uv_value = face_value.get("uv")
                rotation = face_value.get("rotation", 0)
            else:
                uv_value = face_value
                rotation = 0
            uv = numbers(uv_value, f"entity part {index} {direction} uv", 4)
            box, clipped = pixel_box(uv, texture.width, texture.height, block_uv_units=False)
            if clipped:
                warnings.append(
                    f"Entity part {index} ({name}) face {direction} UV pixels {uv} extend outside "
                    f"the {texture.width}x{texture.height} texture; crop was clipped"
                )
            file_name = f"{index:03d}_{safe_name(name)}_{direction}.png"
            texture.crop(box).save(output_dir / file_name, format="PNG")
            manifest_faces.append(
                {
                    "direction": direction,
                    "texture": texture_id,
                    "source_texture": str(texture_file),
                    "source_size": [texture.width, texture.height],
                    "uv_pixels_left_top_right_bottom": uv,
                    "pixel_box_left_top_right_bottom": list(box),
                    "uv_rotation_degrees": rotation,
                    "uv_mirrored_u": uv[2] < uv[0],
                    "uv_mirrored_v": uv[3] < uv[1],
                    "uv_out_of_bounds": clipped,
                    "output": file_name,
                }
            )
            extracted_count += 1
        if manifest_faces:
            manifest_parts.append({"index": index, "name": name, "faces": manifest_faces})

    unmatched = [
        selector
        for selector in selectors
        if not any(
            selected_element(
                selector,
                part,
                index,
                (part.get("name").strip() if isinstance(part.get("name"), str) and part.get("name").strip()
                 else f"part_{index:03d}"),
            )
            for index, part in enumerate(parts)
            if isinstance(part, dict)
        )
    ]
    if unmatched:
        raise ModelError(
            f"Unknown part selector(s): {', '.join(unmatched)}. Available parts: "
            + ("; ".join(part_names) or "none")
        )
    if extracted_count == 0:
        raise ModelError("No extractable entity UV faces found")

    manifest = {
        "format": "minecraft-model-face-crops/1",
        "model": f"entity-map:{map_file.stem}",
        "entity_uv_map": str(map_file),
        "texture": texture_id,
        "assets_roots": [str(root) for root in roots],
        "note": "Entity map rectangles use source-atlas pixel coordinates. UV rotation and mirroring are recorded, not applied to pixels.",
        "elements": manifest_parts,
        "warnings": warnings,
    }
    manifest_path = output_dir / "manifest.json"
    manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return {"manifest": manifest_path, "count": extracted_count, "warnings": warnings}


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Crop Minecraft cuboid-model JSON UV faces into individual PNGs."
    )
    parser.add_argument(
        "model_json",
        nargs="?",
        help="Block/item model JSON path, or a model id such as runeruin:block/firefly_in_a_jar",
    )
    parser.add_argument(
        "--entity-uv-map",
        type=Path,
        help="JSON map of Java mob-model atlas rectangles (pixel-space); see the skill instructions.",
    )
    parser.add_argument(
        "--element",
        action="append",
        help="Select a model element/UV-map part by `name`; use #N for its zero-based array index. Repeatable.",
    )
    parser.add_argument(
        "--face",
        action="append",
        choices=FACES,
        help="Select face direction(s). Repeatable; by default all faces are extracted.",
    )
    parser.add_argument(
        "--assets-root",
        type=Path,
        action="append",
        default=[],
        help="Assets directory containing namespace folders; repeat to include multiple resource roots.",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path("exports/model_faces"),
        help="Root output directory (default: exports/model_faces).",
    )
    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    if args.entity_uv_map is not None and args.model_json is not None:
        parser.error("pass either a block/item model JSON or --entity-uv-map, not both")
    if args.entity_uv_map is None and args.model_json is None:
        parser.error("provide a model JSON path/resource id or --entity-uv-map")
    try:
        result = extract_entity_uv_map(args) if args.entity_uv_map is not None else extract(args)
    except ModelError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 2

    print(f"Extracted {result['count']} face crop(s) to {result['manifest'].parent}")
    print(f"Manifest: {result['manifest']}")
    if result["warnings"]:
        for warning in result["warnings"]:
            print(f"warning: {warning}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
