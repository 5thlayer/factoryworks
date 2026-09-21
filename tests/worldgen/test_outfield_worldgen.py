#!/usr/bin/env python3
"""Terra's outfield structure sets: one per resource, spaced by Factorio's corpus (#320).

The generator's `--check` holds the files to the generator. This holds the generator to the
corpus, and the files to the mod: spacing and separation are re-derived here from
`data/factorio/resource.json`, the structure type is the one `PFWorldgen` registers, and the
biomes are the land tag, which holds no sea.

Whether a set resolves from a running server's registry and places a disc is the GameTest's
(`OutfieldDiscTests`).
"""

import json
import os
import re
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
WORLDGEN = os.path.join(ROOT, "kubejs", "data", "planetaryfactory", "worldgen")
CORPUS = os.path.join(ROOT, "data", "factorio", "resource.json")
LAND_TAG = os.path.join(ROOT, "kubejs", "data", "planetaryfactory", "tags", "worldgen", "biome", "terra_land.json")
PF_WORLDGEN = os.path.join(
    ROOT, "mod", "src", "main", "java", "com", "planetaryfactory", "core", "worldgen", "PFWorldgen.java"
)

RESOURCES = {"coal": "coal", "copper": "copper-ore", "iron": "iron-ore", "stone": "stone"}
CHUNK = 16


def read(path):
    with open(path, encoding="utf-8") as handle:
        return json.load(handle)


def main():
    failures = []
    check = subprocess.run(
        [sys.executable, os.path.join(ROOT, "scripts", "build-outfield-worldgen.py"), "--check"],
        capture_output=True, text=True,
    )
    if check.returncode:
        failures.append("the outfield worldgen is stale:\n" + check.stdout + check.stderr)

    corpus = read(CORPUS)
    by_name = {entry["name"]: entry for entry in corpus["resources"]}
    candidate = corpus["outfield_placement"]["suggested_minimum_candidate_point_spacing"]
    registered = re.search(r'STRUCTURE_TYPES\.register\("([a-z_]+)"', open(PF_WORLDGEN, encoding="utf-8").read())
    land = read(LAND_TAG)["values"]
    if any(biome.endswith("terra_sea") for biome in land):
        failures.append("the land tag holds the sea")

    sets = sorted(name for name in os.listdir(os.path.join(WORLDGEN, "structure_set")) if name.startswith("outfield_"))
    expected = sorted(f"outfield_{block}.json" for block in RESOURCES)
    if sets != expected:
        failures.append(f"outfield structure sets are {sets}, not one per resource {expected} (uranium waits on #321)")

    salts = set()
    for block, factorio in RESOURCES.items():
        name = f"outfield_{block}"
        structure = read(os.path.join(WORLDGEN, "structure", f"{name}.json"))
        placement = read(os.path.join(WORLDGEN, "structure_set", f"{name}.json"))["placement"]
        mean = by_name[factorio]["outfield"]["mean_spacing"]
        if abs(placement["spacing"] * CHUNK - mean) > CHUNK / 2:
            failures.append(f"{name}'s spacing {placement['spacing']} chunks is not its mean spacing {mean:.0f} blocks")
        if placement["separation"] * CHUNK < candidate or (placement["separation"] - 1) * CHUNK >= candidate:
            failures.append(f"{name}'s separation {placement['separation']} is not {candidate:.1f} blocks in whole chunks")
        if placement["type"] != "minecraft:random_spread":
            failures.append(f"{name} is placed by {placement['type']}")
        salts.add(placement["salt"])
        if not registered or structure["type"] != f"planetaryfactory:{registered.group(1)}":
            failures.append(f"{name}'s type {structure['type']} is not the one PFWorldgen registers")
        if structure["resource"] != block:
            failures.append(f"{name} generates {structure['resource']}")
        if structure["biomes"] != "#planetaryfactory:terra_land":
            failures.append(f"{name} is confined to {structure['biomes']}, not Terra's land")
    if len(salts) != len(RESOURCES):
        failures.append("two outfield sets share a salt, and with it a grid")

    for failure in failures:
        print(f"FAIL  {failure}")
    if failures:
        return 1
    print(f"ok    {len(RESOURCES)} outfield structure sets, spaced and separated by the corpus")
    return 0


if __name__ == "__main__":
    sys.exit(main())
