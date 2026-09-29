---
status: accepted
supersedes: [267]
---

# The core pole network is the only carrier, and it pulls from Oritech's generators wirelessly

ADR-0060 left power with two carriers: the core's supply-area poles inside an area, and Oritech's
Energy Transmission Pole between areas. It named the Steam Engine as a thing that "makes 450" but
not how that FE reaches a pole, and #267 answered with a core subclass that pushes FE into the grid.
Neither is Factorio's shape. In Factorio a generator has no wire and no face: it is on a network
because it stands inside a pole's supply area, and poles within wire reach are one network.

## Decision

**One carrier.** Core supply-area poles within wire reach of each other join into one **Electric
Network**. Every generator, accumulator and machine standing in any of their areas belongs to it.
Oritech's Energy Transmission Pole leaves the power path; ADR-0060's two-carrier clause falls. The
visible wire between poles is a separate ticket and is cosmetic to the balance.

**Wireless pull, identified by tag.** A pole recognises a source by the block tags
`factoryworks:generators` and `factoryworks:accumulators`, and moves FE through the block's
`Capabilities.Energy.BLOCK` face. The tag, not the face, decides the role: an extract-capable face
alone would make any third-party battery a generator and drain any machine whose face allows
extraction.

**Factorio's balance.** Demand is met from generators first and accumulators second; only generator
surplus charges accumulators. Load is shared across generators in proportion to each one's maximum
output. This is Minecraft-free arithmetic and is unit-tested as such.

**The Steam Engine is Oritech's block, tagged and mixed in.** Oritech's own mechanics are kept on
purpose, as better play than strict fidelity: master/slave chaining of adjacent engines, a speed that
follows the steam tank's fill, the efficiency curve over that speed, and an FE buffer the engine
stops at when full (`stopOnEnergyFull`). A mixin makes three changes:

- **Calibration.** One engine at the curve's peak (speed 7) burns 30 mB/s of steam and makes
  450 FE/t — Factorio's 900 kW. A starved or flooded engine does worse.
- **No water return.** Oritech returns 90% of spent steam as water because its water supply is
  finite. Ours is not: the Offshore Pump draws from one source block forever (ADR-0050). The return
  would move one pump from twenty Boilers to about two hundred and add a silent stall on a full water
  tank, to solve a problem the pack does not have. Returned water is zero and ADR-0050 stands.
- `factoryworks:steam` replaces Oritech's steam in `c:steam`, and the engine's recipe is emitted by the recipe converter
  from the `steam-engine` item-map row; Oritech's own crafting recipe is swept.

**Why no subclass, against ADR-0060's rule.** Oritech's chaining looks neighbours up by
`BlockEntitiesContent.STEAM_ENGINE_ENTITY` and keeps its slave set private. A core block entity type
never matches, and engines would silently stop chaining. Using Oritech's block and type keeps
chaining intact, and the pole still reaches it through the tag and the face.

**The accumulator follows the same route.** Oritech's Large Energy Storage (1×3), tagged
`factoryworks:accumulators`, mixed in to Factorio's 5 MJ and 300 kW: 50,000 FE at 150 FE/t.
This replaces ADR-0060's "the core's accumulator" and restores that one storage block's recipe.

## Divergences from the corpus (ADR-0054)

- The Steam Engine's footprint is Oritech's hull, not Factorio's 3×5.
- The accumulator is 1×3, not Factorio's 2×2.
- The engine holds an FE buffer and burns into it, where Factorio's burns only what the network
  draws. The buffer is one tick of output, 450 FE per engine in the row, so it hides no outage;
  with the stop on a full buffer, an unloaded engine still burns no steam.
- The engine's steam tank is 200 mB per engine in the row, Factorio's own fluid box, not Oritech's
  8,000 mB. Tank size decides only how fast the fill-driven speed settles, not where: 8,000 mB took
  about three minutes for one engine and hid an outage for as long.
- Oritech lets two masters that receive steam before either scans both claim the empty engines
  between them, counting those twice. The mixin skips an engine already answering to a live master,
  so every engine counts once.

## Consequences

- #267's subclass and push are superseded; #262 is re-scoped around this decision.
- A GameTest must assert that two adjacent engines chain and that a pole pulls from the master, since
  both failures are silent.
- The Oritech Steam Engine is not swept by the duplication rule: it is the pack's engine, not a
  second route.
