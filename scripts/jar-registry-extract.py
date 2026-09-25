#!/usr/bin/env python3
"""Extract every item and fluid id the installed jars register into `data/jars/` (#453, ADR-0088).

Reads Minecraft's client jar and every jar in `mods/` except the pack's own, whose ids are the
repo's and are resolved from its sources. The corpus is committed, so a jar update arrives as a
diff to review, and `scripts/build-obtainable-index.py` never opens a jar.

An item is an item model definition, `assets/<ns>/items/<path>.json`: 26.1 draws every item through
one, so every item a jar ships has one. A fluid has no such file. It is a source fluid a fluid tag
names, or one a `fluid.<ns>.<path>` lang key names, which is how Oritech keys its fluids; flowing
variants are dropped, since EMI lists only the source.

Usage:

    scripts/jar-registry-extract.py            # writes data/jars/item.json and fluid.json
    scripts/jar-registry-extract.py --check    # re-extracts and diffs; skips when the jars are absent
"""
import argparse
import json
import os
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MODS = ROOT / "mods"
VANILLA = Path(os.environ.get("PF_CLIENT_JAR", os.path.expanduser(
    "~/curseforge/Install/versions/26.1.2/26.1.2.jar")))
OUT = ROOT / "data" / "jars"
PACK_JAR = "planetaryfactory_core-"

ITEM_DEFINITION = re.compile(r"assets/([a-z0-9_.-]+)/items/([a-z0-9_./-]+)\.json")
FLUID_TAG = re.compile(r"data/[a-z0-9_.-]+/tags/fluid/.+\.json")
LANG = re.compile(r"assets/([a-z0-9_.-]+)/lang/en_us\.json")


def jars():
    found = [path for path in sorted(MODS.glob("*.jar")) if not path.name.startswith(PACK_JAR)]
    return [VANILLA] + found if VANILLA.is_file() and found else []


def read_json(archive, name):
    try:
        return json.loads(archive.read(name))
    except (ValueError, UnicodeDecodeError):
        return None


def source_fluid(fluid_id):
    return not fluid_id.split(":", 1)[1].startswith("flowing_") and fluid_id != "minecraft:empty"


def extract(archive):
    items, fluids = set(), set()
    for name in archive.namelist():
        if found := ITEM_DEFINITION.fullmatch(name):
            items.add(f"{found[1]}:{found[2]}")
        elif FLUID_TAG.fullmatch(name):
            values = (read_json(archive, name) or {}).get("values", [])
            fluids |= {value for value in values
                       if isinstance(value, str) and not value.startswith("#")}
        elif found := LANG.fullmatch(name):
            namespace = re.escape(found[1])
            for key in read_json(archive, name) or {}:
                if lang := re.fullmatch(rf"fluid\.({namespace})\.([a-z0-9_]+)", key):
                    fluids.add(f"{lang[1]}:{lang[2]}")
    return items, {fluid for fluid in fluids if source_fluid(fluid)}


def corpus():
    items, fluids = {}, {}
    for jar in jars():
        with zipfile.ZipFile(jar) as archive:
            jar_items, jar_fluids = extract(archive)
        for by_jar, ids in ((items, jar_items), (fluids, jar_fluids)):
            if ids:
                by_jar[jar.name] = ids
    return {
        "item.json": render("items", items),
        "fluid.json": render("fluids", fluids),
    }


def render(field, by_jar):
    data = {"jars": sorted(by_jar), field: sorted(set().union(*by_jar.values()))}
    return json.dumps(data, indent=2) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    if not jars():
        if args.check:
            print(f"skip: no client jar at {VANILLA} or no jar in mods/")
            return
        sys.exit(f"no client jar at {VANILLA} or no jar in mods/ -- set PF_CLIENT_JAR or install")
    files = corpus()
    if args.check:
        stale = [name for name, text in files.items()
                 if not (OUT / name).is_file() or (OUT / name).read_text(encoding="utf-8") != text]
        if stale:
            sys.exit(f"stale: data/jars/{', '.join(stale)} -- run scripts/jar-registry-extract.py "
                     "and review the diff")
        print("OK -- " + ", ".join(f"{len(json.loads(text)[key])} {key}"
                                   for key, text in zip(("items", "fluids"), files.values())))
        return
    OUT.mkdir(parents=True, exist_ok=True)
    for name, text in files.items():
        (OUT / name).write_text(text, encoding="utf-8")
        print(f"wrote data/jars/{name}")


if __name__ == "__main__":
    main()
