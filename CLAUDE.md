## Agent skills

### Issue tracker

Issues live in this repo's GitHub Issues, managed with the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical triage roles, used verbatim as label strings. See `docs/agents/triage-labels.md`.

### Rejected scope

`.out-of-scope/` holds one file per rejected enhancement, so a `wontfix` keeps its reasoning and a
repeat request is recognised rather than re-argued. `/triage` reads it while gathering context. Only
rejected enhancements go there — never bugs, never something already built, never a deferral. A
Factorio mechanic the pack does not reproduce belongs in `docs/factorio-mechanics.md` instead, which
distinguishes `excluded` from `blocked`; a decision with a considered alternative belongs in an ADR.
See `.out-of-scope/README.md`.

### Domain docs

Single-context — `CONTEXT.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

### Testing policy

Which check a feature warrants — and whether it warrants one at all — is decided by the claim the
feature makes, not ad hoc per ticket. Six claims, six answers, and a content ticket names its check
kind explicitly so that "no check" is a recorded decision. See `docs/testing/what-to-check.md`.

### Code comments

This overrides "match the surrounding comment density". Much existing code is over-commented, so
do not copy it.

A comment explains why the code is not the obvious code: a trap, a constraint from outside the
code, or a choice a reader would otherwise undo. Write the minimum that explains it, then name the
ADR or ticket that holds the reasoning, e.g. `(ADR-0074)`. Code that is obvious gets no comment.

Keep these out of comments:

- History, such as which ticket built something, what it replaced, or what was tried before. It
  belongs in the commit and the ADR.
- Rejected alternatives. They belong in the ADR.
- How a test was verified, such as "dropping X turns this red". It belongs in the commit.
- A restatement of what the next lines do.

When you edit code, trim the comments you touch to this rule. Leave comments elsewhere alone.

### Running the Python checks

`uv run --with pytest pytest tests/` runs **every** check under `tests/`, and is the gesture to
reach for. Most files here are `main()`-style scripts rather than pytest tests, and pytest collects
nothing from them on its own: `tests/conftest.py` wraps each one as a single test that runs it and
asserts it exited 0, and fails the run if any `test_*.py` produced no tests at all (#171). Before
that shim the same command reported green while 25 of 36 files never executed, which is why the
count guard is there rather than the convention being left to memory. A single script can still be
run directly — `uv run tests/pack/test_rig_assets.py` — and prints its own line.

The GameTest harness, `scripts/check-datapack-load.py` and `scripts/check-client-assets.py` are in
no batch and are not reached by this command; each says above when to run it.

### Checks the 26.1.2 move broke

The move to 26.1.2 (ADR-0060) took GregTech and GCyR out and parked every body but Terra under
`kubejs/parked/`. No check fails for that reason any more.

`tests/flora/test_flora_data.py` was the last one. It now reads Sapros's worldgen from
`kubejs/parked/` while it is parked, and from the live tree once #23 brings it back. One thing it
passes is not settled: a stromatolite's stone drop is still a `gcyr:` id, and GCyR has been removed.
Which stone Sapros drops belongs to #23.

Two more were cleared earlier (#323). `tests/factorio/test_pack_recipes.py` was
red on the Steel Pick's sprite; #241's decision applied, and both picks now wear vanilla art with no
generator. `tests/pack/test_starting_kit.py` was red on `gtceu:prospector.lv`; ADR-0056 had already
ruled the prospector was never canon and ADR-0045 put every ore patch on the surface, so the pocket
drops it and the charting gesture stays open on #116. A third, `tests/pack/test_furnace_assets.py`,
was red on the Electric Furnace's GTCEu textures until #324 gave it art of its own.

`planetaryfactory_core` itself compiles again as of #268, and what that cost is recorded in
`docs/port/blocked-removals-26.1.2.md`: every class deleted because GregTech left or because the
Researchd fork is still on 1.21.1, each with the ticket that owns restoring it (#260, then #262,
then #251). Read it before concluding a mechanic was dropped — the Minecraft-free rules and their
unit tests survived; only the glue that reads a running game went.
`data/pack/item-map.json` and `data/pack/subgroup-owner.json` still name Create, Power Grid and
GregTech targets. They are the conversion's input and are rewritten with it.

### Flora data check

`tests/flora/test_flora_data.py` asserts Sapros's tree and surface data are internally consistent
— features, loot tables, blockstates, textures and lang against what is actually registered, plus
which marshland carries which tree and that no stromatolite drops ore — with no game launch. Run it
after any edit to the trees, the stromatolites or the five biomes. The worldgen half is read from
`kubejs/parked/` while Sapros is parked (ADR-0060).

### Block asset check

`tests/pack/test_block_assets.py` walks every block the mod registers (#254): blockstate, model
parents and textures (the pack's, the installed jars' and the client jar's), lang key, item model
where the block has an item, and a loot table unless it is registered with `noLootTable`; a block
with an item of its own name must drop exactly it. The block list is parsed from every
`DeferredRegister.createBlocks` source and each ladder's names evaluated from its enum's
`blockName()`, so a new block is walked with no edit here, and a registration it cannot name fails.
Blocks with no item are the `NO_ITEM` patterns, each with its reason. Each machine's own
`test_*_assets.py` keeps only what a generic walker cannot judge.
Run it after adding a block or editing any blockstate, model, texture, lang key or loot table.

### Furnace ladder check

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
Minecraft-free unit tests under `mod/src/test/java/com/planetaryfactory/core/smelting/`: the
per-tier duration, the 90 FE/t draw and its buffer, the unsided routing by item, and the stall —
a blocked output starts no smelt, burns no fuel and voids nothing (ADR-0041). Whether the three
blocks smelt in a running game is `gametest/EnergyFaceTests` for the Electric tier (#271) and
`gametest/BurnerFurnaceTests` for the two burners (#432): on coal, each makes iron plate at its
tier's rate for 4,500 J a working tick, keeps the steel smelt's 5:1, and with a full output lights
no coal, spends no banked joule and starts no smelt. Dropping the output check, shrinking the input
by one, or a 4,000 J tick each turns both tiers' tests red.

### GameTest harness

`./gradlew :planetaryfactory_core:runGameTestServer` from the repo root is the pack's only check
that loads a world. It is headless, needs no display and no human, and fails the command when a
test fails. The tests are in `mod/src/main/java/com/planetaryfactory/core/gametest/`, in the
**main** source set — a GameTest is code the game loads, so it cannot live in the Minecraft-free
test source set. 26.1 has no `@GameTestHolder` and no `neoforge.enabledGameTestNamespaces`: a test
is an entry in the `test_instance` datapack registry, registered through NeoForge's
`RegisterGameTestsEvent`. Groundworks and Beltworks register tests too, some of them for cases the
Pack's settings rule out on purpose, so the run selects `--tests planetaryfactory:*` and each repo's
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
than a fixture written to pass, and what `scripts/check-datapack-load.py` watches the game read. The same goes for `config/beltworks-server.toml`, linked in by `linkBeltworksConfig`: loaders need power only because the pack's config says so, and Beltworks' default is off (#447). Two things follow from it. Terra's dimension type starts at y=0 (ADR-0019), below
vanilla's hard-coded test origin of y=-59, so `mixin/minecraft/GameTestServerMixin` places the tests
five blocks above the floor; without it no test block places and the run hangs rather than fails.
And KubeJS reads a Better Advanced Tooltips class on a server as well, so that jar is on the
classpath too. Oritech, Railcraft Reborn, Beltworks and FTB Materials are there because the pack's
recipes name their items.

What is there is `EnergyFaceTests` (#271), `BurnerFurnaceTests` (#432), `ElectricNetworkTests` (#280), `HandSetTests` (#279),
`BoilerTests` (#274), `RigBreakTests` (#310), `ElectricRigTests` (#194), `SteamEngineNetworkTests` (#292, #352), `AccumulatorTests` (#283), `AssemblingMachineTests` (#327), `AssemblingFluidTests` (#295)
`FootprintBreakTests` (#352), `RadarTests` (#368), `PumpjackTests` (#377), `PipeDismantleTests` (#431) and `PipeStretchTests` (#452), all registered only when Oritech is loaded, `ReachTests` (#413), registered always but for its `Screens`, `SpawningRuleTests` (#480), and `BeltworksPackTests`, registered only when
Beltworks (`beltworks`) is loaded. The belt mechanics are Beltworks' own GameTests, in its repo
(#438). What is here is only what a JVM test cannot reach: that `RuntimeHandRecipes` finds the pack's assembling recipes in
the server's recipe manager, resolves a tag ingredient to its items and leaves a fluid recipe out
(forcing the graph empty turns it red); that a pole's
scan finds an Electric Furnace at all, that the pole's demand probe — an insert inside a
transaction it aborts — leaves no FE behind, and that a fed furnace smelts at 90 FE/t while a
starved one freezes where it stood; that an Electric Mining Drill reached only through its part
blocks is one machine, draws 45 FE/t, mines when fed and freezes when starved (making the part its
own energy owner, returning false from `pay`, or dropping the journal each turn one red); and that power crosses a wire between linked poles, stops
beyond reach, and stops again when the link is broken. And that a pole reaching only a slave Steam Engine's
parts draws the whole row's 1,350 FE/t from the master, once, feeding neither -- `SupplyScanTest` holds
the resolve-then-classify rule, and forcing every block to be its own owner turns the GameTest red --
and that the row scan chains an engine whose part, not its anchor, stands in the row; skipping the
part-to-anchor step turns it red. And that either footprint machine, the Assembling Machine or the
Steam Engine (ADR-0077), broken at its anchor or at any part leaves none of its blocks standing and
drops exactly one item; dropping the part's teardown turns the six part tests red. And that an Assembling Machine's Held recipe survives its save hook and
resolves again after it, that every assembling recipe of tier 1's categories in the server's manager is
one Fill Recipe can set and it can hold, while a `crafting-with-fluid` one is refused (#331) -- and that changing the recipe hands the inputs back;
dropping the field from `saveAdditional` turns the first red. The codec itself is `HeldRecipeTest`. Every one is also held through `AssemblingMachineMenu.request`, the setter EMI's Fill Recipe lands on
(#330), and a non-assembling id or a locked recipe is refused there with a message and leaves the Held
recipe alone; making `HoldVerdict.of` always answer held turns that test red. The rule is `HoldVerdictTest`.
And that a small pole's demand probe leaves a Beltworks loader no FE, and that no `beltworks:` recipe
survives the stock-recipe sweep, against the pack's express belt recipe as a control
(`BeltworksPackTests`). The loader's face is Beltworks', so the two static FE checks cannot read it.
Rotate is Groundworks' (#451): the Pack only states that every block turns in place, and its
footprint machines refuse through the library's `TurnsInPlace`. The mechanism's tests are
Groundworks' and Beltworks', in their own runs.
And that the Pick takes up a span of Oritech's fluid pipes, a **Dismantle Family** Groundworks runs
(`PipeDismantleTests`, #431, #448, ADR-0086): each test sneak-clicks a start through the player's
game mode, asks `Dismantles.spanTo` for the end, clicks it plainly, and holds the world, the
inventory and the stored start to the span. A straight run, a bend, a tee's branch between the ends
and one pipe clicked twice leave none of the span's pipes standing, keep every pipe outside it and
hand over a pipe each; a full inventory drops the rest at the player's feet and creative hands over
nothing. Opposite points of a ring, a closed connection and an end on a Boiler change no block,
slot or stored start and name their reason, and an iron pickaxe stores no start, since the Pack
trims `groundworks:dismantles` to the Picks. A join rule that ignores Oritech's connections turns the closed-connection
test red. The shortest path, the tie and the default join are Groundworks' `ShortestPathTest`, and
the red outline and that the Pick's plain click with no start still toggles a connection are a
human check on delivery.
And that Oritech's fluid pipe is laid by Groundworks' Stretch (`PipeStretchTests`, #452,
`stretch/OritechPipeLegs`): a flat stretch and one raised 3, which stacks 3 at the start and runs
level after, lay exactly the plan, each pipe open to the next and no end open to the air, for one
pipe a block. A stone on the leg is gone round on the player's side, too few pipes refuse the stretch
whole, a pipe already beside the leg is joined both ways, and a pipe's Raise reaches the Pack's 16.
Dropping the leg's own pipes from the connection rule turns three red, and not asking Oritech's own
rule for the rest turns the joining test red. The pipes at an interior anchor are not joined yet (#467), and whether a
stretch with a rise and a detour previews as it lays is a human check on delivery.
And that a mining drill broken at its anchor or at any part, through the player's game mode, leaves
none of its blocks standing and drops exactly one drill item. 26.1 removes a block entity before
`affectNeighborsAfterRemoval`, so the part's teardown lives in `RigPartBlockEntity.preRemoveSideEffects`;
moving it back to the block turns both part tests red.
And that the machine crafts its Held recipe at Factorio's rate (20 ticks and exactly 750 FE for
copper cable, typed rather than read off `AssemblingMachineSpec`) and that a full output, no
ingredients and a locked recipe each draw nothing, take nothing and keep the recipe (#328); drawing
the tick's energy before the stall is asked turns all three stalls red. Nothing is locked until #260,
so the lock test uses `AssemblingMachineRecipes.lockForTest`, which locks one id and not a swapped
predicate. The batch's tests run side by side, so each locks a recipe no other test uses. And that
the item face takes the Held recipe's ingredients each in its own slot and refuses everything else
(#329, ADR-0074). That is asserted through the capability on the anchor and on a hull block, on both
overloads. A machine with no recipe takes nothing, and Oritech's `FILL_EVENLY` input mode, which
spreads a slot-0 insert past the filter, stays pinned off. Making `acceptsInput` return true turns
two of them red, and dropping the `cycleInputMode` override turns the third red. The slot rule is
`AssemblingInputSlotsTest`. The rate and the stall order are
`AssemblingMachineSpecTest` and `AssemblingStallTest`; whether an Oritech addon changes the rate in
a world is not checked -- the multipliers are, on the JVM. Tiers 2 and 3 are blocks of their own
(#295, ADR-0075): tier 2 crafts copper cable in 14 ticks for 1,000 FE, typed, and every tier keeps
its paint and the cartridge through a shift-click in the player's game mode (dropping `PaintLock`'s
listener turns all three red). `AssemblingFluidTests` holds tier 2's tank: concrete crafts from 150 mB
and leaves 50, 50 mB stalls with nothing drawn or taken, the fluid face on the anchor and a hull
block takes water only with concrete held and never gives it back, tier 2 holds concrete where tier
1 refuses it, tier 1 has no fluid face, a changed recipe voids the tank and the tank survives the
save hook. A `takeFluids` that always feeds and a change that keeps the tank turn four red. The
per-tier figures are `AssemblingMachineSpecTest`, the fluid stall's order `AssemblingStallTest`. And that the screen's status (#332) is recomputed on
each ask, with no tick between, and names an empty buffer only once nothing earlier in the craft
cycle stops the machine; forcing the power probe true turns it red. The precedence is
`AssemblingStatusTest`, and the energy figures' split across 16-bit data slots `DataSlotHalvesTest`. A pole beside a whole machine counts it
once and fills it, and a small pole reaching only a hull block still finds it: the hull blocks have
no block entity, so they resolve to the anchor through `EnergyOwnerBlock` in `SupplyAreaScan`, and
without it one machine counts as four. Each was checked against the defect it exists for: dropping
the furnace's `journal.updateSnapshots` call, restoring #266's `return 0`, and deleting the
furnace's `Capabilities.Energy.BLOCK` registration each turn two or three of them red; making no
two poles link, never rebuilding the network, and never dropping a broken pole each turn a network
test red.
`tests/pack/test_energy_faces.py` and `tests/pack/test_capability_registration.py` are the static
half and read source text, so they cannot see any of those three.

Two decisions are recorded rather than assumed. The platform generator has a `--check`, like every
other generator here, but **no test file owns it**: the template has no corpus, no tuning dial and
no input to go stale against, so the `--check` is the whole of the guard. And the GameTest run is
in no batch — this repo has no aggregate runner, and this is the one check that builds the mod and
boots a server, so it is run against a change that touched mechanism. Run it after editing
anything under `core/energy/`, `core/smelting/`, `core/fluid/`, `core/oil/`, `core/placement/`,
`core/reach/`, `core/dismantle/`, `core/stretch/`, `core/worldgen/` or `core/gametest/`.

### Replace group check

Which blocks may Fast Replace which is Factorio's `fast_replaceable_group` (ADR-0082), never typed.
`scripts/factorio-machine-extract.py` writes it onto the machine and pole rows, and
`scripts/build-replace-groups.py` joins it onto `data/pack/item-map.json` into the resource
`ReplaceGroups` reads. A row that is `undecided`, `not_emitted` or `blocked_by` is a recorded skip.
`tests/factorio/test_machine_extract.py` holds the groups against the dump when it is on disk.
`tests/pack/test_replace_groups.py` runs the generator's `--check` and holds the resource to its
own join of the two inputs. `ReplaceGroupsTest` covers the parse and the same-group rule. Run them
after re-extracting the corpus or editing the item map.

### Building tag check

What the player breaks at full Reach (16) rather than vanilla's 4.5 is the
`planetaryfactory:buildings` block tag (#413), never typed. `scripts/factorio-building-extract.py`
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
human check on delivery. `ReachTests.Screens`, registered only with Oritech loaded, holds a machine's
screen open 12 blocks off and closed 22 off, both the Assembling Machine's and Oritech's own through
the Steam Engine; each closed at Oritech's 8 before `OritechScreenHandlerMixin` and the menu's own
fix. Run them after re-extracting the corpus or editing the item map.

### Placement plan check

Placement is computed as a **plan** and executed separately (#297, ADR-0069): a `PlacementPlan` is
the positions a held item would fill, the blockstate at each, and a refusal or none. The preview
draws a plan and the click executes one, so the two cannot drift -- a preview that lies is worse
than none, because a player builds against it. The plan, its drawing and the vanilla plan
(deferring to `BlockPlaceContext` for facing, replaceable blocks and state survival) are the
Groundworks library's, which Beltworks bundles and the pack compiles against as it is nested in the
installed Beltworks jar (#446, #465). `Placements.planFor` is the one entry point, and every `planetaryfactory:` block, and every
other block with a facing, an axis or a rotation (`Oriented`, #450), is opted into the vanilla plan in
`PlanetaryFactoryCore`; a door or bed draws one half, an accepted quirk. Only an item whose placement is *not* vanilla's implements
`PlansPlacement` -- the pole's column, the rig's footprint, the pump's dry site -- and its refusals are
`PackRefusal`. What the pack draws beside a plan is `placement/client/`, on the library's
`PlacementPreviewEvent`: the supply area, mining area and wires as an `Overlay`, a family dismantle
as a `Takeover`.

`gametest/PlacementPlanTests` is the check ADR-0069 asks for by name, and the only one that can
exist: ask each item for a plan, then use the block the way a player does, then hold the world to
what the plan promised. An accepted plan must have put **every** block down in the state it named; a
refused plan must have changed **nothing**, which is read before the gesture as well as after,
because "nothing changed" is not the same claim as "the positions are empty". Both halves are load-
bearing -- the preview's two failure modes are promising a placement that does not happen and
refusing one that does. Each test was checked against the defect it exists for: forcing the pole
to the vanilla plan turns four red, forcing the rig's footprint to always fit turns one red,
flattening the rig to a single layer turns two more, dropping the pump's water question turns one,
and giving up on the other-group column walk turns another. A belt piece plans itself, and
Beltworks' GameTests hold it. The Assembling Machine's two (#326) are
the rig's pair for its 2x1x2 footprint (ADR-0072): dropping one block from its plan turns the first
red, and the second's obstruction sits in its upper row. The first also reads `ASSEMBLED` five ticks
after placing, because Oritech's next-tick rescan cleared it and a tick-0 read passed with that live. Three fixtures are load-bearing rather
than arbitrary -- the other-group column is three tall because on a one-tall column "the top of the
column" and "just above the block I hit" are the same block, the rig's size is compared against
`RigGeometry`'s own footprint rather than a floor, and the rig's obstruction sits a block *up*,
where a player cannot see it. The geometry underneath stays Minecraft-free (`RigGeometry`,
`PoleColumn`) and is unit-tested there.

A Fast Replace is a plan too (#388, ADR-0082): its `replaces` names the block it swaps out, and
`PlacementPlanTests.Replaces` asks for it, clicks, and holds the world, the new furnace's contents and
the inventory to it -- Stone to Steel and back, a burner to Electric and back with the fuel handed
over, the last held item's freed slot, and a full inventory refused with nothing changed and the
reason on the action bar. A sneak places beside, and a same-tier furnace or another group's block
replaces nothing. Skipping the handover or letting the inventory check pass turns three red. The
blue the preview draws a replace in is a human check on delivery.
`PlacementPlanTests.PoleReplaces` holds the pole column's (#389): a three-segment small column
clicked at its base, middle or top becomes a medium one of the same height, and back, keeping
exactly the wires touching it, for one item spent and one returned. A substation on a small column
and a small pole on a substation, and a full inventory, change nothing and name their reason. The
fixture stands a third pole wired past the neighbour, since a base wired afresh would take it first;
swapping with the block's placement and removal hooks turns both replaces red.
`PlacementPlanTests.AssemblingReplaces` holds the Assembling Machine's (#390), registered only with
Oritech loaded: tier 1 to 2 keeps copper cable Held, the items, the craft scaled to tier 2's 14 ticks
and the FE; tier 2 to 1 with concrete clears the recipe, voids the tank, drops the fluid face and
hands the inputs back; tier 2 to 3 keeps the tank; a hull block replaces as the anchor does; an Oritech
speed addon stays attached and in effect; and a full inventory changes nothing and names its reason.
One block entity type serves every tier, so the swap keeps it (`shouldChangedStateKeepBlockEntity`)
and sets no neighbour flag, since the anchor's removal hook tears the footprint down. Not keeping the
block entity turns five red, letting the inventory check pass turns the no-room test red, and a part
that does not hand the click to its anchor turns the hull test red.

Run it after editing anything under `core/placement/`, and re-run `scripts/check-datapack-load.py`
too when the platform moves, since the same server reads it.

The platform grew from five blocks tall to seven so a column can reach `MAX_SEGMENTS`; re-run
`scripts/build-gametest-structures.py` if it moves again. Whether the preview **draws** correctly is
a human check on delivery -- no check here claims it, and the library draws on
`SubmitCustomGeometryEvent` rather than the `RenderLevelStageEvent` ADR-0069 names, because 26.1's
collector pipeline is reached through the former.

### Felling check

A tree is one entity holding an amount, and one gesture takes it whole (ADR-0051). Three checks,
none of which launches the game. `mod/src/test/java/com/planetaryfactory/core/felling/` is the rule:
`TreeShapeTest` is the fill over a block-position graph — it terminates on a ring of logs, respects
each of its three bounds, refuses a mid-trunk block, refuses a log cabin (no naturally-grown leaf),
never descends below the base, and does not cross into a touching canopy, which is vanilla's leaf
`distance` doing the work. `FellingCostTest` is the arithmetic: `amount × 0.1375s`, halved by
research, and a four-log tree costing Factorio's own 0.55s exactly — the rate is asked of
`TreeCorpus` rather than typed, because `0.5/4 = 0.125` is the *dead* trees' and the plants' rate and
#205 was written against it. `tests/factorio/test_tree_extract.py` re-derives the rate from the
corpus and names the three prototypes the discriminant must exclude, each of which yields a
different plausible-looking wrong number. `tests/factorio/test_pack_recipes.py` carries the
`fellable` tag: a `TagKey` whose JSON is missing resolves to an empty tag rather than an error —
every tree silently stops felling. Re-run `scripts/factorio-tree-extract.py` and then `scripts/build-tree-assets.py`
after a dump refresh; the second is the copy the mod reads. Whether a tree falls in a running game
is a world load.

### Starting kit check

The pocket and the hold `docs/spec/terra-progression.md` specifies are granted once per *player*
by `core/start/`, not once per join — a grant that re-fires on login is an unlimited iron supply
and would invalidate every pace reading after the first relog (#203). Two checks, neither of which
launches the game. `tests/pack/test_starting_kit.py` asserts every granted id resolves — ours
against the tier enums that produce the registry paths, every foreign one against the installed jars
(the quest book is the only one today, since the prospector went with #323), the hold against
`data/pack/item-map.json` — and that the pocket is the spec's pocket and the hold exactly the spec's
three items: an id that names nothing is a silent empty slot, and the moment the hold holds a green
circuit rung 0 has stopped being taught. The foreign loop filters by namespace rather than by id, so
it is the assertion that a borrowed id resolves at all and the next borrowed pocket entry has to
pass it too — which is what `gtceu:prospector.lv` stopped doing when GregTech left.
`mod/src/test/java/com/planetaryfactory/core/start/` is the once-per-player rule and the flag's
codec round trip, which are Minecraft-free because the kit names items by string. Run both after
editing `core/start/` or the spec's Opening. Whether the kit is in the inventory at spawn is a
world load.

### Fuel table check

What a burner furnace burns is generated datapack JSON, not Forge's burn table (ADR-0047).
`scripts/factorio-fuel-convert.py` joins `data/factorio/fuel.json` onto `data/pack/item-map.json`
into `kubejs/data/planetaryfactory/fuel/`, and nothing is decided in the script: a fuel with no
item-map row, an `undecided` one or a fluid is a *recorded skip*, printed with its reason.
`tests/factorio/test_fuel_convert.py` asserts every decided fuel has a row and nothing else does,
that `uranium-fuel-cell` fails on category as well as on its row, that coal's row still buys 888
whole ticks at the Stone Furnace's own 4,500 J/t, that `wood` arrives as the tag `minecraft:logs`,
and that the mod's listener reads the folder the converter writes. The arithmetic and the
default-deny rule are `FuelBufferTest` and `FuelTableTest` under
`./gradlew :planetaryfactory_core:test`. Run all three after re-extracting the corpus, editing the
item map or touching `core/smelting/`. Whether a furnace burns a log in a running game is a world
load. See `docs/testing/fuel-table-check.md`.

### Assembler queue and resolver check

`mod/src/test/java/com/planetaryfactory/core/assembler/` asserts the Personal Assembler's queue:
Start takes the whole raw cost at once, a chain runs its steps in order and delivers only what no
remaining step needs, cancelling refunds the unspent reservation plus the intermediates already made,
a craft that will not fit pauses the head instead of dropping, and a paused head stops the plans
behind it. `AssemblerCodecsTest` is the data attachment's round trip, which ADR-0038 asks for by
name — a codec that drops a field does not crash, it returns a queue that silently emptied over a
logout. `PlanResolverTest` is the other half: chain-crafting, an intermediate already held being used
rather than remade, `Missing` against `Locked`, and `all` as the largest count the inventory covers.
`ItemKeyTest` is the identity itself (ADR-0052): an item is its registry id plus its data component
patch, encoded as one string, so an empty patch encodes to the bare id and every existing key is
unchanged, two differently-ordered patches encode identically, and matching is exact string equality
— a deliberate divergence from `neoforge:components`' subset match, without which the resolver would
have to compare `ItemStack`s and stop being a unit test. It is what lets Researchd's four science
packs, which are one item told apart by a component, be four items to the queue.
`PlanToQueueTest` is the seam between them, and the one neither side can assert alone — a plan the
resolver calls complete must be one the queue can run to the end, because a step the buffer cannot
feed throws *after* the reservation was taken. All five are
`./gradlew :planetaryfactory_core:test` with no game launch: the queue and the resolver name items by
string and the codec is DataFixerUpper's rather than Minecraft's, which is what keeps them checkable.

`tests/factorio/test_science_packs.py` is the emitted half of #222 — that both science pack recipes
are in the hand set and that every component-bearing output there is one the key format can name.
Whether the `RecipeGraph` admits them is a running server, and the absence of a refusal line naming
them is the signal.

`tests/factorio/test_hand_resolver.py` is the corpus half — that the *design* terminates. All 113
category-`crafting` recipes resolve to plans bottoming out in the 21 known leaves, no item has two
hand recipes (the resolver picks a route with no cost model), and there are no cycles. It reads
`data/factorio/recipe.json` and fails the day a regeneration adds a recipe nothing hand-makes.

`CraftButtonsTest` is the Crafting Plan's one rule (#287, ADR-0064): a press queues at once, so a lit
`+1`, `+5` or `all` is a promise the inventory covers it, and the ceiling is the resolver's
`largestAffordable` rather than anything the screen counts. `FillRequestTest` is EMI's Fill Recipe on
the Assembler's screen (#288, ADR-0065): left queues 1, right 5, Shift all, middle opens the plan, and
a request the ceiling does not cover queues nothing -- five never becomes three -- so the server opens
the plan instead. `CancelClickTest` is a click on a queue icon on the inventory screen
(#290, ADR-0066): left cancels 1, right 5, Shift all; a partial cancel re-resolves the rest of the row,
which `AssemblerQueueTest` asserts keeps its id, place and the craft under way's progress.

Run them after editing anything under `core/assembler/` or after re-extracting the corpus. Whether
each mouse button reaches the handler on the Assembler's screen, Fill Recipe is unchanged on every
other screen, and a plan delivers is a world load, not a static check.

### Terra water fixture

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

### Terra spawning check

Terra spawns no vanilla mob on its own (#480, ADR-0093). `tests/worldgen/test_terra_spawning.py`
runs `scripts/build-terra-worldgen.py --check`, then asserts every biome the live dimension and world
preset name has empty spawner lists and that the noise settings' `disable_mob_generation` is on.
`gametest/SpawningRuleTests` holds the other half: a new world starts with the spawn game rules
`core/worldgen/VanillaSpawning` turns off. The GameTest server turns `spawn_mobs` off itself, so
only the other three can fail there. Whether a night on Terra passes with no mob is a human check on
delivery. Run the static check after editing the generator, and the GameTest run after editing
`VanillaSpawning`.

### Starting-area geometry check

`tests/worldgen/test_start_geometry.py` asserts Terra's starting area can actually deal all four
ore fields: every hub connector sits on the face it points out of, and no two fields overlap each
other or the hub, for every hub variant against every combination of size variants. Vanilla drops
an overlapping jigsaw child silently, so this failure ships as "three patches instead of four" on
some seeds and nothing in a log. Run it after any edit to `scripts/build-terra-start.py`; it reads
the generated `.nbt` files, so it also catches forgetting to re-run the generator.

It is the only check standing behind the opening, and it cannot see the opening being *absent*:
the pools, the processor list and the hub's jigsaw names are referenced from
`TerraStartingArea` by string, with no compiler or test relationship to the datapack. #313 shipped
with those five files parked, which reached a new world as no hub, no water and no patches, and
one `No template pool` line at server start. No check was added for it (#313's own decision); the
symptom is a new world.

### Ore amount checks

An ore block carries an amount and a break draws one unit (ADR-0041). That is five checks, none of
which launches the game: `tests/factorio/test_resource_extract.py` re-derives every starting total
from Factorio's own committed formula rather than trusting the number;
`mod/src/test/java/com/planetaryfactory/core/ore/` asserts a block pays out exactly what it holds
and that an exhausted position retires its delta, since a delta left behind is inherited by the next
block placed there; `tests/pack/test_ore_assets.py` asserts a blockstate variant per stage,
that every ore block is in `c:ores`, and resolves every drop against the installed jars, since an
id nothing registers pays air rather than throwing (#321);
`MiningSpeedTest` asserts a field costs its *amount* times the tier's seconds rather than its
block count; and `OutfieldAmountTest` asserts an outfield disc's uniform amount, read at its centre's
distance from origin with no cap, against every row of the corpus's `outfield.law` table. Run
them after editing anything under `core/ore/`, the two ore generators or the extractor. See `docs/testing/ore-amount-check.md`.

### Outfield disc check

Every patch beyond the starting area is a surface disc placed by worldgen (#320, ADR-0045): one
`planetaryfactory:outfield_disc` structure and one `random_spread` structure set per resource,
uranium included (#321). `scripts/build-outfield-worldgen.py` writes them from
`data/factorio/resource.json`, and `tests/worldgen/test_outfield_worldgen.py` runs its `--check`
and re-derives the spacing from each resource's `mean_spacing` and the separation from
`spot_noise`'s minimum candidate spacing. It also asserts the type is the one `PFWorldgen`
registers and the biomes are the land tag. The footprint is `OutfieldShapeTest`, Minecraft-free:
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

### ADR back-links

An ADR that contradicts a closed ticket's stated answer declares it as `supersedes: [55, 62]` in
frontmatter, and each named ticket gets a comment containing the literal `ADR-00NN`. Tickets are the
route and the ADRs are the state; without the back-link a closed ticket keeps asserting an answer an
ADR has overridden. Run `scripts/adr-backlink-check.sh` after committing an ADR that declares the
key — it needs an authenticated `gh`, so it is not part of any offline check. See
`docs/agents/domain.md`.

### Item-map ticket check

An `undecided` item-map row is a recorded skip only while the ticket it names is open, and a
`blocked_by` only while its blocker is (#278). A closed one leaves the converter skipping the row
for good behind a pointer that looks live. `scripts/item-map-ticket-check.sh` fails every row whose
`ticket` or `blocked_by` names a closed or missing issue, with that issue's title. When a ticket
closes, each row naming it is rewritten to a target, made `not_emitted` or `native_mechanic`, or
pointed at a new open ticket -- never at the reopened old one. The same command checks
`docs/factorio-mechanics.md` (#379): a `planned` or `blocked` section must name at least one open
issue in its `ticket` field, which is prose keeping closed refs as history; `owner` and the inline
sub-rule verdicts are not read. A failing section is re-verdicted or pointed at a new open ticket
the same way. It also checks every row of `data/pack/mechanic-obtainable.json` (#453), which must
name an open `ticket`. It needs an authenticated `gh`, so run it after closing a ticket or editing
any of the three files; it is in no batch.

### Obtainable index check

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
entry that is a bare string, so each is a `{"stack": ...}` object. A mechanic row is
`{id, mechanic, why, ticket}`, for what a mechanic produces with no recipe and no data source.

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
`kubejs/assets/planetaryfactory/obtainable/sources.json` for EMI's Where it is found (ADR-0091).

`tests/pack/test_obtainable_index.py` runs both `--check`s, holds every listed stack to an id the
corpus or the pack registers, and fails a mechanic row naming nothing or one the derivation already
covers. It holds the drops to #454's named ids and to terrain and logs alone, so a plant new to the
live worldgen fails until its loot table is replaced. It holds the loot rule to three cases, and
asserts that no block only a parked body places is a source. No mob drop is derived, since no mob spawns (ADR-0093). Run it after a
jar update, a converter run, an edit to the live worldgen, or an edit to the kit or the mechanic
list. Whether EMI shows exactly the allowlist is a human check: F3+T on a running client.

### Transfer-face check

`tests/pack/test_transfer_guards.py` asserts every item and fluid face in the mod is reachable by
both of the transfer API's overloads. NeoForge states `insert` and `extract` twice -- once naming a
slot, once meaning "anywhere it fits" -- and `DelegatingResourceHandler` forwards the second pair
straight to its delegate, so a subclass that refuses a slot is simply not consulted by a caller
that does not name one. Every face here is a refusal (the furnace, the Boiler and the rig refuse
extraction from what they are burning; the pump refuses insertion; the Boiler's fluid face refuses
each direction on a different tank), so all five are built on
`core/transfer/GuardedResourceHandler`, which overrides both slot-less methods to loop back
through itself. The check is the rule that `DelegatingResourceHandler` is named once, inside the
guard. It is a source-text check because NeoForge is deliberately off the unit-test classpath.
Whether a pipe actually respects the refusal is a world load.

### Capability registration check

`tests/pack/test_capability_registration.py` is the other half of the transfer-face check (#265):
that half asserts a face which *is* registered refuses correctly on both overloads, and this one
asserts the face exists at all. A machine whose registration is missing is not broken, it is
**inert** — it places, ticks and renders, and no pipe, funnel or pole ever reaches it, with nothing
thrown and nothing logged. Two seams make that reachable: a block entity type is declared in
`BLOCK_ENTITIES.register` and its faces in `registerCapabilities`, with no compiler relationship
between them, and both event handlers are reached only by an `addListener` line in
`PlanetaryFactoryCore` — dropping that one line makes every machine in the mod inert at once.
`FACES` and `ITEM_FACES` are the recorded tables of which type gets which faces, listed rather
than discovered so that a new machine fails here instead of being answered "none"; a type that
genuinely wants no face records an empty tuple, which is then a decision somebody wrote down.
`ITEM_FACES` is asserted by *counting* `event.registerItem` calls rather than by matching their
shape, because the next one will be spelled differently and a shape-matching regex would let it
past — which is the failure the table exists to catch. Four assertions are the pack's own rather
than generic plumbing — the pole has **no** face (ADR-0062), so what is asserted instead is that
`ElectricNetworks::onLevelTick` is wired, the one line without which no pole moves energy; each of
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

### FE face check

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

### Offshore Pump check

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

The rule itself is Minecraft-free and lives under `mod/src/test/java/com/planetaryfactory/core/fluid/`:
`OffshorePumpSitingTest` is the predicate — one adjacent source, flowing refused, no minimum size —
and `OffshorePumpSpecTest` the two tick rates, which are the easiest thing here to get wrong, since
`pumping_speed` is stated per *Factorio* tick and its value happens to be Minecraft's tick rate.
`PumpCorpusTest` closes the loop by parsing the generated resource. Whether a pump placed against
the hub pool actually feeds a pipe is a world load.

### Boiler check

Terra's Boiler is the burner model's third customer (#224, ADR-0048): fuel and water in,
low-temperature steam out. Two checks, neither of which launches the game.
`mod/src/test/java/com/planetaryfactory/core/fluid/` holds the arithmetic and the stall —
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
boils water is the third check, `gametest/BoilerTests` (#274), and it is three tests: that a Boiler
with water, fuel and room makes 3 mB a tick and spends the same water doing it — unit for unit,
since Factorio's boiler is a temperature change and not a reaction; that the item face takes fuel
and hands nothing back; and the one thing no static check here can reach, that the fluid face's two
refusals are the right way round **per tank**. That third one asks all four combinations through
the capability a pipe would find, on the *slot-less* overloads, with both tanks part full so no
refusal passes vacuously: swapped, the Boiler accepts steam it cannot use and lets a pipe drain its
water back out, against ADR-0050's rule that water is extracted and never created. The rate is
typed rather than read from `BoilerSpec`, the way `EnergyFaceTests`' furnace demand is — reading it
off the spec would make the test agree with the spec by construction. Each was checked against the
defect it exists for: swapping the two tank indices, making `BoilerSlots.canExtract` return true,
and making the cycle convert nothing each turn exactly one of the three red.

### Fluid colour check

The oil fluids are Oritech's, drawn in Factorio's colours (#277, ADR-0067). A fluid's colour is its
sprite times a tint, and Oritech's tint is a constructor argument that NeoForge refuses to register
twice, so `core/mixin/oritech/FluidModelContentMixin` swaps it for the one `FluidTintCorpus` reads
out of `planetaryfactory_core/fluid/tints.json`. `scripts/build-fluid-tints.py` writes that file from
Factorio's `base_color` (in `data/factorio/fluid.json`) and each sprite's average, read from the
Oritech jar. It retints only where Oritech's colour misses by more than 0.15. The sprite and tint
per Oritech fluid were read off the jar with `javap` and are the one typed table.
`tests/pack/test_fluid_tints.py` runs the `--check`, recomputes each borrowed fluid's drawn colour
against Factorio's, and asserts the mixin is on the client side of the Oritech config.
`FluidTintCorpusTest` covers the parse. Run both after editing a fluid row in the item map,
re-extracting the corpus, or updating Oritech. Whether the colours read right in a running client
is a human check on delivery.

### Steam Engine check

Oritech's Steam Engine entity is the pack's engine (#282, ADR-0062), under the pack's own block,
`planetaryfactory:steam_engine`, placed and broken as one footprint (#352, ADR-0077), and a mixin
(`core/mixin/oritech/SteamEngineEntityMixin`) replaces its `tickMaster` and `setupMaster` whole,
reaching the pack's subclass through inheritance. The HUD's status precedence is `SteamEngineStatusTest`.
`SteamEngineSpecTest` under `./gradlew :planetaryfactory_core:test` is the arithmetic, read from
`SteamChainCorpus`: one engine at speed 7 burns 30 mB/s and makes 450 FE/t **over whole ticks** --
Oritech's `(long)` cast floors 1.5 mB/t to 1, so the spec carries the fraction -- rows are linear, no
water returns, and the tank and FE buffer are 200 mB and 450 FE per engine in the row. The mixin
targets were read off the installed 2.0.0-exp6 jar with `javap`, not the 1.21.1 source clone, and
its config is `required: false` because Oritech is an optional dependency: a renamed target is a
warning in the log and an uncalibrated engine, not a crash. A buffer of one tick's output would
floor a pole-drained row to whole 300 FE millibuckets (1,200 FE/t for three), so the burn keeps
the millibucket that starts inside the room and carries its overshoot as energy (#292). That an
engine chains, through a part as well, and is pulled by a pole through a slave's part is
`SteamEngineNetworkTests`. Run the spec test after editing `core/fluid/SteamEngineSpec` or the mixin.

### Radar check

The Radar (#368, ADR-0079) is a 3x3x3 on the footprint seam that charts one 32-block sector per
10 MJ into its owner's FTB team's chart. `tests/factorio/test_machine_extract.py` holds the `radars`
row against the dump when it is on disk and re-derives 33.3 s per sector;
`tests/pack/test_radar_assets.py` runs `scripts/build-radar-assets.py --check` and holds the mod's
resource against the corpus and each ore's patch-marker lang key (#370). The rules are Minecraft-free under
`mod/src/test/java/com/planetaryfactory/core/radar/`: the draw, the 10 MJ sector and the 250 kJ
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

### Crude oil check

Crude is infinite (#377, ADR-0081): an **oil well** holds an amount, a Pumpjack on it yields
`10 × amount / normal` a cycle and takes 10 off it, down to the higher of 20% yield and 20% of the
well's start. `scripts/factorio-resource-extract.py` slices crude's figures into `amounts.json`
beside the ores, and `tests/factorio/test_resource_extract.py` asserts them and re-derives each
field's wells and centre amount per distance. The derivations are Minecraft-free under
`mod/src/test/java/com/planetaryfactory/core/oil/`: `WellYieldTest` (yield, the 1,000 cap, the floor,
the carried fraction), `OilFieldTest` (1/96 of the mask, 3 apart, ore columns turned away, the
amount), and `PumpjackEnergyTest` and `PumpjackSpecTest` (45 FE/t, a 1.5 FE/t drain paid idle, a
cycle per 900 FE). `scripts/build-pumpjack-assets.py` copies the `pumpjack` drill row and the item
map's crude fluid into the mod's resource, and `tests/pack/test_pumpjack_assets.py` runs its
`--check`, holds the resource against both, and asserts Oritech's two `oil_spring` biome modifiers
are overridden with a no-op -- NeoForge 26.1 has `none` for structure modifiers only. The field's
structure set is `build-outfield-worldgen.py`'s. `gametest/OilFieldTests` places a field 2,300 blocks
out and holds its wells to their drawn amounts, spacing and ground, with an iron disc on the same
centre turning away exactly the wells on its columns; `PumpjackTests` holds a fed Pumpjack to 10 mB a
cycle a second, drained from any face, and a starved one to nothing. Disabling the ore check or the
well refusal turns its test red. Whether the scaled Pump model reads well and the oil-field icons
appear on the FTB map is a human check on delivery. Run these after editing `core/oil/`, the oil
field's structure or piece, or either generator.

### Enemy corpus check

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

### Factorio mechanic ledger

`docs/factorio-mechanics.md` is the tracked list of every Factorio mechanic — base game and Space
Age — and what the pack does about it: one of `planned`, `shipped`, `adapted`, `blocked`,
`excluded`, never `undecided`. Read it before deciding a mechanic is out of scope, and update the
rows a ticket touches; a mechanic dropped without a row is exactly the failure it exists to catch.
It is not derived from `data/pack/subgroup-owner.json` and does not derive it — `not_emitted` there
is never evidence for `excluded` here — and it places nothing on a progression ladder, which is
#25's call. Row keys are Factorio's names by declared exception (ADR-0028).

### Recipe conversion

`scripts/factorio-recipe-convert.py` turns the extracted corpus into `planetaryfactory:assembling`
and `planetaryfactory:smelting` recipe JSON under `kubejs/data/planetaryfactory/recipe/` (#279), reading five committed data files: the corpus, the category
map, the subgroup owners, `data/pack/item-map.json` and `data/pack/recipe-overrides.json`. Nothing is
decided in the script — a decision is a diff to a design document. Generated output is never
hand-edited; re-run the converter. A Factorio name with no item-map row is a hard failure, while an
`undecided` row is a recorded skip. So is a row carrying `blocked_by`, the ticket that makes its
target loadable — a machine #277 has not chosen, a Researchd item #251 has not ported — and a machine
whose `recipe_type` is still null (the Chemical Plant and Oil Refinery, on #277). `--awaited` prints
those deferred recipes by the id they will load under, which is how the research-unlock,
science-pack and duplication checks tell a deferral from a typo.
`tests/factorio/test_recipe_convert.py` is the static check and runs the converter's `--check`; the
recipe *shape* is `scripts/check-datapack-load.py`'s. See
`docs/testing/recipe-conversion-check.md`.

### 26.1 data-format check

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
it is dead with ADR-0060 and re-derived against the chassis #258 names. A sized ingredient
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

### Client asset check

`scripts/check-client-assets.py` is the client half of #273's in-world work (#276), and the only
check here that can see whether anything the pack ships actually **renders**. Every other asset
check is file-to-file — `test_data_formats.py` walks definition → model, the asset-hop checks walk
blockstate → model → texture — and all of them assert the files name each other, never that the
game accepts them. `check-datapack-load.py` and the GameTest harness cannot help: both are
**server** runs, and a model, a texture, a blockstate and an item model definition are read by a
client neither of them starts.

NeoForge 26.1.2.109 has no client test harness — `neoforge/gametest/` is server-side — so this is
the real client, booted by `scripts/launch.py`'s headless context (imported, not reimplemented),
stopped when the log goes quiet for fifteen seconds with `blocks.png-atlas` created, and killed by
process group because `launch.py` is a Python parent holding a Java child. The allowlist rule is
`check-datapack-load.py`'s: every complaint about this namespace is an `EXPECTED` entry with its
ticket, an unlisted one fails, and a stale one fails too. The four today are models naming
departed `gcyr:` textures (#258). Other mods' namespaces
are not scanned; they are not ours to fix.

One blind spot is **measured rather than assumed**: a missing item model definition — the
fifteen-item failure the ticket was filed over — is logged nowhere at all. Deleting
`assets/planetaryfactory/items/boiler.json` produced a log with zero occurrences of `boiler` while
the item rendered as the checkerboard. That half stays with `test_data_formats.py`, and the two are
complementary by measurement. Whether the Boiler's texture is the *right* texture is not claimed
here. It is in no batch; run it after editing any model, blockstate, texture or definition. See
`docs/testing/client-asset-check.md`.

### Load-time codec check

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

### Emitted smelt shape check

`tests/factorio/test_smelting_shape.py` asserts the four emitted `planetaryfactory:smelting`
recipes are shaped the way **26.1** parses an ingredient: a string, `#`-prefixed for a tag, where
1.21.1 took `{"item": ...}`. The old shape does not crash — it is one `Couldn't parse data file`
line at datapack load and the recipe is then absent from the manager, which reaches a player as a
furnace that holds the item, holds power and never smelts. It shipped that way through the port and
cost #266's in-world check. `test_recipe_convert.py` could not see it: it runs the converter's
`--check`, which re-runs the converter and compares the output to what the converter would emit —
self-consistent by construction and blind to a shape Minecraft rejects. The assembling recipes'
shape is `test_data_formats.py`'s and `check-datapack-load.py`'s. Run it after any
converter change. A KubeJS reload is enough to see the fix in a running game — no restart.

