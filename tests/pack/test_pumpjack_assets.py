#!/usr/bin/env python3
"""Assert the Pumpjack's and the oil well's generated halves, and that crude has no other source (#377).

  - `scripts/build-pumpjack-assets.py --check` holds the resource, blockstates, models, lang and loot
    table current against `data/factorio/machine.json` and the item map.
  - The resource is compared with the corpus row field by field here too, and its fluid with the item
    map's `crude-oil` row, so a hand-edited resource still fails.
  - The block names the generator writes files for are the ones `PFBlocks` registers.
  - The refusal message and the oil field's marker have lang keys, read out of the code that names
    them, since a missing one shows the raw key on the gesture or the map.
  - Both of Oritech's `oil_spring` biome modifiers add no feature to no biome: the well is the only
    crude source in the pack (ADR-0081). NeoForge 26.1 registers `none` for structure modifiers
    only, so the no-op is an empty `add_features`.

Usage: tests/pack/test_pumpjack_assets.py
"""

import json
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
RESOURCE = ROOT / "mod/src/main/resources/factoryworks_core/oil/pumpjack.json"
MACHINE_CORPUS = ROOT / "data/factorio/machine.json"
ITEM_MAP = ROOT / "data/pack/item-map.json"
GENERATOR = ROOT / "scripts/build-pumpjack-assets.py"
PF_BLOCKS = ROOT / "mod/src/main/java/com/factoryworks/core/PFBlocks.java"
ITEM = ROOT / "mod/src/main/java/com/factoryworks/core/oil/PumpjackItem.java"
SECTOR_PATCHES = ROOT / "mod/src/main/java/com/factoryworks/core/radar/SectorPatches.java"
LANG = ROOT / "kubejs/assets/factoryworks/lang/en_us.json"
SPRINGS = ROOT / "kubejs/data/oritech/neoforge/biome_modifier"
ORITECH_SPRINGS = ("oil_spring", "oil_spring_desert")


def main():
    failures = []
    generated = subprocess.run([sys.executable, str(GENERATOR), "--check"], capture_output=True, text=True)
    if generated.returncode != 0:
        failures.append(f"{GENERATOR.relative_to(ROOT)} --check: {generated.stderr.strip()}")

    corpus = {r["name"]: r for r in json.loads(MACHINE_CORPUS.read_text())["drills"]}["pumpjack"]
    resource = json.loads(RESOURCE.read_text())["pumpjack"]
    for field, value in resource.items():
        if field == "output_volume":
            want = next(box["volume"] for box in corpus["fluid_boxes"] if box["name"] == "output_fluid_box")
        elif field == "fluid":
            want = json.loads(ITEM_MAP.read_text())["items"]["crude-oil"]["target"]
        else:
            want = corpus.get(field)
        if want != value:
            failures.append(f"pumpjack.json {field} is {value!r}, the corpus says {want!r}")

    source = PF_BLOCKS.read_text()
    for name in ("pumpjack", "pumpjack_part", "oil_well"):
        if f'registerBlock("{name}"' not in source:
            failures.append(f"PFBlocks registers no {name!r}, which the generator writes files for")

    lang = json.loads(LANG.read_text())
    refusal = re.search(r'NO_WELL_KEY = "([^"]+)"', ITEM.read_text())
    if not refusal or refusal.group(1) not in lang:
        failures.append("the Pumpjack's refusal message has no lang key, so the gesture shows the raw key")
    marker = re.search(r'CRUDE_OIL = "([^"]+)"', SECTOR_PATCHES.read_text())
    if not marker or f"map.factoryworks.patch.{marker.group(1)}" not in lang:
        failures.append("the oil field's marker has no lang key, so the map shows the raw key")
    if "map.factoryworks.patch.yield" not in lang:
        failures.append("no lang key map.factoryworks.patch.yield, so an oil field's hover shows the raw key")

    for spring in ORITECH_SPRINGS:
        path = SPRINGS / f"{spring}.json"
        modifier = json.loads(path.read_text()) if path.is_file() else {}
        if modifier.get("type") != "neoforge:add_features" or modifier.get("biomes") != [] or modifier.get("features") != []:
            failures.append(f"oritech:{spring} is not overridden with a no-op, so Oritech's springs still place crude")

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print("ok   the Pumpjack and the oil well match the corpus, and Oritech's springs place nothing")
    return 0


if __name__ == "__main__":
    sys.exit(main())
