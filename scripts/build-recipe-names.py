#!/usr/bin/env python3
"""Write the lang key of every emitted chassis recipe (#490, #303).

A recipe's key is `recipe.factoryworks.<type>.<name>`, for its id
`factoryworks:<type>/<name>`. Factorio names a recipe after its product unless it has no single
product of its own name, and then it names the recipe itself. The corpus holds no Wube text
(ADR-0103), so such a recipe is named from its id -- `heavy-oil-cracking` reads "Heavy oil
cracking" -- and every other recipe's value is `%s`, which the game fills with the main product's
name. Every recipe has a key so a server can send the name without knowing which recipes have one.

The keys live in `kubejs/assets/factoryworks/lang/en_us.json` beside hand-written ones, so the
script owns only the `recipe.factoryworks.` prefix: it drops every key under it and appends the
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
CORPUS = REPO / "data/factorio/recipe.json"
RECIPES = REPO / "kubejs/data/factoryworks/recipe"
LANG = REPO / "kubejs/assets/factoryworks/lang/en_us.json"
TYPES = ("chemistry", "oil_processing")
PREFIX = "recipe.factoryworks."


def id_name(recipe_id):
    words = recipe_id.replace("-", " ")
    return words[:1].upper() + words[1:]


def names_itself(row):
    """A recipe that is not one product under its own name has no product to be named after."""
    results = row["results"]
    return len(results) != 1 or results[0]["name"] != row["name"]


def keys():
    corpus = {row["name"]: row for row in json.loads(CORPUS.read_text(encoding="utf-8"))}
    out = {}
    for recipe_type in TYPES:
        for path in sorted((RECIPES / recipe_type).rglob("*.json")):
            key = ".".join(path.relative_to(RECIPES).with_suffix("").parts)
            recipe_id = path.stem.replace("_", "-")
            row = corpus.get(recipe_id)
            out[PREFIX + key] = id_name(recipe_id) if row and names_itself(row) else "%s"
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
