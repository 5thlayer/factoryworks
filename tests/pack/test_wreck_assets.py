#!/usr/bin/env python3
"""Assert the wreck's generated halves still agree with the corpus and with what registers them (ADR-0107).

  - **The slot count.** `scripts/build-wreck-assets.py` copies `crash-site-spaceship`'s row out of
    `data/factorio/container.json` into the resource `CargoHoldCorpus` reads. A hand-edited
    resource would size the hold by a number somebody chose, and nothing else would fail, so the
    copy is held to the corpus field by field.
  - **The screen's shape.** The hold opens vanilla's hopper screen, which is five slots and
    refuses any other size. The corpus has to still say five.
  - **No item.** None of the wreck blocks has an item, an item model, an item definition or a
    loot table.

Whether the blockstates, models, textures and lang keys resolve is `test_block_assets.py`'s.

Usage: tests/pack/test_wreck_assets.py
"""

import json
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
RESOURCE = ROOT / "mod/src/main/resources/factoryworks_core/wreck/containers.json"
CORPUS = ROOT / "data/factorio/container.json"
GENERATOR = ROOT / "scripts/build-wreck-assets.py"
ASSETS = ROOT / "kubejs/assets/factoryworks"
LOOT = ROOT / "kubejs/data/factoryworks/loot_table/blocks"

FACTORIO_NAME = "crash-site-spaceship"
COPIED_FIELDS = ("inventory_size",)
BLOCKS = ("wreck_hull", "wreck_hull_stairs", "wreck_hull_slab", "wreck_window", "cargo_hold")
HOPPER_SLOTS = 5


def main():
    failures = []

    run = subprocess.run([sys.executable, str(GENERATOR), "--check"], capture_output=True, text=True)
    if run.returncode != 0:
        failures.append(f"the generator's --check failed:\n{run.stdout}{run.stderr}")

    rows = {row["name"]: row for row in json.loads(CORPUS.read_text())["containers"]}
    row = rows.get(FACTORIO_NAME)
    mod_row = json.loads(RESOURCE.read_text()).get(FACTORIO_NAME) if RESOURCE.is_file() else None
    if row is None:
        failures.append(f"{FACTORIO_NAME} is not in container.json")
    elif mod_row is None:
        failures.append(f"containers.json carries no {FACTORIO_NAME} row")
    else:
        for field in COPIED_FIELDS:
            if mod_row.get(field) != row.get(field):
                failures.append(
                    f"{field} is {mod_row.get(field)!r} in containers.json but {row.get(field)!r} "
                    "in the corpus -- every number here is extracted")
        if row.get("inventory_size") != HOPPER_SLOTS:
            failures.append(
                f"the corpus states inventory_size {row.get('inventory_size')!r}, not {HOPPER_SLOTS} "
                "-- the cargo hold opens vanilla's hopper screen, which is exactly five slots")

    for name in BLOCKS:
        for path in (ASSETS / f"items/{name}.json", ASSETS / f"models/item/{name}.json",
                     LOOT / f"{name}.json"):
            if path.exists():
                failures.append(f"{path.relative_to(ROOT)} exists, but {name} has no item (ADR-0107)")

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print("ok   the wreck's resource matches the corpus and its blocks have no item")
    return 0


if __name__ == "__main__":
    sys.exit(main())
