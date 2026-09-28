#!/usr/bin/env python3
"""Write the lang key of every emitted chassis recipe, as Factorio names it (#490).

A recipe's key is `recipe.planetaryfactory.<type>.<name>`, for its id
`planetaryfactory:<type>/<name>`. Its value is `data/factorio/recipe_name.json`'s entry for the
Factorio recipe the file was converted from, or `%s` where there is none: the game fills that with
the main product's name, as Factorio does. Every recipe has a key so a server can send the name
without knowing which recipes have one.

The keys live in `kubejs/assets/planetaryfactory/lang/en_us.json` beside hand-written ones, so the
script owns only the `recipe.planetaryfactory.` prefix: it drops every key under it and appends the
current set, leaving the rest of the file as it was.

Usage:

    scripts/build-recipe-names.py          # writes the lang keys
    scripts/build-recipe-names.py --check  # fails if they are stale
"""

import argparse
import json
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
NAMES = REPO / "data/factorio/recipe_name.json"
RECIPES = REPO / "kubejs/data/planetaryfactory/recipe"
LANG = REPO / "kubejs/assets/planetaryfactory/lang/en_us.json"
TYPES = ("assembling", "chemistry", "oil_processing")
PREFIX = "recipe.planetaryfactory."


def keys():
    names = json.loads(NAMES.read_text(encoding="utf-8"))
    out = {}
    for recipe_type in TYPES:
        for path in sorted((RECIPES / recipe_type).rglob("*.json")):
            key = ".".join(path.relative_to(RECIPES).with_suffix("").parts)
            out[PREFIX + key] = names.get(path.stem.replace("_", "-"), "%s")
    return out


def rendered():
    lang = json.loads(LANG.read_text(encoding="utf-8"))
    kept = {key: value for key, value in lang.items() if not key.startswith(PREFIX)}
    kept.update(keys())
    return json.dumps(kept, indent=2) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    text = rendered()
    if args.check:
        if LANG.read_text(encoding="utf-8") != text:
            sys.exit(f"{LANG.relative_to(REPO)}'s recipe names are stale -- re-run scripts/build-recipe-names.py")
        print(f"{LANG.relative_to(REPO)}'s recipe names are current")
        return
    LANG.write_text(text, encoding="utf-8")
    print(f"wrote {len(keys())} recipe names to {LANG.relative_to(REPO)}")


if __name__ == "__main__":
    main()
