#!/usr/bin/env python3
"""Assert the Offshore Pump's generated halves still agree with what registers it (#213, ADR-0050).

The one block that is the origin of every drop of water in the factory. `fluid/pumps.json` is
hand-owned data (#599). Three things are asserted:

  - **The rate.** The resource states `pumping_speed` 20 and a `void` energy source (ADR-0050): one
    pump feeds twenty boilers, and the pump takes no power. A change is a design change.
  - **The refusal message.** ADR-0050 requires placement to be refused *with a message* -- a pump
    that places and then silently produces nothing reaches the player as a dead factory three
    machines later. A missing lang key does not fail: it renders the raw key. So the key the item
    actually asks for is read out of the item's source rather than typed here.

Whether its blockstate, models, textures, lang key and loot table resolve is
`test_block_assets.py`'s (#254).

Usage: tests/pack/test_pump_assets.py
"""

import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
PUMPS = ROOT / "mod/src/main/resources/factoryworks_core/fluid/pumps.json"
ITEM_MAP = ROOT / "data/pack/item-map.json"
PUMP_ITEM = ROOT / "mod/src/main/java/com/factoryworks/core/fluid/OffshorePumpItem.java"
ASSETS = ROOT / "kubejs/assets/factoryworks"

FACTORIO_NAME = "offshore-pump"
BLOCK_NAME = "offshore_pump"
BLOCK_ID = f"factoryworks:{BLOCK_NAME}"

# ADR-0050's figure.
EXPECTED_PUMPING_SPEED = 20

# `Component.translatable(NO_SOURCE_KEY)` resolves to whatever the constant holds -- so the constant
# is what gets read, not the call.
REFUSAL_KEY_RE = re.compile(r'[A-Z_]+_KEY\s*=\s*"([a-z_.]+)"')
# One refusal: no pumpable source. Counted so that a second refusal added to the item without a
# lang entry, or the one there silently renamed, fails here.
REFUSAL_KEY_COUNT = 1


def check_resource(failures):
    if not PUMPS.is_file():
        failures.append(f"{PUMPS.relative_to(ROOT)} is missing")
        return
    row = json.loads(PUMPS.read_text()).get(FACTORIO_NAME)
    if row is None:
        failures.append(f"pumps.json carries no {FACTORIO_NAME} row")
        return
    if row.get("pumping_speed") != EXPECTED_PUMPING_SPEED:
        failures.append(
            f"pumping_speed is {row.get('pumping_speed')!r}, not {EXPECTED_PUMPING_SPEED} -- "
            "ADR-0050's 'one pump feeds twenty boilers' was written against the latter"
        )
    if row.get("energy_source") != "void":
        failures.append(
            f"{FACTORIO_NAME}'s energy_source is {row.get('energy_source')!r}, not 'void' -- the "
            "pump is powerless by ADR-0050"
        )


def check_item_map(failures):
    """The pump's row must name the block the mod registers -- and only now that it exists.

    ADR-0050 is explicit that the row flips from `undecided` when the block lands and not before:
    an item-map target for a block that does not exist emits a recipe naming nothing.
    """
    row = json.loads(ITEM_MAP.read_text())["items"].get(FACTORIO_NAME)
    if row is None:
        failures.append(f"{FACTORIO_NAME} has no item-map row at all -- an unmapped corpus name is "
                        "a hard failure, not a skip (#72)")
        return
    if row.get("status") == "undecided":
        failures.append(
            f"{FACTORIO_NAME} is still `undecided` in the item map, but the block is registered -- "
            "the pump would have no recipe and be unobtainable"
        )
        return
    if row.get("target") != BLOCK_ID:
        failures.append(
            f"{FACTORIO_NAME} maps onto {row.get('target')!r}, not {BLOCK_ID} -- the row names "
            "something other than the block it is"
        )


def check_refusal_message(lang, failures):
    """The strings the item asks for when it refuses a placement."""
    source = PUMP_ITEM.read_text(encoding="utf-8")
    keys = REFUSAL_KEY_RE.findall(source)
    if len(keys) != REFUSAL_KEY_COUNT:
        failures.append(
            f"{len(keys)} refusal keys parsed out of {PUMP_ITEM.relative_to(ROOT)}, expected "
            f"{REFUSAL_KEY_COUNT} (no source) -- has a constant moved, or has a "
            "refusal stopped saying anything?"
        )
    for key in keys:
        if not lang.get(key):
            failures.append(
                f"{key} has no lang entry -- a refused placement would print its own raw key, "
                "which is worse than the silent failure ADR-0050 wrote the message against"
            )


def main():
    failures = []
    lang = json.loads((ASSETS / "lang/en_us.json").read_text())

    check_resource(failures)
    check_item_map(failures)
    check_refusal_message(lang, failures)

    if failures:
        print(f"FAIL {len(failures)}:")
        for failure in failures:
            print(f"  - {failure}")
        return 1
    print("ok   offshore pump: resource states ADR-0050's rate, item map names the block, refusal message "
          "resolves")
    return 0


if __name__ == "__main__":
    sys.exit(main())
