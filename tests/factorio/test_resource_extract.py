#!/usr/bin/env python3
"""Assert the committed corpus still says what the pump reads off it.

Resource amounts, the outfield law and crude's figures are each Module's own data now, so
nothing here compares them to the corpus (#600).
What stays is the corpus-backed half that belongs to other tickets:

  - **The offshore pump feeds exactly twenty boilers (ADR-0050/#210).** The boiler's water draw
    is re-derived from `machine.json` and `fluid.json`, never read as a trusted 60 mB/s. Two
    traps are named inline.

Usage: tests/factorio/test_resource_extract.py
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent

# ADR-0050/#210's names. `machine.json` and `fluid.json` carry many machines and could
# carry more fluids later; pinning which rows this check reads keeps a future addition to
# either file from silently changing what the ratio is computed against.
BOILER_NAME = "boiler"
PUMP_NAME = "offshore-pump"
WATER_NAME = "water"
STEAM_NAME = "steam"

# Factorio's own tick rate. `pumping_speed` is per Factorio tick, and this is the factor
# that turns it into mB/s -- not Minecraft's tick rate, which is also 20 and is the reason
# this conversion is easy to get wrong; see the trap note in the module docstring.
FACTORIO_TICKS_PER_SECOND = 60

# ADR-0050's claim: one Offshore Pump feeds exactly this many Boilers.
PUMP_TO_BOILER_RATIO = 20

def main():
    failures = []

    # ADR-0050/#210: one Offshore Pump feeds exactly twenty Boilers, re-derived from the
    # corpus rather than trusted. Every term below is read off `machine.json` or
    # `fluid.json`; nothing here is typed.
    machine_data = json.loads((ROOT / "data/factorio/machine.json").read_text())
    fluid_data = json.loads((ROOT / "data/factorio/fluid.json").read_text())
    boiler = next((b for b in machine_data.get("boilers", []) if b["name"] == BOILER_NAME), None)
    pump = next((p for p in machine_data.get("pumps", []) if p["name"] == PUMP_NAME), None)
    fluids = {f["name"]: f for f in fluid_data.get("fluids", [])}
    water = fluids.get(WATER_NAME)
    steam = fluids.get(STEAM_NAME)

    if boiler is None:
        failures.append(f"machine.json carries no boiler named {BOILER_NAME!r}")
    if pump is None:
        failures.append(f"machine.json carries no pump named {PUMP_NAME!r}")
    if water is None:
        failures.append(f"fluid.json carries no fluid named {WATER_NAME!r}")
    if steam is None:
        failures.append(f"fluid.json carries no fluid named {STEAM_NAME!r}")

    if boiler and pump and water and steam:
        target_temperature = boiler.get("target_temperature")
        energy_consumption = boiler.get("energy_consumption")
        pumping_speed = pump.get("pumping_speed")
        # TRAP 1: `pumping_speed` is per *Factorio tick*, and its value (20) coincidentally
        # equals Minecraft's own 20-ticks-per-second -- a reader primed by that number is
        # tempted to treat this as already a per-second rate and skip the multiplication.
        # It is Factorio's tick rate that applies here, not Minecraft's, and Factorio runs
        # at 60 ticks/s, not 20.
        pump_rate = None
        if pumping_speed is not None:
            pump_rate = pumping_speed * FACTORIO_TICKS_PER_SECOND

        water_default_temperature = water.get("default_temperature")
        # TRAP 2: `water.heat_capacity` (2 kJ) is the obvious-looking term and is wrong.
        # ADR-0050 states, from Factorio's own arithmetic, that the boiler's energy spend
        # is governed by STEAM's heat capacity (0.2 kJ) -- six times smaller than water's.
        # Substituting water's here yields a plausible ~10 units/s instead of the true 60,
        # wrong by exactly the 6x that separates the two constants.
        steam_heat_capacity = steam.get("heat_capacity")

        missing = [
            (name, value)
            for name, value in (
                ("boiler.target_temperature", target_temperature),
                ("boiler.energy_consumption", energy_consumption),
                ("pump.pumping_speed", pumping_speed),
                ("water.default_temperature", water_default_temperature),
                ("steam.heat_capacity", steam_heat_capacity),
            )
            if value is None
        ]
        if missing:
            failures.append(
                "the pump:boiler ratio cannot be derived -- missing "
                + ", ".join(name for name, _ in missing)
            )
        else:
            energy_per_unit = (target_temperature - water_default_temperature) * steam_heat_capacity
            if energy_per_unit <= 0:
                failures.append(
                    f"energy per unit is {energy_per_unit} (target {target_temperature}C, "
                    f"water default {water_default_temperature}C, steam heat capacity "
                    f"{steam_heat_capacity}J) -- the boiler cannot heat water under these figures"
                )
            else:
                boiler_draw = energy_consumption / energy_per_unit
                ratio = pump_rate / boiler_draw
                if abs(ratio - PUMP_TO_BOILER_RATIO) > 1e-6:
                    failures.append(
                        f"one {PUMP_NAME} pumps {pump_rate:.1f} mB/s and one {BOILER_NAME} draws "
                        f"{boiler_draw:.1f} mB/s (from {energy_consumption}W / "
                        f"({target_temperature}C - {water_default_temperature}C) / "
                        f"{steam_heat_capacity}J) -- a ratio of {ratio:.3f}, not "
                        f"{PUMP_TO_BOILER_RATIO}. ADR-0050's 'one pump feeds twenty boilers' "
                        "no longer holds against the extracted corpus"
                    )

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print("ok   one pump feeds twenty boilers")
    return 0


if __name__ == "__main__":
    sys.exit(main())
