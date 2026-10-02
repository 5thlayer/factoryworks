#!/usr/bin/env python3
"""Ratchet on references to third-party content mods in shipped data (ADR-0109, #575).

COUNTING RULE. For each forbidden namespace, one count is the sum of:

* every `<ns>:` occurrence (not preceded by a word character) in a text file under `kubejs/`
  (except `kubejs/parked/`, which is never loaded), `mod/src/main/resources/`, `config/`,
  `data/pack/*.json` (not the baseline file) and `mods/*.pw.toml`, plus `index.toml`;
* every such file whose path, lowercased with `-` and `_` removed, contains the namespace, so
  `mods/ftb-materials.pw.toml` and `config/oritech-common.toml` each count once;
* every `index.toml` line `file = "<path>"` whose path matches the same way.

`data/jars/` is an extract of the installed jars, not shipped data, and is never scanned. Only
tracked files count: the game writes untracked client configs, which would make the count differ
between checkouts.

A count above its baseline fails. A count below it fails too, asking for the baseline to be lowered,
so a slice that removes references locks the gain in.
"""

import json
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
BASELINE_FILE = ROOT / "data/pack/independence-baseline.json"
TEXT_SUFFIXES = {".json", ".json5", ".js", ".toml", ".snbt", ".cfg", ".properties", ".txt",
                 ".mcmeta", ".ini", ".css", ".lang", ".bak"}


def shipped_files():
    files = []
    for sub in ("kubejs", "mod/src/main/resources", "config"):
        files += [p for p in (ROOT / sub).rglob("*") if p.is_file()]
    files += [p for p in (ROOT / "data/pack").glob("*.json") if p != BASELINE_FILE]
    files += sorted((ROOT / "mods").glob("*.pw.toml"))
    files.append(ROOT / "index.toml")
    parked = ROOT / "kubejs/parked"
    tracked = set(subprocess.run(["git", "ls-files"], cwd=ROOT, capture_output=True, text=True,
                                 check=True).stdout.splitlines())
    return sorted(p for p in files
                  if parked not in p.parents and p.relative_to(ROOT).as_posix() in tracked)


def normalized(path):
    return re.sub(r"[-_]", "", path.lower())


def count(namespaces):
    counts = dict.fromkeys(namespaces, 0)
    id_res = {ns: re.compile(r"(?<!\w)" + re.escape(ns) + ":") for ns in namespaces}
    file_line = re.compile(r'^file = "([^"]+)"', re.M)
    for path in shipped_files():
        rel = path.relative_to(ROOT).as_posix()
        for ns in namespaces:
            if ns in normalized(rel):
                counts[ns] += 1
        if path.suffix not in TEXT_SUFFIXES:
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        for ns in namespaces:
            counts[ns] += len(id_res[ns].findall(text))
        if rel == "index.toml":
            for target in file_line.findall(text):
                for ns in namespaces:
                    if ns in normalized(target):
                        counts[ns] += 1
    return counts


def main():
    data = json.loads(BASELINE_FILE.read_text())
    namespaces = data["forbidden"]
    baseline = data["baseline"]
    failures = []
    if sorted(baseline) != sorted(namespaces):
        failures.append("baseline keys differ from the forbidden list")
    counts = count(namespaces)
    for ns in namespaces:
        n, base = counts[ns], baseline.get(ns, 0)
        if n > base:
            failures.append(f"{ns}: {n} references, baseline {base} (ADR-0109: the Pack depends on no "
                            "third-party content mod; remove the new reference)")
        elif n < base:
            failures.append(f"{ns}: {n} references, baseline {base}: lower the baseline to {n} "
                            "in data/pack/independence-baseline.json")
    print("counts: " + ", ".join(f"{ns}={counts[ns]}" for ns in namespaces))
    for f in failures:
        print("FAIL " + f)
    if failures:
        return 1
    print("OK independence guard: every count equals its baseline")
    return 0


if __name__ == "__main__":
    sys.exit(main())
