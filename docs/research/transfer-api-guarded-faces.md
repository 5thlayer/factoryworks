# Guarding per-slot rules on a NeoForge 26.1 transfer face

**Answer:** NeoForge ships no per-slot filtering wrapper, and `DelegatingResourceHandler` forwards
the slot-less `insert`/`extract` straight to the delegate, so the bypass is real. AE2 and Oritech
work around it the same way `GuardedResourceHandler` does: they override the slot-less pair to loop
through `this`. MI and Mekanism avoid it by putting the rule where both paths reach it. Keep
`GuardedResourceHandler`.

Read on 2026-10-10. Sources: NeoForge `26.1.x` at `aa824bd`, which is the branch the pack's
`neoforge_version=26.1.2.109` builds from. The other sources are AE2 `26.1` at `4ba637f`, Mekanism
`26.1` at `4718860`, Modern Industrialization `port/26.1` at `4dc28e4`, Oritech `26.1` at `a88166c`,
and Fabric API `26.1` (raw file, not cloned).

## 1. NeoForge

- [`DelegatingResourceHandler`](https://github.com/neoforged/NeoForge/blob/26.1.x/src/main/java/net/neoforged/neoforge/transfer/DelegatingResourceHandler.java)
  forwards `insert(T, int, tx)` and `extract(T, int, tx)` to `getDelegate()`. It does not route
  them through `convertIndex` or through its own indexed methods. Its javadoc says only "delegates
  all calls to another handler". Nothing warns that overriding the indexed methods leaves the
  slot-less pair unguarded. The file is unchanged on `26.3.x`.
- [`ResourceHandler`](https://github.com/neoforged/NeoForge/blob/26.1.x/src/main/java/net/neoforged/neoforge/transfer/ResourceHandler.java)'s
  default slot-less methods loop over `this.insert(index, ...)` and `this.extract(index, ...)`. The
  javadoc prefers the slot-less overloads ("lets the handler decide"), so pipes and hoppers can be
  expected to call them. That is the path that skips the guard.
- [`RangedResourceHandler`](https://github.com/neoforged/NeoForge/blob/26.1.x/src/main/java/net/neoforged/neoforge/transfer/RangedResourceHandler.java),
  the only `DelegatingResourceHandler` subclass NeoForge ships, does override both slot-less methods.
  It has to, so that they stay inside its range. Its loop calls `getDelegate().extract(index, ...)`,
  not `this`, so a subclass of `RangedResourceHandler` that refuses a slot would be bypassed in the
  same way.
- `StacksResourceHandler`/`ItemStacksResourceHandler` have an `isValid` hook. It gates insertion
  only. Neither class has a `canExtract` hook.
- [`WorldlyContainerWrapper`](https://github.com/neoforged/NeoForge/blob/26.1.x/src/main/java/net/neoforged/neoforge/transfer/item/WorldlyContainerWrapper.java)
  implements `ResourceHandler` directly, without delegating. Its indexed methods check
  `canPlaceItemThroughFace`/`canTakeItemThroughFace`, and it keeps the default slot-less loop, so
  both paths reach the check. For a block entity that is a `WorldlyContainer`, this is the vanilla
  way to get per-face, per-slot rules.
- History: both files arrive in "The Transfer Rework" (#2663, 2025-09-30), and nothing has touched
  them since. **Not verified:** whether an issue or PR about the bypass exists. GitHub search
  (`gh search`, `search/issues`) is blocked from this session, and I did not reach the NeoForge
  docs site (docs.neoforged.net).

## 2. Mods on the new API

- **Applied Energistics 2:** [`appeng/util/inv/FilteredInternalInventory.java`](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/26.1/src/main/java/appeng/util/inv/FilteredInternalInventory.java)
  has an inner `FilteringResourceHandler extends DelegatingResourceHandler`. It overrides the
  indexed methods with the filter and re-implements slot-less `extract` as a loop through `this`.
  Its comment reads: *"duplicates the default implementation from ResourceHandler, which is
  inaccessible here. We need to check the filter for each index we access, which is impossible if
  we call the delegate's implementation."* Slot-less `insert` goes through
  `ResourceHandlerUtil.insertStacking(this, ...)`, which also calls back into `this`. This is the
  same fix as ours, and its comment names the same reason.
- **Oritech:** [`api/transfer/item/InOutInventoryStorage.java`](https://github.com/Rearth/Oritech/blob/26.1/src/main/java/rearth/oritech/api/transfer/item/InOutInventoryStorage.java)
  builds its external face as an anonymous `DelegatingResourceHandler` that overrides all four
  methods. The slot-less pair loops over `this` across the input or output range only.
  [`SlotRangeResourceHandler`](https://github.com/Rearth/Oritech/blob/26.1/src/main/java/rearth/oritech/api/transfer/SlotRangeResourceHandler.java)
  implements `ResourceHandler` directly and has its own loops.
- **Modern Industrialization:** [`transfer/IOResourceHandler.java`](https://github.com/AztechMC/Modern-Industrialization/blob/port/26.1/src/main/java/aztech/modern_industrialization/transfer/IOResourceHandler.java)
  extends `DelegatingResourceHandler`, but its rule is per-handler (`allowInsert`/`allowExtract`)
  rather than per-slot. It guards all four methods with that flag, so the bypass cannot happen.
  MI's per-slot rules (locks, I/O) live in its own slot classes underneath.
- **Mekanism:** [`capabilities/proxy/ProxyResourceHandler.java`](https://github.com/mekanism/Mekanism/blob/26.1/src/main/java/mekanism/common/capabilities/proxy/ProxyResourceHandler.java)
  implements `ResourceHandler` directly and passes an `AutomationType` (derived from the side)
  through every method. [`IMekanismResourceHandler`](https://github.com/mekanism/Mekanism/blob/26.1/src/api/java/mekanism/api/resource/IMekanismResourceHandler.java)'s
  slot-less defaults loop over the containers, and each container enforces its own
  `canExtract`/`canInsert` for that automation type. The rule sits on the slot, so no wrapper can
  skip it.
- **Not checked:** Create (its heads list only `mc1.x/dev` branches), Immersive Engineering,
  Industrial Foregoing and Refined Storage 2 (no 26.x branch name was obvious), and Thermal,
  Functional Storage and Sophisticated Storage (not tried).

## 3. Fabric Transfer API

[`FilteringStorage`](https://github.com/FabricMC/fabric-api/blob/26.1/fabric-transfer-api-v1/src/main/java/net/fabricmc/fabric/api/transfer/v1/storage/base/FilteringStorage.java)
filters per resource (`canInsert(T)`, `canExtract(T)`), not per slot. Fabric's `Storage` has no
indexed insert or extract, and a filtered storage exposes slots only through views. `iterator()`
wraps every backing view in a `FilteringStorageView` whose `extract` re-checks `canExtract`. That
closes the one side door. Per-slot rules in Fabric are built by wrapping each `SingleSlotStorage`
and combining the wrapped slots with `CombinedStorage`, so the combined loop only ever sees guarded
parts. With one entry point per operation, this class of bug has nowhere to occur. NeoForge's added
indexed overloads are what reopen it. (The Fabric file was read; the `CombinedStorage` part of this
paragraph comes from the API's documented design and was not re-read for this note.)

## Recommendation

Keep `GuardedResourceHandler` as the suite's shared base. AE2 and Oritech each hand-write the same
override, and NeoForge offers nothing to replace it. Do not subclass `RangedResourceHandler` for a
guarded face, because its loops call the delegate. Where a block entity can be a `WorldlyContainer`,
`WorldlyContainerWrapper` is a reasonable alternative: it implements `ResourceHandler` directly and
both paths reach its checks. Filing upstream is worth a small issue or PR. The fix would make
`DelegatingResourceHandler`'s slot-less pair loop through `this`, or at least document the trap.
Check for an existing issue first, which this session could not do.
