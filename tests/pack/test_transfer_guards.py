#!/usr/bin/env python3
"""Assert every routing face in the mod is reachable by both of the transfer API's overloads.

`docs/testing/what-to-check.md`'s "cross-file references resolve" claim, for the seam the 26.1.2
port opened and no other check can see.

NeoForge's transfer API states `insert` and `extract` twice: once naming a slot, and once meaning
"anywhere it fits". `DelegatingResourceHandler` forwards the second pair straight to the delegate:

    public int extract(T resource, int amount, TransactionContext transaction) {
        return getDelegate().extract(resource, amount, transaction);
    }

The delegate then runs its own loop over its own slots, so a subclass override of
`extract(int, ...)` is never consulted. A refusal written per slot therefore holds for a caller
that names the slot and evaporates for one that does not -- and every face in this mod is a
refusal: the furnace, the Boiler and the rig all refuse extraction from the slots they are burning
or have not finished with, the pump refuses insertion entirely, and the Boiler's fluid face refuses
each direction on a different tank.

That is invisible to a running game short of the exact mod that calls the slot-less overload, and
invisible to every other check here, so it is asserted as a rule about which base class is used:
`DelegatingResourceHandler` is spelled once, inside `GuardedResourceHandler`, which overrides both
slot-less methods to loop back through itself.

It cannot be a Java unit test. `DelegatingResourceHandler` is a NeoForge type and the mod's test
source set has no NeoForge on its classpath by design (`mod/build.gradle`), which is the same
reason `tests/pack/test_smelting_type.py` reads source text.
"""

import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
MOD = ROOT / "mod/src/main/java/com/planetaryfactory/core"
GUARD = MOD / "transfer/GuardedResourceHandler.java"

# The one place NeoForge's own delegating handler may be named.
DELEGATING = "DelegatingResourceHandler"

# Block comments and line comments, stripped before the scan: this check is about what the code
# names, and the javadoc on these very faces has to be free to explain what they are NOT built on.
COMMENTS = re.compile(r"/\*.*?\*/|//[^\n]*", re.DOTALL)

# The faces whose whole content is a routing rule. Listed rather than discovered, because the
# assertion is that each one is built on the guard, and an empty list would assert nothing.
ROUTING_FACES = (
    "smelting/FurnaceItemHandler.java",
    "fluid/BoilerItemHandler.java",
    "mining/rig/RigItemHandler.java",
    "fluid/BoilerBlockEntity.java",
    "fluid/OffshorePumpBlockEntity.java",
)


def java_sources():
    return sorted(MOD.rglob("*.java"))


def code_of(path):
    return COMMENTS.sub("", path.read_text(encoding="utf-8"))


class TransferGuards(unittest.TestCase):

    def test_the_guard_exists_and_overrides_both_slotless_methods(self):
        self.assertTrue(GUARD.exists(), "GuardedResourceHandler.java has moved or gone")
        source = GUARD.read_text(encoding="utf-8")
        self.assertIn(f"extends {DELEGATING}<T>", source,
                      "the guard has to sit on NeoForge's delegating handler to narrow it")
        for method in ("insert", "extract"):
            # The slot-less overload: a resource and an amount, no leading int.
            self.assertRegex(
                source, rf"public\s+int\s+{method}\s*\(\s*T\s+resource\s*,\s*int\s+amount",
                f"GuardedResourceHandler does not override the slot-less {method}, which is the "
                "whole reason it exists")
            self.assertRegex(
                source, rf"{method}\(index, resource, amount - {method}ed, transaction\)",
                f"the slot-less {method} must loop through this handler's own slot method, or the "
                "guard it exists to enforce is skipped again")

    def test_nothing_else_names_neoforges_delegating_handler(self):
        offenders = [str(path.relative_to(ROOT)) for path in java_sources()
                     if path != GUARD and DELEGATING in code_of(path)]
        self.assertEqual([], offenders,
                         f"{DELEGATING} forwards the slot-less insert and extract past every "
                         "per-slot override; use GuardedResourceHandler instead")

    def test_every_routing_face_is_built_on_the_guard(self):
        for name in ROUTING_FACES:
            path = MOD / name
            with self.subTest(source=name):
                self.assertTrue(path.exists(), "a routing face has moved; this list is stale")
                self.assertIn("GuardedResourceHandler", code_of(path),
                              "this face refuses by slot, so it has to be built on the guard or "
                              "the refusal is reachable around")

if __name__ == "__main__":
    unittest.main(verbosity=0, exit=False)
    print("ok   %d routing face(s) on the guard; %d java source(s) scanned and "
          "DelegatingResourceHandler is named once"
          % (len(ROUTING_FACES), len(java_sources())))
