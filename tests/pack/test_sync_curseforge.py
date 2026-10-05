"""`scripts/sync-local-jars.py` pins a jar at once and names its CurseForge file once listed.

Runs the sync over a scratch `~/.m2` and `mods/`, with CurseForge, packwiz and Gradle stood in
for: the local pin never waits on CurseForge, a pending reference passes `--check` and fails
`--check --strict`, and a later plain sync fills it in. FactoryWorks Core's reference follows
`gradle.properties`' `mod_version` the same way.
"""
import hashlib
import importlib.util
import json
import urllib.error
import zipfile
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parent.parent.parent
SYNC = ROOT / "scripts" / "sync-local-jars.py"
PROJECT = 1714527
CORE_PROJECT = 1718187


def core_jar(m2, version):
    folder = m2 / "com" / "factoryworks" / "factoryworks_core" / version
    folder.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(folder / f"factoryworks_core-{version}.jar", "w") as jar:
        jar.writestr("version.txt", version)
    return folder / f"factoryworks_core-{version}.jar"


def metafile(path, jar, project, file_id):
    path.write_text(f'filename = "{jar.name}"\n[download]\nhash-format = "sha1"\n'
                    f'hash = "{hashlib.sha1(jar.read_bytes()).hexdigest()}"\n'
                    f'[update.curseforge]\nfile-id = {file_id}\nproject-id = {project}\n')


@pytest.fixture
def pack(tmp_path, monkeypatch):
    spec = importlib.util.spec_from_file_location("sync_local_jars", SYNC)
    sync = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(sync)

    mods = tmp_path / "mods"
    mods.mkdir()
    m2 = tmp_path / "m2"
    for version in ("0.1.0", "0.2.0"):
        folder = m2 / "io" / "x" / "beltworks" / version
        folder.mkdir(parents=True)
        with zipfile.ZipFile(folder / f"beltworks-{version}.jar", "w") as jar:
            jar.writestr("version.txt", version)
    table = tmp_path / "local-jars.json"
    table.write_text(json.dumps({"jars": [{
        "mod": "beltworks", "group": "io.x", "artifact": "beltworks", "version": "0.1.0",
        "pattern": "beltworks-*.jar", "curseforge": PROJECT}]}))
    index = tmp_path / "index.toml"
    index.write_text('[[files]]\nfile = "mods/beltworks.pw.toml"\n'
                     '[[files]]\nfile = "mods/factoryworks-core.pw.toml"\n')
    # Core is released at 0.1.0 and referenced, as the Pack holds it between Core releases.
    properties = tmp_path / "gradle.properties"
    properties.write_text("mod_id=factoryworks_core\nmod_version=0.1.0\nmaven_group=com.factoryworks\n")
    metafile(mods / "factoryworks-core.pw.toml", core_jar(m2, "0.1.0"), CORE_PROJECT, 10)
    for name, value in (("MODS", mods), ("M2", m2), ("TABLE", table), ("INDEX", index),
                        ("PROPERTIES", properties)):
        monkeypatch.setattr(sync, name, value)

    listed = {}
    core_listed = {}
    queried = []

    def curseforge_file(row):
        queried.append(row["version"])
        return (core_listed if row["curseforge"] == CORE_PROJECT else listed).get(row["version"])

    def run(step, command):
        if command[0] == "packwiz":
            file_id = command[command.index("--file-id") + 1]
            if command[command.index("--addon-id") + 1] == str(CORE_PROJECT):
                version = next(v for v, f in core_listed.items() if str(f) == file_id)
                metafile(mods / "core-slug.pw.toml", core_jar(m2, version), CORE_PROJECT, file_id)
            else:
                version = next(v for v, f in listed.items() if str(f) == file_id)
                metafile(mods / "beltworks-slug.pw.toml", mods / f"beltworks-{version}.jar", PROJECT, file_id)

    monkeypatch.setattr(sync, "curseforge_file", curseforge_file)
    monkeypatch.setattr(sync, "run", run)

    def main(*args):
        monkeypatch.setattr("sys.argv", ["sync-local-jars.py", *args])
        sync.main()

    sync.listed, sync.core_listed, sync.queried, sync.main_with = listed, core_listed, queried, main
    sync.release_core = lambda version: (core_jar(m2, version), properties.write_text(
        properties.read_text().replace("mod_version=0.1.0", f"mod_version={version}")))
    return sync


