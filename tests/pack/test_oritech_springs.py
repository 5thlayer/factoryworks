#!/usr/bin/env python3
"""Assert both of Oritech's `oil_spring` biome modifiers add no feature to no biome (#377).

The oil well is the only crude source in the pack (ADR-0081). NeoForge 26.1 registers `none` for
structure modifiers only, so the no-op is an empty `add_features`.

Usage: tests/pack/test_oritech_springs.py
"""

import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
SPRINGS = ROOT / "kubejs/data/oritech/neoforge/biome_modifier"
ORITECH_SPRINGS = ("oil_spring", "oil_spring_desert")


def main():
    failures = []
    for spring in ORITECH_SPRINGS:
        path = SPRINGS / f"{spring}.json"
        modifier = json.loads(path.read_text()) if path.is_file() else {}
        if modifier.get("type") != "neoforge:add_features" or modifier.get("biomes") != [] or modifier.get("features") != []:
            failures.append(f"oritech:{spring} is not overridden with a no-op, so Oritech's springs still place crude")

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print("ok   Oritech's springs place nothing")
    return 0


if __name__ == "__main__":
    sys.exit(main())
