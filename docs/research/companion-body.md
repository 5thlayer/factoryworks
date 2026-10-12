# What can give an LLM companion a body in a 26.1.2 NeoForge world?

Research for [#680](https://github.com/5thlayer/factoryworks/issues/680), under the wayfinder
[#679](https://github.com/5thlayer/factoryworks/issues/679). Checked 2026-10-12 against NeoForge
26.1.2.77 sources (the suite's build pin), the Showcase pack's NeoForge 26.1.2.109, the suite's
mod sources in `~/minecraft_mods`, and each candidate project's own repository or release page.

## Answer

- **An external protocol bot (Mineflayer) is out.** Mineflayer does speak 26.1.2's protocol. But
  NeoForge disconnects a vanilla-protocol client whenever a mod registers a non-optional payload,
  and every suite mod does.
- **A second NeoForge client is the cheapest body that works today.** It runs the suite mods plus
  an off-the-shelf driver: mcpfabric, which is MCP-native, or Baritone. Its costs are a second game
  process and two gaps you would fill yourself: Module-specific actions such as holding a
  Harvester's recipe, and a username the server accepts.
- **A server-side player built in a NeoForge mod gives the best fit and the most control**, at the
  highest build cost. It has to be a Carpet-style `ServerPlayer` placed through `PlayerList`, not
  NeoForge's `FakePlayer`, and you write pathfinding yourself. SiliconeDolls (NeoForge, 26.1.2,
  LGPL-3.0) already supplies the body, but not the brain bridge or the navigation.

| | Works on 26.1.2 now | Walk | Mine / place | Machine menu | Move items | Chat | Build cost | Upkeep |
|---|---|---|---|---|---|---|---|---|
| External bot (Mineflayer) | Protocol yes, **join no** | — | — | — | — | — | blocked | — |
| Second client + mcpfabric | yes (needs NeoForge ≥ 26.1.2.109) | A\* `navigate_to` | yes | generic slot clicks; no Module payloads | yes (`container_transfer`) | yes | low | third-party ports |
| Second client + Baritone | yes (1.18.0, MC 26.1–26.1.2) | best-in-class | `#mine`, schematic build | no | no | `#` commands only | medium (needs an MCP bridge) | third-party ports |
| Server-side player mod (own) | yes (vanilla + NeoForge API) | write it | `gameMode.destroyBlock` / `useItemOn` | yes, and Module calls directly | yes | yes (via events) | high | ours, ported with the suite |
| …on top of SiliconeDolls | yes (26.1.2 branch) | `move`/`look` only | `use`/`attack` actions | via its menu mixins | partial | — | medium-high | third-party port |
| NeoForge `FakePlayer` | yes | **no** (no tick, no physics, unseen) | yes | **no** (`openMenu` is a no-op) | by hand | no | — | — |

## Candidate 1: a server-side body added by a NeoForge mod

### NeoForge's `FakePlayer` is a tool, not a body

`net.neoforged.neoforge.common.util.FakePlayer` (26.1.2.77 sources jar) extends `ServerPlayer`,
but it overrides several methods to do nothing:

- `tick()` is empty, so the player has no movement, gravity or physics.
- `openMenu(...)` returns `OptionalInt.empty()`, so it never opens a machine's menu.
- `sendSystemMessage` is empty, so it never hears a message.
- `startRiding` returns false.
- `die` is empty, and the constructor calls `setInvulnerable(true)`.

Its `FakePlayerNetHandler` drops every packet in both directions, and `FakePlayerFactory.get`
never adds the player to the level, so clients never see it.

The suite already works around these limits in its game tests:

- Craftworks' `AssemblerMachineTests.request` notes that "a fake player's [openMenu] opens
  nothing". It builds the menu with `createMenu` and assigns `player.containerMenu` by hand.
- Each suite mod's `ListeningPlayer` / `FakeBuilder` overrides `sendSystemMessage` so it can
  record what it is told.
- Both fixtures act through `player.gameMode.useItemOn(...)` and `gameMode.destroyBlock(...)`.

So `FakePlayer` can show that a server-side player is able to place, break and use menus. It cannot
be the body itself.

### A Carpet-style real `ServerPlayer` is

The working pattern comes from Carpet's `/player` bots, which are Fabric-only. You subclass
`ServerPlayer`, give it a dummy `Connection`, and log it in with
`server.getPlayerList().placeNewPlayer(...)`. The player then appears in the tab list and in the
world, saves its inventory with the world, and is ticked like any player.

SiliconeDolls (https://github.com/Anvil-Dev/SiliconeDolls, Modrinth `silicone-dolls`) is a NeoForge
port of that pattern:

- Versions: it has a `releases/26.1.2` branch with `minecraft_version_range=[26.1.2]` and
  `neo_version_range=[26.1,)`, and it requires the RollingGate mod.
- License: LGPL-3.0-only, according to Modrinth. The repository itself has no LICENSE file.
- Login: `entity/FakePlayer.java` logs the bot in through `placeNewPlayer` with a
  `FakeClientConnection`. Its `tick()` calls `super.tick()` and then `doTick()`, which is what
  gives the bot real physics.
- Commands: `/player <name>` can `spawn`, `use`, `attack`, `jump`, `look`, `move`, `hotbar`,
  `dropstack` and `kill`.
- Menus: it has mixins on `ChestMenu`, `Slot` and `ServerConfigurationPacketListenerImpl`.
- Missing: pathfinding, any MCP or other external control surface, and chat.

### What the first loop needs, server-side

- **Walk:** no player pathfinder exists in the game. `PathNavigation` belongs to `Mob`, not to
  `Player`. Moving with `move <rotation>` plus `look` works on flat ground, but crossing terrain
  needs your own A\*, or porting Baritone's movement planner.
  - Automatone (https://github.com/Ladysnake/Automatone) is a server-side Baritone fork. It is
    Fabric-only, its newest branch is 1.20, and it was last pushed in 2024, so it gives no direct
    help.
  - A custom `PathfinderMob` entity gets vanilla navigation for free. But it is not a `Player`, so
    `useItemOn`, menus and the suite's `player.containerMenu instanceof …Menu` checks would all
    need a proxy `ServerPlayer` alongside it, and clients need a renderer for it. The Carpet-style
    player is the simpler of the two.
- **Mine and place:** `ServerPlayerGameMode.destroyBlock` / `useItemOn` respect survival rules,
  tool speed only if you drive `handleBlockBreakAction` over ticks, and every suite placement
  hook. The suite's own tests already call them.
- **Open and use a machine's menu:** with a real `ServerPlayer`, `useWithoutItem` on the block
  opens the menu, and slots are moved with `AbstractContainerMenu.clicked(...)`. A server-side mod
  can also call a Module's server entry point directly, which a client-driven body cannot. For
  example, Fieldworks' Held Recipe is set only through `HoldRecipePacket.receive(ServerPlayer, …)`,
  which requires `player.containerMenu instanceof HarvesterMenu`.
- **Read and send chat:** listen to NeoForge's `ServerChatEvent`, and speak with
  `PlayerList.broadcastSystemMessage`. A bot cannot sign player chat, so its lines arrive as
  system messages. That last point is an inference from vanilla's secure-chat design, not
  verified here.
- **Talk to Claude Code:** embed a loopback HTTP or MCP endpoint in the mod, as mcpfabric does
  (see candidate 3). All game access must run on the server thread (`server.execute`).

**Cost:** the highest build cost (body, navigation, bridge) in one process, using the same Java,
NeoForge and Gradle toolchain the suite already uses. Upkeep is a `ServerPlayer` / `PlayerList` /
`Connection` subclass that moves with every Minecraft port, which the suite has to port anyway.
Peaceful is free: `FakePlayer` is invulnerable already, and a Carpet-style player can call
`setInvulnerable(true)` too.

## Candidate 2: an external protocol bot (Mineflayer or similar)

**It speaks 26.1.2, but NeoForge will not let it in.**

- **Protocol support:** minecraft-data's `protocolVersions.json` gives 26.1, 26.1.1 and 26.1.2 the
  same protocol, 775. Mineflayer's `lib/version.js` lists `26.1` as its latest tested version, and
  node-minecraft-protocol's `src/version.js` defaults to `26.1`. Mineflayer 4.39.0 was released on
  2026-09-06. The expected lag is therefore not the problem.
- **The join is rejected:** in NeoForge 26.1.2.77, `NetworkRegistry.initializeOtherConnection`
  handles any client without NeoForge's handshake. It negotiates the server's registered payloads
  against an empty list. If any payload is non-optional, it disconnects the client with
  `neoforge.network.negotiation.failure.vanilla.client.not_supported` ("…Please install NeoForge
  …").
- **Every suite mod trips it:** each one registers its payloads without `.optional()`:
  - Beltworks: `Beltworks.java:119`
  - Fieldworks: `Fieldworks.java:64`
  - Craftworks: `CraftworksNetwork.java:34`
  - Groundworks: `RaisePayload`, `RotatePayload`
  - Pipeworks: `Pipeworks.java:44`, `TankFluidPayload`
  - Wireworks: `Wireworks.java:48`

  Only NeoForge's own registrar in `NetworkInitialization` is `.optional()`.
- **No working Forge handshake:** `minecraft-protocol-forge` implements only the legacy `FML|HS`
  handshake from the 1.7–1.12 era. Its README shows Forge 11.15. It does not implement NeoForge's
  configuration-phase negotiation.
- **Unofficial workarounds:** two small repositories offer server mods that let Mineflayer join
  NeoForge 1.21.1 (`dev-kai0602/neoforge-bot-compat`, `exMachinaAl/neoforge-bot`). Neither targets
  26.1, and both are low-trust.
- **Even with a forced join, the bot would be half-blind** (inference, not tested here). NeoForge
  sends modded registry contents through its own `SyncRegistries` configuration task, and
  `VanillaConnectionNetworkFilter` strips modded tags and attributes for non-NeoForge clients.
  Mineflayer's block and item tables come from vanilla minecraft-data, so suite blocks, items and
  menus would show up only as unknown numeric IDs. Making the suite's payloads optional to let it
  in would be a suite change made for a personal experiment, and
[#679](https://github.com/5thlayer/factoryworks/issues/679) keeps the experiment out of the suite.

**Cost:** blocked. The only way in is to change the suite, plus a NeoForge handshake and modded
registry data in Node.

## Candidate 3: a second client driven by a client-side mod

A second NeoForge 26.1.2 client runs the full suite, so it passes negotiation, knows every block,
item and menu, and sends the Modules' own payloads.

### mcpfabric

Project: https://github.com/denfry/mcpfabric (MIT, Modrinth `mcpfabric`). It is a mod plus a
TypeScript MCP server that "works with … Claude Code".

- **Versions:** it ships NeoForge jars for 26.1.2. The newest is `0.5.1+26.1.2-neoforge` from
  2026-10-05, and it needs NeoForge ≥ 26.1.2.109, exactly the Showcase pack's pin (`pack.toml`).
- **Bridge:** a loopback HTTP JSON-RPC bridge with a bearer token. All game calls run on the
  main thread.
- **Client-side tools:**
  - movement: `set_movement`, `look_at`, `jump`, and A\* `navigate_to`
  - blocks: `break_block`, `place_block`, `use_item`
  - inventory: `swap_slots`, `select_hotbar_slot`
  - containers: `open_container`, `container_transfer`, `close_container`
  - chat: `send_chat`, `get_recent_chat`, `poll_events`
  - vision: `screenshot`, `describe_scene`
  - agent runtime: per-world memory, goals and background jobs (`collect_blocks`, `travel_to`)
- **Server-side tools:** these are admin tools (`run_command`, `set_block`, `give_item`). They are
  not a body.
- **Containers work with any menu:** `ContainerHandlers.java` moves items with vanilla
  `handleInventoryMouseClick`, using quick-move and slot clicks over `menu.slots`. It skips result
  slots. This works for any menu, so it covers the Refiner's input, fuel and output slots
  (`RefinerMenu.quickMoveStack`).
- **Module payloads are not covered:** anything a suite screen sends through its own payload, such
  as Fieldworks' `HoldRecipePacket`, is outside mcpfabric. You would add a small client mod that
  sends it, or press the screen's control through vision and clicks.
- **The tools act as the local player.** The README says client tools are for when the "AI plays
  as you". A companion therefore needs its own second client. This also overlaps
  [#679](https://github.com/5thlayer/factoryworks/issues/679)'s "Remembering the world" question,
  because the agent runtime already has a memory store.

### Baritone

Release: https://github.com/cabaletta/baritone/releases/tag/v1.18.0.

- **Versions:** v1.18.0 says "For Minecraft 26.1 … NeoForge is supported". The `26.1` branch
  builds against `minecraft_version=26.1.2` and declares `versionRange="[26.1, 26.1.2]"`, and its
  release ships `baritone-*-neoforge-1.18.0.jar`.
- **What it does:** it is the strongest pathing, mining (`#mine`) and schematic building
  available.
- **What it lacks:** it has no menu or item-moving automation, and its only control surface is `#`
  chat commands or its Java API. You would wrap it in your own client mod to expose MCP.
- **Combined with mcpfabric:** Baritone could do the navigation inside the same client, though
  that pairing is untested.

### Costs of a second client

- **Account:** the server must accept a second username. That is easy on an offline-mode
  dedicated or dev server, or on LAN with offline auth. On an online-mode server it needs a second
  Minecraft account.
- **Resources:** a second full client means another few GB of RAM and a GL window. HeadlessMC
  (https://github.com/headlesshq/headlessmc, 3.0.0-RC5 on 2026-10-10) runs clients headlessly, but
  its README limits offline accounts to CI use, and nobody has tested it with this pack.
- **Upkeep:** you track two third-party projects' 26.1.x ports (both currently follow quickly). On
  a Minecraft bump, you wait for them or patch them.

## Recommendation for the first loop

1. Prototype with a second NeoForge client running mcpfabric on the Showcase's NeoForge
   26.1.2.109. It gives walk, mine, place, container moves and chat through MCP, with almost no
   code.
2. Write down every action it cannot do, starting with Fieldworks' Held Recipe. These gaps define
   the small client mod you would add, or argue for the server-side body.
3. Build a server-side Carpet-style player mod only if the second client's costs bite. Examples
   are RAM, the window, the account, or wanting the companion to keep working while you are away.
   That mod could start from SiliconeDolls' pattern; its LGPL-3.0 license allows depending on it,
   with care if you copy code.

## Sources

- NeoForge 26.1.2.77 sources jar (`~/.gradle/caches/.../neoforge-26.1.2.77-sources.jar`):
  `common/util/FakePlayer.java`, `FakePlayerFactory.java`,
  `network/registration/NetworkRegistry.java` (`initializeOtherConnection`),
  `network/NetworkInitialization.java`, `network/configuration/SyncRegistries.java`,
  `network/filters/VanillaConnectionNetworkFilter.java`.
- Suite sources in `~/minecraft_mods`:
  - Craftworks: `gametest/AssemblerMachineTests.java`, `machine/RefinerMenu.java`,
    `network/CraftworksNetwork.java`.
  - Groundworks: `gametest/ListeningPlayer.java`.
  - Fieldworks: `network/HoldRecipePacket.java`, `Fieldworks.java`.
  - The payload registrations in Beltworks, Pipeworks and Wireworks.
  - `factoryworks-showcase/pack.toml`.
- PrismarineJS repositories:
  - `minecraft-data/data/pc/common/protocolVersions.json` and `data/dataPaths.json`.
  - `mineflayer/lib/version.js` and its releases.
  - `node-minecraft-protocol/src/version.js`.
  - The `node-minecraft-protocol-forge` README.
- https://github.com/cabaletta/baritone: releases, plus `gradle.properties` and
  `neoforge.mods.toml` on the `26.1` branch.
- https://github.com/denfry/mcpfabric: the README,
  `src/client/java/dev/mcpfabric/client/handlers/ContainerHandlers.java` and
  `ClientChatHandlers.java`, and the Modrinth versions API.
- https://github.com/Anvil-Dev/SiliconeDolls: the `releases/26.1.2` branch's
  `gradle.properties` and `entity/FakePlayer.java`, and the Modrinth project API (license).
- https://github.com/Ladysnake/Automatone (branches and last push).
- https://github.com/headlesshq/headlessmc (README and releases).
