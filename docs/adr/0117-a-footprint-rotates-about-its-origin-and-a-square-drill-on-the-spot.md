---
status: accepted
---

# A footprint rotates about its origin, and a square drill on the spot

A placed block turns in place by its own contract (ADR-0087). A footprint machine is several blocks
around one origin, which holds the block entity, so turning it has to say where it pivots. Factorio
turns an entity about its centre.

**Decision (#406).** A machine on Groundworks' footprint rotates about its origin, as Groundworks
already does: the origin keeps its block and block entity, takes the next facing, and its parts are
laid again. Each machine declares its origin at the centre of its shape where the shape has one, so
the Radar, the Pumpjack and Craftworks' machines rotate about their centre as Factorio's do. The 3x2
Boiler has no centre block and swings about its front-middle origin.

A mining drill is a square, so it rotates on the spot: it keeps the blocks it stands on, its ore and
its block entity, and only its facing changes, with its Drop Position, output port and lit front. A
held drill under Rotate keeps the same rule: its square stays on the same blocks under the cursor.
The drills keep their own geometry rather than standing on Groundworks' footprint (ADR-0059), which
would make the 2x2 Burner Mining Drill, with no centre block, swing onto three new blocks and change
the ore it mines. A drill's rotation is never refused except where the player may not build; a
blocked output stalls it, as it would after placing.

**Considered: Groundworks rotates every shape about its geometric centre.** Rejected: the origin
would have to move during a turn, taking its block entity data with it, and the change ships through
the release train. It changes only the Boiler, whose Factorio counterpart shifts half a tile when
turned, which no Minecraft block can.

**Consequences.** Where a machine's origin sits in its shape is now a decision about how it rotates,
not only about where it is placed. A future even-sized footprint swings as the Boiler does unless it
is a square that turns on the spot, as the drills do.
