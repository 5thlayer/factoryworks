# Terra: spawn to first launch

The beat-by-beat arc for Terra, against ADR-0018's **20–25 hours** for a Factorio-literate player
following the book. The spine is ADR-0018, the ladder and its gates are ADR-0097, and the recipes
are ADR-0031's corpus. This document is the *route* through them.

**Beats name items and surfaces, never quantities.** Prices live in the corpus and in `#42`'s slot
lists and move constantly; a beat sheet carrying numbers is stale by the next recipe edit.

## The gates

Every chapter opens on a Factorio **gate**: a science rung, or a trigger technology researched by
doing something rather than by packs (ADR-0097). The pack invents no boundary of its own. The
ladder is three science rungs plus rung 0, and `production` comes after the launch.

| Gate | Cost | Opens |
| --- | --- | --- |
| — | — | The opening: the wreck, the Personal Assembler, first plates |
| `steam-power` | trigger: craft 50 iron plate | Steam: the Offshore Pump, the Boiler, the Steam Engine |
| `electronics` | trigger: craft 10 copper plate | The first watt: green circuits, the small pole, the Lab |
| `automation-science-pack` | trigger: craft a Lab | The Lab: rung 1 becomes purchasable |
| `automation` | automation | Rung 1: Assembling Machine I |
| `logistic-science-pack` | automation | Rung 2: movement at scale |
| `oil-processing` | trigger: mine crude oil | The oil chapter: the Refinery and the Chemical Plant |
| `chemical-science-pack` | automation + logistic | Rung 3: the same barrel, split finer |
| `uranium-processing` | trigger: mine uranium ore | The reactor, a branch the launch does not need |
| `rocket-silo` | automation + logistic + chemical | The launch |
| `production-science-pack` | automation + logistic + chemical | After the launch: Assembling Machine III, express belts, reprocessing |

Each row is Factorio's, not chosen. `tests/factorio/test_tech_extract.py` reads this table and fails
when a row stops matching `data/factorio/technology.json`. It also fails if the launch comes to need
a production pack, or if the silo comes to require the reactor.

**There is no hour budget per chapter.** This page had one, priced against five chapters on four
rungs, and none of those chapters survived ADR-0097. Its numbers were guesses from before any recipe
was emitted, and guessing is how rung 3 came to hold a nuclear chapter its 4–5h could not
(ADR-0033). The 20–25h total stands. How it splits across the gates is read off a pace run (`#170`),
not written here first. One reading carries over: the chapters before rung 1 are slow for a player
fluent in Factorio, because every block in them is the pack's own and nothing there is muscle
memory.

## Who teaches what

**The research graph shows cost. The book explains the verb.** Researchd's UI lists what a node
unlocks and what the Lab will eat; the quest book never repeats a price. A chapter opens on a gate
and its quests are *here is what this block does and why you want it*.

They are kept apart on purpose. Prices move every time a recipe is touched; verbs do not. Overlap
them and the player reads neither.

---

## Opening — the first twenty minutes

The wreck is `#100` and `#134`: indestructible, habitable, one cargo hold, and you spawn inside it.

| # | Beat | Surface |
| --- | --- | --- |
| 1 | Wake up inside the wreck. The book is in your inventory; its tooltip points at the inventory. | — |
| 2 | Open the inventory. The Personal Assembler is already there. Craft one thing, badly, slowly. | Personal Assembler |
| 3 | Leave. Three ore fields are visible from the door. | — |
| 4 | Place the Stone Furnace and the Burner Mining Drill from your pocket, the drill facing the furnace. First plates. | hand |
| 5 | Walk out past the starting fields. The outfield's patches are flush with the topsoil and visible on foot. | — |
| 6 | Drill → belt → Furnace → chest. Something runs while you watch. | machine-fed |

Beat 6 is the twenty-minute mark and the first machine-fed beat in the pack.

**What you start with.** Factorio's own split — tools in your pockets, materials from the ship
(`#100`), and Factorio is famously stingy about both.

- **Pocket**: one Stone Furnace, one **Burner Mining Drill**, the **Engineer's Iron Pick**
  (ADR-0039). *This read "one Furnace, one LP Steam Miner"; ADR-0040 deleted that miner and
  ADR-0043's burner rig took its place (`#193`). It also read "the prospector (ADR-0019)", which was
  `gtceu:prospector.lv`: ADR-0056 ruled it was never canon, ADR-0045 put every ore patch on the
  surface so nothing is buried to prospect, and GregTech left with ADR-0060. The charting gesture is
  open on `#116`, and until it is answered beat 5 is walking (`#323`).* A rig covers four tiles
  and beats hands even at 0.25 items/s, which is what earns it a slot in a pocket Factorio is
  famously stingy about.
- **Hold**: iron plate, copper plate, coal. Single digits, matching freeplay's eight-plate debris
  chest.
- **No weapon.** Factorio hands you a pistol; here the wreck is the answer to night one (`#134`),
  and a door is a better answer than a pistol.

