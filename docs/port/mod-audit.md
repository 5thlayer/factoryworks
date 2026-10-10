# `mod/` audit (#662)

Every package of the old `factoryworks_core` subproject, classified against the Module repos as of
2026-10-09 (beltworks 9511003, craftworks 486b406, fieldworks 44c636d, groundworks 2b33c2d,
pipeworks c77ede7, wireworks 249945b), under ADR-0127 and ADR-0128.

- **A** already carried by a Module repo · **B** integration GameTest / Showcase glue, moves with the
  Showcase · **C** orphaned: names the Module that should own it, or drop.

Fieldworks is a skeleton today (`Fieldworks.java` and load tests only), so everything ADR-0127 assigns
to it is C with Fieldworks as owner. No Module carries the Boiler, Steam Engine, Offshore Pump,
Refiner, pipe Dismantle Family or drag-laying yet.

| package | what | size (files/lines) | class | where / proposed owner | evidence |
|---|---|---|---|---|---|
| `FactoryWorksCore`, `PF*` registries | mod entry, registries, pole Replace group binding | 8 / 1086 | C | split: each Module registers its own; pole Replace group → Wireworks (`PoleColumnReplace`) or drop | `PFBlocks`, `PFItems`, `PFBlockEntities`; Oritech referenced in `FactoryWorksCore`, `PFItems` |
| `ore/` | ore patches, outfield discs, patch ledger, census | 16 / 1412 | C | Fieldworks (patches); drop Factorio amounts/ledger beyond ADR-0127 | `OreField`, `OutfieldDisc`, `PatchLedger`; nothing in fieldworks/src |
| `oil/` | oil wells, well yield, Pumpjack | 16 / 920 | C | Fieldworks: wells + `WellYield` (ADR-0081 floor); Pumpjack → drop, replaced by Harvester | `OilWellBlock`, `WellYield`, `PumpjackBlockEntity` |
| `mining/` (incl. `rig/`) | burner/electric drills (Rig), mining speed, Engineer's Pick | 29 / 2850 | C | Rig → Fieldworks Harvester (reimplement); `EngineersPick`, `PickTier`, `MiningSpeed` → drop (ADR-0127 "the Pick and reach") | `RigBlockEntity`, `RigTier`, `EngineersPick` |
| `worldgen/` | outfield-disc and oil-field structures, Terra start, water fixtures, vanilla spawning default | 10 / 1150 | C + B | `OutfieldDisc*`, `OilField*` → Fieldworks; `TerraStartingArea`, `VanillaSpawning`, `WaterFixture/Census`, `GroundProcessor` → Showcase (B) or drop with Terra | `PFWorldgen`, `TerraStartingArea` (ADR-0093 for spawning) |
| `radar/` (incl. `ftb/`, `client/`) | radar block, sectors, team chart, FTB Chunks markers | 25 / 1660 | C | block/sweep/chart → drop (ADR-0127 "What goes"); `PatchMarker`, `FtbMapMarkers`, `PatchAmount` → Fieldworks (one marker per patch, FTB Chunks first) | `RadarBlock`, `RadarSweep`, `FtbMapMarkers` |
| `fluid/` (Boiler, Steam Engine, steam) | Boiler, Steam Engine, steam fluid, spec/footprint | ~22 / ~1900 | C | Wireworks (ADR-0127); not present there | `BoilerBlockEntity`, `SteamEngineSpec`; no boiler/steam class in wireworks |
| `fluid/` (Offshore Pump) | offshore pump, siting | 5 / ~300 | C | Pipeworks as the **Pump** (reimplement, infinite-source tag) | `OffshorePumpBlockEntity`, `OffshorePumpSiting` |
| `fluid/` (crude, barrel, tints, oil client) | `PFFluids`/`PFFluidTypes` (crude etc.), barrel, `FluidTintCorpus`, `WaterConservation` | ~10 / ~800 | C | crude oil → Fieldworks (`c:crude_oil`); oil-chain fluids, barrel → drop; tints follow their fluid | `PFFluids`, `BarrelItem`, `OilFluidClient` |
| `smelting/` | stone/steel/electric furnace tiers, fuel table | 16 / 1915 | C | electric tier → Craftworks **Refiner** (reimplement); stone/steel furnaces + `FuelTable` → drop (ADR-0127) | `FurnaceTier`, `FuelTable`, `FurnaceBlockEntity` |
| `recipes/` | count-carrying smelting recipe type | 2 / 202 | C | drop (ADR-0127: vanilla recipe types only); Refiner uses Craftworks' m:n smelting | `SmeltingRecipe`, `PFRecipes` |
| `felling/` | tree felling | 9 / 742 | C | drop (ADR-0127 "tree felling") | `TreeFelling`, `TreeCorpus` |
| `wreck/` | crash-site wreck, cargo hold, debris | 14 / 539 | C | drop (ADR-0127 "the wreck") | `CargoHoldBlockEntity`, `WreckSpawn` |
| `start/` | starting kit grant | 4 / 215 | B | Showcase (ADR-0127 names the starting kit as a Showcase tweak; may become KubeJS) | `StartingKit`, `StartingKitGrant` |
| `reach/` | player Reach | 1 / 67 | C | drop (ADR-0127 "the Pick and reach") | `Reach` |
| `chest/` (incl. `client/`) | iron/steel chests | 5 / 167 | C | unassigned: drop, or a Showcase tweak (open question) | `ChestTier`, `PackChestBlock` |
| `machine/` (incl. `footprint/`) | multiblock footprint anchor/part/item, Oritech-backed; `OverloadLimit`, `MachineTooltip` | 8 / 659 | A (partial) | Groundworks `Footprint`, `FootprintPartBlock`, `FootprintItem`, `QuarterTurn`; Craftworks `machine/OverloadLimit`. Oritech binding → drop | Oritech imports in `machine/footprint/*` |
| `placement/` (incl. `client/`) | Replace groups, refusal, plan overlays | 6 / 239 | A (partial) | Groundworks `VanillaReplaceGroups`, `Refusal`, `PlacementPreview`; pack-specific `replace_groups.json` → owning Modules | `ReplaceGroups`, `PackRefusal` |
| `dismantle/` | pipe Dismantle Family | 3 / 119 | C | Pipeworks (ADR-0127); not present there | `PipeFamily`, `PipeworksPipeJoin`; no `DismantleFamily` use in pipeworks |
| `stretch/` | pipe drag-laying | 1 / 120 | C | Pipeworks (ADR-0127); not present there | `PipeworksPipeLegs` |
| `energy/` | FE exchange rate, snapshot journal | 2 / 59 | A | Wireworks `ForgeEnergy`, `LongSnapshotJournal` (same names) | identical class names |
| `transfer/` | slot-honouring `DelegatingResourceHandler` | 1 / 60 | C | copy into each Module with routed faces (Fieldworks, Wireworks Boiler) or Groundworks utility | `GuardedResourceHandler`; NeoForge needs it too ([research](../research/transfer-api-guarded-faces.md)) |
| `network/` | radar chunk/markers, fuel-table packets | 4 / 200 | C | `RadarMarkersPacket` → Fieldworks if markers need sync; rest drop | `PFNetwork` |
| `compat/` (incl. `emi/`) | Jade plugins (rig, furnace, ore, radar, steam engine, footprint), EMI smelting | 10 / 876 | C / A | `FootprintJadePlugin` A (Groundworks); others follow their machine (Fieldworks/Wireworks/Craftworks) or drop | `RigJadePlugin`, `SmeltingEmiPlugin` |
| `mixin/minecraft/` | `ChestBlockMixin` (no double chests), `PlayerSpawnFinderMixin` (wreck spawn), `MinecraftMixin` (reach), `GameTestServerMixin` (Terra floor y=0) | 4 / ~120 | B / C | `ChestBlockMixin` → Showcase (ADR-0106) or drop; `PlayerSpawnFinder`, `MinecraftMixin` → drop; `GameTestServerMixin` → Showcase test harness if Terra stays | mixin javadocs |
| `mixin/oritech/` | Oritech outline + machine core on pole network | 2 / ~55 | C | drop (Oritech leaves, ADR-0127) | `MachineCoreEntityMixin` |
| `showcase/` | filmed showcase scenes + `/showcase` command | 2 / 341 | B | Showcase pack | `ShowcaseScenes`, `ShowcaseCommand` |
| `gametest/` harness | `PFGameTests`, `PFGameTestInstance`, `Faces`, `LibraryBlocks`, `ListeningPlayer` | 5 / ~380 | B | Showcase integration harness | registers `factoryworks:*` |
| gametest: integration | `AssemblerOilChainTests`, `AssemblingFluidTests`, `AssemblingMachineTests`, `HandSetTests`, `BeltworksPackTests`, `PipeStretchTests`, `PipeDismantleTests`, `SteamChainTests`, `ElectricRigTests`, `ChestTests`, `ShowcaseSceneTests` | 11 / ~1990 | B | Showcase; `PipeStretch`/`PipeDismantle` partly → Pipeworks with the mechanism | import craftworks/pipeworks/wireworks/groundworks |
| gametest: single-mechanism | `OilFieldTests`, `OutfieldDiscTests`, `PumpjackTests`, `RigBreakTests`, `RadarTests`, `BoilerTests`, `BurnerFurnaceTests`, `FurnaceOverloadTests`, `EnergyFaceTests`, `FootprintBreakTests`, `PackChestTests`, `ReachTests`, `WreckTests`, `WorldgenFixtureTests`, `SpawningRuleTests` | 15 / ~3000 | C | follow their mechanism (Fieldworks: oil/outfield/rig; Wireworks: Boiler; Craftworks: furnace overload/energy face) or drop with it | import only `com.factoryworks.core.*` |
| gametest: `PlacementPlanTests` | placement plans for every footprint machine | 1 / 719 | B / C | split per Module once machines move | imports 8 core packages + groundworks |
| gametest: `SteamEngineNetworkTests` | steam engine on Wireworks + Oritech | 1 / 390 | B / C | Showcase once rewritten without Oritech | imports `rearth.oritech` |

