#!/usr/bin/env python3
"""Assert the re-authored stock recipes resolve and keep the hand graph a plan (#442, ADR-0034).

`scripts/stock-recipe-convert.py` re-authors each stock recipe `data/pack/stock-admissions.json`
admits as a `factoryworks:assembling` recipe, swapping its ingredients through
`data/pack/stock-substitutions.json`. What it asserts:

  - the generator's `--check` passes: the emitted files are what the line writes today
  - every ingredient of a re-authored recipe is made by another emitted pack recipe, or is a
    `keep` row, which records the block that drops it
  - every output is an item an installed jar defines
  - no re-authored recipe has more item ingredients than the Assembling Machine has input slots
  - the wooden stairs are one recipe per species Terra's biomes grow, each from its own logs
    (#444), with the species read out of Terra's biome files
  - each `author` row is emitted on the machine it names: sand on the Assembling Machine, glass
    as a smelt on the pack's type (#445)
  - no re-authored recipe makes a wall: walls are not kept (#441)
  - over the union of every emitted hand recipe, corpus and re-authored alike, no hand route is a
    cycle: the Personal Assembler has no way out of a loop (ADR-0038). A second hand route to one
    item is `test_recipe_duplication.py`'s.

Usage: tests/factorio/test_stock_recipes.py
"""
import json
import os
import subprocess
import sys
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import test_pack_recipes  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent.parent
EMITTED = ROOT / "kubejs/data/factoryworks/recipe"
STOCK = EMITTED / "assembling/stock"
ADMISSIONS = ROOT / "data/pack/stock-admissions.json"
SUBSTITUTIONS = ROOT / "data/pack/stock-substitutions.json"
GENERATOR = ROOT / "scripts/stock-recipe-convert.py"
MODS = ROOT / "mods"
CLIENT_JAR = Path(os.environ.get("PF_CLIENT_JAR", os.path.expanduser(
    os.environ.get("CURSEFORGE_ROOT", "~/curseforge") + "/Install/versions/26.1.2/26.1.2.jar")))
MACHINE_SPECS = ROOT / "mod/src/main/resources/factoryworks_core/machine/specs.json"
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
    admissions = json.loads(ADMISSIONS.read_text())
    authored = admissions.get("author", {})
    stock = {name: r for name, r in recipes.items()
             if name.startswith("assembling/stock/") or name.startswith("smelting/stock/")}
    check(len(stock) == len(admissions["admit"]) + len(authored),
          "%d recipe(s) in the stock subtrees, %d admitted and %d authored"
          % (len(stock), len(admissions["admit"]), len(authored)))
    for output, row in sorted(authored.items()):
        name = "%s/stock/%s" % (row["on"], output.split(":", 1)[1])
        check(name in stock and outputs_of(stock[name]) == [output]
              and stock[name]["type"] == "factoryworks:" + row["on"],
              "`author` row %s is not emitted as %s" % (output, name))

    made = {item for name, r in recipes.items() if name not in stock for item in outputs_of(r)}
    defined = defined_items()
    slots = json.loads(MACHINE_SPECS.read_text())["assembling-machine-1"]["item_inputs"]
    for name, recipe in sorted(stock.items()):
        check(len(ingredients_of(recipe)) <= slots,
              "%s has %d item ingredients and the Assembling Machine has %d input slots (ADR-0074)"
              % (name, len(ingredients_of(recipe)), slots))
        for ingredient in ingredients_of(recipe):
            check(ingredient in made or ingredient in keep,
                  "%s takes `%s`, which no other pack recipe makes and no `keep` row names. The "
                  "player cannot obtain it, so the recipe is craftable nowhere" % (name, ingredient))
        for output in outputs_of(recipe):
            check(output in defined,
                  "%s makes `%s`, which no installed jar defines an item for" % (name, output))


def vanilla_item_tag(name):
    if not CLIENT_JAR.is_file():
        return None
    with zipfile.ZipFile(CLIENT_JAR) as jar:
        return set(json.loads(jar.read("data/minecraft/tags/item/%s.json" % name))["values"])


def check_wooden_stairs(recipes):
    """One stairs recipe per species Terra grows, so a species it stops growing drops its stairs."""
    wooden_stairs = vanilla_item_tag("wooden_stairs")
    if not check(wooden_stairs is not None,
                 "the client jar is not at %s, so the wooden stairs are unchecked" % CLIENT_JAR):
        return
    grown = test_pack_recipes.terra_species()
    wooden = {name.rsplit("/", 1)[1]: recipe for name, recipe in recipes.items()
              if name.startswith("assembling/stock/") and set(outputs_of(recipe)) & wooden_stairs}
    check(set(wooden) == {"%s_stairs" % species for species in grown},
          "wooden stairs %s, and Terra grows %s" % (sorted(wooden), sorted(grown)))
    for name, recipe in sorted(wooden.items()):
        species = name[: -len("_stairs")]
        check(ingredients_of(recipe) == ["#minecraft:%s_logs" % species],
              "%s takes %s; a species' stairs are made from that species' logs"
              % (name, ingredients_of(recipe)))


def check_no_walls(recipes):
    for name, recipe in sorted(recipes.items()):
        if name.startswith("assembling/stock/"):
            for output in outputs_of(recipe):
                check(not output.endswith("_wall"), "%s makes %s, and walls are not kept (#441)"
                      % (name, output))


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
    if check(STOCK.is_dir(), "kubejs/data/factoryworks/recipe/assembling/stock/ does not exist"):
        check_stock(recipes, keep)
        check_wooden_stairs(recipes)
        check_no_walls(recipes)
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
