#!/usr/bin/env python3
"""Assert the `factoryworks:buildings` block tag is Factorio's placeable entities (#413).

`scripts/build-building-tag.py --check` only proves the tag is what the generator would write, so
each entry is also traced back to a Building row of `data/factorio/building.json` through the item
map, and the families the break rule exists for are named: a generator that dropped them would
still pass the `--check`. The corpus is re-extracted and compared when the dump is on disk.

Usage: tests/pack/test_building_tag.py
"""
import importlib.util
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
GENERATOR = ROOT / "scripts" / "build-building-tag.py"
EXTRACTOR = ROOT / "scripts" / "factorio-building-extract.py"
CORPUS = ROOT / "data/factorio/building.json"
TAG = ROOT / "kubejs/data/factoryworks/tags/block/buildings.json"

MUST_HOLD = {
    "furnaces": ("factoryworks:stone_furnace", "factoryworks:steel_furnace",
                 "factoryworks:electric_furnace"),
    "poles": ("wireworks:small_pole", "wireworks:medium_pole",
              "wireworks:large_pole"),
    "Assemblers": ("craftworks:assembler_1", "craftworks:assembler_2", "craftworks:assembler_3"),
    "loaders": ("beltworks:loader", "beltworks:improved_loader", "beltworks:express_loader",
                "beltworks:turbo_loader"),
    "feeders": ("beltworks:feeder", "beltworks:improved_feeder", "beltworks:express_feeder",
                "beltworks:turbo_feeder"),
    "belts": ("beltworks:belt_tile", "beltworks:improved_belt_tile", "beltworks:express_belt_tile",
              "beltworks:turbo_belt_tile"),
    "splitters": ("beltworks:splitter", "beltworks:improved_splitter", "beltworks:express_splitter",
                  "beltworks:turbo_splitter"),
    "chests": ("minecraft:chest", "factoryworks:iron_chest", "factoryworks:steel_chest"),
    "pipes": ("pipeworks:pipe",),
}

# Blocks a player digs up close, and which item-map rows name something that is not a Building.
MUST_NOT_HOLD = ("minecraft:stone_bricks", "minecraft:gray_concrete", "minecraft:cobblestone")
NOT_BUILDINGS = ("stone-brick", "concrete", "landfill", "jellynut-seed", "car", "locomotive")


def corpus_failures(corpus):
    rows = {row["item"]: row for row in corpus}
    failures = [f"{name} is marked a Building" for name in NOT_BUILDINGS
                if rows.get(name, {}).get("building", True)]
    if not rows.get("rail", {}).get("building"):
        failures.append("the rail planner is not a Building")
    spec = importlib.util.spec_from_file_location("extractor", EXTRACTOR)
    extractor = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(extractor)
    if extractor.DEFAULT_DUMP.is_file():
        fresh = extractor.extract(json.loads(extractor.DEFAULT_DUMP.read_text(encoding="utf-8")))
        if fresh != corpus:
            failures.append(f"{CORPUS.relative_to(ROOT)} is stale against the dump -- "
                            f"re-run {EXTRACTOR.relative_to(ROOT)}")
    else:
        print(f"skip the dump is not on disk, so {CORPUS.name} is not re-extracted")
    return failures


def tag_failures(values, corpus):
    items = json.loads((ROOT / "data/pack/item-map.json").read_text(encoding="utf-8"))["items"]
    buildings = {row["item"] for row in corpus if row["building"]}
    failures = []
    for block in values:
        sources = [name for name, row in items.items() if row.get("target") == block]
        if not any(name in buildings for name in sources):
            failures.append(f"{block} traces to no Building row")
        if block.endswith("_ore") or block.endswith("_log") or block in MUST_NOT_HOLD:
            failures.append(f"{block} is in the tag, and a player digs it up close")
    for family, blocks in MUST_HOLD.items():
        failures += [f"{block} is not in the tag ({family})" for block in blocks if block not in values]
    return failures


def main():
    failures = []
    generated = subprocess.run([sys.executable, str(GENERATOR), "--check"],
                               capture_output=True, text=True)
    if generated.returncode != 0:
        failures.append(f"{GENERATOR.relative_to(ROOT)} --check: "
                        f"{(generated.stderr or generated.stdout).strip()}")

    corpus = json.loads(CORPUS.read_text(encoding="utf-8"))["items"]
    failures += corpus_failures(corpus)
    values = json.loads(TAG.read_text(encoding="utf-8"))["values"]
    failures += tag_failures(values, corpus)

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print(f"ok   {len(values)} Buildings trace to Factorio's placeable entities")
    return 0


if __name__ == "__main__":
    sys.exit(main())
