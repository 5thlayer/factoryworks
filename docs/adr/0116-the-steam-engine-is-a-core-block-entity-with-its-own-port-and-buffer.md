---
status: accepted
---

# The Steam Engine is a Core block entity with its own port and buffer

ADR-0077 made the Steam Engine a pack block whose block entity subclassed Oritech's `SteamEngineEntity`,
with a mixin putting Factorio's rate in its place. ADR-0109 removes Oritech, and ADR-0110 and ADR-0114
moved steam onto Pipeworks, so the engine's tank, its master and slave rows and its Oritech screen
plumbing no longer have a job. ADR-0115 moves the engine to Wireworks later, so what is built now has to
move as a package, not be rewritten.

**Decision (#594).**

- `SteamEngineBlockEntity` is a plain block entity. Its anchor is a Pipeworks `FluidPort`: it draws
  `SteamEngineSpec`'s 30 steam a second from the segment it stands in, and holds no tank of its own.
- Its energy is a buffer of one tick of output (450 FE), made at 900 kW and exposed through NeoForge's
  energy capability, extract-only. A pole draws it; the footprint's parts forward to the anchor.
- The footprint, placement, teardown and item are unchanged. The art is still Oritech's until #586
  draws the pack's own: the block entity is drawn by Oritech's `MachineRenderer`, the item model's
  parent is `oritech:item/steam_engine`, and the block model's textures are Oritech's. The block
  entity declares no animation, so the model stands still.
- `SteamEngineEntityMixin` and `FluidStacksCapacityAccessor` are removed, and `SteamEngineSpec` no
  longer takes Oritech's efficiency curve.

**Rows are segments.** ADR-0062's chaining of a row of engines through one master and its tank is gone.
Engines whose ports touch already share one steam segment, and each draws its own rate from it, so a row
of N makes N times 900 kW with no master, slave or scan.

**Considered: keep Oritech's chaining and efficiency curve** on the new block entity, as ADR-0062 kept
them on purpose. Rejected: chaining meant a master holding a row's tank and charge, a row scan, and a
port that had to hold the tank at the curve's peak and leave an empty engine's tank alone so the row
could form, all to reproduce a mechanic Factorio does not have. A shared segment already gives each
engine its steam, and Factorio's engine burns its rate or nothing.

**Consequences.**

- Off the peak the engine no longer wastes steam: Oritech's efficiency curve over the fill-driven speed
  is dropped, so an engine burns Factorio's rate or nothing, as Factorio's does.
- A pole reaching one engine's part draws that engine only, never a row's. The Jade line loses its
  "Chained" row.
- The engine is two steps from Wireworks: its steam is a `FluidPort` and its power a capability.
- This supersedes ADR-0077 and the chaining and speed curve of ADR-0062.
