#!/usr/bin/env python3
"""Assert both mining rigs' generated halves still agree with what registers them (#192, #193).

The rig seam (ADR-0043). `mining/drills.json` is hand-owned data (#599). Asserted here:

  - **The vertical extent.** How many blocks a rig stands is the one figure the corpus cannot
    supply: Factorio is played on a plane and a prototype states `tile_width` and `tile_height`,
    both ground extent. ADR-0043 carries the declared exception; what is asserted here is only that
    it has not silently gone back to one, which is what read as a platform rather than a machine.
  - **That the drill is obtainable and fuellable.** The item-map row has to name the block the mod
    actually registers, and something at rung 0 has to burn: a rig nothing can fuel is a rig that
    never turns, and neither half fails anywhere else. The fuel table is default-deny and category
    filtered, so "there are fuel files" is not the assertion -- "at least one names a `chemical`
    fuel against a real item" is.
  - **The facing and the port are visible.** Each part's `panel` resolves to its own front face on
    every facing, and the port is one texture on both tiers (#536). Whether every hop from
    blockstate to texture, the lang key and the loot table resolve is `test_block_assets.py`'s
    (#254).

The tier list is read out of `RigTier.java` rather than typed here, so a third rung added to the
enum is held to the same assertions.

Usage: tests/pack/test_rig_assets.py
"""

import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
RIG_TIER = ROOT / "mod/src/main/java/com/factoryworks/core/mining/rig/RigTier.java"
DRILLS = ROOT / "mod/src/main/resources/factoryworks_core/mining/drills.json"
ITEM_MAP = ROOT / "data/pack/item-map.json"
FUEL = ROOT / "kubejs/data/factoryworks/fuel"
ASSETS = ROOT / "kubejs/assets/factoryworks"

# `BURNER("burner-mining-drill", 2),` -- the enum constant, the corpus key it reads its ground size
# from, and the one number here the corpus cannot supply: how many blocks tall it stands.
TIER_RE = re.compile(r'^\s{4}([A-Z][A-Z_]*)\("([a-z-]+)",\s*(\d+)\)[,;]', re.MULTILINE)


def registered_tiers():
    tiers = TIER_RE.findall(RIG_TIER.read_text(encoding="utf-8"))
    if not tiers:
        raise AssertionError(f"no rig tiers parsed out of {RIG_TIER} -- has the enum moved?")
    return {name.lower(): (factorio, int(tall)) for name, factorio, tall in tiers}


def check_item_map(tiers, item_map, failures):
    """A decided rig must name the block the mod registers.

    `tests/factorio/test_recipe_convert.py` already asserts that a `factoryworks:` target is
    registered somewhere; what it cannot see is whether *this* rig's row names *this* rig. A row
    pointing at the other rung would resolve, emit a recipe and hand the player the wrong machine.
    An `undecided` row is left alone -- the electric rig is #194's to decide.
    """
    for tier, (factorio_name, _) in sorted(tiers.items()):
        row = item_map.get(factorio_name)
        if row is None:
            failures.append(
                f"{factorio_name} has no item-map row at all -- an unmapped corpus name is a hard "
                "failure, not a skip (#72)"
            )
            continue
        if row.get("status") == "undecided":
            continue
        expected = f"factoryworks:{tier}_mining_drill"
        if row.get("target") != expected:
            failures.append(
                f"{factorio_name} maps onto {row.get('target')!r}, but {tier} registers "
                f"{expected} -- the row names a different machine than the one it is"
            )


def check_fuel_reaches_a_burner(rows, failures):
    """A rig that burns fuel must have something to burn at rung 0.

    ADR-0047's table is default-deny and category filtered, so this cannot be read off "there are
    files in the fuel folder": a table full of `nuclear` rows would leave a burner rig unfuellable
    with every one of them present and valid. What has to hold is that a rig's own
    `fuel_categories` intersect a row that names a real item -- and rung 0 has exactly one answer,
    coal, which is why nothing here would survive the fuel converter quietly dropping it.
    """
    if not FUEL.is_dir():
        failures.append(f"{FUEL.relative_to(ROOT)} does not exist -- run the fuel converter")
        return
    table = [json.loads(path.read_text()) for path in sorted(FUEL.glob("*.json"))]
    for factorio_name, row in sorted(rows.items()):
        categories = set(row.get("fuel_categories") or [])
        if not categories:
            continue
        burnable = [
            fuel for fuel in table
            if fuel.get("fuel_category") in categories
            and (fuel.get("item") or fuel.get("tag"))
            and (fuel.get("fuel_value") or 0) > 0
        ]
        if not burnable:
            failures.append(
                f"{factorio_name} burns {sorted(categories)} and the fuel table names nothing in "
                "those categories -- the rig would never turn"
            )


JADE_PLUGIN = ROOT / "mod/src/main/java/com/factoryworks/core/compat/RigJadePlugin.java"

# `Component.translatable("tooltip.factoryworks.rig.jade.no_ore"` -- every key the HUD plugin
# asks for, read out of the plugin rather than typed here, so a line added to the tooltip without
# its string fails this check instead of shipping a raw key onto the crosshair. Deliberately any
# `factoryworks` key and not just the `rig.jade.` ones: a line that reuses the rig screen's own
# strings is exactly as unchecked, and anchoring on the infix would wave it through.
JADE_KEY_RE = re.compile(r'translatable\(\s*"([a-z_.]*factoryworks[a-z_.]+)"')


