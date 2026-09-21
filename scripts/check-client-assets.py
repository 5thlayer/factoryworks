#!/usr/bin/env python3
"""Load the pack's assets in a running CLIENT and assert the game resolved them (#276).

WHY THIS EXISTS. Every other asset check here is file-to-file: `tests/pack/test_data_formats.py`
walks item definition -> model, and the asset-hop checks walk blockstate -> model -> texture. They
assert the files exist and name each other, never that the client's asset manager accepts them.
#273's two in-world checks cannot help: `check-datapack-load.py` and the GameTest harness are both
SERVER runs, and a model, a texture, a blockstate and an item model definition are read by a
client that neither of them ever starts.

The failure that shape hides is the one #273 shipped fifteen of: an item that renders as the
black-and-magenta missing model in the inventory, in the hand and in EMI, with nothing in any log
that a server run could ever print.

HOW. NeoForge 26.1.2.109 has no client test framework -- `net/neoforged/neoforge/gametest/` is
server-side and there is no client GameTest -- so there is no harness to hang this off. What there
IS, is that the real client already boots on a machine with no display: `scripts/launch.py
--headless` takes FML's early window out of the way (its splash is the only thing that insists on a
primary monitor) and Minecraft's own window creates offscreen quite happily. This script boots that
client, waits for the resource reload to finish, kills it and reads the log.

The client is verbose where the server is silent. It prints, per broken asset:

    [Worker-Main-2/WARN]: Missing textures in model planetaryfactory:block/scrap_pile:
        gcyr:block/mars_regolith
    [Worker-Main-2/WARN]: Missing model for variant: 'Block{planetaryfactory:steam}[level=0]'

WHAT IS EXPECTED. Same rule as `check-datapack-load.py`: every complaint the log may contain about
this pack is listed in EXPECTED with the ticket that owns it, an unlisted one fails, and a LISTED
one that no longer appears fails too -- a stale entry is a defect somebody fixed and a guard nobody
re-armed.

WHAT THIS CANNOT SEE, measured rather than assumed. A missing item model definition -- the
fifteen-item failure #276 was filed over -- is logged NOWHERE. Deleting
`assets/planetaryfactory/items/boiler.json` and running this produced a log with zero occurrences
of the string `boiler` in it, and the item still renders as the checkerboard. The client is verbose
about a model whose textures do not resolve and silent about an item that reaches no model at all,
so that half of the claim stays with `tests/pack/test_data_formats.py`, which walks definition ->
model in both directions statically. The two checks are complementary by measurement, not by
hope.

WHAT IS NOT CHECKED. Only this pack's own namespace. The installed mods produce dozens of these
(Railcraft's posts have no `particle`), and they are not ours to fix or to allowlist; an entry for
each would be a list nobody maintains. And whether the Boiler's texture is the RIGHT texture stays
human on delivery -- this asserts only that it resolves at all.

Usage: scripts/check-client-assets.py
"""
import importlib.util
import os
import re
import signal
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOG = ROOT / "logs/latest.log"
NS = "planetaryfactory"

# How long the client may take to reach the end of its resource reload, and how long the log must
# stay quiet before the reload is taken to be over. The client never exits on its own -- it sits at
# the main menu -- so silence is the signal, not an exit code.
TIMEOUT = 600
IDLE = 15
# Proof the reload actually ran. Without it a green result would mean the client died before it
# ever read a model, which is the one way this check could pass having asserted nothing.
RELOADED = "minecraft:textures/atlas/blocks.png-atlas"

# A complaint the log is allowed to contain, and the ticket that owns it. The key is the subject
# the game named -- a model id, or a block state description for a variant.
EXPECTED = {
    "planetaryfactory:item/scrap_pile":
        "names `gcyr:block/mars_regolith`, whose mod left with ADR-0060. The palette is #258's",
    "planetaryfactory:block/scrap_pile":
        "names `gcyr:block/mars_regolith` -- the same departed mod (#258)",
    "planetaryfactory:item/fulgorite":
        "names `gcyr:block/martian_rock` (#258)",
    "planetaryfactory:block/fulgorite":
        "names `gcyr:block/martian_rock` (#258)",
    "planetaryfactory:steam":
        "the pack's own fluid has no fluid model and its block no variants; both halves of the "
        "steam chain's rendering are #189's, which owns the two pack-owned fluids",
    "planetaryfactory:flowing_steam":
        "the flowing half of the same unregistered fluid model (#189)",
    "planetaryfactory:superheated_steam":
        "the second pack-owned fluid, likewise unrendered (#189)",
    "planetaryfactory:flowing_superheated_steam":
        "the flowing half of the second fluid (#189)",
}

