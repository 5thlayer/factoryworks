#!/usr/bin/env python3
"""Every string a machine's Jade tooltip shows has a lang entry (#333, #352, #468).

A missing key does not fail anywhere: it renders raw on the crosshair. The keys are read out of each
plugin, and the status keys out of the status enum, whose `langKey()` the plugin asks rather than
spelling them. The provider is not reachable from the Minecraft-free test source set, so this is
source text.

Usage: tests/pack/test_machine_jade.py
"""

import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
CORE = ROOT / "mod/src/main/java/com/planetaryfactory/core"
LANG = ROOT / "kubejs/assets/planetaryfactory/lang/en_us.json"

# Each plugin with the helpers that spell its text, and the enum its status line is read from.
MACHINES = {
    "Assembling Machine": ((CORE / "compat/AssemblingMachineJadePlugin.java",
                            CORE / "machine/AssemblingStatusText.java"),
                           CORE / "machine/AssemblingStatus.java", "AssemblingStatus"),
    "Steam Engine": ((CORE / "compat/SteamEngineJadePlugin.java",),
                     CORE / "fluid/SteamEngineStatus.java", "SteamEngineStatus"),
    "Accumulator": ((CORE / "compat/AccumulatorJadePlugin.java",),
                    CORE / "energy/AccumulatorStatus.java", "AccumulatorStatus"),
}

# Any key-shaped literal, not only a `translatable(` argument: a helper that takes the key as a
# parameter would otherwise hide it.
KEY_RE = re.compile(r'"((?:gui|tooltip)\.planetaryfactory\.[a-z_.]+)"')


def status_keys(path, enum):
    source = path.read_text(encoding="utf-8")
    body = re.search(rf"enum {enum} \{{(.*?);", source, re.S).group(1)
    prefix = re.search(r'return "([a-z_.]+)" \+ name\(\)', source).group(1)
    return {prefix + name.strip().lower() for name in body.split(",") if name.strip()}


def main():
    lang = json.loads(LANG.read_text(encoding="utf-8"))
    failures = []
    count = 0
    for machine, (sources, status, enum) in MACHINES.items():
        source = "\n".join(path.read_text(encoding="utf-8") for path in sources)
        keys = set(KEY_RE.findall(source))
        # A plugin whose only text is its status names no key literal of its own.
        if not keys and "langKey()" not in source:
            failures.append(f"no lang keys parsed out of {sources[0].relative_to(ROOT)}")
        if "langKey()" not in source:
            failures.append(f"the {machine} plugin no longer asks {enum}.langKey(); the status keys are unchecked")
        keys |= status_keys(status, enum)
        count += len(keys)
        for key in sorted(keys):
            if not lang.get(key):
                failures.append(f"{key} has no lang entry -- the {machine}'s HUD would show its raw key")

    for index, failure in enumerate(failures, 1):
        print(f"FAIL {index}: {failure}")
    if failures:
        return 1
    print(f"ok   {count} machine HUD keys across {len(MACHINES)} plugins, each with a lang entry")
    return 0


if __name__ == "__main__":
    sys.exit(main())
