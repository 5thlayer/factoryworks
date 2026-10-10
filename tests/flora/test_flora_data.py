#!/usr/bin/env python3
"""Check that Sapros's flora and surface data hang together, without launching Minecraft.

Every failure this covers is one the game reports late, quietly, or not at all: a tree
feature naming a block nobody registers places air; a loot table naming an item nobody
registers drops nothing; a blockstate file missing a variant is a purple-black canopy the
first time a leaf fruits; a stromatolite that drops ore instead of bacteria quietly deletes the
Decay chain the body exists to carry (ADR-0016). All of them are hours away from the edit that caused them, and
all of them are a string comparison here.

What it deliberately does not check is behaviour -- refruiting, growth, whether Create's saw
fells the tree. That needs the game, and it is the launch test's job.

Usage: tests/flora/test_flora_data.py
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
DATA = ROOT / "kubejs/data/factoryworks"
ASSETS = ROOT / "kubejs/assets/factoryworks"
# Sapros's worldgen is parked with every body but Terra (ADR-0060) and still checked there, so it
# cannot rot while it waits; the live tree wins once #23 brings it back.
WORLDGEN = next(d for d in (DATA / "worldgen", ROOT / "kubejs/parked/data/factoryworks/worldgen")
                if (d / "configured_feature/yumako_tree.json").is_file())

# The saplings the deleted Core mod registered, as of fb05f50, which the pack still names until
# the Showcase moves out (#663, ADR-0128).
CORE_SAPLINGS = frozenset({"factoryworks:yumako_sapling", "factoryworks:jellystem_sapling"})
KUBEJS_BLOCKS = ROOT / "kubejs/startup_scripts/blocks.js"
KUBEJS_ITEMS = ROOT / "kubejs/startup_scripts/items.js"

failures = []


def check(condition, message):
    if not condition:
        failures.append(message)
    print(("ok   " if condition else "FAIL ") + message)


def kubejs_ids(path):
    ids = set(re.findall(r"event\.create\('(factoryworks:[a-z_]+)'", path.read_text()))
    # A pattern that stops matching is the one failure this helper cannot report by comparing
    # sets: an empty set agrees with everything. a9a965d's find-and-replace rewrote this regex
    # to `event.simplebelts(` and the check went on passing, so the emptiness is asserted here.
    assert ids, f"{path.name} registers nothing -- has the KubeJS registration call been renamed?"
    return ids


def json_strings(node):
    """Every string anywhere in a JSON document, so a check cannot miss a nesting level."""
    if isinstance(node, str):
        yield node
    elif isinstance(node, dict):
        for value in node.values():
            yield from json_strings(value)
    elif isinstance(node, list):
        for value in node:
            yield from json_strings(value)


def nested_uniform_providers(node):
    """A uniform IntProvider is flat -- {"type","min_inclusive","max_inclusive"}. Wrapping the
    bounds in a "value" object parses nowhere and fails the whole registry load, taking world
    creation with it, so every worldgen file is swept rather than the ones we remember."""
    if isinstance(node, dict):
        if (str(node.get("type", "")).endswith("uniform")
                and isinstance(node.get("value"), dict)
                and "min_inclusive" in node["value"]):
            yield node
        for value in node.values():
            yield from nested_uniform_providers(value)
    elif isinstance(node, list):
        for value in node:
            yield from nested_uniform_providers(value)


def main():
    blocks = CORE_SAPLINGS | kubejs_ids(KUBEJS_BLOCKS)
    items = kubejs_ids(KUBEJS_ITEMS)
    # A block item exists for every block, so a loot table may name either.
    registered = blocks | items

    check(not (CORE_SAPLINGS & kubejs_ids(KUBEJS_BLOCKS)),
          "no sapling is registered by KubeJS too")
    check({"factoryworks:yumako_fresh", "factoryworks:jellynut_fresh"} <= items,
          "the two harvested materials are registered, as Fresh")
    check({"factoryworks:iron_bacteria_fresh",
           "factoryworks:copper_bacteria_fresh"} <= items,
          "both ore bacteria are registered, as Fresh")
    check(all(not i.endswith(("_ripe", "_stale", "_spoiling")) for i in items),
          "no Decay stage beyond Fresh ships here")
    # Jelly is what a Biochamber makes from Jellynut, and it belongs to `Puzzle: Sapros`.
    # Registering it here under any name would settle a design decision this ticket does not own.
    check(not any(re.match(r"factoryworks:jelly(_|$)", i) for i in registered),
          "no id registered here is called Jelly")

    for tree in ("yumako", "jellystem"):
        feature = WORLDGEN / f"configured_feature/{tree}_tree.json"
        check(feature.is_file(), f"{tree} has a configured feature")
        referenced = {s for s in json_strings(json.loads(feature.read_text()))
                      if s.startswith("factoryworks:")}
        unknown = referenced - blocks
        check(not unknown, f"{tree}'s feature names only registered blocks (stray: {unknown})")

        placed = WORLDGEN / f"placed_feature/{tree}_tree.json"
        check(placed.is_file(), f"{tree} has a placed feature")
        check(json.loads(placed.read_text())["feature"] == f"factoryworks:{tree}_tree",
              f"{tree}'s placed feature points at its configured feature")

    # Only this file's own blocks. `loot_table/blocks/` is shared with every other pack-authored
    # block, and `registered` above is built from the two sources flora is registered through --
    # the mod's saplings and KubeJS -- so a block registered in Java anywhere else reads as a
    # stray item here. It is not: the furnaces and machines have loot tables of their own,
    # asserted by their own checks. Each
    # family checks its own drops against its own registry, and sweeping the whole directory from
    # here only lets this check fail for another family's content.
    foreign = []
    for table in sorted((DATA / "loot_table/blocks").glob("*.json")):
        if f"factoryworks:{table.stem}" not in blocks:
            foreign.append(table.stem)
            continue
        referenced = {s for s in json_strings(json.loads(table.read_text()))
                      if s.startswith("factoryworks:")}
        unknown = referenced - registered
        check(not unknown, f"{table.name} drops only registered items (stray: {unknown})")
    # Said out loud rather than skipped in silence, so a flora block that stops being registered
    # leaves its table sitting in this list instead of quietly dropping out of the sweep.
    print(f"     ({len(foreign)} table(s) belong to other families, checked elsewhere: "
          f"{', '.join(foreign) if foreign else 'none'})")

    # Both trees are felled to harvest them, so both materials come off loot tables -- but off
    # different blocks. Yumako out of the canopy, Jellynut out of the trunk. The check is that
    # neither material can be had from the other tree's block, which is what would collapse the
    # two trees into one with two textures.
    leaves = json.loads((DATA / "loot_table/blocks/yumako_leaves.json").read_text())
    leaf_drops = set(json_strings(leaves)) & registered
    check(leaf_drops == {"factoryworks:yumako_sapling", "factoryworks:yumako_fresh"},
          "yumako leaves yield Yumako and the sapling to replant with")
    stem = json.loads((DATA / "loot_table/blocks/jellystem_stem.json").read_text())
    stem_drops = set(json_strings(stem)) & registered
    check(stem_drops == {"factoryworks:jellynut_fresh"},
          "a jellystem stem yields Jellynut and not itself")

    # A harvest-once tree has no state to track, so the leaves are a plain block again: no
    # property, no random tick, no hand-written blockstate file. If any of that comes back,
    # it brings a standing-crop mechanic back with it.
    blocks_js = KUBEJS_BLOCKS.read_text()
    check("property(" not in blocks_js and "randomTick(" not in blocks_js,
          "no flora block carries a blockstate property or a random tick")
    check(not (ROOT / "kubejs/server_scripts/sapros_flora.js").exists(),
          "no script picks fruit off a standing tree")
    check(not (ASSETS / "blockstates/yumako_leaves.json").exists(),
          "yumako leaves use KubeJS's generated blockstate, having only one state")

    for texture in ("block/yumako_log", "block/yumako_leaves",
                    "block/jellystem_stem", "block/jellystem_leaves",
                    "block/yumako_sapling", "block/jellystem_sapling",
                    "item/yumako", "item/jellynut"):
        check((ASSETS / f"textures/{texture}.png").is_file(), f"texture {texture}.png exists")

    for texture in ("block/iron_stromatolite", "block/copper_stromatolite",
                    "item/iron_bacteria", "item/copper_bacteria"):
        check((ASSETS / f"textures/{texture}.png").is_file(), f"texture {texture}.png exists")

    # Sapros's terrain, checked here for the same reason the trees are: every one of these
    # fails silently in-game, hours after the edit.
    #
    # A stromatolite yields bacteria and stone. If one ever yields ore instead, the Decay
    # chain the whole body exists to carry becomes optional, and nothing crashes to say so.
    for metal in ("iron", "copper"):
        table = json.loads(
            (DATA / f"loot_table/blocks/{metal}_stromatolite.json").read_text())
        drops = set(json_strings(table))
        check(f"factoryworks:{metal}_bacteria_fresh" in drops,
              f"a {metal} stromatolite yields {metal} bacteria")
        check(not any(d.endswith("_ore") or "/ores" in d or ":ore" in d for d in drops),
              f"no {metal} stromatolite drop is an ore")
        check(any(d.startswith("gcyr:") for d in drops),
              f"a {metal} stromatolite also yields stone")

    # The two marshlands are two destinations, and they stop being that the moment both
    # trees grow in one of them.
    marshlands = {"green": "yumako", "red": "jellystem"}
    for colour, tree in marshlands.items():
        biome = json.loads(
            (WORLDGEN / f"biome/gleba_{colour}_marshland.json").read_text())
        carried = set(json_strings(biome))
        other = marshlands["red" if colour == "green" else "green"]
        check(f"factoryworks:{tree}_tree" in carried,
              f"the {colour} marshland carries {tree}")
        check(f"factoryworks:{other}_tree" not in carried,
              f"the {colour} marshland does not carry {other}")
        for metal in ("iron", "copper"):
            check(f"factoryworks:sapros_{metal}_stromatolite" in carried,
                  f"the {colour} marshland carries {metal} stromatolites")

    for biome_name in ("gleba_dark_highlands", "gleba_midlands", "gleba_marshes"):
        carried = set(json_strings(json.loads(
            (WORLDGEN / f"biome/{biome_name}.json").read_text())))
        check(not any(c.endswith("_tree") for c in carried),
              f"{biome_name} carries neither tree")

    for path in sorted({*(DATA / "worldgen").rglob("*.json"), *WORLDGEN.rglob("*.json")}):
        check(not list(nested_uniform_providers(json.loads(path.read_text()))),
              f"{path.relative_to(ROOT)} states its uniform providers flat")

    lang = json.loads((ASSETS / "lang/en_us.json").read_text())
    for biome_name in ("gleba_dark_highlands", "gleba_midlands", "gleba_marshes",
                       "gleba_green_marshland", "gleba_red_marshland"):
        check(f"biome.factoryworks.{biome_name}" in lang,
              f"{biome_name} has a display name")
    for sapling in CORE_SAPLINGS:
        key = "block.factoryworks." + sapling.split(":")[1]
        check(key in lang, f"the mod's {sapling} has a lang entry (the pack names it, not the jar)")

    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
