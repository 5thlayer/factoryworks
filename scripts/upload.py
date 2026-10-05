#!/usr/bin/env python3
# Upload FactoryWorks Core's released <version> to Modrinth and CurseForge (ADR-0101): the jar the local
# maven repository holds for it, with that version's section of publish/core/changelog.md as its notes.
#
#   scripts/upload.py [--dry-run] [--site modrinth|curseforge] <version>
#
# Each site is uploaded on its own: a failure on one leaves the other, and --site retries just one.
# Each site's token comes from the environment, and is never printed. When one is missing, the script
# runs itself again through `op run --env-file=publish/upload.env`, which fills in the tokens that
# file names in 1Password, for that run only. Core's projects and required dependencies are
# PROJECTS and DEPENDENCIES below, and the environment can override the projects:
#   Modrinth    $MODRINTH_TOKEN, $MODRINTH_PROJECT_ID
#   CurseForge  $CURSEFORGE_TOKEN (an upload API token), $CURSEFORGE_PROJECT_ID
# upload_release_type in gradle.properties sends every version as release, beta or alpha; left empty,
# a version below 1.0 is a beta and one from 1.0 a release.
# $MAVEN_REPO_LOCAL reads somewhere other than ~/.m2/repository, and $MODRINTH_API_URL,
# $CURSEFORGE_UPLOAD_URL and $CURSEFORGE_API_URL send somewhere other than the sites, to try the
# script out. --dry-run prints the requests it would make and contacts nothing.
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
CHANGELOG = "publish/core/changelog.md"
PROJECTS = {"modrinth": "7wb8sJtC", "curseforge": "1718187"}
# The required dependencies neoforge.mods.toml names; Groundworks arrives nested in Beltworks.
DEPENDENCIES = {"modrinth": ["4sYI62kA", "p4zxipln"], "curseforge": ["oritech", "beltworks"]}
MODRINTH_API = "https://api.modrinth.com/v2"
CURSEFORGE_UPLOAD = "https://minecraft.curseforge.com"
# The upload API can't list a project's files, so the website's own listing, which needs no key, does.
CURSEFORGE_API = "https://www.curseforge.com"
# What the jar task bundles for LGPL and CC BY (ADR-0102).
LICENSING = ["LICENSE", "NOTICE", "LICENSES/LGPL-3.0-only.txt", "LICENSES/CC-BY-4.0.txt"]
SECRET_HEADERS = {"Authorization", "X-Api-Token"}
# What upload_release_type may name; both sites call the three types alike.
RELEASE_TYPES = ["release", "beta", "alpha"]


class Refused(Exception):
    pass


class Published(Refused):
    """The site already has the version, so there is nothing to retry."""


def fail(message):
    sys.exit(f"upload: {message}")


def properties():
    text = (ROOT / "gradle.properties").read_text()
    return dict(re.findall(r"^(\w+)[ \t]*=[ \t]*(.*?)[ \t]*$", text, re.MULTILINE))


def project_of(site):
    """The site's project: the environment's, else Core's."""
    return os.environ.get(f"{site.upper()}_PROJECT_ID") or PROJECTS[site]


def release_type(version, props):
    """upload_release_type's type for every version, else beta below 1.0 and release from it."""
    chosen = props.get("upload_release_type", "")
    if chosen and chosen not in RELEASE_TYPES:
        fail(f"gradle.properties sets upload_release_type = {chosen}; it is empty or one of "
             f"{', '.join(RELEASE_TYPES)}.")
    return chosen or ("beta" if version.startswith("0.") else "release")


def changelog(version):
    """The entries between "## <version>" and the next heading, as release.sh reads Unreleased."""
    lines, on, found = [], False, False
    for line in (ROOT / CHANGELOG).read_text().splitlines():
        if line.startswith("## "):
            on = line == f"## {version}"
            found = found or on
        elif on and line.strip():
            lines.append(line)
    if not found or not lines:
        fail(f"{CHANGELOG} has no entries under \"## {version}\"; the notes are that section.")
    return "\n".join(lines)


def lacking_licensing(data):
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        names = set(z.namelist())
    return [name for name in LICENSING if name not in names]


def multipart(fields):
    """A multipart/form-data body from (name, filename or None, content type, bytes) fields."""
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


def token(name):
    if not os.environ.get(name):
        raise Refused(f"${name} is not set, and publish/upload.env didn't fill it in through op run.")
    return os.environ[name]


