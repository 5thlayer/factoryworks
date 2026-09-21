# `planetaryfactory:assembling_machine`: what Oritech's assembler actually offers a subclass

Read against the **installed jar**, `mods/oritech-2.0.0-exp6.jar`, extracted and disassembled with
`javap -p -c`. Every class claim below is from that jar unless it says otherwise. The source clone at
`~/minecraft_mods/oritech-src` is **v1.2.12 / 1.21.1**, a different major version, and is cited only
where it is the only source for an intent; each such citation says so.

ADR-0060 asserts in one sentence that assemblers, chemical plants and refineries "hold a **player-set
recipe**: set once, inputs filtered to it, no lookup", as `planetaryfactory_core` subclasses reusing
Oritech's model. This file supplies the facts that sentence has never been spent into. It proposes
nothing and decides nothing.

**Read first, not restated here:** `oritech-coverage.md` (the survey ADR-0060 adopted),
`modern-industrialization-slot-locking.md` (the discarded ADR-0056 mechanism), ADR-0060, ADR-0063,
and `docs/factorio-mechanics.md`'s *Recipe selection in a machine* row.

**One correction to the brief.** `docs/research/recipe-locking-mechanisms.md` is about gating recipes
behind **progression** (GameStages, GT's `research` condition, viewer hide-vs-lock). It is not a
survey of machine recipe *selection* and has nothing to say about this ticket. The prior art for this
ticket is `modern-industrialization-slot-locking.md` and the two Oritech facts in §1.4 below.

---

## 0. The five findings that most constrain the design

1. **`MachineBlockEntity`'s entire craft path is typed to `OritechRecipe`, and `OritechRecipe` cannot
   express a Factorio recipe.** `currentRecipe` is a field of that type, and `loadRecipeFromInput`,
   `findActiveRecipe`, `checkCraftingFinished`, `canOutputRecipe`, `getCraftingResults` and
   `getRecipeDuration` all name it. `OritechRecipe.itemInputs` is `List<Ingredient>` — **one entry per
   input slot, one unit each** — so on the Assembler's four slots the schema tops out at four
   ingredients of one item. `planetaryfactory:assembling` carries `List<SizedIngredient>` with
   Factorio's counts (10 plates, 5 circuits). **The pack's recipe type cannot be cast, adapted or
   widened into the base class's.** §1.5, §2.2.
2. **There is no lookup to remove — but removing it is not the work.** The lookup is one protected
   method, `loadRecipeFromInput`, and overriding it is trivial. The work is that everything
   *downstream* of it also speaks `OritechRecipe`. §1.2, §2.2.
3. **Oritech's assembler has no filter, no lock and no ghost of any kind, and its inventory filters
   nothing.** `MachineInventoryStorage.insert` routes among slots by `InventoryInputMode` and never
   consults an item. §1.3.
4. **Oritech ships a ghost-slot filter UI, packet and persisted record — for its pipes, not its
   machines.** `ItemFilterBlockEntity$FilterData` + `ItemFilterPayload` + `ItemFilterScreen$FilterSlotWidget`
   + `JeiItemFilterGhostHandler` is a complete, in-mod, in-version precedent for exactly the surface
   this ticket needs. §1.4.
5. **Scope is one build, not three — at the base-class level.** Assembler, Centrifuge, Refinery,
   Powered Furnace and Foundry all extend `MultiblockMachineEntity → UpgradableMachineBlockEntity →
   MachineBlockEntity`. But the Centrifuge and Refinery each override the recipe and crafting methods
   for fluids, so a shared recipe-holding surface covers all three only if it is written at
   `MachineBlockEntity`'s level and the fluid machines' own overrides are re-derived on top. §5.

---

## 1. Oritech's assembler, concretely (2.0.0-exp6)

### 1.1 The classes

| role | fully-qualified name |
| --- | --- |
| block | `rearth.oritech.block.blocks.processing.AssemblerBlock` extends `rearth.oritech.block.base.block.MultiblockMachine` implements `EntityBlock` |
| block entity | `rearth.oritech.block.entity.processing.AssemblerBlockEntity` extends `rearth.oritech.block.base.entity.MultiblockMachineEntity` |
| BE type | `rearth.oritech.init.BlockEntitiesContent.ASSEMBLER` (a `Supplier<BlockEntityType>`) |
| menu type | `rearth.oritech.client.init.ModScreens.ASSEMBLER_SCREEN`, a `MenuType<rearth.oritech.client.ui.UpgradableOritechScreenHandler>` |
| menu class | `rearth.oritech.client.ui.UpgradableOritechScreenHandler` extends `OritechScreenHandler` — **shared**, not assembler-specific |
| renderer | `rearth.oritech.client.renderers.blocks.MachineRenderer`, registered with the model path `"models/assembler"` in `rearth.oritech.client.init.ModRenderers` |
| recipe type | `rearth.oritech.init.recipes.RecipeContent.ASSEMBLER`, a `RecipeType<OritechRecipe>` |

Class hierarchy, all abstract and all taking a `BlockEntityType` in their constructor:

```
MachineBlockEntity(BlockEntityType<?>, BlockPos, BlockState, int energyPerTick)
  └ UpgradableMachineBlockEntity  (implements MachineAddonController)
      └ MultiblockMachineEntity   (implements MultiblockMachineController)
          └ AssemblerBlockEntity  (pos, state) ONLY
```

`AssemblerBlockEntity`'s sole constructor hard-codes `BlockEntitiesContent.ASSEMBLER` and
`OritechConfig.processingMachines.assemblerData.energyPerTick`. **`oritech-coverage.md` fact 9 still
holds at 2.0.0-exp6**: a subclass of the concrete class cannot carry its own `BlockEntityType`, so the
route is to extend the abstract base and re-derive the concrete logic (which for the Assembler is
~10 short methods; see §1.3).

### 1.2 How it selects a recipe today

Two protected methods on `MachineBlockEntity`:

```java
protected OritechRecipe findActiveRecipe();
protected OritechRecipe loadRecipeFromInput(ServerLevel, OritechRecipeInput, RecipeType<OritechRecipe>);
```

- `findActiveRecipe` keeps `currentRecipe` while `level.getGameTime() != lastChangedAt` and the held
  recipe is non-empty; otherwise it calls `getRecipeInput()`, then `getOwnRecipeType()`, then
  `loadRecipeFromInput`.
- `loadRecipeFromInput` returns `OritechRecipe.EMPTY` on an empty input, re-checks
  `currentRecipe.matches(input, level)` first, and otherwise calls
  `ServerLevel.recipeAccess().getRecipeFor(type, input, level)` — **vanilla's first match**, `.map(...)
  .orElse(EMPTY)`.

**Divergence from `oritech-coverage.md` fact 3, stated loudly:** that note names `getRecipe()` as the
protected lookup seam. **`getRecipe()` does not exist in 2.0.0-exp6.** It has been split into
`findActiveRecipe()` (the cache gate) and `loadRecipeFromInput(...)` (the manager query), and the
manager is reached through `ServerLevel.recipeAccess()` rather than `Level.getRecipeManager()`. Any
design that cites fact 3's method name by name is citing a name that no longer resolves. The
*behaviour* fact 3 describes — first match, no lock, kept while it still matches — is unchanged.

`canOutputRecipe(OritechRecipe)` is still `public` (fact 3's other overridable). `getRecipeInput()` is
`protected`, so what the lookup is keyed on is also a subclass's to change.

Called from the tick: `serverTick` → `workTick()`. `workTick` opens a root `Transaction`, extracts
`calculateEnergyUsage()` from `energyStorage`, increments `progress`, and on
`checkCraftingFinished(currentRecipe)` calls `onProgressCompleted(transaction)` →
`createCraftingOutputs` + `removeCraftingInputs`. `getRecipeDuration()` is literally
`getCurrentRecipe().time()`.

### 1.3 Inventory model

`AssemblerBlockEntity.getSlotAssignments()` returns `new ContainerSlotAssignment(0, 4, 4, 1)` —
inputs at 0..3, output at 4. `getInventorySize()` is `5`. `getGuiSlots()` places the four inputs at
(38,26) (56,26) (38,44) (56,44) and the output at (117,36) with the `boolean` flag set.
`getCorePositions()` is `(0,0,1) (0,1,0) (0,1,1)` — three Machine Cores plus the controller, a 1x2x2.
`getAddonSlots()` is `(0,0,-1) (0,0,2) (1,0,0)`.

**Insertion.** `MachineBlockEntity$MachineInventoryStorage extends
rearth.oritech.api.transfer.item.InOutInventoryStorage` and overrides **all four** `insert` overloads
(slotted and slot-less, `ItemResource` and `Resource`) — the same shape the pack's
`GuardedResourceHandler` enforces. What the overrides actually do:

- `insert(ItemResource, amount, tx)`: if `inventoryInputMode == FILL_EVENLY`, pick the input slot with
  the fewest items of that resource (`Math.clamp`, walking `inputStart..inputStart+inputCount`) and
  insert there; otherwise defer to the superclass's slot-less insert.
- `insert(slot, ItemResource, amount, tx)`: if `FILL_EVENLY` **and** `slotAssignments.isInput(slot)`,
  redirect to the slot-less form; otherwise defer.

**There is no item predicate anywhere in that path.** `InventoryInputMode` is a three-value enum
(`FILL_LEFT_TO_RIGHT`, `FILL_EVENLY`, `SIDED`) about *placement*, not admission. Externally the block
exposes `getItemLookup(Direction)` → either a per-direction handler (`SIDED` mode) or
`inventory.getExternalAccess()`; `InOutInventoryStorage` keeps `inputContainer`, `outputContainer`
and `externalAccess` as separate `ResourceHandler`s, so inputs-only and outputs-only views exist and
are the natural place a filter would go.

**Oritech 2.0 is on NeoForge's new Transfer API** (`net.neoforged.neoforge.transfer.ResourceHandler`,
`ItemResource`, `TransactionContext`) — the same API family `core/transfer/GuardedResourceHandler`
wraps. The two are directly compatible; nothing needs bridging.

### 1.4 Existing notion of a held / locked / selected recipe

**On machines: none.** No lock, no ghost, no selection, no filter. `MachineBlockEntity` has
`currentRecipe`, which is a *cache* — it is re-derived from the inputs the moment
`lastChangedAt == getGameTime()`. It is written in `saveAdditional`/`loadAdditional` only as part of
the machine's transient state, and there is a `private boolean initialRecipeLookup` flag, so it does
not persist as a decision.

**On pipes: a complete precedent, in this mod, in this version.**

| piece | class in the jar |
| --- | --- |
| the persisted selection | `ItemFilterBlockEntity$FilterData` — a record of `useNbt`, `useWhitelist`, `useComponents`, `Map<Integer, ItemStack> items`, with a `StreamCodec` and written through `saveAdditional(ValueOutput)` |
| the client→server gesture | `ItemFilterBlockEntity$ItemFilterPayload(BlockPos, FilterData)`, a `CustomPacketPayload` with `FILTER_PACKET_ID` and a `PACKET_CODEC`, handled by `ItemFilterBlockEntity.handleClientUpdate` |
| the ghost slots | `ItemFilterScreen$FilterSlotWidget` / `$FilterSlotBounds`, on `ItemFilterScreenHandler` |
| the recipe-viewer drag target | `JeiItemFilterGhostHandler implements IGhostIngredientHandler<ItemFilterScreen>` |
| enforcement | `ItemFilterBlockEntity$DelegatingFilterInventory`, a delegating `ResourceHandler` |

Note the viewer: Oritech's compat package is **JEI only** (`rearth.oritech.init.compat.jei.*`). The
jar ships one EMI artifact, `assets/emi/recipe/defaults/oritech.json`, and no EMI plugin class. The
pack runs EMI (ADR-0060 §EMI), so Oritech's ghost handler is not reusable as-is — the pack's own EMI
work is the reusable half (§3.3).

### 1.5 The recipe type and why it is the wall

`RecipeContent` registers ~20 `RecipeType<OritechRecipe>` suppliers — one per machine, all the **same
recipe class**, told apart by the `recipeType` field carried inside the record.
`OritechRecipe.CreateSerializerForType(RecipeType)` builds a serializer per type.

```java
public record OritechRecipe(
    List<Ingredient> itemInputs,                  // ONE PER SLOT, ONE UNIT EACH
    List<ItemStackTemplate> itemResults,
    Optional<SizedFluidIngredient> fluidInput,    // ONE fluid in
    List<FluidStackTemplate> fluidOutputs,
    int time,
    RecipeType<OritechRecipe> recipeType
) implements Recipe<OritechRecipeInput>
```

Evidence for "one per slot, one unit each" is the shipped data, not inference:
`data/oritech/recipe/assembler/motor.json` has `itemInputs: ["#c:ingots/nickel", "#c:ingots/steel",
"oritech:magnetic_coil", "oritech:magnetic_coil"]` — a count of two written as two entries, and the
list is exactly four long because there are four input slots. `OritechRecipe.findMatchingInputSlots`
does the ingredient→slot assignment.

**`oritech-coverage.md` fact 2 is correct in substance but two fields have changed at 2.0:** the fluid
input is now `Optional<SizedFluidIngredient>` rather than a bare `FluidIngredient`, and the results
are `ItemStackTemplate` / `FluidStackTemplate` rather than `ItemStack` / `FluidStack`. The template
change matters to this pack specifically: it is the same `ItemStack.CODEC` binding trap
`tests/pack/test_load_codecs.py` exists for, and Oritech has already taken the template route, so the
pack's `AssemblingRecipe` and Oritech's are aligned on that point.

The pack's type, `com.planetaryfactory.core.recipes.AssemblingRecipe`
(`mod/src/main/java/com/planetaryfactory/core/recipes/AssemblingRecipe.java`), is
`record(String category, List<SizedIngredient> ingredients, List<SizedFluidIngredient>
fluidIngredients, List<ItemStackTemplate> results, List<FluidStackTemplate> fluidResults, int time)`
and its `matches` **returns `false`** with the comment "No block matches it yet". It is registered as
`planetaryfactory:assembling` in `PFRecipes` and read today only by the Personal Assembler's hand set
and the EMI plugin.

Where the two types collide:

| | Oritech | pack |
| --- | --- | --- |
| item ingredient counts | one entry per slot, one unit | `SizedIngredient` |
| fluid inputs | at most 1 | list |
| `category` | none | carried, and ADR-0063 makes the hand set a predicate over it |
| results | list of templates | list of templates ✔ |
| `time` | `int` ticks | `int` ticks ✔ |

A Factorio recipe like `electronic-circuit` (1 iron plate + 3 copper cable) is expressible; `engine-unit`
(1 steel + 1 gear + 2 pipe) is expressible on 4 slots only by spending slots on counts; anything with a
total ingredient-unit count above 4 is not expressible at all. That is the wall.

---

## 2. The subclassing seam

### 2.1 What is subclassable

- `AssemblerBlockEntity` is `public`, **not final**, with a `public` `(BlockPos, BlockState)`
  constructor — subclassable in the Java sense, and useless in practice: it hard-codes
  `BlockEntitiesContent.ASSEMBLER`, so a subclass instance would be reloaded from disk as Oritech's
  class by that type's factory.
- All three abstract bases are `public abstract` with `public` constructors taking a
  `BlockEntityType<?>`. **Extending `MultiblockMachineEntity` directly is the seam**, and that is what
  `oritech-coverage.md` fact 9 already concluded; it is confirmed unchanged at 2.0.0-exp6.
- `AssemblerBlock` is `public`, not final; `MultiblockMachine` is its base.
- **No mixin is needed for the subclass itself.** A mixin is needed only if the design wants to change
  behaviour inside a *concrete* Oritech class (which is what `SteamEngineEntityMixin` does, because
  `SteamEngineEntity` has the same fixed-type problem and its logic could not be re-derived cheaply).

### 2.2 The methods a recipe-holding subclass has to reach

All of these are `protected` or `public` on `MachineBlockEntity` and therefore overridable:

| method | visibility | why it is in play |
| --- | --- | --- |
| `findActiveRecipe()` | protected | the cache gate; where "no lookup" is implemented |
| `loadRecipeFromInput(ServerLevel, OritechRecipeInput, RecipeType)` | protected | the manager query to delete |
| `getRecipeInput()` | protected | what the lookup is keyed on |
| `getOwnRecipeType()` | protected abstract | typed `RecipeType<OritechRecipe>` — **cannot return the pack's type** |
| `canOutputRecipe(OritechRecipe)` | public | room check |
| `checkCraftingFinished(OritechRecipe)` | protected | progress vs `time` |
| `createCraftingOutputs(Transaction)` / `removeCraftingInputs(Transaction)` | protected | the actual item movement |
| `getCraftingResults(OritechRecipe)` | public | what the screen and JEI show |
| `calculateEnergyUsage()` | protected | FE/t |
| `getSlotAssignments()` / `getInventorySize()` / `getGuiSlots()` | abstract/public | slot layout |
| `getCorePositions()` / `getAddonSlots()` | public | footprint |
| `saveAdditional(ValueOutput)` / `loadAdditional(ValueInput)` | protected | where a held recipe would persist |
| `getItemLookup(Direction)` | public | where a filtered face would go |

The `OritechRecipe`-typed signatures are the constraint. Three shapes exist, with costs:

**Option A — adapter.** Hold the `AssemblingRecipe` in a field and synthesise an `OritechRecipe`
carrying only `time` (and the `recipeType` field, which must be a `RecipeType<OritechRecipe>` and so
would have to be a dummy or Oritech's own), so the base class's progress, animation and energy
machinery keep working. Override `canOutputRecipe`, `checkCraftingFinished`, `createCraftingOutputs`,
`removeCraftingInputs` and `getCraftingResults` to consult the held `AssemblingRecipe` instead.
`OritechRecipe` is a record with a public canonical constructor, so synthesising one is legal.
*Cost:* every `OritechRecipe`-typed method becomes a lie the subclass has to keep consistent; the
synthetic recipe is visible through `getCurrentRecipe()`, which is public and which `getRecipeDuration()`
and the screen read.

**Option B — bypass.** Override `workTick()` whole (it is `protected`) and run the pack's own
craft cycle, keeping only the energy storage, inventory, addon system, GeckoLib animation and screen
from the base. *Cost:* re-deriving `workTick`'s transaction discipline — `Transaction.openRoot`,
`DynamicEnergyStorage.internalExtract`, `ProgressStorage.increment(tx)`, commit on success, close on
failure — which is exactly the shape `core/energy/LongSnapshotJournal` and `test_energy_faces.py`
already police in this pack.

**Option C — own block entity.** Extend nothing of Oritech's and speak only `EnergyApi`/`Energy.BLOCK`.
*Cost:* loses the model (§2.4), the addon system, the screen and the multiblock — i.e. everything
ADR-0060 chose Oritech for.

**Undetermined:** whether Option A's synthetic recipe survives `OritechRecipe.EMPTY` comparisons and
`isEmpty()` checks in paths not read here. Establishing that needs the whole of `workTick` and
`onProgressCompleted` disassembled line by line, which was not done.

### 2.3 What has to be re-registered versus inherited

| thing | inherited? |
| --- | --- |
| **block** | no — a new `planetaryfactory:assembling_machine` block, which may extend `MultiblockMachine` (or not, if the multiblock is dropped) |
| **item** | no — `PFItems` |
| **block entity type** | **no** — this is the whole reason for extending the abstract base; a new `PFBlockEntities` entry |
| **menu type** | **optional.** `OritechScreenHandler`'s network constructor is `(id, Inventory, FriendlyByteBuf)` → `buf.readBlockPos()` → `level.getBlockEntity(pos)`, and it stores the BE as a `ScreenProvider`. It is **not typed to Oritech's block entities**, so `ModScreens.ASSEMBLER_SCREEN` can be returned from a pack subclass's `getScreenHandlerType()` and will resolve. A pack-owned menu type is needed only if the screen grows a recipe-selection widget. |
| **screen** | inherited if the menu type is; `ModScreens.registerScreens` binds the screen class to the menu type, and reusing Oritech's menu type reuses its screen |
| **recipe type** | already registered — `PFRecipes.ASSEMBLING_TYPE` |
| **capabilities** | yes, and the pack's `tests/pack/test_capability_registration.py` `FACES` table has to gain the row |
| **renderer** | see §2.4 |

`MachineBlockEntity` implements `ScreenProvider`, `MenuProvider`, `GeoBlockEntity`,
`RedstoneAddonBlockEntity$RedstoneControllable`, `ColorableMachine`, `EnergyProvider` and
`ItemProvider`, and extends `rearth.oritech.api.networking.NetworkedBlockEntity`. All of that comes
free with the base class.

### 2.4 The model

`ModRenderers` registers the assembler as
`new MachineRenderer<>(context, "models/assembler", false)`. **Divergence from
`oritech-coverage.md` fact 9:** that note says the constructor is `new MachineRenderer<>("models/foundry_block")`,
a one-argument call read off 1.2.12. At 2.0.0-exp6 the constructors are
`MachineRenderer(BlockEntityRendererProvider$Context, String)` and
`MachineRenderer(BlockEntityRendererProvider$Context, String, boolean)`. `MachineModel` is in the jar
at `rearth.oritech.client.renderers.models.MachineModel` (#331); `MachineRenderer` now extends
`rearth.oritech.client.renderers.blocks.ModelBoundedGeoBlockRenderer<T, R>` and is generic over
`BlockEntityRenderState & GeoRenderState` (26.1's render-state pipeline).

The substance of fact 9 survives: `MachineRenderer` is public, the model path string resolves into
Oritech's jar (`assets/oritech/geckolib/models/block/models/assembler.geo.json`,
`assets/oritech/textures/models/assembler.png` and `assembler_glowmask.png` are all present), and a
pack block entity that extends `MachineBlockEntity` is already a `GeoBlockEntity`. So "reuses
Oritech's model" mechanically requires: register a `MachineRenderer` for the pack's BE type with the
string `"models/assembler"`, and ship no assets. The renderer also carries
`TEXTURE_OVERRIDE_TICKET` (a `DataTicket<ColorableMachine$ColorVariant>`) and eight colour variants
in `assets/oritech/textures/models/colored/assembler_*.png`, which is a free recolour route if the
pack wants the tiers told apart visually. Whether any of this reads correctly is a client run —
`scripts/check-client-assets.py` is the pack's seam for it.

### 2.5 Addon / API surfaces intended for third parties

- `rearth/oritech/api/` contains exactly three packages: `networking`, `screen`, `transfer`. There is
  **no `api/recipe`, no `api/machine`, no addon registry and no datagen entry point for machines.**
  `rearth.oritech.datagen.builders.AssemblerRecipeBuilder` exists but is a build-time datagen class
  for Oritech's own recipes.
- `oritech.mixins.json` declares `"required": true`, an empty server `mixins` list and three client
  mixins. Oritech does not mix into anything the pack owns.
- The pack's own convention is `"required": false` on its Oritech mixin config
  (`planetaryfactory_core.oritech.mixins.json`, which today lists `FluidStacksCapacityAccessor`,
  `MachineCoreEntityMixin`, `SteamEngineEntityMixin` and the client-side `FluidModelContentMixin`),
  because Oritech is an optional dependency — a renamed target is a log warning, not a crash. A
  *subclass* has no such escape hatch: it is a compile-time and load-time hard dependency on
  `MultiblockMachineEntity`'s constructor signature. That asymmetry is worth a decision.

---

## 3. The recipe-holding surface

### 3.1 Where the selection would live

Two options, both with precedent in this repo:

| option | precedent | cost |
| --- | --- | --- |
| block-entity NBT via `saveAdditional(ValueOutput)` / `loadAdditional(ValueInput)` | Oritech's own `ItemFilterBlockEntity$FilterData`; `MultiblockMachineEntity` and `UpgradableMachineBlockEntity` both already override the pair | must be re-read on `loadAdditional` before the recipe manager is necessarily populated on a client; store the **id**, resolve lazily |
| a data attachment | the pack's `PFAttachments` and `AssemblerCodecs` (ADR-0038's queue, whose codec round trip is asserted by `AssemblerCodecsTest`) | attachments on block entities are a different surface from the player attachment the queue uses; not verified here |

Either way the stored thing should be a `ResourceKey`/`Identifier`, not a recipe object —
ADR-0063's ids are stable (`planetaryfactory:assembling/<name>`) and are already what Researchd's
unlocks key on (`tests/factorio/test_research_unlocks.py`).

### 3.2 How the player would set it

Three gestures, with what each costs:

1. **A screen widget (a recipe picker or ghost slot).** Needs a pack-owned menu type, a pack screen,
   and a client→server packet. Oritech's `ItemFilterPayload` is the shape, and
   `rearth.oritech.api.networking` (`NetworkedBlockEntity`, `SyncField`, `UpdatableField`,
   `NetworkManager$MessagePayload`) is the mod's own transport, already inherited by the base class.
   Oritech's `ScreenProvider` interface has **no hook for an extra widget** — its surface is
   `getGuiSlots`, `getExtraExtensionLabels`, bar/arrow configuration and a set of `show*` booleans —
   so a widget means the pack's own screen class, not a hook into Oritech's.
2. **EMI Fill Recipe.** This is the gesture MI uses for its lock
   (`modern-industrialization-slot-locking.md` §"The gesture"), and **the pack has already built the
   client half**: `core/compat/emi/PersonalAssemblerEmiHandler`, `FillClick`,
   `mixin/emi/RecipeFillButtonWidgetMixin`, and `core/assembler/FillRequest` (#288, ADR-0065), which
   decodes left/right/middle/Shift into `ONE`/`FIVE`/`ALL`/`PLAN`. That enum's semantics are
   *quantities*, which a machine does not want — the reusable parts are the handler registration, the
   button-mixin and the screen-on-top race the handler documents, not `FillRequest` itself.
   `AssemblingEmiPlugin` already registers `planetaryfactory:assembling` as an EMI category and
   explicitly says "No workstation. The Assembling Machine is #277's" — adding the block as EMI's
   workstation for that category is the one-line prerequisite.
3. **An item interaction** (a configuration card). Prior art is Extended Industrialization's Machine
   Config Card, surveyed in `modern-industrialization-slot-locking.md`; ~330 lines over five files,
   and it is also Factorio's *copy settings* gesture, which the mechanic ledger's row calls out
   ("the setting copies to another machine") and which nothing in the pack answers today.

### 3.3 Display

`getCraftingResults(OritechRecipe)` is public and is what the screen and JEI read. A held recipe with
no inputs present has to render as a ghost, which Oritech's machine screen has no concept of —
`OritechScreenHandler.buildItemSlots` builds real `Slot`s from `getGuiSlots()`. The ghost-slot widget
exists only in `ItemFilterScreen`, a different screen class on a different handler.

### 3.4 Input filtering under this pack's transfer rules

`MachineBlockEntity.getItemLookup(Direction)` returns a `ResourceHandler<ItemResource>`. The pack's
rule (`docs/` transfer-face check, `tests/pack/test_transfer_guards.py`) is that **every refusing face
is built on `core/transfer/GuardedResourceHandler`, and `DelegatingResourceHandler` is named exactly
once in the mod, inside that guard.** A filtered input face is a refusal, so it falls under the rule:
the subclass would wrap Oritech's handler in a `GuardedResourceHandler` subclass whose
`insert(int slot, ...)` consults the held recipe, and the guard's slot-less override does the rest.

The check that enforces this is source-text (NeoForge is deliberately off the unit-test classpath), so
a new face here lands in `test_transfer_guards.py`'s sweep automatically, and a new block entity type
lands in `test_capability_registration.py`'s `FACES` table, which is a **recorded list** — a machine
missing from it fails rather than being answered "none".

One interaction worth flagging: Oritech's `MachineInventoryStorage` already overrides all four
`insert` overloads to route by `InventoryInputMode`. Wrapping it in the pack's guard puts two
routing layers on the same handler. Whether the guard's slot-by-slot loop and `FILL_EVENLY`'s
fewest-items choice compose sensibly is **undetermined** and is a real risk, not a formality.

---

## 4. What happens to the ingredient-set collision problem

The facts, without a recommendation:

- **#236's mechanism was GregTech's, and GregTech is gone.** `GTRecipeLookup.recurseIngredientTreeAdd`
  refused a colliding recipe *into the lookup structure* at load; 44 of 139 recipes in 16 groups never
  reached the machine. ADR-0060 removed GregTech.
- **Oritech's mechanism is different and milder.** `loadRecipeFromInput` calls vanilla
  `getRecipeFor`, which returns the **first** match and drops nothing at load. A collision under
  Oritech is "the machine runs the other recipe", not "the recipe does not exist" —
  `oritech-coverage.md` fact 3 already says this.
- **A machine with no lookup consults no candidate set.** If the held recipe is resolved by id from
  the player's selection and the craft cycle reads only that recipe, then two recipes sharing an
  ingredient set are never compared, never ranked and never ordered. The mechanism by which #236's
  symptom arises does not execute.
- **#237 and #238 are both closed, and they are different kinds of thing.** #237 is a *static check*
  ("no two emitted recipes of one type may share an ingredient set, counts ignored, no exception
  table") — it explicitly states it is "chassis-independent … correct on GregTech and on Modern
  Industrialization alike". #238 is a *GameTest* asserting MI's slot lock covers every collision
  group, and it "Depends on ADR-0056", which ADR-0060 superseded. #238's subject no longer exists.
- **#237's own text contains the condition under which it should change**: "If a collision is ever
  legitimate, the decision that makes it legitimate also says what distinguishes the recipes, and that
  mechanism is what the check should assert instead." A player-set recipe is exactly such a mechanism
  — the recipe is distinguished by its id, chosen by a human. Whether that makes #237's assertion
  obsolete, or merely re-points it, is a decision this file does not make.
- **Not everything downstream loses interest in collisions.** ADR-0038's hand set and
  `core/assembler/PlanResolver` pick a route with **no cost model**, and
  `tests/factorio/test_hand_resolver.py` asserts "no item has two hand recipes". That is grouped by
  *output*, not by ingredient set, and it is `test_recipe_duplication.py`'s axis — untouched by
  anything here. The Personal Assembler is not the machine and has no player-set recipe.
- **Unverified:** whether #237's check currently passes. Its own text says "All 16 groups are live
  today … it should go in alongside whatever resolves them". Run the check before asserting either way.

---

## 5. Scope: one machine or three?

**All five processing machines share a base.** Read off the jar:

| class | extends | also implements | overrides recipe/craft methods? |
| --- | --- | --- | --- |
| `AssemblerBlockEntity` | `MultiblockMachineEntity` | — | no — only `getOwnRecipeType` |
| `FoundryBlockEntity` | `MultiblockMachineEntity` | — | no |
| `PoweredFurnaceBlockEntity` | `MultiblockMachineEntity` | — | `loadRecipeFromInput`, `calculateEnergyUsage` |
| `CentrifugeBlockEntity` | `MultiblockMachineEntity` | `api.transfer.fluid.FluidProvider` | **yes** — `loadRecipeFromInput`, `getFluidRecipeInput`, `canOutputRecipe`, `removeCraftingInputs`, `createCraftingOutputs`, plus `InOutFluidStorage fluidContainer`, `hasFluidAddon`, addon hooks |
| `RefineryBlockEntity` | `MultiblockMachineEntity` | `api.transfer.fluid.FluidProvider` | **yes** — `getRecipeInput`, `createCraftingOutputs`, `removeCraftingInputs`, `getCraftingResults`, plus `ownStorage`/`nodeA`/`nodeB`, `moduleCount`, `createFluidResults`, `removeFluidInputs`, `calculateOutputFluids`, `getItemOutputMultiplier` |

So: **one recipe-holding surface can be written once, at the `MachineBlockEntity`/`MultiblockMachineEntity`
level, and inherited by all three.** What does *not* come free is everything the fluid machines
override — each of Centrifuge's and Refinery's fluid-aware `createCraftingOutputs` /
`removeCraftingInputs` / input-building is written against `OritechRecipe`'s one-fluid schema and would
have to be re-derived against `AssemblingRecipe`'s (and the chemical plant's / refinery's own, which
per `data/pack/category-map.json` are still `recipe_type: null` with `blocked_by: 258`).

Two facts that size the remainder:

- `oritech-coverage.md` fact 2 measured the corpus: **4 recipes need two fluid inputs**
  (`advanced-oil-processing`, `heavy-oil-cracking`, `light-oil-cracking`, `sulfur`) and
  **2 need more than four distinct item inputs** (`oil-refinery`, `rocket-silo`). `RefineryBlockEntity`
  has three fluid storages (`ownStorage`, `nodeA`, `nodeB`), so the two-fluid problem may be less
  severe on the refinery than on a two-tank subclass; **undetermined** — what those three are used for
  was not read.
- ADR-0060 names the refinery specifically as "a core subclass of `oritech:refinery`" and the chemical
  plant as `planetaryfactory:chemical_plant`; `category-map.json`'s notes repeat both. #277 chose the
  blocks; #258 builds them and "with it the recipe type and its JSON shape". So the chemical plant and
  refinery do not yet have a recipe type at all — the assembler is the only one of the three whose
  type exists today.

**The honest sizing:** the *recipe-holding* mechanism is one build if it is written at the base level.
The *machines* are three builds, and two of them also owe a recipe type, a serializer, a converter
route and an EMI category that do not exist yet.

---

## 6. Corrections to existing documents

| document | claim | status against the 2.0.0-exp6 jar |
| --- | --- | --- |
| `oritech-coverage.md` fact 3 | the lookup seam is `protected getRecipe()` | **wrong name.** No `getRecipe()` exists. Split into `findActiveRecipe()` and `loadRecipeFromInput(ServerLevel, OritechRecipeInput, RecipeType)`. Behaviour unchanged. |
| `oritech-coverage.md` fact 3 | reached via `level.getRecipeManager()` | **wrong.** `ServerLevel.recipeAccess()`. |
| `oritech-coverage.md` fact 3 | `canOutputRecipe()` is public | correct, unchanged |
| `oritech-coverage.md` fact 9 | `new MachineRenderer<>("models/foundry_block")` | **wrong arity.** Constructors are `(Context, String)` and `(Context, String, boolean)`. |
| `oritech-coverage.md` fact 9 | `MachineModel` is public | **correct**: it is `rearth.oritech.client.renderers.models.MachineModel`, not `blocks` (#331). `MachineRenderer` now extends `ModelBoundedGeoBlockRenderer`. |
| `oritech-coverage.md` fact 9 | concrete machines hard-code their type; extend the abstract base | **correct and still true**, verified on `AssemblerBlockEntity` |
| `oritech-coverage.md` fact 2 | one `FluidIngredient`, result `ItemStack`s | now `Optional<SizedFluidIngredient>` and `ItemStackTemplate`/`FluidStackTemplate`. Substance (one fluid in) unchanged. |
| `oritech-coverage.md` "Assembling machines" row | "Three subclasses: output lock (fact 3), base speed per tier, addon slot counts, 3x3 footprint, fluid tank" | the *output lock* half was written before ADR-0060 replaced the surface with a player-set recipe; the row has not been updated. It also does not mention the `OritechRecipe` typing wall (§1.5), which is the largest cost in the row. |
| `oritech-coverage.md` header | "citations are still at 1.2.12, and re-reading them at 2.0 is one of ADR-0060's open items" | this file discharges that open item **for the assembler path only**. Every other fact in that survey is still unverified at 2.0. |
| brief's framing of `recipe-locking-mechanisms.md` | prior survey of recipe-locking across mods | that file is about **progression gating**, not machine recipe selection. Not prior art for this ticket. |

---

## 7. Open questions only a human can settle

1. **Does the machine keep Oritech's craft cycle or replace it?** §2.2's Options A/B/C. This is the
   single decision everything else follows from, and it cannot be deferred: it fixes whether the pack
   owns `workTick`'s transaction discipline.
2. **Does `planetaryfactory:assembling` stay the recipe type, given it cannot be expressed as an
   `OritechRecipe`?** ADR-0063 says yes and gives three reasons. Nothing found here contradicts it,
   but nothing found here makes it cheap either.
3. **Four input slots, or more?** Factorio recipes exceed four ingredient units routinely, and
   Oritech's assembler screen layout (`getGuiSlots`) is four. A different slot count means a
   pack-owned screen, which also happens to be what a recipe-picker widget needs.
4. **Which gesture sets the recipe** — screen widget, EMI Fill Recipe, config item, or more than one?
   §3.2. The EMI route has the most existing pack code behind it and the least Factorio fidelity;
   the config item is the only one that also answers the ledger's "the setting copies to another
   machine".
5. **Is the multiblock kept?** `getCorePositions()` is a per-subclass override and could be empty,
   but the block would still need `MultiblockMachine.ASSEMBLED` in its state
   (`initMultiblock` reads `state.getValue(MultiblockMachine.ASSEMBLED)` — verified against the jar
   for #326, §9.1). ADR-0059 wants footprint = Factorio tile size placed from one
   item, which is the rig idiom, not Oritech's core-placing gesture.
6. **What happens to #237's check?** §4. Its own text names the condition; someone has to apply it.
7. **Does the pack's Oritech mixin `required: false` convention extend to a hard subclass?** §2.5. A
   subclass of `MultiblockMachineEntity` cannot degrade gracefully when Oritech changes; whether the
   pack accepts that, or pins Oritech the way `kubejs-version-is-pinned` pins KubeJS, is a decision.
8. **Three builds or one?** §5. The assembler alone is buildable today; the chemical plant and
   refinery need a recipe type first, and #258 owns that.
9. **Does the guard compose with `FILL_EVENLY`?** §3.4. Needs a world load to answer, not a reading.

## 8. What was not established

- ~~The full bodies of `workTick`, `onProgressCompleted`, `createCraftingOutputs` and
  `removeCraftingInputs`~~ — read line by line for #326; see §9.2.
- `RefineryBlockEntity`'s three fluid storages and what each is for.
- Whether `BlockEntitiesContent.ASSEMBLER`'s registration constrains anything for a pack-owned type
  (it should not; standard NeoForge `DeferredRegister`), and whether Oritech's `ADDON_ENTITY` fixed
  block list — `oritech-coverage.md` fact 4's one unverified step — is still fixed at 2.0.
- Whether `ModScreens.registerScreens` binds `ASSEMBLER_SCREEN` to a screen class that would render
  correctly for a pack block entity. The menu *handler* resolves by `BlockPos` and is not typed to
  Oritech's entities (§2.3); the screen was not read.
- Whether #237's check passes on `main` today.

---

## 9. Read off the jar for #326

The two facts #326 was told to read before writing code. Both are from `javap -p -c` on
`mods/oritech-2.0.0-exp6.jar`, not the 1.21.1 clone.

### 9.1 `deserializeMultiblock` does not reach `initMultiblock`

`MultiblockMachineEntity.loadAdditional` calls `deserializeMultiblock(ValueInput)`, and that default
method on `MultiblockMachineController` does exactly two things: clears `getConnectedCores()` and
refills it from the `cores` child list (each entry's `pos`, mapped back to world space), then calls
`setCoreQuality(input.getFloatOr("quality", 1.0f))`. **It never calls `initMultiblock` and never reads
`ASSEMBLED`.** A load cannot throw on a block without the property, and cannot divide by zero: an
empty `cores` list leaves the list empty and the quality at 1.0.

**But a load does reach a rescan, which does write it — found in-world, after #326 shipped.**
`MachineControllerLifecycle.onLoad` (NeoForge's `IBlockEntityExtension.onLoad`, which fires on
placement as well as on chunk load) schedules `rescanMultiblock()` on the server's next tick. The
rescan walks `getCorePositions()`, and when the list of cores it found is empty it calls
`resetInvalidMultiblock()`, which sets `ASSEMBLED` to `false`. On an empty core list that is every
time. The anchor then reads as unassembled, and `useWithoutItem` replays `triggerSetupAnimation`
on every right-click instead of opening the screen. The Assembling Machine overrides
`rescanMultiblock` to do nothing, and `PlacementPlanTests` reads `ASSEMBLED` five ticks after
placing — the tick-0 read passed with the bug live.

So the paths that read `ASSEMBLED` are these, all on the block or the controller:

| where | what it does with `ASSEMBLED` |
| --- | --- |
| `MultiblockMachine.useWithoutItem` | `false` → `tryPlaceNextCore(player)`; then **always** `initMultiblock(state)` before opening the screen |
| `initMultiblock(state)` | `true` → returns `true` at once. Only past that does it walk `getCorePositions()`, and the quality it sets is `sum / connected.size()` — `0.0f / 0` is NaN on an empty list |
| `MultiblockMachine.resetMultiblock` (from `playerWillDestroy`, `playerDestroy`, `destroy`, `onExplosionHit`) | `true` → `onControllerBroken()`, which walks `getConnectedCores()` and clears it — harmless when empty |
| `MultiblockMachineEntity.isAssembled(state)` | the unguarded `state.getValue(ASSEMBLED)`; `MachineBlockEntity`'s own returns `true` |
| `rescanMultiblock()` (scheduled by `onLoad`) | no cores found → `resetInvalidMultiblock()` → **writes** `ASSEMBLED=false` |

This is what #326's shape rests on: the block extends `MultiblockMachine` so the property exists,
the item places the anchor with `ASSEMBLED=true` so `useWithoutItem` skips the core placement and
`initMultiblock` returns before the division, and the block entity overrides `isAssembled` to `true`
so the tick does not depend on the state at all.

### 9.2 The craft tick, whole

`MachineBlockEntity.serverTick(level, pos, state, entity)`:

1. `if (!isAssembled(state) || disabledViaRedstone) return;`
2. On the first tick (`initialRecipeLookup`), set `lastChangedAt = level.getGameTime()` and clear the
   flag, remembering it was the first.
3. `currentRecipe = findActiveRecipe()` (the previous one kept in a local).
4. `if (currentRecipe.isEmpty()) { resetProgress(); return; }`
5. `if (!canOutputRecipe(currentRecipe)) { resetProgress(); return; }`
6. If the recipe changed: remember `progress.get()`, `resetProgress()`, and if this was the first
   tick and the *previous* recipe was empty, restore the remembered progress — which is what keeps a
   craft's progress over a world load, where the cached recipe starts empty.
7. `workTick()`.

`workTick()`, all inside one `Transaction.openRoot()` (try-with-resources, closed on every path):

1. `int cost = (int) calculateEnergyUsage();`
2. `if (energyStorage.internalExtract(cost, tx) != cost) return;` — close without commit: an
   under-powered tick draws nothing and makes no progress.
3. `progress.increment(tx);`
4. If `checkCraftingFinished(currentRecipe)` — `progress.get() >= currentRecipe.time() *
   getSpeedMultiplier()`:
   - `if (!onProgressCompleted(tx))` → log `crafting results failed! This should never happen. At: {}`
     and return without committing, so the energy, the progress and any partial output all roll back;
   - otherwise `progress.reset(tx)`.
5. `tx.commit(); setChanged(); onProgressed(); lastWorkedAt = level.getGameTime();`

`onProgressCompleted(tx)` is `createCraftingOutputs(tx) && removeCraftingInputs(tx)`, in that order,
and short-circuits.

- `createCraftingOutputs(tx)`: for each `ItemStackTemplate` in `getCraftingResults(currentRecipe)`,
  insert `ItemResource.of(template)` × `template.count()` into `inventory.getOutputContainer()`; any
  insert short of the count returns `false`.
- `removeCraftingInputs(tx)`: `OritechRecipe.findMatchingInputSlots(currentRecipe.itemInputs(),
  getInputView())` assigns one slot per ingredient; `null` → `false`. Then for each assigned slot,
  extract **one** unit from `inventory.getInputContainer()`; an empty slot or a short extract returns
  `false`.

Three consequences for ADR-0071's replacement, which #325's later tickets own:

- **The rollback is the transaction, not the code.** A failed output or input step returns before
  `commit`, and every mutation — energy, progress, outputs — was journalled against `tx`. A replacement
  `workTick` that mutates anything outside a snapshot-participating store breaks that.
- **`canOutputRecipe` runs every tick before `workTick`, and failing it resets progress.** A full
  output under Oritech's cycle does not hold a craft's progress, it discards it; ADR-0071's "holds it
  and idles" is a behaviour the replacement has to write, not inherit.
- **Input removal is one unit per slot**, which is the `OritechRecipe` wall of §1.5 restated at the
  item-movement level: `removeCraftingInputs` cannot consume a `SizedIngredient`'s count and has to be
  replaced along with the cycle.

For #326 itself only step 4 of `serverTick` matters: the Assembling Machine's `findActiveRecipe`
returns `OritechRecipe.EMPTY`, so the tick resets progress and returns before `workTick` — nothing is
looked up, extracted or consumed.
