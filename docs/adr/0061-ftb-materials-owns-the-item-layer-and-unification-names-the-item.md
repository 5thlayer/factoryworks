---
status: accepted
supersedes: [245]
---

# FTB Materials owns the item layer, and unification names the item rather than the tag

This is the third time the pack has had to answer "whose plate is an iron plate". ADR-0053 answered
"GregTech's"; ADR-0056 took GregTech out. ADR-0057 answered "Modern Industrialization's"; ADR-0060
took Modern Industrialization out. Both answers were correct and both died with their subject,
because both put the alphabet inside a **chassis** — a mod that also ships machines, energy and a
progression of its own, and therefore a mod the pack will eventually swap.

ADR-0060 also removed **AlmostUnified**, which is what had been making the previous two answers
stick. Nothing the pack configures collapses the duplicate lines any more, and the pack's two remaining material-bearing
mods each ship a full one: Railcraft Reborn shares **77** `c:` item tags with FTB Materials and
Oritech shares **47**, with **12** shared by all three. `c:ingots/steel` currently resolves to three
different items and six ids collide by bare name between Railcraft and Oritech alone.

## The rule

**FTB Materials supplies every material form in ADR-0021's alphabet. A tech mod supplies machines,
not material forms.**

`ftb-materials` was installed for exactly this job and is the only installed mod that can hold it
without being a hostage: 689 items, no machines, no energy layer, no progression, nothing to swap it
out for. It already ships every form the pack's dead rows named — `iron_plate`, `copper_plate`,
`steel_plate`, `steel_ingot`, `iron_gear`, `iron_rod`, `copper_wire`, `sulfur_dust`, `coal_dust`,
`iron_dust`.

The second half of the rule is the sentence #277 spends: Oritech, Railcraft Reborn and the
SimpleBelts fork are in the pack for **machines and logistics**. Where one of them also ships a plate, the plate is not the
pack's plate.

It replaces ADR-0057, and ADR-0053 with it, as the owner of the item layer. It **amends ADR-0021**:
the alphabet's material forms are now named by FTB Materials' ids, not by whichever chassis is
installed. It **amends ADR-0031**: "borrow" for a material form means borrow from FTB Materials,
and a tech mod's competing line is never the lender.

## How the duplicates are suppressed

ADR-0053's "unification replaces rather than broadens" is **kept as intent and retired as a
mechanism**. Without AlmostUnified there is no configured arbiter to broaden or replace anything, so:

**A pack-emitted recipe names an item, not a `c:` tag, wherever more than one installed jar
populates that tag.** An ingredient stated as `#c:ingots/steel` accepts all three steels and is
therefore not a decision; `ftbmaterials:steel_plate` is. Tags stay legal where exactly one installed
jar populates them — the two emitted smelts ride `#c:raw_materials/iron` and `#c:raw_materials/copper`,
which no installed mod adds to, so they keep doing what they are for: accepting vanilla's raw ore and
the pack's own.

Four things are deliberately **not** done:

- **No tag overrides.** Shipping a datapack that empties Railcraft's and Oritech's halves of 77 tags
  makes the pack responsible for two mods' internal recipes for as long as both are installed, and
  breaks them silently when either updates.
- **No hiding.** An item the player cannot make is already invisible where it matters, because
  ADR-0034's sweep is default-deny: a competing plate with no surviving recipe is not a parallel
  escape, it is a texture in EMI with no route. Hiding it is EMI configuration, which is a delivery
  decision, not this one.
- **No reliance on FTB Materials' own unifier.** The jar carries one (`unification/RecipeTweaker`,
  `LootTableUnifier`), driven by a `config/ftbmaterials/unifier-db.json` it does not ship; the
  GameTest run logs `can't load unifier DB`. Whether to build and commit that DB so Railcraft's
  and Oritech's own recipes are rewritten too is a separate decision with its own world-load
  check. This ADR's rule holds with or without it, because an emitted recipe that names the item
  does not need anything rewritten.
- **No first-party plates.** ADR-0053's argument outlives its subject — a plate is an item with a
  texture, a tag and a recipe, and authoring forty of them buys nothing.

## Where the pack's own items sit

`factoryworks_core`'s items and the SimpleBelts fork's belts **stay out of the `c:` material
tags**. Neither is a material form: the mod's items are mechanisms (furnaces, rigs, the Boiler, the
picks, the circuits ADR-0031 authors because they carry progression), and a belt is logistics. A
mechanism joining a material tag is how a machine becomes an ingredient by accident, which is the
failure this ADR's whole mechanism is one instance of.

## What this costs

Seven item-map rows move, all of them one line: `iron-plate`, `copper-plate`, `steel-plate`,
`iron-gear-wheel`, `iron-stick`, `copper-cable` and `sulfur`. Three of them are the smelts
`scripts/check-datapack-load.py` watches the game reject today. They go green with this ADR's
rewrite only once the GameTest server also loads FTB Materials (and the FTB Library it requires),
because that server is vanilla plus the mod jar: `mod/build.gradle` puts both on the dev runtime
classpath. It is the one foreign mod the harness loads, because the pack's data names its items.

`copper-cable` is a note against ADR-0031 rather than a contradiction of it. The row borrowed before
and borrows now; what changed is that its previous target belonged to a mod the pack does not have.

The other 24 dead rows — the machines, blocks and oil fluids — are **#277's**, and the 67 `undecided`
rows stay with #166 and #144. What this ADR hands both is the rule above, not an answer to any
particular row. It does note that FTB Materials has made some of the `undecided` dusts answerable,
which is #166's to spend.

## The check

`tests/pack/test_item_map.py` is the guard, and it is the thing neither previous ADR had. Two
assertions: every item-map target resolves against the **installed jars**, and no emitted ingredient
names a `c:` tag more than one installed jar populates. The rows #277 owns cannot resolve until it
lands, so they ship in a deferral table naming that ticket, in `check-datapack-load.py`'s idiom — a
stale entry fails, so the guard re-arms one row at a time rather than the assertion being weakened.

A previous chassis change re-opened this question twice and nothing failed either time. Now
something does.
