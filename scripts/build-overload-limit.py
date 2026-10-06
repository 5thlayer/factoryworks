#!/usr/bin/env python3
"""Emit the Overload Limit's constants (#517) from the corpus.

Copies `data/factorio/overload.json`'s constants to
`mod/src/main/resources/factoryworks_core/machine/overload.json`, which `OverloadLimit` reads for the
furnaces. Factorio's crafting machines are Craftworks' and carry their own figures (ADR-0118,
ADR-0123, ADR-0124).

Usage:

    scripts/build-overload-limit.py            # writes the resource
    scripts/build-overload-limit.py --check    # asserts it is already up to date; no writes
"""
import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OVERLOAD_CORPUS = ROOT / "data" / "factorio" / "overload.json"
OVERLOAD = ROOT / "mod/src/main/resources/factoryworks_core/machine/overload.json"

KEYS = ("dynamic_recipe_overload_factor", "minimum_recipe_overload_multiplier",
        "maximum_recipe_overload_multiplier")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    constants = json.loads(OVERLOAD_CORPUS.read_text(encoding="utf-8"))["constants"]
    text = json.dumps({key: constants[key] for key in KEYS}, indent=2) + "\n"
    if args.check:
        if not OVERLOAD.is_file() or OVERLOAD.read_text(encoding="utf-8") != text:
            sys.exit(f"stale: {OVERLOAD} -- run scripts/build-overload-limit.py")
        print(f"OK -- {OVERLOAD.relative_to(ROOT)}")
        return
    OVERLOAD.parent.mkdir(parents=True, exist_ok=True)
    OVERLOAD.write_text(text, encoding="utf-8")
    print(f"wrote {OVERLOAD.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
