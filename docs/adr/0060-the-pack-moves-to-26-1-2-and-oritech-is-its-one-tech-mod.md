---
status: provisional
supersedes: [28, 148, 156, 178]
---

# The pack moves to Minecraft 26.1.2, and Oritech is its one tech mod

Two surveys, `docs/research/oritech-coverage.md` and `docs/research/simplebelts-coverage.md`, each
priced a hypothesis without deciding it. The first asked what Oritech could carry if the machines were
its. The second asked what SimpleBelts could carry if Simplebelts's belts went. Together they found that the
pack could lose Create entirely. **And once Create goes, nothing ties the pack to Minecraft 1.21.1.**
This ADR adopts both hypotheses and the version change they make possible.

## The rule

**The pack moves to Minecraft 26.1.2 on NeoForge. Its mechanic-bearing palette becomes:**

| role | owner |
| --- | --- |
| machines, the energy layer, fluid logistics | **Oritech**, the one third-party tech mod |
| trains | **Railcraft Reborn** |
| belts, loaders and the splitter | **SimpleBelts, forked by the pack**, which makes it the pack's fourth fork |
| everything no mod carries at Factorio's numbers | `planetaryfactory_core` |

**Leaving:** Create, Create: Power Grid, Modern Industrialization and GCyR. GregTech was already
leaving under ADR-0056.

**Staying:** Researchd and Respoiled, both forks and both ported by the pack. Building Gadgets 2,
FTB Filter System, FTB Quests, KubeJS, Block Runner, and plumbing that has no mechanic of its own.

**Arriving:** FTB Materials, as the item layer (below).

## Why 26.1.2

**Nothing forces 1.21.1, and 1.21.1 is an old version.** The only thing tying the pack to it was
Simplebelts: its GitHub carries `mc1.21.1/*` branches and nothing later. SimpleBelts removes Simplebelts's last
job, so the tie is gone.

The two mods the pack now builds on are *developed* on 26.1.2. SimpleBelts' 1.21.1 line ended at
`v0.2.2`, and its work happens on the `26.1.2` branch. Oritech 2.0 and its Space Age addon exist only
there. A fork taken from the 1.21.1 line would start on a frozen branch.

**The probe says the move is real.** `../pf2612` loads Oritech 2.0.0-exp6, SimpleBelts 2.0.0-exp1,
Railcraft Reborn 1.4.3, Building Gadgets 2, FTB Filter System, FTB Quests and KubeJS 8.0.6 together. A
world was created in it on 2026-09-11 (human). The log shows four errors, none fatal: KubeJS's
Architectury plugin, a Configured config provider, EMI's Oritech recipe defaults, and a data map naming
`oritech:fluxite`.

**None of the forks' dependency chains blocks the move:**

- **Researchd** requires only Porting Dead Libs, and PDL's `main` (1.1.16) targets `[26.1,26.2)`.
  Upstream Researchd is still on 1.21.1, and the pack's fork was never submitted upstream, so the
  pack does the port.
- **Respoiled** upstream has a `multi/26.1` branch, which is a reference for porting the Decay fork.
- **`planetaryfactory_core`** requires `gtceu` and `researchd`. The first leaves under ADR-0056.

## Why Oritech, and not Modern Industrialization

**This is a choice, not something the version forced.** MI has a `port/26.1` branch targeting 26.1.2.
Its last commit was 2026-04-29 and it has no release, so MI could plausibly follow the pack. It leaves
anyway.

**One third-party tech mod, not two.** ADR-0017's rule is one owner per capability, with the losing
blocks recipe-removed. ADR-0035 is what that rule looks like applied over time: Mekanism's rows were
taken back one at a time until nothing was left. MI as the chassis plus Oritech as anything else would
restart that attrition.

**Oritech carries more of the ledger than MI's chassis does.** The Oritech survey's tally has Oritech
carrying the energy layer, the module system and most machine bodies, with every Factorio machine one
subclass away (its fact 9). It **unblocks** Modules and beacons, and it reopens the reactor's neighbour
bonus. MI was adopted by ADR-0056 for its recipe lookup, and that lookup is the one thing the Oritech
survey rebuilds in a subclass: first match plus output locking (its fact 3).

## Why a SimpleBelts fork, and not Simplebelts's belts

ADR-0044 kept Create because the puzzle Factorio's belt carries is mostly two-dimensional, and that
argument still holds. **Undergrounds and lanes stay `excluded`, argued from the medium.** What changed
is that the other claims the belt makes become *reachable*, which Create's RPM-driven belt never
managed:

- **Throughput becomes a known number.** The fork's belts are set to carry 15 / 30 / 45 / 60 items/s,
  which is Factorio's own figure. ADR-0044 deferred the throughput budget to play because Create's
  items-per-entry was "whatever the upstream inserter happened to hand over". The fork clamps that at
  one item per entry plus the researched bonus.
- **The belt holds 512 items per 64 blocks**, Factorio's number, so the belt as buffer is restored.
- **Splitters build balancers.** The fork adds a two-wide splitter and merger, so a balancer is
  constructed out of splitters, which Create's one-block Brass Tunnel never allowed.