## Unit tests (`mod/src/test`)

68 files / 5646 lines, all mechanism, none integration. Each follows its package's row:

| package | files | goes with |
|---|---|---|
| `fluid/` (Boiler, Steam Engine, Offshore Pump, tints, steam) | 13 | Wireworks, Pipeworks; tint/steam corpora frozen or dropped |
| `mining/` (rig corpus, rate, speed) | 11 | Fieldworks Harvester (rewrite), Pick and speed dropped |
| `radar/` (sweep, charts, markers) | 10 | markers to Fieldworks, the rest dropped |
| `ore/` (amounts, outfield, ledger, census, codecs) | 9 | Fieldworks |
| `smelting/` (fuel, furnace tiers) | 9 | electric tier to Craftworks, the rest dropped |
| `oil/` (field, pumpjack, well yield) | 5 | Fieldworks; pumpjack dropped |
| `machine/` (`OverloadLimitTest`, `FootprintTest`, `MachineTooltipTest`) | 3 | Craftworks, Groundworks |
| `wreck/` | 3 | dropped |
| `felling/` | 2 | dropped |
| `placement/` (`ReplaceGroupsTest`) | 1 | Groundworks |
| `start/` | 1 | Showcase, or dropped with Terra |
| `worldgen/` (`WaterCensusTest`) | 1 | Showcase, or dropped with Terra |

