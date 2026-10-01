#!/usr/bin/env python3
"""Build Terra's spawn-anchored starting area (ADR-0019, issue #84).

ADR-0019 makes the starting area "a
spawn-anchored structure with a fixed resource set and a randomized layout and patch sizes".

Three decisions this script encodes, each argued in `docs/adr/0019-*.md` and in issue #84:

**The patches are pack-authored ore blocks, not GregTech's and not a GregTech vein.** ADR-0041
makes an ore block carry an amount, and GregTech models its material ore blocks at runtime --
so the blocks here are `factoryworks:<resource>_ore`, which carry the amount and the eight
sprite stages. What a block pays out is `OreResource`'s, and it is vanilla's raw ore for iron and
copper -- GregTech registered none for a material vanilla already covers, so `gtceu:raw_iron` was
never an item and naming it cost every draw its payout in silence. The blocks still carry `c:ores`.

**The patch total is Factorio's and the per-block amount is a quotient.** This script writes no
amount into the templates. It writes the ore blocks; the mod counts what was actually placed at
stamp time and divides Factorio's own starting total by it (ADR-0041), because the size variant
is drawn at world generation and only the placed field knows its own block count.

**Anchoring is not worldgen's at all.** It was `minecraft:concentric_rings` at distance 0, count 1,
on the reasoning that it is the only vanilla placement type putting a bounded number of a structure
near the world origin -- but the origin is not spawn, and on a seed whose origin is open ocean the
ring's biome search fails and the player gets no opening at all, silently. No `StructurePlacement`
can see world spawn: it is handed a `ChunkGeneratorStructureState` and nothing else, deliberately.
So `factoryworks_core` stamps the start pool onto spawn itself on `ServerStartedEvent`, and this
script writes no structure and no structure set -- see `TerraStartingArea` and ADR-0019's amendment.

**Randomization is jigsaw, not noise.** The hub carries one connector per resource, each
pointing at that resource's own single-purpose pool. One pool per resource is what makes the
*set* fixed -- a shared pool would happily deal three copper patches and no coal -- while the
size variants inside each pool, the three hub variants and vanilla's rotation of both give the
layout its variety.

Patch sizing is anchored on ADR-0020's own figure: a small surface patch worked by hand empties
in about an hour. A mid-size draw here is ~1,170 blocks across iron, copper and coal, which at a
couple of seconds a block is about that. Stone is the fourth field and sits outside the figure: it
is ADR-0041's late addition, it is not on the hand-mining path the hour measures, and its own
mid-size patch adds ~260 on top. It is a tuning number, not a discrete choice.

Run from anywhere; writes into `kubejs/data/factoryworks/`. `--check` writes nothing and exits 1
if any generated file is stale.
"""

import contextlib
import importlib
import io
import json
import math
import os
import random
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import nbt  # noqa: E402
TERRA_PALETTE = importlib.import_module("build-terra-worldgen").PALETTE  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
PF = os.path.join(ROOT, "kubejs", "data", "factoryworks")
STRUCTURES = os.path.join(PF, "structure")
WORLDGEN = os.path.join(PF, "worldgen")

DATA_VERSION = 4790  # 26.1.2, world_version in the client jar's version.json.

# The patch outlines are generated, so they need a seed to be reproducible: rerunning this
# script must not churn nine binary files for no reason. The variety a player sees comes from
# the jigsaw at world generation, not from rerunning this.
SEED = 20260829

# The single ore block each patch is made of. One block, never a mix: the starting patch is the
# tutorial, and it has to answer "what is this a patch of" with one word.
PATCHES = {
    "iron": {
        "block": "factoryworks:iron_ore",
        "radii": [10, 12, 14],
        "facing": "east",
    },
    "copper": {
        "block": "factoryworks:copper_ore",
        "radii": [9, 10, 12],
        "facing": "north",
    },
    "coal": {
        "block": "factoryworks:coal_ore",
        "radii": [9, 11, 13],
        "facing": "west",
    },
    # The fourth field (ADR-0041). ADR-0021 ruled stone ambient terrain and "never a patch", and
    # that is reversed: it discharged stone's function onto a cobble generator, and Terra's noise
    # settings carry `aquifers_enabled: false` and place no lava, so a cobble generator is
    # unbuildable here. The smallest field of the four, which is Factorio's own ordering --
    # stone's starting patch is 160,000 against iron's 400,000.
    "stone": {
        "block": "factoryworks:stone_ore",
        "radii": [8, 9, 11],
        "facing": "south",
    },
}

SIZES = ["small", "medium", "large"]

