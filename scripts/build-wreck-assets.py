#!/usr/bin/env python3
"""Emit the wreck's corpus row and pack-side assets (ADR-0107).

The cargo hold's slot count is Factorio's `crash-site-spaceship` `inventory_size`. This copies the
row out of `data/factorio/container.json` into
`mod/src/main/resources/factoryworks_core/wreck/containers.json`, which `CargoHoldCorpus` reads,
and each debris size class's `mining_time` into `.../wreck/debris.json`, which `DebrisCorpus`
reads (#550). It writes the blockstates, models and lang names of the wreck's blocks and its
debris. None has an item, a loot table or an item model.

Usage:

    scripts/build-wreck-assets.py            # writes the resource and the pack assets
    scripts/build-wreck-assets.py --check    # asserts both are already up to date; no writes
"""
import argparse
import json
import os
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
CONTAINER_CORPUS = os.path.join(ROOT, "data", "factorio", "container.json")
RESOURCE = os.path.join(
    ROOT, "mod", "src", "main", "resources", "factoryworks_core", "wreck", "containers.json"
)
DEBRIS_RESOURCE = os.path.join(
    ROOT, "mod", "src", "main", "resources", "factoryworks_core", "wreck", "debris.json"
)
ASSETS = os.path.join(ROOT, "kubejs", "assets", "factoryworks")
NAMESPACE = "factoryworks"

FACTORIO_NAME = "crash-site-spaceship"
REQUIRED_FIELDS = ("inventory_size",)

# Display choices: vanilla art until the wreck has its own.
BLOCKS = {
    "wreck_hull": ("Wreck Hull", "minecraft:block/iron_block"),
    "wreck_window": ("Wreck Window", "minecraft:block/tinted_glass"),
    "cargo_hold": ("Cargo Hold", "minecraft:block/chiseled_copper"),
    "wreck_debris_big": ("Big Wreck Debris", "minecraft:block/raw_iron_block"),
    "wreck_debris_medium": ("Medium Wreck Debris", "minecraft:block/raw_iron_block"),
    "wreck_debris_small": ("Small Wreck Debris", "minecraft:block/raw_iron_block"),
}
DEBRIS_PREFIX = "crash-site-spaceship-wreck-"
DEBRIS_CLASSES = ("big", "medium", "small")
# The hull's bevel, drawn in the hull's texture.
SHAPED = {
    "wreck_hull_stairs": "Wreck Hull Stairs",
    "wreck_hull_slab": "Wreck Hull Slab",
}
FACING_Y = {"east": 0, "south": 90, "west": 180, "north": 270}


def stairs_blockstate(model):
    variants = {}
    for facing, y in FACING_Y.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                suffix = "" if shape == "straight" else "_" + shape.split("_")[0]
                turn = y
                if shape.endswith("left") and half == "bottom":
                    turn -= 90
                if shape.endswith("right") and half == "top":
                    turn += 90
                variant = {"model": model + suffix}
                if half == "top":
                    variant["x"] = 180
                if turn % 360:
                    variant["y"] = turn % 360
                if variant.keys() - {"model"}:
                    variant["uvlock"] = True
                variants[f"facing={facing},half={half},shape={shape}"] = variant
    return {"variants": variants}


def row_from_corpus():
    with open(CONTAINER_CORPUS, encoding="utf-8") as handle:
        corpus = json.load(handle)
    rows = {row["name"]: row for row in corpus.get("containers", [])}
    row = rows.get(FACTORIO_NAME)
    if row is None:
        sys.exit(f"{FACTORIO_NAME} is not in {CONTAINER_CORPUS} "
                 "-- re-run scripts/factorio-container-extract.py")
    for field in REQUIRED_FIELDS:
        if row.get(field) is None:
            sys.exit(f"{FACTORIO_NAME} has no {field} in {CONTAINER_CORPUS} "
                     "-- re-run scripts/factorio-container-extract.py")
    return {FACTORIO_NAME: {field: row[field] for field in REQUIRED_FIELDS}}


