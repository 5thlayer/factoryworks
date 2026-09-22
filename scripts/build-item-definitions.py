#!/usr/bin/env python3
"""Emit the item model definitions 26.1 resolves every item's model through (#273, ADR-0060).

WHAT CHANGED. Through 1.21.1 an item's model was `assets/<ns>/models/item/<id>.json`, found by
name. 26.1 puts a level of indirection in front of it: the game reads
`assets/<ns>/items/<id>.json`, an *item model definition*, and that file names the model. The
models are unchanged and still where they were; nothing reaches them without a definition. A
missing one is not an error -- the item renders as the black-and-magenta missing model, in the
inventory, in the hand and in EMI, with nothing in any log. The pack came through the port with
fifteen item models and no definitions at all, which is every item it registers.

WHY ONE GENERATOR RATHER THAN A LINE IN EACH. Every other generator here owns a subject -- the
Boiler's assets, the rig's, the pump's -- and the item models the pack ships come from four of
them plus a handful written by hand (the picks, the saplings, the poles, the furnaces, the
barrel). A definition is not a decision about any of those subjects: it is the same three fields
every time, mechanically derived from the model sitting beside it. So `items/` has exactly one
owner, and an item model landing from anywhere at all gets its definition by being there.

**It derives; it decides nothing.** `models/item/<id>.json` in, `items/<id>.json` out, pointing at
`<ns>:item/<id>`. An item wanting a definition of another shape -- a tint, a range dispatch, a
condition -- is a decision, and it stops being this script's and becomes its subject generator's.

The one exclusion is recorded in DEFERRED: `kubejs:oil_refinery` is the GregTech multiblock whose
registration left with ADR-0060, so its leftover model names an item that no longer exists. It is
re-derived with the machine chassis Oritech replaces it with (#258) and given a
definition then, not now.

Usage:

    scripts/build-item-definitions.py            # writes the definitions
    scripts/build-item-definitions.py --check    # asserts they are up to date; no writes
"""
import argparse
import json
import os
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")

# Every asset tree the game loads. `kubejs/parked/` has no assets and is deliberately absent.
ASSET_ROOTS = (
    os.path.join(ROOT, "kubejs", "assets"),
    os.path.join(ROOT, "mod", "src", "main", "resources", "assets"),
)

# `<namespace>:<id>` pairs whose item model is a leftover rather than a live item. Listed rather
# than skipped silently, so a stale model is a recorded deferral instead of an invisible one.
DEFERRED = {
    "kubejs:oil_refinery": "the GregTech multiblock's registration left with ADR-0060; Oritech is the chassis (#258)",
}


# `<namespace>:<id>` pairs whose definition is GeckoLib's special model rather than a plain one: the
# item is drawn by a GeoItem renderer, and a plain definition would reach a model whose parent is
# `builtin/entity` and draw nothing. Still derived, not decided -- the shape is the one Oritech's own
# `items/assembler.json` has, and the base is the item model beside it.
GECKOLIB = {
    "planetaryfactory:assembling_machine": "an OritechGeoItem drawing Oritech's assembler model (#326)",
    "planetaryfactory:assembling_machine_2": "an OritechGeoItem drawing Oritech's assembler model (#295)",
    "planetaryfactory:assembling_machine_3": "an OritechGeoItem drawing Oritech's assembler model (#295)",
    "planetaryfactory:steam_engine": "an OritechGeoItem drawing Oritech's steam engine model (#352)",
    "planetaryfactory:pumpjack": "an OritechGeoItem drawing Oritech's pump model (ADR-0081)",
}


def definitions():
    """Every definition the asset trees imply: (path, content), by the model beside it."""
    wanted = {}
    for root in ASSET_ROOTS:
        if not os.path.isdir(root):
            continue
        for namespace in sorted(os.listdir(root)):
            models = os.path.join(root, namespace, "models", "item")
            if not os.path.isdir(models):
                continue
            for entry in sorted(os.listdir(models)):
                if not entry.endswith(".json"):
                    continue
                item = entry[: -len(".json")]
                if f"{namespace}:{item}" in DEFERRED:
                    continue
                path = os.path.join(root, namespace, "items", entry)
                if f"{namespace}:{item}" in GECKOLIB:
                    wanted[path] = {
                        "model": {
                            "type": "minecraft:special",
                            "base": f"{namespace}:item/{item}",
                            "model": {"type": "geckolib:geckolib"},
                        }
                    }
                    continue
                wanted[path] = {
                    "model": {
                        "type": "minecraft:model",
                        "model": f"{namespace}:item/{item}",
                    }
                }
    return wanted


def existing():
    """Every definition on disk, so one left behind by a renamed model is seen rather than kept."""
    found = set()
    for root in ASSET_ROOTS:
        if not os.path.isdir(root):
            continue
        for namespace in sorted(os.listdir(root)):
            items = os.path.join(root, namespace, "items")
            if not os.path.isdir(items):
                continue
            for entry in sorted(os.listdir(items)):
                if entry.endswith(".json"):
                    found.add(os.path.join(items, entry))
    return found


def write():
    wanted = definitions()
    written = 0
    for path, content in sorted(wanted.items()):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        text = json.dumps(content, indent=2) + "\n"
        if os.path.isfile(path):
            with open(path, encoding="utf-8") as handle:
                if handle.read() == text:
                    continue
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(text)
        written += 1
    removed = 0
    for path in sorted(existing() - set(wanted)):
        os.remove(path)
        removed += 1
    print(
        f"build-item-definitions.py: {len(wanted)} definition(s); "
        f"{written} written, {removed} removed, {len(DEFERRED)} deferred"
    )
    return 0


def check():
    wanted = definitions()
    problems = []
    for path, content in sorted(wanted.items()):
        where = os.path.relpath(path, ROOT)
        if not os.path.isfile(path):
            problems.append(f"missing: {where}")
            continue
        with open(path, encoding="utf-8") as handle:
            if json.load(handle) != content:
                problems.append(f"stale: {where}")
    for path in sorted(existing() - set(wanted)):
        problems.append(
            f"orphan: {os.path.relpath(path, ROOT)} -- no item model stands behind it"
        )
    if problems:
        sys.exit("build-item-definitions.py --check failed:\n  " + "\n  ".join(problems))
    print(f"build-item-definitions.py --check: {len(wanted)} definition(s) up to date")
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check", action="store_true", help="assert the definitions are up to date; no writes"
    )
    args = parser.parse_args()
    return check() if args.check else write()


if __name__ == "__main__":
    sys.exit(main())
