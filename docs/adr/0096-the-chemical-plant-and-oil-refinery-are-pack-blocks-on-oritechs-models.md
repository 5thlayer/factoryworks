---
status: accepted
---

# The Chemical Plant and the Oil Refinery are pack blocks on Oritech's models

ADR-0025 put the oil chapter on two machines the pack registered on a GregTech chassis: a
single-block Chemical Plant and a multiblock Oil Refinery, each sized by `setMaxIOSize`. GregTech
left with ADR-0060, and #277 decided that both come back as `planetaryfactory_core` blocks. Neither
exists, so the converter holds back every `chemistry` and `oil-processing` recipe and the oil
chapter cannot be built (#486).

**Decision (#486).** Both are pack blocks on the **Assembling Machine**'s chassis (ADR-0071): each
holds a **Held recipe** set by the player, filters its inputs to it, and reads its figures from the
corpus.

- **The Chemical Plant** is `planetaryfactory:chemical_plant`. It wears Oritech's Centrifuge model
  and renderer and stands on the Centrifuge's 1x1x2 footprint. It takes up to 2 items and 2 fluids
  in and gives 1 item and 1 fluid out.
- **The Oil Refinery** is `planetaryfactory:oil_refinery`. It wears Oritech's Refinery base with
  both chamber layers, placed and broken as one 3x2x4 footprint from one item (ADR-0077). It takes 2
  fluids in and always gives 3 out. Oritech's module mechanic is not used: no chamber is an item, and
  none of Oritech's refinery tanks or output routing is read.
- **Each runs a recipe type of its own**, `planetaryfactory:chemistry` and
  `planetaryfactory:oil_processing`, named after Factorio's categories as `smelting` is. Both share
  the Assembling Machine's recipe record and codec, so all three types have one JSON shape. The
  converter writes each type under a folder of its name, and each type has its own EMI tab and Fill
  Recipe. A machine holds only a recipe of its own type.
- **Tank and slot counts follow the recipes**, not the Factorio entity: the entity's second output
  box on the Chemical Plant is one no recipe fills, as ADR-0025 already found. Tank volumes,
  crafting speed 1, 210 kW and 420 kW, and the drain are the corpus's, at ADR-0060's 1 FE = 100 J.
- **Fluid amounts are the corpus's**, emitted by the converter's existing rule, with no factor.
- **Fluid faces are unsided**, on every block of the footprint. An insert goes to the input tank
  whose fluid the Held recipe names, a fluid it does not name is refused, and extraction reads only
  the output tanks. Each refusal holds on both overloads (`GuardedResourceHandler`).
- **The Chemical Plant's item face** filters each ingredient into its own slot (ADR-0074). The
  Refinery has none.
- **The stall order is the Assembling Machine's**: output room, then ingredients, then the lock,
  all before any energy is drawn. On the Refinery, output room means room in every output the Held
  recipe fills, so one full fraction stops it.
- **Addons.** The Chemical Plant keeps Oritech's speed and efficiency addons live, meaning what they
  mean on the Assembling Machine until #120 decides otherwise, and refuses the Fluid and Combi
  addons: the machine already has its tanks. The Refinery model has no addon slots.

Both machines stand on their model's footprint rather than Factorio's 3x3 and 5x5, as the
Assembling Machine does (ADR-0072), which asked the question per machine and left it to this one.

**What ADR-0025 keeps.** Its ratios and its rule that sulfur is petroleum-derived stand. Its section
*The two machines* and its *×10* rule are replaced by this record, and its rung re-cut by ADR-0097.

**Considered: one recipe type, split by category.** `planetaryfactory:assembling` would carry the
`chemistry` and `oil-processing` categories and each machine would filter by category. Rejected:
EMI shows a tab per recipe type, so both machines' recipes would sit in the Assembling tab, and
Fill Recipe from there would have to refuse by category on every machine.

**Considered: Oritech's module mechanic.** The Refinery would be Oritech's, gaining an output per
chamber layer the player adds. Rejected: a refinery with fewer than three outputs cannot run
advanced oil processing, and Oritech's output routing doubles one fraction in place of another,
which breaks the ratios a refinery bank is built by.

**Considered: the Assembler model for both.** Both machines would reuse the Assembling Machine's
model and footprint. Rejected: the oil chapter would be three machines that look alike, where
Factorio's three read apart at a glance, and Oritech already ships a Refinery and a Centrifuge
drawn for fluids.

**Considered: fixed pipe ports.** Each fluid would enter and leave at one block face, as in
Factorio. Rejected: Oritech's models mark no ports, so a player would have to find them by trial,
and routing by the Held recipe already keeps each fluid in its tank.

**Why no factor on the amounts.** ADR-0025 multiplied every amount by ten so one craft took a
bucket of crude. Crafting speed and tank volumes are now the corpus's, and a factor on the recipes
alone would make every tank ten crafts smaller than Factorio's. The ratios, which are what a
refinery bank is built by, are the same either way.

**Consequences.**

- `data/pack/category-map.json`'s `chemical_plant` and `oil_refinery` machines name the two new
  types, and the item map's `chemical-plant` and `oil-refinery` rows name the pack blocks, once
  each is registered. Until then both carry `blocked_by` on #486.
- The stock-recipe sweep gains a survivor per type, each naming its machine as the surface
  (ADR-0034).
- Recipe ids read `planetaryfactory:chemistry/<name>` and `planetaryfactory:oil_processing/<name>`,
  and the research tree unlocks those.
- A Factorio player reads both machines as smaller than Factorio's, the cost ADR-0072 already
  accepted for the Assembling Machine.
- Oritech's own Refinery and Centrifuge stay recipe-removed and hidden.
