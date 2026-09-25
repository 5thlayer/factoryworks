---
status: accepted
---

# The recipe viewer lists only what is Obtainable, derived from a committed jar extract

ADR-0034's sweep removes every recipe the pack does not admit, but EMI's index still lists every
item every jar registers: nine vanilla wooden stairs where Terra grows three species, and about
1,500 FTB Materials, Railcraft and Oritech items nothing makes. A player reads the index as a list
of what exists to be had, and builds plans against it (#173).

**Decision (#173).** EMI's index lists only **Obtainable** items and fluids, as an allowlist:
a filter matching every id empties the index and `added` names each Obtainable stack by bare id. Obtainable is
derived, never typed, from five sources: a pack recipe's output, the starting kit, a drop of a
block the live worldgen places, a drop of a mob the live biomes spawn, and a mechanic that produces
it with no recipe (a filled bucket, a Pumpjack's crude, a Boiler's steam, an outfield disc's ore).
Only the last is a hand-kept list, and each row names its reason and an open ticket; a row the
derivation already covers is stale and fails. A parked body's worldgen counts once it is live.

Loot is resolved to a fixpoint rather than flattened. A `match_tool` condition passes only if a
tool satisfying it is itself Obtainable, and a condition on the killer or the damage source fails.
Chance and `killed_by_player` pass. A grass block therefore drops dirt and not itself, since the
Pick has no silk touch, and leaves drop no leaves while shears have no recipe.

What the jars hold -- every item and fluid id, every block and entity loot table, every placed and
configured feature -- is extracted once into a committed corpus, the way Factorio's dump is. The
generator reads only committed files; the extractor re-runs after a jar update, so a new jar's
items arrive as a diff to review, hidden until something makes them Obtainable.

Every pack recipe's ingredient must be Obtainable as well. One that is not is a chain the player
can see and never complete, and each current case is an allowlist entry naming its ticket.

**Considered.** A hide-list of what is unobtainable: every item of a new jar would show until the
list was regenerated, the failure ADR-0034 rejected for recipes. Reading loot tables with their
conditions ignored: it lists grass blocks and leaves the player cannot hold, which is the lie this
removes. Reading vanilla and the mod jars from disk at check time: neither is in the repo, so the
check would be as true as whatever happened to be installed.

**Consequences.** Diamonds, netherite and every other vanilla item Terra does not yield leave the
index. The index shows only what a player can come to hold, so a missing item is a signal and not
noise. Tag cycling and recipes naming a hidden item are unchanged; the sweep removes those recipes
already. Component variants are not listed apart until something in the pack makes one.
