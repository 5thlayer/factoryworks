# Companion movement and building: what can be reused

Question ([#681](https://github.com/5thlayer/factoryworks/issues/681), map
[#679](https://github.com/5thlayer/factoryworks/issues/679)): what can the companion use to move and build in a
Minecraft 26.1.2 NeoForge world without hand-written pathfinding? The three candidates checked are vanilla mob
navigation, Baritone, and anything similar. For each one: walking to a spot, mining a block, and placing a block at a
spot.

## Answer

There are two workable designs. A third one is ruled out.

1. **A second Minecraft client running Baritone.** Baritone 1.18.0 ships a NeoForge build for Minecraft 26.1–26.1.2,
   including an API jar meant for other mods to call. It handles all three verbs: walking to a goal, mining blocks by
   type, and building a schematic at an origin. It tunnels, pillars and bridges on its way there. The catch is that
   Baritone drives a `LocalPlayer`, so the companion has to be a whole second client, with its own account or an
   offline-mode server. A small client mod would bridge MCP to Baritone's API.
2. **A server-side mob body with a `FakePlayer` as its hands.** Vanilla `PathNavigation` works only for a `Mob`. A
   custom `PathfinderMob` gets walking for free, and a NeoForge `FakePlayer` can then break and place through
   `ServerPlayerGameMode`, with the player's events and placement logic. Beltworks' game tests already place belts this
   way. Walking only covers terrain that is already passable, though. Getting to a spot that needs digging or pillaring
   means writing that logic.
3. **Mineflayer (Mindcraft, Voyager-style bots)** is ruled out. Our NeoForge server refuses vanilla-protocol clients
   because the -works mods register required network channels.

For the first loop (harvest, smelt, belt, on peaceful, near the player's base), option 2 is the smaller build and
needs no second client. Option 1 is the one that needs no movement code, and it is the right choice if the companion
has to travel across rough terrain or build multi-block layouts from a schematic.

## 1. Vanilla mob navigation (`PathNavigation`)

Source: the NeoForm-merged, NeoForge-patched Minecraft 26.1.2 sources in the local Gradle cache
(`~/.gradle/caches/neoformruntime/intermediate_results/mergeWithSources_fceccdc1…_output.jar`). This is the code the
mods compile against with NeoForge 26.1.2.77.

**It is bound to `Mob`, not to a player.**

- `PathNavigation` has a `protected final Mob mob` field and the constructor `PathNavigation(Mob mob, Level level)`.
  `GroundPathNavigation(Mob mob, Level level)` is the same
  (`net/minecraft/world/entity/ai/navigation/PathNavigation.java:37,59`; `GroundPathNavigation.java:24`).
- The A* layer is bound to it as well. `PathFinder.findPath(PathNavigationRegion, Mob, Set<BlockPos>, …)`,
  `NodeEvaluator.prepare(PathNavigationRegion, Mob)` and `PathfindingContext(CollisionGetter, Mob)` all take a `Mob`
  (`net/minecraft/world/level/pathfinder/PathFinder.java:44`, `NodeEvaluator.java:26`,
  `PathfindingContext.java:16`).
- Following a path means `PathNavigation.tick()` calling `mob.getMoveControl().setWantedPosition(…)`
  (`PathNavigation.java:231`). A `Player` has no `MoveControl`.
- So `PathNavigation` cannot be pointed at a `ServerPlayer` or `FakePlayer`. Nor at the new `Mannequin`:
  `Mannequin extends Avatar` and `Avatar extends LivingEntity`, not `Mob`
  (`net/minecraft/world/entity/decoration/Mannequin.java:27`, `net/minecraft/world/entity/Avatar.java:13`). A
  companion that wants vanilla pathing has to be a `PathfinderMob` subclass of its own, rendered as a humanoid.

**What it offers:**

- **Walk to a spot: yes.** `moveTo(x, y, z, speed)`, `moveTo(x, y, z, reachRange, speed)`, `moveTo(Entity, speed)`,
  and `createPath(BlockPos, reachRange)` or `createPath(Set<BlockPos>, reachRange)` for "any of these spots"
  (`PathNavigation.java:112-200`). It handles doors and swimming through `setCanOpenDoors` and `setCanFloat`
  (`PathNavigation.java:387,417`). There is a stuck timeout (`resetStuckTimeout`) and a `isDone()`/`getPath()` state
  to poll.
- **Range limits.** Path search only looks inside a `PathNavigationRegion` cube whose radius is the max path length
  around the mob (`PathNavigation.java:152-157`). The node budget is `FOLLOW_RANGE × 16`
  (`PathNavigation.java:62,70`). So the reach is set by the `FOLLOW_RANGE` attribute (or `setRequiredPathLength`),
  and paths stay inside loaded chunks.
- **Mine a block: no.** Navigation never plans block changes. Vanilla mobs break blocks with their own goals, not
  with navigation, for example `BreakDoorGoal` and `RemoveBlockGoal`
  (`net/minecraft/world/entity/ai/goal/`). A companion would break a block once it stands next to it.
- **Place a block: no.** The same applies. No terrain modification of any kind: no pillaring, bridging or digging
  through. A target that can only be reached by changing terrain comes back as a null or partial path.

**Hands for a mob body: NeoForge `FakePlayer`.** Source: `neoforge-26.1.2.77-sources.jar`.

- `FakePlayerFactory.get(ServerLevel, GameProfile)` returns a cached `FakePlayer extends ServerPlayer`, whose
  `tick()` is a no-op and whose network handler is a stub (`net/neoforged/neoforge/common/util/FakePlayer.java:88-141`,
  `FakePlayerFactory.java`). It is not added to the level, so it has no visible body of its own. It is only "hands".
- `player.gameMode.destroyBlock(pos)` fires NeoForge's `BlockEvent.BreakEvent` through `CommonHooks.fireBlockBreak`.
  `useItemOn(player, level, stack, hand, hitResult)` fires `PlayerInteractEvent.RightClickBlock`
  (`net/minecraft/server/level/ServerPlayerGameMode.java:268-330,369-380`, NeoForge-patched). Placing through
  `useItemOn` runs the item's `BlockItem.place` and the block's `getStateForPlacement` with the fake player's look
  direction. That matters for belts: `BeltTileBlock.getStateForPlacement` faces the belt along
  `ctx.getHorizontalDirection()`, and `BeltTileItem` reshapes neighbouring tiles on placement
  (`~/minecraft_mods/beltworks/src/main/java/io/github/_5thlayer/beltworks/blocks/BeltTileBlock.java:134`,
  `items/BeltTileItem.java`). Calling `level.setBlock` directly would skip both.
- This is already done in this suite. Beltworks' game tests drive hand placement with
  `FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), name))` in survival mode, giving each test its own
  profile because "the shared fake player's inventory is every test's at once"
  (`~/minecraft_mods/beltworks/src/main/java/io/github/_5thlayer/beltworks/gametest/BeltTileHandTests.java:261-268`).

A heavier version of this design is a real `ServerPlayer` added to the player list with a fake connection, which is
what Carpet's `/player` does on Fabric. On the server, a player's movement is driven by its client, so that version
would need its own physics ticking. I found no NeoForge 26.1.2 port of that.

## 2. Baritone

Sources: the [cabaletta/baritone](https://github.com/cabaletta/baritone) releases API, and the `26.1` branch at
[`016cc21`](https://github.com/cabaletta/baritone/tree/016cc210434894f371baf0c327ac94907d12dcd3). License LGPL-3.0
(`LICENSE`).

**It exists for NeoForge 26.1.2.**

- Release **v1.18.0** (2026-08-31) is marked "For Minecraft 26.1. Forge is supported. Fabric is supported. NeoForge is
  supported." Its assets include `baritone-api-neoforge-1.18.0.jar`, `baritone-standalone-neoforge-1.18.0.jar` and
  `baritone-unoptimized-neoforge-1.18.0.jar`, plus a signed `checksums.txt`. Later releases v1.19.0 and v1.20.0 target
  26.2 and 26.3.
- On the `26.1` branch, `gradle.properties` sets `minecraft_version=26.1.2`, `neoforge_version=78` and
  `java_version=25`. `neoforge/src/main/resources/META-INF/neoforge.mods.toml` declares `minecraft` as
  `versionRange="[26.1, 26.1.2]"`, with a mixin config. Note the mod id is spelled `baritoe`.
- **As a library:** `SETUP.md` says to use the `baritone-api-*` jar "if another one of your other mods has a Baritone
  integration". In that jar only the non-API packages are obfuscated. The API jar is a release asset; I saw no Maven
  repository documented.

**It is a client-side bot.** `BaritoneProvider` creates its primary instance from `Minecraft.getInstance()`
(`src/main/java/baritone/BaritoneProvider.java:49`). `IBaritoneProvider` looks instances up by `LocalPlayer`,
`Minecraft` or `ClientPacketListener`, and `createBaritone(Minecraft)` makes one per client
(`src/api/java/baritone/api/IBaritoneProvider.java:45-118`). It moves a player by simulating that client's inputs,
and the server sees ordinary player packets. So for the companion it has to run inside **a second Minecraft client**
that joins my world as its own player, with NeoForge, the -works mods, Baritone and a bridge mod exposing MCP. That
client needs its own account, or a server in offline mode. A dev `runClient` uses an offline name and cannot join an
online-mode server or LAN world (my inference, not checked in source).

**What it offers** (`src/api/java/baritone/api/process/`):

- **Walk to a spot:** `getCustomGoalProcess().setGoalAndPath(Goal)` with `GoalBlock`, `GoalNear`, `GoalXZ`, and so on
  (`ICustomGoalProcess.java:29-53`). `IGetToBlockProcess` walks to the nearest block of a type, and `IFollowProcess`
  follows an entity. Unlike vanilla, it breaks and places blocks on the way (`allowBreak` and `allowPlace` default to
  true, `Settings.java:63,78`). It works across chunks and stays inside the client's loaded area.
- **Mine a block:** `getMineProcess().mine(quantity, Block...)` or `mineByName(quantity, "minecraft:stone")`
  (`IMineProcess.java:39-107`). It finds, walks to, breaks, and picks up drops (`mineScanDroppedItems`, default true,
  `Settings.java:1146`). `blocksToDisallowBreaking` and `blocksToAvoidBreaking` keep it off things
  (`Settings.java:247,254`). That is one way to protect my work, but the lists hold block types, not regions.
- **Place a block at a spot:** `getBuilderProcess().build(name, ISchematic, origin)`, a `.schem` or litematic file,
  and `clearArea(corner1, corner2)` (`IBuilderProcess.java:42-70`). One block is a 1×1×1 `ISchematic`
  (`api/schematic/FillSchematic.java` and related). The builder predicts the result of a placement with the block's
  own `getStateForPlacement(new BlockPlaceContext(…))` (`src/main/java/baritone/process/BuilderProcess.java:395-402`).
  It matches orientation unless `buildIgnoreDirection` or `buildIgnoreProperties` is set (`Settings.java:326-332`), so
  modded blocks that face the player's look should work. Belts that reshape their neighbours on placement may confuse
  its notion of "correct", so this needs testing in game. `allowInventory` defaults to false (`Settings.java:93`), so
  it only uses the hotbar unless that is enabled.
- **Command surface:** besides the Java API, `ICommandManager` runs Baritone's chat commands (`#goto`, `#mine`,
  `#build`) from a string (`src/api/java/baritone/api/command/manager/ICommandManager.java`). A bridge could start
  with that before using the typed API.

**Server-side forks.** [Ladysnake/Automatone](https://github.com/Ladysnake/Automatone) is a "Serverside fork of
baritone" that drives any entity. Its branches stop at 1.20 and its last push was 2024-01-21. It is Fabric-only, so it
does not apply here.

## 3. Mineflayer and LLM bots built on it

- [Mineflayer](https://github.com/PrismarineJS/mineflayer) 4.39.0 (2026-09-06) "Supports Minecraft 1.8 to 26.1"
  (`docs/README.md:20`). Plugins `mineflayer-pathfinder` and `mineflayer-collectblock` handle walking, mining and
  placing. [Mindcraft](https://github.com/mindcraft-bots/mindcraft), an LLM agent framework, is built on them
  (`package.json`).
- It speaks the vanilla protocol. When NeoForge sees a vanilla connection, it negotiates every registered payload
  against an empty client list and disconnects with "You are trying to connect to a server that is running NeoForge,
  but you are not" if any payload is not optional
  (`net/neoforged/neoforge/network/registration/NetworkRegistry.java:383-402`, NeoForge 26.1.2.77). Groundworks,
  Beltworks, Fieldworks and Craftworks all register payloads without `.optional()`
  (`groundworks/.../RaisePayload.java:33`, `RotatePayload.java:41`, `beltworks/.../Beltworks.java:119`,
  `fieldworks/.../Fieldworks.java:64`, `craftworks/.../network/CraftworksNetwork.java:34`). The modded block registry
  would not sync to a vanilla client in any case. **So Mineflayer cannot join this world.**

## Comparison

| | Vanilla `PathfinderMob` and `FakePlayer` hands | Baritone in a second client | Mineflayer |
|---|---|---|---|
| Runs where | Server, in a companion mod | A second NeoForge client plus a bridge mod | External Node process |
| Joins this world | Yes | Yes, needs a second account or offline mode | **No**, NeoForge refuses vanilla clients |
| Walk to a spot | Yes, over passable terrain, inside a `FOLLOW_RANGE` cube | Yes, digs, pillars, bridges | n/a |
| Mine a block | Write it: walk adjacent, then `gameMode.destroyBlock` | `mine(n, blocks)`, including pickup | n/a |
| Place a block | Write it: walk adjacent, aim, then `gameMode.useItemOn` | `build(schematic, origin)`, orientation-aware | n/a |
| Events and protection hooks | Break and RightClick events fire as for a player | Ordinary player packets, all events fire | n/a |
| Body | A custom humanoid entity to register and render | A real player, with skin and tab-list entry | n/a |
| Cost | A mod plus mining and placing glue; no terrain planning | A second game instance's RAM and CPU, plus an account | — |

## Open points for the spec

- Whether a second running client is acceptable on this machine alongside mine. That
  decides between Baritone and the mob body.
- Whether Baritone's builder judges a Beltworks belt "correct" after the belt reshapes its neighbours. Test this in
  game with one belt run before relying on `build`.
- A Baritone instance stays in the client's loaded chunks, and a `PathfinderMob` only ticks in loaded chunks. Both
  feed into "Distance and chunks" on the map.
