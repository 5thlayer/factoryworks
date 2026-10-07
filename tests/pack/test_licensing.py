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

import json
import pathlib
import re
import subprocess
import tomllib
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
CORPUS_LICENCE = "LicenseRef-Wube-Factorio-Data"

# ADR-0103, #304: coined proper nouns of Wube's never appear in a string a player reads. A deny-list,
# so a term that matches nothing is the passing state.
COINED_TERMS = {
    "Nauvis": "Wube's name for the starting planet",
    "Vulcanus": "Wube's name for a Space Age planet",
    "Fulgora": "Wube's name for a Space Age planet",
    "Gleba": "Wube's name for a Space Age planet",
    "Aquilo": "Wube's name for a Space Age planet",
    "biter": "Wube's name for its melee enemy",
    "spitter": "Wube's name for its ranged enemy",
    "Wube": "the rights holder's own name; credit belongs in NOTICE and the store pages",
    "Factorio": "a trademark, kept out of player-facing text (ADR-0103)",
}
COINED = re.compile(r"\b(?:" + "|".join(COINED_TERMS) + r")s?\b", re.I)


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


def _js_code(text):
    """`text` with `//` and `/* */` comments blanked, string literals left alone."""
    out, i, quote = [], 0, None
    while i < len(text):
        c = text[i]
        if quote:
            out.append(c)
            if c == "\\":
                out.append(text[i + 1])
                i += 1
            elif c == quote:
                quote = None
        elif c in "'\"`":
            quote = c
            out.append(c)
        elif text.startswith("//", i):
            i = text.find("\n", i)
            i = len(text) if i < 0 else i - 1
        elif text.startswith("/*", i):
            i = text.find("*/", i) + 1
        else:
            out.append(c)
        i += 1
    return "".join(out)


_JS_STRING = r"""'((?:[^'\\\n]|\\.)*)'|"((?:[^"\\\n]|\\.)*)\""""


def _unescape(s):
    return re.sub(r"\\(.)", r"\1", s)


def _strings(text):
    return [_unescape(a or b) for a, b in re.findall(_JS_STRING, text)]


def _lang_values(path):
    """Every value of a lang file that is shown, which is every one whose key is not a `_` comment."""
    text = path.read_text(encoding="utf-8")
    if path.suffix == ".json":
        return [v for k, v in json.loads(text).items() if not k.startswith("_")]
    # FTB Quests' lang is JSON5: a value is a string no colon follows, in a list or after a key.
    out = []
    for m in re.finditer(_JS_STRING, text):
        if re.match(r"\s*:", text[m.end():]):
            continue
        out.append(_unescape(m.group(1) or m.group(2)))
    return out


def _lang_sources():
    files = sorted((ROOT / "kubejs/assets").glob("*/lang/*.json"))
    files += sorted((ROOT / "mod/src/main/resources/assets").glob("*/lang/*.json"))
    return {str(p.relative_to(ROOT)): _lang_values(p) for p in files}


def _display_names():
    out = {}
    for p in sorted((ROOT / "kubejs/startup_scripts").glob("*.js")):
        code = _js_code(p.read_text(encoding="utf-8"))
        out[str(p.relative_to(ROOT))] = [
            _unescape(a or b) for a, b in re.findall(r"\.displayName\(\s*(?:" + _JS_STRING + r")\s*\)", code)]
    return out


def _player_text():
    """source -> the strings a player reads from it."""
    return {**_lang_sources(), **_display_names()}


class LicensingTest(unittest.TestCase):
    def test_no_coined_factorio_name_in_a_string_a_player_reads(self):
        text = _player_text()
        for source in ("kubejs/assets/factoryworks/lang/en_us.json",
                       "kubejs/startup_scripts/blocks.js", "kubejs/startup_scripts/items.js"):
            self.assertTrue(text[source], f"{source} yielded no strings; the scan has rotted")
        hits = {f"{src}: {s!r}": m.group(0) for src, strings in text.items() for s in strings
                if (m := COINED.search(s))}
        self.assertEqual({}, hits, "a coined name in player-facing text (ADR-0103)")

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
        self.assertGreater(len(corpus), 10)
        wrong = [p for p in corpus if _licence_of(p)["SPDX-License-Identifier"] != CORPUS_LICENCE]
        self.assertEqual([], wrong)

    def test_the_corpus_holds_no_wube_display_text(self):
        # ADR-0103, #303: a `localised_*` field or a copy of the locale files is Wube's English.
        def keys(node):
            if isinstance(node, dict):
                for key, value in node.items():
                    yield key
                    yield from keys(value)
            elif isinstance(node, list):
                for value in node:
                    yield from keys(value)

        json_files = [p for p in _tracked() if p.startswith("data/factorio/") and p.endswith(".json")]
        self.assertGreater(len(json_files), 10)
        held = {p: sorted({k for k in keys(json.loads((ROOT / p).read_text(encoding="utf-8")))
                           if k.startswith("localised_")}) for p in json_files}
        self.assertEqual({}, {p: k for p, k in held.items() if k})
        self.assertFalse((ROOT / "data/factorio/recipe_name.json").exists())

    def test_the_packs_own_work_is_under_the_packs_licences(self):
        expect = {
            "mod/src/main/java/com/factoryworks/core/FactoryWorksCore.java": "LGPL-3.0-only",
            "mod/src/main/resources/META-INF/neoforge.mods.toml": "LGPL-3.0-only",
            "scripts/build-radar-assets.py": "LGPL-3.0-only",
            "scripts/upload.py": "MIT",
            "scripts/tests/test_upload.py": "MIT",
            "publish/upload.env": "MIT",
            "tests/pack/test_licensing.py": "LGPL-3.0-only",
            "kubejs/server_scripts/recipes.js": "LGPL-3.0-only",
            "docs/adr/0101-the-pack-is-factoryworks-an-overhaul-modpack.md": "CC-BY-4.0",
            "README.md": "CC-BY-4.0",
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

    def test_the_packs_licence_texts_exist(self):
        for path in ("LICENSE", "LICENSES/LGPL-3.0-only.txt", "LICENSES/CC-BY-4.0.txt",
                     f"LICENSES/{CORPUS_LICENCE}.txt"):
            self.assertTrue((ROOT / path).is_file(), path)

    def test_the_corpus_readme_states_the_exclusion(self):
        readme = (ROOT / "data/factorio/README.md").read_text(encoding="utf-8")
        section = re.search(r"^## Licence\n(.*?)(?=^## )", readme, re.M | re.S)
        self.assertIsNotNone(section, "data/factorio/README.md has no Licence section")
        prose = " ".join(section.group(1).split())
        self.assertIn("Wube Software's", prose)
        self.assertIn("under neither of the Pack's licences", prose)
        for name in ("LGPL-3.0-only", "CC BY 4.0", CORPUS_LICENCE):
            self.assertIn(name, prose)

    def test_the_upload_carries_the_licence_texts(self):
        # packwiz indexes what `.packwizignore` does not exclude, and the upload is the index.
        paths = ["LICENSE", "NOTICE", "REUSE.toml"] + sorted(
            str(p.relative_to(ROOT)) for p in (ROOT / "LICENSES").glob("*.txt"))
        out = subprocess.run(
            ["git", "-c", "core.excludesFile=.packwizignore", "check-ignore", "--no-index", "-v",
             *paths], cwd=ROOT, capture_output=True, text=True).stdout
        self.assertEqual([], [line for line in out.splitlines()
                              if line.startswith(".packwizignore:")])


if __name__ == "__main__":
    unittest.main()
