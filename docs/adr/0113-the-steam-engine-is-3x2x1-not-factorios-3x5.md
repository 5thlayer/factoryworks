---
status: accepted
---

# The Steam Engine is 3x2x1, not Factorio's 3x5

The Steam Engine is 3 blocks wide, 2 tall and 1 deep: three cylinders across the top, and a flywheel,
frame and rotor beneath them. It is drawn as one vanilla model (ADR-0111).

Factorio's engine is 3x5, its three drums packed along the length with pipes and gears running freely
between them, so it aligns with no block grid. Drawn at 3x5 it needs a model larger than a vanilla
model reaches, split across blocks the game draws from, and authored freehand across block lines.
At 3x2x1 each part of the engine sits in one block and the whole fits one model.

**Considered.** Factorio's 3x5, drawn as one model the generator splits between two blocks: rejected
for the authoring cost, though the generator split would have served. Oritech's 2x1x2 hull (ADR-0062,
ADR-0077): rejected, since its only reason was Oritech's model, which ADR-0109 removes.

**Consequences.** A Factorio power layout does not carry over: an engine is 3 tiles where Factorio's is
15, and the 1 boiler to 2 engines block is laid out differently. An engine still makes 900 kW from
30 steam a second, so power per tile is five times Factorio's. ADR-0062's divergence "Oritech's
hull, not Factorio's 3x5" now reads 3x2x1.
