#!/usr/bin/env python3
"""Convert the extracted Factorio recipes into pack recipe JSON (ADR-0026, #87, #279).

Reads five committed inputs and writes recipe JSON on the pack's own types. Nothing here decides anything:
every judgement lives in one of the data files, so a decision is reviewed as a diff to a
design document rather than as a diff to a script.

  data/factorio/recipe.json        the corpus -- 163 Nauvis pre-launch recipes (#72)
  data/pack/category-map.json      which pack machine crafts a Factorio CATEGORY
  data/pack/subgroup-owner.json    which process crafts a recipe, per shelf and per recipe (#88)
  data/pack/item-map.json          Factorio name -> pack item, tag or fluid (ADR-0026)
  data/pack/recipe-overrides.json  every knowing departure from Factorio, with its reason (ADR-0031)

THE CONVERSION RULE (#126, rewriting ADR-0025's table). Nothing is scaled. Item counts transfer
1:1, one Factorio fluid unit is one millibucket, and `energy_required` seconds become ticks at
x 20. `crafting_speed` and power belong to the machine (ADR-0029), so neither appears in an
emitted recipe.

WHAT STOPS A RECIPE BEING EMITTED, in the order it is checked:

  1. its category is `!`-routed          -- deliberately not routed (category-map.json)
  2. its process is `not_emitted`        -- out of the corpus's emit scope (subgroup-owner.json)
  3. its process is `native_mechanic`    -- IN scope, supported by a mod mechanic with no recipe
  4. its process has no registered type  -- blocked on the ticket that registers the machine
  5. it touches an `undecided` item-map row -- blocked on the decision that row names
  6. an override says `skip`             -- a departure, with its reason

An item-map row marked `outside_corpus` has no corpus recipe to stop; it is reported as a skip too.

A Factorio name with NO item-map row at all is none of these: it is a HARD FAILURE (#72), because
a name nobody has looked at must never be quietly skipped.

Usage: scripts/factorio-recipe-convert.py [--check] [--quiet] [--awaited]
  --check    write nothing; exit non-zero if the emitted files on disk differ from what would be
             written. Generated output is never hand-edited (ADR-0026), and this is what says so.
  --awaited  write nothing; print, as JSON, every recipe skipped only because a ticket has not
             landed (4 and the item-map `blocked_by` rows), keyed by the id it WILL load under.
             A check that asserts an id is emitted reads this to tell a typo from a deferral.
"""
import argparse
import json
import shutil
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "kubejs/data/planetaryfactory/recipe"

# The subtrees of OUT_DIR this converter does NOT own. This converter wipes what it owns before
# writing, so without this exclusion a run would delete them, and `--check` would report them as
# unexpected files.
#
# `pack/` is not generated at all: ADR-0039's two Engineer's Pick recipes are
# hand-written, because Factorio has no mining-tool prototype and so the corpus can never author
# them. ADR-0031's exception is stated there and does not generalise -- see ADR-0039 and
# `docs/testing/hand-written-recipe-check.md`. Note that nothing may explain itself in place
# next to those recipes: KubeJS validates every file name under `kubejs/` and rejects an
# uppercase letter with an error that stops a world loading, so a README beside them is not
# an option -- the documentation for that subtree lives here and in `docs/`.
# Each is a path RELATIVE TO OUT_DIR, and each sits under `assembling/` because its recipes are
# `planetaryfactory:assembling` ones -- see `emitted_path` below.
FOREIGN_SUBTREES = ("assembling/pack", "assembling/sapling", "assembling/stock")


def is_ours(path):
    """True for a generated file this converter owns, false for another script's."""
    relative = path.relative_to(OUT_DIR).as_posix()
    return not any(relative.startswith(subtree + "/") for subtree in FOREIGN_SUBTREES)

# `subgroup-owner.json` names a process as `<owner>:<machine>`; `category-map.json` names the same
# machines and holds their recipe types. The two vocabularies meet here and nowhere else -- the
# recipe type itself, and the ticket that registers one that does not exist yet, are read from the
# map rather than kept in a table of this script's own.
#
# `pack:smelting` is the pack's three furnace tiers (#155) -- `planetaryfactory_core` blocks
# reading `planetaryfactory:smelting`, the mod's own type, and reading nothing else. The type
# carries a count on the ingredient, which is what lets the m:n smelts be expressed at all; every
# category-`smelting` recipe in the corpus emits onto it.
MACHINE_OF_PROCESS = {"pack:smelting": "smelting"}

