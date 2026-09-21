#!/usr/bin/env python3
"""Extract Factorio's resource amounts -- how much ore a patch holds.

ADR-0041 makes an ore block carry an amount, and ADR-0022's extract-never-transcribe rule
says none of those numbers may be chosen. They do not have to be: Factorio's resource
amounts are *closed-form in the prototype dump*, not buried in map generation.

Three things are read, and nothing is decided:

  - **The starting patch total.** `resource_autoplace_all_patches` defines it outright as
    `20000 * base_density * (frequency_multiplier + 1) * size_multiplier`. The formula is
    read out of the function's own local expressions rather than typed here, and each
    resource's `base_density` out of the arguments its `default-<name>-patches` noise
    expression passes. At default controls -- frequency and size both 1, which is what a
    default map deals -- that is 400,000 for iron and 320,000 for copper and coal.
  - **The distance law.** Every resource's `richness_expression` carries the same term,
    `max((1000 + distance) / 2600, 1)`: flat inside spawn's neighbourhood and rising
    linearly beyond. The break-even distance is solved from the term, not typed, and it is
    the arithmetic saying Factorio does not reward leaving early. The outfield law --
    `regular_density_at`, with its three radii -- is closed-form in the same function and
    comes across whole, so a later body siting outfield veins reads it here.
  - **The outfield spot.** The four values the outfield law names and the committed functions
    did not carry: `random_spot_size_minimum`/`maximum`, `regular_rq_factor` and
    `regular_blob_amplitude_multiplier` are arguments each `default-<name>-patches` passes,
    and `regular_blob_amplitude_maximum_distance` is a local expression, evaluated per
    resource. The law they feed -- a spot's quantity, radius, peak height and blob amplitude
    against distance, and the mean spacing -- is evaluated here out of the dump's own
    expressions and re-derived from a closed form by the check (#317). The minimum spacing
    `spot_noise` keeps between candidate spots comes too, as the outfield structure sets'
    separation (#320).
  - **The hand-mining numbers.** Each resource's `minable.mining_time`, the character's own
    `mining_speed`, and what `steel-axe` adds to it. ADR-0039 labelled these as *transcribed
    from the wiki, not extracted*, because `data/factorio/` held no resource dump and so
    they could not be checked against the repo the way the technology tree can. It holds
    one now, and `tests/factorio/test_resource_extract.py` asserts `PickTier` against them.
  - **The character's walking speed.** `running_speed` sits beside `mining_speed` in the same
    `character` prototype, in tiles per tick. It is here because the starting area's traversal
    budget has two halves and both were unextracted (#207): Factorio's engineer's speed against
    Minecraft's, and Factorio's `starting_resource_placement_radius` against Terra's own
    distances. On foot only -- vehicles are #121.
  - **The stage thresholds.** `stage_counts` is what Factorio renders its eight sprite
    stages against. They are extracted as *ratios of each resource's own first rung*,
    because that is the only form usable against blocks holding a thousand units rather
    than fifteen thousand. Iron, copper, coal and stone share one list; uranium's is that
    list scaled by about 2/3 and then rounded to two or three figures, so the fraction sets
    agree to rounding and not exactly. The rounding is preserved rather than smoothed --
    `stage_ratios` is each resource's own -- and `tests/factorio/test_resource_extract.py`
    is where the agreement is asserted with the tolerance stated.

A Factorio tile and a Minecraft block are both one metre, so every distance here is a
block count already. Nothing in this file converts anything.

Scope is the resources placed by `resource_autoplace_all_patches` -- Nauvis's six. A
resource placed by some other expression (Vulcanus's calcite, Fulgora's scrap) has no
starting amount to read and is skipped by name in the output, the way the other extractors
report what they left out.

Two files are written from one read. `data/factorio/resource.json` is the corpus, and
`mod/src/main/resources/planetaryfactory_core/ore/amounts.json` is the slice the mod loads at
class-init: the five resources ADR-0041 puts on Terra, their patch totals, their stage ratios, the
distance law and the outfield density law. It is a *classpath* resource rather than a datapack file because the stage count
sizes a blockstate property, which is fixed before any world exists -- and it is generated here
rather than typed in Java for the same reason nothing else in the pack is typed twice.

Usage:

    scripts/factorio-resource-extract.py            # finds the dump, writes both files
    scripts/factorio-resource-extract.py --dump PATH
    scripts/factorio-resource-extract.py --check    # exits 1 if either file is stale
"""

