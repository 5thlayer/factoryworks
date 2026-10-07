#!/usr/bin/env python3
"""Assert the conversion's data files and its emitted recipes still say what the decisions say.

This is #87's static half -- the "cross-file references resolve" check in
`docs/testing/what-to-check.md`'s terms. It launches no game, so what it can prove is that the
files agree with each other:

  - `item-map.json` covers every item and fluid the corpus names, and nothing else. A missing row
    is the converter's one hard failure (#72); an extra row is a name that left the corpus at a
    regeneration and took its decision with it
  - every row is one of the three legible kinds: a target, an `undecided` with a reason, or a
    `native_mechanic` (#93). Never a bare name
  - every target names a namespace the pack actually ships, and every first-party target is
    registered in `kubejs/startup_scripts/`. A row pointing at an item nobody registers is the
    failure that reaches the player as a recipe missing from JEI
  - every override carries a `reason` and names a recipe in the corpus (ADR-0031) -- the converter
    does not repeat this check, so this is the only place it is made
  - the emitted JSON is exactly what the converter emits today. Generated output is never
    hand-edited (ADR-0026), and this is the assertion that says so
  - every emitted recipe's ingredients and results resolve through the item map, and its `type`
    is a recipe type this pack registers

WHAT IT CANNOT PROVE is that the recipe SHAPE is right: `AssemblingRecipe`'s codec is Java, and a
wrong shape is one ERROR line at datapack load and a recipe absent from the manager. That is
`scripts/check-datapack-load.py`, which loads every emitted recipe on a GameTest server (#279).

Usage: tests/factorio/test_recipe_convert.py
"""
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
EMITTED = ROOT / "kubejs/data/factoryworks/recipe"
CONVERTER = ROOT / "scripts/factorio-recipe-convert.py"
# Subtrees of EMITTED the converter does not write, each held by a check of its own. Read out of
# the converter rather than restated, so the two can never disagree about what it owns.
FOREIGN_SUBTREES = tuple(re.findall(r'"([^"]+)"', re.search(
    r"^FOREIGN_SUBTREES = \((.*)\)$", CONVERTER.read_text(encoding="utf-8"), re.MULTILINE).group(1)))
STARTUP = ROOT / "kubejs/startup_scripts"
MOD = ROOT / "mod/src/main/java/com/factoryworks/core"
PF_BLOCKS = MOD / "PFBlocks.java"
PF_ITEMS = MOD / "PFItems.java"
FURNACE_TIER = MOD / "smelting/FurnaceTier.java"
RIG_TIER = ROOT / "mod/src/main/java/com/factoryworks/core/mining/rig/RigTier.java"
CHEST_TIER = ROOT / "mod/src/main/java/com/factoryworks/core/chest/ChestTier.java"

# Factorio's three assembling categories are Craftworks' Assembling recipes (ADR-0118).
CRAFTWORKS_ASSEMBLING = "craftworks:assembling"

# The Personal Assembler plans a recipe whose category is `crafting`.
HAND_CATEGORY = "crafting"

# The pack's own smelting type (#155). Its ingredient carries a count, which vanilla's cannot,
# and it is the only type the three furnace tiers read.
PACK_SMELTING = "factoryworks:smelting"

# The namespaces a LIVE item-map target may live in. Whether a target actually resolves against the
# installed jars is `tests/pack/test_item_map.py`'s question; this is the coarser one of whether the
# row names a mod
# the pack ships at all. A row naming a mod ADR-0060 removed passes only while it is `blocked_by`
# the ticket that re-targets it (#277, #260), and no emitted recipe may name one.
NAMESPACES = {"minecraft", "factoryworks",
              # `c:` is the common tag namespace, which belongs to no mod.
              "c",
              # Researchd owns the research-pack item; `factory_works:` (an underscore) is the
              # id space its packs are declared in, and is not this pack's item namespace.
              "researchd", "factory_works",
              # Oritech is the pack's tech mod (ADR-0060): its engine (#282), pipes, tanks and fluids.
              "oritech",
              # Beltworks' belts, ADR-0060's logistics (#277).
              "beltworks",
              # Craftworks' Assemblers and its recipe type (ADR-0118).
              "craftworks",
              # Wireworks' electric poles, a Library carved out of the pack (#476).
              "wireworks",
              # Pipeworks' pipe and storage tank, the fluid Library (#557, ADR-0110).
              "pipeworks"}


