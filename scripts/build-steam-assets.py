#!/usr/bin/env python3
"""Emit Terra's steam-chain corpus resource and its two fluids' pack-side assets (#223, ADR-0048).

ADR-0048 is explicit that the Boiler and the Steam Engine (#224, #225) must be authored against
numbers that are *read*, not chosen: `data/factorio/machine.json`'s `boilers` array carries the
`boiler` prototype and its `generators` array carries `steam-engine` (both added by #188's widened
extractor). This script copies both rows -- whole, every field, nothing selected -- into
`mod/src/main/resources/factoryworks_core/fluid/steam_chain.json`, which
{@code SteamChainCorpus} reads at class-init the same way {@code PumpCorpus} reads `pumps.json` and
{@code RigCorpus} reads `mining/drills.json`.

**Nothing is decided here.** The script copies the rows whole and the tickets that consume them
pick their own fields out of what is already on disk. A hand-edited resource would run the Boiler
at a rate somebody chose, with nothing else failing; that is the one thing this script exists to
prevent.

#224 added two things to it: the two Factorio *fluid* rows the Boiler's rate is derived from -- the
rise is paid for at **steam's** heat capacity, and water's is ten times larger, so both are copied
rather than either being typed into Java -- and the Boiler block's own pack-side
blockstate/model/lang/loot-table plumbing, which under ADR-0015's split is the pack's rather than
the mod's. #225's Steam Engine will add its own.

Alongside the corpus copy, this script writes the two fluids' `fluid_type` lang keys. Registration
itself -- the `Fluid`, the `FluidType` and the `LiquidBlock` -- is mechanism (ADR-0015) and lives in
the mod as ordinary Java; only the display names are pack-side data.

**Neither fluid has a bucket**, so there is no bucket model and no bucket lang key to write.
ADR-0037 already answered portable fluid for this pack -- `factoryworks:barrel`, any fluid at
Factorio's own 50 mB -- and states that capacity as a rule a later container "does not get to be
re-argued from Minecraft's bucket" against. See `PFFluids`' javadoc.

Usage:

    scripts/build-steam-assets.py            # writes the resource and the pack assets
    scripts/build-steam-assets.py --check    # asserts both are already up to date; no writes
"""
import argparse
import json
import math
import os
import struct
import sys
import zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
MACHINE_CORPUS = os.path.join(ROOT, "data", "factorio", "machine.json")
FLUID_CORPUS = os.path.join(ROOT, "data", "factorio", "fluid.json")
STEAM_CHAIN_RESOURCE = os.path.join(
    ROOT, "mod", "src", "main", "resources", "factoryworks_core", "fluid", "steam_chain.json"
)
ASSETS = os.path.join(ROOT, "kubejs", "assets", "factoryworks")
DATA = os.path.join(ROOT, "kubejs", "data", "factoryworks")
NAMESPACE = "factoryworks"

BOILER_NAME = "boiler"
STEAM_ENGINE_NAME = "steam-engine"

# The two fluids the Boiler's arithmetic is derived from (#224). `water` is what it consumes and
# `steam` what it makes, and the governing constant is *steam's* heat capacity rather than water's
# -- see `BoilerSpec`, which is where the trap is written down. Copied whole, the same rule the two
# machine rows follow: a heat capacity typed into Java is a rate nobody can check.
FLUID_NAMES = ("water", "steam")

BOILER_BLOCK = "boiler"
BOILER_DISPLAY = "Boiler"

# Display choices, not numbers ADR-0022 governs -- the same vanilla art the pump and the rigs use.
BOILER_TEXTURES = {
    "front": "minecraft:block/furnace_front_on",
    "side": "minecraft:block/blast_furnace_side",
    "top": "minecraft:block/blast_furnace_top",
}

FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}

# The suite's fluid port convention (agreed 2026-10-04, shared with Craftworks' machines): a ring on
# the machine's own casing, blue where a fluid goes in and orange where one comes out. Stand-in art.
PORT_RINGS = {
    "fluid_port_input": {"fill": (52, 124, 236), "edge": (20, 52, 116)},
    "fluid_port_output": {"fill": (244, 140, 28), "edge": (124, 62, 8)},
}
TEXTURES = os.path.join(ASSETS, "textures", "block")

# BoilerFootprint's parts, numbered as Footprint.of numbers them (the anchor is 0), and the local face
# each port opens outward on (BoilerPorts.opens). Local x runs backward from the front.
BOILER_PARTS = [(0, 0, -1), (0, 0, 1), (1, 0, -1), (1, 0, 0), (1, 0, 1)]
BOILER_PORT_FACES = {1: ("water", (0, 0, -1)), 2: ("water", (0, 0, 1)), 4: ("steam", (1, 0, 0))}
PORT_RING_OF = {"water": "fluid_port_input", "steam": "fluid_port_output"}
MAX_PARTS = 26  # FootprintPartBlock.PART's range: every state needs a variant