import argparse
import json
import math
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent

DEFAULT_DUMP = (
    Path.home()
    / "Library/Application Support/factorio/script-output/data-raw-dump.json"
)

# The autoplace function whose arguments carry the amounts. A resource placed by anything
# else is out of scope; see the module docstring.
PATCH_FUNCTION = "resource_autoplace_all_patches"

# The map-generation controls, at the settings a default map deals. Factorio's own
# defaults for `frequency`, `size` and `richness` are 1; the dump carries the controls'
# existence but not their default values, so this is the one number here that is stated
# rather than read -- and it is a map setting, not a property of any resource.
DEFAULT_CONTROL = 1.0

# Factorio's tick rate. `running_speed` is tiles *per tick*, so this is what turns it into the
# tiles per second a Minecraft blocks-per-second figure can be set beside. It is an engine
# constant rather than a property of any prototype, which is why it is stated here and is the
# only number in the movement extraction that is not read.
TICKS_PER_SECOND = 60

# The local expressions worth carrying whole: the outfield law's three radii, in tiles,
# which are blocks one-for-one.
CARRIED_CONSTANTS = (
    "starting_resource_placement_radius",
    "regular_patch_fade_in_distance",
    "double_density_distance",
    "starting_patches_split",
)

# The per-resource arguments the outfield spot reads (ADR-0045).
OUTFIELD_ARGUMENTS = (
    "random_spot_size_minimum",
    "random_spot_size_maximum",
    "regular_rq_factor",
    "regular_blob_amplitude_multiplier",
)

# The local expressions the outfield spot reads. The radius has no name of its own in the
# dump: it is the `spot_radius_expression` argument inside `regular_patches`.
OUTFIELD_LOCALS = ("regular_blob_amplitude_maximum_distance", "regular_spot_quantity_expression")
RADIUS_SOURCE = ("regular_patches", "spot_radius_expression")

# The ragged edge: `regular_patches` is the spot's cone plus noise octaves scaled by the blob
# amplitude, and `blobs0` holds two of the octaves (ADR-0045).
EDGE_LOCALS = ("regular_patches", "blobs0")
OCTAVE = re.compile(r"basis_noise\{[^}]*input_scale\s*=\s*([^,}]+),\s*output_scale\s*=\s*([^,}]+)\}")
CANDIDATE_SPACING = re.compile(r"suggested_minimum_candidate_point_spacing\s*=\s*([0-9.]+)")
RADIUS_CAP = re.compile(r"^\s*min\(\s*([0-9.]+)\s*,")
EDGE_OFFSET = re.compile(r"-\s*([0-9./]+)\s*\)\s*\*\s*regular_blob_amplitude_at\(distance\)\s*$")

# Where the law is tabulated: the three radii's edges, the richness crossover and two points past
# it, so a reader sees the spot stop growing where the richness term starts.
LAW_DISTANCES = (0, 150, 300, 450, 1000, 1600, 3000, 10000)

# Terra's alphabet (ADR-0041), and the Factorio resource each pack ore block reads its amounts
# from. The keys are the pack's block names and the values are Factorio's, which is ADR-0028's
# declared exception: the corpus is keyed by Factorio's own names.
TERRA_ALPHABET = {
    "iron": "iron-ore",
    "copper": "copper-ore",
    "coal": "coal",
    "uranium": "uranium-ore",
    "stone": "stone",
}

