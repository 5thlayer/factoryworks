---
status: accepted
---

# The Assembling Machine holds a player-set recipe, and the pack owns its craft cycle

ADR-0060 spends one sentence on the machines: they are `planetaryfactory_core` subclasses of
Oritech's, and an assembler, chemical plant or refinery holds a **player-set recipe** -- set once,
inputs filtered to it, no lookup. #277 spent that sentence on *which block*
(`assembling-machine-1` -> `planetaryfactory:assembling_machine`) and left the mechanism unspent, so
nothing was buildable and nothing was filed. What the sentence costs was never priced, and the
price is the whole decision.

**The fact that makes the one-sentence version unbuildable.** `MachineBlockEntity`'s entire craft
path is typed to `OritechRecipe` -- `currentRecipe` is a field of that type, and
`loadRecipeFromInput`, `findActiveRecipe`, `checkCraftingFinished`, `canOutputRecipe`,
`getCraftingResults` and `getRecipeDuration` all name it. And `OritechRecipe.itemInputs` is
`List<Ingredient>` with **one entry per input slot, one unit each**: Oritech's own shipped data
writes a count of 2 as the ingredient repeated twice, in a list exactly as long as the machine has
slots. `planetaryfactory:assembling` (ADR-0063) carries `List<SizedIngredient>` with Factorio's
counts, and the pack's emitted recipes reach 29 ingredient *units* across 4 distinct ingredients.
**The pack's recipe type cannot be cast, adapted or widened into the base class's.** Removing the
lookup is one protected method; everything downstream of it is the cost. Recorded here because it
is the reason this ADR exists, and it is written down nowhere else but
`docs/research/oritech-assembler-subclass.md`.

**Decision.** `planetaryfactory:assembling_machine` extends `MultiblockMachineEntity` and
**overrides `workTick()` whole**, running the pack's own craft cycle against a **Held recipe**: a
`planetaryfactory:assembling` recipe id the player sets, stored in block-entity NBT and resolved
lazily. The machine keeps Oritech's energy storage, inventory, addon system, GeckoLib model and
multiblock base; it keeps none of Oritech's recipe machinery. Four input slots, which covers 138 of
the corpus's 140 recipes in Factorio's three crafting categories. The recipe is set through a
pack-owned screen widget and through EMI's Fill Recipe. Inputs are filtered to the Held recipe
through `core/transfer/GuardedResourceHandler`. Duration and energy are the pack's, from
`energy_required x crafting_speed` (ADR-0029), with Oritech's speed and throughput addons live on
top. A machine whose Held recipe it cannot run -- unresearched, unfed, or with a full output --
holds it and idles, and never clears it silently.

**Considered: an adapter, synthesising an `OritechRecipe` that carries only `time`.** Rejected. It
keeps more of the base class working, but every `OritechRecipe`-typed signature becomes a fiction
the subclass maintains, and the synthetic recipe leaks through the public `getCurrentRecipe()` into
the screen and `getRecipeDuration()`. Whether it survives `OritechRecipe.EMPTY` and `isEmpty()`
comparisons on paths nobody has disassembled is *undetermined* -- an unbounded cost traded for a
bounded one. `workTick`'s transaction discipline is bounded, is written down, and is already this
pack's idiom: `core/energy/LongSnapshotJournal` and `tests/pack/test_energy_faces.py` exist to
police it, and `core/mixin/oritech/SteamEngineEntityMixin` already replaces `tickMaster` and
`setupMaster` whole (#292) on the same reasoning.

**Considered: a block entity owing Oritech nothing.** Rejected: it loses the model, the addon
system, the screen and the multiblock, which is everything ADR-0060 chose Oritech for.

**Considered: keeping Oritech's controller-plus-cores placement.** Rejected. Factorio's assembler is
one item placed as a footprint, which is ADR-0069's plan and the rig's idiom; a pack where one
machine places as a footprint and another has the player stack hull blocks by hand is two building
gestures for one idea. `getCorePositions()` returns empty and `isAssembled()` is overridden to
`true` -- which is what `MachineBlockEntity` itself returns, before `MultiblockMachineEntity`
overrides it with an unguarded `state.getValue(ASSEMBLED)`. The block still extends
`MultiblockMachine` so the property is declared and nothing can throw. **Not** the bare
empty-core-list route: `initMultiblock` over an empty list computes `0.0f / 0` and sets a **NaN**
core quality.

**Considered: Oritech as an optional dependency, the way the mixins are.** Rejected. Every pack
mixin on Oritech is `required: false`, so a renamed target is a log line and one uncalibrated
engine. A subclass has no such escape hatch -- it is a compile-time and load-time dependency on
`MultiblockMachineEntity`'s constructor. Oritech is *the* tech mod as of ADR-0060 and the pack does
not function without it, so the dependency is accepted and **the Oritech version is pinned**, the
way KubeJS is. 2.0.0-exp6 is an experimental build, which is the part that has to be re-checked on
every bump.

**Consequences.**

- Oritech's own `oritech:assembler` is a rival for a Factorio row the pack now fills, so it is
  recipe-removed and hidden, which is what CONTEXT.md's **Plate** entry already does to a rival
  material form. #173 owns the hiding half. Until then it is the defect this ADR was opened over: a
  registered, visible, uncraftable, inert block.
- The chemical plant and the refinery inherit the surface, which is written at `MachineBlockEntity`'s
  level. They are not built here: `category-map.json` has them on `recipe_type: null`, so there is
  no recipe for them to hold, and #258 owns that.
- `#237` and `#238` are not re-pointed, because **#237's check never existed**: commit `507be72`
  closed both, recording that "#237's invariant is false as stated" under ADR-0056's slot locking.
  The failure they guarded against -- a recipe going silently unreachable -- survives the change of
  mechanism in a new shape, so one fresh check replaces them, in the GameTest: that every emitted
  `planetaryfactory:assembling` recipe is selectable, and that a Held recipe survives a world reload
  and still crafts. It is a world load rather than a static check because selectability is a
  question about the server's recipe manager, not about the emitted files. It also guards
  `ItemStack.CODEC`'s binding trap (`tests/pack/test_load_codecs.py`): a dropped field does not
  crash, it empties every assembler in the world over a reload.
- Oritech's `MachineInventoryStorage` already overrides all four `insert` overloads to route by
  `InventoryInputMode`, so a guarded face puts two routing layers on one handler. Whether the
  guard's slot-by-slot loop composes with `FILL_EVENLY` is a **world load**, not a reading, and is
  one of the GameTests.
- The Steam Engine is Oritech's block, placed Oritech's way, and the pack registers nothing for it.
  Bringing it onto the pack's placement is a separate ticket this one unblocks -- it means
  registering a pack Steam Engine block, not writing an adapter.
- #295 may collapse: if Oritech's addons stay live and tier 1 is the only machine block, then
  `native_mechanic` for tiers 2 and 3 is most of the way answered here.
