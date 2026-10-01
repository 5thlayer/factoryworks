#!/usr/bin/env python3
"""Emit the wreck's corpus row and pack-side assets (ADR-0107).

The cargo hold's slot count is Factorio's `crash-site-spaceship` `inventory_size`. This copies the
row out of `data/factorio/container.json` into
`mod/src/main/resources/factoryworks_core/wreck/containers.json`, which `CargoHoldCorpus` reads,
and writes the blockstate, model and lang name of the three wreck blocks. None has an item, a loot
table or an item model.

Usage:

    scripts/build-wreck-assets.py            # writes the resource and the pack assets
    scripts/build-wreck-assets.py --check    # asserts both are already up to date; no writes
"""
import argparse
import json
import os
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
CONTAINER_CORPUS = os.path.join(ROOT, "data", "factorio", "container.json")
RESOURCE = os.path.join(
    ROOT, "mod", "src", "main", "resources", "factoryworks_core", "wreck", "containers.json"
)
ASSETS = os.path.join(ROOT, "kubejs", "assets", "factoryworks")
NAMESPACE = "factoryworks"

FACTORIO_NAME = "crash-site-spaceship"
REQUIRED_FIELDS = ("inventory_size",)

# Display choices: vanilla art until the wreck has its own.
BLOCKS = {
    "wreck_hull": ("Wreck Hull", "minecraft:block/iron_block"),
    "wreck_window": ("Wreck Window", "minecraft:block/tinted_glass"),
    "cargo_hold": ("Cargo Hold", "minecraft:block/chiseled_copper"),
}


def row_from_corpus():
    with open(CONTAINER_CORPUS, encoding="utf-8") as handle:
        corpus = json.load(handle)
    rows = {row["name"]: row for row in corpus.get("containers", [])}
    row = rows.get(FACTORIO_NAME)
    if row is None:
        sys.exit(f"{FACTORIO_NAME} is not in {CONTAINER_CORPUS} "
                 "-- re-run scripts/factorio-container-extract.py")
    for field in REQUIRED_FIELDS:
        if row.get(field) is None:
            sys.exit(f"{FACTORIO_NAME} has no {field} in {CONTAINER_CORPUS} "
                     "-- re-run scripts/factorio-container-extract.py")
    return {FACTORIO_NAME: {field: row[field] for field in REQUIRED_FIELDS}}


def planned_files():
    files = {RESOURCE: row_from_corpus()}
    for name, (_, texture) in BLOCKS.items():
        model = f"{NAMESPACE}:block/{name}"
        files[os.path.join(ASSETS, "blockstates", f"{name}.json")] = {
            "variants": {"": {"model": model}}
        }
        files[os.path.join(ASSETS, "models", "block", f"{name}.json")] = {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": texture},
        }
    lang = {f"block.{NAMESPACE}.{name}": display for name, (display, _) in BLOCKS.items()}
    return files, lang


def lang_path():
    return os.path.join(ASSETS, "lang", "en_us.json")


def read_json(path):
    with open(path, encoding="utf-8") as handle:
        return json.load(handle)


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2)
        handle.write("\n")


def check():
    files, lang = planned_files()
    problems = []
    for path, expected in files.items():
        if not os.path.isfile(path):
            problems.append(f"missing: {path}")
        elif read_json(path) != expected:
            problems.append(f"stale: {path}")
    existing = read_json(lang_path()) if os.path.isfile(lang_path()) else {}
    for key, value in lang.items():
        if existing.get(key) != value:
            problems.append(f"lang key out of date: {key}")
    if problems:
        sys.exit("build-wreck-assets.py --check failed:\n" + "\n".join(problems))
    print(f"OK -- {len(files)} files, {len(lang)} lang keys")


def build():
    files, lang = planned_files()
    for path, data in files.items():
        write(path, data)
    existing = read_json(lang_path()) if os.path.isfile(lang_path()) else {}
    existing.update(lang)
    write(lang_path(), existing)
    print(f"wrote {len(files)} files, updated {len(lang)} lang keys")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    check() if args.check else build()


if __name__ == "__main__":
    main()
