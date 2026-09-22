---
status: accepted
---

# The Radar charts terrain through FTB Chunks' internal chunk task, and marks patches through its public icon API

ADR-0045 made the Radar Factorio's Radar: it charts map at range and detects nothing hidden. The
pack's map is FTB Chunks, and FTB Chunks draws only what the client has loaded. Its map is built on
the client from vanilla chunk packets, the server holds no "explored" state, and the build installed
here (26.1.2.8) has no working route for the server to push map data: the region-sharing packets read
nothing and `RequestMapDataPacket` is empty.

**Decision (#116).** The Radar charts a **sector** (32×32 blocks, Factorio's chunk) by generating its
four Minecraft chunks on the server and sending them to every member of the owner's team in a
payload of the pack's own. The client builds a standalone chunk from the payload, never adds it to
the level, and hands it to FTB Chunks' `ChunkUpdateTask` on `FTBChunksClient.MAP_EXECUTOR`, which
colours, stores and persists it exactly as a walked chunk. Each outfield patch in a charted sector
gets a marker drawn as an FTB `MapIcon` through `FTBChunksClientEvent.MapIcon`, fed by a second
payload and removed by a third when the patch is mined out. The markers go through a minimal client
renderer interface, as GregTech-Modern's map integration does, with FTB Chunks the only
implementation. All FTB code lives in one package, `compileOnly` against a pinned version, and loads
only when `ftbchunks` is present.

**Scan order (amended, #369).** The Radar scans as Factorio's does, with a wider nearby area. Every
250 kJ it pulses its nearby area, and every 10 MJ it scans one long-range sector within 14, beyond
that area. Both count
the same draw, so the long range keeps Factorio's 33.3 s a sector. The long range walks square rings
outward, each from its top-left sector clockwise, takes the first sector the team's chart lacks, and
re-scans in turn once none is left. Three things are adapted. The nearby area is 9×9 sectors, 18
Minecraft chunks, where Factorio's is 7×7: at one tile per block Factorio's is 14 chunks across,
inside a normal render distance, so it would reveal nothing a player at the Radar has not seen. It
stays odd so the Radar's own sector is its middle. "Unexplored" is the team's chart, since
the server does not know what a player walked. A pulse charts only the area's uncharted sectors, one
a tick, since 324 chunks generated in one tick is a stall and a charted sector is never re-sent. A
re-scan spends its 10 MJ and changes no map, for the same reason, until the chart has a live refresh.

**Markers (amended, #370).** A patch belongs to the sector holding its centre. When a team charts a
sector, the outfield discs whose structure starts its four chunks keep are recorded for it, with the
surface height at each centre, and the record is saved with the chart. A starting field is a jigsaw
piece, not a disc, and gets none. Walking marks a patch too, for the walker only, since walked
terrain is on the walker's map and not the team's chart: when the server sends a player the chunk
holding a disc's start, the patch is recorded as theirs and saved. A marker is drawn as the item the
resource drops, raw iron for iron, and names the resource on hover, with no amount. The client keeps
its markers in memory only. The server sends a player every marker of their team's chart and of their
own walking that their client lacks, at each login, on joining a team and on entering a dimension, and
each new one once as it is charted or walked. What a player has been sent is not saved. A marker is a few bytes, so re-sending at login
costs little, and unlike the terrain a wiped client loses none.

**Considered: nearest-first long range with no nearby area.** It was the first build. Rejected: it
spent its first minutes re-charting ground the player had just walked, so the first new terrain
appeared after several 33 s scans.

**Considered: vanilla chunk packets.** Rejected: the client drops a chunk outside its storage
radius, and FTB's hook ignores a chunk the level does not hold.

**Considered: writing FTB's `MapRegionData` arrays from a compact payload.** Rejected: smaller on
the wire, but it reimplements FTB's block and biome colour indexing, which is more internal
surface than one task constructor.

**Considered: FTB waypoints for the markers.** Rejected: they land in the player's own waypoint
list, the player can delete them, and removing one needs a second per-client call that can miss.
An icon is recomputed from the pack's registry on each refresh, so removal is exact.

**Consequences.**

- `ChunkUpdateTask`, `MapManager` and `MAP_EXECUTOR` are not FTB's API. An FTB Chunks update can
  break the terrain half at compile time or silently at run time. The icon half is public API and
  should survive one. Bump FTB Chunks deliberately, and check the map by hand after a bump.
- The chart is the team's and FTB stores it per client, so the server keeps the team's charted
  sectors and each player's delivered set, and sends a player what they missed at login, throttled.
  A sector counts as delivered when it is sent, not when the client draws it. A client that drops
  one never gets it again: a client without FTB Chunks, a map not ready after the login grace, a
  player who changed dimension in flight, or a wiped local map. Acknowledging each sector is the
  fix if a human check finds holes.
- A Radar runs only while its own chunk is loaded. That leaves an outpost's Radar to FTB Chunks'
  force-load claims, and is to be revisited with combat (#230).
