#!/usr/bin/env python3
"""Emit EMI's index as the Obtainable allowlist, and each block drop's source (#453, #454).

Obtainable is derived from every output of every recipe the pack emits, every item the starting kit
grants, `data/pack/mechanic-obtainable.json`, the hand-kept rows for what a mechanic produces with
no recipe, and the drops of every block the live worldgen places. `data/pack/creative-listed.json`
adds the Pack's creative test items to the index only; they are not Obtainable (ADR-0105). No mob spawns, so none drops (ADR-0093). Reads only
committed files: the pack's own data and the jar corpus under `data/jars/`.

The worldgen walk starts at each live dimension (`kubejs/data/`, never `kubejs/parked/`): its noise
settings' default block and fluid and every state its surface rule places, then each biome's
features, followed from placed to configured feature and on through the features those name, and
the palettes of the templates the live template pools place, which are the starting area's. A
feature type that places blocks its config does not name is in `IMPLICIT`; a walked type in neither
table fails the run, since the walk cannot see what it places.

Drops resolve to a fixpoint, since a `match_tool` condition passes only when an Obtainable item
satisfies it, and a drop can be that item. A tool predicate other than an item list, such as silk
touch, is satisfied by nothing, since the pack has no enchanting: a grass block drops dirt and not
itself. The conditions in `EITHER_WAY` depend on chance or the world, not on what is held. A pack
loot table under `kubejs/data/` replaces the jar's, which is how a placed plant drops nothing
(ADR-0092). EMI's Where it is found reads `SOURCES` (ADR-0091).

EMI reads index stacks only under the `emi` namespace, and applies a file's `filters` before its
`added`, so a filter matching every id empties the index and `added` refills it. `disable` would
also hide every recipe naming a filtered stack, so it is left off. An `added` entry that is a bare
string is skipped, so each is a `{"stack": ...}` object.

Usage:

    scripts/build-obtainable-index.py            # writes the index
    scripts/build-obtainable-index.py --check    # asserts it is already up to date; no writes
"""
import argparse
import functools
import importlib.util
import json
import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import nbt  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
RECIPES = ROOT / "kubejs/data/factoryworks/recipe"
# The kit the deleted Core mod granted, as of fb05f50, until the Showcase moves out (#663, ADR-0128).
KIT = ("factoryworks:stone_furnace", "factoryworks:burner_mining_drill",
       "factoryworks:engineers_iron_pick", "factoryworks:iron_plate", "factoryworks:copper_plate",
       "minecraft:coal")
MECHANICS = ROOT / "data/pack/mechanic-obtainable.json"
CREATIVE = ROOT / "data/pack/creative-listed.json"
INDEX = ROOT / "kubejs/assets/emi/index/stacks/obtainable.json"
SOURCES = ROOT / "kubejs/assets/factoryworks/obtainable/sources.json"
LIVE = ROOT / "kubejs/data"
JARS = ROOT / "data/jars"

DATA_DRIVEN = {
    "minecraft:alter_ground", "minecraft:attached_to_leaves", "minecraft:attached_to_logs",
    "minecraft:block_column",
    "minecraft:fallen_tree", "minecraft:place_on_ground", "minecraft:random_selector",
    "minecraft:simple_block", "minecraft:simple_random_selector", "minecraft:tree",
}
IMPLICIT = {
    "minecraft:beehive": ["minecraft:bee_nest"],
    "minecraft:trunk_vine": ["minecraft:vine"],
    "minecraft:kelp": ["minecraft:kelp", "minecraft:kelp_plant"],
    "minecraft:seagrass": ["minecraft:seagrass", "minecraft:tall_seagrass"],
}
EITHER_WAY = {
    "minecraft:block_state_property", "minecraft:entity_properties", "minecraft:location_check",
    "minecraft:random_chance", "minecraft:random_chance_with_enchanted_bonus",
    "minecraft:survives_explosion", "minecraft:table_bonus",
}


def recipe_outputs():
    outputs = set()
    for path in sorted(RECIPES.rglob("*.json")):
        recipe = json.loads(path.read_text(encoding="utf-8"))
        fluids = recipe.get("fluid_results", [])
        results = recipe.get("results") or ([] if fluids else [recipe.get("result")])
        if not all(results):
            sys.exit(f"{path.relative_to(ROOT)} has no `results`, `result` or `fluid_results`")
        outputs |= {stack_key(result) for result in results}
        outputs |= {"fluid:" + fluid["id"] for fluid in fluids}
    return outputs


def stack_key(result):
    """A result's stack as a string, its components appended as JSON when it carries any: an item told
    apart by a component (ADR-0052) is hidden by EMI unless listed."""
    components = result.get("components")
    suffix = json.dumps(components, sort_keys=True, separators=(",", ":")) if components else ""
    return "item:" + result["id"] + suffix


