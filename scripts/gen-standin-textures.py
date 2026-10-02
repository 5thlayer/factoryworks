# Stand-in art for four items that once borrowed Railcraft's sprites (ADR-0109): the three
# circuit tiers and the engine unit. 16x16 RGBA PNGs, pure stdlib, drawn here rather than copied.
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


def engine():
    rows = [[CLEAR] * 16 for _ in range(16)]
    steel = (150, 154, 160, 255)
    for y in range(5, 12):
        for x in range(3, 13):
            rows[y][x] = dark(steel, -40 if y in (5, 11) or x in (3, 12) else 0)
    for x in (5, 7, 9):
        for y in range(2, 5):
            rows[y][x] = dark(steel, -20)
    for y in range(7, 10):
        rows[y][13] = rows[y][14] = dark(steel, -60)
    return rows


png(f"{BASE}/electronic_circuit.png", board((70, 150, 78, 255), 1))
png(f"{BASE}/advanced_circuit.png", board((176, 70, 62, 255), 2))
png(f"{BASE}/processing_unit.png", board((72, 108, 176, 255), 3))
png(f"{BASE}/engine_unit.png", engine())
print("ok")
