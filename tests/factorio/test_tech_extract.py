#!/usr/bin/env python3
"""Check that the extracted Factorio tech tree is still a tree, without launching anything.

The extractor drops 106 of Factorio's 268 technologies -- the infinite ones, the ones
costed by a formula, and the levelled upgrade chains (ADR-0022). Dropping a node whose
children survive would orphan them, so prerequisites are re-pointed through every dropped
node to its nearest surviving ancestors. That walk is the one piece of real logic in the
extractor, and its failure mode is silent: a tree that loads fine and simply omits half
the pack, or one with a research nobody can ever reach.

So the claim under check is narrow and structural: *the pruned tree is still a valid tree*.

What this deliberately does not check is the pack's content: whether an icon is the right
item, whether an unlocked recipe id resolves, whether the costs are fun. That is authoring,
and it is the launch test's job. The one content claim held here is the ladder's: each gate
`docs/spec/terra-progression.md` names is in the tree at the cost it claims (ADR-0097).
"""

import importlib.util
import json
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent.parent
DATA = REPO / "data" / "factorio"
FAILURES = []


def check(condition, message):
    if not condition:
        FAILURES.append(message)


def load_extractor():
    """Import the extractor by path -- its filename is hyphenated, so it is not importable."""
    path = REPO / "scripts" / "factorio-tech-extract.py"
    spec = importlib.util.spec_from_file_location("factorio_tech_extract", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def check_pruning_rules(x):
    """The three families the extractor drops, and the one it must not."""
    check(x.is_pruned({"max_level": "infinite"}) == "infinite", "infinite tech not pruned")
    check(
        x.is_pruned({"unit": {"count_formula": "1000*(L-2)"}}) == "count_formula",
        "count_formula tech not pruned",
    )
    check(x.is_pruned({"upgrade": True}) == "upgrade", "upgrade tech not pruned")
    check(
        x.is_pruned({"unit": {"count": 50, "time": 5}}) is None,
        "an ordinary tech was pruned",
    )
    check(
        x.is_pruned({"name": "automation-2", "unit": {"count": 50}}) is None,
        "a tech was pruned for its name rather than its flags",
    )


def check_reparenting(x):
    """Dropped nodes are transparent: children re-point through them, not into a void."""
    raw = {
        "root": {"prerequisites": []},
        "gone": {"prerequisites": ["root"], "upgrade": True},
        "child": {"prerequisites": ["gone"]},
    }
    keep = {"root", "child"}
    check(
        x.surviving_parents("child", raw, keep) == ["root"],
        "a child of a dropped tech was not re-pointed to its grandparent",
    )

    # Two dropped nodes deep, and a diamond: the same ancestor reached twice is emitted once.
    raw = {
        "root": {"prerequisites": []},
        "mid-a": {"prerequisites": ["root"], "upgrade": True},
        "mid-b": {"prerequisites": ["root"], "upgrade": True},
        "child": {"prerequisites": ["mid-a", "mid-b"]},
    }
    check(
        x.surviving_parents("child", raw, {"root", "child"}) == ["root"],
        "a diamond through dropped nodes produced a duplicate parent",
    )

    # Malformed data must not hang the extractor.
    raw = {"a": {"prerequisites": ["b"]}, "b": {"prerequisites": ["a"]}}
    x.surviving_parents("a", raw, set())


def check_effect_collapsing(x):
    """Generated reverse-crafts collapse to their rule; hand-authored ones do not."""
    derivable = {"iron-plate"}
    effects = [
        {"type": "unlock-recipe", "recipe": "recycler"},
        {"type": "unlock-recipe", "recipe": "iron-plate-recycling"},
        {"type": "unlock-recipe", "recipe": "raw-fish-recycling", "hidden": True},
        {"type": "unlock-recipe", "recipe": "bespoke-recycling"},
    ]
    out = x.collapse_effects(effects, derivable)
    families = [e for e in out if e["type"] == "unlock-recipe-family"]
    check(len(families) == 1 and families[0]["recipes"] == 2, "reverse-crafts did not collapse")
    kept = {e.get("recipe") for e in out if e["type"] == "unlock-recipe"}
    check(kept == {"recycler", "bespoke-recycling"}, f"wrong effects survived collapsing: {kept}")


def check_tree(techs):
    """The committed tree: connected, acyclic, no dangling or self-referential parents."""
    by_name = {t["name"]: t for t in techs}
    check(len(by_name) == len(techs), "duplicate technology names in technology.json")

    for tech in techs:
        for field in ("name", "suggested_id", "source", "cost_kind"):
            check(tech.get(field), f"{tech['name']}: missing {field}")
        check(
            tech["suggested_id"] == "factory_works:" + tech["name"].replace("-", "_"),
            f"{tech['name']}: suggested_id does not follow from the name",
        )
        check(
            tech["cost_kind"] in ("packs", "trigger"),
            f"{tech['name']}: unknown cost_kind {tech['cost_kind']}",
        )
        if tech["cost_kind"] == "packs":
            check(tech.get("unit"), f"{tech['name']}: pack-costed but has no unit")
        else:
            check(
                tech.get("research_trigger"),
                f"{tech['name']}: trigger-costed but has no research_trigger",
            )
        check(
            tech["name"] not in tech["prerequisites"],
            f"{tech['name']}: is its own prerequisite",
        )
        for parent in tech["prerequisites"]:
            check(
                parent in by_name,
                f"{tech['name']}: prerequisite '{parent}' was dropped without re-pointing",
            )

    # Every technology must be reachable from a root, or nothing can ever research it.
    roots = [t["name"] for t in techs if not t["prerequisites"]]
    check(roots, "the tree has no root -- every technology has a prerequisite")
    children = {}
    for tech in techs:
        for parent in tech["prerequisites"]:
            children.setdefault(parent, []).append(tech["name"])
    seen, stack = set(roots), list(roots)
    while stack:
        for child in children.get(stack.pop(), []):
            if child not in seen:
                seen.add(child)
                stack.append(child)
    unreachable = sorted(set(by_name) - seen)
    check(not unreachable, f"unreachable from any root (a cycle): {unreachable[:5]}")


def check_no_pruned_families(techs):
    """Nothing the extractor claims to drop survived into the committed file."""
    for tech in techs:
        unit = tech.get("unit") or {}
        check("count_formula" not in unit, f"{tech['name']}: count_formula survived")
        check(
            not re.search(r"^(mining-productivity|worker-robot-speed|braking-force)-\d+$", tech["name"]),
            f"{tech['name']}: an upgrade chain survived",
        )


def ancestors(name, by_name):
    seen, stack = set(), [name]
    while stack:
        for parent in by_name[stack.pop()]["prerequisites"]:
            if parent not in seen:
                seen.add(parent)
                stack.append(parent)
    return seen


def pack_set(tech):
    return {i[0] for i in (tech.get("unit") or {}).get("ingredients", [])}


def check_ladder(techs):
    """Every gate Terra's arc names is in the corpus at the cost the arc claims (ADR-0097).

    The gate table is read out of the spec rather than restated, so a gate the spec adds is
    held to the corpus with no edit here.
    """
    by_name = {t["name"]: t for t in techs}
    spec = REPO / "docs" / "spec" / "terra-progression.md"
    text = spec.read_text(encoding="utf-8")
    match = re.search(r"^## The gates\n(.*?)(?=^## |^---)", text, re.S | re.M)
    check(match, "terra-progression.md has no `## The gates` section")
    if not match:
        return

    gates = {}
    rows = [line for line in match.group(1).splitlines() if line.startswith("|")][2:]
    for line in rows:
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if cells[0] == "—":
            continue
        gate = re.fullmatch(r"`([a-z0-9-]+)`", cells[0]) if len(cells) >= 3 else None
        check(gate, f"a gate row names no technology: {line}")
        if not gate:
            continue
        check(gate.group(1) not in gates, f"the gate table names `{gate.group(1)}` twice")
        gates[gate.group(1)] = cells[1]
    check(len(gates) >= 10, f"the gate table names {len(gates)} technologies, expected at least 10")

    for name, cost in gates.items():
        tech = by_name.get(name)
        check(tech, f"the spec's gate `{name}` is not a technology in technology.json")
        if not tech:
            continue
        if cost.startswith("trigger"):
            check(
                tech["cost_kind"] == "trigger",
                f"`{name}` is a trigger gate in the spec but costs {sorted(pack_set(tech))}",
            )
            continue
        claimed = {p.strip() + "-science-pack" for p in cost.split("+")}
        check(
            tech["cost_kind"] == "packs" and pack_set(tech) == claimed,
            f"`{name}` costs {tech['cost_kind']} {sorted(pack_set(tech))} in the corpus, "
            f"the spec claims {sorted(claimed)}",
        )

    for name in ("rocket-silo", "production-science-pack", "uranium-processing"):
        check(name in gates, f"the spec's gate table does not name `{name}`")
    if "rocket-silo" not in by_name:
        return
    silo_path = ancestors("rocket-silo", by_name) | {"rocket-silo"}
    costs_production = sorted(
        n for n in silo_path if "production-science-pack" in pack_set(by_name[n])
    )
    check(
        not costs_production,
        f"the launch needs a production pack via {costs_production} -- the ladder is four "
        "rungs again",
    )
    check(
        "uranium-processing" not in silo_path,
        "the silo now requires uranium-processing -- the reactor is no longer a terminal branch",
    )


def main():
    tech_path = DATA / "technology.json"
    if not tech_path.is_file():
        sys.exit(f"no {tech_path.relative_to(REPO)} -- run scripts/factorio-tech-extract.py")

    techs = json.loads(tech_path.read_text(encoding="utf-8"))
    packs = json.loads((DATA / "science_packs.json").read_text(encoding="utf-8"))
    extractor = load_extractor()

    check_pruning_rules(extractor)
    check_reparenting(extractor)
    check_effect_collapsing(extractor)
    check_tree(techs)
    check_no_pruned_families(techs)
    check_ladder(techs)

    check(packs, "no science packs extracted -- the prototype key moved again")
    check(
        {p["name"] for p in packs} >= {"automation-science-pack", "promethium-science-pack"},
        "the science pack list is missing base or Space Age packs",
    )

    if FAILURES:
        for failure in FAILURES:
            print(f"FAIL  {failure}")
        print(f"\n{len(FAILURES)} failures")
        sys.exit(1)
    print(f"ok  {len(techs)} technologies, {len(packs)} science packs")


if __name__ == "__main__":
    main()
