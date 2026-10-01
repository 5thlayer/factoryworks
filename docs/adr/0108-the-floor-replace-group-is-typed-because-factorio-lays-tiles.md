---
status: accepted
---

# The floor Replace Group is typed, because Factorio lays tiles rather than replacing them

ADR-0082 makes every Replace Group Factorio's `fast_replaceable_group`, read from the corpus and
never typed. Factorio's floors have no such group. Stone path and concrete are tiles, and laying a
tile over grass or another tile is tile placement, a separate mechanic with no group in the corpus.
The Pack's floors are blocks, so the only way to lay one over terrain in a click is a Replace Group
(#552).

## Decision

**One group, `groundworks:replace_group/floor`, is typed in data**: dirt, grass and gravel, the
terrain a road is laid over, with stone bricks and all sixteen concretes, the floors (#512 made the
same items stretchable). It is a block tag, the Groundworks vanilla Consumer's form, so ADR-0082's
corpus-derived groups are untouched and stay the only groups on the Pack's own blocks.

Accepted with it:

- **The group is symmetric.** Groundworks has no one-way group, so held dirt replaces a road block
  and one concrete colour another. Factorio never lays terrain over a floor; here every replace is
  item for item, so nothing is gained.
- **A replaced grass block hands back a grass block**, which no break without silk touch yields.
  Groundworks hands back the block's own item, and 5thlayer/groundworks#38 asks for its drops.

## Considered

- **No group, dig then place.** Factorio's player never digs to lay a floor.
- **Leaving grass out** until #38 lands. Grass is most of the surface a road crosses.
