---
status: accepted
supersedes: [95, 160]
---

# The inventory screen is the Personal Assembler

ADR-0038 and #95 made the Personal Assembler a panel opened from the inventory screen, and #160 built
it: a menu of its own whose only jobs were to list the queue, carry a cancel button per row, and be a
`MenuType` so EMI would offer **Fill Recipe**. Once #288 moved queueing into Fill Recipe itself and the
hotbar overlay showed the queue all the time, the panel was a second inventory screen with a list on
it (#290).

**Decision.** The inventory screen is the Assembler. The panel, its inventory tab and its key are gone.

- **Fill Recipe** is registered against the player's inventory, which EMI keys under a null menu type.
  ADR-0065's clicks are unchanged; only the screen they apply to moved. EMI's own 2x2 handler shares
  the key but claims only vanilla's crafting category, and the hand set is the pack's assembling one.
- **The queue** is drawn in the area the removed 2x2 grid left blank, in the hotbar overlay's row
  format, two rows and a "+N more" line, as Factorio does. The overlay hides while the inventory is
  open.
- **Cancel is Factorio's.** On either icon of a row: left cancels 1, right 5, Shift all, of the row's
  final item. Fewer left than asked cancels what is there; any other button cancels nothing. The step
  icon cancels final items, never the intermediate alone, which would leave a row that cannot finish.
- **The Crafting Plan's Close** returns to the inventory screen.

**Partial cancel re-resolves the rest.** A row is a resolved plan, and how many gears three belts want
is the resolver's question, not the queue's. So the row is refunded whole and what is left is resolved
again against the refunded inventory, keeping the row's id and place. That reuses the intermediates
already made, refunds exactly the cancelled crafts' share, and keeps the craft under way's progress
when the new plan starts on the same recipe. The queue takes the resolver as a seam, so this is a unit
test. This amends ADR-0038's "the plan is the unit of cancellation, never re-resolved": a plan is still
never re-resolved while it runs, only when the player shrinks it.

**Rejected.**

- **Shrinking a row's steps in place.** It needs each intermediate step's share of the cancelled crafts,
  which the flattened plan no longer records, and a tag ingredient drawn from two items makes the shares
  uneven.
- **A side panel beside the inventory.** Deferred rather than rejected: if two rows prove too few, it is
  the follow-up.

**Consequences.**

- A partial cancel whose refund did not all fit cancels the row whole, since the dropped part can no
  longer pay for the rest.
- A partial cancel the resolver cannot plan again -- a recipe locked since, a tag ingredient now
  resolving to an item the refund does not hold -- leaves the row exactly as it was and cancels nothing.
- `CancelClickTest` holds the click rule and `AssemblerQueueTest` the partial cancel. That the queue
  draws in the blank area, the icons take the clicks, and Fill Recipe works on the inventory screen is a
  world load.
