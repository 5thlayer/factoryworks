# `factoryworks_core`

<!-- Copied from publish/core/description.md; edit both. -->
FactoryWorks Core holds the machines and rules of the FactoryWorks modpack: the furnaces, mining drills, Assembling Machines, Chemical Plant and Oil Refinery, the Boiler and Steam Engine, electric poles, the Radar, ore patches that hold an amount and run out, and the ore fields and oil wells laid across Terra. Each runs at the rates Factorio gives it.

It adds no recipes and no research of its own. Those live in the modpack's scripts and data, so the mod on its own places machines nobody can craft.

The pack's first-party NeoForge mod, built as a Gradle subproject of this repo (ADR-0014).

## What is in here, and what is not

Its remit is **mechanism only** (ADR-0015): what a scripting API in this pack cannot express. Each
machine is the pack's own block, several on an Oritech model or entity (ADR-0060). Today that is:

- **Furnaces** (`smelting/`) — the Stone, Steel and Electric Furnace, the pack's own smelting
  recipe type, and the fuel table the burners read (ADR-0047).
- **Mining** (`mining/`) — the Engineer's Pick (ADR-0039) and the burner and electric mining drills,
  rigs that eject onto the ground (ADR-0043).
- **Ore** (`ore/`) — an ore block that carries an amount and a patch that runs out (ADR-0041), and
  the outfield discs laid across Terra (ADR-0045).
- **Oil** (`oil/`) — oil wells and the Pumpjack; crude is infinite (ADR-0081).
- **Steam and fluids** (`fluid/`) — the Offshore Pump (ADR-0050), the Boiler (ADR-0048), the Steam
  Engine on Oritech's entity (ADR-0077), the Barrel, and Oritech's oil fluids drawn in Factorio's
  colours (ADR-0067).
- **Electricity** (`energy/`) — the poles, their wires and supply areas, the one network that
  carries power (ADR-0036, ADR-0062, ADR-0068); the Solar Panel and Accumulator are Wireworks'.
- **Crafting machines** (`machine/`, `recipes/`) — the three Assembling Machines, the Chemical
  Plant and the Oil Refinery on one chassis and one recipe shape (ADR-0096), and the Held recipe
  EMI sets (ADR-0073).
- **The Radar** (`radar/`) — charting through FTB Chunks and marking the patches it finds (ADR-0079).
- **Building** — placement as a plan the preview draws (`placement/`, ADR-0069), Fast Replace
  (ADR-0082), the Pick's Dismantle and Stretch of Oritech's pipes (`dismantle/`, `stretch/`,
  ADR-0086), and Reach (`reach/`). The mechanisms are Groundworks'; this is the pack's side of them.
- **Trees** (`felling/`, `PFTrees`) — felling a tree whole (ADR-0051), and two saplings.
- **Terra's world** (`worldgen/`) — the starting area stamped onto world spawn (ADR-0019), the
  `factoryworks:ground` processor, the outfield and oil-field structures, and vanilla spawning
  turned off (ADR-0093).
- **The starting kit** (`start/`) — granted once per player.
- **Glue** — `compat/` (Jade tooltips, EMI's recipe categories and Fill Recipe, Researchd's machine
  locks), `network/`, `transfer/`'s guarded item and fluid faces, and the Minecraft and Oritech
  mixins.
- **`gametest/`** — the pack's GameTests (below).
- **Not the Personal Assembler.** It is Craftworks, a local jar (ADR-0089). The pack ships only
  its recipes, the `hand/` copies `scripts/build-hand-recipes.py` writes, and `config/craftworks-server.toml`.

Nothing a designer would tune is compiled in. A machine's figures are read from resources under
`factoryworks_core/` that the repo's generators write from the Factorio corpus, and models,
textures, display names and recipes are pack data. The jar's own lang file holds only the strings a
Java class passes to `Component.translatable` with no pack-side author to own them. Which research
locks which recipe is `kubejs/server_scripts/researchd.js`; this mod only asks Researchd.

Note the two names, which are deliberately different:

| | |
| --- | --- |
| Mod id | `factoryworks_core` |
| Registry namespace | `factoryworks` — shared with KubeJS |

So `factoryworks:yumako_sapling` is this mod's, and `factoryworks:yumako_leaves` is
`kubejs/startup_scripts/blocks.js`'s. Registering the same id twice is a startup crash whose
message will not mention either file, so read the ownership table in ADR-0015 before adding a block.

## Building

From the **repo root**, not from `mod/`:

```sh
./gradlew :factoryworks_core:installToPack
```

That builds the jar and copies it into `mods/`, which is the whole install step. `mods/` is
gitignored, so this has to be run once on any machine that intends to launch the pack — including
after a fresh clone, where the pack will otherwise start with two saplings missing and every
`factoryworks:*_sapling` reference failing to resolve.

`./gradlew :factoryworks_core:build` builds without installing. The first run downloads and
decompiles Minecraft and takes a few minutes; later runs are seconds.

Requires **JDK 25** and **ModDevGradle 2.0.147** (the plugin version in `mod/build.gradle`; 2.0.107
carries no NeoForm runtime for 26.1 and fails before javac with `Function for step preProcessJar has
invalid tool: null`). 25 is not a preference: Minecraft 26.1.2 and every 26.1 mod jar in `mods/` are
compiled to class file version 69.

On this repo's macOS setup the JDK is Homebrew's and the usual macOS locations are empty, so Gradle
has to be told where it is. Registering it once is better than prefixing every command, because the
toolchain is resolved per build rather than from `JAVA_HOME`:

```sh
brew install openjdk@25
echo 'org.gradle.java.installations.paths=/opt/homebrew/opt/openjdk@25' >> ~/.gradle/gradle.properties
```

`./gradlew -q javaToolchains` lists what Gradle can see. Optionally, to put it on `java_home` as
well: `sudo ln -sfn /opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-25.jdk`.

## Version pinning

`gradle.properties` pins Minecraft `26.1.2` and NeoForge `26.1.2.109` to match the pack. Both are
also written into the mod's dependency ranges, so a mismatched jar refuses to load rather than
crashing obscurely. When the pack's NeoForge build moves, move `neoforge_version` with it.

There is no Parchment block. ParchmentMC has published no mappings for 26.1 -- its newest data is
`parchment-1.21.9` -- so naming one fails the build rather than silently falling back. Restore the
block in `mod/build.gradle`, and the two `parchment_*` keys here, when a 26.1 release exists.

## Tests

```
./gradlew :factoryworks_core:test
```

JUnit 5, run on a plain JVM. This is the pack's "this pack logic computes something" row in
[what to check](../docs/testing/what-to-check.md): each package's rules and arithmetic — a machine's
rate and stall, a pole network, an ore amount, a tree's shape — are written free of any Minecraft
type and tested under `src/test/`, so the check needs no game. Registration — blocks, items, trees
— has nothing to assert that the game does not assert louder at startup, and gets no test.

**The test source set is deliberately absent from `neoForge.mods` in `build.gradle`**, so Minecraft
is not on its classpath. That is what keeps the split honest: logic that drifts into needing a
`Level` stops compiling in the test source set rather than quietly becoming untestable.

The subproject also carries a headless NeoForge GameTest run — `./gradlew
:factoryworks_core:runGameTestServer` from the repo root. The tests live in `core/gametest/`, in the **main**
source set: a GameTest is code the running game loads, so it cannot live in the Minecraft-free
test source set described above. What is there is only what a JVM test cannot reach; see
`docs/testing/what-to-check.md`.
