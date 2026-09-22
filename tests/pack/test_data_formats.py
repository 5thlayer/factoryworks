#!/usr/bin/env python3
"""Assert every file the pack emits is shaped the way 26.1 reads it, not the way 1.21.1 did (#273).

WHY THIS EXISTS. ADR-0060 called the cost of the 26.1.2 move: "every generator and every asset
check is written against 1.21.1's data formats". The first instance was the ingredient shape, which
reached a player as a furnace that holds the item, holds power and never smelts
(`tests/factorio/test_smelting_shape.py`). The second is in this file.

The interesting half of #273 is not the shapes but the KIND of check. Every generator here has a
`--check` that RE-RUNS THE GENERATOR AND DIFFS ITS OWN OUTPUT -- self-consistent by construction,
and blind to a shape Minecraft rejects. So is every asset-hop check, which walks from a blockstate
to a model to a texture and never asks whether the game would read any of them. This file is the
other kind: it asserts the shape THE GAME PARSES, against no generator at all, and it is deliberately
written so that a format landing in a subtree nobody thought about still fails here.

WHAT IT CHECKS.

  - **Item model definitions exist.** 26.1 resolves an item's model through
    `assets/<ns>/items/<id>.json`, not through `models/item/<id>.json` directly -- the models are
    still there, but nothing reaches them without a definition. A missing definition is not an
    error: the item renders as the black-and-magenta missing model, in the inventory, in the hand
    and in EMI, with nothing in any log. The pack shipped the port with fifteen item models and
    zero definitions, which is every item it registers.
  - **The definitions point at something.** A definition naming a model that is not there fails the
    same silent way as having no definition at all, so the hop is walked.
  - **No orphan definitions.** A definition left behind after its model was renamed is a file the
    game reads and an item nobody can get -- the direction a generator's `--check` does catch, kept
    here so the `items/` tree has exactly one owner.
  - **Ingredients are strings.** Everywhere, in every live emitted recipe, not only in the smelts
    `test_smelting_shape.py` owns. 26.1 parses an ingredient as a string, `#`-prefixed for a tag.
  - **No 1.21.1 directory names.** `loot_tables/`, `recipes/`, `tags/items/` and their kind were
    renamed to the singular before 26.1. A datapack directory the game does not know is not an
    error either: it is simply never walked, and every file under it is absent.

WHAT IT IS NOT. `kubejs/parked/` is excluded: nothing loads it. The `gtceu:` recipe subtrees it
used to defer were deleted when #279 re-targeted the converter onto `planetaryfactory:assembling`,
and a `gtceu:` recipe appearing again fails here like any other stale shape.

It cannot tell whether an id RESOLVES; that is a running server, and the cheap version of it -- a
datapack load with zero `Couldn't parse data file` lines -- is #273's remaining half.

Usage: tests/pack/test_data_formats.py
"""
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GENERATOR = ROOT / "scripts/build-item-definitions.py"

# Every tree the game loads. `kubejs/parked/` is deliberately absent.
ASSET_ROOTS = (
    ROOT / "kubejs/assets",
    ROOT / "mod/src/main/resources/assets",
)
DATA_ROOTS = (
    ROOT / "kubejs/data",
    ROOT / "mod/src/main/resources/data",
)


# Item models left behind by a registration ADR-0060 removed. Named rather than skipped by shape,
# so the day the chassis lands the entry is deleted and the definition is asserted like any other.
DEFERRED_ITEM_MODELS = {
    "kubejs:oil_refinery": "the GregTech multiblock's registration left with ADR-0060 (#258)",
}

# Items drawn by a GeckoLib renderer, whose definition is GeckoLib's special model over the item
# model beside it rather than a plain model. `scripts/build-item-definitions.py` holds the same
# table and writes the shape; this asserts it.
GECKOLIB_ITEMS = {
    "planetaryfactory:assembling_machine": "an OritechGeoItem drawing Oritech's assembler model (#326)",
    "planetaryfactory:steam_engine": "an OritechGeoItem drawing Oritech's steam engine model (#352)",
    "planetaryfactory:pumpjack": "an OritechGeoItem drawing Oritech's pump model (ADR-0081)",
}

# Where an ingredient can appear in a recipe the pack emits. A value under one of these keys is a
# 26.1 ingredient: a string, or a list of them.
INGREDIENT_KEYS = ("ingredient", "ingredients", "key")

# Directory names that were plural before 1.21.2 and are singular now. A datapack directory the
# game does not know is never walked, so every file under it is silently absent.
RENAMED_DIRECTORIES = {
    "loot_tables": "loot_table",
    "recipes": "recipe",
    "advancements": "advancement",
    "structures": "structure",
    "tags/blocks": "tags/block",
    "tags/items": "tags/item",
    "tags/fluids": "tags/fluid",
    "tags/entity_types": "tags/entity_type",
    "tags/game_events": "tags/game_event",
}

failures = []


def check(condition, message):
    if not condition:
        failures.append(message)


def namespaces(root):
    if not root.is_dir():
        return []
    return sorted(path for path in root.iterdir() if path.is_dir())


