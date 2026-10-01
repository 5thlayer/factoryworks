#!/usr/bin/env python3
"""Assert EMI's index is the Obtainable allowlist, and the mechanic list is neither dead nor stale.

ADR-0088: EMI lists only what a player can come to hold. `scripts/jar-registry-extract.py` writes
what the jars register to `data/jars/`, and `scripts/build-obtainable-index.py` derives the
allowlist from committed files alone. Both `--check`s run here. A mechanic-list row is the one hand-
kept source, so each must name something registered, and none may already be derived: a row the
derivation covers would outlive the reason it was written (#453).

The drops of the live worldgen's blocks are held by the ids #454 names, and to terrain and logs
alone: a plant the live worldgen places drops nothing (ADR-0092), so a new one fails here until its
loot table is replaced. No block only a parked body places is a source (#454).
"""
import importlib.util
import json
import re
import subprocess
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
EXTRACTOR = ROOT / "scripts/jar-registry-extract.py"
GENERATOR = ROOT / "scripts/build-obtainable-index.py"
KUBEJS = ROOT / "kubejs"
KIT = ROOT / "mod/src/main/java/com/factoryworks/core/start/StartingKit.java"
PARKED = KUBEJS / "parked/data"
PACK = "factoryworks"
TERRAIN_AND_LOGS = {"item:minecraft:" + name for name in (
    "dirt", "sand", "red_sand", "gravel", "sandstone", "red_sandstone", "cobblestone",
    "oak_log", "birch_log", "acacia_log")}


def load_generator():
    spec = importlib.util.spec_from_file_location("build_obtainable_index", GENERATOR)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def pack_registered():
    """The pack's own ids, which the corpus leaves out: its item definitions, lang and KubeJS creates.

    A `block.` key is not read, since a block with no item (a rig part, a fluid block) has one.
    """
    keys = set()
    for path in KUBEJS.glob("assets/%s/lang/*.json" % PACK):
        keys |= set(json.loads(path.read_text(encoding="utf-8")))
    items = {"item:%s:%s" % (PACK, path.stem) for path in KUBEJS.glob("assets/%s/items/*.json" % PACK)}
    items |= {"item:%s:%s" % (PACK, found[1])
              for key in keys if (found := re.fullmatch(r"item\.%s\.([a-z0-9_]+)" % PACK, key))}
    fluids = {"fluid:%s:%s" % (PACK, found[1])
              for key in keys if (found := re.fullmatch(r"fluid_type\.%s\.([a-z0-9_]+)" % PACK, key))}
    for path in KUBEJS.glob("startup_scripts/*.js"):
        items |= {"item:" + created for created in re.findall(
            r"event\.create\('(%s:[a-z0-9_/]+)'" % PACK, path.read_text(encoding="utf-8"))}
    return items | fluids


def registered():
    corpus = set()
    for kind, field in (("item", "items"), ("fluid", "fluids")):
        data = json.loads((ROOT / ("data/jars/%s.json" % kind)).read_text(encoding="utf-8"))
        corpus |= {"%s:%s" % (kind, found) for found in data[field]}
    return corpus | pack_registered()


def mechanic_failures(rows, derived, known):
    failures = []
    for row in rows:
        stack = row.get("id", "")
        if set(row) != {"id", "mechanic", "why", "owner"} or not re.fullmatch(r"ADR-\d{4}", str(row["owner"])):
            failures.append("%s: a row is exactly {id, mechanic, why, owner}, owner an ADR-00NN" % stack)
        elif not list((ROOT / "docs/adr").glob("%s-*.md" % row["owner"][4:])):
            failures.append("%s: its owner %s names no ADR" % (stack, row["owner"]))
        if stack not in known:
            failures.append("%s names nothing a jar or the pack registers" % stack)
        if stack in derived:
            failures.append("%s is already Obtainable by derivation -- delete the row" % stack)
    return failures


class MechanicFailures(unittest.TestCase):
    ROW = {"id": "item:minecraft:raw_iron", "mechanic": "m", "why": "w", "owner": "ADR-0041"}

    def test_a_row_naming_nothing_registered_fails(self):
        self.assertTrue(mechanic_failures([self.ROW], set(), {"item:minecraft:stone"}))

    def test_a_row_already_derived_fails(self):
        known = {self.ROW["id"]}
        self.assertTrue(mechanic_failures([self.ROW], known, known))

    def test_a_row_naming_no_adr_fails(self):
        row = dict(self.ROW, owner="ADR-9999")
        self.assertTrue(mechanic_failures([row], set(), {row["id"]}))

    def test_a_live_row_passes(self):
        self.assertEqual([], mechanic_failures([self.ROW], set(), {self.ROW["id"]}))


