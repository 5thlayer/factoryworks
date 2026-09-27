#!/usr/bin/env python3
"""Extract what the installed jars register and drop into `data/jars/` (#453, #454, ADR-0088).

Reads Minecraft's client jar and every jar in `mods/` except the pack's own, whose ids are the
repo's and are resolved from its sources. The corpus is committed, so a jar update arrives as a
diff to review, and `scripts/build-obtainable-index.py` never opens a jar.

An item is an item model definition, `assets/<ns>/items/<path>.json`: 26.1 draws every item through
one, so every item a jar ships has one. A fluid has no such file. It is a source fluid a fluid tag
names, or one a `fluid.<ns>.<path>` lang key names, which is how Oritech keys its fluids; flowing
variants are dropped, since EMI lists only the source.

A block loot table keeps only its pools' entries and conditions. A placed or configured feature keeps the block states it places and the
features it names. Block states under a predicate or a placement are what a feature tests for,
not what it places, so they are left out.

Usage:

    scripts/jar-registry-extract.py            # writes data/jars/*.json
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
BLOCK_LOOT = re.compile(r"data/([a-z0-9_.-]+)/loot_table/(blocks/[a-z0-9_./-]+)\.json")
FEATURE = re.compile(
    r"data/([a-z0-9_.-]+)/worldgen/(placed|configured)_feature/([a-z0-9_./-]+)\.json")
NOT_PLACED = {"placement", "predicate", "target", "if_true", "replaceable_blocks",
              "valid_base_block"}


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
    items, fluids, loot, features = set(), set(), {}, {}
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
        elif found := BLOCK_LOOT.fullmatch(name):
            if (table := read_json(archive, name)) is not None:
                loot[f"{found[1]}:{found[2]}"] = reduce_loot(table)
        elif found := FEATURE.fullmatch(name):
            if (feature := read_json(archive, name)) is not None:
                features[(found[2], f"{found[1]}:{found[3]}")] = feature
    return items, {fluid for fluid in fluids if source_fluid(fluid)}, loot, features


def reduce_condition(condition):
    reduced = {"condition": condition["condition"]}
    if condition["condition"] == "minecraft:match_tool":
        reduced["predicate"] = condition.get("predicate", {})
    if "term" in condition:
        reduced["term"] = reduce_condition(condition["term"])
    if "terms" in condition:
        reduced["terms"] = [reduce_condition(term) for term in condition["terms"]]
    return reduced


def reduce_entry(entry):
    reduced = {"type": entry["type"]}
    if "name" in entry:
        reduced["name"] = entry["name"]
    if entry.get("conditions"):
        reduced["conditions"] = [reduce_condition(c) for c in entry["conditions"]]
    if "children" in entry:
        reduced["children"] = [reduce_entry(child) for child in entry["children"]]
    return reduced


def reduce_loot(table):
    pools = []
    for pool in table.get("pools", []):
        reduced = {"entries": [reduce_entry(entry) for entry in pool.get("entries", [])]}
        if pool.get("conditions"):
            reduced["conditions"] = [reduce_condition(c) for c in pool["conditions"]]
        pools.append(reduced)
    return pools


def reduce_feature(kind, feature, placed_ids):
    found = {"blocks": set(), "configured": set(), "placed": set(), "types": set()}

    def placed(value):
        inner = value.get("feature")
        if isinstance(inner, str):
            found["configured"].add(inner)
        elif isinstance(inner, dict):
            configured(inner)

    def configured(value):
        found["types"].add(value.get("type"))
        walk(value.get("config"), None)

    def walk(value, key):
        if isinstance(value, dict):
            if isinstance(value.get("Name"), str):
                found["blocks"].add(value["Name"])
            elif "feature" in value and "placement" in value:
                placed(value)
            elif "type" in value and "config" in value:
                configured(value)
            else:
                for inner_key, inner in value.items():
                    if inner_key != "type" and inner_key not in NOT_PLACED \
                            and not inner_key.endswith("predicate"):
                        walk(inner, inner_key)
                if key and key.endswith("decorators"):
                    found["types"].add(value.get("type"))
        elif isinstance(value, list):
            for inner in value:
                walk(inner, key)
        elif isinstance(value, str) and value in placed_ids:
            found["placed"].add(value)

    placed(feature) if kind == "placed" else configured(feature)
    return {field: sorted(ids) for field, ids in found.items() if ids}


def corpus():
    items, fluids, loot, features, feature_jars = {}, {}, {}, {}, set()
    for jar in jars():
        with zipfile.ZipFile(jar) as archive:
            jar_items, jar_fluids, jar_loot, jar_features = extract(archive)
        for by_jar, ids in ((items, jar_items), (fluids, jar_fluids)):
            if ids:
                by_jar[jar.name] = ids
        if jar_loot:
            loot[jar.name] = jar_loot
        if jar_features:
            feature_jars.add(jar.name)
            features.update(jar_features)
    placed_ids = {name for kind, name in features if kind == "placed"}
    graph = {"placed": {}, "configured": {}}
    for (kind, name), feature in sorted(features.items()):
        graph[kind][name] = reduce_feature(kind, feature, placed_ids)
    tables = {}
    for jar_loot in loot.values():
        tables.update(jar_loot)
    return {
        "item.json": render("items", items),
        "fluid.json": render("fluids", fluids),
        "loot.json": by_line({"jars": sorted(loot), "tables": dict(sorted(tables.items()))}),
        "feature.json": by_line({"jars": sorted(feature_jars), **graph}),
    }


def render(field, by_jar):
    data = {"jars": sorted(by_jar), field: sorted(set().union(*by_jar.values()))}
    return json.dumps(data, indent=2) + "\n"


def by_line(data):
    """One id a line, so a jar update diffs as the ids it changed."""
    fields = []
    for field, value in data.items():
        if isinstance(value, dict):
            lines = ",\n".join(f"    {json.dumps(key)}: {json.dumps(inner, separators=(',', ':'))}"
                               for key, inner in value.items())
            fields.append(f'  "{field}": {{\n{lines}\n  }}')
        else:
            fields.append(f'  "{field}": {json.dumps(value)}')
    return "{\n" + ",\n".join(fields) + "\n}\n"


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
        print("OK -- " + ", ".join(f"{len(json.loads(files[name])[key])} {key}" for name, key in (
            ("item.json", "items"), ("fluid.json", "fluids"), ("loot.json", "tables"),
            ("feature.json", "placed"), ("feature.json", "configured"))))
        return
    OUT.mkdir(parents=True, exist_ok=True)
    for name, text in files.items():
        (OUT / name).write_text(text, encoding="utf-8")
        print(f"wrote data/jars/{name}")


if __name__ == "__main__":
    main()
