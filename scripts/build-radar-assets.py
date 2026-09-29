#!/usr/bin/env python3
"""Emit the Radar's corpus row and pack-side assets (#368, ADR-0079).

`data/factorio/machine.json`'s `radars` row is copied field by field to
`mod/src/main/resources/factoryworks_core/radar/radars.json`, which `RadarCorpus` reads at
class-init. Turning watts and joules into FE is `RadarSpec`'s, where a unit test holds it.

The model is a placeholder until #367: every block of the 3x3x3 footprint draws the same cube,
so the whole structure is visible.

Usage:

    scripts/build-radar-assets.py            # writes the resource and the pack assets
    scripts/build-radar-assets.py --check    # asserts both are already up to date; no writes
"""
import argparse
import json
import os
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
MACHINE_CORPUS = os.path.join(ROOT, "data", "factorio", "machine.json")
RADAR_RESOURCE = os.path.join(
    ROOT, "mod", "src", "main", "resources", "factoryworks_core", "radar", "radars.json"
)
ASSETS = os.path.join(ROOT, "kubejs", "assets", "factoryworks")
DATA = os.path.join(ROOT, "kubejs", "data", "factoryworks")
NAMESPACE = "factoryworks"

BLOCK_NAME = "radar"
PART_NAME = "radar_part"
FACTORIO_NAME = "radar"

TEXTURE = "minecraft:block/lodestone_side"

BLOCK_LANG = {
    f"block.{NAMESPACE}.{BLOCK_NAME}": "Radar",
    f"block.{NAMESPACE}.{PART_NAME}": "Radar (part)",
}

FIELDS = (
    "energy_usage",
    "energy_per_sector",
    "energy_per_nearby_scan",
    "max_distance_of_sector_revealed",
    "max_distance_of_nearby_sector_revealed",
    "tile_width",
    "tile_height",
)


def radar_from_corpus():
    with open(MACHINE_CORPUS, encoding="utf-8") as handle:
        machine = json.load(handle)
    rows = {row["name"]: row for row in machine.get("radars", [])}
    row = rows.get(FACTORIO_NAME)
    if row is None:
        sys.exit(
            f"{FACTORIO_NAME} is not in {MACHINE_CORPUS}'s radars "
            "-- re-run scripts/factorio-machine-extract.py"
        )
    for field in FIELDS:
        if row.get(field) is None:
            sys.exit(
                f"{FACTORIO_NAME} has no {field} in {MACHINE_CORPUS} "
                "-- re-run scripts/factorio-machine-extract.py"
            )
    return {FACTORIO_NAME: {field: row[field] for field in FIELDS}}


def self_drop_loot_table(block_id):
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "rolls": 1,
                "entries": [{"type": "minecraft:item", "name": block_id}],
            }
        ],
    }


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2)
        handle.write("\n")


def planned_files(rows):
    model_name = f"{NAMESPACE}:block/{BLOCK_NAME}"
    blockstate = {"variants": {"": {"model": model_name}}}
    return {
        RADAR_RESOURCE: rows,
        os.path.join(ASSETS, "blockstates", f"{BLOCK_NAME}.json"): blockstate,
        os.path.join(ASSETS, "blockstates", f"{PART_NAME}.json"): blockstate,
        os.path.join(ASSETS, "models", "block", f"{BLOCK_NAME}.json"): {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": TEXTURE},
        },
        os.path.join(ASSETS, "models", "item", f"{BLOCK_NAME}.json"): {"parent": model_name},
        os.path.join(DATA, "loot_table", "blocks", f"{BLOCK_NAME}.json"): self_drop_loot_table(
            f"{NAMESPACE}:{BLOCK_NAME}"
        ),
    }


def lang_path():
    return os.path.join(ASSETS, "lang", "en_us.json")


def read_lang():
    if not os.path.isfile(lang_path()):
        return {}
    with open(lang_path(), encoding="utf-8") as handle:
        return json.load(handle)


def check():
    files = planned_files(radar_from_corpus())
    problems = []
    for path, expected in files.items():
        if not os.path.isfile(path):
            problems.append(f"missing: {path}")
            continue
        with open(path, encoding="utf-8") as handle:
            if json.load(handle) != expected:
                problems.append(f"stale: {path}")
    existing = read_lang()
    for key, value in BLOCK_LANG.items():
        if existing.get(key) != value:
            problems.append(f"lang key out of date: {key}")
    if problems:
        sys.exit("build-radar-assets.py --check failed:\n" + "\n".join(problems))
    print(f"OK -- {len(files)} files, {len(BLOCK_LANG)} lang keys")


def build():
    files = planned_files(radar_from_corpus())
    for path, data in files.items():
        write(path, data)
    lang = read_lang()
    lang.update(BLOCK_LANG)
    write(lang_path(), lang)
    print(f"wrote {len(files)} files, updated {len(BLOCK_LANG)} lang keys")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    check() if args.check else build()


if __name__ == "__main__":
    main()
