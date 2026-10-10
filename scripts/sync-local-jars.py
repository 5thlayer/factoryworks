#!/usr/bin/env python3
"""Install the 5thlayer jars `data/pack/local-jars.json` pins, from `~/.m2` (#465, ADR-0024).

A `mod=version` argument rewrites that row's pin first. Every run then copies each pinned jar out
of `~/.m2` into `mods/`, removing any other file its row's pattern matches. A row with a
`curseforge` project id gets `mods/<mod>.pw.toml` naming the pinned version's CurseForge file
(#532). While CurseForge does not list that file yet, the row's CurseForge reference is pending:
any metafile for the project is removed, so no export names an older file than the pin, and the
sync carries on. A later run fills in what is pending. It then refreshes the manifest with
`scripts/pack-check.sh --fix`. Nothing is written until every pinned jar is in `~/.m2`.

`--check` changes nothing and contacts nothing. It fails when the jar in `mods/` is not the pinned
one or differs from `~/.m2`'s by sha256; it skips the sha256 when
`~/.m2` lacks the pin, and names newer versions `~/.m2` holds without failing. For a `curseforge`
row with a metafile it also fails when the metafile names another file or project, hashes another
jar, or when `index.toml` indexes the jar instead of the metafile. A row with no metafile is named
pending and passes, unless `--strict`, which an export must pass.

Don't run it while the game is running: it rewrites jars in `mods/`.

Usage:

    scripts/sync-local-jars.py beltworks=0.2.0   # pin, install, refresh
    scripts/sync-local-jars.py                   # install whatever is pinned, fill pending references
    scripts/sync-local-jars.py --check           # assert mods/ matches the pins; no writes
    scripts/sync-local-jars.py --check --strict  # also fail on a pending CurseForge reference
"""
import argparse
import fnmatch
import hashlib
import json
import re
import shutil
import subprocess
import sys
import tomllib
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TABLE = ROOT / "data" / "pack" / "local-jars.json"
MODS = ROOT / "mods"
M2 = Path.home() / ".m2" / "repository"
INDEX = ROOT / "index.toml"
# The upload API can't list a project's files, so the website's own listing, which needs no key,
# does. It omits a file still under review.
CURSEFORGE_FILES = "https://www.curseforge.com/api/v1/mods/{project}/files"


def artifact_dir(row):
    return M2.joinpath(*row["group"].split("."), row["artifact"])


def jar_name(row):
    return f"{row['artifact']}-{row['version']}.jar"


def published(row):
    return artifact_dir(row) / row["version"] / jar_name(row)


def installed(row):
    if not MODS.is_dir():
        sys.exit(f"no {MODS} -- run this from the pack instance")
    return sorted(p for p in MODS.iterdir() if fnmatch.fnmatch(p.name, row["pattern"]))


def metafile(row):
    return MODS / f"{row['mod']}.pw.toml"


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def sha1(path):
    return hashlib.sha1(path.read_bytes()).hexdigest()


def version_key(version):
    return tuple((0, int(n), "") if n.isdigit() else (1, 0, n) for n in re.split(r"[.\-]", version))


def newer(row):
    metadata = artifact_dir(row) / "maven-metadata-local.xml"
    if not metadata.is_file():
        return []
    versions = [v.text for v in ET.parse(metadata).getroot().iter("version")]
    pin = version_key(row["version"])
    return sorted((v for v in versions if version_key(v) > pin), key=version_key)


