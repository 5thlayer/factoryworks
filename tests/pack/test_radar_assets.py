#!/usr/bin/env python3
"""Assert the Radar's generated halves agree with the corpus and with what registers it (#368).

  - `scripts/build-radar-assets.py --check` holds the resource, blockstates, models, lang and loot
    table current against `data/factorio/machine.json`.
  - The resource is compared with the corpus row field by field here too, so a hand-edited
    resource that someone also taught the generator to accept still fails.
  - The block names the generator writes files for are the ones `PFBlocks` registers.
  - Every ore has the lang key its patch marker is named by (#370), which `FtbMapMarkers` builds
    from the resource key, so a missing one shows the raw key on the map.

Usage: tests/pack/test_radar_assets.py
"""

import json
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
RESOURCE = ROOT / "mod/src/main/resources/planetaryfactory_core/radar/radars.json"
MACHINE_CORPUS = ROOT / "data/factorio/machine.json"
GENERATOR = ROOT / "scripts/build-radar-assets.py"
PF_BLOCKS = ROOT / "mod/src/main/java/com/planetaryfactory/core/PFBlocks.java"
ORE_SLICE = ROOT / "mod/src/main/resources/planetaryfactory_core/ore/amounts.json"
MARKERS = ROOT / "mod/src/main/java/com/planetaryfactory/core/radar/ftb/FtbMapMarkers.java"
LANG = ROOT / "kubejs/assets/planetaryfactory/lang/en_us.json"
MARKER_KEY = "map.planetaryfactory.patch."


def main():
    failures = []
    generated = subprocess.run(
        [sys.executable, str(GENERATOR), "--check"], capture_output=True, text=True
    )
    if generated.returncode != 0:
        failures.append(f"{GENERATOR.relative_to(ROOT)} --check: {generated.stderr.strip()}")

    corpus = {r["name"]: r for r in json.loads(MACHINE_CORPUS.read_text())["radars"]}["radar"]
    resource = json.loads(RESOURCE.read_text())["radar"]
    for field, value in resource.items():
        if corpus.get(field) != value:
            failures.append(f"radars.json {field} is {value!r}, the corpus says {corpus.get(field)!r}")

    source = PF_BLOCKS.read_text()
    for name in ("radar", "radar_part"):
        if f'registerBlock("{name}"' not in source:
            failures.append(f"PFBlocks registers no {name!r}, which the generator writes files for")

    if f'"{MARKER_KEY}"' not in MARKERS.read_text():
        failures.append(f"FtbMapMarkers no longer names its markers by {MARKER_KEY!r}")
    lang = json.loads(LANG.read_text())
    for ore in json.loads(ORE_SLICE.read_text())["resources"]:
        if MARKER_KEY + ore not in lang:
            failures.append(f"no lang key {MARKER_KEY + ore}, so its patch marker shows the raw key")

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print(f"ok   radar resource matches the corpus on {len(resource)} fields; assets current")
    return 0


if __name__ == "__main__":
    sys.exit(main())