ARGUMENT = re.compile(r"(\w+)\s*=\s*([^,{}]+?)\s*(?=,\s*\w+\s*=|\}$)")
DISTANCE_TERM = re.compile(r"max\(\s*\(\s*(\d+)\s*\+\s*distance\s*\)\s*/\s*(\d+)\s*,\s*1\s*\)")


def patch_arguments(expression):
    """The arguments a `default-<name>-patches` expression passes to the patch function.

    Returns `None` when the expression calls something else, which is how a resource
    placed by another planet's rules falls out of scope.
    """
    if PATCH_FUNCTION not in expression:
        return None
    body = expression[expression.index("{"):]
    return {name: value for name, value in ARGUMENT.findall(body)}


def number(value):
    """An argument that is a literal number, or `None` when it is a control variable."""
    try:
        return float(value)
    except ValueError:
        return None


def starting_amount(formula, base_density):
    """The starting patch total, evaluated from the function's own formula.

    The formula is read out of the dump and evaluated here against the default controls,
    so a Factorio release that changes the constant changes this output rather than
    disagreeing with it silently.
    """
    return eval(  # noqa: S307 -- the formula is the dump's, and the names are bound below
        formula,
        {"__builtins__": {}},
        {
            "base_density": base_density,
            "frequency_multiplier": DEFAULT_CONTROL,
            "size_multiplier": DEFAULT_CONTROL,
        },
    )


def distance_law(richness_expression):
    """The `max((1000 + distance) / 2600, 1)` term, and where it stops being flat.

    The break-even distance is solved from the term the prototype carries -- the point at
    which the ratio reaches 1 -- rather than typed, because it is the number ADR-0041
    quotes for why leaving the starting area early buys nothing.
    """
    found = DISTANCE_TERM.search(richness_expression or "")
    if not found:
        return None
    offset, divisor = int(found.group(1)), int(found.group(2))
    return {
        "term": found.group(0),
        "offset": offset,
        "divisor": divisor,
        "flat_within": divisor - offset,
    }


def call_argument(expression, name):
    """The text of one `name = ...` argument inside a `f{...}` call, up to its top-level comma."""
    start = expression.index(name + " = ") + len(name) + 3
    depth = 0
    for index in range(start, len(expression)):
        char = expression[index]
        if char in "({":
            depth += 1
        elif char in ")}":
            if depth == 0:
                return expression[start:index].strip()
            depth -= 1
        elif char == "," and depth == 0:
            return expression[start:index].strip()
    return expression[start:].strip()


def noise_evaluator(functions, bindings):
    """Evaluate a Factorio noise expression string against one resource's arguments.

    `random_penalty_between(min, max, 1)` is a per-spot random draw; it is taken at its midpoint,
    `(min + max) / 2`, which is what Factorio's own `regular_spot_height_typical_at` uses.
    """
    scope = {
        "pi": math.pi,
        "min": min,
        "max": max,
        "clamp": lambda value, low, high: min(max(value, low), high),
        "_if": lambda condition, then, otherwise: then if condition else otherwise,
        "random_penalty_between": lambda low, high, _seed: (low + high) / 2,
    }
    scope.update(bindings)

    def compile_(expression):
        return re.sub(r"\bif\(", "_if(", str(expression)).replace("^", "**")

    def value(expression, **extra):
        return eval(  # noqa: S307 -- the expression is the dump's, and the names are bound here
            compile_(expression), {"__builtins__": {}}, {**scope, **extra}
        )

    def function(body):
        return lambda *args: value(body["expression"], **dict(zip(body["parameters"], args)))

    for name, body in functions.items():
        scope[name] = function(body)
    return value


