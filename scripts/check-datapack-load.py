#!/usr/bin/env python3
"""Load every emitted data file in a running game and assert the game read it (#273).

WHY THIS EXISTS. `tests/pack/test_data_formats.py` asserts the SHAPES the pack writes; this asserts
that the game, given those files, actually ends up holding them. Nothing else here can. A datapack
file the game rejects is not a crash and not a missing file: it is one ERROR line at load, at the
point nobody is watching, after which the entry is simply absent from its manager. That reaches a
player as a furnace that holds the item, holds power and never smelts (#266) -- an evening of
in-world debugging for a line that was printed the first time the world loaded.

    [Worker-Main-7/ERROR] [minecraft/SimpleJsonResourceReloadListener]:
      Couldn't parse data file 'planetaryfactory:iron_plate': ... Unknown registry key ...

It is also the only check here that can see the OTHER half of a data file: not the shape, which a
static check can read, but whether the ids inside it name anything. `gcyr:mercury_rock` below is
exactly that -- a perfectly shaped loot table, passing every static check in the repo, naming an
item whose mod left with ADR-0060.

HOW. The GameTest server (#271) is already a headless world load that needs no display and no
human, and `gameTestPack` already hands it the pack's own data as a datapack. This script runs it
and reads the log, which nothing did before. `--rerun-tasks` is not optional: Gradle would
otherwise report the run up to date, print no log at all, and this check would pass having loaded
nothing.

WHAT IS EXPECTED. Every rejection the log may contain is listed in EXPECTED, with the ticket that
owns it. An unlisted rejection fails, and so does a LISTED one that no longer appears -- a stale
entry is a defect somebody fixed and a guard nobody re-armed, so the entry is deleted as part of
the fix rather than left to excuse the next one.

Usage: scripts/check-datapack-load.py
"""
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GRADLE = ["./gradlew", ":planetaryfactory_core:runGameTestServer", "--rerun-tasks"]

# A rejection the log is allowed to contain, and why. Two kinds, deliberately not merged: a defect
# this repo owns and has deferred, and an id that only a KubeJS-installed game has. The GameTest
# server is vanilla, the mod jar and FTB Materials (ADR-0061), so KubeJS's
# `StartupEvents.registry` items do not exist on it -- that is the harness, not the pack, and saying so is the whole reason the reasons are here.
EXPECTED = {
    "planetaryfactory:blocks/yumako_log":
        "drops `planetaryfactory:yumako_log`, registered by KubeJS, which a GameTest server does "
        "not load. Harness, not pack",
    "planetaryfactory:blocks/yumako_leaves":
        "drops `planetaryfactory:yumako_fresh`, a KubeJS-registered item. Harness, not pack",
    "planetaryfactory:blocks/jellystem_stem":
        "drops `planetaryfactory:jellynut_fresh`, a KubeJS-registered item. Harness, not pack",
    "planetaryfactory:blocks/iron_stromatolite":
        "drops `planetaryfactory:iron_bacteria_fresh` (KubeJS, harness) AND `gcyr:mercury_rock`, "
        "whose mod left with ADR-0060 -- a real dangling drop, found by this check. The palette "
        "is #258's; what a stromatolite drops instead is Sapros content (#23)",
    # #279: `planetaryfactory:assembling` recipes naming an item `kubejs/startup_scripts/` registers.
    # The shape is the pack's and loads -- every recipe here parses to the field that names the
    # missing item, and the other 19 assembling recipes load clean.
    "planetaryfactory:assembling/advanced_circuit":
        "names `planetaryfactory:advanced_circuit` and `plastic_bar`, registered by KubeJS. Harness, not pack",
    "planetaryfactory:assembling/electric_furnace":
        "names `planetaryfactory:advanced_circuit`, registered by KubeJS. Harness, not pack",
    "planetaryfactory:assembling/electronic_circuit":
        "names `planetaryfactory:electronic_circuit`, registered by KubeJS. Harness, not pack",
    "planetaryfactory:assembling/iron_chest":
        "names `planetaryfactory:iron_chest`, registered by KubeJS. Harness, not pack",
    "planetaryfactory:assembling/low_density_structure":
        "names `planetaryfactory:low_density_structure` and `plastic_bar`, registered by KubeJS. Harness, not pack",
    "planetaryfactory:assembling/steel_chest":
        "names `planetaryfactory:steel_chest`, registered by KubeJS. Harness, not pack",
    "planetaryfactory:assembling/substation":
        "names `planetaryfactory:advanced_circuit`, registered by KubeJS. Harness, not pack",
    "planetaryfactory:blocks/copper_stromatolite":
        "drops `planetaryfactory:copper_bacteria_fresh` (KubeJS, harness) AND `gcyr:mercury_rock` "
        "-- the same dangling drop (#258, #23)",
}

# The line the game prints per rejected file, and the two ways a whole registry can fail to load
# instead -- those are fatal rather than per-file, and are never expected.
REJECTED = re.compile(r"Couldn't parse data file '([^']+)'")
FATAL = (
    "Registry loading errors:",
    "Unbound values in registry",
    "Failed to load vanilla datapack",
)
# Proof the run reached the end. Without it a green result would mean the server died early.
COMPLETED = "GAME TESTS COMPLETE"


def main():
    run = subprocess.run(GRADLE, cwd=ROOT, capture_output=True, text=True)
    log = run.stdout + run.stderr
    failures = []

    if COMPLETED not in log:
        failures.append(
            "the GameTest server never reached '%s', so no datapack was loaded and this check "
            "asserted nothing. Gradle exited %d" % (COMPLETED, run.returncode)
        )
    if run.returncode != 0:
        failures.append("gradle %s exited %d" % (" ".join(GRADLE[1:]), run.returncode))

    for marker in FATAL:
        if marker in log:
            failures.append(
                "the log holds `%s`. A registry that fails to load takes every file in it with "
                "it, which is a different and larger failure than one rejected file" % marker
            )

    rejected = {}
    for line in log.splitlines():
        found = REJECTED.search(line)
        if found:
            rejected[found.group(1)] = line.strip()

    for name in sorted(set(rejected) - set(EXPECTED)):
        failures.append(
            "the game rejected `%s` and nothing here expects it. The file is absent from its "
            "manager; in front of a player that is a recipe that never runs or a block that drops "
            "nothing.\n      %s" % (name, rejected[name])
        )
    for name in sorted(set(EXPECTED) - set(rejected)):
        failures.append(
            "`%s` is listed as expected but the game no longer rejects it: %s. Delete the entry -- "
            "a stale one is a guard nobody re-armed" % (name, EXPECTED[name])
        )

    for failure in failures:
        print("FAIL: " + failure)
    if failures:
        print("\n%d failure(s)" % len(failures))
        return 1
    print("ok: the game loaded the pack's emitted data; %d rejection(s), each expected and owned"
          % len(rejected))
    return 0


if __name__ == "__main__":
    sys.exit(main())
