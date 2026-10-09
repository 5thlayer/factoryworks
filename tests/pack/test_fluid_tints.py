#!/usr/bin/env python3
"""Assert Core's five oil and chemistry fluids are drawn, registered and named as Factorio names them (#277, ADR-0109).

`docs/testing/what-to-check.md`'s "this looks right" claim, as far as a static check reaches it.
Core draws each fluid as a generated sprite under a constant tint, and a missing or wrong tint fails
nowhere else: the fluid loads, flows and fills pipes, and reaches a player as sulfuric acid that
looks like a green potion.

`fluid/tints.json` and the two sprites are hand-owned data (#599). Three seams, none of which
launches the game:

1. **Something draws it.** `OilFluidClient` registers a sprite for each fluid in the resource, and
   `PFFluids` registers the fluid, its flowing form and its block.
2. **The name is Factorio's.** Each fluid's fluid-type and block lang keys say the Factorio name,
   title-cased as the pack's other names are.
3. **The resource names the five fluids.** Its keys are the five Core fluids.

And one absence: no `oritech:still_*` fluid is named by a recipe, tag, item-map row or index.

Whether the colours read right in a running client is a human's on delivery.
"""

import json
import pathlib
import re
import subprocess
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
RESOURCE = ROOT / "mod/src/main/resources/factoryworks_core/fluid/tints.json"
LANG = ROOT / "kubejs/assets/factoryworks/lang/en_us.json"
CLIENT = ROOT / "mod/src/main/java/com/factoryworks/core/fluid/client/OilFluidClient.java"
PF_FLUIDS = ROOT / "mod/src/main/java/com/factoryworks/core/fluid/PFFluids.java"
# Where a recipe, tag, item-map row or index could still name a borrowed fluid.
SHIPPED = ("kubejs/data", "kubejs/assets/emi", "data/pack", "mod/src/main/resources")
BORROWED = re.compile(r"oritech:still_(heavy_oil|naphtha|diesel|biofuel|sulfuric_acid)")


class FluidTints(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.tints = json.loads(RESOURCE.read_text(encoding="utf-8"))
        cls.fluids = {row["factorio"]: fluid for fluid, row in cls.tints.items()}

    def test_the_resource_holds_exactly_the_five_fluids(self):
        self.assertEqual(
            {"heavy-oil", "light-oil", "petroleum-gas", "lubricant", "sulfuric-acid"}, set(self.fluids))
        for name, fluid in self.fluids.items():
            self.assertEqual("factoryworks:" + name.replace("-", "_"), fluid)

    def test_the_client_draws_each_fluid(self):
        source = CLIENT.read_text(encoding="utf-8")
        drawn = set(re.findall(r'registerCorpusTinted\(event, "(\w+)"', source))
        self.assertEqual({fluid.split(":", 1)[1] for fluid in self.fluids.values()}, drawn)

    def test_core_registers_each_fluid_its_block_and_its_flowing_form(self):
        code = PF_FLUIDS.read_text(encoding="utf-8")
        for fluid in self.fluids.values():
            name = fluid.split(":", 1)[1]
            for registered in (name, "flowing_" + name):
                self.assertIn('FLUIDS.register("%s"' % registered, code)
            self.assertIn('BLOCKS.registerBlock("%s"' % name, code)

    def test_every_fluid_is_named_as_factorio_names_it(self):
        lang = json.loads(LANG.read_text(encoding="utf-8"))
        for name, fluid in self.fluids.items():
            path = fluid.split(":", 1)[1]
            wanted = name.replace("-", " ").title()
            for key in ("fluid_type.factoryworks.%s" % path, "block.factoryworks.%s" % path):
                self.assertEqual(wanted, lang.get(key), "%s should be %r" % (key, wanted))

    def test_nothing_shipped_names_an_oritech_fluid(self):
        tracked = subprocess.run(["git", "ls-files", *SHIPPED], cwd=ROOT, capture_output=True,
                                 text=True, check=True).stdout.splitlines()
        for rel in tracked:
            text = (ROOT / rel).read_text(encoding="utf-8", errors="replace")
            found = BORROWED.search(text)
            self.assertIsNone(found, "%s still names %s" % (rel, found and found.group(0)))


if __name__ == "__main__":
    unittest.main()
