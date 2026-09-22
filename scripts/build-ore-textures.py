#!/usr/bin/env python3
"""Build the ore blocks' eight stage sprites, one set per resource (ADR-0041).

ADR-0041 renders a block's remaining amount as one of Factorio's eight sprite stages. Factorio's
own thresholds are amounts -- 15000 down to 80 -- which do not port to blocks holding about a
thousand; what ports is the *ratio set*, and `data/factorio/resource.json` carries it per resource
as `stage_ratios`. Those ratios are where a block changes stage. **What each stage draws is an even
step**: stage `i` of `n` keeps `(n - i) / n` of the full sprite's ore, because the late ratios
(8.7% down to 0.5%) are too small to see and Jade shows the exact amount (#321). The ore goes
in a fixed order, from the outside in, so a thinning block keeps its core and each stage is a
subset of the one before.

**Coal, copper, iron and uranium wear borrowed art** (#321): Malcolm Riley's unused textures, CC BY
4.0 and credited in `NOTICE`, committed unmodified under `data/art/`. The source's ore pixels are
the ore, and a removed pixel takes the colour of a nearby stone pixel in the source.

**Stone is generated** (#363) as Factorio's stone reads: tan boulders standing on the ground. They
are placed from a fixed seed and go whole.

Run after re-extracting the corpus, in case Factorio changed its stage counts:

    scripts/build-ore-textures.py
    scripts/build-ore-textures.py --check    # what tests/ runs: regenerate and diff

Writes `kubejs/assets/planetaryfactory/textures/block/ore/<resource>_stage<N>.png`.
"""
import argparse
import colorsys
import json
import os
import random
import struct
import sys
import zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
CORPUS = os.path.join(ROOT, "data", "factorio", "resource.json")
OUT = os.path.join(ROOT, "kubejs", "assets", "planetaryfactory", "textures", "block", "ore")

# Terra's alphabet, and the Factorio resource each block's amounts are read from. The block ids
# are the pack's; the keys are Factorio's, because that is what the corpus is keyed by (ADR-0028).
RESOURCES = {
    "iron": "iron-ore",
    "copper": "copper-ore",
    "coal": "coal",
    "uranium": "uranium-ore",
    "stone": "stone",
}

STONE = (122, 122, 122)

# Stone's boulders take Factorio's stone colours: the highlight is its `map_color` and the body the
# median of its sprite's lit rock. The shadow is chosen darker than any matrix pixel, so each boulder
# stands up (#363).
BOULDER_LIGHT = (176, 156, 109)
BOULDER = (155, 135, 88)
BOULDER_SHADOW = (72, 64, 50)
# One per stage, so each stage takes exactly one boulder.
BOULDERS = 8
BOULDER_SHAPES = [
    [(0, 0), (1, 0), (0, 1), (1, 1)],
    [(0, 0), (1, 0), (2, 0), (0, 1), (1, 1)],
    [(0, 0), (1, 0), (0, 1), (1, 1), (1, 2)],
    [(1, 0), (0, 1), (1, 1), (2, 1), (1, 2)],
    [(0, 0), (1, 0), (2, 0), (1, 1), (2, 1)],
    [(0, 0), (1, 0), (0, 1)],
]

SOURCED = {"coal": "ore_slade_coal.png", "copper": "ore_stone_copper_2.png", "iron": "ore_slade_iron.png", "uranium": "ore_stone_soul.png"}
ART = os.path.join(ROOT, "data", "art")

# A source pixel is ore above the HSV saturation or below the value. The value catches a dark ore
# and a crystal's shadow, which must go with the crystal or a spent block keeps a dark hole. Coal
# and iron sit on slade, a dark stone, so their value is tighter; coal has no colour at all.
ORE_PIXEL = {
    "coal": (1.1, 0.2),
    "copper": (0.25, 0.0),
    "iron": (0.25, 0.2),
    "uranium": (0.25, 0.3),
}

# One seed per resource, so a rerun does not churn forty binaries for no reason.
SEED = 20260905


def png(pixels):
    """A 16x16 RGBA PNG, as bytes."""
    raw = b"".join(b"\x00" + b"".join(bytes(p) for p in row) for row in pixels)

    def chunk(tag, body):
        data = tag + body
        return struct.pack(">I", len(body)) + data + struct.pack(">I", zlib.crc32(data) & 0xFFFFFFFF)

    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(raw, 9))
        + chunk(b"IEND", b"")
    )


def read_png(path):
    """A 16x16 8-bit RGBA PNG's pixels, as rows of [r, g, b, a]."""
    data = open(path, "rb").read()
    width, height, depth, kind = struct.unpack(">IIBB", data[16:26])
    if (width, height, depth, kind) != (16, 16, 8, 6):
        sys.exit(f"{path} is not a 16x16 8-bit RGBA PNG")
    idat, i = b"", 8
    while i < len(data):
        (length,) = struct.unpack(">I", data[i:i + 4])
        if data[i + 4:i + 8] == b"IDAT":
            idat += data[i + 8:i + 8 + length]
        i += 12 + length
    raw, stride, rows, prev = zlib.decompress(idat), 64, [], bytearray(64)
    for y in range(16):
        kind, line = raw[y * (stride + 1)], bytearray(raw[y * (stride + 1) + 1:(y + 1) * (stride + 1)])
        for x in range(stride):
            a = line[x - 4] if x >= 4 else 0
            b, c = prev[x], prev[x - 4] if x >= 4 else 0
            if kind == 1:
                line[x] = (line[x] + a) & 255
            elif kind == 2:
                line[x] = (line[x] + b) & 255
            elif kind == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif kind == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append([list(line[x * 4:x * 4 + 4]) for x in range(16)])
        prev = line
    return rows


