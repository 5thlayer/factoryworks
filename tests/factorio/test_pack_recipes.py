#!/usr/bin/env python3
"""Assert the hand-written recipe subtree, and the Engineer's Pick's pack-side files.

`docs/testing/what-to-check.md`'s "cross-file references resolve" claim, for the one subtree of
`kubejs/data/factoryworks/recipe/` that no converter generates.

`recipe/assembling/pack/` is ADR-0031's single stated exception: the corpus authors every recipe it contains,
and Factorio has no mining-tool prototype, so the Engineer's Pick's two recipes cannot come from
`data/factorio/recipe.json` at all. That exemption is what makes this file necessary -- every other
recipe here is regenerated and checked against the corpus, and these two are checked against
nothing unless something checks them here.

What fails quietly without it:

  - a converter run wiping the subtree, because `FOREIGN_SUBTREES` stopped naming it. The pick
    recipes vanish and the pack is back to the state #165 describes: nothing can be mined at all.
  - dropping `category: crafting` or `hand_craftable`, which are the whole definition of the
    Personal Assembler's hand set (ADR-0118). The recipe survives, is craftable in a machine the
    player cannot build yet, and rung 0 is a dead end.
  - a file under `kubejs/` whose name carries an uppercase letter. KubeJS validates every name it
    scans and rejects one outright -- `Invalid file name: Uppercase 'R' in
    kubejs/data/factoryworks/recipe/assembling/pack/README.md` -- and that ERROR stops a world from
    loading. It is asserted here because this subtree is the one place a human writes files under
    `kubejs/` by hand rather than generating them, and a README next to the recipes is the obvious
    thing to reach for.
  - a pick with no model, texture or lang key -- the black-and-magenta cube and a raw translation
    key, neither of which is logged as an error.
  - the steel recipe not consuming the iron pick, which ADR-0039 states in one line and which no
    other file would notice.

Usage: tests/factorio/test_pack_recipes.py
"""
import json
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
EMITTED = ROOT / "kubejs/data/factoryworks/recipe"
SUBTREE = "assembling/pack"
PACK = EMITTED / SUBTREE
ASSETS = ROOT / "kubejs/assets/factoryworks"
DATA = ROOT / "kubejs/data"
# The two trees KubeJS scans and name-validates. `kubejs/README.txt` sits above both, which is why
# the roots are named rather than `kubejs/` itself.
SCANNED = (ROOT / "kubejs/data", ROOT / "kubejs/assets")
CONVERTER = ROOT / "scripts/factorio-recipe-convert.py"
MODS = ROOT / "mods"
NAMESPACE = "factoryworks"

# The category a hand-craftable recipe carries, and nothing else is hand-craftable.
HAND_CATEGORY = "crafting"

PICKS = ("engineers_iron_pick", "engineers_steel_pick")
FOREIGN_RE = re.compile(r"^FOREIGN_SUBTREES = \((.*)\)$", re.MULTILINE)

failures = []


def check(condition, message):
    if not condition:
        failures.append(message)
    return condition




def items_of(recipe, side):
    """`(item or #tag, count)` pairs off a `craftworks:assembling` recipe."""
    if side == "inputs":
        return [(entry["ingredient"], entry.get("count", 1))
                for entry in recipe.get("ingredients", [])]
    return [(entry["id"], entry.get("count", 1)) for entry in recipe.get("results", [])]


def texture_resolves(item, layer):
    """That an item model's texture is one that will actually be there at load.

    Both picks are dressed from vanilla (#241, on #323): the Iron Pick wears
    `minecraft:item/iron_pickaxe` and the Steel Pick `minecraft:item/netherite_pickaxe`. The Steel
    Pick used to wear GTCEu's Damascus Steel pickaxe, flattened into our namespace by a generator,
    because GT's tool art is three greyscale layers that only become a material under GregTech's
    item-colour handler -- which never sees an item that is not a GT tool. GregTech left with
    ADR-0060 and the source left with it, so the sprite is a borrow from Minecraft now and the
    generator is gone. A foreign namespace is still resolved here rather than assumed, because the
    pack borrows art from other jars elsewhere and the next layer0 may not be vanilla's.

    A texture that is not there renders as the black-and-magenta checkerboard with only a
    client-side warning, so each namespace is resolved where it can be: our own against the file,
    a mod's against the jar the pack ships, and vanilla's against nothing -- the client jar is not
    in this repo, and `minecraft:item/iron_pickaxe` is not a name that moves.
    """
    namespace, _, path = layer.partition(":")
    if namespace == NAMESPACE:
        check((ASSETS / "textures" / (path + ".png")).is_file(),
              "%s's model points at %r and that file is missing, which renders as the "
              "missing-texture checkerboard" % (item, layer))
    elif namespace == "minecraft":
        return
    else:
        jars = sorted(MODS.glob("%s-*.jar" % namespace))
        if check(len(jars) == 1,
                 "%s's model points at %r, and mods/ holds %d %s jar(s) to resolve it against"
                 % (item, layer, len(jars), namespace)):
            with zipfile.ZipFile(jars[0]) as jar:
                names = set(jar.namelist())
            check(("assets/%s/textures/%s.png" % (namespace, path)) in names,
                  "%s's model points at %r, which the installed %s jar does not contain"
                  % (item, layer, namespace))