def mod_registered_blocks():
    """The `factoryworks:` blocks `factoryworks_core` registers, not KubeJS.

    ADR-0015 splits registration by what a thing is: content goes to KubeJS, mechanism to the mod.
    Both land in the same namespace, so an item-map row cannot tell which side registered its
    target -- and until the supply-area poles (#147) nothing on the mod's side had a row at all.
    Reading only the startup scripts would now report four registered blocks as unregistered, and
    the natural "fix" for that is to weaken the check, which is the one thing it must not do.

    The furnaces and the mining rigs derive their ids from their tier enums, so they are
    read the same way rather than typed out: a fourth furnace or a third rung of the drill ladder is
    then registered here without this file being edited.

    The rig parts are deliberately absent. A part has no `BlockItem` -- it is placed only by the
    anchor's own item and never held -- so a row naming one would be a row naming something a
    player cannot have.
    """
    blocks = set(re.findall(r'BLOCKS\.register(?:Block)?\("([a-z0-9_]+)"',
                            (PF_BLOCKS).read_text(encoding="utf-8")))
    furnaces = re.findall(r"^\s{4}([A-Z][A-Z_]*)\([^)]*\)[,;]",
                          FURNACE_TIER.read_text(encoding="utf-8"), re.MULTILINE)
    blocks |= {f"{tier.lower()}_furnace" for tier in furnaces}
    rigs = re.findall(r"^\s{4}([A-Z][A-Z_]*)\([^)]*\)[,;]",
                      RIG_TIER.read_text(encoding="utf-8"), re.MULTILINE)
    blocks |= {f"{tier.lower()}_mining_drill" for tier in rigs}
    blocks |= set(re.findall(r'^\s{4}[A-Z]+\("([a-z0-9_]+)"',
                             CHEST_TIER.read_text(encoding="utf-8"), re.MULTILINE))
    return {f"factoryworks:{name}" for name in blocks}


def mod_registered_items():
    """The `factoryworks:` items `factoryworks_core` registers with no block behind them.

    Everything the mod registered used to be a block, so reading `PFBlocks` and the tier enums
    covered it. The barrel (ADR-0037) is the first item that is only an item: it carries a fluid capability,
    which is mechanism and therefore the mod's under ADR-0015, and it has nothing to place. Without
    this its row would read as unregistered while sitting in `PFItems` -- and the natural "fix" for
    that is to weaken the check, which is the one thing it must not do.
    """
    return {f"factoryworks:{name}" for name in re.findall(
        r'ITEMS\.register(?:SimpleItem|Item)?\(\s*"([a-z0-9_]+)"', PF_ITEMS.read_text(encoding="utf-8"))}


def first_party_items():
    """The `factoryworks:` items and machines the pack actually registers.

    Both halves of ADR-0015's split: the KubeJS startup scripts and `factoryworks_core`.
    """
    items = set(re.findall(r"event\.create\('(factoryworks:[a-z0-9_]+)'",
                           (STARTUP / "items.js").read_text()))
    # A block registers an item too, so its rows would otherwise read as unregistered.
    items |= set(re.findall(r"event\.create\('(factoryworks:[a-z0-9_]+)'",
                            (STARTUP / "blocks.js").read_text()))
    # Same guard as `kubejs_ids` in the flora check, and for the same reason: an empty
    # first-party set makes every "is this item registered" assertion below pass vacuously,
    # which is exactly what a9a965d's rename of `event.create(` produced.
    assert items, "the KubeJS startup scripts register nothing -- has `event.create(` been renamed?"
    items |= mod_registered_blocks()
    items |= mod_registered_items()
    return items


