#!/usr/bin/env python3
"""Assert what the pole ladder's files have to say beyond resolving (ADR-0036, #147).

Whether each pole's blockstate, models, textures, lang key and loot table resolve is
`test_block_assets.py`'s (#254). What stays here is the ladder's own: the tiers are Factorio's
supply areas, the dropped big pole leaves nothing behind, every key the pole's tooltip and Jade
line translate exists and takes the arguments it is given, and the creative pole is a derived
sprite, not a tier, and craftable nowhere.

The tier list is read out of `PoleTier.java`. A tier *removed* from the enum leaves files no tier
list reaches, so those are asserted absent by name.
"""

import json
import pathlib
import re
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
POLE_TIER = ROOT / "mod/src/main/java/com/planetaryfactory/core/energy/PoleTier.java"
CREATIVE_POLE = ROOT / "mod/src/main/java/com/planetaryfactory/core/energy/CreativeSupplyAreaPoleBlock.java"
TEXTURE_SCRIPT = ROOT / "scripts/build-creative-pole-texture.py"
TRANSLATING_SOURCES = (
    ROOT / "mod/src/main/java/com/planetaryfactory/core/energy/SupplyAreaPoleItem.java",
    ROOT / "mod/src/main/java/com/planetaryfactory/core/compat/PoleJadePlugin.java",
)
ASSETS = ROOT / "kubejs/assets/planetaryfactory"
DATA = ROOT / "kubejs/data/planetaryfactory"

# `SMALL(5),` -- the enum constant and its Factorio supply size.
TIER_RE = re.compile(r"^\s{4}([A-Z][A-Z_]*)\((\d+)(?:, [\d.]+)?\)[,;]", re.MULTILINE)


def registered_tiers():
    source = POLE_TIER.read_text(encoding="utf-8")
    tiers = TIER_RE.findall(source)
    if not tiers:
        raise AssertionError(f"no pole tiers parsed out of {POLE_TIER} -- has the enum moved?")
    return {name.lower() + "_electric_pole": int(size) for name, size in tiers}


def creative_pole_name():
    """The creative pole's registry path: a pole, but deliberately not a `PoleTier` (#272)."""
    source = CREATIVE_POLE.read_text(encoding="utf-8")
    found = re.search(r'BLOCK_NAME\s*=\s*"([a-z_]+)"', source)
    if not found:
        raise AssertionError(f"no BLOCK_NAME parsed out of {CREATIVE_POLE} -- has it moved?")
    return found.group(1)


def translatable_calls(source):
    """Every `Component.translatable("key", arg, ...)` in a Java source, as (key, argument count).

    Parsed rather than regexed because the pole's own tooltip nests one `Component.translatable`
    inside another's arguments, and a regex that stops at the first `)` would read the inner call's
    arguments as the outer call's.
    """
    calls = []
    marker = "Component.translatable("
    start = source.find(marker)
    while start != -1:
        i = start + len(marker)
        depth, args, current = 1, [], []
        while depth > 0:
            char = source[i]
            if char in "([":
                depth += 1
            elif char in ")]":
                depth -= 1
                if depth == 0:
                    break
            if char == "," and depth == 1:
                args.append("".join(current).strip())
                current = []
            else:
                current.append(char)
            i += 1
        args.append("".join(current).strip())
        key = args[0].strip()
        if key.startswith('"') and key.endswith('"'):
            calls.append((key[1:-1], len(args) - 1))
        start = source.find(marker, i)
    return calls