- **`logistics-2`, `logistics-3` and `turbo-transport-belt` buy something again**, namely the belt
  tiers.

These rulings from the SimpleBelts survey are part of this decision:

- **Cost per length is mandatory:** one belt item per block, and a belt that costs one item for any
  length does not ship.
- **There is no inserter.** A belt's ends load and unload it, through the **loader**, and tier-1
  loaders stand in for the burner inserter.
- **Loaders from tier 2 up draw FE per item moved**, anchored on the fast and bulk inserters and derived
  by simulation, never transcribed. This is a balance cost.
- **The splitter draws no power.**
- **The tap is postponed.**

## What this supersedes and amends

- **ADR-0023 has nothing left to pin.** ADR-0056 already expired its constraint, and KubeJS 8 is what
  26.1.2 runs.
- **ADR-0044 is superseded.** Its analysis of the two-dimensional puzzle is kept and cited above. Its
  conclusion, its Create dials and its `maxBeltLength` go.
- **ADR-0056's chassis clause is superseded.** *GregTech leaves* stands, and so does its list of what
  that departure costs. *Modern Industrialization becomes the machine chassis* does not.
- **ADR-0057 is superseded.** Its account of what unification reaches remains correct history. With
  Create and MI both gone it arbitrates between nothing, and **AlmostUnified leaves**. What replaces it
  is ADR-0017's own rule: the item-layer decision names one supplier per part and recipe-removes the
  others.
