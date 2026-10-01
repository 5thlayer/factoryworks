#!/usr/bin/env python3
"""Assert the container corpus still holds the crash site's inventory (ADR-0107).

`scripts/factorio-container-extract.py` reads a dump that is not in the repo. The committed row is
held to a positive whole-slot inventory, and to the dump itself when it is on disk.
"""

import importlib.util
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
EXTRACTOR = ROOT / "scripts/factorio-container-extract.py"
CORPUS = ROOT / "data/factorio/container.json"


def load_extractor():
    spec = importlib.util.spec_from_file_location("container_extract", EXTRACTOR)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main():
    failures = []
    corpus = json.loads(CORPUS.read_text(encoding="utf-8"))
    rows = {row["name"]: row for row in corpus["containers"]}
    row = rows.get("crash-site-spaceship")
    if row is None:
        failures.append("crash-site-spaceship is not in container.json")
    elif not isinstance(row.get("inventory_size"), int) or row["inventory_size"] < 1:
        failures.append(f"crash-site-spaceship's inventory_size is {row.get('inventory_size')!r}")

    extractor = load_extractor()
    if extractor.DEFAULT_DUMP.is_file():
        dump = json.loads(extractor.DEFAULT_DUMP.read_text(encoding="utf-8"))
        if extractor.extract(dump) != corpus:
            failures.append("container.json differs from the dump -- re-run the extractor")

    for failure in failures:
        print(f"FAIL: {failure}")
    if failures:
        return 1
    print("ok   container corpus holds crash-site-spaceship's inventory_size")
    return 0


if __name__ == "__main__":
    sys.exit(main())
