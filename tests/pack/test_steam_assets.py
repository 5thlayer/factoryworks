#!/usr/bin/env python3
"""Assert the steam chain's resource and its fluids' pack-side assets agree (#223, ADR-0048).

`fluid/steam_chain.json` is hand-owned data (#599). Asserted here:

  - **The Boiler and the Steam Engine agree.** The Boiler's target temperature is the Steam Engine's
    maximum (ADR-0048's one boiler tier).
  - **The fluids' lang keys.** These are `factoryworks:` fluids, so nothing else in the pack
    names them, and a missing `fluid_type` key renders the raw key in a tank tooltip.
  - **That neither fluid has a bucket.** ADR-0037 answered portable fluid for this pack --
    `factoryworks:barrel`, any fluid at Factorio's own 50 mB -- and states that capacity as a
    rule a later container "does not get to be re-argued from Minecraft's bucket" against. A
    1 000 mB bucket of steam is the twentyfold dose that ADR rejects, and it hands the player a
    hand-carry route around the Boiler-pipe-Engine chain rung 0 exists to teach. Asserted as an
    absence rather than left undone, because a bucket is the obvious thing to add back.
  - **That neither fluid is a GT material.** ADR-0048's central point: `gtceu:steam` is not inert,
    and nothing here may reach for it. Checked by grepping the fluid registration source for the
    string, since a static check cannot ask GregTech's own registry what accepted it.

What this file cannot assert is that the fluid actually renders in a tank -- that is a
`IClientFluidTypeExtensions` wiring fact, a world/client load, not a static one; see the ticket's
own Checks section.

Usage: tests/pack/test_steam_assets.py
"""

import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
STEAM_CHAIN = ROOT / "mod/src/main/resources/factoryworks_core/fluid/steam_chain.json"
ASSETS = ROOT / "kubejs/assets/factoryworks"

FLUID_JAVA_DIR = ROOT / "mod/src/main/java/com/factoryworks/core/fluid"
PF_FLUID_TYPES = FLUID_JAVA_DIR / "PFFluidTypes.java"
PF_FLUIDS = FLUID_JAVA_DIR / "PFFluids.java"

BOILER_NAME = "boiler"
STEAM_ENGINE_NAME = "steam-engine"

FLUIDS = ("steam", "superheated_steam", "crude_oil", "heavy_oil", "light_oil", "petroleum_gas",
          "lubricant", "sulfuric_acid")

def resolves(path):
    return path.is_file()


def check_chain(failures):
    if not resolves(STEAM_CHAIN):
        failures.append(f"{STEAM_CHAIN.relative_to(ROOT)} is missing")
        return
    rows = json.loads(STEAM_CHAIN.read_text())
    boiler = rows.get(BOILER_NAME)
    engine = rows.get(STEAM_ENGINE_NAME)
    if boiler is None or engine is None:
        failures.append(f"steam_chain.json lacks a {BOILER_NAME} or {STEAM_ENGINE_NAME} row")
        return
    if boiler.get("target_temperature") != engine.get("maximum_temperature"):
        failures.append(
            "the Boiler's target_temperature and the Steam Engine's maximum_temperature no longer "
            "agree -- ADR-0048's 'one boiler tier, no heat layer' was written against them matching"
        )


def check_assets(lang, failures):
    for fluid_name in FLUIDS:
        fluid_type_key = f"fluid_type.factoryworks.{fluid_name}"
        if not lang.get(fluid_type_key):
            failures.append(f"{fluid_name} has no {fluid_type_key} lang entry -- it would show its "
                            "raw key in a tank tooltip")


def check_no_bucket(lang, failures):
    """ADR-0037: the barrel is this pack's portable fluid container, at Factorio's 50 mB.

    A bucket is the obvious thing for a later hand to add back, so the absence is asserted rather
    than merely left undone.
    """
    source = PF_FLUIDS.read_text(encoding="utf-8")
    code = re.sub(r"/\*.*?\*/", "", source, flags=re.DOTALL)
    code = re.sub(r"//.*", "", code)
    if "BucketItem" in code:
        failures.append(
            "PFFluids registers a BucketItem -- ADR-0037 makes factoryworks:barrel this pack's "
            "portable fluid container at Factorio's 50 mB, and says a later container does not get "
            "to be re-argued from Minecraft's bucket"
        )

    for fluid_name in FLUIDS:
        bucket_key = f"item.factoryworks.{fluid_name}_bucket"
        if lang.get(bucket_key):
            failures.append(f"{bucket_key} is a lang entry, but {fluid_name} has no bucket")

        model = ASSETS / f"models/item/{fluid_name}_bucket.json"
        if resolves(model):
            failures.append(f"{model} exists, but {fluid_name} has no bucket")


def check_not_gtceu_steam(failures):
    """ADR-0048's central point: gtceu:steam is not inert, so nothing here may reach for it.

    Only *code* is checked, not prose: this file's own docstrings and the classes' javadoc are
    allowed to name `gtceu:steam` when explaining what must not be used, so block comments and
    line comments are stripped before the search.
    """
    for path in (PF_FLUID_TYPES, PF_FLUIDS):
        if not resolves(path):
            failures.append(f"{path.relative_to(ROOT)} is missing")
            continue
        source = path.read_text(encoding="utf-8")
        code = re.sub(r"/\*.*?\*/", "", source, flags=re.DOTALL)
        code = re.sub(r"//.*", "", code)
        if "gtceu" in code.lower():
            failures.append(
                f"{path.relative_to(ROOT)} mentions gtceu outside a comment -- ADR-0048 is "
                "explicit that these are factoryworks: fluids, never GregTech's own steam"
            )


def main():
    failures = []
    lang = json.loads((ASSETS / "lang/en_us.json").read_text())

    check_chain(failures)
    check_assets(lang, failures)
    check_no_bucket(lang, failures)
    check_not_gtceu_steam(failures)

    if failures:
        print(f"FAIL {len(failures)}:")
        for failure in failures:
            print(f"  - {failure}")
        return 1
    print("ok   steam chain: boiler and steam-engine agree, "
          "both pack fluids named, no bucket, neither is gtceu:steam")
    return 0


if __name__ == "__main__":
    sys.exit(main())
