---
status: accepted
---

# FactoryWorks is a modular tech mod suite, not a modpack

The product was an overhaul modpack recreating Factorio on Terra (ADR-0101). What players and pack
authors can actually reuse is the mods it is built from, and an overhaul that rewrites the world
cannot sit beside other tech mods. FactoryWorks becomes a suite of mods, shaped like Mekanism, that
plays in an ordinary world and mixes with other tech mods.

**Decision.**

- **The dependency chain is Groundworks, then FactoryWorks, then the Modules.** Groundworks needs
  nothing and stays useful alone in vanilla. **FactoryWorks** (mod id `factoryworks`, renamed from
  `factoryworks_core`) is the base every Module requires. The four **Modules** are Beltworks, Pipeworks,
  Wireworks and Craftworks. A Module requires FactoryWorks and no other Module, so Craftworks stops
  nesting Pipeworks.
- **Groundworks owns the footprint**, the multi-block placed and broken whole that the assemblers,
  boiler, steam engine and pumpjack stand on. It is placement, it lets Craftworks build its machines
  before FactoryWorks is split out, and any mod can use it in plain vanilla.
- **Every dependency is a required CurseForge dependency**, never jar-in-jar. Beltworks and
  Wireworks stop nesting Groundworks.
- **Mods talk only through NeoForge's energy, fluid and item capabilities**, to each other and to
  other tech mods. A Pipeworks segment ends at a machine face and hands off through the capability.
- **FactoryWorks holds what the Modules share.** That is its ore patches (added beside vanilla's
  untouched ores), uranium, the oil fields and pumpjack, the drill, the Pick, the radar (FTB Chunks
  now, other map mods later), and the intermediates. Every common material carries a `c:` tag, and
  recipes consume the tag, so vanilla raw iron feeds our furnaces and other mods process our ore.
- **Each Module holds the machines on its own network.** Craftworks takes the assemblers, chemical
  plant, refinery and furnaces. Wireworks takes the accumulator, solar, boiler and steam engine.
  Pipeworks takes the offshore pump and the barrel. Each Binding moves into the mod it configures.
- **Factorio is the reference for mechanics, not the authority for numbers.** Today's generated
  values are frozen as each mod's own data. Then the corpus, its extractors, the mechanic ledger,
  `docs/research/` and `docs/spec/` move to a private repository, and nothing public reads Wube's
  data.
- **The modpack becomes FactoryWorks Showcase**, which demos and integration-tests the suite. It
  keeps KubeJS for small tweaks, but has no quests, no wreck and no Factorio names. FTB Quests and
  FTB Teams leave it. Labworks is dropped. Research (Researchd, Porting Dead Libs and the tech tree)
  stays in the Showcase until it is extracted to the private repository.
- **Craftworks keeps the crafting grid removed**, and its `vanillaRecipes` flag turns vanilla's
  recipes into Assembling recipes. The Showcase drops its stock-recipe sweep (ADR-0034).
- **Repositories.** A new repository from the libworks template takes `5thlayer/factoryworks` for
  the mod, carrying `mod/`'s history, and copies this ADR as its ADR-0001. This repository becomes
  `factoryworks-showcase` and keeps the earlier ADRs as history.
- **The release train is Groundworks, then FactoryWorks, then the four Modules in any order, then
  the Showcase.**

**Considered: FactoryWorks as optional glue on top of standalone Modules.** This is Thermal
Integration's shape, and it would let a pack install Beltworks alone. It was rejected because the
ores, worldgen and intermediates the Modules craft from need a home that every Module can count on.

**Considered: folding Groundworks into FactoryWorks**, as Mekanism's base holds its shared API. It
was rejected because Groundworks would then stop being usable in plain vanilla, and that is work
already published on CurseForge and Modrinth.

**Considered: vanilla ore veins only.** It was rejected because the patches and their ores are made
for FactoryWorks' drills and machines. They are kept and added beside vanilla's ores, not in place
of them.

**Consequences.**

- This supersedes ADR-0101 and ADR-0090 and the dependency table in ADR-0109, and most of ADR-0103,
  whose naming rule still holds. The overhaul ADRs are superseded as a set: Terra and its worldgen,
  the stock-recipe sweep, the Nether and the End, vanilla spawning, the wreck, and the corpus as the
  authority for numbers. ADR-0066's grid removal stays, as Craftworks' own.
- Mechanics kept only for Factorio fidelity are dropped: the circuit network (ADR-0095) and the
  small lamp.
- The migration runs in this order: freeze the generated numbers, move the corpus out, un-nest
  Groundworks, split the FactoryWorks repository and rename the mod id, move each Core package into
  its Module, then clean up the Showcase.
- Open tickets that assume the overhaul are closed with this ADR as the reason. A rejected direction
  that will be asked for again (quests, a tech tree) gets a file in `.out-of-scope/`.
