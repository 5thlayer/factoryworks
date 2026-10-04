#!/usr/bin/env python3
"""Assert the extracted machine corpus is internally consistent and still routable.

`scripts/factorio-machine-extract.py` reads a 28MB Factorio dump that is not in the repo,
so nothing here re-runs it. What is checkable without the dump -- and without launching the
game -- is whether the *committed* output still says what the decisions say it says:

  - every route in `data/pack/category-map.json` names a category some extracted entity
    actually declares, because a route naming a category nothing crafts is a typo that
    would silently send recipes nowhere. `!`-prefixed routes are checked too: a deliberate
    non-route still has to name a real category.
  - every routed category a pack machine owns is declared by at least one *in-scope*
    machine, because that machine is where the pack machine's speed and power come from.
    `hand-crafting` is the one exception, and it is one by nature: Factorio hands it to the
    character rather than to any machine, so the pack's `personal_assembler` has no
    prototype to read a speed off and never will.
  - every machine carries the four numbers `#126`'s rule reads -- crafting speed, energy
    usage, module slots, categories -- because a null is a number nobody extracted
  - drain is derived, not copied: an electric machine's drain is exactly `energy_usage/30`
    unless the prototype set one, and a burner machine has none. `#126` excludes drain from
    the conversion; the ledger quotes this figure, so it must not drift silently.
  - a burner machine carries its `burner` block and an electric one does not, because
    ADR-0047 spends a fuel's joules against `energy_usage` and `effectivity` and admits it
    on `fuel_categories`. A null block on a burner is two of those three terms missing.
  - the fluid anchors are present, because `#126`'s 1 unit = 1 mB is derived from them
  - `#188`'s five prototypes are all there and carry every field their consumers read:
    both drills for ADR-0043's rigs, the boiler and the steam engine for ADR-0048's chain.
    A missing field here reaches the consumer as a number typed by hand, which is the thing
    ADR-0041 forbids.
  - the Radar's row (#368) matches the prototype in the dump when the dump is on disk, and
    its own terms give Factorio's 33.3 s per sector at full power.
  - every machine and pole carries Factorio's `fast_replaceable_group` (ADR-0082), matching the
    dump when it is on disk. Without the dump, the groups' shape is held instead: the furnaces
    share one, the Assembling Machines share one, the small and medium poles share one, and the
    substation's is its own.
  - the widening did not perturb the twelve crafting machines. Drills, boilers and
    generators do not craft and must not appear in `machines`; a drill that leaked in would
    carry a null `crafting_speed` and the recipe converter would inherit it silently.

Usage: tests/factorio/test_machine_extract.py
"""
import json
import sys
import tomllib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent

DUMP = Path.home() / "Library/Application Support/factorio/script-output/data-raw-dump.json"

DEFAULT_DRAIN_FRACTION = 30

# Steam's own figures, from the fluid prototype the generator's box filters on. Transcribed
# rather than read, which everywhere else in this repo would be the bug: a check that reads
# the extractor's own source re-runs the multiplication instead of testing it, and this file
# deliberately never opens the 28MB dump. The transcription is the independent second source,
# and its cost is that a changed steam prototype has to be changed here too.
STEAM_DEFAULT_TEMPERATURE = 15
STEAM_HEAT_CAPACITY = 200.0
TICKS_PER_SECOND = 60

# Declared by `character` and `god-controller`, never by a machine. A route to it is
# correct and still has no crafting-speed source; see the module docstring.
CHARACTER_CATEGORIES = {"hand-crafting"}

# The twelve `#126` extracted, before `#188` widened the script. Pinned by name because the
# widening must add lists, never rows: see the module docstring.
CRAFTING_MACHINES = {
    "assembling-machine-1",
    "assembling-machine-2",
    "assembling-machine-3",
    "centrifuge",
    "chemical-plant",
    "crusher",
    "oil-refinery",
    "electric-furnace",
    "steel-furnace",
    "stone-furnace",
    "rocket-silo",
    "lab",
}