- **ADR-0029's EU scale falls; its derivation stands.** With GregTech gone there is no voltage
  ladder to anchor `32 / 420_000` on, and a machine's draw is its Factorio `energy_usage` at this
  ADR's rate of 100 J to the FE. The Electric Furnace, the one machine that had been given a number
  under the old scale, re-derives from the same 180 kW to **90 FE/t** and buffers one steel craft at
  14,400 FE (#266). What is unchanged is the rule ADR-0029 exists for -- the number comes from the
  machine's own prototype, not from a scalar somebody chose -- along with its `excluded` idle draw.

- **ADR-0017's table is amended row by row.** Item logistics goes to the fork and Railcraft, fluid
  logistics to Oritech, power generation to Oritech plus the pack's engine, and the machine chassis to
  Oritech. The rule itself is unchanged, and it is the reason for this ADR's one-tech-mod choice.
- **ADR-0035's energy reasoning is reversed; its removal of Mekanism is not.** FE was demoted because
  nothing distributed it. With Oritech, Power Grid and Simplebelts gone, there is no EU, no volts and no
  rotation, and **FE is the pack's only energy currency**. ADR-0036's pole distributes it once it
  stops asking for GregTech's capability.
- **ADR-0048's rotation clause falls.** Its only argument for a steam engine that emits rotation was
  ADR-0036's choice of Power Grid. The engine emits electricity, which is the Factorio-faithful reading
  ADR-0048 turned down only because of that choice.
- **ADR-0036's brownout requirement and wire-tier ladder fall.** Both were satisfied by Power Grid
  and neither is rebuilt: a machine short of power stops, and the only wire is Oritech's pole. The
  supply-area pole stands, and it now distributes FE rather than GregTech's EU.
- **ADR-0053's plate owner changes.** Its rule, one plate per material and no ingot step, stands;
  the plate is FTB Materials' rather than GregTech's.
- **ADR-0018 rung 2's "movement at scale", Create 6 packages,** loses its mechanism and is dropped,
  not replaced: Factorio has no mass package logistics. `#28`'s cut list granted it at rung 2, and
  `docs/spec/terra-progression.md`'s Rung 2 is amended to match by #257.

`#148` chose Create: Power Grid and `#178` answered "no" to Factorio belts; both are contradicted.
`#102` is open and is left to the frontier: its answer becomes *the loader*.

## What it costs

**Porting three forks is the real cost:**

- **Researchd** needs a port onto PDL's 26.1 line.
- **Respoiled** needs a port with upstream's `multi/26.1` as a reference.
- **The core** has ten classes on the old item, fluid and energy capability API, which NeoForge 26.1
  replaced with the transfer API, plus the pole on GregTech's energy capability, plus Minecraft 26.1's
  renames everywhere else.

**Every generator and every asset check is written against 1.21.1's data formats.** Recipe
ingredients, item model definitions, loot tables and worldgen all changed shape after 1.21.1. The
converters regenerate their output, but the formats they write and the hops the static checks walk
are re-derived, not carried over.

**Several lines are deleted outright, not ported:**

- the Create kinetic recipe line (`create-recipe-convert.py`, `data/pack/create-substitutions.json`,
  `test_create_recipes.py`)
- the Power Grid recipe line (`powergrid-recipe-convert.py`, `data/pack/grid-substitutions.json`,
  `test_grid_recipes.py`)
- the GCyR fork, with its ADR-0001/0003 build and patch

**Four pillars are pre-releases, pinned by exact version:**

- **Oritech 2.0.0-exp6.** The Oritech survey's citations are at 1.2.12, and its *Java on Oritech*
  level couples the core to `block.base.entity`, which is not Oritech's API.
- **SimpleBelts 2.0.0-exp1**, the fork's base.
- **Railcraft Reborn 1.4.3.**
- **EMI**, which is an unofficial, unstable port. The Personal Assembler's Fill Recipe runs through it.
  JEI has an official 26.1.2 build and is already an optional dependency of the core, so it is the
  fallback, not a replacement chosen here.

**Mods that don't make the move:**

- AE2 and Sophisticated Backpacks, which aren't faithful to Factorio
- Tree Harvester, since the pack fells trees itself (ADR-0051)
- AlmostUnified, as above
- ProbeJS, which agent-driven development does not need
- GCyR, together with its rockets, platforms and planets. The pack is still on Terra, so nothing it
  plays today goes with GCyR.

## What is decided here and what is not

**Decided at the first writing:**

- the version, 26.1.2
- the palette and its owners
- the mods that leave
- FE as the single energy currency
- the steam engine emits electricity
- the belt rulings above

**Decided in the grilling that followed (2026-09-11).** These were this ADR's open list:

- **1 FE = 100 J.** Factorio's wattages then land inside Oritech's own range: an Assembling Machine 1
  draws 37.5 FE/t, an electric mining drill 45, a Steam Engine makes 450 and a Boiler's steam is
  worth 900. The core's existing joule figures convert at the same rate.
- **The item layer is FTB Materials.** It supplies the Plate and every metal-derived intermediate —
  gear, rod, wire — for every metal in ADR-0021's alphabet. Railcraft Reborn's plates and gears and
  Oritech's ingots are recipe-removed, which is ADR-0017's rule applied rather than an arbitration.
  Materials outside the alphabet are disabled through FTB Materials' own startup config, and the
  forms the pack does not use — ingots, nuggets, dusts and every ore-processing form — are
  recipe-removed, since Factorio has no ore multiplication.
- **Power has two carriers and no brownout.** The core's poles feed the machines standing in an
  area (ADR-0036's supply area). Oritech's Energy Transmission Pole carries power between areas in
  the role of Factorio's big electric pole: wire reach 1–32, Factorio's 32, and 18,000 FE/t, which
  is one full steam block of 20 Boilers and 40 Steam Engines. Its 2-tile supply area is not
  reproduced. Oritech's energy pipes, Enderic Laser and storage blocks are recipe-removed. **An
  underpowered machine stops rather than slowing**, which is Oritech's own behaviour, and
  ADR-0036's brownout requirement is dropped rather than built.
- **Energy storage is the core's accumulator**, at Factorio's 5 MJ and 300 kW, which is 50,000 FE
  at 150 FE/t. Oritech's smallest store is twenty times that and would retire the puzzle.
- **Machines are core subclasses of Oritech's.** An Oritech machine with a Factorio counterpart
  becomes a `planetaryfactory_core` subclass that reuses Oritech's model. Assemblers, chemical
  plants and refineries hold a **player-set recipe**: set once, inputs filtered to it, no lookup.
  Furnaces keep Oritech's first match, which is Factorio's own split. Oritech recipes that conflict
  with the corpus are reauthored. Every Oritech machine with no Factorio row is recipe-removed —
  the Pulverizer, the forges and the ore-doubling Centrifuge among them.
- **Storage is the core's.** Factorio's wooden, iron and steel chests and the barrel are registered
  by the core. Their textures are authored, since nothing in the palette ships them.
- **The outfield patches are placed by the core**, alongside the starting fields, so every patch
  carries the amount ADR-0041 gives it.
- **Interplanetary travel is deferred** until Oritech ships a first-party space addon. The rows
  that depend on reaching orbit are `blocked` in the ledger, and the parked bodies stay parked.
- **The port order is the core, then Researchd, then the SimpleBelts fork.** The core's dependency
  on Researchd becomes optional, with research gating inert, until the Researchd port lands.
  Respoiled is deferred: Decay matters only on Sapros, which is parked.

**Still not decided:**

- **The re-read of the Oritech survey at 2.0** is done for energy, recipe lookup, items and storage
  (2026-09-11). Its module and addon system has not been re-read, so whether Oritech's addons carry
  Modules and beacons is still the survey's claim, and that row stays `blocked`.

## Considered alternatives

- **Drop Simplebelts and stay on 1.21.1**, with Oritech 1.2.12 and a fork of SimpleBelts 0.2.2. The belt
  decision does not require the version change. Rejected because nothing requires staying either: both
  mods develop on 26.1.2, the belt fork would start from a frozen line, and the pack is pre-release,
  so no world depends on the old version.
- **Keep Create for belts and trains, on 1.21.1** (ADR-0044 and ADR-0056 as written). Rejected on the
  belt argument above. A throughput nobody can compute, no buffer and no constructed balancer are
  defects the fork closes, and Railcraft Reborn carries trains without Create.
- **Move to 26.1.2 with MI as the chassis, if MI's port lands.** Rejected on ADR-0017's rule: a second
  full-stack tech mod beside Oritech restarts the row-by-row attrition ADR-0035 records.

## Status

Provisional under ADR-0042. It is argued from two surveys and one world load, not from a rung played
end to end. It is promoted when Terra's rung 0 has been played on 26.1.2.
