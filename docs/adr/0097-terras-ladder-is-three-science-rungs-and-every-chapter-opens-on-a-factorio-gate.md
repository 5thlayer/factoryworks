---
status: accepted
supersedes: [26]
---

# Terra's ladder is three science rungs, and every chapter opens on a Factorio gate

`#26` chose four science rungs plus an unscienced rung 0 and put the launch on the fourth,
`production`. ADR-0018 wrote that table down and ADR-0025 re-cut it, and both justify the fourth rung
with one sentence: **the Rocket silo is the technology that costs production science.**

It is not. In the extracted Space Age tree, `data/factorio/technology.json`, `rocket-silo` costs
1000 × (automation + logistic + chemical). No technology among its 28 ancestors costs a production
pack, and `production-science-pack` is not one of them. The sentence is Factorio 1.1's, and the
pack cites 2.x's data. So is the silo's other requirement both ADRs name, rocket control units,
which 2.0 removed: a rocket part is a blue circuit, a low density structure and rocket fuel. This is the fourth recorded instance of the pack's own failure class, which
ADR-0017 named and ADR-0034 counted three times: a claim read off a table without checking the data
underneath (`#136`). Here the table was the pack's memory of Factorio, not the corpus it extracted.

## Decision

**The pre-launch ladder is three science rungs plus rung 0: `automation`, `logistic`, `chemical`.**
`production` is the first rung after the launch and carries what Space Age puts there: Assembling
Machine III, the express belts and nuclear fuel reprocessing.

**A chapter is bounded by a Factorio gate: a science rung or a trigger technology. The pack
invents no boundary of its own.** A trigger technology costs no pack and is researched by doing
something, such as crafting 50 iron plates or mining crude. Five of them open pre-launch chapters,
and they let an oversized rung split at Factorio's own seams instead of the pack's:

| Gate | Kind | Opens |
| --- | --- | --- |
| `steam-power` | trigger: craft 50 iron plate | Rung 0, steam |
| `electronics` | trigger: craft 10 copper plate | Green circuits and inserters |
| `automation-science-pack` | trigger: craft a Lab | The Lab, and rung 1 |
| `logistic-science-pack` | automation | Rung 2 |
| `oil-processing` | trigger: mine crude oil | The oil chapter |
| `chemical-science-pack` | automation + logistic | Rung 3 |
| `uranium-processing` | trigger: mine uranium ore | The reactor |
| `rocket-silo` | automation + logistic + chemical | The launch |
| `production-science-pack` | automation + logistic + chemical | Post-launch |

`docs/spec/terra-progression.md` holds the full list of chapters, one per gate, and
`tests/factorio/test_tech_extract.py` reads that table and holds every row to the corpus.

**The oil chapter opens on `oil-processing`, not on the `logistic` rung.** ADR-0025 hung the
chapter on the rung, so rung 2 carried 69 of the corpus's 163 recipes. The trigger relieves that
without an invented seam. `oil-gathering`, the Pumpjack, stays logistic-tier, so the rung still
grants the means to reach oil and the trigger grants what to do with it.

**The reactor is a terminal branch.** `uranium-processing` is not among `rocket-silo`'s ancestors,
and the only consumer of what the Centrifuge makes is the fuel cell, whose only consumer is the
reactor. Nothing downstream requires it, which is its status in Factorio too, where nuclear is the
optional power upgrade. ADR-0018's spine rule is amended to admit it.

## Considered Options

- **Keep four rungs and argue the fourth on something else.** Rejected: nothing pre-launch needs a
  production pack. The rung would grant only Assembling Machine III, the express belts and
  reprocessing, which the silo does not need. That is a rung whose capability nothing downstream
  requires, the exact thing ADR-0018's spine forbids.
- **Keep four rungs and have the silo cost production anyway.** Rejected: it is a pack-authored
  price on the one technology the pack most wants to read as Factorio's, and ADR-0031 makes the
  corpus the price authority.
- **Three rungs with the chapters left as rungs.** Rejected: rung 2 would then hold the whole oil
  chapter as well as the belt tiers and rail, which is 42% of the corpus in one chapter. The
  triggers are what split it, and they are Factorio's.

## Consequences

- **ADR-0018's rung table and spine sentence, ADR-0025's rung table and rung-4 argument, and the
  sentence of ADR-0096 that kept that re-cut are amended in place** to point here. None of them is
  re-argued there.
- **The hour budget per chapter is withdrawn.** The chapters it priced no longer exist, and guessing
  new figures is how rung 3 came to hold a nuclear chapter its 4–5h could not. The 20–25h total
  stands as ADR-0018's figure. The per-chapter split is a human reading on delivery.
- **The research tree must implement the triggers.** A trigger technology is packless research on
  Researchd (`#42`, and ADR-0033 already does this for `uranium-processing`). Which of the five
  `researchd.js` declares today is the research tree's business, not this ADR's.
- **`production`'s own pack recipe needs a productivity module**, and modules are `#120`'s. With the
  rung post-launch, that stops blocking Terra's launch.