# The Boiler's screen, which is the furnace ladder's screen with a second gauge on it. The keys are
# pack-side beside the block name for the reason the pump's refusal message is: they name the block
# and read as part of it.
BOILER_LANG = {
    f"block.{NAMESPACE}.{BOILER_BLOCK}": BOILER_DISPLAY,
    f"block.{NAMESPACE}.{BOILER_BLOCK}_part": f"{BOILER_DISPLAY} (part)",
    f"tooltip.{NAMESPACE}.boiler.fuel": "%s / %s J",
    f"tooltip.{NAMESPACE}.boiler.fuel.seconds": "%s s at %s J/t",
    f"tooltip.{NAMESPACE}.boiler.fuel.out": "Out of fuel",
    f"tooltip.{NAMESPACE}.boiler.water": "Water: %s / %s mB",
    f"tooltip.{NAMESPACE}.boiler.steam": "Steam: %s / %s mB",
}

# The two fluids ADR-0048 registers. Both `factoryworks:`, never `gtceu:steam` -- see the ADR.
FLUIDS = {
    "steam": "Steam",
    "superheated_steam": "Superheated Steam",
}

FLUID_LANG = {f"fluid_type.{NAMESPACE}.{name}": display for name, display in FLUIDS.items()}


def fluids_from_corpus():
    """The two fluid prototypes the Boiler's arithmetic reads, copied whole."""
    with open(FLUID_CORPUS, encoding="utf-8") as handle:
        rows = {row["name"]: row for row in json.load(handle).get("fluids", [])}
    fluids = {}
    for name in FLUID_NAMES:
        row = rows.get(name)
        if row is None:
            sys.exit(
                f"{name} is not in {FLUID_CORPUS} -- re-run scripts/factorio-fluid-extract.py"
            )
        fluids[name] = row
    return fluids


def steam_chain_from_corpus():
    with open(MACHINE_CORPUS, encoding="utf-8") as handle:
        machine = json.load(handle)
    boilers = {row["name"]: row for row in machine.get("boilers", [])}
    generators = {row["name"]: row for row in machine.get("generators", [])}

    boiler = boilers.get(BOILER_NAME)
    if boiler is None:
        sys.exit(
            f"{BOILER_NAME} is not in {MACHINE_CORPUS}'s boilers "
            "-- re-run scripts/factorio-machine-extract.py"
        )
    steam_engine = generators.get(STEAM_ENGINE_NAME)
    if steam_engine is None:
        sys.exit(
            f"{STEAM_ENGINE_NAME} is not in {MACHINE_CORPUS}'s generators "
            "-- re-run scripts/factorio-machine-extract.py"
        )

    # Whole rows, copied rather than filtered: see the module docstring.
    return {
        BOILER_NAME: boiler,
        STEAM_ENGINE_NAME: steam_engine,
        "fluids": fluids_from_corpus(),
    }


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2)
        handle.write("\n")


def blockstate(model_name):
    return {
        "variants": {
            f"facing={facing}": ({"model": model_name} if y == 0 else {"model": model_name, "y": y})
            for facing, y in FACINGS.items()
        }
    }


def oriented_model():
    """A model with a distinct front face, so the Boiler's fuel side is visible on the block."""
    return {
        "parent": "minecraft:block/orientable",
        "textures": {
            "front": BOILER_TEXTURES["front"],
            "side": BOILER_TEXTURES["side"],
            "top": BOILER_TEXTURES["top"],
            "particle": BOILER_TEXTURES["side"],
        },
    }


def rotate(local, facing):
    """Oritech's Geometry.rotatePosition, which places the footprint and opens its ports."""
    x, y, z = local
    return {
        "north": (z, y, x),
        "west": (x, y, -z),
        "south": (-z, y, -x),
        "east": (-x, y, z),
    }[facing]


# The y a model facing north is turned by to face this way.
ROTATION_TO = {(0, 0, -1): 0, (1, 0, 0): 90, (0, 0, 1): 180, (-1, 0, 0): 270}


def part_blockstate():
    """Casing on every part, a port model turned to the face each port opens on."""
    casing = {"model": f"{NAMESPACE}:block/{BOILER_BLOCK}_part"}
    variants = {}
    for facing in FACINGS:
        for part in range(1, MAX_PARTS + 1):
            port = BOILER_PORT_FACES.get(part)
            if port is None:
                variant = casing
            else:
                kind, face = port
                y = ROTATION_TO[rotate(face, facing)]
                variant = {"model": f"{NAMESPACE}:block/{BOILER_BLOCK}_port_{kind}"}
                if y:
                    variant["y"] = y
            variants[f"facing={facing},part={part}"] = variant
    return {"variants": variants}


