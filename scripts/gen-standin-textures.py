# Stand-in art for the items whose sprites came from All Rights Reserved mods (ADR-0109): the three
# circuit tiers, the engine units, the battery, the two sheets and the six material forms (#622). 16x16 RGBA PNGs, pure stdlib, drawn here rather than copied.
# They are placeholders, meant to be replaced by commissioned art.
import os
import struct
import zlib

BASE = "kubejs/assets/factoryworks/textures/item"
CLEAR = (0, 0, 0, 0)


def png(path, pixels):
    raw = b"".join(b"\x00" + b"".join(bytes(p) for p in row) for row in pixels)

    def chunk(t, d):
        c = t + d
        return struct.pack(">I", len(d)) + c + struct.pack(">I", zlib.crc32(c) & 0xffffffff)

    data = (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "wb").write(data)


def dark(c, d):
    return tuple(max(0, min(255, v + d)) for v in c[:3]) + (255,)


def board(base, pins):
    """One board silhouette; the tiers differ in colour and in how many chips sit on it."""
    rows = [[CLEAR] * 16 for _ in range(16)]
    for y in range(2, 14):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (2, 13)
            rows[y][x] = dark(base, -50 if edge else 0)
    for x in range(4, 12, 2):
        rows[3][x] = (214, 188, 90, 255)
        rows[12][x] = (214, 188, 90, 255)
    chips = [(4, 5, 4, 3), (9, 5, 3, 3), (5, 9, 6, 2)][:pins]
    for cx, cy, w, h in chips:
        for y in range(cy, cy + h):
            for x in range(cx, cx + w):
                rows[y][x] = (40, 40, 44, 255)
    return rows


def engine(steel=(150, 154, 160, 255), coil=None):
    rows = [[CLEAR] * 16 for _ in range(16)]
    for y in range(5, 12):
        for x in range(3, 13):
            rows[y][x] = dark(steel, -40 if y in (5, 11) or x in (3, 12) else 0)
    for x in (5, 7, 9):
        for y in range(2, 5):
            rows[y][x] = dark(steel, -20)
    for y in range(7, 10):
        rows[y][13] = rows[y][14] = dark(steel, -60)
    if coil:
        for y in (6, 8, 10):
            for x in range(4, 12):
                rows[y][x] = dark(coil, -20 if x in (4, 11) else 10)
    return rows


def battery(base):
    rows = blank()
    fill(rows, 4, 4, 11, 14, base)
    fill(rows, 6, 2, 9, 3, (178, 184, 192, 255))
    for x in range(5, 11):
        rows[8][x] = dark(base, 40)
    return rows


def fill(rows, x0, y0, x1, y1, color, edge=-45):
    """A rectangle with a darker one-pixel border."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            border = x in (x0, x1) or y in (y0, y1)
            rows[y][x] = dark(color, edge if border else 0)


def blank():
    return [[CLEAR] * 16 for _ in range(16)]


def plate(base):
    rows = blank()
    fill(rows, 2, 5, 13, 11, base)
    for x in range(4, 12):
        rows[6][x] = dark(base, 35)
    return rows


def gear(base):
    rows = blank()
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            tooth = (x in (7, 8) or y in (7, 8) or abs(x - y) <= 1 or abs(x + y - 15) <= 1) and d <= 7
            if (d <= 5.2 and d >= 2.2) or tooth:
                rows[y][x] = dark(base, -40 if d > 4.6 else 0)
    return rows


def stick(base):
    rows = blank()
    for i in range(12):
        for dx in (0, 1):
            rows[13 - i][2 + i + dx] = dark(base, -35 if dx else 10)
    return rows


def cable(base):
    rows = blank()
    for x in range(2, 14):
        y = 7 + (1 if (x // 3) % 2 else 0)
        rows[y][x] = dark(base, 15)
        rows[y + 1][x] = dark(base, -40)
    return rows


png(f"{BASE}/electronic_circuit.png", board((70, 150, 78, 255), 1))
png(f"{BASE}/advanced_circuit.png", board((176, 70, 62, 255), 2))
png(f"{BASE}/processing_unit.png", board((72, 108, 176, 255), 3))
png(f"{BASE}/engine_unit.png", engine())
png(f"{BASE}/electric_engine_unit.png", engine((150, 154, 160, 255), (204, 124, 78, 255)))
png(f"{BASE}/battery.png", battery((70, 110, 190, 255)))
png(f"{BASE}/plastic_bar.png", plate((236, 232, 220, 255)))
png(f"{BASE}/low_density_structure.png", plate((48, 50, 56, 255)))
png(f"{BASE}/iron_plate.png", plate((178, 184, 192, 255)))
png(f"{BASE}/copper_plate.png", plate((204, 124, 78, 255)))
png(f"{BASE}/steel_plate.png", plate((104, 112, 130, 255)))
png(f"{BASE}/iron_gear_wheel.png", gear((150, 156, 164, 255)))
png(f"{BASE}/iron_stick.png", stick((160, 166, 174, 255)))
png(f"{BASE}/copper_cable.png", cable((210, 130, 84, 255)))
print("ok")
