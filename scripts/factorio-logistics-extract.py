#!/usr/bin/env python3
"""Extract Factorio's belt, splitter, loader and inserter prototypes into the logistics corpus.

The raw prototype fields go in unchanged; every figure the belt fork types is derived from them
by `tests/factorio/test_logistics_extract.py` (#344, ADR-0060, ADR-0076).

Three inputs are engine behaviour rather than prototype data, so they are typed here and written
under `documented` with their source: a belt's items per tile, each inserter's base hand size,
and the length of the item spike at each end of a swing.

Usage:

    scripts/factorio-logistics-extract.py            # finds the dump, writes data/factorio/logistics.json
    scripts/factorio-logistics-extract.py --dump PATH
"""

import argparse
import json
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent

DEFAULT_DUMP = (
    Path.home()
    / "Library/Application Support/factorio/script-output/data-raw-dump.json"
)

BELTS = ("transport-belt", "fast-transport-belt", "express-transport-belt", "turbo-transport-belt")
SPLITTERS = ("splitter", "fast-splitter", "express-splitter", "turbo-splitter")
LOADERS = ("loader", "fast-loader", "express-loader", "turbo-loader")
# long-handed is kept so the ledger's `excluded` row has the prototype it excludes.
INSERTERS = ("burner-inserter", "inserter", "long-handed-inserter", "fast-inserter",
             "bulk-inserter")

INSERTER_FIELDS = ("rotation_speed", "extension_speed", "energy_per_rotation",
                   "energy_per_movement", "pickup_position", "insert_position", "bulk")

# The bulk inserter's 2 is not in its prototype (it has no `stack_size_bonus`); the engine
# grants it to every `bulk` inserter.
BELT_DENSITY = {
    "items_per_tile": 8,
    "source": "https://wiki.factorio.com/Transport_belts/Physics ('Density': 4 per tile per lane, "
              "two lanes)",
}

HAND_SIZE = {
    "values": {"burner-inserter": 1, "inserter": 1, "long-handed-inserter": 1,
               "fast-inserter": 1, "bulk-inserter": 2},
    "source": "https://wiki.factorio.com/Inserter_capacity_bonus_(research) -- the 'none' row: "
              "(1) non-bulk, (2) bulk",
}

# Measured, not derivable: truncating 0.2 / extension_speed gives 2 for the fast and bulk
# inserters, and the game spends 1.
ITEM_SPIKE_TICKS = {
    "values": {"burner-inserter": 5, "inserter": 5, "long-handed-inserter": 4,
               "fast-inserter": 1, "bulk-inserter": 1},
    "source": "https://wiki.factorio.com/Inserters#Power_usage ('Tick duration of Item Spike'), "
              "measured in https://forums.factorio.com/viewtopic.php?t=128389",
}

SWING = {
    "rule": "one swing is two half-spins, each floor(0.5 / rotation_speed) whole ticks at "
            "energy_per_rotation * rotation_speed per tick, plus an item spike at each end of "
            "item_spike_ticks at energy_per_movement * extension_speed per tick",
    "per_cycle_kj": {"burner-inserter": 66.9, "inserter": 6.65, "long-handed-inserter": 7,
                     "fast-inserter": 8.12, "bulk-inserter": 23.2},
    "source": "https://wiki.factorio.com/Inserters#Energy_costs ('Cost per transfer cycle in "
              "kJ', theoretically derived and empirically confirmed); the page states it is "
              "valid up to 2.0.77",
}


def pick(prototype, fields):
    return {field: prototype[field] for field in fields if field in prototype}


def extract(dump):
    def row(kind, name, fields):
        prototype = dump[kind][name]
        return {"name": name, **pick(prototype, fields)}

    inserters = []
    for name in INSERTERS:
        prototype = dump["inserter"][name]
        source = prototype["energy_source"]
        inserters.append({
            "name": name,
            **pick(prototype, INSERTER_FIELDS),
            "energy_source": source["type"],
            "drain": source.get("drain"),
        })

    return {
        "belts": [row("transport-belt", name, ("speed",)) for name in BELTS],
        "splitters": [row("splitter", name, ("speed", "related_transport_belt"))
                      for name in SPLITTERS],
        "loaders": [row("loader", name, ("speed",)) for name in LOADERS],
        "inserters": inserters,
        "documented": {
            "belt_density": BELT_DENSITY,
            "hand_size": HAND_SIZE,
            "item_spike_ticks": ITEM_SPIKE_TICKS,
            "swing": SWING,
        },
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dump", type=Path, default=DEFAULT_DUMP)
    parser.add_argument("--out", type=Path,
                        default=REPO / "data" / "factorio" / "logistics.json")
    args = parser.parse_args()

    if not args.dump.is_file():
        sys.exit(
            f"no dump at {args.dump}\n"
            "run:  factorio --dump-data --mod-directory <dir with base+SA only>"
        )

    corpus = extract(json.loads(args.dump.read_text(encoding="utf-8")))
    args.out.write_text(json.dumps(corpus, indent=2) + "\n", encoding="utf-8")

    print(f"wrote {args.out.relative_to(REPO)}\n")
    for belt in corpus["belts"]:
        print(f"  {belt['name']:24} speed {belt['speed']}")
    for inserter in corpus["inserters"]:
        print(f"  {inserter['name']:24} rotation {inserter['rotation_speed']:.3f}  "
              f"drain {inserter['drain']}")


if __name__ == "__main__":
    main()
