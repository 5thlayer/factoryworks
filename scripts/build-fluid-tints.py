#!/usr/bin/env python3
"""Tint Core's five oil and chemistry fluids in Factorio's colours, and generate their sprites
(#277, ADR-0067, ADR-0109).

Core registers heavy oil, light oil, petroleum gas, lubricant and sulfuric acid, and draws each
from a sprite under a constant tint (`core/fluid/client/OilFluidClient`). Factorio's colours are
dark orange, purple and yellow, and a player reading a pipe by its colour is reading Factorio's
palette.

**The sprites.** Two animated liquid sprites are generated here, stand-in art, `procgen`: drifting
sine bands over a flat base colour, nothing drawn and nothing taken from another mod. `liquid_amber`
draws the two oils and `liquid_pale` draws lubricant and sulfuric acid. Petroleum gas draws #620's
steam sprite, which `build-steam-assets.py` generates.

**What "the colour" is.** Minecraft draws a fluid as its sprite multiplied by a tint, so the colour
a player sees is roughly the sprite's average times the tint. This script averages each sprite's
first frame's opaque pixels and compares `average x tint` for the tint in `SPRITES` against
Factorio's `base_color` from `data/factorio/fluid.json`. Within TOLERANCE, the tint is emitted
unchanged. Beyond it, the emitted tint is `base_color / average`, clamped to [0, 1], which lands the
rendered average on Factorio's colour as nearly as the sprite allows.

**What is typed here.** `SPRITES`: which sprite each fluid is drawn from, and its tint.
`OilFluidClient` names the same sprites, which `tests/pack/test_fluid_tints.py` holds to this table.

Usage:

    scripts/build-fluid-tints.py            # writes the tint resource and the two sprites
    scripts/build-fluid-tints.py --check    # asserts they are up to date; no writes
"""
import argparse
import json
import math
import os
import struct
import sys
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEM_MAP = os.path.join(ROOT, "data", "pack", "item-map.json")
FLUID_CORPUS = os.path.join(ROOT, "data", "factorio", "fluid.json")
OUT = os.path.join(ROOT, "mod", "src", "main", "resources", "factoryworks_core", "fluid",
                   "tints.json")

SPRITE_DIR = os.path.join(ROOT, "kubejs", "assets", "factoryworks", "textures", "block", "fluid")

# How far, in RGB on [0, 1], a rendered colour may sit from Factorio's before it is retinted.
# 0.15 keeps light oil (0.13 off, orange against orange), and catches the three that read as a different fluid (0.4 and more).
TOLERANCE = 0.15

# A colour chosen over Factorio's `base_color`, and why. Empty is the default; an entry is a
# decision somebody looked at in game, not a correction to the corpus.
#
# petroleum-gas: Factorio's (0.3, 0.1, 0.3) drawn over a near-white sprite reads as a bright purple
# Factorio's pipes never show. Darkened to a near-black purple that still sits apart from crude
# oil's near-black brown (#277, on review in game).
TARGET_OVERRIDES = {
    "petroleum-gas": (0.15, 0.06, 0.16),
}


def hex_tint(code):
    return tuple(int(code[i:i + 2], 16) / 255 for i in (1, 3, 5))


# Factorio fluid -> (sprite under the pack's textures/, tint).
SPRITES = {
    "heavy-oil": ("block/fluid/liquid_amber", hex_tint("#BE6700")),
    "light-oil": ("block/fluid/liquid_amber", hex_tint("#F2EDBE")),
    "petroleum-gas": ("block/fluid/steam", hex_tint("#341330")),
    "lubricant": ("block/fluid/liquid_pale", hex_tint("#405116")),
    "sulfuric-acid": ("block/fluid/liquid_pale", hex_tint("#FFCC1E")),
}

FRAMES = 8
# Mean colour of each generated sprite's first frame, which the tints above were made against.
LIQUIDS = {
    "liquid_amber": (171, 82, 35),
    "liquid_pale": (189, 207, 219),
}
MCMETA = b'{\n\t"animation": {\n\t\t"frametime": 6\n\t}\n}\n'


def liquid_pixels(base):
    """FRAMES stacked 16x16 frames: base colour modulated by sine bands that drift and tile.

    Whole cycles per tile make each frame's modulation average zero, so the mean stays `base`.
    """
    rows = []
    for frame in range(FRAMES):
        phase = 2 * math.pi * frame / FRAMES
        for y in range(16):
            row = []
            for x in range(16):
                wave = (math.sin(2 * math.pi * (x + 2 * y) / 16 + phase)
                        + math.sin(2 * math.pi * (2 * x - y) / 16 - phase)) / 2
                row.append(tuple(round(c * (1 + 0.15 * wave)) for c in base) + (255,))
            rows.append(row)
    return rows


def png_bytes(rows):
    raw = b"".join(b"\x00" + bytes(c for px in row for c in px) for row in rows)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    header = struct.pack(">IIBBBBB", len(rows[0]), len(rows), 8, 6, 0, 0, 0)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header)
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def planned_sprites():
    files = {}
    for name, base in LIQUIDS.items():
        path = os.path.join(SPRITE_DIR, name + ".png")
        files[path] = png_bytes(liquid_pixels(base))
        files[path + ".mcmeta"] = MCMETA
    return files


def core_id(name):
    return "factoryworks:" + name.replace("-", "_")


