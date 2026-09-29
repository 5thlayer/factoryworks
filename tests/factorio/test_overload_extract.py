#!/usr/bin/env python3
"""Assert the Overload Limit corpus re-derives what the probe measured (#517).

Every measured input case is recomputed from `constants` and the recipe and machine corpora. The
engine unit on Assembling Machine 2 is the case that fixes the order: adding 1 after the clamp
gives 3 crafts where the game holds 2. Each output case must stop at its product's stack.
"""

import json
import math
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DUMP = Path.home() / "Library/Application Support/factorio/script-output/data-raw-dump.json"


def crafts(constants, speed, energy_required, plus_one_first=True):
    lo = constants["minimum_recipe_overload_multiplier"]
    hi = constants["maximum_recipe_overload_multiplier"]
    base = math.ceil(constants["dynamic_recipe_overload_factor"] * speed / energy_required)
    if plus_one_first:
        return min(max(base + 1, lo), hi)
    return min(max(base, lo), hi) + 1


def main():
    corpus = json.loads((ROOT / "data/factorio/overload.json").read_text(encoding="utf-8"))
    recipes = {r["name"]: r for r in json.loads((ROOT / "data/factorio/recipe.json").read_text(encoding="utf-8"))}
    machines = {m["name"]: m for m in json.loads((ROOT / "data/factorio/machine.json").read_text(encoding="utf-8"))["machines"]}
    constants = corpus["constants"]
    failures = []

    order_told = False
    for case in corpus["measured"]["input"]["cases"]:
        recipe = recipes[case["recipe"]]
        speed = machines[case["machine"]]["crafting_speed"]
        n = crafts(constants, speed, recipe["energy_required"])
        want = {i["name"]: i["amount"] * n for i in recipe["ingredients"]}
        if want != case["held"]:
            failures.append(f"{case['machine']} {case['recipe']}: rule gives {want}, probe held {case['held']}")
        if n != crafts(constants, speed, recipe["energy_required"], plus_one_first=False):
            order_told = True
    if not order_told:
        failures.append("no input case tells +1-then-clamp from clamp-then-+1")

    dump = json.loads(DUMP.read_text(encoding="utf-8")) if DUMP.is_file() else None
    for case in corpus["measured"]["output"]["cases"]:
        if case["held"] != case["stack_size"]:
            failures.append(f"{case['recipe']}: output stopped at {case['held']}, not its stack {case['stack_size']}")
        if dump:
            product = recipes[case["recipe"]]["results"][0]["name"]
            if dump["item"][product]["stack_size"] != case["stack_size"]:
                failures.append(f"{product}: stack_size is {dump['item'][product]['stack_size']} in the dump")
    if dump:
        utility = dump["utility-constants"]["default"]
        for name, value in constants.items():
            if utility[name] != value:
                failures.append(f"{name} is {utility[name]} in the dump, {value} in the corpus")

    for failure in failures:
        print(f"FAIL {failure}")
    if failures:
        sys.exit(1)
    print(f"OK -- {len(corpus['measured']['input']['cases'])} input and "
          f"{len(corpus['measured']['output']['cases'])} output cases re-derived")


if __name__ == "__main__":
    main()