def debris_from_corpus():
    """Each size class's one mining time, read off every prototype of the class."""
    with open(CONTAINER_CORPUS, encoding="utf-8") as handle:
        rows = json.load(handle).get("debris", [])
    classes = {}
    for size in DEBRIS_CLASSES:
        times = {row["minable"]["mining_time"] for row in rows
                 if row["name"].startswith(f"{DEBRIS_PREFIX}{size}-")}
        if len(times) != 1:
            sys.exit(f"{size} debris has mining times {sorted(times)} in {CONTAINER_CORPUS} "
                     "-- re-run scripts/factorio-container-extract.py")
        classes[size] = {"mining_time": times.pop()}
    return classes


def planned_files():
    files = {RESOURCE: row_from_corpus(), DEBRIS_RESOURCE: debris_from_corpus()}
    for name, (_, texture) in BLOCKS.items():
        model = f"{NAMESPACE}:block/{name}"
        variants = {"": {"model": model}}
        if name == "cargo_hold":
            # Same model either way; the property only says which block owns the inventory (#548).
            variants = {f"anchor={flag}": {"model": model} for flag in ("false", "true")}
        if name == "wreck_hull":
            # One model until the hull has art of its own (#551).
            variants = {f"scorched={flag}": {"model": model} for flag in ("false", "true")}
        files[os.path.join(ASSETS, "blockstates", f"{name}.json")] = {"variants": variants}
        files[os.path.join(ASSETS, "models", "block", f"{name}.json")] = {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": texture},
        }
    hull = BLOCKS["wreck_hull"][1]
    sides = {"bottom": hull, "top": hull, "side": hull}
    stairs = f"{NAMESPACE}:block/wreck_hull_stairs"
    files[os.path.join(ASSETS, "blockstates", "wreck_hull_stairs.json")] = stairs_blockstate(stairs)
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        files[os.path.join(ASSETS, "models", "block", f"wreck_hull_stairs{suffix}.json")] = {
            "parent": f"minecraft:block/{parent}",
            "textures": sides,
        }
    slab = f"{NAMESPACE}:block/wreck_hull_slab"
    files[os.path.join(ASSETS, "blockstates", "wreck_hull_slab.json")] = {"variants": {
        "type=bottom": {"model": slab},
        "type=top": {"model": slab + "_top"},
        "type=double": {"model": f"{NAMESPACE}:block/wreck_hull"},
    }}
    for suffix, parent in (("", "slab"), ("_top", "slab_top")):
        files[os.path.join(ASSETS, "models", "block", f"wreck_hull_slab{suffix}.json")] = {
            "parent": f"minecraft:block/{parent}",
            "textures": sides,
        }
    lang = {f"block.{NAMESPACE}.{name}": display for name, (display, _) in BLOCKS.items()}
    lang.update({f"block.{NAMESPACE}.{name}": display for name, display in SHAPED.items()})
    return files, lang


def lang_path():
    return os.path.join(ASSETS, "lang", "en_us.json")


def read_json(path):
    with open(path, encoding="utf-8") as handle:
        return json.load(handle)


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2)
        handle.write("\n")


def check():
    files, lang = planned_files()
    problems = []
    for path, expected in files.items():
        if not os.path.isfile(path):
            problems.append(f"missing: {path}")
        elif read_json(path) != expected:
            problems.append(f"stale: {path}")
    existing = read_json(lang_path()) if os.path.isfile(lang_path()) else {}
    for key, value in lang.items():
        if existing.get(key) != value:
            problems.append(f"lang key out of date: {key}")
    if problems:
        sys.exit("build-wreck-assets.py --check failed:\n" + "\n".join(problems))
    print(f"OK -- {len(files)} files, {len(lang)} lang keys")


def build():
    files, lang = planned_files()
    for path, data in files.items():
        write(path, data)
    existing = read_json(lang_path()) if os.path.isfile(lang_path()) else {}
    existing.update(lang)
    write(lang_path(), existing)
    print(f"wrote {len(files)} files, updated {len(lang)} lang keys")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    check() if args.check else build()


if __name__ == "__main__":
    main()
