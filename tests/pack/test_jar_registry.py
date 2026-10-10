#!/usr/bin/env python3
"""Assert `data/jars/` still matches what the installed jars register."""
import subprocess
import sys
import unittest
from pathlib import Path

EXTRACTOR = Path(__file__).resolve().parents[2] / "scripts/jar-registry-extract.py"


class JarRegistry(unittest.TestCase):
    def test_the_extract_is_current(self):
        result = subprocess.run([sys.executable, str(EXTRACTOR), "--check"],
                                capture_output=True, text=True)
        self.assertEqual(0, result.returncode, (result.stdout + result.stderr).strip())


if __name__ == "__main__":
    unittest.main()
