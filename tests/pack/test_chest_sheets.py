#!/usr/bin/env python3
"""Assert the Iron and Steel Chests' sheets are still what their generator derives from their sources (#543).

A sheet edited in place, or a source that changed under it, fails here rather than surviving until
the next regeneration reverts it. The generator needs Pillow, so it runs through `uv`.

Usage: tests/pack/test_chest_sheets.py
"""
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent


def main():
    run = subprocess.run(
        ["uv", "run", "--with", "pillow", str(ROOT / "scripts/build-chest-sheets.py"), "--check"],
        capture_output=True, text=True)
    print((run.stdout or run.stderr).strip())
    return run.returncode


if __name__ == "__main__":
    sys.exit(main())
