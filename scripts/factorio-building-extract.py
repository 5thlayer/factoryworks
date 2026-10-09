#!/usr/bin/env python3
"""Extract which Factorio items place a Building (#413).

A Building is what an item places as an entity: an item with a `place_result`, or a rail planner's
`rails`. An item laid with `place_as_tile` -- stone brick, concrete, landfill -- is not one, and is
extracted anyway so the file shows the split rather than only asserting it.

Three kinds of placed entity are not Buildings, and are marked rather than dropped: a `plant`,
which a seed places; a vehicle, which moves; and a robot, which flies off. They are named by
Factorio entity type in `MOBILE` below.

Usage:

    scripts/factorio-building-extract.py            # finds the dump, writes data/factorio/building.json
    scripts/factorio-building-extract.py --dump PATH
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

ITEM_TYPES = ("item", "item-with-entity-data", "rail-planner")

MOBILE = {
    "plant": "a plant a seed places",
    "car": "a vehicle",
    "spider-vehicle": "a vehicle",
    "locomotive": "a vehicle",
    "cargo-wagon": "a vehicle",
    "fluid-wagon": "a vehicle",
    "artillery-wagon": "a vehicle",
    "infinity-cargo-wagon": "a vehicle",
    "construction-robot": "a robot",
    "logistic-robot": "a robot",
}


def entity_types(dump):
    """`{entity name: its prototype type}` over every prototype that is an entity."""
    found = {}
    for kind, prototypes in dump.items():
        if kind in ITEM_TYPES:
            continue
        for name, prototype in prototypes.items():
            if isinstance(prototype, dict) and ("collision_box" in prototype or "max_health" in prototype):
                if name in found and found[name] != kind:
                    sys.exit(f"{name} is an entity of two types, {found[name]} and {kind}")
                found[name] = kind
    return found


def extract(dump):
    types = entity_types(dump)
    rows = []
    for kind in ITEM_TYPES:
        for name, prototype in sorted((dump.get(kind) or {}).items()):
            placed = prototype.get("rails") or (
                [prototype["place_result"]] if prototype.get("place_result") else [])
            tile = (prototype.get("place_as_tile") or {}).get("result")
            if not placed and not tile:
                continue
            row = {"item": name, "item_type": kind}
            if placed:
                missing = [entity for entity in placed if entity not in types]
                if missing:
                    sys.exit(f"{name} places {missing}, which no entity prototype names")
                row["entities"] = {entity: types[entity] for entity in placed}
                mobile = sorted({MOBILE[t] for t in row["entities"].values() if t in MOBILE})
                row["building"] = not mobile
                if mobile:
                    row["not_a_building"] = ", ".join(mobile)
            else:
                row["tile"] = tile
                row["building"] = False
                row["not_a_building"] = "laid as a tile"
            rows.append(row)
    return sorted(rows, key=lambda r: r["item"])


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dump", type=Path, default=DEFAULT_DUMP)
    parser.add_argument("--out", type=Path, default=REPO / "data" / "factorio" / "building.json")
    args = parser.parse_args()

    if not args.dump.is_file():
        sys.exit(
            f"no dump at {args.dump}\n"
            "run:  factorio --dump-data --mod-directory <dir with base+SA only>"
        )

    rows = extract(json.loads(args.dump.read_text(encoding="utf-8")))
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps({"items": rows}, indent=2) + "\n", encoding="utf-8")

    buildings = [r for r in rows if r["building"]]
    print(f"{len(rows)} placing items, {len(buildings)} of them Buildings")
    print(f"wrote {args.out.relative_to(REPO)}")
    for row in rows:
        if not row["building"]:
            print(f"  not a Building: {row['item']:32} {row['not_a_building']}")


if __name__ == "__main__":
    main()