def check_item_definitions():
    """Every `models/item/X.json` has an `items/X.json` naming it, and nothing else does."""
    total = 0
    for root in ASSET_ROOTS:
        for namespace in namespaces(root):
            models = namespace / "models/item"
            definitions = namespace / "items"
            model_names = {path.stem for path in models.glob("*.json")
                           if "%s:%s" % (namespace.name, path.stem) not in DEFERRED_ITEM_MODELS}
            definition_names = {path.stem for path in definitions.glob("*.json")}
            total += len(model_names)

            for name in sorted(model_names - definition_names):
                failures.append(
                    "%s/items/%s.json is missing. 26.1 resolves an item's model through its "
                    "definition, so `models/item/%s.json` is never reached: the item renders as "
                    "the missing model in the inventory, in the hand and in EMI, with nothing in "
                    "any log" % (namespace.name, name, name)
                )
            for name in sorted(definition_names - model_names):
                failures.append(
                    "%s/items/%s.json has no `models/item/%s.json` behind it. An orphan definition "
                    "is a file the game reads for an item that has no model" % (
                        namespace.name, name, name)
                )

            for name in sorted(model_names & definition_names):
                path = definitions / ("%s.json" % name)
                where = "%s/items/%s.json" % (namespace.name, name)
                definition = json.loads(path.read_text())
                model = definition.get("model")
                check(isinstance(model, dict),
                      "%s states `model` as %r. A definition is an object with a `model` object; "
                      "26.1 has no bare-string form" % (where, model))
                if not isinstance(model, dict):
                    continue
                if "%s:%s" % (namespace.name, name) in GECKOLIB_ITEMS:
                    check(model.get("type") == "minecraft:special"
                          and model.get("base") == "%s:item/%s" % (namespace.name, name)
                          and model.get("model") == {"type": "geckolib:geckolib"},
                          "%s is recorded as GeckoLib-drawn but is not GeckoLib's special model over "
                          "the item model beside it, so the item draws nothing" % where)
                    continue
                check(model.get("type") == "minecraft:model",
                      "%s names the model type `%s`. The pack's items are all plain models; a "
                      "type the game does not know leaves the item with no model at all"
                      % (where, model.get("type")))
                expected = "%s:item/%s" % (namespace.name, name)
                check(model.get("model") == expected,
                      "%s points at `%s` rather than `%s`, so it does not reach the item model "
                      "sitting beside it" % (where, model.get("model"), expected))

    check(total > 0,
          "no item model was found under any asset root at all, so this check asserted nothing. "
          "Every assertion below it is vacuous")
    return total


def ingredient_strings(value, where, key):
    """Fail on anything under an ingredient key that 26.1 would not parse."""
    if isinstance(value, str):
        check(value != "",
              "%s states an empty ingredient under `%s`" % (where, key))
        check(":" in value.lstrip("#"),
              "%s states the ingredient `%s` under `%s`, which names no namespace"
              % (where, value, key))
        return
    if isinstance(value, list):
        for item in value:
            ingredient_strings(item, where, key)
        return
    if isinstance(value, dict) and "ingredient" in value:
        # NeoForge's sized ingredient, `{"ingredient": ..., "count"|"amount": n}` -- the list
        # entries of `planetaryfactory:assembling` (#279). The count is beside the ingredient.
        ingredient_strings(value["ingredient"], where, key)
        return
    if isinstance(value, dict) and "neoforge:ingredient_type" in value:
        # NeoForge's custom-ingredient map, the other half of its `Codec.xor` with the string.
        return
    if isinstance(value, dict) and key == "key":
        # A shaped recipe's `key` is a map of symbol to ingredient, not an ingredient itself.
        for symbol, ingredient in value.items():
            ingredient_strings(ingredient, where, "key[%s]" % symbol)
        return
    failures.append(
        "%s states %r under `%s`. 26.1 parses an ingredient as a string -- a bare id, or a "
        "`#`-prefixed tag. The 1.21.1 object fails at datapack load with `No key type in "
        "MapLike[...]` and the recipe is then absent from the manager, with one ERROR line and no "
        "other symptom" % (where, value, key)
    )


def walk_ingredients(node, where):
    if isinstance(node, dict):
        for key, value in node.items():
            if key in INGREDIENT_KEYS:
                ingredient_strings(value, where, key)
            else:
                walk_ingredients(value, where)
    elif isinstance(node, list):
        for item in node:
            walk_ingredients(item, where)


def check_recipe_ingredients():
    """Every live recipe, whoever emitted it -- not only the subtree one check happens to own."""
    total = 0
    for root in DATA_ROOTS:
        for namespace in namespaces(root):
            for path in sorted((namespace / "recipe").rglob("*.json")):
                recipe = json.loads(path.read_text())
                recipe_type = recipe.get("type", "")
                total += 1
                where = path.relative_to(ROOT).as_posix()
                check(recipe_type != "",
                      "%s names no recipe type" % where)
                walk_ingredients(recipe, where)
    return total


def check_directory_names():
    for root in ASSET_ROOTS + DATA_ROOTS:
        for namespace in namespaces(root):
            for legacy, current in sorted(RENAMED_DIRECTORIES.items()):
                if (namespace / legacy).is_dir():
                    failures.append(
                        "%s holds a `%s/` directory, which 26.1 renamed to `%s/`. The game does "
                        "not walk it: every file under it is silently absent, with nothing in any "
                        "log" % (namespace.relative_to(ROOT).as_posix(), legacy, current)
                    )


def check_generator():
    """The definitions' one owner is current, so an item model landing anywhere fails here."""
    generated = subprocess.run(
        [sys.executable, str(GENERATOR), "--check"], capture_output=True, text=True
    )
    check(generated.returncode == 0,
          "%s --check: %s" % (GENERATOR.relative_to(ROOT),
                              (generated.stdout + generated.stderr).strip()))


def main():
    items = check_item_definitions()
    check_generator()
    recipes = check_recipe_ingredients()
    check_directory_names()

    for failure in failures:
        print("FAIL: " + failure)
    if failures:
        print("\n%d failure(s)" % len(failures))
        return 1
    print("ok: %d item model definition(s) and %d live recipe(s), each shaped the way 26.1 reads "
          "it" % (items, recipes))
    return 0


if __name__ == "__main__":
    sys.exit(main())