# How far a patch's centre sits from the hub, per size variant. Far enough that the three
# patches read as separate fields rather than one blob, near enough that the whole opening is
# walkable before the player has a road. Bounded by the structure's max_distance_from_center.
DISTANCES = [34, 48, 62]

# Terra's land: every palette biome but the sea (#356).
LAND_BIOMES = sorted("factoryworks:" + entry[0] for entry in TERRA_PALETTE if entry[0] != "terra_sea")

# Jigsaw orientations are `<front>_<top>`; every connector here is horizontal, so the top is up.
OPPOSITE = {"east": "west", "west": "east", "north": "south", "south": "north"}


def jigsaw_block(name, target, pool, facing):
    """A jigsaw block, palette entry and block entity together.

    `final_state` is air: the connector is scaffolding, and the patch it attaches has to be
    the only thing the player finds.
    """
    return (
        {"Name": "minecraft:jigsaw", "Properties": {"orientation": facing + "_up"}},
        {
            "id": "minecraft:jigsaw",
            "name": name,
            "target": target,
            "pool": pool,
            "final_state": "minecraft:air",
            "joint": "aligned",
            "placement_priority": 0,
            "selection_priority": 0,
        },
    )


def disc(rng, radius):
    """A patch outline: a disc whose edge wanders, so it reads as a field rather than a token.

    The wander is a low-frequency sum of two sines with random phase -- enough to break the
    circle at a distance, not so much that the patch stops reading as one shape.
    """
    phase_a, phase_b = rng.uniform(0, math.tau), rng.uniform(0, math.tau)
    cells = []
    span = radius + 3
    for dx in range(-span, span + 1):
        for dz in range(-span, span + 1):
            distance = math.hypot(dx, dz)
            if distance < 0.5:
                cells.append((dx, dz))
                continue
            angle = math.atan2(dz, dx)
            edge = radius * (1.0 + 0.16 * math.sin(2 * angle + phase_a)
                             + 0.09 * math.sin(3 * angle + phase_b))
            if distance <= edge:
                cells.append((dx, dz))
    return cells


def write_template(path, size, palette, blocks):
    nbt.write(path, {
        "DataVersion": nbt.Int(DATA_VERSION),
        "size": [nbt.Int(n) for n in size],
        "palette": palette,
        "blocks": blocks,
        "entities": [],
    })
    print("wrote %s (%d blocks)" % (os.path.relpath(path, ROOT), len(blocks)))


def build_patch(rng, resource, size_name, radius, distance):
    """One patch template: a connector at the west edge, the ore field `distance` blocks east.

    The template is one block tall, and `factoryworks:ground` drops each of its columns onto
    the terrain, so y=0 is the topsoil block itself: the ore replaces it and the field lies flush
    with the surface. That is the Factorio reading ADR-0019 asks for -- a patch you see the outline
    of and plan a miner over -- and it is also what makes the field legible after half of it has
    been dug. Under a wood the ore goes beneath the trees, which still stand on it.
    """
    spec = PATCHES[resource]
    cells = disc(rng, radius)
    span = radius + 3
    # The connector sits at x=0; the field's centre at x=distance. The template is padded to
    # hold both, with the field's own span either side in z.
    centre_x, centre_z = distance, span
    width, depth = distance + span + 1, 2 * span + 1

    palette_index = {}
    palette = []
    blocks = []

    def state_of(entry):
        key = json.dumps(entry, sort_keys=True)
        if key not in palette_index:
            palette_index[key] = len(palette)
            palette.append(entry)
        return palette_index[key]

    # Always west, whatever direction the field is meant to run. The ore lies along +x in
    # template space, so the connector -- which points back at the hub from the far end --
    # is -x. Which way the field actually runs is then decided by rotation, and vanilla picks
    # the rotation for us: `JigsawBlock.canAttach` only accepts the one that leaves this front
    # opposite the hub's. Writing OPPOSITE[facing] here instead names the right direction in
    # world space and the wrong one in template space, which rotates the field off its axis --
    # copper's would land on top of iron's.
    connector, connector_nbt = jigsaw_block(
        "factoryworks:terra_start_patch",
        "factoryworks:terra_start_hub",
        "minecraft:empty",
        "west",
    )
    blocks.append({
        "pos": [nbt.Int(0), nbt.Int(0), nbt.Int(centre_z)],
        "state": nbt.Int(state_of(connector)),
        "nbt": connector_nbt,
    })

    ore = state_of({"Name": spec["block"]})
    for dx, dz in cells:
        x, z = centre_x + dx, centre_z + dz
        if x == 0 and z == centre_z:
            continue  # never bury the connector
        blocks.append({
            "pos": [nbt.Int(x), nbt.Int(0), nbt.Int(z)],
            "state": nbt.Int(ore),
        })

    write_template(
        os.path.join(STRUCTURES, "terra_start_%s_%s.nbt" % (resource, size_name)),
        (width, 1, depth), palette, blocks)


