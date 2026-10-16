# Vanilla portal mechanics for moving items unattended

Research for [#694](https://github.com/5thlayer/factoryworks/issues/694), part of the map
[#693](https://github.com/5thlayer/factoryworks/issues/693). It answers how items cross dimensions
in Minecraft 26.1.2 Java with no mod installed, and whether a crossing can run unattended.

## Sources

- **[src]**: the decompiled, NeoForge-patched 26.1.2 sources at
  `build/moddev/artifacts/minecraft-patched-26.1.2.77-sources.jar` in any of the mod checkouts
  (26.1 ships unobfuscated, so names are Mojang's). This is the primary source for every claim
  below unless another is named. Classes are cited by simple name; all are under `net.minecraft`.
- **[wiki-cl]**: minecraft.wiki, [Chunk loading](https://minecraft.wiki/w/Chunk_loading), for which
  version introduced a behaviour.
- **[wiki-np]**: minecraft.wiki, [Nether portal](https://minecraft.wiki/w/Nether_portal), history.
- **[wiki-ep]**: minecraft.wiki, [End portal](https://minecraft.wiki/w/End_portal).
- **[wiki-mc]**: minecraft.wiki, [Minecart](https://minecraft.wiki/w/Minecart).

Nothing here was tested in game. Claims marked *inferred* follow from reading the code, not from
watching it run.

## Answer in short

- Any non-passenger, living entity crosses: item entities, minecarts of every kind (contents are
  copied with the entity's full saved data), boats. Non-player entities cross at once, with no
  4-second wait.
- Overworld ↔ Nether by Nether portal; Overworld/Nether → End by an End portal; End → **world spawn**
  (normally the Overworld surface at spawn) by the End's exit portal. A Nether portal cannot be lit
  in the End, so there is no direct End ↔ Nether route. End gateways move entities only within the
  End.
- Every crossing places a **portal ticket** at the arrival point: 3×3 chunks entity-ticking (5×5
  block-ticking) for **300 ticks (15 s)**, refreshed by each later arrival, and persisted across
  restarts. The departure side gets nothing: it must already be entity-ticking for the entity to
  tick into the portal.
- Spawn chunks no longer exist (removed in 1.21.9). The only vanilla ways to keep a departure point
  loaded without a player nearby are `/forceload` (an operator command) or a portal-ticket chain
  (entities cycling between two portals so each arrival refreshes the other end).
- So a crossing can run with **no player in either dimension**, but only while some player is
  online somewhere (a dedicated server pauses ticking 60 s after the last player leaves by default)
  and only if the departure chunks are force-loaded or ticket-chained.
- The End arrival point is fixed: every non-player entity lands at (100.5, 50, 0.5) and **every
  arrival rebuilds the 5×5 obsidian platform**, breaking (and dropping) any block in its 5×5×3 air
  box and floor. Collection has to sit outside that box.

## 1. What can cross

`Entity.canUsePortal(false)` is true for an entity that is alive and not riding anything [src,
`Entity`]. `ItemEntity`, the minecart classes and boats do not override it, so all of them can use
every portal. A passenger never triggers a portal itself; it travels with its vehicle
(`Entity.teleportCrossDimension` re-mounts passengers on the new vehicle) [src].

A cross-dimension teleport creates a new entity of the same type in the target level and copies
the old one's entire saved data into it (`Entity.restoreFrom` saves and reloads the full tag)
[src]. So a chest or hopper minecart arrives with its inventory, an item entity with its stack,
age and thrower (`ItemEntity.restoreFrom`) [src].

Delay before teleport: `Portal.getPortalTransitionTime` defaults to 0, and `NetherPortalBlock`
returns a non-zero delay only for players (the `players_nether_portal_*_delay` game rules) [src].
Non-player entities teleport on the first tick they are inside the portal.

Cooldown after teleport (`Entity.getDimensionChangingDelay`) [src]:

| Entity | Cooldown |
|---|---|
| Item entity, most others | 300 ticks (15 s) |
| Minecart, boat (`VehicleEntity`) | 10 ticks |
| Vehicle carrying a player | the player's |

Being inside a portal while on cooldown *resets* the cooldown (`Entity.setAsInsidePortal`) [src],
so an entity that arrives inside the exit Nether portal and stays there never bounces back; it
returns only after it leaves the portal and the cooldown runs out. The 10-tick vehicle cooldown
dates from 1.21.2 (24w33a); before it was 15 s [wiki-np].

Momentum: Nether portal, End portal and gateway transitions all keep velocity, rotated by the
change in facing (`Relative.DELTA` includes `ROTATE_DELTA`; `PositionMoveRotation.calculateAbsolute`)
[src]. *Inferred*: an item entity's facing is random (`ItemEntity` sets a random yaw at spawn), so
an item entering the End keeps its speed but leaves the platform in an effectively random
horizontal direction. A minecart's facing follows its track, so its exit direction is predictable.

Game rule: `allow_entering_nether_using_portals` (default true) blocks portal travel *into* the
Nether when false (`ServerLevel.isAllowedToEnterPortal`) [src].

NeoForge note: `Entity.teleport` first fires `CommonHooks.onTravelToDimension`, so any mod can
cancel a crossing [src].

## 2. Routes

### Overworld ↔ Nether (Nether portal)

- Target position is the entity's position scaled by the dimension coordinate scale (÷8 or ×8),
  clamped to the world border (`NetherPortalBlock.getPortalDestination`) [src].
- The game picks the nearest existing portal block within 16 blocks (into the Nether) or 128
  (into the Overworld), by POI search (`PortalForcer.findClosestPortalPosition`) [src]. The search
  loads the target chunks itself, so it works with no player in the target dimension.
- If none is found, **a non-player entity builds a new portal** (`PortalForcer.createPortal`) [src].
  Mobs and items regained this in 1.21 (24w21a) [wiki-np]. A badly linked pair therefore
  silently grows a stray portal and items go there.
- The arrival point inside the exit portal keeps the entity's relative position across the portal
  face, then nudges it to a collision-free spot (`createDimensionTransition`) [src].
- The Nether's leg to the End is two crossings: Nether → Overworld, then an End portal. A Nether
  portal cannot be lit in the End: `BaseFireBlock` forms portals only in the Overworld and the
  Nether [src].

### Overworld or Nether → End (End portal)

- Any End portal outside the End sends to the End (`EndPortalBlock.getPortalDestination`) [src].
  End portal blocks only exist in the stronghold's frame (or by command); building a second one in
  survival is not possible, so every factory's End route goes through the stronghold portal.
- The portal block's entity-inside shape is 6–12 px high (`Block.column(16, 6, 12)`) [src]: an
  entity has to fall or fly into it. *Inferred*: rails cannot sit on the portal, so a minecart has
  to be launched or dropped in.
- Arrival for every non-player entity: (100.5, 50, 0.5), with momentum kept (players are put one
  block lower) [src, `ServerLevel.END_SPAWN_POINT` is (100, 50, 0)].
- **Every arrival calls `EndPlatformFeature.createEndPlatform(..., dropResources = true)`** [src]:
  for x 98–102, z −2–2 it sets y 48 to obsidian and y 49–51 to air, destroying with drops anything
  else there. This runs for item entities too, not only players. [wiki-ep] dates the drop-instead-
  of-overwrite behaviour to 1.21.5 (25w03a). *Inferred* consequences: hoppers, rails or water
  placed in that box are broken on the next arrival; a collector must sit outside x 98–102 / z
  −2–2 or below y 48, and items must be moved off the platform by their own momentum or by
  something reaching in from outside (flowing water from outside the box would be cleared and
  re-flow on each arrival — untested).
- The dragon need not be dead for entities to enter the End.

### End → back (exit portal)

- An End portal inside the End sends a non-player entity to the dimension of the world's respawn
  data (normally the Overworld), to the world spawn's x/z, one block above the top
  `MOTION_BLOCKING_NO_LEAVES` block of that column (`EndPortalBlock`, `Entity.adjustSpawnLocation`)
  [src]. It does **not** go to the portal it came from, nor to any factory.
- *Inferred*: whatever stands at the top of the spawn column receives returning items, so a hopper
  placed as the top block at world spawn would catch them; moving the destination means moving the
  world spawn (`/setworldspawn`, operator).
- The exit portal at (0, ~, 0) on the main island only fills with portal blocks once the dragon is
  killed. It is ~100 blocks from the arrival platform.

### End gateways

- Same-dimension only: they move an entity from the main island to the outer islands and back
  (`EndGatewayBlock.getPortalDestination` targets `currentLevel`) [src].
- Each gateway block has a **40-tick cooldown triggered by any entity** (`TheEndGatewayBlockEntity`,
  `COOLDOWN_TIME = 40`, `EndGatewayBlock.entityInside`) [src]: one entity per gateway every 2 s.
  This is the throughput limit for reaching an outer-island End base. A full chest minecart is
  still one entity.
- Gateways also place a portal ticket on arrival [src].

## 3. Chunk loading at each end

### The portal ticket

`Entity.placePortalTicket` runs after every Nether portal, End portal and gateway teleport, and
after an ender pearl changes dimension [src, `NetherPortalBlock`, `EndPortalBlock`,
`EndGatewayBlock`, `ThrownEnderpearl`]. It adds `TicketType.PORTAL` with radius 3 at the arrival
chunk (for a found Nether portal, at that portal's block) [src]:

- Level 33 − 3 = 30 (`TicketStorage.addTicketWithRadius`) [src]: by the standard level table,
  entity-ticking 3×3 chunks, block-ticking 5×5, loaded 7×7 [wiki-cl].
- Timeout 300 ticks (15 s) [src, `TicketType.PORTAL = register("portal", 300L, 15)`]. Re-adding a
  ticket of the same type and level on the same chunk resets the timer (`TicketStorage.addTicket`)
  [src], so a steady flow keeps the arrival end loaded indefinitely.
- Flags 15: persists across save/restart (with its remaining ticks), loads, simulates, and keeps
  the dimension active [src, `TicketType` flags].

The departure side gets no ticket. An entity only ticks, and so only enters a portal, in a chunk
that is entity-ticking (`ServerLevel.tick` checks `inEntityTickingRange`) [src].

### A dimension with nobody in it

`ServerLevel.tick` stops ticking entities and block entities once the level has gone 300 ticks
without any ticket flagged "keep dimension active" (player, forced, portal or ender-pearl tickets)
[src]. A portal arrival also calls `resetEmptyTime` [src]. So arrivals wake the target dimension;
without them, an empty End freezes after 15 s (its hoppers and item entities stop, nothing is
lost, and it resumes on the next arrival — *inferred*).

### What else loads chunks in vanilla 26.1.2

- **Spawn chunks: gone.** Removed in 1.21.9 (25w31a) with their game rule [wiki-cl]; 26.1.2 has no
  spawn-area ticket (`MinecraftServer` world-spawn setup adds none; `TicketType` has no such type)
  [src].
- `/forceload`: `TicketType.FORCED`, no timeout, persisted, loads and simulates [src]. Operator
  command, so it is a player-set configuration, not a survival mechanic.
- Ender pearls: `ENDER_PEARL` ticket, 40 ticks, re-registered while the pearl lives and its owner
  is an online `ServerPlayer` (`ThrownEnderpearl`) [src]. Useless with no owner online.
- Minecarts load nothing; a moving cart beyond simulation distance freezes until it is in range
  again [wiki-mc]. A long rail line between factory and portal is loaded only by players or
  forceload.
- Portal-ticket chains (*inferred* from the above, a known technical-Minecraft pattern): two linked
  portals with an entity cycling between them (a minecart, with its 10-tick cooldown, on a loop that
  carries it out of the exit portal and back in) refresh both ends' tickets forever. This keeps a
  departure portal's 3×3 chunks loaded with no player. It must be primed once by hand.

### No player anywhere

`MinecraftServer.tickServer` stops ticking the whole server once it has had zero players for
`pause-when-empty-seconds` (default 60, `DedicatedServerProperties`) [src]. A dedicated server with
nobody online moves nothing unless an admin sets that to 0. Single player always has its player
online, wherever they stand.

## 4. Throughput

- End portal and Nether portal blocks impose no per-block limit: every entity inside teleports on
  its own tick (*inferred* from `entityInside` → `setAsInsidePortal` per entity, no block cooldown)
  [src]. The limit is how fast the departure side can feed entities in.
- End gateways: one entity per 40 ticks per gateway block [src].
- Density per entity: an item entity carries one stack (64); a hopper minecart 5 slots; a chest
  minecart 27 slots. *Inferred*: a chest minecart of 27 full shulker boxes moves
  27 × 27 × 64 = 46,656 items as one entity, but loading and unloading shulker boxes unattended needs
  dispensers, pistons and hoppers on both sides.
- A hopper moves 2.5 items/s (8-tick transfer cooldown) — the common bottleneck for feeding item
  entities in or collecting them out (well established; not re-checked in the source).

## 5. Failure modes

1. **Departure end unloaded.** Nothing enters the portal. Needs a player in simulation range,
   `/forceload`, or a primed ticket chain.
2. **Server paused** with nobody online (dedicated server, default after 60 s).
3. **Stray portals.** An item arriving where no portal is within 16/128 blocks builds a new one;
   the flow then splits or reroutes.
4. **Stuck in the exit Nether portal.** An item that lands inside the portal and does not move out
   stays there, cooldown resetting each tick, until something pushes it out.
5. **End platform rebuild** destroys anything in the 5×5×3 box and floor on every arrival,
   dropping it as items into the very stream being collected.
6. **Random exit direction** for items in the End; they may scatter off the platform edge into the
   void (*inferred*: speed is kept, so an item fired in fast can clear the platform).
7. **Despawn.** An item entity despawns after 6000 ticks of ticking (`ItemEntity.LIFETIME`) [src].
   A stalled collector in a loaded area loses items after 5 minutes; in a frozen area the age does
   not advance.
8. **Return lands at world spawn**, not at a factory, and only after the dragon is dead.
9. **Nether-entry game rule** `allow_entering_nether_using_portals` false blocks the Nether leg.
10. **A mod cancels the travel** through NeoForge's travel-to-dimension event.
11. **Entity count.** Thousands of item entities in flight cost server time; packed carriers
    (chest minecarts) avoid it.

## 6. Version notes (1.21 → 26.1.2)

- 1.21 (24w21a): non-player entities build portals again at a target with none [wiki-np].
- 1.21.2 (24w33a): minecart and boat portal cooldown 15 s → 0.5 s [wiki-np]; ender pearls load
  chunks and persist through logout (the behaviour is in [wiki-cl] and [src]; the version is from
  memory, not confirmed in a fetched page).
- 1.21.5 (25w03a): End platform drops the blocks it replaces instead of deleting them [wiki-ep];
  the code in 26.1.2 confirms `dropResources = true` [src].
- 1.21.9 (25w31a): spawn chunks and the spawn-chunk game rule removed [wiki-cl].
- Minecart improvements are still an experiment (`FeatureFlags.MINECART_IMPROVEMENTS`) in 26.1.2
  [src]; the default cart speed cap is 8 blocks/s [wiki-mc].
- [26.1's changelog](https://minecraft.wiki/w/Java_Edition_26.1) moves dimension save folders and
  adds world clocks, and lists no portal, ticket or minecart change (as summarised from the wiki
  page; not read line by line).

## Open questions (need an in-game test)

- Whether items arriving in the End reliably stay on the 5×5 platform, and which collector outside
  the rebuild box catches them well.
- Whether an outside water flow survives the per-arrival platform rebuild well enough to sweep
  items.
- Whether `/setworldspawn` in 26.1 can name a dimension other than the Overworld, which would move
  where End returns land (`RespawnData` carries a dimension [src]; the command was not checked).
