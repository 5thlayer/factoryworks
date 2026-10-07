---
status: superseded by ADR-0125
---

# FactoryWorks' Oil Refinery is Craftworks' Oil Refinery

ADR-0096 made the Oil Refinery a FactoryWorks block on the Assembling Machine's chassis, wearing
Oritech's Refinery model and its two chamber layers, running a recipe type of its own. ADR-0123 moved the
Chemical Plant to Craftworks and left the Refinery. Craftworks 0.5.0 has an Oil Refinery
(`craftworks:oil_refinery`) that holds an `oil-processing` recipe of `craftworks:assembling` and moves its
fluid through Fluid Connections (Craftworks ADR-0015), as its Chemical Plant does.

**Decision (#581).**

- **The Oil Refinery is `craftworks:oil_refinery`.** The item map names it `borrowed` for `oil-refinery`,
  and FactoryWorks registers no Oil Refinery block, part, item, block entity type, model, loot table or
  renderer. It stands on Craftworks' 5x5 footprint three blocks tall, which is Factorio's tile square
  (ADR-0059), and the art is Craftworks' own. Oritech's Refinery model and renderer go and nothing takes
  their place (ADR-0122).
- **Factorio's `oil-processing` recipes are `craftworks:assembling` recipes** with
  `category: oil-processing`, under their old ids `factoryworks:oil_processing/<name>`, so no research
  unlock moves. They have no item ingredient and no item result, and the converter still writes both
  keys, as ADR-0123 has it, though Craftworks 0.5.0 accepts them left out. `factoryworks:oil_processing`
  is no recipe type.
- **FactoryWorks' fluid handling for the machine is none.** The Refinery's five Fluid Connections pull
  from and push into whatever block they face, so Pipeworks pipes carry the Pumpjack's crude and water
  in and the three fractions out, and no Pipeworks port is added to it. The Showcase's GameTest holds
  that: a Pumpjack's crude through a line of pipes becomes heavy oil, light oil and petroleum gas in
  three tanks.
- **The Pack has no machine chassis left.** Nothing extends `AssemblingMachineBlockEntity`, so it goes
  with the menu, screen, spec, Held recipe, stall and status types, the machine fluid handler, the EMI
  category, handler and recipe, the Jade provider, the Fill Recipe packet, the Oritech paint lock, the
  Researchd lock source and the `lockSources` config, the Oritech screen-handler mixin that only the
  Refinery's screen needed, and `factoryworks:oil_processing`'s recipe type. What the chassis shared with
  the furnaces, the Overload Limit's constants, stays, without its fluid half.

**What ADR-0096 keeps.** Nothing: both machines are Craftworks'. ADR-0096 is superseded.

**Considered: keep the Refinery as a Pack block and add Pipeworks ports to it.** Rejected. The Pack would
keep a chassis, footprint, item face and lock that Craftworks now has, for one machine, and the ports
would repeat what Fluid Connections already do.

**Consequences.**

- The independence guard's Oritech and Researchd baselines fall, in data and in Java.
- The Fluid Connections have no direction, and a pipe that has run dry takes whatever the Refinery
  pushes. A Refinery tries its connections in the order its description gives them, two on the edge it
  faces and then three on the other, and pushes each product into the first neighbour that takes it. A
  crude line that the Refinery empties between crafts is therefore safest on the last connection, which
  `OilRefineryTests` and the oil scene do. A Pack that wants no such care needs Craftworks to name
  connections by role, which is Craftworks' to decide.
- The 5x5 footprint, three blocks tall, replaces the 22-block Oritech footprint, so existing factories
  with a Refinery do not carry over.
- The oil Showcase scene is laid out again for the larger footprint.
- The Refinery's art is not final: Craftworks owns it, and the Showcase takes it with a version sync.