# How far a connector may wander along its face. The hub grows to accommodate it.
SCATTER = 7

# The hub's water pool (ADR-0050, issue #212).
#
# ADR-0050's rule is that water is extracted and transported, never created: source formation is
# off, no bucket is craftable, and every source block in the world is one worldgen or a structure
# placed. So water is not a thing rung 0 can make, it is a *place* rung 0 has to find -- and until
# it is in the hub, finding it is an unbounded walk ADR-0049's traversal budget has no room for.
# The pool ships with the opening rather than being found.
#
# It was argued for Create's water wheel, which was the pack's only rotational source before the
# burner line. Create left with ADR-0060 and the pool's reason got stronger, not weaker: what it
# now sites is the Offshore Pump (ADR-0050, #213), the one block water enters the factory through,
# and behind it the Boiler (ADR-0048, #224). With no pool at spawn rung 0 has no water at all,
# where before it merely had no rotation.
#
# It is *the hub's own blocks*, not a fifth jigsaw child. A child would have to attach through a
# connector on a hub face, which is where the four ore fields already are, and it would then be
# subject to vanilla's silent overlap rejection -- the failure this file's geometry check exists
# to catch. Blocks in the hub template cannot be dropped: they are placed with the hub or the hub
# did not place.
#
# One block deep and flush with the ground. The water replaces the topsoil block, so its surface
# is level with the grass around it and every neighbouring column is solid terrain at that y --
# nothing to spill into, on a world ADR-0019 makes flat. `POOL_CLEARANCE` layers of air go above
# it so that whatever grew there (tall grass is two blocks) is not left standing in the water.
#
# It sits beside the wreck's doorway (ADR-0107), off the doorway's own line so the way out is dry
# ground.

POOL_RADIUS = 4
POOL_CLEARANCE = 2
# Dry blocks between the pool and the wreck's wall, and between it and the doorway's line.
POOL_GAP = 2

# The wreck (ADR-0107): a ship's hull inside a box of outside x, y, z at the hub's centre. A nose at
# -x, a blunt engine end at +x, walls that curve in at the top. An open doorway two tall in the
# middle of the +z long wall, windows on the -z long wall and the nose, and the cargo hold, five by
# two, flush in the flat engine end (#548, #549). `TerraStartingArea` faces the spawn toward
# template +z, and `test_start_geometry.py` holds the doorway there.
WRECK_SIZE = (15, 7, 11)
# The hull's half-width in z at the nose, one entry per x from the tip, and by layer, bottom up.
NOSE_HALF_WIDTH = (2, 3, 4)
LAYER_HALF_WIDTH = (5, 5, 5, 5, 5, 4, 3)
# The first x of each layer: the nose slopes back toward the roof.
LAYER_START_X = (0, 0, 0, 0, 0, 1, 2)
WRECK_HULL = {"Name": "factoryworks:wreck_hull", "Properties": {"scorched": "false"}}
SCORCHED_HULL = {"Name": "factoryworks:wreck_hull", "Properties": {"scorched": "true"}}
WRECK_WINDOW = {"Name": "factoryworks:wreck_window"}
# One block of the ten is the anchor, the one with the inventory. A boolean does not rotate with the
# template, which a stored direction to the anchor would (ADR-0107).
CARGO_HOLD = {"Name": "factoryworks:cargo_hold", "Properties": {"anchor": "false"}}
CARGO_HOLD_ANCHOR = {"Name": "factoryworks:cargo_hold", "Properties": {"anchor": "true"}}
HOLD_WIDTH = 5
# Light blocks are for reading the room at night; nothing spawns on Terra (ADR-0093).
WRECK_LIGHT = {"Name": "minecraft:light", "Properties": {"level": "15", "waterlogged": "false"}}
AIR = {"Name": "minecraft:air"}

# The wreck's damage (#550) lands only on the -z half, the nose and the roof: the engine end holds
# the cargo hold, and the +z wall the doorway the pool sits beside.
SCORCH_NEAR = 0.6
SCORCH_ELSEWHERE = 0.2
# Earth heaped against the nose, by distance from the hull: the wreck reads as half buried.
HEAP_HEIGHT = {1: 2, 2: 1}
EARTH = {"Name": "minecraft:dirt"}
TURF = {"Name": "minecraft:grass_block", "Properties": {"snowy": "false"}}
# A piece of each Factorio size class, in blocks; how many of each is the corpus's.
DEBRIS_SHAPES = {
    "big": [(0, 1, 0), (1, 1, 0), (0, 2, 0)],
    "medium": [(0, 1, 0), (1, 1, 0)],
    "small": [(0, 1, 0)],
}
# The trail runs behind the engine end and off toward -z, away from the hold's face and the pool:
# its bearing, in degrees from +x toward -z, and how far one piece may stray from it.
DEBRIS_BEARING = (25, 65)
DEBRIS_SPREAD = 12
DEBRIS_CLEARANCE = 3


