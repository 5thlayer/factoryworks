#!/usr/bin/env python3
"""Assert no item is made by two recipes unless a decision says it is.

Every other recipe check in this directory owns ONE subtree and reads ONE input table: the Factorio
converter's and the hand-written pack subtree's. That is
the right shape for asking "did this converter do its job", and it is blind to the one question
none of them can ask -- whether two converters, or one converter twice, made the same item.

The failure is quiet in the way that matters. Two routes to one block is not an error, does not
fail a schema, appears in no log, and loads perfectly. It reaches the player as two entries in EMI
for the same thing, and if both carry `category: crafting` it also reaches the Personal
Assembler's resolver, which picks a route with no cost model and therefore cannot choose between
them (`test_hand_resolver.py` asserts that property over the Factorio corpus; this asserts it over
what is actually emitted). It shipped once: Create's two gearbox conversions and the large
cogwheel's second route were emitted alongside the direct recipes they duplicate, and every
subtree-local check passed.

It used to hold a file-path invariant as well: GregTech re-registered every loaded GTRecipe under its
type's own path, so a file anywhere else loaded twice (#87). The pack's own types are re-registered
by nothing, and that rule left with GregTech (#279).

WHAT IT ASSERTS

  - every item emitted by more than one recipe is named in `MULTI_ROUTE` with the reason it earns
    a second route. Anything else is a duplicate
  - a `MULTI_ROUTE` row that no longer has two routes is removed, unless the converter is holding
    its routes back until a ticket lands (`--awaited`). A row nobody reads is a rule a
    converter change left behind, and it would silently re-admit a duplicate later
  - at most one route per item is hand-craftable, wherever the routes come from

WHAT IT IS NOT. It is not a check that the routes are BALANCED -- two routes at wildly different
costs is a progression escape, and costing is a decision no static check can make. It only asserts
that a second route was chosen by somebody.

Usage: tests/factorio/test_recipe_duplication.py
"""
import collections
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
EMITTED = ROOT / "kubejs/data/factoryworks/recipe"
# The pack's furnace type (#155): a count-bearing smelt, whose output is shaped differently.
PACK_SMELTING = "factoryworks:smelting"

# Items an emitted recipe is allowed to make twice, and why. A row here is a DECISION: it says the
# second route earns its EMI entry. The default is one route per item, because under ADR-0034's
# default-deny sweep every recipe in the game is one the pack chose to author.
MULTI_ROUTE = {
    "factoryworks:solid_fuel": (
        "Factorio's own three routes -- heavy oil, light oil and petroleum gas each make solid "
        "fuel, and which one is worth running is the whole point of the oil line. ADR-0031 says "
        "the corpus authors what it contains, and it contains all three."),
}

failures = []


def check(condition, message):
    if not condition:
        failures.append(message)


def outputs_of(recipe):
    """The items a recipe produces, each its id plus its components (ADR-0052). Fluids are out of
    scope -- nothing duplicates one."""
    if recipe.get("type") == PACK_SMELTING:
        return [item_key(recipe["result"])]
    return [item_key(entry) for entry in recipe.get("results", [])]


def item_key(entry):
    components = entry.get("components")
    return entry["id"] + (json.dumps(components, sort_keys=True) if components else "")


def awaited_results():
    """Item -> how many routes to it the converter is holding back until a ticket lands."""
    run = subprocess.run([sys.executable, str(ROOT / "scripts/factorio-recipe-convert.py"),
                          "--awaited"], capture_output=True, text=True, check=True)
    counts = collections.Counter()
    for waiting in json.loads(run.stdout).values():
        counts.update(waiting["results"])
    return counts


def main():
    check(EMITTED.is_dir(), "kubejs/data/factoryworks/recipe/ does not exist")
    if not EMITTED.is_dir():
        return report()

    routes = collections.defaultdict(list)
    for path in sorted(EMITTED.rglob("*.json")):
        recipe = json.loads(path.read_text())
        where = path.relative_to(EMITTED).as_posix()
        hand = recipe.get("type") == "craftworks:assembling" and recipe.get("hand_craftable", True)
        for item in outputs_of(recipe):
            routes[item].append((where, hand))

    total = sum(len(paths) for paths in routes.values())

    awaited = awaited_results()

    for item, paths in sorted(routes.items()):
        if len(paths) > 1:
            check(item in MULTI_ROUTE,
                  "`%s` is made by %d recipes (%s) and MULTI_ROUTE does not name it. Two routes "
                  "to one item is two EMI entries for the same thing, and nothing in the game "
                  "reports it -- skip one where it is generated, or add a row here saying what "
                  "the second route earns"
                  % (item, len(paths), ", ".join(where for where, _ in paths)))

        hands = [where for where, hand in paths if hand]
        check(len(hands) <= 1,
              "`%s` has %d hand recipes (%s). The Personal Assembler's resolver picks a route "
              "with no cost model, so it cannot choose between them -- at most one route per "
              "item may be `hand_craftable`"
              % (item, len(hands), ", ".join(hands)))

    for item, why in sorted(MULTI_ROUTE.items()):
        found = len(routes.get(item, []))
        check(found > 1 or found + awaited[item] > 1,
              "MULTI_ROUTE names `%s`, which %s. The row decides nothing now, and left in place "
              "it would silently re-admit a duplicate later. Its reason was: %s"
              % (item,
                 "no recipe emits" if found == 0 else "only one recipe emits",
                 why))

    return report(routes, total)


def report(routes=None, total=0):
    for failure in failures:
        print("FAIL: " + failure)
    if failures:
        print("\n%d failure(s)" % len(failures))
        return 1
    routes = routes or {}
    multi = sum(1 for paths in routes.values() if len(paths) > 1)
    print("ok: %d recipe output(s) across %d item(s); %d item(s) have a second route and each is "
          "a recorded decision" % (total, len(routes), multi))
    return 0


if __name__ == "__main__":
    sys.exit(main())
