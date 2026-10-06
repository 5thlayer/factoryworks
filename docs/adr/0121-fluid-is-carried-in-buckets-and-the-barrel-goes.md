---
status: accepted
---

# Fluid is carried in buckets, and the barrel goes

ADR-0037 made `factoryworks:barrel` the one portable fluid container, at Factorio's 50 mB, and
ADR-0050 excluded buckets beside it and forced `waterSourceConversion` off so that water is never
created. Both rested on the overhaul: Factorio's numbers were binding, and the Pack owned the world.
ADR-0115 ends both. And since Create left (ADR-0060), nothing fills or empties a barrel: ADR-0037
gave it no recipes because Create's Spout and Item Drain did that work.

**Decision.** A player carries fluid in a bucket, or in any other mod's fluid-holding item. The
barrel goes, and ADR-0115 no longer gives one to Pipeworks. FactoryWorks registers a bucket for each
of its liquids (crude oil, heavy oil, light oil, lubricant, sulfuric acid) and none for its gases
(steam, superheated steam, petroleum gas), as vanilla has no gas buckets. Pipeworks' storage tank
and pipes fill from and empty into a fluid-holding item used on them, through NeoForge's fluid item
capability, so a bucket and another mod's portable tank both work. FactoryWorks no longer forces
`waterSourceConversion` off: a suite mod in an ordinary world does not rewrite its water rules.

**Considered: keep the barrel and give it a filler**, a Pipeworks block or a fill and empty recipe
on the Assembler. Rejected: it keeps a FactoryWorks-only container that no other mod's machine
knows, to serve Factorio's belt- and train-borne fluid, which the suite does not yet need. A
stackable container can come back as a Pipeworks feature if it does.

**Considered: a barrel at bucket size.** Rejected: it is a bucket with a second item id.

**Consequences.**

- ADR-0037 is superseded. ADR-0050's rule that water is never created goes; its Offshore Pump stays.
- The barrel's item, fluid handler, data component, recipe, item-map rows and assets are removed.
- The ledger's barrelling rows read `excluded` and its bucket row `shipped`.
