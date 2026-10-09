---
status: accepted
---

# Fieldworks holds the resources, and no Module requires another

ADR-0115 made FactoryWorks the base every Module requires, holding the ore patches, oil, the drill,
the Pick, the radar and the shared intermediates. It rejected standalone Modules because the
materials they craft from "need a home that every Module can count on". Once the Pack's Factorio
recipes go and each Module crafts from its own vanilla-material recipes, that home is vanilla. The
base has nothing left to share but resources, and oil, cut off from its Factorio chain, needs a
job of its own (#648).

**Decision.**

- **No Module requires another.** Each mod plays alone in vanilla, and Groundworks is the only
  dependency any of them shares. Every tag a mod consumes has a default that vanilla can reach.
- **A mod's recipes name only vanilla items, tags with a vanilla member, or its own items.** No
  intermediate is shared between mods.
- **A mod ships only vanilla recipe types and stations**: crafting, smelting, blasting, smoking,
  stonecutting, and brewing, which NeoForge registers through `RegisterBrewingRecipesEvent` because
  vanilla builds its brewing table in code. No recipe replaces a vanilla one, and none is conditional
  on another mod. Adapting recipes to a custom machine is the job of the mod that owns it, such as
  Craftworks' `modRecipes`.
- **Mods meet only in `c:` tags and NeoForge's capabilities.** No mod names another's ids or writes
  into its namespace.
- **FactoryWorks Core becomes Fieldworks**, mod id `fieldworks`, in its own repository and
  CurseForge project. It holds resources and their base forms, never intermediates: the ore patches
  (iron, copper, coal, stone, uranium, sulfur), each behind a generation switch; the oil wells and
  crude oil, whose yield decays to a floor and never to zero (ADR-0081); and the **Harvester**. One map
  marker per patch names its resource and what it has left, through each map mod's API, FTB Chunks
  first. Sulfur, crude and uranium carry `c:dusts/sulfur`, `c:crude_oil` and `c:ingots/uranium`.
- **The Harvester** replaces the drills and the Pumpjack, in a burner tier and an FE tier. It works the
  layer under its footprint. Over an oil well it only pumps; otherwise it draws from patch ore and
  breaks any block in `fieldworks:harvestable` (default `c:ores`) for its normal loot. Burners read
  vanilla burn time.
- **Patch ore is mined by hand with vanilla pickaxes**, at vanilla hardness and tool tiers, one unit per
  break (ADR-0041).
- **Wireworks** takes the Boiler, the Steam Engine, steam, solar, nuclear (the Reactor and the Steam
  Turbine), the poles, the transformer and the accumulator, and the **plastic**, **battery**, **tar**
  and **bioresin** items. Plastic is blasted 1:1 from `#wireworks:plastic_resins`: tar, vanilla's
  resin clump, and bioresin, brewed from a water bottle and sugar cane or crafted from honeycomb and
  kelp. A battery takes one each of a cathode (default copper ingot), an anode (default iron ingot) and
  an electrolyte (default redstone and `#c:dusts/sulfur`). A fuel cell takes `#wireworks:fissile`,
  which defaults to `#c:ingots/uranium` and glowstone dust.
- **Oil's one job is plastic.** A crude-oil source block (any fluid in `c:crude_oil`) that touches
  lava becomes a tar block, and the lava stays. A piston breaks the tar block into one tar. No mod
  ships a shortcut from crude to plastic.
- **Pipeworks** takes the pipes, the storage tank, the pipe Dismantle Family and drag-laying, and
  requires Groundworks. The Offshore Pump becomes the **Pump**, which never depletes a source in its
  infinite-source tag (default water) and takes any other source as a bucket would. The **Outlet**
  places its fluid as a source block in the space it faces, a bucket's worth at a time.
- **Craftworks** takes the electric furnace as the **Refiner**: vanilla smelting and blasting, blasting
  winning where an input has both, and Craftworks' own m:n smelting recipes.
- **What goes**: the oil chain past crude (heavy and light oil, petroleum gas, lubricant, sulfuric
  acid, cracking, advanced oil processing); the Radar's block, sectors and team chart; the Pick and
  reach; the stone and steel furnaces and the fuel table; tree felling; the Centrifuge, enrichment and
  the acid gate on uranium; the wreck; the Pack's Factorio recipes and their KubeJS items; and
  Oritech.
- **The Showcase** keeps the name FactoryWorks and this repository. It holds the brand, a demo pack,
  integration tests and small KubeJS tweaks such as the starting kit.

**Considered: a shared base, as ADR-0115 has it.** Its reason was a home for the shared materials.
With no shared intermediates and vanilla defaults on every tag, nothing is left for a base to
hold, and requiring it would stop a pack from taking one Module.

**Considered: each mod registering its own copy of a common intermediate under one `c:` tag.** A
pack with several Modules would show the same gear several times over, which is the duplication
players already resent in other tech suites.

**Considered: letting a Module consume a tag that only another mod fills**, with uranium as the
example. It keeps nuclear honest but breaks "plays alone". The fissile fallback is glowstone, a
Nether trip like a mod's uranium ore is a dig. The Nether star was turned down as too far above it.

**Considered: crude flowing into lava, or a flowing crude block, making tar.** A crude source would
then yield tar without end, and oil would stop being worth piping.

**Considered: sugar for bioresin.** Sugar on a water bottle is vanilla's Mundane Potion, and NeoForge's
recipes are checked first, so it would replace a vanilla mix.

**Considered: keeping the Radar's charting, or tree felling.** The markers are what players read, and
they need no building to exist. Felling is what tree-felling mods already offer, and trees are not a
resource Fieldworks tracks.

**Consequences.**

- ADR-0115's dependency chain, its "FactoryWorks holds" clause, its Module ownership list, its mod
  id and its repository split are replaced here. Its other clauses stand: capabilities between mods,
  frozen numbers, the corpus leaving, and the Showcase.
- This supersedes ADR-0025, ADR-0039, ADR-0040, ADR-0043, ADR-0047, ADR-0051 and ADR-0125, and amends
  ADR-0033, ADR-0079, ADR-0081 and ADR-0098.
- FactoryWorks Core 0.3.0 is the last release under that name. Its CurseForge project is marked
  superseded, and worlds are not migrated to the `fieldworks` namespace.
- The release train is Groundworks, then the other mods in any order, then the Showcase.
- #598 is amended in place, and its tickets are re-cut to match.
