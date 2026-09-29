#!/usr/bin/env python3
"""Extract Factorio's Overload Limit into data/factorio/overload.json (#517).

The three constants are `utility-constants` fields and are read from the dump. The rule's shape and
the output threshold are engine behaviour, so they are written under `measured` with the figures
`scripts/factorio-overload-probe/` read on Factorio 2.1.20. `tests/factorio/test_overload_extract.py`
re-derives every measured input case from the constants and the recipe and machine corpora.

To re-measure, load the probe as a mod beside `base` only, `--create` a map and `--benchmark` it
for 3,700 ticks; it appends to `script-output/overload_probe.txt`. The fluid cases are
`scripts/factorio-overload-fluid-probe/`, run the same way into `overload_fluid_probe.txt` (#519).

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

# Unpowered, fed by infinity pipes; `held` is where each input box stopped, `speed` the machine's
# crafting speed with any beacons, so a high-speed case shows the fluid rule ignores speed.
FLUID_INPUT_CASES = [
    {"machine": "assembling-machine-2", "recipe": "concrete", "speed": 0.75, "held": {"water": 400}},
    {"machine": "assembling-machine-3", "recipe": "concrete", "speed": 1.25, "held": {"water": 400}},
    {"machine": "assembling-machine-3", "recipe": "concrete", "speed": 5.84225, "held": {"water": 400}},
    {"machine": "chemical-plant", "recipe": "plastic-bar", "speed": 1, "held": {"petroleum-gas": 80}},
    {"machine": "chemical-plant", "recipe": "lubricant", "speed": 1, "held": {"heavy-oil": 40}},
    {"machine": "chemical-plant", "recipe": "lubricant", "speed": 4.6738, "held": {"heavy-oil": 40}},
    {"machine": "chemical-plant", "recipe": "sulfuric-acid", "speed": 1, "held": {"water": 400}},
    {"machine": "chemical-plant", "recipe": "heavy-oil-cracking", "speed": 1, "held": {"water": 120, "heavy-oil": 160}},
    {"machine": "chemical-plant", "recipe": "heavy-oil-cracking", "speed": 4.6738,
     "held": {"water": 120, "heavy-oil": 160}},
    {"machine": "oil-refinery", "recipe": "basic-oil-processing", "speed": 1, "held": {"crude-oil": 400}},
    {"machine": "oil-refinery", "recipe": "basic-oil-processing", "speed": 4.6738, "held": {"crude-oil": 400}},
    {"machine": "chemical-plant", "recipe": "light-oil-cracking", "speed": 1, "held": {"water": 120, "light-oil": 120}},
    {"machine": "chemical-plant", "recipe": "solid-fuel-from-light-oil", "speed": 1, "held": {"light-oil": 40}},
    {"machine": "oil-refinery", "recipe": "advanced-oil-processing", "speed": 1,
     "held": {"water": 200, "crude-oil": 400}},
]

# Powered and kept fed; `held` is each output box when the machine reported full_output, and
# `volume` the box's volume the engine reported with the recipe set.
FLUID_OUTPUT_CASES = [
    {"machine": "chemical-plant", "recipe": "lubricant", "held": {"lubricant": 200}, "volume": {"lubricant": 200}},
    {"machine": "chemical-plant", "recipe": "sulfuric-acid", "held": {"sulfuric-acid": 200},
     "volume": {"sulfuric-acid": 200}},
    {"machine": "chemical-plant", "recipe": "heavy-oil-cracking", "held": {"light-oil": 180},
     "volume": {"light-oil": 200}},
    {"machine": "oil-refinery", "recipe": "basic-oil-processing", "held": {"petroleum-gas": 135},
     "volume": {"petroleum-gas": 135}},
    {"machine": "oil-refinery", "recipe": "advanced-oil-processing",
     "held": {"heavy-oil": 75, "light-oil": 135, "petroleum-gas": 165},
     "volume": {"heavy-oil": 100, "light-oil": 135, "petroleum-gas": 165}},
]

# Unpowered, the recipe set; `volume` is each output box the engine reported, `boxes` the machine's
# output boxes. The probe's own recipes (`probe-*`, in its data.lua) have amounts base lacks.
# basic-oil-processing pins its product to a box with `fluidbox_index`.
FLUID_OUTPUT_VOLUME_CASES = [
    {"machine": "chemical-plant", "recipe": "lubricant", "amounts": [10], "volume": [200]},
    {"machine": "chemical-plant", "recipe": "sulfuric-acid", "amounts": [50], "volume": [200]},
    {"machine": "chemical-plant", "recipe": "heavy-oil-cracking", "amounts": [30], "volume": [200]},
    {"machine": "chemical-plant", "recipe": "light-oil-cracking", "amounts": [20], "volume": [200]},
    {"machine": "chemical-plant", "recipe": "probe-chem-60", "amounts": [60], "volume": [200]},
    {"machine": "chemical-plant", "recipe": "probe-chem-100", "amounts": [100], "volume": [300]},
    {"machine": "chemical-plant", "recipe": "probe-chem-two", "amounts": [10, 10], "volume": [100, 100]},
    {"machine": "oil-refinery", "recipe": "basic-oil-processing", "pinned": True, "amounts": [45], "volume": [135]},
    {"machine": "oil-refinery", "recipe": "advanced-oil-processing", "amounts": [25, 45, 55],
     "volume": [100, 135, 165]},
    {"machine": "oil-refinery", "recipe": "coal-liquefaction", "amounts": [90, 20, 10], "volume": [270, 100, 100]},
    {"machine": "oil-refinery", "recipe": "probe-refinery-20", "amounts": [20], "volume": [300]},
    {"machine": "oil-refinery", "recipe": "probe-refinery-two", "amounts": [20, 20], "volume": [200, 100]},
    {"machine": "assembling-machine-2", "recipe": "empty-water-barrel", "amounts": [50], "volume": [1000]},
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
    "fluid_input": {
        "rule": "an input fluid box holds at most amount * multiplier, whatever the crafting speed",
        "multiplier": 4,
        "cases": FLUID_INPUT_CASES,
    },
    "fluid_output": {
        "rule": "a craft starts only while each fluid product's amount fits in the room left in its box",
        "cases": FLUID_OUTPUT_CASES,
    },
    "fluid_output_volume": {
        "rule": "each fluid product's box is the larger of amount * multiplier and its own volume; the first "
                "product also takes the volume of every output box the recipe leaves unused, unless the recipe "
                "pins its products to boxes; crafting speed does not enter",
        "multiplier": 3,
        "cases": FLUID_OUTPUT_VOLUME_CASES,
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
