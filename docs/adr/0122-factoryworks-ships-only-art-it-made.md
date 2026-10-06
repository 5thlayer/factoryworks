---
status: accepted
---

# FactoryWorks ships only art it made

FactoryWorks is built with AI assistance, and is judged harder for it. Art taken from other people,
even under an open licence that allows it, reads as one more thing the project did not make. Under
ADR-0102 the Showcase still ships Futureazoo's CC BY-NC-SA machine textures and malcolmriley's CC BY
ores, chests, wreck and crude oil. Beltworks' belt surface is re-authored from Simple Conveyor
Belts' art, itself from malcolmriley's. Pipeworks' pipe and tank are Oritech's CC0 models. The store
media film Oritech's machines.

**Decision.** FactoryWorks, its Modules and the Showcase ship only art they made: textures, models,
animations, sounds, and the covers, icons and clips on their store pages. Art copied, retinted or
re-authored from someone else's work counts as borrowed, whatever its licence. Where nothing of our
own exists yet, the asset is Stand-in art. Minecraft's own textures are the platform, not borrowed
art: a model may use them, and store media may show them. Code lineage, such as Beltworks forking
Simple Conveyor Belts' code, is not art and is outside this rule.

**Considered: borrow only from art collections, not from mods.** Rejected: whoever drew it, the
asset is not ours, and that is what a reader notices.

**Considered: keep borrowed art until real art is commissioned.** Rejected: a stand-in is honest
about being unfinished, and borrowed art is not.

**Consequences.**

- This supersedes the art paragraph of ADR-0109 and amends ADR-0102: no third-party art is added,
  and each `NOTICE` art entry and `REUSE.toml` exception goes when its asset is replaced.
- The provenance manifest (#564) keeps its `vendored` kind only to list what is left to replace.
- A third-party texture licence such as CC BY-NC-SA leaves the repository with its last asset.