## Resources (`mod/src/main/resources`)

| path | namespace | still referenced |
|---|---|---|
| `factoryworks_core/{ore,oil,mining,radar,felling,fluid,machine,placement,wreck}/*.json` | `factoryworks_core` (corpora read by `*Corpus`) | yes: written by `scripts/build-{ore,radar,tree,wreck}-assets.py`, read by `tests/pack/test_fluid_tints.py` |
| `assets/factoryworks_core/lang/` | `factoryworks_core` | `tests/pack/test_licensing.py` |
| `data/factoryworks/structure/gametest/` (2 structures) | `factoryworks` | GameTest harness → Showcase |
| `data/minecraft/tags/block/mineable/` | `minecraft` | tag for Core blocks |
| `META-INF/neoforge.mods.toml` | — | requires **oritech**, groundworks, pipeworks; optional emi, ftbchunks; read by `test_licensing.py` |
| `factoryworks_core.mixins.json`, `.oritech.mixins.json` | — | mixin configs |

Other repo scripts naming `factoryworks_core`: `sync-local-jars.py`, `check-datapack-load.py`,
`jar-registry-extract.py`, `build-terra-start.py`, `factorio-recipe-convert.py`, `scripts/tests/test_upload.py`,
`tests/factorio/test_{recipe_convert,subgroup_owner,resource_extract}.py`.

## Gradle (`mod/build.gradle`)

- Compile classpath: every `data/pack/local-jars.json` row (Groundworks range intersected into
  `groundworks_version_range`), FTB Chunks, and `mods/` jars `oritech-*`, `emi-*`; dev runtime adds
  Oritech, Athena, GeckoLib, KubeJS, Rhino.
- `linkKubeJS`, `linkServerConfigs`: link repo `kubejs/` and server configs into `mod/run/` before
  every `prepare*Run` → Showcase.
- `runGameTestServer` (`--tests factoryworks:*`, `factoryworks_showcase:*`) → Showcase harness.
- `installToPack`: copies the jar into `mods/`, refused under `-PsiblingBuilds` → retire once Core
  ships as Fieldworks from its own repo.
- `-PsiblingBuilds` composite of local-jar checkouts → Showcase.
- `mavenJava` publication → retire.

## Open questions

1. Chests (`chest/`, `ChestBlockMixin`, ADR-0106): ADR-0127 assigns them nowhere. Showcase tweak or drop?
2. Does the Showcase keep Terra? `TerraStartingArea`, `GameTestServerMixin`, `WaterFixture`, `WorldgenFixtureTests`, `SpawningRuleTests` hang on it.
3. Pipe Dismantle Family / drag-laying: port from `dismantle/`+`stretch/` into Pipeworks, or rewrite there? Its tests (`PipeDismantleTests`, `PipeStretchTests`) would move too.
4. Boiler/Steam Engine port to Wireworks: reuse `fluid/` code (needs Oritech-free rewrite of `SteamEngineNetworkTests`) or reimplement?
5. Harvester: reuse Rig code (`mining/rig/`) or start fresh in Fieldworks?
6. `transfer/GuardedResourceHandler`: shared helper in Groundworks, or copied per Module? ([research](../research/transfer-api-guarded-faces.md))
7. Corpus JSONs under `factoryworks_core/` and their generators: move with Fieldworks, or freeze numbers (ADR-0115 "frozen numbers") into Fieldworks code?
