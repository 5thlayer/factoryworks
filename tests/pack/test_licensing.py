#!/usr/bin/env python3
"""Assert every tracked file has a licence, and that the boundaries ADR-0102 draws are where it says.

`REUSE.toml` is the licence map (#302): code LGPL-3.0-only, the Pack's own content CC BY 4.0, and
everything that is not the Pack's own -- Wube's corpus, third-party art, vendored skills -- under
its own. `reuse lint` is the authority and runs in CI, but it needs libmagic, which a developer machine may
lack, so the same rule is implemented here: an annotation's globs, the last match winning.

A file no annotation reaches is "unlicensed", which to a redistributor reads as all rights
reserved. The boundary assertions are the ones a glob change can silently move: the corpus must
never fall under either of the Pack's licences, and each art credit in `NOTICE` must resolve to the
licence `NOTICE` names for it.
"""

import pathlib
import re
import subprocess
import tomllib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
CORPUS_LICENCE = "LicenseRef-Wube-Factorio-Data"


def _glob(pattern):
    """REUSE's glob: `*` stops at `/`, `**` does not, and `**/` may match no directory."""
    out, i = [], 0
    while i < len(pattern):
        if pattern.startswith("**/", i):
            out.append("(?:.*/)?")
            i += 3
        elif pattern.startswith("**", i):
            out.append(".*")
            i += 2
        elif pattern[i] == "*":
            out.append("[^/]*")
            i += 1
        else:
            out.append(re.escape(pattern[i]))
            i += 1
    return re.compile("".join(out) + r"\Z")


def _annotations():
    data = tomllib.loads((ROOT / "REUSE.toml").read_text(encoding="utf-8"))
    for a in data["annotations"]:
        paths = [a["path"]] if isinstance(a["path"], str) else a["path"]
        yield [_glob(p) for p in paths], a


def _tracked():
    """What `reuse lint` walks: `LICENSE`, `REUSE.toml` and the texts need no licence of their own."""
    out = subprocess.run(["git", "ls-files", "-z"], cwd=ROOT, check=True,
                         capture_output=True, text=True).stdout
    return [p for p in out.split("\0")
            if p and p not in ("LICENSE", "REUSE.toml") and not p.startswith("LICENSES/")]


def _licence_of(path):
    """The annotation that decides `path`: an `override` beats the rest, else the last match."""
    hit = None
    for globs, a in _annotations():
        if any(g.match(path) for g in globs):
            if a.get("precedence") == "override":
                return a
            hit = a
    return hit


def _ids(expression):
    return {t for t in re.split(r"\s+(?:AND|OR)\s+", expression.strip())}


class LicensingTest(unittest.TestCase):
    def test_every_tracked_file_has_a_licence_and_a_holder(self):
        bare = [p for p in _tracked() if (a := _licence_of(p)) is None
                or not a.get("SPDX-License-Identifier") or not a.get("SPDX-FileCopyrightText")]
        self.assertEqual([], bare[:20], f"{len(bare)} tracked files no annotation reaches")

    def test_licences_and_texts_match_both_ways(self):
        used = set()
        for _, a in _annotations():
            used |= _ids(a["SPDX-License-Identifier"])
        shipped = {p.stem for p in (ROOT / "LICENSES").glob("*.txt")}
        self.assertEqual(set(), used - shipped, "an annotation names a licence with no text")
        self.assertEqual(set(), shipped - used, "a licence text nothing uses")

    def test_the_corpus_is_under_neither_of_the_packs_licences(self):
        corpus = [p for p in _tracked() if p.startswith("data/factorio/")
                  and p != "data/factorio/README.md"]
        corpus.append("kubejs/server_scripts/factorio_tech_data.js")
        self.assertGreater(len(corpus), 10)
        wrong = [p for p in corpus if _licence_of(p)["SPDX-License-Identifier"] != CORPUS_LICENCE]
        self.assertEqual([], wrong)

    def test_the_packs_own_work_is_under_the_packs_licences(self):
        expect = {
            "mod/src/main/java/com/factoryworks/core/FactoryWorksCore.java": "LGPL-3.0-only",
            "mod/src/main/resources/META-INF/neoforge.mods.toml": "LGPL-3.0-only",
            "scripts/build-radar-assets.py": "LGPL-3.0-only",
            "tests/pack/test_licensing.py": "LGPL-3.0-only",
            "kubejs/server_scripts/recipes.js": "LGPL-3.0-only",
            "docs/adr/0101-the-pack-is-factoryworks-an-overhaul-modpack.md": "CC-BY-4.0",
            "publish/description.md": "CC-BY-4.0",
            "data/pack/item-map.json": "CC-BY-4.0",
            "gradlew": "Apache-2.0",
        }
        for path, licence in expect.items():
            self.assertTrue((ROOT / path).exists(), path)
            self.assertEqual(licence, _licence_of(path)["SPDX-License-Identifier"], path)

    def test_the_mod_declares_the_licence_the_map_gives_it(self):
        # Gradle expands `${...}` in this file, so it is not yet TOML.
        text = (ROOT / "mod/src/main/resources/META-INF/neoforge.mods.toml").read_text(
            encoding="utf-8")
        self.assertEqual("LGPL-3.0-only", re.search(r'^license = "([^"]+)"', text, re.M).group(1))

    def test_notice_credits_resolve_to_the_licence_notice_names(self):
        notice = (ROOT / "NOTICE").read_text(encoding="utf-8")
        by_url = {
            "creativecommons.org/licenses/by-nc-sa/4.0": "CC-BY-NC-SA-4.0",
            "creativecommons.org/licenses/by/4.0": "CC-BY-4.0",
        }
        checked = 0
        for block in re.split(r"\n(?=[^\n]+\n-{5,}\n)", notice):
            files = re.findall(r"^\s*(?:Files:)?\s+((?:kubejs|data)/\S+\.png)$", block, re.M)
            url = re.search(r"Licence:.*?(creativecommons\.org/licenses/\S+?)/?\s*$", block,
                            re.M | re.S)
            if not files or not url:
                continue
            want = by_url[url.group(1)]
            for f in files:
                checked += 1
                self.assertIn(want, _ids(_licence_of(f)["SPDX-License-Identifier"]), f)
        self.assertGreater(checked, 30)

    def test_the_corpus_readme_states_the_exclusion(self):
        readme = (ROOT / "data/factorio/README.md").read_text(encoding="utf-8")
        self.assertIn(CORPUS_LICENCE, readme)
        self.assertIn("LGPL-3.0", readme)
        self.assertIn("CC BY 4.0", readme)


if __name__ == "__main__":
    unittest.main()
