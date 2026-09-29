#!/usr/bin/env python3
"""Assert each feeder tier's energy in the Beltworks config is one swing of its inserter (#514, ADR-0100).

The figures are copied from the Factorio dump into `config/beltworks-server.toml`, so they are
re-derived here with Beltworks' `InserterSwing.joulesPerSwing` rule. The dump is not committed;
without it the check says so and passes.

Usage: tests/pack/test_feeder_energy.py
"""

import json
import math
import pathlib
import sys
import tomllib

ROOT = pathlib.Path(__file__).resolve().parents[2]
DUMP = pathlib.Path.home() / "Library/Application Support/factorio/script-output/data-raw-dump.json"

# Beltworks' spike ticks per tier; the burner's is ADR-0100's assumption.
TIERS = {
    "belt": ("burner-inserter", 5),
    "improved": ("inserter", 5),
    "express": ("fast-inserter", 1),
    "turbo": ("bulk-inserter", 1),
}


def joules(value):
    assert value.endswith("kJ"), value
    return float(value[:-2]) * 1000


def swing(inserter, spike_ticks):
    rotation = 2 * math.floor(0.5 / inserter["rotation_speed"]) * joules(inserter["energy_per_rotation"]) \
        * inserter["rotation_speed"]
    movement = 2 * spike_ticks * joules(inserter["energy_per_movement"]) * inserter["extension_speed"]
    return round(rotation + movement)


def main():
    config = tomllib.loads((ROOT / "config/beltworks-server.toml").read_text())["feederJoulesPerItem"]
    if not DUMP.is_file():
        print(f"skip: no dump at {DUMP}")
        return 0
    inserters = json.loads(DUMP.read_text())["inserter"]
    failures = [
        f"{tier}: config {config.get(tier)}, {name}'s swing {swing(inserters[name], spikes)}"
        for tier, (name, spikes) in TIERS.items()
        if config.get(tier) != swing(inserters[name], spikes)
    ]
    for failure in failures:
        print("FAIL:", failure)
    if not failures:
        print("ok   every feeder tier pays one swing of its inserter")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
