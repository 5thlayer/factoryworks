#!/usr/bin/env python3
"""Assert the Pack ships no Wireworks accumulators tag (ADR-0062).

Wireworks tags its own solar panel and accumulator, so the accumulators tag is not the Pack's.
"""

import pathlib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
TAGS = ROOT / "kubejs/data/wireworks/tags/block"


class NetworkTags(unittest.TestCase):

    def test_the_pack_ships_no_accumulators_tag(self):
        self.assertFalse((TAGS / "accumulators.json").exists())


if __name__ == "__main__":
    unittest.main()