# Each way the client says an asset did not resolve, and the group that holds the subject. They are
# listed separately rather than merged because they are different failures: a model that resolved
# but whose textures did not, a state the blockstate file never covered, and a model that could not
# be read at all.
COMPLAINTS = (
    re.compile(r"Missing textures in model ([\w.-]+:[\w/.-]+)"),
    re.compile(r"Missing texture references in model ([\w.-]+:[\w/.-]+)"),
    re.compile(r"Missing model for variant: 'Block\{([\w.-]+:[\w/.-]+)\}"),
    re.compile(r"Missing block model: ([\w.-]+:[\w/.-]+)"),
    re.compile(r"Unable to load model: '([\w.-]+:[\w/.-]+)'"),
    re.compile(r"Missing FluidModel for fluid '([\w.-]+:[\w/.-]+)'"),
    re.compile(r"Missing item model definition for '([\w.-]+:[\w/.-]+)'"),
)


def boot():
    """Run the headless client until its resource reload has gone quiet, and return the log."""
    # `launch.py`'s headless context is imported rather than reimplemented: it edits config/fml.toml
    # and restores it, and two copies of that would be two chances to leave a player's splash off.
    spec = importlib.util.spec_from_file_location("pf_launch", ROOT / "scripts/launch.py")
    launch = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(launch)

    before = LOG.stat().st_mtime if LOG.exists() else 0
    with launch.headless():
        proc = subprocess.Popen(
            [sys.executable, str(ROOT / "scripts/launch.py")],
            cwd=ROOT, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
            start_new_session=True)
        try:
            deadline = time.time() + TIMEOUT
            last_size, last_change = -1, time.time()
            while time.time() < deadline:
                time.sleep(3)
                if proc.poll() is not None:
                    break
                if not LOG.exists() or LOG.stat().st_mtime <= before:
                    continue
                size = LOG.stat().st_size
                if size != last_size:
                    last_size, last_change = size, time.time()
                    continue
                text = LOG.read_text(errors="replace")
                if RELOADED in text and time.time() - last_change >= IDLE:
                    return text
        finally:
            # The whole process group: `launch.py` is a python parent holding the java child, and
            # killing only the parent leaves a headless Minecraft running with nobody watching it.
            # Guarded because the group is already gone when the client died on its own, and an
            # exception raised HERE would replace whatever the body was reporting -- including a
            # real failure -- with a ProcessLookupError traceback.
            try:
                group = os.getpgid(proc.pid)
                os.killpg(group, signal.SIGTERM)
                try:
                    proc.wait(timeout=30)
                except subprocess.TimeoutExpired:
                    os.killpg(group, signal.SIGKILL)
            except ProcessLookupError:
                pass
    return None


def main():
    text = boot()
    # `boot` returns the log only on the one path that proves the reload ran and then went quiet.
    # Every other way out -- the client exiting on its own, the timeout expiring mid-reload -- is a
    # failure rather than a log to scan: a half-written log can hold the atlas line and simply not
    # have reached the model that would have failed, which would pass this check having asserted
    # less than it looked like.
    if text is None:
        print(f"FAIL: the client never finished its resource reload within {TIMEOUT}s, or exited "
              f"early -- nothing was asserted.")
        print(f"      read {LOG}")
        return 1

    found = {}
    for line in text.splitlines():
        for pattern in COMPLAINTS:
            hit = pattern.search(line)
            if hit and hit.group(1).startswith(f"{NS}:"):
                found.setdefault(hit.group(1), line.strip())

    unexpected = {k: v for k, v in found.items() if k not in EXPECTED}
    stale = [k for k in EXPECTED if k not in found]

    for subject, line in sorted(unexpected.items()):
        print(f"UNEXPECTED: {subject}")
        print(f"            {line}")
    for subject in sorted(stale):
        print(f"STALE:      {subject} no longer complains -- delete its EXPECTED entry")
        print(f"            was: {EXPECTED[subject]}")

    if unexpected or stale:
        print(f"\n{len(unexpected)} unexpected, {len(stale)} stale, "
              f"{len(found) - len(unexpected)} expected. Log: {LOG}")
        return 1
    print(f"client assets clean -- {len(found)} known gap(s), all in EXPECTED. Log: {LOG}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
