#!/usr/bin/env python3
"""Retint the fluids the pack borrows from Oritech to Factorio's own colours (#277, ADR-0067).

A borrowed fluid keeps Oritech's id and its recipes, but not necessarily its colour: Oritech draws
heavy oil near-black, diesel -- the pack's petroleum gas -- olive-yellow, and sulfuric acid bright
green, where Factorio's are dark orange, purple and yellow. A player reading a pipe by its colour
is reading Factorio's palette, so where Oritech's colour misses, the core replaces the tint Oritech
registers (`core/mixin/oritech/FluidModelContentMixin`).

**What "the colour" is.** Minecraft draws a fluid as its sprite multiplied by a tint, so the colour
a player sees is roughly the sprite's average times the tint -- not the tint alone. This script
reads each sprite out of the installed Oritech jar, averages its first frame's opaque pixels, and
compares `average x tint` against Factorio's `base_color` from `data/factorio/fluid.json`. Within
TOLERANCE, Oritech's tint stands and nothing is emitted. Beyond it, the emitted tint is
`base_color / average`, clamped to [0, 1], which lands the rendered average on Factorio's colour
as nearly as the sprite allows.

**What is typed here.** `ORITECH_MODELS`: which sprite and which tint Oritech registers per fluid.
Oritech states them as constructor arguments in `rearth.oritech.client.init.FluidModelContent`,
not in any data file, so they were read off the installed 2.0.0-exp6 jar with `javap -c`. Only borrowed fluids are listed. The
check reads every sprite named here out of the jar, and fails on a borrowed Oritech fluid with no
entry -- so a new borrowed fluid fails rather than rendering in whatever colour.

Usage:

    scripts/build-fluid-tints.py            # writes the tint resource the mixin reads
    scripts/build-fluid-tints.py --check    # asserts it is up to date; no writes
"""
import argparse
import glob
import json
import math
import os
import struct
import sys
import zipfile
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEM_MAP = os.path.join(ROOT, "data", "pack", "item-map.json")
FLUID_CORPUS = os.path.join(ROOT, "data", "factorio", "fluid.json")
OUT = os.path.join(ROOT, "mod", "src", "main", "resources", "planetaryfactory_core", "fluid",
                   "tints.json")

# How far, in RGB on [0, 1], a rendered colour may sit from Factorio's before it is retinted.
# 0.15 keeps crude oil (0.13 off, near-black against black) and light naphtha (0.13 off, orange
# against orange), and catches the three that read as a different fluid (0.4 and more).
TOLERANCE = 0.15

# A colour chosen over Factorio's `base_color`, and why. Empty is the default; an entry is a
# decision somebody looked at in game, not a correction to the corpus.
#
# petroleum-gas: Factorio's (0.3, 0.1, 0.3) drawn over Oritech's steam sprite, whose highlights are
# near-white, reads as a bright purple Factorio's pipes never show. Darkened to a near-black purple
# that still sits apart from crude oil's near-black brown (#277, on review in game).
TARGET_OVERRIDES = {
    "petroleum-gas": (0.15, 0.06, 0.16),
}

# Oritech fluid id -> (sprite under assets/oritech/textures/, tint Oritech registers). Read off
# `FluidModelContent.registerFluidModels` in oritech-2.0.0-exp6 with `javap -c`.
ORITECH_MODELS = {
    "oritech:still_oil": ("block/fluid/fluid_gas_dark", (0.478, 0.478, 0.478)),
    "oritech:still_biofuel": ("block/fluid/fluid_strange_pale_2", (0.25, 0.316, 0.086)),
    "oritech:still_heavy_oil": ("block/fluid/fluid_molten", (0.135, 0.135, 0.135)),
    "oritech:still_diesel": ("block/fluid/fluid_steam", (0.735, 0.735, 0.235)),
    "oritech:still_naphtha": ("block/fluid/fluid_molten", (0.949, 0.929, 0.745)),
    "oritech:still_sulfuric_acid": ("block/fluid/fluid_steam", (0.398, 1.0, 0.3)),
}


def read_png(blob):
    """RGBA rows of an 8-bit, non-interlaced PNG -- `build-pick-textures.py`'s reader, on bytes."""
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


def oritech_jar():
    jars = sorted(glob.glob(os.path.join(ROOT, "mods", "oritech-*.jar")))
    if not jars:
        sys.exit("no Oritech jar in mods/ -- run packwiz first")
    return jars[-1]


def sprite_average(archive, sprite):
    """The mean colour of a sprite's first frame, over its opaque pixels, on [0, 1]."""
    w, _, rows = read_png(archive.read("assets/oritech/textures/%s.png" % sprite))
    pixels = [px for row in rows[:w] for px in row if px[3] > 0]
    return tuple(sum(px[i] for px in pixels) / len(pixels) / 255 for i in range(3))


def borrowed_fluids():
    """Factorio fluid name -> the Oritech fluid the item map borrows for it."""
    rows = json.load(open(ITEM_MAP, encoding="utf-8"))["items"]
    return {name: row["target"] for name, row in sorted(rows.items())
            if row.get("kind") == "fluid" and row.get("target", "").startswith("oritech:")}


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
    """(the tint resource, a report line per borrowed fluid)."""
    colours = factorio_colours()
    tints, report = {}, []
    with zipfile.ZipFile(oritech_jar()) as archive:
        for name, target in borrowed_fluids().items():
            if target not in ORITECH_MODELS:
                sys.exit("item-map row %r borrows %s, which ORITECH_MODELS does not describe -- "
                         "read its sprite and tint off FluidModelContent and add it" % (name, target))
            if name not in colours:
                sys.exit("data/factorio/fluid.json has no base_color for %r -- re-run "
                         "scripts/factorio-fluid-extract.py" % name)
            sprite, oritech_tint = ORITECH_MODELS[target]
            average = sprite_average(archive, sprite)
            miss = math.dist(rendered(average, oritech_tint), colours[name])
            if miss <= TOLERANCE:
                report.append("  keep    %-14s %-28s %.2f off" % (name, target, miss))
                continue
            tint = tuple(min(1.0, c / a) if a > 0 else 0.0 for c, a in zip(colours[name], average))
            tints[target] = {
                "factorio": name,
                "color": "#%02X%02X%02X" % tuple(round(t * 255) for t in tint),
            }
            report.append("  retint  %-14s %-28s %.2f off -> %s" % (name, target, miss,
                                                                    tints[target]["color"]))
    return tints, report


def render(tints):
    return json.dumps(tints, indent=2, sort_keys=True) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    tints, report = plan()
    text = render(tints)
    if args.check:
        current = open(OUT, encoding="utf-8").read() if os.path.exists(OUT) else None
        if current != text:
            sys.exit("%s is stale -- re-run scripts/build-fluid-tints.py" % os.path.relpath(OUT, ROOT))
        print("ok   %d borrowed fluids, %d retinted" % (len(report), len(tints)))
        return
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    open(OUT, "w", encoding="utf-8").write(text)
    print("\n".join(report))
    print("wrote %s" % os.path.relpath(OUT, ROOT))


if __name__ == "__main__":
    main()