def check_item_map(items, corpus, failures):
    referenced = {e["name"] for r in corpus for e in r["ingredients"] + r["results"]}
    for name in sorted(referenced - set(items)):
        failures.append(f"item map has no row for {name} -- a hard failure, not a skip (#72)")
    outside = {name for name, row in items.items() if "outside_corpus" in row}
    for name in sorted(set(items) - referenced - outside):
        failures.append(f"item map row {name} names nothing in the corpus")
    for name in sorted(outside & referenced):
        failures.append(f"item map row {name} is marked outside_corpus, and the corpus names it")

    registered = first_party_items()
    for name, row in sorted(items.items()):
        status = row.get("status")
        if status in ("undecided", "native_mechanic", "not_emitted", "blocked"):
            if not row.get("note"):
                failures.append(f"{name} is {status} with no note saying what decides it")
            # AN UNDECIDED ROW MUST NAME THE TICKET THAT DECIDES IT. Without this, a row can sit
            # `undecided` with a note pointing at prose -- an ADR, a closed ticket, another row --
            # and nothing ever comes back to it. #87 found 40 such rows, a quarter of the map:
            # eight of them cited an ADR that had already been accepted and had decided them.
            # `native_mechanic` and `not_emitted` are terminal and need no ticket; `undecided` is
            # a promise that someone will decide, and a promise needs an owner.
            if status == "undecided" and not isinstance(row.get("ticket"), int):
                failures.append(f"{name} is undecided and names no ticket -- say who decides it, "
                                "or the row is a decision nobody is coming back to")
            continue
        if status is not None:
            failures.append(f"{name} has unknown status {status!r}")
            continue
        for field in ("kind", "target", "source", "note"):
            if not row.get(field):
                failures.append(f"{name} has no {field}")
        if row.get("kind") not in ("item", "tag", "fluid"):
            failures.append(f"{name} has kind {row.get('kind')!r}")
        if row.get("source") not in ("borrowed", "authored"):
            failures.append(f"{name} has source {row.get('source')!r} -- ADR-0031's rule has two")
        target = row.get("target", "")
        namespace = target.split(":", 1)[0]
        if namespace not in NAMESPACES and "blocked_by" not in row:
            failures.append(f"{name} maps onto {target}, whose namespace the pack does not ship")
        for component, value in (row.get("components") or {}).items():
            for id_ in (component, value):
                if id_.split(":", 1)[0] not in NAMESPACES and "blocked_by" not in row:
                    failures.append(f"{name} names {id_}, whose namespace the pack does not ship")
        if "blocked_by" in row:
            if not isinstance(row["blocked_by"], int):
                failures.append(f"{name} has blocked_by {row['blocked_by']!r}, not a ticket number")
        if row.get("source") == "authored" and namespace != "factoryworks":
            failures.append(f"{name} is authored but maps onto {target}")
        if namespace == "factoryworks" and row.get("kind") == "item" \
                and target not in registered and "blocked_by" not in row:
            failures.append(f"{name} maps onto {target}, which no startup script registers")
        # `blocked_by` is the one escape, and it is narrow: a row whose item is DECIDED but is
        # `factoryworks_core`'s to register, naming the ticket that builds it. KubeJS cannot
        # register a furnace with a fuel slot or a chunk-charting block, so without this the map
        # could not record a decision the mod has not caught up with -- and the alternative,
        # leaving the row `undecided`, would say nobody had decided rather than nobody had built.
        if "blocked_by" in row and namespace == "factoryworks" and target in registered:
            failures.append(f"{name} is blocked_by #{row['blocked_by']} and is already "
                            "registered -- drop the field, the ticket landed")


def check_overrides(overrides, corpus, failures):
    names = {r["name"] for r in corpus}
    for name, override in sorted(overrides.items()):
        if not override.get("reason"):
            failures.append(f"override {name} has no reason (ADR-0031)")
        if name not in names:
            failures.append(f"override {name} names no recipe in the corpus")


