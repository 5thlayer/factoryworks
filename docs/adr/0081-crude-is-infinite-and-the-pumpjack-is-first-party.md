---
status: accepted
supersedes: [86]
---

# Crude is infinite, and the Pumpjack is first-party on a well Factorio's autoplace deals

Terra has had no crude source since ADR-0060 removed GregTech's bedrock fluid deposit (#86). A ruling
on 2026-09-11 (`docs/research/oritech-coverage.md`, the pumpjack row) proposed finite crude: Oritech's
oil springs retargeted at Terra, drained by Oritech's Pump. No ADR adopted it.

**Decision (#377).** Crude is Factorio's infinite resource.

- An **oil well** is one block that holds an amount and cannot be broken.
- A **Pumpjack** is a `planetaryfactory_core` 3x3 footprint machine on the pack's own seam
  (ADR-0059). It is placed only on a well and draws 90 kW plus a 3 kW drain from a pole.
- Each cycle yields `10 × amount / normal` crude, capped at 1,000, and takes
  `infinite_depletion_amount` off the well. `normal` is 300,000.
- Depletion stops at the higher of `minimum` (20% yield) and 20% of the well's starting amount. The
  second half of that floor is the engine's, stated only on the wiki's *Crude oil* page.
- A field is a placed structure built from the corpus. It uses the outfield law's radius and height,
  its shape mask and its spacing.
  - Each column of the mask carries a well with probability 1/96.
  - Wells stand at least 3 blocks apart, from the collision box.
  - A well's amount is `(48·H·cone(r) + 220000) · max((1000+d)/2600, 1)`, with no cap. A far well
    starts above 100%.
  - Fields go on land only, never within the first 150 blocks, and never on a column already holding
    ore.
- Oritech's `oil_spring` biome modifiers become `neoforge:none`, so the well is the only crude
  source in the pack.
- An oil field gets a patch marker like an ore patch, when a Radar charts it or a player walks past
  it (ADR-0079). The marker shows the field's summed yield and is never removed.

The ruling was a simplification of the kind Factorio fidelity overrides. A finite pool also has no
Factorio meaning. Oritech's Pump runs at a hard-coded bucket every 5 ticks for 512 FE, which cannot
reach 10/s at 90 kW without Java. The pack already has the two parts a faithful well needs: a block
that holds an amount (ADR-0041) and a per-resource structure placed from the extracted law (ADR-0045).

**1/96, not 1/48.** Crude's autoplace is
`random_penalty{source = clamp(P, 0, 1), amplitude = 1/random_probability}` with
`random_probability` 1/48. `random_penalty` "subtracts a random value in the [0, amplitude) range
from source", which leaves a positive value on 1/48 of the tiles in a field. The
`probability_expression` it feeds is then "evaluated … to determine probability", which is a second
roll, averaging ½ over those tiles. The corpus stores 1/48 as extracted, and the 1/96 is derived in a
unit-tested class. The richness keeps its ×48, so a field holds about half what the 1/48 reading
would give.

## Departures

- **Crude leaves through any face.** Factorio's pumpjack has one rotatable output.
- **The model is Oritech's Pump, scaled to 3x3.** If the scaled model reads badly, the Pumpjack
  becomes 1x1 and the 3-block well spacing stays as slack. That is a human check on delivery.
- **The pumpjack's two module slots** wait for modules.

## Considered Options

- **Adopt the 2026-09-11 ruling.** Rejected. Crude would be finite, run at a rate no config reaches,
  and come from a 1x1 block.
- **Oritech's spring as the visible well, with a core pumpjack reading an amount stored on it.**
  Rejected. It keeps a flood-fill pool of source blocks that has no Factorio meaning.

## Consequences

- The ledger's *Infinite late-game resource (oil-style yield decay)* sub-rule is `planned` under
  #377, not `excluded`.
- `docs/spec/terra-progression.md`'s Fluid Drilling Rig beat and the coverage doc's pumpjack row no
  longer describe crude.
- The `pumpjack` item-map row names `planetaryfactory:pumpjack`.
