#!/usr/bin/env python3
"""Extract Factorio's English recipe names -- the `[recipe-name]` locale entries.

Factorio names a recipe by its own `recipe-name` entry when it has one, and otherwise by its main
product: `heavy-oil-cracking` is "Heavy oil cracking to light oil", `plastic-bar` has no entry and is
named after the plastic bar. Only the first half needs extracting; the second is the product's own
name in game. `scripts/build-recipe-names.py` turns this file into the pack's lang keys (#490).

Kept: entries for recipes in `recipe.json`, with no `__1__` parameter (those are Factorio's
generated barrel recipes, which the pack does not have).

Usage:

    scripts/factorio-recipe-name-extract.py              # finds the Steam install
    scripts/factorio-recipe-name-extract.py --data PATH  # Factorio's data/ directory
    scripts/factorio-recipe-name-extract.py --check      # diffs against the committed file
"""

import argparse
import json
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
OUT = REPO / "data/factorio/recipe_name.json"
RECIPES = REPO / "data/factorio/recipe.json"
DEFAULT_DATA = (Path.home()
                / "Library/Application Support/Steam/steamapps/common/Factorio/factorio.app/Contents/data")
LOCALES = ("base/locale/en/base.cfg", "space-age/locale/en/space-age.cfg")


def recipe_names(text):
    names, section = {}, None
    for line in text.splitlines():
        line = line.strip()
        if line.startswith("[") and line.endswith("]"):
            section = line[1:-1]
        elif section == "recipe-name" and "=" in line:
            key, value = line.split("=", 1)
            names[key.strip()] = value.strip()
    return names


def corpus_names():
    data = json.loads(RECIPES.read_text(encoding="utf-8"))
    rows = data["recipes"] if isinstance(data, dict) else data
    return {row["name"] for row in rows}


def extract(data_dir):
    names = {}
    for locale in LOCALES:
        path = data_dir / locale
        if not path.is_file():
            sys.exit(f"no locale at {path}")
        names.update(recipe_names(path.read_text(encoding="utf-8")))
    known = corpus_names()
    return {name: value for name, value in sorted(names.items())
            if name in known and "__" not in value}


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--data", type=Path, default=DEFAULT_DATA)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if not args.data.is_dir():
        print(f"no Factorio install at {args.data}; nothing to extract or check")
        return
    text = json.dumps(extract(args.data), indent=2) + "\n"
    if args.check:
        if not OUT.is_file() or OUT.read_text(encoding="utf-8") != text:
            sys.exit(f"{OUT.relative_to(REPO)} is stale -- re-run scripts/factorio-recipe-name-extract.py")
        print(f"{OUT.relative_to(REPO)} is current")
        return
    OUT.write_text(text, encoding="utf-8")
    print(f"wrote {OUT.relative_to(REPO)}")


if __name__ == "__main__":
    main()