# The Pack's own namespace and its Libraries', whose items the Pack's Bindings test (ADR-0105).
CREATIVE_NAMESPACES = {PACK} | {row["mod"] for row in json.loads(
    (ROOT / "data/pack/local-jars.json").read_text())["jars"] if row["group"] == "io.github.5thlayer"}


def creative_failures(rows, derived, known):
    failures = []
    for row in rows:
        stack, why = row.get("id", ""), row.get("why")
        if set(row) != {"id", "why"} or not isinstance(why, str) or not why.strip():
            failures.append("%s: a row is exactly {id, why}, with a reason" % stack)
        if stack.split(":")[0] not in CREATIVE_NAMESPACES or "item:" + stack not in known:
            failures.append("%s names no item the Pack or one of its Libraries registers" % stack)
        if "item:" + stack in derived:
            failures.append("%s is already Obtainable -- delete the row" % stack)
    return failures


def strings(node):
    if isinstance(node, str):
        yield node
    elif isinstance(node, dict):
        for value in node.values():
            yield from strings(value)
    elif isinstance(node, list):
        for value in node:
            yield from strings(value)


class CreativeFailures(unittest.TestCase):
    ROW = {"id": "wireworks:creative_electric_pole", "why": "w"}
    KNOWN = {"item:wireworks:creative_electric_pole"}

    def test_a_live_row_passes(self):
        self.assertEqual([], creative_failures([self.ROW], set(), self.KNOWN))

    def test_a_foreign_or_unregistered_row_fails(self):
        self.assertTrue(creative_failures([self.ROW], set(), set()))
        row = {"id": "oritech:creative_thing", "why": "w"}
        self.assertTrue(creative_failures([row], set(), {"item:oritech:creative_thing"}))

    def test_a_row_without_a_reason_fails(self):
        self.assertTrue(creative_failures([dict(self.ROW, why=" ")], set(), self.KNOWN))

    def test_an_obtainable_row_fails(self):
        self.assertTrue(creative_failures([self.ROW], self.KNOWN, self.KNOWN))


class LootRule(unittest.TestCase):
    SHEARS = {"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}}
    SILK = {"condition": "minecraft:match_tool", "predicate": {"predicates": {
        "minecraft:enchantments": [{"enchantments": "minecraft:silk_touch"}]}}}

    @classmethod
    def setUpClass(cls):
        cls.generator = load_generator()

    def drops(self, entry, held=frozenset()):
        return self.generator.table_drops([{"entries": [entry]}], set(held))

    def alternatives(self, first):
        return {"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": "minecraft:grass_block", "conditions": [first]},
            {"type": "minecraft:item", "name": "minecraft:dirt"}]}

    def test_silk_touch_is_held_by_nothing(self):
        self.assertEqual({"item:minecraft:dirt"}, self.drops(self.alternatives(self.SILK)))

    def test_a_tool_passes_only_when_it_is_obtainable(self):
        self.assertNotIn("item:minecraft:grass_block", self.drops(self.alternatives(self.SHEARS)))
        self.assertEqual({"item:minecraft:grass_block", "item:minecraft:dirt"}, self.drops(
            self.alternatives(self.SHEARS), {"item:minecraft:shears"}))

    def test_an_inverted_tool_passes_bare_handed(self):
        entry = {"type": "minecraft:item", "name": "minecraft:stick",
                 "conditions": [{"condition": "minecraft:inverted", "term": self.SHEARS}]}
        self.assertEqual({"item:minecraft:stick"}, self.drops(entry))


def key(stack):
    """An index entry back as the generator's string key."""
    if isinstance(stack, str):
        return stack
    return "item:" + stack["id"] + json.dumps(stack["componentChanges"], sort_keys=True, separators=(",", ":"))


