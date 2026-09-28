#!/usr/bin/env python3
"""Assert every emitted chassis recipe has its name key, and every key names a recipe (#490).

`scripts/build-recipe-names.py --check` proves the lang keys are what the generator would write.
A recipe with no key renders its raw key on the machine's screen, in Jade and in a refusal message,
so each emitted recipe of a chassis type is held to one, and each key to an emitted recipe, since a
key nothing reads is a rename the generator missed. Each non-`%s` value is held to the corpus's
recipe name, and `scripts/factorio-recipe-name-extract.py --check` re-extracts when Factorio is
installed.

Usage: tests/pack/test_recipe_names.py
"""
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
RECIPES = ROOT / "kubejs/data/planetaryfactory/recipe"
LANG = ROOT / "kubejs/assets/planetaryfactory/lang/en_us.json"
NAMES = ROOT / "data/factorio/recipe_name.json"
TYPES = ("assembling", "chemistry", "oil_processing")
PREFIX = "recipe.planetaryfactory."


def main():
    failures = []
    for script in ("factorio-recipe-name-extract.py", "build-recipe-names.py"):
        run = subprocess.run([sys.executable, str(ROOT / "scripts" / script), "--check"],
                             capture_output=True, text=True)
        if run.returncode != 0:
            failures.append(f"{script} --check: {(run.stderr or run.stdout).strip()}")
    lang = {key: value for key, value in json.loads(LANG.read_text(encoding="utf-8")).items()
            if key.startswith(PREFIX)}
    names = json.loads(NAMES.read_text(encoding="utf-8"))
    emitted = {PREFIX + ".".join(path.relative_to(RECIPES).with_suffix("").parts): path
               for recipe_type in TYPES for path in (RECIPES / recipe_type).rglob("*.json")}
    for key in sorted(set(emitted) - set(lang)):
        failures.append(f"{key} has no name key")
    for key in sorted(set(lang) - set(emitted)):
        failures.append(f"{key} names no emitted recipe")
    for key, value in sorted(lang.items()):
        if value != "%s" and names.get(emitted[key].stem.replace("_", "-")) != value:
            failures.append(f"{key} is {value!r}, which is not the corpus's name for it")
    if not any(value != "%s" for value in lang.values()):
        failures.append("no recipe carries a name of its own, so the %s fallback is all that was checked")
    if failures:
        print("FAIL")
        for failure in failures:
            print(f"  {failure}")
        sys.exit(1)
    print(f"ok: {len(lang)} recipe name keys, {sum(v != '%s' for v in lang.values())} of them Factorio's own")


if __name__ == "__main__":
    main()
