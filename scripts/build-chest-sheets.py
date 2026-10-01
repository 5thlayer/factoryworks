#!/usr/bin/env python3
"""Build the Iron Chest's sheet from Futureazoo's iron frame (#543).

No installed jar ships an iron chest sheet, so it is derived from `data/art/iron_frame_side.png`
and laid out the way 26.1's `ChestModel` reads a single chest: a 64x64 sheet of three boxes, each
with the standard box UV. The lid and base faces tile the source at the face's own sheet
coordinates with a 1px border at 55% brightness; the latch is the source at 135%. Every other
pixel is transparent.

    uv run --with pillow scripts/build-chest-sheets.py
    uv run --with pillow scripts/build-chest-sheets.py --check    # what tests/ runs

Pillow, unlike the other texture generators here, because the brightness steps are Pillow's own
rounding and a hand-rolled copy would drift from the approved art. `--check` compares decoded
pixels, not file bytes, so a Pillow release that encodes differently does not fail it.
"""
import pathlib
import sys

from PIL import Image, ImageDraw, ImageEnhance

ROOT = pathlib.Path(__file__).resolve().parent.parent
SOURCE = ROOT / "data/art/iron_frame_side.png"
OUT = ROOT / "kubejs/assets/factoryworks/textures/entity/chest/iron_chest.png"


def box(u, v, w, h, d):
    """Each face's (x, y, width, height) on the sheet, by vanilla's box UV."""
    return [
        (u + d, v, w, d),
        (u + d + w, v, w, d),
        (u, v + d, d, h),
        (u + d, v + d, w, h),
        (u + d + w, v + d, d, h),
        (u + 2 * d + w, v + d, w, h),
    ]


LID = box(0, 0, 14, 5, 14)
BASE = box(0, 19, 14, 10, 14)
LATCH = box(0, 0, 2, 4, 1)


def tile(src, w, h, ox=0, oy=0):
    out = Image.new("RGBA", (w, h))
    for y in range(h):
        for x in range(w):
            out.putpixel((x, y), src.getpixel(((x + ox) % src.width, (y + oy) % src.height)))
    return out


def build():
    src = Image.open(SOURCE).convert("RGBA")
    sheet = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    border = tuple(ImageEnhance.Brightness(src).enhance(0.55).getpixel((0, 0)))
    for x, y, w, h in LID + BASE:
        face = tile(src, w, h, x, y)
        ImageDraw.Draw(face).rectangle([0, 0, w - 1, h - 1], outline=border)
        sheet.paste(face, (x, y))
    latch = ImageEnhance.Brightness(src).enhance(1.35)
    for x, y, w, h in LATCH:
        sheet.paste(tile(latch, w, h), (x, y))
    return sheet


def main():
    sheet = build()
    if "--check" in sys.argv:
        if not OUT.exists() or Image.open(OUT).convert("RGBA").tobytes() != sheet.tobytes():
            print(f"FAIL: {OUT.relative_to(ROOT)} is missing or stale")
            return 1
        print("ok   the Iron Chest sheet matches its source")
        return 0
    OUT.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(OUT)
    print(f"wrote {OUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