### Stock-recipe sweep

`kubejs/server_scripts/recipes.js` removes every recipe the pack does not admit by name, and
`recipe_survivors.js` is the allowlist it negates (ADR-0034: a stock recipe ships only if a
decision names it and names the surface it is crafted on). A survivor is a *surface*, not a
recipe, and its filter's recipe type must be the one `data/pack/category-map.json` registers for
that machine — so a machine landing later without a survivor entry fails
`tests/factorio/test_recipe_sweep.py` rather than having its recipes swept in silence. Run that
check after editing either script, the category map or the emitted recipes; whether the sweep
removed the right things in a running game is a world load, not a static check.

### Recipe duplication check

`tests/factorio/test_recipe_duplication.py` asserts no item is made by two emitted recipes unless
`MULTI_ROUTE` names it and says what the second route earns. Every other recipe check owns one
subtree and one input table, which is the right shape for "did this converter do its job" and blind
to the question none of them can ask: whether two converters, or one converter twice, made the same
item. Two routes to one block fails no schema, appears in no log and loads perfectly — it reaches
the player as two EMI entries for the same thing, and if both are `category: crafting` the
Personal Assembler's resolver has no cost model to choose between them. It shipped once, when
Create's two gearbox conversions and the large cogwheel's second route were emitted alongside the
direct recipes they duplicate and every subtree-local check passed. One item legitimately has a
second route: solid fuel, which Factorio makes from each of its three oils, and that is a row
with its reason — all three of whose routes the converter currently holds back on #277, which the
check reads from the converter's `--awaited` rather than calling the row stale. The file-path
invariant it used to hold existed because GregTech re-registered every GTRecipe under its type's
path (#87); the pack's own types are re-registered by nothing, and the rule left with GregTech (#279).

Run it after any converter change. It does not assert the routes are balanced; costing is a
decision.

### Hand-written recipe check

`kubejs/data/planetaryfactory/recipe/assembling/pack/` is the one subtree no converter generates: ADR-0039's
two Engineer's Pick recipes, which the corpus can never author because Factorio has no mining-tool
prototype. `tests/factorio/test_pack_recipes.py` is what holds them, since every other recipe here
is checked against the corpus and these are checked against nothing otherwise — that the
converter still lists `pack` as foreign, which its own check reads from it rather than restating (a
run that forgets deletes them, and the sweep leaves no stock pickaxe to fall back on), that both land on a surface
`recipe_survivors.js` admits and carry `category: crafting` so the Personal Assembler
plans them at rung 0, that the steel recipe consumes the iron pick, and that each registered tier
has its model, texture, lang key, `c:tools/wrench` and `groundworks:dismantles`, the two tags that
carry its verbs, that `groundworks:dismantles` holds nothing else (#448), and the block tag the jar asks for by name. Both sprites are vanilla's own — the Iron Pick's `iron_pickaxe` and
the Steel Pick's `netherite_pickaxe` (#241, applied on #323). The Steel Pick used to wear GTCEu's
Damascus Steel pickaxe, flattened by a generator because GT's tool art is three greyscale layers
that only become a material under a colour handler our item never reaches; GregTech left with
ADR-0060 and took the source with it, so `scripts/build-pick-textures.py` and its `--check` are
gone rather than restated. The tier list is read out of `PickTier.java`. The pick's arithmetic —
that Factorio's seconds survive Minecraft's break-time formula — is `MiningSpeedTest` under
`./gradlew :planetaryfactory_core:test`. Whether the Pick mines every block class is a world load. See
`docs/testing/hand-written-recipe-check.md`.

### Stock recipe re-authoring check

A stock recipe the pack keeps is re-authored, never admitted as shipped (ADR-0034's tail).
`scripts/stock-recipe-convert.py` reads each recipe `data/pack/stock-admissions.json` admits out of
the installed jar, flattens a shaped pattern with every count kept, swaps each ingredient through
`data/pack/stock-substitutions.json` and writes a hand recipe under
`kubejs/data/planetaryfactory/recipe/assembling/stock/`. An ingredient in neither table, a table row
nothing reads, and a recipe no jar or two jars ship each fail the line (#442). An admission can
carry a `rewrite` instead of being flattened: its ingredients, all `keep` rows, and its yield,
chosen and recorded with a reason, and only the output is read from the jar (#444).
An `author` row is a recipe no jar ships, written whole from its row on the machine it names;
sand is ground on the Assembling Machine and smelted to glass under `recipe/smelting/stock/` (#445).
`tests/factorio/test_stock_recipes.py` runs the `--check`, holds each ingredient to an item another
pack recipe makes or a `keep` row, each output to an item a jar defines, each recipe to the
machine's input slots, and the union of every emitted hand recipe to no cycle; a second hand route
is `test_recipe_duplication.py`'s. It also holds the wooden stairs to one per species Terra's biomes
grow, read out of the biome files, and the subtree to no wall. Run it after editing either file or
after a jar update. Whether the filter appears in EMI with a route to follow is a human check on
delivery.

### Item map check

`tests/pack/test_item_map.py` holds ADR-0061: FTB Materials owns every material form, and a tech
mod supplies machines. It asserts every `data/pack/item-map.json` target resolves against the
installed jars (the pack's own via its lang and KubeJS's `event.create`, vanilla via the client jar
when present), that no row names a mod ADR-0060 removed, that the seven material-form rows are
`ftbmaterials:`, and that no emitted recipe or item tag names a `c:` tag more than one installed jar
populates -- with AlmostUnified gone, `#c:ingots/steel` accepts three items and is not a decision.
The rows #277 (machines, blocks, oil fluids) and #251 (Researchd) own sit in `DEFERRED`, and each
must carry `blocked_by` with that ticket so the converter emits nothing naming it; a stale entry
fails, so delete one as its row resolves. Run it after editing the item map or re-running a converter.

### Research unlock check

`tests/factorio/test_research_unlocks.py` asserts every recipe id a research grants is a recipe the
pack emits, or one the converter's `--awaited` holds back on a ticket. Researchd gates by recipe id and a recipe's id follows its type, so re-surfacing a
recipe leaves the research locked to an id nothing emits — with no error, no failed recipe and no
log line, reaching the player as a research that unlocks nothing. Run it after editing
`researchd.js` or after re-running the converter. It is the second half of #97; the first half —
nothing emitted or admitted is a vanilla grid recipe — lives in the sweep check. See
`docs/testing/research-unlock-check.md`.

### Factorio tech tree

The pack's research tree takes its shape from Factorio's, extracted rather than transcribed
(ADR-0022). `data/factorio/technology.json` is the committed reference; `researchd.js` declares
each research with `fromFactorio(name, {icon, unlocks, ...})` and supplies only the
Minecraft-specific parts. Regeneration and provenance are in `data/factorio/README.md`.
`tests/factorio/test_tech_extract.py` asserts the pruned tree is still a valid tree and that every
declared name exists — run it after re-extracting or after editing `researchd.js`.

### Pack manifest

The jar set is a packwiz manifest tracked in git (ADR-0024) — `pack.toml`, `index.toml` and one
`mods/*.pw.toml` per externally-sourced mod. `mods/*` is gitignored with `!mods/*.pw.toml` re-included;
never rewrite that as a bare `mods`, or the manifest silently stops being tracked. The local
Beltworks jar is an unmanaged hashed entry and `planetaryfactory_core` is not indexed at all.
`scripts/pack-check.sh` asserts the installed jars still match. See `docs/pack/packwiz-workflow.md`.

### Local jar check

Beltworks is a **local jar**: `data/pack/local-jars.json` pins the version the Pack runs, and
`scripts/sync-local-jars.py beltworks=<version>` writes the pin, copies that jar out of `~/.m2` into
`mods/`, refreshes the manifest and rebuilds the core mod (#465, ADR-0024). The build reads the same
table and names no Library (#475, ADR-0090): every pinned jar, and every jar its row `nests`, is on
the compile classpath and the dev runs, and each nested artifact's range is read from the jarjar
metadata into `neoforge.mods.toml` as `<artifact>_version_range`. So the Pack names no Groundworks
version, and adding a Library is a row and a sync. `tests/pack/test_local_jars.py` runs
the sync's `--check`: the jar in `mods/` is the pinned one, byte for byte `~/.m2`'s when `~/.m2`
holds it, and nests Groundworks; a newer version in `~/.m2` is named without failing. Run it after
the sync or any change to `mods/`. Take a new Beltworks with the sync, never by copying a jar.
A change that crosses Groundworks, Beltworks and the Pack goes through the `release-train` skill
(`skillworks:release-train`, from 5thlayer/skillworks): each checkout is owned by the session working in it, and
nothing is pushed without the user's word.

`-PsiblingBuilds` is for trying such a change in the Pack before a library is released (#466, #475).
It includes, as a composite, the checkout of every row of `local-jars.json` and of each jar the row
nests, or of only the rows named (`-PsiblingBuilds=craftworks`), each at `-P<name>Dir`, default
`~/minecraft_mods/<name>`. The compile and every dev run, `runGameTestServer` included, use those
checkouts and never their `mods/` jars. The build prints one `siblingBuilds:` line per checkout,
naming its version and HEAD.
`installToPack` refuses under it, and `scripts/check-datapack-load.py --sibling-builds` forwards it.
It never installs, and a green run under it proves nothing about the pinned jars: the change still
ships through the release train. A Groundworks checkout outside the range Beltworks nests it under
fails the build, naming the range, so a Groundworks minor needs Beltworks' range moved in its
checkout too.

### First-party mod

`planetaryfactory_core` is a Gradle subproject in `mod/`, built from the repo root with
`./gradlew :planetaryfactory_core:installToPack` — required after a fresh clone, since the jar
lands in the gitignored `mods/`. It owns mechanism only; ADR-0015 has the ownership table for
what goes in the mod, in KubeJS and in datapack JSON. See `mod/README.md`.

<!-- rtk-instructions v2 -->
# RTK (Rust Token Killer) - Token-Optimized Commands

## Golden Rule

**Always prefix commands with `rtk`**. If RTK has a dedicated filter, it uses it. If not, it passes through unchanged. This means RTK is always safe to use.

**Important**: Even in command chains with `&&`, use `rtk`:
```bash
# ❌ Wrong
git add . && git commit -m "msg" && git push

# ✅ Correct
rtk git add . && rtk git commit -m "msg" && rtk git push
```

## RTK Commands by Workflow

### Build & Compile (80-90% savings)
```bash
rtk cargo build         # Cargo build output
rtk cargo check         # Cargo check output
rtk cargo clippy        # Clippy warnings grouped by file (80%)
rtk tsc                 # TypeScript errors grouped by file/code (83%)
rtk lint                # ESLint/Biome violations grouped (84%)
rtk prettier --check    # Files needing format only (70%)
rtk next build          # Next.js build with route metrics (87%)
```

### Test (60-99% savings)
```bash
rtk cargo test          # Cargo test failures only (90%)
rtk go test             # Go test failures only (90%)
rtk jest                # Jest failures only (99.5%)
rtk vitest              # Vitest failures only (99.5%)
rtk playwright test     # Playwright failures only (94%)
rtk pytest              # Python test failures only (90%)
rtk rake test           # Ruby test failures only (90%)
rtk rspec               # RSpec test failures only (60%)
rtk test <cmd>          # Generic test wrapper - failures only
```

### Git (59-80% savings)
```bash
rtk git status          # Compact status
rtk git log             # Compact log (works with all git flags)
rtk git diff            # Compact diff (80%)
rtk git show            # Compact show (80%)
rtk git add             # Ultra-compact confirmations (59%)
rtk git commit          # Ultra-compact confirmations (59%)
rtk git push            # Ultra-compact confirmations
rtk git pull            # Ultra-compact confirmations
rtk git branch          # Compact branch list
rtk git fetch           # Compact fetch
rtk git stash           # Compact stash
rtk git worktree        # Compact worktree
```

Note: Git passthrough works for ALL subcommands, even those not explicitly listed.

### GitHub (26-87% savings)
```bash
rtk gh pr view <num>    # Compact PR view (87%)
rtk gh pr checks        # Compact PR checks (79%)
rtk gh run list         # Compact workflow runs (82%)
rtk gh issue list       # Compact issue list (80%)
rtk gh api              # Compact API responses (26%)
```

### JavaScript/TypeScript Tooling (70-90% savings)
```bash
rtk pnpm list           # Compact dependency tree (70%)
rtk pnpm outdated       # Compact outdated packages (80%)
rtk pnpm install        # Compact install output (90%)
rtk npm run <script>    # Compact npm script output
rtk npx <cmd>           # Compact npx command output
rtk prisma              # Prisma without ASCII art (88%)
rtk uv run <cmd>        # Compact uv project command output
```

### Files & Search (60-75% savings)
```bash
rtk ls <path>           # Tree format, compact (65%)
rtk read <file>         # Code reading with filtering (60%)
rtk grep <pattern>      # Search grouped by file (75%). Format flags (-c, -l, -L, -o, -Z) run raw.
rtk find <pattern>      # Find grouped by directory (70%)
```

### Analysis & Debug (70-90% savings)
```bash
rtk err <cmd>           # Filter errors only from any command
rtk log <file>          # Deduplicated logs with counts
rtk json <file>         # JSON structure without values
rtk deps                # Dependency overview
rtk env                 # Environment variables compact
rtk summary <cmd>       # Smart summary of command output
rtk diff                # Ultra-compact diffs
```

### Infrastructure (85% savings)
```bash
rtk docker ps           # Compact container list
rtk docker images       # Compact image list
rtk docker logs <c>     # Deduplicated logs
rtk kubectl get         # Compact resource list
rtk kubectl logs        # Deduplicated pod logs
```

### Network (65-70% savings)
```bash
rtk curl <url>          # Compact HTTP responses (70%)
rtk wget <url>          # Compact download output (65%)
```

### Meta Commands
```bash
rtk gain                # View token savings statistics
rtk gain --history      # View command history with savings
rtk discover            # Analyze Claude Code sessions for missed RTK usage
rtk proxy <cmd>         # Run command without filtering (for debugging)
rtk init                # Add RTK instructions to CLAUDE.md
rtk init --global       # Add RTK to ~/.claude/CLAUDE.md
```

## Token Savings Overview

| Category | Commands | Typical Savings |
|----------|----------|-----------------|
| Tests | vitest, playwright, cargo test | 90-99% |
| Build | next, tsc, lint, prettier | 70-87% |
| Git | status, log, diff, add, commit | 59-80% |
| GitHub | gh pr, gh run, gh issue | 26-87% |
| Package Managers | pnpm, npm, npx | 70-90% |
| Files | ls, read, grep, find | 60-75% |
| Infrastructure | docker, kubectl | 85% |
| Network | curl, wget | 65-70% |

Overall average: **60-90% token reduction** on common development operations.
<!-- /rtk-instructions -->