# The pack's own furnace type (#155). Vanilla's `SmeltingRecipe` holds a bare `Ingredient` with no
# count while its result carries one, so `steel-plate` (5 plates to 1) and `stone-brick` (2 stone
# to 1, ADR-0046) have no vanilla shape at all; both used to be reported as skips here. This type
# carries the count, so nothing about a smelt is dropped on the way out.
#
# The vanilla type is not read alongside it. ADR-0034's sweep removes every vanilla smelting
# recipe, so a dual read would have no live consumer -- and a recipe on it would carry vanilla's
# cook time and get no tier scaling.
PACK_SMELTING = "planetaryfactory:smelting"

# Factorio's `crafting` / `advanced-crafting` / `crafting-with-fluid` all collapse to one
# machine, and the Personal Assembler needs the distinction back: it is a filtered view of the
# Assembling Machine's recipes, not a machine with a recipe type (#125's decision 6, CONTEXT.md).
# So the source category rides on the emitted recipe as a field of the type's own codec -- not
# as an item tag, and not by having the Personal Assembler read `data/factorio/recipe.json` at
# runtime, which would make a regenerable build input into a shipped runtime asset.
SOURCE_CATEGORY_KEY = "category"

# The pack's assembling type (#279). GregTech's `gtceu:assembling` left with ADR-0060, and every
# recipe on it was a file nothing read. Its codec is `AssemblingRecipe` in `planetaryfactory_core`.
PACK_ASSEMBLING = "planetaryfactory:assembling"


def load(path):
    return json.loads((ROOT / path).read_text())


def machine_of(process):
    """The `category-map.json` machine a `subgroup-owner.json` process names."""
    if process in MACHINE_OF_PROCESS:
        return MACHINE_OF_PROCESS[process]
    return process.split(":", 1)[1]


def process_of(recipe, subgroups):
    """The process that crafts this recipe: its own row if it has one, else its shelf's."""
    shelf = subgroups[f"{recipe['group']}/{recipe['subgroup']}"]
    process = shelf.get("per_recipe", {}).get(recipe["name"])
    if process is None:
        process = shelf.get("process") or shelf.get("owner")
    # `owner / process` where the two disagree; the machine is the right-hand side.
    if " / " in process:
        process = process.split(" / ", 1)[1]
    return process


def ingredient_of(row):
    """A 26.1 item `Ingredient`: a bare id, a `#`-prefixed tag, or NeoForge's custom-ingredient map.

    `Ingredient.CODEC` is NeoForge's `Codec.xor` of vanilla's string form and a map dispatched on
    `neoforge:ingredient_type`. 1.21.1's `{"item": ...}` object is neither, and fails at datapack
    load with one ERROR line and the recipe absent from the manager (#266).
    """
    if row.get("components"):
        # NeoForge's DataComponentIngredient: one item told apart by the components it carries.
        # The science packs are the case -- one `researchd:research_pack` item, four variants.
        return {"neoforge:ingredient_type": "neoforge:components", "items": row["target"],
                "components": row["components"]}
    if row["kind"] == "tag":
        return "#" + row["target"]
    return row["target"]


def convert_smelting(recipe, items, override):
    """One pack furnace recipe (#155), on all three tiers.

    `count` is Factorio's ingredient amount, carried rather than dropped -- it is the whole reason
    the pack registers a type of its own. `cookingtime` is `energy_required * 20` with no speed
    divisor in it: ADR-0029 puts `crafting_speed` on the machine, and the block divides.
    """
    ingredient = items[recipe["ingredients"][0]["name"]]
    result = items[recipe["results"][0]["name"]]
    return {
        "type": PACK_SMELTING,
        # 26.1's `Ingredient.CODEC` is a string, not an object: a bare id for an item and a
        # `#`-prefixed one for a tag. The 1.21.1 `{"item": ...}` / `{"tag": ...}` form this
        # replaces does not fail loudly -- it fails at datapack load with "No key type in
        # MapLike[...]" and the recipe is simply absent, which reads in-world as a furnace that
        # will not smelt anything (#266's in-world check).
        "ingredient": ("#" + ingredient["target"]) if ingredient["kind"] == "tag"
        else ingredient["target"],
        "count": recipe["ingredients"][0]["amount"],
        "result": {"id": result["target"], "count": recipe["results"][0]["amount"]},
        "cookingtime": override.get("duration", round(recipe["energy_required"] * 20)),
    }


