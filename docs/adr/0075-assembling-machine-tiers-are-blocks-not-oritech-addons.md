---
status: superseded by ADR-0118
---

# Assembling Machine tiers are blocks, not Oritech's addons

Factorio climbs from `assembling-machine-1` to `-3` by crafting a new machine: speed 0.5, 0.75 and
1.25, draw 75, 150 and 375 kW, and from tier 2 a fluid input and the `crafting-with-fluid`
category. Oritech has a different ladder: one block and a growing ring of addons. Its speed addon
cuts the duration to 0.5 at 1.2 times the energy, its efficiency addon cuts the energy to 0.8, and
`machine_core_N` blocks raise how many addons fit. #277 left the choice between the two to #295, and
ADR-0071 expected Oritech's ladder to win, because tier 1 already takes addons.

**Decision.** Tiers 2 and 3 are further `factoryworks_core` blocks of the same Assembling
Machine subclass, one per tier, each with its own Factorio speed, draw and categories, and each
crafted from Factorio's own recipe. Oritech's addons stay live on all three, with Oritech's
meaning, and are not part of the ladder. What an addon is in Factorio's terms is #120's question.

The tiers are told apart by Oritech's machine paint: tier 1 `ORANGE`, tier 2 `DIAMOND`, tier 3
`INDUSTRIAL`, echoing Factorio's brown, blue and yellow. The paint is **locked**: a tier refuses
recoloring, because a repainted tier 1 would claim to be tier 3 at a glance.

**Considered: the tiers are Oritech's addons (`native_mechanic`).** One block, and Factorio's tier 2
and 3 recipes are never emitted. Rejected. An addon's trade, more speed for more energy, is
Factorio's speed *module*, not its tier. Spending the addons on the tier ladder would fold two
Factorio mechanics into one and leave modules with nothing to be. It would also have put tier 2's
fluid crafting on an item that has no fluid of its own.

**Considered: core tiers.** Three `machine_core_N` blocks at Factorio's speeds, with addons removed.
Rejected. It is a tier ladder spelled in Oritech's vocabulary, with a multiblock the player must
assemble in place of the one item Factorio places, and it closes #120 by removing its only
candidate.

**Considered: a pack texture per tier.** No paint is Factorio's deeper blue. Deferred, not
rejected: it needs a renderer seam beside Oritech's, and whether `DIAMOND` reads wrong in-world is
a judgment best made after seeing it.

**Consequences.**

- Tiers 2 and 3 craft with a fluid. They have one input tank of Factorio's 1,000 mB, and no output
  tank until a recipe with a fluid result is emitted (barrel emptying). The fluid face follows
  ADR-0074's rule: it accepts only a fluid the Held recipe names, from any block of the footprint,
  and never gives the input tank back. Tier 1 has no fluid face.
- Changing the Held recipe hands the items back and **voids** the tank, as Factorio does. Keeping
  the fluid would strand it behind a face that refuses extraction.
- Oritech's addons are live, unpriced, on every tier, until #120 decides them.
- Tier 3 is registered but cannot be crafted yet. Factorio's recipe takes four speed modules, and
  `speed-module` is `undecided` on #120, so the converter records the recipe as a skip rather than
  emitting it.
- Placing a higher tier over a lower one to upgrade it is #299's, alongside the pole and furnace
  ladders.