def outfield(arguments, locals_, functions, radius_expression):
    """One resource's outfield spot: its four arguments and the law they feed."""
    constants = {key: number(locals_.get(key)) for key in CARRIED_CONSTANTS}
    bindings = {name: number(arguments.get(name)) for name in OUTFIELD_ARGUMENTS}
    bindings.update(
        base_density=number(arguments.get("base_density")),
        base_spots_per_km2=number(arguments.get("base_spots_per_km2")),
        has_starting_area_placement=number(arguments.get("has_starting_area_placement")),
        frequency_multiplier=DEFAULT_CONTROL,
        size_multiplier=DEFAULT_CONTROL,
        **{key: constant for key, constant in constants.items() if constant is not None},
    )
    reach = noise_evaluator(functions, bindings)(locals_["regular_blob_amplitude_maximum_distance"])
    bindings["regular_blob_amplitude_maximum_distance"] = reach
    value = noise_evaluator(functions, bindings)

    def at(distance):
        quantity = value(locals_["regular_spot_quantity_expression"], distance=distance)
        return {
            "distance": distance,
            "density": value("regular_density_at(distance)", distance=distance),
            "spot_quantity": quantity,
            "spot_radius": value(
                radius_expression, distance=distance, regular_spot_quantity_expression=quantity
            ),
            "spot_height": value("regular_spot_height_typical_at(distance)", distance=distance),
            "blob_amplitude": value("regular_blob_amplitude_at(distance)", distance=distance),
        }

    # Spots are placed until the density is met, so the spacing follows the mean spot quantity,
    # and with it the size factor's midpoint, not the base quantity.
    spots_per_block = value("regular_density_at(distance)", distance=reach) / value(
        locals_["regular_spot_quantity_expression"], distance=reach
    )
    return {
        **{name: bindings[name] for name in OUTFIELD_ARGUMENTS},
        "regular_blob_amplitude_maximum_distance": reach,
        "mean_spacing": 1 / math.sqrt(spots_per_block),
        "law": [at(distance) for distance in LAW_DISTANCES],
    }


def after_spot_noise(expression):
    """What `expression` adds to its `spot_noise{...}` call, with the leading `+` dropped."""
    start = expression.index("spot_noise{") + len("spot_noise{")
    depth = 0
    for index in range(start, len(expression)):
        if expression[index] == "{":
            depth += 1
        elif expression[index] == "}":
            if depth == 0:
                return expression[index + 1 :].strip().removeprefix("+").strip()
            depth -= 1
    sys.exit("regular_patches' spot_noise call never closes")


def edge(locals_):
    """The outfield spot's ragged edge: `(octaves - offset) * regular_blob_amplitude_at(distance)`."""
    expression = after_spot_noise(locals_["regular_patches"])
    octaves = [
        {"input_scale": eval(scale, {"__builtins__": {}}), "output_scale": eval(weight, {"__builtins__": {}})}  # noqa: S307
        for scale, weight in OCTAVE.findall(locals_["blobs0"] + expression)
    ]
    offset = EDGE_OFFSET.search(expression)
    if not octaves or not offset or "blobs0" not in expression:
        sys.exit(f"regular_patches' edge no longer reads as octaves minus an offset: {expression}")
    return {
        "expression": expression,
        "blobs0": locals_["blobs0"],
        "octaves": octaves,
        "offset": eval(offset.group(1), {"__builtins__": {}}),  # noqa: S307
    }


def placement(locals_):
    """How close two spots may be: the spacing `spot_noise` keeps between candidate points (#320)."""
    spacing = CANDIDATE_SPACING.search(locals_["regular_patches"])
    if not spacing:
        sys.exit("regular_patches' spot_noise no longer suggests a minimum candidate spacing")
    return {"suggested_minimum_candidate_point_spacing": float(spacing.group(1))}


