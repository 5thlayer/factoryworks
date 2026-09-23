#!/usr/bin/env python3
"""Assert the mod's FE faces take energy, and journal it so an aborted probe leaves none.

`docs/testing/what-to-check.md`'s "cross-file references resolve" claim, for the half of an energy
face that `tests/pack/test_capability_registration.py` cannot see. That check asserts the furnace
*has* an `Energy` face (a pole has none since ADR-0062); this one asserts the face does something when a pole inserts
into it.

The failure it exists to catch shipped (#266). The furnace's face was carried over from the EU
buffer, where refusing insertion kept a GregTech cable and ADR-0036's pole from meeting at one
block, and it read:

    public int insert(int amount, TransactionContext transaction) {
        return 0; // The pole is the boundary: a GT cable run gets nothing.
    }

With GregTech gone and FE the pack's one currency (ADR-0060) there is no second route to refuse,
and that line only meant the Electric Furnace could never be powered by the one thing built to
power it. Nothing throws, nothing logs, the block places and ticks and renders, and Jade reports
the machine in range and demanding -- the "inert" reading again, one layer in from the registration
the other check holds.

The second half is the snapshot. ADR-0036's pole measures a machine's room by inserting inside a
transaction it then aborts, so a face that takes energy without journalling it keeps a probe's
worth every tick -- a furnace running on power nobody spent, which looks like a working factory.
`LongSnapshotJournal` is where that rule is spelled, and it is asserted to be spelled once for the
same reason `GuardedResourceHandler` is in `test_transfer_guards.py`.

It cannot be a Java unit test: `SnapshotJournal` and `TransactionContext` are NeoForge types and
the mod's test source set has no NeoForge on its classpath by design (`mod/build.gradle`). The
arithmetic under the faces is Minecraft-free and is held by `FurnaceEnergyBufferTest` and
`NetworkBalanceTest`.

A powered loader's face is not here: it is the SimpleBelts fork's, whose source is not in this
repo, and it cannot use `LongSnapshotJournal`, since the fork never depends on the pack. It journals
its own buffer, and `gametest/BeltTileTests` holds the probe and the charge in a world (#348).
"""

import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
MOD = ROOT / "mod/src/main/java/com/planetaryfactory/core"
JOURNAL = MOD / "energy/LongSnapshotJournal.java"

COMMENTS = re.compile(r"/\*.*?\*/|//[^\n]*", re.DOTALL)

# NeoForge's own base class, which LongSnapshotJournal is the mod's one subclass of. Anchored so
# that the mod's own `LongSnapshotJournal`, which every face names, is not read as a second copy.
SNAPSHOT_JOURNAL = re.compile(r"(?<!Long)\bSnapshotJournal\b")

# The FE faces, and the buffer call each one's insert has to reach. Listed rather than
# discovered: a third machine that takes power arrives with a row here, or it is answered "no face
# to check" by a scan that found nothing.
FE_FACES = {
    "smelting/FurnaceBlockEntity.java": "addEnergy(",
    "mining/rig/RigBlockEntity.java": "addEnergy(",
}


def code_of(path):
    return COMMENTS.sub("", path.read_text(encoding="utf-8"))


def method_body(source, signature):
    """The braced body of the first method matching `signature`, by brace matching."""
    declaration = re.search(signature, source)
    if declaration is None:
        return None
    open_brace = source.find("{", declaration.end())
    depth = 0
    for index in range(open_brace, len(source)):
        if source[index] == "{":
            depth += 1
        elif source[index] == "}":
            depth -= 1
            if depth == 0:
                return source[open_brace:index + 1]
    return None


INSERT = r"public\s+int\s+insert\s*\(\s*int\s+amount\s*,\s*TransactionContext"
EXTRACT = r"public\s+int\s+extract\s*\(\s*int\s+amount\s*,\s*TransactionContext"


class EnergyFaces(unittest.TestCase):

    def test_every_fe_face_accepts_and_journals(self):
        for name, buffer_call in FE_FACES.items():
            path = MOD / name
            with self.subTest(face=name):
                self.assertTrue(path.exists(), "an FE face has moved; this list is stale")
                body = method_body(code_of(path), INSERT)
                self.assertIsNotNone(
                    body, "this face has no slot-less insert, so ADR-0036's pole cannot feed it")
                self.assertIn(buffer_call, body,
                              "insert reaches no buffer: the pole will report this machine as "
                              "demanding and never power it, with nothing thrown and nothing "
                              "logged")
                self.assertIn("journal.updateSnapshots(", body,
                              "insert without a snapshot cannot be rolled back, and the pole's "
                              "own demand probe is an insert it aborts -- this face would keep "
                              "the probe's energy every tick")

    def test_every_fe_face_refuses_extraction(self):
        """A grid may fill these; nothing pulls back out of them (ADR-0036)."""
        for name in FE_FACES:
            with self.subTest(face=name):
                body = method_body(code_of(MOD / name), EXTRACT)
                self.assertIsNotNone(body, "this face states no extract, so NeoForge's default "
                                           "decides whether the buffer can be drained")
                self.assertRegex(body.replace("\n", " "), r"\{\s*return 0;\s*\}",
                                 "extraction is a flat refusal here; anything else is a hole a "
                                 "cable drains the machine's own buffer through")

    def test_the_journal_is_spelled_once(self):
        self.assertTrue(JOURNAL.exists(), "LongSnapshotJournal.java has moved or gone")
        offenders = [str(path.relative_to(ROOT)) for path in sorted(MOD.rglob("*.java"))
                     if path != JOURNAL and SNAPSHOT_JOURNAL.search(code_of(path))]
        self.assertEqual([], offenders,
                         "a second copy of the journal is a place for one face to stop taking its "
                         "snapshot; use LongSnapshotJournal")


if __name__ == "__main__":
    unittest.main(verbosity=0, exit=False)
    print("ok   %d FE face(s) accept, journal and refuse extraction" % len(FE_FACES))
