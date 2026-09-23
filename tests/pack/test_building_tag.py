#!/usr/bin/env python3
"""Assert the `planetaryfactory:buildings` block tag is Factorio's placeable entities (#413).

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
TAG = ROOT / "kubejs/data/planetaryfactory/tags/block/buildings.json"

MUST_HOLD = {
    "furnaces": ("planetaryfactory:stone_furnace", "planetaryfactory:steel_furnace",
                 "planetaryfactory:electric_furnace"),
    "poles": ("planetaryfactory:small_electric_pole", "planetaryfactory:medium_electric_pole",
              "planetaryfactory:substation_electric_pole"),
    "Assembling Machines": ("planetaryfactory:assembling_machine",
                            "planetaryfactory:assembling_machine_2",
                            "planetaryfactory:assembling_machine_3"),
    "loaders": ("belts:chute", "belts:improved_chute", "belts:express_chute", "belts:turbo_chute"),
    "splitters": ("belts:splitter", "belts:improved_splitter", "belts:express_splitter",
                  "belts:turbo_splitter"),
    "chests": ("minecraft:chest", "planetaryfactory:iron_chest", "planetaryfactory:steel_chest"),
    "pipes": ("oritech:fluid_pipe",),
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
