---
status: accepted
---

# The Pack depends on no third-party content mod

Every block, item, fluid and machine a player builds or holds comes from a 5thlayer Library or from
FactoryWorks Core. Third-party libraries (GeckoLib, FTB Library, FTB Teams, KubeJS), the recipe
viewer, perf mods and pack infrastructure (FTB Quests, FTB Chunks, FTB Filter System) stay. Railcraft,
Oritech, FTB Materials and Researchd (with Porting Dead Libs) go.

A borrowed mod's design leaks into the Pack: Oritech's chassis carries addons, input modes and energy
pushing the Pack has to strip (ADR-0071, ADR-0074), Researchd can only approximate Factorio's trigger
technologies by item presence, and every jar brings hundreds of items the recipe viewer has to hide
(ADR-0088). Owning the mechanism lets the Pack follow Factorio rather than work around another mod.

**Where things go.** Generic mechanism goes to a Library and Factorio's content to the Binding, as
ADR-0090 has it:

| Replaced | By |
|---|---|
| Oritech pipes, tank, fluid transport | **Pipeworks**, a new Library, fluid-agnostic |
| Oritech's six oil fluids | Core registers them, with water and steam |
| Oritech's machine chassis (assemblers, chemical plant, refinery) | **Craftworks** places machines, written fresh; `factoryworks:assembling` merges into `craftworks:assembling`, with a hand-craftable flag in place of the hand copies |
| Oritech's steam engine, solar panel, accumulator, small lamp | Core blocks feeding the Wireworks network |
| Researchd | **Labworks**, a new Library: tech DAG, queue, Lab, real Factorio triggers, recipe locks; FTB Teams optional |
| FTB Materials' eight items | Core items |
| Railcraft's train items | nothing yet: trains are `blocked` on a train Library |

Craftworks nests Pipeworks, so a machine's fluid slots and the pipes that feed them share one
fluid model. Groundworks stays the Library for placed blocks and does not take fluids.

**Art.** Oritech's code and its own assets are CC0 and are vendored. Its ArtOfTecharium models (the
Assembler, the Foundry) are CC BY-NC and are vendored too, on the same footing as the Futureazoo art
(ADR-0103). FTB Materials and Railcraft are All Rights Reserved, so their sprites get stand-in art.
Researchd's licence forbids redistributing a fork under 50% altered, so Labworks is a clean-room
rewrite.

**Considered.** Keeping Railcraft until a train Library exists: rejected, since it ships six items and
a train chapter the Pack has not designed. Forking Oritech's chassis: rejected, since most of what it
holds is what the Pack already strips. Putting fluid ports in Groundworks so Craftworks never depends
on Pipeworks: rejected, since Groundworks is about placed blocks, not their contents.

**Consequences.** This supersedes ADR-0060's choice of Oritech as the Pack's tech mod, ADR-0061 in
full, and ADR-0018's choice of Researchd as the Lab. The Oritech ADRs (0067, 0071, 0072, 0074, 0075,
0077, 0096, 0098) are superseded one by one as their slices land. The release train gains Pipeworks
and Labworks (Pipeworks before Craftworks, Labworks before the Pack).

## Amended by #587

**Release train order.** Pipeworks sits after Wireworks, not right after Groundworks: Groundworks,
Beltworks, Wireworks, Pipeworks, Craftworks, then Labworks and the Pack. Pipeworks depends on no
other Library, so only Craftworks, which nests it, must come after it; CLAUDE.md states this order.

**Pipe Dismantle and drag-laying stayed in the Pack.** #566 gave them to Pipeworks, but the pipe
Dismantle Family (`core/dismantle/PipeFamily`) and drag-laying (`core/stretch/PipeworksPipeLegs`) are
Pack Bindings, reading the arms Pipeworks' pipes report (ADR-0110). No ticket records why they were
not moved into the Library.

## Amended by ADR-0122

The **Art** paragraph is superseded: nothing is vendored, from Oritech or anyone else, and every
borrowed asset becomes Stand-in art until FactoryWorks has its own.
