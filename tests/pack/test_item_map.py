#!/usr/bin/env python3
"""Assert every item-map target names something installed, and no ingredient rides a contested tag.

`docs/testing/what-to-check.md`'s "cross-file references resolve" claim, for `data/pack/item-map.json`
-- the file where a Factorio name becomes an item id, and therefore the file every emitted recipe in
the pack is downstream of. `tests/pack/test_starting_kit.py` already does this walk for the five ids
the starting kit grants; this is the same walk over all 166 rows.

TWO ASSERTIONS, AND THE SECOND IS WHY THIS FILE EXISTS (#275, ADR-0061).

1. **Every target resolves against the installed jars.** A row naming a departed mod is not a crash:
   it is one `Couldn't parse data file` line at datapack load and then a recipe absent from its
   manager -- a furnace that holds the item, holds power and never smelts (#266). ADR-0060 removed
   six mods at once and 31 rows kept naming them; three of the pack's four smelts were rejected in
   a running game with every static check in the repo green.

2. **No emitted ingredient names a `c:` tag that more than one installed jar populates.** This is
   the rule ADR-0061 states and nothing else here can see. With AlmostUnified gone (ADR-0060) there
   is no arbiter: Railcraft Reborn shares 77 `c:` item tags with FTB Materials and Oritech shares
   47, so `#c:ingots/steel` accepts three different items and is therefore not a decision. Nothing
   fails -- the file is valid, the recipe loads, and it reaches a player as one material with three
   EMI entries and a recipe that takes whichever it feels like. A tag with exactly one populating
   jar stays legal, which is what keeps `#c:raw_materials/iron` doing its job.

WHAT A DEFERRAL IS. The rows #258, #293, #294, #295, #260 and #378 own cannot resolve until those tickets land, so they are
listed in DEFERRED with the ticket that owns each, in `scripts/check-datapack-load.py`'s idiom: an
unlisted failure fails, and a LISTED row that now resolves fails too. The guard re-arms one row at a
time rather than the assertion being weakened, and each ticket deletes its entry as its block lands.

HOW A TARGET IS RESOLVED. By the lang key its namespace's jar ships, which is what
`test_starting_kit.py` uses and the only registry evidence a static check has. Three namespaces are
not in `mods/`: `planetaryfactory`'s own items are the mod's lang plus KubeJS's `event.create` ids,
and `minecraft`'s are the installed client jar's -- which is outside the repo, so vanilla rows are
skipped rather than guessed when it is not there.
"""

import collections
import functools
import json
import os
import pathlib
import re
import unittest
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
ITEM_MAP = ROOT / "data/pack/item-map.json"
MODS = ROOT / "mods"
KUBEJS = ROOT / "kubejs"
MOD_ASSETS = ROOT / "mod/src/main/resources/assets"
VANILLA = pathlib.Path(
    os.environ.get("PF_CLIENT_JAR", os.path.expanduser(
        "~/curseforge/Install/versions/26.1.2/26.1.2.jar")))

# The `kubejs/data` subtrees that ADR-0060 left dead. They name registries that went with GregTech
# and GCyR and are re-derived against the chassis (#258) by the converter (#279); scanning them for
# contested tags would be asserting against files nobody intends to load.
DEAD_SUBTREES = ("gtceu", "gt_materials", "gcyr")

# A row whose target cannot resolve yet, and the ticket that owns it. Deleting an entry is part of
# that ticket's fix -- a stale one is a guard nobody re-armed.
CHASSIS = (
    "names a `planetaryfactory_core` subclass of an Oritech machine (ADR-0060) that #277 decided and "
    "the chassis has not built yet: #258 owns this row")
PUMP = (
    "names the first-party in-line pump, since Oritech's Pump drains the world rather than a pipe: "
    "#293 owns this row")
POWER_SWITCH = (
    "names the first-party power switch, since Oritech ships none and its Flux Gate is an item: "
    "#294 owns this row")
SILO = (
    "names the first-party Rocket Silo and the part it makes, which #378 owns and has not built yet")
DEFERRED = {
    "chemical-plant": CHASSIS,
    "oil-refinery": CHASSIS,
    "pump": PUMP,
    "power-switch": POWER_SWITCH,
    "rocket-silo": SILO,
    "rocket-part": SILO,
}

# The namespaces ADR-0060 took out of the pack. A row naming one can only be a deferral.
REMOVED_BY_ADR_0060 = ("gtceu", "create", "powergrid", "gcyr", "modern_industrialization",
                       "almostunified")

# A contested `c:` tag an emitted ingredient may still name, and the item that wins. Empty by
# design: ADR-0061's answer to a contested tag is to name the item instead, so an entry here is a
# row somebody argued for rather than a default.
TAG_WINNERS = {}

# The lang prefixes a registered thing can be named under. A fluid's bucket is an item; the fluid
# itself is a `fluid_type`, or -- Oritech's spelling, keyed by the fluid's own id -- a `fluid`.
PREFIXES = ("item", "block", "fluid_type", "fluid")


