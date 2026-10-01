#!/usr/bin/env python3
"""Extract Factorio's container prototypes the pack sizes an inventory from into the container corpus.

Only `crash-site-spaceship` today: the cargo hold in the wreck is that container, and its slot
count is `inventory_size` (ADR-0107). The raw field goes in unchanged.

Usage:

    scripts/factorio-container-extract.py            # finds the dump, writes data/factorio/container.json
    scripts/factorio-container-extract.py --dump PATH
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

CONTAINERS = ("crash-site-spaceship",)

FIELDS = ("inventory_size", "inventory_type")


def extract(dump):
    rows = []
    for name in CONTAINERS:
        prototype = dump["container"][name]
        rows.append({"name": name, **{f: prototype[f] for f in FIELDS if f in prototype}})
    return {"containers": rows}


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dump", type=Path, default=DEFAULT_DUMP)
    parser.add_argument("--out", type=Path,
                        default=REPO / "data" / "factorio" / "container.json")
    args = parser.parse_args()

    if not args.dump.is_file():
        sys.exit(
            f"no dump at {args.dump}\n"
            "run:  factorio --dump-data --mod-directory <dir with base+SA only>"
        )

    corpus = extract(json.loads(args.dump.read_text(encoding="utf-8")))
    args.out.write_text(json.dumps(corpus, indent=2) + "\n", encoding="utf-8")

    print(f"wrote {args.out.relative_to(REPO)}\n")
    for row in corpus["containers"]:
        print(f"  {row['name']:24} inventory_size {row['inventory_size']}")


if __name__ == "__main__":
    main()
