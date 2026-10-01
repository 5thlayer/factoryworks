#!/usr/bin/env python3
"""Assert every block entity in the mod has its Transfer API faces, and only those.

`docs/testing/what-to-check.md`'s "cross-file references resolve" claim, for #265 -- the half of
the 26.1.2 port that `tests/pack/test_transfer_guards.py` cannot see.

The guard check asserts that a face which *is* registered refuses correctly on both of the API's
overloads. It says nothing about a face that was never registered at all, and that is the louder
failure: a machine whose capability registration is missing is not broken, it is **inert**. No pipe
finds it, no funnel finds it, no pole counts it as a customer. Nothing throws, nothing is logged,
and the block places and ticks and renders exactly as it should. It reaches a player as "the hopper
doesn't do anything", days later, with no thread back to the omission.

Two things make that reachable here rather than hypothetical. A block entity type is declared in
one place (`BLOCK_ENTITIES.register`) and its faces in another (`registerCapabilities`), with no
compiler relationship between them -- adding the first and forgetting the second compiles. And
`registerCapabilities` is reached only by an `addListener` call in `FactoryWorksCore`, so
dropping that one line makes *every* machine in the mod inert at once, again silently.

#265's own acceptance criterion -- that the faces are the Transfer API's rather than the legacy
capability system's -- is held by the positive half above: each recorded face is asserted to be
spelled `Capabilities.<Kind>.BLOCK`, which is the 26.1 name. The negative half is asserted too, by
name, but it is honestly the weaker of the two: `Capabilities.ItemHandler`,
`net.neoforged.neoforge.items.*` and the rest do not exist on this NeoForge, and GregTech's jars
left with ADR-0060, so reintroducing one is a compile error rather than a silent mis-registration.
That assertion is a cheap regression marker for a port backslide, not a live guard, and it is
recorded here as such rather than dressed up.

It cannot be a Java unit test, for the reason `test_transfer_guards.py` and
`test_smelting_type.py` cannot: `RegisterCapabilitiesEvent` is a NeoForge type and the mod's test
source set has no NeoForge on its classpath by design (`mod/build.gradle`).

Whether a pipe placed against a Boiler actually moves steam is a world load.

The loaders' `Energy` face is registered by Beltworks, not in `PFBlockEntities`, so it has no row
in `FACES`: this repo holds none of that source. Beltworks' own GameTests hold the face, and
`gametest/BeltworksPackTests` a pole's demand probe against it.
"""

import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
MOD = ROOT / "mod/src/main/java/com/factoryworks/core"
BLOCK_ENTITIES = MOD / "PFBlockEntities.java"
ITEMS = MOD / "PFItems.java"
CORE = MOD / "FactoryWorksCore.java"

# Block comments and line comments, stripped before every scan: these assertions are about what the
# code *names*, and the javadoc on these faces has to stay free to explain what they are not built
# on -- the pole's comment names `IEnergyContainer` precisely to say it is gone.
COMMENTS = re.compile(r"/\*.*?\*/|//[^\n]*", re.DOTALL)

# Every block entity type the mod registers, against the faces it must expose and the method in
# `PFBlockEntities` that exposes them. Listed rather than discovered, because the point is that a
# type arriving without a recorded answer fails here -- a discovered table would answer "none" for
# a new machine and assert nothing. A type that genuinely wants no face records an empty tuple,
# which is then a decision somebody wrote down.
FACES = {
    "furnace": ("registerFurnaceCapabilities", ("Item", "Energy")),
    # Item (#540): NeoForge wires vanilla's chest types only, so the pack's own gets its own.
    "chest": ("registerChestCapabilities", ("Item",)),
    # Item (ADR-0107): automation feeds and drains the wreck's hold, on every side.
    "cargo_hold": ("registerCargoHoldCapabilities", ("Item",)),
    # Energy (#194): the electric rig's alone at run time, registered for both tiers.
    "rig": ("registerRigCapabilities", ("Item", "Energy")),
    "rig_part": ("registerRigCapabilities", ("Item", "Energy")),
    "offshore_pump": ("registerPumpCapabilities", ("Fluid",)),
    "boiler": ("registerBoilerCapabilities", ("Fluid", "Item")),
    # Energy (#328): the craft cycle draws FE, and without the face no pole counts the machine.
    # Item (#329): inputs filtered to the Held recipe, on the guard.
    # Fluid (#295): tiers 2 and 3's input tank, taking only the Held recipe's fluid.
    "assembling_machine": ("registerAssemblingMachineCapabilities", ("Energy", "Item", "Fluid")),
    # The Assembling Machine's three faces, on the chassis (ADR-0096).
    "chemical_plant": ("registerChemicalPlantCapabilities", ("Energy", "Item", "Fluid")),
    # No item slot, so no item face (ADR-0096).
    "oil_refinery": ("registerOilRefineryCapabilities", ("Energy", "Fluid")),
    "steam_engine": ("registerSteamEngineCapabilities", ("Energy", "Fluid")),
    # Energy (#283): a pole charges and draws it, on every block of the footprint.
    "solar_panel": ("registerSolarPanelCapabilities", ("Energy",)),
    "accumulator": ("registerAccumulatorCapabilities", ("Energy",)),
    # Energy (#368): the scan draws FE, on every block of the footprint.
    "radar": ("registerRadarCapabilities", ("Energy",)),
    # Energy and Fluid (ADR-0081): a pole feeds it and a pipe drains crude, on every block.
    "pumpjack": ("registerPumpjackCapabilities", ("Energy", "Fluid")),
    # No face: a well is read by the Pumpjack standing on it, never by a pipe.
    "oil_well": (None, ()),
}

