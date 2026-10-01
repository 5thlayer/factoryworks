#!/usr/bin/env python3
"""Assert the mod's Replace Groups are the corpus's, block by block (#387, ADR-0082).

`scripts/build-replace-groups.py` joins `data/factorio/machine.json`'s `fast_replaceable_group`
onto `data/pack/item-map.json` and writes the resource the mod reads. Its `--check` only proves the
resource is what the generator would write, so each entry is also traced back: its key is a block
the pack has a blockstate for, or a Library's jar registers, and some corpus row maps to that key and carries that group. #299's
three families are named because a generator that dropped them would still pass both.

Usage: tests/pack/test_replace_groups.py
"""
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
GENERATOR = ROOT / "scripts" / "build-replace-groups.py"
RESOURCE = ROOT / "mod/src/main/resources/factoryworks_core/placement/replace_groups.json"
BLOCKSTATES = ROOT / "kubejs/assets/factoryworks/blockstates"

FAMILIES = (
    "stone-furnace", "steel-furnace", "electric-furnace",
    "small-electric-pole", "medium-electric-pole", "substation",
    "assembling-machine-1", "assembling-machine-2", "assembling-machine-3",
)


def entry_failures(actual):
    machine = json.loads((ROOT / "data/factorio/machine.json").read_text(encoding="utf-8"))
    items = json.loads((ROOT / "data/pack/item-map.json").read_text(encoding="utf-8"))["items"]
    rows = {row["name"]: row for row in machine["machines"] + machine["poles"]}
    target_of = {name: items.get(name, {}).get("target") for name in rows}
    # A Library's blocks, such as Wireworks' poles, whose group the Pack states (#476).
    jar_items = set(json.loads((ROOT / "data/jars/item.json").read_text(encoding="utf-8"))["items"])
    failures = []
    for block, group in sorted(actual.items()):
        namespace, _, path = block.partition(":")
        if not ((BLOCKSTATES / f"{path}.json").is_file() if namespace == "factoryworks" else block in jar_items):
            failures.append(f"{block} is neither a block the pack registers a blockstate for nor one a jar registers")
        sources = [name for name, target in target_of.items() if target == block]
        if not sources:
            failures.append(f"{block} traces to no corpus row")
        for name in sources:
            if rows[name]["fast_replaceable_group"] != group:
                failures.append(f"{block} is {group!r}, but {name}'s group is "
                                f"{rows[name]['fast_replaceable_group']!r}")
    for name in FAMILIES:
        if target_of.get(name) not in actual:
            failures.append(f"{name} has no entry -- #299 would replace nothing there")
    return failures


def main():
    failures = []
    generated = subprocess.run(
        [sys.executable, str(GENERATOR), "--check"], capture_output=True, text=True
    )
    if generated.returncode != 0:
        failures.append(f"{GENERATOR.relative_to(ROOT)} --check: "
                        f"{(generated.stderr or generated.stdout).strip()}")

    if not RESOURCE.is_file():
        failures.append(f"no {RESOURCE.relative_to(ROOT)}")
    else:
        failures += entry_failures(json.loads(RESOURCE.read_text(encoding="utf-8")))

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print(f"ok   {len(json.loads(RESOURCE.read_text()))} blocks carry the corpus's group")
    return 0


if __name__ == "__main__":
    sys.exit(main())
