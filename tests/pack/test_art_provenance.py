#!/usr/bin/env python3
"""Assert every shipped texture, model and animation has a provenance row, and every row a file (#564).

`data/pack/art-provenance.json` says, per asset, whether FactoryWorks drew it, vendored it from
someone else, ships it as a stand-in or has not yet found out (ADR-0122). `scripts/art_provenance.py`
lists what is shipped and what a row may say. This check fails on:

  - a shipped asset with no row, so new art cannot arrive unaccounted for;
  - a row whose file is missing or is not a shipped asset, so a rename or deletion cannot leave a
    stale claim behind;
  - a malformed row: an unknown kind, a vendored row without source and licence, a stand-in row
    without a subkind and generator, an unsorted or duplicated path.

Usage: tests/pack/test_art_provenance.py
"""

import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "scripts"))
import art_provenance as ap  # noqa: E402

TEXTURE = "kubejs/assets/factoryworks/textures/block/a.png"
MODEL = "kubejs/assets/factoryworks/models/block/a.json"
GENERATOR = "scripts/gen.py"


def on_disk(*paths):
    return lambda p: p in paths


def must_fail(label, rows, assets, files, expect):
    found = ap.problems(rows, assets, on_disk(*files))
    if not any(expect in f for f in found):
        return [f"the rules did not name {label}: wanted {expect!r}, got {found}"]
    return []


def self_test():
    ok = [{"path": MODEL, "kind": "drawn"}, {"path": TEXTURE, "kind": "unknown"}]
    assets = [MODEL, TEXTURE]
    out = []
    found = ap.problems(ok, assets, on_disk(MODEL, TEXTURE))
    if found:
        out.append(f"the rules flagged a good manifest: {found}")

    def vendored(**kw):
        return [{"path": TEXTURE, "kind": "vendored", "source": "x", "licence": "CC0-1.0", **kw}]

    def stand_in(**kw):
        return [{"path": TEXTURE, "kind": "stand-in", "subkind": "procgen", "generator": GENERATOR,
                 **kw}]

    one = [TEXTURE]
    out += must_fail("an asset with no row", ok[:1], assets, assets, f"{TEXTURE} is shipped and has no row")
    out += must_fail("a row for a missing file", ok, [MODEL], [MODEL], f"{TEXTURE} has a row but the file is missing")
    out += must_fail("a row for a file that is not art", ok, [MODEL], assets, f"{TEXTURE} has a row but is not a shipped asset")
    out += must_fail("an unknown kind", [{"path": TEXTURE, "kind": "stolen"}], one, one, "has kind 'stolen'")
    out += must_fail("a vendored row with no licence", vendored(licence=""), one, one, "needs a non-empty `licence`")
    out += must_fail("a vendored row with no source", [{"path": TEXTURE, "kind": "vendored", "licence": "CC0-1.0"}],
                     one, one, "needs a non-empty `source`")
    out += must_fail("a stand-in with no subkind", [{"path": TEXTURE, "kind": "stand-in", "generator": GENERATOR}],
                     one, one, "needs a non-empty `subkind`")
    out += must_fail("a stand-in with no generator", [{"path": TEXTURE, "kind": "stand-in", "subkind": "ai"}],
                     one, one, "needs a non-empty `generator`")
    out += must_fail("a stand-in subkind", stand_in(subkind="sketch"), one, one, "has subkind 'sketch'")
    out += must_fail("a generator that is not a file", stand_in(), one, one, "which is not a file")
    out += must_fail("a field the kind does not take", [{"path": TEXTURE, "kind": "drawn", "licence": "CC0-1.0"}],
                     one, one, "may not carry licence")
    out += must_fail("a duplicated path", ok + ok[:1], assets, assets, "has more than one row")
    out += must_fail("rows out of order", ok[::-1], assets, assets, "not sorted by path")
    if ap.problems(vendored(), one, on_disk(TEXTURE)):
        out.append("a vendored row with a source and licence was flagged")
    if ap.problems(stand_in(subkind="ai", generator="a model's name"), one, on_disk(TEXTURE)):
        out.append("an `ai` stand-in was held to naming a file")
    if ap.problems(stand_in(), one, on_disk(TEXTURE, GENERATOR)):
        out.append("a stand-in naming an existing generator was flagged")

    for licence, non_commercial in (("CC-BY-NC-SA-4.0", True), ("CC-BY-4.0 AND CC-BY-NC-4.0", True),
                                    ("CC-BY-4.0", False), ("CC0-1.0", False),
                                    ("LicenseRef-Mojang", False)):
        row = {"path": TEXTURE, "kind": "vendored", "source": "x", "licence": licence}
        if ap.is_non_commercial(row) != non_commercial:
            out.append(f"{licence} was not read as non-commercial={non_commercial}")
    return out


def main():
    failures = self_test()

    assets = ap.shipped_assets()
    for tree in ("textures/", "models/", "data/art/", "publish/"):
        if not any(tree in a for a in assets):
            failures.append(f"the scan found no shipped asset under {tree}; the enumeration is broken")

    failures += ap.checkout_problems(ap.load_rows(), assets)

    if failures:
        print("art provenance:")
        for f in failures:
            print(f"  {f}")
        return 1
    print(f"art provenance ok: {len(assets)} shipped assets, each with one row")
    return 0


if __name__ == "__main__":
    sys.exit(main())
