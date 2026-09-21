---
status: accepted
---

# The Assembling Machine's input filter sits on Oritech's inventory, with Oritech's input mode pinned

ADR-0073 says an input slot the Held recipe does not use accepts nothing, on the item face #329
builds, and the pack's rule is that every refusing face is a `GuardedResourceHandler`. #329 named the
risk: Oritech's machine inventory routes inserts by its own input mode, and a second routing layer
over it may not compose. It does not, and that was measured rather than assumed.

Read off the 2.0.0-exp6 jar with `javap`, `MachineBlockEntity$MachineInventoryStorage` overrides the
**per-slot** insert as well as the slot-less one. In `FILL_EVENLY` mode, an insert naming slot 0
calls the slot-less insert, which spreads the stack over every input slot. The guard's slot-less
loop asks the pack's per-slot filter, the filter says slot 0 may take the item, and Oritech then
puts it in slots 1 to 3 as well. With `FILL_LEFT_TO_RIGHT`, Oritech's default, the per-slot insert
falls through to `ItemStacksResourceHandler`'s, which writes one slot and routes nothing.

**Decision.** The face (`AssemblingMachineItemHandler`) is a `GuardedResourceHandler` over Oritech's
inventory, and the machine **pins the input mode** to `FILL_LEFT_TO_RIGHT`: `cycleInputMode` is
overridden to do nothing, and `loadAdditional` resets the mode Oritech reads back from the save. That
leaves one routing layer, the pack's. The slot a given ingredient goes in is `AssemblingInputSlots`,
the rule #334's ghosts will be drawn from: the `n`th ingredient goes in the `n`th slot, and a slot
past the last ingredient takes nothing. A machine with no Held recipe takes nothing. Only the
output slot can be extracted from.

**Considered: the machine keeps its own inventory and does not use Oritech's.** Rejected. The
inventory is what Oritech's save format, its client sync, the pack's menu and the craft cycle all
read. Replacing it is a rewrite of all four to remove one field write that a one-line override
already prevents.

**Considered: wrap Oritech's inventory and leave the mode alone.** Rejected, because this is the
failure above. The pack's screen has no mode button, but Oritech's mode packet is addressed to any
Oritech machine at a position, so the mode can still change.

**Consequences.**

- `inputModeStaysPinned` in `AssemblingMachineTests` fails if the override is dropped: the boiler
  recipe's pipes then spread over all four slots. An Oritech update that adds a new route to the
  field, or a new mode, gets past the pin, and that GameTest is where it will show.
- **One route is known and left open: Oritech's Inventory Proxy addon.** It reaches the machine
  through `getInventoryForAddon`, which returns the raw inventory, and that method's type is a
  `StacksResourceHandler` the guarded face is not. A proxy beside the machine would insert past
  the filter. It is latent, because `recipe_survivors.js` admits no Oritech recipe, so no proxy can
  be crafted. Admitting the addons is the moment to close it.
- A fifth ingredient has no slot. `tests/factorio/test_recipe_convert.py` fails an emitted
  assembling recipe with more item ingredients than `AssemblingInputSlots.INPUTS`, which it reads
  out of the source.
- The player's hand is filtered by the same rule on the server. The client has no recipe manager,
  so the menu's opening packet carries each recipe's slot ingredients and the client's slot asks
  the same rule of them (#334): a wrong item is refused in the hand, not placed and put back.