# `#188`'s deliverable, by name: the prototypes ADR-0043 and ADR-0048 are authored against.
REQUIRED_DRILLS = ("burner-mining-drill", "electric-mining-drill")
REQUIRED_BOILER = "boiler"
REQUIRED_GENERATOR = "steam-engine"

# The Radar's prototype as the dump states it, for when the dump is not on disk. The seconds
# per sector is Factorio's own tooltip figure and is what the row must give back.
RADAR_PROTOTYPE = {
    "energy_usage": "300kW",
    "energy_per_sector": "10MJ",
    "energy_per_nearby_scan": "250kJ",
    "max_distance_of_sector_revealed": 14,
    "max_distance_of_nearby_sector_revealed": 3,
}
RADAR_SECONDS_PER_SECTOR = 33.3

# The accumulator's energy source as the dump states it, for when the dump is not on disk.
ACCUMULATOR_SOURCE = {
    "buffer_capacity": "5MJ",
    "input_flow_limit": "300kW",
    "output_flow_limit": "300kW",
}
WIREWORKS_CONFIG = ROOT / "config/wireworks-server.toml"
RADAR_TILES = (3, 3)

# The solar panel's prototype as the dump states it, for when the dump is not on disk (#529).
SOLAR_PROTOTYPE = {"production": "60kW"}
SOLAR_TILES = (3, 3)
SI = {"k": 1e3, "M": 1e6}

# The key set every crafting-machine row has carried since `#126`. Pinned alongside the
# names: "no existing machine row changes" is a claim about the row's shape as well as
# about which rows there are.
MACHINE_FIELDS = {
    "name",
    "type",
    "crafting_speed",
    "energy_usage",
    "energy_type",
    "drain",
    "drain_source",
    "burner",
    "module_slots",
    "fast_replaceable_group",
    "crafting_categories",
    "fluid_boxes",
    "tile_width",
    "tile_height",
}


def si(text):
    number = str(text).rstrip("WJ")
    return float(number[:-1]) * SI[number[-1]] if number[-1] in SI else float(number)


def radar_failures(radar):
    if radar is None:
        return ["no radar -- #368's Radar has no draw, sector cost or reach to read"]
    failures = []
    prototype = RADAR_PROTOTYPE
    if DUMP.is_file():
        prototype = json.loads(DUMP.read_text(encoding="utf-8"))["radar"]["radar"]
    else:
        print(f"note no dump at {DUMP}; the radar row is compared to the transcription")
    for field in RADAR_PROTOTYPE:
        want = prototype[field]
        want = si(want) if isinstance(want, str) else want
        if radar.get(field) != want:
            failures.append(f"radar's {field} is {radar.get(field)!r}, the prototype says {want!r}")
    if (radar.get("tile_width"), radar.get("tile_height")) != RADAR_TILES:
        failures.append(f"radar is {radar.get('tile_width')}x{radar.get('tile_height')}, not 3x3")
    if radar.get("energy_type") != "electric":
        failures.append(f"radar is {radar.get('energy_type')}, not electric")
    seconds = radar["energy_per_sector"] / radar["energy_usage"]
    if round(seconds, 1) != RADAR_SECONDS_PER_SECTOR:
        failures.append(
            f"radar charts a sector every {seconds:.2f} s, not Factorio's "
            f"{RADAR_SECONDS_PER_SECTOR} s"
        )
    return failures


