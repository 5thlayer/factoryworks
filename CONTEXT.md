# PlanetaryFactory

A Minecraft 26.1.2 / NeoForge modpack that reproduces the progression, production-chain routing and
interplanetary scope of Factorio's Space Age expansion.

**Factorio is the subject; the mods are the implementation.** What the pack reproduces, adapts or
drops is the ledger in `docs/factorio-mechanics.md`, written in Factorio's terms and privileging no
mod. Which mod owns each capability is ADR-0017's table, amended by ADR-0060 — read it there rather
than here, because a copy of it in this file has now gone stale twice, and this file is a glossary
and nothing else. The pack registers its own machines where no installed mod can express Factorio's
recipe shape. KubeJS binds them into a stationary, automation-first loop.

No mod is the spine. Naming one where the concept, the Factorio mechanic or another mod's capability
is what is actually meant is the drift this file exists to prevent (`#94`).

How the pack is built out of our own mods -- a **Library** and the **Binding** that configures it for
Factorio's rules -- is 5thlayer/skillworks' vocabulary, defined in its `CONTEXT.md` (ADR-0090).

## Language

### Naming exceptions

The _Avoid_ lists below govern the pack's own prose. Two places quote Factorio deliberately and are
exempt: `data/factorio/*.json`, which is an extracted dump of Factorio's own prototypes, and
`docs/factorio-mechanics.md`, whose row keys are `Gleba` and `Vulcanus` because the ledger's value is
being diffable against Factorio. Both carry the `factorio-` or `data/factorio/` marker that says so.
Every body row names the pack's own body in its `where` field, so the mapping is never lost. See ADR-0028 —
this is not drift to be tidied up.

### Places

**Terra**:
The Overworld. The starter loop where basic extraction, first automation and the first rockets happen. The only body where Illager raids occur. Internal ID `overworld`.
_Avoid_: Overworld, home planet, spawn, Nauvis

**Terra Orbit**:
The orbit above Terra, reached by rocket, where asteroid chunks are harvested into Space Science and the first Platform is established. Internal ID `overworld_orbit`.
_Avoid_: space, the void, orbital dimension, Nauvis Orbit

**Ignus**:
The volcanic planet. Thermal and fluid processing; heavy metals and byproduct management. Internal ID `vulcanus`.
_Avoid_: the lava planet, Venus, Vulcanus

**Electro**:
The recycling planet. Has no natural ores; all material comes from recycling generated ruins. Internal ID `fulgora`.
_Avoid_: the scrap planet, Mars, Fulgora

**Sapros**:
The organics planet. Agricultural automation under spoilage time limits; sole source of Cryo-Pods. Internal ID `gleba`.
_Avoid_: the swamp, Glacio, Gleba

**Gelida**:
The ice planet. Cryogenics and heat management — fluids freeze without active heating. Internal ID `aquilo`.
_Avoid_: Aquilo, Glacio, the ice world

**Atlantis**:
The orbit-only endgame destination, reachable only once a Platform is established in its orbit. Its mechanics are deliberately undefined. Internal ID `shattered_planet`.
_Avoid_: the shattered planet, Fragmenta

**Display name**:
The Latin or Greek name a player sees for a **celestial body**, supplied by lang files only. Every body has one, and it is never the identifier. The convention is a body-naming rule and stops there (ADR-0004) — science packs and everything else keep their own names.
_Avoid_: label, alias

**Internal ID**:
The Factorio-derived identifier a body is registered under, never shown to players.
_Avoid_: registry name, dimension key

**Platform**:
A player-expanded orbital factory that mines and processes asteroids. Static — it never travels (ADR-0006).
_Avoid_: space station, orbital base, ship, vessel

### Terra terrain

