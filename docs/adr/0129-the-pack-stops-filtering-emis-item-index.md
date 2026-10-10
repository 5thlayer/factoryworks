---
status: accepted
---

# The Pack stops filtering EMI's item index

ADR-0088 made EMI's index an allowlist of what a player can come to hold, derived from the Pack's
closed Factorio recipe set on Terra. That premise is gone: the stock-recipe sweep was removed and
`vanillaRecipes` turned on (#603), Terra was removed (#602), and every mod plays alone in vanilla
(ADR-0127). Since #603 the allowlist hid items players can craft, among them stairs, glass, ladders,
the concretes and `ftbfiltersystem:smart_filter`.

**Decision (#670).** The Pack ships no EMI index filter. EMI lists every item the installed mods
register, and vanilla's recipes show through Craftworks. The derivation goes with it: the recipe,
kit, mechanic-row and worldgen walk, `data/pack/mechanic-obtainable.json` and
`data/pack/creative-listed.json`. "Where it is found" goes too: its only input was the worldgen
walk, and its `sources.json` held no stack once Terra left. `data/jars/` and
`scripts/jar-registry-extract.py` stay, since `tests/pack/test_item_map.py` reads the item and
fluid ids.

**Considered.** Extending the allowlist to follow `vanillaRecipes`: the Pack would re-derive what
Craftworks and vanilla already provide.

**Consequences.** ADR-0088, ADR-0091 and ADR-0105 are superseded. The Obtainable term leaves the
glossary. A creative test item is listed like any other registered item, so ADR-0105's list needs no
replacement.
