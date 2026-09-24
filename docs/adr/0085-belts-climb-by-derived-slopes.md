---
status: accepted
---

# Belts climb by derived slopes

ADR-0084 made a belt a run of flat tiles. The pack's terrain is not flat, so a belt across a hill
needed the ground levelled first, and a stretch aimed above or below its start was refused. Two
flat lines could not cross at all: ADR-0044 excluded undergrounds because a belt crosses another by
going over it, and after ADR-0084 no belt could (#412).

**Decision.** A tile has a **pitch**: level, rising or descending along its travel, one block of
height per block of travel. It amends ADR-0084's consequence that belts are flat.

- **Pitch is derived, never chosen,** as a corner is. It follows from two heights relative to the
  tile, each one up, level or one down: the tile feeding it and the tile it feeds.

  | feeder \ fed | −1 | 0 | +1 |
  |---|---|---|---|
  | **−1** | no connection | top (up) | middle (up) |
  | **0** | top (down) | level | foot (up) |
  | **+1** | middle (down) | foot (down) | no connection |

  A connection is both tiles' or neither's: a tile takes a step only where the tile at its other
  end takes it too. A corner is always level. It is re-derived when a tile within two blocks is
  placed, broken or turned, since a slope's other end is a block up or down, which no neighbour
  update reaches.
- **What a tile feeds is chosen in order:** a level tile ahead facing the same way, then one a
  block up or down ahead facing the same way, then a level tile ahead facing another way. A tile
  beside a line therefore climbs over it rather than side-loading it.
- **The shapes draw one straight 45° line,** each inside its own block. In pixels from each tile's
  floor, where a level tile's surface is at 6: a **foot** is flat at 6 for 6 px then rises to 16, a
  **middle** rises from 0 to 16, and a **top** rises from 0 to 6 then is flat for 10 px. A foot and
  a top are the same profile turned half round. Descending uses the mirrored shapes.
- **A slope is one block of line,** as a corner is. The climb is drawing only: a line over a hill
  carries its tier's 15, 30, 45 or 60 items/s and holds eight items a tile (ADR-0076). The line scan
  follows a line across heights, so a climb is one transport line.
- **A slope takes no side-load.** A tile that would be both a corner and a slope is a slope, and
  a tile facing its side stops there. A placement that would turn a corner into a slope is refused,
  with its reason and nothing changed (#419). A tile facing another's side a block up or down is
  not a sloped corner but a crossing, and places level. A loader or splitter half meets a foot's or
  a top's level end as it meets a level tile, since that end is drawn level. Rotate refusing a slope
  belongs to placed-block rotation (#405), which does not exist yet.
- **A middle or top over air stands on a wedge,** placed with its tile for nothing and broken with
  it, so a belt can climb through open air. It takes the place of a replaceable block such as grass,
  and none is needed over a sturdy top. A tile, loader, splitter, machine or fluid there refuses the
  placement that would need it, so a slope never stands on a belt: a crossing's tops stand beside the
  crossed line, never over it. A tile's top counts as ground for the level tile over it (#420). A two-click stretch follows the ground one block up or
  down at a time, and climbs over a line across its path in five tiles: a foot, a top, a level tile
  on the crossed line, a top and a foot. A stretch takes the lowest heights over the ground that change
  by at most a block a column, so it starts a climb early through the air to clear a wall, comes
  down a drop through the air, and widens a one-column peak into a two-tile top, since a crest does
  not connect; each slope over the air stands on a wedge, as one built tile by tile does. It is
  refused whole where no such path fits: a wall taller than the run-up to it, an end part way down
  a drop, a dip a level tile would bridge over nothing, and a corner on a step. A block with a block entity is no ground for a stretch, as it is none for
  a wedge (#421).

**Considered: underground belts.** Rejected, as ADR-0044 argued. They solve weaving on a plane, and
this medium has a third axis.

**Considered: one 1:1 slope shape per tile.** Rejected. A 45° slope from a level tile's surface to
the next block's would draw 6 px outside its block at each end, into the tile beside it.

**Considered: crests and valleys**, a tile rising and falling inside one block. Rejected. Neither
reads as a belt, and each needs a shape of its own.

**Considered: a wedge under tops only.** Rejected. A middle over air would float.

**Consequences.**

- ADR-0044's exclusion of undergrounds holds again, argued now from slopes: one belt crosses
  another by climbing over it.
- A rider is lifted over each rising collision slab, since an item cannot step up. On a slope it
  climbs in small hops; whether that reads right is a human check.
- Tile by tile ships first (#417), then the wedge and a crossing built by hand (#420), then the
  stretch following the ground (#421); the stretch's crossing is a later slice of #412.
