#!/usr/bin/env python3
"""Assert nothing in the mod decodes an ItemStack at datapack load (#273).

`docs/testing/what-to-check.md`'s "cross-file references resolve" claim, for the half of #273 the
emitted-format checks cannot reach: the codecs in this jar that READ those files.

#273's framing was "every emitted data format is still 1.21.1-shaped". Its second instance was not
emitted at all. `SmeltingRecipe` decoded its result with `ItemStack.CODEC`, which in 26.1 is
`Item.CODEC_WITH_BOUND_COMPONENTS`, and an item's data components are bound during the SAME
datapack load that reads the recipes:

    Couldn't parse data file 'factoryworks:stone_brick':
      Item minecraft:stone_bricks does not have components yet

One ERROR line at load and the recipe is then absent from the manager: the furnace holds the item,
holds power and never smelts. The emitted JSON is identical either way -- `ItemStackTemplate` reads
the same `{id, count, components}` object, and a bare id string as well -- which is precisely why
`tests/pack/test_data_formats.py` and `tests/factorio/test_smelting_shape.py` are blind to it by
construction. It also vanishes on `/reload`, because a KubeJS reload rebinds components first, so
it is a bug that exists only on a clean world load.

So this file sweeps the whole mod rather than the one class `test_smelting_type.py` pins, and it
does it in both directions:

  - **No load-time reader names a stack codec.** A class that registers a recipe serializer, a
    `SimpleJsonResourceReloadListener` or a datapack-registry `MapCodec` is read while components
    are still being bound. `ItemStackTemplate` is the spelling that survives there.
  - **Every other use is named, with its reason.** One use is legitimate today -- Jade's tooltip
    transport, which runs on a live server long after binding -- and it is listed rather than
    matched by shape, so the next one is a decision somebody wrote down. A listed file that stops
    using the codec fails too: a stale entry is a guard nobody re-armed.

Source text for the reason `test_smelting_type.py` is: the mod's test source set has no Minecraft
on its classpath by design (`mod/build.gradle`), so neither `ItemStack` nor `ItemStackTemplate` is
nameable from a JVM test. Whether the game actually accepts the files is
`scripts/check-datapack-load.py`, which is the in-world half and the only check here that reads a
log.

Usage: tests/pack/test_load_codecs.py
"""

import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
MOD = ROOT / "mod/src/main/java/com/factoryworks/core"

# The spellings that resolve to `Item.CODEC_WITH_BOUND_COMPONENTS` and so refuse an item whose
# components are not bound yet. `ItemStackTemplate` is deliberately not among them.
STACK_CODECS = (
    "ItemStack.CODEC",
    "ItemStack.OPTIONAL_CODEC",
    "ItemStack.STRICT_CODEC",
    "ItemStack.STRICT_OPTIONAL_CODEC",
    "ItemStack.SINGLE_ITEM_CODEC",
)

# What makes a class one the game decodes during a datapack load. Each is a registration the pack
# actually uses; a new kind landing here is a line to add, and the failure it would otherwise ship
# is the one in this file's header.
LOAD_TIME_MARKERS = (
    "RecipeSerializer",
    "SimpleJsonResourceReloadListener",
    "AddServerReloadListenersEvent",
)

# Uses that are not read at datapack load, each with why. Keyed by path under the mod's package.
ALLOWED = {
    "compat/JadeStacks.java":
        "Jade's tooltip transport: a stack encoded on a running server and decoded on its client, "
        "which is long after components are bound. 26.1 took away the save/parse pair the two "
        "providers used, so the codec is what is left.",
}


def sources():
    return sorted(MOD.rglob("*.java"))


def uses(source):
    return [codec for codec in STACK_CODECS if codec in source]


class LoadTimeCodecs(unittest.TestCase):

    def test_no_load_time_reader_decodes_a_stack(self):
        for path in sources():
            source = path.read_text(encoding="utf-8")
            # The header quotes the failure and names the codec; a comment is not a use.
            code = re.sub(r"(?m)^\s*(//|\*|/\*).*$", "", source)
            markers = [marker for marker in LOAD_TIME_MARKERS if marker in code]
            if not markers:
                continue
            with self.subTest(file=path.relative_to(MOD).as_posix()):
                self.assertEqual(
                    [], uses(code),
                    "%s is read during a datapack load (%s) and decodes an ItemStack. An item's "
                    "components are bound by that same load, so the file is rejected with one "
                    "ERROR line and is then absent from its manager. Use ItemStackTemplate."
                    % (path.relative_to(MOD).as_posix(), ", ".join(markers)))

    def test_every_other_use_is_one_somebody_wrote_down(self):
        for path in sources():
            code = re.sub(r"(?m)^\s*(//|\*|/\*).*$", "", path.read_text(encoding="utf-8"))
            if not uses(code):
                continue
            key = path.relative_to(MOD).as_posix()
            with self.subTest(file=key):
                self.assertIn(key, ALLOWED,
                              "%s decodes an ItemStack through %s. If it is never reached by a "
                              "datapack load, say so in ALLOWED with the reason; otherwise it "
                              "needs ItemStackTemplate." % (key, ", ".join(uses(code))))

    def test_no_allowed_entry_is_stale(self):
        for key in sorted(ALLOWED):
            path = MOD / key
            with self.subTest(file=key):
                self.assertTrue(path.is_file(), "%s is listed in ALLOWED and is gone" % key)
                code = re.sub(r"(?m)^\s*(//|\*|/\*).*$", "", path.read_text(encoding="utf-8"))
                self.assertTrue(uses(code),
                                "%s no longer decodes an ItemStack: delete its ALLOWED entry, or "
                                "the next file to need one inherits an exemption nobody reviewed"
                                % key)

    def test_the_pack_recipe_is_the_worked_example(self):
        # The one load-time reader that does decode an item, kept here as the positive half: the
        # sweep above is a refusal and would pass just as well if nothing decoded anything.
        recipe = (MOD / "recipes/SmeltingRecipe.java").read_text(encoding="utf-8")
        self.assertIn("ItemStackTemplate", recipe,
                      "the smelt's result is the mod's one item decoded at datapack load")


if __name__ == "__main__":
    unittest.main(verbosity=2)
