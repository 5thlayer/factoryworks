#!/usr/bin/env python3
"""Assert EMI's index is the Obtainable allowlist, and the mechanic list is neither dead nor stale.

ADR-0088: EMI lists only what a player can come to hold. `scripts/jar-registry-extract.py` writes
what the jars register to `data/jars/`, and `scripts/build-obtainable-index.py` derives the
allowlist from committed files alone. Both `--check`s run here. A mechanic-list row is the one hand-
kept source, so each must name something registered, and none may already be derived: a row the
derivation covers would outlive the reason it was written (#453).
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
KIT = ROOT / "mod/src/main/java/com/planetaryfactory/core/start/StartingKit.java"
PACK = "planetaryfactory"


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
        if set(row) != {"id", "mechanic", "why", "ticket"} or not isinstance(row["ticket"], int):
            failures.append("%s: a row is exactly {id, mechanic, why, ticket}, ticket a number" % stack)
        if stack not in known:
            failures.append("%s names nothing a jar or the pack registers" % stack)
        if stack in derived:
            failures.append("%s is already Obtainable by derivation -- delete the row" % stack)
    return failures


class MechanicFailures(unittest.TestCase):
    ROW = {"id": "item:minecraft:raw_iron", "mechanic": "m", "why": "w", "ticket": 1}

    def test_a_row_naming_nothing_registered_fails(self):
        self.assertTrue(mechanic_failures([self.ROW], set(), {"item:minecraft:stone"}))

    def test_a_row_already_derived_fails(self):
        known = {self.ROW["id"]}
        self.assertTrue(mechanic_failures([self.ROW], known, known))

    def test_a_live_row_passes(self):
        self.assertEqual([], mechanic_failures([self.ROW], set(), {self.ROW["id"]}))


class ObtainableIndex(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.generator = load_generator()
        cls.index = json.loads(cls.generator.INDEX.read_text(encoding="utf-8"))
        cls.added = [entry["stack"] for entry in cls.index["added"]]

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
            results = recipe.get("results") or [recipe["result"]]
            outputs |= {"item:" + result["id"] for result in results}
        kit = {"item:" + item for item in re.findall(
            r'new Entry\("([^"]+)"', KIT.read_text(encoding="utf-8"))}
        self.assertTrue(outputs and kit)
        self.assertEqual(set(), (outputs | kit) - set(self.added))

    def test_every_listed_stack_is_registered(self):
        self.assertEqual([], sorted(set(self.added) - registered()))

    def test_the_mechanic_list_is_live(self):
        self.assertEqual([], mechanic_failures(
            self.generator.mechanic_rows(), self.generator.derived(), registered()))


if __name__ == "__main__":
    unittest.main()
