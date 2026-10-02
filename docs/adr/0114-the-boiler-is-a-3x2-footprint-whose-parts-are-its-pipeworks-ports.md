---
status: accepted
---

# The Boiler is a 3x2 footprint whose parts are its Pipeworks ports

A Pipeworks port is one block in one segment (ADR-0110). The Boiler takes water and gives steam, so it
has to sit in two segments, which one block cannot do.

**Decision (#558).** The Boiler becomes Factorio's 3x2: three blocks wide, two deep and one tall,
placed from one item and broken as one, as the Pumpjack is (ADR-0081). Its parts are its ports. The
three front blocks are water ports opening on their sides, so the row joins one water segment and water
passes through it from end to end, as Factorio's boiler chains water. The back middle block is the steam
port, opening backwards. Each port adds Factorio's 200 mb box (`fluid_boxes` in
`data/factorio/machine.json`) to its segment. Fuel still goes in through any face.

**Considered: one block with a node per face**, Pipeworks letting a block entity sit in a water segment
and a steam segment at once. Rejected: it is a Library change shipped through the release train, the
Boiler stays one block where Factorio's is six tiles, and water could not pass through it.

**Consequences.** The Boiler's model grows to 3x2. That model is hands-on art, as the Steam Engine's is
(#586); until it exists, the existing model sits on the anchor and the parts draw nothing. The front-row
layout is the pack's reading of Factorio's water ends and steam outlet: the extracted corpus does not
carry `pipe_connections`.
