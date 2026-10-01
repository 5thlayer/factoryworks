#!/usr/bin/env python3
"""Terra's starting area: no two ore fields may claim the same ground, and the hub has its wreck
and its water.

Vanilla drops a jigsaw child whose bounding box overlaps one already placed, and it does so
without logging anything -- a rejected child is an ordinary outcome, not an error. So a hub
whose fields overlap does not fail loudly; it quietly deals three patches instead of four, and
only on some draws. That is what shipped once already: the fields' boxes missed each other by
a single block on the seed the harness happened to use, and collided on the next world.

A patch template's box is a rectangle `2*span+1` wide running the whole way from its connector
to the far end of the field, so two fields on perpendicular hub faces both cover the corner
beside the hub unless the hub is wide enough to hold them apart. This asserts that for every
hub variant against every combination of size variants -- the size is drawn at world
generation, so only the worst case is a guarantee.

The pool is the same failure class and so it is asserted here rather than anywhere else. ADR-0050
refuses a bucket and Create's water wheel is the pack's only rotational source before the burner
line, so a hub that arrives without its pool ships as "rung 0 has no power" -- with nothing in any
log, because a template that quietly lost a block is not an error either.

The wreck (ADR-0107) is held to its shape here because `TerraStartingArea` reads none of it: it
puts the spawn at the hub's centre, one block above the floor, facing template +z, and the
`factoryworks:ground` level box in `terra_start_hub_ground` lays the wreck on one height. A
doorway on another wall, a wreck off the centre or a level box that misses it is a world where the
player wakes in a wall or the room is staggered, with nothing in a log.

Reads the generated .nbt templates, not the generator's own tables, so it fails if
`scripts/build-terra-start.py` is edited and not re-run.
"""

import itertools
import json
import os
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
STRUCTURES = os.path.join(ROOT, "kubejs", "data", "factoryworks", "structure")
HUB_GROUND = os.path.join(ROOT, "kubejs", "data", "factoryworks", "worldgen", "processor_list",
                          "terra_start_hub_ground.json")

sys.path.insert(0, os.path.join(ROOT, "scripts"))
import nbt  # noqa: E402

STEP = {"east": (1, 0), "west": (-1, 0), "north": (0, -1), "south": (0, 1)}
# Four fields since ADR-0041: stone is the fourth, and a fourth connector is exactly the kind of
# addition that pushes two boxes into each other on some draws and not others.
RESOURCES = ["iron", "copper", "coal", "stone"]
SIZES = ["small", "medium", "large"]

# #134's box, outside x, y, z (ADR-0107).
WRECK = (15, 7, 11)
HULL = "factoryworks:wreck_hull"
WINDOW = "factoryworks:wreck_window"
HOLD = "factoryworks:cargo_hold"
# The pool may sit no further than this from the doorway's outer cell.
POOL_REACH = 8


def jigsaws(template):
    """Every jigsaw block in a template, as (pos, facing, pool)."""
    palette = template["palette"]
    out = []
    for block in template["blocks"]:
        entry = palette[block["state"]]
        if entry["Name"] != "minecraft:jigsaw":
            continue
        facing = entry["Properties"]["orientation"].split("_")[0]
        out.append((tuple(block["pos"]), facing, block["nbt"]["pool"]))
    return out


def water(template):
    """The pool: every water cell in a template, as (x, z), with the air above each checked.

    The clearance matters as much as the water. The pool is one block deep and flush with the
    ground, so whatever grew on those columns -- tall grass is two blocks -- is left standing in
    the water unless the template clears it.
    """
    palette = template["palette"]
    cells = set()
    air = set()
    for block in template["blocks"]:
        name = palette[block["state"]]["Name"]
        x, y, z = block["pos"]
        if name == "minecraft:water":
            assert y == 0, "water at y=%d: the pool is one block deep and flush" % y
            cells.add((x, z))
        elif name == "minecraft:air":
            air.add((x, y, z))
    return cells, air


def blocks_by_pos(template):
    palette = template["palette"]
    return {tuple(block["pos"]): palette[block["state"]]["Name"] for block in template["blocks"]}


