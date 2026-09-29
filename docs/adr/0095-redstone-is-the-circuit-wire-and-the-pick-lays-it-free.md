---
status: accepted
supersedes: [148]
---

# Redstone is the circuit network's wire, and the Engineer's Pick lays it free

ADR-0030 gave the pack Factorio's circuit network as vanilla redstone plus Create's redstone line. Two
things have since taken both halves away:

- **Create left with ADR-0060**, and its Redstone Link, latches, switches, Smart Observer and Display
  Link with it.
- **Redstone cannot be made.** ADR-0021 cut it as a patch, and #124 closed Terra's input alphabet to
  its four ores, its stone and what its surface grows, for every pack-authored recipe. So no dust,
  repeater, comparator, torch or observer is Obtainable, and the network ADR-0030 called `adapted` did
  not exist in a world.

#119 asked where redstone comes from. The answer is that it does not have to come from anywhere:
**Factorio 2.0 has no wire item.** The corpus holds no `red-wire`, `green-wire` or `copper-wire`
recipe; once `circuit-network` is researched a player lays wire for nothing. Only what the wire
connects — combinators, lamps, speakers, displays — is crafted.

**Decision (#119).** The circuit network's wire is **Redstone**, vanilla dust, and it is never an
item:

- **The Engineer's Pick lays it.** A right-click with either tier places `minecraft:redstone_wire`
  wherever a held dust item would, spending nothing. It is the Pick's last verb: a stored Dismantle
  start, a pole's Wire and a pipe connection each take the click first.
- **It drops nothing.** Broken, washed away or left unsupported, dust leaves no item, as a worldgen
  plant does (ADR-0092). Nothing lists it as Obtainable.
- **It is gated on `circuit-network`.** Researchd is not on 26.1.2 yet, so it ships ungated and the
  gate is #260's.
- **The Placement Preview draws it**, through a vanilla plan the Pick opts into (ADR-0069).

The network's components are not decided here. With Create gone nothing supplies them, and they are
#484's, under the tracking ticket #482. The item-map rows #148 made `not_emitted` because Create
supplied the capability — the four combinators and the display panel — are `undecided` on #484.
Whether the pack grows past redstone's one channel to a real red and green wire is #485's.

The ledger row stays `adapted`, supplied by `native_mechanic` and `factoryworks_core`.

**Considered.**

- *Redstone as a crafted item*, from copper cable or a circuit. It needs an exception to #124 for a
  material Factorio does not have, and puts a wire item in the inventory that Factorio removed.
- *Exclude the circuit network.* Factorio's automation leans on it, and the mechanism is already in
  the base game; only the material was missing.
- *A first-party signal network*, named signals on a wire. That is #485's question, not a prerequisite:
  redstone gives a working network now.
- *A shortcut key* rather than the Pick's click. A second gesture to learn, where the Pick is already
  the tool that lays and cuts a pole's Wire.
- *Amend ADR-0030.* Its supplier half is dead and its "supply is open" half is answered, so an
  amendment would keep a record whose argument no longer holds. Its lesson — that `native_mechanic`
  is a real answer — stands and is restated here by being applied.

**Consequences.**

- Redstone is a circuit network with one channel, 0 to 15, block to block. No reading a belt's
  contents off one wire and no arithmetic on a signal; the ledger's `notice` says so.
- Until #484, the only logic is what dust does alone. The network carries a signal and nothing reads
  or decides with it.
- A vanilla recipe or loot table that yields redstone must stay swept or emptied; one that survives
  puts the item back.
