#!/usr/bin/env python3
"""Extract Factorio's Overload Limit into data/factorio/overload.json (#517).

The three constants are `utility-constants` fields and are read from the dump. The rule's shape and
the output threshold are engine behaviour, so they are written under `measured` with the figures
`scripts/factorio-overload-probe/` read on Factorio 2.1.20. `tests/factorio/test_overload_extract.py`
re-derives every measured input case from the constants and the recipe and machine corpora.

To re-measure, load the probe as a mod beside `base` only, `--create` a map and `--benchmark` it
for 3,700 ticks; it appends to `script-output/overload_probe.txt`.

Usage:

    scripts/factorio-overload-extract.py            # finds the dump, writes data/factorio/overload.json
    scripts/factorio-overload-extract.py --dump PATH
"""

import argparse
import json
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent

DEFAULT_DUMP = (
    Path.home()
    / "Library/Application Support/factorio/script-output/data-raw-dump.json"
)

CONSTANTS = ("dynamic_recipe_overload_factor", "minimum_recipe_overload_multiplier",
             "maximum_recipe_overload_multiplier")

# Read with a burner inserter of hand size 1 into an unpowered machine; `held` is what it stopped at.
INPUT_CASES = [
    {"machine": "assembling-machine-1", "recipe": "iron-gear-wheel", "held": {"iron-plate": 6}},
    {"machine": "assembling-machine-2", "recipe": "engine-unit",
     "held": {"steel-plate": 2, "iron-gear-wheel": 2, "pipe": 4}},
    {"machine": "assembling-machine-3", "recipe": "copper-cable", "held": {"copper-plate": 4}},
    {"machine": "assembling-machine-2", "recipe": "electronic-circuit",
     "held": {"iron-plate": 3, "copper-cable": 9}},
    {"machine": "assembling-machine-3", "recipe": "iron-gear-wheel", "held": {"iron-plate": 8}},
]

# Powered and kept fed; `held` is the product when the machine reported full_output.
OUTPUT_CASES = [
    {"machine": "assembling-machine-2", "recipe": "iron-gear-wheel", "held": 100, "stack_size": 100},
    {"machine": "assembling-machine-2", "recipe": "copper-cable", "held": 200, "stack_size": 200},
    {"machine": "assembling-machine-3", "recipe": "copper-cable", "held": 200, "stack_size": 200},
    {"machine": "assembling-machine-1", "recipe": "pipe", "held": 100, "stack_size": 100},
]

MEASURED = {
    "game": "Factorio 2.1.20, base only",
    "probe": "scripts/factorio-overload-probe/",
    "input": {
        "rule": "an inserter drops while an ingredient holds fewer than amount * clamp(ceil(factor * "
                "crafting_speed / energy_required) + 1, minimum, maximum); the +1 is added before the clamp",
        "cases": INPUT_CASES,
    },
    "output": {
        "rule": "a machine stops crafting when its product's slot holds a full stack; no recipe multiple applies",
        "cases": OUTPUT_CASES,
    },
    "not_measured": {
        "hand": "a benchmark run has no player, so the hand and Quick transfer are not read here",
    },
}


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dump", type=Path, default=DEFAULT_DUMP)
    args = parser.parse_args()

    dump = json.loads(args.dump.read_text(encoding="utf-8"))
    utility = dump["utility-constants"]["default"]
    out = {"constants": {name: utility[name] for name in CONSTANTS}, "measured": MEASURED}
    target = REPO / "data/factorio/overload.json"
    target.write_text(json.dumps(out, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {target.relative_to(REPO)}")


if __name__ == "__main__":
    main()
