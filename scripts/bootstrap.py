#!/usr/bin/env python3
"""Put the Pack's jars on a fresh machine, each byte for byte the jar the manifest names.

    scripts/bootstrap.py instance   # link $CURSEFORGE_ROOT/Instances/FactoryWorks here, adopt CurseForge's profile
    scripts/bootstrap.py jars       # every mods/*.pw.toml jar, from CurseForge's CDN
    scripts/bootstrap.py local      # every local-jars.json row into ~/.m2, from its source
    scripts/bootstrap.py            # all three, in that order

Then `scripts/sync-local-jars.py` installs the local jars. A rebuild that differs from its recorded
hash stops the run: the pin is the user's to move, never this script's.
"""
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tomllib
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MODS = ROOT / "mods"
TABLE = ROOT / "data" / "pack" / "local-jars.json"
M2 = Path.home() / ".m2" / "repository"
CHECKOUTS = Path.home() / "minecraft_mods"
CURSEFORGE = Path(os.environ.get("CURSEFORGE_ROOT", "~/curseforge")).expanduser()
INSTANCE_NAME = "FactoryWorks"
AGENT_INSTANCES = Path.home() / ".config/CurseForge/agent/GameInstances/MinecraftGameInstance.json"


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def instance():
    link = CURSEFORGE / "Instances" / INSTANCE_NAME
    if not (CURSEFORGE / "Instances").is_dir():
        sys.exit(f"{CURSEFORGE}/Instances does not exist -- install CurseForge, or set CURSEFORGE_ROOT")
    if link.is_symlink() and link.resolve() == ROOT:
        print(f"ok   {link} -> {ROOT}")
    elif link.exists() or link.is_symlink():
        sys.exit(f"{link} exists and is not a link to {ROOT} -- move it aside first")
    else:
        link.symlink_to(ROOT)
        print(f"linked {link} -> {ROOT}")
    profile = ROOT / "minecraftinstance.json"
    if profile.exists():
        print(f"ok   {profile.name} exists")
        reconcile(link, profile)
        return
    # CurseForge's profile embeds NeoForge's version JSON and install profile, which only its own
    # keyed API serves, so the profile is CurseForge's to create. Named FactoryWorks beside the
    # link, it lands in "FactoryWorks (1)", which is adopted here.
    created = [p for p in (CURSEFORGE / "Instances").glob(f"{INSTANCE_NAME} (*)")
               if (p / "minecraftinstance.json").is_file()]
    if len(created) != 1:
        versions = tomllib.loads((ROOT / "pack.toml").read_text())["versions"]
        sys.exit(f"no profile to adopt -- in CurseForge, create a custom profile named {INSTANCE_NAME} "
                 f"on Minecraft {versions['minecraft']}, NeoForge {versions['neoforge']}, "
                 f"then run this again")
    source = created[0]
    stray = [p.name for p in source.rglob("*") if p.is_file()
             and p.name not in ("minecraftinstance.json", ".curseclient")]
    if stray:
        sys.exit(f"{source} holds more than a new profile ({', '.join(stray[:5])}) -- merge it by hand")
    text = (source / "minecraftinstance.json").read_text(encoding="utf-8-sig")
    profile.write_text(text.replace(f"Instances/{source.name}/", f"Instances/{INSTANCE_NAME}/"),
                       encoding="utf-8")
    if (source / ".curseclient").exists():
        shutil.copyfile(source / ".curseclient", ROOT / ".curseclient")
    shutil.rmtree(source)
    print(f"adopted {source.name}'s profile")
    reconcile(link, profile)


def reconcile(link, profile):
    """Point CurseForge's own instance list at the adopted profile; it overrides the profile's file."""
    if not AGENT_INSTANCES.is_file():
        return
    if subprocess.run(["pgrep", "-x", "curseforge"], capture_output=True).returncode == 0:
        sys.exit("CurseForge is running and rewrites its instance list -- quit it, then run this again")
    adopted = json.loads(profile.read_text(encoding="utf-8-sig"))
    guid = adopted["guid"]
    path = f"{link}/"
    entries = json.loads(AGENT_INSTANCES.read_text(encoding="utf-8-sig"))
    kept = [e for e in entries if e.get("guid") == guid or e.get("installPath") != path]
    if not any(e.get("guid") == guid for e in kept):
        kept.append(adopted)
    changed = kept != entries
    for entry in kept:
        if entry.get("guid") == guid and entry.get("installPath") != path:
            entry["installPath"], changed = path, True
    if changed:
        if not AGENT_INSTANCES.with_suffix(".json.bak").exists():
            shutil.copyfile(AGENT_INSTANCES, AGENT_INSTANCES.with_suffix(".json.bak"))
        AGENT_INSTANCES.write_text(json.dumps(kept), encoding="utf-8")
        print(f"pointed CurseForge's instance list at {path} (backup: {AGENT_INSTANCES.name}.bak)")
    else:
        print("ok   CurseForge's instance list")


def download(url, target):
    request = urllib.request.Request(url, headers={"User-Agent": "factoryworks-bootstrap"})
    with urllib.request.urlopen(request) as response, open(target, "wb") as out:
        shutil.copyfileobj(response, out)


