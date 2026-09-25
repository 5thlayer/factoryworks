# PlanetaryFactory — Game Design Document

A Minecraft 26.1.2 / NeoForge modpack reproducing the progression, logistics puzzles and
interplanetary scope of Factorio's Space Age expansion, built on a curated mod stack and bound
together by `planetaryfactory_core` and KubeJS into a stationary, automation-first loop.

This document describes intended design. Decisions that are hard to reverse are recorded as ADRs in
`docs/adr/`; domain vocabulary is defined in `CONTEXT.md` and used here verbatim. Where an ADR or
the ledger owns a subject, this document points at it rather than restating it, and design prose
has no standing against either (ADR-0054).

**The design is Factorio's, and the mods implement it.** Which mechanics the pack reproduces, adapts
or drops is `docs/factorio-mechanics.md`, ordered by Factorio's own structure and describing the
pack in Factorio's terms. This document describes the pack's own shape on top of that; where it
names a mod it should be because that mod owns the capability under discussion, not because the mod
is the pack's subject. **None of them is.**

## 1. Core Technology Stack

Exactly one mod owns each capability (ADR-0017), and ADR-0060 says which on 26.1.2. No mod is the
ladder — the ladder is Factorio's science packs (ADR-0018). The jar set and its exact versions are
the packwiz manifest (ADR-0024); ADR-0060 records which of them are pre-releases pinned by version.

- **Oritech** — the one third-party tech mod: the machine bodies the pack's machines subclass, and
  fluid logistics (ADR-0060). Its placed logistics blocks and its oil fluids, drawn in Factorio's
  colours, are ADR-0067.
- **FTB Materials** — the item layer: every material form in ADR-0021's alphabet. A tech mod's
  competing plate is not the pack's plate (ADR-0061).
- **Beltworks** (5thlayer/beltworks) — belts, loaders and splitters, at Factorio's
  throughput (ADR-0060, ADR-0076, ADR-0084). Its vocabulary is its own `CONTEXT.md`.
- **Railcraft Reborn** — trains (ADR-0060).
- **Researchd**, forked by the pack — the research tree and the Research Lab that gates it
  (ADR-0022). The tree's shape is Factorio's, extracted rather than transcribed, and research is
  server-global (ADR-0058). The fork's port to 26.1.2 is #251; until it lands, research gating is
  inert. FTB Quests keeps the book and the reward surface, and gates nothing.
- **`planetaryfactory_core`** — the pack's own mod, for mechanism no other mod supplies at
  Factorio's numbers (ADR-0014, ADR-0015). Among it: the pole network, the only power carrier
  (ADR-0062), the pack's recipe types (ADR-0063) and the machines built on Oritech's bodies.
- **KubeJS** — registers the pack's items and runs the stock-recipe sweep (ADR-0015, ADR-0034).

FE is the pack's only energy currency, at 1 FE = 100 J (ADR-0060). Create, Create: Power Grid,
GregTech, GCyR, Modern Industrialization, AlmostUnified and AE2 left with ADR-0060, and Mekanism
before them with ADR-0035. Dedicated item-routing mods are not in the pack: one mod owns each
capability, and a substitute routing idiom bypasses the ladder.

## 2. The Solar System

Six bodies, seven destinations — Terra Orbit is not a planet in its own right but Terra's orbit,
as every body here has one. Internal identifiers use Factorio's names; the names players see are
Latin and Greek, supplied by lang files only (ADR-0004).

| Display name | ID | Thematic puzzle | Core mechanics |
| --- | --- | --- | --- |
| Terra | `overworld` | Standard starter loop | Basic extraction, first automation, the first rocket launch. The only body with enemy nests (ADR-0055). |
| Terra Orbit | `overworld_orbit` | Orbital logistics | Asteroid chunks (ice, carbon) processed into Space Science. |
| Ignus | `vulcanus` | Thermal and fluid processing | Tungsten, infinite lava extraction, molten metal solidification, strict slag management. |
| Electro | `fulgora` | Recycling and electricity | No natural ores (ADR-0009). Generated ruins are the source of scrap. Solar and lightning power. |
| Sapros | `gleba` | Organics and spoilage | Agricultural automation under spoilage time limits. |
| Gelida | `aquilo` | Cryogenics and heat management | Fluids freeze without active heating; ammonia chemistry; every process needs a thermal budget. |
| Atlantis | `shattered_planet` | Endgame destination | Open: whether it is a mechanic or only a name is #131. |