def check_jade_lang(lang, failures):
    """Every string the Jade plugin translates (#199).

    A Jade provider is not reachable from the mod's Minecraft-free test source set, so nothing on
    that side can see these keys at all; and a missing one does not fail, it renders its own key on
    the HUD of the machine whose whole point is being readable from outside.
    """
    keys = set(JADE_KEY_RE.findall(JADE_PLUGIN.read_text(encoding="utf-8")))
    if not keys:
        failures.append(
            f"no jade lang keys parsed out of {JADE_PLUGIN.relative_to(ROOT)} -- has the plugin "
            "moved, or stopped translating its lines?"
        )
    for key in sorted(keys):
        if not lang.get(key):
            failures.append(f"{key} has no lang entry -- the rig's HUD would show its raw key")


def check_screen_lang(lang, failures):
    """The rig screen's own strings. A missing one ships a raw translation key on the hover."""
    for key in ("tooltip.factoryworks.rig.fuel",
                "tooltip.factoryworks.rig.fuel.seconds",
                "tooltip.factoryworks.rig.fuel.out"):
        if not lang.get(key):
            failures.append(f"{key} has no lang entry -- the fuel hover would show its raw key")


PORT_TEXTURE = "factoryworks:block/mining_drill_port"
PANELS = ("casing", "front", "port")
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}


def front_of(model):
    declared = json.loads((ASSETS / f"models/{model.split(':', 1)[-1]}.json").read_text())
    return (declared.get("textures") or {}).get("front")


def check_panels(tier, failures):
    """The front face each block wears (#536).

    Which block of the footprint wears which panel is `RigPanelsTest`'s. What is held here is that
    each panel resolves to the face it names on every facing: the port only on `panel=port`, the same
    texture on both tiers, and the front lit only while `lit=true`.
    """
    block = f"{tier}_mining_drill"
    textures = f"factoryworks:block/{block}"
    def expected(panel, lit):
        if panel == "front":
            return f"{textures}/front_{'on' if lit else 'off'}"
        return {"casing": f"{textures}/side", "port": PORT_TEXTURE}[panel]

    anchor = json.loads((ASSETS / f"blockstates/{block}.json").read_text()).get("variants") or {}
    for key, entry in anchor.items():
        if front_of(entry["model"]) != expected("casing", False):
            failures.append(
                f"{block}'s anchor at {key} wears {front_of(entry['model'])!r} -- the anchor is the "
                "back corner and never the port or the front"
            )
    if {entry.get("y", 0) for entry in anchor.values()} != set(FACINGS.values()):
        failures.append(f"{block}'s blockstate does not turn through all four quarters")

    part = json.loads((ASSETS / f"blockstates/{block}_part.json").read_text()).get("variants") or {}
    for facing, y in FACINGS.items():
        for panel in PANELS:
            for lit in ("false", "true"):
                key = f"facing={facing},lit={lit},panel={panel}"
                entry = part.get(key)
                if entry is None:
                    failures.append(f"{block}_part has no variant {key}")
                    continue
                if entry.get("y", 0) != y:
                    failures.append(f"{block}_part's {key} turns {entry.get('y', 0)}, not {y}")
                want = expected(panel, lit == "true")
                if front_of(entry["model"]) != want:
                    failures.append(
                        f"{block}_part's {key} wears {front_of(entry['model'])!r} on its front, "
                        f"not {want!r}"
                    )
    stray = set(part) - {
        f"facing={f},lit={lit},panel={p}" for f in FACINGS for p in PANELS for lit in ("false", "true")
    }
    if stray:
        failures.append(f"{block}_part names variants no state has: {sorted(stray)}")


def main():
    failures = []
    tiers = registered_tiers()
    rows = json.loads(DRILLS.read_text())["drills"]
    item_map = json.loads(ITEM_MAP.read_text())["items"]
    lang = json.loads((ASSETS / "lang/en_us.json").read_text())

    for tier, (factorio_name, blocks_tall) in sorted(tiers.items()):
        # The vertical extent is chosen, not extracted -- Factorio states two ground figures and
        # no third -- so it is asserted here rather than against the corpus. A rig one block tall
        # is the thing that read as a platform rather than as a machine, and nothing upstream of
        # this file can catch a silent return to it. ADR-0043 carries the declared exception.
        if blocks_tall < 2:
            failures.append(
                f"{tier} stands {blocks_tall} block tall -- a one-block rig reads as a platform; "
                "see ADR-0043's declared exception"
            )
        if factorio_name not in rows:
            failures.append(f"{factorio_name} has no row in drills.json")

        check_panels(tier, failures)

    stray = set(rows) - {factorio for factorio, _ in tiers.values()}
    if stray:
        failures.append(
            f"drills.json carries {sorted(stray)}, which no registered tier reads -- "
            "a tier removed from the enum leaves its row behind"
        )

    check_item_map(tiers, item_map, failures)
    check_fuel_reaches_a_burner(rows, failures)
    check_screen_lang(lang, failures)
    check_jade_lang(lang, failures)

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print(f"ok   {len(tiers)} rigs, every panel wears its face")
    return 0


if __name__ == "__main__":
    sys.exit(main())
