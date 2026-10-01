#!/usr/bin/env python3
"""Assert the Pack fills Wireworks' two role tags with its generators and accumulator (#476, ADR-0062).

A pole decides a block is a generator or an accumulator by Wireworks' block tags. Wireworks ships
both empty, so without the Pack's files every Steam Engine is a consumer the network tries to fill:
nothing thrown, nothing logged, no power. Only an anchor is listed, since a part resolves to its
anchor before the tag is asked.
"""

import json
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
TAGS = ROOT / "kubejs/data/wireworks/tags/block"

ROLES = {
    "generators": {"factoryworks:steam_engine", "factoryworks:solar_panel"},
    "accumulators": {"factoryworks:accumulator"},
}


class NetworkTags(unittest.TestCase):

    def test_each_role_tag_holds_the_packs_anchors(self):
        for name, anchors in ROLES.items():
            with self.subTest(tag=name):
                path = TAGS / f"{name}.json"
                self.assertTrue(path.exists(), f"{path} is missing; Wireworks' tag stays empty")
                self.assertEqual(set(json.loads(path.read_text())["values"]), anchors)


if __name__ == "__main__":
    unittest.main()
