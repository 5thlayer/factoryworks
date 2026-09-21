---
status: accepted
---

# The Assembling Machine stands on its model's footprint, not Factorio's 3x3

ADR-0059 says a pack machine's horizontal footprint is its Factorio entity's tile size, 1:1, and
names the Assembling Machine as 3x3. ADR-0071 says the machine reuses Oritech's assembler model and
ships no art. #326 found that those two cannot both hold: read off `oritech-2.0.0-exp6.jar`,
`assets/oritech/geckolib/models/block/models/assembler.geo.json` spans about three and three
quarter blocks wide -- one past the anchor on one side, two on the other -- one deep and two and a
half tall. Oritech's own assembler occupies a 1x2x2 of controller plus three Machine Cores and lets
the model overhang it on both sides.

**Decision.** `planetaryfactory:assembling_machine` occupies **four wide, one deep, two tall** --
the model's own extent in whole blocks, one lateral column on the side away from Oritech's cores
and two on theirs -- placed from one item as a footprint, the way
ADR-0069 places every multiblock. The offsets live in `core/machine/AssemblingMachineFootprint`, in
Oritech's controller-local frame, and are rotated into the world by Oritech's own
`Geometry.rotatePosition` with the block's facing, the same call `initMultiblock` makes on its core
positions. That is what keeps the blocks under the model the renderer draws.

**Considered: 3x3 per ADR-0059, with the model drawn unscaled inside it.** Rejected: the player
would collide with, and break, blocks where no machine is drawn, and a 4-wide model would overhang a
3-wide pad.

**Considered: 3x3, with the model scaled up to fill it.** Rejected for now: it means wrapping
Oritech's renderer to scale a model built for a different silhouette, and the result is a stretched
assembler, not a Factorio one. Fidelity to the 3x3 is better bought with the pack's own model, if
it is bought at all.

**What this does not change.** ADR-0059's rule stands for every other machine, and for this one the
moment it has art of its own. The height is still authored per machine rather than derived -- two
is the model's two and a half, where the half is a thin top the player does not collide with -- and
`data/pack/machine-heights.json`, which ADR-0059 names and which does not exist yet, gains this row
when it does.

**Consequences.**

- A Factorio player reading the factory's shape reads the Assembling Machine as smaller than it is
  in Factorio. That is the cost, accepted by the pack's owner on #326.
- Oritech's addon slots at lateral -1 and 2 fall inside this footprint, so the side slots move one
  block further out, to -2 and 3; the one behind the anchor is unchanged.
- The first draft was 2x1x4, from a bounding box that misread the model's axes; it was placed
  upright on its end, and corrected on #326's in-world check.
- The parts carry no block entity: a part's blockstate names its index and facing, and the anchor is
  recomputed from those. There is nothing to store and nothing to lose over a reload.
- A chemical plant or refinery built on another Oritech model faces the same question, and it is
  asked per machine rather than answered here.
