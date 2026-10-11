# FactoryWorks

A suite of Minecraft 26.1.2 / NeoForge tech mods that brings Factorio's logistics into an ordinary
world and mixes with other tech mods. Its tagline is "the factory just works". Factorio is the
design reference for mechanics, not the authority for numbers.

**Module**:
One of the suite's mods, each owning one capability: Groundworks (placement), Beltworks (items on
belts), Pipeworks (fluids), Wireworks (energy), Craftworks (crafting machines), Fieldworks
(resources), Gearworks (Gear) and Voidworks (void energy). Each plays in vanilla and requires no
other Module but Groundworks (ADR-0128).
_Avoid_: Library, Binding

**Groundworks**:
The Module that owns placement: previews, rotation, stretching, dismantling and footprints, for
vanilla blocks and any mod that plans through it. It is the one Module others may require
(ADR-0128).

**Fieldworks**:
The Module that holds resources and their base forms: ore patches, oil wells and crude oil, and the
Harvester that works them. It holds no intermediates.
_Avoid_: Core, FactoryWorks (the mod)

**FactoryWorks**:
The suite's name. No mod is called FactoryWorks, and the suite ships no modpack of its own (ADR-0115).
_Avoid_: the Pack, the Showcase

**Harvester**:
Fieldworks' machine that stands over an ore patch or an oil well and works the layer beneath it,
drawing ore or pumping crude oil.
_Avoid_: drill, rig, pumpjack, deposit

**Harvest Recipe**:
A recipe the Harvester runs: a resource it works, an optional Drilling Fluid it consumes, and what it
yields. A Harvester holds one, set by the player.
_Avoid_: drilling recipe, mining recipe

**Drilling Fluid**:
Any fluid a Harvest Recipe consumes, whether the resource needs it or it raises the yield.
_Avoid_: mud (only the default fluid), lubricant, coolant

**Pump**:
Pipeworks' machine that draws a fluid's source block into a pipe, leaving water and any other
infinite source in place.
_Avoid_: Offshore Pump

**Outlet**:
Pipeworks' machine that sets a pipe's fluid down in the world as a source block.
_Avoid_: Spout

**Refiner**:
Craftworks' electric furnace, which both smelts and blasts.
_Avoid_: Electric Furnace, Oil Refinery

**Tar**:
The solid that crude oil sets into where its source block meets lava. One of the resins.

**Bioresin**:
A resin brewed or crafted from plants.

**Resin**:
Anything that blasts into Plastic: Tar, Bioresin and vanilla's resin clump.

**Plastic**:
Wireworks' material for its larger electrical parts, blasted from any Resin.
_Avoid_: plastic bar

**Battery**:
Wireworks' part made of one cathode, one anode and one electrolyte, which the accumulator and the
Reactor are built from.

**Sink**:
A consumer whose demand grows with the factory and never runs out, so a bigger factory always has
somewhere for its output to go.
_Avoid_: endgame (for the consumer itself), resource drain, money sink

**Gear**:
What the player wears that the suite's materials make, upgraded through a Gear Grid: one suit, in
the chest slot.
_Avoid_: equipment (that is belt equipment, ADR-0100), armor, tools

**Gear Grid**:
The two-dimensional area in a piece of Gear that holds its upgrades, each covering a rectangle of it.
_Avoid_: equipment grid, upgrade grid

**Gear Upgrade**:
An item that fits a Gear Grid and gives the Gear one of the stats Gearworks owns. It carries its own
level, which rises without limit at an exponentially growing cost. Any mod can make one.
_Avoid_: equipment, module, augment

**Gearworks**:
The Module that owns Gear.

## Language

Each Module's glossary defines its own terms in full. The terms below are the ones the suite shares
or that cross Modules.

### Resources

**Patch**:
A disc of ore blocks of one resource, generated in new land beyond the spawn area, with a total its
blocks share. Fieldworks' term, defined in its glossary.
_Avoid_: vein, deposit, ore blob, ore body, ore field

**Amplitude**:
How much one block of a patch holds. It rises with distance from the world origin up to a cap, beyond which a patch grows wider instead.
_Avoid_: richness, density, per-tile amount

**Radius**:
How wide a patch is. Amplitude and Radius split a patch's quantity between them: the radius stays fixed until the amplitude reaches its cap, then grows with distance.
_Avoid_: size, footprint, patch size

**Oil well**:
One block that holds an amount of **Crude Oil** and never runs dry: each draw lowers its yield toward a floor, where it stays. A crude-oil field is a scattering of wells, not a filled patch.
_Avoid_: oil spring, oil deposit, bedrock fluid deposit

**Crude Oil**:
The fluid an **Oil well** yields, in the `c:crude_oil` tag. Its one job is plastic: where its source block meets lava it sets into Tar (ADR-0127).
_Avoid_: raw oil, oil, petroleum, oil spring

### Placing and dismantling