Only Terra is played. Interplanetary travel waits on #340, and every other body's worldgen is parked
under `kubejs/parked/` until it can be reached (ADR-0060). What each body's dimension is built on
is decided when travel is.

## 3. Establishing a Presence

Open. How a player arrives on a body other than Terra, and what they find there, waits on
interplanetary travel (#340).

- **The Vanguard Kit** and its **Gateway Flag** predate the fidelity standard and were recorded as
  suspect by #100: Factorio's later arrivals are a platform overhead and a cargo drop, not a
  deployable kit. Whether the Kit survives is decided with travel (#340).
- **Platforms** were GCyR space stations (ADR-0006), and GCyR left with ADR-0060. What a Platform
  is built on is #340's, and asteroid mining at Terra Orbit is #114.
- **Atlantis** is #131.

## 4. Moving Things

### The launch

The launch is the payoff moment, physical and watchable, and is never simulated. The Rocket Silo is
a `planetaryfactory_core` machine that makes Rocket Parts and launches, and it does not wait on an
Oritech release (ADR-0080). What a launch does, with no travel yet to give it a destination, is
#378.

### Travel and cargo

Open, on #340: player transit, unattended cargo and its **Flight** timer, and the terminals that
load and receive it.

### Export infrastructure

**Localized assembly** — planet-specific buildings are craftable only where they belong, forcing a
factory on every planet rather than one factory and a shipping lane. It is the ledger's
*Planet-locked buildings* row (#115).

## 5. Crafting and Recipe Routing

**The crafting grid is removed** (`#90`), and the Personal Assembler replaces it permanently.
Automation pressure comes from which recipe type each product uses — a recipe is craftable
wherever its type is, and that routing is inherent to the recipe system rather than something to
script.

Policy: **the corpus authors every recipe it contains** (ADR-0031). Factorio's own `category` decides
which machine a recipe lands on (ADR-0017 as amended by `#93`), not any mod's stock assignment, and
a stock recipe ships only where a decision names it and names the surface it is crafted on —
everything else is swept (ADR-0034). Where two installed mods supply one material form, the
emitted recipe names FTB Materials' item rather than the shared tag (ADR-0061).

### Science and research

Progression is **Factorio's science packs** — four packs plus an unscienced rung 0 (`automation`,
`logistic`, `chemical`, `production`), each rung granting a capability the next rung's production
physically requires. The gate is **Researchd's Research Lab**, fed by pipe and consumed unattended;
FTB Quests keeps the book and the reward surface but does not gate. No tech mod's own tree is the
ladder. The spine is recorded in ADR-0018; which mod owns each rung is ADR-0017 as amended by
ADR-0060.

The beat-by-beat arc from spawn to the first launch — chapters, hour budget and what each rung
grants — is `docs/spec/terra-progression.md` (`#34`).

**Terra's science packs are inert items. Sapros's science pack decays** — the buffer-as-liability
puzzle belongs to that body and is specified with it, not here.

### The Personal Assembler

The vanilla inventory screen itself (ADR-0066), and the player's only hand-crafting surface. Every
fluid-free `crafting` recipe reaches it (`#88`), on the pack's own `planetaryfactory:assembling`
type: the hand set is a predicate over the Assembling Machine's recipes, so one emitted recipe
serves both surfaces (ADR-0063). It is not an item — there is nothing to craft, nothing to lose and
nothing to grant. Hand-crafting stops being how you *produce* long before it stops being available,
which is a pacing outcome rather than a removed feature.

It is a **planner, not a queue** (ADR-0038): request a recipe whose ingredients you lack and the
sub-crafts are queued for you, as Factorio's hand-crafting does. How a request is made — EMI's
**Fill Recipe** on the inventory screen, left for 1, right for 5, Shift for all, and the
**Crafting Plan** when a craft cannot start — is ADR-0064 to ADR-0066. Plans run serially at
Factorio's durations (ADR-0029), pay their whole raw cost at once, and pause rather than drop a
craft that will not fit.

**EMI is a hard requirement of the pack.** The Assembler has no recipe browser of its own — a
client-side mod is load-bearing for a core verb, which is acceptable in a curated pack with a fixed
manifest and is recorded here so it does not read as an accident later. JEI stays and is the
fallback (ADR-0060). The **2x2 grid is removed**, server-guarded, and the vanilla recipe book goes
with it (`#140`): a 2x2 that still works teaches that planned crafting is optional.

It is unpowered, works anywhere, and is deliberately slow. Because it can never be missing, the
opening's job is to **teach** it — Terra's wreckage and the quest book's tooltip hint, not a grant
(`#100`). Whether it gains upgrade modules is open at `#99`, reframed as whether Assembler speed
scales by science rung so that one ladder remains.

## 6. Emission and Enemies

Emission is the pack's own, since no installed mod ships a pollution system (ADR-0005). Its shape
is Factorio's: emission scored per entity from the corpus, spreading and decaying chunk by chunk,
absorbed by enemy nests that turn it into attacks, with evolution and expansion on Factorio's own
rules, and nests and waves held as saved data with entities only their rendering (ADR-0055). Terra
is the only body with nests. What emission does on other bodies is the ledger's *Pollution* row.

## 7. Open Questions

- **Cross-mod recipe audit.** The specific stock recipes that let a player skip a machine step have
  not been enumerated. ADR-0034's sweep removes everything unnamed, so the audit is over the
  survivor list rather than over any one mod's catalogue.
- **Arrival, platforms, travel and cargo** — #340, with the Vanguard Kit's fate (§3).
- **Atlantis** — #131.
- **What a launch does** — #378.
- **Assembler upgrade modules** — #99.

## 8. Resource substitution policy

Per-body content is drawn from `docs/planets.md`, a transcription of Factorio's own
resource lists organised under Factorio's names. Every resource named there is resolved by this rule,
applied in order:

1. **An FTB Materials form.** Always preferred: FTB Materials is the pack's item layer (ADR-0061).
2. **An item an installed mod already registers**, where FTB Materials has none, chosen for having
   the item rather than for owning anything.
3. **An item the pack registers itself**, only where the puzzle depends on the thing existing
   separately from anything that already exists, in KubeJS startup scripts (ADR-0015).

All three tiers rank *where an item comes from*. None of them says which mod owns the capability the
item feeds, or which machine its recipe lands on — those are ADR-0017's table and Factorio's own
`category`, and neither follows from an item's source.

Scrap on Electro is the clearest candidate for tier 3, its whole role being to be a distinct thing
that recycles into a spread of outputs. Each body ticket argues only about its own exceptions to
this rule, and checks FTB Materials' coverage of its resources itself.

Save compatibility is not a design constraint for this pack. Worlds under `saves/` are disposable test
state; where a change is only observable in a fresh world that is a testing fact, not a reason to
defer it.

## Delivery sequence

Terra comes first and is the only body in play: its flow to the first rocket launch is
`docs/spec/terra-progression.md`, and the launch itself is #378. Every other body waits on
interplanetary travel (#340).

Once travel exists, bodies land one at a time, each finished before the next, and each body is two
tickets: a **`Body:`** ticket delivers terrain, stone, resources, fluid deposits, dimension marker
and sky — everything that makes arriving there complete — and a **`Puzzle:`** ticket, cut after its
body ships, delivers that body's processing chains, machine restrictions and craft gating from
`docs/planets.md`.

1. **Body: Ignus** — carries the scaffolding: the first custom stone, worldgen layer and noise
   settings, so every body after it is content against a proven pattern.
2. **Body: Electro** — no natural ores by design (ADR-0009); ruins as the source of scrap.
3. **Research: spoilage** — how this pack implements decay on industrial intermediates. Blocks Sapros,
   because specifying it first would be inventing the answer rather than finding it.
4. **Body: Sapros** — `#23`, cut once the spoilage research closes, and not before.
5. **Body: Gelida** — the last body before the endgame; fluids and no solid ore.
6. **`Puzzle:` tickets** — one per body, each sequenced after its own body ships.

Emission and enemies (ADR-0055) are Terra's and are not held behind travel.
