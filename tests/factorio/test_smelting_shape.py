#!/usr/bin/env python3
"""Assert the emitted pack smelts are shaped the way 26.1 parses an ingredient.

WHY THIS EXISTS. 26.1 replaced `Ingredient.CODEC`. An ingredient is a STRING -- a bare id for an
item, a `#`-prefixed one for a tag -- where 1.21.1 took an object, `{"item": "..."}` or
`{"tag": "..."}`. The converter kept writing the old shape through the port, and the result is not
a crash:

    [Worker-Main-3/ERROR]: Couldn't parse data file 'factoryworks:stone_brick':
      No key type in MapLike[{"item":"minecraft:cobblestone"}]

One ERROR line during datapack load, at the point nobody is watching, and the recipe is then simply
absent from the manager. In front of a player that is a furnace that takes the item, has power, and
never smelts -- the reading that cost #266's in-world check an evening.

WHAT IT CHECKS. Every emitted `factoryworks:smelting` file: the ingredient is a string, a tag
is `#`-prefixed, the result is still an object with an id, and `count` survives -- the count is the
whole reason the pack registers a recipe type of its own (#155, ADR-0046), and a shape change is
exactly where it would be dropped without anything failing.

WHAT IT IS NOT. It does not check the GregTech subtrees, which carry the same stale ingredient
shape. Those are `gtceu:` recipe types and GregTech left with ADR-0060, so they are dead wholesale
rather than mis-shaped, and they are the port's to re-derive -- asserting their shape here would be
a permanently red check for a reason this file does not own.

It also cannot tell whether the ids RESOLVE. Three of the four smelts name `gtceu:` results and
fail to load for that second, independent reason; that is the item map's rewrite, not this shape.

Usage: tests/factorio/test_smelting_shape.py
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
EMITTED = ROOT / "kubejs/data/factoryworks/recipe"
PACK_SMELTING = "factoryworks:smelting"

failures = []


def check(condition, message):
    if not condition:
        failures.append(message)


def main():
    check(EMITTED.is_dir(), "kubejs/data/factoryworks/recipe/ does not exist")
    if not EMITTED.is_dir():
        return report(0)

    smelts = [path for path in sorted(EMITTED.rglob("*.json"))
              if json.loads(path.read_text()).get("type") == PACK_SMELTING]
    check(smelts != [],
          "no `%s` recipe is emitted at all. The converter writes four (ADR-0046's stone brick and "
          "the three plates); none of them reaching the tree means the furnace ladder has nothing "
          "to smelt and every furnace check below is vacuous" % PACK_SMELTING)

    for path in smelts:
        recipe = json.loads(path.read_text())
        where = path.relative_to(EMITTED).as_posix()
        ingredient = recipe.get("ingredient")

        check(isinstance(ingredient, str),
              "%s states its ingredient as %r. 26.1 parses an ingredient as a string; an object "
              "fails at datapack load with `No key type in MapLike[...]` and the recipe is absent "
              "from the manager -- a furnace that holds the item, holds power and never smelts"
              % (where, ingredient))
        if not isinstance(ingredient, str):
            continue

        check(ingredient != "",
              "%s states an empty ingredient, which would be a free recipe if it parsed" % where)
        check(":" in ingredient.lstrip("#"),
              "%s states the ingredient `%s`, which names no namespace" % (where, ingredient))

        result = recipe.get("result")
        check(isinstance(result, dict) and "id" in result,
              "%s states its result as %r. The result is still an ItemStack object with an `id`; "
              "only the ingredient's shape moved in 26.1" % (where, result))

        check(isinstance(recipe.get("count"), int) and recipe["count"] >= 1,
              "%s has no `count`. The count on the ingredient is the whole reason the pack "
              "registers its own recipe type (#155): vanilla's smelting recipe cannot express "
              "`2 cobblestone -> 1 stone brick` (ADR-0046) and would silently make it 1:1"
              % where)

    return report(len(smelts))


def report(total):
    for failure in failures:
        print("FAIL: " + failure)
    if failures:
        print("\n%d failure(s)" % len(failures))
        return 1
    print("ok: %d pack smelt(s), each shaped the way 26.1 parses an ingredient" % total)
    return 0


if __name__ == "__main__":
    sys.exit(main())