def accumulator_failures(accumulator):
    """The row against the dump, and the Pack's Wireworks config against the row (#283, #617)."""
    if accumulator is None:
        return ["no accumulator -- the Wireworks config has no buffer or flow to be held to"]
    failures = []
    source = ACCUMULATOR_SOURCE
    if DUMP.is_file():
        source = json.loads(DUMP.read_text(encoding="utf-8"))["accumulator"]["accumulator"][
            "energy_source"
        ]
    else:
        print(f"note no dump at {DUMP}; the accumulator row is compared to the transcription")
    for field in ("buffer_capacity", "input_flow_limit", "output_flow_limit"):
        want = si(source[field])
        if accumulator.get(field) != want:
            failures.append(
                f"accumulator's {field} is {accumulator.get(field)!r}, the prototype says {want!r}"
            )
    if accumulator.get("input_flow_limit") != accumulator.get("output_flow_limit"):
        failures.append("accumulator's flow limits differ, and Wireworks has one max_watts")
    config = tomllib.loads(WIREWORKS_CONFIG.read_text(encoding="utf-8")).get("accumulator", {})
    held = {
        "capacity_joules": accumulator.get("buffer_capacity"),
        "max_watts": accumulator.get("input_flow_limit"),
    }
    for key, want in held.items():
        if config.get(key) != want:
            failures.append(f"wireworks-server.toml [accumulator] {key} is {config.get(key)!r}, the corpus says {want!r}")
    return failures


def solar_panel_failures(panel):
    if panel is None:
        return ["no solar-panel -- #529's Solar Panel has no peak to read"]
    failures = []
    prototype = SOLAR_PROTOTYPE
    if DUMP.is_file():
        prototype = json.loads(DUMP.read_text(encoding="utf-8"))["solar-panel"]["solar-panel"]
    else:
        print(f"note no dump at {DUMP}; the solar panel row is compared to the transcription")
    want = si(prototype["production"])
    if panel.get("production") != want:
        failures.append(f"solar-panel's production is {panel.get('production')!r}, the prototype says {want!r}")
    if (panel.get("tile_width"), panel.get("tile_height")) != SOLAR_TILES:
        failures.append(f"solar-panel is {panel.get('tile_width')}x{panel.get('tile_height')}, not 3x3")
    return failures


REPLACE_GROUPS_SHARED = (
    ("stone-furnace", "steel-furnace", "electric-furnace"),
    ("assembling-machine-1", "assembling-machine-2", "assembling-machine-3"),
    ("small-electric-pole", "medium-electric-pole"),
)
POLES = ("small-electric-pole", "medium-electric-pole", "big-electric-pole", "substation")


def replace_group_failures(data):
    rows = {m["name"]: m for m in data["machines"]}
    poles = {p["name"]: p for p in data.get("poles") or []}
    failures = [f"no pole row {name}" for name in POLES if name not in poles]
    rows.update(poles)
    failures += [
        f"{name} carries no fast_replaceable_group key"
        for name, row in sorted(rows.items())
        if "fast_replaceable_group" not in row
    ]
    if failures:
        return failures

    if DUMP.is_file():
        dump = json.loads(DUMP.read_text(encoding="utf-8"))
        for name, row in sorted(rows.items()):
            prototype = dump[row["type"]][name]
            want = prototype.get("fast_replaceable_group")
            if row["fast_replaceable_group"] != want:
                failures.append(
                    f"{name}'s fast_replaceable_group is {row['fast_replaceable_group']!r}, "
                    f"the dump says {want!r}"
                )
    else:
        print(f"note no dump at {DUMP}; the replace groups are held to their shape only")

    group = {name: row["fast_replaceable_group"] for name, row in rows.items()}
    for names in REPLACE_GROUPS_SHARED:
        if None in {group[n] for n in names} or len({group[n] for n in names}) != 1:
            failures.append(f"{', '.join(names)} do not share one group: "
                            f"{[group[n] for n in names]}")
    others = {g for n, g in group.items() if n != "substation"}
    if group["substation"] is None or group["substation"] in others:
        failures.append(f"the substation's group {group['substation']!r} is not its own")
    return failures


