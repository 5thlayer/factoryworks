#!/usr/bin/env python3
"""Print the art the Pack has still to replace or account for (#564, ADR-0122).

Reads `data/pack/art-provenance.json` and lists what is not yet FactoryWorks' own finished art:
stand-ins by generator, vendored art by licence with the non-commercial first, and the rows still
`unknown`. Drawn rows are only counted. Exits 1 after printing if the manifest and the shipped
assets disagree; `tests/pack/test_art_provenance.py` names each disagreement.

    scripts/art-worklist.py
"""

import collections
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import art_provenance as ap  # noqa: E402


def section(title, rows):
    print(f"\n{title} ({len(rows)})")


def group(rows, key):
    out = collections.defaultdict(list)
    for r in rows:
        out[key(r)].append(r["path"])
    return out


def show(groups, heading):
    for key in sorted(groups, key=lambda k: (k[0], k)):
        paths = groups[key]
        print(f"\n  {heading(key)} -- {len(paths)}")
        for p in paths:
            print(f"    {p}")


def main():
    rows = ap.load_rows()
    by_kind = {k: [r for r in rows if r["kind"] == k] for k in ap.KINDS}
    non_commercial = [r for r in by_kind["vendored"] if ap.is_non_commercial(r)]
    print(f"{len(rows)} shipped assets: {len(by_kind['drawn'])} drawn, "
          f"{len(by_kind['stand-in'])} stand-in, {len(by_kind['vendored'])} vendored "
          f"({len(non_commercial)} non-commercial), {len(by_kind['unknown'])} unknown")

    section("STAND-IN", by_kind["stand-in"])
    show(group(by_kind["stand-in"], lambda r: (r["subkind"], r["generator"])),
         lambda k: f"{k[0]}, {k[1]}")

    section("VENDORED", by_kind["vendored"])
    show(group(by_kind["vendored"], lambda r: (not ap.is_non_commercial(r), r["licence"])),
         lambda k: k[1] + ("" if k[0] else "  (non-commercial)"))

    section("UNKNOWN", by_kind["unknown"])
    for r in by_kind["unknown"]:
        print(f"    {r['path']}")

    found = ap.problems(rows, ap.shipped_assets(), lambda p: (ap.ROOT / p).is_file())
    if found:
        print(f"\n{len(found)} manifest problems; run tests/pack/test_art_provenance.py",
              file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
