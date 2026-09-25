#!/usr/bin/env python3
"""Re-author the admitted stock recipes as pack recipes on the Assembling Machine (#442, ADR-0034).

Reads each recipe `data/pack/stock-admissions.json` admits out of the installed jar that ships it
(a mod jar, or the client jar for vanilla) and writes it under
`kubejs/data/planetaryfactory/recipe/assembling/stock/`. Nothing is decided here: which recipes are
kept is the admissions file, and what each ingredient becomes is `data/pack/stock-substitutions.json`.
An admission with a `rewrite` takes its ingredients and yield from that row instead of the jar's
(#444); only its output is read from the jar, and each ingredient must be a `keep` row.

Usage: scripts/stock-recipe-convert.py [--check] [--quiet]
  --check  write nothing; fail if the files on disk differ from what would be written
"""
import argparse
import json
import os
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MODS = ROOT / "mods"
CLIENT_JAR = Path(os.environ.get("PF_CLIENT_JAR", os.path.expanduser(
    "~/curseforge/Install/versions/26.1.2/26.1.2.jar")))
ADMISSIONS = ROOT / "data/pack/stock-admissions.json"
SUBSTITUTIONS = ROOT / "data/pack/stock-substitutions.json"
# `factorio-recipe-convert.py` lists this subtree in FOREIGN_SUBTREES and leaves it alone.
OUT_DIR = ROOT / "kubejs/data/planetaryfactory/recipe/assembling/stock"

CATEGORY_OF_SOURCE = {
    "minecraft:crafting_shaped": "crafting",
    "minecraft:crafting_shapeless": "crafting",
}


def installed_jars():
    return sorted(MODS.glob("*.jar")) + ([CLIENT_JAR] if CLIENT_JAR.is_file() else [])


def read_stock(recipe_ids, problems):
    """Each admitted id's recipe JSON, from the one installed jar that ships it."""
    wanted = {}
    for recipe_id in recipe_ids:
        namespace, path = recipe_id.split(":", 1)
        wanted["data/%s/recipe/%s.json" % (namespace, path)] = recipe_id
    found = {}
    for jar in installed_jars():
        with zipfile.ZipFile(jar) as archive:
            for entry in wanted.keys() & set(archive.namelist()):
                found.setdefault(wanted[entry], []).append((jar.name, json.loads(archive.read(entry))))
    recipes = {}
    for recipe_id in sorted(recipe_ids):
        sources = found.get(recipe_id, [])
        if len(sources) != 1:
            problems.append("%s is shipped by %s installed jars (%s); the line reads it from exactly one"
                            % (recipe_id, len(sources), ", ".join(j for j, _ in sources) or "none"))
            continue
        recipes[recipe_id] = sources[0][1]
    return recipes


def flatten(recipe_id, recipe, problems):
    """(ingredient, count) pairs in first-appearance order, counts preserved."""
    if recipe["type"] == "minecraft:crafting_shaped":
        cells = [recipe["key"][c] for row in recipe["pattern"] for c in row if c != " "]
    else:
        cells = recipe["ingredients"]
    counts = {}
    for cell in cells:
        if not isinstance(cell, str):
            problems.append("%s names %r, an ingredient that is not one item or one tag; give it a "
                            "row the line can read" % (recipe_id, cell))
            continue
        counts[cell] = counts.get(cell, 0) + 1
    return list(counts.items())


