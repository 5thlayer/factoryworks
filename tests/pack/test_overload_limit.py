#!/usr/bin/env python3
"""Assert the Overload Limit resource the furnaces read is the corpus's (#517).

`scripts/build-overload-limit.py --check` proves the resource is what the generator would write.

Usage: tests/pack/test_overload_limit.py
"""
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
GENERATOR = ROOT / "scripts" / "build-overload-limit.py"


def main():
    generated = subprocess.run([sys.executable, str(GENERATOR), "--check"], capture_output=True, text=True)
    if generated.returncode != 0:
        print(f"FAIL {GENERATOR.relative_to(ROOT)} --check: {(generated.stderr or generated.stdout).strip()}")
        return 1
    print("ok   the Overload Limit resource is the corpus's")
    return 0


if __name__ == "__main__":
    sys.exit(main())