def hand_mining(dump):
    """The character's mining speed, and the speed `steel-axe` leaves them mining at.

    ADR-0039's two tiers are these two numbers: the bare character, and the character after
    the research. Both are read rather than transcribed, which is the weakness that ADR
    labelled and named an extractor as the fix for.

    **`character-mining-speed` is a fraction, not an addend.** Factorio applies the modifier
    as `base * (1 + modifier)`, so `steel-axe`'s `1` is +100% and takes the character from
    0.5 to 1.0 rather than to 1.5. ADR-0039's prose says the research "adds 1 to it" and its
    `PickTier.STEEL` ships 1.0 -- the number is right and the sentence describes the wrong
    operation, which is exactly the kind of drift a transcription hides and an extraction
    does not.
    """
    character = (dump.get("character") or {}).get("character") or {}
    effects = ((dump.get("technology") or {}).get("steel-axe") or {}).get("effects") or []
    bonus = sum(
        effect.get("modifier", 0)
        for effect in effects
        if effect.get("type") == "character-mining-speed"
    )
    return {
        "character_mining_speed": character.get("mining_speed"),
        "steel_axe_modifier": bonus,
        "character_mining_speed_researched": (character.get("mining_speed") or 0) * (1 + bonus),
    }


def character_movement(dump):
    """The character's walking speed, read off the same prototype `mining_speed` comes from.

    `running_speed` is a sibling key of `mining_speed` in the `character` prototype and carries
    tiles per tick; at 60 ticks a second that is the tiles per second a Minecraft walking speed
    is compared against (#207). A Factorio tile and a Minecraft block are both one metre, so the
    comparison needs no conversion beyond the tick rate.

    Base movement only. `character-running-speed` modifiers are collected so that a research
    granting one would appear here rather than be assumed absent -- in the base game and Space
    Age the bonuses come from equipment (exoskeletons), not technology, and the list is empty.
    Vehicles are out of scope: that is Personal transport, #121.
    """
    character = (dump.get("character") or {}).get("character") or {}
    speed = character.get("running_speed")
    modifiers = sorted(
        name
        for name, technology in (dump.get("technology") or {}).items()
        for effect in (technology.get("effects") or [])
        if effect.get("type") == "character-running-speed"
    )
    return {
        "running_speed": speed,
        "ticks_per_second": TICKS_PER_SECOND,
        "running_speed_per_second": None if speed is None else speed * TICKS_PER_SECOND,
        "running_speed_technologies": modifiers,
    }


def extract(dump):
    function = (dump.get("noise-function") or {}).get(PATCH_FUNCTION)
    if not function:
        sys.exit(f"dump carries no noise function {PATCH_FUNCTION!r}")
    locals_ = function.get("local_expressions") or {}
    functions = function.get("local_functions") or {}

    formula = locals_.get("starting_amount")
    if not formula:
        sys.exit(f"{PATCH_FUNCTION} carries no `starting_amount` expression")

    missing = [key for key in (*OUTFIELD_LOCALS, *EDGE_LOCALS) if key not in locals_]
    if missing:
        sys.exit(f"{PATCH_FUNCTION} carries no {', '.join(missing)}")
    radius_expression = call_argument(locals_[RADIUS_SOURCE[0]], RADIUS_SOURCE[1])

    expressions = dump.get("noise-expression") or {}
    resources, skipped = [], []
    laws = {}

    for name, prototype in sorted((dump.get("resource") or {}).items()):
        autoplace = prototype.get("autoplace") or {}
        patches = expressions.get(f"default-{name}-patches") or {}
        arguments = patch_arguments(patches.get("expression", ""))
        if arguments is None:
            skipped.append(name)
            continue

        density = number(arguments.get("base_density"))
        starts = number(arguments.get("has_starting_area_placement")) == 1
        stages = prototype.get("stage_counts") or []
        law = distance_law(autoplace.get("richness_expression"))
        if law:
            laws[law["term"]] = laws.get(law["term"], 0) + 1

        resources.append(
            {
                "name": name,
                "category": prototype.get("category", "basic-solid"),
                "infinite": bool(prototype.get("infinite")),
                "minimum": prototype.get("minimum"),
                "base_density": density,
                "base_spots_per_km2": number(arguments.get("base_spots_per_km2")),
                "has_starting_area_placement": starts,
                # A resource with no starting patch has no starting total to state, and
                # `null` says so rather than a zero that reads as an empty patch.
                "starting_amount": starting_amount(formula, density) if starts else None,
                "mining_time": (prototype.get("minable") or {}).get("mining_time"),
                "required_fluid": (prototype.get("minable") or {}).get("required_fluid"),
                "stage_counts": stages,
                "stage_ratios": [count / stages[0] for count in stages] if stages and stages[0] else [],
                "distance_law": law,
                "outfield": outfield(arguments, locals_, functions, radius_expression),
            }
        )

    return {
        "starting_amount_formula": formula,
        "controls": {
            "frequency_multiplier": DEFAULT_CONTROL,
            "size_multiplier": DEFAULT_CONTROL,
            "richness": DEFAULT_CONTROL,
        },
        "constants": {
            key: number(locals_[key]) for key in CARRIED_CONSTANTS if key in locals_
        },
        "outfield_law": {
            name: {
                "parameters": body.get("parameters"),
                "expression": body.get("expression"),
            }
            for name, body in sorted(functions.items())
        },
        "outfield_expressions": {
            **{key: locals_[key] for key in OUTFIELD_LOCALS},
            "regular_spot_radius_expression": radius_expression,
        },
        "outfield_edge": edge(locals_),
        "outfield_placement": placement(locals_),
        "hand_mining": hand_mining(dump),
        "character_movement": character_movement(dump),
        "resources": resources,
        "skipped": skipped,
    }, laws


