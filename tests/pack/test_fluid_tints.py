#!/usr/bin/env python3
"""Assert Core's five oil and chemistry fluids render in Factorio's colour and name (#277, ADR-0109).

`docs/testing/what-to-check.md`'s "this looks right" claim, as far as a static check reaches it.
Core draws each fluid as a generated sprite under a constant tint, and a missing or wrong tint fails
nowhere else: the fluid loads, flows and fills pipes, and reaches a player as sulfuric acid that
looks like a green potion.

Four seams, none of which launches the game:

1. **The resource is current.** The generator's `--check`, so a re-extracted corpus or a remapped
   row that changes a tint fails until it is re-run.
2. **The colour lands.** Recomputed here from the resource rather than trusted: each fluid's sprite
   average, times the tint that will draw it, is within TOLERANCE of Factorio's `base_color`.
3. **Something draws it.** `OilFluidClient` names the same sprite the generator's table does, and
   `PFFluids` registers the fluid, its flowing form and its block.
4. **The name is Factorio's.** Each fluid's fluid-type and block lang keys say the Factorio name,
   title-cased as the pack's other names are.

And one absence: no `oritech:still_*` fluid is named by a recipe, tag, item-map row or index.

Whether the colours read right in a running client is a human's on delivery.
"""

import importlib.util
import json
import math
import pathlib
import re
import subprocess
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "scripts/build-fluid-tints.py"
RESOURCE = ROOT / "mod/src/main/resources/factoryworks_core/fluid/tints.json"
LANG = ROOT / "kubejs/assets/factoryworks/lang/en_us.json"
CLIENT = ROOT / "mod/src/main/java/com/factoryworks/core/fluid/client/OilFluidClient.java"
PF_FLUIDS = ROOT / "mod/src/main/java/com/factoryworks/core/fluid/PFFluids.java"
CRUDE_SPRITE = ROOT / "kubejs/assets/factoryworks/textures/block/fluid/crude_oil.png"
# Where a recipe, tag, item-map row or index could still name a borrowed fluid.
SHIPPED = ("kubejs/data", "kubejs/assets/emi", "data/pack", "mod/src/main/resources")
BORROWED = re.compile(r"oritech:still_(heavy_oil|naphtha|diesel|biofuel|sulfuric_acid)")


def generator():
    spec = importlib.util.spec_from_file_location("build_fluid_tints", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class FluidTints(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.gen = generator()
        cls.tints = json.loads(RESOURCE.read_text(encoding="utf-8"))

    def test_resource_is_current(self):
        result = subprocess.run([sys.executable, str(SCRIPT), "--check"],
                                capture_output=True, text=True)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_every_fluid_renders_in_factorios_colour(self):
        colours = self.gen.factorio_colours()
        fluids = self.gen.pack_fluids()
        self.assertEqual(set(fluids.values()), set(self.tints),
                         "tints.json should hold exactly the five fluids Core registers")
        for name, fluid in fluids.items():
            sprite, _ = self.gen.SPRITES[name]
            hex_colour = self.tints[fluid]["color"]
            tint = tuple(int(hex_colour[i:i + 2], 16) / 255 for i in (1, 3, 5))
            drawn = self.gen.rendered(self.gen.sprite_average(sprite), tint)
            miss = math.dist(drawn, colours[name])
            self.assertLessEqual(
                miss, self.gen.TOLERANCE,
                "%r (%s) draws as %s, %.2f from Factorio's %s"
                % (name, fluid, tuple(round(c, 2) for c in drawn), miss, colours[name]))

    def test_the_client_draws_the_sprite_the_tint_was_computed_for(self):
        source = CLIENT.read_text(encoding="utf-8")
        drawn = dict(re.findall(r'registerCorpusTinted\(event, "(\w+)", "([\w/]+)"', source))
        self.assertEqual({n.replace("-", "_"): s for n, (s, _) in self.gen.SPRITES.items()}, drawn)

    def test_core_registers_each_fluid_its_block_and_its_flowing_form(self):
        code = PF_FLUIDS.read_text(encoding="utf-8")
        for fluid in self.gen.pack_fluids().values():
            name = fluid.split(":", 1)[1]
            for registered in (name, "flowing_" + name):
                self.assertIn('FLUIDS.register("%s"' % registered, code)
            self.assertIn('BLOCKS.registerBlock("%s"' % name, code)

    def test_every_fluid_is_named_as_factorio_names_it(self):
        lang = json.loads(LANG.read_text(encoding="utf-8"))
        for name, fluid in self.gen.pack_fluids().items():
            path = fluid.split(":", 1)[1]
            wanted = name.replace("-", " ").title()
            for key in ("fluid_type.factoryworks.%s" % path, "block.factoryworks.%s" % path):
                self.assertEqual(wanted, lang.get(key), "%s should be %r" % (key, wanted))

    def test_crude_oil_renders_in_factorios_colour(self):
        source = CLIENT.read_text(encoding="utf-8")
        argb = int(re.search(r"CRUDE_OIL_TINT = 0xFF([0-9A-Fa-f]{6});", source).group(1), 16)
        tint = tuple(((argb >> shift) & 0xFF) / 255 for shift in (16, 8, 0))
        w, _, rows = self.gen.read_png(CRUDE_SPRITE.read_bytes())
        pixels = [px for row in rows[:w] for px in row if px[3] > 0]
        average = tuple(sum(px[i] for px in pixels) / len(pixels) / 255 for i in range(3))
        wanted = self.gen.factorio_colours()["crude-oil"]
        miss = math.dist(self.gen.rendered(average, tint), wanted)
        self.assertLessEqual(miss, self.gen.TOLERANCE,
                             "crude oil draws %.2f from Factorio's %s" % (miss, wanted))

    def test_nothing_shipped_names_an_oritech_fluid(self):
        tracked = subprocess.run(["git", "ls-files", *SHIPPED], cwd=ROOT, capture_output=True,
                                 text=True, check=True).stdout.splitlines()
        for rel in tracked:
            text = (ROOT / rel).read_text(encoding="utf-8", errors="replace")
            found = BORROWED.search(text)
            self.assertIsNone(found, "%s still names %s" % (rel, found and found.group(0)))


if __name__ == "__main__":
    unittest.main()
