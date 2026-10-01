#!/usr/bin/env python3
"""Build the wreck's block textures from committed art (#551).

Every source is a texture from malcolmriley's unused-textures, copied unmodified under `data/art/`.
The hull, the cargo hold and the debris wear their source as it is. Two have no source and are
derived from the hull plating:

  - **The scorched hull** darkens the plating where a value noise runs high, into irregular
    patches. The noise wraps at the tile's edges, so a wall of scorched blocks has no seam.
  - **The window** is a dark glass porthole inside a frame of the plating. It is opaque, so the
    window's render layer does not matter.

    uv run --with pillow scripts/build-wreck-textures.py
    uv run --with pillow scripts/build-wreck-textures.py --check    # what tests/ runs

`--check` compares decoded pixels, not file bytes, so a Pillow release that encodes differently does
not fail it.
"""
import pathlib
import sys

from PIL import Image, ImageEnhance

ROOT = pathlib.Path(__file__).resolve().parent.parent
ART = ROOT / "data/art"
OUT = ROOT / "kubejs/assets/factoryworks/textures/block/wreck"

HULL = ART / "space-plating-rivets-horizontal.png"
COPIED = {
    "hull": HULL,
    "cargo_hold": ART / "space-plating-access-hatch.png",
    "debris_big": ART / "space-plating-stripe-black-horizontal-damaged.png",
    "debris_medium": ART / "space-plating-stripe-gray-horizontal-damaged.png",
    "debris_small": ART / "steel_block_rusty.png",
}

OCTAVES = ((4, 0.65), (2, 0.35))
# The plating is near white, so a scorch needs a deep floor to read as one (#551).
SCORCH_START, SCORCH_FULL, SCORCH_FLOOR = 0.42, 0.72, 0.2
SOOT_GREEN, SOOT_BLUE = 0.04, 0.1
GLASS_MIN, GLASS_MAX = 4, 11
GLASS = (22, 28, 36, 255)
GLINT = (70, 86, 100, 255)
RIM = 0.55


def lattice(i, j, seed):
    h = (i * 374761393 + j * 668265263 + seed * 2246822519) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return (h ^ (h >> 16)) / 0xFFFFFFFF


def noise(x, y, size):
    total = 0.0
    for seed, (step, weight) in enumerate(OCTAVES):
        cells = size // step
        i, j = x // step, y // step
        fx, fy = (x % step) / step, (y % step) / step
        fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        a, b = lattice(i % cells, j % cells, seed), lattice((i + 1) % cells, j % cells, seed)
        c, d = lattice(i % cells, (j + 1) % cells, seed), lattice((i + 1) % cells, (j + 1) % cells, seed)
        top, bottom = a + (b - a) * fx, c + (d - c) * fx
        total += weight * (top + (bottom - top) * fy)
    return total


def scorched(src):
    out = src.copy()
    for y in range(src.height):
        for x in range(src.width):
            t = (noise(x, y, src.width) - SCORCH_START) / (SCORCH_FULL - SCORCH_START)
            t = max(0.0, min(1.0, t))
            shade = 1 - (1 - SCORCH_FLOOR) * t
            red, green, blue, alpha = src.getpixel((x, y))
            out.putpixel((x, y), (round(red * shade), round(green * shade * (1 - SOOT_GREEN * t)),
                                  round(blue * shade * (1 - SOOT_BLUE * t)), alpha))
    return out


def window(src):
    out = src.copy()
    rim = ImageEnhance.Brightness(src).enhance(RIM)
    for y in range(GLASS_MIN - 1, GLASS_MAX + 2):
        for x in range(GLASS_MIN - 1, GLASS_MAX + 2):
            if GLASS_MIN <= x <= GLASS_MAX and GLASS_MIN <= y <= GLASS_MAX:
                glint = x + y in (GLASS_MIN * 2 + 3, GLASS_MIN * 2 + 4)
                out.putpixel((x, y), GLINT if glint else GLASS)
            else:
                out.putpixel((x, y), rim.getpixel((x, y)))
    return out


def textures():
    hull = Image.open(HULL).convert("RGBA")
    made = {name: Image.open(path).convert("RGBA") for name, path in COPIED.items()}
    made["hull_scorched"] = scorched(hull)
    made["window"] = window(hull)
    return made


def main():
    check = "--check" in sys.argv
    failed = 0
    for name, image in textures().items():
        out = OUT / f"{name}.png"
        if check:
            if not out.exists() or Image.open(out).convert("RGBA").tobytes() != image.tobytes():
                print(f"FAIL: {out.relative_to(ROOT)} is missing or stale")
                failed = 1
            continue
        out.parent.mkdir(parents=True, exist_ok=True)
        image.save(out)
        print(f"wrote {out.relative_to(ROOT)}")
    if check and not failed:
        print(f"ok   the wreck's {len(COPIED) + 2} textures match their sources")
    return failed


if __name__ == "__main__":
    sys.exit(main())