**The Pick is the one tool, and it is craftable.** `wood ×1` plus `iron plate ×1`, both of which the
opening already puts within reach — a log comes off a tree barehanded and the hold carries plates.
It is in the pocket so that beat 4 does not open on a crafting detour, and craftable so that losing
it is not a dead save. There is no axe, no shovel and no shears; there is no second tier until
`steel-axe` at rung 1 (ADR-0039).

Nothing in the hold is otherwise unobtainable. It removes the pre-tool grind; it does not seed a
tier. The moment the hold contains a green circuit, rung 0 stops being taught.

**Beat 2 is the one to playtest first.** The entire pack rests on a player finding a crafting surface that
nobody handed them. `#95` chose that deliberately — there is nothing to grant and nothing to lose —
but the cost is that discovery is the opening's only job.

---

## Steam — `steam-power`

*Researched by crafting 50 iron plates, which the opening's furnace is already doing.*

**Granted**: pipes, the **Offshore Pump** (ADR-0050), the pack's **Boiler** (ADR-0048) and the
pack's **Steam Engine** on Oritech's engine (ADR-0077). Every block on this list is the pack's.
The rest of what the chapter runs on was there from the first minute: the Burner Mining Drill, the
Stone Furnace, the belt, the wooden chest and the barrel. Barrelling is gated by nothing (`#93`).

| Beat | Fed by |
| --- | --- |
| The Burner Mining Drill over the starting iron, facing a furnace. It is your ore supply from here on, and it feeds what it points at. | hand |
| Feed it coal, and belt what it does not hand straight over to the furnace bank. | machine |
| An Offshore Pump on the hub pool, a Boiler, a Steam Engine. Water becomes steam, and steam becomes the engine's own charge. Nothing draws it yet. | machine |

**What the next chapter needs it for**: the engine's charge is the first energy in the pack, and it
has nowhere to go until a pole reaches it.

---

## The first watt — `electronics`

*Researched by crafting 10 copper plates.*

**Granted**: copper cable, green circuits, the inserter, the Lab and the small electric pole.

| Beat | Fed by |
| --- | --- |
| Green circuits by hand. They are Assembling Machine I's own key (`#55`). | Personal Assembler |
| A small pole beside the Steam Engine. The pole carries energy to what stands in its supply area, with no wire to the engine (ADR-0062). | machine |
| Craft a Lab. | Personal Assembler |

The inserter's place is the loader's (ADR-0076), so this is where the belt first meets a chest.

---

## The Lab — `automation-science-pack`

*Researched by crafting a Lab.*

**Granted**: the `automation` science pack.

| Beat | Fed by |
| --- | --- |
| Place the Lab and hand-feed it its first `automation` packs. | hand |

`automation` packs are Personal-Assembler-only forever (`#42`), so this chapter's job is to make the
plates that feed them faster than your hands can. The player should leave it slightly sick of
walking packs to the Lab. That is the argument for rung 1, made by the game rather than by the book.

---

## Rung 1 — `automation`

**Granted**: Assembling Machine I, **steel** and the steel chest, and — off `steel-processing`, at
no pack cost — **`steel-axe` and the Engineer's Steel Pick**, which halves seconds-per-ore from 2.0
to 1.0 (ADR-0039). Then the Electric Mining Drill, the Radar (ADR-0079), the splitter and the
underground belt, and the loaders' second tier.

**What rung 2 needs it for**: the belt build-out is an assembly problem — a belt costs one item per
block (ADR-0060) — and everything past here is assembled.

| Beat | Fed by |
| --- | --- |
| Assembling Machine I. The Personal Assembler stops being how you *produce* — it never stops being how you *craft*. | machine |
| Feed the Assembler from the belt, not from your hands. | machine |
| Steel, and the Steel Pick it triggers. Mining doubles. | Personal Assembler |
| The Electric Mining Drill, on the grid, and the outfield patches it makes worth reaching. The Radar charts them. | machine |

**Assembling Machine I has no fluid tanks** (ADR-0018, amended by `#125`). It cannot run the
corpus's `crafting-with-fluid` rows, and it is not meant to — the tier that can drink a fluid
arrives on rung 2. This is the machine's visible shape, not a hidden gate.

---

## Rung 2 — `logistic`

