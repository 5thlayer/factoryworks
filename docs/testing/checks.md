# Checks

What each check asserts, why, and the defect it exists for. `CLAUDE.md` indexes them by when to run each.

## Flora data check

`tests/flora/test_flora_data.py` asserts Sapros's tree and surface data are internally consistent
— features, loot tables, blockstates, textures and lang against what is actually registered, plus
which marshland carries which tree and that no stromatolite drops ore — with no game launch. Run it
after any edit to the trees, the stromatolites or the five biomes. The worldgen half is read from
`kubejs/parked/` while Sapros is parked (ADR-0060).

## Block asset check

`tests/pack/test_block_assets.py` walks every block the mod registers (#254): blockstate, model
parents and textures (the pack's, the installed jars' and the client jar's), lang key, item model
where the block has an item, and a loot table unless it is registered with `noLootTable`; a block
with an item of its own name must drop exactly it. The block list is parsed from every
`DeferredRegister.createBlocks` source and each ladder's names evaluated from its enum's
`blockName()`, so a new block is walked with no edit here, and a registration it cannot name fails.
Blocks with no item are the `NO_ITEM` patterns, each with its reason. Each machine's own
`test_*_assets.py` keeps only what a generic walker cannot judge.
Run it after adding a block or editing any blockstate, model, texture, lang key or loot table.

## Furnace ladder check

`tests/pack/test_furnace_assets.py` asserts the three furnace tiers `FurnaceTier.java` registers
have a blockstate covering both `facing` and `lit`; that each file resolves is the block asset
check's. Two of its assertions are the ladder's own -- the Electric tier wears no texture a burner tier wears, so it reads as a
different machine at a glance, and every texture it names is in the pack's namespace and credited
in `NOTICE`, since the art is copied from a CC BY-NC-SA repository (#324).
`tests/pack/test_smelting_type.py` holds the recipe type itself: that the pack's recipe class is **not** assignable to vanilla's
`SmeltingRecipe` -- GT's `proxyRecipes` converts that class specifically and would drop the count,
turning `5 iron_plate -> 1 steel_plate` into a 1:1 with no error and no log line -- that the count
survives both codecs, and that nothing in the mod reads recipes off vanilla's smelting type. It is
a source-text check because the assertion needs to name a Minecraft class the unit-test classpath
deliberately does not have. The arithmetic and the rules are
Minecraft-free unit tests under `mod/src/test/java/com/factoryworks/core/smelting/`: the
per-tier duration, the 90 FE/t draw and its buffer, the unsided routing by item, and the stall —
a blocked output starts no smelt, burns no fuel and voids nothing (ADR-0041). Whether the three
blocks smelt in a running game is `gametest/EnergyFaceTests` for the Electric tier (#271) and
`gametest/BurnerFurnaceTests` for the two burners (#432): on coal, each makes iron plate at its
tier's rate for 4,500 J a working tick, keeps the steel smelt's 5:1, and with a full output lights
no coal, spends no banked joule and starts no smelt. Dropping the output check, shrinking the input
by one, or a 4,000 J tick each turns both tiers' tests red.
`gametest/FurnaceOverloadTests` holds the input to the Overload Limit of the smelt taking it on all
three tiers (#518): 2 raw iron, 10 iron plates for the 5:1 steel smelt, fuel uncapped, and a
shift-click in the screen still placing 64; `FurnaceOverloadTest` holds the figures. Dropping the
cap in `FurnaceItemHandler` turns all three tiers red.

## GameTest harness

`./gradlew :factoryworks_core:runGameTestServer` from the repo root is the pack's only check
that loads a world. It is headless, needs no display and no human, and fails the command when a
test fails. The tests are in `mod/src/main/java/com/factoryworks/core/gametest/`, in the
**main** source set — a GameTest is code the game loads, so it cannot live in the Minecraft-free
test source set. 26.1 has no `@GameTestHolder` and no `neoforge.enabledGameTestNamespaces`: a test
is an entry in the `test_instance` datapack registry, registered through NeoForge's
`RegisterGameTestsEvent`. Groundworks and Beltworks register tests too, some of them for cases the
Pack's settings rule out on purpose, so the run selects `--tests factoryworks:*` and each repo's
own run holds its tests (#448). The selector takes one wildcard pattern, not a list.
`PFGameTestInstance` is the shape that event has no answer for — vanilla's `function` instance
resolves a `Consumer` out of the `test_function` registry, which is populated during `Bootstrap`,
before any mod is loaded.

Two seams are the harness's own rather than generic plumbing. The tests stand on a **generated**
stone platform (`scripts/build-gametest-structures.py`), for the reason `build-terra-start.py`
exists: a committed `.nbt` nobody can regenerate is a binary with no source. And the pack's data
reaches the run through **KubeJS**, on the dev runtime classpath with Rhino, reading the repo's own
`kubejs/` through a link `mod/run/kubejs` that the `linkKubeJS` Gradle task makes before every dev
run (#338). KubeJS resolves `kubejs/` against the game directory with no setting to move it, and
`mod/run/` is untracked, so the link is built rather than committed. There is no second copy: the
startup scripts register the pack's items, the server scripts run the recipe sweep, and every file
under `kubejs/data/` loads. That is what lets a test assert against the recipe the pack ships rather
than a fixture written to pass, and what `scripts/check-datapack-load.py` watches the game read. The same goes for `config/beltworks-server.toml`, `config/craftworks-server.toml`, and `config/factoryworks_core-server.toml`, linked in by `linkServerConfigs`: loaders need power (#447) only because the pack's configs say so, and every default is off. Two things follow from it. Terra's dimension type starts at y=0 (ADR-0019), below
vanilla's hard-coded test origin of y=-59, so `mixin/minecraft/GameTestServerMixin` places the tests
five blocks above the floor; without it no test block places and the run hangs rather than fails.
And KubeJS reads a Better Advanced Tooltips class on a server as well, so that jar is on the
classpath too. Oritech, Railcraft Reborn, Beltworks and FTB Materials are there because the pack's
recipes name their items.

What is there is `EnergyFaceTests` (#271), `BurnerFurnaceTests` (#432), `FurnaceOverloadTests` (#518), `HandSetTests` (#279),
`BoilerTests` (#274), `SteamChainTests` (#593), `RigBreakTests` (#310), `ElectricRigTests` (#194), `SteamEngineNetworkTests` (#292, #352), `AssemblingMachineTests` (#559), `AssemblingFluidTests` (#580), `AssemblerOilChainTests` (#644)
`PackChestTests` (#540), `FootprintBreakTests` (#352), `RadarTests` (#368), `PumpjackTests` (#377), `PipeDismantleTests` (#431) and `PipeStretchTests` (#452), all registered only when Oritech is loaded, `ReachTests` (#413), registered always but for its `Screens`, `SpawningRuleTests` (#480), `ChestTests` (#542), `WreckTests` (#544, #545, #546), and `BeltworksPackTests`, registered only when
Beltworks (`beltworks`) is loaded. The belt mechanics are Beltworks' own GameTests, in its repo
(#438). `ShowcaseSceneTests` (#538) build the `core/showcase/` scenes that `/factoryworks showcase` builds
for filming, under `factoryworks_showcase:*` so the run never selects them; swap the run's selector
for theirs to check each still makes its product. What is here is only what a JVM test cannot reach: that Craftworks plans every hand-craftable
recipe the server loaded, resolves a tag ingredient to its items and leaves out a fluid recipe and
one that is not hand-craftable (`HandSetTests`, ADR-0118); that a pole
placed first feeds an Electric Furnace placed after it within one rescan interval, that the furnace's
face, probed as a pole probes it — an insert inside a transaction it aborts — reports its whole buffer
as room and keeps no FE, and that a fed furnace smelts at 90 FE/t while a
starved one freezes where it stood; that an Electric Mining Drill's parts name its anchor as their
energy owner, that a pole reaching only those parts feeds it, and that it draws 45 FE/t, mines when fed
and freezes when starved (making the part its
own energy owner, returning false from `pay`, or dropping the journal each turn one red). How a pole
scans, counts and rations, and how power crosses a wire, stops beyond
reach and stops when the link is broken, are Wireworks' own GameTests (#476). And that a furnace on a pole reaching only a Steam Engine's
parts receives that engine's 450 FE every tick, once -- `SupplyScanTest` holds
the resolve-then-classify rule, and forcing every block to be its own owner turns the GameTest red.
And that a footprint machine, such as the Steam Engine (ADR-0116) or the Radar, broken at its anchor or at any part leaves none of its blocks standing and
drops exactly one item; dropping the part's teardown turns the part tests red. The Assemblers are Craftworks' (ADR-0118), and its GameTests hold the machine. `AssemblingMachineTests`
holds what the Pack owns of them: an Assembler on a creative pole's area is powered and crafts copper
cable in whole crafts of two; Fill Recipe answers `HELD` for a player who has unlocked nothing, since
the linked-in `craftworks-server.toml` names no Lock source (ADR-0126); and every `factoryworks:assembling/` recipe in the manager is one some tier can hold,
its fluids included. How many machines a pole counts is Wireworks', so the test asks only that the
Assembler crafts. It and `HandSetTests` call Craftworks' internals, the one exception to the
Consumer rule (`what-to-check.md`), until Craftworks names a Consumer API (5thlayer/craftworks#37).
Every other GameTest names a Library's blocks by registry id, through `LibraryBlocks`.
`AssemblingFluidTests` holds the Pack's claim over a Craftworks Fluid Connection and a Pipeworks pipe
(#580): a tier 2 Assembler holding `electric_engine_unit` (lubricant) or `concrete` (water), with a
pipe from its connection to a `LibraryBlocks.storageTank()` filled through the tank's own capability,
crafts the recipe on a creative pole's power, and tier 1 answers both recipes with anything but `HELD`
and holds nothing. The recipe is held before the pipe is placed, since a connection exists only while
the Held recipe has a fluid. The pull is Craftworks' and the segment Pipeworks', so the Pack moves no
fluid and the test asks only that the product comes out; a Pack-side fluid mover is the thing it must
never need.
And that a small pole's demand probe leaves a Beltworks loader no FE, that the loader's face reports
its buffer as room to a probe made by hand and keeps nothing from it, and that no `beltworks:` recipe
survives the stock-recipe sweep, against the pack's express belt recipe as a control
(`BeltworksPackTests`). The loader's face is Beltworks', so the two static FE checks cannot read it.
Rotate is Groundworks' (#451): the Pack only states that every block turns in place, and its
footprint machines refuse through the library's `TurnsInPlace`. The mechanism's tests are
Groundworks' and Beltworks', in their own runs.
And that the Pick takes up a span of Pipeworks' pipes, a **Dismantle Family** Groundworks runs
(`PipeDismantleTests`, #431, #448, ADR-0086): each test sneak-clicks a start through the player's
game mode, asks `Dismantles.spanTo` for the end, clicks it plainly, and holds the world, the
inventory and the stored start to the span. A straight run leaves none of the span's pipes standing,
keeps every pipe outside it and hands over a pipe each. Opposite points of a ring, a closed connection
and an end on a Boiler change no block, slot or stored start and name their reason, and an iron
pickaxe stores no start, since the Pack trims `groundworks:dismantles` to the Picks. A join rule that
ignores the pipes' links turns the closed-connection test red. The shortest path, a bend, a tee, one
block and the tie are Groundworks' `ShortestPathTest`, and a stale start and a sneak-use in the air
its `DismantlesTest` and `DismantleTests`. A full inventory dropping the rest at the player's feet and
creative handing over nothing are Groundworks' too, kept here until its own tests hold them
(5thlayer/groundworks#43). The red outline and that the Pick's plain click with no start still
toggles a connection are a human check on delivery.
And that Pipeworks' pipe is laid by Groundworks' Stretch (`PipeStretchTests`, #452,
`stretch/PipeworksPipeLegs`): a flat stretch and one raised 3, which stacks 3 at the start and runs
level after, lay exactly the plan, each pipe open to the next and no end open to the air, for one
pipe a block. A pipe already beside the leg is joined both ways, and a pipe's Raise reaches the
Pack's 16. The detour round a block and the refusal for too few items are Groundworks' `StretchTests`.
Dropping the leg's own pipes from the links the plan draws turns the laying tests red, and not asking Pipeworks for
the rest turns the joining test red. The pipes at an interior anchor are not joined yet (#467), and whether a
stretch with a rise and a detour previews as it lays is a human check on delivery.
And that a mining drill broken at its anchor or at any part, through the player's game mode, leaves
none of its blocks standing and drops exactly one drill item. 26.1 removes a block entity before
`affectNeighborsAfterRemoval`, so the part's teardown lives in `RigPartBlockEntity.preRemoveSideEffects`;
moving it back to the block turns both part tests red.
The Overload Limit's rule is `OverloadLimitTest`. `AssemblerOilChainTests` holds what the Pack owns of its chemistry
and oil recipes on Craftworks' Assembler (#644, ADR-0125), which is how they meet Pipeworks: with a creative
pole beside it, an Assembler 2 with a segment of petroleum gas on one connection and coal in its slot makes
plastic, and one with a segment of water on a connection of the edge it faces and sulfur and iron in its slots sends
sulfuric acid out of a connection of the opposite edge through a pipe to a storage tank. An Assembler 3 on advanced oil
processing, with water and crude from two tanks on its fourth and sixth connections, sends heavy oil, light
oil and petroleum gas out of the first three through pipes into three tanks, one fluid to a tank and in
whole crafts of 25, 45 and 55 mB. Craftworks 0.7.0 spaces the six connections, so no two pipes touch and the
test closes no pipe side. The connections have no direction and the Assembler pushes through them in
`FluidLayout.ASSEMBLER`'s order, so the products leave by the first three and the supplies stay full. Craftworks' own GameTests hold the Assembler, and none asserts on art
(ADR-0119). And that the screen's status (#332) is recomputed on
each ask, with no tick between, and names an empty buffer only once nothing earlier in the craft
cycle stops the machine; forcing the power probe true turns it red. The precedence is
`AssemblingStatusTest`, and the energy figures' split across 16-bit data slots `DataSlotHalvesTest`. A pole reaching only one hull block of a footprint machine still finds it: the hull blocks have
no block entity, so they resolve to the anchor through `EnergyOwnerBlock` in `SupplyAreaScan`, and
without it one machine counts as more than one. Each was checked against the defect it exists for: dropping
the furnace's `journal.updateSnapshots` call, restoring #266's `return 0`, and deleting the
furnace's `Capabilities.Energy.BLOCK` registration each turn two or three of them red.
`tests/pack/test_energy_faces.py` and `tests/pack/test_capability_registration.py` are the static
half and read source text, so they cannot see any of those three.

Two decisions are recorded rather than assumed. The platform generator has a `--check`, like every
other generator here, but **no test file owns it**: the template has no corpus, no tuning dial and
no input to go stale against, so the `--check` is the whole of the guard. And the GameTest run is
in no batch — this repo has no aggregate runner, and this is the one check that builds the mod and
boots a server, so it is run against a change that touched mechanism. Run it after editing
anything under `core/energy/`, `core/smelting/`, `core/fluid/`, `core/oil/`, `core/placement/`,
`core/reach/`, `core/dismantle/`, `core/stretch/`, `core/worldgen/` or `core/gametest/`.

## Wreck check

The wreck's blocks, the hull with its stairs and slab, the window and the cargo hold, have hardness -1 and no item
(ADR-0107, #544). The hold's slot count is Factorio's `crash-site-spaceship` `inventory_size`:
`scripts/factorio-container-extract.py` writes `data/factorio/container.json`, and
`scripts/build-wreck-assets.py` copies the row into the resource `CargoHoldCorpus` reads and writes
the blocks' blockstates, models and lang names. The **Debris** is three blocks, one per Factorio size
class, each breakable for nothing in its class's `mining_time` by hand: the extractor writes the
`crash-site-spaceship-wreck-*` rows, the generator copies each class's time into the resource
`DebrisCorpus` reads, and `DebrisCorpusTest` holds the hardness to Factorio's seconds on both Picks
(#550). `tests/pack/test_wreck_assets.py` runs its `--check`,
holds the copy to the corpus field by field, holds the corpus to the hopper screen's five slots and
holds the blocks to no item; `CargoHoldCorpusTest` is the parse. `gametest/WreckTests` holds a
survival player breaking each block through the game mode, the hold's face taking and giving on
every side on both overloads, its contents through the save hook, and the spawn on the wreck's
floor (#545). The hold is ten blocks, 5x2, and stores no offsets, since the template is rotated per
world (#548): the `anchor` boolean marks the one block with a block entity, and `HoldAnchor`, whose
walk is `HoldAnchorTest`, finds it from any part by a bounded flood fill. The Item face is
registered on the block, so every part answers with the anchor's inventory. `WreckTests` builds the
hold along x and along z and holds an insert through any part, on both overloads, to coming out of
any other, a hold with no anchor or two to answering nothing, one block entity in ten, and a
survival break of a part or the anchor to leaving it standing, and a survival break of each debris
block to removing it with no drop. Resolving a part to itself, giving
every block a block entity and registering the face on the anchor's type each turn tests red.
The blocks' textures are `scripts/build-wreck-textures.py`'s, from unused-textures' art under
`data/art/`, deriving the scorched hull and the window (#551); the same test runs its `--check`.
Run them after editing `core/wreck/` or either generator. How the blocks look is a human check on delivery.

## Recipe name check

The corpus holds no Wube text (ADR-0103, #303), so a chemistry or oil-processing recipe is named from what
`data/factorio/recipe.json` holds (#490). Factorio names a recipe after its main product unless it
is not one product under its own name -- advanced oil processing has three results, heavy oil
cracking one that is not its id -- and then it names the recipe itself. `scripts/build-recipe-names.py`
writes a `recipe.factoryworks.<type>.<name>` key for every emitted chemistry and oil
processing recipe: for such a recipe its id read as words (`heavy-oil-cracking` is "Heavy oil
cracking"), and `%s` for every other, which is filled with the product.
`tests/pack/test_recipe_names.py` runs the `--check` and re-derives both halves from the corpus,
holding the keys to the emitted recipes both ways. Run it after any converter run.

## Overload Limit check

The furnaces read the Overload Limit's constants (#517) from `data/factorio/overload.json`, never typed.
`scripts/build-overload-limit.py` copies them to `factoryworks_core/machine/overload.json`, which
`OverloadLimit` reads. Factorio's crafting machines are Craftworks', and carry their own figures.
`tests/pack/test_overload_limit.py` runs the `--check`; `OverloadLimitTest` holds the rule with typed
figures. Run them after re-extracting the corpus.

## Replace group check

Which blocks may Fast Replace which is Factorio's `fast_replaceable_group` (ADR-0082), never typed.
`scripts/factorio-machine-extract.py` writes it onto the machine and pole rows, and
`scripts/build-replace-groups.py` joins it onto `data/pack/item-map.json` into the resource
`ReplaceGroups` reads. A row that is `undecided`, `not_emitted` or `blocked_by`, or whose target is neither the pack's block nor Wireworks', is a recorded skip.
`tests/factorio/test_machine_extract.py` holds the groups against the dump when it is on disk.
`tests/pack/test_replace_groups.py` runs the generator's `--check` and holds the resource to its
own join of the two inputs. `ReplaceGroupsTest` covers the parse and the same-group rule. Run them
after re-extracting the corpus or editing the item map.

## Building tag check

What the player breaks at full Reach (16) rather than vanilla's 4.5 is the
`factoryworks:buildings` block tag (#413), never typed. `scripts/factorio-building-extract.py`
writes `data/factorio/building.json`, every Factorio item that places an entity, and
`scripts/build-building-tag.py` joins its Buildings onto `data/pack/item-map.json` by the block of
the target's own id. A missing, `undecided`, `not_emitted`, `native_mechanic` or `blocked_by` row,
and a target that places no block of its id, is a recorded skip. `tests/pack/test_building_tag.py` runs the `--check`,
traces each entry to a Building row, names the families the rule exists for and re-extracts when the
dump is on disk. `ReachTests` holds the rule in a world: through `handleBlockBreakAction`, stone 6
blocks off is refused with the block and inventory unchanged, a Stone Furnace 6 off and stone 4 off
break, and so does a burner drill's part 7 off, since a footprint's or a rig's part answers as its
anchor. Dropping the listener turns the first red, and dropping the part's resolution the last. Only
a break's start is refused, with vanilla's 1.0 of server lenience, so a start the client allowed is
never refused behind it. An Oritech machine core is not synced its controller, so the client refuses
one beyond 4.5. On the client a refused start is attacked as a miss (`mixin/minecraft/MinecraftMixin`),
swinging once the way vanilla does out of reach rather than cracking the block every tick; that is a
human check on delivery. Run them after re-extracting the corpus or editing the item map.

## Placement plan check

Placement is computed as a **plan** and executed separately (#297, ADR-0069): a `PlacementPlan` is
the positions a held item would fill, the blockstate at each, and a refusal or none. The preview
draws a plan and the click executes one, so the two cannot drift -- a preview that lies is worse
than none, because a player builds against it. The plan, its drawing and the vanilla plan
(deferring to `BlockPlaceContext` for facing, replaceable blocks and state survival) are the
Groundworks library's, which Beltworks bundles and the pack compiles against as it is nested in the
installed Beltworks jar (#446, #465). `Placements.planFor` is the one entry point, and every `factoryworks:` block, and every
other block with a facing, an axis or a rotation (`Oriented`, #450), is opted into the vanilla plan in
`FactoryWorksCore`; a door or bed draws one half, an accepted quirk. Only an item whose placement is *not* vanilla's implements
`PlansPlacement` -- the rig's footprint, the pump's dry site -- and its refusals are `PackRefusal`.
What the pack draws beside a plan is `placement/client/`, on the library's `PlacementPreviewEvent`:
the mining area as an `Overlay`, a family dismantle as a `Takeover`. A pole's column plan, its
supply area and its wires are Wireworks' (#476).

`gametest/PlacementPlanTests` is the check ADR-0069 asks for by name, and the only one that can
exist: ask each item for a plan, then use the block the way a player does, then hold the world to
what the plan promised. An accepted plan must have put **every** block down in the state it named; a
refused plan must have changed **nothing**, which is read before the gesture as well as after,
because "nothing changed" is not the same claim as "the positions are empty". Both halves are load-
bearing -- the preview's two failure modes are promising a placement that does not happen and
refusing one that does. Each test was checked against the defect it exists for: forcing the rig's
footprint to always fit turns one red, flattening the rig to a single layer turns two more, and
dropping the pump's water question turns one. A belt piece plans itself, and
Beltworks' GameTests hold it, and an Assembler's is Craftworks'. Two fixtures are load-bearing rather
than arbitrary -- the rig's size is compared against `RigGeometry`'s own footprint rather than a
floor, and the rig's obstruction sits a block *up*, where a player cannot see it. The geometry
underneath stays Minecraft-free (`RigGeometry`) and is unit-tested there.

A Fast Replace is a plan too (#388, ADR-0082): its `replaces` names the block it swaps out, and
`PlacementPlanTests.Replaces` asks for it, clicks, and holds the world, the new furnace's contents and
the inventory to it -- Stone to Steel and back, a burner to Electric and back with the fuel handed
over, the last held item's freed slot, and a full inventory refused with nothing changed and the
reason on the action bar. A sneak places beside, and a same-tier furnace or another group's block
replaces nothing. Skipping the handover or letting the inventory check pass turns three red. The
blue the preview draws a replace in is a human check on delivery.
A pole column's replace is Groundworks' Fast Replace through Wireworks' `PoleColumnReplace`, held by
Wireworks' own GameTests. The Pack states only the group (#476): `FactoryWorksCore` passes the
builder to `FastReplace.group` with the blocks `ReplaceGroups` puts in Factorio's `electric-pole`.
`PlacementPlanTests.PoleReplaces` holds that statement: a medium pole replaces a small column, and a
substation does not. Dropping the statement turns the first red.
An Assembler's Fast Replace is Craftworks' `AssemblerReplace` (ADR-0118), so the Pack holds none.

Run it after editing anything under `core/placement/`, and re-run `scripts/check-datapack-load.py`
too when the platform moves, since the same server reads it.

The platform grew from five blocks tall to seven so a column can reach `MAX_SEGMENTS`; re-run
`scripts/build-gametest-structures.py` if it moves again. Whether the preview **draws** correctly is
a human check on delivery -- no check here claims it, and the library draws on
`SubmitCustomGeometryEvent` rather than the `RenderLevelStageEvent` ADR-0069 names, because 26.1's
collector pipeline is reached through the former.

## Felling check

A tree is one entity holding an amount, and one gesture takes it whole (ADR-0051). Three checks,
none of which launches the game. `mod/src/test/java/com/factoryworks/core/felling/` is the rule:
`TreeShapeTest` is the fill over a block-position graph — it terminates on a ring of logs, respects
each of its three bounds, refuses a mid-trunk block, refuses a log cabin (no naturally-grown leaf),
never descends below the base, and does not cross into a touching canopy, which is vanilla's leaf
`distance` doing the work. `FellingCostTest` is the arithmetic: `amount × 0.1375s`, halved by
the Steel Pick, and a four-log tree costing Factorio's own 0.55s exactly — the rate is asked of
`TreeCorpus` rather than typed, because `0.5/4 = 0.125` is the *dead* trees' and the plants' rate and
#205 was written against it. `tests/factorio/test_tree_extract.py` re-derives the rate from the
corpus and names the three prototypes the discriminant must exclude, each of which yields a
different plausible-looking wrong number. `tests/factorio/test_pack_recipes.py` carries the
`fellable` tag: a `TagKey` whose JSON is missing resolves to an empty tag rather than an error —
every tree silently stops felling. Re-run `scripts/factorio-tree-extract.py` and then `scripts/build-tree-assets.py`
after a dump refresh; the second is the copy the mod reads. Whether a tree falls in a running game
is a world load.

## Starting kit check

The pocket `docs/spec/terra-progression.md` specifies is granted once per *player* by `core/start/`,
not once per join, and the hold is put in the wreck's cargo hold by `TerraStartingArea`'s stamp, once
per world (ADR-0107, #546). `gametest/WreckTests` finds the cargo hold the server stamped around the
level's spawn and holds it to exactly `StartingKit.HOLD`; removing the stamp's fill turns it red.
A grant that re-fires on login is an unlimited iron supply and would invalidate every pace reading
after the first relog (#203). Two static checks, neither of which launches the game. `tests/pack/test_starting_kit.py` asserts every granted id resolves — ours
against the tier enums that produce the registry paths, every foreign one against the installed jars
(none today), the hold against
`data/pack/item-map.json` — and that the pocket is the spec's pocket and the hold exactly the spec's
three items: an id that names nothing is a silent empty slot, and the moment the hold holds a green
circuit rung 0 has stopped being taught. The foreign loop filters by namespace rather than by id, so
it is the assertion that a borrowed id resolves at all and the next borrowed pocket entry has to
pass it too — which is what `gtceu:prospector.lv` stopped doing when GregTech left.
`mod/src/test/java/com/factoryworks/core/start/` is the once-per-player rule and the flag's
codec round trip, which are Minecraft-free because the kit names items by string. Run both after
editing `core/start/` or the spec's Opening. Whether the kit is in the inventory at spawn is a
world load.

## Fuel table check

What a burner furnace burns is generated datapack JSON, not Forge's burn table (ADR-0047).
`scripts/factorio-fuel-convert.py` joins `data/factorio/fuel.json` onto `data/pack/item-map.json`
into `kubejs/data/factoryworks/fuel/`, and nothing is decided in the script: a fuel with no
item-map row, an `undecided` one or a fluid is a *recorded skip*, printed with its reason.
`tests/factorio/test_fuel_convert.py` asserts every decided fuel has a row and nothing else does,
that `uranium-fuel-cell` fails on category as well as on its row, that coal's row still buys 888
whole ticks at the Stone Furnace's own 4,500 J/t, that `wood` arrives as the tag `minecraft:logs`,
and that the mod's listener reads the folder the converter writes. The arithmetic and the
default-deny rule are `FuelBufferTest` and `FuelTableTest` under
`./gradlew :factoryworks_core:test`. Run all three after re-extracting the corpus, editing the
item map or touching `core/smelting/`. Whether a furnace burns a log in a running game is a world
load. See `docs/testing/fuel-table-check.md`.

## Hand recipe check

The Personal Assembler is Craftworks, a local jar (ADR-0089), and its rules are tested in its repo.
It plans only a `craftworks:assembling` recipe whose `hand_craftable` is true, with no fluid and one
result, and the recipes the Assemblers hold are the same recipes (ADR-0118): the converter writes the
flag for a first Factorio category of `crafting`, as do the stock re-authoring, and the two Pick
recipes are written with it. `config/craftworks-server.toml` names no Lock source, so every recipe
is unlocked from the start (ADR-0126). `tests/factorio/test_hand_recipes.py`
re-derives the set from the corpus, holds every emitted recipe to it, and asserts the
`factoryworks:hand/*` copies, their generator and `withHandCopies` are gone. `gametest/HandSetTests`
holds that Craftworks plans every hand-craftable recipe the server loaded.

`tests/factorio/test_hand_resolver.py` is the corpus half: all 113 category-`crafting` recipes
resolve to plans bottoming out in the 21 known leaves, no item has two hand recipes (the resolver
picks a route with no cost model), and there are no cycles. It reads `data/factorio/recipe.json` and
fails the day a regeneration adds a recipe nothing hand-makes.

## Terra water fixture

`gametest/WorldgenFixtureTests` is the world-load fixture harness (#356), in the GameTest run's
default set. The GameTest world is flat, so each test decodes the datapack's own
`minecraft:dimension/overworld.json` and samples ±4096 at 128-block spacing, on three seeds, through
the biome source and `getBaseHeight`, without generating a chunk. It takes about a second. Each body
is a `WaterFixture` row, and a new body adds a row, not harness code. `WaterCensus` holds the
verdict and is unit-tested as `WaterCensusTest`. Terra's row asserts that water is 20–32% of the
map, that the Sea sits on the water and the water under the Sea (at least 90% each way), that the
Shore is at most 5%, that the shelf is 8–25% of the water and every other water column reaches the
bedrock band, and that no water lies within `TerraStartingArea`'s reach of the spawn search's
point. The terrain's thresholds in `scripts/build-terra-worldgen.py` are tuned against it, and the
test logs each seed's continentalness quantiles for that. Run it after editing that script, whose
`--check` asserts the generated files are current. Terra's pre-#356 noise and the old sea point
each turn all three tests red.

## Terra spawning check

Terra spawns no vanilla mob on its own (#480, ADR-0093). `tests/worldgen/test_terra_spawning.py`
runs `scripts/build-terra-worldgen.py --check`, then asserts every biome the live dimension and world
preset name has empty spawner lists and that the noise settings' `disable_mob_generation` is on.
`gametest/SpawningRuleTests` holds the other half: a new world starts with the spawn game rules
`core/worldgen/VanillaSpawning` turns off. The GameTest server turns `spawn_mobs` off itself, so
only the other three can fail there. Whether a night on Terra passes with no mob is a human check on
delivery. Run the static check after editing the generator, and the GameTest run after editing
`VanillaSpawning`.

## Starting-area geometry check

`tests/worldgen/test_start_geometry.py` asserts Terra's starting area can actually deal all four
ore fields: every hub connector sits on the face it points out of, and no two fields overlap each
other or the hub, for every hub variant against every combination of size variants. Vanilla drops
an overlapping jigsaw child silently, so this failure ships as "three patches instead of four" on
some seeds and nothing in a log. Run it after any edit to `scripts/build-terra-start.py`; it runs
the generator's `--check` and reads the generated `.nbt` files.

It also holds the wreck (ADR-0107, #545) at every hub's centre: hull floor and roof, the one
doorway on the template's +z long wall, windows on the -z and -x walls, the cargo hold in the +x
wall as a 5x2 centred along z with exactly one `anchor=true` block, the bottom middle one (#548),
and the pool beside the doorway and off its line. The sealed room is held on the generator's
undamaged hull, and the damage on the template is held to the -z half, the nose and the roof, never
the engine end, the +z wall, the doorway or the spawn; Factorio's debris is held to its count per
size class and kept off the wreck, the pool, the doorway's line, the hold's face and the connectors
(#550). `TerraStartingArea` reads none of that: it
puts the spawn point on the floor at the hub's centre facing template +z turned by the hub's
rotation, and the hub's processor list lays the wreck's box on one height. A doorway moved or a
level box that misses the wreck fails here; the stamp itself is a world. `PlayerSpawnFinderMixin`
returns that spawn point unscattered at its own height, and `WreckTests` holds `findSpawn` to a
roofed room's floor; removing the mixin turns it red. Waking inside, respawning inside and seeing
the fields from the doorway are a human check on delivery.

It is the only check standing behind the opening, and it cannot see the opening being *absent*:
the pools, the processor list and the hub's jigsaw names are referenced from
`TerraStartingArea` by string, with no compiler or test relationship to the datapack. #313 shipped
with those five files parked, which reached a new world as no hub, no water and no patches, and
one `No template pool` line at server start. No check was added for it (#313's own decision); the
symptom is a new world.

## Ore amount checks

An ore block carries an amount and a break draws one unit (ADR-0041). That is four checks, none of
which launches the game: `amounts.json` is hand-owned data (#600), so no check compares it to a corpus;
`mod/src/test/java/com/factoryworks/core/ore/` asserts a block pays out exactly what it holds
and that an exhausted position retires its delta, since a delta left behind is inherited by the next
block placed there; `tests/pack/test_ore_assets.py` asserts a blockstate variant per stage,
that every ore block is in `c:ores`, and resolves every drop against the installed jars, since an
id nothing registers pays air rather than throwing (#321);
`MiningSpeedTest` asserts a field costs its *amount* times the tier's seconds rather than its
block count; and `OutfieldAmountTest` asserts an outfield disc's uniform amount, read at its centre's
distance from origin with no cap. Run
them after editing anything under `core/ore/` or the two ore generators. See `docs/testing/ore-amount-check.md`.

## Outfield disc check

Every patch beyond the starting area is a surface disc placed by worldgen (#320, ADR-0045): one
`factoryworks:outfield_disc` structure and one `random_spread` structure set per resource,
uranium included (#321). The structure and structure-set JSON under `kubejs/data/factoryworks/worldgen/`
is hand-owned (#600) and no static check holds it; `OutfieldDiscTests` resolves each set from a
running server. The footprint is `OutfieldShapeTest`, Minecraft-free:
each column is asked for its own biome, because vanilla asks only at the centre and a coastal disc
would run onto the seabed, and the column mask is saved with the piece so placement never
recomputes it.

`gametest/OutfieldDiscTests` is the world half, in the GameTest run's default set. For each
resource it resolves the structure set out of the server's registry by id, generates a disc about
2,300 blocks out with a tree on its centre, and places it chunk by chunk the way `/place` does. It
also records the start in its chunk, which `/place` skips. Then it asserts every column holds
exactly the shape's ore, one deep and flush with the terrain, and that the centre's ore is under the
trunk. It asserts that the placed count is the piece's stored count, that the reach fits the law's
radius for the disc's size and distance, and that nothing reaches the pack's saved data. Breaking a
block reads `amountPerBlock` back through the structure manager, which is the only check that
reaches #319's read path. A second test per resource generates every chunk within 150 blocks of the
origin and gets no start, and a third generates against the structure's own biome predicate on the
flat world's plains and gets none. An off-by-one count, placing on the heightmap instead of walking down to
the ground, a second block below, the wrong resource's ore, a doubled radius, a density fade from 0
instead of from 150, and a read path that skips the piece each turn their test red. The GameTest
world is all plains, so a column-by-column confinement is `OutfieldShapeTest`'s alone. Whether discs
land on real terrain across Terra's biomes is a world load. Run both after editing `core/ore/`, the
disc's structure or piece, or the generator.

## ADR back-links

An ADR that contradicts a closed ticket's stated answer declares it as `supersedes: [55, 62]` in
frontmatter, and each named ticket gets a comment containing the literal `ADR-00NN`. Tickets are the
route and the ADRs are the state; without the back-link a closed ticket keeps asserting an answer an
ADR has overridden. Run `scripts/adr-backlink-check.sh` after committing an ADR that declares the
key — it needs an authenticated `gh`, so it is not part of any offline check. See
`docs/agents/domain.md`.

## Item-map ticket check

An `undecided` item-map row is a recorded skip only while the ticket it names is open, and a
`blocked_by` only while its blocker is (#278). A closed one leaves the converter skipping the row
for good behind a pointer that looks live. `scripts/item-map-ticket-check.sh` fails every row whose
`ticket` or `blocked_by` names a closed or missing issue, with that issue's title. When a ticket
closes, each row naming it is rewritten to a target, made `not_emitted` or `native_mechanic`, or
pointed at a new open ticket -- never at the reopened old one. The same command checks
`docs/factorio-mechanics.md` (#379): a `planned` or `blocked` section must name at least one open
issue in its `ticket` field, which is prose keeping closed refs as history; `owner` and the inline
sub-rule verdicts are not read. A failing section is re-verdicted or pointed at a new open ticket
the same way. It needs an authenticated `gh`, so run it after closing a ticket or editing either
file; it is in no batch.

## Obtainable index check

EMI's index lists only **Obtainable** items and fluids, as an allowlist (#173, ADR-0088).
`scripts/jar-registry-extract.py` writes to `data/jars/` every item and fluid id the client jar and
`mods/` register, every block loot table reduced to its entries and conditions, and
every placed and configured feature reduced to the block states it places and the features it names.
The pack's own jar is excluded. Its `--check` re-extracts and diffs when the jars are on disk, so a
jar update arrives as a diff to review. `scripts/build-obtainable-index.py` reads only committed
files and writes `kubejs/assets/emi/index/stacks/obtainable.json`: a `filters` entry matching every
id, then `added` naming every emitted recipe's output, every starting-kit item, every row of
`data/pack/mechanic-obtainable.json` and every drop of a block the live worldgen places. EMI reads
the file only under the `emi` namespace, applies `filters` before `added`, and skips an `added`
entry that is a bare string, so each is a `{"stack": ...}` object. A stack with components is
listed by its `componentChanges`, since EMI hides a variant not listed. A mechanic row is
`{id, mechanic, why, owner}`, for what a mechanic produces with no recipe and no data source, and
its `owner` is the ADR that makes it permanent.

The worldgen walk (#454) starts at each dimension under `kubejs/data/`, never `kubejs/parked/`: the
noise settings' default block and fluid and its surface rule, each biome's features followed from
placed to configured and on through the features they name, and the palettes of the live template
pools' templates, which are the starting area's. A feature type whose blocks are not all in its
config is in the generator's `IMPLICIT`, and a walked type in neither it nor `DATA_DRIVEN` fails
the run. Drops resolve to a fixpoint: a `match_tool` condition passes only when an Obtainable item
satisfies it, and any other tool predicate, silk touch included, is satisfied by nothing. So a grass
block drops dirt and not itself. A loot table under `kubejs/data/` replaces the jar's: every plant
the live worldgen places, leaves included, has an empty one, and gravel drops no flint (ADR-0092).
Each drop's block and loot table are written to
`kubejs/assets/factoryworks/obtainable/sources.json` for EMI's Where it is found (ADR-0091).

`tests/pack/test_obtainable_index.py` runs both `--check`s, holds every listed stack to an id the
corpus or the pack registers, and fails a mechanic row naming nothing, naming no ADR, or one the derivation already
covers. It holds the drops to #454's named ids and to terrain and logs alone, so a plant new to the
live worldgen fails until its loot table is replaced. It holds the loot rule to three cases, and
asserts that no block only a parked body places is a source. No mob drop is derived, since no mob spawns (ADR-0093). Run it after a
jar update, a converter run, an edit to the live worldgen, or an edit to the kit or the mechanic
list. Whether EMI shows exactly the allowlist is a human check: F3+T on a running client.

`data/pack/creative-listed.json` (ADR-0105, #539) adds the Pack's own creative test items, `{id, why}`
and `factoryworks:` only, to the emitted index and to nothing else: they are not Obtainable, so no
derivation reads them and `test_obtainable_index.py` fails a recipe that takes one, a row naming no
registered item, and a row already Obtainable.

## Independence guard

The Pack depends on no third-party content mod (ADR-0109), and `tests/pack/test_independence_guard.py`
is the ratchet that holds the removal slices of #566 to it. For each namespace in
`data/pack/independence-baseline.json`'s `forbidden` list (`railcraft`, `oritech`, `ftbmaterials`,
`researchd`, `portingdeadlibs`) it counts references across shipped data and compares the count to
that file's `baseline`. Static; no game launch. `researchd` and `portingdeadlibs` stay forbidden
though both mods are gone (ADR-0126), so neither grows back; Craftworks' generated comment in
`config/craftworks-server.toml` still names its `researchd` Lock source.

Each namespace has two counts, each with its own baseline: the data count below, and a Java count
(`java_baseline`).

The data count is the sum of three things. Each `<ns>:` occurrence in a text file under `kubejs/` (not
`kubejs/parked/`, which is never loaded), `mod/src/main/resources/`, `config/`, `data/pack/*.json`
(not the baseline file) and `mods/*.pw.toml`, and in `index.toml`. Each of those files whose path,
lowercased with `-` and `_` removed, contains the namespace, so `mods/ftb-materials.pw.toml` and
`config/oritech-common.toml` count once each. Each `index.toml` `file = "..."` line whose path matches
the same way. `data/jars/` is an extract of the installed jars, not shipped data, and is never read. Only
tracked files count, since the game writes untracked client configs that would make the count
differ between checkouts.

The Java count covers `mod/src/main/java` and `mod/src/test/java`. It is the sum of every `import` or
`import static` line of the namespace's package root (`rearth.oritech` for `oritech`,
`com.portingdeadmods.researchd` for `researchd`, `com.portingdeadmods.portingdeadlibs` for
`portingdeadlibs`) and every `"<ns>:` string id. `railcraft` and `ftbmaterials` have no package root,
since the Pack has no Java against them, so they count string ids only. The roots are `JAVA_PACKAGES` in
the guard.

A count, data or Java, above its baseline fails: a new reference to a mod the Pack is leaving. A count below it
fails too, naming the number to lower the baseline to, so a slice that removes references records
the gain in the same commit and nothing can later grow back into the headroom. Run it after
removing a third-party content mod's references, or adding anything that names one.

## Transfer-face check

`tests/pack/test_transfer_guards.py` asserts every item and fluid face in the mod is reachable by
both of the transfer API's overloads. NeoForge states `insert` and `extract` twice -- once naming a
slot, once meaning "anywhere it fits" -- and `DelegatingResourceHandler` forwards the second pair
straight to its delegate, so a subclass that refuses a slot is simply not consulted by a caller
that does not name one. Every face here is a refusal (the furnace, the Boiler and the rig refuse
extraction from what they are burning), so all of them are built on
`core/transfer/GuardedResourceHandler`, which overrides both slot-less methods to loop back
through itself. The check is the rule that `DelegatingResourceHandler` is named once, inside the
guard. It is a source-text check because NeoForge is deliberately off the unit-test classpath.
Whether a pipe actually respects the refusal is a world load.

## Capability registration check

`tests/pack/test_capability_registration.py` is the other half of the transfer-face check (#265):
that half asserts a face which *is* registered refuses correctly on both overloads, and this one
asserts the face exists at all. A machine whose registration is missing is not broken, it is
**inert** — it places, ticks and renders, and no pipe, funnel or pole ever reaches it, with nothing
thrown and nothing logged. Two seams make that reachable: a block entity type is declared in
`BLOCK_ENTITIES.register` and its faces in `registerCapabilities`, with no compiler relationship
between them, and both event handlers are reached only by an `addListener` line in
`FactoryWorksCore` — dropping that one line makes every machine in the mod inert at once.
`FACES` and `ITEM_FACES` are the recorded tables of which type gets which faces, listed rather
than discovered so that a new machine fails here instead of being answered "none"; a type that
genuinely wants no face records an empty tuple, which is then a decision somebody wrote down.
`ITEM_FACES` is asserted by *counting* `event.registerItem` calls rather than by matching their
shape, because the next one will be spelled differently and a shape-matching regex would let it
past — which is the failure the table exists to catch. Three assertions are the pack's own rather
than generic plumbing — each of
the **ladders** registers for every tier off its own
enum (a loop over fewer ships the remaining tiers inert, and the face assertion cannot see it
because the spelling is still there), the rig's **parts** answer as well as its anchor (which
corner holds the anchor is not visible, so a hopper under the wrong one finds nothing), and the
Barrel's face is on the **item**, in `PFItems`, where no block-side assertion reaches.
#265's own criterion — that the faces are the Transfer API's rather than the legacy system's — is
carried by the positive half, each recorded face asserted to be spelled `Capabilities.<Kind>.BLOCK`.
The legacy spellings are asserted absent too, but that half is a marker rather than a guard and
says so: none of those names resolves on this NeoForge and GregTech's jars left with ADR-0060, so
the compiler catches a backslide first. Source-text for the reason the guard check is:
`RegisterCapabilitiesEvent` is a NeoForge type and the test source set has no NeoForge on it by
design. Whether a pipe placed against a Boiler moves steam is a world load.

## FE face check

`tests/pack/test_energy_faces.py` is one layer in from the capability-registration check: that one
asserts the furnace *has* an `Energy` face, and this one asserts the face does
anything when a pole inserts into it. The failure shipped (#266) — the furnace's `insert` was
`return 0`, carried over from the EU buffer where refusing insertion kept a GregTech cable and
ADR-0036's pole from meeting at one block. With FE the pack's one currency there is no second route
to refuse, and the line only meant the Electric Furnace could never be powered by the one thing
built to power it, with nothing thrown and nothing logged. The other half is the snapshot: the pole
measures a machine's room with an insert it then **aborts**, so a face that takes energy without
journalling keeps a probe's worth every tick and runs on power nobody spent.
`core/energy/LongSnapshotJournal` is where that rule is spelled, and it is asserted to be spelled
once, the way `GuardedResourceHandler` is. Source-text for the same reason: `SnapshotJournal` and
`TransactionContext` are NeoForge types and the test source set has no NeoForge by design. The
arithmetic under the faces is `FurnaceEnergyBufferTest` and `EnergyLedgerTest`. Whether a pole
placed beside an Electric Furnace lights it is a world load.

## Offshore Pump check

`tests/pack/test_pump_assets.py` asserts the one block water enters the factory through (#213,
ADR-0050). `scripts/build-pump-assets.py` copies `data/factorio/machine.json`'s `pumps` row into a
resource the mod reads at class-init, the way `build-rig-assets.py` feeds `RigCorpus`, and the check
asserts that copy **field by field against the corpus** rather than against literals — a
hand-edited resource would run the pump at a rate somebody chose with nothing else failing. It also
holds the seam neither the corpus check nor the asset hops can see: that `pumping_speed` is still
Factorio's 20, so ADR-0050's "one pump feeds twenty boilers" has not quietly changed meaning; that
the item-map row names the block now that it exists; and that the **refusal message** has a lang key,
read out of `OffshorePumpItem` rather than typed, because a missing one renders the raw key on the
very gesture the message exists to explain.

The rule itself is Minecraft-free and lives under `mod/src/test/java/com/factoryworks/core/fluid/`:
`OffshorePumpSitingTest` is the predicate — one adjacent source, flowing refused, no minimum size —
and `OffshorePumpSpecTest` the two tick rates, which are the easiest thing here to get wrong, since
`pumping_speed` is stated per *Factorio* tick and its value happens to be Minecraft's tick rate.
`PumpCorpusTest` closes the loop by parsing the generated resource. Whether a pump placed against
the hub pool actually feeds a pipe is a world load.

## Boiler check

Terra's Boiler is the burner model's third customer (#224, ADR-0048): fuel and water in,
low-temperature steam out. Two checks, neither of which launches the game.
`mod/src/test/java/com/factoryworks/core/fluid/` holds the arithmetic and the stall —
`BoilerSpecTest` is the rate, and every figure in it is reachable by a wrong route that looks
right: the rise is paid for at **steam's** 0.2 kJ and water's is ten times larger (6 mB/s instead
of 60), and `energy_consumption` is per *second* against a buffer drained per tick. `BoilerCycleTest`
is the stall #224 names as mattering as much as the rate — a full steam tank makes no steam, burns
no fuel and, because water and room are asked *before* the fuel buffer is, lights no item either;
a boiler quietly eating coal into a full tank is a leak with no symptom.
`tests/pack/test_boiler_assets.py` is the pack side: every `facing` and the gauge's lang keys, that `boiler`'s
item-map row is `authored` and names the block the mod registers rather than the LP Solid Boiler it
replaces, and a **second, independent derivation** of the 60 mB/s straight from the corpus. Run both
after editing `core/fluid/`, `scripts/build-steam-assets.py` or the corpus. Whether a placed Boiler
boils water is the third check, `gametest/BoilerTests` (#274, #593): that a Boiler with water in
its front row, fuel and room in its steam segment makes 3 mB a tick and spends the same water
doing it -- unit for unit, since Factorio's boiler is a temperature change and not a reaction;
that the item face takes fuel and hands nothing back; that the three front blocks are one 600 mB
water segment, the back middle a separate 200 mB steam segment and the back corners in none (a
water row sharing the steam port's segment would launder water through a machine that consumes it);
and that no block answers a fluid capability. The rate is typed rather than read from
`BoilerSpec`, the way `EnergyFaceTests`' furnace demand is -- reading it off the spec would make
the test agree with the spec by construction. `gametest/SteamChainTests` (#593) is the chain on
Pipeworks: a pump, a pipe, the Boiler, two pipes and an Engine make power; water passes through one
Boiler's front row to a second; and a pipe that would join the steam to the water row waits outside
both segments and moves neither.

## Fluid colour check

Core registers the oil and chemistry fluids (ADR-0109, #619): crude, heavy oil, light oil, petroleum gas,
lubricant and sulfuric acid. Crude is drawn from malcolmriley's unused-textures sprite under a tint typed in
`OilFluidClient` (#557); the other five from Oritech's sprites under the tint in
`factoryworks_core/fluid/tints.json`. A fluid's colour is its sprite times a tint, so
`test_fluid_tints.py` holds both to Factorio's `base_color`. `scripts/build-fluid-tints.py` writes the
tint file from `base_color` (`data/factorio/fluid.json`) and each sprite's average, read from the Oritech jar.
It keeps the tint Oritech draws the sprite with where that lands within 0.15 of Factorio's colour, and
computes one where it does not. The sprite and Oritech's tint per fluid were read off the jar with `javap`
and are the one typed table. `tests/pack/test_fluid_tints.py` runs the `--check`, recomputes each fluid's
drawn colour against Factorio's, asserts `OilFluidClient` names the sprite the table does and that Core
registers each fluid with its flowing form and block, and that no recipe, tag, item-map row or index
names an `oritech:still_*` fluid. `FluidTintCorpusTest` covers the parse. Run both after editing a fluid
row in the item map, re-extracting the corpus, or updating Oritech. Whether the colours read right in a
running client is a human check on delivery.

## Steam Engine check

The Steam Engine is a Core block entity (#282, #594, ADR-0062, ADR-0116), placed and broken as one
footprint (#352). The HUD's status precedence is `SteamEngineStatusTest`. `SteamEngineSpecTest` under
`./gradlew :factoryworks_core:test` is the arithmetic, read from `SteamChainCorpus`: one engine burns
30 mB/s and makes 450 FE/t **over whole ticks** -- a segment moves whole millibuckets and 1.5 mB/t
floors to 1, so the spec carries the fraction -- and the port and buffer are 200 mB and 450 FE. A
buffer of one tick's output would floor a pole-drained engine to whole 300 FE millibuckets (300 FE/t),
so the burn keeps the millibucket that starts inside the room and carries its overshoot as energy
(#292). `SteamEngineSpecTest` also holds a row on one segment: fed, each engine makes 450 FE/t;
starved, the row burns what it is fed and no engine passes its rate. `SteamEngineNetworkTests` is
that a pole draws an engine through a part, once; that the engine stands in a segment by its anchor
alone, makes nothing from water and fills its buffer from steam; that its charge and carried
fractions survive a save; and that a row's touching anchors are one segment, each engine drawing its
own 450 FE/t when fed and none passing it when starved. Run the spec test after editing `core/fluid/SteamEngineSpec`.

## Blockbench model check

`tests/pack/test_model_assets.py` runs `scripts/build-model-assets.py --check`, which regenerates every
model and texture from `data/art/models/*/*.bbmodel` and compares byte for byte (ADR-0112). It exists
because nobody exports from Blockbench: a hand-exported or hand-edited model is stale here. The same
run refuses what the exporter cannot see:

- a Blockbench format other than 5.2.1's "5.0", a model format other than `java_block`, or a Java
  block version other than 1.21.11;
- an embedded texture or a saved reference image, since whatever a `.bbmodel` saves ships
  (ADR-0103);
- an absolute path, a path outside `data/art/models/`, or a file or folder name the game cannot
  load, and a model not named after its folder;
- a cube off the 1/16 grid, inflated, beyond -16..32, or rotated off one axis's 22.5° steps up to 45°;
- a model path that exists but carries no `credit` naming this generator, so a hand-made model is
  never overwritten; a generated model whose `.bbmodel` is gone; and a stray file in a generated
  texture folder.

ADR-0111's art rules, 16 px per block and a status light on every model, are checked by eye on
delivery, not here. The test exports the committed template as a machine, asserting the three status
children name the kit's lamps, and breaks each rule once on a copy to prove the generator names it. The template is held
to the rules but never exported. A whole texture folder left by a deleted machine is not caught. Run it
after editing a `.bbmodel`, anything under `data/art/models/`, or the generator.

## Radar check

The Radar (#368, ADR-0079) is a 3x3x3 on the footprint seam that charts one 32-block sector per
10 MJ into its owner's FTB team's chart. `tests/factorio/test_machine_extract.py` holds the `radars`
row against the dump when it is on disk and re-derives 33.3 s per sector;
`tests/pack/test_radar_assets.py` runs `scripts/build-radar-assets.py --check` and holds the mod's
resource against the corpus and each ore's patch-marker lang key (#370). The rules are Minecraft-free under
`mod/src/test/java/com/factoryworks/core/radar/`: the draw, the 10 MJ sector and the 250 kJ
nearby pulse counted from the same draw (`RadarSpecTest`, `RadarEnergyTest`), the 9x9 nearby area
and the long range's clockwise rings, unexplored first (`RadarSweepTest`), the 3x3x3
(`RadarFootprintTest`) and the chart's round trip (`RadarChartsTest`). `gametest/RadarTests` is
the world half: a pole-fed Radar has charted exactly its 9x9 by tick 100, nothing more at 640, and
the fifth ring's top-left sector by 720, and a starved one charts nothing; dropping the pulse turns
it red; its placement and break are in `PlacementPlanTests` and
`FootprintBreakTests`. Without FTB Teams on the classpath, as in the GameTest run, a player is their
own team. What each player's map is sent (#369) is `ChartDeliveryTest`: at login, on joining a team
and on entering a dimension, exactly the team's sectors that map lacks, a sector at a time, and
nothing twice. Which outfield patches a charted sector marks, by the disc's centre and never a
starting field, is `SectorPatchesTest`, and which markers each player is sent -- every one of the
team's chart and of their own walking that their client lacks, again after a logout since the client
holds them in memory -- is `MarkerDeliveryTest` (#370), and the walked record `WalkedPatchesTest`.
The same test holds the amount a marker carries: sent again only when a chart, re-scan or walk finds
it changed, and once as a removal to every map holding it when the patch runs out, never to a map
that did not (#371). What a patch has left is `PatchLedgerTest`, its hover text `PatchAmountTest`, and
`gametest/OutfieldDiscTests`' `outfield_last_block_removes_its_marker` holds the world half: a
charted disc sends its total, breaking all but one block and re-scanning sends one block's worth,
and mining the last block out sends exactly one removal and nothing before it. The drawing calls FTB Chunks' internal `ChunkUpdateTask`, compiled against 26.1.2.8
by name, and the GameTest run has no FTB Chunks, so it is also the check that the Radar charts
without it. Whether the terrain and the patch icons appear on the big map and minimap is a human check on
delivery.
Run these after editing `core/radar/` or the generator.

## Crude oil check

Crude is infinite (#377, ADR-0081): an **oil well** holds an amount, a Pumpjack on it yields
`10 × amount / normal` a cycle and takes 10 off it, down to the higher of 20% yield and 20% of the
well's start. Crude's figures sit in `amounts.json` beside the ores, as hand-owned data (#600). The derivations are Minecraft-free under
`mod/src/test/java/com/factoryworks/core/oil/`: `WellYieldTest` (yield, the 1,000 cap, the floor,
the carried fraction), `OilFieldTest` (1/96 of the mask, 3 apart, ore columns turned away, the
amount), and `PumpjackEnergyTest` and `PumpjackSpecTest` (45 FE/t, a 1.5 FE/t drain paid idle, a
cycle per 900 FE). `scripts/build-pumpjack-assets.py` copies the `pumpjack` drill row and the item
map's crude fluid into the mod's resource, and `tests/pack/test_pumpjack_assets.py` runs its
`--check`, holds the resource against both, and asserts Oritech's two `oil_spring` biome modifiers
are overridden with a no-op -- NeoForge 26.1 has `none` for structure modifiers only. The field's
structure set is hand-owned under `kubejs/data/factoryworks/worldgen/`. `gametest/OilFieldTests` places a field 2,300 blocks
out and holds its wells to their drawn amounts, spacing and ground, with an iron disc on the same
centre turning away exactly the wells on its columns; `PumpjackTests` holds a fed Pumpjack to 10 mB a
cycle a second into its Pipeworks segment, reached by a pipe on any face and filling a storage tank
through three pipes (#557, ADR-0110), and a starved one to nothing. Disabling the ore check or the
well refusal turns its test red. Whether the scaled Pump model reads well and the oil-field icons
appear on the FTB map is a human check on delivery. Run these after editing `core/oil/`, the oil
field's structure or piece, or the pumpjack generator.

## Enemy corpus check

`tests/factorio/test_enemy_extract.py` holds the eighth extractor's output — the units,
nests, turrets, walls, map-settings coefficients and per-entity emission rates ADR-0055 is
argued in. It **re-derives** rather than trusts, the way `test_resource_extract.py` does:
the evolution factor is stepped through Factorio's own published update and checked against
the closed form of the same differential equation, so a hand-edited `time_factor` fails here
and nowhere in a running game; a nest's absorption is compared against the Boiler's own
emission rather than against a literal; and each unit's `damage_per_shot` is recomputed from
its `damages` and its `damage_modifier`. Four prototypes are the walk's controls, each of
which yields a different plausible-looking wrong number: a premature wriggler's
`source_effects` hold a *negative* damage the attacker pays itself, a small spitter's damage
is 1 in a `stream` prototype and 12 in the game, a laser turret's is in a `beam` prototype
and reads as none if the reference is not followed, and a gun turret genuinely has none
because a magazine decides it — and the magazines are extracted too, so the turrets that
state no damage still have one in the corpus. Run it after re-running `scripts/factorio-enemy-extract.py`.
Nothing consumes this corpus yet; ADR-0055's arithmetic is filed against later tickets.

## Factorio mechanic ledger

`docs/factorio-mechanics.md` is the tracked list of every Factorio mechanic — base game and Space
Age — and what the pack does about it: one of `planned`, `shipped`, `adapted`, `blocked`,
`excluded`, never `undecided`. Read it before deciding a mechanic is out of scope, and update the
rows a ticket touches; a mechanic dropped without a row is exactly the failure it exists to catch.
It is not derived from `data/pack/subgroup-owner.json` and does not derive it — `not_emitted` there
is never evidence for `excluded` here — and it places nothing on a progression ladder, which is
#25's call. Row keys are Factorio's names by declared exception (ADR-0028).

## Recipe conversion

`scripts/factorio-recipe-convert.py` turns the extracted corpus into `craftworks:assembling` (ADR-0118) and `factoryworks:smelting` recipe JSON under `kubejs/data/factoryworks/recipe/` (#279), reading five committed data files: the corpus, the category
map, the subgroup owners, `data/pack/item-map.json` and `data/pack/recipe-overrides.json`. Nothing is
decided in the script — a decision is a diff to a design document. Generated output is never
hand-edited; re-run the converter. A Factorio name with no item-map row is a hard failure, while an
`undecided` row is a recorded skip. So is a row carrying `blocked_by`, the ticket that makes its
target loadable, such as a machine #277 has not chosen, and a machine
whose `recipe_type` is still null (the Centrifuge and the Rocket Silo). `--awaited` prints
those deferred recipes by the id they will load under, which is how the duplication check tells a
deferral from a typo.
`tests/factorio/test_recipe_convert.py` is the static check and runs the converter's `--check`; the
recipe *shape* is `scripts/check-datapack-load.py`'s. See
`docs/testing/recipe-conversion-check.md`.

## 26.1 data-format check

`tests/pack/test_data_formats.py` is the check kind #273 exists to add. Every generator here has a
`--check` that re-runs the generator and diffs its own output — self-consistent by construction and
blind to a shape Minecraft rejects — and every asset-hop check walks blockstate to model to texture
without asking whether the game reads any of them. This one asserts the shape **the game parses**,
against no generator, over every live tree at once, so a format landing in a subtree nobody thought
about still fails here. It holds four shapes: that every `models/item/X.json` has an
`assets/<ns>/items/X.json` pointing at it (26.1 resolves an item's model through that definition,
and a missing one is the black-and-magenta missing model in inventory, hand and EMI with nothing in
any log — the pack shipped the port with fifteen item models and zero definitions), that no
definition is an orphan, that every ingredient in every live recipe is a string rather than 1.21.1's
object, and that no namespace holds a pre-1.21.2 plural directory (`loot_tables/`, `tags/items/`),
which the game does not walk at all. `kubejs:oil_refinery` is a recorded deferral, not a silent skip:
it is dead with ADR-0060 and re-derived against the chassis #486 builds. A sized ingredient
(`{"ingredient": ..., "count": n}`) is read through to the string inside it.

`scripts/build-item-definitions.py` is the definitions' single owner — one generator rather than a
line in each asset generator, because a definition is not a decision about the Boiler or the rig but
the same three fields mechanically derived from the model beside it, and half the pack's item models
are hand-written with no generator to add the line to. An item wanting a tint, a range dispatch or a
condition stops being this script's and becomes its subject generator's. Run its `--check` (the test
does) after adding any item model. Whether the emitted files actually load is a datapack-load run
with zero `Couldn't parse data file` lines; nothing reads the log today, and that is the rest of
#273.

`scripts/check-datapack-load.py` is that in-world half, and the only check here that reads a log.
It runs the GameTest server — which loads the pack's whole `kubejs/` through KubeJS — and asserts
the game did not reject any of it. A rejected file is one ERROR
line at load and then an entry absent from its manager, which is the exact shape of #266's evening.
It is also the only check that can see whether the **ids inside** a file name anything: it found
`gcyr:mercury_rock`, a perfectly shaped stromatolite drop, naming an item whose mod left with
ADR-0060 and passing every static check in the repo. Every rejection the log may hold is in
`EXPECTED` with the ticket that owns it, and an entry that stops appearing fails too — a stale one
is a guard nobody re-armed. `--rerun-tasks` is not optional: Gradle would otherwise call the run up
to date, print no log, and the check would pass having loaded nothing. Like the GameTest run it is
in no batch; run it after a converter change, after editing the dev runtime classpath, or after any
edit to `kubejs/`.

## Pack check restore

`tests/pack/test_pack_check.py` runs `scripts/pack-check.sh` in a scratch repo with a stand-in
`packwiz` whose refresh rewrites the manifest and adds a metafile (#625). A failed check must leave
the manifest exactly as it was before the run: unstaged, staged and untracked edits a sync left are
kept, the staged diff is unchanged, and the metafile the refresh wrote is gone. It exists because
the restore once ran `git checkout` over `mods/`, wiping a sync that had not been committed.
It also asserts that a jar `data/pack/local-jars.json` pins is not STRAY without a metafile, since
the sync leaves a pending CurseForge reference that way, while an unpinned jar still is.
Run it after editing `scripts/pack-check.sh`.

## Sync CurseForge references

`tests/pack/test_sync_curseforge.py` runs `scripts/sync-local-jars.py` over a scratch `~/.m2` and
`mods/`, with stand-ins for CurseForge's listing, `packwiz` and Gradle. A listed file gets its
metafile. An unlisted or unreachable one still pins and installs the jar, removes the older
metafile and is reported pending. Plain `--check` passes on a pending row and `--check --strict`
fails. A later plain sync fills the reference in, and a metafile that already names the pin is not
queried again. FactoryWorks Core's metafile follows `mod_version` the same way, and fails `--check`
when it names an older Core or hashes another jar than `~/.m2`'s. It exists so the Pack can take and
test a Library released to `~/.m2` before the jar is uploaded, without ever exporting an older
CurseForge file than its pin, or an older Core than the last released. Run it after editing
`scripts/sync-local-jars.py`.

## Load-time codec check

`tests/pack/test_load_codecs.py` is the other direction of the 26.1 format work (#273): the data
checks assert the shape of the files the pack *emits*, and this one asserts the codecs in the jar
that *read* them. The trap is `ItemStack.CODEC`, which in 26.1 is
`Item.CODEC_WITH_BOUND_COMPONENTS`, and an item's components are bound during the same datapack
load that reads the recipes -- so a stack decoded there fails with `Item ... does not have
components yet`, one ERROR line, and the file is then absent from its manager. The emitted JSON is
identical either way, which is why `test_data_formats.py` and `test_smelting_shape.py` are blind to
it by construction, and a KubeJS `/reload` rebinds first, so it is a bug that exists only on a
clean world load. It shipped once, on all four smelts, and #271's GameTest server found it.
The rule is swept both ways: no class carrying a load-time registration (`RecipeSerializer`,
`SimpleJsonResourceReloadListener`, `AddServerReloadListenersEvent`) may name a stack codec, and
every other use is an `ALLOWED` entry with its reason -- one today, Jade's tooltip transport, which
runs on a live server long after binding. A stale entry fails too. Source-text for the reason
`test_smelting_type.py` is: the test source set has no Minecraft, so `ItemStackTemplate` is not
nameable from a JVM test. Run it after adding a reload listener, a recipe serializer or any codec
that decodes an item.

## Emitted smelt shape check

`tests/factorio/test_smelting_shape.py` asserts the four emitted `factoryworks:smelting`
recipes are shaped the way **26.1** parses an ingredient: a string, `#`-prefixed for a tag, where
1.21.1 took `{"item": ...}`. The old shape does not crash — it is one `Couldn't parse data file`
line at datapack load and the recipe is then absent from the manager, which reaches a player as a
furnace that holds the item, holds power and never smelts. It shipped that way through the port and
cost #266's in-world check. `test_recipe_convert.py` could not see it: it runs the converter's
`--check`, which re-runs the converter and compares the output to what the converter would emit —
self-consistent by construction and blind to a shape Minecraft rejects. The assembling recipes'
shape is `test_data_formats.py`'s and `check-datapack-load.py`'s. Run it after any
converter change. A KubeJS reload is enough to see the fix in a running game — no restart.

## Stock-recipe sweep

`kubejs/server_scripts/recipes.js` removes every recipe the pack does not admit by name, and
`recipe_survivors.js` is the allowlist it negates (ADR-0034: a stock recipe ships only if a
decision names it and names the surface it is crafted on). A survivor is a *surface*, not a
recipe, and its filter's recipe type must be the one `data/pack/category-map.json` registers for
that machine — so a machine landing later without a survivor entry fails
`tests/factorio/test_recipe_sweep.py` rather than having its recipes swept in silence. Run that
check after editing either script, the category map or the emitted recipes; whether the sweep
removed the right things in a running game is a world load, not a static check.

## Recipe duplication check

`tests/factorio/test_recipe_duplication.py` asserts no item is made by two emitted recipes unless
`MULTI_ROUTE` names it and says what the second route earns. Every other recipe check owns one
subtree and one input table, which is the right shape for "did this converter do its job" and blind
to the question none of them can ask: whether two converters, or one converter twice, made the same
item. Two routes to one block fails no schema, appears in no log and loads perfectly — it reaches
the player as two EMI entries for the same thing, and if both are `hand_craftable` the
Personal Assembler's resolver has no cost model to choose between them. It shipped once, when
Create's two gearbox conversions and the large cogwheel's second route were emitted alongside the
direct recipes they duplicate and every subtree-local check passed. One item legitimately has a
second route: solid fuel, which Factorio makes from each of its three oils, and that is a row
with its reason, all three of whose routes are `factoryworks:chemistry` recipes (#488). The file-path
invariant it used to hold existed because GregTech re-registered every GTRecipe under its type's
path (#87); the pack's own types are re-registered by nothing, and the rule left with GregTech (#279).

Run it after any converter change. It does not assert the routes are balanced; costing is a
decision.

## Hand-written recipe check

`kubejs/data/factoryworks/recipe/assembling/pack/` is the one subtree no converter generates: ADR-0039's
two Engineer's Pick recipes, which the corpus can never author because Factorio has no mining-tool
prototype. `tests/factorio/test_pack_recipes.py` is what holds them, since every other recipe here
is checked against the corpus and these are checked against nothing otherwise — that the
converter still lists `pack` as foreign, which its own check reads from it rather than restating (a
run that forgets deletes them, and the sweep leaves no stock pickaxe to fall back on), that both land on a surface
`recipe_survivors.js` admits and carry `category: crafting` and `hand_craftable` so the Personal Assembler
plans them at rung 0, that the steel recipe consumes the iron pick, and that each registered tier
has its model, texture, lang key, `c:tools/wrench` and `groundworks:dismantles`, the two tags that
carry its verbs, that `groundworks:dismantles` holds nothing else (#448), and the block tag the jar asks for by name. Both sprites are vanilla's own — the Iron Pick's `iron_pickaxe` and
the Steel Pick's `netherite_pickaxe` (#241, applied on #323). The Steel Pick used to wear GTCEu's
Damascus Steel pickaxe, flattened by a generator because GT's tool art is three greyscale layers
that only become a material under a colour handler our item never reaches; GregTech left with
ADR-0060 and took the source with it, so `scripts/build-pick-textures.py` and its `--check` are
gone rather than restated. The tier list is read out of `PickTier.java`. The pick's arithmetic —
that Factorio's seconds survive Minecraft's break-time formula — is `MiningSpeedTest` under
`./gradlew :factoryworks_core:test`. Whether the Pick mines every block class is a world load. See
`docs/testing/hand-written-recipe-check.md`.

## Stock recipe re-authoring check

A stock recipe the pack keeps is re-authored, never admitted as shipped (ADR-0034's tail).
`scripts/stock-recipe-convert.py` reads each recipe `data/pack/stock-admissions.json` admits out of
the installed jar, flattens a shaped pattern with every count kept, swaps each ingredient through
`data/pack/stock-substitutions.json` and writes a hand recipe under
`kubejs/data/factoryworks/recipe/assembling/stock/`. An ingredient in neither table, a table row
nothing reads, and a recipe no jar or two jars ship each fail the line (#442). An admission can
carry a `rewrite` instead of being flattened: its ingredients, all `keep` rows, and its yield,
chosen and recorded with a reason, and only the output is read from the jar (#444).
An `author` row is a recipe no jar ships, written whole from its row on the machine it names;
sand is ground on an Assembler and smelted to glass under `recipe/smelting/stock/` (#445).
`tests/factorio/test_stock_recipes.py` runs the `--check`, holds each ingredient to an item another
pack recipe makes or a `keep` row, each output to an item a jar defines, each recipe to the
machine's input slots, and the union of every emitted hand recipe to no cycle; a second hand route
is `test_recipe_duplication.py`'s. It also holds the wooden stairs to one per species Terra's biomes
grow, read out of the biome files, and the subtree to no wall. Run it after editing either file or
after a jar update. Whether the filter appears in EMI with a route to follow is a human check on
delivery.

## Item map check

`tests/pack/test_item_map.py` holds ADR-0109: the Pack owns every material form, and a tech
mod supplies machines. It asserts every `data/pack/item-map.json` target resolves against the
installed jars (the pack's own via its lang and KubeJS's `event.create`, vanilla via the client jar
when present), that no row names a mod ADR-0060 removed, that the eight material-form rows are
authored `factoryworks:` items, and that no emitted recipe or item tag names a `c:` tag more than one installed jar
populates -- with AlmostUnified gone, `#c:ingots/steel` accepts three items and is not a decision.
A row whose target cannot resolve yet sits in `DEFERRED` with the ticket that owns it, and must
carry `blocked_by` with that ticket so the converter emits nothing naming it; a stale entry
fails, so delete one as its row resolves. Run it after editing the item map or re-running a converter.

## Factorio tech tree

The Showcase has no research (ADR-0126), but Terra's arc is measured against Factorio's tech tree,
extracted rather than transcribed (ADR-0022). `data/factorio/technology.json` and
`science_packs.json` are the committed reference. Regeneration and provenance are in
`data/factorio/README.md`. `tests/factorio/test_tech_extract.py` asserts the pruned tree is still a
valid tree: the extractor drops the infinite, formula-costed and upgrade technologies, re-points a
dropped node's children to its nearest surviving ancestors, and collapses generated reverse-crafts,
and the committed tree has no duplicate, dangling or self-referential prerequisite and every
technology is reachable from a root. It also reads the gate table in
`docs/spec/terra-progression.md` and holds each gate to its cost in the corpus, the launch to no
production pack and the reactor to a branch the silo does not require (ADR-0097). Run it after
re-extracting or after editing that table.

## Licence check

Which licence covers which file is `REUSE.toml`'s, with the texts in `LICENSES/` (#302,
ADR-0102): code LGPL-3.0-only, the Pack's content CC BY 4.0, Wube's corpus under neither, and
third-party files under their own. No file carries an SPDX header. `tests/pack/test_licensing.py`
implements `reuse lint`'s rule, since that tool needs libmagic and CI runs it, and holds the
boundaries a glob edit can silently move: the corpus never under the Pack's licences, each art
credit in `NOTICE` resolving to the licence `NOTICE` names, the mod declaring what the map
gives it, and `.packwizignore` leaving the licence texts in the upload. The core jar bundles them too. Run it after adding a file of a new kind, any third-party art, or an edit to
`REUSE.toml`.

## Art provenance check

`data/pack/art-provenance.json` has one row per shipped texture, model and animation, saying whose it
is (#564, ADR-0122): `drawn`; `vendored`, with its `source` and `licence`; `stand-in`, with a
`subkind` of `placeholder`, `procgen` or `ai` and its `generator`; or `unknown`, for what no
`REUSE.toml` entry, `NOTICE` credit or generator has yet classified. `tests/pack/test_art_provenance.py`
holds the manifest to the files. What counts as shipped is `scripts/art_provenance.py`'s `SHIPPED`: every
file under a `textures/`, `models/` or `sounds/` folder of `kubejs/assets/*/` and the mod's
`assets/*/` (so each `.png.mcmeta` animation and each model JSON), the images and `.bbmodel`
sources under `data/art/`, and the images and clips under `publish/`. Blockstates, item
definitions and lang only point at art and have no row. The check fails on:

- a shipped asset with no row, including one not yet `git add`ed;
- a row whose file is missing, or is not a shipped asset;
- a malformed row: a kind outside the four, a field the kind does not take, a `vendored` row without
  `source` and `licence`, a `stand-in` row without a `subkind` and `generator`, a `procgen` or
  `placeholder` generator that is not a file in the repo (an `ai` one names the model), a path that is
  duplicated or out of order.

It also runs those rules on made-up rows, so a rule that stopped firing fails here instead of passing
vacuously. It does not compare a row to `REUSE.toml` or `NOTICE`: a `vendored` row's `licence` is the
row's own claim, and `test_licensing.py` still holds the credits.

The manifest is kept by hand, one row per line, sorted by path, and its `note` is free text. A row's
kind is the decision, so changing it is a diff a reviewer reads. Add the row in the commit that adds the
file; a file nobody has looked at is `unknown`. When a vendored asset is replaced, its row goes with
it (ADR-0122: `vendored` stays only to list what is left to replace).

`scripts/art-worklist.py` prints what is left: the stand-ins by subkind and generator, every vendored
row by licence with the non-commercial ones first, and the `unknown` rows, after a count line for all
four kinds. It exits 1 after printing if the manifest and the files disagree. Run the check after
adding, renaming or deleting any texture, model, animation, store image or clip, or a `.bbmodel`.

## Coined-name check

No string a player reads names a coined Factorio term (#304, ADR-0103). `COINED_TERMS` in
`tests/pack/test_licensing.py` is a recorded deny-list, term to reason, matched as whole words,
case-insensitive, plural included; a term that matches nothing is the passing state, so nothing
there goes stale. It scans every value of every shipped lang file (`kubejs/assets/*/lang/` and the
mod's), skipping keys that start with `_`; the literal argument of each `.displayName(...)` in
`kubejs/startup_scripts/`, with comments ignored. Registry ids and lang keys are exempt, and
`kubejs/parked/` and `publish/` are out of scope. A source that yields no strings fails, so a
scanner that stops matching cannot pass by finding nothing. Run it after adding a lang entry, a
display name or a quest.

## Upload check

`python3 -m unittest discover scripts/tests` drives `scripts/upload.py` as a release does, a
version, the environment and a maven repository holding Core's jar, against a stand-in server on
localhost (`scripts/tests/standin.py`), and checks only what reaches it and the exit status: the jar
byte for byte, the changelog section, the release type and `upload_release_type`, Core's projects and
required dependencies, a missing token filled through `op run` and never printed, each site on its
own, and a version a site already has refused. Both scripts and the tests are copies of
5thlayer/libworks' template, kept level with it by skillworks' `template-drift`; a fix lands in the
template too. Run it after editing either script; it is in no batch.
