---
status: accepted
---

# Loaders are the inserter chain, and splitters are tiered

ADR-0060 replaces the inserter with the **loader**. Its tiers 2 to 4 draw FE per item, anchored on
`fast-inserter` for tier 2 and `bulk-inserter` for tiers 3 and 4. That left open where a loader
comes from, since Factorio's own loaders are hidden and have no recipe.

**Decision.** There are four loader tiers, each crafted from the corpus's recipe for the matching
rung of the inserter chain, and each unlocked by that inserter's technology:

| loader | recipe of | items/s | power |
| --- | --- | --- | --- |
| 1 | `burner-inserter` | 15 | none |
| 2 | `inserter` | 30 | per item, as `inserter` |
| 3 | `fast-inserter` | 45 | per item, as `fast-inserter` |
| 4 | `bulk-inserter` | 60 | per item, as `bulk-inserter` |

A loader's energy per item is derived from the swing of the inserter it is made from, which
**amends ADR-0060's anchors**. `long-handed-inserter` is `excluded`, because there is no swing arm
to lengthen.

Splitters are tiered as well, four of them from Factorio's four splitter recipes. A belt, a loader
and a splitter each cap only their own flow, and any mix of tiers may be joined, so a line runs at
its slowest piece.

Each half of a splitter is a block of belt of the splitter's tier, as each side of Factorio's is: one
lane of a Factorio splitter is 128 positions in and 128 out, the 256 of a straight belt block. A
half holds eight items and moves them at its tier's belt speed, and an item changes side at the
splitter's midline. Factorio's 51-position input buffer is left out, because it aligns items
across two lanes and the pack's belts have one (#373).

**Considered: a loader takes its belt's tier.** One loader block, with its speed and draw read from
the belt attached to it. Rejected, because every inserter technology would then buy nothing, and
the loader would have no recipe the corpus can author.

**Considered: loaders on the belt ladder.** Loader tier *n* unlocks with belt tier *n*. Rejected,
because Factorio has no loader recipe to convert, so all four would be pack-authored and the
inserter chain would be emitted for nothing.

**Considered: one splitter for every tier.** The fork's splitter hands items from one belt to the
next, so a single block could run at whatever belts are attached. Rejected, because a green-circuit
splitter would then carry express throughput, and `logistics-2` and `-3` would each unlock one
fewer thing.

**Consequences.**

- Tier-4 loaders and splitters have recipes before the tier-4 belt does, since
  `turbo-transport-belt`'s recipe is outside the corpus. That is harmless, because a loader cannot
  run faster than its belt.
- `turbo-splitter` is registered but has no recipe, the same as the turbo belt.
