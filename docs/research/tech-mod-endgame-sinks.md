# How do tech mods handle endgame content and resource sinks?

**Answer: almost no tech mod ships a real *never-ending* sink. Most mods end in a one-off milestone
(a creative or "infinity" item, a fusion reactor, a capstone multiblock) plus player gear. The sinks
that keep demand alive are either packs' quest and recipe glue, or a small set of mechanisms with
running costs: fuel-burning reactors, energy-hungry player gear, upkeep storage and
exponentially priced items. Mods that left their costs in config or recipe JSON are the ones packs
retune. Mods that hard-coded them get replaced or worked around in KubeJS.**

## How far this was verified

Most mod wikis (aidancbrady.com, guide.appliedenergistics.org, blakesmods.com, fandom, Modrinth)
were blocked by the session's egress proxy. Only `raw.githubusercontent.com` and `github.com` could
be read. So:

- **[V]** means verified this session against the source line or file that is cited.
- **[U]** means unverified this session. It comes from prior knowledge of the mod. The URL given is
  the primary source to check, but it was not fetched. Treat version-specific numbers in [U] claims
  as approximate.

## Per mod

| Mod | Endgame goal | Shape | Upkeep / running cost | Pack-tunable? |
| --- | --- | --- | --- | --- |
| Mekanism | MekaSuit and Meka-Tool, SPS antimatter, fusion, QIO | gear + scaling sink | yes: fusion needs D-T fuel and SPS needs large FE | config + recipe JSON |
| AE2 | autocrafting at scale, singularities, Spatial IO | milestone + capacity | yes: idle power draw per device | config + recipe JSON |
| Create | big contraptions, trains; no capstone | no sink (sandbox) | stress units, no consumption | recipe JSON |
| GregTech CEu / Modern | UHV+ tiers, fusion, creative-tier items | very long milestone ladder | yes: EU/t, fuels, maintenance | KubeJS/Java API |
| Draconic Evolution | Chaotic gear, energy core, reactor | gear + milestone | yes: the reactor consumes fuel and must be fed; gear runs on RF | config + recipe JSON |
| Avaritia / Extended Crafting | Infinity gear, Ultimate Singularity, creative items | one-off milestone | none after crafting | recipe JSON; EC singularities data |
| Immersive Engineering | Excavator, Arc Furnace, Railgun | infrastructure | yes: the Excavator depletes mineral veins and everything draws power | recipe + vein JSON |
| Thermal series | Augmented machines, Resonant tier | gear-ish upgrades | power only | recipe JSON + config |
| Industrial Foregoing | Infinity tools, Mob Duplicator, Laser Drill | gear + automation | yes: Infinity tools need power and Pink Slime | recipe JSON + config |
| Modern Industrialization | Fusion, Quantum Armor, replicator-tier sinks | gear + sink | yes: EU, plasma fuels | recipe JSON (+KubeJS) |
| Oritech | Particle accelerator, exosuit, augments | gear + milestone | yes: RF use, accelerator collisions | recipe JSON |
| Botania | Gaia Guardian, Terrasteel and Gaia gear | boss milestone + gear | mana is spent on gear repair and use | recipe JSON |
| Environmental Tech | Void miners T1 to T6 | scaling resource source | yes: high RF/t | config + JSON lens tables |
| Mystical Agriculture | Insanium tier, Supremium gear, Awakened | gear + crop ladder | none (passive) | crops by config/datapack |
| Ad Astra (and successors) | planets, oxygen and fuel logistics | exploration milestone | yes: rocket fuel and oxygen | planet JSON + recipes |
| Refined Storage | very large storage, autocrafting | capacity | yes: energy usage per device | config |

### Mekanism

- **Goal.** The MekaSuit and Meka-Tool are module-upgraded gear. Modules cost Ultimate-tier parts,
  and the top modules need Antimatter Pellets from the SPS (Supercritical Phase Shifter), which
  turns polonium into antimatter. The other capstones are the fusion reactor and QIO storage. [U]
  https://wiki.aidancbrady.com/wiki/Supercritical_Phase_Shifter
- **Cost is config.** `inputPerAntimatter` defaults to one bucket, and `energyPerInput` defaults to
  1,000,000. [V]
  https://raw.githubusercontent.com/mekanism/Mekanism/1.21.x/src/main/java/mekanism/common/config/GeneralConfig.java