def patch_span(resource):
    """Half the width of this resource's widest field, in blocks.

    The widest, not the drawn one: which size variant a connector deals is decided at world
    generation, so the hub has to be laid out for the largest it could deal.
    """
    return max(PATCHES[resource]["radii"]) + 3


def along_face(resource, dx, dz):
    """A connector's scatter runs along the hub face it sits on: z on an east or west face,
    x on a north or south one. The other component is pinned to the edge."""
    return dz if PATCHES[resource]["facing"] in ("east", "west") else dx


def in_hull(x, y, z):
    """Whether a cell of the wreck's box lies inside the hull's outer surface."""
    sx, sy, sz = WRECK_SIZE
    if not (0 <= y < sy and LAYER_START_X[y] <= x < sx):
        return False
    nose = NOSE_HALF_WIDTH[x] if x < len(NOSE_HALF_WIDTH) else LAYER_HALF_WIDTH[0]
    return abs(z - sz // 2) <= min(nose, LAYER_HALF_WIDTH[y])


def bevel(x, y, z):
    """The palette entry smoothing a top edge of the hull, or None where the hull stays square.

    A cell with open air above and beside it is an edge. Below the roof a stair rises toward the
    hull's middle, an outer corner where the nose turns; on the roof a bottom slab finishes the
    curve. The engine end is left square, so the hold's face stays flat.
    """
    if in_hull(x, y + 1, z):
        return None
    out_x = not in_hull(x - 1, y, z)
    out_z = [d for d in (-1, 1) if not in_hull(x, y, z + d)]
    if not out_x and not out_z:
        return None
    if y == WRECK_SIZE[1] - 1:
        return {"Name": "factoryworks:wreck_hull_slab",
                "Properties": {"type": "bottom", "waterlogged": "false"}}
    if out_x and out_z:
        facing, shape = "east", "outer_right" if out_z == [-1] else "outer_left"
    elif out_x:
        facing, shape = "east", "straight"
    else:
        facing, shape = "south" if out_z == [-1] else "north", "straight"
    return {"Name": "factoryworks:wreck_hull_stairs",
            "Properties": {"facing": facing, "half": "bottom", "shape": shape,
                           "waterlogged": "false"}}


def wreck_blocks():
    """The wreck in its own coordinates, as (x, y, z, palette entry).

    Every cell of the box above the floor is written, as air outside the hull, so whatever grew
    there is cleared. The floor is template y=0, which replaces the ground block the way the pool
    and the fields do, and the ground outside the hull's floor is left as it is.
    """
    sx, sy, sz = WRECK_SIZE
    door_x = sx // 2
    mid_z = sz // 2
    cells = []
    for x in range(sx):
        for y in range(sy):
            for z in range(sz):
                if not in_hull(x, y, z):
                    if y > 0:
                        cells.append((x, y, z, AIR))
                    continue
                # Diagonals count: an outer corner stair is open on its room side (#549).
                shell = y in (0, sy - 1) or any(
                    not in_hull(x + dx, y + dy, z + dz)
                    for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, 0, 1), (0, 0, -1),
                                       (1, 0, 1), (1, 0, -1), (-1, 0, 1), (-1, 0, -1)))
                edge = bevel(x, y, z) if shell else None
                if not shell:
                    entry = WRECK_LIGHT if (y == sy - 2 and x in (3, sx - 4)
                                            and z in (2, sz - 3)) else AIR
                elif edge is not None:
                    entry = edge
                elif z == sz - 1 and x == door_x and y in (1, 2):
                    entry = AIR
                elif z == 0 and y in (2, 3) and 4 <= x <= sx - 3:
                    entry = WRECK_WINDOW
                elif x == 0 and y in (2, 3) and abs(z - mid_z) <= 1:
                    entry = WRECK_WINDOW
                elif x == sx - 1 and y in (1, 2) and abs(z - mid_z) <= HOLD_WIDTH // 2:
                    entry = CARGO_HOLD_ANCHOR if (y == 1 and z == mid_z) else CARGO_HOLD
                else:
                    entry = WRECK_HULL
                cells.append((x, y, z, entry))
    return cells


