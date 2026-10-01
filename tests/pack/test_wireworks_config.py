#!/usr/bin/env python3
"""Assert the Wireworks config gives each pole tier Factorio's supply area and wire reach (#476).

Wireworks' tiers are a server config (its ADR 0006), so the Pack's Factorio figures are a Binding:
`config/wireworks-server.toml`, re-derived here from `data/factorio/machine.json`. A supply area is
`2 * supply_area_distance` blocks on a side, and a wire reach is `maximum_wire_distance`.

Usage: tests/pack/test_wireworks_config.py
"""

import json
import pathlib
import sys
import tomllib

ROOT = pathlib.Path(__file__).resolve().parents[2]
CONFIG = ROOT / "config/wireworks-server.toml"
CORPUS = ROOT / "data/factorio/machine.json"

# Wireworks' tiers, by the Factorio pole each stands for. The big pole is not shipped (ADR-0036).
TIERS = {"small": "small-electric-pole", "medium": "medium-electric-pole", "substation": "substation"}


def main():
    config = tomllib.loads(CONFIG.read_text())
    poles = {pole["name"]: pole for pole in json.loads(CORPUS.read_text())["poles"]}
    failures = []
    for tier, name in TIERS.items():
        pole = poles[name]
        expected = {"supplySize": round(2 * pole["supply_area_distance"]),
                    "wireReach": float(pole["maximum_wire_distance"])}
        got = config.get(tier, {})
        for key, value in expected.items():
            if got.get(key) != value:
                failures.append(f"{tier}.{key} is {got.get(key)}, Factorio's {name} gives {value}")
    if set(config) != set(TIERS):
        failures.append(f"the config names tiers {sorted(config)}, Wireworks has {sorted(TIERS)}")
    for failure in failures:
        print("FAIL", failure)
    if failures:
        sys.exit(1)
    print(f"ok   {CONFIG.relative_to(ROOT)} holds Factorio's figures for {len(TIERS)} tiers")


if __name__ == "__main__":
    main()