@functools.lru_cache(maxsize=None)
def jar_lang():
    """Every lang key the installed jars ship, per namespace."""
    keys = collections.defaultdict(set)
    for jar in sorted(MODS.glob("*.jar")):
        with zipfile.ZipFile(jar) as archive:
            for name in archive.namelist():
                found = re.fullmatch(r"assets/([a-z0-9_.-]+)/lang/en_us\.json", name)
                if not found:
                    continue
                try:
                    keys[found.group(1)] |= set(json.loads(archive.read(name)))
                except (ValueError, KeyError):
                    continue
    return keys


@functools.lru_cache(maxsize=None)
def pack_lang():
    """The pack's own lang keys -- the mod's resources and KubeJS's assets together."""
    keys = set()
    for path in list(KUBEJS.glob("assets/*/lang/*.json")) + list(MOD_ASSETS.glob("*/lang/*.json")):
        keys |= set(json.loads(path.read_text(encoding="utf-8")))
    return keys


@functools.lru_cache(maxsize=None)
def kubejs_created():
    """The ids KubeJS's startup scripts register, which have no lang key of their own."""
    created = set()
    for path in KUBEJS.glob("startup_scripts/*.js"):
        created |= set(re.findall(r"event\.create\('([a-z0-9_]+:[a-z0-9_/]+)'",
                                  path.read_text(encoding="utf-8")))
    return created


@functools.lru_cache(maxsize=None)
def vanilla_lang():
    """The client jar's lang keys, or an empty set when the jar is not on this machine."""
    if not VANILLA.exists():
        return frozenset()
    with zipfile.ZipFile(VANILLA) as archive:
        return frozenset(json.loads(archive.read("assets/minecraft/lang/en_us.json")))


def resolves(target):
    """Whether one `namespace:path` names something an installed jar registers."""
    namespace, _, path = target.partition(":")
    if namespace == "planetaryfactory":
        if target in kubejs_created():
            return True
        keys = pack_lang()
    elif namespace == "minecraft":
        keys = vanilla_lang()
    else:
        # An item a jar names by some other key, such as Researchd's Lab, is still in the registry.
        if target in jar_registry():
            return True
        keys = jar_lang().get(namespace, set())
    return any("%s.%s.%s" % (prefix, namespace, path) in keys for prefix in PREFIXES)


@functools.lru_cache(maxsize=None)
def jar_registry():
    """Every item and fluid id `scripts/jar-registry-extract.py` read out of the installed jars."""
    jars = ROOT / "data" / "jars"
    return {entry for name in ("item.json", "fluid.json")
            for entry in json.loads((jars / name).read_text(encoding="utf-8"))["items" if name == "item.json" else "fluids"]}


@functools.lru_cache(maxsize=None)
def tag_sources():
    """Which installed jars' namespaces populate each `c:` item tag, by the items they put in it."""
    sources = collections.defaultdict(set)
    for jar in sorted(MODS.glob("*.jar")):
        with zipfile.ZipFile(jar) as archive:
            for name in archive.namelist():
                found = re.fullmatch(r"data/c/tags/item/(.+)\.json", name)
                if not found:
                    continue
                try:
                    values = json.loads(archive.read(name)).get("values", [])
                except ValueError:
                    continue
                for value in values:
                    if isinstance(value, dict):
                        value = value.get("id", "")
                    if isinstance(value, str) and value and not value.startswith("#"):
                        sources["c:" + found.group(1)].add(value.split(":")[0])
    return sources


def live_ingredient_files():
    """Every recipe and item tag under `kubejs/data` outside an ADR-0060 dead subtree.

    Only the files that take ITEMS: a block tag naming `#c:ores` is a mining rule over placed
    blocks, where two mods' ores both being ores is the point rather than an ambiguity.
    """
    data = KUBEJS / "data"
    for path in data.rglob("*.json"):
        parts = path.relative_to(data).parts
        if any(part in DEAD_SUBTREES for part in parts):
            continue
        if "recipe" not in parts and parts[1:3] != ("tags", "item"):
            continue
        yield path