def debris_counts():
    """How many pieces of each size class Factorio's crash site has, read off the corpus."""
    with open(os.path.join(ROOT, "data", "factorio", "container.json")) as handle:
        rows = json.load(handle)["debris"]
    return {size: sum(1 for row in rows
                      if row["name"].startswith("crash-site-spaceship-wreck-%s-" % size))
            for size in DEBRIS_SHAPES}


def damaged_wreck(rng):
    """`wreck_blocks` with its damage: a caved roof with its fall on the floor below, a breach and
    missing panes on the -z wall and the nose, and scorch around them."""
    sx, sy, sz = WRECK_SIZE
    cells = {(x, y, z): entry for x, y, z, entry in wreck_blocks()}
    mid_z = sz // 2
    damage = set()

    hole_x, hole_w, hole_d = rng.randint(3, 8), rng.choice((2, 3)), rng.choice((2, 3))
    for x in range(hole_x, hole_x + hole_w):
        for z in range(2, 2 + hole_d):
            cells[(x, sy - 1, z)] = AIR
            damage.add((x, sy - 1, z))
            if rng.random() < 0.5:
                cells[(x, 1, z)] = WRECK_HULL

    breach_x = rng.randint(4, sx - 5)
    for cell in ((breach_x, 1, 0), (breach_x, 2, 0), (breach_x + 1, 1, 0)):
        cells[cell] = AIR
        damage.add(cell)

    panes = [c for c, entry in cells.items() if entry == WRECK_WINDOW]
    for side in ([c for c in panes if c[2] == 0], [c for c in panes if c[0] == 0]):
        for cell in rng.sample(side, min(len(side) - 1, rng.randint(1, 3))):
            cells[cell] = AIR
            damage.add(cell)

    for (x, y, z), entry in list(cells.items()):
        if entry != WRECK_HULL or x == sx - 1 or not (z < mid_z or x <= 2 or y == sy - 1):
            continue
        open_side = any(not in_hull(x + dx, y + dy, z + dz) or (x + dx, y + dy, z + dz) in damage
                        for dx, dy, dz in ((-1, 0, 0), (0, 0, -1), (0, 1, 0)))
        near = any(max(abs(x - a), abs(y - b), abs(z - c)) <= 2 for a, b, c in damage)
        if open_side and rng.random() < (SCORCH_NEAR if near else SCORCH_ELSEWHERE):
            cells[(x, y, z)] = SCORCHED_HULL
    return [(x, y, z, entry) for (x, y, z), entry in sorted(cells.items())]