def mod_slice(out):
    """The part of the corpus the mod loads, keyed by the pack's own block names.

    Deliberately thin: a total, a ratio set, a mining time, the distance law, the per-resource
    arguments and three constants the outfield amount takes (#319), and what the disc's shape
    takes: its radius factor and cap, blob amplitude and edge octaves (#320). Everything else
    in the corpus is read by scripts, and a number that reaches Java is a number that has to
    survive a recompile to be corrected.

    `mining_time` is here because a rig's operation rate is `mining_speed / mining_time` -- the
    drill's figure over the resource's -- and ADR-0043 gives the rig an explicit
    operations-per-second rather than a welded constant. Uranium's 2 against everything else's 1
    is the whole reason it cannot live on the drill.
    """
    by_name = {entry["name"]: entry for entry in out["resources"]}
    law = next(
        (entry["distance_law"] for entry in out["resources"] if entry["distance_law"]), None
    )
    resources = {}
    for block, factorio in sorted(TERRA_ALPHABET.items()):
        entry = by_name.get(factorio)
        if entry is None:
            sys.exit(f"{factorio} is not in the dump -- Terra's alphabet has a hole")
        resources[block] = {
            "factorio_name": factorio,
            "starting_amount": entry["starting_amount"],
            "mining_time": entry["mining_time"],
            "stage_ratios": entry["stage_ratios"],
            "outfield": {
                "base_density": entry["base_density"],
                "base_spots_per_km2": entry["base_spots_per_km2"],
                "random_spot_size_minimum": entry["outfield"]["random_spot_size_minimum"],
                "random_spot_size_maximum": entry["outfield"]["random_spot_size_maximum"],
                "regular_rq_factor": entry["outfield"]["regular_rq_factor"],
                "regular_blob_amplitude_multiplier": entry["outfield"]["regular_blob_amplitude_multiplier"],
                "regular_blob_amplitude_maximum_distance": entry["outfield"][
                    "regular_blob_amplitude_maximum_distance"
                ],
            },
        }
    constants = out["constants"]
    return {
        "__generated_by": "scripts/factorio-resource-extract.py",
        "distance_law": law,
        "density_law": {
            "starting_resource_placement_radius": constants["starting_resource_placement_radius"],
            "regular_patch_fade_in_distance": constants["regular_patch_fade_in_distance"],
            "double_density_distance": constants["double_density_distance"],
        },
        "outfield_edge": {
            "radius_cap": radius_cap(out["outfield_expressions"]["regular_spot_radius_expression"]),
            "octaves": out["outfield_edge"]["octaves"],
            "offset": out["outfield_edge"]["offset"],
        },
        "resources": resources,
    }


