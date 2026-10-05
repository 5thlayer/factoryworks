---
status: superseded by ADR-0118
---

# The Assembling Machine stands on its model's footprint, not Factorio's 3x3

ADR-0059 says a pack machine's horizontal footprint is its Factorio entity's tile size, 1:1, and
names the Assembling Machine as 3x3. ADR-0071 says the machine reuses Oritech's assembler model and
ships no art. #326 found that those two cannot both hold: Oritech's model is drawn over Oritech's
own assembler, a 2x1x2 of controller plus three Machine Cores, and cannot fill a 3x3.

**Decision.** `factoryworks:assembling_machine` occupies **two wide, one deep, two tall** --
exactly Oritech's controller-and-cores layout, `(0,0,0)` plus `AssemblerBlockEntity.getCorePositions()`
-- placed from one item as a footprint, the way ADR-0069 places every multiblock. The offsets live
in `core/machine/AssemblingMachineFootprint`, in Oritech's controller-local frame, and are rotated
into the world by Oritech's own `Geometry.rotatePosition` with the block's facing, the same call
`initMultiblock` makes on its core positions. That is what keeps the blocks under the model the
renderer draws, and it keeps Oritech's addon slots, beside and behind, where Oritech put them.

**Considered: 3x3 per ADR-0059, with the model drawn unscaled inside it.** Rejected: the player
would collide with, and break, blocks where no machine is drawn.

**Considered: 3x3, with the model scaled up to fill it.** Rejected for now: it means wrapping
Oritech's renderer to scale a model built for a different silhouette, and the result is a stretched
assembler, not a Factorio one. Fidelity to the 3x3 is better bought with the pack's own model, if
it is bought at all.

**What this does not change.** ADR-0059's rule stands for every other machine, and for this one the
moment it has art of its own. The height is still authored per machine rather than derived -- two,
Oritech's -- and
`data/pack/machine-heights.json`, which ADR-0059 names and which does not exist yet, gains this row
when it does.

**Consequences.**

- A Factorio player reading the factory's shape reads the Assembling Machine as smaller than it is
  in Factorio. That is the cost, accepted by the pack's owner on #326.
- Two drafts were wrong, both from reading the model's bounding box instead of Oritech's own
  layout: 2x1x4, then 4x1x2. #326's in-world check corrected both. The layout Oritech already
  ships was the answer, and the geometry file was the wrong thing to measure.
- The parts carry no block entity: a part's blockstate names its index and facing, and the anchor is
  recomputed from those. There is nothing to store and nothing to lose over a reload.
- A chemical plant or refinery built on another Oritech model faces the same question, and it is
  asked per machine rather than answered here.