def check_row(row, pending):
    failures = []
    present = installed(row)
    if [p.name for p in present] != [jar_name(row)]:
        failures.append(f"{row['mod']}: mods/ holds {[p.name for p in present] or 'nothing'} "
                        f"for {row['pattern']}, not {jar_name(row)} alone "
                        f"-- run scripts/sync-local-jars.py")
        return failures
    jar = present[0]

    source = published(row)
    if not source.is_file():
        print(f"skip {row['mod']}: {source} is not on this machine, so the sha256 is not compared")
    elif sha256(jar) != sha256(source):
        failures.append(f"{row['mod']}: mods/{jar.name} differs from {source} "
                        f"-- {row['version']} was republished, or the jar was replaced by hand")

    if "curseforge" in row:
        if metafile(row).is_file():
            failures += check_metafile(row, jar)
        else:
            pending.append(row)

    waiting = newer(row)
    if waiting:
        print(f"note {row['mod']}: ~/.m2 holds {', '.join(waiting)}, newer than the pin")
    return failures


def check_metafile(row, jar):
    meta = metafile(row)
    name = f"mods/{meta.name}"
    if not meta.is_file():
        return [f"{row['mod']}: no {name} -- run scripts/sync-local-jars.py"]
    toml = tomllib.loads(meta.read_text(encoding="utf-8"))
    curseforge = toml.get("update", {}).get("curseforge", {})
    failures = []
    if toml.get("filename") != jar.name:
        failures.append(f"{row['mod']}: {name} names {toml.get('filename')}, not the pinned {jar.name}")
    if curseforge.get("project-id") != row["curseforge"]:
        failures.append(f"{row['mod']}: {name} names CurseForge project {curseforge.get('project-id')}, "
                        f"not {row['curseforge']}")
    download = toml.get("download", {})
    if not jar.is_file():
        print(f"skip {row['mod']}: {jar} is not on this machine, so the sha1 is not compared")
    elif download.get("hash-format") == "sha1" and download.get("hash") != sha1(jar):
        where = f"mods/{jar.name}" if jar.parent == MODS else jar
        failures.append(f"{row['mod']}: {name} hashes another file than {where} "
                        f"-- CurseForge's {row['version']} is not ~/.m2's")
    index = {f["file"] for f in tomllib.loads(INDEX.read_text(encoding="utf-8")).get("files", [])}
    if f"mods/{jar.name}" in index:
        failures.append(f"{row['mod']}: index.toml indexes mods/{jar.name}, which {name} names")
    if name not in index:
        failures.append(f"{row['mod']}: index.toml does not index {name}")
    if not failures:
        print(f"ok   {row['mod']} {row['version']} is CurseForge file {curseforge['file-id']}")
    return failures


def check(rows, strict):
    pending = []
    failures = [f for row in rows for f in check_row(row, pending)]
    for row in pending:
        print(f"pending {row['mod']} {row['version']}: no CurseForge reference yet "
              f"-- run scripts/sync-local-jars.py once CurseForge lists {jar_name(row)}")
    if strict:
        failures += [f"{row['mod']}: the CurseForge reference for {row['version']} is pending"
                     for row in pending]
    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}", file=sys.stderr)
    if failures:
        sys.exit(1)
    print(f"OK -- {len(rows)} local jar(s) match their pin"
          + (f", {len(pending)} CurseForge reference(s) pending" if pending else ""))


def pin(table, assignments):
    by_mod = {row["mod"]: row for row in table["jars"]}
    for assignment in assignments:
        mod, sep, version = assignment.partition("=")
        if not sep or not version:
            sys.exit(f"expected mod=version, got {assignment!r}")
        if mod not in by_mod:
            sys.exit(f"{mod} has no row in {TABLE.relative_to(ROOT)} (rows: {', '.join(by_mod)})")
        by_mod[mod]["version"] = version


def preflight(rows):
    """Every refusal the sync can meet, so that a refused sync leaves the tree as it was."""
    for row in rows:
        installed(row)
        if not published(row).is_file():
            sys.exit(f"{published(row)} does not exist -- publish {row['mod']} {row['version']} "
                     f"with publishToMavenLocal first")


def install(row):
    source = published(row)
    for stale in installed(row):
        if stale.name != jar_name(row):
            stale.unlink()
            print(f"removed mods/{stale.name}")
    shutil.copyfile(source, MODS / jar_name(row))
    print(f"installed mods/{jar_name(row)} from {source}")


