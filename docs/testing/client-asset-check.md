# The client asset check

`scripts/check-client-assets.py` boots the pack's real client with no display, waits for the
resource reload to finish, and asserts the client's asset manager complained about none of this
pack's assets except the gaps listed in the script with the ticket that owns each one.

It is the client half of #273's in-world work, and it is the only check here that can see whether
anything the pack ships actually renders.

## Why a client boot rather than a sixth hop walker

Every other asset check here is file-to-file. `tests/pack/test_data_formats.py` walks item
definition → model; the asset-hop checks walk blockstate → model → texture. Both assert that the
files exist and name each other. Neither asserts that the game accepts any of them, and they cannot:
a definition with a wrong `type`, a model with an unresolvable `parent`, a texture that is not a
valid PNG and a blockstate missing a variant the block can be in each pass all of them and each
renders as the black-and-magenta checkerboard.

The two in-world checks #273 built cannot help either. `scripts/check-datapack-load.py` and #271's
GameTest harness are **server** runs. A model, a texture, a blockstate and an item model definition
are read by a client, which neither of them ever starts.

## What was found out before it was built

The ticket's three open questions, answered by measurement:

**Is there a client test harness on 26.1?** No. NeoForge 26.1.2.109 ships
`net/neoforged/neoforge/gametest/` and `RegisterGameTestsEvent`, all server-side; there is no client
GameTest and no test framework to hang one off. So this is not a harness, it is the real client.

**Can it run without a display?** Yes, and the pack already could. `scripts/launch.py --headless`
patches `config/fml.toml` to drop FML's early loading window — that splash is the only thing that
insists on a primary monitor — and Minecraft's own window creates offscreen quite happily. The
check imports that context manager rather than reimplementing it, so there is one copy of the code
that has to put a player's splash screen back.

**Does the asset manager log anything?** For most failures, loudly:

```
[Worker-Main-2/WARN]: Missing textures in model planetaryfactory:block/scrap_pile:
    gcyr:block/mars_regolith
[Worker-Main-2/WARN]: Missing model for variant: 'Block{planetaryfactory:steam}[level=0]'
[Worker-Main-2/WARN]: Missing FluidModel for fluid 'planetaryfactory:steam'
```

**But not for the failure the ticket was filed over.** A missing item model definition is logged
nowhere. Deleting `assets/planetaryfactory/items/boiler.json` and running this check produced a log
containing the string `boiler` zero times, while the item renders as the checkerboard in the
inventory, in the hand and in EMI. That is the measured shape of #273's fifteen, and it is why the
definition half of the claim stays with `test_data_formats.py`, which walks definition → model in
both directions statically. The two checks are complementary by measurement rather than by hope,
and that is recorded here so the next person does not delete the static one believing this covers
it.

## How it decides the run is over

The client never exits: it sits at the main menu. So the signal is silence — the log going quiet
for fifteen seconds — plus `minecraft:textures/atlas/blocks.png-atlas` having been created, which
is the proof the reload actually ran. Without that second condition a client that died during mod
construction would pass this check having asserted nothing, which is the one way a green result
here could mean nothing at all. The run is then killed by **process group**: `launch.py` is a Python
parent holding a Java child, and killing only the parent leaves a headless Minecraft running with
nobody watching it.

## The allowlist, and the stale rule

Same rule as `check-datapack-load.py`. Every complaint the log may contain about this pack is an
`EXPECTED` entry with its reason and its ticket; an unlisted one fails; and a listed one that no
longer appears fails too, because a stale entry is a defect somebody fixed and a guard nobody
re-armed. The eight today are two groups: four models naming `gcyr:` textures whose mod left with
ADR-0060, which the chassis ticket (#258) owns, and the four steam fluids, whose
rendering is #189's along with the rest of the two pack-owned fluids.

Only this pack's namespace is scanned. The installed mods produce dozens of the same warnings —
Railcraft's posts have no `particle` texture — and they are not ours to fix; an entry for each would
be a list nobody maintains.

## What it does not claim

Whether the Boiler's texture is the **right** texture is not mechanisable and is not claimed here.
That stays human on delivery. This asserts only that every asset the client was asked for resolved
to something.

## Running it

```
scripts/check-client-assets.py
```

It is in no batch, like the GameTest run and the datapack-load check: it boots the whole pack and
takes about a minute. Run it after adding or editing any model, blockstate, texture or item model
definition, and after any change to `scripts/build-item-definitions.py` or the asset generators.
