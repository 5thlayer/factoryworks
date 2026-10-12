# How a Claude Code session talks to a running Minecraft server, both ways

Research for [#682](https://github.com/5thlayer/factoryworks/issues/682), under the companion map
[#679](https://github.com/5thlayer/factoryworks/issues/679). Sources were read on 2026-10-12, against
Claude Code 2.1.295 (installed locally), MCP spec revision 2025-11-25, the Java MCP SDK v2.0.1 and
NeoForge `26.1.x`.

## Answer

- **Session to game.** Use a small **stdio MCP server that Claude Code spawns** (a sidecar). It
  exposes the game tools and talks to the mod over a localhost socket that the mod opens. Do not
  run the MCP server inside the mod. The game is a long-lived process launched through quicklaunch,
  and the documented channel contract only covers a stdio subprocess that Claude Code spawns (see
  below). Claude Code can also reach an in-mod Streamable HTTP server for tools alone, but the Java
  SDK's HTTP transport needs a Jakarta Servlet container embedded in the mod.
- **Game to session.** Use a **Claude Code channel**. The same sidecar declares the `claude/channel`
  capability and emits `notifications/claude/channel` for each chat line the mod forwards. An idle
  session starts working on the event. A busy session queues it and gets it on its next turn. Claude
  answers in game through a `reply`-style tool that the mod shows in chat.
- **Latency.** The socket and the tick hop take milliseconds. The model takes seconds for each
  tool round trip, so a request that needs several tool calls takes tens of seconds (an estimate,
  see [Latency](#latency)).

```
 in-game chat ──ServerChatEvent──▶ mod bridge (localhost socket, own thread)
                                         │  ▲
                                         ▼  │ JSON lines / WebSocket
                         sidecar (stdio MCP server, spawned by Claude Code)
                           • experimental capability  claude/channel
                           • notifications/claude/channel  ──▶ session
                           • tools: say, look, move_to, mine, … ◀── session
```

## Game to session: channels

**What a channel is.** A channel is an MCP server that pushes events into a session that is
already running. It can be two-way: "Claude reads the event and replies back through the same
channel". Events arrive only while the session is open.
([Channels](https://code.claude.com/docs/en/channels))

**Contract** ([Channels reference](https://code.claude.com/docs/en/channels-reference)):

- The server declares `capabilities.experimental['claude/channel']: {}`, emits
  `notifications/claude/channel` and connects over stdio. The overview says "Claude Code spawns it
  as a subprocess and communicates over stdio."
- The notification params are `content` (string, the body of the tag) and `meta`
  (`Record<string,string>`; each key becomes a tag attribute, and keys may contain only letters,
  digits and `_`, or they are silently dropped). Claude sees
  `<channel source="<server>" player="…">…</channel>`.
- Claude Code gets the `instructions` string as context when the server connects. Use it to tell
  Claude that a channel message is the player talking and that the answer goes through the reply
  tool.
- **Idle and busy.** In the webhook walkthrough, after the event arrives "You'll then see Claude
  start responding", so an idle session wakes. "Events queue into the session and are processed in
  order. If several notifications arrive while Claude is busy, they're delivered together on the
  next turn."
- Claude Code does not acknowledge a notification, and if the server is not registered as a
  channel it drops events silently. If the companion needs to know a message was received, it
  needs its own acknowledgement, such as the reply tool.
- **Reply.** Any standard MCP tool can be the reply tool. The terminal shows the tool call and
  "sent", not the reply text ([Channels](https://code.claude.com/docs/en/channels)). That fits
  #679, which puts play in game and teaching in the terminal.
- **Gating.** "An ungated channel is a prompt injection vector." Gate on the sender's identity,
  here the player UUID, before emitting.
- **Permission relay** (optional): `claude/channel/permission` forwards tool-approval prompts as
  `notifications/claude/channel/permission_request`, and the verdict comes back as
  `notifications/claude/channel/permission` with `request_id` and `behavior`. A `yes abcde` typed in
  game could then approve a tool. The Java SDK has no hook for custom inbound notifications (see
  below), so relay is easier from a TypeScript sidecar.

**Requirements and caveats**:

- Research preview. The account must use claude.ai or Console authentication, not Bedrock, Vertex
  or Foundry. Pro and Max accounts without an org skip the org gate. The flags do not appear in
  `claude --help`, and "the `--channels` flag syntax and protocol contract may change".
  ([Channels](https://code.claude.com/docs/en/channels#research-preview))
- A custom channel is not on the allowlist. Start each session with
  `claude --dangerously-load-development-channels server:<name>`, which shows a full-screen warning
  that you confirm. The flag is **ignored in `-p` and Agent SDK runs**, so the companion must be an
  interactive session.
  ([Test during the research preview](https://code.claude.com/docs/en/channels-reference#test-during-the-research-preview))
- Being listed in `.mcp.json` is not enough. The server must also be named in `--channels` or the
  dev flag.
- **Protocol trap.** On Claude Code's v2 MCP client runtime, a channel server that negotiates
  protocol revision **2026-07-28** cannot deliver channel messages and is not registered as a
  channel. The v2 runtime is used on Bedrock, Vertex and Foundry, and in sessions with telemetry or
  feature-flag fetching disabled. `MCP_PROTOCOL_NEGOTIATION=legacy` keeps servers on the earlier
  handshake. ([MCP: push messages with channels](https://code.claude.com/docs/en/mcp))
  The Java SDK v2.0.1 advertises only `2024-11-05` through `2025-11-25` (MIGRATION-2.0.md,
  "`protocolVersions()` default"), so a Java sidecar cannot trip this trap. A TypeScript sidecar on
  SDK 2.x might, so pin SDK 1.x or set the env var.
- While a permission prompt is open, the session pauses until someone answers. For play without
  prompts, allow `mcp__<server>__*` in permissions. `--dangerously-skip-permissions` exists, but
  the docs advise it only in trusted environments.

**Other ways in, all worse for chat:**

| Mechanism | Why not as the primary path |
| --- | --- |
| A polling tool, e.g. `get_chat()` | Claude only calls it during a turn. An idle session never polls, so it cannot answer chat. |
| `Monitor` tool ([tools reference](https://code.claude.com/docs/en/tools-reference#monitor-tool)) | Feeds each output line back to Claude, but every watch has a deadline (5 min by default, 30 min at most) and must be re-armed. Its WebSocket source denies private addresses. A `command` watch such as `curl -N` would work but is clumsier than a channel. It is also unavailable when `DISABLE_TELEMETRY` is set. |
| `asyncRewake` hook ([hooks](https://code.claude.com/docs/en/hooks)) | Runs in the background and wakes Claude on exit code 2 with stderr as a system reminder. It fires once per hook run, so it is not a message stream. |
| `UserPromptSubmit` / `Stop` `additionalContext` | Adds context to a turn that is already starting or ending. Cannot start a turn. |
| `Notification` hook | Observes only. Cannot wake the session. |

## Session to game: tools

**Transports Claude Code supports** ([MCP](https://code.claude.com/docs/en/mcp)): stdio, HTTP
(Streamable, recommended for remote servers), SSE (deprecated) and WebSocket (`"type": "ws"`,
configured in `.mcp.json`).

**The MCP spec** ([Transports, 2025-11-25](https://modelcontextprotocol.io/specification/2025-11-25/basic/transports)):
with stdio, "The client launches the MCP server as a subprocess". With Streamable HTTP, the server
"operates as an independent process", and a client GET opens an SSE stream for messages the server
initiates. A local HTTP server **MUST** validate `Origin` and **SHOULD** bind to `127.0.0.1`.

**Official Java MCP SDK** ([modelcontextprotocol/java-sdk](https://github.com/modelcontextprotocol/java-sdk), v2.0.1, 2026-08-19):

- Java 17+. Maven `io.modelcontextprotocol.sdk:mcp` bundles `mcp-core` and Jackson 3, and
  `mcp-json-jackson2` is the alternative. The API is reactive (Reactor `Mono`) with a sync wrapper.
  No Spring is needed.
- Server transports in `mcp-core` are `StdioServerTransportProvider`,
  `HttpServletStreamableServerTransportProvider`, `HttpServletStatelessServerTransport` and the
  deprecated `HttpServletSseServerTransportProvider`. All the HTTP ones are **Jakarta Servlet**
  classes. There is no transport on the JDK's built-in `HttpServer`, so an in-mod HTTP MCP server
  must also embed Jetty or Tomcat (README, "Server transport"). Shading Reactor, Jackson 3 and a
  servlet container into a NeoForge mod is real weight and a classloading risk. Nothing was
  measured.
- A channel in Java is possible.
  `McpSchema.ServerCapabilities.Builder.experimental(Map<String,Object>)` declares
  `claude/channel`, and the public
  `McpServerTransportProviderBase.notifyClients(String method, Object params)` sends
  `notifications/claude/channel`. Two limits: `McpAsyncServer` registers inbound notification
  handlers only for `initialized` and `roots/list_changed` (`prepareNotificationHandlers`), so
  permission relay would need a custom transport or session. And the default `requestTimeout` is
  10 hours. (Source: `McpSchema.java`, `McpAsyncServer.java`, `McpServerTransportProviderBase.java`
  at tag `v2.0.1`.)

**Recommended split.** The **mod** opens a dependency-free localhost bridge: JSON lines over a
socket, or a WebSocket on the JDK's `com.sun.net.httpserver` / `java.net` APIs. It forwards chat
and events out, and runs commands sent in. The **sidecar** speaks MCP and holds the tool schemas.
It can be either:

- TypeScript on `@modelcontextprotocol/sdk`, the path the channel docs document (Node 26.9 is
  installed here, and Bun is not needed). Permission relay is easy.
- Java on the Java SDK's stdio transport, run as `java -jar`. It shares code with the mod and is
  safe on protocol version, but has no relay.

Claude Code starts and stops the sidecar with the session. The game outlives sessions, so the
sidecar must handle "game not running" and reconnect to the bridge.

**Long actions.** MCP tool calls in the main conversation that run past **2 minutes** move to a
background task (`CLAUDE_CODE_MCP_AUTO_BACKGROUND_MS`). Stdio servers get a 30-minute idle timeout
(`CLAUDE_CODE_MCP_TOOL_IDLE_TIMEOUT`). Output is capped at 25,000 tokens by default
(`MAX_MCP_OUTPUT_TOKENS`). ([MCP](https://code.claude.com/docs/en/mcp)) Walking, mining or
building should therefore return at once with a job id, then report the outcome as a channel event.
That way the session stays free to hear chat. It also means "it reports what it notices" uses the
same path as the player's chat.

## Inside the mod (NeoForge 26.1)

- **Chat in.** `ServerChatEvent` fires "whenever a `ServerboundChatPacket` is received", on the
  game bus, "only on the logical server". It is cancellable and exposes `getPlayer()`,
  `getUsername()` and `getRawText()`. Cancel it to keep companion-addressed lines out of public
  chat if wanted. (`neoforged/NeoForge@26.1.x`,
  `src/main/java/net/neoforged/neoforge/event/ServerChatEvent.java`)
- **Lifecycle.** `ServerStartedEvent` and `ServerStoppingEvent` (in `event/server/`) are where the
  bridge opens and closes. Singleplayer works because "the player's physical client spins up a
  logical server" ([Sides](https://docs.neoforged.net/docs/concepts/sides)), so the bridge lives in
  the integrated server, which quicklaunch already starts.
- **Threads.** Game-bus events "always run on the same thread"
  ([Events](https://docs.neoforged.net/docs/concepts/events)). The bridge's socket thread must
  hand every world read and action to the server thread. Vanilla's `MinecraftServer` is a
  `BlockableEventLoop`, so `server.execute(...)` / `server.submit(...)` does this. That comes from
  vanilla code, not the NeoForge docs, so check it against 26.1.2 when building.
- **Chat out.** The reply tool sends a system chat message to the player. The vanilla API is not
  researched here.

## Latency

No primary source publishes end-to-end numbers. This is a breakdown to measure against, **not a
finding**:

| Leg | Expected |
| --- | --- |
| Localhost socket, mod to sidecar to Claude Code | well under 10 ms |
| Hop to the server thread | up to one tick, 50 ms at 20 TPS |
| Channel event to the first model action | one model call, which takes seconds |
| Each further tool call | one more model call each |
| "Come here and chop that tree" (about 3–6 tool calls) | roughly 10–60 s, dominated by the model |

So build coarse tools (one `harvest_area` instead of 40 `break_block` calls), and send a quick
"on it" before long work. The first slice should time one round trip with the sidecar logging
timestamps.

## Open questions

- Does `claude/channel` register over an HTTP or `ws` server, not just stdio? The docs describe
  only stdio, and the ws transport is described as suiting servers "that push events to Claude
  unprompted". Not tested. Assume stdio.
- Does the session's MCP client run on the v1 or the v2 runtime here (`/mcp` or `--debug`)? It
  decides whether `MCP_PROTOCOL_NEGOTIATION=legacy` is needed.
- Are plugin-declared monitors bound by the same Monitor deadline? Only relevant if channels are
  withdrawn from the preview.
