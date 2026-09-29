---
status: accepted
---

# Hand and assembling recipes are the pack's own `factoryworks:assembling` type

ADR-0026 put Factorio's three assembling categories on `gtceu:assembling`, and ADR-0038 made the
Personal Assembler's hand set a predicate over that type. ADR-0060 removed GregTech, so every
emitted recipe on it was a file nothing read (#279).

**Decision.** The converter emits Factorio's `crafting`, `advanced-crafting` and
`crafting-with-fluid` recipes onto `factoryworks:assembling`, a `factoryworks_core` recipe
type (`AssemblingRecipe`). Each recipe carries its Factorio `category`. The hand set is still a
predicate over this type: `category == crafting`, no fluids. The Assembling Machine that #277
chooses reads this type rather than bringing its own. The Chemical Plant and Oil Refinery stay
`recipe_type: null` until #277 picks their blocks. An item-map row whose mod is not on 26.1.2
carries `blocked_by`, and the converter skips every recipe that touches it.

**Considered: vanilla `minecraft:crafting_shaped`/`shapeless`.** Rejected for three reasons. A 3x3
grid cannot hold Factorio's counts (for example, 10 plates, 5 circuits and 3 gears). A grid recipe
carries no category, so the hand set loses its predicate. And #97 forbids emitting grid recipes
because no block in the pack runs them.

**Considered: waiting for #277's Oritech type.** Rejected. Oritech's assembler recipe is one of
that mod's types, and its shape is not ours to extend with a category. The hand set would also stay
empty until a machine decision that has nothing to do with hand crafting.

**Consequences.** Recipe ids are unchanged (`factoryworks:assembling/<name>`), so research
unlocks keep their keys. GregTech's file-path invariant (#87) no longer applies. The JSON shape is
NeoForge's own codecs, composed: sized ingredients and item/fluid templates.