def main():
    picks = PICKS

    # The subtree exists and is exactly the two recipes. A third file here is a decision this ADR
    # did not make: its exception is narrow by design, and a general escape hatch was rejected.
    check(PACK.is_dir(), "%s does not exist -- ADR-0039's hand-written recipes are missing" % PACK)
    if not PACK.is_dir():
        return report()
    recipes = {p.stem: json.loads(p.read_text()) for p in sorted(PACK.glob("*.json"))}
    check(set(recipes) == set(picks),
          "recipe/%s/ holds %s; ADR-0039's exception covers exactly %s. A third recipe here needs "
          "its own decision, not this one's precedent"
          % (SUBTREE, sorted(recipes), sorted(picks)))

    # The converter must leave the subtree alone. Its check reads the same list out of it.
    declared = FOREIGN_RE.search(CONVERTER.read_text(encoding="utf-8"))
    if check(declared is not None, "%s declares no FOREIGN_SUBTREES" % CONVERTER.name):
        check(('"%s"' % SUBTREE) in declared.group(1),
              "%s does not list `%s` as foreign, so a converter run deletes ADR-0039's "
              "hand-written recipes and nothing can be mined again (#165)"
              % (CONVERTER.name, SUBTREE))

    for name, recipe in sorted(recipes.items()):
        where = "%s/%s.json" % (SUBTREE, name)
        check(recipe.get("category") == HAND_CATEGORY and recipe.get("hand_craftable") is True,
              "%s is not category %r with `hand_craftable` true, so the Personal Assembler will not "
              "plan it and rung 0 has no route to a pick" % (where, HAND_CATEGORY))
        outputs = items_of(recipe, "outputs")
        check(outputs == [("%s:%s" % (NAMESPACE, name), 1)],
              "%s outputs %s; a recipe under pack/ is named for the single item it makes"
              % (where, outputs))

    # ADR-0039: the steel recipe CONSUMES the iron pick, so the player holds one or the other.
    steel = recipes.get("engineers_steel_pick")
    if steel is not None:
        inputs = dict(items_of(steel, "inputs"))
        check(inputs.get("%s:engineers_iron_pick" % NAMESPACE) == 1,
              "the Steel Pick recipe does not consume the Iron Pick. ADR-0039 has the player "
              "holding one tier or the other, never both")

    # The pack-side files each registered pick needs. Every one of these fails silently.
    lang = json.loads((ASSETS / "lang/en_us.json").read_text(encoding="utf-8"))
    for item in sorted(picks):
        check(("item.%s.%s" % (NAMESPACE, item)) in lang,
              "%s has no lang key, so it ships showing its raw translation key" % item)
        model = ASSETS / "models/item" / (item + ".json")
        if check(model.is_file(), "%s has no item model" % item):
            layer = json.loads(model.read_text())["textures"]["layer0"]
            texture_resolves(item, layer)
        check(item in recipes,
              "%s is registered but nothing crafts it" % item)

    # Two of the Pick's verbs are tag entries, not code, each read by another jar: left out of
    # `c:tools/wrench` it toggles no Oritech pipe connection, and out of Groundworks' tag it takes up
    # no belt or pipe run (#448).
    for tag in ("c/tags/item/tools/wrench.json", "groundworks/tags/item/dismantles.json"):
        path = DATA / tag
        if check(path.is_file(), "%s is missing, so the Pick lacks the verb it carries" % tag):
            values = set(json.loads(path.read_text())["values"])
            for item in sorted(picks):
                check(("%s:%s" % (NAMESPACE, item)) in values,
                      "%s is not in %s, so it lacks the verb that tag carries" % (item, tag))

    # Beltworks adds every pickaxe and wrench to Groundworks' tag; only the Picks dismantle here.
    dismantles = json.loads((DATA / "groundworks/tags/item/dismantles.json").read_text())
    check(dismantles.get("replace") is True
          and set(dismantles["values"]) == {"%s:%s" % (NAMESPACE, item) for item in picks},
          "groundworks:dismantles is not replaced by exactly the Picks, so a vanilla pickaxe "
          "dismantles belts and pipes (#448)")

    # Every name KubeJS will scan, lowercase. This is not about tidiness: the validator refuses an
    # uppercase letter with an ERROR, and the world does not load.
    for root in SCANNED:
        for path in sorted(root.rglob("*")):
            if not path.is_file():
                continue
            check(path.name == path.name.lower(),
                  "`%s` has an uppercase letter in its name. KubeJS rejects it -- `Invalid file "
                  "name` -- and that stops a world from loading"
                  % path.relative_to(ROOT).as_posix())

    return report()


def report():
    for failure in failures:
        print("FAIL " + failure)
    if failures:
        return 1
    print("ok   %d pick recipe(s) and both picks dressed" % len(list(PACK.glob("*.json"))))
    return 0


if __name__ == "__main__":
    sys.exit(main())
