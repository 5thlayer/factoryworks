---
status: accepted
---

# A plant the worldgen places drops nothing

#454 made the drops of every block the live worldgen places Obtainable. On Terra that brought in
flowers, mushrooms, sugar cane, cactus, kelp, leaf litter and wheat seeds from the grass, sticks
from a dead bush, and flint from gravel. None of them is part of the factory. Factorio's
decoratives cannot be mined at all, and what a player takes from the land is wood, stone and ore.

**Decision (#454).** Every plant the live worldgen places, the leaves of Terra's three trees
included, drops nothing. Gravel drops only gravel. This is done by replacing each block's loot
table under `kubejs/data/minecraft/loot_table/blocks/`, so what the game gives is what EMI lists.
What stays Obtainable from the land is its terrain (dirt, sand, red sand, gravel, sandstone, red
sandstone, cobblestone) and the logs of the Terra trees. A plant that a later change adds to the
live worldgen gets an empty table of its own; `tests/pack/test_obtainable_index.py` fails until it
has one.

**Considered.** Leaving the game alone and excluding these items from the Obtainable derivation
only. EMI would then hide items a player still picks up, which is the gap ADR-0088's allowlist
exists to close.

**Consequences.** Leaves no longer drop a sapling, a stick or an apple when broken by hand or when
they decay, which is ADR-0051's "no sapling, dropped or crafted" made true in the game rather than in
the index alone. A block's drop is replaced only for blocks the live worldgen places. A body that
comes out of `kubejs/parked/` brings its plants under this rule when it does.
