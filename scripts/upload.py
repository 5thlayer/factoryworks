#!/usr/bin/env python3
# Upload FactoryWorks Core's released <version> to CurseForge (ADR-0101): the jar the local maven
# repository holds for it, with that version's section of publish/core/changelog.md as its notes.
#
#   scripts/upload.py [--dry-run] <version>
#
# $CURSEFORGE_TOKEN is an upload API token and is never printed. When it is missing, the script runs
# itself again through `op run --env-file=publish/upload.env`. $CURSEFORGE_PROJECT_ID overrides the
# project. $MAVEN_REPO_LOCAL reads somewhere other than ~/.m2/repository, and $CURSEFORGE_UPLOAD_URL
# and $CURSEFORGE_API_URL send somewhere other than the site. --dry-run contacts nothing.
import io
import json
import os
import re
import shutil
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CHANGELOG = ROOT / "publish/core/changelog.md"
# Filled in once the project exists on CurseForge (#533).
CURSEFORGE_PROJECT = ""
CURSEFORGE_UPLOAD = "https://minecraft.curseforge.com"
# The upload API can't list a project's files, so the website's own listing, which needs no key, does.
CURSEFORGE_API = "https://www.curseforge.com"
# What the jar task bundles for LGPL and CC BY (ADR-0102).
LICENSING = ["LICENSE", "NOTICE", "LICENSES/LGPL-3.0-only.txt", "LICENSES/CC-BY-4.0.txt"]
# The required dependencies neoforge.mods.toml names; Groundworks arrives nested in Beltworks.
REQUIRED = ["oritech", "beltworks"]
SECRET_HEADERS = {"X-Api-Token"}


class Refused(Exception):
    pass


def fail(message):
    sys.exit(f"upload: {message}")


def properties():
    text = (ROOT / "gradle.properties").read_text()
    return dict(re.findall(r"^(\w+) *= *(.*?)\s*$", text, re.MULTILINE))


def changelog(version):
    lines, on, found = [], False, False
    for line in CHANGELOG.read_text().splitlines():
        if line.startswith("## "):
            on = line == f"## {version}"
            found = found or on
        elif on and line.strip():
            lines.append(line)
    if not found or not lines:
        fail(f"{CHANGELOG.name} has no entries under \"## {version}\"; the notes are that section.")
    return "\n".join(lines)


def lacking_licensing(data):
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        names = set(z.namelist())
    return [name for name in LICENSING if name not in names]


def multipart(fields):
    boundary = uuid.uuid4().hex
    body = io.BytesIO()
    for name, filename, content_type, content in fields:
        disposition = f'form-data; name="{name}"' + (f'; filename="{filename}"' if filename else "")
        body.write(f"--{boundary}\r\nContent-Disposition: {disposition}\r\n"
                   f"Content-Type: {content_type}\r\n\r\n".encode())
        body.write(content + b"\r\n")
    body.write(f"--{boundary}--\r\n".encode())
    return f"multipart/form-data; boundary={boundary}", body.getvalue()


def send(method, url, headers, body=None):
    request = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        raise Refused(f"{method} {url} failed with {error.code}: "
                      f"{error.read().decode(errors='replace')[:500]}") from None
    except urllib.error.URLError as error:
        raise Refused(f"{method} {url} failed: {error.reason}") from None


def show(method, url, headers, **fields):
    print(f"{method} {url}")
    for name, value in headers.items():
        print(f"  {name}: {'<redacted>' if name in SECRET_HEADERS else value}")
    for name, value in fields.items():
        print(f"  {name}: {value}")


def through_op(args):
    env_file = ROOT / "publish/upload.env"
    if (os.environ.get("CURSEFORGE_TOKEN") or os.environ.get("UPLOAD_THROUGH_OP")
            or not env_file.is_file() or not shutil.which("op")):
        return
    os.environ["UPLOAD_THROUGH_OP"] = "1"
    sys.stdout.flush()
    os.execvp("op", ["op", "run", f"--env-file={env_file}", "--", sys.executable, __file__, *args])


