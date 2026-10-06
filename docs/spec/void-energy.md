# Void energy and Mutation — design

Resolved in a grilling session on 2026-10-06, from the original idea in
[docs/ideas/mutation_system.md](../ideas/mutation_system.md), which stays as written. Factorio
fidelity was not a constraint.

## Terms

These move to a glossary of their own once the mechanic has code.

**Void energy**:
FactoryWorks' endgame currency, harvested by filling empty space with blocks. It is not FE: only
void machines and void gear spend it. Bare "void" means this energy; removing items is
"destroying" or "trashing" them.
_Avoid_: voiding (for destroying items), void as "needs no power"

**Density**:
How much void energy a block yields when harvested. It is set per block, by tag, and steepens
sharply with each step: stone is cheap, and storage blocks are how harvesting scales.

**Mutation**:
Turning an item into another: upgrading it to the next tier of its Family, or breaking it down into
its ingredients. A Mutation Chamber does it quickly for void energy; exposure to Void Pressure
upgrades items slowly and for free. An upgrade is a chance roll that keeps the item on failure,
and a breakdown never returns more than went in.
_Avoid_: transmutation, recycling

**Void Pressure**:
The ambient void level at a position: a base per dimension, highest in the End, then the Overworld,
lowest in the Nether, and raised near open void. It multiplies a Void Displacer's yield and sets how
fast exposure upgrades items.

**Mutation Chamber**:
The machine that performs a Mutation, spending void energy.

**Void Crucible**:
A block that holds items in Void Pressure until they upgrade up their Family. It needs no void
energy, only time, and progresses only while its chunk is loaded.
_Avoid_: altar, cradle

**Void Displacer**:
The machine that harvests void energy by placing blocks into the empty space of its zone, holding a
little of what it harvests. It cannot be broken or moved while its zone has empty space left; when
none is left it becomes Voidstone itself.
_Avoid_: harvester, Void Well

**Void Siphon**:
The item a player carries to harvest void energy by hand and to hold it. With it active, a block
placed in empty air becomes Voidstone, at a far worse yield than a Void Displacer's.
_Avoid_: glove

**Void Well**:
A large container of void energy.
_Avoid_: Void Displacer

**Voidstone**:
The indestructible block a harvested position becomes. Every harvest leaves one, so harvesting
permanently consumes empty space, which is what bounds void energy.
_Avoid_: bedrock, residue

**Family**:
An ordered ladder of materials that an upgrade climbs, such as copper → iron → gold. Families are
data, so other mods' materials can join one.
_Avoid_: tier, quality

## Where it lives

In the FactoryWorks base mod for now. It moves to a Module of its own if it grows into a standalone
mechanic.

## Void energy

- Its own currency, not FE. Only void machines and void gear spend it.
- A source that is cheap to start and costly to scale. The resource sink comes free: dense blocks
  are the best fuel.
- What it buys: power for endgame gear, and creation of void-only materials (a void alloy and the
  like). It never creates ordinary items; Mutation is the only path from void energy to those.

## Harvesting

- A **Void Displacer** places blocks into the empty air of its zone. Each placed block becomes
  **Voidstone** and yields void energy by its **Density**. Blocks already in the zone are skipped:
  terrain is never harvested.
- While its zone has empty air left, the Displacer cannot be broken or moved. When none is left, it
  becomes Voidstone itself. Relocating means building a new one.
- Zones can grow through upgrades that cost void energy. Relocation stays the main loop.
- The Displacer's recipe costs void energy; running it is free.
- The **Void Siphon** bootstraps the first Displacer: with it held and active, a block placed in
  empty air becomes Voidstone, at a far worse yield than a Displacer's. It has an on/off toggle.
- Density is set per block by tag, and energy rises steeply with it (shape: about 4^density).
  Stone from a cobblestone generator is the cheap start; storage blocks are how harvesting scales.
- **Void Pressure** multiplies the yield: a base per dimension (End > Overworld > Nether), plus a
  bonus near open void.

## Mutation

- The **Mutation Chamber** does two things for void energy:
  - **Upgrade**: an item to the next tier of its **Family**. A chance roll; on failure the item is
    kept and the energy is spent.
  - **Breakdown**: an item into its crafting recipe's ingredients, read from the recipe in reverse,
    with a datapack override list. Items with no recipe or several are skipped unless overridden.
    It returns about 25% of the ingredients, more as more energy is spent, never more than went in.
- Families are data: tags or datapack ladders, shipped as separate metal and gem ladders, so other
  mods' materials can join.
- The **Void Crucible** upgrades items for free, slowly, at a rate set by Void Pressure. Upgrade only,
  no breakdown. Progresses only while its chunk is loaded. Hoppers and belts feed and empty it.

## Loops

Loops that come out ahead on void energy are allowed and are meant to be the endgame puzzle. What
bounds them is that every harvest permanently consumes empty space and every Displacer ends as
Voidstone. The Void Siphon alone must never close a profitable loop.

## Open

- **Void logistics** — cells, transport, how gear and machines draw void energy, the Void Well as a
  large container. Its own grilling session; the intent is to depart from known logistics.
- **Base mod or Module** — revisit if the mechanic grows.
- **Numbers** — the Density curve, zone sizes and upgrade costs, upgrade chances per tier, breakdown
  yield against energy, Void Pressure per dimension and the near-void bonus.
