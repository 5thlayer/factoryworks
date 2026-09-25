#!/usr/bin/env python3
"""Assert the re-authored stock recipes resolve and keep the hand graph a plan (#442, ADR-0034).

`scripts/stock-recipe-convert.py` re-authors each stock recipe `data/pack/stock-admissions.json`
admits as a `planetaryfactory:assembling` recipe, swapping its ingredients through
`data/pack/stock-substitutions.json`. What it asserts:

  - the generator's `--check` passes: the emitted files are what the line writes today
  - every ingredient of a re-authored recipe is made by another emitted pack recipe, or is a
    `keep` row, which records the block that drops it
  - every output is an item an installed jar defines
  - no re-authored recipe has more item ingredients than the Assembling Machine has input slots
  - over the union of every emitted hand recipe, corpus and re-authored alike, no hand route is a
    cycle: the Personal Assembler has no way out of a loop (ADR-0038). A second hand route to one
    item is `test_recipe_duplication.py`'s.

Usage: tests/factorio/test_stock_recipes.py
"""
import json
import os
import re
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
EMITTED = ROOT / "kubejs/data/planetaryfactory/recipe"
STOCK = EMITTED / "assembling/stock"
ADMISSIONS = ROOT / "data/pack/stock-admissions.json"
SUBSTITUTIONS = ROOT / "data/pack/stock-substitutions.json"
GENERATOR = ROOT / "scripts/stock-recipe-convert.py"
MODS = ROOT / "mods"
CLIENT_JAR = Path(os.environ.get("PF_CLIENT_JAR", os.path.expanduser(
    "~/curseforge/Install/versions/26.1.2/26.1.2.jar")))
INPUT_SLOTS = ROOT / "mod/src/main/java/com/planetaryfactory/core/machine/AssemblingInputSlots.java"
HAND = "crafting"

failures = []


def check(condition, message):
    if not condition:
        failures.append(message)
    return condition


def emitted():
    return {p.relative_to(EMITTED).with_suffix("").as_posix(): json.loads(p.read_text())
            for p in sorted(EMITTED.rglob("*.json"))}


def outputs_of(recipe):
    if "result" in recipe:
        return [recipe["result"]["id"]]
    return [entry["id"] for entry in recipe.get("results", [])]


def ingredients_of(recipe):
    """Item ids and `#`-tags a recipe consumes."""
    if "ingredient" in recipe:
        return [recipe["ingredient"]]
    found = []
    for entry in recipe.get("ingredients", []):
        ingredient = entry["ingredient"]
        found.append(ingredient["items"] if isinstance(ingredient, dict) else ingredient)
    return found


def defined_items():
    """Every `namespace:path` an installed jar ships an item model definition for."""
    items = set()
    for jar in sorted(MODS.glob("*.jar")) + ([CLIENT_JAR] if CLIENT_JAR.is_file() else []):
        with zipfile.ZipFile(jar) as archive:
            for name in archive.namelist():
                parts = name.split("/")
                if len(parts) == 4 and parts[0] == "assets" and parts[2] == "items" \
                        and parts[3].endswith(".json"):
                    items.add("%s:%s" % (parts[1], parts[3][:-5]))
    return items


def check_generator():
    run = subprocess.run([sys.executable, str(GENERATOR), "--check", "--quiet"],
                         capture_output=True, text=True)
    check(run.returncode == 0, "stock-recipe-convert.py --check failed:\n" + run.stdout + run.stderr)


def check_stock(recipes, keep):
    admitted = json.loads(ADMISSIONS.read_text())["admit"]
    stock = {name: r for name, r in recipes.items() if name.startswith("assembling/stock/")}
    check(len(stock) == len(admitted),
          "%d recipe(s) under assembling/stock/ and %d admitted" % (len(stock), len(admitted)))

    made = {item for name, r in recipes.items() if name not in stock for item in outputs_of(r)}
    defined = defined_items()
    slots = int(re.search(r"public static final int INPUTS = (\d+);", INPUT_SLOTS.read_text()).group(1))
    for name, recipe in sorted(stock.items()):
        check(len(recipe["ingredients"]) <= slots,
              "%s has %d item ingredients and the Assembling Machine has %d input slots (ADR-0074)"
              % (name, len(recipe["ingredients"]), slots))
        for ingredient in ingredients_of(recipe):
            check(ingredient in made or ingredient in keep,
                  "%s takes `%s`, which no other pack recipe makes and no `keep` row names. The "
                  "player cannot obtain it, so the recipe is craftable nowhere" % (name, ingredient))
        for output in outputs_of(recipe):
            check(output in defined,
                  "%s makes `%s`, which no installed jar defines an item for" % (name, output))


def check_hand_graph(recipes):
    """No hand route that needs its own output."""
    makers = {}
    for name, recipe in recipes.items():
        if recipe.get("category") != HAND:
            continue
        for output in outputs_of(recipe):
            makers.setdefault(output, []).append(name)

    needs = {item: {i for name in names for i in ingredients_of(recipes[name])}
             for item, names in makers.items()}
    done, path = set(), []

    def walk(item):
        if item in done:
            return
        if item in path:
            loop = path[path.index(item):] + [item]
            check(False, "hand routes form a cycle: " + " -> ".join(loop))
            return
        path.append(item)
        for ingredient in sorted(needs.get(item, ())):
            walk(ingredient)
        path.pop()
        done.add(item)

    for item in sorted(needs):
        walk(item)


def main():
    check_generator()
    recipes = emitted()
    keep = json.loads(SUBSTITUTIONS.read_text())["keep"]
    if check(STOCK.is_dir(), "kubejs/data/planetaryfactory/recipe/assembling/stock/ does not exist"):
        check_stock(recipes, keep)
    check_hand_graph(recipes)

    for failure in failures:
        print("FAIL: " + failure)
    if failures:
        print("\n%d failure(s)" % len(failures))
        return 1
    stock = sum(1 for name in recipes if name.startswith("assembling/stock/"))
    print("ok: %d re-authored stock recipe(s) resolve; %d hand recipe(s) in the union and no cycle" % (stock, sum(1 for r in recipes.values()
                                                 if r.get("category") == HAND)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
