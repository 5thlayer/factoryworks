---
status: accepted
supersedes: [89]
---

# The nuclear chapter's machines are pack blocks on Oritech's models, and Terra's heat layer comes later

ADR-0033 accepted three machines for Terra's nuclear chapter — the Centrifuge, the Nuclear Reactor
and the Steam Turbine — on a GregTech chassis, and sent the heat network to Gelida. GregTech left
with ADR-0060. This restates the three on the pack's current idiom (ADR-0060, ADR-0096) and
reverses where the heat layer goes. #135 is the spec.

## The heat layer is Terra's, later

Nuclear is a late, optional branch on Terra: `uranium-processing` is not among `rocket-silo`'s
ancestors (ADR-0097). That is the reason the first cut ships ADR-0033's flattening — the Reactor
emits **Superheated Steam** directly — and it is not a reason for Terra never to have the heat
layer. ADR-0033 already split heat into two systems: **conduction** (temperature, 1 MJ/°C,
differential flow, the 500 °C exchanger threshold) and **freezing** (adjacency above 30 °C,
per-entity draw, immunity). The conduction layer — heat pipes, the Heat Exchanger and the
neighbour bonus — comes to Terra with #497. Gelida keeps only the freezing layer.

**Superheated Steam stays a fluid of its own for good.** When #497 lands, the Heat Exchanger
becomes its producer and the Reactor makes heat instead; the Steam Turbine and the two-way fence
(the Turbine refuses Steam, the Steam Engine refuses Superheated Steam) do not change.

## The three machines

- **Centrifuge** — a crafting-chassis machine (ADR-0096) with its own recipe type, `centrifuging`,
  wearing Oritech's **Foundry** model and taking its footprint. The Chemical Plant already wears
  Oritech's Centrifuge. Uranium processing's 0.7% / 99.3% split is a per-result probability the
  chassis rolls (#502).
- **Steam Turbine** — a second pack block on Oritech's Steam Engine entity, behind the pack's
  Steam Engine mixin (ADR-0062, ADR-0077), with its own spec row read from the corpus and a fluid
  filter that admits only Superheated Steam. It wears the Steam Engine model painted apart.
- **Nuclear Reactor** — first-party: a 5x5 footprint dressed in Oritech's reactor block models.
  It burns Uranium Fuel Cells at Factorio's 40 MW, hands back depleted cells, and turns water into
  Superheated Steam. Oritech's reactor makes FE from its own heat model, so its tick cannot be
  borrowed; its recipes are swept and its blocks are not Obtainable. Its rods, heat pipes and vents
  are #497's art candidates. The neighbour bonus is `excluded` until #497.

## The isotopes and the cells

**U-235 and U-238 are pack items on FTB Materials' `uranium_ingot` texture, referenced and never
copied** — FTB Materials is All Rights Reserved. U-235 wears it untinted and U-238 carries a
darkening constant tint, since a tint can only darken and Factorio's rare isotope is the brighter
green. FTB Materials has no isotope forms, and mapping U-238 onto its ingot would let an ingot from
any source count as U-238. **Both fuel cells are pack items**: Oritech's pellets carry Oritech's
burn times.

## Considered Options

- **Keep the heat network Gelida's** (ADR-0033). Refused: its argument was that conduction must
  wait for Gelida's design, but ADR-0033 itself separated conduction from freezing, and the layout
  puzzle is the nuclear chapter's own gameplay.
- **Build the heat layer now.** Refused for this cut: the branch is optional, and the flattened
  reactor ships the chapter while #497 is designed.
- **Borrow Oritech's reactor**, as a structure with its tick replaced. Refused: a free-size shell
  with rods as fuel slots is not a footprint machine (ADR-0077), and nothing of its tick survives.
- **A first-party Turbine generator.** Refused: it rebuilds the burn, row chaining, pole pull and
  fraction carry the Steam Engine already has and #292 already tests.
- **Move the Chemical Plant off Oritech's Centrifuge.** Refused: it reopens a closed ticket for a
  model.

## Consequences

- ADR-0033's "The real heat network is Gelida's" section is superseded; its fluid split stands.
- `heat-pipe` and `heat-exchanger` stay `undecided`, now pointing at #497, and the ledger's heat
  sub-rule is `planned` on Terra.
- The corpus gains a reactor row and each recipe result's `probability`.
