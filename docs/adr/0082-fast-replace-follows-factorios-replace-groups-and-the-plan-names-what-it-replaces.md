---
status: accepted
---

# Fast Replace follows Factorio's replace groups, and the Placement Plan names what it replaces

Fast Replace is placing a block over a placed block of another tier and swapping it in place (#299).
Two things had to be settled: which blocks may replace which, and how the preview and the click
agree on a replace.

**Decision (#299).** A block replaces another only when both are in the same **Replace Group**. The
groups are Factorio's `fast_replaceable_group`, extracted into the corpus and copied into a resource
the mod reads. They are not derived from the pack's footprints. So the small and medium poles are one
group and the substation is alone, even though all three are one block wide in the pack today. The
three furnaces are one group, and so are the three Assembling Machine tiers.

The Placement Plan gains a field naming the placed blocks it replaces. This extends ADR-0069. The
Placement Preview draws a replace in a colour of its own, from that field, and the click executes the
same plan, so a preview that promises a replace cannot disagree with the click. A replace plan is
refused as a whole, the way a multiblock plan is.

**Considered.** Grouping by footprint would have let the substation replace a small pole, because
the pack draws every pole tier one block wide. We rejected it. Factorio keeps the substation apart
even from the big pole, which is also 2x2, so the group is data rather than a consequence of size.
The pack's single-block substation is the simplification that should give way, and #385 makes it a
2x2 column.

**Consequences.** A group that isn't in the corpus doesn't exist, and a new tiered block replaces
nothing until its row carries a group. The substation cannot be reached by Fast Replace until #385.
Belts, splitters and loaders are Factorio's `transport-belt` and `loader` groups, and #384 takes them
on.
