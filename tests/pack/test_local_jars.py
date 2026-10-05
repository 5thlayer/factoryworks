#!/usr/bin/env python3
"""Assert `mods/` holds the local jars `data/pack/local-jars.json` pins (#465, ADR-0024).

Runs `scripts/sync-local-jars.py --check`: each pinned jar is the one in `mods/`, byte for byte the
one `~/.m2` published when `~/.m2` holds it, and nests what its row says it nests. A row with a
`curseforge` project id has a metafile naming the pinned file, indexed in place of the jar (#532),
or none while its CurseForge reference is pending.

Usage: tests/pack/test_local_jars.py
"""
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
SYNC = ROOT / "scripts" / "sync-local-jars.py"


def main():
    result = subprocess.run([sys.executable, str(SYNC), "--check"], capture_output=True, text=True)
    print(result.stdout, end="")
    if result.returncode != 0:
        print(f"FAIL: {SYNC.relative_to(ROOT)} --check\n{result.stderr.strip()}")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