def curseforge_file(row):
    """The CurseForge file id of the pinned jar, or None when CurseForge does not list it."""
    url = CURSEFORGE_FILES.format(project=row["curseforge"])
    page_index, seen = 0, 0
    while True:
        query = urllib.parse.urlencode({"pageIndex": page_index, "pageSize": 50})
        request = urllib.request.Request(f"{url}?{query}", headers={"Accept": "application/json"})
        try:
            with urllib.request.urlopen(request) as response:
                page = json.load(response)
        except OSError as error:
            print(f"CurseForge project {row['curseforge']} could not be read: {error}")
            return None
        for file in page["data"]:
            if file.get("fileName") == jar_name(row):
                return file["id"]
        seen += len(page["data"])
        if not page["data"] or seen >= page["pagination"].get("totalCount", 0):
            return None
        page_index += 1


def referenced(row):
    """True when the metafile already names the pinned jar in the row's project."""
    if not metafile(row).is_file():
        return False
    toml = tomllib.loads(metafile(row).read_text(encoding="utf-8"))
    return (toml.get("filename") == jar_name(row)
            and toml.get("update", {}).get("curseforge", {}).get("project-id") == row["curseforge"])


def reference(row):
    """Write the row's metafile, or leave none and return False while CurseForge lacks the file."""
    if referenced(row):
        return True
    file_id = curseforge_file(row)
    if file_id is None:
        for stale in naming_project(row["curseforge"]):
            stale.unlink()
            print(f"removed mods/{stale.name}, which names an older file than the pin")
        print(f"pending {row['mod']} {row['version']}: CurseForge project {row['curseforge']} "
              f"lists no {jar_name(row)} yet")
        return False
    write_metafile(row, file_id)
    return True


def naming_project(project):
    return [p for p in MODS.glob("*.pw.toml")
            if tomllib.loads(p.read_text(encoding="utf-8")).get("update", {})
            .get("curseforge", {}).get("project-id") == project]


def write_metafile(row, file_id):
    for stale in naming_project(row["curseforge"]):
        stale.unlink()
    run(f"the {row['mod']} metafile", ["packwiz", "curseforge", "add", "--addon-id", str(row["curseforge"]),
                                       "--file-id", str(file_id), "-y"])
    written = naming_project(row["curseforge"])
    if len(written) != 1:
        sys.exit(f"packwiz wrote {len(written)} metafiles for {row['mod']}, not one")
    # packwiz names the file after the project's slug; the refresh after re-indexes the rename.
    written[0].rename(metafile(row))


def run(step, command):
    print(f"\n$ {' '.join(command)}")
    if subprocess.run(command, cwd=ROOT).returncode != 0:
        sys.exit(f"{step} failed")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--strict", action="store_true",
                        help="with --check, fail on a pending CurseForge reference")
    parser.add_argument("pins", nargs="*", metavar="mod=version")
    args = parser.parse_args()
    if args.check and args.pins:
        parser.error("--check changes nothing, so it takes no pins")
    if args.strict and not args.check:
        parser.error("--strict qualifies --check")

    table = json.loads(TABLE.read_text(encoding="utf-8"))
    if args.check:
        check(table["jars"], args.strict)
        return
    if args.pins:
        pin(table, args.pins)
    preflight(table["jars"])
    if args.pins:
        TABLE.write_text(json.dumps(table, indent=2) + "\n", encoding="utf-8")
    pending = []
    for row in table["jars"]:
        install(row)
        if "curseforge" in row and not reference(row):
            pending.append(row)
    run("the manifest refresh", ["scripts/pack-check.sh", "--fix"])
    if pending:
        print()
        for row in pending:
            print(f"pending {row['mod']} {row['version']}: run the sync again once CurseForge "
                  f"lists {jar_name(row)}; an export waits on --check --strict")


if __name__ == "__main__":
    main()
