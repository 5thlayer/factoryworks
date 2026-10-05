#!/usr/bin/env python3
"""Emit the per-machine spec the crafting chassis reads (#489, ADR-0096).

Writes one row per Factorio machine whose recipes the pack emits on `AssemblingRecipe`'s shape --
the Chemical Plant and the Oil Refinery -- to
`mod/src/main/resources/factoryworks_core/machine/specs.json`, which `MachineSpecs` reads, and
the Overload Limit's constants from `data/factorio/overload.json` beside it, which `OverloadLimit`
reads (#517).

Speed, energy use, drain, the Fast Replace group, the categories and the tank volumes are copied
from `data/factorio/machine.json`. The recipe type is the one `data/pack/category-map.json` routes
the machine's first category to.

Slot and tank counts follow the recipes, not the entity (ADR-0096): each is the most any emitted
recipe of the machine's type and categories needs. The Chemical Plant's second output box is one no
recipe fills. A machine whose recipes need more
tanks than the entity has boxes is a hard failure.

Usage:

    scripts/build-machine-specs.py            # writes the resource
    scripts/build-machine-specs.py --check    # asserts it is already up to date; no writes
"""
import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MACHINE_CORPUS = ROOT / "data" / "factorio" / "machine.json"
CATEGORY_MAP = ROOT / "data" / "pack" / "category-map.json"
EMITTED = ROOT / "kubejs" / "data" / "factoryworks" / "recipe"
RESOURCE = ROOT / "mod/src/main/resources/factoryworks_core/machine/specs.json"
OVERLOAD_CORPUS = ROOT / "data" / "factorio" / "overload.json"
OVERLOAD = RESOURCE.parent / "overload.json"

# The types that share AssemblingRecipe's record, and so the chassis (ADR-0096).
CHASSIS_TYPES = ("factoryworks:chemistry", "factoryworks:oil_processing")


def recipe_type(row, category_map):
    route = category_map["routes"].get(row["crafting_categories"][0]) if row.get("crafting_categories") else None
    if route is None or route.startswith("!"):
        return None
    return category_map["machines"][route]["recipe_type"]


def needs(recipe_type_id, categories):
    """The most items and fluids in and out any emitted recipe of this type and these categories needs."""
    folder = EMITTED / recipe_type_id.split(":")[1]
    most = {"ingredients": 0, "results": 0, "fluid_ingredients": 0, "fluid_results": 0}
    found = 0
    for path in sorted(folder.rglob("*.json")):
        recipe = json.loads(path.read_text(encoding="utf-8"))
        if recipe.get("type") != recipe_type_id or recipe.get("category") not in categories:
            continue
        found += 1
        for field in most:
            most[field] = max(most[field], len(recipe.get(field, [])))
    return most, found


def volumes(row, production_type, count):
    boxes = [box["volume"] for box in row["fluid_boxes"] if box["production_type"] == production_type]
    if len(boxes) < count:
        sys.exit(f"{row['name']}'s recipes need {count} {production_type} tanks and it has "
                 f"{len(boxes)} fluid boxes in {MACHINE_CORPUS}")
    return boxes[:count]


def pinned():
    """Emitted chassis recipes whose Factorio recipe pins a fluid product to a box (#520)."""
    recipes = json.loads((ROOT / "data" / "factorio" / "recipe.json").read_text(encoding="utf-8"))
    out = []
    for recipe in recipes:
        if not any("fluidbox_index" in r for r in recipe.get("results", [])):
            continue
        stem = recipe["name"].replace("-", "_")
        found = [t for t in CHASSIS_TYPES if (EMITTED / t.split(":")[1] / f"{stem}.json").is_file()]
        if len(found) != 1:
            sys.exit(f"{recipe['name']} pins a fluid product and is emitted under {found}")
        out.append(f"{found[0]}/{stem}")
    return sorted(out)


def specs():
    machine = json.loads(MACHINE_CORPUS.read_text(encoding="utf-8"))
    category_map = json.loads(CATEGORY_MAP.read_text(encoding="utf-8"))
    out = {}
    for row in machine["machines"]:
        type_id = recipe_type(row, category_map)
        if type_id not in CHASSIS_TYPES:
            continue
        most, found = needs(type_id, row["crafting_categories"])
        if found == 0:
            sys.exit(f"{row['name']}: no emitted {type_id} recipe of {row['crafting_categories']} "
                     "-- re-run scripts/factorio-recipe-convert.py")
        out[row["name"]] = {
            "recipe_type": type_id,
            "categories": row["crafting_categories"],
            "crafting_speed": row["crafting_speed"],
            "energy_usage": row["energy_usage"],
            "drain": row["drain"],
            "fast_replaceable_group": row["fast_replaceable_group"],
            "item_inputs": most["ingredients"],
            "item_outputs": most["results"],
            "fluid_inputs": volumes(row, "input", most["fluid_ingredients"]),
            "fluid_outputs": volumes(row, "output", most["fluid_results"]),
            "fluid_output_boxes": [box["volume"] for box in row["fluid_boxes"] if box["production_type"] == "output"],
        }
    return out


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    corpus = json.loads(OVERLOAD_CORPUS.read_text(encoding="utf-8"))
    overload = {**corpus["constants"],
                "fluid_input_multiplier": corpus["measured"]["fluid_input"]["multiplier"],
                "fluid_output_multiplier": corpus["measured"]["fluid_output_volume"]["multiplier"],
                "pinned_fluid_outputs": pinned()}
    outputs = {RESOURCE: specs(), OVERLOAD: overload}
    for path, data in outputs.items():
        text = json.dumps(data, indent=2) + "\n"
        if args.check:
            if not path.is_file() or path.read_text(encoding="utf-8") != text:
                sys.exit(f"stale: {path} -- run scripts/build-machine-specs.py")
            print(f"OK -- {path.relative_to(ROOT)}")
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        print(f"wrote {path.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
