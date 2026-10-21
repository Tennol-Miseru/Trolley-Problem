#!/usr/bin/env python3
"""Fast static repository checks (not a substitute for Gradle build/game tests)."""
from pathlib import Path
import json
import re
import struct
import sys

ROOT = Path(__file__).resolve().parent.parent
RESOURCES = ROOT / "src/main/resources"
ERRORS = []


def error(message):
    ERRORS.append(message)


# JSON is the format for every gameplay resource, item model, and language file.
json_files = sorted(RESOURCES.rglob("*.json"))
for path in json_files:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        error(f"Invalid JSON: {path.relative_to(ROOT)} ({exc})")
        continue

    # Resource references from declared model textures must resolve locally.
    if "/models/" in path.as_posix() and isinstance(data, dict):
        textures = data.get("textures", {})
        if isinstance(textures, dict):
            for key, value in textures.items():
                if not isinstance(value, str) or value.startswith("#") or ":" not in value:
                    continue
                namespace, texture_path = value.split(":", 1)
                if namespace == "minecraft":
                    continue  # Asset provided by the game itself.
                texture = RESOURCES / "assets" / namespace / "textures" / (texture_path + ".png")
                if not texture.is_file():
                    error(f"Missing texture ({key}): {texture.relative_to(ROOT)}")

# Validate every PNG is well-formed at least as a file header.
png_files = sorted(RESOURCES.rglob("*.png"))
for png in png_files:
    contents = png.read_bytes()
    if len(contents) < 24 or not contents.startswith(b"\x89PNG\r\n\x1a\n"):
        error(f"Invalid PNG header: {png.relative_to(ROOT)}")
        continue
    width, height = struct.unpack(">II", contents[16:24])
    if not (0 < width <= 16384 and 0 < height <= 16384):
        error(f"Invalid PNG dimensions: {png.relative_to(ROOT)}")

# Version and mod id must be stable and nonempty.
properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")
for key in ("mod_id", "mod_version", "minecraft_version", "neo_version"):
    if not re.search(rf"(?m)^{key}=[^\n]+$", properties):
        error(f"Missing Gradle property: {key}")
if "mod_id=mod_e539c988" not in properties:
    error("Mod ID changed; changing it could corrupt existing saves")

java_files = sorted((ROOT / "src/main/java").rglob("*.java"))
if not java_files:
    error("No Java sources found")

if ERRORS:
    print("Repository validation failed:")
    for message in ERRORS:
        print(" -", message)
    sys.exit(1)
print(f"Static checks OK: {len(java_files)} Java sources, {len(json_files)} JSON files, {len(png_files)} PNG textures.")
print("Full compilation requires: gradle --no-daemon build")