- **Shape.** Antimatter is a scaling sink: each module level eats more pellets, and polonium needs a
  fission reactor that burns fuel and makes waste. The fusion reactor needs a continuous
  deuterium-tritium feed. [U]
- **Criticism.** The SPS is often called "wait for a number to fill". Once the suit is maxed,
  antimatter has nothing left to buy. [U]

### Applied Energistics 2

- **Goal.** No single capstone. The endgame is network scale: autocrafting throughput, channels,
  P2P and Spatial IO. Singularities come from the Matter Condenser and are used for Quantum
  Entangled Singularities. [U] https://guide.appliedenergistics.org/
- **Config.** `condenserSingularityPower` = 256000, `spatialPowerMultiplier` = 1250.0,
  `spatialPowerExponent` = 1.35, plus a `channels` mode. [V]
  https://raw.githubusercontent.com/AppliedEnergistics/Applied-Energistics-2/main/src/main/java/appeng/core/AEConfig.java
- **Upkeep.** Every network device draws AE idle power. The Spatial IO cost grows with the cube's
  volume through the exponent above. [V for the config keys; U for the idle draw]
- **Note.** AE2 is used as a *sink enabler* in packs, because autocrafting makes the exponential
  pack recipes (E2E-style) feasible. It is not a sink in its own right.

### Create and addons

- **Goal.** None. Create is a sandbox: stress units are a capacity budget, not a consumable. [U]
  https://github.com/Creators-of-Create/Create
- **Addons.** Create: New Age and Create Crafts & Additions add power conversion. Some packs add
  "Create: Above and Beyond"-style progression made of quest and recipe chains. ("Above and Beyond"
  is a modpack, not a mod.) [U]
- **Criticism.** "Nothing to build towards" outside a pack. Packs supply the goals.

### GregTech CEu / Modern

- **Goal.** A voltage ladder from ULV up to MAX, fusion reactors Mk I to III, and late items such as
  the creative-tier containers in some versions. Every tier gates the next machine. [U]
  https://gregtechceu.github.io/GregTech-Modern/
- **Upkeep.** Machines draw EU/t only while running. Multiblocks can need maintenance (in CEu, the
  maintenance hatch). Fusion needs plasma inputs. [U]
- **Tunable.** Materials and recipes are registered through a KubeJS/Java API. Packs such as
  GregTech: New Horizons and Monifactory rebuild the whole ladder. [U]
- **Criticism.** "Endgame is bigger numbers": each tier repeats the previous one at ×4 voltage.
  The tedium is a deliberate feature.

### Draconic Evolution

- **Goal.** Wyvern, Draconic and Chaotic gear, the Energy Core (tiered RF storage) and the Draconic
  Reactor. [U] https://github.com/Draconic-Inc/Draconic-Evolution
- **Upkeep.** The reactor consumes awakened draconium fuel and can explode if it is mismanaged.
  Gear shields drain RF. [U]
- **Criticism.** The Energy Core is a "number go up" battery with nothing to spend its contents on.
  Chaos shards are gated behind a boss. [U]

### Avaritia and Extended Crafting

- **Goal.** Infinity gear and the Ultimate Singularity, made in 9×9 tables. In packs, creative
  items are the usual "you won" marker. [U]
- **Shape.** A pure one-off milestone with no upkeep after the craft. Extended Crafting singularities
  are defined as data or config, so packs choose the materials. [U]
  https://github.com/BlakeBr0/ExtendedCrafting (the 1.x wiki and blakesmods.com docs could not be
  read: one was empty, the other blocked)
- **Criticism.** "Throw everything into a compressor": volume, not challenge.

### Immersive Engineering

- **Goal.** Industrial infrastructure (Excavator, Arc Furnace, Diesel, Railgun). No capstone. [U]
  https://github.com/BluSunrize/ImmersiveEngineering
- **Upkeep.** Excavator mineral veins deplete. Vein definitions and recipes are datapack JSON. [U]

### Thermal series

- **Goal.** Machine augments and the Resonant/Creative augment tiers. No capstone. [U]
  https://github.com/CoFH/ThermalExpansion

### Industrial Foregoing

