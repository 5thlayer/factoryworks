---
status: accepted
supersedes: [581, 582]
---

# The Pack's oil and chemistry recipes run on Craftworks' Assembler

ADR-0123 and ADR-0124 made the Chemical Plant and the Oil Refinery Craftworks' own machines, holding the
`chemistry` and `oil-processing` categories. Craftworks 0.6.0 removes both machines and both categories
(Craftworks ADR-0018): every Assembler has two input and three output fluid boxes on six Fluid
Connections, and any recipe with a fluid in or out is `crafting-with-fluid`.

**Decision (#644).**

- **The Pack's chemistry and oil recipes are `crafting-with-fluid` recipes on Assembler 2 and 3.** They
  keep their ids, `factoryworks:chemistry/<name>` and `factoryworks:oil_processing/<name>`, so no research
  unlock moves, and they run at the Assembler's speed with their `time` unchanged. The Pack's config keeps
  `crafting-with-fluid` off Assembler 1.
- **Basic oil processing is gone.** Advanced oil processing is the only way to split crude, and it unlocks
  with oil gathering, since chemical science needs petroleum gas. Factorio's data is left as it is: the
  Pack's drop is recorded in `recipe-overrides.json`.
- **Nothing in the Pack names the two machines.** Their build recipes, research unlocks, buildings tag
  entries, config sections and item-map rows go. A world loses any it placed, as Craftworks' changelog says.

**Considered: moving the recipes under `assembling/`.** Their ids would follow the machine that makes them,
but every research unlock and lang key naming them would move with no gain to a player.

**Considered: keeping basic oil processing.** It is Factorio's early petroleum source and would avoid
moving the advanced recipe's unlock, but the Showcase no longer follows Factorio, and one way to split
crude is simpler to show.

**Consequences.**

- ADR-0123 and ADR-0124 are superseded.
- Five fluids on one Assembler need five pipe networks, and the Assembler's connections stand side by side,
  so neighbouring pipes join unless a side is closed. The Pack's game test and oil scene close them in code;
  a player has no tool for it until the Pack fills Pipeworks' `closes_sides` tag.
- A Fluid Connection pushes into any neighbour that takes the fluid, and the Assembler tries the three
  connections it faces first, so a supply line that runs dry takes a product. The oil scene and its game
  test put the products on the connections the Assembler faces and keep the supplies full.
