#!/usr/bin/env python3
"""Build the Iron and Steel Chests' sheets from committed art (#543).

No installed jar ships either sheet, so each is derived from a texture under `data/art/` and laid
out the way 26.1's `ChestModel` reads a single chest: a 64x64 sheet of three boxes, each with the
standard box UV. Each lid and base face is cut from its source at the chest's origin, wrapping
where the source is smaller than the face, with a 1px border at 55% brightness; the latch is the
source at 135%. Every other pixel is transparent.

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
OUT = ROOT / "kubejs/assets/factoryworks/textures/entity/chest"
# A block's particle is drawn from the block atlas, which the entity sheets are not in.
PARTICLE_OUT = ROOT / "kubejs/assets/factoryworks/textures/block"

# chest -> (source, origin). The iron origin is clear of its source's 2px frame, which drew a seam
# across the chest's front, and of the dark rivet at (3, 3). The steel origin centres one of the
# source's two 8px plates on a 14px face (#543).
CHESTS = {
    "iron_chest": (ROOT / "data/art/iron_frame_side.png", (5, 2)),
    "steel_chest": (ROOT / "data/art/metal_alloy_block.png", (5, 0)),
}


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
# The face a placed chest shows as its front. The model reads it mirrored against the lid's top, so
# it is drawn mirrored to match (#543).
FRONTS = {LID[5], BASE[5]}


def tile(src, w, h, ox=0, oy=0):
    out = Image.new("RGBA", (w, h))
    for y in range(h):
        for x in range(w):
            out.putpixel((x, y), src.getpixel(((x + ox) % src.width, (y + oy) % src.height)))
    return out


def build(source, origin):
    src = Image.open(source).convert("RGBA")
    sheet = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    border = tuple(ImageEnhance.Brightness(src).enhance(0.55).getpixel((0, 0)))
    for x, y, w, h in LID + BASE:
        face = tile(src, w, h, *origin)
        if (x, y, w, h) in FRONTS:
            face = face.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
        ImageDraw.Draw(face).rectangle([0, 0, w - 1, h - 1], outline=border)
        sheet.paste(face, (x, y))
    latch = ImageEnhance.Brightness(src).enhance(1.35)
    for x, y, w, h in LATCH:
        sheet.paste(tile(latch, w, h), (x, y))
    return sheet


def main():
    check = "--check" in sys.argv
    failed = 0
    outputs = []
    for chest, (source, origin) in CHESTS.items():
        outputs.append((OUT / f"{chest}.png", build(source, origin)))
    outputs.append((PARTICLE_OUT / "steel_chest_particle.png",
                    Image.open(CHESTS["steel_chest"][0]).convert("RGBA")))
    for out, sheet in outputs:
        if check:
            if not out.exists() or Image.open(out).convert("RGBA").tobytes() != sheet.tobytes():
                print(f"FAIL: {out.relative_to(ROOT)} is missing or stale")
                failed = 1
            else:
                print(f"ok   {out.relative_to(ROOT)} matches its source")
            continue
        out.parent.mkdir(parents=True, exist_ok=True)
        sheet.save(out)
        print(f"wrote {out.relative_to(ROOT)}")
    return failed


if __name__ == "__main__":
    sys.exit(main())
