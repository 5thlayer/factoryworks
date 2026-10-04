#!/usr/bin/env python3
"""Assert the Wireworks config gives Factorio's figures to the poles, the solar panel and the accumulator (#476, #617).

Wireworks' figures are a server config (its ADR 0006), so the Pack's Factorio figures are a Binding:
`config/wireworks-server.toml`, re-derived here from `data/factorio/machine.json`. A supply area is
`2 * supply_area_distance` blocks on a side, and a wire reach is `maximum_wire_distance`. The solar
panel's `peak_watts` is its `production`; the accumulator's `capacity_joules` is its
`buffer_capacity`, and its one `max_watts` needs the input and output flow limits to be equal.

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
TIERS = {"small": "small-electric-pole", "medium": "medium-electric-pole", "large": "substation"}


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
    corpus = json.loads(CORPUS.read_text())
    panel = {p["name"]: p for p in corpus["solar_panels"]}["solar-panel"]
    accumulator = {a["name"]: a for a in corpus["accumulators"]}["accumulator"]
    if accumulator["input_flow_limit"] != accumulator["output_flow_limit"]:
        failures.append("the accumulator's flow limits differ, and Wireworks has one max_watts")
    expected = {
        "solar_panel": {"peak_watts": round(panel["production"])},
        "accumulator": {"capacity_joules": round(accumulator["buffer_capacity"]),
                        "max_watts": round(accumulator["input_flow_limit"])},
    }
    for section, figures in expected.items():
        got = config.get(section, {})
        for key, value in figures.items():
            if got.get(key) != value:
                failures.append(f"{section}.{key} is {got.get(key)}, Factorio's corpus gives {value}")
    sections = set(TIERS) | set(expected)
    if set(config) != sections:
        failures.append(f"the config names sections {sorted(config)}, Wireworks has {sorted(sections)}")
    for failure in failures:
        print("FAIL", failure)
    if failures:
        sys.exit(1)
    print(f"ok   {CONFIG.relative_to(ROOT)} holds Factorio's figures for {len(TIERS)} tiers, the solar panel and the accumulator")


if __name__ == "__main__":
    main()
