---
status: accepted
supersedes: [133, 134]
---

# The player wakes inside the wreck, and its cargo hold is filled once per world

The Opening begins inside the **wreck** (`docs/spec/terra-progression.md`, beat 1). #100, #133 and
#134 designed it on 1.21.1, and nothing placed it. Three of their premises no longer hold (#498).

- **#134 made the wreck the answer to the first night.** ADR-0093 removed the night's threat, so
  the lever-opened iron door and the lighting kept as a spawn guarantee have nothing left to do.
- **#134 put the wreck at the hub's centre "which is the world spawn anchor".** It is not. Vanilla
  offsets the hub so that one of its connectors lands on the spawn point, so the player spawns on
  the hub's edge (`TerraStartingArea`). The water pool (ADR-0050) has held the centre since. And
  vanilla 26.1 never puts a player at the spawn point exactly: `PlayerSpawnFinder.findSpawn` scatters
  them by `respawn_radius` (10 by default) and picks the height off the `MOTION_BLOCKING` heightmap,
  which is a roof's top. `respawn_radius` 0 still lands on the roof.
- **#133 made the cargo hold a KubeJS block entity.** That was verified on KubeJS 2101.7.1. The mod
  now registers the Pack's chests itself (ADR-0106), and the hold has to be filled by the starting
  area's stamp, which is the mod's.

## Decision

**The wreck is fiction and the cargo hold's home, not a shelter.** It is #134's box, 15×11×7 outside
and 13×9×5 inside, at the hub's centre, roofed, with windows and an open doorway in a long wall. The
water pool moves out of the centre to beside the doorway. There is no door and no lever. Invisible
`minecraft:light` blocks keep the room readable at night. Every block of the wreck has hardness -1.

**The world's spawn point is the wreck's floor, exactly.** The stamp moves it there, and a mixin on
`PlayerSpawnFinder.findSpawn` returns it unscattered and at its own height whenever it is the
point asked about. That one method serves both a first join and a respawn with no bed, so the
player wakes inside and returns inside after dying. A bed or respawn anchor the player sets does not
reach `findSpawn` and still wins.

**The cargo hold has five slots and is filled once per world.** Five is the `inventory_size` of
Factorio's `crash-site-spaceship`, extracted from the corpus rather than typed. The stamp fills it
with the starting kit's **Hold**, once, as Factorio's freeplay builds its crash site once, for the
first player (`storage.init_ran`). Every player still gets the **Pocket** on first join. A player
who joins later finds what is left.

**Every block of the wreck is `factoryworks_core`'s.** This amends ADR-0015's table, whose second
row sent every custom block without special vanilla behaviour to KubeJS. On 26.1 the mod registers
the Pack's machines and chests, and a block the stamp fills is
the mod's too.

## Considered Options

- **Wake at the doorway, not inside.** It needs no mixin, only `respawn_radius` 0, but beat 1 is
  waking inside, and the heightmap would still pick the height.
- **A teleport on first login and `PlayerRespawnPositionEvent` on death.** Two hooks for one rule,
  and the first shows the roof for a frame before the teleport. NeoForge has no first-join position
  event: `PlayerEvent.LoadFromFile` fires before the spawn position is applied.
- **The pool keeps the centre and the wreck stands beside it.** The wreck is the landmark the base
  is built around, so it takes the middle.
- **A door opened by hand.** With nothing to keep out, a doorway is the simpler gesture.
- **Refill the hold for each new player, or give later players the Hold in their inventory.**
  Factorio does neither, and a later player joins a factory that has outgrown the Hold.
- **27 or 36 slots.** Either would be a free Wooden or Iron Chest at spawn, against ADR-0106's ladder.

## Consequences

- #134's lever, iron door and lighting-as-shelter are withdrawn, and so is its "centred on the hub
  is the spawn anchor". Its shell, its windows and "no bed, no respawn call" stand: the mixin is not
  a respawn call, and death still returns the player to the wreck.
- #133's KubeJS cargo hold is withdrawn. Its findings about KubeJS inventories stay in ADR-0015 as
  facts about that build.
- `StartingKitGrant` grants the Pocket only, and `StartingKit.HOLD` becomes what the stamp puts in
  the cargo hold.
- The wreck's art is vanilla's for now; its own art and a ship's shape are a follow-up.
