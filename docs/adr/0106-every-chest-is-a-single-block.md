---
status: accepted
---

# Every chest is a single block

The Pack's chest ladder has three rungs: the Wooden Chest (vanilla's `minecraft:chest`, 27 slots),
the Iron Chest (36) and the Steel Chest (54). Vanilla pairs two adjacent chests into one double
chest. Factorio has no double chests: every chest occupies one tile and holds only its own slots.

**Decision (#269).** No chest pairs, and that includes vanilla's own. Two chests placed side by
side stay two single chests, each with its own inventory, menu and lid.

**Considered.** Letting the Iron and Steel Chests pair, since fitting chests into a layout would
add a space puzzle. A double Iron Chest holds 72 slots and a double Steel Chest 108, and both
exceed the 54 that vanilla's largest chest screen shows, so pairing needs a custom wide, tall or
scrolling screen. Railcraft's void chest sprite also had no left and right halves, so a double
Steel Chest would have had no art to draw without committing a derived asset (#234). Railcraft has
left the Pack (ADR-0109); the Steel Chest's sheet is the Pack's own. Pairing only the
Wooden Chest: then one rung of the ladder would behave differently from the other two.

**Consequences.** A chest's capacity is the ladder's figure and nothing else, so the three rungs
compare directly. Vanilla's chest pairing is switched off in the mod, for every `ChestBlock`.
