---
status: accepted
---

# Rotate is a quarter-turn offset on the held stack, and vanilla placement reads it

Factorio's `R` turns what is about to be placed, and #365 decided the pending facing is stored on the
held stack so a Placement Plan stays a function of the item and the aimed spot (ADR-0069). What had to
be settled is what is stored and who reads it (#386).

**Decision (#386).** The stack carries a quarter-turn offset from the way the player looks, 0 to 3,
absent meaning 0. Rotate adds one, Reverse Rotate subtracts one, and the key press is sent to the
server, which writes the component; the client's preview reads it off the synced stack. The offset is
relative rather than Factorio's absolute compass direction, because Factorio's camera never turns and
the player's does: an absolute facing would make the same press mean a different turn depending on
where the player stood.

Nothing implements a contract to be turned. A mixin on `BlockPlaceContext` answers the look directions
turned by the held stack's offset, so the plan and the click, which both read that context, turn
together, and every block whose
placement reads the look -- vanilla's, the belts fork's, the pack's -- turns with no code of its own.
That is what lets the fork's belt tile, which cannot depend on core, be the first customer, and it
retires the tile's sneak-flips-facing stand-in (#383), which is offset 2. The gate widens from
`planetaryfactory:` blocks to the fork's, as it already did for the splitter's plan.

**Considered.** An interface in the fork that core dispatches to, or the fork depending on core. Both
put a contract where none is needed for the held half; a placed rotation is a different action with a
contract of its own, since what turning means there is the block's -- an underground belt swaps its
ends, a machine keeps its contents -- and that is the follow-up's decision, not this one's.

**Consequences.** The component stays on the stack until the last of it is placed, so a stack put
down and picked up again is still turned. A block that ignores the look direction ignores the key.
Placed-block rotation, footprint machines and the splitter's side priority are each their own ticket.

## Amended by Groundworks ADR 0003

The mechanism now lives in the Groundworks library as **Rotate the Plan** (#451). The held stack's
quarter turn is Groundworks' `quarter_turn` component, the look is turned by the library's mixins,
and `R` and `Shift+R` are the library's keys. What this ADR decides is unchanged: the turn is
relative to the look, it stays on the stack until the last item is placed, and every block whose
placement reads the look turns with no code of its own. The Pack keeps no keys, packet, component
or mixin of its own, and what is drawn is still the Pack's Opt-in (#450).
