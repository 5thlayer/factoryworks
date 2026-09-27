"""A minimal NBT writer and reader, enough for a Minecraft structure template.

The pack authors its worldgen as data rather than in-game, and a structure template is the
one worldgen file that is not JSON. Rather than build the starting area by hand in a creative
world and export it with a structure block -- which makes the layout unreviewable in a diff
and unregenerable after a tuning change -- the templates are generated, and this is the
writer they go through.

The writer implements only the tags a structure template uses: byte, int, string, list and
compound. The reader takes every tag a template can hold.
"""

import gzip
import struct

TAG_END = 0
TAG_BYTE = 1
TAG_INT = 3
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10


class Int(int):
    """An int that must be written as TAG_Int even where a bare int would fit a byte."""


def _utf8(value):
    raw = value.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw


def _tag_id(value):
    if isinstance(value, Int):
        return TAG_INT
    if isinstance(value, bool):
        return TAG_BYTE
    if isinstance(value, int):
        return TAG_INT
    if isinstance(value, str):
        return TAG_STRING
    if isinstance(value, list):
        return TAG_LIST
    if isinstance(value, dict):
        return TAG_COMPOUND
    raise TypeError("no NBT tag for %r" % (value,))


def _payload(value):
    tag = _tag_id(value)
    if tag == TAG_BYTE:
        return struct.pack(">b", 1 if value else 0)
    if tag == TAG_INT:
        return struct.pack(">i", int(value))
    if tag == TAG_STRING:
        return _utf8(value)
    if tag == TAG_LIST:
        # An empty list is TAG_End-typed, which is what vanilla writes and reads back as
        # empty regardless of the type the field expects.
        element = _tag_id(value[0]) if value else TAG_END
        for item in value:
            if _tag_id(item) != element:
                raise TypeError("heterogeneous NBT list")
        body = b"".join(_payload(item) for item in value)
        return struct.pack(">Bi", element, len(value)) + body
    if tag == TAG_COMPOUND:
        body = b""
        for name, item in value.items():
            body += struct.pack(">B", _tag_id(item)) + _utf8(name) + _payload(item)
        return body + struct.pack(">B", TAG_END)
    raise TypeError("no NBT payload for %r" % (value,))


def write(path, root):
    """Write `root` (a dict) as a gzipped, unnamed root compound -- the structure format."""
    data = struct.pack(">B", TAG_COMPOUND) + _utf8("") + _payload(root)
    with gzip.GzipFile(path, "wb", mtime=0) as handle:
        handle.write(data)


def read(path):
    """Read a gzipped structure template into dicts and lists."""
    with gzip.open(path, "rb") as handle:
        data = handle.read()
    pos = [0]

    def take(n):
        chunk = data[pos[0]:pos[0] + n]
        pos[0] += n
        return chunk

    def name():
        return take(struct.unpack(">H", take(2))[0]).decode("utf8")

    def value(tag):
        if tag == 1:
            return struct.unpack(">b", take(1))[0]
        if tag == 2:
            return struct.unpack(">h", take(2))[0]
        if tag == 3:
            return struct.unpack(">i", take(4))[0]
        if tag == 4:
            return struct.unpack(">q", take(8))[0]
        if tag == 5:
            return struct.unpack(">f", take(4))[0]
        if tag == 6:
            return struct.unpack(">d", take(8))[0]
        if tag == 7:
            return take(struct.unpack(">i", take(4))[0])
        if tag == 8:
            return name()
        if tag == 9:
            element = take(1)[0]
            return [value(element) for _ in range(struct.unpack(">i", take(4))[0])]
        if tag == 10:
            out = {}
            while True:
                inner = take(1)[0]
                if inner == 0:
                    return out
                # Name first, deliberately: `out[name()] = value(inner)` would read the
                # payload before the key, because Python evaluates the right side first.
                key = name()
                out[key] = value(inner)
        if tag == 11:
            return [struct.unpack(">i", take(4))[0]
                    for _ in range(struct.unpack(">i", take(4))[0])]
        raise AssertionError("unhandled tag %d" % tag)

    assert take(1)[0] == 10
    name()
    return value(10)
