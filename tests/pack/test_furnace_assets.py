#!/usr/bin/env python3
"""Assert every furnace tier the mod registers has the pack-side files it needs (#155).

`docs/testing/what-to-check.md`'s "cross-file references resolve" claim, for the furnace seam.

The furnace is split across the boundary ADR-0015 draws: `planetaryfactory_core` registers the
blocks because a fuel slot and a recipe type are mechanism, and everything a designer would tune --
model, texture, name, drop -- is data under `kubejs/`. Each way of breaking the two halves apart
fails quietly: a missing blockstate or model renders the black-and-magenta cube with a client-side
warning, a missing texture does the same one hop down, a missing lang key ships the raw key as the
block's name, and a missing loot table makes the block break into nothing.

Two things here are specific to this ladder rather than generic asset plumbing:

  - **The Electric tier looks unlike the burner tiers.** Its point is that it carries no fuel, and a
    player who cannot tell it from a Steel Furnace at a glance has lost the one thing the block
    says about itself (#324). So no texture it names may be one a burner tier names.
  - **The Electric tier's art is borrowed, and every borrowed file is credited.** Its textures are
    copied from a CC BY-NC-SA repository into the pack's own namespace (#324), and that licence
    holds only with attribution, so each one is asserted to be named in `NOTICE`. A copy nobody
    credited fails nothing else and is still a licence breach. The art used to be read out of the
    GTCEu jar, and a `loader` assertion guarded against copying GregTech's model with it; with the
    art in our namespace there is no foreign model to copy, and that assertion went with GregTech.
  - **Every tier has a lit variant.** The `LIT` blockstate drives the lit front on all three,
    including the Electric one, where it is the only thing telling "running" from "waiting for the
    pole" from outside.

The tier list is read out of `FurnaceTier.java`, so a fourth tier fails this check rather than
shipping without assets.
"""

import json
import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
FURNACE_TIER = ROOT / "mod/src/main/java/com/planetaryfactory/core/smelting/FurnaceTier.java"
ASSETS = ROOT / "kubejs/assets/planetaryfactory"
DATA = ROOT / "kubejs/data/planetaryfactory"
NOTICE = ROOT / "NOTICE"

# `STONE(1.0f, true),` -- the enum constant, whatever its arguments are.
TIER_RE = re.compile(r"^\s{4}([A-Z][A-Z_]*)\([^)]*\)[,;]", re.MULTILINE)


def registered_tiers():
    tiers = TIER_RE.findall(FURNACE_TIER.read_text(encoding="utf-8"))
    if not tiers:
        raise AssertionError(f"no furnace tiers parsed out of {FURNACE_TIER} -- has the enum moved?")
    return [name.lower() + "_furnace" for name in tiers]


def texture_exists(texture):
    namespace, path = texture.split(":", 1)
    if namespace == "planetaryfactory":
        return (ASSETS / "textures" / f"{path}.png").is_file()
    # Vanilla ships no jar here to read; a vanilla path is taken on the same trust as every vanilla
    # parent in these models.
    return namespace == "minecraft"


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
                self.assertTrue(blockstate.is_file(), f"{blockstate} is missing")
                variants = json.loads(blockstate.read_text(encoding="utf-8"))["variants"]
                expected = {f"facing={facing},lit={lit}"
                            for facing in ("north", "east", "south", "west")
                            for lit in ("false", "true")}
                self.assertEqual(expected, set(variants),
                                 "a state with no variant renders as a missing model")
                for variant in variants.values():
                    model = variant["model"]
                    self.assertTrue(model.startswith("planetaryfactory:"), model)
                    path = ASSETS / "models" / (model.split(":", 1)[1] + ".json")
                    self.assertTrue(path.is_file(), f"{blockstate} names {model}, which is missing")

    def test_every_model_names_textures_that_exist(self):
        for name in self.tiers:
            for suffix in ("", "_on"):
                with self.subTest(furnace=name + suffix):
                    path = ASSETS / "models" / "block" / f"{name}{suffix}.json"
                    self.assertTrue(path.is_file(), f"{path} is missing")
                    model = json.loads(path.read_text(encoding="utf-8"))
                    for texture in model.get("textures", {}).values():
                        self.assertTrue(texture_exists(texture),
                                        f"{path.name} names {texture}, which does not exist")

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
                self.assertEqual("planetaryfactory", namespace,
                                 "the Electric tier's art is copied into the pack's namespace")
                file = (ASSETS / "textures" / f"{path}.png").relative_to(ROOT).as_posix()
                self.assertIn(file, notice, f"{file} is borrowed art with no credit in NOTICE")

    def test_the_item_model_resolves_to_a_model_that_exists(self):
        for name in self.tiers:
            with self.subTest(furnace=name):
                path = ASSETS / "models" / "item" / f"{name}.json"
                self.assertTrue(path.is_file(), f"{path} is missing")
                parent = json.loads(path.read_text(encoding="utf-8"))["parent"]
                self.assertTrue(parent.startswith("planetaryfactory:"), parent)
                self.assertTrue((ASSETS / "models" / (parent.split(":", 1)[1] + ".json")).is_file(),
                                f"the item model names {parent}, which is missing")

    def test_every_tier_is_named(self):
        lang = json.loads((ASSETS / "lang" / "en_us.json").read_text(encoding="utf-8"))
        for name in self.tiers:
            with self.subTest(furnace=name):
                key = f"block.planetaryfactory.{name}"
                self.assertIn(key, lang, f"{key} has no translation, so the block shows its key")
                self.assertTrue(lang[key].strip(), f"{key} is blank")

    def test_every_tier_drops_itself(self):
        for name in self.tiers:
            with self.subTest(furnace=name):
                path = DATA / "loot_table" / "blocks" / f"{name}.json"
                self.assertTrue(path.is_file(),
                                f"{path} is missing, so the furnace breaks into nothing")
                table = json.loads(path.read_text(encoding="utf-8"))
                dropped = {entry["name"] for pool in table["pools"] for entry in pool["entries"]
                           if entry.get("type") == "minecraft:item"}
                self.assertEqual({f"planetaryfactory:{name}"}, dropped)


if __name__ == "__main__":
    unittest.main(verbosity=2)
