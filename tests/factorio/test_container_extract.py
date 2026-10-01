#!/usr/bin/env python3
"""Assert the container corpus still holds the crash site's inventory (ADR-0107) and its debris (#550).

`scripts/factorio-container-extract.py` reads a dump that is not in the repo. The committed rows are
held to a positive whole-slot inventory and to debris that is minable for nothing, one mining time
per size class, and to the dump itself when it is on disk.
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

    classes = {}
    for row in corpus.get("debris", []):
        minable = row.get("minable", {})
        if "result" in minable or "results" in minable:
            failures.append(f"{row['name']} yields something when mined -- Debris yields nothing")
        size = row["name"].rsplit("-", 2)[-2]
        classes.setdefault(size, set()).add(minable.get("mining_time"))
    counts = {size: sum(1 for row in corpus.get("debris", []) if f"-{size}-" in row["name"])
              for size in classes}
    if counts != {"big": 2, "medium": 3, "small": 6}:
        failures.append(f"debris prototypes per class are {counts}, not Factorio's 2, 3 and 6")
    for size, times in classes.items():
        if len(times) != 1 or not all(isinstance(t, (int, float)) and t > 0 for t in times):
            failures.append(f"{size} debris has mining times {sorted(times, key=str)}, not one")

    extractor = load_extractor()
    if extractor.DEFAULT_DUMP.is_file():
        dump = json.loads(extractor.DEFAULT_DUMP.read_text(encoding="utf-8"))
        if extractor.extract(dump) != corpus:
            failures.append("container.json differs from the dump -- re-run the extractor")

    for failure in failures:
        print(f"FAIL: {failure}")
    if failures:
        return 1
    print("ok   container corpus holds crash-site-spaceship's inventory_size and its debris")
    return 0


if __name__ == "__main__":
    sys.exit(main())