def earth_heap():
    """Earth piled against the nose, in the wreck's coordinates, as (x, y, z, palette entry)."""
    sx, _, sz = WRECK_SIZE
    hull = {(x, z) for x in range(sx) for z in range(sz) if in_hull(x, 1, z)}
    cells = []
    for x in range(-max(HEAP_HEIGHT), 3):
        for z in range(-max(HEAP_HEIGHT), sz + max(HEAP_HEIGHT)):
            if (x, z) in hull:
                continue
            distance = min(max(abs(x - hx), abs(z - hz)) for hx, hz in hull)
            height = HEAP_HEIGHT.get(distance, 0)
            if x < 0 and abs(z - sz // 2) <= 1:
                # Below the nose's windows.
                height = min(height, 1)
            for y in range(1, height + 1):
                cells.append((x, y, z, TURF if y == height else EARTH))
    return cells


def debris_keep_clear(width, door_x, occupied):
    """What the debris keeps off besides the pool, as hub (x, z) -> margin: the wreck and its heap,
    the doorway's line, the hold's face and the connectors."""
    sx, _, sz = WRECK_SIZE
    ox, oz = wreck_origin(width)
    keep_clear = {}
    for x in range(-max(HEAP_HEIGHT), sx):
        for z in range(-max(HEAP_HEIGHT), sz + max(HEAP_HEIGHT)):
            keep_clear[(ox + x, oz + z)] = DEBRIS_CLEARANCE
    for z in range(oz + sz, width):
        keep_clear[(door_x, z)] = 1
    for x in range(ox + sx, width):
        for z in range(oz + sz // 2 - 3, oz + sz // 2 + 4):
            keep_clear[(x, z)] = 0
    for x, z in occupied:
        keep_clear[(x, z)] = 3
    return keep_clear


def scatter_debris(rng, width, keep_clear):
    """Factorio's debris along one bearing behind the wreck, in hub coordinates, as
    (x, y, z, palette entry). `keep_clear` is every hub column a piece may not come within its
    margin of: (x, z) -> margin."""
    sx, _, sz = WRECK_SIZE
    ox, oz = wreck_origin(width)
    cx, cz = ox + sx // 2, oz + sz // 2
    bearing = rng.uniform(*DEBRIS_BEARING)
    reach = (sx // 2 + DEBRIS_CLEARANCE + 1, width // 2 - 2)
    placed, cells = set(), []
    for size, count in sorted(debris_counts().items()):
        for _ in range(count):
            for _attempt in range(500):
                angle = math.radians(bearing + rng.uniform(-DEBRIS_SPREAD, DEBRIS_SPREAD))
                distance = rng.uniform(*reach)
                x0 = round(cx + distance * math.cos(angle))
                z0 = round(cz - distance * math.sin(angle))
                turn = rng.randrange(4)
                shape = [(x0 + (dx, -dz, -dx, dz)[turn], y, z0 + (dz, dx, -dz, -dx)[turn])
                         for dx, y, dz in DEBRIS_SHAPES[size]]
                columns = {(x, z) for x, _, z in shape}
                if all(1 <= x < width - 1 and 1 <= z < width - 1 for x, z in columns) \
                        and not any(max(abs(x - a), abs(z - b)) <= margin
                                    for x, z in columns for (a, b), margin in keep_clear.items()) \
                        and not any(max(abs(x - a), abs(z - b)) <= 1
                                    for x, z in columns for a, b in placed):
                    break
            else:
                raise AssertionError("no room for %s debris along bearing %.0f" % (size, bearing))
            placed |= columns
            entry = {"Name": "factoryworks:wreck_debris_%s" % size}
            cells += [(x, y, z, entry) for x, y, z in shape]
    return cells


def hub_width():
    return 2 * (max(patch_span(resource) for resource in PATCHES) + SCATTER) + 1


def wreck_origin(width):
    """The wreck's -x -z corner in hub coordinates: centred, so the hub's centre is its floor's."""
    sx, _, sz = WRECK_SIZE
    return width // 2 - sx // 2, width // 2 - sz // 2


def build_hub(rng, index, offsets):
    """One hub variant: one connector per resource, at scattered positions, the wreck and the pool.

    The hub places no *terrain* block of its own. Its job is to hold the four connectors far
    enough apart, and at different enough offsets, that the fields do not land on a fixed figure
    every world -- and, since ADR-0050, to carry the pool rung 0's Offshore Pump is sited on.

    Every connector sits on the hub's own outer face, pointing out of it, and that is not a
    style choice. Vanilla marks the parent's *entire* bounding box occupied the moment a
    connector points at a block inside it (`JigsawPlacement.Placer.tryPlacingChildren`), and
    then rejects every child that overlaps the occupied shape -- which is every child, since
    each one starts at that same interior block. An interior connector therefore attaches
    nothing at all, silently, with no warning in the log. So the scatter runs *along* each
    face rather than across the hub's interior.
    """
    palette = []
    palette_index = {}
    blocks = []

    def state_of(entry):
        key = json.dumps(entry, sort_keys=True)
        if key not in palette_index:
            palette_index[key] = len(palette)
            palette.append(entry)
        return palette_index[key]

    # The hub is sized by the widest field, not by the connector offsets, and this is the whole
    # reason it is large. A patch template's bounding box is a rectangle `2*span+1` wide running
    # the *entire* way from its connector to the far end of the field. Two fields on perpendicular
    # faces therefore both cover the diagonal corner beside the hub, overlap there, and vanilla
    # drops whichever it happens to try second -- silently, since a rejected child is not an error.
    # Making the hub at least as wide as the widest field plus its scatter keeps every field's
    # sideways extent inside the hub's own footprint, so no two can reach each other's corner.
    width = hub_width()
    occupied = set()
    for resource, (dx, dz) in offsets.items():
        facing = PATCHES[resource]["facing"]
        block, block_nbt = jigsaw_block(
            "factoryworks:terra_start_hub",
            "factoryworks:terra_start_patch",
            "factoryworks:terra_start_" + resource,
            facing,
        )
        # Clamped by this resource's own span, which is what makes the guarantee hold rather
        # than merely usually hold: the size variant is drawn at world generation, so the hub
        # has to fit the largest one this connector could ever deal.
        span = patch_span(resource)
        along = min(max(width // 2 + along_face(resource, dx, dz), span), width - 1 - span)
        x, z = {
            "east": (width - 1, along),
            "west": (0, along),
            "north": (along, 0),
            "south": (along, width - 1),
        }[facing]
        assert (x, z) not in occupied, "hub %d puts two connectors in one cell" % index
        occupied.add((x, z))
        blocks.append({
            "pos": [nbt.Int(x), nbt.Int(0), nbt.Int(z)],
            "state": nbt.Int(state_of(block)),
            "nbt": block_nbt,
        })

    # The wreck holds the centre: the connectors are all on the outer faces and the fields all run
    # outward from them, so the middle is the one part of the footprint nothing else claims.
    ox, oz = wreck_origin(width)
    wreck_rng = random.Random("%d-wreck-%d" % (SEED, index))
    wreck = {(x, y, z): entry for x, y, z, entry in damaged_wreck(wreck_rng)}
    wreck.update({(x, y, z): entry for x, y, z, entry in earth_heap()})
    for (x, y, z), entry in sorted(wreck.items()):
        assert (ox + x, oz + z) not in occupied, "hub %d's wreck covers a connector" % index
        blocks.append({
            "pos": [nbt.Int(ox + x), nbt.Int(y), nbt.Int(oz + z)],
            "state": nbt.Int(state_of(entry)),
        })

    # The pool beside the doorway: its nearest cell POOL_GAP clear of the wall and of the doorway's
    # line, on the +x side.
    water = state_of({"Name": "minecraft:water", "Properties": {"level": "0"}})
    air = state_of(AIR)
    cells = disc(rng, POOL_RADIUS)
    keep_clear = {}
    door_x = ox + WRECK_SIZE[0] // 2
    wall_z = oz + WRECK_SIZE[2] - 1
    shift_x = door_x + POOL_GAP + 1 - min(dx for dx, _ in cells)
    shift_z = wall_z + POOL_GAP + 1 - min(dz for _, dz in cells)
    for dx, dz in cells:
        x, z = shift_x + dx, shift_z + dz
        assert 0 <= x < width and 0 <= z < width, "hub %d's pool leaves the hub" % index
        assert (x, z) not in occupied, "hub %d floods a connector at %d,%d" % (index, x, z)
        keep_clear[(x, z)] = 2
        blocks.append({
            "pos": [nbt.Int(x), nbt.Int(0), nbt.Int(z)],
            "state": nbt.Int(water),
        })
        for y in range(1, POOL_CLEARANCE + 1):
            blocks.append({
                "pos": [nbt.Int(x), nbt.Int(y), nbt.Int(z)],
                "state": nbt.Int(air),
            })

    keep_clear.update(debris_keep_clear(width, door_x, occupied))
    for x, y, z, entry in scatter_debris(wreck_rng, width, keep_clear):
        blocks.append({
            "pos": [nbt.Int(x), nbt.Int(y), nbt.Int(z)],
            "state": nbt.Int(state_of(entry)),
        })

    write_template(os.path.join(STRUCTURES, "terra_start_hub_%d.nbt" % index),
                   (width, max(WRECK_SIZE[1], 1 + POOL_CLEARANCE), width), palette, blocks)


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as handle:
        json.dump(obj, handle, indent=2, sort_keys=True)
        handle.write("\n")
    print("wrote %s" % os.path.relpath(path, ROOT))


def build_datapack(hub_count):
    ox, oz = wreck_origin(hub_width())
    write_json(os.path.join(WORLDGEN, "processor_list", "terra_start_hub_ground.json"), {
        "_comment": "Generated by scripts/build-terra-start.py. The ground drop of "
                    "terra_start_ground, except that the wreck's box is laid on one height, the "
                    "ground at its centre, so its walls and roof stay level over uneven ground "
                    "(ADR-0107).",
        "processors": [{
            "processor_type": "factoryworks:ground",
            "level": {
                "min_x": ox,
                "min_z": oz,
                "max_x": ox + WRECK_SIZE[0] - 1,
                "max_z": oz + WRECK_SIZE[2] - 1,
            },
        }],
    })

    write_json(os.path.join(PF, "tags", "worldgen", "biome", "terra_land.json"), {
        "_comment": "Generated by scripts/build-terra-start.py. Terra's land biomes: every "
                    "palette biome but the sea (#356).",
        "replace": False,
        "values": LAND_BIOMES,
    })

    write_json(os.path.join(WORLDGEN, "template_pool", "terra_start.json"), {
        "_comment": "Generated by scripts/build-terra-start.py. Hub variants: the four connectors "
                    "that decide where the fields land, the wreck the player wakes in (ADR-0107), "
                    "and the water pool rung 0's Offshore Pump is sited on (ADR-0050).",
        "fallback": "minecraft:empty",
        "elements": [
            {
                "weight": 1,
                "element": {
                    "element_type": "minecraft:single_pool_element",
                    "location": "factoryworks:terra_start_hub_%d" % index,
                    # Rigid for the patches' reason: the pool and the wreck have to sit in the
                    # ground rather than at the hub's own y, and `rigid` is what keeps vanilla's
                    # gravity processor -- which reads a heightmap that stops at leaves -- off them.
                    "projection": "rigid",
                    "processors": "factoryworks:terra_start_hub_ground",
                },
            }
            for index in range(hub_count)
        ],
    })

    write_json(os.path.join(WORLDGEN, "processor_list", "terra_start_ground.json"), {
        "_comment": "Generated by scripts/build-terra-start.py. `factoryworks:ground` is "
                    "registered by factoryworks_core. It drops each column of a patch onto the "
                    "terrain, walking down through whatever grew there. Vanilla's "
                    "`minecraft:gravity` -- the one the `terrain_matching` projection applies -- "
                    "reads WORLD_SURFACE instead, which is 'the highest block that is not air', so "
                    "a field crossing a wood landed on the canopy as ore in place of leaves. No "
                    "vanilla heightmap avoids that; OCEAN_FLOOR and MOTION_BLOCKING stop at leaves "
                    "and logs too.",
        "processors": [{"processor_type": "factoryworks:ground"}],
    })

    for resource in PATCHES:
        write_json(os.path.join(WORLDGEN, "template_pool", "terra_start_%s.json" % resource), {
            "_comment": "Generated by scripts/build-terra-start.py. One pool per resource is "
                        "what makes ADR-0019's resource set fixed: a shared pool would deal "
                        "three of one ore and none of another. The size variants inside it are "
                        "what makes the patch sizes randomized.",
            "fallback": "minecraft:empty",
            "elements": [
                {
                    "weight": 1,
                    "element": {
                        "element_type": "minecraft:single_pool_element",
                        "location": "factoryworks:terra_start_%s_%s" % (resource, size),
                        # `rigid`, not `terrain_matching`, and the field still follows the ground:
                        # the processor below is what drops each column, and the projection is what
                        # would otherwise add vanilla's own gravity processor on top of it. See the
                        # processor list's comment for why vanilla's will not do.
                        "projection": "rigid",
                        "processors": "factoryworks:terra_start_ground",
                    },
                }
                for size in SIZES
            ],
        })

    # No `worldgen/structure/terra_starting_area.json` is written, and that is the decision rather
    # than an omission. `TerraStartingArea` stamps the *pool* onto world spawn and never reads a
    # structure or a structure set, so a jigsaw structure entry here is registered and consulted by
    # nothing: every field on it -- the biome list, the step, the start height, the radius -- would
    # be a second, silently divergent copy of numbers the Java holds, and `/locate` cannot find the
    # area either way. It shipped that way once, carrying its own `max_distance_from_center: 112`
    # against the stamper's 128, and a reader who found it would reasonably conclude worldgen
    # places the opening. See ADR-0019's amendment and issue #313.


def generate():
    rng = random.Random(SEED)
    os.makedirs(STRUCTURES, exist_ok=True)

    for resource, spec in PATCHES.items():
        for size, radius, distance in zip(SIZES, spec["radii"], DISTANCES):
            build_patch(rng, resource, size, radius, distance)

    # Three hubs, each spreading the connectors differently, so the fields do not sit on the
    # same figure in every world. Four connectors since ADR-0041 -- one per face, which is what
    # a fourth field costs: the hub has four of them and the fifth resource, uranium, has no
    # starting patch in Factorio and so needs none here.
    hub_offsets = [
        {"iron": (0, 0), "copper": (-6, 4), "coal": (2, -7), "stone": (5, 3)},
        {"iron": (3, -5), "copper": (0, 0), "coal": (-8, 2), "stone": (-3, 6)},
        {"iron": (-4, 6), "copper": (7, 1), "coal": (0, 0), "stone": (2, -4)},
    ]
    for index, offsets in enumerate(hub_offsets):
        build_hub(rng, index, offsets)

    build_datapack(len(hub_offsets))


def check():
    """Generate into a scratch tree and compare it to the committed one, writing nothing."""
    global PF, STRUCTURES, WORLDGEN
    committed = PF
    with tempfile.TemporaryDirectory() as scratch:
        PF = scratch
        STRUCTURES = os.path.join(PF, "structure")
        WORLDGEN = os.path.join(PF, "worldgen")
        with contextlib.redirect_stdout(io.StringIO()):
            generate()
        stale = []
        for folder, _, names in os.walk(scratch):
            for name in names:
                fresh = os.path.join(folder, name)
                relative = os.path.relpath(fresh, scratch)
                target = os.path.join(committed, relative)
                with open(fresh, "rb") as a:
                    want = a.read()
                if not os.path.isfile(target) or open(target, "rb").read() != want:
                    stale.append(relative)
    for relative in sorted(stale):
        print("stale %s -- re-run scripts/build-terra-start.py" % relative)
    if stale:
        return 1
    print("ok   Terra's starting area templates and datapack files are current")
    return 0


def main():
    if "--check" in sys.argv[1:]:
        sys.exit(check())
    generate()


if __name__ == "__main__":
    main()
