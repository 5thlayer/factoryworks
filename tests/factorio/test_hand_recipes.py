#!/usr/bin/env python3
"""Assert each Personal Assembler hand copy matches its machine recipe (#291, ADR-0089).

`scripts/build-hand-recipes.py` writes a `craftworks:assembling` copy of every `crafting` recipe
under `recipe/assembling/` into `recipe/hand/`. Its `--check` proves the copies are what it would
write. This check also compares each copy to its machine recipe independently, so a change to the
generator can't pass by being consistent with itself. It also asserts that research unlocks reach
the copies: Craftworks asks Researchd about the copy's id, and without `withHandCopies` in the DSL
every hand recipe stays unlocked.

Usage: tests/factorio/test_hand_recipes.py
"""
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
GENERATOR = ROOT / "scripts" / "build-hand-recipes.py"
MACHINE = ROOT / "kubejs/data/factoryworks/recipe/assembling"
HAND = ROOT / "kubejs/data/factoryworks/recipe/hand"
DSL = ROOT / "kubejs/server_scripts/factorio_tech_dsl.js"


def main():
    failures = []
    generated = subprocess.run([sys.executable, str(GENERATOR), "--check"], capture_output=True, text=True)
    if generated.returncode != 0:
        failures.append(f"{GENERATOR.relative_to(ROOT)} --check: {(generated.stderr or generated.stdout).strip()}")

    hand_set = {p.relative_to(MACHINE).with_suffix("").as_posix(): json.loads(p.read_text(encoding="utf-8"))
                for p in MACHINE.rglob("*.json")}
    hand_set = {stem: r for stem, r in hand_set.items() if r.get("category") == "crafting"}
    copies = {p.relative_to(HAND).with_suffix("").as_posix(): json.loads(p.read_text(encoding="utf-8"))
              for p in HAND.rglob("*.json")}

    for stem in sorted(set(hand_set) - set(copies)):
        failures.append(f"assembling/{stem} is category crafting and has no hand copy")
    for stem in sorted(set(copies) - set(hand_set)):
        failures.append(f"hand/{stem} copies no crafting recipe")
    for stem in sorted(set(hand_set) & set(copies)):
        machine, copy = hand_set[stem], copies[stem]
        if copy.get("type") != "craftworks:assembling":
            failures.append(f"hand/{stem} is {copy.get('type')}, not craftworks:assembling")
        if copy.get("ingredients") != machine["ingredients"]:
            failures.append(f"hand/{stem}'s ingredients differ from its machine recipe's")
        if copy.get("results") != machine["results"]:
            failures.append(f"hand/{stem}'s results {copy.get('results')} are not {machine['results']}")
        if copy.get("time") != machine["time"]:
            failures.append(f"hand/{stem} takes {copy.get('time')} ticks, its machine recipe {machine['time']}")

    if "unlockRecipes(withHandCopies(" not in DSL.read_text(encoding="utf-8"):
        failures.append(f"{DSL.name} does not pass unlocks through withHandCopies, so no research locks a hand recipe")

    for failure in failures:
        print(f"FAIL: {failure}")
    if failures:
        sys.exit(1)
    print(f"ok: {len(copies)} hand copies match their machine recipes")


if __name__ == "__main__":
    main()
