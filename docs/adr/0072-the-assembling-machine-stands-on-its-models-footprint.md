---
status: accepted
---

# The Assembling Machine stands on its model's footprint, not Factorio's 3x3

ADR-0059 says a pack machine's horizontal footprint is its Factorio entity's tile size, 1:1, and
names the Assembling Machine as 3x3. ADR-0071 says the machine reuses Oritech's assembler model and
ships no art. #326 found that those two cannot both hold: read off `oritech-2.0.0-exp6.jar`,
`assets/oritech/geckolib/models/block/models/assembler.geo.json` spans about two blocks wide, one
deep and three and a half tall. Oritech's own assembler occupies a 1x2x2 of controller plus three
Machine Cores and lets the model overhang it.

**Decision.** `planetaryfactory:assembling_machine` occupies **two wide, one deep, four tall** --
the model's own extent, rounded up to whole blocks -- placed from one item as a footprint, the way
ADR-0069 places every multiblock. The offsets live in `core/machine/AssemblingMachineFootprint`, in
Oritech's controller-local frame, and are rotated into the world by Oritech's own
`Geometry.rotatePosition` with the block's facing, the same call `initMultiblock` makes on its core
positions. That is what keeps the blocks under the model the renderer draws.

**Considered: 3x3 per ADR-0059, with the model drawn unscaled inside it.** Rejected: the player
would collide with, and break, blocks where no machine is drawn, and the machine would read as a
2-wide object on a 3-wide pad.

**Considered: 3x3, with the model scaled up to fill it.** Rejected for now: it means wrapping
Oritech's renderer to scale a model built for a different silhouette, and the result is a stretched
assembler, not a Factorio one. Fidelity to the 3x3 is better bought with the pack's own model, if
it is bought at all.

**What this does not change.** ADR-0059's rule stands for every other machine, and for this one the
moment it has art of its own. The height is still authored per machine rather than derived -- four
is the model's three and a half, rounded up so nothing overhangs its footprint -- and
`data/pack/machine-heights.json`, which ADR-0059 names and which does not exist yet, gains this row
when it does.

**Consequences.**

- A Factorio player reading the factory's shape reads the Assembling Machine as smaller than it is
  in Factorio. That is the cost, accepted by the pack's owner on #326.
- The parts carry no block entity: a part's blockstate names its index and facing, and the anchor is
  recomputed from those. There is nothing to store and nothing to lose over a reload.
- A chemical plant or refinery built on another Oritech model faces the same question, and it is
  asked per machine rather than answered here.
