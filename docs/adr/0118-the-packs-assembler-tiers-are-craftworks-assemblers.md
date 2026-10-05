---
status: accepted
---

# The Pack's assembler tiers are Craftworks' Assemblers

ADR-0071 gave the Pack an Assembling Machine on Oritech's chassis, ADR-0072 stood it on Oritech's
2x1x2, ADR-0074 pinned Oritech's input mode under its filter and ADR-0075 made its three tiers three
blocks of one block entity. ADR-0109 takes Oritech out of the Pack, and ADR-0115 gives the assemblers
to Craftworks. Craftworks 0.4.0 can now hold what the Pack's recipes need: an Assembling recipe has a
`category` and a list of `results`, and its Assemblers take the categories their server config lists.

**Decision (#559).**

- **The three Assemblers are `craftworks:assembler_1`, `_2` and `_3`.** The item map names them as
  `borrowed` for `assembling-machine-1` to `-3`, and the Pack registers no assembler tier, block,
  item, block entity type, model or renderer. They stand on Craftworks' 3x3 footprint two blocks
  tall, which is Factorio's tile square again (ADR-0059), and the art is Craftworks', which the Pack
  does not override.
- **Factorio's `crafting`, `advanced-crafting` and `crafting-with-fluid` recipes are
  `craftworks:assembling` recipes**, each with its `category`, a `results` list and `hand_craftable`.
  The converter sets the flag for a first category of `crafting`, which leaves out the eleven fluid-free
  recipes Factorio withholds from the hand. Recipe ids stay `factoryworks:assembling/<name>`, so no
  research unlock changes. The stock re-authoring and the two Engineer's Pick recipes move the same
  way. `factoryworks:assembling` is no longer a recipe type.
- **The hand copies go.** A recipe has one type, so ADR-0089's `factoryworks:hand/*` copy of every
  hand recipe existed only because the Personal Assembler could not read the machine's. The flag
  replaces it: `scripts/build-hand-recipes.py`, `recipe/hand/`, `hand_recipes.js` and `withHandCopies`
  are deleted, and Researchd locks the one recipe both the Assembler and the Personal Assembler use.
- **Chemistry and oil processing stay on the Pack's types**, and so does the chassis they run on:
  `AssemblingMachineBlockEntity` and its helpers, `PaintLock` and the five input slots stay until the
  Chemical Plant and Oil Refinery move to Craftworks (#581, #582). The chassis block entity is now
  abstract, and the slot count is a constant, so the two machines do not change.
- **The research lock is checked once, at Fill Recipe, against the player who presses it**, as
  Craftworks' ADR-0013 decides. The Pack's `craftworks-server.toml` sets `lockSources = ["researchd"]`.
  The per-craft lock, asked of the team that placed the machine, does not carry over: a recipe
  researched by one player can be set on an Assembler a teammate then runs, and one that is locked
  after it is held keeps being made until someone changes it. The Chemical Plant and Oil Refinery
  keep the per-craft lock until they move.

**Considered: keep the Pack's tiers and give Craftworks only the hand set.** Rejected. It is the two
machines ADR-0115 refused, with Oritech under one of them, and the Pack would carry a footprint,
block entity and lock that Craftworks already has.

**Considered: a machine lock hook, so an Assembler is locked per craft by its placer's team.**
Rejected for the reason Craftworks gives in ADR-0013: it needs a team, which Craftworks has none of,
and a second hook every Lock source must learn, for a case that barely happens.

**Consequences.**

- Oritech's addons no longer attach to an assembler, and ADR-0074's pinned input mode and filter are
  Craftworks' own filter now. Fast Replace between tiers is Craftworks' (`AssemblerReplace`), so the
  Pack's Replace Group rows for the assemblers are gone.
- The five `crafting-with-fluid` recipes are emitted, but Craftworks refuses to hold a recipe that
  names a fluid until it has Fluid Connections (#580). Tier 3's recipe is still not emitted, since it
  takes the `speed-module` that #120 has not decided.
- The Assemblers' art is not final: 5thlayer/craftworks#22 is reopened to draw it, and the Pack takes
  it with a version sync.
- This supersedes ADR-0071, ADR-0072, ADR-0074 and ADR-0075. ADR-0063's type for the assembling
  recipes, ADR-0082's Fast Replace of the assemblers and ADR-0089's hand copies no longer hold for
  them, and ADR-0096 holds only for the Chemical Plant and the Oil Refinery.
