#!/usr/bin/env python3
"""Emit the Replace Groups the mod reads, from Factorio's `fast_replaceable_group` (#387, ADR-0082).

Joins `data/factorio/machine.json`'s machine and pole rows onto `data/pack/item-map.json` and
writes `{pack block id: group}` to
`mod/src/main/resources/planetaryfactory_core/placement/replace_groups.json`. A row whose item-map
entry is `undecided`, `not_emitted` or `blocked_by` a ticket is a recorded skip, printed with its
reason, as the recipe converter does.

Usage:

    scripts/build-replace-groups.py            # writes the resource
    scripts/build-replace-groups.py --check    # asserts it is already up to date; no writes
"""
import argparse
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MACHINE_CORPUS = ROOT / "data" / "factorio" / "machine.json"
ITEM_MAP = ROOT / "data" / "pack" / "item-map.json"
RESOURCE = ROOT / "mod/src/main/resources/planetaryfactory_core/placement/replace_groups.json"


def groups():
    machine = json.loads(MACHINE_CORPUS.read_text(encoding="utf-8"))
    items = json.loads(ITEM_MAP.read_text(encoding="utf-8"))["items"]
    if "poles" not in machine:
        sys.exit(f"{MACHINE_CORPUS} has no poles -- re-run scripts/factorio-machine-extract.py")
    out, skipped = {}, []
    for row in machine["machines"] + machine["poles"]:
        name, group = row["name"], row.get("fast_replaceable_group")
        mapped = items.get(name)
        if group is None:
            skipped.append(f"{name}: no fast_replaceable_group")
        elif mapped is None:
            sys.exit(f"{name} has no row in {ITEM_MAP}")
        elif "status" in mapped:
            skipped.append(f"{name}: {mapped['status']}")
        elif "blocked_by" in mapped:
            skipped.append(f"{name}: blocked by #{mapped['blocked_by']}")
        elif "target" not in mapped:
            sys.exit(f"{name}'s row in {ITEM_MAP} has no target, status or blocked_by")
        else:
            out[mapped["target"]] = group
    return dict(sorted(out.items())), skipped


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    data, skipped = groups()
    text = json.dumps(data, indent=2) + "\n"
    if args.check:
        if not RESOURCE.is_file() or RESOURCE.read_text(encoding="utf-8") != text:
            sys.exit(f"stale: {RESOURCE.relative_to(ROOT)} -- run scripts/build-replace-groups.py")
        print(f"OK -- {len(data)} blocks")
        return
    RESOURCE.parent.mkdir(parents=True, exist_ok=True)
    RESOURCE.write_text(text, encoding="utf-8")
    print(f"wrote {RESOURCE.relative_to(ROOT)}: {len(data)} blocks")
    for block, group in data.items():
        print(f"  {block:45} {group}")
    for line in skipped:
        print(f"  skip {line}")


if __name__ == "__main__":
    main()