def read_png(blob):
    """RGBA rows of an 8-bit, non-interlaced PNG. The same minimal reader every generator here
    carries, taking bytes rather than a path."""
    i, ihdr, idat, plte, trns = 8, None, b"", b"", b""
    while i < len(blob):
        length = struct.unpack(">I", blob[i:i + 4])[0]
        tag, body = blob[i + 4:i + 8], blob[i + 8:i + 8 + length]
        if tag == b"IHDR":
            ihdr = struct.unpack(">IIBBBBB", body)
        elif tag == b"IDAT":
            idat += body
        elif tag == b"PLTE":
            plte = body
        elif tag == b"tRNS":
            trns = body
        i += 12 + length
    w, h, depth, ctype, _, _, interlace = ihdr
    assert depth == 8 and interlace == 0, (depth, interlace)
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    raw = zlib.decompress(idat)
    stride = w * channels
    rows, prev, pos = [], bytearray(stride), 0
    for _ in range(h):
        f = raw[pos]
        pos += 1
        line = bytearray(raw[pos:pos + stride])
        pos += stride
        for x in range(stride):
            a = line[x - channels] if x >= channels else 0
            b = prev[x]
            c = prev[x - channels] if x >= channels else 0
            if f == 1:
                line[x] = (line[x] + a) & 0xFF
            elif f == 2:
                line[x] = (line[x] + b) & 0xFF
            elif f == 3:
                line[x] = (line[x] + (a + b) // 2) & 0xFF
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 0xFF
        prev = line
        row = []
        for x in range(w):
            px = line[x * channels:(x + 1) * channels]
            if ctype == 6:
                row.append(tuple(px))
            elif ctype == 2:
                row.append((px[0], px[1], px[2], 255))
            elif ctype == 4:
                row.append((px[0], px[0], px[0], px[1]))
            elif ctype == 0:
                row.append((px[0], px[0], px[0], 255))
            else:
                r, g, bl = plte[px[0] * 3:px[0] * 3 + 3]
                row.append((r, g, bl, trns[px[0]] if px[0] < len(trns) else 255))
        rows.append(row)
    return w, h, rows


def sprite_average(sprite):
    """The mean colour of a sprite's first frame, over its opaque pixels, on [0, 1]."""
    path = os.path.join(ROOT, "kubejs", "assets", "factoryworks", "textures", "%s.png" % sprite)
    with open(path, "rb") as handle:
        w, _, rows = read_png(handle.read())
    pixels = [px for row in rows[:w] for px in row if px[3] > 0]
    return tuple(sum(px[i] for px in pixels) / len(pixels) / 255 for i in range(3))


def pack_fluids():
    """Factorio fluid name -> Core's id, for each fluid in SPRITES; exits if the item map disagrees."""
    rows = json.load(open(ITEM_MAP, encoding="utf-8"))["items"]
    fluids = {}
    for name in sorted(SPRITES):
        row = rows.get(name) or {}
        if row.get("kind") != "fluid" or row.get("target") != core_id(name):
            sys.exit("item-map row %r should send the fluid to %s, not %r"
                     % (name, core_id(name), row.get("target")))
        fluids[name] = core_id(name)
    return fluids


def factorio_colours():
    """The colour each fluid is drawn toward: Factorio's `base_color`, or its TARGET_OVERRIDES row."""
    colours = {f["name"]: tuple(f["base_color"])
               for f in json.load(open(FLUID_CORPUS, encoding="utf-8"))["fluids"]
               if f.get("base_color") is not None}
    colours.update(TARGET_OVERRIDES)
    return colours


def rendered(average, tint):
    return tuple(a * t for a, t in zip(average, tint))


def plan():
    """(the tint resource, a report line per fluid)."""
    colours = factorio_colours()
    tints, report = {}, []
    for name, fluid in pack_fluids().items():
        if name not in colours:
            sys.exit("data/factorio/fluid.json has no base_color for %r -- re-run "
                     "scripts/factorio-fluid-extract.py" % name)
        sprite, oritech_tint = SPRITES[name]
        average = sprite_average(sprite)
        miss = math.dist(rendered(average, oritech_tint), colours[name])
        if miss <= TOLERANCE:
            tint, verb = oritech_tint, "keep"
        else:
            tint = tuple(min(1.0, c / a) if a > 0 else 0.0 for c, a in zip(colours[name], average))
            verb = "retint"
        tints[fluid] = {
            "factorio": name,
            "color": "#%02X%02X%02X" % tuple(round(t * 255) for t in tint),
        }
        report.append("  %-6s %-14s %-28s %.2f off -> %s" % (verb, name, fluid, miss,
                                                             tints[fluid]["color"]))
    return tints, report


def render(tints):
    return json.dumps(tints, indent=2, sort_keys=True) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    sprites = planned_sprites()
    if args.check:
        stale = [os.path.relpath(p, ROOT) for p, data in sprites.items()
                 if not os.path.isfile(p) or open(p, "rb").read() != data]
        if stale:
            sys.exit("stale or missing: %s -- re-run scripts/build-fluid-tints.py" % ", ".join(stale))
    else:
        os.makedirs(SPRITE_DIR, exist_ok=True)
        for path, data in sprites.items():
            open(path, "wb").write(data)
    tints, report = plan()
    text = render(tints)
    if args.check:
        current = open(OUT, encoding="utf-8").read() if os.path.exists(OUT) else None
        if current != text:
            sys.exit("%s is stale -- re-run scripts/build-fluid-tints.py" % os.path.relpath(OUT, ROOT))
        print("ok   %d fluids tinted, %d sprite files" % (len(tints), len(sprites)))
        return
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    open(OUT, "w", encoding="utf-8").write(text)
    print("\n".join(report))
    print("wrote %s" % os.path.relpath(OUT, ROOT))


if __name__ == "__main__":
    main()
