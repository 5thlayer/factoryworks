#!/usr/bin/env python3
"""Emit Terra's outfield structures and structure sets, one per resource (#320, ADR-0045).

Each resource's patches are one `planetaryfactory:outfield_disc` structure, confined to Terra's
land biome tag, and one `random_spread` structure set. Both placement numbers are read out of
`data/factorio/resource.json`, not chosen:

  - `spacing` is the resource's `mean_spacing` -- one spot of mean size per that many blocks --
    in chunks, rounded. Random spread deals one structure per `spacing`-chunk square, so the
    realised density is Factorio's.
  - `separation` is the minimum distance Factorio's `spot_noise` keeps between candidate spots,
    `suggested_minimum_candidate_point_spacing`, in chunks, rounded up.

The salt is a hash of the set's id, so two resources never share a grid.

Usage:

    scripts/build-outfield-worldgen.py            # writes the files
    scripts/build-outfield-worldgen.py --check    # exits 1 if any is stale; no writes
"""
import argparse
import json
import math
import sys
import zlib
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
CORPUS = REPO / "data" / "factorio" / "resource.json"
WORLDGEN = REPO / "kubejs" / "data" / "planetaryfactory" / "worldgen"
NAMESPACE = "planetaryfactory"
CHUNK = 16

# The pack's block name and the corpus's resource.
RESOURCES = {
    "coal": "coal",
    "copper": "copper-ore",
    "iron": "iron-ore",
    "stone": "stone",
    "uranium": "uranium-ore",
}

LAND = f"#{NAMESPACE}:terra_land"

# After the trees, so the ground walk passes a trunk and puts the ore under it.
STEP = "top_layer_modification"


def emit():
    corpus = json.loads(CORPUS.read_text(encoding="utf-8"))
    by_name = {entry["name"]: entry for entry in corpus["resources"]}
    candidate = corpus["outfield_placement"]["suggested_minimum_candidate_point_spacing"]
    separation = math.ceil(candidate / CHUNK)
    files = {}
    for block, factorio in sorted(RESOURCES.items()):
        name = f"outfield_{block}"
        spacing = round(by_name[factorio]["outfield"]["mean_spacing"] / CHUNK)
        files[WORLDGEN / "structure" / f"{name}.json"] = {
            "type": f"{NAMESPACE}:outfield_disc",
            "resource": block,
            "biomes": LAND,
            "step": STEP,
            "spawn_overrides": {},
            "terrain_adaptation": "none",
        }
        files[WORLDGEN / "structure_set" / f"{name}.json"] = {
            "structures": [{"structure": f"{NAMESPACE}:{name}", "weight": 1}],
            "placement": {
                "type": "minecraft:random_spread",
                "spacing": spacing,
                "separation": separation,
                "salt": zlib.crc32(f"{NAMESPACE}:{name}".encode()) & 0x7FFFFFFF,
            },
        }
    return {path: json.dumps(body, indent=2) + "\n" for path, body in files.items()}


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true", help="exit 1 if any emitted file is stale")
    args = parser.parse_args()

    written = emit()
    if args.check:
        stale = [p for p, text in written.items() if not p.is_file() or p.read_text(encoding="utf-8") != text]
        for path in stale:
            print(f"stale      {path.relative_to(REPO)} -- re-run scripts/build-outfield-worldgen.py")
        if stale:
            sys.exit(1)
        print(f"ok         {len(written)} outfield worldgen files are current")
        return

    for path, text in written.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        print(f"wrote      {path.relative_to(REPO)}")


if __name__ == "__main__":
    main()