class PoleAssets(unittest.TestCase):
    def setUp(self):
        self.tiers = registered_tiers()

    def test_the_three_shipped_tiers_are_registered(self):
        self.assertEqual(
            {
                "small_electric_pole": 5,
                "medium_electric_pole": 7,
                "substation_electric_pole": 18,
            },
            self.tiers,
            "the supply areas are Factorio's own; Factorio's fourth tier, the big pole, is "
            "deliberately absent (see PoleTier)",
        )

    def test_the_dropped_big_pole_leaves_no_files_behind(self):
        # A tier is a name spread over seven files in three trees, and dropping one is seven
        # deletions with nothing checking that they all happened. A blockstate, model, texture or
        # lang key for a block nothing registers is inert -- it ships, and nothing reports it. The
        # recipe JSON is worse: it is generated, so a stale one comes back as a converter diff on
        # whoever next runs it rather than as a failure here. Named rather than generalised over
        # every unregistered pole, because this asserts one decision, not a rule about files.
        stale = sorted(
            path.relative_to(ROOT).as_posix()
            for tree in (ASSETS, DATA)
            for path in tree.rglob("*big_electric_pole.*")
        )
        self.assertEqual([], stale, "the big pole is dropped, so nothing may still name it")

        lang = json.loads((ASSETS / "lang" / "en_us.json").read_text(encoding="utf-8"))
        self.assertNotIn("block.planetaryfactory.big_electric_pole", lang)

    def test_every_translation_key_the_pole_uses_exists(self):
        # The pole is the only block in the pack that explains itself -- no cable to trace, no GUI,
        # no recipe that hints at reach -- so a missing key here is not a cosmetic blemish. It ships
        # the raw key where the explanation should be, and nothing logs it.
        lang = json.loads((ASSETS / "lang" / "en_us.json").read_text(encoding="utf-8"))
        for source in TRANSLATING_SOURCES:
            for key, _ in translatable_calls(source.read_text(encoding="utf-8")):
                if not key.startswith("tooltip.planetaryfactory."):
                    continue
                with self.subTest(source=source.name, key=key):
                    self.assertIn(key, lang, f"{source.name} translates {key}, which has no entry")
                    self.assertTrue(lang[key].strip(), f"{key} is blank")

    def test_every_translation_key_takes_the_arguments_it_is_given(self):
        # A key whose `%s` count disagrees with the Java crashes the client formatting it, at the
        # moment a player hovers the item. Nothing at build time compares the two sides.
        lang = json.loads((ASSETS / "lang" / "en_us.json").read_text(encoding="utf-8"))
        for source in TRANSLATING_SOURCES:
            for key, count in translatable_calls(source.read_text(encoding="utf-8")):
                if key not in lang:
                    continue  # the test above owns that failure
                with self.subTest(source=source.name, key=key):
                    self.assertEqual(
                        lang[key].count("%s"), count,
                        f"{key} takes {lang[key].count('%s')} arguments, "
                        f"{source.name} passes {count}",
                    )

    def test_the_creative_poles_sprite_is_the_derived_one_and_is_current(self):
        """The pink sprite is generated from the substation's, so it can go stale (#272).

        It is the only thing that tells the two blocks apart in a world, and the substation's art
        is the input: redraw that and this file is silently the old pole, in the old colour, at the
        old pixel count. Running the generator's own `--check` is how every other derived asset
        here is held.
        """
        import subprocess
        result = subprocess.run(
            [sys.executable, str(TEXTURE_SCRIPT), "--check"],
            capture_output=True, text=True)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_the_creative_pole_is_not_a_tier(self):
        # The ladder is Factorio's footprints and three loops walk it; a creative row would reach
        # the item map, the recipe sweep and docs/factorio-mechanics.md, none of which have a row
        # to give a dev tool. Asserted rather than assumed, because "add it to the enum" is the
        # obvious next edit and it is the wrong one.
        self.assertNotIn(creative_pole_name(), self.tiers)
        self.assertIn("extends SupplyAreaPoleBlock",
                      CREATIVE_POLE.read_text(encoding="utf-8"),
                      "the creative pole has to be the shipped pole with a different ledger, or "
                      "what it is used to test is not what ships")

    def test_the_creative_pole_is_craftable_nowhere(self):
        # It ships (a creative building tool, like vanilla's creative-only blocks), so the only
        # thing keeping it out of survival is the absence of a recipe. A converter run that learned
        # to emit one would be silent.
        recipes = sorted(
            path.relative_to(ROOT).as_posix()
            for path in (DATA / "recipe").rglob("*.json")
            if creative_pole_name() in path.read_text(encoding="utf-8"))
        self.assertEqual([], recipes, "the creative pole must have no recipe anywhere")


if __name__ == "__main__":
    unittest.main()