def convert(recipe, items, override):
    """One `planetaryfactory:assembling` recipe (#279).

    The shape is `AssemblingRecipe.CODEC`'s, which composes NeoForge's own codecs rather than
    inventing any: an item ingredient is `SizedIngredient.NESTED_CODEC` (`ingredient` + `count`), a
    fluid one `SizedFluidIngredient.CODEC` (`ingredient` + `amount`), an item result
    `ItemStackTemplate.CODEC` (`id` + `count`) and a fluid result `FluidStackTemplate.CODEC` (`id` +
    `amount`). Every list is optional, so a recipe with no fluid writes no fluid key.
    """
    out = {"type": PACK_ASSEMBLING, SOURCE_CATEGORY_KEY: recipe["category"]}
    sides = {"ingredients": [], "fluid_ingredients": [], "results": [], "fluid_results": []}
    for entry in recipe["ingredients"]:
        row = items[entry["name"]]
        if row["kind"] == "fluid":
            sides["fluid_ingredients"].append({"ingredient": row["target"],
                                               "amount": entry["amount"]})
        else:
            sides["ingredients"].append({"ingredient": ingredient_of(row),
                                         "count": entry["amount"]})
    for entry in recipe["results"]:
        row = items[entry["name"]]
        if row["kind"] == "fluid":
            sides["fluid_results"].append({"id": row["target"], "amount": entry["amount"]})
        else:
            result = {"id": row["target"], "count": entry["amount"]}
            if row.get("components"):
                result["components"] = row["components"]
            sides["results"].append(result)
    for field, entries in sides.items():
        if entries:
            out[field] = entries
    out["time"] = override.get("duration", round(recipe["energy_required"] * 20))
    return out


def emitted_path(recipe_type, name):
    """`<type path>/<name>`, which is also the recipe's id and therefore what research unlocks.

    #87 made this a rule rather than a habit: GregTech re-registered every GTRecipe under its type's
    path, so a file anywhere else loaded twice. The pack's own type is not re-registered by
    anything, so the directory is no longer load-bearing for duplication -- it is kept because
    `researchd.js` unlocks `planetaryfactory:assembling/<name>` and the foreign subtrees sit inside
    it.
    """
    return "%s/%s" % (recipe_type.split(":", 1)[1], name.replace("-", "_"))