def radius_cap(radius_expression):
    """The `min(32, ...)` bound on a spot's radius, which the disc's shape reads (#320)."""
    cap = RADIUS_CAP.match(radius_expression)
    if not cap:
        sys.exit(f"the spot radius is no longer min(cap, ...): {radius_expression}")
    return float(cap.group(1))


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dump", type=Path, default=DEFAULT_DUMP)
    parser.add_argument("--out", type=Path, default=REPO / "data" / "factorio" / "resource.json")
    parser.add_argument(
        "--mod-out",
        type=Path,
        default=REPO / "mod/src/main/resources/planetaryfactory_core/ore/amounts.json",
        help="the slice the mod loads at class-init",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="write nothing; exit 1 if either committed file differs from a fresh extraction",
    )
    args = parser.parse_args()

    if not args.dump.is_file():
        sys.exit(
            f"no dump at {args.dump}\n"
            "run:  factorio --dump-data --mod-directory <dir with base+SA only>"
        )

    dump = json.loads(args.dump.read_text(encoding="utf-8"))
    out, laws = extract(dump)
    slice_ = mod_slice(out)
    written = {
        args.out: json.dumps(out, indent=2) + "\n",
        args.mod_out: json.dumps(slice_, indent=2) + "\n",
    }

    if args.check:
        stale = [
            path for path, text in written.items()
            if not path.is_file() or path.read_text(encoding="utf-8") != text
        ]
        for path in stale:
            print(f"stale      {path.relative_to(REPO)} -- re-run scripts/factorio-resource-extract.py")
        if stale:
            sys.exit(1)
        print(f"ok         {len(written)} files match a fresh extraction")
        return

    for path, text in written.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")

    print(f"starting amount = {out['starting_amount_formula']}")
    print(
        f"hand mining     character {out['hand_mining']['character_mining_speed']}, "
        f"steel-axe +{out['hand_mining']['steel_axe_modifier']:.0%} "
        f"-> {out['hand_mining']['character_mining_speed_researched']}"
    )
    movement = out["character_movement"]
    print(
        f"movement        character {movement['running_speed']} tiles/tick "
        f"-> {movement['running_speed_per_second']} tiles/s"
    )
    print(f"wrote      {args.out.relative_to(REPO)}")
    print(f"wrote      {args.mod_out.relative_to(REPO)} ({len(slice_['resources'])} pack ores)")
    print(f"out of scope {len(out['skipped'])}: " + ", ".join(out["skipped"]))
    print()
    print(f"{'resource':14} {'density':>7} {'starting total':>15} {'stages':>7}  distance law")
    for resource in out["resources"]:
        total = resource["starting_amount"]
        print(
            f"{resource['name']:14} {resource['base_density'] or 0:7} "
            f"{'none' if total is None else f'{total:,.0f}':>15} "
            f"{len(resource['stage_counts']):7}  "
            f"{(resource['distance_law'] or {}).get('term', '-')}"
        )
    print()
    print(f"{'resource':14} {'spacing':>7} {'rq':>5} {'spot size':>10} {'radius':>7} {'height':>8}  at")
    for resource in out["resources"]:
        spot = resource["outfield"]
        far = next(row for row in spot["law"] if row["distance"] >= spot["regular_blob_amplitude_maximum_distance"])
        print(
            f"{resource['name']:14} {spot['mean_spacing']:7.0f} {spot['regular_rq_factor']:5} "
            f"{spot['random_spot_size_minimum']:>4}-{spot['random_spot_size_maximum']:<5} "
            f"{far['spot_radius']:7.1f} {far['spot_height']:8.0f}  {far['distance']}"
        )
    print()
    for term, count in sorted(laws.items()):
        law = next(r["distance_law"] for r in out["resources"] if (r["distance_law"] or {}).get("term") == term)
        print(f"{count} resources carry {term} -- flat within {law['flat_within']} tiles")


if __name__ == "__main__":
    main()
