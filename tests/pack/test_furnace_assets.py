#!/usr/bin/env python3
"""Assert what the furnace ladder's art has to say about each tier (#155, #324).

Whether each tier's blockstate, models, textures, lang key and loot table resolve is
`test_block_assets.py`'s (#254). What stays here is specific to this ladder:

  - **Every tier has a lit variant.** The `LIT` blockstate drives the lit front on all three,
    including the Electric one, where it is the only thing telling "running" from "waiting for the
    pole" from outside.
  - **The Electric tier looks unlike the burner tiers.** Its point is that it carries no fuel, and a
    player who cannot tell it from a Steel Furnace at a glance has lost the one thing the block
    says about itself (#324). So no texture it names may be one a burner tier names.
  - **The Electric tier's art is borrowed, and every borrowed file is credited.** Its textures are
    copied from a CC BY-NC-SA repository into the pack's own namespace (#324), and that licence
    holds only with attribution, so each one is asserted to be named in `NOTICE`.

The tier list is read out of `FurnaceTier.java`.
"""

import json
import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
FURNACE_TIER = ROOT / "mod/src/main/java/com/factoryworks/core/smelting/FurnaceTier.java"
ASSETS = ROOT / "kubejs/assets/factoryworks"
NOTICE = ROOT / "NOTICE"

# `STONE(1.0f, true),` -- the enum constant, whatever its arguments are.
TIER_RE = re.compile(r"^\s{4}([A-Z][A-Z_]*)\([^)]*\)[,;]", re.MULTILINE)


def registered_tiers():
    tiers = TIER_RE.findall(FURNACE_TIER.read_text(encoding="utf-8"))
    if not tiers:
        raise AssertionError(f"no furnace tiers parsed out of {FURNACE_TIER} -- has the enum moved?")
    return [name.lower() + "_furnace" for name in tiers]


def model_textures(name):
    path = ASSETS / "models" / "block" / f"{name}.json"
    return set(json.loads(path.read_text(encoding="utf-8")).get("textures", {}).values())


class FurnaceAssets(unittest.TestCase):
    def setUp(self):
        self.tiers = registered_tiers()

    def test_the_three_shipped_tiers_are_registered(self):
        self.assertEqual(["stone_furnace", "steel_furnace", "electric_furnace"], self.tiers)

    def test_every_tier_has_a_blockstate_covering_both_facing_and_lit(self):
        for name in self.tiers:
            with self.subTest(furnace=name):
                blockstate = ASSETS / "blockstates" / f"{name}.json"
                variants = json.loads(blockstate.read_text(encoding="utf-8"))["variants"]
                expected = {f"facing={facing},lit={lit}"
                            for facing in ("north", "east", "south", "west")
                            for lit in ("false", "true")}
                self.assertEqual(expected, set(variants),
                                 "a state with no variant renders as a missing model")

    def test_the_electric_tier_wears_no_burner_tiers_texture(self):
        burners = set()
        for name in self.tiers:
            if name != "electric_furnace":
                burners |= model_textures(name) | model_textures(name + "_on")
        for suffix in ("", "_on"):
            with self.subTest(model=f"electric_furnace{suffix}"):
                shared = model_textures(f"electric_furnace{suffix}") & burners
                self.assertFalse(shared, f"the Electric tier looks like a burner tier: {shared}")

    def test_every_borrowed_electric_texture_is_credited(self):
        notice = NOTICE.read_text(encoding="utf-8")
        textures = model_textures("electric_furnace") | model_textures("electric_furnace_on")
        for texture in sorted(textures):
            with self.subTest(texture=texture):
                namespace, path = texture.split(":", 1)
                self.assertEqual("factoryworks", namespace,
                                 "the Electric tier's art is copied into the pack's namespace")
                file = (ASSETS / "textures" / f"{path}.png").relative_to(ROOT).as_posix()
                self.assertIn(file, notice, f"{file} is borrowed art with no credit in NOTICE")


if __name__ == "__main__":
    unittest.main(verbosity=2)