These are Groundworks' terms, which every Module that plans through it shares.

**Placement Plan**:
What a held item would do at an aimed spot: the positions it would fill, the blockstate at each, and a refusal or none. Placing executes a plan, and the **Placement Preview** draws one, so both ask one rule (ADR-0069). A multiblock is one plan and refuses whole.
_Avoid_: placement context (vanilla's own type, one input to a plan), build plan, preview state

**Placement Preview**:
What a player sees while holding a placeable block and aiming at a spot: the block drawn translucent where placement would put it, red where placement would be refused. It shows what placing would do and changes nothing in the world.
_Avoid_: ghost (Factorio's ghost is an entity left for robots to build, a mechanic the suite excludes), hologram, blueprint preview

**Dismantle**:
Taking up a span of one **Dismantle Family** from one block to another, both included: a sneak-click stores the start, and a click names the end. What the span's blocks drop goes to the inventory, and what does not fit drops at the player's feet. Blocks outside the family are never taken. Distinct from mining, which breaks one block and drops it.
_Avoid_: deconstruct, mass mine, unstretch

**Dismantle Family**:
The blocks one **Dismantle** takes up together as a single span, and the rule for the span between two of them. Groundworks ships vanilla's, Beltworks owns belts, and Pipeworks owns pipes, whose span is the shortest path joined only where each pipe draws an arm toward the other.
_Avoid_: dismantle group, dismantle kind, replace group (a different grouping)

**Dismantle Plan**:
What a **Dismantle** would take up at an aimed block: the blocks of the span from the stored start, and a refusal or none. Dismantling executes a plan and the preview draws one, as with a **Placement Plan**; the two are separate things.
_Avoid_: removal plan, placement plan (for a dismantle)

**Fast Replace**:
Placing a block over a placed one of the same **Replace Group** but another tier, which swaps it in place, up a tier or down. It takes one item and gives the old one back, and the new block keeps the old one's facing. A sneak-right-click still places beside. The **Placement Preview** draws a replace in a colour of its own.
_Avoid_: upgrade (it goes down a tier too), swap, overwrite

**Replace Group**:
The blocks that can **Fast Replace** each other, stated by the Module that owns them: the tiers of an Assembler are one, belts and splitters of every tier are one.
_Avoid_: family, tier ladder (a ladder is one kind's tiers; a group can hold two kinds, as belts and splitters do)

**Rotate**:
Groundworks' two rotation actions, on a key of their own (`R` by default). Held (**Rotate the Plan**): it turns the facing of what is about to be placed, a quarter turn each press, and the **Placement Preview** redraws with it. Placed (**Rotate in Place**): with nothing rotatable held, it turns the block under the crosshair, and what turning means is the block's own. A footprint machine pivots on its origin (ADR-0117). The turn is relative to the way the player looks, not a compass direction as Factorio's is, because the player's camera turns. A block whose turned shape does not fit is refused with its reason and nothing changes.
_Avoid_: wrench rotate, turn, rotate key

**Reverse Rotate**:
**Rotate** the other way (`Shift+R` by default), on both targets. A separate action rather than a modifier so it can be rebound alone.
_Avoid_: counter-rotate, rotate back

### Moving things

The belt vocabulary -- belt, tile, slope, stretch, transport line, loader, splitter and the rest -- is Beltworks', and is defined in its `GLOSSARY.md` (5thlayer/beltworks). Pipes are Pipeworks', defined in its `GLOSSARY.md` (5thlayer/pipeworks).

**Logistics puzzle**:
The production-chain routing problem -- what feeds what, at what ratio, over what distance. Explicitly
not the belt-lane micro-puzzle: lane balancing, sushi belts and weaving undergrounds through a fixed
footprint are 2D problems the suite does not have, and ADR-0044 records why.
_Avoid_: belt puzzle, the logistics game

### Handling things

**Stock interaction**:
Vanilla behaviour outside any recipe that turns an item or block into a different one: stripping a log, tilling dirt, water meeting lava, concrete powder hardening, waxing copper, a leaf's drop. Vanilla's stays as it is (ADR-0127). A mechanic a Module builds, such as the Pump, is not a stock interaction.
_Avoid_: world interaction (a pole's wire and a Dismantle act on the world too), recipe

**Overload Limit**:
The most of one ingredient, or of one product, that automated transfer leaves in a Craftworks machine: a number of crafts' worth set by the recipe and the machine's crafting speed, as Factorio computes it. A belt, loader, feeder or pipe stops at it, and a machine whose product has reached it stops crafting. The player's own hand in a machine's screen is not held to it.
_Avoid_: insertion limit, cap, 2x rule

### Powering things

**Steam**:
The low-temperature fluid Wireworks' **Boiler** makes from water and fuel and its **Steam Engine** burns. Real, pipeable and buffered, as in Factorio.
_Avoid_: low-pressure steam, LP steam, GT steam

**Superheated Steam**:
The high-temperature fluid accepted only by the **Steam Turbine**; the **Steam Engine** will not take it. The **Nuclear Reactor** emits it directly (ADR-0127).
_Avoid_: high-pressure steam, hot steam, 500-degree steam

**Boiler**:
The Wireworks machine that burns fuel to turn water into **Steam**. A 3x2 footprint, placed and broken whole.
_Avoid_: LP Solid Boiler, heater, steam generator

**Steam Engine**:
The Wireworks generator that burns **Steam** at Factorio's rate. Engines whose steam connects each draw their own rate from it, so a row of N makes N times one engine's power. It joins an **Electric Network** by standing in a pole's supply area. Placed as a footprint from one item and broken as one (ADR-0116).
_Avoid_: Create's Steam Engine, alternator, turbine

**Steam Turbine**:
The Wireworks generator that burns **Superheated Steam** at Factorio's rate, and nothing else, into an **Electric Network**. A Turbine never takes a Steam Engine's steam, and a Steam Engine never takes its (ADR-0127).
_Avoid_: large turbine, generator

**Solar Panel**:
The Wireworks generator that makes power from daylight alone: nothing at night, ramping through dusk and dawn, and nothing without open sky above it. It holds no more than one tick of its own output, so the night is the accumulator's to bridge.
_Avoid_: Big Solar Panel, solar generator, photovoltaic

**Nuclear Reactor**:
Wireworks' machine that burns **Uranium Fuel Cells** and turns water into **Superheated Steam**. Placed and broken as one footprint (ADR-0127).
_Avoid_: fission reactor, reactor controller, reactor multiblock

**Uranium Fuel Cell**:
What the **Nuclear Reactor** burns, made from a fissile ingredient: `#wireworks:fissile`, which defaults to uranium ingots and glowstone dust (ADR-0127).
_Avoid_: fuel rod, uranium pellet

**Electric Network**:
The poles joined by **Wire**s, and every generator, accumulator and machine standing in their supply areas: one balance of supply and demand. Wireworks' term, defined in its glossary.
_Avoid_: grid, power net, FE network

**Wire**:
A connection between two poles that makes them one **Electric Network**; only a wire joins poles. A placed pole wires itself to nearby poles, and a wire tool adds or cuts one by hand. Wireworks' term, defined in its glossary.
_Avoid_: link, cable, connection

### Making things

**Ingot**:
The form a metal takes in the suite's recipes. Any mod's ingot of that metal serves, vanilla's included, through the `c:ingots` tags.
_Avoid_: plate, sheet, unified plate

**Personal Assembler**:
Craftworks' crafting planner for the player, on the inventory screen: it replaces the crafting grid and crafts nothing directly, because every craft is a **Crafting Plan** (ADR-0089). It plans through each Assembling recipe that is hand-craftable, serially.
_Avoid_: hand crafter, personal crafter, portable crafter

**Crafting Plan**:
The resolved, flattened tree of crafts the Personal Assembler produces when an amount is chosen, and the unit in which the player commits and is refunded. It names every intermediate it will make, and every ingredient the player lacks; it is paid for in full when it starts. Craftworks' term (ADR-0089).
_Avoid_: crafting job, batch, order

**Plan queue**:
The serial list of Crafting Plans awaiting execution. One plan runs at a time, and a plan whose finished craft cannot fit in the player's inventory pauses and stops the whole queue rather than dropping anything. Craftworks' term (ADR-0089).
_Avoid_: crafting queue, assembler queue, backlog

**Missing ingredient**:
A leaf of a Crafting Plan the player does not have and the Personal Assembler cannot make.
_Avoid_: shortfall, unavailable

**Assembler**:
Craftworks' placed machine that runs Assembling recipes, in three tiers, and Craftworks' glossary defines it. It holds a **Held recipe** rather than matching on what it is fed, and is placed and broken whole from one item.
_Avoid_: Assembling Machine, crafter, fabricator

**Held recipe**:
The single recipe a player sets on a machine, which the machine then runs and nothing else. It is Factorio's own gesture: the machine is told its recipe rather than deducing one from what it is fed, so there is no lookup, no first match and no ambiguity between two recipes sharing an ingredient set. It is held whether or not the machine can currently run it, and is never cleared silently. Craftworks' Assembler and Fieldworks' Harvester both hold one.
_Avoid_: locked recipe, recipe lock, selected recipe, machine lock

### Art

**Stand-in art**:
A shipped texture, model or animation meant to be replaced: a placeholder, a generated sprite or an AI-generated one. Every stand-in is listed so it can be commissioned or redrawn.
_Avoid_: temp art, programmer art, WIP texture

**Vendored art**:
Third-party art shipped under its own open licence, credited in the repository's `NOTICE` and `REUSE.toml` (ADR-0130). It is final art, not a **Stand-in art**.
_Avoid_: borrowed art, temporary art
