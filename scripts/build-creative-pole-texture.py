#!/usr/bin/env python3
"""Build the creative pole's sprite: the substation's own art, recoloured pink (#272).

The creative pole is a dev tool that wears the substation's footprint and, until this script
existed, the substation's texture as well -- which is the one thing about it a player cannot check.
A tool whose whole job is "place it, place a machine, read Jade" has to be *visibly* not the block
it imitates: one left behind in a test world otherwise powers a factory that looks self-sufficient,
and nothing in the world says which of the two poles is doing it. Pink is the convention for a
creative-only block, so pink it is.

**Derived rather than drawn.** The substation's sprite is the subject and this is a recolour of it,
so that a redrawn substation carries its shading, its silhouette and its pixel count straight
through to the creative pole instead of leaving a stale hand-painted copy nobody re-derives. Each
pixel keeps its own brightness and is re-lit in {CREATIVE_PINK:#08x}: the ramp, and therefore the
shading, is the substation's.

Run after the substation's texture changes:

    scripts/build-creative-pole-texture.py

`--check` re-derives and diffs, like every other generator here. No PIL: the codec below is a
minimal 8-bit reader and an RGBA writer, the same trade `scripts/build-pick-textures.py` makes.
"""

import pathlib
import struct
import sys
import zlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
TEXTURES = ROOT / "kubejs/assets/planetaryfactory/textures/block"
SOURCE = TEXTURES / "substation_electric_pole.png"
OUT = TEXTURES / "creative_electric_pole.png"

#: The colour a mid-bright pixel lands on. Chosen to read as pink against both the stone and the
#: copper the pack's machines are built out of, and far enough from the substation's blue that the
#: two do not pass for each other at a glance.
CREATIVE_PINK = 0xE65AC8

#: Rec. 601 luma, the same weighting every other greyscale decision in the pack uses.
LUMA = (0.299, 0.587, 0.114)


def read_png(path):
    """An 8-bit, non-interlaced PNG as rows of RGBA tuples."""
    data = pathlib.Path(path).read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", path
    i, idat, plte, trns, ihdr = 8, b"", None, None, None
    while i < len(data):
        length = struct.unpack(">I", data[i:i + 4])[0]
        tag = data[i + 4:i + 8]
        body = data[i + 8:i + 8 + length]
        if tag == b"IHDR":
            ihdr = struct.unpack(">IIBBBBB", body)
        elif tag == b"IDAT":
            idat += body
        elif tag == b"PLTE":
            plte = body
        elif tag == b"tRNS":
            trns = body
        i += 12 + length
    width, height, depth, ctype, _comp, _filt, interlace = ihdr
    assert depth == 8 and interlace == 0, (path, depth, interlace)
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    raw = zlib.decompress(idat)
    stride = width * channels
    rows, prev, pos = [], bytearray(stride), 0
    for _ in range(height):
        filt = raw[pos]
        pos += 1
        line = bytearray(raw[pos:pos + stride])
        pos += stride
        for x in range(stride):
            a = line[x - channels] if x >= channels else 0
            b = prev[x]
            c = prev[x - channels] if x >= channels else 0
            if filt == 1:
                line[x] = (line[x] + a) & 0xFF
            elif filt == 2:
                line[x] = (line[x] + b) & 0xFF
            elif filt == 3:
                line[x] = (line[x] + (a + b) // 2) & 0xFF
            elif filt == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 0xFF
        prev = line
        row = []
        for x in range(width):
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
                idx = px[0]
                r, g, b = plte[idx * 3:idx * 3 + 3]
                row.append((r, g, b, trns[idx] if trns and idx < len(trns) else 255))
        rows.append(row)
    return rows


def png_bytes(rows):
    height, width = len(rows), len(rows[0])
    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in rows)

    def chunk(tag, body):
        block = tag + body
        return struct.pack(">I", len(body)) + block + struct.pack(">I", zlib.crc32(block))

    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))


def luma(pixel):
    return sum(weight * channel for weight, channel in zip(LUMA, pixel[:3]))


def recolour(rows, colour):
    """Re-light every pixel in `colour`, keeping its own brightness relative to that colour's.

    Not a multiply by the colour, which is what `build-pick-textures.py` does to a *greyscale*
    layer. The substation's sprite is already blue, so multiplying would carry the blue through and
    land on a muddy purple; dividing by the target's own luma instead makes a mid-bright source
    pixel come out as exactly `colour` and the rest of the ramp fall either side of it. The
    midpoint is the sprite's own mean brightness rather than a constant, so a substation redrawn
    lighter or darker still lands on the same pink.
    """
    base = ((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF)
    midpoint = sum(luma(px) for row in rows for px in row if px[3] > 0) / max(
        1, sum(1 for row in rows for px in row if px[3] > 0))
    out = []
    for row in rows:
        pixels = []
        for px in row:
            if px[3] == 0:
                pixels.append(px)
                continue
            # Capped so the brightest channel lands on 255 rather than being clipped there.
            # Clipping one channel and not the others rotates the hue, which on a two-shade
            # highlight is the difference between pink and white.
            scale = min(luma(px) / midpoint, 255.0 / max(base))
            pixels.append(tuple(round(channel * scale) for channel in base) + (px[3],))
        out.append(pixels)
    return out


def main():
    if not SOURCE.is_file():
        print("FAIL %s is missing -- the creative pole's art is the substation's" %
              SOURCE.relative_to(ROOT))
        return 1
    image = png_bytes(recolour(read_png(SOURCE), CREATIVE_PINK))
    if "--check" in sys.argv:
        if image != (OUT.read_bytes() if OUT.is_file() else b""):
            print("FAIL %s is stale -- re-run scripts/build-creative-pole-texture.py"
                  % OUT.relative_to(ROOT))
            return 1
        print("ok   %s is current" % OUT.relative_to(ROOT))
        return 0
    OUT.write_bytes(image)
    print("wrote %s" % OUT.relative_to(ROOT))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