def jars():
    failed = []
    for meta in sorted(MODS.glob("*.pw.toml")):
        if meta.name == "factoryworks-core.pw.toml":
            continue  # installToPack builds it, and the dev runs want that build (ADR-0101)
        toml = tomllib.loads(meta.read_text())
        file_id = toml.get("update", {}).get("curseforge", {}).get("file-id")
        if file_id is None:
            print(f"skip {meta.name}: no CurseForge file")
            continue
        name, want = toml["filename"], toml["download"]["hash"]
        target = MODS / name
        algorithm = toml["download"]["hash-format"]
        if target.exists() and hashlib.new(algorithm, target.read_bytes()).hexdigest() == want:
            print(f"ok   {name}")
            continue
        path = f"files/{file_id // 1000}/{file_id % 1000}/{urllib.parse.quote(name)}"
        for host in ("https://mediafilez.forgecdn.net", "https://edge.forgecdn.net"):
            try:
                download(f"{host}/{path}", target)
                break
            except OSError:
                continue
        else:
            failed.append(f"{name}: no CDN served file {file_id}")
            continue
        if hashlib.new(algorithm, target.read_bytes()).hexdigest() != want:
            failed.append(f"{name}: hash differs from {meta.name}")
            continue
        print(f"got  {name}")
    if failed:
        sys.exit("\n".join(["FAILED"] + failed))


def published(group, artifact, version):
    return M2.joinpath(*group.split("."), artifact, version, f"{artifact}-{version}.jar")


def tag_hash(checkout, tag):
    message = subprocess.run(["git", "-C", checkout, "tag", "-l", "--format=%(contents)", tag],
                             capture_output=True, text=True, check=True).stdout
    found = re.search(r"jar sha256 ([0-9a-f]{64})", message)
    return found and found.group(1)


def index_hash(jar_name):
    for entry in tomllib.loads((ROOT / "index.toml").read_text())["files"]:
        if entry["file"] == f"mods/{jar_name}":
            return entry["hash"]
    return None


def checkout(repo, ref, name):
    path = CHECKOUTS / name
    if not path.exists():
        subprocess.run(["gh", "repo", "clone", repo, str(path), "--", "-q"], check=True)
    subprocess.run(["git", "-C", path, "fetch", "-q", "--tags", "origin"], check=True)
    dirty = subprocess.run(["git", "-C", path, "status", "--porcelain", "--untracked-files=no"],
                           capture_output=True, text=True, check=True).stdout
    if dirty:
        sys.exit(f"{path} has uncommitted changes -- its owner settles them first")
    subprocess.run(["git", "-C", path, "checkout", "-q", ref], check=True)
    return path


def publish(path, tasks):
    for task in tasks:
        subprocess.run(["sh", "./gradlew", "-q", task], cwd=path, check=True)


def place(row, source, want, fallback=None):
    """Put `row`'s jar in ~/.m2 from its source; return a problem, or None."""
    jar = published(row["group"], row["artifact"], row["version"])
    if jar.exists() and sha256(jar) == want:
        print(f"ok   {jar.name}")
        return None
    if "maven" in source:
        jar.parent.mkdir(parents=True, exist_ok=True)
        base = f"{source['maven']}/{row['group'].replace('.', '/')}/{row['artifact']}/{row['version']}"
        for ext in ("jar", "pom"):
            download(f"{base}/{row['artifact']}-{row['version']}.{ext}", jar.with_suffix(f".{ext}"))
    else:
        ref = source["ref"].format(version=row["version"])
        path = checkout(source["repo"], ref, row.get("checkout", row["mod"]))
        want = want or tag_hash(path, ref)
        if not want:
            return f"{jar.name}: no recorded hash to check it against"
        if jar.exists() and sha256(jar) == want:
            print(f"ok   {jar.name}")
            return None
        # A 5thlayer release on CurseForge is the jar its tag hashes, and a rebuild may not
        # reproduce it, or even resolve the dependencies it was built against.
        if fallback and fallback.exists() and sha256(fallback) == want:
            jar.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(fallback, jar)
            print(f"took {jar.name} from {fallback.relative_to(ROOT)}, the release its tag hashes")
            return None
        publish(path, source.get("tasks", ["publishToMavenLocal"]))
    if not want:
        return f"{jar.name}: no recorded hash to check it against"
    if sha256(jar) == want:
        print(f"built {jar.name}")
        return None
    return f"{jar.name}: built {sha256(jar)[:12]}, want {want[:12]}"


def local():
    rows = json.loads(TABLE.read_text())["jars"]
    problems = []
    # Groundworks first: Beltworks and Wireworks resolve the newest one in ~/.m2 within their
    # range, so only the version their released jars nest goes there.
    nested = {}
    for row in rows:
        for artifact in row.get("nests", []):
            jar = MODS / f"{row['artifact']}-{row['version']}.jar"
            if not jar.exists():
                continue
            with zipfile.ZipFile(jar) as z:
                for entry in json.loads(z.read("META-INF/jarjar/metadata.json"))["jars"]:
                    if entry["identifier"]["artifact"] == artifact:
                        nested[artifact] = (entry["identifier"]["group"], entry["version"]["artifactVersion"])
    for artifact, (group, version) in nested.items():
        library = {"mod": artifact, "group": group, "artifact": artifact, "version": version}
        problem = place(library, {"repo": f"5thlayer/{artifact}", "ref": "v{version}"}, None)
        if problem:
            problems.append(problem)
    for row in rows:
        jar_name = f"{row['artifact']}-{row['version']}.jar"
        want = index_hash(jar_name)
        fallback = MODS / jar_name if "curseforge" in row else None
        problem = place(row, row["source"], want, fallback)
        if problem:
            problems.append(problem)
    if problems:
        sys.exit("\n".join(["MISMATCH -- the pin is yours to move, or copy the jar from another machine:"]
                           + problems))
    print("\nnext: scripts/sync-local-jars.py")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("step", nargs="?", choices=["instance", "jars", "local"])
    step = parser.parse_args().step
    for name, run in (("instance", instance), ("jars", jars), ("local", local)):
        if step in (None, name):
            run()


if __name__ == "__main__":
    main()