- **Goal.** Infinity Drill and other Infinity tools, which are charged with power and need Pink Slime
  for higher tiers. Also the Laser Drill and the Mob Duplicator, which uses essence. [U]
  https://github.com/InnovativeOnlineIndustries/Industrial-Foregoing

### Modern Industrialization

- **Goal.** Fusion Reactor, plasma turbine, Quantum Armor and tools, and late replicator-style
  sinks. The README itself frames the goal only as "total automation". [V for the README framing]
  https://raw.githubusercontent.com/AztechMC/Modern-Industrialization/master/README.md;
  [U] for the specific items (https://modern-industrialization.fandom.com/wiki/Quantum_Armor, blocked)

### Oritech

- **Goal.** Particle accelerator, exosuit, jetpack and player augments. [U]
  https://modrinth.com/mod/oritech (blocked)

### Botania

- **Goal.** Gaia Guardian I and II, Gaia gear, and mana-repaired relics. A boss is the milestone. [U]
  https://botaniamod.net/

### Environmental Tech (void miners)

- **Goal.** Ore and resource void miners in tiers 1 to 6, with lenses that bias the output. This is a
  source, not a sink, but each tier's RF/t is a running cost. [U]

### Mystical Agriculture

- **Goal.** Crop tiers up to Insanium, Supremium gear and Awakened Supremium. No upkeep. [U]
  https://github.com/BlakeBr0/MysticalAgriculture
- **Note.** It works as an infinite *source*, which in packs often kills other sinks.

### Ad Astra and space mods

- **Goal.** Planets, oxygen and fuel logistics. Planets are JSON. [U]
  https://github.com/terrarianmorgan/Ad-Astra. See also our `gcyr-planet-definition.md`, which shows
  GCyR planets are pure data.

### Refined Storage

- **Goal.** Storage scale and autocrafting. Energy use per device is in config. [U]
  https://github.com/refinedmods/refinedstorage2

### Packs on top (E2E, ATM, FTB)

- Packs add the never-ending part: Expert recipes (E2E), the "ATM Star" (ATM), and quest loops with
  reward shops (FTB Quests). The mods supply the mechanism, and the pack supplies the cost curve. [U]

## Synthesis

**Recurring patterns.**

1. Most mods stop at a milestone item. Once it is crafted, the economy has nothing left to buy.
2. Player gear that consumes something is the most common lasting sink: Meka modules, Draconic
   shields, Infinity tools, Quantum Armor.
3. Running costs show up as power draw (AE2, RS, IE, void miners) or as fuel (fusion, Draconic
   reactor, SPS). Power-only upkeep stops mattering once power is solved. Fuel-based upkeep keeps
   upstream chains running.

**Sink shapes that keep demand alive.**

- **Consumed-per-use or per-tick fuel at the top tier.** Fusion D-T feed, reactor fuel and antimatter
  keep the whole upstream factory relevant.
- **Exponential or open-ended scaling.** AE2's spatial power exponent, or each further gear level
  costing more. This is closest to Factorio's infinite research.
- **Throughput targets rather than item counts.** Rate-based goals (Factorio SPM) are almost absent
  from MC mods, and packs fake them with quests.
- **Weak shapes.** One-off crafts (Infinity, Creative), and capacity-only storage such as the Energy
  Core.

**Mechanism versus content.**

- The retunable mods put **costs in config** (Mekanism, AE2, RS) and **content in recipe or data
  JSON** (singularities, veins, planets).
- GregTech is the counterexample: a code API that only KubeJS packs can bend.

**Lessons for the -works suite.**

- **Ship a default endgame** made of three pieces, each a datapack value with a sensible default:
  (a) a milestone (a launch- or rocket-like capstone); (b) an infinite sink that is rate-based or
  exponentially priced; (c) Gearworks gear whose upgrades consume a top-tier product.
- **Make upkeep fuel- or material-based, not only power-based**, so that solving power does not
  solve the endgame. Keep every rate and amount in data or config, following Mekanism's
  `inputPerAntimatter` / `energyPerInput` model.
- **Pitfalls:**
  - infinite free sources (void miners, magic crops) defeat sinks, so a source should carry a running
    cost;
  - a "bigger numbers" ladder with no new mechanics;
  - endgame storage with nothing to spend on;
  - hard-coded costs that force packs into KubeJS;
  - cross-mod sinks that assume a sibling is installed. Each -works mod needs its own sink or none.
