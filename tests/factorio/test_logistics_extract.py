#!/usr/bin/env python3
"""Assert the logistics corpus still derives the belt fork's figures (#344).

`scripts/factorio-logistics-extract.py` reads a dump that is not in the repo, so nothing here
re-runs it. Every figure the fork and its GameTest type is re-derived from the committed rows:

  - **belt rates.** A belt's `speed` is tiles per Factorio tick; at 60 ticks a second and eight
    items a tile, the four tiers carry 15, 30, 45 and 60 items a second, and each splitter and
    loader carries its tier's rate. A Minecraft block is a tile, so tier 1 moves 3/32 of a block
    per game tick.
  - **capacity.** Eight items a tile is a 1/8-block spacing, and a 64-block belt holds 512.
  - **loader energy.** Loader tiers 2 to 4 pay what the inserter their recipe is built from pays
    per item, at the pack's 100 J per FE: `inserter`, `fast-inserter` and `bulk-inserter`. A
    swing's joules come from the rule under `documented.swing`, which must reproduce the wiki's
    own table, and are divided by the hand size. Tier 1 is built from the burner inserter and
    draws no power.
  - **the GameTest's typed figures.** `BeltHandoffTests` types each tier's items a second and 512
    rather than reading them off the fork; they are asserted here against the derivation.
"""

import json
import math
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

FACTORIO_TICKS_PER_SECOND = 60
MINECRAFT_TICKS_PER_SECOND = 20
JOULES_PER_FE = 100

EXPECTED_ITEMS_PER_SECOND = {
    "transport-belt": 15, "fast-transport-belt": 30,
    "express-transport-belt": 45, "turbo-transport-belt": 60,
}
EXPECTED_BLOCKS_PER_SECOND = {
    "transport-belt": 1.875, "fast-transport-belt": 3.75,
    "express-transport-belt": 5.625, "turbo-transport-belt": 7.5,
}
LONG_BELT_BLOCKS = 64
EXPECTED_LONG_BELT_HOLDS = 512

# Loader tier -> the inserter its recipe is built from (#344).
LOADER_INSERTERS = {1: "burner-inserter", 2: "inserter", 3: "fast-inserter", 4: "bulk-inserter"}
EXPECTED_FE_PER_ITEM = {2: 66.5, 3: 81.2, 4: 116.0}
EXPECTED_DRAIN_FE_PER_SECOND = {2: 4.0, 3: 5.0, 4: 10.0}

GAMETEST = (ROOT / "mod" / "src" / "main" / "java" / "com" / "planetaryfactory" / "core"
            / "gametest" / "BeltHandoffTests.java")


def joules(raw):
    """Factorio's own `5kJ`/`0.4kW` strings, in J or W."""
    match = re.fullmatch(r"([0-9.]+)(k|M)?(J|W)", raw)
    if not match:
        raise ValueError(f"unreadable energy {raw!r}")
    return float(match[1]) * {None: 1, "k": 1e3, "M": 1e6}[match[2]]


def swing_kj(inserter, spike_ticks):
    """One swing: two half-spins of whole ticks, and an item spike at each end."""
    half_spin_ticks = math.floor(round(0.5 / inserter["rotation_speed"], 9))
    rotation = 2 * half_spin_ticks * joules(inserter["energy_per_rotation"]) * inserter["rotation_speed"]
    movement = 2 * spike_ticks * joules(inserter["energy_per_movement"]) * inserter["extension_speed"]
    return (rotation + movement) / 1e3


def typed_int(source, name):
    match = re.search(rf"\b{name}\s*=\s*(\d+)\s*;", source)
    return int(match[1]) if match else None