def main():
    data = json.loads((ROOT / "data/factorio/machine.json").read_text())
    routes = json.loads((ROOT / "data/pack/category-map.json").read_text())["routes"]
    machines = data["machines"]
    categories = data["categories"]
    by_name = {m["name"]: m for m in machines}
    failures = []

    if not machines:
        failures.append("machine.json lists no machines")

    for category, machine in sorted(routes.items()):
        if category not in categories:
            failures.append(
                f"route {category!r} -> {machine} names a category no entity declares"
            )
        elif not machine.startswith("!") and not categories[category]:
            failures.append(
                f"route {category!r} -> {machine} names a category with no crafting entity"
            )

    in_scope = {c for m in machines for c in (m["crafting_categories"] or [])}
    for category, machine in sorted(routes.items()):
        if machine.startswith("!") or category not in categories:
            continue
        if category in CHARACTER_CATEGORIES:
            continue
        if category not in in_scope:
            failures.append(
                f"{machine} crafts {category!r}, which no in-scope machine declares "
                "-- its speed and power have no source"
            )

    for machine in machines:
        for field in ("crafting_speed", "energy_usage", "crafting_categories"):
            if machine[field] in (None, [], 0):
                failures.append(f"{machine['name']} has no {field}")
        if machine["module_slots"] is None:
            failures.append(f"{machine['name']} has no module_slots")

    radars = {r["name"]: r for r in data.get("radars") or []}
    failures.extend(radar_failures(radars.get("radar")))
    accumulators = {a["name"]: a for a in data.get("accumulators") or []}
    failures.extend(accumulator_failures(accumulators.get("accumulator")))
    panels = {p["name"]: p for p in data.get("solar_panels") or []}
    failures.extend(solar_panel_failures(panels.get("solar-panel")))

    for machine in (
        machines
        + (data.get("drills") or [])
        + (data.get("boilers") or [])
        + list(radars.values())
    ):
        drain, source = machine["drain"], machine["drain_source"]
        if machine["energy_type"] != "electric":
            if drain is not None or source != "none":
                failures.append(
                    f"{machine['name']} is a {machine['energy_type']} machine with a drain"
                )
        elif source == "default":
            want = machine["energy_usage"] / DEFAULT_DRAIN_FRACTION
            if drain is None or abs(drain - want) > 1e-6:
                failures.append(
                    f"{machine['name']} drains {drain}, but the engine default on "
                    f"{machine['energy_usage']}W is {want}"
                )
        elif source != "explicit":
            failures.append(f"{machine['name']} has drain_source {source!r}")

    for machine in machines:
        burner = machine.get("burner")
        if machine["energy_type"] == "burner":
            if not burner:
                failures.append(
                    f"{machine['name']} is a burner with no burner block -- ADR-0047's "
                    "fuel arithmetic has no effectivity and no category filter"
                )
            elif not burner.get("fuel_categories") or burner.get("effectivity") is None:
                failures.append(f"{machine['name']}'s burner block is incomplete: {burner}")
        elif burner is not None:
            failures.append(
                f"{machine['name']} is {machine['energy_type']} and still carries a burner block"
            )

    anchors = {c["name"]: c for c in data["containers"]}
    for name in ("storage-tank", "pipe"):
        anchor = anchors.get(name)
        if not anchor or not anchor.get("volume") or not anchor.get("tile_width"):
            failures.append(f"no fluid anchor for {name} -- #126's 1 unit = 1 mB is underived")

    for machine in machines:
        if set(machine) != MACHINE_FIELDS:
            failures.append(
                f"{machine['name']}'s fields changed: "
                f"{sorted(set(machine) ^ MACHINE_FIELDS)}"
            )

    if {m["name"] for m in machines} != CRAFTING_MACHINES:
        failures.append(
            "machines is no longer the twelve crafting machines: "
            f"{sorted(set(m['name'] for m in machines) ^ CRAFTING_MACHINES)}"
        )

    drills = {d["name"]: d for d in data.get("drills") or []}
    boilers = {b["name"]: b for b in data.get("boilers") or []}
    generators = {g["name"]: g for g in data.get("generators") or []}

    for name in REQUIRED_DRILLS:
        drill = drills.get(name)
        if not drill:
            failures.append(f"no {name} -- ADR-0043's rig has no mining speed to read")
            continue
        for field in (
            "mining_speed",
            "energy_usage",
            "tile_width",
            "tile_height",
            "resource_categories",
        ):
            if not drill.get(field):
                failures.append(f"{name} has no {field}")

        # The two figures ADR-0043's eject and mining area would otherwise be typed from.
        # A regeneration that drops either leaves the rig reading a null and falling back to
        # a number somebody chose, which is the failure ADR-0041 exists to make visible.
        radius = drill.get("resource_searching_radius")
        if not isinstance(radius, (int, float)):
            failures.append(
                f"{name} has no resource_searching_radius -- the mining area would be typed"
            )
        vector = drill.get("vector_to_place_result")
        if not (
            isinstance(vector, list)
            and len(vector) == 2
            and all(isinstance(part, (int, float)) for part in vector)
        ):
            failures.append(
                f"{name}'s vector_to_place_result is not an [x, y]: {vector!r} "
                "-- the output tile would be typed"
            )

        burner = drill.get("burner")
        if drill["energy_type"] == "burner" and not (
            burner and burner.get("fuel_categories") and burner.get("effectivity") is not None
        ):
            failures.append(f"{name}'s burner block is missing or incomplete: {burner}")
        if drill["energy_type"] != "burner" and drill.get("burner") is not None:
            failures.append(f"{name} is {drill['energy_type']} and carries a burner block")

    boiler = boilers.get(REQUIRED_BOILER)
    if not boiler:
        failures.append("no boiler -- ADR-0048's steam chain has no consumption figure")
    else:
        for field in ("energy_consumption", "target_temperature", "effectivity"):
            if not boiler.get(field):
                failures.append(f"boiler has no {field}")
        if boiler["energy_type"] == "burner" and not boiler.get("burner"):
            failures.append("the boiler is a burner with no burner block")
        kinds = {b["production_type"] for b in boiler.get("fluid_boxes") or []}
        if not {"input", "output"} <= kinds:
            failures.append(f"the boiler has no input and output fluid boxes: {kinds}")
        for box in boiler.get("fluid_boxes") or []:
            if not box.get("volume"):
                failures.append(
                    f"the boiler's {box['production_type']} box has no volume "
                    "-- ADR-0048 holds what the prototype says it holds"
                )

    if REQUIRED_GENERATOR not in generators:
        failures.append("no steam-engine -- the pack's own is authored against it")
    # Every generator, not only the required one: a derived figure nothing re-derives is
    # the same untrusted number whichever entity carries it.
    for name, generator in sorted(generators.items()):
        for field in ("effectivity", "fluid_usage_per_tick", "maximum_temperature"):
            if generator.get(field) is None:
                failures.append(f"{name} has no {field}")
        power, source = generator.get("max_power_output"), generator.get("max_power_output_source")
        if not power:
            failures.append(f"{name} has no max_power_output")
        elif source == "derived":
            # `fluid_usage_per_tick` * degrees above the fluid's default * heat capacity *
            # effectivity * 60. Re-derived here so the figure cannot drift silently.
            box = (generator.get("fluid_boxes") or [{}])[0]
            span = generator["maximum_temperature"] - STEAM_DEFAULT_TEMPERATURE
            want = (
                generator["fluid_usage_per_tick"]
                * span
                * STEAM_HEAT_CAPACITY
                * generator["effectivity"]
                * TICKS_PER_SECOND
            )
            if box.get("filter") != "steam":
                failures.append(f"{name} consumes {box.get('filter')!r}, not steam")
            elif abs(power - want) > 1e-6:
                failures.append(f"{name} reports {power}W, but its own terms give {want}")
        elif source != "explicit":
            failures.append(f"{name} has max_power_output_source {source!r}")
        box = (generator.get("fluid_boxes") or [{}])[0]
        if box.get("minimum_temperature") is None:
            failures.append(
                f"{name}'s consumption box has no minimum_temperature "
                "-- the temperature bound is the whole contract"
            )

    failures += replace_group_failures(data)

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print(
        f"ok   {len(machines)} machines, {len(data.get('drills') or [])} drills, "
        f"{len(data.get('boilers') or [])} boilers, "
        f"{len(data.get('generators') or [])} generators, "
        f"{len(data['containers'])} fluid anchors, "
        f"every route names a declared category"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
