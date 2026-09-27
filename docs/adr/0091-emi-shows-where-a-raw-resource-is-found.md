---
status: accepted
---

# EMI shows where a raw resource is found, derived from the Obtainable sources

ADR-0088 lists every Obtainable item and fluid in EMI's index, including those no recipe makes:
ores, logs, water, crude oil and steam. Looking one up shows no recipe, so the index names a thing
and gives no route to it. Factoriopedia answers the same question on a resource's page with the
resource, what mines it and where it is found (#478).

**Decision (#478).** A pack EMI category, **Where it is found**, gives each Obtainable stack one
entry per source that is not a recipe or the starting kit. The entry shows the stack, what extracts
it as the category's workstations, and a line on where it is found:

- an ore: the patches that hold it, the starting area's fields or the outfield discs, and the
  Pick and the mining drills;
- a log: the Terra trees that drop it, and felling;
- water: a source block beside an Offshore Pump;
- crude oil: a Pumpjack on an oil well;
- steam: the Boiler, with water as its input;
- a block or mob drop, once #454 and #455 derive them: the block broken or the mob killed.

The entries are derived and never typed per item. The Obtainable generator already decides why each
stack is Obtainable, and it writes that reason beside the index as a client resource the category
reads. A mechanic's workstations and its text are one row per mechanic, keyed by the `mechanic` of
`data/pack/mechanic-obtainable.json`, so a new ore is an entry with no edit. What mines a solid or a
fluid resource is joined from Factorio's own `resource_categories` onto the item map, as Factorio
decides it. Where an ore's patches appear is read from the starting area's fields and the outfield
structure sets the generators write.

**Considered.** Hiding the raw resources from the index: EMI would still list what they are used
in, but the index would stop being exactly the Obtainable set, and a player would meet raw iron in a
recipe and find no entry for it. A tooltip line on the stack: it names a source but has no page,
so it cannot show the drills or the patches, and it needs a mixin per tooltip surface.

**Consequences.** ADR-0088 is unchanged. Each of #454's and #455's derivations becomes an entry in
this category as it lands, so both must keep the source they derived, not only the id. A stack that
a recipe also makes shows both.
