#!/usr/bin/env python3
"""Terra spawns no vanilla mob on its own (#480, ADR-0093).

Every biome the live overworld names, through the dimension and through the world preset, has
empty spawner lists, and the noise settings place no animal at chunk generation. Read from the
generated files, after the generator's `--check` holds them to the generator.

The game rules that stop the custom spawners are `gametest/SpawningRuleTests`'.
"""

import json
import os
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DATA = os.path.join(ROOT, "kubejs", "data")
MC = os.path.join(DATA, "minecraft")


def read(path):
    with open(path, encoding="utf-8") as handle:
        return json.load(handle)


def overworlds():
    yield "dimension/overworld.json", read(os.path.join(MC, "dimension", "overworld.json"))
    preset = read(os.path.join(MC, "world_preset", "normal.json"))
    yield "world_preset/normal.json", preset["dimensions"]["minecraft:overworld"]


def main():
    failures = []

    check = subprocess.run(
        [sys.executable, os.path.join(ROOT, "scripts", "build-terra-worldgen.py"), "--check"],
        capture_output=True, text=True,
    )
    if check.returncode != 0:
        failures.append("build-terra-worldgen.py --check failed:\n" + check.stdout + check.stderr)

    biomes = set()
    for where, dimension in overworlds():
        generator = dimension["generator"]
        settings = generator["settings"]
        if settings != "minecraft:overworld":
            failures.append(f"{where} uses noise settings {settings}, which this check does not read")
        for entry in generator["biome_source"]["biomes"]:
            biomes.add(entry["biome"])
    if not biomes:
        failures.append("the live overworld names no biome")

    for biome in sorted(biomes):
        namespace, path = biome.split(":", 1)
        data = read(os.path.join(DATA, namespace, "worldgen", "biome", path + ".json"))
        spawning = {category: mobs for category, mobs in data["spawners"].items() if mobs}
        if spawning:
            failures.append(f"{biome} spawns {spawning}")
        if data.get("spawn_costs"):
            failures.append(f"{biome} has spawn costs {data['spawn_costs']}")

    noise = read(os.path.join(MC, "worldgen", "noise_settings", "overworld.json"))
    if noise.get("disable_mob_generation") is not True:
        failures.append("noise_settings/overworld.json places animals at chunk generation"
                        f" (disable_mob_generation = {noise.get('disable_mob_generation')})")

    for failure in failures:
        print("FAIL:", failure)
    if failures:
        sys.exit(1)
    print(f"ok: {len(biomes)} live Terra biomes spawn nothing, and chunk generation places no animal")


if __name__ == "__main__":
    main()
