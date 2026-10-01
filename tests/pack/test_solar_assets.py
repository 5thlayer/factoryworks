#!/usr/bin/env python3
"""Assert the Solar Panel's generated halves agree with the corpus and with what registers it (#529).

  - `scripts/build-solar-assets.py --check` holds the resource, blockstates, models, lang and loot
    table current against `data/factorio/machine.json`.
  - The resource is compared with the corpus row field by field here too, so a hand-edited
    resource that someone also taught the generator to accept still fails.
  - The block names the generator writes files for are the ones `PFBlocks` registers, and the
    panel is in `wireworks:generators`, without which no pole draws it (ADR-0062).

Usage: tests/pack/test_solar_assets.py
"""

import json
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
RESOURCE = ROOT / "mod/src/main/resources/factoryworks_core/energy/solar_panels.json"
MACHINE_CORPUS = ROOT / "data/factorio/machine.json"
GENERATOR = ROOT / "scripts/build-solar-assets.py"
PF_BLOCKS = ROOT / "mod/src/main/java/com/factoryworks/core/PFBlocks.java"
GENERATORS_TAG = ROOT / "kubejs/data/wireworks/tags/block/generators.json"
FIELDS = ("production", "tile_width", "tile_height")


def main():
    failures = []
    generated = subprocess.run(
        [sys.executable, str(GENERATOR), "--check"], capture_output=True, text=True
    )
    if generated.returncode != 0:
        failures.append(f"{GENERATOR.relative_to(ROOT)} --check: {generated.stderr.strip()}")

    rows = {r["name"]: r for r in json.loads(MACHINE_CORPUS.read_text()).get("solar_panels", [])}
    corpus = rows.get("solar-panel", {})
    resource = json.loads(RESOURCE.read_text()).get("solar-panel", {})
    for field in FIELDS:
        if field not in resource:
            failures.append(f"solar_panels.json carries no {field}")
        elif corpus.get(field) != resource[field]:
            failures.append(
                f"solar_panels.json {field} is {resource[field]!r}, the corpus says {corpus.get(field)!r}"
            )

    source = PF_BLOCKS.read_text()
    for name in ("solar_panel", "solar_panel_part"):
        if f'registerBlock("{name}"' not in source:
            failures.append(f"PFBlocks registers no {name!r}, which the generator writes files for")

    if "factoryworks:solar_panel" not in json.loads(GENERATORS_TAG.read_text())["values"]:
        failures.append("wireworks:generators does not hold the Solar Panel, so no pole draws it")

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print(f"ok   solar resource matches the corpus on {len(FIELDS)} fields; assets current")
    return 0


if __name__ == "__main__":
    sys.exit(main())