class ItemMapTargetsResolve(unittest.TestCase):
    def setUp(self):
        self.rows = json.loads(ITEM_MAP.read_text(encoding="utf-8"))["items"]
        self.assertTrue(jar_lang(), "no jar in mods/ ships a lang file -- run packwiz first")

    def targets(self, include_blocked=False):
        """The rows that name a target at all, minus -- by default -- the ones the map records as
        blocked."""
        for name, row in self.rows.items():
            # A tag row is not a registered thing; its half is NoIngredientRidesAContestedTag.
            if "target" not in row or row.get("kind") == "tag":
                continue
            if "blocked_by" in row and not include_blocked:
                continue
            yield name, row["target"]

    def test_every_target_resolves_or_is_a_recorded_deferral(self):
        for name, target in self.targets():
            if target.startswith("minecraft:") and not vanilla_lang():
                continue
            if resolves(target):
                continue
            self.assertIn(
                name, DEFERRED,
                "item-map row %r names %s, which no installed jar registers. Nothing fails until "
                "a world loads, and then it is one ERROR line and a recipe absent from its "
                "manager (ADR-0061)" % (name, target))

    def test_no_deferral_is_stale(self):
        unresolved = {name for name, target in self.targets(include_blocked=True)
                      if not resolves(target)}
        for name in sorted(DEFERRED):
            self.assertIn(name, self.rows, "DEFERRED names %r, which is not an item-map row" % name)
            self.assertIn(
                name, unresolved,
                "row %r is listed as deferred (%s) but its target resolves now. Delete the entry "
                "-- a stale one is a guard nobody re-armed" % (name, DEFERRED[name]))

    def test_every_deferral_is_blocked_by_its_ticket(self):
        """A deferred row carries `blocked_by`, which is what makes the converter skip it (#279).

        Without the field the converter emits a recipe naming the dead id, and the game rejects it
        at load -- one ERROR line and a recipe absent from its manager.
        """
        for name, reason in sorted(DEFERRED.items()):
            ticket = int(re.search(r"#(\d+) owns", reason).group(1))
            self.assertEqual(
                self.rows.get(name, {}).get("blocked_by"), ticket,
                "item-map row %r is deferred to #%d but does not say `\"blocked_by\": %d`, so the "
                "converter emits recipes naming a target no installed jar registers"
                % (name, ticket, ticket))

    def test_no_row_names_a_mod_adr_0060_removed(self):
        """The namespaces are named outright, because a removed mod's id resolving is impossible.

        `test_every_target_resolves_or_is_a_recorded_deferral` would catch these too, via DEFERRED
        -- but only while a row is deferred. This assertion is what stops a deferred row being answered
        by putting `gtceu:` back.
        """
        for name, target in self.targets():
            namespace = target.split(":")[0]
            if namespace not in REMOVED_BY_ADR_0060 or name in DEFERRED:
                continue
            self.fail("item-map row %r names %s, whose mod ADR-0060 removed from the pack"
                      % (name, target))

    def test_the_material_forms_are_ftb_materials(self):
        """ADR-0061's rule, on the seven rows #275 rewrote.

        Stated as a namespace rather than as seven ids: the ids are the map's to change, and what
        this asserts is that the next chassis change does not quietly take the alphabet with it
        for a third time.
        """
        forms = ("iron-plate", "copper-plate", "steel-plate", "iron-gear-wheel", "iron-stick",
                 "copper-cable", "sulfur")
        for name in forms:
            row = self.rows[name]
            self.assertEqual(
                "ftbmaterials", row["target"].split(":")[0],
                "item-map row %r names %s. ADR-0061 gives every material form to FTB Materials: a "
                "tech mod supplies machines, not material forms" % (name, row["target"]))
            self.assertEqual("borrowed", row["source"],
                             "%r is a material form, and ADR-0061 authors none of them" % name)


class NoIngredientRidesAContestedTag(unittest.TestCase):
    """ADR-0061's suppression rule: unification names the item, because nothing arbitrates tags."""

    def setUp(self):
        self.sources = tag_sources()
        self.assertTrue(self.sources, "no installed jar populates any `c:` item tag")

    def contested(self, tag):
        jars = self.sources.get(tag, set())
        return sorted(jars) if len(jars) > 1 else None

    def test_no_emitted_ingredient_names_a_contested_tag(self):
        for path in live_ingredient_files():
            text = path.read_text(encoding="utf-8")
            for tag in sorted(set(re.findall(r'"#(c:[a-z0-9_/.-]+)"', text))):
                jars = self.contested(tag)
                if jars is None or tag in TAG_WINNERS:
                    continue
                self.fail(
                    "%s names `#%s`, which %d installed mods populate (%s). The ingredient "
                    "accepts all of them, which is not a decision -- name the item (ADR-0061), or "
                    "add a TAG_WINNERS row saying which wins and why"
                    % (path.relative_to(ROOT), tag, len(jars), ", ".join(jars)))

    def test_no_item_map_tag_row_names_a_contested_tag(self):
        rows = json.loads(ITEM_MAP.read_text(encoding="utf-8"))["items"]
        for name, row in rows.items():
            if row.get("kind") != "tag":
                continue
            tag = row["target"]
            if not tag.startswith("c:"):
                continue
            jars = self.contested(tag)
            if jars is None or tag in TAG_WINNERS:
                continue
            self.fail(
                "item-map row %r is the tag `%s`, which %d installed mods populate (%s) -- so "
                "every recipe downstream of this row accepts all of them (ADR-0061)"
                % (name, tag, len(jars), ", ".join(jars)))

    def test_tag_winners_are_not_stale(self):
        for tag in sorted(TAG_WINNERS):
            self.assertIsNotNone(
                self.contested(tag),
                "TAG_WINNERS names `%s`, which is no longer contested. Delete the entry" % tag)


if __name__ == "__main__":
    unittest.main()