def port_model(ring):
    """The part's casing with the port's ring laid just outside its north face."""
    casing = "minecraft:block/iron_block"
    return {
        "render_type": "minecraft:cutout",
        "textures": {"casing": casing, "ring": f"{NAMESPACE}:block/{ring}", "particle": casing},
        "elements": [
            {
                "from": [0, 0, 0],
                "to": [16, 16, 16],
                "faces": {d: {"texture": "#casing", "cullface": d}
                          for d in ("down", "up", "north", "south", "west", "east")},
            },
            {
                "from": [0, 0, -0.01],
                "to": [16, 16, -0.01],
                "faces": {"north": {"texture": "#ring", "cullface": "north"}},
            },
        ],
    }


def ring_pixels(fill, edge):
    """A 16x16 ring, its outer and inner rims in the darker edge colour, transparent elsewhere."""
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            if 4.0 <= r <= 7.0:
                colour = edge if r < 5.0 or r > 6.2 else fill
                row.append((*colour, 255))
            else:
                row.append((0, 0, 0, 0))
        rows.append(row)
    return rows


def png_bytes(rows):
    """An RGBA PNG, written with the standard library so the generator needs nothing installed."""
    raw = b"".join(b"\x00" + bytes(c for px in row for c in px) for row in rows)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    header = struct.pack(">IIBBBBB", len(rows[0]), len(rows), 8, 6, 0, 0, 0)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header)
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def planned_textures():
    return {os.path.join(TEXTURES, f"{name}.png"): png_bytes(ring_pixels(**colours))
            for name, colours in PORT_RINGS.items()}


def self_drop_loot_table(block_id):
    return {
        "type": "minecraft:block",
        "pools": [
            {
                "rolls": 1,
                "entries": [{"type": "minecraft:item", "name": block_id}],
            }
        ],
    }


def planned_files(rows):
    files = {STEAM_CHAIN_RESOURCE: rows}
    model_name = f"{NAMESPACE}:block/{BOILER_BLOCK}"
    files[os.path.join(ASSETS, "blockstates", f"{BOILER_BLOCK}.json")] = blockstate(model_name)
    # Iron until the 3x2 model replaces it (#595); the ports ringed in the suite's colours.
    files[os.path.join(ASSETS, "blockstates", f"{BOILER_BLOCK}_part.json")] = part_blockstate()
    for kind, ring in PORT_RING_OF.items():
        files[os.path.join(ASSETS, "models", "block", f"{BOILER_BLOCK}_port_{kind}.json")] = port_model(ring)
    files[os.path.join(ASSETS, "models", "block", f"{BOILER_BLOCK}_part.json")] = {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": "minecraft:block/iron_block"},
    }
    files[os.path.join(ASSETS, "models", "block", f"{BOILER_BLOCK}.json")] = oriented_model()
    files[os.path.join(ASSETS, "models", "item", f"{BOILER_BLOCK}.json")] = {"parent": model_name}
    files[os.path.join(DATA, "loot_table", "blocks", f"{BOILER_BLOCK}.json")] = self_drop_loot_table(
        f"{NAMESPACE}:{BOILER_BLOCK}"
    )
    lang = dict(FLUID_LANG)
    lang.update(BOILER_LANG)
    return files, lang


def lang_path():
    return os.path.join(ASSETS, "lang", "en_us.json")


def check():
    files, lang = planned_files(steam_chain_from_corpus())
    problems = []
    for path, expected in files.items():
        if not os.path.isfile(path):
            problems.append(f"missing: {path}")
            continue
        with open(path, encoding="utf-8") as handle:
            actual = json.load(handle)
        if actual != expected:
            problems.append(f"stale: {path}")

    for path, expected in planned_textures().items():
        if not os.path.isfile(path):
            problems.append(f"missing: {path}")
        elif open(path, "rb").read() != expected:
            problems.append(f"stale: {path}")

    existing_lang = {}
    if os.path.isfile(lang_path()):
        with open(lang_path(), encoding="utf-8") as handle:
            existing_lang = json.load(handle)
    for key, value in lang.items():
        if existing_lang.get(key) != value:
            problems.append(f"lang key out of date: {key}")

    if problems:
        sys.exit("build-steam-assets.py --check failed:\n" + "\n".join(problems))
    print(f"OK -- {len(files)} files, {len(lang)} lang keys")


def build():
    files, lang = planned_files(steam_chain_from_corpus())
    for path, data in files.items():
        write(path, data)
    for path, data in planned_textures().items():
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as handle:
            handle.write(data)

    existing_lang = {}
    if os.path.isfile(lang_path()):
        with open(lang_path(), encoding="utf-8") as handle:
            existing_lang = json.load(handle)
    existing_lang.update(lang)
    write(lang_path(), existing_lang)
    print(f"wrote {len(files)} files, updated {len(lang)} lang keys")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    check() if args.check else build()


if __name__ == "__main__":
    main()
