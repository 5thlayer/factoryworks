# What Fieldworks, Craftworks and Beltworks let a companion read and do

Research for [#683](https://github.com/5thlayer/factoryworks/issues/683), under the map
[#679](https://github.com/5thlayer/factoryworks/issues/679). It is raw material for the companion's
tool surface: what the mods already expose, and what would need a new hook.

**Sources.** These are the public mod checkouts in `~/minecraft_mods/` as they stand on 2026-10-12,
plus this repo's `GLOSSARY.md` and `docs/adr/`. Path prefixes used below:

| Prefix | Path |
|---|---|
| `FW/` | `~/minecraft_mods/fieldworks/src/main/java/io/github/_5thlayer/fieldworks/` |
| `CW/` | `~/minecraft_mods/craftworks/src/main/java/io/github/_5thlayer/craftworks/` |
| `BW/` | `~/minecraft_mods/beltworks/src/main/java/io/github/_5thlayer/beltworks/` |
| `GW/` | `~/minecraft_mods/groundworks/src/main/java/io/github/_5thlayer/groundworks/` |
| `LW/` | `~/minecraft_mods/libworks/runtime/src/main/java/io/github/_5thlayer/libworks/` |

Where a finding is an inference rather than something the code states, it says so.

## Headlines

1. **No Craftworks machine smelts with fuel.** Craftworks' only smelter is the **Refiner**. It is
   an electric furnace that runs on FE and has no fuel slot (`CW/machine/RefinerSlots.java`:
   `INPUT=0`, `OUTPUT=1`). The stone and steel furnaces were dropped in ADR-0127
   (`docs/adr/0127-fieldworks-holds-the-resources-and-no-module-requires-another.md`, "What goes").
   Wireworks is out of the map's scope, so the "smelt" step needs one of these:
   - a vanilla furnace;
   - Craftworks' creative energy source (`CW/machine/CreativeEnergySourceBlockEntity.java`);
   - a decision that brings power in.

   **This is a question for the map.**
2. **A belt cannot deliver into a chest by itself.** Belt tiles, splitters and loaders register no
   item capability; only loaders and feeders register energy (`BW/Beltworks.java`, around line 149).
   That is deliberate (Beltworks ADR 0008, `~/minecraft_mods/beltworks/docs/adr/0008-*`).
   - A line's end hands items only to a loader facing back at it, a splitter, or the side of
     another tile (`BW/blocks/BeltOutlet.java`).
   - So the loop is: Harvester → belt → **loader** → chest. The loader goes in front of the chest
     with its mouth toward the belt.
3. **The Harvester pushes its output into the block two ahead of its origin.** That is
   `worldPosition.relative(FACING, 2)`, the block just outside its front face
   (`FW/harvester/HarvesterBlockEntity.java`, line 392).
   - It pushes only into an item or fluid handler. It never drops items in the world.
   - A belt tile has no handler, so a Harvester cannot feed a belt directly. Something in front of
     it has to take the output: a chest plus a loader, a loader, or a feeder. **This needs checking
     in game** (see "Open points").
4. **Placement takes a `BlockPlaceContext` everywhere.** No method places a machine from
   (level, pos, facing). The game tests place things with a `FakePlayer` that has a set `yRot` and
   calls `gameMode.useItemOn`. A companion body that is a real player entity can do the same.
5. **No mod posts server-side events or registers commands.**
   - State can be read by polling public block-entity getters on the server, or through the
     standard NeoForge capabilities.
   - Jade plugins already serialise most of it. The Jade plugins are `FW/compat/HarvesterJadePlugin.java`,
     `CW/compat/jade/RefinerReport.java`, `CW/compat/jade/AssemblerReport.java` and
     `BW/compat/BeltTileJadePlugin.java`.

## Fieldworks: the Harvester

There is one block entity type, `fieldworks:harvester`, used by two tiers:

- `harvester_1`: burner, takes fuel;
- `harvester_2`: electric, takes FE.

`FW/harvester/HarvesterBlockEntity.java` is a public final class.

### Read

The main read API is **`status()`**, which returns a public `HarvesterStatus` record
(`FW/harvester/HarvesterStatus.java`). Its fields:

- whether it takes fuel or energy;
- `burnLeft`, `burnTotal`, `burnPerOperation`;
- `energy` and capacity;
- `progress` and `operationTicks`;
- `idle` (an `Optional<IdleReason>`);
- the Drilling Fluid tank, the product (crude) tank, and the tank capacity.

The record also has `progressFraction()`, `burnFraction()` and `burnOperations()`.

Other public methods:

- `idleReason()`;
- `heldRecipe()`;
- `heldView(ServerLevel)`;
- `serverTick(ServerLevel)`, which the game tests call directly.

**Idle reasons.** `OperationChoice.IdleReason` (`FW/harvester/OperationChoice.java`) is
`NOTHING_TO_WORK`, `NOTHING_MATCHING`, `TIER_TOO_LOW`, `NO_FLUID`, `NO_POWER` or `OUTPUT_FULL`.
An empty `idle` means it is working. Translated lines come from
`FW/harvester/HarvesterStatusLines.java`.

**Slot contents** can be read only through the item capability. The accessors `bufferSlots()`,
`fuelSlots()` and `itemFace()` are package-private.

**Capabilities.** They are registered in `Harvesters.registerCapabilities`
(`FW/harvester/Harvesters.java`, lines 102–104). Each ignores the side, and Groundworks forwards
all 27 footprint positions to the origin (`GW/Footprint.java`, `forwardCapabilities`).

- **Item.** Slot 0 is fuel: insert only, and only on a tier that burns fuel. Slots 1–2 are
  fluid-container fill and drain. Slots 3–11 are the output buffer: extract only.
- **Fluid.** Box 0 is the Drilling Fluid tank: insert only. Box 1 is crude: extract only.
- **Energy.** Present only on a tier that takes FE.

**Mining area.** It is the 3×3 layer directly under the footprint, `origin.below()` ±1 in x and z,
and its order does not depend on facing. It is computed by the private `layer()`, and the sources in
it are classified by the package-private `HarvestSources`. If a well is anywhere in the layer, only
wells are worked.

**Ore remaining.** `FW/ore/OreMining.remaining(Level, BlockPos, OreBlock)` and
`FW/ore/PatchLedgerData` give the totals left in each patch.

**No client sync.** There is no `getUpdateTag` or `sendBlockUpdated`. A client sees the state only
through an open `HarvesterMenu` (`FW/harvester/HarvesterMenu.java`, 15 `ContainerData` slots) or
through Jade. A companion that reads on the server does not have this limit.

### Do

**Placement.** It is a 3×3×3 footprint with its origin at the bottom centre
(`FootprintShape.square(3, 3)` in `Harvesters`). It faces the player who places it
(`GW/Footprint.java`, line 202: `getHorizontalDirection().getOpposite()`).

- It is refused only when a position is out of the world, cannot be replaced, or has an entity in
  it (`Footprint.fits`, line 220).
- Nothing checks that there is ore underneath. A Harvester over plain stone places fine and idles
  with `NOTHING_TO_WORK`, so the companion has to check the 3×3 below for ore itself.

**What it needs to run.** The checks run in this order (`serverTick`, `OperationChoice`):

1. A loaded tier file. Without one it returns silently.
2. If a Held recipe is set, it must still exist, and the tier rank must be high enough for it.
3. A workable source in the layer.
4. Drilling Fluid, if the Held recipe names one.
5. Room in the buffer or the tank.
6. Power: a lit burn, enough FE, or a fuel item it can light.

Note that a fuel's remainder (an empty bucket) needs room in the buffer.

**Held recipe.** It is optional; without one the Harvester works the default yield.
`setHeldRecipe(Identifier)` and `clearHeldRecipe()` are public, but `setHeldRecipe` does not
validate the id. The in-game route is `HoldRecipePacket`, which acts only on the menu the player
has open.

**Breaking.** Breaking it drops the slot contents. In my reading the tanks are lost.

## Craftworks: the Refiner (smelt) and the Assembler

### Refiner

`CW/machine/RefinerBlockEntity.java` is one block that faces away from the player
(`HorizontalDirectionalBlock`). Its facing does not change any capability.

**Read.** Public methods:

- `state()`, which returns a `MachineState` and works only on the server;
- `smeltProgress()` and `smeltDuration()`;
- `energy()` and `energyCapacity()`;
- `inventory()`, which returns the raw handler. It bypasses the input filter, so the companion
  should read with it and not write through it.

The `RefinerBlock.LIT` blockstate shows whether it is smelting, and a client can read it. The recipe
currently smelting is a private field with no getter.

**`MachineState`** (`CW/machine/MachineState.java`) is one of `NO_RECIPE`, `CANT_RUN`,
`MISSING_INGREDIENTS`, `OUTPUT_FULL`, `NEEDS_POWER` or `CRAFTING`.

- On the Refiner, an empty input reads `NO_RECIPE`, and an input nothing smelts reads `CANT_RUN`.
- `MachineState` is not in the menu's `ContainerData`; only Jade sends it to the client.

**Capabilities** (`CW/machine/Refiners.java`, lines 57–58). Each ignores the side.

- **Item.** It inserts into the input, and only items that some smelting or blasting recipe takes,
  up to the Overload Limit (`CW/machine/OverloadLimit.java`). It extracts from the output only.
- **Energy.** Insert only.

**Do.** The recipe is found from the input automatically (blasting wins over smelting), so there is
nothing to set. To run it needs FE, a smeltable input and room in the output.

### Assembler

The Assembler is not needed for harvest → smelt → belt; this section is for completeness.
`CW/machine/AssemblerBlockEntity.java` covers tiers 1–3 on a 3×2×3 footprint
(`FootprintShape.square(3, 2)`).

**Read:**

- `state()`, `craftProgress()`, `craftDuration()`;
- `heldRecipe()`;
- `fluids()` and `fluidSide()`;
- the `AssemblerBlock.FLUID_CONNECTIONS` blockstate.

**Do.** It runs a player-set **Held recipe**. Three ways to set one:

- **`AssemblerMenu.request`** (`CW/machine/AssemblerMenu.java`). It runs the full checks but needs
  the menu to be open.
- **`setHeldRecipe(Identifier, Player)`**. It is public but runs no checks, not even the Lock. On a
  change it returns the input items to the player.
- **Validating first.** `HeldRecipes.verdict(player, tier, id)` (`CW/machine/HeldRecipes.java`) is
  the public check, and its result is a `HoldVerdict`.

**Item insert** goes to slot *n* only if the item matches the Held recipe's *n*th ingredient.

### What already exists for other mods

- **Lock hooks:** `CW/api/LockHooks.java`.
- **KubeJS:** `CW/compat/kubejs/` has a `CraftworksEvents.lock` event and the `craftworks:assembling`
  recipe schema.
- **Personal Assembler:** `CW/assembler/PersonalAssembler.java` has public static `craft(...)` and
  `queueOf(...)`. It lets the companion's own player body hand-craft, which could matter for
  "gathers its own materials". That is my inference.

## Beltworks: belts, loaders, feeders

### Read

**Storage.** Each tile is a `BeltTileBlockEntity` (`BW/blocks/BeltTileBlockEntity.java`), and
neighbouring tiles merge at runtime into one `TransportLine` (`BW/model/TransportLine.java`). There
is no global manager and a line does not span unloaded chunks. A belt has one lane, holding 8 items
per tile (Beltworks ADR 0003; `BW/model/BeltContents.java`).

**Public reads on a tile:**

- `heldHere()`, which returns a `List<ItemStack>`;
- `held()`, which returns each stack with its offset;
- `line()`, which returns the `TransportLine`. From it you get `contents().entries()`, `length()`,
  `speed()` and `capacity()`;
- `index()` and `holder()`;
- `tier()`, `shape()`, `pitch()`;
- `travel()`, the `Direction` the items move in.

**Blockstate** (`BW/blocks/BeltTileBlock.java`):

- `facing` (`HORIZONTAL_FACING`) is the direction of travel.
- `shape` is `straight`, `from_left` or `from_right`.
- `pitch` is `level`, or one of the up and down slope steps.
- Shape and pitch are always derived from the neighbours. There is no curve block and no
  underground belt.

**Splitter** (`BW/blocks/SplitterBlock.java`). It is two blocks wide, and its properties are
`facing` and `side`. `partner()` finds the other half. Its settings, `setInputPriority`,
`setOutputPriority` and `setSplitterFilter`, live on the left half.

**Loader** (`BW/blocks/BeltEndBlockEntity.java`). Its `FACING` is the mouth, toward the belt, and
the inventory is behind it. Whether it loads or unloads comes **only from geometry**; there is no
mode property.

- Public `acceptFromLine(ItemStack)` unloads into the inventory.
- Public `extractOne()` loads from it.

**Feeder** (Beltworks' inserter: `BW/blocks/FeederBlock.java`, `FeederBlockEntity.java`,
`BW/model/FeederArms.java`):

- The head takes from 1–3 blocks behind it; the tail drops 1–3 blocks ahead, turned by the
  `tail_turn` blockstate.
- An end can be any item handler, a level belt tile, or a splitter half. The head also picks up
  loose item entities.
- `arms()` and `setArms()` are public.

### Do

**Placing tiles.**

- A tile faces the way the player looks, turned by Groundworks' Rotate the Plan
  (`GW/mixin/BlockPlaceContextMixin.java`).
- It curves and climbs by itself.
- Plain `level.setBlock(pos, tile.defaultBlockState().setValue(FACING, dir), UPDATE_ALL)` is
  supported: the tile re-derives itself and its neighbours (Beltworks ADR 0008). It skips charging
  an item, though.

**Placing loaders and feeders.** They face the clicked horizontal face; on a top or bottom face,
they face the player. To place a loader beside a chest, stand looking at the chest
(`BW/gametest/LineSmokeTest.java` shows the pattern).

**Rotating.** It is Groundworks' `R` key (`GW/Rotate.java`, public `press(Player, BlockPos, boolean)`).

- Level tiles turn.
- Slopes and splitters refuse.
- A feeder turns only its tail.

**Putting items on a belt without a capability:**

- `BeltTileBlockEntity.dropOnto(ItemStack, double)`;
- `TransportLine.take`, `drop` and `insert`, which bypass the tier budget. After using them, call
  `lineChanged()`;
- an item entity resting on a tile joins the line on its own (`BeltLanding`).

**Filters.** On loaders and feeders, `assignFilterItem` and `resetFilterItem` need a `Player`,
because they send that player a chat message.

**Power.** Loaders and feeders need FE only when the server config `loadersNeedPower` is on
(`beltworks-server.toml`).

## Groundworks: placement and orientation rules

ADR-0001 in Groundworks makes its API semver.

**Plan API:**

- `GW/Placements.java`:
  - `planFor(Level, Player?, InteractionHand, ItemStack, BlockHitResult)`, which tries Stretch,
    then Fast Replace, then the item's own plan;
  - `planFor(Item, BlockPlaceContext)`;
  - `vanillaPlan`.

  These are read-only; they never place anything.
- `GW/PlacementPlan.java` holds the blocks, what they replace, and a `Refusal`.
- `GW/Refusal.java` lists the reasons.

**Footprints** (`GW/Footprint.java`, Groundworks ADR 0009):

- `plan(BlockPlaceContext)` and `place(BlockItem, BlockPlaceContext)` place every block or none.
- `positions(origin, facing)`, `standingOrigin(level, pos, state)` and `Footprint.of(state)` are
  lookups.
- The origin goes at the clicked position and faces the player.
- Breaking any part breaks the whole footprint, which drops one item.

**Permission checks.** I found no per-position `mayInteract` check or place event in footprint
placement. The turn path has both (`Footprint.java`, line 335). This matters for "don't build over
my work": claims would not protect the hidden positions. **Inferred, not tested.**

**Other actions:**

- Stretch (drag-placing a run of belt) and Dismantle need the player's gesture, and their entry
  points are package-private.
- `Rotate.press` and `Raise.press` are public.

**libworks.** `LW/transfer/GuardedResourceHandler.java` is the per-slot guarded item face that the
Harvester uses. libworks has no shared status or capability helpers.

## Already exposed and what needs a hook

Already exposed (✔) and gaps that need a new hook (✘):

| Need | Today | Gap |
|---|---|---|
| Harvester output and fuel | ✔ item capability; ✔ `status()` (burn, energy, progress, idle reason) | Slot getters, tier figures and output position are package-private or private |
| Harvester target area | ✘ recompute `origin.below()` ±1 and classify the blocks yourself | Getter for the layer and its sources |
| Smelter inputs, progress, power | ✔ Refiner `state()`, `smeltProgress()`, energy, item capability | Current recipe id has no getter; there is no fuel furnace (see Headline 1) |
| Belt contents and direction | ✔ `heldHere()`, `line().contents()`, `travel()`, blockstate `facing` | No stable read API; the model types are internal and carry no semver promise |
| Belt to chest | ✔ through a loader only | No item capability on belts, by design (Beltworks ADR 0008) |
| Place at (pos, facing) | ✘ needs a player-driven `BlockPlaceContext` or `useItemOn` | `placeAt(Level, BlockPos, Direction, Player?, ItemStack)` and a public `fits` |
| Ore under a Harvester site | ✘ | A placement-validity hint, or scan it yourself |
| Held recipe without a menu | ⚠ public setters with no checks | A checked, menu-free setter (Craftworks: `verdict` plus a set) |
| Loader or feeder filter without a chat message | ⚠ needs a `Player` | `setFilter(ItemStack)` |
| Change notifications | ✘ no events in any mod | Operation done, idle-reason change, line delivered; or poll |
| Debug commands | ✘ none | An inspect or dump command, if the MCP bridge doesn't provide one |

**Inference.** A companion with a real player body can do most of this today. It would place
machines through `useItemOn` with a set yaw, and read state on the server by polling getters and
capabilities. The hooks above would make it cleaner rather than make it possible. The two gaps that
are real are:

- the smelt step's power or fuel;
- the missing claim checks on footprint placement.

## Open points

- **Does a Harvester feed a loader in front of it?** The loader's `behind()` lookup would have to
  resolve the Harvester's capability. Placing the loader's mouth toward the belt and its back to the
  Harvester should work, but this is unverified and needs checking in game.
- **What `getHorizontalDirection()` returns for a `BlockPlaceContext` with a null player.** Not
  checked.
- **Whether the companion's MCP bridge runs as a mod.** If it does, it gets server-side reads. If it
  runs as an external client bot, it is limited to menus and Jade-style data. That is a decision for
  another ticket.