# The ladders, against the enum every one of their blocks has to be walked from. A face is
# registered per *block*, so a ladder whose loop names one tier ships the other two inert -- and
# that is not visible in the capability assertion above, which only asks that the face is spelled
# somewhere in the method. The pack has shipped a ladder tier with no face exactly once, and it
# read as "the Electric Furnace ignores hoppers".
LADDERS = {
    "registerFurnaceCapabilities": "FurnaceTier.values()",
    "registerRigCapabilities": "RigTier.values()",
    "registerAssemblingMachineCapabilities": "AssemblingTier.values()",
}

# Blocks that get a face without being a row in any ladder enum. FACES above is keyed by block
# entity *type*, and a block reusing an existing type needs no row there -- which is exactly how a
# face can go missing with every check in this repo still green. Empty since the creative pole's
# face went with every pole's (ADR-0062); kept so the next such block has a row to land in.
UNLADDERED_BLOCKS = {}

# The Barrel's face is on the item rather than a block, so it is registered in `PFItems` and no
# block-side assertion sees it. Listed for the same reason FACES is: a second item capability
# arriving later would otherwise be answered "none".
ITEM_FACES = {
    "BARREL": ("Fluid", "BarrelFluidHandler"),
}

# The pre-26.1 spellings of the three capabilities this mod uses, plus the legacy handler packages
# behind them and GregTech's, which left with ADR-0060. None of these resolves on this NeoForge, so
# the compiler catches them first -- see the docstring. Kept as a marker against a port backslide
# that reintroduces the jar as well as the name.
LEGACY = (
    "Capabilities.ItemHandler",
    "Capabilities.FluidHandler",
    "Capabilities.EnergyStorage",
    "net.neoforged.neoforge.items.",
    "net.neoforged.neoforge.energy.",
    "net.neoforged.neoforge.fluids.capability.",
    "GTCapability",
    "IEnergyContainer",
)


def code_of(path):
    return COMMENTS.sub("", path.read_text(encoding="utf-8"))


def method_body(source, name):
    """The braced body of `name`, by brace matching from its declaration.

    Anchored on the declaration rather than on the bare name: every one of these methods is also
    *called* by name a few lines above, and a search that found the call site would brace-match
    into whichever method happens to be declared next.
    """
    declaration = re.search(rf"^\s*(?:\w+\s+)*void\s+{name}\s*\(", source, re.MULTILINE)
    if declaration is None:
        return None
    open_brace = source.find("{", declaration.end())
    if open_brace < 0:
        return None
    depth = 0
    for index in range(open_brace, len(source)):
        if source[index] == "{":
            depth += 1
        elif source[index] == "}":
            depth -= 1
            if depth == 0:
                return source[open_brace:index + 1]
    return None