def convert(recipes, admit, subs, problems):
    """The re-authored recipes, keyed by the file stem each is written to, and every source
    ingredient they read."""
    keep, substitute = subs["keep"], subs["substitute"]
    emitted, used = {}, set()
    for recipe_id, recipe in sorted(recipes.items()):
        category = CATEGORY_OF_SOURCE.get(recipe["type"])
        if category is None:
            problems.append("%s is a %s, which the line does not re-author" % (recipe_id, recipe["type"]))
            continue
        rewrite = admit[recipe_id].get("rewrite")
        if rewrite:
            sources, yields = rewrite["ingredients"].items(), rewrite["count"]
        else:
            sources, yields = flatten(recipe_id, recipe, problems), recipe["result"].get("count", 1)
        merged = {}
        for source, count in sources:
            used.add(source)
            if source in keep:
                target = source
            elif source in substitute and not rewrite:
                target = substitute[source]["to"]
            else:
                problems.append("%s takes `%s`, which stock-substitutions.json %s" % (
                    recipe_id, source, "does not keep" if rewrite else "neither keeps nor substitutes"))
                continue
            merged[target] = merged.get(target, 0) + count
        stem = recipe_id.split(":", 1)[1]
        if stem in emitted:
            problems.append("two admitted recipes are both named %s" % stem)
        emitted[stem] = {
            "type": "planetaryfactory:assembling",
            "category": category,
            "ingredients": [{"ingredient": item, "count": count} for item, count in merged.items()],
            "results": [{"id": recipe["result"]["id"], "count": yields}],
            "time": admit[recipe_id]["time"],
        }
    return emitted, used


def unused_rows(used, subs, problems):
    for table in ("keep", "substitute"):
        for row in sorted(set(subs[table]) - used):
            problems.append("stock-substitutions.json `%s` names `%s`, which no admitted recipe takes"
                            % (table, row))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--quiet", action="store_true")
    args = parser.parse_args()

    admit = json.loads(ADMISSIONS.read_text())["admit"]
    subs = json.loads(SUBSTITUTIONS.read_text())

    problems = []
    for recipe_id, row in sorted(admit.items()):
        if not row.get("reason") or not isinstance(row.get("time"), int) or row["time"] <= 0:
            problems.append("stock-admissions.json row %s needs a `reason` and a positive `time`"
                            % recipe_id)
        rewrite = row.get("rewrite")
        if rewrite is not None and not (
                isinstance(rewrite, dict) and rewrite.get("reason") and isinstance(rewrite.get("count"), int) and rewrite["count"] > 0
                and rewrite.get("ingredients")
                and all(isinstance(n, int) and n > 0 for n in rewrite["ingredients"].values())):
            problems.append("stock-admissions.json row %s's `rewrite` needs `ingredients` with "
                            "positive counts, a positive `count` and a `reason`" % recipe_id)
    for source, row in sorted(subs["substitute"].items()):
        if not row.get("to") or not row.get("reason"):
            problems.append("stock-substitutions.json `substitute` row %s needs `to` and `reason`"
                            % source)
    for source, reason in sorted(subs["keep"].items()):
        if not reason:
            problems.append("stock-substitutions.json `keep` row %s needs a reason" % source)
    if problems:
        for problem in problems:
            print("FAIL: " + problem)
        return 1
    recipes = read_stock(admit, problems)
    emitted, used = convert(recipes, admit, subs, problems)
    if len(recipes) == len(admit):
        unused_rows(used, subs, problems)

    if problems:
        for problem in problems:
            print("FAIL: " + problem)
        return 1

    emitted = {stem: json.dumps(body, indent=2) + "\n" for stem, body in emitted.items()}
    on_disk = {p.stem: p.read_text() for p in OUT_DIR.glob("*.json")} if OUT_DIR.exists() else {}
    if args.check:
        if on_disk != emitted:
            print("FAIL: assembling/stock/ is stale -- re-run scripts/stock-recipe-convert.py")
            for label, names in (("missing", set(emitted) - set(on_disk)),
                                 ("unexpected", set(on_disk) - set(emitted)),
                                 ("changed", {k for k in set(emitted) & set(on_disk)
                                              if emitted[k] != on_disk[k]})):
                if names:
                    print("  %s: %s" % (label, ", ".join(sorted(names))))
            return 1
    else:
        for stale in set(on_disk) - set(emitted):
            (OUT_DIR / (stale + ".json")).unlink()
        OUT_DIR.mkdir(parents=True, exist_ok=True)
        for stem, body in sorted(emitted.items()):
            (OUT_DIR / (stem + ".json")).write_text(body)

    if not args.quiet:
        print("ok: %d stock recipe(s) re-authored" % len(emitted))
    return 0


if __name__ == "__main__":
    sys.exit(main())
