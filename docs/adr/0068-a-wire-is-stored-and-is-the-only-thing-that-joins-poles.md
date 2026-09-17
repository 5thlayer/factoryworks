---
status: accepted
supersedes: [281]
---

# A wire is stored, and is the only thing that joins poles

ADR-0062 stored no topology. Two poles were linked when they stood within reach of each other, and
#281 drew a wire for every such pair, working it out from where the poles stood. Factorio does it
differently: a pole wires itself when placed, and the player then adds or cuts wires by hand, so a
pair can stand in reach and still not be joined. #296 brings in the hand gesture, and that needs
somewhere to keep each wire.

## Decision

**A wire is stored state, and only a wire joins two poles.** Reach limits which wires may exist. It
does not create one. This replaces ADR-0062's "no stored topology" clause. The rest of ADR-0062
(wireless pull, the balance, the Steam Engine) stands.

We rejected a list of cut pairs laid over the old reach rule. It would store less, but a pole placed
later would join a pair the player had cut.

**Stored once, on the level.** The level's saved data holds each wire as an unordered pair of base
positions. There are no per-pole copies that could disagree. A wire names the base of each pole's
column, so adding or removing segments leaves the wire in place and only changes where it is drawn
from. Breaking a pole deletes its wires. Worlds saved before this decision have no wires, and nothing
migrates them (the pack is pre-release).

**Placement follows Factorio's rule.** A placed pole wires itself to every pole in reach that shares
no neighbour with it, so it adds no triangles. It adds at most 5 wires this way, taking the nearest
poles first and breaking ties by position. The rule and the cap come from Factorio's electric-system
wiki page and its `ElectricPolePrototype.auto_connect_up_to_n_wires` field, whose default is 5.
Factorio does not document which poles win when more than 5 qualify, so nearest-first is the pack's
choice. Wires made by hand have no cap, as in Factorio 2.0.7 and later. The Placement Preview shows
exactly the wires placement would add.

**The gesture.** A plain right-click with either Engineer's Pick on any segment of a pole sets the
first end, which is kept as a data component on the Pick. A right-click on a second pole within reach
wires the pair if it is unwired and cuts the wire if it is wired. Clicking the same pole cancels.
Clicking a block that is not a pole does nothing. The pending end clears when:

- the anchor pole breaks,
- the Pick leaves the main hand,
- the player changes dimension, or
- the player moves farther from the anchor than the anchor's wire reach plus the player's block
  interaction range.

The interaction range is added so that any pole the player can click, out of reach or not, is
answered by the second click. At the wire reach alone, walking over to an out-of-reach pole dropped
the end first, and the click silently started a new wire instead of being refused. A dropped end
plays a chain's break at the player, since nothing else shows it.

**Feedback is in the world, never text.** While an end is pending, a slack wire hangs from the
anchor's top segment to the Pick. Looking at another pole moves its end to that pole's top, as a
preview of the wire, tinted green when the click would wire, orange when it would cut (hiding the
stored wire it would replace) and red when it would be refused. In first person the end hangs just
ahead of the eye, since the hand's rope position is behind the camera. A made wire, a cut
wire and a refusal each play their own sound, from three different sound files (vanilla's dispenser
fail and tripwire attach are the same click), and a refusal changes nothing. Particles run along the
wire when it is made (`electric_spark`) or cut (`smoke`).

**The client is sent wires to draw, never networks.** A client receives the wires with an end in a
chunk it tracks, when it starts tracking that chunk, and afterwards each change to them. A wire whose
ends sit in two chunks goes with either one. We rejected sending a dimension's whole wire set because
its size grows with the base. The #281 renderer draws from these wires instead of working them out
from pole positions.

## Consequences

- `PoleLinks.linked` stops deciding networks. Networks are built from the stored wires, and reach
  is the rule for which wires may exist.
- #281's `PoleWires`, which works wires out from positions, is superseded.
- Tests without Minecraft: the placement rule, the toggle and its refusals, building networks from
  wires, and the wire set's codec round trip. A GameTest on a chain A–B–C with the generator on A and
  the machine on C: cutting A–B stops the machine, wiring it again powers it, and placing a pole in
  reach of the cut pair does not restore that wire.
- Not included, and recorded in the ledger as `planned`: placing a pole with no wires (Factorio's
  shift-place) and clearing all of a pole's wires at once.
