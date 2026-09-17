#!/usr/bin/env python3
"""Assert every fluid the pack borrows from Oritech renders in Factorio's colour (#277, ADR-0067).

`docs/testing/what-to-check.md`'s "this looks right" claim, as far as a static check reaches it.
A borrowed fluid keeps Oritech's id and Oritech's tint unless `scripts/build-fluid-tints.py`
retints it, and a missing or wrong retint fails nowhere else: the fluid loads, flows and fills
pipes, and reaches a player as sulfuric acid that looks like a green potion.

Three seams, none of which launches the game:

1. **The resource is current.** The generator's `--check`, so a re-extracted corpus or a remapped
   row that changes a tint fails until it is re-run.
2. **The colour lands.** Recomputed here from the resource rather than trusted: each borrowed
   fluid's sprite average, times the tint that will actually draw it -- the resource's where it has
   one, Oritech's otherwise -- is within TOLERANCE of Factorio's `base_color`. A retint that clamps
   short of the target fails here rather than shipping a colour nobody looked at.
3. **Something applies it.** The mixin is listed on the client side of the Oritech mixin config
   and its target class is still in the installed jar; without either, the resource is read by
   nothing and every fluid keeps Oritech's colour with no error.

4. **The name is Factorio's too.** Every lang key the Oritech jar names a borrowed fluid under -- the
   fluid, its fluid type, its bucket and its source block -- is overridden in
   `kubejs/assets/oritech/lang/en_us.json` to the Factorio name, title-cased as the pack's other
   renames are. A key left out shows the player "Biofuel" in one tooltip and "Lubricant" in the next.

Whether the colours read right in a running client is a human's on delivery.
"""

import importlib.util
import json
import math
import pathlib
import subprocess
import sys
import unittest
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "scripts/build-fluid-tints.py"
RESOURCE = ROOT / "mod/src/main/resources/planetaryfactory_core/fluid/tints.json"
MIXINS = ROOT / "mod/src/main/resources/planetaryfactory_core.oritech.mixins.json"
LANG = ROOT / "kubejs/assets/oritech/lang/en_us.json"
MIXIN = "FluidModelContentMixin"
MIXIN_TARGET = "rearth/oritech/client/init/FluidModelContent.class"


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

    def test_every_borrowed_fluid_renders_in_factorios_colour(self):
        colours = self.gen.factorio_colours()
        borrowed = self.gen.borrowed_fluids()
        self.assertTrue(borrowed, "the item map borrows no Oritech fluid -- nothing to check")
        with zipfile.ZipFile(self.gen.oritech_jar()) as archive:
            for name, target in borrowed.items():
                self.assertIn(target, self.gen.ORITECH_MODELS,
                              "%r borrows %s, whose sprite and tint nobody read" % (name, target))
                sprite, tint = self.gen.ORITECH_MODELS[target]
                if target in self.tints:
                    hex_colour = self.tints[target]["color"]
                    tint = tuple(int(hex_colour[i:i + 2], 16) / 255 for i in (1, 3, 5))
                drawn = self.gen.rendered(self.gen.sprite_average(archive, sprite), tint)
                miss = math.dist(drawn, colours[name])
                self.assertLessEqual(
                    miss, self.gen.TOLERANCE,
                    "%r (%s) draws as %s, %.2f from Factorio's %s"
                    % (name, target, tuple(round(c, 2) for c in drawn), miss, colours[name]))

    def test_no_tint_names_a_fluid_the_map_does_not_borrow(self):
        borrowed = set(self.gen.borrowed_fluids().values())
        for target in self.tints:
            self.assertIn(target, borrowed, "tints.json retints %s, which no row borrows" % target)

    def test_every_borrowed_fluid_is_named_as_factorio_names_it(self):
        with zipfile.ZipFile(self.gen.oritech_jar()) as archive:
            oritech = json.loads(archive.read("assets/oritech/lang/en_us.json"))
        overrides = json.loads(LANG.read_text(encoding="utf-8")) if LANG.exists() else {}
        for name, target in self.gen.borrowed_fluids().items():
            path = target.split(":", 1)[1]
            base = path.removeprefix("still_")
            keys = ["fluid.oritech.%s" % path, "fluid_type.oritech.%s_fluid_type" % base,
                    "item.oritech.%s_bucket" % path, "block.oritech.%s_block" % path]
            wanted = name.replace("-", " ").title()
            for key in keys:
                if key not in oritech:
                    continue
                suffix = " Bucket" if key.startswith("item.") else ""
                self.assertEqual(
                    wanted + suffix, overrides.get(key),
                    "%s is Oritech's %r; the pack borrows it as Factorio's %r, so %s must say so"
                    % (key, oritech[key], name, LANG.relative_to(ROOT)))

    def test_the_mixin_is_wired_on_the_client(self):
        config = json.loads(MIXINS.read_text(encoding="utf-8"))
        self.assertIn(MIXIN, config.get("client", []),
                      "the tint mixin is not on the client side of %s, so nothing reads tints.json"
                      % MIXINS.name)
        with zipfile.ZipFile(self.gen.oritech_jar()) as archive:
            self.assertIn(MIXIN_TARGET, archive.namelist(),
                          "Oritech no longer ships %s; the mixin applies to nothing" % MIXIN_TARGET)


if __name__ == "__main__":
    unittest.main()