def through_op(args, sites):
    """Runs the script again under `op run` when a token it needs is missing, once."""
    needed = {"modrinth": "MODRINTH_TOKEN", "curseforge": "CURSEFORGE_TOKEN"}
    env_file = ROOT / "publish/upload.env"
    if (all(os.environ.get(needed[site]) for site in sites) or os.environ.get("UPLOAD_THROUGH_OP")
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
        self.release_type = release_type(version, props)
        self.agent = f"5thlayer/{props['mod_id']}/{version}"
        artifact = props["mod_id"]
        repo = Path(os.environ.get("MAVEN_REPO_LOCAL") or Path.home() / ".m2/repository")
        self.jar = repo / props["maven_group"].replace(".", "/") / artifact / version / f"{artifact}-{version}.jar"
        if not self.jar.is_file():
            fail(f"{version} is not in {repo}; only a released version is uploaded.")
        self.data = self.jar.read_bytes()
        missing = lacking_licensing(self.data)
        if missing:
            fail(f"{self.jar.name} lacks its licensing: {', '.join(missing)}")
        self.notes = changelog(version)

    def file_part(self):
        return "file", self.jar.name, "application/java-archive", self.data

    def described(self):
        return f"{self.jar.name} ({len(self.data)} bytes) from {self.jar}"

    def modrinth(self):
        secret = token("MODRINTH_TOKEN")
        project = project_of("modrinth")
        api = os.environ.get("MODRINTH_API_URL", MODRINTH_API).rstrip("/")
        metadata = {
            "name": self.name,
            "version_number": self.version,
            "changelog": self.notes,
            "dependencies": [{"project_id": d, "dependency_type": "required"}
                             for d in DEPENDENCIES["modrinth"]],
            "game_versions": [self.minecraft],
            "version_type": self.release_type,
            "loaders": ["neoforge"],
            "featured": True,
            "project_id": project,
            "file_parts": ["file"],
            "primary_file": "file",
        }
        headers = {"Authorization": secret, "User-Agent": self.agent}
        listing = f"{api}/project/{project}/version"
        if self.dry_run:
            show("GET", listing, headers)
            show("POST", f"{api}/version", headers, data=json.dumps(metadata, indent=2), file=self.described())
            return
        # A published version is final on Modrinth too: it is never replaced.
        if any(v.get("version_number") == self.version for v in send("GET", listing, headers)):
            raise Published(f"Modrinth already has {self.version} in {project}, and a published version never changes.")
        content_type, body = multipart([("data", None, "application/json", json.dumps(metadata).encode()),
                                        self.file_part()])
        created = send("POST", f"{api}/version", {**headers, "Content-Type": content_type}, body)
        print(f"Uploaded {self.jar.name} to Modrinth as {self.version} ({created.get('id')})")

    def curseforge(self):
        secret = token("CURSEFORGE_TOKEN")
        project = project_of("curseforge")
        upload = os.environ.get("CURSEFORGE_UPLOAD_URL", CURSEFORGE_UPLOAD).rstrip("/")
        api = os.environ.get("CURSEFORGE_API_URL", CURSEFORGE_API).rstrip("/")
        upload_headers = {"X-Api-Token": secret, "User-Agent": self.agent}
        api_headers = {"User-Agent": self.agent, "Accept": "application/json"}
        files = f"{api}/api/v1/mods/{project}/files"
        metadata = {
            "changelog": self.notes,
            "changelogType": "markdown",
            "displayName": self.name,
            "releaseType": self.release_type,
        }
        required = DEPENDENCIES["curseforge"]
        if required:
            metadata["relations"] = {"projects": [{"slug": d, "type": "requiredDependency"} for d in required]}
        if self.dry_run:
            show("GET", files, api_headers)
            show("GET", f"{upload}/api/game/version-types", upload_headers)
            show("GET", f"{upload}/api/game/versions", upload_headers,
                 note=f"picks the ids of Minecraft {self.minecraft}, NeoForge, Client and Server")
            show("POST", f"{upload}/api/projects/{project}/upload-file", upload_headers,
                 metadata=json.dumps({**metadata, "gameVersions": f"<ids of {self.minecraft}, NeoForge, Client, Server>"}, indent=2),
                 file=self.described())
            return
        # A published version is final on CurseForge too: it is never replaced.
        if {self.jar.name, self.name} & self.curseforge_files(files, api_headers):
            raise Published(f"CurseForge already has {self.version} in {project}, "
                          "and a published version never changes.")
        metadata["gameVersions"] = self.curseforge_game_versions(upload, upload_headers)
        content_type, body = multipart([("metadata", None, "application/json", json.dumps(metadata).encode()),
                                        self.file_part()])
        created = send("POST", f"{upload}/api/projects/{project}/upload-file",
                       {**upload_headers, "Content-Type": content_type}, body)
        print(f"Uploaded {self.jar.name} to CurseForge as {self.version} ({created.get('id')})")

    def curseforge_files(self, url, headers):
        """The file and display names of every file the project has, across the listing's pages."""
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
        """The ids of the Minecraft version, of NeoForge, and of both environments (the Mod is needed on
        client and server alike), which the upload API names by id."""
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


SITES = {"modrinth": ("Modrinth", Release.modrinth), "curseforge": ("CurseForge", Release.curseforge)}


def main(args):
    usage = "usage: scripts/upload.py [--dry-run] [--site modrinth|curseforge] <major.minor.patch>"
    dry_run = "--dry-run" in args
    args = [a for a in args if a != "--dry-run"]
    sites = list(SITES)
    if args[:1] == ["--site"]:
        if len(args) < 2 or args[1] not in SITES:
            fail(usage)
        sites, args = [args[1]], args[2:]
    if len(args) != 1 or not re.fullmatch(r"\d+\.\d+\.\d+", args[0]):
        fail(usage)

    release = Release(args[0], dry_run)
    through_op(sys.argv[1:], sites)
    failed, published = [], []
    for site in sites:
        name, upload = SITES[site]
        try:
            upload(release)
        except Refused as refused:
            print(f"upload: {name}: {refused}", file=sys.stderr)
            (published if isinstance(refused, Published) else failed).append(site)
    if published and not failed:
        sys.exit(1)
    if failed:
        done = "" if len(failed) == len(sites) else "; the other site is done"
        fail(f"retry with {' and '.join('--site ' + site for site in failed)} once fixed{done}.")


if __name__ == "__main__":
    main(sys.argv[1:])