**Sea**:
Terra's biome over water. The terrain decides it: a column is sea because its ground lies below sea level, and the biome is only the name painted on it, never the other way round. About a quarter of Terra, Nauvis's own share: a shallow **shelf** along the shore, then deep water whose floor is a thin seabed over bedrock (#356). Internal ID `terra_sea`.
_Avoid_: ocean, lake, water biome

**Shore**:
The thin band of Terra just above the water line. It is land in every sense, and an ore patch may lie on it, as Factorio's patches reach the water's edge (#356). Internal ID `terra_shore`.
_Avoid_: beach, coast, shoreline biome

**Land**:
Every Terra biome but the **Sea**, the Shore included. The only ground an **outfield patch** lands on.
_Avoid_: inland, dry land, continent

### Sapros terrain

Sapros's five biomes. Identifiers carry Factorio's terms so the mapping to the wiki stays free;
display names are plain English. Transcribed in `docs/research/gleba-worldgen.md`.

**Dark Highlands**:
Sapros's elevated stone biome, and where its stone is found. Internal ID `gleba_dark_highlands`.
_Avoid_: the highlands, mountains, uplands

**Midlands**:
Sapros's other elevated biome, distinguished from the Dark Highlands by carrying no shallow water. Internal ID `gleba_midlands`.
_Avoid_: orange midlands, turquoise midlands, the plateau

**Marshes**:
Sapros's wetland biome, found beside its deep water lakes. Holds neither tree. Internal ID `gleba_marshes`.
_Avoid_: blue marshes, the swamp, wetlands

**Green Marshland**:
The marshland where Yumako trees grow, and one of the two biomes bearing Stromatolites. Internal ID `gleba_green_marshland`.
_Avoid_: the green biome, yumako forest, the swamp

**Red Marshland**:
The marshland where Jellystem grows, and the other biome bearing Stromatolites. Internal ID `gleba_red_marshland`.
_Avoid_: the red biome, jellystem swamp, the swamp

### Sapros flora

**Yumako**:
The fruit harvested from a Yumako tree's leaves. The tree is felled to take it and replanted from a sapling; it is not a standing crop that regrows. A spoilable material, so ultimately four items per Freshness.
_Avoid_: yumako fruit, the orange fruit, fruiting leaves

**Jellystem**:
The tree of Sapros's red marshland, whose stem blocks yield Jellynut. Named for the tree, never for its fruit.
_Avoid_: jelly tree, jelly stem, the jelly plant

**Jellynut**:
The material taken from a Jellystem's stem blocks — from the trunk, not picked from a canopy. A spoilable material. Distinct from Jelly, which is what a Biochamber makes from it.
_Avoid_: jelly, jelly nut, jellyfruit

**Stromatolite**:
The surface-generated block of Sapros's two marshlands, mined by hand for iron or copper bacteria plus stone. It is not an ore and yields no metal directly; the bacteria become metal by Decaying.
_Avoid_: ore patch, bacteria ore, iron ore, copper ore

**Ore Bacteria**:
What a Stromatolite drops — Iron Bacteria or Copper Bacteria. A spoilable material whose Decay product is metal, which is the only way Sapros yields metal at all. Until the Decay engine ships it is inert (ADR-0016).
_Avoid_: bacteria ore, ore culture, iron dust, raw ore

**Saprine**:
The adjective for anything of Sapros — the rock its ground is made of, and the ore variants that rock would carry if the body had any veins. Never Mercurian: the block is GCyR's orphan, the name is not (ADR-0008).
_Avoid_: Mercurian, Gleban, Sapran

### Establishing a presence

**Orbital Starter Kit**:
GCyR's station package item, crafted on Terra and launched to orbit, where GCyR's own station creation builds the foundational Platform.
_Avoid_: platform kit, station seed

**Vanguard Kit**:
The item carried by an uncrewed rocket to a virgin planet, which deploys a minimal beachhead — platform floor and Receiving Terminal — so the player has somewhere to stand on arrival. It supplies no oxygen and no return trip.
_Avoid_: beachhead kit, lander, drop kit

**Gateway Flag**:
The global marker recording that a planet has a deployed landing platform and is therefore safe to travel to.
_Avoid_: unlock, planet flag, safe flag


### Moving things

The belt vocabulary -- belt, tile, slope, stretch, transport line, loader, splitter and the rest -- is Beltworks', and is defined in its `CONTEXT.md` (5thlayer/beltworks). The terms below are the pack's own.

**Logistics puzzle**:
The production-chain routing problem — what feeds what, at what ratio, over what distance. Explicitly
not the belt-lane micro-puzzle: lane balancing, sushi belts and weaving undergrounds through a fixed
footprint are 2D problems this pack does not have, and ADR-0044 records why.
_Avoid_: belt puzzle, the logistics game

**Dismantle**:
The generalisation of Beltworks' belt Dismantle to any **Dismantle Family**: taking up a span of one family from one block to another, both included, with the **Engineer's Pick**: a sneak-click stores the start, and a click names the end. Groundworks runs it for every family (#448). A pipe span is the shortest path between them through pipes joined to one another; an end outside the family, not joined to the start, or reached by two equally short paths is refused and keeps the start, and a sneak-click after the start is gone is a new start. What the span's blocks drop goes to the inventory, and what does not fit drops at the player's feet. Blocks outside the family, such as the machines a pipe run feeds, are never taken. A belt line is Beltworks' family, taken with the same Pick and the same clicks. Distinct from mining, which breaks one block and drops it (#404, #431).
_Avoid_: deconstruct, mass mine, unstretch

**Dismantle Family**:
The blocks one **Dismantle** takes up together as a single span, and the rule for the span between two of them. Fluid pipes are the Pack's family: the block tag `planetaryfactory:dismantle/pipes`, whose span is the shortest path, joined only where Oritech's connection is open (#431, #448). Belts are Beltworks' family.
_Avoid_: dismantle group, dismantle kind, replace group (a different grouping)

**Launch Terminal**:
The structure cargo and fuel are loaded into for a journey subject to a travel timer.
_Avoid_: rocket silo, launch pad, cargo bay

**Receiving Terminal**:
The surface structure that accepts arriving cargo on a planet with a deployed landing platform.
_Avoid_: landing pad, receiver, drop point

**Drop Hatch**:
The orbital structure that sends cargo down to a linked Receiving Terminal instantly and without fuel cost.
_Avoid_: cargo drop, chute

**Drop Pod**:
The temporary container generated on a surface when cargo is dropped with no Receiving Terminal present; it disappears once emptied.
_Avoid_: crate, temp chest, cargo pod

**Flight**:
An in-progress journey — cargo or passenger — held as data with a remaining travel timer rather than as a moving entity.
_Avoid_: shipment, trip, transit

**Simulation Handoff**:
The deferred transition of a fully automated Platform from a physically built factory to a background throughput calculation. A contingency held in reserve against measured TPS cost, not a system currently being built.
_Avoid_: abstraction, going virtual

### Handling things

**Obtainable**:
An item or fluid a player can come to hold on a live body without creative mode: a pack recipe makes it, the starting kit grants it, a block the live worldgen places drops it, a mob the live biomes spawn drops it, or a mechanic produces it (a filled bucket, a Pumpjack's crude, a Boiler's steam). A parked body's worldgen does not count until it is live. Only an Obtainable item is listed in the recipe viewer's index (#173), and not every one: an item that is no part of the factory, such as the quest book, is hidden even though the starting kit grants it (#458).
_Avoid_: reachable (that is **Reach**), available, craftable (a mob drop is Obtainable and not craftable)

**Stock interaction**:
Vanilla behaviour outside any recipe that turns an item or block into a different one: stripping a log, tilling dirt, water meeting lava, concrete powder hardening, waxing copper, a leaf's drop. None ships unless a decision names it, as a stock recipe does not (#440); a sapling growing into a tree is the one named (ADR-0051). A mechanic the pack builds, such as felling or the Offshore Pump, is not a stock interaction, and what a denied one would make is not Obtainable.
_Avoid_: world interaction (a pole's wire and the Pick's Dismantle act on the world too), recipe

**Shelf**:
Where an Obtainable stack is listed in Factorio's crafting menu: an item subgroup and an order within it. A stack takes the shelf of the Factorio item it maps to; one with no Factorio item borrows a neighbour's, listed just after it (#458).
_Avoid_: category (that is a recipe's), group (a shelf's subgroup belongs to one)

**Reach**:
How far the player places a block, uses a block and breaks a **Building**: 16 blocks, one chunk. Longer than Factorio's build distance of 10, as Satisfactory builds from far off. Anything that is not a Building breaks only within Minecraft's own reach of 4.5, so ore, trees and terrain are dug up close. Entities are reached at vanilla's 3 (#413).
_Avoid_: build distance, range, interaction range

**Building**:
A block the player places as part of the factory: every machine, belt piece, pole, pipe, rail, chest and wall. Factorio's own split: what it places as an entity is a Building, and what it lays as a tile, such as stone brick, concrete or landfill, is not. Ore, trees and terrain are not Buildings, and neither is a building block such as bricks, planks or glass, placed or not.
_Avoid_: entity, structure, machine (a Building that runs)

**Quick transfer**:
The gesture that moves the held stack into the block the player is looking at, or — with an empty hand — takes out everything that block will give up, without opening its screen. **Reach** is the player's own, so the gesture and the screen answer to the same ray trace. It reads and writes through the target's item handler and imposes no slot policy of its own, which is why it can never strip a furnace of its fuel or of an input it has not smelted yet, and why a GregTech machine mid-recipe has nothing left to take back.
_Avoid_: fast entity transfer, ctrl-click, quick insert, fast transfer

**Quick split**:
Quick transfer at half the magnitude — half the held stack in, or half of what the block will give up out.
_Avoid_: fast entity split, ctrl-right-click, half stack transfer

### Terra's opening

**Starting area**:
The structure stamped onto world spawn once per world, and the only place a **starting field** is found: a **hub**, the four fields it deals, and the **water pool**. Anchored to spawn rather than to the world origin, and so not a thing ordinary worldgen places (ADR-0019 and its amendment).
_Avoid_: spawn structure, starting hub, tutorial area, start island

**Hub**:
The starting area's centre piece, and where the player spawns. It places no terrain of its own: it is what holds the four fields apart and carries the water pool.
_Avoid_: spawn platform, base, hub structure

**Water pool**:
The body of water in the hub, one block deep and flush with the ground. Since water is never created (ADR-0050), it is what makes water somewhere rung 0 already stands rather than somewhere it has to go.
_Avoid_: pond, lake, starting water, spawn pool

### Terra's ore

**Ore patch**:
The only shape ore takes on Terra: a filled disc of a single ore block, one block deep, lying flush with the terrain surface. There are no buried veins — the whole planet deals the same shape, and a patch is one resource rather than a mix, so a patch answers "what is this a patch of" with one word (ADR-0045).
_Avoid_: vein, deposit, ore blob, ore body, ore field

**Starting field**:
One of the patches the starting area deals at spawn, whose total is Factorio's stated starting amount divided over the blocks that actually landed. Distinguished from an **outfield patch** because Factorio states its total and the world has to count its blocks; everything else derives both.
_Avoid_: starting patch, spawn patch, tutorial patch

**Outfield patch**:
Every ore patch beyond the starting area. Placed by ordinary worldgen at Factorio's own spacing, excluded from the first 150 blocks around the origin, land-only, and reached by rail rather than by belt — a patch is roughly forty chunks from its neighbours of the same resource, and a uranium patch nearly a hundred (ADR-0045).
_Avoid_: regular patch, wild patch, remote patch

**Amplitude**:
How much one block of a patch holds. It rises with distance from the world origin up to a cap, after which further quantity widens the patch instead.
_Avoid_: richness, density, per-tile amount

**Radius**:
How wide a patch is. Fixed until the **amplitude** cap is reached and growing with distance beyond it, which is why a far patch is bigger as well as richer. Amplitude and radius split a patch's quantity between them; they never multiply it.
_Avoid_: size, footprint, patch size

**Radar**:
The building that charts map at range for its team: the chunks it scans appear on the map as if walked, and each **outfield patch** it charts gets a marker showing what the patch had left when last charted or walked past, gone once the patch is mined out. It detects nothing hidden — ore is visible where it lies, so finding a patch is exploration rather than prospecting, and the marker only labels what the chart already shows (ADR-0045, ADR-0079).
_Avoid_: prospector, scanner, ore detector

**Sector**:
What a **Radar** charts in one scan: a 32×32-block square, which is Factorio's chunk and four Minecraft chunks. A Radar charts the sectors around it at once, then one sector at range at a time, unexplored ones first, and re-scans its reach in turn once all are charted.
_Avoid_: chunk (a Minecraft chunk is a quarter of a sector), scan area

**Chart**:
What a team has seen through its Radars: every sector a Radar of theirs has charted. It belongs to the team, so a player who joins later or was offline receives it too.
_Avoid_: explored area, revealed map, fog

### Powering things

**Steam**:
The pack's own low-temperature fluid, made by the **Boiler** from water and solid fuel and consumed by the **Steam Engine**. Real, pipeable and buffered, as in Factorio, and `planetaryfactory:` rather than any mod's (ADR-0048).
_Avoid_: low-pressure steam, LP steam, GT steam

**Superheated Steam**:
The pack's own high-temperature fluid, emitted directly by the reactor with no heat layer (ADR-0033) and accepted only by the **Steam Turbine**. The **Steam Engine** will not take it.
_Avoid_: high-pressure steam, hot steam, 500-degree steam

**Boiler**:
Terra's rung 0 pack-authored machine that burns solid fuel to turn water into **Steam**. One tier; joules in a buffer drained at its own rate (ADR-0047). It replaces the mod boiler the ledger used to name. Its water draw is Factorio's own 60 mB/s, which makes one **Offshore Pump** exactly twenty Boilers (ADR-0050).
_Avoid_: LP Solid Boiler, heater, steam generator

**Steam Engine**:
The pack's rung 0 generator: it burns **Steam** into its own charge, faster the fuller its steam tank, and stops when that charge is full; the steam is spent, not returned as water. Engines placed in a row chain behind one **Master Engine**. It has no wire; it joins an **Electric Network** by standing inside a **Supply Area Pole**'s area. A pack block on Oritech's engine, placed as a footprint from one item and broken as one (ADR-0077); not Oritech's own Steam Engine, which is recipe-removed; hiding it is #173's.
_Avoid_: Create's Steam Engine, Oritech's steam engine, alternator, turbine


**Master Engine**:
A **Steam Engine** holding steam, which burns and holds the charge for itself and the empty engines beside it in its row; those only mirror its speed. It is not chosen: the first engine of a row to receive steam becomes one, and steam piped into any other engine of its row reaches its tank. A pole that reaches any engine of the row reaches its Master Engine.
_Avoid_: master/slave, lead engine, chain head

**Electric Network**:
Every **Supply Area Pole** joined to another by a **Wire**, directly or through other poles, plus every generator, accumulator and machine standing in any of their areas. One balance of supply and demand; there is no second carrier.
_Avoid_: grid, power net, FE network

**Wire**:
A connection between two **Supply Area Pole**s that makes them one **Electric Network**. Only a wire joins poles: two poles within reach of each other but not wired are not connected. A wire can only exist between poles within wire reach. Placing a pole adds wires on its own, and the player adds or cuts one by hand with the **Engineer's Pick**. A wire belongs to its two poles and goes when either pole is broken — a column gaining a segment below its base is not a new pole and keeps its wires, while breaking any segment drops the column above it, so a broken base takes the whole column and its wires with it.
_Avoid_: link, cable, connection

**Redstone**:
The pack's circuit network: vanilla redstone dust, laid free by a right-click with the **Engineer's Pick** wherever a dust item could go, once the `circuit-network` research is done. It is never an item: nothing crafts it, and it drops nothing when broken, washed away or left unsupported. A click the Pick already answers — a **Dismantle**'s end, a **Wire**, a pipe connection — lays none. Distinct from a **Wire**, which carries power between poles, never a signal.
_Avoid_: wire, circuit wire, redstone dust item

**Placement Plan**:
What a held item would do at an aimed spot: the positions it would fill, the blockstate at each, and a refusal or none. Placing executes a plan, and the **Placement Preview** draws one, so both ask one rule (ADR-0069). A multiblock is one plan and refuses whole. A belt piece's plan is Beltworks' own Placement Plan, asked through the same entry point.
_Avoid_: placement context (vanilla's own type, one input to a plan), build plan, preview state

**Dismantle Plan**:
What a **Dismantle** would take up at an aimed block: the blocks of the span from the stored start, and a refusal or none. Dismantling executes a plan, and the preview draws one, so both ask one rule, as a **Placement Plan** does; the two are separate things. In code it is Groundworks' `DismantleSpan` (#404, #448).
_Avoid_: removal plan, placement plan (for a dismantle)

**Placement Preview**:
What a player sees while holding a placeable block -- the pack's, or any block with a facing, an axis or a rotation -- and aiming at a spot: the block drawn translucent where placement would put it, red where placement would be refused; for a pole, also the wires it would add and its **Supply Area Box**; with the **Engineer's Pick** and a dismantle's start stored, the blocks its **Dismantle Plan** would take up, in red, and none when it would be refused. It shows what placing or dismantling would do and changes nothing in the world.
_Avoid_: ghost (Factorio's ghost is an entity left for robots to build, a mechanic the pack excludes), hologram, blueprint preview

**Fast Replace**:
Placing a block over a placed one of the same **Replace Group** but another tier, which swaps it in place, up a tier or down. It takes one item and gives the old one back, per entity, so a whole pole column swaps for one item and keeps its height and wires. The new block keeps everything of the old one's it can hold, the Assembling Machine's recipe included where the new tier can craft it, and the rest goes to the player; if the player cannot take it, nothing is replaced. The new block keeps the old one's facing. A plain right-click with another tier of the group in hand replaces, rather than opening the block's screen, and on a multiblock any of its blocks answers; a sneak-right-click still places beside. The **Placement Preview** draws a replace in a colour of its own.
_Avoid_: upgrade (it goes down a tier too), swap, overwrite

**Rotate**:
The Pack's name for Groundworks' **Rotate the Plan** (held) and **Rotate in Place** (placed), which the library runs (#451). One of the pack's two rotation actions, on a key of its own (`R` by default) and reused by everything with an orientation. Held: it turns the facing of what is about to be placed, a quarter turn each press, and the **Placement Preview** redraws with it. The turn is relative to the way the player looks, not a compass direction as Factorio's is, because Factorio's camera never turns and the player's does. Placed: with nothing rotatable held, it turns the block under the crosshair in place, and what turning means is the block's own -- a belt tile turns, a machine keeps its contents. A block whose turned shape does not fit is refused with its reason and nothing changes; there is no preview of a placed rotation. Factorio's rule for which target the key takes: the held item if it is rotatable -- it places a block with a facing, an axis or a rotation -- otherwise the aimed block.
_Avoid_: wrench rotate (a departed GregTech verb, #386), turn, rotate key

**Reverse Rotate**:
**Rotate** the other way (`Shift+R` by default), on both targets. A separate action rather than a modifier so it can be rebound alone, as in Factorio.
_Avoid_: counter-rotate, rotate back

**Replace Group**:
The blocks that can **Fast Replace** each other, read from Factorio's `fast_replaceable_group` rather than chosen: the small and medium poles are one group and the substation is alone; the three furnaces are one; the three Assembling Machine tiers are one; belts and splitters of every tier are one; loaders of every tier are one.
_Avoid_: family, tier ladder (a ladder is one kind's tiers; a group can hold two kinds, as belts and splitters do)

**Supply Area Box**:
The bright yellow wireframe of a **Supply Area Pole**'s area — the whole volume it covers, anchored at the base, plus an outline around every machine the pole reaches. Shown while holding a pole or looking at a placed one (ADR-0070). It says where the area lands and what is inside it, never whether anything inside is being *fed*, which is the Jade line's answer on the machine.
_Avoid_: supply area overlay, footprint overlay, coverage grid, range indicator

**Offshore Pump**:
The pack-authored block that is the **only** origin of water on any body: placed against one natural water source block, it emits 1,200 mB/s and needs no power, both Factorio's own figures (ADR-0050). ADR-0048 had written it off; that call is reversed, because Create's Mechanical Pump moves fluid between pipes and never touches world water.
_Avoid_: water pump, water extractor, intake, Mechanical Pump

**Natural water**:
Water a world generator or a structure placed — the only water the **Offshore Pump** accepts. It is not a tracked property and needs no marking: the pack never creates a source block, so every source in the world is natural by construction (ADR-0050). The rule in one line is that **water is extracted and transported, never created**.
_Avoid_: real water, unplaced water, virgin water, source water

### Making things

**Rung**:
A science pack's tier of Terra's research ladder: rung 0 has no pack, then `automation`, `logistic` and `chemical`. The launch is on rung 3; `production` is the first rung after it (ADR-0097). Each rung opens on a **Gate**, but most gates open a chapter inside a rung.
_Avoid_: tier, age, era, stage

**Gate**:
A Factorio technology that opens a chapter of Terra's arc: a **Rung**'s science pack, or a trigger technology researched by doing something rather than by packs, such as crafting a Lab or mining crude. Every chapter boundary is one, read off the corpus; the pack invents none (ADR-0097). Unrelated to a **Gated recipe**, which is about where a recipe is crafted.
_Avoid_: milestone, checkpoint, unlock

**Plate**:
The pack's one item per material — FTB Materials', for every metal. It is what a furnace yields, since ore smelts 1:1 to a plate with no ingot step, and it is the form every recipe consumes. There is exactly one per material and never a second: where another mod ships a rival form for the same material, the rival's recipes are removed and it becomes unobtainable. The metal-derived intermediates — gear, rod, wire — come from the same supplier. Ingots, nuggets, dusts and raw forms exist in the jars but the pack does not use them.
_Avoid_: sheet, ingot, GT plate, unified plate

**Engineer's Pick**:
The player's only mining tool, in two tiers — **Engineer's Iron Pick** and **Engineer's Steel Pick** — both indestructible, the steel one unlocked by the `steel-axe` research and crafted from the iron one, which it consumes. It mines every block class, so the pack has no axe, shovel or shears, and it is what **dismantles** a pipe run and a belt line, the only tool that does in the Pack. The tiers differ only in mining speed: Terra's ores, coal and stone take a flat second by hand and half a second after the research, while everything else keeps vanilla hardness. Factorio's two mining speeds and the ratio between the tiers are kept, but the mining time itself is the pack's — half of Factorio's, after 2.0s failed ADR-0039's human-on-delivery check.
_Avoid_: pickaxe, the pick, mining tool, wrench

**Burner Mining Drill**:
Terra's rung 0 drill and the pack's own block: it burns solid fuel, occupies one place, and breaks the ore blocks in an area beneath it. It exists because Factorio's opening machine is a fuel-burning drill and GregTech ships none — its extraction line starts at a steam miner fed by a boiler — so ADR-0040 authors it first-party and removes the LP Steam Miner. It is in the opening pocket rather than crafted, because a drill covering four tiles beats hand-mining from the first minute.
_Avoid_: burner drill, steam miner, LP Steam Miner, mining rig

**Basic Miner**:
Terra's rung 1 drill, `gtceu:lv_miner`, and the ladder's second and last rung — it arrives with the electricity that runs it and is what makes the **outfield patches** worth reaching. GregTech owns the electric ladder; rung 0's drill is the pack's (ADR-0040).
_Avoid_: Basic Ore Drilling Rig, electric drill, LV miner

**Operation**:
One unit of mining work against an ore block: it consumes one unit of the block's amount and pays out `yield` items. Drills and hands both perform them, which is what makes "seconds per ore" literal rather than aspirational. Terra's two drills differ in operations per second, footprint and reach, never in yield, which stays 1.0 until a productivity bonus raises it on another body.
_Avoid_: mining tick, drill cycle, swing

**Personal Assembler**:
The player's inventory screen, as the player's only hand-crafting surface. It is Craftworks' mechanic, and Craftworks' glossary defines it (ADR-0089). The pack gives it a copy of each **Assembling Machine** recipe that Factorio marks hand-craftable — first category `crafting`, minus the eleven Factorio withholds (`#88`) — crafted at speed 1, serially (ADR-0029). It **replaces** the crafting grid, and crafts nothing by hand directly: every craft is a **Crafting Plan**. It is always present: it is taught by the opening, never granted by it (`#100`).
_Avoid_: hand crafter, personal crafter, portable crafter

**Crafting Plan**:
The resolved, flattened tree of crafts the Personal Assembler produces when an amount is chosen, and the unit in which the player commits and is refunded. It names every intermediate it will make, every ingredient the player lacks and every recipe the team has not researched; it is paid for in full when it starts and is never re-resolved. Craftworks' term (ADR-0089).
_Avoid_: crafting job, batch, order

**Assembler queue**:
The serial list of Crafting Plans awaiting execution. One plan runs at a time, and a plan whose finished craft cannot fit in the player's inventory pauses and stops the whole queue rather than dropping anything. Craftworks' term (ADR-0089).
_Avoid_: crafting queue, backlog

**Missing ingredient**:
A leaf of a Crafting Plan the player does not have and the Assembler cannot make — it is mined, smelted or machine-made. Distinct from **Locked**, which the player cannot make *yet*: the two demand different actions, so the plan names them separately.
_Avoid_: shortfall, unavailable

**Locked**:
A recipe inside a Crafting Plan that the team has not researched: Craftworks asks Researchd about the hand copy, which unlocks with its machine recipe (ADR-0089). The resolver plans only through unlocked recipes, so a locked intermediate stops a plan exactly as a missing ingredient does, for a reason the player fixes with research rather than with mining.
_Avoid_: unavailable recipe, gated

**Assembling Machine**:
The machine that runs Factorio's crafting recipes, on Oritech's chassis: a `planetaryfactory_core` subclass reusing Oritech's model, energy storage, inventory and addons, and replacing its craft cycle whole (ADR-0071). It runs `planetaryfactory:assembling` (ADR-0063), holds a **Held recipe** rather than matching on input, and is placed as a footprint from one item like every other pack block (ADR-0069). It comes in three tiers, one block each, at Factorio's speeds and in Factorio's colours; tiers 2 and 3 also craft with a fluid (`#295`). Oritech's addons are not the tier ladder -- what they are is #120's. Not Oritech's own Assembler, which is a rival for the same row and is recipe-removed and hidden.
_Avoid_: assembler, Oritech assembler, GT assembler, crafter, fabricator

**Held recipe**:
The single `planetaryfactory:assembling` recipe a player sets on an **Assembling Machine**, which the machine then runs and nothing else. It is Factorio's own gesture: the machine is told its recipe rather than deducing one from what it is fed, so there is no lookup, no first match and no ambiguity between two recipes sharing an ingredient set. The machine's inputs are filtered to it, and it is held whether or not the machine can currently run it — an unfed, unresearched or output-blocked machine displays its Held recipe and idles, and never clears it silently. Stored as the recipe id, which is stable and is what research unlocks already key on.
_Avoid_: locked recipe, recipe lock, selected recipe, machine lock

**Gated recipe**:
A recipe whose type routes it away from the crafting grid to a machine or the Personal Assembler. Gating is a recipe-authoring choice, not a scripted restriction.
_Avoid_: locked recipe, blocked recipe

**Research lock**:
A Researchd `unlock_recipe` effect withholding a recipe from a team until they research it. Distinct from a **Gated recipe** in both mechanism and meaning: gating is where a recipe is crafted and is permanent, a research lock is whether a team may craft it yet and is lifted by play. A lock is held per team, so the same recipe can be locked for one team and not another.
_Avoid_: recipe unlock, tech lock, gated recipe

**Lock annotation**:
The badge and tooltip a recipe viewer draws on a recipe under a **Research lock** the viewing player's team has not lifted, naming the research that would lift it. The pack annotates rather than hides, in both EMI and JEI and for every recipe source alike: hiding is vanilla's habit and tells the player nothing, and applying either policy to one viewer or one recipe source only relocates the incoherence (issue #75).
_Avoid_: hidden recipe, greyed-out recipe, locked overlay

**Unowned machine**:
A machine carrying no Researchd placed-by attachment, so it belongs to no team and no **Research lock** applies to it — it runs every recipe. Ordinary placement always stamps an owner; this is what `/setblock`, `/clone` and worldgen leave behind. Failing open is deliberate (issue #74, ADR-0058's amendment).
_Avoid_: ownerless machine, orphan machine, teamless machine

**Fuel buffer**:
The joules a burner furnace holds. Lighting a fuel item consumes it whole and adds its `fuel_value` to the buffer; a tick of work subtracts the machine's own `energy_usage / 20`, which is 4,500 J on both burner tiers. Nothing is measured in burn ticks and there is no conversion constant — burn time is a quotient, as it is in Factorio, and a buffer that still holds joules keeps them while the furnace is idle. It is the same quantity the Electric tier's buffer holds and is shown with the same gauge; only the way it is refilled differs (ADR-0047).
_Avoid_: burn time, fuel ticks, lit ticks, burn value

**Fuel category**:
Factorio's classification of a fuel item, carried in the pack's fuel table and filtered on: a furnace burns `chemical` and nothing else. An item with no row in the table is not fuel, which is ADR-0034's default-deny applied to burning. The filter is what stops a `nuclear` item becoming furnace fuel merely by having a `fuel_value`.
_Avoid_: fuel type, burnable, fuel class


### The oil chapter

**Oil Refinery**:
The machine that splits crude: basic and advanced oil processing, two fluids in and three out, the only machine in the pack that emits three fluids at once (ADR-0025). A pack block wearing Oritech's Refinery with both of its chamber layers, placed and broken as one footprint from one item, always with three outputs. It holds a **Held recipe** of its own recipe type, not the **Assembling Machine**'s. Not Oritech's own Refinery, whose chambers add outputs one at a time and which is recipe-removed and hidden. Coal liquefaction is Space Age and arrives with Ignus or not at all (#12).
_Avoid_: distillation tower, refinery multiblock, cracker, refinery chamber

**Chemical Plant**:
The machine carrying Factorio's chemical-plant recipe list: both crackings, lubricant, plastic, sulfur, solid fuel, sulfuric acid and battery (ADR-0025). Up to two items and two fluids in, one item and one fluid out. A pack block wearing Oritech's Centrifuge, on the Centrifuge's footprint; it holds a **Held recipe** of its own recipe type, not the **Assembling Machine**'s. Not Oritech's own Centrifuge, which is recipe-removed and hidden.
_Avoid_: chemical reactor, chem plant, reaction chamber, centrifuge

**Oil well**:
One block, flush with the terrain, that holds an amount of **Crude Oil** and never runs dry: each draw lowers its amount toward a floor — a fifth of what it started with, or 20% yield, whichever is higher — and its yield, the fraction of full output a Pumpjack gets from it, falls with it. A far well can start well above 100%. A crude-oil field is a scattering of wells, not a filled patch. It is the only crude source anywhere in the pack (#377).
_Avoid_: oil spring, oil deposit, bedrock fluid deposit

**Pumpjack**:
The machine that draws **Crude Oil** from one **oil well**. It stands on the well and nowhere else, runs on electricity, and its output is its full rate times the well's yield. Crude leaves it through any face (#377).
_Avoid_: pump, fluid drilling rig, oil derrick

**Crude Oil**:
The unprocessed fluid a **Pumpjack** draws from an **oil well**, and the sole input to oil processing. It never runs out: a well's yield falls toward a floor and stops there, as in Factorio (#377).
_Avoid_: raw oil, oil, petroleum, oil spring

**Petroleum Gas**:
The lightest fraction, and the one that feeds sulfur and plastic. `oritech:still_diesel`, drawn in Factorio's colour (ADR-0067).
_Avoid_: refinery gas, natural gas, naphtha

**Heavy Oil** / **Light Oil**:
The two heavier fractions. `oritech:still_heavy_oil` and `oritech:still_naphtha`, which Oritech names Naphtha (ADR-0067). Heavy Oil is also what Electro's oceans are made of (ADR-0009).
_Avoid_: heavy fuel, light fuel, fuel oil, kerosene

**The oil chapter**:
Everything from crude to plastic, lubricant and launch fuel. It opens on the `oil-processing` **Gate**, which mining crude researches, and spans rungs 2 and 3 rather than sitting in one, because sulfur is petroleum-derived and sulfur gates chemical science (ADR-0097).
_Avoid_: the oil rung, rung 4, the petroleum tier

### Spoiling

**Decay**:
The process by which an organic material loses freshness over time and is eventually replaced by
something else. It runs continuously, everywhere, on every body and in flight — it is not a property
of any one machine or dimension.
_Avoid_: spoiling, rotting, decomposition, aging

**Freshness**:
How far through Decay a material has travelled, expressed as one of four named states rather than a
percentage or a timer. Freshness is part of what a material *is*, not a hidden value attached to it.
_Avoid_: spoilage level, staleness, condition, quality, durability

**Fresh**, **Ripe**, **Stale**, **Spoiling**:
The four freshness states, in order. **Spoiling** is the last state before a material is replaced,
not the state of having been replaced — that is Spoilage.
_Avoid_: spoiled (for the fourth state), stage 1-4, tier

**Spoilage**:
The material that organics become at the end of Decay. It is a feedstock in its own right, not
waste — biosulfur is made from it.
_Avoid_: rot, waste, compost, garbage, trash

**Biochamber**:
The Sapros machine that every recipe involving a spoilable material runs in. Processing organics
elsewhere is not an alternative path; there is no alternative path.
_Avoid_: bioreactor, fermenter, organics processor

**Clog**:
The state of a machine holding a material that has Decayed past what its recipe accepts, halting it
until the Spoilage is removed. A stated hazard the player is responsible for designing around, not a
fault. A Clog is an inability to consume, never a refused write — Decay writes slots directly and
cannot be turned away.
_Avoid_: jam, deadlock, stall, blockage

**Drain**:
The player-built route by which Spoilage leaves a Clogged machine. Terminal Spoilage, and only
terminal Spoilage, may be pulled out of a bus that otherwise refuses extraction, so a hopper under an
input bus clears a Clog while leaving every un-Decayed stage locked inside. There is no dedicated
bus and no automatic removal: the Drain is something the player builds, or does not.
_Avoid_: purge, trash, trash slot, reject, waste output, eject, idle draw (Factorio's `drain`, an
unrelated mechanic -- ADR-0029)

### Hazards

**Emission**:
The per-chunk score accumulated from the EU/t draw of every running machine, which decays over time and spreads to neighbouring chunks. EU/t is the input because it is the one number a machine on a GregTech chassis already exposes, not because the mechanic is GregTech's — it is the pack's own construct, and GTCEu has no pollution system (ADR-0005).
_Avoid_: pollution, smog, contamination

**Overseer**:
The stationary villager that anchors an outpost and that raiding Illagers path toward. Manufactured from Sapros organics, not bred or recruited.
_Avoid_: villager, anchor, guard

**Command Center**:
The block an Overseer is deployed onto; it powers the outpost and halts it if its Overseer dies.
_Avoid_: core block, outpost controller

**Cryo-Pod**:
The item produced from Sapros organics that deploys into an Overseer.
_Avoid_: villager egg, pod

**Dormant Siege**:
The state of an unloaded outpost whose accumulated Emission has crossed the raid threshold; its production halts and the raid instantiates only when a player arrives.
_Avoid_: pending raid, queued attack

### Orbit

**Dyson Swarm**:
The late-game orbital power infrastructure.
_Avoid_: solar swarm, dyson sphere
