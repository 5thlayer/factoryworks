# The Factorio mechanic ledger

Every mechanic Factorio has, base game and Space Age, and what FactoryWorks does about it: built as
Factorio has it, adapted to Minecraft's shape, planned, blocked, or left out on purpose, each with its
reason. It is the list the pack is developed against, so it changes as the pack does.

The first release is Terra, the starting planet. The other planets are parked until travel between
them is built, so a mechanic that belongs to one of them says *parked*. Mechanics are named as
Factorio names them, and each section gives the pack's own name beside it.

## Verdicts

| verdict | meaning |
| --- | --- |
| `planned` | in, not built |
| `shipped` | in, built — registered *and* its warranted check under `docs/testing/what-to-check.md` passing |
| `adapted` | in, but Minecraft's shape differs. Mandatory `notice` sentence |
| `blocked` | wanted, no known implementation |
| `excluded` | deliberately not reproduced. Requires a written reason |

## Summary

### Base game

| mechanic | verdict | where |
| --- | --- | --- |
| [Resource patches and finite ore](#resource-patches-and-finite-ore) | `adapted` | Terra; Ignus, Sapros parked |
| [Manual mining](#manual-mining) | `adapted` | Terra |
| [Trees and wood](#trees-and-wood) | `adapted` | Terra |
| [Mining drills](#mining-drills) | `adapted` | Terra |
| [Water as a resource](#water-as-a-resource) | `adapted` | Terra |
| [Fluid handling](#fluid-handling) | `shipped` | Terra |
| [Oil processing](#oil-processing) | `planned` | Terra; Ignus, Gelida parked |
| [Smelting](#smelting) | `shipped` | Terra |
| [Assembling machines and recipe categories](#assembling-machines-and-recipe-categories) | `shipped` | Terra |
| [Handcrafting and the crafting queue](#handcrafting-and-the-crafting-queue) | `shipped` | Terra |
| [Transport belts](#transport-belts) | `adapted` | Terra |
| [Inserters](#inserters) | `adapted` | Terra |
| [Logistic robots](#logistic-robots) | `excluded` | — |
| [Construction robots and blueprints](#construction-robots-and-blueprints) | `excluded` | Terra |
| [Building by hand: placement preview and fast replace](#building-by-hand-placement-preview-and-fast-replace) | `shipped` | Terra |
| [Trains](#trains) | `adapted` | Terra |
| [Circuit network](#circuit-network) | `adapted` | Terra |
| [Electric network and transmission](#electric-network-and-transmission) | `adapted` | Terra |
| [Power generation](#power-generation) | `adapted` | Terra |
| [Nuclear fission](#nuclear-fission) | `adapted` | Terra |
| [Pollution](#pollution) | `planned` | Terra |
| [Enemies and evolution](#enemies-and-evolution) | `planned` | Terra |
| [Wildlife and natural mob spawning](#wildlife-and-natural-mob-spawning) | `shipped` | Terra |
| [Combat: guns, ammo, turrets, walls](#combat-guns-ammo-turrets-walls) | `planned` | Terra |
| [Armor and the equipment grid](#armor-and-the-equipment-grid) | `planned` | Terra |
| [Capsules](#capsules) | `planned` | Terra |
| [Modules and beacons](#modules-and-beacons) | `blocked` | — |
| [Research and science packs](#research-and-science-packs) | `planned` | Terra |
| [The technology tree](#the-technology-tree) | `shipped` | Terra |
| [Rocket silo and rocket launch](#rocket-silo-and-rocket-launch) | `planned` | Terra |
| [Character movement on foot](#character-movement-on-foot) | `adapted` | Terra |
| [Character reach](#character-reach) | `adapted` | Terra |
| [Personal transport](#personal-transport) | `blocked` | — |
| [Terrain modification](#terrain-modification) | `adapted` | Terra |
| [Repair and entity damage](#repair-and-entity-damage) | `blocked` | — |
| [Radar and map exploration](#radar-and-map-exploration) | `adapted` | Terra |
| [The logistic request and trash system](#the-logistic-request-and-trash-system) | `excluded` | — |
| [Day and night cycle](#day-and-night-cycle) | `shipped` | Terra; Sapros parked |
| [Crash site](#crash-site) | `planned` | Terra |
| [Controls](#controls) | `planned` | Terra |
| [Factoriopedia](#factoriopedia) | `planned` | Terra |

### Space Age

| mechanic | verdict | where |
| --- | --- | --- |
| [Interplanetary travel](#interplanetary-travel) | `blocked` | between planets, parked |
| [Space platforms](#space-platforms) | `blocked` | orbits, parked |
| [Asteroid mining and reprocessing](#asteroid-mining-and-reprocessing) | `blocked` | orbits, parked |
| [Interplanetary logistics](#interplanetary-logistics) | `blocked` | between planets, parked |
| [Spoilage](#spoilage) | `adapted` | Sapros parked |
| [Quality](#quality) | `blocked` | — |
| [Recycling](#recycling) | `planned` | Electro parked |
| [Vulcanus: lava and calcite](#vulcanus-lava-and-calcite) | `planned` | Ignus parked |
| [Fulgora: scrap and lightning](#fulgora-scrap-and-lightning) | `planned` | Electro parked |
| [Gleba: agriculture and nutrients](#gleba-agriculture-and-nutrients) | `planned` | Sapros parked |
| [Aquilo: cold and ammonia](#aquilo-cold-and-ammonia) | `planned` | Gelida parked |
| [Planet-locked buildings](#planet-locked-buildings) | `planned` | each planet; Terra only so far |
| [Elevated rails](#elevated-rails) | `excluded` | — |
| [Fusion power](#fusion-power) | `planned` | Gelida parked |
| [The Shattered Planet](#the-shattered-planet) | `blocked` | Atlantis parked |

---

## Conventions

The Summary's `where` names what the first release has. A section's `where` names every body the design
places the mechanic on, parked or not.

**Row keys are Factorio's own names** (`Gleba`, not Sapros; `Vulcanus`, not Ignus). This is a declared
exception to `CONTEXT.md`'s _Avoid_ lists, on the same footing as `data/factorio/*.json`: the ledger's
value is being diffable against Factorio, so it must speak Factorio. The pack's name for the same
thing appears in `where`. See ADR-0028.

**This file is not derived from `data/pack/subgroup-owner.json` and does not derive it.** That file
answers _is a Factorio recipe emitted, and on whose machine_. This one answers _does the mechanic
exist in the pack, by any means_. **`not_emitted` is never evidence for `excluded`** — `combat/defensive-structure`
is `not_emitted` and the pack still ships a Radar (#57).

**This file does not place anything on a progression ladder.** A row of `planned` says the pack has
the mechanic; where on Terra's ladder it lands is #25's call, and it may land nowhere near Terra.

`owner` is an ADR/issue link where the decision was already made, `unargued` where this ledger is the
first place it has been written down, or `by-consequence` where it fell out of a decision about
something else and was never argued on its own merits. **`by-consequence` rows are owned by this
ledger**, not by the ticket that caused them.

`via` reuses `subgroup-owner.json`'s owner tokens — `gregtech`, `create`, `powergrid`,
`pack`, `kubejs`, `native_mechanic` — and a value must exist in `index.toml`. **`gcyr` is no longer
one of them** (ADR-0060): GCyR left the manifest, and the rows that named it list their candidates
instead. **`mekanism` is
no longer one of them** (ADR-0035): the mod is out of the manifest, so a row naming it would fail the
must-exist rule. **`electro` is no longer one of them either** (#148): it named Create: Electro
Energetics, which Create: Power Grid replaced, and it never satisfied the must-exist rule in the
first place — the mod id was `electroenergetics`. The three rows that wrote `electro` (GCyR) meant
GCyR. `candidates` is free
text and commits to no jar; **`pack` is admissible as a candidate only with a named mechanism**
(ADR-0015).

---

## Base game

### Resource patches and finite ore

- **verdict**: `adapted`
- **where**: Terra, Ignus, Sapros
- **via**: `factoryworks_core`
- **owner**: ADR-0019, ADR-0020, ADR-0021, ADR-0041, ADR-0045, ADR-0060
- **notice**: a patch is one block deep, flush with the terrain, its ragged edge drawn from
  Minecraft's noise, and an ore block shows its amount by stage and Jade line rather than a map layer.
- **ticket**: #320, #321 and #377 closed with Terra's patches shipped; Ignus and Sapros land with #12
  and #23

Terra deals one ore shape: a filled disc of a single ore block, one deep, flush with the terrain
surface, at Factorio's own spacing. `scripts/worldgen-check.py` asserted it against GregTech's
registries and left with GregTech (ADR-0060), so nothing asserts it on 26.1.2. **ADR-0045 deletes Terra's buried veins entirely** — they were
ADR-0019's leftover default rather than a decision, and ADR-0043's surface-working rig made keeping
them a demand for the digging gesture ADR-0019 removed the caves for. Ignus and Sapros are unaffected.
*This entry described GregTech ore veins in chunk-aligned disc patches, retargeted onto the pack's own
ore blocks by ADR-0041 with the vein shape unchanged.*

Sub-rules:

- **Patches are finite and run out** — `shipped`. ADR-0020: the fix for exhaustion is another planet.
  Since ADR-0041 this is finite in the literal sense as well as the generated one: the patch holds a
  number of units and mining spends them, rather than being finite only because the disc has edges.
- **Stone is a resource patch, not scenery** — `shipped`, ADR-0041. Factorio mines stone out of a
  patch like anything else, and Terra now deals a fourth starting field for it, with its own vein
  beyond. *ADR-0021 ruled stone "ambient terrain, never a patch", on the grounds that
  "a stone patch in a world made of stone reads as a joke". ADR-0041 reverses it: the mechanism
  ADR-0021 discharged stone's bulk-material function onto was never built, and quarries exist on
  Earth because what makes one is concentration, not the rock being absent elsewhere.*
- **Ore is visible where it lies** — `shipped` (#313, #320), ADR-0045. Every patch is on the surface, so finding
  one is exploration and the Radar reveals map rather than detecting ore — Factorio's own Radar.
  *This row read "ore is prospected, not stumbled on", `adapted` under ADR-0019: surface indicators
  first. ADR-0045 discharges that prerequisite rather than meeting it,
  and the indicators become dead.*
- **Infinite late-game resource (oil-style yield decay)** — `shipped` (#377), ADR-0081. Crude is
  Factorio's infinite resource: an **oil well** holds an amount, a Pumpjack standing on it yields
  `10 × amount / 300,000` a cycle and takes 10 off it, and the amount stops at the higher of 20%
  yield and 20% of what the well started with. A field is a structure placed from the outfield law,
  a well on 1/96 of its columns, 3 blocks apart, and a far well starts above 100%. **Oil is the
  only resource that gets this**: bedrock *ore* deposits would be infinite ore patches, so Terra
  carries none (ADR-0020 as amended, ADR-0021 as amended). *This entry read `adapted`, on GregTech's
  bedrock fluid deposit tapped by a Fluid Drilling Rig (#86), which left with ADR-0060.*
- **Resource richness varies per patch** — `shipped` at the design level, ADR-0041. An ore block
  carries an **amount** and mining draws one unit at a time, so richness is a real quantity rather
  than a patch size. The numbers are extracted, not chosen:
  `starting_amount = 20000 * base_density * (frequency_multiplier + 1) * size_multiplier`, and the
  per-block amount is that total over the blocks in the patch.
- **Patch spacing is Factorio's spots per km²** — `shipped` (#320, uranium #321), ADR-0045. `base_spots_per_km2` is
  extracted, not chosen: 2.5 for coal, copper, iron and stone and 1.25 for uranium. With the mean
  spot size that is ~671 and ~1549 blocks, ~42 and ~97 chunks, of mean spacing (#317). An outfield
  patch is a train ride, not a belt run. Each resource is one `random_spread` structure set, separated
  by Factorio's own minimum candidate spot spacing (~45 blocks, 3 chunks).
- **Regular patches are suppressed near spawn and fade in** — `shipped` (#320), ADR-0045. No patch inside
  `starting_resource_placement_radius` (150), and a spot's quantity ramps up over the 300 blocks
  beyond it. Both fall out of the density law rather than an exclusion.
- **Patch size rises with distance** — `shipped` (#320), ADR-0045. A spot's quantity, and with it its
  radius and blob amplitude, grows with density until 1600 blocks and then stops; neither cap binds
  at default settings. Each spot draws its size factor between `random_spot_size_minimum` and
  `maximum`.
- **A patch has a ragged edge** — `adapted`, ADR-0045. Factorio's three octaves, scales, weights and
  −1/3 offset times the blob amplitude, sampled from Minecraft's `ImprovedNoise` because
  `basis_noise` is not published.
- **Richness rises with distance from spawn** — `shipped` at the design level, ADR-0041. Factorio's
  own term, `max((1000 + distance) / 2600, 1)`, ported metre-for-metre: flat inside 1600 blocks,
  linear beyond. This is why leaving the starting area early buys nothing. *ADR-0045 measures it from
  the world origin rather than from spawn — worldgen cannot see spawn, and one mechanic may not have
  two datums. It is not capped: past 1600 blocks a spot stops growing and only richness rises, which
  is Factorio's (#317).*
- **An ore tile shows its remaining amount** — `adapted`, ADR-0041. Factorio's eight sprite stages
  are kept as a material-independent ratio set (`stage_counts`), computed from the block's own
  amount; the exact number is a Jade line rather than a tooltip. `adapted` because no patch carries
  vein metadata, so they get stages and Jade but no map layer.

### Manual mining

- **verdict**: `adapted`
- **notice**: ~~mining is a Minecraft block break, so it is per-block rather than a hold-to-mine
  timer against a patch total.~~ **Corrected by ADR-0041**: an ore block carries an amount and one
  break gesture draws one unit from it, leaving the block standing until the amount is spent — so
  mining *is* per-ore, and "seconds per ore" is now literal. The player is not expected to do much
  of it: the burner drill is in the pocket at spawn (ADR-0040). It is **not** tool-tiered: one tool in two tiers, the Engineer's Pick,
  mines every block class, and a flat seconds-per-item stands in for vanilla's hardness spread —
  1.0s for Terra's five resources, halved to 0.5s by `steel-axe`. Factorio's two mining
  speeds are kept and so is the ratio between the tiers, but the mining time is **the pack's own,
  half of Factorio's**: 2.0s shipped first and failed ADR-0039's human-on-delivery check, because
  Factorio's number assumes an engineer who hand-mines thirty ore, not ADR-0019's 1150-block
  starting area. *This row read "per-block and tool-tiered";
  ADR-0039 reverses that half. Nothing supplied a tool at all, so the claim described a mechanic
  the pack could not deliver.*
- **where**: all bodies
- **via**: `native_mechanic`
- **owner**: ADR-0039

Sub-rules:

- **Mining speed is doubled by research** — `shipped` at the design level, ADR-0039. Factorio's
  `steel-axe` is a trigger technology costing no packs; the pack declares it as `CheckItemPresence`
  on 50 steel plates, because Researchd has no craft-triggered research method, and swaps its
  `character-mining-speed` effect for an `unlock-recipe` granting the Engineer's Steel Pick.
- **Picking up a placed entity is the same gesture as mining** — `adapted`. The Engineer's Pick is
  the correct tool for every block, so a placed entity is taken by breaking it (ADR-0039), and a
  belt line's tiles or a pipe run are taken up many at once by a **Dismantle**, a sneak-click then a
  click of the Pick (#404, #431, #448). The two wrench item tags stay on it because Oritech's pipes read them for their
  connection toggle. *This
  entry named GregTech's wrench verbs, which #168 declared on the Pick as `ItemAbility` strings;
  GregTech left with ADR-0060 and #425 removed them.*
- **Rotating a placed entity (`R`)** — `planned`, #406. Two pack actions, **Rotate** and **Reverse
  Rotate** (`CONTEXT.md`), on `R` and `Shift+R`, for the held item's facing and the aimed block in
  place. The held half shipped with #386 (ADR-0083) and the placed half with #405 (ADR-0087), and
  both now run in Groundworks (#451); the footprint machines (#406) are refused until they turn whole, and a placed splitter is refused for good, since players break and re-place one rather than turn it (5thlayer/beltworks#28, wontfix). *This entry read "`shipped`, #168":
  #168 declared NeoForge's `wrench_rotate` ability on the Engineer's Pick, which GregTech gated the
  verb on, and GregTech left with ADR-0060. Nothing consumed the ability after that, so the row is
  planned again and #425 removed it.*

### Trees and wood

- **verdict**: `adapted`
- **where**: all bodies
- **via**: `pack`
- **owner**: ADR-0051
- **ticket**: #205

A Factorio tree is a **single entity**: one mining gesture removes it and yields its wood, with no
trunk, no canopy and no second gesture. Minecraft's log-by-log felling is a mechanic the pack
inherited rather than one Factorio has, so felling is re-authored in `factoryworks_core` — one
gesture at the base removes the connected tree and pays out at the base block.

The **yield diverges from the corpus on purpose**. Factorio's tree gives a flat `wood ×4`; the pack
gives **the log count of the tree actually broken**, so a jungle giant pays more than a birch. What is
kept from Factorio is the **rate**: `tree-01.mining_time 0.55` for `wood ×4` is **0.1375 s per log**,
extracted into `data/factorio/tree.json`, and the gesture costs `amount × 0.1375 s` — so a 4-log tree
costs Factorio's own 0.55 s exactly. The divergence is cheap because wood is terminal in Factorio:
five recipes consume it (`wooden-chest`, `small-electric-pole`, `shotgun`, `combat-shotgun`,
`tree-seed`) and no ratio downstream depends on it.

Sub-rules:

- **Felling is the base gesture only** — `adapted`, ADR-0051. A log with a log beneath it is
  mid-trunk and breaks normally, which is also what stops a touching canopy being felled from the
  wrong tree. The fill is bounded by count and radius; over the bound it fells what fits and leaves
  the rest standing.
- **A placed structure never fells** — `adapted`, ADR-0051. The fill requires at least one
  non-persistent leaf, which a build has none of. Nether stems fall out of this as a consequence
  rather than by name.
- **Leaves are removed with the tree** — `adapted`, ADR-0051. No drops, no decay ticks. Factorio has
  no leaves at all.
- **Felling time is halved by research** — `adapted`, ADR-0051. It rides ADR-0039's `steel-axe`
  ladder rather than declaring a second speed rule.
- **No sapling, dropped or crafted** — `excluded`, ADR-0051. Base Factorio has no replanting: a
  wild tree yields only wood, and `tree-seed` is Space Age's. Trees are finite like an ore patch.
- **Trees are not a fuel or a science input beyond Factorio's own use** — `shipped`. The fuel table
  already carries `wood` as the tag `minecraft:logs` (ADR-0047).
- **A decorative drops nothing** — `adapted`, ADR-0092. Factorio's decoratives cannot be mined. A
  plant the worldgen places, leaves included, has an empty loot table, and gravel drops no flint.
- **A log is not processed into planks or sticks** — `excluded`, ADR-0034's tail (#441). Factorio's
  wood has no processing chain, so vanilla's planks, sticks and the wooden blocks made from them are
  deliberately uncraftable. Wooden stairs are the one exception, made straight from each species'
  log (#444).

### Mining drills

- **verdict**: `adapted`
- **where**: all bodies
- **via**: `factoryworks_core`
- **owner**: ADR-0043
- **ticket**: #105; the pumpjack is #377's
- **notice**: Terra's two rigs are pack-authored and GregTech owns no drill here. A rig works the
  **layer directly beneath it** — Factorio's tiles, in a game that has a third axis — so a rig is
  placed on a patch rather than scanning downward for one. Its rate is the drill's `mining_speed`
  over the **resource's** `mining_time`, so uranium costs the same rig twice what iron does; its
  area falls out of `resource_searching_radius` and its output tile out of
  `vector_to_place_result`. *This entry read that a buried vein is reached by digging down;
  ADR-0045 deletes the buried veins, and the digging with them.*

Sub-rules:

- **The pumpjack stands on an oil well** — `shipped` (#377), ADR-0081. A 3x3x3 footprint placed
  only over a well, fed by a pole at 90 kW plus a 3 kW drain, one cycle a second. Three declared
  departures: crude leaves through **any face**, where Factorio's has one rotatable output; the model
  is Oritech's Pump scaled to 3x3, a human check on delivery; and the two module slots wait for
  modules.
- **A drill takes turns over the ore beneath it** — `shipped` (#537). Ten operations on a block, then
  the next block with ore in the area's fixed order, wrapping; a block that empties hands over at
  once, and every block gets the same share whatever its ore or amount, as Factorio's does.
- **Burner tier before electric** — `adapted`. The tier exists and is Factorio's own block rather
  than GregTech's steam stand-in. ADR-0040.
- **The electric drill is powered by a pole's supply area** — `shipped` (#194). It takes no fuel and
  no wire: 90 kW is 45 FE/t (ADR-0060), paid only on a tick it works, from a buffer the pole fills
  (ADR-0036). A pole reaching any block of the 3x3 finds it, and counts it once.
- **Drills output onto the tile they face** — `shipped` (#193). ADR-0043 reverses ADR-0040's
  `excluded`, which was argued entirely about belts and had deleted the drill-into-furnace pair as
  collateral. A rig pushes into an item handler on its faced tile and **otherwise stalls**, holding
  what it mined and burning nothing. *This entry read that it "else drops one item on the ground
  there and waits for it to be taken"; #182 showed the one-item-per-tile rule governs items already
  on the ground and that no Factorio machine spills when blocked, and #193 deleted the rule. This
  entry also read "Drills output onto a belt directly — `unargued`, no verdict".*
  **Auto-output is a prototype property and not a machine rule** — `vector_to_place_result` is
  carried by the mining drills and the recycler and by nothing else, which is why the pack's furnace
  is emptied rather than pushing.
- **Output onto a moving belt with no intermediate block** — `shipped`. A bare Create belt answers
  `Capabilities.ItemHandler.BLOCK`, so a rig faced at one puts ore on it with nothing in between,
  which is what a Factorio drill does. *This entry read `planned`, blocked on a rig not being able
  to reach Create's funnel. #182 closed that as `wontfix` on its premise: the funnel's inbound
  surface exists for Create's own transport handing over, and the right arrangement for a machine
  with a buffer is the funnel sitting **on** it in extract mode, pulling through the machine's own
  item handler — which needs no `DirectBeltInputBehaviour` call and no Create dependency. The
  entry also read that the item-handler path "ignores the belt's direction", which the bytecode
  refuted: the item lands on the queried segment and travels normally.*
- **A drill shows which tiles it is working** — `adapted`. The rig tints the top face of every ore
  block in its area, when looked at and when held for placement. Factorio shows this on a flat map;
  here it is a render on a surface the player walks on.
- **A body-locked large drill** (Vulcanus's Big Mining Drill) — `planned`, see [Planet-locked buildings](#planet-locked-buildings).

### Water as a resource

- **verdict**: `adapted`
- **where**: all bodies
- **via**: `pack` (the Offshore Pump), vanilla water, `create` for pipes
- **owner**: ADR-0050
- **notice**: Minecraft water forms new sources and fills buckets, so source formation is off and
  there is no bucket; water enters the factory only through the Offshore Pump.
- **ticket**: #200, closed with ADR-0050's design shipped (#210–#213)

Factorio's water is infinite in volume and **fixed in place** — that property is the whole reason the
offshore pump exists and why shoreline is a siting concern. ADR-0050 keeps it with one rule: **water
is extracted and transported, never created.**

Sub-rules:

- **The offshore pump** — `shipped` (#213), pack-authored. ADR-0048 had made it `not_emitted` on the
  reasoning that Create's Mechanical Pump covered the water half; that block is a pipe-network pump
  and does not extract from the world at all, so ADR-0050 reverses the call. One adjacent source
  block, no minimum body size, no power (`energy_source: void`), 1,200 mB/s — which is exactly twenty
  Boilers at their extracted 60 mB/s. Placement is refused with a message where no source adjoins;
  the rate is read from the corpus and never typed, and `tests/pack/test_pump_assets.py` is what
  holds the copy honest.
- **Water source formation** — `excluded`. `waterSourceConversion` is off, forced by the mod on level
  load. Vanilla's 3x1x1 trench turns two buckets into unlimited water anywhere, which is water
  creation and defeats every siting constraint above it.
- **Buckets** — `excluded`. ADR-0050 refuses a 1,000 mB hand container beside ADR-0037's 50 mB barrel.
  Rung 0 reaches water by pumping or digging a channel from the hub pool, not by carrying it.
- **Water in the starting area** — `shipped` (#212, restored #313). `scripts/build-terra-start.py`
  puts a pool in the hub itself — the hub's own blocks, one deep and flush with the ground, not a
  fifth jigsaw child that vanilla could drop silently. Factorio starts the player beside water and
  the pack has no bucket, so under the rule above water is not something rung 0 can make but a
  *place* it has to find, and without the pool that is an unbounded walk ADR-0049's traversal budget
  has no room for. It was argued for Create's water wheel; Create left with ADR-0060 and what the
  pool now sites is the Offshore Pump, and behind it the Boiler.
  `tests/worldgen/test_start_geometry.py` asserts the pool's presence — but not the opening's, which
  the 26.1.2 move parked whole until #313.
- **Placed flowing water** — `planned`. A pack outlet block maintaining flowing water from a pipe, for
  contraptions tidier than a dug channel. Safe without any tracking because what it places is never a
  source. Lands after the pump and pipes.
- **Water barrelling** — `shipped` via the barrel; see [Fluid handling](#fluid-handling). Hauling
  water in barrels is Factorio's own answer and is not a hole in the rule: the water still came from
  a pump on natural water.

### Fluid handling

- **verdict**: `shipped`
- **where**: all bodies
- **via**: `oritech`, `factoryworks_core`
- **owner**: ADR-0060 and ADR-0067 (Oritech's Fluid Pipe is `pipe` and its Portable Tank
  `storage-tank`), ADR-0037 (the barrel), ADR-0050 (the Offshore Pump)
- **ticket**: #293 (the in-line pump); #106 (the barrel) closed

Sub-rules:

- **Pipes and storage tanks** — `shipped`. Oritech's Fluid Pipe, laid by Stretch (#452) and taken up
  by Dismantle (#431), and its Portable Tank, both crafted from Factorio's recipes.
- **The in-line pump** — `planned`, #293. `pump` is `factoryworks:pump`, `blocked_by` #293,
  since Oritech's pump is a well pump.

- **Barrelling and unbarrelling** — `shipped` as `native_mechanic`; `subgroup-owner.json`'s barrel
  shelves emit nothing because the mechanic already works (#93). The container is
  `factoryworks:barrel` (ADR-0037), pack-registered at Factorio's 50 units — 50 mB under the
  converter's 1:1 rule — stacking to 10. **Factorio's fluid restriction is not ported**: the barrel
  accepts any fluid, because that list is a content budget for nine items and eighteen recipes, and
  the pack has one container and none.
- **Underground pipes** — `excluded`. The same argument as underground belts, one level up: a
  pipe routes freely in three dimensions, so the crossing problem Factorio's pipe-to-ground exists to
  solve does not arise. `subgroup-owner.json` marks `pipe-to-ground` `not_emitted` on that reasoning.
- **Fluid mixing is forbidden in a pipe network** — `excluded`. `by-consequence`: no mod in the stack
  enforces single-fluid pipe networks, and adding it would be a pack mechanism nobody asked for.
- **Pumps and flow rate over distance** — `unargued`, no verdict.

### Oil processing

- **verdict**: `shipped`
- **where**: Terra, Ignus, Gelida
- **via**: `pack`
- **owner**: ADR-0096 (the Chemical Plant and Oil Refinery are pack blocks on the Assembling
  Machine's chassis, on Oritech's models) and ADR-0067 (the oil fluids are Oritech's, retinted to
  Factorio's colours)
- **ticket**: #486, closed (the Chemical Plant shipped with #490 and the Oil Refinery with #491); #258 before it

Sub-rules:

- **Basic then advanced oil processing** — `shipped`. Advanced is not gated by research yet (#493).
- **Cracking to resolve the three-output imbalance** — `shipped`. The chapter's whole puzzle.
- **Coal liquefaction** — `planned`, on Ignus (`docs/planets.md`).

### Smelting

- **verdict**: `shipped`
- **where**: all bodies
- **via**: `factoryworks_core` — the three tiers are pack blocks (#91, #149, #155)
- **owner**: #91
- **ticket**: #432 (the burner tiers' GameTests) and #271 (the Electric tier's); #155 closed

Sub-rules:

- **Ore smelts one-to-one straight to plate, with no intermediate step** — `shipped`. Recorded in
  `subgroup-owner.json`; the pack does not get to add a hop. The converter emits `iron_plate` and
  `copper_plate` as `factoryworks:smelting`.
- **A furnace recipe may consume more than one item** — `shipped`, #155. Vanilla's `SmeltingRecipe`
  holds an `Ingredient` with no count, so Factorio's `steel-plate` (5 plates to 1) has no vanilla
  shape at all; the pack's three furnaces read a count-bearing `factoryworks:smelting` type
  and **only** that one. `stone-brick` (2 stone to 1) rides the same type — ADR-0046 collapsed
  #87's earlier split, which had `stone-brick` take a vanilla 1:1 shape instead. The vanilla type
  is not read alongside it: under ADR-0034's sweep it carries no live recipe, which also makes
  Minecraft's food cooking gone rather than merely uncraftable (#183).
- **A machine with a blocked output stops** — `shipped`, #155. A furnace whose output slot cannot
  take the result does not start the smelt, burns no fuel and draws no FE; nothing is voided,
  overflowed or dropped. Factorio has no machine that ejects to the ground, and under ADR-0041
  Terra's ore is finite, so backing up is the only answer that does not destroy a resource the
  world cannot re-make.
- **Fuel is rated in joules per item, and a burner drains its own `energy_usage`** — `shipped`,
  #187, ADR-0047 (#185). Lighting an item banks its `fuel_value`; a working tick spends `energy_usage / 20`,
  which is 4,500 J on both burner furnace tiers. There is no burn-time number anywhere in Factorio and
  there is none here: burn time is a quotient, and an idle burner keeps the joules it has not spent.
  #155 shipped the tiers reading Forge's vanilla burn table instead, which is the defect ADR-0047
  closes — both burners drawing the same 90 kW is the actual reason the Steel tier gets twice the
  items from one coal. The burners' flame went with the tick model: they hold the same kind of
  thing the Electric tier holds, so they get the same horizontal gauge.
- **Fuel categories** — `shipped`, #187, ADR-0047 (#185). A furnace burns `chemical` and nothing else, as
  Factorio's do. Every fuel reachable today is `chemical` — coal, wood, solid fuel, rocket fuel and
  `nuclear-fuel`, which despite its name is an ordinary chemical fuel — so the filter currently
  excludes nothing. It is carried anyway: `uranium-fuel-cell` is the `nuclear` one, it has a
  `fuel_value`, and #135 will land it. An item with no row in the fuel table is not fuel, which is
  ADR-0034's default-deny applied to burning. `nuclear-fuel` has no `item-map.json` row, so it is a
  recorded skip in the join rather than a table row — the table names four items today: coal, wood
  (through `minecraft:logs`), solid fuel and rocket fuel.
- **No ore multiplication** — `planned`, settled by ADR-0032: cut pack-wide, Mekanism's ladder and
  Create's rung-0 Crushing Wheels alike. Yield gain by research or module is `blocked`, not
  `excluded` — the lab cannot express levelled research (ADR-0022 prunes 106 such technologies) and
  Terra is deliberately not compensated for its scarcity (ADR-0020). See #120.

### Assembling machines and recipe categories

- **verdict**: `shipped`
- **where**: all bodies
- **via**: `factoryworks_core`, `oritech`
- **owner**: ADR-0026, ADR-0029, ADR-0056, ADR-0060, ADR-0075
- **ticket**: #120, which tier 3's recipe waits on for `speed-module`. Closed: #87 converts the
  recipes; #326 registers
  tier 1 as `factoryworks:assembling_machine` on Oritech's base, placed and inert (ADR-0071,
  ADR-0072); #327 gives it a Held recipe and #328 crafts it at `assembling-machine-1`'s speed 0.5
  and 75 kW, stalling without consuming; #331 emits the recipes naming it, and refuses it every
  `crafting-with-fluid` one, which tier 1's `crafting_categories` does not list; #295 adds tiers 2
  and 3 as blocks of their own (ADR-0075), which craft with a fluid

Three pack-authored Assembling Machines. Recipe routing follows Factorio's own `category`
(ADR-0021), not the owning mod. The three machines, their tiers and their recipe type are
pack-authored on Oritech's machine base (ADR-0060, ADR-0071, ADR-0072).

Sub-rules:

- **`crafting_speed` as a machine property** — `shipped`. ADR-0029 puts it on the machine, at
  Factorio's raw values (0.5 / 0.75 / 1.25), which is what makes `energy_required x 20` produce
  Factorio's own felt durations. `AssemblingTier` carries one per tier (#295).
- **`energy_usage` as a machine property** — `shipped`. No recipe carries energy; each tier draws
  its own 75, 150 or 375 kW at 1 FE = 100 J, priced per craft so tier 1's 37.5 FE/t and tier 3's
  187.5 FE/t sum exactly (#328, #295).
- **Machine tiers** — `shipped` for tiers 1 and 2 (ADR-0075). Each tier is its own block, crafted
  from Factorio's recipe, and wears Oritech's `ORANGE`, `DIAMOND` or `INDUSTRIAL` paint, which it
  refuses to change. Oritech's addons stay live on every tier, unpriced until #120. Tier 3 is
  registered, and its recipe is a recorded skip on `speed-module` (#120). Placing a higher tier over
  a lower one is #299's.
- **Fluid inputs** — `shipped` on tiers 2 and 3 (ADR-0075). One 1,000 mB input tank whose face, on
  every block of the machine, takes only the Held recipe's fluid and gives nothing back. A tank short
  of one craft's fluid stalls the machine as missing items do, and the status names the fluid. A
  changed recipe voids the tank, as Factorio's does. There is no output tank until a recipe with a
  fluid result is emitted (barrel emptying).
- **Recipe selection in a machine** — `adapted`. In Factorio a machine is *told* its recipe: the
  player picks it from a grid on the machine, the machine displays it, holds it whether or not it is
  fed, and the setting copies to another machine. The Assembling Machine holds a **Held recipe**
  (ADR-0071) -- one recipe id, kept whether or not the machine is fed, resolved against the recipe
  manager when asked, so a tag ingredient stays a tag. The adaptation is **where it is picked**
  (ADR-0073): the recipe viewer is the only picker. EMI's Fill Recipe on the open machine sets the
  Held recipe, lit with an empty inventory, and the server refuses one the machine cannot hold with a
  message -- including a `crafting-with-fluid` recipe, since tier 1 has no fluid box (#331). The machine's screen has **no recipe list** -- #327's was a second browser beside EMI's,
  missing its search and navigation, and #336 removed it. It shows the Held recipe as its result's
  icon and name, whose tooltip carries the recipe when EMI is loaded, and a progress bar between the
  inputs and the output. There is no clear: a machine without a recipe does nothing, so a recipe is
  replaced, never removed. That the viewer can set every emitted assembling recipe of tier 1's
  categories is `AssemblingMachineTests`' GameTest. Its inputs are filtered to the Held recipe (#329, ADR-0074): the
  `n`th ingredient goes in the `n`th slot, and a slot the recipe does not use takes nothing. The
  filter applies to a belt, a loader or the player's hand, and the hand is refused on the client
  too, since the menu carries each slot's ingredient. A machine with no recipe takes nothing. The
  Held recipe is drawn in the slots by the same rule (#334): each ingredient ghosted with the count
  one craft needs, the product in the output, a tag cycling through its members, and an input slot
  holding less than one craft drawn red. The screen also shows the energy bar and the stall line
  (#332). There is no player-set slot capacity (ADR-0073, amended on #335). EMI is the
  only setter, as it is the Personal Assembler's transfer target; JEI gets no transfer handler
  (#337). Copying the setting
  is one id, and the configuration card is not built. Furnaces keep Oritech's first match, which is
  Factorio's own split.

  *History.* Under ADR-0056 the surface was Modern Industrialization's **locked output slot**: one
  mechanism selected the recipe (a locked slot refused a rival recipe's product), showed it and
  guarded against overfill, and EMI's Fill Recipe set it with the ingredients absent. It keyed on
  the *product*, so it needed a static check that no two recipes of one type share an ingredient set
  and an in-world check that locking covered every collision group. ADR-0060 took MI out, and a Held
  recipe is an id with no lookup at all, so neither check has anything left to guard.
- **Machine idle draw** — `excluded`. A Factorio machine consumes power while idle: the
  [Electric system](https://wiki.factorio.com/Electric_system) page notes *"an active assembling
  machine 2 will consume 155 kW (150 kW energy consumption + 5 kW drain)"*, about a thirtieth of the
  draw, and the engine default is `energy_usage / 30` since no crafting machine sets the field.
  GregTech has no equivalent -- an idle GT machine consumes nothing -- and reproducing it means real
  idle draw built in `factoryworks_core` for a lesson (*don't over-build*) that ore depletion
  (ADR-0020) and Emission already teach more cheaply. Folding it into `EUt` is worse than either: it
  looks like fidelity and behaves as a flat tax. Called **idle draw** in pack prose, never "drain",
  which `CONTEXT.md` owns for an unrelated Sapros mechanic.

### Handcrafting and the crafting queue

- **verdict**: `shipped`
- **where**: all bodies
- **via**: `craftworks`
- **owner**: ADR-0089 and Craftworks' own ADRs, `docs/gdd.md` §5
- **ticket**: #99 (upgrade modules); #160, #161, #100, #140, #291 closed

The crafting grid is removed (#90) and the Personal Assembler replaces it permanently (#95) — it is
the player's only hand-crafting surface, not a bootstrap crutch, and every fluid-free `crafting`
recipe reaches it (#88). Craftworks removes the 2x2 inventory grid, rather than
a recipe removal, because it is a vanilla menu and no recipe removal reaches it (ADR-0034).

**Chain-crafting is the mechanic, not the timer.** Factorio's wiki names it as what separates the
hand from an assembling machine: request a recipe whose ingredients you lack and the sub-crafts are
queued for you. The pack reproduces it as a resolved **Crafting Plan** on Applied Energistics 2's
autocrafting shape — an amount dialog, then a flattened plan naming every intermediate and every
shortfall, then one commitment that pays the whole cost (ADR-0038). Two departures from Factorio are
deliberate and recorded there: cancellation takes the plan as its unit, and a plan with a missing
ingredient cannot be started.

**The Assembler ships in `factoryworks_core`**, not as pack scripting: KubeJS cannot register a
menu or a screen on 1.21.1 (#96, ADR-0015). It has no recipe type of its own — the hand-craftable set
is a predicate over Assembling Machine 1's recipes (#88), so one emitted recipe serves both surfaces.

**The queue's slowness is serial, not a multiplier.** The character prototype sets no `crafting_speed`
at all -- it is not a crafting machine -- so Factorio hand-crafting runs at exactly `energy_required`
seconds. What makes it slow is that the queue is serial: one plan at a time, no modules, no
parallelism. The pack reproduces the mechanism rather than approximating it with a penalty, and
ADR-0029 gives the Assembler speed 1 with durations of `energy_required x 20` unmodified.

### Transport belts

- **verdict**: `adapted`
- **notice**: a belt is a row of tiles carrying Factorio's items per second and holding Factorio's
  buffer, but in one lane and with no undergrounds, so the lane-and-underground patterns a Factorio
  player has memorised do not transfer (ADR-0044, ADR-0076, ADR-0084, ADR-0085).
- **where**: all bodies
- **via**: `beltworks` (Beltworks, 5thlayer/beltworks, ADR-0060)
- **owner**: ADR-0076, ADR-0044, ADR-0084, ADR-0085, #341

Sub-rules:

- **Belt shape** — `shipped` (#383). Factorio's tile: one block of belt per block, straight or a
  one-block corner, the corner derived from what feeds the tile (#391). Contiguous tiles merge at
  runtime into one transport line that ticks once (#398). A stretch lays straight legs
  joined by corners (#393) through Groundworks, climbing or descending only where the player presses
  Raise or Lower, and going round flat whatever is in its way, a crossing line included, which it
  never cuts (5thlayer/beltworks#46). A tile placed a block above or below and ahead
  of a line's end makes a slope, one block of line like any tile; the pitch is derived as the corner
  is (#417, ADR-0085), and a climb of more than a block has a middle per extra block (#418). A middle or
  top over air stands on a wedge, placed and broken with it (#420). A slope never turns and takes no
  side-load, and a placement that would turn a corner into one is refused (#419). A raised line
  draws supports down to the first solid top or belt piece, at corners, line ends, a slope's foot
  and top, splitters and spaced along straight runs, and the Placement Preview draws them too; they
  are only drawn and never refuse a placement (#412, 5thlayer/beltworks#1).
- **Belt ends** — `shipped` (#383). A belt ends where its tiles do: at a loader, which a player
  places against an inventory, at a splitter half, or at its last tile, where it backs up as a
  Factorio belt ending in nothing does (ADR-0084).

- **Belt tiers** — `shipped` for tiers 1 and 2. The fork's four belt tiles, `belt_tile`, `improved_belt_tile`,
  `express_belt_tile` and `turbo_belt_tile`, carry Factorio's 15, 30, 45 and 60 items/s under Factorio's names
  (#345, ADR-0076). `logistics-2` and `logistics-3` unlock the fast and express recipes on the
  Assembling surface. Express needs lubricant, so it is a `crafting-with-fluid` recipe, which
  Assembling Machine 2 and 3 craft (#295). The turbo
  belt has no recipe, because Space Age's is outside the corpus. *This entry read `adapted`, against
  Create's one RPM-driven belt.*
- **Throughput as a ratio budget** — `shipped`. The fork's belt carries one item per entry at
  Factorio's 1/8-block spacing and each tier's speed, so a belt carries exactly its tier's items/s,
  and a loader loads several entries in a tick when a tier needs more than 20 (#344, #345,
  ADR-0076).
- **Mixed tiers** — `shipped`. A belt, each loader and each side of a splitter cap only their own
  flow, so any tiers may be joined and a line runs at its slowest piece: a tier-3 belt between tier-1
  loaders carries 15 items/s, so does a tier-1 belt between tier-4 loaders, and so does a tier-3 line
  through a tier-1 splitter (#347, #349, ADR-0076).
- **Underground belts** — `excluded`. Not for want of a Create block: undergrounds solve a *weaving*
  problem — two lanes past each other in a fixed footprint — that exists only in two dimensions.
  Argued from the medium, not from a mod's shortfall (ADR-0044), and now from slopes: a belt crosses
  another by climbing over it in five tiles, a foot, a top on a wedge, a level tile on the crossed
  tile, a top on a wedge and a foot, built by hand (#420, ADR-0085) or laid by a stretch across the
  line (#422).
- **Splitters** — `shipped`. Four tiers from Factorio's four splitter recipes, turbo registered
  with no recipe. A splitter is two blocks wide, placed and broken as one; each half ends one belt at
  its back and starts one at its front, and items pass from an input's end to an output's head with
  nothing held between. It splits evenly, merges evenly, sends everything to the free side when the
  other backs up, draws no power, and each side passes no more than its tier's items/s (#349,
  ADR-0076). The placement preview is #355. It meets belt tiles face to face, and placed across a
  straight tile line running its way takes the tile's place (#394). *This entry read `adapted` to Create's tunnels, which
  left with ADR-0060.*
- **Balancers** — `shipped`. A balancer is built from splitters, not bought as a block: chained
  splitters make Factorio's 2x2 and 4x4 balancers (#349).
- **Splitter priority and filter** — `shipped`. A splitter's screen, opened by sneaking and using
  either half with an empty hand, sets an input priority, an output priority and a filter. The
  filter is set by clicking its slot with an item or dragging one from JEI or EMI, and takes
  nothing. What it matches goes only to the output-priority side and everything else only to the
  other, each waiting when its side backs up (5thlayer/beltworks#20, 5thlayer/beltworks#21, 5thlayer/beltworks#22).
- **Two lanes per belt** — `excluded`. Lane balancing is a compression trick for a conveyor one tile
  wide on a plane — what you do when the only free axis runs along the belt. It goes with the
  undergrounds and for the same reason (ADR-0044). *This entry read `by-consequence` of Create
  having no lane model; the ledger now owns a reason of its own.*
  The fork *draws* a belt's items in two lanes of four per block, so they read at a size a player
  can see without overlapping, but the belt is one lane: no lane fills or empties apart from the
  other (#344).
- **Side-loading** — `adapted`. A belt feeding the side of a straight tile merges into the line it
  feeds, into its gaps, with the line from behind going first; a full line backs the side up. With
  one lane there is no far lane to fill, so it is Factorio's side-load reduced to a merge (#409).
- **Drop onto a belt** (`Z`) — `adapted` via vanilla's drop key. Q aimed at a belt tile puts one
  item from the selected slot into the tile's line at the aimed point, or at the nearest gap on the
  same tile, and throws nothing; a tile with no gap keeps the item in the slot and says why on the
  action bar. Ctrl+Q, and Q aimed at anything but a belt tile, stay vanilla's throw. It reaches the
  player's block interaction range, which the Pack's Building reach sets to 16 (#413). An item that
  lands on a tile joins the line where it lands when there is room, a stack one item at a time, and
  otherwise rests on the items under it, moving with them, until a gap opens
  (5thlayer/beltworks#91, 5thlayer/beltworks#92). `Z` into a machine stays `excluded` under Controls.
- **Belt as buffer** — `shipped`. A backed-up belt queues from its end at eight items per block at
  every tier, so a 64-tile belt holds 512 (#344). *This entry read `excluded`,
  against Create's one item per block.*
- **Cost per length** — `shipped`. A belt costs one tile of its tier per block, as a Factorio belt
  costs one item per tile. A stretch the player cannot pay for is refused with a message and charges
  nothing (#393). Breaking a tile returns it and the items on it (#346, ADR-0084). *Upstream
  SimpleBelts charged one item whatever the length.*

`logistics`, `logistics-2` and `logistics-3` are declared in `researchd.js`; each unlocks its
splitter, and the last two their belt tier (#345, #349). Their underground belts are excluded above.

### Inserters

- **verdict**: `adapted`
- **notice**: the inserter is the **feeder**, a block that moves one item at a time between ends up
  to 3 blocks apart, each a chest, a machine or a belt tile, and picks up loose items. The **loader**
  loads an inventory onto a belt or unloads a belt into one, and is belt equipment.
- **where**: all bodies
- **via**: `beltworks` (Beltworks, 5thlayer/beltworks, ADR-0060)
- **owner**: ADR-0100, ADR-0076, ADR-0060, #341

Sub-rules:

- **The inserter chain** — `adapted` as the feeder. The fork's four feeders, `feeder`,
  `improved_feeder`, `express_feeder` and `turbo_feeder`, are crafted from the `burner-inserter`,
  `inserter`, `fast-inserter` and `bulk-inserter` recipes on the Assembling surface, each unlocked by
  its inserter's technology (#514, ADR-0100). A feeder moves a tenth of its tier's loader rate.
- **Loaders** — `adapted` from Factorio's hidden loaders. `loader`, `improved_loader` and
  `express_loader` are crafted from the `loader`, `fast-loader` and `express-loader` recipes, unlocked
  by `logistics`, `logistics-2` and `logistics-3` (#514, ADR-0100). The tier-1 loader takes 5 inserters,
  which are tier-2 feeders here, so it cannot be crafted before `electronics`. `turbo_loader` has no
  recipe, since its recipe takes the turbo belt, which has none. A loader moves its tier's 15, 30, 45
  or 60 items/s whatever belt it is on.
- **Inserter energy** — `adapted` as the feeder's. Each feeder pays one swing of its inserter per
  item, since it carries one item a swing: 669, 66.5, 81.2 and 232 FE, the burner inserter's included, set in
  `config/beltworks-server.toml` (#514). Loaders keep the figures ADR-0076 gave them: tier 1
  unpowered, tiers 2 to 4 66.5, 81.2 and 116 FE an item and 4, 5 and 10 FE/s of drain, until
  5thlayer/beltworks#84 makes them configurable (#348, ADR-0076).
- **Inserter filter** — `adapted`. A loader or a feeder takes FTB Filter System's smart filter as its filter
  (ADR-0084). The filter is hand-made from 4 iron sticks, 4 copper cable and 1 electronic circuit,
  its stock recipe re-authored under ADR-0034's tail (#442).
- **Long-handed inserter** — `adapted` as the feeder's reach. A feeder's head and tail each reach 1 to
  3 blocks, set by key, so there is no long-handed tier; `long-handed-inserter` is `not_emitted`
  (#514, ADR-0100).
- **Overload limit** — `shipped` on the crafting chassis (#517) and the furnaces (#518); fluids #519.
  Automated insertion stops at a recipe's Overload Limit, the crafts one inserter swing (1.166 s)
  completes plus one, between 2 and 100, measured in `data/factorio/overload.json`. Quick transfer (#208, unbuilt) will be held to it through the item handler; the hand in a screen is
  not held to it. A product stops the craft at a full stack, as Factorio's does. The bonus `allow_inserter_overload` gives of 4× the stack
  inserter's stack size is not reproduced, since no feeder or loader carries a stack.
- **Stack-size bonus research** — `planned`. A loader moves one item per belt entry, and a feeder one item a swing, until #25 picks
  the technologies (#341).

### Logistic robots

- **verdict**: `excluded`
- **where**: —
- **owner**: ADR-0017

ADR-0017 gives item logistics to Create and cuts the dedicated routing mods, because a substitute
routing idiom is a straight bypass of the ladder. AE2 is the one gated exception, unlocked at endgame
once every planet's puzzle is done — it is not a logistic-robot analogue and is not this row.

### Building by hand: placement preview and fast replace

- **verdict**: `shipped`
- **notice**: a held pack block draws translucent where placing would put it and red where placing
  would be refused (#297), and a furnace, pole column or Assembling Machine of another tier placed
  over one swaps it in place, drawn blue (#388, #389, #390); a held pole also draws the wires it would
  add and its supply area (#298, #158). Belts, splitters and loaders do not fast replace yet.
- **where**: all bodies
- **via**: `pack`
- **owner**: #297, #298, #299
- **ticket**: #384 (fast replace for belts, splitters and loaders), #401; #297, #298, #158, #299
  closed

Sub-rules:

- **A held block previews where it lands, red where refused** — `shipped`, #297. The pack's term
  is **Placement Preview**, not ghost: a ghost is the excluded robot-built entity under
  [Construction robots and blueprints](#construction-robots-and-blueprints).
- **A held belt previews the tiles it would place, red where refused** — `shipped`, #393. The tile
  item's click executes the plan its preview draws, and the refusal's reason is on the action bar.
- **A held pole previews the wires it would add** — `shipped`, #298.
- **A held pole shows its supply area and those of the poles around it** — `shipped`, #158.
- **Fast replace: placing another furnace tier over a furnace swaps it in place** — `shipped`,
  #388. The swap keeps the facing, the items the new tier holds, the smelt's fraction done, the
  joules between the burners and FE up to the new buffer; the rest goes to the player, and a player
  with no room for it is refused on the action bar.
- **Fast replace: placing a small or medium pole over a column of the other swaps the whole column
  in place** — `shipped`, #389. Every segment swaps for the one item a column costs, the column keeps
  its height and every wire, and a substation, alone in its Replace Group, is refused with its reason
  on the action bar.
- **Fast replace: placing another tier over an Assembling Machine swaps it in place** — `shipped`,
  #390. Any of its four blocks answers. The machine keeps its facing, its craft's fraction done, its
  FE, its output and its Oritech addons, and the Held recipe when the new tier can hold it; otherwise
  the recipe clears and its inputs go to the player. The tank empties when the new tier has none, and
  a player with no room for what comes back is refused on the action bar.
- **Fast replace for belts, splitters and loaders** — `planned`, #384.

### Construction robots and blueprints

- **verdict**: `excluded`
- **where**: all bodies
- **owner**: [#144](https://github.com/5thlayer/factoryworks/issues/144)

Nothing in the pack copies a built shape. Building Gadgets 2 was the closest thing to a blueprint,
covering the *shape* half and none of the *logistics* half, and it left the manifest with #144:
the stock-recipe sweep had already made its gadgets uncraftable, and it was removed rather than
re-authored.

**Create's Schematicannon is not this row.** Create left with ADR-0060, and a Schematicannon was a
building tool with a hopper, not a construction network, so the two mechanics were never
interchangeable with Factorio's.

Sub-rules:

- **Copy a built shape and stamp it down again** — `excluded`. Building Gadgets 2 left the manifest
  (#144).
- **A blueprint is an item you can hand to another player, or keep in a library** — `unargued`,
  no verdict.
- **Pasting leaves ghosts that something else fills in** — `excluded`. This is the half that makes
  blueprints a logistics mechanic rather than a building tool, and nothing in the stack has it.
- **Construction robots build, repair and rebuild from a roboport's range** — `excluded`.
  `by-consequence` of [Logistic robots](#logistic-robots): ADR-0017 cuts the routing mods, and a
  construction network is that decision applied to building. The second half of that argument — "a
  network with nothing to repair" — died with ADR-0055; see
  [Repair and entity damage](#repair-and-entity-damage). ADR-0017 still carries the row on its own.
- **Deconstruction planner** — `unargued`, no verdict. The **Dismantle** (#404, #431, ADR-0086) is
  its belt and pipe precursor: a span of one family, in two clicks, with no marks left in the world.

### Trains

- **verdict**: `adapted`
- **notice**: trains are Railcraft Reborn's on vanilla rail, and a stop is a buffer stop that ends a
  line rather than a schedule target by name.
- **where**: Terra
- **via**: `railcraft`
- **owner**: ADR-0060 (Railcraft Reborn carries trains), and #277 and #278 for which item each row names
- **ticket**: #435 (schedules, stations and train limits)

Railcraft runs on vanilla rail: `rail` is `minecraft:rail`, the locomotive is Railcraft's Steam
Locomotive, the wagons its Cargo Minecart and Minecart with Tank, and the stop its Iron Buffer Stop
Track.

Sub-rules:

- **Schedules and stations** — `unargued`, no verdict. A buffer stop ends a line; it is not a
  schedule target by name.
- **Rail signals and block-based traffic** — `adapted`. `rail-signal` is Railcraft's Block Signal and
  `rail-chain-signal` its Distant Signal, which repeats the aspect of the signal it is linked to but
  reserves no path through a junction.
- **Train limits at a station** — `unargued`, no verdict.

### Circuit network

- **verdict**: `adapted`
- **notice**: the wire is redstone, so a signal is a strength from 0 to 15 on a block-to-block
  circuit rather than a named channel on a coloured wire — there is no reading a whole belt's contents
  off one wire, and no arithmetic on a signal. Until #484 nothing reads or decides with it.
- **where**: all bodies
- **via**: `native_mechanic`, `factoryworks_core`
- **owner**: ADR-0095
- **ticket**: #482, with #483 for the wire, #484 for the components and #485 for a red and green wire

**The wire is Minecraft's redstone, laid free by the Engineer's Pick** (ADR-0095). Factorio 2.0 has
no wire item — the corpus holds no `red-wire` or `green-wire` recipe, and a researched player lays wire
for nothing — so redstone dust is never an item either: a right-click with the Pick lays it, and it
drops nothing. That keeps Terra's closed alphabet (#124), which no recipe for redstone could.

ADR-0030 first gave this row to vanilla redstone plus Create's redstone line, overturning a `blocked`
that looked for one mod's capability and missed the base game. Create left with ADR-0060 and took the
components with it, and vanilla's own need redstone or quartz, so what the wire connects is authored
or nothing, and #484 decides which.

Sub-rules:

- **Lay and cut the wire** — `adapted`. The Pick lays and breaks dust, gated on `circuit-network`
  (#483).
- **Read a machine's or container's contents as a signal** — `planned`, #484. A comparator is the
  obvious reader and is not Obtainable.
- **Combinator logic — arithmetic, decider, constant, selector** — `planned`, #484. The four rows are
  `undecided` on it.
- **Wireless signal over distance** — `planned`, #485. Create's Redstone Link, which covered it, left
  with ADR-0060.
- **Lamps and display panels as readouts** — `planned`, #484. `small-lamp` is Oritech's Industrial
  Light and `display-panel` is `undecided`; `power-switch` is a core block joining two pole networks,
  #294's.
- **An alert that fires on a condition** — `planned`, #484. `programmable-speaker` is `undecided`.
- **Two independent networks on one wire (red and green)** — `planned`, #485. Redstone has one
  channel; a real red and green wire is the only way to it.
- **Circuit-controlled inserters and belts** — `unargued`, no verdict. A loader is the inserter chain
  (ADR-0076), and what drives one waits on #484 and #485.

### Electric network and transmission

- **verdict**: `adapted`
- **notice**: power reaches a machine in all-or-nothing ticks. A machine short of power stops
  rather than slowing, and nothing carries FE between areas except a wire between two poles.
- **where**: all bodies
- **via**: `wireworks`, `oritech`
- **owner**: ADR-0017 as amended by ADR-0035, ADR-0036, ADR-0060 and ADR-0062

FE is the pack's only energy currency (ADR-0060), at **1 FE = 100 J**. **One** carrier moves it: the
core's **supply-area pole** (ADR-0036), which feeds every machine standing in its area, and which
since ADR-0062 reaches other areas by linking to the poles within its wire reach — one network, one
balance. ADR-0062 supersedes ADR-0060's two-carrier clause, and **Oritech's Energy Transmission
Pole** left the power path with it (#284): like Oritech's energy pipes and its Enderic Laser, it is
swept by ADR-0034's default-deny (`recipes.js` admits no Oritech surface), so it has no recipe and
does not appear in EMI. Oritech still supplies the things that stand *in* an area rather than carry
between them — the Steam Engine as generator, the Large Energy Storage as accumulator (ADR-0067) —
which is why it remains under `via`. *Before ADR-0060 this row was Create: Power Grid's, with
voltage drop, wire gauge and a brownout model; the mod left with Create.*

Sub-rules:

- **Brownout: insufficient supply degrades what is running** — `adapted`. Factorio derates every
  machine on the network in proportion to the shortfall. Here a machine that cannot draw its full
  tick's energy makes no progress that tick, which is Oritech's own `MachineBlockEntity.workTick`,
  and ADR-0060 keeps it rather than building a derate. The slope becomes a stutter: an area at half
  supply runs at roughly half speed, but by ticks skipped rather than by a slower craft.
- **Voltage tiers** — `excluded`. Factorio's electric network has none to begin with; the tiers
  were Power Grid's wire gauge, which left with it (ADR-0060).
- **Power poles have a supply area** — `planned`. The core's pole, three tiers at 5x5, 7x7 and
  18x18, Factorio's own numbers. It was `shipped` on GregTech's energy capability and is ported to
  NeoForge's transfer API with the rest of the core.
- **Power poles have a wire reach** — `shipped` (#280, ADR-0062). The core's poles link to every
  pole within the shorter of the two reaches -- 7.5, 9 and 18 blocks, Factorio's own -- and every
  linked pole is one network with one balance: generators first, accumulators second, only generator
  surplus charges. Reach is measured in three dimensions, which Factorio has no need to. The wire is
  drawn between every linked pair and disappears when the link breaks (#281). Oritech's transmission
  pole left the power path with #284, so a wire between two core poles is the only thing that
  crosses between areas.
- **Poles wire themselves on placement, and the player adds or cuts wires by hand** — `planned`,
  #296, ADR-0068. A stored wire, not reach, joins two poles. Placement wires to up to 5 poles in
  reach that share no neighbour; the Engineer's Pick adds or cuts one wire at a time.
- **Place a pole with no wires (shift-place)** — `planned`, `unargued`. Left out of #296.
- **Clear all of a pole's wires at once** — `planned`, `unargued`. Left out of #296.
- **Transformers between voltage levels** — `excluded`. `by-consequence` of having no voltage.
- **The power graph as a diagnostic surface** — `unargued`, no verdict.

### Power generation

- **verdict**: `adapted`
- **notice**: the Steam Engine is Oritech's engine under a pack block, calibrated by mixin to
  Factorio's 30 mB/s and 450 FE/t, and the solar panel is a pack block on Oritech's Big Solar Panel
  entity at Factorio's 60 kW on Factorio's day (#508).
- **where**: all bodies
- **via**: `pack`, `oritech`
- **owner**: ADR-0048, ADR-0060, ADR-0062, ADR-0077
- **ticket**: #135 (the Steam Turbine), #7 (per-body solar); #104, #189, #224, #283 closed

Sub-rules:

- **Boiler and steam engine as the first power** — `adapted`. **ADR-0062 (#282) makes it two steps
  again**: the pack's Boiler makes steam and the **pack's Steam Engine** burns it into FE, which a
  pole pulls through the `wireworks:generators` tag. **ADR-0077 (#352)** makes the engine
  `factoryworks:steam_engine`, a pack block on Oritech's engine entity, placed from one item as
  its whole 2x1x2 footprint and broken as one, like the Assembling Machine; Oritech's own engine is
  swept. A mixin calibrates it to Factorio — 30 mB/s and 450 FE/t per engine at the efficiency
  curve's peak, no water returned — and keeps Oritech's chaining and fill-driven speed. The history
  below is superseded where it disagrees.
  *Before ADR-0062:* the chain was **four** steps, not two:
  the **pack's Boiler** burns solid fuel and makes low-temperature steam, the **pack's Steam Engine**
  eats that steam and emits Create rotation, Power Grid's generator assembly turns SU into watts, and
  the grid carries them. A Factorio player's boiler-and-engine pair has a rotational stage wedged in
  the middle of it, and the grid is granted at a rung rather than arriving with the first fire.
  *(#104 corrects "three steps" and "a Create Steam Engine burns fuel": the Steam Engine burns
  nothing — it is the prime mover.)* **ADR-0048 re-cut both of the first two steps.** The first was
  #37's LP Solid Boiler; the boiler is now pack-authored, one tier, under ADR-0047's burner model —
  the third customer of the buffer the Furnace and the Burner Mining Drill already share, and #224
  shipped it: `factoryworks:boiler`, fuel and water in, low-temperature steam out at Factorio's
  own 60 mB/s. The second
  was *"a Create Steam Engine turns that steam into rotation"*, and it **was never implementable**:
  Create has no steam fluid at all. Its boiler is a Fluid Tank multiblock holding **water**, heated
  by Blaze Burners, and the Steam Engine mounts on that tank — it cannot consume steam from a pipe,
  from any boiler or from anything else. The pack authors that step. The engine emits rotation and
  not electricity on purpose: an engine that fed a pole directly would route around every mechanic
  ADR-0036 selected Power Grid for.
- **Accumulators** — `shipped`, #283. `accumulator` is Oritech's Large Energy Storage, mixed in to
  Factorio's 5 MJ and 300 kW.
- **Solar panels** — `shipped`, #529-#531. `solar-panel` is `factoryworks:solar_panel`, a pack block on
  Oritech's Big Solar Panel entity (ADR-0062, ADR-0077), placed and broken as one footprint with no
  machine cores. It makes Factorio's 60 kW (30 FE/t) at noon along Factorio's day curve, nothing at
  night or under a roof, with a one-tick buffer and no weather. It is crafted from Factorio's
  recipe, Oritech's own panel is swept and unlisted, and Jade shows what it is making now.
  Per-body output is #7. *Before ADR-0060 both were Power Grid's (#148).*
- **Steam as a stored, pipeable intermediate** — `shipped` for low-temperature steam, which the
  Boiler makes and pipes carry to the Steam Engine; high-temperature steam waits on #135. **Two
  fluids rather than one**. ADR-0048 registers low-temperature steam, which the Boiler makes and the Steam Engine eats,
  and high-temperature steam, which ADR-0033's reactor emits and only the Steam Turbine takes — two
  registry entries rather than one fluid carrying a temperature, because Factorio has exactly two
  temperatures with exactly two consumers. Both are `factoryworks:`.
- **The Steam Turbine, on superheated steam** — `planned`, the pack's, and **the pack's only FE-side
  generator**. *Moved here from [Nuclear fission](#nuclear-fission) by #104*: ADR-0033 names the row
  **for the fluid, not for fission**, and the Turbine has two producers on two bodies — Terra's
  Nuclear Reactor and Ignus's acid neutralisation. Filing a cross-body generator under Terra's
  fission chapter hid what it is. Superheated steam is a **pack-owned fluid** — `factoryworks:`,
  not GregTech's, since `gtceu:steam` is not inert and GregTech's own steam machines accept it,
  which would re-open the power layer #37 removed (ADR-0048; this corrects an earlier "its own GT
  material") — and **only the Turbine accepts it**; ordinary steam keeps the four-step chain above, which the Turbine will not take, and
  that fluid split — not the Converter — is what stops it retiring the rung-1 generator assembly. **Not
  registered yet** (#107's siblings): ADR-0033's stated design, unbuilt.

### Nuclear fission

- **verdict**: `adapted`
- **notice**: in the first cut the reactor emits **superheated steam** directly, so Factorio's
  reactor-to-exchanger ratio and heat-pipe layout puzzles are not here yet. The heat layer is
  deferred to #497, not absent (ADR-0098).
- **where**: Terra
- **via**: `pack`
- **owner**: ADR-0033, ADR-0098
- **ticket**: #135; #89 (closed)

**Settled by ADR-0033: the chapter ships, pack-authored.** Factorio's own tech costs place it —
`uranium-mining`, `uranium-processing` and `nuclear-power` are all chemical science, so rung 3;
`nuclear-fuel-reprocessing` adds production, so rung 4; **Kovarex costs space science** and is
post-launch, so Terra runs at raw 0.7% U-235 exactly as Nauvis does. Three pack machines —
Centrifuge, Nuclear Reactor, Steam Turbine — restated on Oritech's models by ADR-0098 after the GT
chassis left with ADR-0060.

**Mekanism was refused, and the earlier `via: mekanism` was a mistake of fact**: the Fission Reactor
lives in **MekanismGenerators**, which this pack does not install. Adopting it would have brought six
further generators onto ADR-0017's Power generation row. **#104 later struck that row's Mekanism
clause on the same fact**, and the refusal here is why: base Mekanism registers no generator block.

**The Steam Turbine is not on this row.** #104 moved it to [Power generation](#power-generation),
where ADR-0033's own framing puts it — the row is named for the fluid, not for fission, and the
Turbine's second producer is Ignus's acid neutralisation, on a body with no reactor. This row keeps
the Reactor, the Centrifuge and the fuel chain.

Sub-rules:

- **Kovarex enrichment** — `blocked`, and correctly so: it costs **space science** in Factorio, so it
  belongs to the post-launch map rather than to Terra. Terra's 0.7% yield is the fidelity, not a gap.
- **Reactor neighbour bonus** — `excluded` until #497. `by-consequence` of the first cut having no
  heat layer: there is nothing to conduct between reactors, so the bonus has no board.
- **Heat pipes and heat exchangers as a separate transport network** — `planned`, Terra, #497.
  ADR-0098 brings the **conduction** layer back from Gelida: heat pipes buffer 1 MJ/°C over
  500–1000 °C and flow only down a differential, and the Heat Exchanger then produces Superheated
  Steam in the Reactor's place. Factorio's two thresholds — **≥500 °C for a heat exchanger, ≥30 °C
  to keep a building warm** — are the seam: the **freezing** layer, where buildings freeze by
  adjacency with per-entity draw and an immunity list, stays Gelida's.

### Pollution

- **verdict**: `planned`
- **where**: all bodies
- **via**: `kubejs`, `pack`
- **owner**: ADR-0055 (supersedes ADR-0005)
- **ticket**: #433; #109, #118 closed

GregTech 7.0.2 has no pollution system — the mod contains nothing matching `pollut` — so Emission is
ours and none of it is built yet.

**ADR-0005's EU/t proxy was superseded before it shipped.** That ADR scored Emission per chunk off
the **EU/t draw of running GT machines** and rejected per-entity rates as costing "tagging every
recipe in a GregTech pack". Factorio states emission per prototype as
`energy_source.emissions_per_minute`, and that field is in the dump the extractors already read —
extraction is not tagging, and the pipeline that makes it free was built *after* ADR-0005. ADR-0055
takes the corpus rates instead. The proxy also misranks: a boiler pollutes far more per joule than
an assembler, and an idle machine emits its idle rate rather than zero, so under EU/t a coal-fired
base and an electric one of equal draw are equally dirty.

Per-chunk accumulation, decay and diffusion are unchanged from ADR-0005 — they are what make the
score a spatial problem and outpost placement a decision.

Sub-rules:

- **Spread to neighbouring chunks, and decay over time** — `planned`. Both, and they are what makes
  outpost placement a decision.
- **Absorption by terrain and trees** — `unargued`, no verdict.
- **Per-planet consequences** — `blocked`. Named in principle, unspecified everywhere but Terra;
  migrated here out of `docs/gdd.md` §7.

### Enemies and evolution

- **verdict**: `planned`
- **where**: Terra
- **via**: `pack`, `native_mechanic`
- **owner**: ADR-0055
- **ticket**: #434, after #227 and #433; #118 closed

Nothing here is built. **The shape changed wholesale with ADR-0055**, which reversed the previous
entry in this row: emission was to attract *Illager raids to an Overseer at your outpost*, with no
nest, no expansion and no evolution factor. That design existed because `minecraft:raid` is
village-anchored and needed something to path at. It was never argued against Factorio's own loop,
and `docs/gdd.md` §6 now points at ADR-0055 instead (#375).

Factorio's loop is one mechanism: nests absorb the pollution that reaches them, and absorbed
pollution is what buys the attack groups. The raid *is* the nest's output. The pack reproduces
that, with nests and waves held as saved data and entities as their rendering — the Dormant Siege's
own idea, applied to the thing it is cheaper to apply it to.

Sub-rules:

- **Pollution triggers attacks** — `planned`. Via nest absorption, not a threshold on the outpost.
- **Attacks are state until a player is present** (the Dormant Siege) — `planned`. Generalised by
  ADR-0055: nests and in-flight waves are both records, and mobs instantiate near the player a wave
  is aimed at. A raider abandons its raid beyond 112 blocks, so a wave cannot walk Factorio's
  distances as entities.
- **Nests, expansion and clearing territory** — `planned`. ADR-0055. Was `excluded`/`by-consequence`
  on the raid design; expansion runs on its own timer rather than on the cloud, capped by the same
  distance-density rule that placed the original nests.
- **Evolution factor rising with pollution and time** — `planned`. Was `blocked` for want of a nest
  to evolve. One global scalar on all three of Factorio's inputs — time, emission produced, and
  nests destroyed — the third being why clearing the map is not a permanent win.
- **Enemies destroy structures** — `adapted`. Factorio's biters eat walls and turrets; Minecraft
  mobs grief nothing, so this is ours to build, and it is bounded to the
  `factoryworks:destructible` tag rather than to anything in the way.
- **Gleba's pentapods** — `unargued`, no verdict. `docs/planets.md` marks them TBD.

### Wildlife and natural mob spawning

- **verdict**: `shipped`
- **where**: Terra
- **via**: `pack`
- **owner**: ADR-0093
- **ticket**: #480

Base Factorio has no wildlife but fish, and no enemy but the biters. Terra spawns no vanilla mob on
its own: every biome's spawner lists are empty and chunk generation places no animal, and a new
world starts with the mob, phantom, patrol and wandering-trader game rules off. The biters are
[Enemies and evolution](#enemies-and-evolution), which spawns its own mobs.

Sub-rules:

- **Fish** — `unargued`, no verdict. Factorio's fish swim in water, are mined for raw fish and heal
  the player. Terra's sea spawns none.

### Combat: guns, ammo, turrets, walls

- **verdict**: `planned`
- **where**: Terra
- **via**: `pack`
- **owner**: #118
- **ticket**: #230

**This was the canonical `by-consequence` row, and #118 reversed it.** #26 dropped Military science
because its ingredients feed nothing downstream, and seven `combat/*` shelves went `not_emitted`
behind it — turrets, guns, ammo, armor, capsules, equipment and walls. Nobody decided this pack has
no combat; a science-pack pruning decided it for them. The premise is now false: with ADR-0055's
nests on the map, those ingredients feed the thing you defend against and the thing you go and
clear, so **all seven shelves come back together** rather than two of them staying cut for a reason
nobody believes.

Sub-rules:

- **Turrets fed by ammo items** — `planned`. Not powered-and-free: ammo is a production cost with a
  research ladder, and Factorio deliberately shares magazines between the gun turret and the SMG,
  so one ammo line has two customers.
- **Personal firearms** — `planned`. Pistol and SMG, sharing the turrets' ammo. A player expected to
  go and clear a nest needs something to clear it with.
- **Walls** — `planned`. Factorio's wall and gate, and the designated member of the
  `factoryworks:destructible` tag — without one, every player picks a different block and the
  mechanic has no shape.
- **Military science returns** — `planned`. #26's pruning is reversed on its own stated reason; the
  Factorio tech tree gates `military-2/3/4`, the laser/rocket turrets, `railgun`, `uranium-ammo`,
  the shields and the power-armor rungs behind it, and ADR-0022 imports that tree as data precisely
  so its prerequisites are not retyped. Reopens #26 and touches ADR-0018.
- **Mechanism is first-party; art and possibly logic are delegated** — `unargued`. A research ticket
  specifies which third-party mods supply models, textures and any borrowed behaviour, and what
  their licenses permit. The Steel Pick's deleted `build-pick-textures.py` was the precedent for
  deriving art from a jar the pack already depends on, and that reasoning never transferred to a mod
  the pack would install only for its assets — nor did it survive the jar leaving (#241, #323).

Note that `not_emitted` did **not** settle the shelf even while the row read `excluded`:
`combat/defensive-structure` is `not_emitted` and #57 still shipped a Radar. That is the proof case
for the two axes never reading each other, and it is why reversing this row is a ledger edit rather
than a regeneration.

### Armor and the equipment grid

- **verdict**: `planned`
- **where**: Terra
- **via**: `pack`
- **owner**: #118
- **ticket**: #231

Same #26 cascade, reversed with the rest of it. **The old note here was stale twice over**: it read
"MekaSuit is the spacesuit (`docs/gdd.md` §1), and a MekaSuit *is* an equipment grid", but ADR-0035
removed Mekanism from the pack jars and all (#146), and `docs/gdd.md` §1 now says so itself. The
mechanic does not ship under another name; nothing in the pack expresses it.

Factorio's shape is a six-rung ladder — `light-armor`, `heavy-armor`, `modular-armor`,
`power-armor`, `power-armor-mk2`, `mech-armor` — with the grid arriving at `modular-armor` and
seventeen equipment technologies above it. Factorio has **no** space suit; `grep -i
"space-suit|spacesuit|oxygen|pressure|life-support"` over `data/factorio/*.json` returns nothing
across 163 recipes and 162 technologies. Whether GCyR's suit and its `enableOxygen` default survive
contact with the armour ladder is a reconciliation downstream of this row, not an input to it — the
pack builds GCyR from source (ADR-0001), so it is a thing that changes rather than a constraint to
route around.

### Capsules

- **verdict**: `planned`
- **where**: Terra
- **via**: `pack`
- **owner**: #118
- **ticket**: #230

Same #26 cascade, reversed with the rest of it. The seven shelves shared one stated reason and it is
false, so leaving this one behind would keep a shelf cut for an argument nobody holds. `personal-roboport`
in the utility-equipment shelf will collide with ADR-0017's one-mod-owns-each-capability rule, since
Create owns logistics before AE2 — that needs its own argument, and it gets one rather than
inheriting a dead premise.

### Modules and beacons

- **verdict**: `blocked`
- **where**: —
- **owner**: `unargued`
- **ticket**: #120

`production/module` is `undecided` in `subgroup-owner.json` on one recipe, `beacon`. Factorio's module
system has no pack analogue; #42 names a Mekanism upgrade in the `production` pack's slot list, which
is a data point and not an answer. Follow-on: #120.

Speed/productivity/efficiency as a three-way tradeoff you retrofit into an existing factory is a large
part of Factorio's mid-game, and nothing in the stack reproduces it. `blocked`, not `excluded` — the
argument has not been had.

### Research and science packs

- **verdict**: `planned`
- **where**: all bodies
- **via**: `pack`, `kubejs`
- **owner**: ADR-0018, ADR-0022
- **ticket**: #217 (the Lab on the grid), #228 (Military science); #66, #82, #103, #260 closed

Four packs plus an unscienced rung 0, gated by Researchd's Research Lab, fed by pipe and consumed
unattended.

Sub-rules:

- **Each pack rung grants a capability the next rung physically requires** — `planned`. ADR-0018.
- **Military science** — `planned`. #26 dropped it because its ingredients fed nothing downstream;
  #118 makes them feed the combat line, so the pruning is reversed on its own reason. See
  [Combat](#combat-guns-ammo-turrets-walls). Reopens #26 and touches ADR-0018.
- **Sapros's science pack spoils** — `planned`. The buffer-as-liability puzzle.
- **Research consumes packs continuously while running** — `adapted`. Researchd's Lab consumes on
  completion of a pack batch rather than metering a rate; only `consumePack` reads the Lab.
- **A lab draws power, so research competes with the factory for it** — `shipped`. Researchd's Lab
  draws `research_lab_energy_usage` FE a tick while researching, set in `config/researchd-common.toml`
  to 30: Factorio's 60 kW at the pack's 100 J to the FE (ADR-0060).

### The technology tree

- **verdict**: `shipped`
- **where**: pack-wide
- **via**: `kubejs`
- **owner**: ADR-0022

The tree's topology is extracted from Factorio rather than transcribed —
`data/factorio/technology.json` is committed, `researchd.js` declares each node with
`fromFactorio(...)`, and `tests/factorio/test_tech_extract.py` asserts the pruned tree is still a
valid tree and that every declared name exists. Registered, and the check its claim warrants passes.

Sub-rules:

- **Prerequisites form a DAG the player navigates** — `shipped`.
- **Infinite research tiers with escalating cost** — `excluded`. `by-consequence`: the extraction
  prunes them, and nothing downstream wants them.
- **Research triggers (SA: unlock by doing, not by paying)** — `unargued`, no verdict.

### Rocket silo and rocket launch

- **verdict**: `planned`
- **where**: all bodies
- **via**: `factoryworks_core`
- **owner**: #378, which authors the silo rather than waiting on Oritech: Space Age
- **ticket**: #378, on #25's map of Terra's flow to the first rocket launch

Sub-rules:

- **The launch is a physical, watchable event** — `planned`. `docs/gdd.md` §4 makes it
  explicit that the launch is the payoff and is never simulated.
- **Rocket parts are produced continuously and buffer in the silo** — `unargued`, no verdict.
- **Cargo landing pad** — `planned`, the post-launch arc.

### Character movement on foot

- **verdict**: `adapted`
- **where**: all bodies
- **via**: Minecraft's own walk, against Terra's starting-area distances
- **owner**: ADR-0049, #207

Base movement on foot only. Vehicles are [Personal transport](#personal-transport) and #121; the two
do not collide.

Factorio's engineer and Minecraft's player do not walk at the same speed, and the pack does not
change that. The traversal budget has two halves, and both are extracted rather than felt:

| | Factorio | Terra |
| --- | --- | --- |
| speed | `character.running_speed` 0.15 tiles/tick × 60 = **9.0 tiles/s** | Minecraft's walk **4.317 blocks/s** (sprint 5.612) |
| furthest starting resource | `starting_resource_placement_radius` **150 tiles** | `DISTANCES` in `scripts/build-terra-start.py`, furthest field **62 blocks** |
| hub to furthest field | 150 / 9.0 = **16.7 s** | 62 / 4.317 = **14.4 s** |
| two fields, perpendicular | 212 / 9.0 = **23.6 s** | 88 / 4.317 = **20.3 s** |
| two fields, opposite | 300 / 9.0 = **33.3 s** | 124 / 4.317 = **28.7 s** |

A tile and a block are both one metre, so nothing is converted but the tick rate. The speed is read
in `scripts/factorio-resource-extract.py`'s `character_movement()` into
`data/factorio/resource.json` and asserted by `tests/factorio/test_resource_extract.py`; the radius
is the corpus constant the same file already carried.

**Both halves drifted, in opposite directions, and they cancel.** Terra's player walks at 48% of the
engineer's speed and its fields sit at 41% of Factorio's starting radius, so **every** leg comes out
at 0.86× Factorio's time — the ratio is the same whichever pair you measure, which is what keeps the
verdict from resting on a chosen leg. Three legs are tabled rather than one because #170's report is
about moving *between patches*, not out from the hub: Terra's four fields sit on the four cardinal
faces (iron east, copper north, coal west, stone south) at the size variant's distance, so the
traversal a player actually makes is a chord — up to 124 blocks — and not the 62-block radius.
So: **no base speed is set, and `DISTANCES` does not move.** A flat global buff would also have spent
Block Runner's concrete bonus ([Terrain modification](#terrain-modification)), which is `adapted`
precisely so that a built surface is the thing that makes you faster.

**The one soft number is Factorio's side.** `starting_resource_placement_radius` is the bound a
starting patch may be placed within, not where patches typically land. If Factorio's own starting
patches cluster well inside 150, the 0.86 flatters Terra and this row is worth reopening against
measured patch positions rather than the bound.

The playtest report that opened #207 stands as a report — the opening *feels* long, and a measured
patch-to-patch leg is about 100 blocks, some 23 s walked — but the arithmetic says the cause is not
distance or speed relative to Factorio. Factorio lets you zoom out and see all three
patches at once; Minecraft does not. That is legibility, and its surfaces are #116 (radar and surface
indicators) and #158 (pole supply-area overlay), not movement.

Sprinting is not counted above. It burns hunger, and #183 has not decided whether Minecraft's hunger
mechanic stays in the pack at all; a budget that assumed sprinting would be load-bearing on an
undecided mechanic.

### Character reach

- **verdict**: `adapted`
- **where**: all bodies
- **via**: `native_mechanic`
- **owner**: #413
- **notice**: build and reach distance 10 → **16**, one chunk, following Satisfactory's generous
  Build Gun; resource reach 2.7 → Minecraft's own **4.5**, extended to every block that is not a
  Building, because Minecraft has terrain and Factorio does not.

Factorio's character carries four distances: `build_distance` 10, `reach_distance` 10,
`reach_resource_distance` 2.7 and `enter_vehicle_distance` 3. The pack keeps one reach of 16 for
placing any block, using any block and breaking a **Building** -- a block whose Factorio item has a
`place_result`, or a rail planner's `rails`, generated into the `factoryworks:buildings` block
tag. Anything else, ore, trees and terrain included, breaks only within 4.5, in every game mode.
Entity reach stays at vanilla's 3, which is Factorio's `enter_vehicle_distance`, and raising it would
also raise melee reach against the biters [Enemies and evolution](#enemies-and-evolution) is balanced
on. Research or equipment that raises reach is not here.

### Personal transport

- **verdict**: `blocked`
- **where**: —
- **owner**: `unargued`
- **ticket**: #121

`logistics/transport` is `undecided` on one recipe, `car`. Personal transport is not an ADR-0017
capability and no rung grants it. Factorio's car, tank and spidertron have no pack answer, and
Minecraft's own movement options (elytra, horses, boats) are neither gated nor factory-produced.
Follow-on: #121.

### Terrain modification

- **verdict**: `adapted`
- **where**: all bodies
- **via**: Block Runner (walking speed), vanilla concrete (the block)
- **owner**: `by-consequence` for cliffs, ADR-0019 for landfill, #87 for concrete

Sub-rules:

- **Landfill** — `excluded`. Vanilla block placement already is the verb (#356). Bridging the shelf
  costs one block per column, while filling the deep sea costs about sixty per column down to the
  bedrock band and is a project, which is roughly Factorio's cost shape. This one *is* argued.
- **Cliffs and cliff explosives** — `excluded`. `by-consequence`: no body generates cliffs as an
  obstacle, so nothing needs removing.
- **Concrete and its speed bonus** — `adapted`. #87 maps concrete onto vanilla's grey concrete and
  refined concrete onto light grey, and routes their recipes to the Assembling Machine like every
  other `crafting-with-fluid` craft. The two hazard concretes are not emitted (#473): vanilla has no
  striped block, and 10 grey concrete recolours into 10 of any other colour but light grey instead.
  **The walking-speed bonus does have an analogue**: Block Runner gives a block a configurable
  walk/run speed, which is exactly what Factorio's concrete is for. The earlier `excluded` verdict
  was written before that mod was in the pack and is superseded rather than reversed on argument.
- **Laying a floor over terrain** — `adapted`. Factorio places a tile over grass or another tile;
  here a Fast Replace lays stone bricks or concrete over dirt, grass or gravel, through a group typed
  in data rather than read from the corpus (ADR-0108, #552).

### Repair and entity damage

- **verdict**: `blocked`
- **where**: —
- **owner**: #118
- **ticket**: #436

`production/tool` is `undecided` on one recipe, `repair-pack`, and the reason this row carried —
"nothing on Terra takes damage the way a Factorio entity does; with no biters attacking buildings,
the whole repair loop has nothing to repair" — **was falsified by ADR-0055**. Enemies now damage
blocks in the `factoryworks:destructible` tag, so there is something to repair.

`blocked` rather than `planned`: the premise is gone but the argument has not been had. It is also
load-bearing in the other direction — ADR-0055 bounded destruction to a tag partly because there is
no repair mechanic underneath it, so a repair loop and the size of that tag are one question, not
two.

### Radar and map exploration

- **verdict**: `adapted`
- **where**: Terra
- **via**: `pack`
- **owner**: #116
- **notice**: the nearby area is 9x9 sectors rather than Factorio's 7x7, and it is charted once
  rather than kept live, so the map shows it as first charted.
- **ticket**: #116, confirmed on a player's map; the art is #367

Factorio's Radar (ADR-0045, ADR-0079): a 3x3x3 `factoryworks:radar` drawing 150 FE/t that
charts map at range for its owner's team. It detects nothing hidden, since ore lies on the surface.
#368 built the machine and the team's chart on the server, and #369 sends the chart to each member's
FTB Chunks map, late joiners included. Since #370 each outfield patch whose centre is in the chart,
or in a chunk the player has walked into view, gets a marker on both maps showing the resource's
drop item, name and what is left, as of when it was last charted or walked past; starting fields
get none, and a mined-out patch's marker is removed (#371).

**This row is the proof case for the two axes never reading each other.** `combat/defensive-structure`
is `not_emitted` in `subgroup-owner.json`, and a ledger that read its verdicts out of that file would
have recorded "radar: excluded" — which is wrong whatever this row's verdict turns out to be.

Sub-rules:

- **Reveals map by scanning distant sectors** — `shipped`. One 32-block sector per 10 MJ within 14
  sectors, beyond the nearby area, in square rings outward from each ring's top-left sector
  clockwise, unexplored sectors first and then a re-scan in turn, which changes no map while a
  charted sector is never re-sent. Charted on the server since #368
  and sent to every team member's map since #369. "Unexplored" is the team's chart: the server does
  not know what a player has walked.
- **Keeps the nearby area live** — `adapted`. The 9x9 sectors around the Radar, 18 Minecraft
  chunks, pulse every 250 kJ, 0.83 s at full power, from the same draw as the sector scan.
  Factorio's 7x7 is 14 chunks, inside a normal render distance. A pulse charts only the area's uncharted
  sectors, one a tick, and a charted sector is never re-sent, so the map shows the area as first
  charted rather than live.

### The logistic request and trash system

- **verdict**: `excluded`
- **where**: —
- **owner**: `by-consequence`

Follows [Logistic robots](#logistic-robots): personal logistic requests and auto-trash are the bot
network's player-facing half, and they go with it.

### Day and night cycle

- **verdict**: `planned`
- **notice**: Terra still runs vanilla's 20-minute day; ADR-0099 gives it Nauvis's 7 minutes, and
  the row becomes `adapted` when #509 lands.
- **where**: Terra, Sapros
- **via**: `native_mechanic`, `pack`
- **owner**: ADR-0099
- **ticket**: #509 (Terra), #8 (the other bodies)

Sub-rules:

- **Solar output follows the cycle, and accumulators bridge the night on Terra** — `shipped`, #529-#531.
  The panel follows Factorio's day curve and banks no night, so the Accumulator carries it, at
  Factorio's 0.84 per panel on Terra's seven-minute day (#509).
- **Solar output on Electro** — `planned`, #7. It is the planet's identity, a multiplier on the
  Terra panel's output; Electro's own cycle is #8's.

### Crash site

- **verdict**: `planned`
- **where**: Terra
- **via**: `pack`
- **owner**: ADR-0107
- **ticket**: #498

Factorio's freeplay starts the engineer beside the ship they crashed in, once per world, for the
first player: the hull and its debris are minable for nothing, and its containers hold the starting
plates. Terra's **wreck** is one indestructible room at the hub's centre that the player wakes in and
returns to after dying, with one five-slot cargo hold (the `crash-site-spaceship`'s inventory) filled
with the Hold once per world. The debris, the scattered segments and the opening cutscene are not
reproduced.

### Controls

- **verdict**: `planned`
- **where**: all bodies
- **via**: `factoryworks_core` (quick transfer), Mouse Tweaks (in-GUI), `native_mechanic` (pipette)
- **owner**: #208
- **ticket**: #208

Factorio's control surface is a mechanic in its own right: the gestures are how the player moves
items without a screen, and a pack that reproduces the production chains while making every transfer
a two-step GUI operation has reproduced the arithmetic and not the game. The rows below are the whole
surface, not only the one #208 asked for — a section admitting a single gesture would be re-argued
the next time one came up.

Sub-rules:

- **Fast entity transfer and fast entity split** — `planned`, #208. The pack calls these **quick
  transfer** and **quick split** (`CONTEXT.md`); Factorio's own names appear here and nowhere else,
  per ADR-0028. Two `KeyMapping`s in `factoryworks_core`, defaulting to `CTRL` + left and right
  mouse and declared in Controls so a conflict with Carry On or Building Gadgets is the player's to
  resolve. Magnitude is Factorio's verbatim — the held stack in, everything the target will give up
  out, halved for the split. The target set is every GregTech machine and every pack-authored block
  that holds items, reached through the block's own item handler with no pack-authored slot policy.
  **The mid-recipe question answers itself on both engines**: GregTech consumes inputs at
  `RecipeLogic.setupRecipe`, so a running machine has nothing to take back, and the pack's furnace —
  which consumes at completion instead — already refuses extraction from anything but its output slot
  in `FurnaceItemHandler`. Delegating to the handler is what makes the two timings invisible.
- **In-GUI stack and inventory transfer** (`SHIFT`/`CTRL` + click inside a machine screen) —
  `shipped` via Mouse Tweaks, which is in the manifest and does exactly this. Distinct from the fast
  entity gestures above: those need no screen open.
- **Pipette tool** — `adapted`. Vanilla's pick-block is a near-exact match, already bound, and picks
  from the inventory in survival. No work.
- **Drop item into a machine** (`Z`) — `excluded`. It is a one-item quick transfer, and shipping both
  means two bindings differing only in magnitude.
- **Drag-building** — `adapted`. Belts and Oritech's fluid pipes are laid by Groundworks' **Stretch**:
  a sneak-click stores the start, each further sneak-click an anchor, and a click lays the line,
  charged one item a block, with Raise and Lower setting its height and a detour round what is in
  the way (#452). Joining pipes across an interior anchor is #467.

### Factoriopedia

- **verdict**: `planned`
- **where**: pack-wide
- **via**: `emi`
- **owner**: ADR-0088 for the index; ADR-0091 for where a resource is found
- **ticket**: #479; #478 closed

Factoriopedia is Factorio's in-game encyclopedia. For every item, fluid and entity it shows what
makes the thing, what uses it and where it is found. EMI is the pack's recipe viewer, and it answers
the first two questions from the recipes the pack emits. It has no answer to the third for anything
that no recipe makes.

Sub-rules:

- **Made by and used in** — `adapted`, via EMI. Factoriopedia opens on a page per item. EMI opens on
  the item's recipes and uses.
- **Lists only what exists in the game** — `shipped`, ADR-0088. The index is the Obtainable set,
  derived rather than typed, and it takes in the drops of every block the live worldgen places
  (#454). No mob spawns, so no mob drop is in it (ADR-0093).
- **Where a raw resource is found** — `planned`, ADR-0091, #479. Ores, logs, water, crude oil and
  steam come from a mechanic rather than a recipe, so EMI shows nothing for them. A pack EMI
  category, Where it is found, gives each one what extracts it and where, derived from the
  Obtainable sources as Factoriopedia's resource page does.

---

## Space Age

### Interplanetary travel

- **verdict**: `blocked`
- **where**: pack-wide
- **candidates**: Oritech: Space Age, or `factoryworks_core` (#340)
- **owner**: ADR-0060, ADR-0006, `docs/gdd.md` §2
- **ticket**: #340 (the wait); #112, #54 closed

Six bodies, seven destinations. **`blocked` by ADR-0060**: GCyR left, and travel waits for a
first-party Oritech space addon. This row and the three orbital rows below it are blocked together,
and the bodies other than Terra are parked under `kubejs/parked/`. The travel rows and this one have
no `via` until #340 picks a route; the Rocket silo is first-party (#378).

As of 2026-09-21 that addon exists but is unreleased: `space-age/` on Oritech's `26.1` branch builds a
separate jar that no release ships. It has rockets assembled from blocks, a pad, a flight planner on
a 2.5D star map of Earth, Sun, Mars and asteroids, and asteroid tugging. It has no dimensions and no
planet surfaces. A flight lands in the dimension it left from, so it is the launch half of this row
and none of the travel. The pack keeps waiting rather than adopting it or building travel itself (#340).

Sub-rules:

- **Each planet is a distinct surface with its own resources and its own puzzle** — `planned`.
- **The player travels physically and pays fuel** — `planned`.
- **Arrival is hostile and you must establish a foothold** — `adapted`. The Vanguard Kit pastes a
  beachhead; Factorio drops you into a working platform's cargo pod, so the shape of the first five
  minutes differs entirely.

### Space platforms

- **verdict**: `blocked`
- **where**: Terra Orbit, and every body's orbit
- **candidates**: `factoryworks_core`. Oritech: Space Age has no stations (#340)
- **owner**: ADR-0006
- **ticket**: #340

A Platform is a static orbital factory, not a ship — no thrusters, no navigation, no interplanetary
transit, and therefore no asteroid defence and no hull mass to manage. That is an argued divergence
(ADR-0006) rather than an unbuilt one, so expect this row to become `adapted` with it as the notice
once Platforms exist.

Sub-rules:

- **A platform is built outward from a starter foundation** — `planned`.
- **Platforms fly between planets** — `excluded`. ADR-0006; this is the argued core of the adaptation.
- **Asteroid collision damages the platform, and it must shoot back** — `excluded`.
  `by-consequence` of static platforms.
- **Cargo travels by platform between planets** — `adapted`. See
  [Interplanetary logistics](#interplanetary-logistics); the pack's cargo is a Flight timer instead.

### Asteroid mining and reprocessing

- **verdict**: `blocked`
- **where**: orbits
- **via**: `pack`
- **owner**: `docs/gdd.md` §3, Map #25 (out of scope for the first arc)
- **ticket**: #114

Sub-rules:

- **Collectors harvest passing asteroid chunks** — `planned`. Ice and carbon at Terra Orbit.
- **Crushers break chunks into resources** — `planned`.
- **Reprocessing converts chunk types into one another, closing the loop** — `unargued`, no verdict.
  This is what makes the asteroid economy an economy rather than a drip, and nobody has thought about
  it.
- **Asteroid composition varies by orbit and by route** — `unargued`, no verdict.

A candidate, not a decision (2026-09-21): Oritech's Space Age addon tugs a whole asteroid down to the
surface, and a harder landing makes a bigger crater and recovers less of it. If the addon is adopted, that would
sit beside orbital collectors as a bulk ore delivery, the other answer to ADR-0020's exhaustion. It would not
replace them, since it reproduces neither the chunk loop nor reprocessing.

### Interplanetary logistics

- **verdict**: `blocked`
- **where**: pack-wide
- **candidates**: Oritech: Space Age, or `factoryworks_core` (#340)
- **owner**: `docs/gdd.md` §4
- **ticket**: #340 (the wait on interplanetary travel); #111 closed

Launch Terminals, Receiving Terminals and Drop Hatches, with unattended cargo held as a Flight with a
travel timer rather than as a moving entity. What the terminals are built on waits on #340.

Sub-rules:

- **Requesting from another planet, and cargo arriving unattended** — `planned`.
- **Orbit-to-surface drops are cheap and immediate** — `adapted`. Free and instant here; Factorio
  still pays a pod.
- **A drop with no receiver leaves a container to collect** — `planned`. Drop Pods.
- **Localized assembly forces on-site factories** — `planned`. Factorio's planet-locked buildings,
  generalised to components.

### Spoilage

- **verdict**: `adapted`
- **notice**: freshness is item identity with coarser stages rather than a continuously ticking
  percentage, so a stack does not have one blended freshness value and a conveyor of half-spoiled
  goods does not exist.
- **where**: Sapros, and any body holding its outputs
- **via**: `pack` (the Decay fork)
- **owner**: ADR-0010, ADR-0011

Sub-rules:

- **Spoiled results are themselves an input** — `planned`. Biosulfur from spoilage.
- **Spoilables cannot be parked in digital storage** — `planned`. ADR-0013; a pack addition with no
  Factorio equivalent, because Factorio has no AE2.
- **Freshness survives being processed** — `planned`. ADR-0010.
- **Chunk unload does not pause the clock** — `adapted`. ADR-0012: catch-up is sampled, not replayed.

### Quality

- **verdict**: `blocked`
- **where**: —
- **owner**: `unargued`
- **ticket**: #122

Five tiers of every item, quality modules, the recycler-plus-quality loop, and legendary as the
end state. Nothing in the stack has an item-quality axis, and bolting one on would touch every
recipe in the pack. `blocked` and not `excluded`: this is one of Space Age's three headline
mechanics and its absence has never been argued.

### Recycling

- **verdict**: `planned`
- **where**: Electro
- **candidates**: `factoryworks_core`, as a recycler on an Oritech machine body (ADR-0060)
- **owner**: `docs/gdd.md` §2, `docs/planets.md`
- **ticket**: #13

Sub-rules:

- **Scrap recycles into a spread of unrelated outputs, and the surplus is the puzzle** — `planned`.
  Scrap comes from generated ruins; the machine that recycles it is #13's.
- **Any item can be recycled back into a quarter of its ingredients** — `blocked`, following
  [Quality](#quality); without quality the universal recycler has no second purpose.
- **Voiding the surplus is a legitimate answer** — `unargued`, no verdict.

### Vulcanus: lava and calcite

- **verdict**: `planned`
- **where**: Ignus
- **via**: `pack`, `gregtech`
- **owner**: `docs/planets.md`
- **ticket**: #12, then that body's `Puzzle:` ticket

Sub-rules:

- **Lava is an infinite fluid resource** — `shipped` (#256). The Offshore Pump admits a lava source
  and pumps lava at its one corpus rate (ADR-0050); what lies on Ignus to pump is the body's work.
- **Molten metal as a fluid intermediate, and the foundry** — `planned`.
- **Sulfuric acid geysers, and acid neutralisation to water** — `planned`. Its steam output is the
  second consumer of ADR-0033's **Steam Turbine**, which is why that row is not Terra-only.
- **Demolishers as territorial obstacles** — `blocked`. No enemy model outside Terra, and this is the
  one place Space Age puts a boss between you and a resource.

### Fulgora: scrap and lightning

- **verdict**: `planned`
- **where**: Electro
- **via**: `create`, `powergrid`
- **owner**: `docs/planets.md`
- **ticket**: #13, then that body's `Puzzle:` ticket

Sub-rules:

- **No natural ore; everything comes from scrap** — `planned`. ADR-0009, ADR-0016.
- **Lightning damages what is not protected, and can be harvested** — `planned`.
- **Islands constrain buildable space** — `planned`.
- **Holmium and the electromagnetic plant** — `planned`.

### Gleba: agriculture and nutrients

- **verdict**: `planned`
- **where**: Sapros
- **via**: `pack`
- **owner**: `docs/planets.md`, ADR-0016
- **ticket**: #23, then that body's `Puzzle:` ticket

Sub-rules:

- **Crops are farmed and replanted, not mined** — `planned`. Yumako and jellystem are Factorio
  `plant` prototypes, not `tree`s: `growth_ticks 18000`, grown from a seed, and one harvest yields
  **50 fruit and zero wood**, consuming the plant. They are deliberately **out of ADR-0051's felling
  rule** — wood is terminal, while these are the first link of the agricultural science loop
  (`yumako-mash ×15 + jelly ×12 → bioflux ×4`; `bioflux + pentapod-egg → agricultural-science-pack`;
  `yumako-mash ×4 → nutrients ×6`, feeding the towers that produce the input). The real mechanic is
  the Agricultural Tower, so building the harvest gesture alone would be a different mechanic wearing
  the same blocks. Until #23, Sapros's trees stay log-by-log and `yumako_leaves` keeps rolling fruit
  and sapling.
- **Seeds come from processing the fruit, never from harvesting** — `planned`. `yumako-processing`
  and `jellynut-processing` return a seed at `independent_probability 0.02` alongside mash or jelly.
  Both are category-less (so hand-craftable in Factorio) but `enabled: false`, and **neither is in
  `data/factorio/recipe.json` nor `data/pack/item-map.json` today** — Space Age pruning dropped them.
  The 2 % output is also the pack's first probabilistic recipe result, which ADR-0038's queue
  contract does not currently admit: a step that can deliver nothing is not a step
  `PlanToQueue` can guarantee runs to the end. #23 owns both problems.
- **Nutrients as a consumable that machines eat** — `unargued`, no verdict. The biochamber's whole
  economy hangs on it.
- **Metal arrives by bacteria that spoil into ore** — `planned`. ADR-0016; no veins on Sapros.
- **Pentapods and eggs that hatch if you stall** — `unargued`, no verdict. TBD in `docs/planets.md`.

### Aquilo: cold and ammonia

- **verdict**: `planned`
- **where**: Gelida
- **via**: `pack`, `gregtech`
- **owner**: `docs/planets.md`
- **ticket**: #15, then that body's `Puzzle:` ticket

Sub-rules:

- **Fluids freeze without active heating, so every process carries a thermal budget** — `planned`.
- **Heating towers and heat distribution** — `planned`.
- **Ammonia chemistry, lithium brine, fluorine** — `planned`.
- **Cryogenic plant and quantum processors** — `planned`.

### Planet-locked buildings

- **verdict**: `planned`
- **where**: all bodies
- **via**: `kubejs`
- **owner**: `docs/gdd.md` §4, `docs/planets.md`
- **ticket**: #115

Foundries and big drills on Ignus, electromagnetic plants on Electro, biochambers on Sapros,
cryogenic plants on Gelida — craftable only where they belong, which is what forces a factory on
every planet instead of one factory and a shipping lane.

### Elevated rails

- **verdict**: `excluded`
- **where**: —
- **owner**: `by-consequence`

Follows [Trains](#trains): vanilla rail climbs and descends a block at a time, so a line that must
cross another goes over it, and an elevated-rail tier has nothing to add. Argued from the medium,
as underground belts are (ADR-0044).

### Fusion power

- **verdict**: `planned`
- **where**: Gelida
- **candidates**: MekanismGenerators
- **owner**: `docs/planets.md`
- **ticket**: #15, then Gelida's `Puzzle:` ticket

Fusion generator and reactor, craftable only on Gelida. **This row has no `via`, and cannot have
  one**: it named MekanismGenerators, which the pack has never installed, and ADR-0035 has since
  taken base Mekanism out of the manifest too — so the jar is now two manifest decisions away, not
  one. Adopting it is a decision Gelida's puzzle must argue, not inherit.

### The Shattered Planet

- **verdict**: `blocked`
- **where**: Atlantis
- **owner**: `docs/gdd.md` §7, migrated here
- **ticket**: #131

A named, orbit-only endgame destination with no defined puzzle, resource or attrition model.
Migrated out of the GDD's Open Questions.

Sub-rules:

- **Promethium science and the final research tier** — `blocked`.
- **A one-way journey of escalating attrition** — `blocked`, and `by-consequence` of static platforms:
  the journey *is* a platform flight, and ADR-0006 has no flying platform.

---

## Follow-on tickets

Load-bearing `by-consequence` and `blocked` rows get their own `Grilling:` issue rather than being
settled inside a row. Filed:

- #119 — where does redstone come from, now that #58 has cut it from Terra and the circuit network
  needs it? **Answered.** ADR-0095: nowhere, since Factorio 2.0 has no wire item; the Pick lays it free.
- #118 — does the pack have combat — biters, turrets, walls — or did Military science take them?
  **Answered.** ADR-0054 and ADR-0055: nests absorb emission and send the waves, all seven `combat/*`
  shelves come back, and Military science returns with them.
- #120 — modules and beacons, and whether the retrofit-tradeoff mid-game exists here at all.
- #121 — personal transport.
- #122 — quality, and whether an item-quality axis is affordable at all.
- #131 — the Shattered Planet: is Atlantis a mechanic, or a name on the map? The attrition half is
  `by-consequence` of ADR-0006's static platforms, so what is open is whether the destination
  survives without the journey.

## Every `planned` row has a ticket

A `planned` row says the pack has the mechanic and has not built it. Without a ticket that is
indistinguishable from having forgotten it, so **every `planned` row carries a `ticket` field** and
that is an invariant of this file: promoting a row to `planned` means filing something, or pointing
at what already exists.

Four rows point at a body ticket *and then* at a `Puzzle:` ticket that does not exist yet. That is
deliberate and not a gap — the GDD's delivery sequence cuts a body's `Puzzle:` ticket only **after
that body ships**, so writing them now would be inventing content for terrain nobody has built.

## How this stays honest

Convention plus discoverability. A body or puzzle ticket updates its own rows, and the CLAUDE.md skill
entry is what makes an agent find this file. **No automated check** — per `docs/testing/what-to-check.md`
this is a design ledger making no runtime claim, and a test here would be testing prose.
