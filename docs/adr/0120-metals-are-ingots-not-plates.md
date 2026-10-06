---
status: accepted
supersedes: [220]
---

# Metals are ingots, not plates

ADR-0053 gave each metal one plate and no ingot step, as Factorio does, and the Pack registers
`factoryworks:iron_plate`, `copper_plate` and `steel_plate` for it. ADR-0115 makes Factorio the
reference for mechanics rather than the authority, and puts FactoryWorks in an ordinary world beside
other tech mods, where vanilla already supplies iron and copper ingots and most tech mods ship their
own.

**Decision.** A metal is an ingot. FactoryWorks' recipes consume `c:ingots/<metal>`, so any mod's
ingot serves, vanilla's included, and its furnaces smelt ore to ingots. The three plate items go.
FactoryWorks registers an ingot only where vanilla has none, as for steel. This replaces ADR-0053's
plate rule, which ADR-0060 had carried forward.

**Considered: a plate that ingots also satisfy**, with recipes consuming `c:plates/<metal>` and
`c:ingots/<metal>` together. Rejected: it keeps a second item per metal that nothing needs, and the
word "plate" in the vocabulary for a form the player never has to make.

**Considered: a plate that only plates satisfy**, so raw ore enters the factory through a
FactoryWorks furnace. Rejected: it makes a vanilla ingot useless to the suite, which is the
Factorio fidelity ADR-0115 stopped holding to.

**Consequences.** Every recipe naming a plate is rewritten to the ingot tag, and the plate items,
their textures and lang entries are removed; #610, which registers the intermediates and their tags,
carries it. Factorio's own data still says "plate", so a recipe ported from it maps plate to ingot.
