"""`scripts/pack-check.sh` undoes only its own refresh when it fails (#625).

Runs the script in a scratch repo with a stand-in `packwiz` whose refresh rewrites the manifest
and writes a metafile of its own, after a sync has left manifest edits unstaged, staged and
untracked.
"""
import os
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent

FAKE_PACKWIZ = """#!/usr/bin/env bash
[[ "$1" == refresh ]] || exit 2
echo "# refreshed" >> index.toml
echo "# refreshed" >> mods/a.pw.toml
printf 'filename = "a.jar"\\n' > mods/new.pw.toml
"""


def git(repo, *args):
    return subprocess.run(["git", *args], cwd=repo, check=True, capture_output=True, text=True).stdout


def make_repo(tmp_path):
    repo = tmp_path / "pack"
    (repo / "scripts").mkdir(parents=True)
    shutil.copy(ROOT / "scripts" / "pack-check.sh", repo / "scripts" / "pack-check.sh")
    (repo / ".gitignore").write_text("mods/*\n!mods/*.pw.toml\n")
    (repo / "index.toml").write_text("index = 1\n")
    (repo / "pack.toml").write_text("pack = 1\n")
    (repo / "mods").mkdir()
    (repo / "mods" / "a.pw.toml").write_text('filename = "a.jar"\n')
    (repo / "mods" / "b.pw.toml").write_text('filename = "b.jar"\n')
    for jar in ("a.jar", "b.jar", "c.jar"):
        (repo / "mods" / jar).write_bytes(b"jar")
    git(repo, "init", "-q")
    git(repo, "add", ".")
    git(repo, "-c", "user.name=t", "-c", "user.email=t@t", "commit", "-qm", "base")

    bin_dir = tmp_path / "bin"
    bin_dir.mkdir()
    (bin_dir / "packwiz").write_text(FAKE_PACKWIZ)
    (bin_dir / "packwiz").chmod(0o755)
    return repo, bin_dir


def run_check(repo, bin_dir):
    env = {**os.environ, "PATH": f"{bin_dir}{os.pathsep}{os.environ['PATH']}"}
    return subprocess.run(["bash", "scripts/pack-check.sh"], cwd=repo, env=env,
                          capture_output=True, text=True)


def test_failed_check_keeps_uncommitted_manifest_edits(tmp_path):
    repo, bin_dir = make_repo(tmp_path)
    mods = repo / "mods"
    (mods / "a.pw.toml").write_text('filename = "a.jar"\n# synced\n')
    (repo / "index.toml").write_text("index = 2\n")
    (mods / "b.pw.toml").write_text('filename = "b.jar"\n# staged\n')
    git(repo, "add", "mods/b.pw.toml")
    (mods / "b.pw.toml").write_text('filename = "b.jar"\n# staged, then edited\n')
    (mods / "c.pw.toml").write_text('filename = "c.jar"\n')
    staged_before = git(repo, "diff", "--cached")

    result = run_check(repo, bin_dir)

    assert result.returncode == 1, result.stdout + result.stderr
    assert (mods / "a.pw.toml").read_text() == 'filename = "a.jar"\n# synced\n'
    assert (repo / "index.toml").read_text() == "index = 2\n"
    assert (mods / "b.pw.toml").read_text() == 'filename = "b.jar"\n# staged, then edited\n'
    assert (mods / "c.pw.toml").read_text() == 'filename = "c.jar"\n'
    assert not (mods / "new.pw.toml").exists()
    assert git(repo, "diff", "--cached") == staged_before


def test_failed_check_on_a_clean_tree_leaves_it_clean(tmp_path):
    repo, bin_dir = make_repo(tmp_path)

    result = run_check(repo, bin_dir)

    assert result.returncode == 1, result.stdout + result.stderr
    assert git(repo, "status", "--porcelain") == ""
