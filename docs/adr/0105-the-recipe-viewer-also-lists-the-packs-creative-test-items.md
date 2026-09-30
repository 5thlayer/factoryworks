---
status: accepted
supersedes: [173]
---

# The recipe viewer also lists the Pack's creative test items

ADR-0088 made EMI's index an allowlist of what a player can come to hold. That hides every item that
exists only for testing, such as the creative electric pole, even with EMI's cheat mode on, because
cheat mode changes what a click does, not what the index holds. Showing creative items in the recipe
viewer is common practice in modpacks, and testing a build is something a player does, not a
cheat: an Alpha Tester trying a power layout looks for the creative pole in EMI (#539).

**Decision (#539).** The index lists every Obtainable item, as ADR-0088 derives it, **plus** each
of the Pack's own creative test items. These are a hand-kept list, one row per item with the reason
it is there. Only items the Pack registers are listed, and only ones that test a mechanic the Pack
ships. Being listed does not make an item Obtainable: no pack recipe may take one as an ingredient.

**Considered.** Listing every jar's creative items: they are untested against the Pack's rules, and
a creative block from Oritech or Railcraft could hand a tester something that bypasses a mechanic
the Pack authored. Leaving the index as it was: testers would find the creative items only through
the vanilla creative tabs or `/give`, which is not where a recipe-viewer player looks.

**Consequences.** The index is no longer only "what exists to be had": a creative-listed item
appears in it as well. A player recognises it by its name, and the list stays short because each
row has to name what it tests.
