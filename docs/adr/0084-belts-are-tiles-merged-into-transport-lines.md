---
status: accepted
supersedes: [362, 366]
---

# Belts are tiles, merged at runtime into transport lines

ADR-0078 kept the fork's belt a spline between grid-aligned supports and bounded its curve. It read
as organic rather than as a factory: curves of every size, posts at every corner, belts in the air.
Supports were the shape's control points, the only belt end that was not an inventory, and the only
way to break a belt from its middle. A player had to learn a geometry Factorio never asks for (#383).

**Decision.** A belt is made of **tiles**, as in Factorio. It supersedes ADR-0078.

- **A tile is one block of belt.** It is placed, broken and paid for on its own, one belt item a
  tile, and faces the way items travel through it. It is straight, or a one-block corner when it is
  fed from exactly one side and from nothing behind. The shape is derived from the neighbours, never
  chosen (#391).
- **A stretch is laid on solid ground.** A two-click stretch refuses as a whole, changing nothing,
  when any tile would stand on a block with no sturdy top face (#393). A single tile placed on its
  own is placed as vanilla places a block.
- **Contiguous tiles are one transport line.** A run of tiles each feeding the next, around
  corners, is merged at runtime into a line that ticks once for the whole run (#398). The line is
  derived state: each tile saves its own share of the items, and the line is rebuilt when a tile is
  placed, broken or turned, or when its chunk loads or unloads. It never spans an unloaded chunk
  (#395).
- **The tier table is unchanged.** A line carries 15, 30, 45 or 60 items/s in one lane, holds eight
  items a tile, and runs at its slowest tile. A loader and a splitter still cap only their own flow
  (ADR-0076).
- **A belt ends where its tiles do.** Its ends are a loader, which meets a tile face to face and
  carries no items itself (#408), a splitter half (#394), or nothing, where the line backs up. The
  belt places no loaders; a player places them.
- **A loader takes its filter whether or not a line reaches it.** A click with an item sets it and an
  empty hand clears it, so it is set before the belt is built. A block is placed against a loader
  with a sneak, as against a chest.

The spline belt item, the support, the span bounds, the loader choice at open ends (#354), the
splitter's belt cut (#361) and the far-end hand hold (#360) are removed from the fork.

**Considered: keep the spline and add drawing modes.** Rejected. #365 would only have put a
generator over the same model, and a curve between supports was the problem.

**Considered: keep supports as a belt end.** Rejected. A line's last tile is already a dead end, and
a tile standing on the ground needs no post.

**Consequences.**

- Belts are flat. A stretch refuses to climb, and height is #412's.
- ADR-0044 excluded undergrounds because a spline climbed over another belt. With flat tiles that
  argument waits on #412.
- A tile whose ground is removed after it is placed stays where it is.
