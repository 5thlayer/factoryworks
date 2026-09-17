"""The structure templates the GameTests are placed into (#271).

A NeoForge GameTest is a structure template plus code: the runner clears a patch of the test
world, places the template, and hands the test a helper whose coordinates are relative to the
template's north-west corner. So even a test that places every block it needs -- which the FE
face tests do, because a pole and a furnace are two blocks and writing them into a template
would hide them from review -- still needs a template to stand on.

That is all this writes: one flat platform, stone floor and air above it. It is generated rather
than exported from a creative world for the same reason `build-terra-start.py` is: a committed
`.nbt` nobody can regenerate is a binary with no source.
"""

import os
import sys
import tempfile

import nbt

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
STRUCTURES = os.path.join(
    ROOT, "mod", "src", "main", "resources", "data", "planetaryfactory", "structure", "gametest")

DATA_VERSION = 4790  # 26.1.2, world_version in the client jar's version.json.

# Seven deep, which is the small pole's 5x5 supply area with a block of margin on each side, so
# nothing a test places sits on the structure's own edge. Twenty-three wide, for the network tests
# (#280): a creative pole's 18x18 area, a small pole wired to it seven blocks east, and a machine
# in the small pole's area but outside the creative one's -- so power reaching it can only have
# crossed the wire. Seven tall: the floor, and six blocks of headroom, which covers the pole's +-2
# vertical reach from a machine standing on it, and the pole's own MAX_SEGMENTS column with a block
# to spare above it, which is what the Placement Preview's column tests need to reach the cap (#297).
SIZE = (23, 7, 7)
FLOOR = "minecraft:stone"


def platform():
    """The floor, and nothing else.

    Air is left out rather than written as `minecraft:air`: the runner clears the whole bounding
    box to air before placing, so an explicit air block would be 168 entries saying what has
    already happened.
    """
    width, _, depth = SIZE
    blocks = []
    for x in range(width):
        for z in range(depth):
            blocks.append({"pos": [nbt.Int(x), nbt.Int(0), nbt.Int(z)], "state": nbt.Int(0)})
    return blocks


def _rendered(size, palette, blocks):
    """The bytes `write_template` would write, for --check to compare against.

    Through a temporary file rather than a buffer, because `nbt.write` takes a path -- and going
    through the same call is the point: a comparison against separately-assembled bytes would be
    checking this function rather than the writer.
    """
    with tempfile.TemporaryDirectory() as directory:
        path = os.path.join(directory, "platform.nbt")
        write_template(path, size, palette, blocks, quiet=True)
        with open(path, "rb") as handle:
            return handle.read()


def write_template(path, size, palette, blocks, quiet=False):
    nbt.write(path, {
        "DataVersion": nbt.Int(DATA_VERSION),
        "size": [nbt.Int(n) for n in size],
        "palette": palette,
        "blocks": blocks,
        "entities": [],
    })
    if not quiet:
        print("wrote %s (%d blocks)" % (os.path.relpath(path, ROOT), len(blocks)))


def main():
    path = os.path.join(STRUCTURES, "platform.nbt")
    if "--check" in sys.argv:
        # The same assertion every other generator's --check makes: that the committed file is the
        # one this script would write. Here it is nearly a formality -- the platform has no input
        # to go stale against, no corpus and no tuning dial, which is why no test file owns it --
        # but a committed binary nobody can re-derive is the thing the rule exists to prevent.
        if not os.path.exists(path):
            print("FAIL: %s does not exist; run this script" % os.path.relpath(path, ROOT))
            return 1
        want = _rendered(SIZE, [{"Name": FLOOR}], platform())
        if open(path, "rb").read() != want:
            print("FAIL: %s is stale; re-run this script" % os.path.relpath(path, ROOT))
            return 1
        print("ok: the gametest platform is up to date")
        return 0
    os.makedirs(STRUCTURES, exist_ok=True)
    write_template(path, SIZE, [{"Name": FLOOR}], platform())
    return 0


if __name__ == "__main__":
    sys.exit(main())
