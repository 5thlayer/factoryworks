# Long-range and cross-dimension transport: prior art

Research for [#696](https://github.com/5thlayer/factoryworks/issues/696), feeding
[#697](https://github.com/5thlayer/factoryworks/issues/697). Question: how do established Minecraft mods and
Factorio move items far within a dimension and between dimensions or planets, what does each cost to build and
run, what limits it, and does it work with chunks unloaded?

Method: each claim is taken from the owner's source code (config defaults, recipes, tick logic) or the owner's
own documentation (wiki.factorio.com, lua-api.factorio.com, AE2's in-repo guidebook, PneumaticCraft's in-repo
Patchouli book). Numbers that are config defaults name the config file. Source was read at the branch named in
each section, on 2026-10-16. Anything not traced to a primary source is marked **unverified**.

Factorio facts come only from the public wiki and the Lua API docs, not from Wube's data files (ADR-0103).

## Summary

| Mechanism | Moves | Throughput | Build cost | Running cost | Cross-dim | Unloaded chunks |
|---|---|---|---|---|---|---|
| Factorio logistic robots | items | 1 item/trip (4 with research), 3 tiles/s base | robot: 2 adv. circuits + flying robot frame; roboport: 45 adv. circuits, 45 gears, 45 steel | electricity per second aloft and per tile flown; roboport 50 kW drain | no (one surface) | n/a: Factorio keeps running entities active |
| Factorio Space Age rocket (surface to orbit) | items (fluids only in barrels) | 1000 kg per launch | silo, then 50 rocket parts per launch | the 50 parts (50 each of processing unit, low density structure, rocket fuel) | yes (surface to platform) | n/a |
| Factorio platform drop (orbit to surface) | items | landing pad takes 3 stacks at once (+1 per cargo bay) | landing pad, one per planet | none ("no material costs") | yes (platform to surface) | n/a |
| Factorio platform travel | the platform and its hub contents | speed falls with width more than mass | platform, thrusters | thruster fuel + oxidizer | yes (planet to planet) | n/a |
| Create trains | items, fluids, passengers | 28 b/s, 40 b/s fuelled; capacity = vaults/tanks on the train (vault: 20 stacks per block) | track, station, bogeys, carriages | none, or burnable fuel for the speed boost | Nether by portal track; Aether, BetterEnd by compat | train simulated globally; cargo moves only where loaded; no chunk loading |
| Mekanism teleporter | entities (players, mobs, dropped items) | per entity per trigger | teleporter + frame (refined obsidian, refined glowstone, teleportation core) | per entity: base + per-block distance + flat dimension penalty, x(1+passengers) | yes | source must be loaded; Anchor Upgrade chunk-loads |
| Mekanism quantum entangloporter | items, fluids, chemicals, energy, heat | bounded by connected pipes; buffer is 1 item slot | refined obsidian, ultimate circuits, atomic alloy, teleportation core | none | yes | Anchor Upgrade chunk-loads |
| AE2 quantum network bridge | ME channels (so anything on the network) | 32 channels | 8 rings + link chamber per side, a pair of entangled singularities (one singularity = 256,000 items condensed) | 22 AE/t idle per bridge block entity | yes | both sides must be loaded; Spatial Anchor needed per dimension |
| AE2 P2P tunnels | items, fluids, FE, redstone, light, channels | as source/sink allow; FE taxed 2.5% | one tunnel per end | AE power; FE tunnel 2.5% | only across a quantum bridge | needs the network loaded |
| Modular Routers Sender Mk3 | items | 1 to 64 items per operation, every 20 ticks base, down to 2 | Sender Mk2 + end stone + ender chest | 0 FE by default (configurable) | yes (dimension blacklist) | router and target must both be loaded; no chunk loading |
| PneumaticCraft logistics drones | items, fluids | 1 slot + 1 per inventory upgrade | drone + frames | pressurised air | no | refuses unloaded chunks by default; Chunkloader Upgrade costs 5 air/tick each |
| Thermal tesseract (1.7.10) | items, fluids, energy (**unverified**) | **unverified** | **unverified** | **unverified** | **unverified** | **unverified** |
| Thermal Dynamics warp itemduct (1.12) | items | near-instant over the duct | ducts | 50 RF per advance | no (physical duct) | duct must be loaded |
| Ender IO dimensional transceiver (1.12) | items, fluids, energy | 8 in + 8 out slots | machine | 10 RF/t base upkeep (capacitor-scaled) | yes | deregisters on chunk unload |
| Ender IO travel anchor | players only | n/a | anchor | staff energy | no | n/a |
| Draconic dislocator receptacle | entities | per entity entering the portal | receptacle + bound dislocator | none found per teleport | yes | **unverified** |
| Ender Storage ender chest | items (27 slots shared), fluids (ender tank) | as fast as hoppers or pipes at each end | ender chest | none | yes | inventory is global saved data; pipes at each end need their chunk loaded |
| RFTools matter transmitter | players | per teleport | transmitter, receiver, dialer | 5,000 RF start local, 100,000 cross-dim, plus 500 RF/t while teleporting | yes | **unverified** |

## Factorio (design reference)

Version: Factorio 2.0 / Space Age, as the wiki stated on 2026-10-16.

### Logistic robots and roboports

- A logistic robot carries 1 item, raised to 4 by the worker robot cargo size research. It flies at 0.05
  tiles/tick (3 tiles/s) with power; speed research is infinite. It draws power for each second aloft and for
  each tile flown. Battery: 1.5 MJ at normal quality. Out of power, it flies at 20% speed until it reaches a
  roboport to recharge. Recipe: 2 advanced circuits + 1 flying robot frame.
  [Logistic robot](https://wiki.factorio.com/Logistic_robot)
- A roboport covers a 50x50 logistic area and a 110x110 construction area, charges 4 robots at once (4x500 kW
  at normal quality), drains 50 kW, holds 7 robot slots and 7 repair-pack slots. Recipe: 45 advanced circuits,
  45 iron gears, 45 steel. [Roboport](https://wiki.factorio.com/Roboport)
- Robots only work inside a roboport network, so their range is the network the player builds. They cannot
  leave a surface.

Levers: per-trip capacity (1 to 4 items), speed, energy per tile (distance-scaled running cost), the
infrastructure needed to cover the distance (roboports), recharge stops.

### Rockets, platforms and landing pads (Space Age)

- **Weight limit.** A rocket lifts 1000 kg: `rocket_lift_weight` defaults to 1,000,000 (grams). Each item's
  weight is derived from its recipe (ingredient weight x 0.5 by default, `ingredient_to_weight_coefficient`),
  falling back to `default_item_weight` = 100 g, and items may set their own weight. Capacity per item type
  is lift weight / item weight, rounded down to whole stacks when above one stack.
  [Item weight](https://lua-api.factorio.com/latest/auxiliary/item-weight.html)
- **Build and per-launch cost.** In Space Age a rocket needs 50 rocket parts, each 1 processing unit + 1 low
  density structure + 1 rocket fuel. Fluids cannot go in rockets, but full barrels can. With logistic
  requests the silo is filled by robots and launches itself when full, unless the requesting platform has set
  a smaller minimum payload. [Rocket silo](https://wiki.factorio.com/Rocket_silo)
- **Automation.** Platforms make logistic requests of planets. A request-driven launch carries one item type
  per rocket, and a manual launch can mix types. [Space platform](https://wiki.factorio.com/Space_platform)
- **Down is free.** Platforms drop cargo to the surface in cargo pods from the hub, and the wiki says these
  transfers "come at no material costs". [Space platform](https://wiki.factorio.com/Space_platform)
- **Cargo landing pad.** Only one per surface. It acts as a requester chest for orbiting platforms, which
  deliver only while stopped over that planet. It has 3 hatches, so it receives 3 stacks at once; each
  adjacent cargo bay adds 20 slots and 1 more simultaneous stack. Base storage 80 slots.
  [Cargo landing pad](https://wiki.factorio.com/Cargo_landing_pad)
- **Travel.** A platform needs one or more thrusters to travel between locations. Thrusters burn thruster
  fuel and oxidizer (6 to 120 units/s each at normal quality, depending on how full they are), and are most
  efficient at a low fill level. Speed depends far more on platform width than mass. The hub weighs 40 t and
  each foundation tile 200 kg. Asteroid density and speed grow with platform speed, so defence is a running
  cost too. [Thruster](https://wiki.factorio.com/Thruster),
  [Space platform](https://wiki.factorio.com/Space_platform),
  [Space platform foundation](https://wiki.factorio.com/Space_platform_foundation)
- **Schedules.** Platforms follow schedules with wait conditions (item counts, circuit signals and others)
  and route through intermediate stops on their own. [Space platform](https://wiki.factorio.com/Space_platform)
- Distances between planets (Nauvis to Vulcanus is often quoted as 15,000 km) were not found on a wiki page:
  **unverified**.

### Simulation

Factorio does not unload chunks the way Minecraft does. Chunks are only made inactive "when nothing important
is happening in them", such as only fish or idle enemies. [Map structure](https://wiki.factorio.com/Map_structure)
Robots, silos and platforms therefore keep running wherever the player is, so Factorio never has to solve the
unloaded-chunk problem.

Levers: a weight cap per launch, an expensive per-launch build (50 parts), a free return leg, one receiving pad
per planet with a throughput set by hatches, travel paid in fuel and in exposure to asteroids, and latency (a
platform must be parked over the planet).

## Create trains

Version: Create 6.0.11 for MC 1.21.1, branch `mc1.21.1/dev` at
[`7ff2760`](https://github.com/Creators-of-Create/Create/tree/7ff2760e4924d35d4bc2e22c4b19923f0178cd16). Create
does not exist for MC 26.1.2.

- **What it moves.** A train is a contraption, so its cargo is whatever storage is built onto the carriages:
  item vaults, chests, fluid tanks, seats. A vault holds `vaultCapacity` = 20 stacks per block
  ([`CLogistics.java`](https://github.com/Creators-of-Create/Create/blob/7ff2760e4924d35d4bc2e22c4b19923f0178cd16/src/main/java/com/simibubi/create/infrastructure/config/CLogistics.java)).
  Loading and unloading at a station goes through Portable Storage Interfaces.
- **Speed and limits.** From
  [`CTrains.java`](https://github.com/Creators-of-Create/Create/blob/7ff2760e4924d35d4bc2e22c4b19923f0178cd16/src/main/java/com/simibubi/create/infrastructure/config/CTrains.java):
  `trainTopSpeed` 28 b/s, `trainTurningTopSpeed` 14, `trainAcceleration` 3 b/s². A fuelled ("powered") train
  gets `poweredTrainTopSpeed` 40 b/s and turning speed 20. `maxAssemblyLength` 128, `maxBogeyCount` 20,
  `maxTrackPlacementLength` 32.
- **Running cost.** None by default. `Train.burnFuel()` takes any burnable item from fuel inventories on the
  carriages, and while `fuelTicks > 0` the train uses the powered stats
  ([`Train.java`](https://github.com/Creators-of-Create/Create/blob/7ff2760e4924d35d4bc2e22c4b19923f0178cd16/src/main/java/com/simibubi/create/content/trains/entity/Train.java)).
  Fuel is only a speed boost, never a requirement.
- **Cross-dimension.** Track can run through a portal. `AllPortalTracks.registerDefaults()` registers the
  Nether portal, plus the Aether, Aether II and BetterEnd portals when those mods are loaded. Simple
  integrations only go from the Overworld to another dimension and back
  ([`AllPortalTracks.java`](https://github.com/Creators-of-Create/Create/blob/7ff2760e4924d35d4bc2e22c4b19923f0178cd16/src/main/java/com/simibubi/create/content/trains/track/AllPortalTracks.java)).
  A carriage keeps one `DimensionalCarriageEntity` per dimension it straddles
  ([`Carriage.java`](https://github.com/Creators-of-Create/Create/blob/7ff2760e4924d35d4bc2e22c4b19923f0178cd16/src/main/java/com/simibubi/create/content/trains/entity/Carriage.java)).
- **Unloaded chunks.** Trains are simulated, not ticked as entities.
  - `GlobalRailwayManager.tick` runs only when the level is the Overworld, and from there ticks every train
    in every dimension
    ([`GlobalRailwayManager.java`](https://github.com/Creators-of-Create/Create/blob/7ff2760e4924d35d4bc2e22c4b19923f0178cd16/src/main/java/com/simibubi/create/content/trains/GlobalRailwayManager.java),
    called from `CommonEvents.onServerWorldTick`).
  - `Carriage.manageEntities` creates the carriage entity only where
    `ServerLevel.isPositionEntityTicking` is true, and removes and saves it when it leaves ticking chunks
    ([`CarriageEntityHandler.java`](https://github.com/Creators-of-Create/Create/blob/7ff2760e4924d35d4bc2e22c4b19923f0178cd16/src/main/java/com/simibubi/create/content/trains/entity/CarriageEntityHandler.java)).
  - The train therefore keeps moving along the track graph through unloaded chunks as data. Its cargo moves
    with it, but loading and unloading need a loaded station, since PSIs are block entities that work with a
    spawned carriage.
  - The schedule condition "station unloaded" waits until the current station's chunk is no longer entity
    ticking, made for routes whose far end is unloaded
    ([`StationUnloadedCondition.java`](https://github.com/Creators-of-Create/Create/blob/7ff2760e4924d35d4bc2e22c4b19923f0178cd16/src/main/java/com/simibubi/create/content/trains/schedule/condition/StationUnloadedCondition.java)).
  - Create has no chunk-loading code: no `TicketType`, `forceChunk` or ticket controller under `src/main/java`.

Levers: the build cost is the infrastructure itself (track over the whole distance), capacity is what you build
onto the train, there is no running cost, and travel is simulated while transfers happen only at loaded ends.

## Mekanism

Version: Mekanism 10.8.0 for MC 26.1.2, branch
[`26.1`](https://github.com/mekanism/Mekanism/tree/26.1). The 1.21.x branch (10.7.19, MC 1.21.1) is given
for comparison. Code was read on branch `26.3` and the cited lines checked against `26.1`.

### Teleporter

- **What it moves.** Entities in the frame. `getToTeleport` collects every `Entity` in the teleport bounds
  that is not a spectator, a part entity, tagged `teleporting_not_supported`, or unable to use portals
  ([`TileEntityTeleporter.java`](https://github.com/mekanism/Mekanism/blob/26.1/src/main/java/mekanism/common/tile/TileEntityTeleporter.java)).
  That includes dropped item entities, so an item can travel by dropping it into the frame (read from the
  source, not tested in game).
- **Cost per entity**, from `calculateEnergyCost`:
  - The cost is `base + round(distance x distanceCost)`. Across dimensions it adds the flat
    `dimensionPenalty`, and the distance is computed with the dimensions' coordinate scales to find the
    cheaper ordering.
  - The result is multiplied by `(1 + passengers)`.
  - Defaults, 26.1
    ([`UsageConfig.java`](https://github.com/mekanism/Mekanism/blob/26.1/src/main/java/mekanism/common/config/UsageConfig.java)):
    `base` 100, `distance` 2 FE per block, `dimensionPenalty` 1000.
  - Defaults, 1.21.x
    ([`UsageConfig.java`](https://github.com/mekanism/Mekanism/blob/1.21.x/src/main/java/mekanism/common/config/UsageConfig.java)):
    `base` 1000, `distance` 10, `dimensionPenalty` 10,000.
- **Buffer.** The teleporter's energy storage defaults to 1,000 on 26.1 and 5,000,000 on 1.21.x
  ([`StorageConfig.java` 26.1](https://github.com/mekanism/Mekanism/blob/26.1/src/main/java/mekanism/common/config/StorageConfig.java)).
  `canTeleport` sums the cost of every entity and fails if the buffer cannot supply it. With the 26.1
  defaults, a same-dimension jump of more than about 450 blocks (100 + 2 x 450 = 1000) would not fit the
  buffer, and a cross-dimension jump would almost never fit. This reading is **unverified in game**; the
  rebalance may be intentional or may be scaled elsewhere.
- **Build.** Teleporter: 4 basic circuits, 4 steel casings, 1 teleportation core. Teleporter frame (x9): 8
  refined obsidian + 1 refined glowstone
  ([`MekanismRecipeProvider.java`](https://github.com/mekanism/Mekanism/blob/26.1/src/datagen/main/java/mekanism/common/recipe/impl/MekanismRecipeProvider.java)).
  Teleporters pair through a frequency.
- **Chunks.** The teleporter is an `IChunkLoader` and accepts only the Anchor Upgrade
  (`AttributeUpgradeSupport.ANCHOR_ONLY` in `MekanismBlockTypes`). With the upgrade it holds chunk tickets,
  if `allowChunkloading` is on (`TileComponentChunkLoader.canOperate`). Whether an unloaded destination works
  without its own anchor is **unverified**.

### Quantum entangloporter

- **What it moves.** Each frequency holds one item slot, one fluid tank, one chemical tank, an energy
  container and a heat capacitor
  ([`InventoryFrequency.java`](https://github.com/mekanism/Mekanism/blob/26.1/src/main/java/mekanism/common/content/entangloporter/InventoryFrequency.java)).
  Every entangloporter on the frequency shares them, so it works as a shared buffer that crosses dimensions.
- **Buffers**, from
  [`GeneralConfig.java`](https://github.com/mekanism/Mekanism/blob/26.1/src/main/java/mekanism/common/config/GeneralConfig.java)
  (`quantum_entangloporter`): energy is an ultimate energy cube's capacity, fluid an ultimate tank's (256
  buckets on 26.3), and chemical an ultimate chemical tank's. Throughput is limited by the pipes and cables
  connected at each end, and items go through a single slot.
- **Build.** 4 refined obsidian, 2 ultimate circuits, 2 atomic alloy, 1 teleportation core
  (`MekanismRecipeProvider.java`, same file).
- **Running cost.** None found in source.
- **Chunks.** `IChunkLoader`, so the Anchor Upgrade can chunk-load each end
  ([`TileEntityQuantumEntangloporter.java`](https://github.com/mekanism/Mekanism/blob/26.1/src/main/java/mekanism/common/tile/TileEntityQuantumEntangloporter.java)).

### Logistical transporter (baseline)

Tiers in
[`TransporterTier.java`](https://github.com/mekanism/Mekanism/blob/26.1/src/main/java/mekanism/common/tier/TransporterTier.java)
(pull amount, base speed): basic (1, 5), advanced (16, 10), elite (32, 20), ultimate (64, 50). A source comment
says the unit of speed is unclear ("configured as per half second??"). These are physical pipes: the same
dimension, and the chunks must be loaded.

## Applied Energistics 2

Version: AE2 branch [`26.1`](https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/26.1) (MC 26.1);
guide text and code were also identical on `main` (26.3). The guide is the in-repo `guidebook/` that
guide.appliedenergistics.org renders.

- **Quantum network bridge.** It extends a network over any distance and between dimensions, carrying 32
  channels in total, like a wireless dense cable. "Both sides must be loaded", so a Spatial Anchor or other
  chunk loader is needed when the ends are far apart.
  ([`quantum_bridge.md`](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/26.1/guidebook/items-blocks-machines/quantum_bridge.md))
  - Structure: 8 quantum rings around 1 quantum link chamber per side. The chamber holds one quantum entangled
    singularity.
  - `QuantumCluster.canUseNode` logs a warning when the partner's chunk is unloaded
    ([`QuantumCluster.java`](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/26.1/src/main/java/appeng/me/cluster/implementations/QuantumCluster.java)).
- **Running cost.** Each `QuantumBridgeBlockEntity` sets an idle draw of 22 AE/t
  ([`QuantumBridgeBlockEntity.java`](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/26.1/src/main/java/appeng/blockentity/qnb/QuantumBridgeBlockEntity.java)).
  The cost does not scale with distance or dimension.
- **Build gate.** A singularity takes 256,000 items or buckets in a condenser. An explosion near a singularity
  and an ender pearl (or ender dust) makes a matched pair of entangled singularities
  ([`singularities.md`](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/26.1/guidebook/items-blocks-machines/singularities.md)).
  The cost is paid once, up front, and is large.
- **Spatial Anchor.** It force-loads the chunks its network occupies, and the loading spreads across quantum
  bridges but not across dimensions, so each dimension needs its own anchor. Draw: e = 80 + x(x+1)/2 AE/t for
  x chunks, which grows quadratically with footprint. By default it also enables random ticks
  (`spatialAnchorEnableRandomTicks`, true, in `AEConfig.java`).
  ([`spatial_anchor.md`](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/26.1/guidebook/items-blocks-machines/spatial_anchor.md))
- **P2P tunnels.** One-way links between two block faces. Variants carry ME channels, items, fluids, FE,
  redstone or light. FE tunnels charge a 2.5% tax as extra AE draw. ME P2P channels cannot nest. Ends are
  linked with a memory card frequency. A P2P link works at range within a network, and across dimensions only
  when the network spans them through a quantum bridge.
  ([`p2p_tunnels.md`](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/26.1/guidebook/items-blocks-machines/p2p_tunnels.md))
- An AE2 network needs to be chunk-loaded for any of its devices to work (spatial_anchor.md).

## Modular Routers

Version: 13.2.7 for MC 1.21.1, branch
[`MC1.21-master`](https://github.com/desht/ModularRouters/tree/MC1.21-master).

- **Range.** From
  [`CommonConfig.java`](https://github.com/desht/ModularRouters/blob/MC1.21-master/src/main/java/me/desht/modularrouters/config/CommonConfig.java):
  Sender Mk1 has a base range of 8 (16 max with range upgrades), and Sender Mk2 24 (48 max). Sender Mk3 is not
  range-limited (`isRangeLimited() = false`) and works across dimensions, subject to a dimension blacklist
  ([`SenderModule3.java`](https://github.com/desht/ModularRouters/blob/MC1.21-master/src/main/java/me/desht/modularrouters/item/module/SenderModule3.java),
  [`CompiledModule.isTargetValid`](https://github.com/desht/ModularRouters/blob/MC1.21-master/src/main/java/me/desht/modularrouters/logic/compiled/CompiledModule.java)).
- **Throughput.** The router runs every `baseTickRate` = 20 ticks, 2 ticks faster per speed upgrade, down to
  `hardMinTickRate` 2. Items per operation are 2^n for n stack upgrades, capped at 64
  (`ModularRouterBlockEntity.java`, `CompiledModule.getItemsPerTick`).
- **Build.** Sender Mk3 = Sender Mk2 + end stone + ender chest, shapeless
  ([recipe](https://github.com/desht/ModularRouters/blob/MC1.21-master/src/generated/resources/data/modularrouters/recipe/sender_module_3.json)).
- **Running cost.** `senderModule1/2/3EnergyCost` default to 0. Energy is opt-in, set by the pack maker.
- **Chunks.** The target inventory is reached through NeoForge's `BlockCapabilityCache`, which returns no
  capability when the position is not loaded
  ([`BlockCapabilityCache.java`](https://github.com/neoforged/NeoForge/blob/1.21.x/src/main/java/net/neoforged/neoforge/capabilities/BlockCapabilityCache.java)).
  The router is a ticking block entity, so both ends must be loaded. The mod does no chunk loading.

## Drone mods

### PneumaticCraft: Repressurized

Version: 8.2.23 for MC 1.21.1, branch [`1.21`](https://github.com/TeamPneumatic/pnc-repressurized/tree/1.21).
This is the notable Factorio-like drone logistics mod with a verifiable source.

- **Logistics drones** work on inventories and tanks that carry a Logistics Frame (requester, provider and
  similar) within a 33x33x33 cube centred where the drone was deployed. They run on pressurised air and seek a
  charging station when low.
  ([`logistics_drone.json`](https://github.com/TeamPneumatic/pnc-repressurized/blob/1.21/src/main/resources/assets/pneumaticcraft/patchouli_books/book/en_us/entries/logistics/logistics_drone.json))
- **Capacity.** 1 item slot + 1 per inventory upgrade. The fluid tank is 16,000 mB x (1 + upgrades)
  (`DroneEntity.java`).
- **Air costs** ([`PneumaticValues.java`](https://github.com/TeamPneumatic/pnc-repressurized/blob/1.21/src/main/java/me/desht/pneumaticcraft/lib/PneumaticValues.java)):
  10 per stack moved, 10,000 per teleport, and 5 per tick per Chunkloader Upgrade (`DRONE_USAGE_CHUNKLOAD`).
- **Chunks.** From
  [`CommonConfig.java`](https://github.com/TeamPneumatic/pnc-repressurized/blob/1.21/src/main/java/me/desht/pneumaticcraft/common/config/CommonConfig.java):
  - `allow_navigate_to_unloaded_chunks` is false by default; the comment warns of performance and stability
    problems when it is enabled.
  - A drone with no path teleports, and `max_drone_teleport_range` 0 means unlimited.
  - `chunkload_offline_time` 0 lets a drone keep chunk-loading after its owner logs out.
- **Amadron.** The Amadron Tablet orders items for delivery: one drone spawns to collect the payment from a
  bound inventory, and a second delivers the goods. Player-to-player trades use GPS-set locations.
  ([`amadron_tablet.json`](https://github.com/TeamPneumatic/pnc-repressurized/blob/1.21/src/main/resources/assets/pneumaticcraft/patchouli_books/book/en_us/entries/tools/amadron_tablet.json))
  This is a "drone arrives from offscreen" delivery: the drone is spawned near the destination, not flown from
  the source.

### Others

- Create Aeronautics, "Logistic Drones" mods and similar: no primary source was checked. **Unverified**, and
  not included.
- Integrated Dynamics / Integrated Tunnels: not covered. Their networks are cable-bound and offer no
  long-range or cross-dimension link of their own (**unverified**).

## Thermal series

- **Tesseract (Thermal Expansion 4, MC 1.7.10).** It existed: the 4.x changelog mentions tesseract frames and a
  tesseract shader ([`thermalexpansion_changelog.txt`](https://github.com/CoFH/Version/blob/master/thermalexpansion_changelog.txt)).
  TE4's source is not in CoFH's public repositories (the earliest public TE is the 1.12 legacy repository,
  which has no tesseract), so what it moved, what it cost and how it handled chunks are **unverified**. King
  Lemming's 2017 post says tesseracts were not brought back, with a design doc still pending
  ([`2017-10-11-whats-next.md`](https://github.com/CoFH/cofh.github.io/blob/master/_posts/2017-10-11-whats-next.md)).
- **Thermal Dynamics 1.12.** The warp itemduct moves items almost instantly while its ender energy grid is
  powered. It takes `TDProps.ENDER_TRANSMIT_COST` = 50 RF per multi-advance, and without power it behaves like
  a 60-length duct
  ([`DuctUnitItemWarp.java`](https://github.com/CoFH/ThermalDynamics-1.12-Legacy/blob/1.12/src/main/java/cofh/thermaldynamics/duct/item/DuctUnitItemWarp.java),
  [`TDProps.java`](https://github.com/CoFH/ThermalDynamics-1.12-Legacy/blob/1.12/src/main/java/cofh/thermaldynamics/init/TDProps.java)).
  It is a physical duct: one dimension, and the chunks must be loaded. Ender fluxducts and fluiducts also
  exist (`DuctUnitEnergyEnder`, `DuctUnitFluidEnder`). Viaducts (`DuctUnitTransportLongRange`) carry players.
- **Current Thermal (1.20.x).** No ender transport ships.
  - The `ender_tunnel` render registration is commented out in
    [`ThermalDynamics.java`](https://github.com/CoFH/ThermalDynamics/blob/1.20.x/src/main/java/cofh/thermal/dynamics/ThermalDynamics.java).
  - ThermalCore's lang has only a search keyword for it ("entangloporter quantum tesseract") and the name
    "Ender Frequency Tuner" for a dynamo augment
    ([`en_us.json`](https://github.com/CoFH/ThermalCore/blob/1.20.x/src/main/resources/assets/thermal/lang/en_us.json)).
  - Code search found no implementation of either. "Thermal ender tuning / ender augments" are **unverified**,
    and probably unreleased.

## Ender IO

- **Current rewrite** (branch [`1.21.1`](https://github.com/Team-EnderIO/EnderIO/tree/1.21.1)). Travel anchors
  and the travel staff teleport players only. Ranges from
  [`ItemsConfig.java`](https://github.com/Team-EnderIO/EnderIO/blob/1.21.1/enderio/src/main/java/com/enderio/enderio/config/base/common/ItemsConfig.java):
  `blockToBlockRange` 96, `itemToBlockRange` 192, `blinkRange` 24. The staff uses 1000 energy per use.
  - The rewrite has transceiver GUI textures but no transceiver Java class, so the dimensional transceiver has
    not been ported.
- **1.12 dimensional transceiver** (branch [`master`](https://github.com/SleepyTrousers/EnderIO/tree/master)).
  - It sends power, items and fluids over named channels and has 8 input and 8 output slots
    ([`TileTransceiver.java`](https://github.com/SleepyTrousers/EnderIO/blob/master/enderio-machines/src/main/java/crazypants/enderio/machines/machine/transceiver/TileTransceiver.java)).
  - Upkeep is `block_transceiver/use` base 10 (quadratic capacitor scaling), with a 500,000 buffer and 40,960
    intake
    ([`capacitor_machines.xml`](https://github.com/SleepyTrousers/EnderIO/blob/master/enderio-machines/src/main/resources/assets/enderio/config/recipes/capacitor_machines.xml)).
  - It deregisters from the `TransceiverRegistry` on `onChunkUnload`, so an unloaded end drops out of the
    channel.
  - The cost is a continuous upkeep, not a per-item or per-distance charge.

## Draconic Evolution

Version: 3.1.4 for MC 1.21.1, branch [`1.21`](https://github.com/Draconic-Inc/Draconic-Evolution/tree/1.21).

- **Dislocators.** These are player-carried teleport items.
  - The basic dislocator loses 1 durability per use (`Dislocator.java`).
  - The advanced dislocator burns ender-pearl fuel: `dislocatorBlinksPerPearl` 4, `dislocatorMaxFuel` 1024,
    `dislocatorBlinkRange` 32, `dislocatorBlinkCooldown` 5 ticks
    ([`DEConfig.java`](https://github.com/Draconic-Inc/Draconic-Evolution/blob/1.21/src/main/java/com/brandon3055/draconicevolution/DEConfig.java)).
  - Bound and P2P dislocators teleport living entities across dimensions (`BoundDislocator.java`).
- **Dislocator receptacle.** It builds a portal and teleports any entity that touches it to the inserted
  dislocator's target
  ([`TileDislocatorReceptacle.java`](https://github.com/Draconic-Inc/Draconic-Evolution/blob/1.21/src/main/java/com/brandon3055/draconicevolution/blocks/tileentity/TileDislocatorReceptacle.java)).
  It can also link an energy crystal through the portal (`ICrystalLink`). No per-teleport fuel use was found
  in the receptacle tick. Item-entity transport and chunk behaviour were not checked: **unverified**.
- DE has no dedicated long-range item mover; its wireless energy crystals are an energy network.

## Ender Storage

Version: 2.14.0 for MC 1.21.11, branch [`master`](https://github.com/TheCBProject/EnderStorage).

- Ender chests and tanks share an inventory by colour frequency, across any distance and any dimension. The
  size is `item_storage_size`, default 1 = 27 slots (0 = 9, 2 = 54)
  ([`EnderStorageConfig.java`](https://github.com/TheCBProject/EnderStorage/blob/master/src/main/java/codechicken/enderstorage/config/EnderStorageConfig.java),
  [`EnderItemStorage.java`](https://github.com/TheCBProject/EnderStorage/blob/master/src/main/java/codechicken/enderstorage/storage/EnderItemStorage.java)).
- The storage lives in the world save under `EnderStorage/`, outside any chunk
  ([`EnderStorageManager.java`](https://github.com/TheCBProject/EnderStorage/blob/master/src/main/java/codechicken/enderstorage/manager/EnderStorageManager.java)).
  The inventory therefore exists while both chests are unloaded. Moving items in or out still needs a hopper
  or pipe in a loaded chunk.
- No running cost, and no throughput cap beyond the pipes at each end.

## RFTools matter transmitter

Version: RFTools Utility, branch [`1.21_neo`](https://github.com/McJtyMods/RFToolsUtility/tree/1.21_neo).
Player teleportation: the config comments describe "the player" standing in the beam. Defaults in
[`TeleportConfiguration.java`](https://github.com/McJtyMods/RFToolsUtility/blob/1.21_neo/src/main/java/mcjty/rftoolsutility/modules/teleporter/TeleportConfiguration.java):

- Start cost: `rfStartTeleportLocal` 5,000 + `rfStartTeleportDist` 10 per block in the same dimension, or a
  flat `rfStartTeleportDim` 100,000 across dimensions (distance ignored).
- During the teleport: `rfTeleportPerTick` 500. On arrival: `rfPerTeleportReceiver` 5,000 at the receiver.
- Time: `timeTeleportBaseLocal` 5 ticks + `timeTeleportDist` 10 milliticks per block, or
  `timeTeleportBaseDim` 50 ticks across dimensions.
- Buffers: transmitter 200,000, receiver 100,000.

Chunk handling: **unverified**.

## Patterns for a void-paid, unattended mechanism

### Recurring levers

1. **Where the cost falls.**
   - *Per trip*: Factorio rockets pay 50 parts per launch, however full the rocket. RFTools pays a start cost.
     Mekanism's teleporter is in effect per trip too, since it charges per entity and one item entity can be
     a whole stack.
   - *Per item*: Factorio robots carry 1 to 4 items per flight and pay energy per tile.
   - *Upkeep*: AE2 bridges (22 AE/t), the Ender IO transceiver (10 RF/t), PNC chunkloader upgrades (5 air/t).
   - *None*: Ender Storage, the entangloporter, Create trains, and Modular Routers by default.

   Per-trip pricing rewards filling the carrier (Factorio's minimum payload, Create's "cargo idle" waits).
   Upkeep rewards few, busy links. A void cost could be per trip with a weight cap, which reproduces the
   rocket.
2. **Distance and dimension scaling.**
   - Mekanism charges linearly with distance plus a flat dimension penalty, and accounts for coordinate scale.
   - RFTools charges by distance locally but flat across dimensions.
   - AE2, Ender Storage, the entangloporter and Modular Routers Mk3 ignore distance completely.
   - Factorio's only distance cost is travel time and thruster fuel. Its leg costs are asymmetric: up is
     expensive (rocket parts) and down is free (cargo pods).
3. **Capacity and weight.** Factorio is the only one with a weight model (1000 kg lift, recipe-derived item
   weight). The rest cap slots (Ender Storage 27, entangloporter 1 slot) or the rate (Modular Routers up to 64
   items per 2 ticks), or leave capacity to the player's build (Create vaults, PNC inventory upgrades). Weight
   makes dense, high-tier goods cheap to ship and bulk raw goods expensive, which pushes processing to happen
   on site.
4. **Build-cost gating.** The expensive mechanisms gate entry with one big crafted cost: AE2's 256,000-item
   singularity, Mekanism's teleportation core with ultimate circuits and atomic alloy, the Factorio silo and
   landing pad (one per planet). Cheap ones (Ender Storage, Sender Mk3 = Mk2 + end stone + ender chest) end up
   as default logistics, and pack makers often rebalance them.
5. **Chunk-loading strategies.** Four were found:
   - *Require both ends loaded and offer a loader*: AE2 with the Spatial Anchor, whose draw grows
     quadratically with chunks.
   - *Built-in loader upgrade*: Mekanism's Anchor Upgrade on the teleporter and entangloporter, and PNC's
     Chunkloader Upgrade, which costs air per tick.
   - *Do nothing*: Modular Routers and the Ender IO transceiver, which silently stop when an end unloads.
   - *Simulate in transit and act only at loaded ends*: Create trains, which carry on as track-graph data, and
     Ender Storage, whose inventory lives in the save.

   Factorio never faces the problem, because it keeps active chunks running.
6. **Virtual versus physical transit.**
   - Physical, but simulated off-screen: Create trains, Factorio robots and platforms.
   - Physical and entity-bound: PNC drones, which by default refuse unloaded chunks.
   - Virtual: Mekanism, AE2, Ender Storage, Modular Routers, the Ender IO transceiver.

   Virtual movers have no transit risk and no latency. Physical ones carry latency, and in Factorio
   interception too (asteroids).
7. **Latency.** Factorio platforms travel and must park over the planet. Create trains travel at 28 to 40 b/s.
   RFTools takes 5 ticks + distance (50 ticks across dimensions). Everything else is instant.

### What this means for a mover that costs void and runs with chunks unloaded

- **Simulate the transit; never tick an entity across unloaded terrain.** No mod surveyed moves an entity
  through unloaded chunks on purpose. PNC disables it by default and warns of instability. Create, the only
  mod that runs long routes unattended, keeps the train as saved data ticked from the Overworld and spawns
  the entity only in entity-ticking chunks. A Void Dragon, train or drone should work the same way: a record
  with a route, departure tick, arrival tick and cargo, advanced by a server-level tick, with a visual entity
  spawned only where a player can see it.
- **The ends still need a loaded chunk to transfer, or an inventory that lives outside the world.** Every
  mechanism that hands items to block inventories needs that block loaded (Create's PSI, Modular Routers,
  AE2, Ender IO). There are two ways round it.
  - Hold the cargo in saved data, as Ender Storage does, so arrival is just a write to a virtual buffer, which
    automation drains once the chunk loads.
  - Chunk-load the endpoints, and charge for it, as Mekanism's Anchor Upgrade and AE2's Spatial Anchor do. A
    void upkeep per loaded chunk fits AE2's 80 + x(x+1)/2 shape.
  - A docking station with a virtual inbox, filled on arrival whether loaded or not, combines the two and
    avoids chunk-loading altogether.
- **Price per trip with a capacity cap, scaled by distance and with a dimension surcharge.** A void price per
  trip (`base + k x distance`, plus a flat cross-dimension penalty, as in Mekanism and RFTools), with a
  Factorio-style weight or slot cap per trip, keeps the player choosing between filling a carrier and
  sending it now. A per-item price is simpler to reason about but loses that choice. Asymmetric legs, as in
  Factorio (expensive out, cheap back), are an option if the suite wants an "export from home" direction.
- **Physical versus virtual.** Teleport (virtual, instant) is the common Minecraft answer, and it makes
  distance mean nothing except as a price. A physical mover (dragon, train, drone) adds what Factorio uses
  to make logistics a design problem: latency, carrier count as the throughput limit, and possibly risk. Under
  the hood both become the same record. The difference is whether the record has a travel time and a capacity
  per carrier, and whether a visual entity is shown on the way. A simulated physical mover costs little more
  to build than a teleport and keeps the Factorio feel. A pure teleport is simplest, but leaves only the void
  price to balance it.
- **Gate entry with a large one-off build cost** (an AE2-style singularity, Factorio's silo and landing pad),
  and keep the running cost in void, so the void economy stays the main lever rather than infrastructure
  spread across the whole route, as with Create track.

## Sources

Factorio (wiki and Lua API docs, Factorio 2.0 / Space Age):
- https://wiki.factorio.com/Logistic_robot
- https://wiki.factorio.com/Roboport
- https://wiki.factorio.com/Rocket_silo
- https://wiki.factorio.com/Space_platform
- https://wiki.factorio.com/Space_platform_foundation
- https://wiki.factorio.com/Thruster
- https://wiki.factorio.com/Cargo_landing_pad
- https://wiki.factorio.com/Map_structure
- https://lua-api.factorio.com/latest/auxiliary/item-weight.html

Create (6.0.11, MC 1.21.1, commit 7ff2760):
- https://github.com/Creators-of-Create/Create/tree/7ff2760e4924d35d4bc2e22c4b19923f0178cd16 — `infrastructure/config/CTrains.java`,
  `CLogistics.java`, `content/trains/GlobalRailwayManager.java`, `entity/Train.java`, `entity/Carriage.java`,
  `entity/CarriageEntityHandler.java`, `track/AllPortalTracks.java`,
  `schedule/condition/StationUnloadedCondition.java`, `foundation/events/CommonEvents.java`

Mekanism (10.8.0 MC 26.1.2; 10.7.19 MC 1.21.1):
- https://github.com/mekanism/Mekanism/tree/26.1 — `common/config/UsageConfig.java`, `StorageConfig.java`,
  `GeneralConfig.java`, `common/tile/TileEntityTeleporter.java`, `TileEntityQuantumEntangloporter.java`,
  `common/tile/component/TileComponentChunkLoader.java`, `common/tier/TransporterTier.java`,
  `common/content/entangloporter/InventoryFrequency.java`, `datagen/.../MekanismRecipeProvider.java`
- https://github.com/mekanism/Mekanism/tree/1.21.x — `UsageConfig.java`, `StorageConfig.java`

Applied Energistics 2 (branch 26.1):
- https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/26.1 — `guidebook/items-blocks-machines/quantum_bridge.md`,
  `singularities.md`, `spatial_anchor.md`, `p2p_tunnels.md`, `appeng/blockentity/qnb/QuantumBridgeBlockEntity.java`,
  `appeng/me/cluster/implementations/QuantumCluster.java`, `appeng/core/AEConfig.java`

Modular Routers (13.2.7, MC 1.21.1):
- https://github.com/desht/ModularRouters/tree/MC1.21-master — `config/CommonConfig.java`,
  `item/module/SenderModule3.java`, `logic/compiled/CompiledModule.java`, `logic/ModuleTarget.java`,
  `block/tile/ModularRouterBlockEntity.java`, `src/generated/.../recipe/sender_module_3.json`
- https://github.com/neoforged/NeoForge/blob/1.21.x/src/main/java/net/neoforged/neoforge/capabilities/BlockCapabilityCache.java

PneumaticCraft: Repressurized (8.2.23, MC 1.21.1):
- https://github.com/TeamPneumatic/pnc-repressurized/tree/1.21 — `common/config/CommonConfig.java`,
  `lib/PneumaticValues.java`, `common/entity/drone/DroneEntity.java`, Patchouli entries
  `logistics/logistics_drone.json`, `tools/amadron_tablet.json`

Thermal (CoFH):
- https://github.com/CoFH/Version/blob/master/thermalexpansion_changelog.txt
- https://github.com/CoFH/cofh.github.io/blob/master/_posts/2017-10-11-whats-next.md
- https://github.com/CoFH/ThermalDynamics-1.12-Legacy/tree/1.12 — `duct/item/DuctUnitItemWarp.java`, `init/TDProps.java`
- https://github.com/CoFH/ThermalDynamics/blob/1.20.x/src/main/java/cofh/thermal/dynamics/ThermalDynamics.java
- https://github.com/CoFH/ThermalCore/blob/1.20.x/src/main/resources/assets/thermal/lang/en_us.json

Ender IO:
- https://github.com/Team-EnderIO/EnderIO/tree/1.21.1 — `enderio/.../config/base/common/ItemsConfig.java`
- https://github.com/SleepyTrousers/EnderIO/tree/master — `enderio-machines/.../transceiver/TileTransceiver.java`,
  `capacitor/CapacitorKey.java`, `resources/.../recipes/capacitor_machines.xml`

Draconic Evolution (3.1.4, MC 1.21.1):
- https://github.com/Draconic-Inc/Draconic-Evolution/tree/1.21 — `DEConfig.java`, `items/tools/Dislocator.java`,
  `items/tools/BoundDislocator.java`, `blocks/tileentity/TileDislocatorReceptacle.java`

Ender Storage (2.14.0, MC 1.21.11):
- https://github.com/TheCBProject/EnderStorage — `config/EnderStorageConfig.java`, `storage/EnderItemStorage.java`,
  `manager/EnderStorageManager.java`

RFTools Utility (branch 1.21_neo):
- https://github.com/McJtyMods/RFToolsUtility/blob/1.21_neo/src/main/java/mcjty/rftoolsutility/modules/teleporter/TeleportConfiguration.java