def level_box():
    with open(HUB_GROUND) as handle:
        processors = json.load(handle)["processors"]
    boxes = [p["level"] for p in processors if p.get("processor_type") == "factoryworks:ground"
             and "level" in p]
    assert len(boxes) == 1, "%s has %d level boxes, expected one" % (HUB_GROUND, len(boxes))
    box = boxes[0]
    return box["min_x"], box["min_z"], box["max_x"], box["max_z"]


def wreck_failures(hub_file, hub, width):
    """The wreck at the hub's centre, its doorway on +z, and the level box over exactly it."""
    failures = []
    at = blocks_by_pos(hub)
    sx, sy, sz = WRECK
    centre = width // 2
    ox, oz = centre - sx // 2, centre - sz // 2
    if hub["size"][1] < sy:
        failures.append("%s is %d tall: the wreck needs %d" % (hub_file, hub["size"][1], sy))
    if level_box() != (ox, oz, ox + sx - 1, oz + sz - 1):
        failures.append("%s: the level box %s is not the wreck's footprint %s"
                        % (hub_file, level_box(), (ox, oz, ox + sx - 1, oz + sz - 1)))

    holds, windows, openings = [], set(), []
    for x in range(sx):
        for z in range(sz):
            for y in (0, sy - 1):
                if at.get((ox + x, y, oz + z)) != HULL:
                    failures.append("%s: wreck %s at %d,%d,%d is %s, not hull"
                                    % (hub_file, "floor" if y == 0 else "roof", x, y, z,
                                       at.get((ox + x, y, oz + z))))
            on_wall = x in (0, sx - 1) or z in (0, sz - 1)
            for y in range(1, sy - 1):
                name = at.get((ox + x, y, oz + z))
                if not on_wall:
                    if name not in ("minecraft:air", "minecraft:light"):
                        failures.append("%s: wreck interior %d,%d,%d is %s"
                                        % (hub_file, x, y, z, name))
                elif name == HOLD:
                    holds.append((x, y, z))
                elif name == WINDOW:
                    windows.add("+x" if x == sx - 1 else "-x" if x == 0
                                else "+z" if z == sz - 1 else "-z")
                elif name != HULL:
                    openings.append((x, y, z))

    door = [(sx // 2, 1, sz - 1), (sx // 2, 2, sz - 1)]
    if sorted(openings) != door:
        failures.append("%s: the wreck's openings are %s, not the doorway %s"
                        % (hub_file, sorted(openings), door))
    if windows != {"-z", "-x"}:
        failures.append("%s: windows on %s, not the -z long wall and the -x short wall"
                        % (hub_file, sorted(windows)))
    if holds != [(sx - 1, 1, sz // 2)]:
        failures.append("%s: cargo holds at %s, not one flush in the +x short wall"
                        % (hub_file, holds))
    spawn = (centre, 1, centre)
    if at.get((spawn[0], 0, spawn[2])) != HULL or at.get(spawn) != "minecraft:air" \
            or at.get((spawn[0], 2, spawn[2])) != "minecraft:air":
        failures.append("%s: the spawn at the hub's centre is not two air on the floor" % hub_file)
    if not any(name == "minecraft:light" for name in at.values()):
        failures.append("%s: the wreck has no light" % hub_file)
    return failures, (ox, oz, ox + sx - 1, oz + sz - 1), (ox + sx // 2, oz + sz)


def patch_box(connector, facing, template):
    """Where a patch template lands, in hub-local coordinates.

    Its own connector sits one block in front of the hub's, template +x runs away from the hub,
    and the field is centred on the connector across that axis.
    """
    width, _, depth = template["size"]
    span = (depth - 1) // 2
    dx, dz = STEP[facing]
    px, pz = -dz, dx                     # the axis across the field
    cx, _, cz = connector
    corners = []
    for along in (1, width):
        for across in (-span, span):
            corners.append((cx + dx * along + px * across,
                            cz + dz * along + pz * across))
    xs = [c[0] for c in corners]
    zs = [c[1] for c in corners]
    return min(xs), min(zs), max(xs), max(zs)


def overlaps(a, b):
    return not (a[2] < b[0] or b[2] < a[0] or a[3] < b[1] or b[3] < a[1])


def main():
    generator = subprocess.run(
        [sys.executable, os.path.join(ROOT, "scripts", "build-terra-start.py"), "--check"],
        capture_output=True, text=True)
    print(generator.stdout, end="")
    if generator.returncode != 0:
        return 1

    failures = []
    patches = {
        (resource, size): nbt.read(
            os.path.join(STRUCTURES, "terra_start_%s_%s.nbt" % (resource, size)))
        for resource in RESOURCES for size in SIZES
    }

    hubs = sorted(f for f in os.listdir(STRUCTURES) if f.startswith("terra_start_hub_"))
    assert hubs, "no hub templates -- run scripts/build-terra-start.py"

    for hub_file in hubs:
        hub = nbt.read(os.path.join(STRUCTURES, hub_file))
        width, _, depth = hub["size"]
        hub_box = (0, 0, width - 1, depth - 1)
        connectors = jigsaws(hub)
        assert len(connectors) == len(RESOURCES), \
            "%s has %d connectors, expected %d" % (hub_file, len(connectors), len(RESOURCES))

        # Every connector must sit on the face it points out of. One that points at a block
        # inside the hub makes vanilla treat the whole hub as occupied, and then no child can
        # ever attach -- the bug this pack shipped before the fields ever appeared.
        for (cx, _, cz), facing, _ in connectors:
            dx, dz = STEP[facing]
            ahead = (cx + dx, cz + dz)
            if hub_box[0] <= ahead[0] <= hub_box[2] and hub_box[1] <= ahead[1] <= hub_box[3]:
                failures.append("%s: connector at %d,%d faces %s into its own box"
                                % (hub_file, cx, cz, facing))

        pool, air = water(hub)
        if not pool:
            failures.append("%s: no water pool -- rung 0 has no power and nothing logs it"
                            % hub_file)
        for (cx, _, cz), _, _ in connectors:
            if (cx, cz) in pool:
                failures.append("%s: the pool floods the connector at %d,%d" % (hub_file, cx, cz))
        for x, z in sorted(pool):
            if not (hub_box[0] <= x <= hub_box[2] and hub_box[1] <= z <= hub_box[3]):
                failures.append("%s: pool cell %d,%d is outside the hub's own box" % (hub_file, x, z))
            if (x, 1, z) not in air:
                failures.append("%s: pool cell %d,%d has nothing cleared above it" % (hub_file, x, z))

        found, wreck_box, outside_door = wreck_failures(hub_file, hub, width)
        failures.extend(found)
        for x, z in sorted(pool):
            if overlaps((x, z, x, z), wreck_box):
                failures.append("%s: pool cell %d,%d is inside the wreck" % (hub_file, x, z))
            if x == outside_door[0]:
                failures.append("%s: pool cell %d,%d is on the doorway's line" % (hub_file, x, z))
        if pool and min(abs(x - outside_door[0]) + abs(z - outside_door[1])
                        for x, z in pool) > POOL_REACH:
            failures.append("%s: the pool is more than %d from the doorway" % (hub_file, POOL_REACH))

        pool_box = (min(x for x, _ in pool), min(z for _, z in pool),
                    max(x for x, _ in pool), max(z for _, z in pool)) if pool else None

        for draw in itertools.product(SIZES, repeat=len(connectors)):
            boxes = []
            for (connector, facing, pool), size in zip(connectors, draw):
                resource = pool.rsplit("_", 1)[-1]
                boxes.append((resource, size,
                              patch_box(connector, facing, patches[(resource, size)])))
            for (ra, sa, ba), (rb, sb, bb) in itertools.combinations(boxes, 2):
                if overlaps(ba, bb):
                    failures.append(
                        "%s: %s %s overlaps %s %s (%s vs %s) -- one of them will not place"
                        % (hub_file, ra, sa, rb, sb, ba, bb))
            for resource, size, box in boxes:
                if pool_box and overlaps(box, pool_box):
                    failures.append("%s: %s %s overlaps the water pool (%s vs %s)"
                                    % (hub_file, resource, size, box, pool_box))
                if overlaps(box, hub_box):
                    failures.append("%s: %s %s overlaps the hub itself (%s)"
                                    % (hub_file, resource, size, box))

    for line in sorted(set(failures)):
        print("FAIL %s" % line)
    if failures:
        print("\n%d starting-area geometry failure(s)" % len(set(failures)))
        return 1
    print("ok   %d hub variant(s) x %d size draw(s): every hub has its wreck and its pool by the "
          "doorway, and no field overlaps another, the pool or the hub"
          % (len(hubs), len(SIZES) ** len(RESOURCES)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