class CapabilityRegistration(unittest.TestCase):

    def setUp(self):
        self.source = code_of(BLOCK_ENTITIES)

    def test_every_registered_block_entity_type_has_a_recorded_face_set(self):
        registered = set(re.findall(r'BLOCK_ENTITIES\.register\(\s*"([a-z0-9_]+)"', self.source))
        self.assertTrue(registered, "no block entity types found; the registration idiom has moved")
        self.assertEqual(
            sorted(FACES), sorted(registered),
            "a block entity type has no recorded face set. A machine with no capability "
            "registration is inert -- it places, ticks and renders, and no pipe, funnel or pole "
            "ever reaches it, with nothing thrown and nothing logged. Add it to FACES, with an "
            "empty tuple if it genuinely wants no face.")

    def test_every_recorded_face_is_registered_on_the_transfer_api(self):
        for entity, (method, kinds) in sorted(FACES.items()):
            if method is None:
                continue
            body = method_body(self.source, method)
            with self.subTest(entity=entity):
                self.assertIsNotNone(body, f"{method} has gone; {entity} exposes nothing")
                for kind in kinds:
                    self.assertIn(
                        f"Capabilities.{kind}.BLOCK", body,
                        f"{entity} no longer exposes a {kind} face, so nothing can move "
                        f"{kind.lower()}s in or out of it")

    def test_every_face_method_is_reached_from_the_event_handler(self):
        # A helper that exists and is never called is the same inert machine as one that does not
        # exist, and it compiles just as quietly.
        entry = method_body(self.source, "registerCapabilities")
        self.assertIsNotNone(entry, "PFBlockEntities.registerCapabilities has gone")
        for method in sorted({method for method, _ in FACES.values() if method}):
            with self.subTest(method=method):
                self.assertIn(f"{method}(event)", entry)

    def test_the_event_handlers_are_wired_to_the_mod_bus(self):
        # One dropped line here makes every machine in the mod inert at once.
        core = code_of(CORE)
        for holder in ("PFBlockEntities", "PFItems"):
            with self.subTest(holder=holder):
                self.assertIn(f"modBus.addListener({holder}::registerCapabilities)", core)

    def test_every_ladder_registers_its_face_for_every_tier(self):
        for method, tiers in sorted(LADDERS.items()):
            body = method_body(self.source, method)
            with self.subTest(method=method):
                self.assertIsNotNone(body)
                self.assertIn(tiers, body,
                              "this ladder's face has to be registered for every tier -- a loop "
                              "over fewer ships the remaining tiers inert, and nothing else here "
                              "can see it")

    def test_every_unladdered_block_has_its_own_registration(self):
        for method, blocks in sorted(UNLADDERED_BLOCKS.items()):
            body = method_body(self.source, method)
            self.assertIsNotNone(body)
            for block in blocks:
                with self.subTest(method=method, block=block):
                    self.assertIn(block, body,
                                  "a block outside the ladder loop needs a registration of its "
                                  "own; without one it is inert, and neither FACES nor LADDERS "
                                  "above can see that it is missing")


    def test_the_rig_part_forwards_to_its_anchor(self):
        # Three quarters of a 2x2 is part, and which corner holds the anchor is not visible. A
        # hopper under the wrong corner otherwise finds nothing.
        body = method_body(self.source, "registerRigCapabilities")
        self.assertIsNotNone(body)
        self.assertIn("PFBlocks.rigPart(tier)", body)
        self.assertIn("rigOf(", body)
        self.assertRegex(self.source, r"RigBlockEntity\s+rigOf\([^)]*\)\s*\{[^}]*RigPartBlockEntity")

    def test_every_item_face_is_recorded_and_registered(self):
        items = code_of(ITEMS)
        registration = method_body(items, "registerCapabilities")
        self.assertIsNotNone(registration, "PFItems.registerCapabilities has gone")
        # Counted rather than pattern-matched out of the source. A second registration can be
        # spelled any number of ways -- a different lambda shape, a holder reached through a map --
        # and a regex that recognised only the shape written today would let the next one past,
        # which is the whole failure ITEM_FACES exists to catch.
        self.assertEqual(
            len(ITEM_FACES), registration.count("event.registerItem("),
            "an item capability has no recorded face. A capability on an item is registered in "
            "PFItems, where no block-side assertion above can see it. Add it to ITEM_FACES.")
        for holder, (kind, handler) in sorted(ITEM_FACES.items()):
            with self.subTest(item=holder):
                self.assertIn(f"Capabilities.{kind}.ITEM", registration)
                self.assertIn(handler, registration)
                self.assertIn(f"{holder}.get()", registration)

    def test_the_barrel_handler_is_the_transfer_apis_item_access_one(self):
        self.assertIn(
            "extends ItemAccessFluidHandler",
            code_of(MOD / "fluid/BarrelFluidHandler.java"),
            "the barrel's handler has to be the Transfer API's item-access one, or the component "
            "it writes is not the one a filled barrel is read back out of")

    def test_no_legacy_capability_name_survives_anywhere_in_the_mod(self):
        offenders = []
        for path in sorted(MOD.rglob("*.java")):
            code = code_of(path)
            offenders.extend(f"{path.relative_to(ROOT)}: {name}"
                             for name in LEGACY if name in code)
        self.assertEqual([], offenders,
                         "the legacy capability system's names are not the Transfer API's; a face "
                         "registered under one is never consulted by a caller using the other. "
                         "The compiler normally catches these first -- reaching this assertion "
                         "means the jar came back too")


if __name__ == "__main__":
    unittest.main(verbosity=0, exit=False)
    print("ok   %d block entity type(s) with recorded faces; %d legacy name(s) asserted absent"
          % (len(FACES), len(LEGACY)))
