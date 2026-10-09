#!/usr/bin/env python3
"""Assert the mod's Replace Groups name real blocks, and the families fast replace exists for (#387, ADR-0082).

The resource is hand-owned data (#599). Each key is a block the pack has a blockstate for, or a
Library's jar registers, and #299's three families stay named.

Usage: tests/pack/test_replace_groups.py
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
RESOURCE = ROOT / "mod/src/main/resources/factoryworks_core/placement/replace_groups.json"
BLOCKSTATES = ROOT / "kubejs/assets/factoryworks/blockstates"

FAMILIES = (
    "stone-furnace", "steel-furnace", "electric-furnace",
    "small-electric-pole", "medium-electric-pole", "substation",
)


def entry_failures(actual):
    items = json.loads((ROOT / "data/pack/item-map.json").read_text(encoding="utf-8"))["items"]
    # A Library's blocks, such as Wireworks' poles, whose group the Pack states (#476).
    jar_items = set(json.loads((ROOT / "data/jars/item.json").read_text(encoding="utf-8"))["items"])
    failures = []
    for block in sorted(actual):
        namespace, _, path = block.partition(":")
        if not ((BLOCKSTATES / f"{path}.json").is_file() if namespace == "factoryworks" else block in jar_items):
            failures.append(f"{block} is neither a block the pack registers a blockstate for nor one a jar registers")
    for name in FAMILIES:
        if items.get(name, {}).get("target") not in actual:
            failures.append(f"{name} has no entry -- #299 would replace nothing there")
    return failures


def main():
    failures = []
    if not RESOURCE.is_file():
        failures.append(f"no {RESOURCE.relative_to(ROOT)}")
    else:
        failures += entry_failures(json.loads(RESOURCE.read_text(encoding="utf-8")))

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print(f"ok   {len(json.loads(RESOURCE.read_text()))} blocks name a real block")
    return 0


if __name__ == "__main__":
    sys.exit(main())
