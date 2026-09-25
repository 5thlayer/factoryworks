#!/usr/bin/env python3
"""Emit EMI's index as the Obtainable allowlist (#453, ADR-0088).

Obtainable is derived from every output of every recipe the pack emits, every item the starting kit
grants, and `data/pack/mechanic-obtainable.json`, the hand-kept rows for what a mechanic produces
with no recipe. Worldgen and mob drops are #454's and #455's. Reads only committed files.

EMI reads index stacks only under the `emi` namespace, and applies a file's `filters` before its
`added`, so a filter matching every id empties the index and `added` refills it. `disable` would
also hide every recipe naming a filtered stack, so it is left off. An `added` entry that is a bare
string is skipped, so each is a `{"stack": ...}` object.

Usage:

    scripts/build-obtainable-index.py            # writes the index
    scripts/build-obtainable-index.py --check    # asserts it is already up to date; no writes
"""
import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RECIPES = ROOT / "kubejs/data/planetaryfactory/recipe"
KIT = ROOT / "mod/src/main/java/com/planetaryfactory/core/start/StartingKit.java"
MECHANICS = ROOT / "data/pack/mechanic-obtainable.json"
INDEX = ROOT / "kubejs/assets/emi/index/stacks/obtainable.json"


def recipe_outputs():
    outputs = set()
    for path in sorted(RECIPES.rglob("*.json")):
        recipe = json.loads(path.read_text(encoding="utf-8"))
        results = recipe.get("results") or [recipe.get("result")]
        if not all(results):
            sys.exit(f"{path.relative_to(ROOT)} has no `results` or `result`")
        outputs |= {"item:" + result["id"] for result in results}
    return outputs


def kit_items():
    items = {"item:" + item for item in re.findall(
        r'new Entry\("([^"]+)"', KIT.read_text(encoding="utf-8"))}
    if not items:
        sys.exit(f"{KIT.relative_to(ROOT)} has no `new Entry(...)` -- the kit has changed shape")
    return items


def derived():
    return recipe_outputs() | kit_items()


def mechanic_rows():
    return json.loads(MECHANICS.read_text(encoding="utf-8"))["rows"]


def index():
    stacks = derived() | {row["id"] for row in mechanic_rows()}
    ordered = sorted(stacks, key=lambda stack: (not stack.startswith("item:"), stack))
    return {"filters": ["/.*/"], "added": [{"stack": stack} for stack in ordered]}


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    data = index()
    text = json.dumps(data, indent=2) + "\n"
    where = INDEX.relative_to(ROOT)
    if args.check:
        if not INDEX.is_file() or INDEX.read_text(encoding="utf-8") != text:
            sys.exit(f"stale: {where} -- run scripts/build-obtainable-index.py")
        print(f"OK -- {len(data['added'])} stacks")
        return
    INDEX.parent.mkdir(parents=True, exist_ok=True)
    INDEX.write_text(text, encoding="utf-8")
    print(f"wrote {where}: {len(data['added'])} stacks")


if __name__ == "__main__":
    main()
