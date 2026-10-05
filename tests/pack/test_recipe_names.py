#!/usr/bin/env python3
"""Assert every emitted chassis recipe has its name key, and every key names a recipe (#490).

`scripts/build-recipe-names.py --check` proves the lang keys are what the generator would write.
A recipe with no key renders its raw key on the machine's screen, in Jade and in a refusal message,
so each emitted recipe of a chassis type is held to one, and each key to an emitted recipe, since a
key nothing reads is a rename the generator missed. The corpus holds no Wube text (ADR-0103, #303):
a recipe that is not one product under its own name is named from its id, and every other value is
`%s`. Both halves are re-derived here from `recipe.json` rather than read back from the generator.

Usage: tests/pack/test_recipe_names.py
"""
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
RECIPES = ROOT / "kubejs/data/factoryworks/recipe"
LANG = ROOT / "kubejs/assets/factoryworks/lang/en_us.json"
CORPUS = ROOT / "data/factorio/recipe.json"
TYPES = ("chemistry", "oil_processing")
PREFIX = "recipe.factoryworks."


def main():
    failures = []
    run = subprocess.run([sys.executable, str(ROOT / "scripts/build-recipe-names.py"), "--check"],
                         capture_output=True, text=True)
    if run.returncode != 0:
        failures.append(f"build-recipe-names.py --check: {(run.stderr or run.stdout).strip()}")
    lang = {key: value for key, value in json.loads(LANG.read_text(encoding="utf-8")).items()
            if key.startswith(PREFIX)}
    corpus = {row["name"]: row for row in json.loads(CORPUS.read_text(encoding="utf-8"))}
    emitted = {PREFIX + ".".join(path.relative_to(RECIPES).with_suffix("").parts): path
               for recipe_type in TYPES for path in (RECIPES / recipe_type).rglob("*.json")}
    for key in sorted(set(emitted) - set(lang)):
        failures.append(f"{key} has no name key")
    for key in sorted(set(lang) - set(emitted)):
        failures.append(f"{key} names no emitted recipe")
    for key, value in sorted(lang.items()):
        recipe_id = emitted[key].stem.replace("_", "-")
        row = corpus.get(recipe_id)
        own = row is not None and (len(row["results"]) != 1 or row["results"][0]["name"] != recipe_id)
        want = recipe_id.replace("-", " ").capitalize() if own else "%s"
        if value != want:
            failures.append(f"{key} is {value!r}, expected {want!r}")
    for key, want in (("chemistry.heavy_oil_cracking", "Heavy oil cracking"),
                      ("oil_processing.advanced_oil_processing", "Advanced oil processing")):
        if lang.get(PREFIX + key) != want:
            failures.append(f"{PREFIX}{key} is {lang.get(PREFIX + key)!r}, expected {want!r}")
    if not any(value != "%s" for value in lang.values()):
        failures.append("no recipe is named from its id, so the %s fallback is all that was checked")
    if failures:
        print("FAIL")
        for failure in failures:
            print(f"  {failure}")
        sys.exit(1)
    print(f"ok: {len(lang)} recipe name keys, {sum(v != '%s' for v in lang.values())} named from their id")


if __name__ == "__main__":
    main()