def main():
    path = ROOT / "data" / "factorio" / "logistics.json"
    if not path.is_file():
        sys.exit(f"missing {path} -- run scripts/factorio-logistics-extract.py")
    data = json.loads(path.read_text(encoding="utf-8"))
    failures = []
    documented = data["documented"]
    items_per_tile = documented["belt_density"]["items_per_tile"]
    if items_per_tile != 8:
        failures.append(f"a belt holds {items_per_tile} items per tile, expected 8")

    belts = {belt["name"]: belt for belt in data["belts"]}
    rates = {}
    for name, expected in EXPECTED_ITEMS_PER_SECOND.items():
        belt = belts.get(name)
        if belt is None:
            failures.append(f"{name} is not in belts")
            continue
        blocks_per_second = belt["speed"] * FACTORIO_TICKS_PER_SECOND
        if blocks_per_second != EXPECTED_BLOCKS_PER_SECOND[name]:
            failures.append(f"{name} moves {blocks_per_second} blocks/s, expected "
                            f"{EXPECTED_BLOCKS_PER_SECOND[name]}")
        rates[name] = blocks_per_second * items_per_tile
        if rates[name] != expected:
            failures.append(f"{name} carries {rates[name]} items/s, expected {expected}")

    splitter_rates = {}
    for splitter in data["splitters"]:
        belt_name = splitter["related_transport_belt"]
        if belt_name not in EXPECTED_ITEMS_PER_SECOND:
            failures.append(f"{splitter['name']} names {belt_name}, which is not a belt tier")
            continue
        rate = splitter["speed"] * FACTORIO_TICKS_PER_SECOND * items_per_tile
        splitter_rates[splitter["name"]] = rate
        if rate != EXPECTED_ITEMS_PER_SECOND[belt_name]:
            failures.append(f"{splitter['name']} carries {rate} items/s, its belt "
                            f"{EXPECTED_ITEMS_PER_SECOND[belt_name]}")
    if len(splitter_rates) != 4:
        failures.append(f"{len(splitter_rates)} splitter rates, expected 4")

    if len(data["loaders"]) != 4:
        failures.append(f"{len(data['loaders'])} loaders, expected 4")
    for loader, belt_name in zip(data["loaders"], EXPECTED_ITEMS_PER_SECOND):
        rate = loader["speed"] * FACTORIO_TICKS_PER_SECOND * items_per_tile
        if rate != EXPECTED_ITEMS_PER_SECOND[belt_name]:
            failures.append(f"{loader['name']} loads {rate} items/s, its belt tier "
                            f"{EXPECTED_ITEMS_PER_SECOND[belt_name]}")

    tier_1 = belts.get("transport-belt")
    if tier_1:
        per_game_tick = tier_1["speed"] * FACTORIO_TICKS_PER_SECOND / MINECRAFT_TICKS_PER_SECOND
        if per_game_tick != 3 / 32:
            failures.append(f"tier 1 moves {per_game_tick} blocks a game tick, expected 3/32")

    spacing = 1 / items_per_tile
    holds = math.floor(LONG_BELT_BLOCKS / spacing)
    if holds != EXPECTED_LONG_BELT_HOLDS:
        failures.append(f"a {LONG_BELT_BLOCKS}-block belt holds {holds}, expected "
                        f"{EXPECTED_LONG_BELT_HOLDS}")

    for key in ("belt_density", "hand_size", "item_spike_ticks", "swing"):
        if not documented[key].get("source", "").startswith("https://"):
            failures.append(f"documented.{key} cites no source")

    inserters = {inserter["name"]: inserter for inserter in data["inserters"]}
    spikes = documented["item_spike_ticks"]["values"]
    swing_kjs = {}
    for name, table_kj in documented["swing"]["per_cycle_kj"].items():
        derived = round(swing_kj(inserters[name], spikes[name]), 6)
        swing_kjs[name] = derived
        if derived != table_kj:
            failures.append(f"the swing rule gives {name} {derived} kJ, the wiki's table {table_kj}")

    burner = inserters[LOADER_INSERTERS[1]]
    if burner["energy_source"] != "burner" or burner["drain"] is not None:
        failures.append("the burner inserter has an electric source; tier 1 is meant to draw nothing")

    hands = documented["hand_size"]["values"]
    for tier, name in LOADER_INSERTERS.items():
        if tier == 1:
            continue
        inserter = inserters[name]
        fe_per_item = round(swing_kjs[name] * 1e3 / JOULES_PER_FE / hands[name], 6)
        if fe_per_item != EXPECTED_FE_PER_ITEM[tier]:
            failures.append(f"loader tier {tier} ({name}) derives {fe_per_item} FE per item, "
                            f"expected {EXPECTED_FE_PER_ITEM[tier]}")
        drain = round(joules(inserter["drain"]) / JOULES_PER_FE, 6)
        if drain != EXPECTED_DRAIN_FE_PER_SECOND[tier]:
            failures.append(f"loader tier {tier} ({name}) drains {drain} FE/s idle, expected "
                            f"{EXPECTED_DRAIN_FE_PER_SECOND[tier]}")

    source = GAMETEST.read_text(encoding="utf-8")
    typed_rates = [(f"TIER_{tier}_ITEMS_PER_SECOND", rates.get(belt_name))
                   for tier, belt_name in enumerate(EXPECTED_ITEMS_PER_SECOND, start=1)]
    for name, want in typed_rates + [("LONG_BELT_BLOCKS", LONG_BELT_BLOCKS),
                                     ("LONG_BELT_HOLDS", holds)]:
        typed = typed_int(source, name)
        if typed != want:
            failures.append(f"BeltHandoffTests types {name} = {typed}, the corpus derives {want}")

    for failure in failures:
        print(f"FAIL  {failure}")
    if failures:
        sys.exit(1)
    print(f"ok  belts at {sorted(rates.values())} items/s, {holds} per {LONG_BELT_BLOCKS} "
          f"blocks, loaders at {EXPECTED_FE_PER_ITEM} FE per item")


if __name__ == "__main__":
    main()
