# Enriched ore — a FactoryWorks Showcase mechanic

Enriched ore joins Voidworks to FactoryWorks, so it belongs to neither alone: FactoryWorks
registers the blocks, and the FactoryWorks Showcase fills Voidworks' displacement table with them.
Without both mods it does not exist. Resolved in the void grilling sessions of 2026-10-06; the void
design is [void-energy.md](https://github.com/5thlayer/voidworks/blob/main/docs/spec/void-energy.md).

## Terms

**Enriched ore**:
A FactoryWorks ore block made when an Overworld Void Displacer displaces a vanilla ore, in one of
three richness tiers. Each tier is its own item and starts at its own amount.
_Avoid_: ore patch (worldgen's)

## The mechanic

- The Showcase maps each vanilla ore with a FactoryWorks counterpart, stone excepted for now, to an
  Enriched ore in Voidworks' displacement table. The Displacer then outputs the Enriched ore in
  place of the vanilla ore.
- Three richness tiers, each its own registered item (the precedent is ADR-0010), set by the fed
  block's Density and starting at the tier's amount (ADR-0041).
- Placed anywhere and mined by hand or by FactoryWorks' drills, drawing the amount down.
- Never in Voidworks' natural-block tag, so it is never displaced again.
- Silk touch returns the highest tier whose starting amount does not exceed what is left; below the
  lowest tier it mines one unit as usual. It is never a way to gain ore.
- Enriched ore multiplies ore; ADR-0032's cut of every multiplier is superseded by ADR-0115.

## Ore patches

Ore patches become a FactoryWorks worldgen option, so that drills can be a post-End mechanic: with
patches off, a drill has nothing to mine until Enriched ore exists. It defaults to on, so
FactoryWorks alone keeps its drills from the start; installing Voidworks does not change it. The
Showcase decides its own setting. The option only affects chunks generated after it is set.

## Open

- **Numbers** — the richness tiers' starting amounts and which Densities map to each.
