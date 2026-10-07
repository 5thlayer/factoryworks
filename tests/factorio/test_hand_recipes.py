#!/usr/bin/env python3
"""Assert the Personal Assembler's hand set is the recipes' own `hand_craftable` flag (ADR-0118).

Craftworks plans only a `craftworks:assembling` recipe whose `hand_craftable` is true, with no fluid
and exactly one item result. The converter writes the flag for a first Factorio category of `crafting`,
which excludes the eleven fluid-free recipes Factorio withholds from the hand. This check re-derives
the set from the corpus rather than from the converter, so a change to the converter can't pass by
being consistent with itself, and holds every emitted recipe, whichever script wrote it, to it.

It also asserts the `factoryworks:hand/*` copies are gone for good: the generator, the id list and
`withHandCopies`, since the hand set is a flag on the machine recipe.

Usage: tests/factorio/test_hand_recipes.py
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
RECIPES = ROOT / "kubejs/data/factoryworks/recipe"
CORPUS = ROOT / "data/factorio/recipe.json"
ASSEMBLING = "craftworks:assembling"
HAND_CATEGORY = "crafting"

GONE = (
    ROOT / "scripts/build-hand-recipes.py",
    ROOT / "kubejs/server_scripts/hand_recipes.js",
    RECIPES / "hand",
)


def main():
    failures = []
    for path in GONE:
        if path.exists():
            failures.append(f"{path.relative_to(ROOT)} should be gone: the hand set is a flag on the machine recipe")
    for script in (ROOT / "kubejs/server_scripts").glob("*.js"):
        text = script.read_text(encoding="utf-8")
        for stale in ("withHandCopies", "PF_HAND_RECIPES", "factoryworks:hand/"):
            if stale in text:
                failures.append(f"{script.name} still names {stale}")

    emitted = {}
    for path in sorted(RECIPES.rglob("*.json")):
        recipe = json.loads(path.read_text(encoding="utf-8"))
        if recipe.get("type") == ASSEMBLING:
            emitted[path.relative_to(RECIPES).with_suffix("").as_posix()] = recipe

    corpus = {row["name"].replace("-", "_"): row for row in json.loads(CORPUS.read_text(encoding="utf-8"))}
    hand = 0
    for stem, recipe in sorted(emitted.items()):
        flag = recipe.get("hand_craftable", True)
        if flag:
            hand += 1
            if recipe.get("fluid_ingredients") or recipe.get("fluid_results"):
                failures.append(f"{stem} is hand-craftable and names a fluid, which the Personal Assembler cannot hold")
            if len(recipe["results"]) != 1:
                failures.append(f"{stem} is hand-craftable with {len(recipe['results'])} results, "
                                "and the Personal Assembler plans one")
        row = corpus.get(stem.removeprefix("assembling/"))
        if row is not None and not stem.startswith(("assembling/stock/", "assembling/pack/")):
            want = row["category"] == HAND_CATEGORY
            if flag != want:
                failures.append(f"{stem} has hand_craftable {flag}, but Factorio's first category "
                                f"{row['category']!r} says {want}")

    if not hand:
        failures.append("no emitted recipe is hand-craftable, so the Personal Assembler plans nothing")

    for failure in failures:
        print(f"FAIL: {failure}")
    if failures:
        sys.exit(1)
    print(f"ok: {hand} of {len(emitted)} Assembling recipes are hand-craftable, as Factorio's categories say")


if __name__ == "__main__":
    main()
