#!/usr/bin/env python3
"""Assert every item the Showcase's starting kit grants resolves (ADR-0127).

`docs/testing/what-to-check.md`'s "cross-file references resolve" claim. The kit is a list of id
strings in `kubejs/server_scripts/starting_kit.js`, and a string that names nothing reaches the
player as an empty slot with nothing logged. A `minecraft:` id is vanilla's and needs no jar; every
other id is checked against the item definition its mod's installed jar ships.
"""

import pathlib
import re
import unittest
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "kubejs/server_scripts/starting_kit.js"
MODS = ROOT / "mods"
SUITE = {"minecraft", "beltworks", "craftworks", "wireworks", "pipeworks"}

ENTRY = re.compile(r"\['([a-z0-9_.-]+):([a-z0-9_./-]+)',\s*(\d+)\]")


def kit():
    source = SCRIPT.read_text(encoding="utf-8")
    body = re.search(r"const STARTING_KIT = \[(.*?)\n\]", source, re.DOTALL)
    assert body is not None, "STARTING_KIT in starting_kit.js has moved or changed shape"
    return [(ns, path, int(count)) for ns, path, count in ENTRY.findall(body.group(1))]


def jar_files(namespace):
    """Every file name in the installed jars that ship a lang file for this namespace."""
    names = set()
    for jar in sorted(MODS.glob("*.jar")):
        with zipfile.ZipFile(jar) as archive:
            found = archive.namelist()
            if "assets/%s/lang/en_us.json" % namespace in found:
                names |= set(found)
    return names


class StartingKit(unittest.TestCase):
    def test_the_kit_is_not_empty(self):
        self.assertTrue(kit())

    def test_every_count_is_positive(self):
        for ns, path, count in kit():
            self.assertGreaterEqual(count, 1, "%s:%s is granted %d" % (ns, path, count))

    def test_the_kit_names_only_the_suite_and_vanilla(self):
        for ns, path, _ in kit():
            self.assertIn(ns, SUITE, "%s:%s is outside the suite" % (ns, path))

    def test_every_non_vanilla_item_is_defined_by_an_installed_jar(self):
        for ns, path, _ in kit():
            if ns == "minecraft":
                continue
            names = jar_files(ns)
            self.assertTrue(names, "no installed jar ships assets for %r (is mods/ populated?)" % ns)
            self.assertIn("assets/%s/items/%s.json" % (ns, path), names,
                          "the starting kit grants %s:%s, which no installed jar defines" % (ns, path))


if __name__ == "__main__":
    unittest.main()