**Granted**: the fast belt and its splitter, rail and trains, the Steel Furnace (the
core's furnace ladder, ADR-0060), Assembling Machine II, concrete, and the **Pumpjack**
(`oil-gathering`), which is the means to reach oil but not yet anything to do with it.

| Beat | Fed by |
| --- | --- |
| Pipe the Lab. `logistic` packs arrive without you. | machine |
| The fast belt and its splitter. Throughput stops being one number and becomes a choice — `logistics-2` buys a known one (ADR-0060). The express belt costs production science and waits for the launch. | machine |
| Rail and trains. Distance stops being a wall. | machine |
| The Pumpjack stands on an oil well. Crude never runs out: a well's yield falls to a floor and stops there, and a far well starts above 100% (ADR-0081). | machine |
| Oil in the barrel you have had since the opening. A fluid becomes an item, and the belt and the train can carry it. | machine |

**Movement at scale is belts and rail, and that is the whole of it.** Factorio has no mass package
logistics (ADR-0060). What is left is the pair Factorio itself runs on — a belt whose throughput is a
number you choose, and a train for when distance beats the belt.

**Pantographs are not in Terra's first iteration.** Rail is Factorio's `Railway`, which is red +
green and lands exactly here; electrified rail is not a Factorio mechanic, so there is no citation
to honour and no reason to spend a beat on it before Terra ships.

---

## The oil chapter — `oil-processing`

*Researched by mining crude oil, which the Pumpjack does. What it opens costs logistic packs:
the trigger opens a chapter inside rung 2, not a rung.*

**Granted**: the **Oil Refinery**, the **Chemical Plant** (ADR-0096), basic oil processing and solid
fuel, then, on `logistic` packs, sulfur, sulfuric acid, plastic and the red circuit (ADR-0025,
`#125`).

**What rung 3 needs it for**: sulfur buys the `chemical` pack. That is the spine rule, stated
plainly.

| Beat | Fed by |
| --- | --- |
| Oil Refinery and Chemical Plant. Two new machine idioms in one beat, each holding a recipe the player sets, of its own kind (ADR-0096). | machine |
| Solid fuel, and the Steel Furnace that burns it. Fuel throughput becomes a constraint you can feel. | machine |
| Sulfur → sulfuric acid. | machine |
| Plastic, and the red circuit it makes. | machine |

**The chapter's thesis**: *oil exists, and it makes three things you already wanted.* Fuel, sulfur,
plastic. A Factorio player recognises every one of them, which is what carries them through two
unfamiliar machines.

---

## Rung 3 — `chemical`

**Granted**: advanced oil processing, heavy and light cracking, lubricant (ADR-0025), the blue
circuit (`#55`), the Electric Furnace (the core's furnace ladder), rocket fuel and low density
structures.

**What the launch needs from it**: rocket fuel is light oil and solid fuel, and a rocket part is a
blue circuit, a low density structure and rocket fuel.

| Beat | Fed by |
| --- | --- |
| Advanced oil processing. The barrel splits three ways instead of two. | machine |
| Cracking. Heavy → light → gas, and suddenly the ratios are yours to choose. | machine |
| Lubricant. | machine |
| Blue circuits, on acid. | machine |
| Electric Furnace. Fuel stops being a constraint, one chapter after it started being one. | machine |
| Rocket fuel and low density structures. | machine |

**The chapter's thesis**: *the same barrel, split finer.* Cracking is the first beat in the pack
that is about a ratio rather than an unlock — nothing new is revealed, you simply decide what your
oil becomes. For a Factorio-literate player this is the moment of recognition the whole arc has been
building to, so it gets the chapter's weight even though it unlocks the least.

---

## The reactor — `uranium-processing`

*Researched by mining uranium ore, once `uranium-mining` lets a drill mine with the sulfuric acid
rung 2 made (`#58`). It costs chemical packs, and it comes after cracking: the launch needs the
chapter before this one, not this one.*

**Granted**: the Centrifuge, then, on `nuclear-power`, the reactor and the Steam Turbine. The
reactor makes superheated steam, and only the Turbine accepts it (ADR-0033).

| Beat | Fed by |
| --- | --- |
| Uranium past the acid gate, then the Centrifuge. | machine |
| A reactor, and the Turbine that drinks its steam. | machine |

**Nothing downstream needs it.** `uranium-processing` is not among the silo's prerequisites, and
what the Centrifuge makes feeds only the fuel cell, which feeds only the reactor. It is Factorio's
optional power upgrade, and it is here for the player whose steam grid is groaning, not because the
launch asks for it (ADR-0018, amended by ADR-0097). Kovarex costs space science, so Terra's reactors
run at raw 0.7% U-235.

---

## The launch — `rocket-silo`

*The silo costs chemical packs and no production pack (ADR-0097).*

**Granted**: the Rocket Silo and the rocket part (ADR-0080).

Short by design. By now the factory builds things while you watch, and the chapter's difficulty is
throughput rather than novelty.

| Beat | Fed by |
| --- | --- |
| Build the Rocket Silo. The largest machine in the pack. | hand + machine |
| Feed it rocket parts: blue circuits, low density structures, rocket fuel. The count is `#53`'s. | machine |
| Launch. | — |

**The part count is the real final exam.** Nothing is unlocked by it and nothing is taught; the beat
asks one question — *is your factory finished?* — and the answer is a number of hours, not a recipe.
The player watches the count climb.

What a launch *does* is not decided: there is no travel, no platform and no satellite yet, and
`#378` owns it.

---

## After the launch — `production`

Not part of this arc, and named so that its absence reads as a decision. `production` science
carries Assembling Machine III, the express belts and nuclear fuel reprocessing, which is where
Space Age puts them. Its own pack recipe takes a productivity module, which is `#120`'s.

## What this document does not decide

- **Every quantity on this page**, because there are none.
- **The per-chapter hours**, which a pace run reads rather than this page guessing (`#170`).
- **What a launch does** — `#378`.
- **Emission's pre-launch readout** — it keys on a metric ADR-0018 leaves open, so placing a beat
  for it now would be placing a beat to delete later. Decided with the Emission work.
