---
status: accepted
supersedes: [282]
---

# The Steam Engine is a pack block on Oritech's entity

ADR-0062 took Oritech's Steam Engine whole: its block, its chaining, its fill-driven speed and its
efficiency curve, with a mixin putting Factorio's rate in place of Oritech's. That left the block
Oritech's multiblock, which the player assembles by placing three Machine Cores and right-clicking,
and which breaks into a controller and loose cores. Every other multiblock the pack ships is placed
from one item and breaks as one (ADR-0069, ADR-0072).

**Decision (#352).** The Steam Engine is `planetaryfactory:steam_engine`, a `planetaryfactory_core` block
whose block entity subclasses Oritech's `SteamEngineEntity`, on the Assembling Machine's pattern
(ADR-0071). It is placed as its whole 2x1x2 footprint from one item, its other three blocks are
invisible parts with no block entity, every block of it forwards the anchor's faces and its Jade
line, and breaking any block removes all four and drops one engine. ADR-0062's arithmetic, chaining
and curve are unchanged: the mixin reaches the subclass through inheritance. Oritech's own engine is
recipe-removed (hiding it in EMI is #173's, per ADR-0061), and the `steam-engine` item-map row names the pack block.

**Considered: keep Oritech's block and place its cores pre-linked.** The item would put down the
controller and three `machine_core` blocks already assembled, and a mixin would make breaking a core
break the machine. Rejected: the cores keep their own item and loot table, so every break path has
to be patched against a second drop, and the change lives entirely in mixins into Oritech's core
and controller code rather than in blocks the pack owns.

**Consequences.**

- The footprint, part block, teardown, placement plan, energy-owner resolution, capability
  forwarding and Jade redirect are one seam shared with the Assembling Machine and its tiers
  (ADR-0075), not a copy per machine.
- Oritech's row scan finds engines by block entity *type*, which the subclass does not have. The
  mixin already replaces `setupMaster` whole, so the pack's scan resolves a part to its anchor and
  matches the subclass.
- A slave's Jade line shows its Master Engine's steam, output and charge, since its own tank and
  buffer are empty by design.
