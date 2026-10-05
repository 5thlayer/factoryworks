# Drives scripts/upload.py as a person does, a version, the environment and a maven repository
# holding a jar, against the local stand-in, and checks only what reaches it and the exit status.
# Run with: python3 -m unittest discover scripts/tests
import io
import json
import os
import shutil
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

from standin import StandIn

SCRIPT = Path(__file__).resolve().parents[1] / "upload.py"
SECRETS = {"MODRINTH_TOKEN": "mrp_standin-secret-token", "CURSEFORGE_TOKEN": "cf-upload-secret-token"}
MODRINTH_PROJECT = "factoryworks-core-standin"
CF_PROJECT = "123456"
NOTES = "- A Consumer can read the thing.\n- The other thing is faster."

CHANGELOG = """# Changelog

## Unreleased

- Not yet released.

## 0.3.9

- A Consumer can read the thing.
- The other thing is faster.

## 0.3.8

- Older.
"""

PROPERTIES = """minecraft_version=26.1.2
mod_id=factoryworks_core
mod_name=FactoryWorks Core
mod_version=0.3.9
maven_group=com.factoryworks
upload_release_type=
"""


def jar(**overrides):
    entries = {"LICENSE": "LGPL", "NOTICE": "credits", "LICENSES/LGPL-3.0-only.txt": "LGPL",
               "LICENSES/CC-BY-4.0.txt": "CC BY", "com/factoryworks/core/FactoryWorksCore.class": b"\xca\xfe\xba\xbe",
               **overrides}
    out = io.BytesIO()
    with zipfile.ZipFile(out, "w") as z:
        for name, data in entries.items():
            if data is not None:
                z.writestr(name, data)
    return out.getvalue()