# Craftworks' Fill Recipe refuses a recipe with more distinct ingredients than its `AssemblerSlots.INPUTS`.
CRAFTWORKS_ASSEMBLER_INPUTS = 5

# What Craftworks' Assembler holds of a recipe besides its inputs: one product slot, and two input and
# three output fluid boxes (`FluidLayout.ASSEMBLER`).
CRAFTWORKS_ASSEMBLER_ITEM_OUTPUTS = 1
CRAFTWORKS_ASSEMBLER_FLUID_INPUTS = 2
CRAFTWORKS_ASSEMBLER_FLUID_OUTPUTS = 3


def input_slots():
    """The most input slots any machine of each recipe type has, which is Craftworks' by its constant."""
    return {CRAFTWORKS_ASSEMBLING: CRAFTWORKS_ASSEMBLER_INPUTS}


def check_emitted(items, recipe_types, failures):
    """Every emitted recipe resolves through the item map and onto a recipe type that exists.

    Two shapes reach this directory: `AssemblingRecipe`'s, under Craftworks' type or the pack's
    own (#279, ADR-0118), and the pack's furnace type (#155).
    """
    targets = {row["target"] for row in items.values() if "target" in row}
    # A `blocked_by` row is decided but its item is not registered yet -- it arrives with a
    # `factoryworks_core` ticket. A recipe naming one passes every assertion above and then
    # fails at WORLD LOAD, where KubeJS cannot resolve the item: an error in a log, a recipe
    # nothing can craft, and nothing pointing back at the item map. The converter skips these
    # rows; this is the assertion that it is still doing so.
    awaited = {row["target"]: row["blocked_by"] for row in items.values() if "blocked_by" in row}

    def resolves(path, field, target):
        if target not in targets:
            failures.append(f"{path.name}: {field} names {target}, which no item-map row gives")
        elif target in awaited:
            failures.append(f"{path.name}: {field} names {target}, which is blocked_by "
                            f"#{awaited[target]} and is not registered yet -- re-run the converter")

    # Neither foreign subtree is this converter's output.
    #
    # `recipe/pack/` is ADR-0039's exception to ADR-0031: the two Engineer's Pick recipes are
    # hand-written because Factorio has no mining-tool prototype for the corpus to author, so their
    # items have no Factorio name and no item-map row either, by construction.
    # `tests/factorio/test_pack_recipes.py` holds that subtree to its own rules.
    for path in sorted(EMITTED.rglob("*.json")):
        if any(path.relative_to(EMITTED).as_posix().startswith(s + "/")
               for s in FOREIGN_SUBTREES):
            continue
        recipe = json.loads(path.read_text())
        if recipe.get("type") == PACK_SMELTING:
            named = [recipe["ingredient"].removeprefix("#"), recipe["result"]["id"]]
            for target in named:
                resolves(path, "the recipe", target)
            if not isinstance(recipe.get("cookingtime"), int) or recipe["cookingtime"] <= 0:
                failures.append(f"{path.name} has cookingtime {recipe.get('cookingtime')!r}")
            # THE COUNT IS WHY THIS TYPE EXISTS. A smelt that lost it would be a working recipe
            # asking for one item instead of five -- wrong output, no error and no log line, which
            # is the failure #155 registered a type of its own to make impossible.
            if not isinstance(recipe.get("count"), int) or recipe["count"] <= 0:
                failures.append(f"{path.name} has count {recipe.get('count')!r}")
            continue
        if recipe.get("type") not in recipe_types:
            failures.append(f"{path.name} has type {recipe.get('type')!r}, which is not registered")
        if not isinstance(recipe.get("time"), int) or recipe["time"] <= 0:
            failures.append(f"{path.name} has time {recipe.get('time')!r}")
        if not recipe.get("category"):
            failures.append(f"{path.name} carries no Factorio category, so no hand set can read it")
        if recipe.get("type") == CRAFTWORKS_ASSEMBLING:
            # The Personal Assembler plans a hand-craftable recipe of one result and no fluid, so a
            # flag that disagrees with the category either hides a hand recipe or plans a machine one.
            if recipe.get("hand_craftable") != (recipe.get("category") == HAND_CATEGORY):
                failures.append(f"{path.name} is category {recipe.get('category')!r} with hand_craftable "
                                f"{recipe.get('hand_craftable')!r}")
            if not recipe.get("results") and not recipe.get("fluid_results"):
                failures.append(f"{path.name} makes nothing, which Craftworks refuses at load")
            # Both keys are required by Craftworks' codec, though either list may be empty.
            for key in ("ingredients", "results"):
                if key not in recipe:
                    failures.append(f"{path.name} has no `{key}` key, so Craftworks drops it at load")
            limits = (("results", CRAFTWORKS_ASSEMBLER_ITEM_OUTPUTS),
                      ("fluid_ingredients", CRAFTWORKS_ASSEMBLER_FLUID_INPUTS),
                      ("fluid_results", CRAFTWORKS_ASSEMBLER_FLUID_OUTPUTS))
            for key, most in limits:
                if len(recipe.get(key, [])) > most:
                    failures.append(f"{path.name} has {len(recipe[key])} `{key}` and Craftworks' Assembler "
                                    f"holds {most}")
        # One ingredient per input slot, so one past the last could never be inserted (ADR-0074).
        slots = input_slots().get(recipe.get("type"), 0)
        if len(recipe.get("ingredients", [])) > slots:
            failures.append(f"{path.name} has {len(recipe['ingredients'])} item ingredients and no "
                            f"{recipe.get('type')} machine has more than {slots} input slots")
        for field in ("ingredients", "fluid_ingredients"):
            for entry in recipe.get(field, []):
                ingredient = entry["ingredient"]
                target = ingredient["items"] if isinstance(ingredient, dict) \
                    else ingredient.removeprefix("#")
                resolves(path, field, target)
                if not isinstance(entry.get("count" if field == "ingredients" else "amount"), int):
                    failures.append(f"{path.name}: {field} entry {entry} has no count")
        for field in ("results", "fluid_results"):
            for entry in recipe.get(field, []):
                resolves(path, field, entry["id"])
                if str(entry["id"]).startswith("#"):
                    failures.append(f"{path.name} has a tag as its result")


