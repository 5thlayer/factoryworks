#!/usr/bin/env python3
"""Emit the Pumpjack's and the oil well's corpus row and pack-side assets (#377, ADR-0081).

`data/factorio/machine.json`'s `pumpjack` drill row is copied field by field to
`mod/src/main/resources/factoryworks_core/oil/pumpjack.json`, which `PumpjackCorpus` reads at
class-init, beside the fluid it pumps: the `crude-oil` row of `data/pack/item-map.json`. Turning
watts into FE is `PumpjackSpec`'s, where a unit test holds it.

The Pumpjack is stand-in art: plain cubes of vanilla textures (#621), cut into one slice per block
of its 3x3x2 footprint so that every block draws its own part and none is invisible. The anchor and
each part have a model, turned by facing; the item draws the cubes whole. The well's texture is a
placeholder too.

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


FACES = ("north", "east", "south", "west", "up", "down")
MAX_PARTS = 26  # FootprintPartBlock.PART's range: every state needs a variant
PART_COUNT = 17  # PumpjackFootprint: 3x3x2, less the anchor
LAYERS = 2

# The anchor's frame at facing north: x east, z south, the anchor block at 0..16. Parts are numbered
# as Footprint.standing numbers them, a local (x, y, z) standing at world (z, y, x).
PART_BLOCKS = [(z, y, x) for y in range(LAYERS) for x in (-1, 0, 1) for z in (-1, 0, 1)
               if (x, y, z) != (0, 0, 0)]


def cube(name, low, high, texture):
    return {"name": name, "from": low, "to": high, "texture": texture}


# The whole machine, -16..32 across and 0..32 up, inside a vanilla model's reach (ADR-0111). The beam
# runs along x. Every block of the 3x3x2 holds at least one piece, so none draws nothing. Where two
# cubes meet, one ends strictly inside the other, since faces sharing a plane flicker.
CUBES = [
    cube("base", [-16, 0, -16], [32, 6, 32], "#dark"),
    cube("motor", [20, 6, 18], [28, 14, 26], "#body"),
    cube("tower", [6.5, 6, 6.5], [9.5, 24, 9.5], "#body"),
    cube("crossbar", [6, 16, -12], [10, 20, 28], "#dark"),
    cube("post_nw", [-14, 6, -14], [-10, 30, -10], "#body"),
    cube("post_ne", [26, 6, -14], [30, 30, -10], "#body"),
    cube("post_sw", [-14, 6, 26], [-10, 30, 30], "#body"),
    cube("post_se", [26, 6, 26], [30, 30, 30], "#body"),
    cube("beam", [-13, 22, 6], [30, 26, 10], "#dark"),
    cube("head", [-14, 14, 5], [-10, 28, 11], "#body"),
    cube("rod", [-13, 6, 7.5], [-11, 14, 8.5], "#body"),
    cube("counterweight", [24, 14, 5], [28, 27, 11], "#body"),
]
TEXTURES = {"body": BODY, "dark": DARK, "particle": BODY}

# Which axes a face's texture runs along, and which end of the cube it sits on.
FACE_AXES = {"north": (0, 1, 2, "lo"), "south": (0, 1, 2, "hi"), "west": (2, 1, 0, "lo"),
             "east": (2, 1, 0, "hi"), "up": (0, 2, 1, "hi"), "down": (0, 2, 1, "lo")}


def slice_element(box, offset):
    """The part of one cube inside the block at `offset`, moved into that block's 0..16; None if empty."""
    low = [max(box["from"][i], offset[i] * 16) for i in range(3)]
    high = [min(box["to"][i], offset[i] * 16 + 16) for i in range(3)]
    if any(low[i] >= high[i] for i in range(3)):
        return None
    fraction = lambda axis, v: (v - box["from"][axis]) / (box["to"][axis] - box["from"][axis]) * 16
    faces = {}
    for face in FACES:
        u, v, normal, end = FACE_AXES[face]
        cut = low[normal] != box["from"][normal] if end == "lo" else high[normal] != box["to"][normal]
        if cut:
            continue
        v0, v1 = fraction(v, low[v]), fraction(v, high[v])
        if face not in ("up", "down"):
            v0, v1 = 16 - v1, 16 - v0
        faces[face] = {"uv": [fraction(u, low[u]), v0, fraction(u, high[u]), v1], "texture": box["texture"]}
    return {
        "name": box["name"],
        "from": [low[i] - offset[i] * 16 for i in range(3)],
        "to": [high[i] - offset[i] * 16 for i in range(3)],
        "faces": faces,
    }


def slice_model(offset):
    elements = [e for e in (slice_element(box, offset) for box in CUBES) if e is not None]
    if not elements:
        sys.exit(f"the block at {offset} of the Pumpjack draws nothing")
    return {"textures": TEXTURES, "elements": elements}


def pumpjack_model():
    return slice_model((0, 0, 0))


def whole_model():
    """Every cube whole, for the item."""
    elements = []
    for box in CUBES:
        element = {"name": box["name"], "from": box["from"], "to": box["to"],
                   "faces": {face: {"uv": [0, 0, 16, 16], "texture": box["texture"]} for face in FACES}}
        elements.append(element)
    return {"textures": TEXTURES, "elements": elements}


# The item model has no parent to lend it block transforms, so it scales itself down, as
# Craftworks' multi-block machines do.
ITEM_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, -2, 0], "scale": [0.36, 0.36, 0.36]},
    "ground": {"translation": [0, 3, 0], "scale": [0.12, 0.12, 0.12]},
    "fixed": {"scale": [0.2, 0.2, 0.2]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.1, 0.1, 0.1]},
    "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.1, 0.1, 0.1]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.12, 0.12, 0.12]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.12, 0.12, 0.12]},
}

def part_blockstate():
    variants = {}
    for facing, y in FACINGS.items():
        for part in range(1, MAX_PARTS + 1):
            model = f"{NAMESPACE}:block/{PART_NAME}_{min(part, PART_COUNT)}"
            variants[f"facing={facing},part={part}"] = {"model": model} | ({"y": y} if y else {})
    return {"variants": variants}


def anchor_blockstate(model):
    return {"variants": {f"facing={facing}": ({"model": model} | ({"y": y} if y else {}))
                         for facing, y in FACINGS.items()}}


def planned_files(rows):
    machine = f"{NAMESPACE}:block/{BLOCK_NAME}"
    well = f"{NAMESPACE}:block/{WELL_NAME}"
    part_models = {
        os.path.join(ASSETS, "models", "block", f"{PART_NAME}_{index}.json"): slice_model(offset)
        for index, offset in enumerate(PART_BLOCKS, 1)
    }
    return {
        **part_models,
        RESOURCE: rows,
        os.path.join(ASSETS, "blockstates", f"{BLOCK_NAME}.json"): anchor_blockstate(machine),
        os.path.join(ASSETS, "blockstates", f"{PART_NAME}.json"): part_blockstate(),
        os.path.join(ASSETS, "models", "block", f"{BLOCK_NAME}.json"): pumpjack_model(),
        os.path.join(ASSETS, "models", "item", f"{BLOCK_NAME}.json"): whole_model() | {"display": ITEM_DISPLAY},
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