class Upload(unittest.TestCase):
    def setUp(self):
        self.site = StandIn()
        self.addCleanup(self.site.close)
        self.root = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.root)
        # The script reads the changelog and gradle.properties of the checkout it sits in.
        (self.root / "scripts").mkdir()
        shutil.copy(SCRIPT, self.root / "scripts")
        (self.root / "publish/core").mkdir(parents=True)
        (self.root / "publish/core/changelog.md").write_text(CHANGELOG)
        (self.root / "gradle.properties").write_text(PROPERTIES)
        self.maven = self.root / "m2"
        self.env = {"PATH": os.environ["PATH"], "MAVEN_REPO_LOCAL": str(self.maven), **SECRETS,
                    "MODRINTH_PROJECT_ID": MODRINTH_PROJECT, "MODRINTH_API_URL": self.site.url + "/modrinth",
                    "CURSEFORGE_PROJECT_ID": CF_PROJECT, "CURSEFORGE_UPLOAD_URL": self.site.url + "/cf-upload",
                    "CURSEFORGE_API_URL": self.site.url + "/cf-site"}

    def publish(self, version, data=None):
        folder = self.maven / "com/factoryworks/factoryworks_core" / version
        folder.mkdir(parents=True)
        data = jar() if data is None else data
        (folder / f"factoryworks_core-{version}.jar").write_bytes(data)
        return data

    def upload(self, *args, **env):
        environment = {**self.env, **env}
        environment = {k: v for k, v in environment.items() if v is not None}
        result = subprocess.run([sys.executable, str(self.root / "scripts/upload.py"), *args],
                                env=environment, capture_output=True, text=True)
        for secret in SECRETS.values():
            self.assertNotIn(secret, result.stdout + result.stderr)
        return result

    def modrinth_post(self):
        posts = self.site.sent("POST", "/modrinth/")
        self.assertEqual(len(posts), 1)
        return posts[0]

    def curseforge_post(self):
        posts = self.site.sent("POST", "/cf-upload/")
        self.assertEqual(len(posts), 1)
        return posts[0]

    def assertRefusedBeforeAnyRequest(self, result, message):
        self.assertNotEqual(result.returncode, 0)
        self.assertIn(message, result.stderr)
        self.assertEqual(self.site.requests, [])

    # Both sites

    def test_both_sites_get_the_jar_in_the_maven_repository_byte_for_byte(self):
        data = self.publish("0.3.9")
        result = self.upload("0.3.9")
        self.assertEqual(result.returncode, 0, result.stderr)
        for post in [self.modrinth_post(), self.curseforge_post()]:
            self.assertEqual(post.parts()["file"], (data, "factoryworks_core-0.3.9.jar"))

    def test_the_maven_repository_defaults_to_the_home_one(self):
        home = self.root / "home"
        self.maven = home / ".m2/repository"
        data = self.publish("0.3.9")
        self.assertEqual(self.upload("0.3.9", MAVEN_REPO_LOCAL=None, HOME=str(home)).returncode, 0)
        self.assertEqual(self.modrinth_post().parts()["file"][0], data)
        self.assertEqual(self.curseforge_post().parts()["file"][0], data)

    def test_refuses_a_version_missing_from_the_maven_repository(self):
        self.assertRefusedBeforeAnyRequest(self.upload("0.3.9"), "not in")

    def test_refuses_a_version_the_changelog_lacks(self):
        self.publish("0.3.7")
        self.assertRefusedBeforeAnyRequest(self.upload("0.3.7"), "publish/core/changelog.md")

    def test_refuses_a_jar_that_lacks_its_licensing(self):
        for lacking in ["LICENSE", "NOTICE", "LICENSES/LGPL-3.0-only.txt", "LICENSES/CC-BY-4.0.txt"]:
            with self.subTest(lacking):
                shutil.rmtree(self.maven, ignore_errors=True)
                self.publish("0.3.9", jar(**{lacking: None}))
                self.assertRefusedBeforeAnyRequest(self.upload("0.3.9"), lacking)

    def test_a_missing_token_is_fetched_through_op_run(self):
        # A stand-in for the 1Password CLI, first on PATH: it fills each op:// reference in the env
        # file with a token, as `op run --env-file=<file> -- <command>` does, and runs the command.
        bin = self.root / "bin"
        bin.mkdir()
        (bin / "op").write_text(r"""#!/bin/sh
[ "$1" = run ] || exit 9
file="${2#--env-file=}"; shift 3
while IFS='=' read -r name value; do
    case "$name" in \#*|"") continue ;; esac
    export "$name=op-filled-${value##*/}"
done < "$file"
exec "$@"
""")
        (bin / "op").chmod(0o755)
        (self.root / "publish/upload.env").write_text("# references\nMODRINTH_TOKEN=op://Private/Beltworks Modrinth/modrinth-credential\n")
        self.publish("0.3.9")
        result = self.upload("--site", "modrinth", "0.3.9", MODRINTH_TOKEN=None,
                             PATH=f"{bin}{os.pathsep}{os.environ['PATH']}")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.modrinth_post().headers["Authorization"], "op-filled-modrinth-credential")

    def test_a_token_op_run_leaves_missing_fails_instead_of_looping(self):
        bin = self.root / "bin"
        bin.mkdir()
        (bin / "op").write_text('#!/bin/sh\nshift 3\nexec "$@"\n')
        (bin / "op").chmod(0o755)
        (self.root / "publish/upload.env").write_text("")
        self.publish("0.3.9")
        result = self.upload("--site", "modrinth", "0.3.9", MODRINTH_TOKEN=None,
                             PATH=f"{bin}{os.pathsep}{os.environ['PATH']}")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("MODRINTH_TOKEN", result.stderr)

    def properties(self, **values):
        """gradle.properties with the given settings replaced."""
        text = PROPERTIES
        for name, value in values.items():
            text = "".join(f"{name}={value}\n" if line.startswith(f"{name}=") else line + "\n"
                           for line in text.splitlines())
        (self.root / "gradle.properties").write_text(text)

    def test_the_projects_are_cores_own(self):
        self.publish("0.3.9")
        result = self.upload("--dry-run", "0.3.9", MODRINTH_PROJECT_ID=None, CURSEFORGE_PROJECT_ID=None)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("/modrinth/project/7wb8sJtC/version", result.stdout)
        self.assertIn("/cf-upload/api/projects/1718187/upload-file", result.stdout)

    def test_a_missing_token_fails_clearly_and_leaves_the_other_site(self):
        self.publish("0.3.9")
        curseforge, modrinth = ("cf-site", "cf-upload"), ("modrinth",)
        for name, skipped, other in [("MODRINTH_TOKEN", modrinth, "/cf-upload/"),
                                     ("CURSEFORGE_TOKEN", curseforge, "/modrinth/")]:
            for unset in [None, ""]:
                with self.subTest(name, unset=unset):
                    self.site.requests.clear()
                    self.site.modrinth.clear()
                    self.site.curseforge.clear()
                    result = self.upload("0.3.9", **{name: unset})
                    self.assertNotEqual(result.returncode, 0)
                    self.assertIn(name, result.stderr)
                    self.assertIn("publish/upload.env", result.stderr)
                    self.assertFalse({r.path.split("/")[1] for r in self.site.requests} & set(skipped))
                    self.assertEqual(len(self.site.sent("POST", other)), 1)

    def test_a_failure_on_one_site_leaves_the_other_and_it_can_be_retried_alone(self):
        self.publish("0.3.9")
        self.site.broken = {"modrinth"}
        result = self.upload("0.3.9")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("500", result.stderr)
        self.assertEqual(len(self.site.sent("POST", "/cf-upload/")), 1)

        self.site.broken = set()
        self.site.requests.clear()
        result = self.upload("--site", "modrinth", "0.3.9")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual({r.path.split("/")[1] for r in self.site.requests}, {"modrinth"})
        self.modrinth_post()

    def test_a_failure_on_curseforge_leaves_modrinth(self):
        self.publish("0.3.9")
        self.site.broken = {"cf-upload"}
        result = self.upload("0.3.9")
        self.assertNotEqual(result.returncode, 0)
        self.modrinth_post()

    def test_refuses_an_unknown_site(self):
        self.publish("0.3.9")
        self.assertRefusedBeforeAnyRequest(self.upload("--site", "planetminecraft", "0.3.9"), "usage")

    def test_a_dry_run_prints_the_requests_for_both_sites_and_contacts_nothing(self):
        self.publish("0.3.9")
        result = self.upload("--dry-run", "0.3.9")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.site.requests, [])
        for line in [f"GET {self.site.url}/modrinth/project/{MODRINTH_PROJECT}/version",
                     f"POST {self.site.url}/modrinth/version",
                     "Authorization: <redacted>",
                     f"GET {self.site.url}/cf-site/api/v1/mods/{CF_PROJECT}/files",
                     f"GET {self.site.url}/cf-upload/api/game/versions",
                     f"POST {self.site.url}/cf-upload/api/projects/{CF_PROJECT}/upload-file",
                     "X-Api-Token: <redacted>",
                     "factoryworks_core-0.3.9.jar", '"version_type": "beta"', '"releaseType": "beta"']:
            self.assertIn(line, result.stdout)

    # Modrinth

    def test_modrinth_authenticates_with_the_token(self):
        self.publish("0.3.9")
        self.upload("0.3.9")
        self.assertEqual(self.modrinth_post().headers["Authorization"], SECRETS["MODRINTH_TOKEN"])

    def test_modrinth_gets_the_changelog_section_and_the_game(self):
        self.publish("0.3.9")
        self.upload("0.3.9")
        data = json.loads(self.modrinth_post().parts()["data"][0])
        self.assertEqual(data["changelog"], NOTES)
        self.assertEqual(data["project_id"], MODRINTH_PROJECT)
        self.assertEqual(data["version_number"], "0.3.9")
        self.assertEqual(data["game_versions"], ["26.1.2"])
        self.assertEqual(data["loaders"], ["neoforge"])
        self.assertEqual(data["version_type"], "beta")
        self.assertEqual(data["dependencies"], [{"project_id": "4sYI62kA", "dependency_type": "required"},
                                                {"project_id": "p4zxipln", "dependency_type": "required"}])
        self.assertEqual(data["file_parts"], ["file"])

    def test_a_release_from_1_0_is_a_release_on_both_sites(self):
        (self.root / "publish/core/changelog.md").write_text("## 1.0.0\n\n- Stable.\n")
        self.publish("1.0.0")
        self.assertEqual(self.upload("1.0.0").returncode, 0)
        self.assertEqual(json.loads(self.modrinth_post().parts()["data"][0])["version_type"], "release")
        self.assertEqual(json.loads(self.curseforge_post().parts()["metadata"][0])["releaseType"], "release")

    def test_a_gradle_properties_without_upload_release_type_keeps_the_default(self):
        (self.root / "gradle.properties").write_text(PROPERTIES.replace("upload_release_type=\n", ""))
        self.publish("0.3.9")
        self.assertEqual(self.upload("0.3.9").returncode, 0)
        self.assertEqual(json.loads(self.modrinth_post().parts()["data"][0])["version_type"], "beta")
        self.assertEqual(json.loads(self.curseforge_post().parts()["metadata"][0])["releaseType"], "beta")

    def test_upload_release_type_sets_the_type_on_both_sites(self):
        (self.root / "publish/core/changelog.md").write_text(CHANGELOG + "\n## 1.0.0\n\n- Stable.\n")
        self.publish("0.3.9")
        self.publish("1.0.0")
        for kind in ["release", "beta", "alpha"]:
            for version in ["0.3.9", "1.0.0"]:
                with self.subTest(kind, version=version):
                    self.site.requests.clear()
                    self.site.modrinth.clear()
                    self.site.curseforge.clear()
                    self.properties(upload_release_type=kind)
                    self.assertEqual(self.upload(version).returncode, 0)
                    self.assertEqual(json.loads(self.modrinth_post().parts()["data"][0])["version_type"], kind)
                    self.assertEqual(json.loads(self.curseforge_post().parts()["metadata"][0])["releaseType"], kind)

    def test_a_dry_run_shows_the_upload_release_type(self):
        self.properties(upload_release_type="release")
        self.publish("0.3.9")
        result = self.upload("--dry-run", "0.3.9")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('"version_type": "release"', result.stdout)
        self.assertIn('"releaseType": "release"', result.stdout)

    def test_refuses_an_unknown_upload_release_type(self):
        self.properties(upload_release_type="stable")
        self.publish("0.3.9")
        self.assertRefusedBeforeAnyRequest(self.upload("0.3.9"), "upload_release_type")

    def test_refuses_a_version_modrinth_already_has(self):
        self.publish("0.3.9")
        self.site.modrinth[MODRINTH_PROJECT] = ["0.3.9"]
        result = self.upload("--site", "modrinth", "0.3.9")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Modrinth already has 0.3.9", result.stderr)
        self.assertNotIn("retry", result.stderr)
        self.assertEqual([r.method for r in self.site.requests], ["GET"])

    def test_a_modrinth_error_fails_the_site(self):
        self.publish("0.3.9")
        result = self.upload("--site", "modrinth", "0.3.9", MODRINTH_API_URL=self.site.url + "/elsewhere")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("404", result.stderr)

    # CurseForge

    def test_curseforge_authenticates_the_upload_with_the_token_and_lists_files_without_it(self):
        self.publish("0.3.9")
        self.upload("0.3.9")
        self.assertEqual(self.curseforge_post().headers["X-Api-Token"], SECRETS["CURSEFORGE_TOKEN"])
        listings = self.site.sent("GET", "/cf-site/")
        self.assertTrue(listings)
        for listing in listings:
            self.assertNotIn("X-Api-Token", listing.headers)

    def test_curseforge_gets_the_changelog_section_and_the_game(self):
        self.publish("0.3.9")
        self.upload("0.3.9")
        metadata = json.loads(self.curseforge_post().parts()["metadata"][0])
        self.assertEqual(metadata["changelog"], NOTES)
        self.assertEqual(metadata["changelogType"], "markdown")
        self.assertEqual(metadata["displayName"], "FactoryWorks Core 0.3.9")
        # 26.1.2 the Minecraft version, not the Bukkit one, NeoForge the loader, and both environments.
        self.assertEqual(sorted(metadata["gameVersions"]), [101, 301, 401, 402])
        self.assertEqual(metadata["releaseType"], "beta")
        self.assertEqual(metadata["relations"], {"projects": [{"slug": "oritech", "type": "requiredDependency"},
                                                              {"slug": "beltworks", "type": "requiredDependency"}]})

    def test_refuses_a_version_curseforge_already_has(self):
        self.publish("0.3.9")
        # Past the first page of the listing.
        self.site.curseforge[CF_PROJECT] = ["factoryworks_core-0.3.6.jar", "factoryworks_core-0.3.7.jar",
                                          "factoryworks_core-0.3.9.jar"]
        result = self.upload("--site", "curseforge", "0.3.9")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("CurseForge already has 0.3.9", result.stderr)
        self.assertEqual(self.site.sent("POST", "/cf-upload/"), [])

    def test_refuses_a_version_curseforge_has_under_another_file_name(self):
        self.publish("0.3.9")
        self.site.curseforge[CF_PROJECT] = ["FactoryWorks Core 0.3.9"]
        result = self.upload("--site", "curseforge", "0.3.9")
        self.assertIn("CurseForge already has 0.3.9", result.stderr)
        self.assertEqual(self.site.sent("POST", "/cf-upload/"), [])

    def test_uploads_a_version_curseforge_lacks_among_others(self):
        self.publish("0.3.9")
        self.site.curseforge[CF_PROJECT] = ["factoryworks_core-0.3.6.jar", "factoryworks_core-0.3.7.jar",
                                          "factoryworks_core-0.3.8.jar"]
        self.assertEqual(self.upload("--site", "curseforge", "0.3.9").returncode, 0)
        self.curseforge_post()

    def test_refuses_a_game_version_curseforge_does_not_know(self):
        self.publish("0.3.9")
        (self.root / "gradle.properties").write_text(PROPERTIES.replace("26.1.2", "27.0.0"))
        result = self.upload("--site", "curseforge", "0.3.9")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("27.0.0", result.stderr)
        self.assertEqual(self.site.sent("POST", "/cf-upload/"), [])


if __name__ == "__main__":
    unittest.main()
