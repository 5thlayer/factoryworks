#!/usr/bin/env python3
"""Assert the per-machine spec the chassis reads is the corpus's (#489, ADR-0096).

`scripts/build-machine-specs.py --check` only proves the resource is what the generator would
write. Each row is also held to its `data/factorio/machine.json` row field by field, and every
emitted recipe of a chassis type to some machine of that type with a slot or tank for each of its
inputs and outputs, since a recipe that fits no machine is one no machine can hold.

Usage: tests/pack/test_machine_specs.py
"""
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
GENERATOR = ROOT / "scripts" / "build-machine-specs.py"
RESOURCE = ROOT / "mod/src/main/resources/factoryworks_core/machine/specs.json"
EMITTED = ROOT / "kubejs/data/factoryworks/recipe"

MACHINES = ("oil-refinery",)

COPIED = {"crafting_speed": "crafting_speed", "energy_usage": "energy_usage", "drain": "drain",
          "fast_replaceable_group": "fast_replaceable_group", "categories": "crafting_categories"}


def row_failures(specs):
    machine = json.loads((ROOT / "data/factorio/machine.json").read_text(encoding="utf-8"))
    rows = {row["name"]: row for row in machine["machines"]}
    failures = []
    for name in MACHINES:
        if name not in specs:
            failures.append(f"{name} has no spec")
    for name, spec in specs.items():
        row = rows[name]
        for field, source in COPIED.items():
            if spec[field] != row[source]:
                failures.append(f"{name}.{field} is {spec[field]!r} and the corpus says {row[source]!r}")
        for field, production in (("fluid_inputs", "input"), ("fluid_outputs", "output")):
            boxes = [box["volume"] for box in row["fluid_boxes"] if box["production_type"] == production]
            if spec[field] != boxes[:len(spec[field])]:
                failures.append(f"{name}.{field} is {spec[field]} and its {production} boxes are {boxes}")
    return failures


def room(spec):
    return (spec["item_inputs"], spec["item_outputs"], len(spec["fluid_inputs"]), len(spec["fluid_outputs"]))


def recipe_failures(specs):
    failures = []
    for type_id in sorted({spec["recipe_type"] for spec in specs.values()}):
        machines = [spec for spec in specs.values() if spec["recipe_type"] == type_id]
        for path in sorted((EMITTED / type_id.split(":")[1]).rglob("*.json")):
            recipe = json.loads(path.read_text(encoding="utf-8"))
            needs = (len(recipe.get("ingredients", [])), len(recipe.get("results", [])),
                     len(recipe.get("fluid_ingredients", [])), len(recipe.get("fluid_results", [])))
            if not any(all(need <= have for need, have in zip(needs, room(spec))) for spec in machines):
                failures.append(f"{path.relative_to(ROOT)} needs {needs} and no {type_id} machine has room")
    return failures


def main():
    failures = []
    generated = subprocess.run([sys.executable, str(GENERATOR), "--check"], capture_output=True, text=True)
    if generated.returncode != 0:
        failures.append(f"{GENERATOR.relative_to(ROOT)} --check: "
                        f"{(generated.stderr or generated.stdout).strip()}")
    if not RESOURCE.is_file():
        failures.append(f"no {RESOURCE.relative_to(ROOT)}")
    else:
        specs = json.loads(RESOURCE.read_text(encoding="utf-8"))
        failures += row_failures(specs) + recipe_failures(specs)

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print(f"ok   {len(MACHINES)} machine specs are the corpus's and every chassis recipe fits one")
    return 0


if __name__ == "__main__":
    sys.exit(main())
