# Void energy and Mutation — design

Resolved in two grilling sessions on 2026-10-06, the second on void logistics, from the original
idea in [docs/ideas/mutation_system.md](../ideas/mutation_system.md), which stays as written.
Factorio fidelity was not a constraint.

## Terms

These move to a glossary of their own once the mechanic has code.

**Voidworks**:
The Module that holds void: harvesting, Voidstone networks, the Void Dragon and Mutation. Like every
Module it requires FactoryWorks and no other Module.

**Mote**:
The unit of void, harvested, stored and moved. A mote carries a grade: the band of Void Pressure
where it was harvested. Motes of one grade stack; motes of different grades never merge.

**Void energy**:
What a mote releases when it is spent: the more its grade exceeds the Void Pressure where it is
spent, the more it releases. Nothing stores void energy; everything stores motes. It is not FE.
Bare "void" means this mechanic; removing items is "destroying" or "trashing" them.
_Avoid_: voiding (for destroying items), void as "needs no power"

**Void Pressure**:
The ambient void level of a place: a base per dimension, highest in the End, then the Overworld,
lowest in the Nether, and raised near the End's open void. It sets the grade of motes harvested
there, the value of motes spent there, how fast a Void Crucible works there, and how fast a Void
Dragon weakens there. Motes stored nearby never change it.
_Avoid_: network pressure (the motes' own, which only steers flow)

**Density**:
How many motes a block yields when fed to a Void Displacer. It is set per block, by tag, and
steepens sharply with each step: stone is cheap, and storage blocks are how harvesting scales.

**Voidstone**:
The indestructible block a harvested position becomes. Every harvest leaves one, so harvesting
permanently consumes the world, which is what bounds void. Touching Voidstone forms a network that
carries motes.
_Avoid_: bedrock, residue

**Void Displacer**:
A machine that harvests motes in its zone, attuned to the dimension it is built for and working
only there. It cannot be broken or moved while its zone has anything left to convert; when nothing
is left it becomes Voidstone itself.
_Avoid_: harvester, Void Well

**Void Siphon**:
The item a player harvests motes with by hand, at a far worse yield than a Void Displacer's. It
holds motes, and void gear draws from it.
_Avoid_: glove

**Void Well**:
A large store of motes on a Voidstone network, and the stop where the Void Dragon loads, unloads
and heals.
_Avoid_: roost, Void Displacer

**Void Dragon**:
A dragon raised on void that carries motes between Void Wells, diving through the void between
them, across dimensions. It grows from a Dragonling.
_Avoid_: Ender Dragon (the boss), tamed dragon

**Dragonling**:
A young Void Dragon, hatched from an egg soaked in motes. It cannot carry yet.

**Drained Dragon Egg**:
The egg every dragon kill after the first drops: a dragon egg drained of void, which hatches once
it has soaked up enough motes.
_Avoid_: lesser egg

**Enriched ore**:
A FactoryWorks ore block made by displacing a vanilla ore, in one of three richness tiers. Each
tier is its own item and starts at its own amount.
_Avoid_: ore patch (worldgen's)

**Mutation**:
Turning an item into another: upgrading it to the next tier of its Family, or breaking it down into
its ingredients. A Mutation Chamber does it quickly for motes; a Void Crucible upgrades items
slowly and for free. An upgrade is a chance roll that keeps the item on failure, and a breakdown
never returns more than went in.
_Avoid_: transmutation, recycling

**Mutation Chamber**:
The machine that performs a Mutation, spending motes.

**Void Crucible**:
A block that holds items in Void Pressure until they upgrade up their Family. It spends no motes,
only time, and progresses only while its chunk is loaded.
_Avoid_: altar, cradle

**Family**:
An ordered ladder of materials that an upgrade climbs, such as copper → iron → gold. Families are
data, so other mods' materials can join one.
_Avoid_: tier, quality

## Where it lives

In **Voidworks**, a Module of its own in its own repository, started from libworks' template. Nothing
the other Modules do uses void, so it is not something the base mod holds for them, and a player can
leave the End-centred endgame out. It requires FactoryWorks because Enriched ore is a FactoryWorks
ore block (ADR-0041) and FactoryWorks' drills mine it; everything else goes through NeoForge's item
and fluid faces.

## Progression

Void is post-End: easy to start means easy once the dragon is beaten. The Void Siphon's recipe
needs dragon's breath. Each Void Displacer's recipe needs its dimension's materials and motes.
Nothing forces the first Displacer into the End; the End is where harvesting pays best.

## Motes and void energy

- A mote's grade is the band of Void Pressure where it was harvested: a few bands, such as Nether,
  Overworld, End, and End near the open void.
- Spending a mote releases void energy by how far its grade exceeds the local Void Pressure. A
  machine refuses a mote whose grade is not above it, and spends the lowest grade still worth
  something first.
- So the End is the reservoir and the Nether the turbine: one mote does the most work in the
  Nether, and moving motes downhill is why void logistics exists.
- Every spend follows this rule — Mutation, gear, zone upgrades, Displacer recipes, hatching,
  void-only materials — except healing the Void Dragon, which happens only at a Void Well.
- What motes buy: power for endgame gear, Mutation, and void-only materials (a void alloy and the
  like). They never create ordinary items; Mutation is the only path from motes to those.

## Harvesting

Three Void Displacers, one per dimension, each working only in its own. All three consume fed
blocks, yield motes by the fed block's Density at the grade of the local Void Pressure, and turn
each harvested position into Voidstone.

- **End**: places fed blocks into the empty air of its zone.
- **Overworld**: swaps fed blocks with natural terrain in its zone — the blocks in a natural-block
  tag (stone, deepslate, dirt, ores…), which placed stone also matches. The terrain block goes to
  the Displacer's output. A vanilla ore with a FactoryWorks counterpart comes out as an **Enriched
  ore** instead, stone excepted for now.
- **Nether**: displaces lava sources in its zone into an internal tank with a fluid output face, so
  any mod's pipe or tank, or a bucket, drains it (ADR-0121).

Each Displacer has one item output inventory with an item face. A full output inventory or tank
stalls it. While its zone has anything left to convert — air, natural terrain, lava — it cannot be
broken or moved; when nothing is left it becomes Voidstone itself, and relocating means building a
new one. Zones can grow through upgrades paid in motes; relocation stays the main loop.

Density rises steeply (shape: about 4^density): stone is the cheap start, storage blocks are how
harvesting scales.

### By hand

The **Void Siphon** is one item that behaves by the dimension it is in, fed from the off hand, at a
far worse yield than a Displacer's and with an on/off toggle:

- **End**: the block goes into air and becomes Voidstone; nothing comes back.
- **Overworld**: the block swaps with a natural block, which comes back to the player.
- **Nether**: the block displaces a lava source, which fills an empty bucket or another mod's fluid
  item from the inventory. With none, it refuses.

### Enriched ore

- Three richness tiers, each its own registered item (the precedent is ADR-0010), set by the fed
  block's Density and starting at the tier's amount (ADR-0041).
- Placed anywhere and mined by hand or by drill, drawing the amount down.
- Never natural-tagged, so it is never displaced again.
- Silk touch returns the highest tier whose starting amount does not exceed what is left; below the
  lowest tier it mines one unit as usual. It is never a way to gain ore.

### Ore patches

Ore patches become a worldgen option, so that drills can be a post-End mechanic: with patches off,
a drill has nothing to mine until Enriched ore exists. The option is FactoryWorks' and defaults to
on, so FactoryWorks alone keeps its drills from the start; installing Voidworks does not change it.
Voidworks' documentation recommends turning it off for a post-End drill game. The option only
affects chunks generated after it is set.

Enriched ore multiplies ore; ADR-0032's cut of every multiplier is superseded by ADR-0115.

## Moving motes

- **Voidstone conducts.** Motes crawl through face-touching Voidstone as a visible glint, at a
  finite speed and without loss, from high network pressure to low. Each grade flows on its own.
- Networks that touch merge, harmlessly: pressure, not topology, decides where motes go. There is no
  seal or valve.
- Network pressure is the motes' own and only steers flow. It is capped below the End's Void
  Pressure, so a full network never makes a place a stand-in for the End.
- Any void machine touching Voidstone joins the network. The Void Siphon charges or discharges while
  the player stands on it.

### The Void Well

One block: a large store of motes, the Void Dragon's stop and its healing point. Its pressure stays
low until it is nearly full, so with no fill target it is a reservoir that soaks up surplus and
gives it back. With a fill target, a Void Dragon delivers to it until the target is met.

### The Void Dragon

Named and raised on the pattern of vanilla's Dried Ghast, Ghastling and Happy Ghast.

- **Hatching.** The first dragon kill gives the vanilla egg; every later kill drops a **Drained
  Dragon Egg**. Either egg is placed touching a Voidstone network and soaks up motes from it over
  time, in visible stages, until it hatches; each egg needs more motes than the last. So carriers
  scale with dragon fights won.
- **Growing.** It hatches a **Dragonling**, which grows into a **Void Dragon**, faster when fed
  motes. A Dragonling cannot carry.
- **Routing.** A Void Dragon serves all its owner's Void Wells in every dimension, carrying from high
  pressure to low until each Well's fill target is met. Several dragons share the work.
- **Travel.** It flies visibly near its Wells and dives into the void between them, emerging after
  a travel time set by distance. It is the only thing that carries motes between dimensions.
- **Riding.** A player mounts it at a Void Well and picks another of their Wells; it dives and
  emerges there, in any dimension. It is not free flight. A rider costs nothing and the load
  travels as usual.
- **Harm.** Outside the End it weakens at a rate set by how far the local Void Pressure is below the
  End's, so it suffers most in the Nether, where motes pay most. Hurt, it flies more slowly; it
  cannot die. It heals with motes at a Void Well.

## Mutation

- The **Mutation Chamber** does two things for motes:
  - **Upgrade**: an item to the next tier of its **Family**. A chance roll; on failure the item is
    kept and the motes are spent.
  - **Breakdown**: an item into its crafting recipe's ingredients, read from the recipe in reverse,
    with a datapack override list. Items with no recipe or several are skipped unless overridden.
    It returns about 25% of the ingredients, more as more motes are spent, never more than went in.
- Families are data: tags or datapack ladders, shipped as separate metal and gem ladders, so other
  mods' materials can join.
- The **Void Crucible** upgrades items for free, slowly, at a rate set by Void Pressure, so items go
  to the End while motes leave it. Upgrade only, no breakdown. Progresses only while its chunk is
  loaded. Hoppers and belts feed and empty it.

## Loops

Loops that come out ahead are allowed and are meant to be the endgame puzzle, enriched ore
included. What bounds them is that every harvest permanently consumes the world and every Displacer
ends as Voidstone. The Void Siphon alone must never close a profitable loop.

## Open

- **Voidstone raising Void Pressure around it** — a later layer, compounding a site's harvests.
- **The Voidworks repository** — not created yet; this spec moves there with it.
- **Numbers** — the grades, the Density curve, zone sizes and upgrade costs, the richness tiers'
  amounts, upgrade chances per tier, breakdown yield against motes, mote speed, Void Well capacity
  and the network pressure cap, the Void Dragon's load, speed and harm rate, and the rise in hatching
  cost.