def strict_check_fails(pack):
    with pytest.raises(SystemExit) as exit:
        pack.main_with("--check", "--strict")
    return exit.value.code == 1


def test_listed_file_is_referenced(pack):
    pack.listed["0.2.0"] = 22

    pack.main_with("beltworks=0.2.0")

    meta = (pack.MODS / "beltworks.pw.toml").read_text()
    assert 'filename = "beltworks-0.2.0.jar"' in meta and "file-id = 22" in meta
    pack.main_with("--check", "--strict")


def test_unlisted_file_pins_at_once_and_is_pending(pack, capsys):
    pack.listed["0.1.0"] = 11
    pack.main_with()
    capsys.readouterr()

    pack.main_with("beltworks=0.2.0")

    assert json.loads(pack.TABLE.read_text())["jars"][0]["version"] == "0.2.0"
    assert sorted(p.name for p in pack.MODS.glob("beltworks*")) == ["beltworks-0.2.0.jar"]
    assert "pending beltworks 0.2.0" in capsys.readouterr().out
    pack.main_with("--check")
    assert "pending beltworks 0.2.0" in capsys.readouterr().out
    assert strict_check_fails(pack)


def test_later_sync_fills_pending_reference(pack):
    pack.main_with("beltworks=0.2.0")
    assert strict_check_fails(pack)

    pack.listed["0.2.0"] = 22
    pack.main_with()

    assert "file-id = 22" in (pack.MODS / "beltworks.pw.toml").read_text()
    pack.main_with("--check", "--strict")


def test_filled_reference_is_not_queried_again(pack):
    pack.listed["0.2.0"] = 22
    pack.main_with("beltworks=0.2.0")
    pack.queried.clear()

    pack.main_with()

    assert pack.queried == []


def test_stale_reference_fails_even_plain_check(pack, capsys):
    pack.listed["0.1.0"] = 11
    pack.main_with()
    table = json.loads(pack.TABLE.read_text())
    table["jars"][0]["version"] = "0.2.0"
    pack.TABLE.write_text(json.dumps(table))
    (pack.MODS / "beltworks-0.1.0.jar").unlink()
    pack.install(table["jars"][0])

    with pytest.raises(SystemExit) as exit:
        pack.main_with("--check")

    assert exit.value.code == 1
    assert "names beltworks-0.1.0.jar" in capsys.readouterr().err


def test_unreachable_curseforge_is_pending(pack, monkeypatch):
    spec = importlib.util.spec_from_file_location("sync_unstubbed", SYNC)
    sync = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(sync)

    def offline(*args, **kwargs):
        raise urllib.error.URLError("offline")

    monkeypatch.setattr(sync.urllib.request, "urlopen", offline)

    assert sync.curseforge_file({"curseforge": PROJECT, "artifact": "beltworks",
                                 "version": "0.2.0"}) is None


def test_a_core_release_is_pending_until_curseforge_lists_it(pack, capsys):
    pack.listed["0.1.0"] = 11
    pack.release_core("0.2.0")

    pack.main_with()

    assert "pending factoryworks-core 0.2.0" in capsys.readouterr().out
    assert not (pack.MODS / "factoryworks-core.pw.toml").exists()
    assert strict_check_fails(pack)

    pack.core_listed["0.2.0"] = 33
    pack.main_with()

    meta = (pack.MODS / "factoryworks-core.pw.toml").read_text()
    assert 'filename = "factoryworks_core-0.2.0.jar"' in meta and "file-id = 33" in meta
    pack.main_with("--check", "--strict")


def test_a_core_reference_older_than_mod_version_fails_even_plain_check(pack, capsys):
    pack.listed["0.1.0"] = 11
    pack.main_with()
    pack.main_with("--check", "--strict")
    capsys.readouterr()
    pack.release_core("0.2.0")

    with pytest.raises(SystemExit) as exit:
        pack.main_with("--check")

    assert exit.value.code == 1
    assert "names factoryworks_core-0.1.0.jar" in capsys.readouterr().err


def test_a_core_reference_to_another_jar_than_the_released_one_fails(pack, capsys):
    pack.main_with()
    jar = pack.M2 / "com/factoryworks/factoryworks_core/0.1.0/factoryworks_core-0.1.0.jar"
    with zipfile.ZipFile(jar, "w") as rebuilt:
        rebuilt.writestr("version.txt", "rebuilt")

    with pytest.raises(SystemExit):
        pack.main_with("--check")

    assert "hashes another file" in capsys.readouterr().err