class ObtainableIndex(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.generator = load_generator()
        cls.index = json.loads(cls.generator.INDEX.read_text(encoding="utf-8"))
        cls.added = [key(entry["stack"]) for entry in cls.index["added"]]

    def run_check(self, script):
        result = subprocess.run([sys.executable, str(script), "--check"],
                                capture_output=True, text=True)
        self.assertEqual(0, result.returncode, (result.stdout + result.stderr).strip())

    def test_the_extractor_is_current(self):
        self.run_check(EXTRACTOR)

    def test_the_index_is_current(self):
        self.run_check(GENERATOR)

    def test_the_generator_reads_no_jar(self):
        source = GENERATOR.read_text(encoding="utf-8")
        self.assertNotIn("zipfile", source)
        self.assertNotIn(".jar", source)

    def test_the_index_is_where_emi_reads(self):
        self.assertEqual(KUBEJS / "assets/emi/index/stacks",
                         self.generator.INDEX.parent)

    def test_the_index_is_an_allowlist(self):
        self.assertEqual(["/.*/"], self.index["filters"])
        self.assertNotIn("disable", self.index)
        self.assertEqual(len(self.added), len(set(self.added)))

    def test_every_recipe_output_and_kit_item_is_listed(self):
        outputs = set()
        for path in (KUBEJS / "data" / PACK / "recipe").rglob("*.json"):
            recipe = json.loads(path.read_text(encoding="utf-8"))
            fluids = recipe.get("fluid_results", [])
            results = recipe.get("results") or ([] if fluids else [recipe["result"]])
            outputs |= {self.generator.stack_key(result) for result in results}
            outputs |= {"fluid:" + fluid["id"] for fluid in fluids}
        kit = {"item:" + item for item in re.findall(
            r'new Entry\("([^"]+)"', KIT.read_text(encoding="utf-8"))}
        self.assertTrue(outputs and kit)
        self.assertEqual(set(), (outputs | kit) - set(self.added))

    def test_every_listed_stack_is_registered(self):
        self.assertEqual([], sorted({stack.partition("{")[0] for stack in self.added} - registered()))

    def test_the_live_worldgen_drops_what_454_names(self):
        for stack in ("oak_stairs", "birch_stairs", "acacia_stairs", "dirt", "oak_log"):
            self.assertIn("item:minecraft:" + stack, self.added)
        for stack in ("spruce_stairs", "grass_block", "diamond", "oak_leaves", "oak_sapling"):
            self.assertNotIn("item:minecraft:" + stack, self.added)

    def test_only_terrain_and_logs_drop(self):
        sources = json.loads(self.generator.SOURCES.read_text(encoding="utf-8"))["stacks"]
        self.assertEqual(TERRAIN_AND_LOGS, set(sources))

    def test_every_drop_is_listed_with_its_source(self):
        sources = json.loads(self.generator.SOURCES.read_text(encoding="utf-8"))["stacks"]
        self.assertEqual({"minecraft:dirt", "minecraft:grass_block"},
                         {source["broken"] for source in sources["item:minecraft:dirt"]})
        self.assertEqual(set(), set(sources) - set(self.added))

    def test_no_block_only_a_parked_body_places_is_a_source(self):
        live = self.generator.Worldgen([self.generator.LIVE]).walk().blocks
        parked = self.generator.Worldgen([PARKED, self.generator.LIVE]).walk().blocks - live
        self.assertIn("factoryworks:yumako_log", parked)
        sources = json.loads(self.generator.SOURCES.read_text(encoding="utf-8"))["stacks"]
        broken = {source["broken"] for found in sources.values() for source in found}
        self.assertEqual(set(), broken & parked)

    def test_the_mechanic_list_is_live(self):
        self.assertEqual([], mechanic_failures(
            self.generator.mechanic_rows(), self.generator.derived(), registered()))

    def test_the_creative_list_is_live(self):
        self.assertEqual([], creative_failures(
            self.generator.creative_rows(), self.generator.derived(), registered()))

    def test_creative_items_are_listed_and_not_obtainable(self):
        rows = self.generator.creative_rows()
        self.assertTrue(rows)
        for row in rows:
            self.assertIn("item:" + row["id"], self.added)
            self.assertNotIn("item:" + row["id"], self.generator.derived())

    def test_no_pack_recipe_takes_a_creative_item(self):
        creative = {row["id"] for row in self.generator.creative_rows()}
        for path in (KUBEJS / "data" / PACK / "recipe").rglob("*.json"):
            found = creative & set(strings(json.loads(path.read_text(encoding="utf-8"))))
            self.assertEqual(set(), found, str(path.relative_to(ROOT)))


if __name__ == "__main__":
    unittest.main()
