# The suite's endgame

Where the -works suite's output finally goes, decided in the endgame grilling of 2026-10-09/10. The
GitHub shape (which repo holds the parent issue, which children) is left to a later session. The
prior art is [tech-mod-endgame-sinks.md](../research/tech-mod-endgame-sinks.md), mostly unverified.

## Decisions

1. **The endgame is the suite's, not a pack's.** A plain install of the mods has one. It is three
   things together: a milestone, a never-ending Sink, and Gear.
2. **Mods ship the mechanism and default costs.** Every amount and rate is datapack data, so a pack
   retunes the endgame without code.
3. **Sinks.** Voidworks drains the factory (running costs, Mutation, zone upgrades). Gearworks drains
   the player (upgrade levels whose cost rises exponentially, uncapped). Fieldworks adds a capped
   tier through bonus Drilling Fluids (decision 8). Each works with no other mod present.
4. **Milestones.** Each Module ends on its own capstone; there is no suite-wide capstone, since one
   would need several mods (ADR-0127). The Showcase shows how a pack ties them together.
5. **Endless sources pay materials to run, not only power.** Voidworks' displacers already do: fed
   blocks are consumed and Voidstone is permanent. Fieldworks does through Drilling Fluids.

### Gearworks, a new Module

6. **Gearworks owns Gear**: what the player wears or carries to build and move through the factory
   faster, not combat gear. Upgrades fit a **Gear Grid**.
   - An item fits the Grid by tag plus a data map that names its stats. Gearworks owns every upgrade
     behaviour; other mods contribute only items, through tags.
   - Gear draws FE through NeoForge's energy capability. Voidworks can be its best source by handing
     out FE converted from motes; neither mod names the other.
   - Top upgrades cost a top-tier material through `c:` tags with a vanilla default (netherite, nether
     stars), which other mods' materials join. No recipe is conditional on another mod (ADR-0127).
   - The ledger row "Armor and the equipment grid" in `docs/factorio-mechanics.md` moves to Gearworks once the
     Gearworks ticket exists; the row's `owner` and `ticket` fields name it, so the edit waits for it.

### Voidworks

7. **Voidworks is a Module** — it requires nothing but vanilla, as ADR-0127 has every Module do — and
   is both a source and a Sink. Its own docs call it a "Library"; that wording changes in its repo.

### Fieldworks: Harvest Recipes and Drilling Fluids

8. **The Harvester runs Harvest Recipes**, several per resource, each matching a resource and an
   optional Drilling Fluid. Roles fall out of which recipes exist:
   - a resource with no fluid-free recipe always needs a fluid (uranium, say);
   - an endless resource can need a fluid only at its decay floor (oil), through a recipe condition on
     the source's yield state;
   - a recipe with a fluid and a higher yield is a **bonus** — the capped Fieldworks Sink tier.
9. **A Harvester holds a player-set Harvest Recipe**, chosen from the recipe viewer (EMI/JEI), the way
   Craftworks' assembler holds one (ADR-0071, ADR-0073).
10. **Drilling Fluids are a tag, `#fieldworks:drilling_fluids`**, defaulting to a drilling mud brewed in
    vanilla's brewing stand (a water bottle with clay or slime). Other mods and packs add fluids to it.
    Which resources need one by default is not decided here.
