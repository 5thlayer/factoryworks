---
status: accepted
---

# A Patch exists for every mineral a Module recipe uses

ADR-0127 gave Fieldworks six Patch resources: iron, copper, coal, stone, uranium and sulfur. The
Modules' recipes also need redstone, gold, diamond, quartz, glowstone and netherite in bulk, so a
factory built around Harvesters still sends the player strip-mining exactly when it needs the most.
Two other things stopped fitting: a Patch had no rarity gate besides the distance law, and drilling
mud's only job was a bonus that pushed a decayed Oil well above its floor, which contradicts the
floor being the well's final yield (ADR-0081). Every Harvester Tier could also work every resource,
so the Electric Harvester gated nothing (fieldworks#35).

**Decision.**

- **The rule: a Patch exists for every mineral a Module recipe uses in bulk**, in the dimension where
  vanilla keeps it. Each resource is bound to exactly one dimension. Quartz, glowstone and netherite
  are Nether-only; the rest are Overworld. Gold in the Nether, despite vanilla's nether gold ore, is
  not a Patch.
- **End Patches are excluded.** Nothing generates in the End.
- **The list is twelve.** Overworld: iron, copper, coal, stone, sulfur, uranium, redstone, gold and
  diamond. Nether: quartz, glowstone and netherite. A draw pays what the vanilla block drops without
  Silk Touch, and netherite pays netherite scrap.
- **Each resource has a minimum distance from origin**, below which none of its Patches generate:
  0 for iron, copper, coal, stone, quartz and glowstone; 300 for sulfur and redstone; 500 for gold;
  600 for uranium; 1500 for diamond; 2000 for netherite. Distance is in Overworld-equivalent
  blocks: a Nether centre's distance is its Nether distance ×8, so one set of law numbers serves
  both dimensions. The figures are a starting point for playtesting.
- **Nether Patches lie on the open Nether floor**, found by scanning down from under the bedrock
  roof, never on the roof or a lava sea, and follow the floor column by column, a column further than 8
  blocks above or below the centre's floor being left out, so a patch is one place to build beside and
  not a scatter over every cave level.
- **Rare resources sit behind a tech step.** The Burner Harvester works every common resource.
  Uranium and diamond need the Electric Harvester and Overworld Drilling Fluid; netherite needs the
  Electric Harvester and Nether Drilling Fluid; Oil wells need the Electric Harvester and no fluid.
  These gates are ordinary written Harvest Recipes that replace the default, so a pack can override
  them.
- **Drilling Fluids belong to a dimension.** Drilling mud is the Overworld fluid; **magma slurry**,
  crafted from a lava bucket and soul soil, is the Nether fluid. Which dimension a recipe wants is
  held only by the fluid tag it names; the Harvester checks no dimension. The slurry recipe is a
  first pass, to be tuned.
- **The mud bonus on Oil wells goes.** A well's floor is final.
- **Iron and coal swap** their amount, base density and rich-quality factor, so fuel is never the
  bottleneck.

**Considered: data-driven Patch resources.** The set of resources is fixed in code and the Patch
numbers are read when blocks register, before any datapack exists, so a pack cannot add one. It
stays out of scope.

**Considered: a Harvester check that refuses a fluid from the wrong dimension.** The recipe tags
already carry the rule, and a pack author can put any fluid in either tag.

**Consequences.**

- This supersedes ADR-0127's Patch list (iron, copper, coal, stone, uranium, sulfur) and the Oil
  well drilling-mud bonus Fieldworks shipped under it. Its other clauses stand, including "no
  Module requires another", vanilla recipe types only and meeting in `c:` tags.
- Existing worlds get the new Patches only in chunks generated after the update.
- Fieldworks ships a minor release, since its breaking changes bump the minor. The Showcase then
  bumps Fieldworks and adds the six new Generation switches to its config.