def main():
    corpus = json.loads((ROOT / "data/factorio/recipe.json").read_text())
    items = json.loads((ROOT / "data/pack/item-map.json").read_text())["items"]
    overrides = json.loads((ROOT / "data/pack/recipe-overrides.json").read_text())["recipes"]

    failures = []
    check_item_map(items, corpus, failures)
    check_overrides(overrides, corpus, failures)
    machines = json.loads((ROOT / "data/pack/category-map.json").read_text())["machines"]
    # The types that exist today, read from the map rather than listed here: registering the
    # Centrifuge should be one edit to one design document (#135), not three.
    recipe_types = {m["recipe_type"] for m in machines.values() if m["recipe_type"]}
    check_emitted(items, recipe_types, failures)

    stale = subprocess.run(
        [sys.executable, str(ROOT / "scripts/factorio-recipe-convert.py"), "--check", "--quiet"],
        capture_output=True, text=True)
    if stale.returncode != 0:
        failures.extend(line for line in stale.stdout.splitlines() if line.strip())

    for number, failure in enumerate(failures, 1):
        print(f"FAIL {number}: {failure}")
    if failures:
        return 1
    decided = sum(1 for row in items.values() if "target" in row)
    ours = sum(1 for p in EMITTED.rglob("*.json")
               if not any(p.relative_to(EMITTED).as_posix().startswith(s + "/")
                              for s in FOREIGN_SUBTREES))
    print(f"ok   {len(items)} item-map rows ({decided} decided), "
          f"{ours} recipes emitted and current")
    return 0


if __name__ == "__main__":
    sys.exit(main())
