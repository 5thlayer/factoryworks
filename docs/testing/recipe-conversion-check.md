# The recipe conversion, and the two checks on it

`scripts/factorio-recipe-convert.py` turns `data/factorio/recipe.json` into recipe JSON on the
pack's own types under `kubejs/data/planetaryfactory/recipe/`. ADR-0026 is the decision; #87 is the
build, and #279 re-targeted it from GregTech's types, which left with ADR-0060.

## What decides what

Nothing is decided in the script. Five committed files hold the judgements:

| File | Answers |
| --- | --- |
| `data/factorio/recipe.json` | the corpus — 163 Nauvis pre-launch recipes, extracted (#72) |
| `data/pack/category-map.json` | which pack machine crafts a Factorio *category*, and which are `!`-routed |
| `data/pack/subgroup-owner.json` | which process crafts a *recipe*, per shelf and per recipe (#88) |
| `data/pack/item-map.json` | Factorio name → pack item, tag or fluid |
| `data/pack/recipe-overrides.json` | every knowing departure from Factorio, with its reason (ADR-0031) |

So a decision lands as a diff to a design document, never as a diff to the converter.

## The conversion rule

Nothing is scaled (#126, which rewrote ADR-0025's table). Item counts transfer 1:1, one Factorio
fluid unit is one millibucket, and `energy_required` seconds become ticks at ×20. `crafting_speed`
and power belong to the machine (ADR-0029), so neither appears in a recipe.

Two shapes come out, both read off the codec in `planetaryfactory_core`:

- **`planetaryfactory:assembling`** (`AssemblingRecipe`) for Factorio's three assembling
  categories: `category`, then optional `ingredients` (NeoForge's sized ingredient,
  `{"ingredient": "<id or #tag>", "count": n}`), `fluid_ingredients` (`{"ingredient", "amount"}`),
  `results` (an item template, `{"id", "count"}`), `fluid_results` (`{"id", "amount"}`), and `time`
  in ticks.
- **`planetaryfactory:smelting`** for the four smelts (#155).

The Chemical Plant and Oil Refinery have `recipe_type: null`, `blocked_by: 277`: their type and
shape are the block #277 chooses, and are read off that mod's codec when it does.

Factorio's source category rides on the emitted recipe as `category`, because
`category-map.json` collapses three crafting categories into one machine and the Personal
Assembler needs the distinction back — it is a filtered view of the Assembling Machine's recipes,
not a machine of its own (#125).

An item-map row may carry `components`, and the converter emits NeoForge's custom ingredient
(`"neoforge:ingredient_type": "neoforge:components"`) for it, and a `components` patch on a result.
The science packs are the case: they are Researchd research packs, so the player holds one
`researchd:research_pack` item told apart by a data component, not four items. Both rows are
`blocked_by: 251` until Researchd is on 26.1.2.

## What stops a recipe being emitted

In the order the converter checks: a `!`-routed category; a `not_emitted` shelf; a
`native_mechanic` shelf (in scope, supported by a mod mechanic that needs no recipe — the
eighteen barrel recipes, #93); a process whose machine is not registered yet (the converter names
the ticket); an `undecided` item-map row (the row names what decides it); a `blocked_by` item-map
row, whose target the game cannot load until that ticket lands; an override that says `skip`.

`--awaited` prints the recipes held back only by a ticket (an unregistered machine or a `blocked_by`
row), keyed by the id each will load under. A check asserting that an id is emitted reads it, so a
deferral is not a failure and a typo still is.

One more, found while building: **`smelting` has no vanilla shape above 1:1.** Vanilla's
`SmeltingRecipe` holds a bare `Ingredient` with no count field, while its result is an `ItemStack`
that has one — so 1:n emits and m:n cannot be written at all. Factorio's `steel-plate` is 5 iron
plates and `stone-brick` is 2 stone. #87 first resolved them as a pair split — `steel-plate` earned
a count-bearing type, `stone-brick` took a vanilla 1:1 shape — and ADR-0046 collapsed the split:

- **Both ride a count-bearing `planetaryfactory:smelting` recipe type** on the pack's three furnaces
  (#155), read alongside vanilla smelting. `steel-plate` is the only surviving alloy on Terra (#72)
  and was always going to earn that type; once it exists, `stone-brick` at its exact 2:1 ratio is a
  second recipe on it at no extra cost, which is cheaper than the fidelity loss the 1:1 shape bought.
- **Until #155 lands, both stay reported skips** — the converter reports them as "no vanilla shape"
  rather than emitting anything.

A Factorio name with **no item-map row at all** is none of those. It is a hard failure (#72): a
name nobody has looked at must never be quietly skipped.

## The two checks

**`tests/factorio/test_recipe_convert.py`** — static, no game. The item map covers the corpus and
nothing else; every row is a target or a recorded reason; every target names a namespace the pack
ships and every first-party target is registered in `kubejs/startup_scripts/` (items and blocks
both) or carries `blocked_by`, the ticket that will register it; every `undecided` row names the
ticket that decides it; every override has
a `reason` and names a real recipe; every emitted recipe resolves through the map and carries a
registered recipe type; and the emitted files are exactly what the converter emits today, because
generated output is never hand-edited.

**`scripts/check-datapack-load.py`** — the shape. The static check cannot prove it: the codec is
Java, and a wrong shape is one `Couldn't parse data file` line at datapack load and a recipe absent
from the manager. That failure is exactly what ADR-0026 was written about. The check boots the
GameTest server with every emitted recipe in its datapack and fails on any rejection not listed in
its `EXPECTED`; the listed ones are recipes naming a KubeJS-registered item, which that server does
not load. Whether the recipes appear in EMI and the Personal Assembler plans them is a human on
delivery.

### Two rules the map earns by having been wrong

**`undecided` must name a ticket.** #87 found 40 rows — a quarter of the map — sitting `undecided`
with a note pointing at prose rather than at an owner. Eight of them cited ADR-0030 as the thing
that *might* decide them, and ADR-0030 was already accepted and had decided them. A status that
means "someone will decide this" needs to say who.

**`blocked_by` is the narrow escape for a row whose target the game cannot load yet.** KubeJS
cannot register a furnace with a fuel slot or a chunk-charting block, so those belong to
`planetaryfactory_core` and arrive with their ticket, and the check fails once the item *is*
registered — so the map cannot keep pointing at a ticket that closed. Since ADR-0060 it also covers
a borrowed row whose mod is not on 26.1.2 (#277, #251): the target stays as the standing proposal, and
`tests/pack/test_item_map.py` asserts every such row is in its `DEFERRED` with the same ticket.

## Re-running it

    scripts/factorio-recipe-convert.py            # rewrite the emitted recipes
    scripts/factorio-recipe-convert.py --check    # fail if what is on disk is stale
    scripts/factorio-recipe-convert.py --awaited  # the recipes waiting on a ticket, as JSON
    tests/factorio/test_recipe_convert.py         # the static check, which runs --check too
