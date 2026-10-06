#!/usr/bin/env python3
"""Emit the Pumpjack's and the oil well's corpus row and pack-side assets (#377, ADR-0081).

`data/factorio/machine.json`'s `pumpjack` drill row is copied field by field to
`mod/src/main/resources/factoryworks_core/oil/pumpjack.json`, which `PumpjackCorpus` reads at
class-init, beside the fluid it pumps: the `crude-oil` row of `data/pack/item-map.json`. Turning
watts into FE is `PumpjackSpec`'s, where a unit test holds it.

The Pumpjack is stand-in art: plain cubes of vanilla textures over its footprint (#621), which a
block model draws from its anchor, turned by facing. The well's texture is a placeholder too.

Usage:

    scripts/build-pumpjack-assets.py            # writes the resource and the pack assets
    scripts/build-pumpjack-assets.py --check    # asserts both are already up to date; no writes
"""
import argparse
import json
import os
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
MACHINE_CORPUS = os.path.join(ROOT, "data", "factorio", "machine.json")
ITEM_MAP = os.path.join(ROOT, "data", "pack", "item-map.json")
RESOURCE = os.path.join(ROOT, "mod", "src", "main", "resources", "factoryworks_core", "oil", "pumpjack.json")
ASSETS = os.path.join(ROOT, "kubejs", "assets", "factoryworks")
DATA = os.path.join(ROOT, "kubejs", "data", "factoryworks")
NAMESPACE = "factoryworks"

BLOCK_NAME = "pumpjack"
PART_NAME = "pumpjack_part"
WELL_NAME = "oil_well"
FACTORIO_NAME = "pumpjack"
CRUDE = "crude-oil"

LANG = {
    f"block.{NAMESPACE}.{BLOCK_NAME}": "Pumpjack",
    f"block.{NAMESPACE}.{PART_NAME}": "Pumpjack (part)",
    f"item.{NAMESPACE}.{BLOCK_NAME}": "Pumpjack",
    f"block.{NAMESPACE}.{WELL_NAME}": "Oil Well",
    f"message.{NAMESPACE}.{BLOCK_NAME}.no_well": "A Pumpjack must stand on an oil well.",
    f"fluid_type.{NAMESPACE}.crude_oil": "Crude Oil",
    f"map.{NAMESPACE}.patch.crude_oil": "Crude oil",
    f"map.{NAMESPACE}.patch.yield": "%s: %s yield",
}

FIELDS = ("mining_speed", "energy_usage", "drain", "tile_width", "tile_height")


def pumpjack_from_corpus():
    with open(MACHINE_CORPUS, encoding="utf-8") as handle:
        machine = json.load(handle)
    row = {row["name"]: row for row in machine.get("drills", [])}.get(FACTORIO_NAME)
    if row is None:
        sys.exit(f"{FACTORIO_NAME} is not in {MACHINE_CORPUS}'s drills -- re-run scripts/factorio-machine-extract.py")
    for field in FIELDS:
        if row.get(field) is None:
            sys.exit(f"{FACTORIO_NAME} has no {field} in {MACHINE_CORPUS} -- re-run scripts/factorio-machine-extract.py")
    boxes = [box for box in row["fluid_boxes"] if box["name"] == "output_fluid_box"]
    if len(boxes) != 1:
        sys.exit(f"{FACTORIO_NAME} has {len(boxes)} output fluid boxes, not one")
    with open(ITEM_MAP, encoding="utf-8") as handle:
        crude = json.load(handle)["items"].get(CRUDE) or {}
    if crude.get("kind") != "fluid" or not crude.get("target"):
        sys.exit(f"the item map's {CRUDE} row names no fluid for the Pumpjack to pump")
    return {
        FACTORIO_NAME: {
            **{field: row[field] for field in FIELDS},
            "output_volume": boxes[0]["volume"],
            "fluid": crude["target"],
        }
    }


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2)
        handle.write("\n")


FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
BODY = "minecraft:block/iron_block"
DARK = "minecraft:block/black_concrete"


def cube(name, low, high, texture):
    return {
        "name": name,
        "from": low,
        "to": high,
        "faces": {face: {"uv": [0, 0, 16, 16], "texture": texture}
                  for face in ("north", "east", "south", "west", "up", "down")},
    }


# The footprint is 3x3 wide and the anchor is its middle column, so the model spans -16..32 across
# and a vanilla model cannot rise past 32 (ADR-0111). The beam runs along x.
def pumpjack_model():
    return {
        "textures": {"body": BODY, "dark": DARK, "particle": BODY},
        "elements": [
            cube("base", [-12, 0, -12], [28, 3, 28], "#dark"),
            cube("tower", [6, 3, 6], [10, 24, 10], "#body"),
            cube("beam", [-12, 22, 6], [28, 26, 10], "#dark"),
            cube("head", [-14, 14, 5], [-10, 28, 11], "#body"),
            cube("rod", [-13, 3, 7.5], [-11, 14, 8.5], "#body"),
            cube("counterweight", [24, 14, 5], [28, 26, 11], "#body"),
        ],
    }


def anchor_blockstate(model):
    return {"variants": {f"facing={facing}": ({"model": model} | ({"y": y} if y else {}))
                         for facing, y in FACINGS.items()}}


def planned_files(rows):
    machine = f"{NAMESPACE}:block/{BLOCK_NAME}"
    well = f"{NAMESPACE}:block/{WELL_NAME}"
    return {
        RESOURCE: rows,
        os.path.join(ASSETS, "blockstates", f"{BLOCK_NAME}.json"): anchor_blockstate(machine),
        os.path.join(ASSETS, "blockstates", f"{PART_NAME}.json"): {"variants": {"": {"model": machine}}},
        os.path.join(ASSETS, "models", "block", f"{BLOCK_NAME}.json"): pumpjack_model(),
        os.path.join(ASSETS, "models", "item", f"{BLOCK_NAME}.json"): {"parent": machine},
        os.path.join(ASSETS, "blockstates", f"{WELL_NAME}.json"): {"variants": {"": {"model": well}}},
        os.path.join(ASSETS, "models", "block", f"{WELL_NAME}.json"): {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {
                "top": "minecraft:block/black_concrete_powder",
                "bottom": "minecraft:block/stone",
                "side": "minecraft:block/stone",
            },
        },
        os.path.join(DATA, "loot_table", "blocks", f"{BLOCK_NAME}.json"): {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NAMESPACE}:{BLOCK_NAME}"}]}],
        },
    }


def lang_path():
    return os.path.join(ASSETS, "lang", "en_us.json")


def read_lang():
    with open(lang_path(), encoding="utf-8") as handle:
        return json.load(handle)


def check():
    files = planned_files(pumpjack_from_corpus())
    problems = []
    for path, expected in files.items():
        if not os.path.isfile(path):
            problems.append(f"missing: {path}")
            continue
        with open(path, encoding="utf-8") as handle:
            if json.load(handle) != expected:
                problems.append(f"stale: {path}")
    existing = read_lang()
    problems += [f"lang key out of date: {key}" for key, value in LANG.items() if existing.get(key) != value]
    if problems:
        sys.exit("build-pumpjack-assets.py --check failed:\n" + "\n".join(problems))
    print(f"OK -- {len(files)} files, {len(LANG)} lang keys")


def build():
    files = planned_files(pumpjack_from_corpus())
    for path, data in files.items():
        write(path, data)
    lang = read_lang()
    lang.update(LANG)
    write(lang_path(), lang)
    print(f"wrote {len(files)} files, updated {len(LANG)} lang keys")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    check() if args.check else build()


if __name__ == "__main__":
    main()
