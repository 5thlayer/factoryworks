"""Which art the Pack ships, and the manifest that says where each piece came from (#564, ADR-0122).

Shared by `tests/pack/test_art_provenance.py` and `scripts/art-worklist.py`.
"""

import json
import pathlib
import re
import subprocess

ROOT = pathlib.Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "data/pack/art-provenance.json"

KINDS = ("drawn", "vendored", "stand-in", "unknown")
STAND_IN_SUBKINDS = ("placeholder", "procgen", "ai")

_MEDIA = "png|jpe?g|gif|webp|svg|mp4|webm|mov"
# Blockstates, item definitions and lang only point at art, so they have no row (#564).
SHIPPED = (
    re.compile(r"(?:kubejs|mod/src/main/resources)/assets/[^/]+/(?:textures|models|sounds)/.+\Z"),
    re.compile(r"data/art/.+\.(?:" + _MEDIA + r"|bbmodel)\Z"),
    re.compile(r"publish/.+\.(?:" + _MEDIA + r")\Z"),
)

_FIELDS = {
    "drawn": set(),
    "vendored": {"source", "licence"},
    "stand-in": {"subkind", "generator"},
    "unknown": set(),
}
_COMMON = {"path", "kind", "note"}
_NON_COMMERCIAL = re.compile(r"(?:^|[-\s(])NC(?:[-\s)]|\Z)")


def shipped_assets():
    """Every texture, model, animation and store medium present in the checkout, repo-relative.

    Untracked files count, so a new asset fails before it is staged.
    """
    out = subprocess.run(
        ["git", "ls-files", "-z", "--cached", "--others", "--exclude-standard"],
        cwd=ROOT, check=True, capture_output=True, text=True).stdout
    return sorted(p for p in out.split("\0")
                  if p and (ROOT / p).is_file() and any(s.match(p) for s in SHIPPED))


def load_rows():
    return json.loads(MANIFEST.read_text(encoding="utf-8"))["rows"]


def is_non_commercial(row):
    return row["kind"] == "vendored" and bool(_NON_COMMERCIAL.search(row["licence"]))


def _text(value):
    return isinstance(value, str) and bool(value.strip())


def _row_problems(i, row, exists):
    if not isinstance(row, dict):
        return [f"row {i} is not an object"]
    path = row.get("path")
    name = f"row {i} ({path})" if _text(path) else f"row {i}"
    out = []
    if not _text(path):
        out.append(f"{name} has no `path`")
    kind = row.get("kind")
    if kind not in KINDS:
        out.append(f"{name} has kind {kind!r}, not one of {', '.join(KINDS)}")
        return out
    stray = set(row) - _COMMON - _FIELDS[kind]
    if stray:
        out.append(f"{name} is `{kind}` and may not carry {', '.join(sorted(stray))}")
    for field in sorted(_FIELDS[kind]):
        if not _text(row.get(field)):
            out.append(f"{name} is `{kind}` and needs a non-empty `{field}`")
    if "note" in row and not _text(row["note"]):
        out.append(f"{name} has an empty `note`")
    if kind == "stand-in" and _text(row.get("subkind")):
        if row["subkind"] not in STAND_IN_SUBKINDS:
            out.append(f"{name} has subkind {row['subkind']!r}, "
                       f"not one of {', '.join(STAND_IN_SUBKINDS)}")
        elif (row["subkind"] != "ai" and _text(row.get("generator"))
              and not exists(row["generator"])):
            out.append(f"{name} names generator {row['generator']!r}, which is not a file")
    return out


def problems(rows, assets, exists):
    """What is wrong between the manifest's rows and the assets the checkout ships.

    `exists` answers whether a repo-relative path is a file, so a test can run this on made-up data.
    """
    out = []
    if not isinstance(rows, list):
        return ["`rows` is not a list"]
    paths = []
    for i, row in enumerate(rows):
        out += _row_problems(i, row, exists)
        if isinstance(row, dict) and _text(row.get("path")):
            paths.append(row["path"])
    if paths != sorted(paths):
        out.append("rows are not sorted by path")
    seen = set()
    for p in paths:
        if p in seen:
            out.append(f"{p} has more than one row")
        seen.add(p)
    shipped = set(assets)
    for p in sorted(shipped - seen):
        out.append(f"{p} is shipped and has no row")
    for p in sorted(seen - shipped):
        out.append(f"{p} has a row but is not a shipped asset" if exists(p)
                   else f"{p} has a row but the file is missing")
    return out


def checkout_problems(rows, assets):
    return problems(rows, assets, lambda p: (ROOT / p).is_file())
