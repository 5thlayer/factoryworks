#!/usr/bin/env python3
"""Assert the Pack fills Wireworks' generators tag with the Steam Engine (#476, #617, ADR-0062), and
Pipeworks' closes_sides tag with the Picks (ADR-0125).

A pole decides a block is a generator by Wireworks' block tag. Wireworks ships it empty of the
Pack's blocks, so without the Pack's file every Steam Engine is a consumer the network tries to
fill: nothing thrown, nothing logged, no power. Only an anchor is listed, since a part resolves to
its anchor before the tag is asked. Wireworks tags its own solar panel and accumulator, so the
accumulators tag is not the Pack's.
"""

import json
import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
TAGS = ROOT / "kubejs/data/wireworks/tags/block"
CLOSES_SIDES = ROOT / "kubejs/data/pipeworks/tags/item/closes_sides.json"
PICKS = {"factoryworks:engineers_iron_pick", "factoryworks:engineers_steel_pick"}

ROLES = {
    "generators": {"factoryworks:steam_engine"},
}


class NetworkTags(unittest.TestCase):

    def test_the_pack_ships_no_accumulators_tag(self):
        self.assertFalse((TAGS / "accumulators.json").exists())

    def test_each_role_tag_holds_the_packs_anchors(self):
        for name, anchors in ROLES.items():
            with self.subTest(tag=name):
                path = TAGS / f"{name}.json"
                self.assertTrue(path.exists(), f"{path} is missing; Wireworks' tag stays empty")
                self.assertEqual(set(json.loads(path.read_text())["values"]), anchors)

    # Pipeworks ships the tag empty, so without the Pack's file no player can part two pipes
    # beside an Assembler's neighbouring Fluid Connections (ADR-0125).
    def test_the_picks_close_pipe_sides(self):
        self.assertEqual(set(json.loads(CLOSES_SIDES.read_text())["values"]), PICKS)


if __name__ == "__main__":
    unittest.main()
