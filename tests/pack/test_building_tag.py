#!/usr/bin/env python3
"""Assert the `factoryworks:buildings` block tag holds the families the break rule exists for (#413).

The tag is hand-owned data (#599). Nothing a player digs up close may be in it.

Usage: tests/pack/test_building_tag.py
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
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

MUST_NOT_HOLD = ("minecraft:stone_bricks", "minecraft:gray_concrete", "minecraft:cobblestone")


def tag_failures(values):
    failures = []
    for block in values:
        if block.endswith("_ore") or block.endswith("_log") or block in MUST_NOT_HOLD:
            failures.append(f"{block} is in the tag, and a player digs it up close")
    for family, blocks in MUST_HOLD.items():
        failures += [f"{block} is not in the tag ({family})" for block in blocks if block not in values]
    return failures


def main():
    values = json.loads(TAG.read_text(encoding="utf-8"))["values"]
    failures = tag_failures(values)

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print(f"ok   the tag holds {len(values)} Buildings, and none a player digs up close")
    return 0


if __name__ == "__main__":
    sys.exit(main())
