#!/usr/bin/env python3
"""Assert the two tags an Electric Network sorts its area by resolve to files (#280, ADR-0062).

A pole decides a block is a generator or an accumulator by block tag. A `TagKey` whose JSON is
missing resolves to an empty tag rather than an error, so a misspelled or deleted file turns every
Steam Engine into a consumer the network tries to fill -- nothing thrown, nothing logged, no power.
The tag names are read out of the source rather than typed here, so a rename fails too.
"""

import json
import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
POLE = ROOT / "mod/src/main/java/com/factoryworks/core/energy/SupplyAreaPoleBlockEntity.java"
TAGS = ROOT / "kubejs/data/factoryworks/tags/block"
TAG_RE = re.compile(r'TagKey\.create\(\s*Registries\.BLOCK,\s*'
                    r'Identifier\.fromNamespaceAndPath\(FactoryWorksCore\.NAMESPACE,\s*"([a-z_]+)"\)')


class NetworkTags(unittest.TestCase):

    def test_every_role_tag_the_pole_names_has_a_file(self):
        names = TAG_RE.findall(POLE.read_text(encoding="utf-8"))
        self.assertEqual(sorted(names), ["accumulators", "generators"],
                         "the pole's role tags have changed; this check names both")
        for name in names:
            with self.subTest(tag=name):
                path = TAGS / f"{name}.json"
                self.assertTrue(path.exists(), f"{path} is missing; the tag resolves empty")
                self.assertIsInstance(json.loads(path.read_text())["values"], list)


if __name__ == "__main__":
    unittest.main()
