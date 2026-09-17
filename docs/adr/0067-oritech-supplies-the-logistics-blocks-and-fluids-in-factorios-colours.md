---
status: accepted
---

# Oritech supplies the logistics blocks and fluids, in Factorio's colours

ADR-0060 removed six mods, and 24 item-map rows kept naming them: the machines, the logistics
blocks and the oil fluids (#277). ADR-0061 settled the material forms and said a tech mod supplies
machines. This spends that rule, row by row.

**Decision.**

- **Machines are core blocks.** `assembling-machine-1`, `chemical-plant` and `oil-refinery` name
  `planetaryfactory:` subclasses of Oritech machines, as ADR-0060 says, blocked on #258's chassis.
  Not Oritech's assembler, which reads its own recipe type rather than `planetaryfactory:assembling`
  (ADR-0063), and not Oritech's Centrifuge, which ADR-0060 recipe-removes. Tiers 2 and 3 are #295's.
- **Placed blocks are borrowed.** Oritech's Fluid Pipe, Portable Tank, Big Solar Panel and Industrial
  Light; vanilla rail, Railcraft Reborn's Block Signal and Iron Buffer Stop Track; SimpleBelts' two
  belt tiers. The in-line pump (#293) and the power switch (#294) have no Oritech block that does
  Factorio's job, so they are first-party and blocked.
- **The accumulator is Oritech's Large Energy Storage.** This amends ADR-0060, which made it a core
  block at Factorio's 5 MJ. The capacity is Oritech's.
- **The oil fluids are Oritech's Refinery fractions**: crude oil, heavy oil, light naphtha as light
  oil and diesel as petroleum gas, plus Oritech's sulfuric acid. Lubricant, which Oritech lacks,
  borrows Biofuel, the Oritech fluid in Factorio's lubricant green.
- **A borrowed fluid is drawn in Factorio's colour.** Where Oritech's sprite times its tint lands
  more than 0.15 from Factorio's `base_color`, the core swaps the tint
  (`FluidModelContentMixin`, fed by `scripts/build-fluid-tints.py`). Today that is heavy oil,
  petroleum gas and sulfuric acid.
- **Circuit readouts are not emitted.** `display-panel` and `programmable-speaker` show a circuit
  signal, and the pack has none.

**Considered: nearest-colour mapping.** Rejected. Mapping sulfuric acid to Diesel because Diesel is
yellow would give one fluid two rows and break the Refinery's three-fraction shape.

**Considered: first-party oil fluids.** Rejected. Oritech's Refinery already splits oil into three
fractions, and its machines and generators consume them. Authoring the fluids would put a second
set beside them.

**Consequences.** Diesel renders purple wherever Oritech uses it, and Biofuel serves as lubricant
while keeping its Oritech name and fuel value. `tests/pack/test_fluid_tints.py` holds the colours.
Whether they read right in a running client is a human check on delivery.