class Release:
    def __init__(self, version, dry_run):
        self.version, self.dry_run = version, dry_run
        props = properties()
        self.name = f"{props['mod_name']} {version}"
        self.minecraft = props["minecraft_version"]
        self.release_type = "beta" if version.startswith("0.") else "release"
        artifact = props["mod_id"]
        self.agent = f"5thlayer/{artifact}/{version}"
        repo = Path(os.environ.get("MAVEN_REPO_LOCAL") or Path.home() / ".m2/repository")
        self.jar = repo / props["maven_group"].replace(".", "/") / artifact / version / f"{artifact}-{version}.jar"
        if not self.jar.is_file():
            fail(f"{version} is not in {repo}; only a released version is uploaded.")
        self.data = self.jar.read_bytes()
        missing = lacking_licensing(self.data)
        if missing:
            fail(f"{self.jar.name} lacks its licensing: {', '.join(missing)}")
        self.notes = changelog(version)

    def described(self):
        return f"{self.jar.name} ({len(self.data)} bytes) from {self.jar}"

    def curseforge(self):
        project = os.environ.get("CURSEFORGE_PROJECT_ID") or CURSEFORGE_PROJECT
        if not project:
            raise Refused("no CurseForge project id: set CURSEFORGE_PROJECT in scripts/upload.py.")
        upload = os.environ.get("CURSEFORGE_UPLOAD_URL", CURSEFORGE_UPLOAD).rstrip("/")
        api = os.environ.get("CURSEFORGE_API_URL", CURSEFORGE_API).rstrip("/")
        secret = os.environ.get("CURSEFORGE_TOKEN", "")
        upload_headers = {"X-Api-Token": secret, "User-Agent": self.agent}
        api_headers = {"User-Agent": self.agent, "Accept": "application/json"}
        files = f"{api}/api/v1/mods/{project}/files"
        metadata = {
            "changelog": self.notes,
            "changelogType": "markdown",
            "displayName": self.name,
            "releaseType": self.release_type,
            "relations": {"projects": [{"slug": s, "type": "requiredDependency"} for s in REQUIRED]},
        }
        if self.dry_run:
            show("GET", files, api_headers)
            show("GET", f"{upload}/api/game/version-types", upload_headers)
            show("GET", f"{upload}/api/game/versions", upload_headers,
                 note=f"picks the ids of Minecraft {self.minecraft}, NeoForge, Client and Server")
            show("POST", f"{upload}/api/projects/{project}/upload-file", upload_headers,
                 metadata=json.dumps({**metadata, "gameVersions": f"<ids of {self.minecraft}, NeoForge, Client, Server>"}, indent=2),
                 file=self.described())
            return
        if not secret:
            raise Refused("$CURSEFORGE_TOKEN is not set, and publish/upload.env didn't fill it in through op run.")
        if {self.jar.name, self.name} & self.curseforge_files(files, api_headers):
            raise Refused(f"CurseForge already has {self.version} in {project}, "
                          "and a published version never changes.")
        metadata["gameVersions"] = self.curseforge_game_versions(upload, upload_headers)
        content_type, body = multipart([("metadata", None, "application/json", json.dumps(metadata).encode()),
                                        ("file", self.jar.name, "application/java-archive", self.data)])
        created = send("POST", f"{upload}/api/projects/{project}/upload-file",
                       {**upload_headers, "Content-Type": content_type}, body)
        print(f"Uploaded {self.jar.name} to CurseForge as {self.version} ({created.get('id')})")

    def curseforge_files(self, url, headers):
        names, page_index, seen = set(), 0, 0
        while True:
            query = urllib.parse.urlencode({"pageIndex": page_index, "pageSize": 50})
            page = send("GET", f"{url}?{query}", headers)
            names |= {f.get("fileName") for f in page["data"]} | {f.get("displayName") for f in page["data"]}
            seen += len(page["data"])
            if not page["data"] or seen >= page["pagination"].get("totalCount", 0):
                return names
            page_index += 1

    def curseforge_game_versions(self, upload, headers):
        types = send("GET", f"{upload}/api/game/version-types", headers)
        minecraft = {t["id"] for t in types if t["slug"].startswith("minecraft-")}
        loaders = {t["id"] for t in types if t["slug"] == "modloader"}
        environments = {t["id"] for t in types if t["slug"] == "environment"}
        versions = send("GET", f"{upload}/api/game/versions", headers)
        ids = []
        for name, kinds in [(self.minecraft, minecraft), ("NeoForge", loaders),
                            ("Client", environments), ("Server", environments)]:
            found = [v["id"] for v in versions if v["name"] == name and v["gameVersionTypeID"] in kinds]
            if len(found) != 1:
                raise Refused(f"CurseForge names {len(found)} game versions {name}; it needs exactly one.")
            ids += found
        return ids


def main(args):
    dry_run = "--dry-run" in args
    rest = [a for a in args if a != "--dry-run"]
    if len(rest) != 1 or not re.fullmatch(r"\d+\.\d+\.\d+", rest[0]):
        fail("usage: scripts/upload.py [--dry-run] <major.minor.patch>")
    release = Release(rest[0], dry_run)
    if not dry_run:
        through_op(args)
    try:
        release.curseforge()
    except Refused as refused:
        fail(f"CurseForge: {refused}")


if __name__ == "__main__":
    main(sys.argv[1:])
