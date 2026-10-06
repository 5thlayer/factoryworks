---
status: accepted
---

# The Pack's Chemical Plant is Craftworks' Chemical Plant

ADR-0096 made the Chemical Plant a Pack block on the Assembling Machine's chassis, wearing Oritech's
Centrifuge model on its 1x1x2 footprint, running a recipe type of its own. ADR-0109 takes Oritech out of
the Pack and ADR-0115 gives the machine to Craftworks. Craftworks 0.4.2 has a Chemical Plant
(`craftworks:chemical_plant`) that holds a `chemistry` recipe of `craftworks:assembling` and moves its fluid
through Fluid Connections (Craftworks ADR-0015).

**Decision (#582).**

- **The Chemical Plant is `craftworks:chemical_plant`.** The item map names it `borrowed` for
  `chemical-plant`, and the Pack registers no Chemical Plant block, part, item, block entity type, model,
  loot table or renderer. It stands on Craftworks' 3x3 footprint two blocks tall, which is Factorio's tile
  square again (ADR-0059), and the art is Craftworks' own. Oritech's Centrifuge model goes and nothing
  takes its place (ADR-0122).
- **Factorio's `chemistry` recipes are `craftworks:assembling` recipes** with `category: chemistry`. A
  recipe keeps its id, `factoryworks:chemistry/<name>`, so no research unlock moves: the converter reads
  the folder from `recipe_dir` in `category-map.json`, since the id is the file's path and no longer its
  type's. `factoryworks:chemistry` is no longer a recipe type, and the chassis keeps one family,
  `oil_processing`. Craftworks' codec requires an `ingredients` and a `results` key, though either list
  may be empty, so the converter writes both for every `craftworks:assembling` recipe: seven of the
  twelve have no item ingredient or no item result.
- **The Pack's fluid handling for the machine is none.** Its Fluid Connections pull from and push into
  whatever block they face, so a Pipeworks pipe or tank beside one feeds it and drains it. The Pack's
  GameTest holds that a segment of petroleum gas makes plastic and that sulfuric acid reaches a tank
  through a pipe, and the machine's own behaviour is Craftworks'.
- **The `chemical-plant` machine spec, its Replace Group, its EMI category and its item face go.**
  Fast Replace and the recipe tab are Craftworks'.
- **The research lock is Craftworks' once-only lock**, asked of the player who presses Fill Recipe, as
  ADR-0118 decided for the Assemblers. The per-craft lock now holds only for the Oil Refinery.

**What ADR-0096 keeps.** The Oil Refinery: its Oritech model, its 22-block footprint, its recipe type and
its figures stand until it moves to Craftworks (#581). The Chemical Plant sections of ADR-0096 are
replaced by this record.

**Considered: keep the Pack's Chemical Plant on its own type and take only the model from Craftworks.**
Rejected. The Pack would keep a block entity, footprint, item face and lock that Craftworks now has, and
one machine on two chassis.

**Consequences.**

- The Showcase has no Oritech reference left for the Chemical Plant: the independence guard's baselines
  fall, in data and in Java.
- Fluid connections are four, two to an edge on the two opposite bottom edges, and turn with the
  facing. A Factorio player reads the pipe positions as Factorio's. The 3x3 footprint replaces the
  Centrifuge's 1x1x2, so existing factories with a Chemical Plant do not carry over.
- The oil Showcase scene is laid out again for the larger footprint.
- The Chemical Plant's art is not final: Craftworks owns it, and the Pack takes it with a version sync.
