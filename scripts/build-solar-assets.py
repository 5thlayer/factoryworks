#!/usr/bin/env python3
"""Emit the Solar Panel's corpus row and pack-side assets (#529).

`data/factorio/machine.json`'s `solar_panels` row is copied field by field to
`mod/src/main/resources/factoryworks_core/energy/solar_panels.json`, which `SolarPanelCorpus` reads
at class-init. Turning watts into FE is `SolarPanelSpec`'s, where a unit test holds it.

The panel is drawn by Oritech's GeckoLib renderer, so the block model is only its particle and
breaking texture, as the Steam Engine's is.

Usage:

    scripts/build-solar-assets.py            # writes the resource and the pack assets
    scripts/build-solar-assets.py --check    # asserts both are already up to date; no writes
"""
import argparse
import json
import os
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
MACHINE_CORPUS = os.path.join(ROOT, "data", "factorio", "machine.json")
SOLAR_RESOURCE = os.path.join(
    ROOT, "mod", "src", "main", "resources", "factoryworks_core", "energy", "solar_panels.json"
)
ASSETS = os.path.join(ROOT, "kubejs", "assets", "factoryworks")
DATA = os.path.join(ROOT, "kubejs", "data", "factoryworks")
NAMESPACE = "factoryworks"

BLOCK_NAME = "solar_panel"
PART_NAME = "solar_panel_part"
FACTORIO_NAME = "solar-panel"

BLOCK_LANG = {
    f"block.{NAMESPACE}.{BLOCK_NAME}": "Solar Panel",
    f"block.{NAMESPACE}.{PART_NAME}": "Solar Panel (part)",
    f"item.{NAMESPACE}.{BLOCK_NAME}": "Solar Panel",
}

FIELDS = ("production", "tile_width", "tile_height")


def panel_from_corpus():
    with open(MACHINE_CORPUS, encoding="utf-8") as handle:
        machine = json.load(handle)
    rows = {row["name"]: row for row in machine.get("solar_panels", [])}
    row = rows.get(FACTORIO_NAME)
    if row is None:
        sys.exit(
            f"{FACTORIO_NAME} is not in {MACHINE_CORPUS}'s solar_panels "
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
        SOLAR_RESOURCE: rows,
        os.path.join(ASSETS, "blockstates", f"{BLOCK_NAME}.json"): blockstate,
        os.path.join(ASSETS, "blockstates", f"{PART_NAME}.json"): blockstate,
        os.path.join(ASSETS, "models", "block", f"{BLOCK_NAME}.json"): {
            "parent": "minecraft:block/cube_all",
            "textures": {
                "all": "oritech:block/machine_frame_block",
                "particle": "oritech:block/machine_particle_texture",
            },
        },
        os.path.join(ASSETS, "models", "item", f"{BLOCK_NAME}.json"): {
            "parent": "oritech:item/big_solar_panel"
        },
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
    files = planned_files(panel_from_corpus())
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
        sys.exit("build-solar-assets.py --check failed:\n" + "\n".join(problems))
    print(f"OK -- {len(files)} files, {len(BLOCK_LANG)} lang keys")


def build():
    files = planned_files(panel_from_corpus())
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