def sourced_sprites(resource, ratios):
    """One sprite per stage from a borrowed full sprite: the ore shrinks toward its centre."""
    source = read_png(os.path.join(ART, SOURCED[resource]))
    saturation, value = ORE_PIXEL[resource]
    cells = [(x, y) for y in range(16) for x in range(16)]
    hsv = {c: colorsys.rgb_to_hsv(*(v / 255 for v in source[c[1]][c[0]][:3])) for c in cells}
    ore = [c for c in cells if hsv[c][1] > saturation or hsv[c][2] < value]
    stone = [c for c in cells if c not in ore]
    cx = sum(x for x, _ in ore) / len(ore)
    cy = sum(y for _, y in ore) / len(ore)
    # The most ore-like pixel near the centroid is the last to go, so the final stage still reads
    # as ore rather than as a shadow: the brightest coloured pixel, or for coal the darkest.
    coloured = [c for c in ore if hsv[c][1] > saturation]
    strength = (lambda c: hsv[c][2]) if coloured else (lambda c: 1 - hsv[c][2])
    centre = max(coloured or ore,
                 key=lambda c: (strength(c) - 0.1 * ((c[0] - cx) ** 2 + (c[1] - cy) ** 2) ** 0.5, -c[1], -c[0]))
    ore.sort(key=lambda c: ((c[0] - centre[0]) ** 2 + (c[1] - centre[1]) ** 2, c[1], c[0]))

    # The median of the nearest stone pixels by brightness, not the nearest one, so a filled hole
    # does not copy a highlight and read as a bright speck.
    def fill(c):
        near = sorted(stone, key=lambda s: ((s[0] - c[0]) ** 2 + (s[1] - c[1]) ** 2, s[1], s[0]))[:5]
        near.sort(key=lambda s: (hsv[s][2], s[1], s[0]))
        return source[near[2][1]][near[2][0]]

    out = []
    for stage in range(len(ratios)):
        pixels = [[p[:] for p in row] for row in source]
        for x, y in ore[kept(len(ore), stage, len(ratios)):]:
            pixels[y][x] = fill((x, y))
        out.append(png(pixels))
    return out


def kept(full, stage, stages):
    """How many of `full` ore pieces stage `stage` of `stages` keeps. Never zero, so a block still
    holding ore never draws as bare stone."""
    return -(-full * (stages - stage) // stages)


def shade(colour, delta):
    return [max(0, min(255, value + delta)) for value in colour] + [255]


def boulder_sprites(resource, ratios):
    """One sprite per stage: boulders lit from the upper left on the grey matrix, going whole and
    from the outside in, the way Factorio's stone sheet thins (#363)."""
    rng = random.Random(f"{SEED}:{resource}")
    ground = [[shade(STONE, rng.randint(-9, 9)) for _ in range(16)] for _ in range(16)]
    taken, boulders = set(), []
    while len(boulders) < BOULDERS:
        shape = rng.choice(BOULDER_SHAPES)
        ox, oy = rng.randrange(15), rng.randrange(15)
        body = {(ox + dx, oy + dy) for dx, dy in shape}
        shadow = {(x + dx, y + dy) for x, y in body for dx, dy in ((1, 0), (0, 1), (1, 1))} - body
        footprint = body | shadow
        if any(not (0 <= x < 16 and 0 <= y < 16) for x, y in footprint):
            continue
        if any((x + dx, y + dy) in taken for x, y in footprint for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
            continue
        taken |= footprint
        drawn = {c: shade(BOULDER_SHADOW, rng.randint(-6, 6)) for c in shadow}
        for x, y in body:
            lit = (x - 1, y) not in body and (x, y - 1) not in body
            drawn[(x, y)] = shade(BOULDER_LIGHT if lit else BOULDER, rng.randint(-6, 6))
        cx = sum(x for x, _ in body) / len(body)
        cy = sum(y for _, y in body) / len(body)
        boulders.append(((cx - 7.5) ** 2 + (cy - 7.5) ** 2, drawn))
    boulders.sort(key=lambda b: b[0])

    out = []
    for stage in range(len(ratios)):
        pixels = [row[:] for row in ground]
        for _, drawn in boulders[:kept(len(boulders), stage, len(ratios))]:
            for (x, y), colour in drawn.items():
                pixels[y][x] = colour
        out.append(png(pixels))
    return out


def build():
    corpus = json.load(open(CORPUS, encoding="utf-8"))
    by_name = {entry["name"]: entry for entry in corpus["resources"]}
    files = {}
    for resource, factorio in sorted(RESOURCES.items()):
        entry = by_name.get(factorio)
        if entry is None:
            sys.exit(f"{factorio} is not in the corpus -- re-run scripts/factorio-resource-extract.py")
        ratios = entry["stage_ratios"]
        if len(ratios) < 2:
            sys.exit(f"{factorio} carries {len(ratios)} stage ratios; there is nothing to render")
        images = (sourced_sprites(resource, ratios) if resource in SOURCED
                  else boulder_sprites(resource, ratios))
        for stage, image in enumerate(images):
            files[os.path.join(OUT, f"{resource}_stage{stage}.png")] = image
    return files


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true",
                        help="regenerate and compare, without writing")
    args = parser.parse_args()

    files = build()
    if args.check:
        stale = [
            path for path, image in sorted(files.items())
            if not os.path.exists(path) or open(path, "rb").read() != image
        ]
        for path in stale:
            print(f"FAIL: {os.path.relpath(path, ROOT)} is missing or stale")
        if stale:
            return 1
        print(f"ok   {len(files)} ore stage sprites match the corpus's stage count")
        return 0

    os.makedirs(OUT, exist_ok=True)
    for path, image in sorted(files.items()):
        open(path, "wb").write(image)
    print(f"wrote {len(files)} sprites into {os.path.relpath(OUT, ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