def apply_override(recipe, override):
    if not ({"ingredients", "results"} & set(override)):
        return recipe
    recipe = dict(recipe)
    for field in ("ingredients", "results"):
        if field in override:
            recipe[field] = [dict(e) for e in override[field]]
    return recipe


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true",
                        help="write nothing; fail if the emitted files differ")
    parser.add_argument("--quiet", action="store_true")
    parser.add_argument("--awaited", action="store_true",
                        help="print the recipes waiting on a ticket, as JSON, and write nothing")
    args = parser.parse_args()

    recipes = load("data/factorio/recipe.json")
    routes = load("data/pack/category-map.json")["routes"]
    subgroups = load("data/pack/subgroup-owner.json")["subgroups"]
    items = load("data/pack/item-map.json")["items"]
    overrides = load("data/pack/recipe-overrides.json")["recipes"]
    machines = load("data/pack/category-map.json")["machines"]

    # The overrides file's own invariants -- a `reason` on every entry, and no entry naming a
    # recipe outside the corpus -- are asserted by `tests/factorio/test_recipe_convert.py`.
    failures = []

    emitted, skipped = {}, []
    # Recipe id -> the tickets it waits on and the items it will make. Only the two skips that
    # resolve by a ticket landing; an `undecided` row or an override is a decision, not a wait.
    awaiting = {}

    def await_(name, machine, tickets, recipe):
        awaiting["planetaryfactory:%s/%s" % (machine, name.replace("-", "_"))] = {
            "tickets": sorted(tickets),
            "results": sorted(items[e["name"]]["target"] for e in recipe["results"]
                              if "target" in items.get(e["name"], {})),
        }
    for recipe in recipes:
        name = recipe["name"]
        override = overrides.get(name, {})
        route = routes.get(recipe["category"])
        if route is None:
            failures.append(f"{name}: category {recipe['category']} is routed by nothing")
            continue
        if route.startswith("!"):
            skipped.append((name, "unrouted category", route[1:]))
            continue

        process = process_of(recipe, subgroups)
        if process in ("not_emitted", "native_mechanic", "undecided"):
            skipped.append((name, process, f"{recipe['group']}/{recipe['subgroup']}"))
            continue
        machine = machine_of(process)
        if machine not in machines:
            failures.append(f"{name}: process {process} names no machine in category-map.json")
            continue
        recipe_type = machines[machine]["recipe_type"]
        if recipe_type is None:
            ticket = machines[machine].get("blocked_by")
            skipped.append((name, "machine not registered",
                            f"{machine}" + (f", #{ticket}" if ticket else ", no ticket")))
            if ticket:
                await_(name, machine, [ticket], recipe)
            continue

        recipe = apply_override(recipe, override)
        missing = [e["name"] for e in recipe["ingredients"] + recipe["results"]
                   if e["name"] not in items]
        if missing:
            # #72's decision, and the one failure this converter must never soften.
            failures.append(f"{name}: no item-map row for " + ", ".join(sorted(set(missing))))
            continue
        # A `native_mechanic` row is reported under its own name: the capability is fully
        # supported and the recipe is not a cut, so it must never read as a blocked decision (#93).
        for status, label in (("native_mechanic", "native mechanic"),
                              ("not_emitted", "not_emitted item-map row"),
                              ("undecided", "undecided item-map row")):
            blocked = sorted({e["name"] for e in recipe["ingredients"] + recipe["results"]
                              if items[e["name"]].get("status") == status})
            if blocked:
                skipped.append((name, label, ", ".join(blocked)))
                break
        if blocked:
            continue
        # A `blocked_by` row has a target the game cannot load yet: a first-party item that
        # arrives with a `planetaryfactory_core` ticket, or a borrowed one whose mod is not on
        # 26.1.2 (#277, #260). Emitting a
        # recipe against it produces JSON that names an item nothing registers, and KubeJS fails
        # to read the recipe at WORLD LOAD rather than at conversion time: an error in a log,
        # nothing craftable, and no clue pointing back here. The static check already tolerates
        # these rows; the converter has to skip them, or the tolerance ships a broken world.
        awaited = sorted({e["name"] for e in recipe["ingredients"] + recipe["results"]
                          if "blocked_by" in items[e["name"]]})
        if awaited:
            tickets = sorted({items[n]["blocked_by"] for n in awaited})
            skipped.append((name, "item not registered yet",
                            ", ".join(awaited) + " — "
                            + ", ".join(f"#{t}" for t in tickets)))
            await_(name, machine, tickets, recipe)
            continue
        on_tag = [e["name"] for e in recipe["results"] if items[e["name"]]["kind"] == "tag"]
        if on_tag:
            failures.append(f"{name}: result {on_tag[0]} maps onto a tag, which cannot be a result")
            continue
        if override.get("skip"):
            skipped.append((name, "override", override["reason"]))
            continue

        if recipe_type == PACK_SMELTING:
            emitted[name.replace("-", "_")] = convert_smelting(recipe, items, override)
        else:
            if recipe_type != PACK_ASSEMBLING:
                failures.append(f"{name}: recipe type {recipe_type} has no emitter -- "
                                "category-map.json names a type this converter cannot shape")
                continue
            emitted[emitted_path(recipe_type, name)] = convert(recipe, items, override)

    for name, row in sorted(items.items()):
        if "outside_corpus" in row:
            skipped.append((name, "outside the corpus", row["outside_corpus"]))

    if failures:
        for failure in failures:
            print("FAIL " + failure)
        return 1

    if args.awaited:
        print(json.dumps(awaiting, indent=2, sort_keys=True))
        return 0

    if args.check:
        written = {p.relative_to(OUT_DIR).with_suffix("").as_posix(): json.loads(p.read_text())
                   for p in OUT_DIR.rglob("*.json")
                   if is_ours(p)} if OUT_DIR.exists() else {}
        if written != emitted:
            added = sorted(set(emitted) - set(written))
            removed = sorted(set(written) - set(emitted))
            changed = sorted(k for k in set(emitted) & set(written) if emitted[k] != written[k])
            print("FAIL emitted recipes on disk are stale -- re-run "
                  "scripts/factorio-recipe-convert.py")
            for label, names in (("missing", added), ("unexpected", removed), ("changed", changed)):
                if names:
                    print(f"  {label}: " + ", ".join(names))
            return 1
    else:
        if OUT_DIR.exists():
            # File by file rather than by directory: a foreign subtree now sits INSIDE a directory
            # this converter owns (`assembling/`), so removing that directory wholesale would take
            # another converter's output with it. `is_ours` is the only thing that decides.
            for path in sorted(OUT_DIR.rglob("*.json")):
                if is_ours(path):
                    path.unlink()
            for path in sorted(OUT_DIR.rglob("*"), reverse=True):
                if path.is_dir() and not any(path.iterdir()):
                    path.rmdir()
        OUT_DIR.mkdir(parents=True, exist_ok=True)
        for stem, body in emitted.items():
            path = OUT_DIR / f"{stem}.json"
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(body, indent=2) + "\n")

    if not args.quiet:
        reasons = Counter(reason for _, reason, _ in skipped)
        print(f"ok   {len(emitted)} recipes emitted, {len(skipped)} not")
        for reason, count in reasons.most_common():
            print(f"     {count:3d} {reason}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
