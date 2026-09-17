"""Makes `pytest tests/` run every check in this tree, whichever style it is written in.

Most checks here are `main()`-style scripts guarded by `if __name__ == "__main__"`: they print a
line and exit non-zero on failure. pytest collects nothing from them, so before this file
`pytest tests/` reported green while 25 of 36 files never executed (#171) -- a check that goes
green while the thing it checks is broken, on the gesture an agent reaches for first.

Two halves:

* `pytest_collect_file` wraps each script-style file as a single test that runs it and asserts it
  exited 0. Its stdout and stderr are the failure message, so a failing check reads the same way
  it does when run by hand.
* `pytest_collection_modifyitems` is the guard the ticket asks for by name: every `test_*.py` in
  this tree must yield at least one collected item. A new check that is neither pytest-style nor a
  runnable script fails the run rather than being silently skipped, which is the failure mode this
  whole file exists to end.

A file counts as pytest-style if it defines `def test_`; anything else is run as a script.
"""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

import pytest

ROOT = Path(__file__).resolve().parent.parent
TESTS = Path(__file__).resolve().parent


def _is_script_style(path: Path) -> bool:
    """True when pytest would collect nothing from this file."""
    try:
        return "def test_" not in path.read_text(encoding="utf-8")
    except OSError:
        return False


class ScriptCheckFailed(Exception):
    """A script-style check exited non-zero."""


class ScriptCheckItem(pytest.Item):
    """One `main()`-style check, run as a subprocess."""

    def runtest(self) -> None:
        result = subprocess.run(
            [sys.executable, str(self.path)],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        if result.returncode != 0:
            raise ScriptCheckFailed(
                f"{self.path.relative_to(ROOT)} exited {result.returncode}\n\n"
                f"{result.stdout}{result.stderr}"
            )

    def repr_failure(self, excinfo, style=None):  # noqa: ANN001 - pytest's signature
        if isinstance(excinfo.value, ScriptCheckFailed):
            return str(excinfo.value)
        return super().repr_failure(excinfo, style)

    def reportinfo(self):
        return self.path, 0, f"script check: {self.path.relative_to(ROOT)}"


class NotACheckItem(pytest.Item):
    """A `test_*.py` that is neither style: it would pass by doing nothing."""

    def runtest(self) -> None:
        raise ScriptCheckFailed(
            f"{self.path.relative_to(ROOT)} defines no `def test_` and has no "
            '`if __name__ == "__main__"` block, so running it asserts nothing and would pass '
            "vacuously (#171). Give it either style."
        )

    def repr_failure(self, excinfo, style=None):  # noqa: ANN001 - pytest's signature
        return str(excinfo.value)

    def reportinfo(self):
        return self.path, 0, f"not a check: {self.path.relative_to(ROOT)}"


class ScriptCheckFile(pytest.File):
    def collect(self):
        source = self.path.read_text(encoding="utf-8")
        if "__main__" not in source:
            yield NotACheckItem.from_parent(self, name=self.path.stem)
        else:
            yield ScriptCheckItem.from_parent(self, name=self.path.stem)


def pytest_collect_file(parent, file_path: Path):
    if (
        file_path.suffix == ".py"
        and file_path.name.startswith("test_")
        and _is_script_style(file_path)
    ):
        return ScriptCheckFile.from_parent(parent, path=file_path)
    return None


def pytest_collection_modifyitems(session, config, items) -> None:
    """Every check file must have produced at least one item (#171)."""
    if session.config.option.keyword or session.config.option.markexpr:
        return  # a filtered run is deliberately partial

    args = [Path(a).resolve() for a in session.config.args]
    if not any(a == TESTS or a in TESTS.parents for a in args):
        return  # not a whole-tree run; nothing to be complete about

    on_disk = {p.resolve() for p in TESTS.rglob("test_*.py")}
    collected = {Path(str(item.path)).resolve() for item in items}
    missing = sorted(p.relative_to(ROOT) for p in on_disk - collected)
    if missing:
        raise pytest.UsageError(
            "these check files produced no tests and would have been silently skipped "
            "(#171):\n  " + "\n  ".join(str(p) for p in missing)
        )