def emi_stack(key):
    """The stack as EMI's index reads it: a string, or an object with `componentChanges`."""
    if "{" not in key:
        return key
    item, brace, components = key.partition("{")
    return {"type": "item", "id": item.removeprefix("item:"), "componentChanges": json.loads(brace + components)}


def kit_items():
    return {"item:" + item for item in KIT}


def mechanic_rows():
    return json.loads(MECHANICS.read_text(encoding="utf-8"))["rows"]


def creative_rows():
    return json.loads(CREATIVE.read_text(encoding="utf-8"))["rows"]


@functools.cache
def extractor():
    spec = importlib.util.spec_from_file_location(
        "jar_registry_extract", ROOT / "scripts/jar-registry-extract.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def resource(roots, folder, name, suffix=".json"):
    namespace, path = name.split(":", 1)
    for root in roots:
        if (found := root / namespace / folder / (path + suffix)).is_file():
            return found
    return None


def values_at(value, keys):
    """Every string held under one of `keys`, at any depth."""
    if isinstance(value, dict):
        found = {value[key] for key in keys if isinstance(value.get(key), str)}
        return found.union(*(values_at(inner, keys) for inner in value.values()))
    if isinstance(value, list):
        return set().union(*(values_at(inner, keys) for inner in value))
    return set()


class Worldgen:
    """The blocks the worldgen under `roots[0]` places, reading any file it names from `roots`."""

    def __init__(self, roots):
        self.roots = roots
        self.corpus = read(JARS / "feature.json")
        self.reduce = extractor().reduce_feature
        self.placed_ids = set(self.corpus["placed"]) | {
            f"{path.parts[-4]}:{path.stem}" for root in roots
            for path in root.glob("*/worldgen/placed_feature/*.json")}
        self.blocks, self.unresolved, self.unknown_types = set(), set(), set()
        self.seen = set()

    def feature(self, kind, name):
        if (kind, name) in self.seen:
            return
        self.seen.add((kind, name))
        if path := resource(self.roots, f"worldgen/{kind}_feature", name):
            reduced = self.reduce(kind, read(path), self.placed_ids)
        elif name in self.corpus[kind]:
            reduced = self.corpus[kind][name]
        else:
            self.unresolved.add(f"{kind} feature {name}")
            return
        self.blocks |= set(reduced.get("blocks", []))
        for kind_type in reduced.get("types", []):
            if kind_type in IMPLICIT:
                self.blocks |= set(IMPLICIT[kind_type])
            elif kind_type not in DATA_DRIVEN:
                self.unknown_types.add(kind_type)
        for inner in reduced.get("configured", []):
            self.feature("configured", inner)
        for inner in reduced.get("placed", []):
            self.feature("placed", inner)

    def walk(self):
        for dimension in sorted(self.roots[0].glob("*/dimension/*.json")):
            generator = read(dimension)["generator"]
            settings = generator.get("settings")
            if isinstance(settings, str):
                if path := resource(self.roots, "worldgen/noise_settings", settings):
                    settings = read(path)
                else:
                    self.unresolved.add(f"noise settings {settings}")
            if isinstance(settings, dict):
                self.blocks |= values_at([settings.get("default_block"),
                                          settings.get("default_fluid"),
                                          settings.get("surface_rule")], ("Name",))
            for biome in sorted(values_at(generator.get("biome_source"), ("biome", "biomes"))):
                path = resource(self.roots, "worldgen/biome", biome)
                if not path:
                    self.unresolved.add(f"biome {biome}")
                    continue
                for step in read(path).get("features", []):
                    for placed in step:
                        self.feature("placed", placed)
        for pool in sorted(self.roots[0].glob("*/worldgen/template_pool/*.json")):
            for element in read(pool).get("elements", []):
                location = element["element"].get("location")
                path = location and resource(self.roots, "structure", location, ".nbt")
                if not path:
                    self.unresolved.add(f"template {location}")
                    continue
                template = nbt.read(path)
                palettes = template.get("palettes") or [template.get("palette", [])]
                self.blocks |= {state["Name"] for palette in palettes for state in palette}
        return self


def placed_blocks():
    worldgen = Worldgen([LIVE]).walk()
    if worldgen.unresolved:
        sys.exit("the live worldgen names what nothing defines: "
                 + ", ".join(sorted(worldgen.unresolved)))
    if worldgen.unknown_types:
        sys.exit("decide whether these feature types place blocks their config does not name, "
                 "and add each to DATA_DRIVEN or IMPLICIT: "
                 + ", ".join(sorted(worldgen.unknown_types)))
    return worldgen.blocks


def loot_tables():
    tables = read(JARS / "loot.json")["tables"]
    reduce = extractor().reduce_loot
    for path in sorted(LIVE.glob("*/loot_table/blocks/**/*.json")):
        namespace = path.relative_to(LIVE).parts[0]
        table = path.relative_to(LIVE / namespace / "loot_table").with_suffix("").as_posix()
        tables[f"{namespace}:{table}"] = reduce(read(path))
    return tables


def condition_outcomes(condition, held):
    """Whether `condition` can pass, and whether it can fail, for a player holding only `held`."""
    kind = condition["condition"]
    if kind == "minecraft:match_tool":
        predicate = dict(condition.get("predicate", {}))
        items = predicate.pop("items", None)
        if predicate:
            return False, True
        if items is None:
            return True, True
        items = [items] if isinstance(items, str) else items
        if any(item.startswith("#") for item in items):
            sys.exit(f"match_tool names an item tag, {items} -- the corpus holds no item tags")
        return any("item:" + item in held for item in items), True
    if kind == "minecraft:inverted":
        passes, fails = condition_outcomes(condition["term"], held)
        return fails, passes
    if kind in ("minecraft:any_of", "minecraft:all_of"):
        outcomes = [condition_outcomes(term, held) for term in condition["terms"]]
        passes, fails = [o[0] for o in outcomes], [o[1] for o in outcomes]
        if kind == "minecraft:any_of":
            return any(passes), all(fails)
        return all(passes), any(fails)
    if kind in EITHER_WAY:
        return True, True
    sys.exit(f"no rule for loot condition {kind} -- decide whether it can pass, and add it")


def entry_outcomes(entry, held):
    """The items `entry` can yield, whether it can succeed, and whether it can fail."""
    passes, fails = condition_outcomes(
        {"condition": "minecraft:all_of", "terms": entry.get("conditions", [])}, held)
    if not passes:
        return set(), False, True
    kind, children = entry["type"], entry.get("children", [])
    if kind == "minecraft:item":
        return {"item:" + entry["name"]}, True, fails
    if kind in ("minecraft:empty", "minecraft:dynamic"):
        return set(), True, fails
    outcomes = []
    for child in children:
        outcomes.append(entry_outcomes(child, held))
        if kind == "minecraft:alternatives" and not outcomes[-1][2]:
            break
        if kind == "minecraft:sequence" and not outcomes[-1][1]:
            break
    items = set().union(*(o[0] for o in outcomes))
    if kind == "minecraft:alternatives":
        return items, any(o[1] for o in outcomes), fails or all(o[2] for o in outcomes)
    if kind == "minecraft:sequence":
        return items, all(o[1] for o in outcomes), fails or any(o[2] for o in outcomes)
    if kind == "minecraft:group":
        return items, True, fails
    sys.exit(f"no rule for loot entry {kind} -- decide what it yields, and add it")


def table_drops(pools, held):
    drops = set()
    for pool in pools:
        if condition_outcomes({"condition": "minecraft:all_of",
                               "terms": pool.get("conditions", [])}, held)[0]:
            for entry in pool["entries"]:
                drops |= entry_outcomes(entry, held)[0]
    return drops


def block_drops(held):
    """Each item the placed blocks drop, with its sources, at the fixpoint over `held`."""
    tables, blocks = loot_tables(), sorted(placed_blocks())
    while True:
        sources = defaultdict(list)
        for block in blocks:
            namespace, path = block.split(":", 1)
            table = f"{namespace}:blocks/{path}"
            for item in sorted(table_drops(tables.get(table, []), held)):
                sources[item].append({"broken": block, "loot_table": table})
        if set(sources) <= held:
            return dict(sources)
        held = held | set(sources)


def block_drop_sources():
    return dict(sorted(block_drops(
        recipe_outputs() | kit_items() | {row["id"] for row in mechanic_rows()}).items()))


def derived():
    return recipe_outputs() | kit_items() | set(block_drop_sources())


def index(drops):
    stacks = recipe_outputs() | kit_items() | set(drops) | {row["id"] for row in mechanic_rows()}
    stacks |= {"item:" + row["id"] for row in creative_rows()}
    ordered = sorted(stacks, key=lambda stack: (not stack.startswith("item:"), stack))
    return {"filters": ["/.*/"], "added": [{"stack": emi_stack(stack)} for stack in ordered]}


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    drops = block_drop_sources()
    data = index(drops)
    files = {INDEX: json.dumps(data, indent=2) + "\n",
             SOURCES: json.dumps({"stacks": drops}, indent=2) + "\n"}
    if args.check:
        stale = [str(path.relative_to(ROOT)) for path, text in files.items()
                 if not path.is_file() or path.read_text(encoding="utf-8") != text]
        if stale:
            sys.exit(f"stale: {', '.join(stale)} -- run scripts/build-obtainable-index.py")
        print(f"OK -- {len(data['added'])} stacks, {len(drops)} block drops")
        return
    for path, text in files.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
    print(f"wrote {INDEX.relative_to(ROOT)}: {len(data['added'])} stacks")
    print(f"wrote {SOURCES.relative_to(ROOT)}: {len(drops)} block drops")


if __name__ == "__main__":
    main()
