# Manufactio 2 - Nuclear Edition: the sequel, compared

Researched 2026-09-27 for a comparison with FactoryWorks, as a companion to
`docs/research/manufactio.md` (the original pack). Where a claim comes from inside the modpack zip,
the citation is a path inside `Manufactio NE-1.01.zip` (CurseForge file 4405041, downloaded from
`https://mediafilez.forgecdn.net/files/4405/41/Manufactio%20NE-1.01.zip`). The pack's overrides sit
under `overrides/`, so `scripts/3_research/000.zs` means `overrides/scripts/3_research/000.zs`, and
`config/...` means `overrides/config/...`. Quest chapters live in
`config/ftbquests/normal/chapters/<id>/`; "quest `X`" means the FTB Quests entry titled `X` there.
Our verdicts are quoted from `docs/factorio-mechanics.md` as of this date. **Inferred** marks
anything not stated by a source.

## 1. Overview

| field | value | source |
| --- | --- | --- |
| Name | Manufactio 2 - Nuclear Edition (`"name": "Manufactio NE"` in the manifest) | https://www.curseforge.com/minecraft/modpacks/manufactio-2-nuclear-edition, `manifest.json` |
| Author | Golrith (CurseForge owner); `"author": "Golrith"` in the manifest | https://api.cfwidget.com/minecraft/modpacks/manufactio-2-nuclear-edition, `manifest.json` |
| CurseForge project ID | 500287 | cfwidget |
| Minecraft / loader | 1.12.2, Forge 14.23.5.2860 | `manifest.json` (`forge-14.23.5.2860`) |
| Project created | 2021-07-05 | cfwidget `created_at` |
| Latest version | 1.01 (`Manufactio NE-1.01.zip`, 2023-02-20) | cfwidget; https://www.curseforge.com/minecraft/modpacks/manufactio-2-nuclear-edition/files/all |
| Mods in the manifest | 153 | `manifest.json` (153 `files`) |
| Categories | Tech, Quests | cfwidget |
| License | All Rights Reserved | CurseForge project page (via WebFetch) |
| Summary | "Research & Tech Tree based progression pack inspired by modded Factorio" | cfwidget `summary` |

**Description summary** (CurseForge description via cfwidget). The page opens with a list of
questions: more machines, bigger machines, deeper recipes, fewer invasions, more horrifying monsters,
being stranded again on an alien planet, being irradiated. It says the pack keeps the original's
tech tree and research system but is "inspired by Modded Factorio". It calls itself a WIP beta with
frequent updates and links a Discord. It ships an optional resource pack, a modified Odyssey 1.12
used with permission, which needs OptiFine. Servers should set `level-type=RTG`.

The earliest file the CurseForge files tab still lists is 0.08 (2021-08-12); the project was
created five weeks earlier, so versions before 0.08 are not public there
(https://www.curseforge.com/minecraft/modpacks/manufactio-2-nuclear-edition/files/all). 1.0 came
out on 2023-01-21 and 1.01 a month later; there has been no upload since.

**Story.** Quest `Story` (chapter Background): having escaped the previous planet in a starship,
the player is stranded again after an emergency teleport to "this habitable moon orbiting a gas
giant", near a black hole. `config/advRocketry/PlanetDefs.xml` matches it: the overworld (DIM 0) is
the moon "Solitude" of the gas giant "Gigantica", in a system with a black-hole star "Limbo". Its
siblings are Ardethe, Niflheim, Vulcan, Theron, Hellus and an Asteroid Belt dimension.

**How the pack is built.** As with the original, there is no Manufactio mod. Everything is stitched
together from other mods plus scripts, but the stitching changed:

- **Research moved from Better Questing + Game Stages to FTB Quests + "Plans".** Game Stages, Item
  Stages, Mob Stages, MultiBlock Stages and Better Questing are all gone from the manifest. A research
  quest consumes research items and rewards a **Plan** item, for example quest `Modules [A19]`: 100
  `contenttweaker:research_green` in, `contenttweaker:plana19` out
  (`config/ftbquests/normal/chapters/0d6146f3/b7f7cf9e.snbt`). Recipes then take the Plan as a
  non-consumed ingredient (`setChance(0.00)` on a Modular Machinery recipe, `"Count": -1` in a
  Project Table recipe; e.g. `scripts/3_research/a19_modules.zs`, `config/projectTable/000_furnace.json`).
  Plans copy with paper (`recipes.addShapeless(<contenttweaker:plana19>*2,[<contenttweaker:plana19>,<minecraft:paper>])`).
  The quest `Plans` spells out the second use: "Plans can also be used in various machines to set
  the correct crafting mode so machines do not accidently craft the wrong items."
- **CraftTweaker scripts**: 257 files under `scripts/` (247 `.zs`), grouped into `1_removals/`,
  `2_processing/`, `3_research/` (one file per research code), `contenttweaker/`,
  `nc_script_addons/` and `x_environment/`. Modular Machinery recipes: 645 `RecipeBuilder` calls in
  the scripts plus 471 JSON files under `config/modularmachinery/recipes/`.
- **FTB Quests**: 13 chapters, 454 quest files (`config/ftbquests/normal/chapters/`). Research
  chapters are Factory Basics, Automation, Processing, Logistics, Power, Military, Science, Organic,
  Nuclear, Space and Challenges; plus Background (lore and rules) and a 129-entry Library that hands
  back any unlocked Plan (quest `Plan Library`: "Lost a plan? ... All you need is paper").
- **Modular Machinery**: 76 machine definitions (`config/modularmachinery/machinery/`), roughly
  double the original's 26. Most come in four tiers (for example `assembler1`–`assembler4`,
  `chemical_plant`–`chemical_plant4`, `ore_washer`–`ore_washer4`).
- **Project Table** (mod 325309): the hand-crafting bench, 186 recipe files in `config/projectTable/`,
  most of them gated by a Plan.

## 2. Full mod list

All 153 entries in `manifest.json`. `manifest.json` gives only `projectID`/`fileID`; each project's
title, CurseForge URL and the file name for that `fileID` come from `https://api.cfwidget.com/<projectID>`.
The names match `modlist.html`. For mods the pack's own files use in a specific way, the "Role"
column gives that use and cites the file. For every other mod, the column quotes the mod's own
CurseForge summary and makes no claim about how the pack uses it. The mod whose CurseForge title is
"Additional Enchanted Miner" registers the `quarryplus:` items the pack renames into drills (inferred
from `config/quarryplus/` and the item IDs in `scripts/3_research/000.zs`).

| # | Mod (CurseForge project ID) | File in manifest | Role |
| --- | --- | --- | --- |
| 1 | [Abyssal Depths](https://www.curseforge.com/minecraft/mc-mods/abyssal-depths) (360647) | `AbyssalDepths-1.12.2-0.3.2.jar` | CF summary: Beware the abyss |
| 2 | [Actually Additions](https://www.curseforge.com/minecraft/mc-mods/actually-additions) (228404) | `ActuallyAdditions-1.12.2-r152.jar` | CF summary: Fan-favorite magitech mod full of fun and useful gadgets |
| 3 | [Additional Enchanted Miner](https://www.curseforge.com/minecraft/mc-mods/additional-enchanted-miner) (282837) | `AdditionalEnchantedMiner-1.12.2-12.5.4.jar` | **Pack role:** QuarryPlus items: `quarryplus:solidquarry` renamed Burner Drill, `quarryplus:quarry` renamed Powered Drill (`scripts/3_research/000.zs`, `a12_mining_drill.zs`) |
| 4 | [Advanced Rocketry](https://www.curseforge.com/minecraft/mc-mods/advanced-rocketry) (236542) | `AdvancedRocketry-1.12.2-1.7.0-240-universal.jar` | **Pack role:** spacesuits, rocket travel to the moons of the gas giant Gigantica (`config/advRocketry/PlanetDefs.xml`, Space chapter) |
| 5 | [AI Improvements: Performance Tuning](https://www.curseforge.com/minecraft/mc-mods/ai-improvements) (233019) | `AIImprovements-1.12-0.0.1b3.jar` | CF summary: Performance and logic upgrades for mod AIs |
| 6 | [AmbientSounds 6](https://www.curseforge.com/minecraft/mc-mods/ambientsounds) (254284) | `AmbientSounds_v3.1.7_mc1.12.2.jar` | CF summary: #listentonature |
| 7 | [Apoca-Mobs](https://www.curseforge.com/minecraft/mc-mods/apoca-mobs) (538703) | `apoca-mobs-version1-1.12.2.jar` | **Pack role:** radiation mobs (bloatfly, cockroach, radscorpion, feral ghoul) on irradiated ground (`config/incontrol/spawn.json`) |
| 8 | [AppleSkin](https://www.curseforge.com/minecraft/mc-mods/appleskin) (248787) | `AppleSkin-mc1.12-1.0.14.jar` | CF summary: Adds some useful information about food/hunger to the HUD |
| 9 | [ArchitectureCraft - TridentMC Version](https://www.curseforge.com/minecraft/mc-mods/architecturecraft-tridev) (277631) | `architecturecraft-1.12-3.108.jar` | CF summary: Distinguished architectural features for your Minecraft buildings. Ported to 1.12 |
| 10 | [Aroma1997Core](https://www.curseforge.com/minecraft/mc-mods/aroma1997core) (223735) | `Aroma1997Core-1.12.2-2.0.0.2.b167.jar` | CF summary: This is a mod required by most of my other mods, so you should install it, if you want to run any other of my mods. |
| 11 | [AromaBackup](https://www.curseforge.com/minecraft/mc-mods/aromabackup) (225658) | `AromaBackup-1.12.2-3.0.0.0.b135.jar` | CF summary: IT DOES BACKUPS!!! (Works in SSP and SMP) |
| 12 | [Athenaeum](https://www.curseforge.com/minecraft/mc-mods/athenaeum) (284350) | `athenaeum-1.12.2-1.19.5.jar` | CF summary: This is my Minecraft mod library. There are many like it, but this one is mine. |
| 13 | [AttributeFix](https://www.curseforge.com/minecraft/mc-mods/attributefix) (280510) | `AttributeFix-1.12.2-1.0.4.jar` | CF summary: Removes arbitrary limits on Minecraft's attribute system. |
| 14 | [AutoRegLib](https://www.curseforge.com/minecraft/mc-mods/autoreglib) (250363) | `AutoRegLib-1.3-32.jar` | CF summary: A library to ease menial tasks in mod development. |
| 15 | [B.A.S.E](https://www.curseforge.com/minecraft/mc-mods/base) (246996) | `base-1.12.2-3.14.0.jar` | CF summary: Main Library for The Acronym Coders Mod Projects |
| 16 | [Baubles](https://www.curseforge.com/minecraft/mc-mods/baubles) (227083) | `Baubles-1.12-1.5.2.jar` | CF summary: An addon module and API for Thaumcraft |
| 17 | [Better Boilers](https://www.curseforge.com/minecraft/mc-mods/better-boilers) (284383) | `BetterBoilers-1.2.jar` | **Pack role:** Basic Boiler multiblock, delivered as a Package (`scripts/3_research/000.zs`) |
| 18 | [BetterFps](https://www.curseforge.com/minecraft/mc-mods/betterfps) (229876) | `BetterFps-1.4.8.jar` | CF summary: Performance Improvements |
| 19 | [Biomes O' Plenty](https://www.curseforge.com/minecraft/mc-mods/biomes-o-plenty) (220318) | `BiomesOPlenty-1.12.2-7.0.1.2445-universal.jar` | CF summary: Adds 50+ unique biomes to enhance your world, with new trees, flowers, and more! |
| 20 | [Born in a Barn](https://www.curseforge.com/minecraft/mc-mods/born-in-a-barn) (285789) | `Born In A Barn V1.8-1.12-1.1.jar` | CF summary: Fixed a Village ChunkLoading Issue |
| 21 | [Bouncy Creepers](https://www.curseforge.com/minecraft/mc-mods/bouncy-creepers) (295271) | `BouncyCreepers-0.0.4.jar` | CF summary: Adds Bouncy Creepers |
| 22 | [bspkrsCore Updated](https://www.curseforge.com/minecraft/mc-mods/bspkrscore-updated) (323993) | `[1.12]bspkrsCore-universal-7.6.0.1.jar` | CF summary: This is an updated bspkrsCore mod for newer versions of MC (1.8.9 - 1.12.2) |
| 23 | [Building Gadgets](https://www.curseforge.com/minecraft/mc-mods/building-gadgets) (298187) | `BuildingGadgets-2.8.4.jar` | CF summary: A collection of Gadgets to make building large structures a little bit easier! |
| 24 | [Capsule](https://www.curseforge.com/minecraft/mc-mods/capsule) (235338) | `Capsule-1.12.2-3.4.76.jar` | **Pack role:** every multiblock is a Package that unpacks into a chest plus a capsule that places it (quest `Creating Power - Steam`) |
| 25 | [CB Multipart](https://www.curseforge.com/minecraft/mc-mods/cb-multipart) (258426) | `ForgeMultipart-1.12.2-2.6.2.83-universal.jar` | CF summary: An opensource library for having multiple things in the one block space |
| 26 | [Chisel](https://www.curseforge.com/minecraft/mc-mods/chisel) (235279) | `Chisel-MC1.12.2-1.0.2.45.jar` | CF summary: A builder's best friend. |
| 27 | [Clumps](https://www.curseforge.com/minecraft/mc-mods/clumps) (256717) | `Clumps-3.1.2.jar` | CF summary: Clumps XP orbs together to reduce lag |
| 28 | [CodeChicken Lib 1.8.+](https://www.curseforge.com/minecraft/mc-mods/codechicken-lib-1-8) (242818) | `CodeChickenLib-1.12.2-3.2.3.358-universal.jar` | CF summary: Contains various tools to make modding easier. |
| 29 | [CoFH Core](https://www.curseforge.com/minecraft/mc-mods/cofh-core) (69162) | `CoFHCore-1.12.2-4.6.6.1-universal.jar` | CF summary: Contains Core Functionality for all Team CoFH mods. Also does some really cool stuff on its own! |
| 30 | [CoFH World](https://www.curseforge.com/minecraft/mc-mods/cofh-world) (271384) | `CoFHWorld-1.12.2-1.4.0.1-universal.jar` | **Pack role:** ore generation replaced: surface ore spikes and infinite ore clusters (`config/cofh/world/`, `ReplaceStandardGeneration=true`) |
| 31 | [Collective](https://www.curseforge.com/minecraft/mc-mods/collective) (342584) | `collective-1.12.2-3.0.jar` | CF summary: 🎓 Collective is a shared library with common code for all of Serilum's mods. |
| 32 | [ConnectedTexturesMod](https://www.curseforge.com/minecraft/mc-mods/ctm) (267602) | `CTM-MC1.12.2-1.0.2.31.jar` | CF summary: A resource pack extension library |
| 33 | [ContentTweaker](https://www.curseforge.com/minecraft/mc-mods/contenttweaker) (237065) | `ContentTweaker-1.12.2-4.10.0.jar` | **Pack role:** science packs, research items, Plans, modules, circuits (`scripts/contenttweaker/items.zs`) |
| 34 | [Controlling](https://www.curseforge.com/minecraft/mc-mods/controlling) (250398) | `Controlling-3.0.10.jar` | CF summary: Adds a search bar to the Key-Bindings menu |
| 35 | [Corpse](https://www.curseforge.com/minecraft/mc-mods/corpse) (316582) | `corpse-1.12.2-1.0.8.jar` | CF summary: Never lose your items again! |
| 36 | [CraftTweaker](https://www.curseforge.com/minecraft/mc-mods/crafttweaker) (239197) | `CraftTweaker2-1.12-4.1.20.685.jar` | **Pack role:** all recipe scripts (`overrides/scripts/`, 190+ files) |
| 37 | [CreativeCore](https://www.curseforge.com/minecraft/mc-mods/creativecore) (257814) | `CreativeCore_v1.10.70_mc1.12.2.jar` | CF summary: a core mod |
| 38 | [Custom Main Menu](https://www.curseforge.com/minecraft/mc-mods/custom-main-menu) (226406) | `CustomMainMenu-MC1.12.2-2.0.9.1.jar` | CF summary: Allows you to modify the main menu using a simple json file |
| 39 | [Derelict](https://www.curseforge.com/minecraft/mc-mods/derelict) (494881) | `Derelict 1.3 Junk Update.jar` | CF summary: Props for Derelict Server |
| 40 | [Diet Hoppers](https://www.curseforge.com/minecraft/mc-mods/diet-hoppers) (278385) | `diethopper-1.1.jar` | CF summary: Tightens the selection box of the hopper to allow you to access visible blocks behind them. |
| 41 | [Dropt](https://www.curseforge.com/minecraft/mc-mods/dropt) (284973) | `dropt-1.12.2-1.19.3.jar` | CF summary: Modify block drops based on player, gamestage, dimension, biome, held item, y-level, items dropped, fortune level, silkt |
| 42 | [Ender Storage 1.8.+](https://www.curseforge.com/minecraft/mc-mods/ender-storage-1-8) (245174) | `EnderStorage-1.12.2-2.4.6.137-universal.jar` | CF summary: linked color coded ender chests |
| 43 | [Ender-Rift](https://www.curseforge.com/minecraft/mc-mods/ender-rift) (233780) | `EnderRift-1.12.2-2.1.10.jar` | **Pack role:** Singularity Infinite Storage (quest `[L31]`) |
| 44 | [Engineer's Decor](https://www.curseforge.com/minecraft/mc-mods/engineers-decor) (313866) | `engineersdecor-1.12.2-1.1.5.jar` | CF summary: Adds cosmetic blocks for the engineer's factory, workshop, and home. |
| 45 | [Farseek](https://www.curseforge.com/minecraft/mc-mods/farseek) (229708) | `Farseek-1.12-2.5.1.jar` | CF summary: A Scala API for Minecraft mods. |
| 46 | [Fast Leaf Decay](https://www.curseforge.com/minecraft/mc-mods/fast-leaf-decay) (230976) | `FastLeafDecay-v14.jar` | CF summary: Makes leaves decay faster when a tree is cut down. |
| 47 | [Fish's Undead Rising](https://www.curseforge.com/minecraft/mc-mods/fishs-undead-rising) (321139) | `Fish's Undead Rising-1.3.1.jar` | CF summary: Fill your world with all kinds of mobs, undead-related or not. |
| 48 | [Foam​Fix](https://www.curseforge.com/minecraft/mc-mods/foamfix-optimization-mod) (278494) | `foamfix-0.10.15-1.12.2.jar` | CF summary: Simple, targetted optimizations for a popular block game |
| 49 | [Forestry](https://www.curseforge.com/minecraft/mc-mods/forestry) (59751) | `forestry_1.12.2-5.8.2.422.jar` | CF summary: Bringing bees, butterflies and more trees. as well as eco-friendly energy production and mail to Minecraft. |
| 50 | [FTB Library (Forge) (Legacy)](https://www.curseforge.com/minecraft/mc-mods/ftb-library-legacy-forge) (237167) | `FTBLib-5.4.7.2.jar` | CF summary: FTB Library is a library mod that is used for some of our mods. |
| 51 | [FTB Quests (NeoForge)](https://www.curseforge.com/minecraft/mc-mods/ftb-quests-forge) (289412) | `FTBQuests-1202.9.0.15.jar` | **Pack role:** research tree: quests consume research items and reward Plans (`config/ftbquests/normal/`) |
| 52 | [FTB Utilities (Forge)](https://www.curseforge.com/minecraft/mc-mods/ftb-utilities-forge) (237102) | `FTBUtilities-5.4.1.131.jar` | CF summary: FTB Utilities is a mod by FTB. It aims to provide several useful utilities within the FTB Platform ranging from friends  |
| 53 | [Funky Locomotion](https://www.curseforge.com/minecraft/mc-mods/funky-locomotion) (224190) | `funky-locomotion-1.12.2-1.1.2.jar` | CF summary: Every block in the groove |
| 54 | [Gauges and Switches](https://www.curseforge.com/minecraft/mc-mods/redstone-gauges-and-switches) (296686) | `rsgauges-1.12.2-1.2.8.jar` | CF summary: Adds buttons, levers, plates, timers, detectors, sensors, gauges and lamps. Configurable and wirelessly linkable. |
| 55 | [HammerLib](https://www.curseforge.com/minecraft/mc-mods/hammer-lib) (247401) | `HammerLib-1.12.2-2.0.6.32.jar` | CF summary: Library used by all of DragonForge team's mods. |
| 56 | [Hooked](https://www.curseforge.com/minecraft/mc-mods/hooked) (297209) | `hooked-1.0.3.jar` | CF summary: Grappling hooks done simple, natural, and useful. |
| 57 | [Hwyla](https://www.curseforge.com/minecraft/mc-mods/hwyla) (253449) | `Hwyla-1.8.26-B41_1.12.2.jar` | CF summary: Hwyla (Here's What You're Looking At) is a UI improvement mod aimed at providing block information directly in-game. It  |
| 58 | [Hydrophobia](https://www.curseforge.com/minecraft/mc-mods/hydrophobia) (273084) | `hydrophobia-1.0.4c.jar` | CF summary: Don't let it rain on your parade... |
| 59 | [iChunUtil](https://www.curseforge.com/minecraft/mc-mods/ichunutil) (229060) | `iChunUtil-1.12.2-7.2.2.jar` | CF summary: Shared library used by iChun's mods |
| 60 | [Immersive Engineering](https://www.curseforge.com/minecraft/mc-mods/immersive-engineering) (231951) | `ImmersiveEngineering-0.12-98.jar` | **Pack role:** Improved Blast Furnace, wires, lanterns/floodlights (quests `Blast Furnaces [P07]`, `Optics [S01]`) |
| 61 | [Immersive Posts](https://www.curseforge.com/minecraft/mc-mods/immersiveposts) (314645) | `ImmersivePosts-0.2.1.jar` | CF summary: Extendable treated-wood, aluminium, steel posts and more for ImmersiveEngineering. |
| 62 | [Impractical Storage](https://www.curseforge.com/minecraft/mc-mods/impractical-storage) (263456) | `ImpracticalStorage-1.5.0-MC1.12.2.jar` | CF summary: Because really, who can fit 1728 gold blocks in a chest!? |
| 63 | [In Control!](https://www.curseforge.com/minecraft/mc-mods/in-control) (257356) | `incontrol-1.12-3.9.18.jar` | **Pack role:** spawn rules: no hostiles on concrete or where they see the sky; mutant mobs only on irradiated ground (`config/incontrol/spawn.json`) |
| 64 | [Industrial Renewal](https://www.curseforge.com/minecraft/mc-mods/industrial-renewal) (299849) | `IndustrialRenewal_1.12.2-0.21.8.jar` | **Pack role:** Bulk Conveyor, LV cable, fluid pipe, pump, rails, cargo and fluid wagons, Advanced Mining Drill (`scripts/3_research/000.zs`, `l11_rails.zs`, `a12_mining_drill.zs`) |
| 65 | [Insomniac](https://www.curseforge.com/minecraft/mc-mods/insomniac) (270193) | `insomniac-1.6.jar` | CF summary: Disables sleeping, but you can configure if beds still sets your spawnpoint or not. |
| 66 | [Inventory Tweaks [1.12 only]](https://www.curseforge.com/minecraft/mc-mods/inventory-tweaks) (223094) | `InventoryTweaks-1.63.jar` | CF summary: Tweaks to inventory handling for ease of use, including sorting and automatic replacement of broken tools or exhausted s |
| 67 | [Iron Chests](https://www.curseforge.com/minecraft/mc-mods/iron-chests) (228756) | `ironchest-1.12.2-7.0.72.847.jar` | CF summary: Iron Chest mod |
| 68 | [Item Filters](https://www.curseforge.com/minecraft/mc-mods/item-filters) (309674) | `ItemFilters-1.0.4.2.jar` | CF summary: Item Filters is a library mod that is used by mods like FTB Quests. It adds several filter items that let you precisely  |
| 69 | [IvToolkit](https://www.curseforge.com/minecraft/mc-mods/ivtoolkit) (224535) | `IvToolkit-1.3.3-1.12.jar` | CF summary: Versatile mod framework |
| 70 | [JourneyMap](https://www.curseforge.com/minecraft/mc-mods/journeymap) (32274) | `journeymap-1.12.2-5.7.1.jar` | CF summary: Real-time map used for mapping in-game or your browser as you explore. |
| 71 | [Just Enough Items (JEI)](https://www.curseforge.com/minecraft/mc-mods/jei) (238222) | `jei_1.12.2-4.16.1.1001.jar` | CF summary: View Items and Recipes |
| 72 | [LagGoggles](https://www.curseforge.com/minecraft/mc-mods/laggoggles) (283525) | `LagGoggles-1.12.2-5.8-132.jar` | CF summary: A minecraft forge mod which profiles and visualises lag in the world. |
| 73 | [LibrarianLib](https://www.curseforge.com/minecraft/mc-mods/librarianlib) (252910) | `librarianlib-1.12.2-4.22.jar` | CF summary: An extensive collection of tools, utilities, and frameworks. |
| 74 | [LibVulpes](https://www.curseforge.com/minecraft/mc-mods/libvulpes) (236541) | `LibVulpes-1.12.2-0.4.2-75-universal.jar` | CF summary: Common Library required for my mods |
| 75 | [Light Level Overlay Reloaded](https://www.curseforge.com/minecraft/mc-mods/light-level-overlay-reloaded) (226670) | `LLOverlayReloaded-1.1.6-mc1.12.2.jar` | CF summary: A mod visualizes the light level on top of blocks. |
| 76 | [Logistics Pipes](https://www.curseforge.com/minecraft/mc-mods/logistics-pipes) (232838) | `logisticspipes-0.10.4.35.jar` | **Pack role:** requester/provider pipe networks (quests `[L10]`, `[L20]`) |
| 77 | [LootTableTweaker](https://www.curseforge.com/minecraft/mc-mods/loottabletweaker) (261339) | `LootTableTweaker-1.12.2-1.1.14.jar` | CF summary: LootTable support for CraftTweaker / MineTweaker3 |
| 78 | [MAGE (Graphical Tweaks)](https://www.curseforge.com/minecraft/mc-mods/mage) (287471) | `MAGE-0.2.4.jar` | CF summary: Mildly Advanced Graphics Extensions |
| 79 | [Magma Monsters](https://www.curseforge.com/minecraft/mc-mods/magma-monsters) (262292) | `MagmaMonsters-0.3.0.jar` | CF summary: Adds Magma Monsters to the world. |
| 80 | [Magneticraft](https://www.curseforge.com/minecraft/mc-mods/magneticraft) (224808) | `Magneticraft_1.12-2.8.3-dev.jar` | **Pack role:** the Inserter and its speed/capacity upgrades; Small Steam Engine; Tesla Tower wireless power (`scripts/3_research/000.zs`, `l04_*`, `l05_*`, `r16_tesla_towers.zs`) |
| 81 | [Mantle](https://www.curseforge.com/minecraft/mc-mods/mantle) (74924) | `Mantle-1.12-1.3.3.55.jar` | CF summary: Shared code for Forge mods |
| 82 | [McJtyLib](https://www.curseforge.com/minecraft/mc-mods/mcjtylib) (233105) | `mcjtylib-1.12-3.5.4.jar` | CF summary: Companion mod for all mods by McJty (RFTools, XNet, Deep Resonance, Not Enough Wands, SignTastic, ...)) |
| 83 | [MixinBooter](https://www.curseforge.com/minecraft/mc-mods/mixin-booter) (419286) | `!mixinbooter-7.0.jar` | CF summary: The Mixin Library for 1.8 - 1.12.2. |
| 84 | [Mob Spawner Control](https://www.curseforge.com/minecraft/mc-mods/mob-spawner-control) (284754) | `SpawnerControl-1.6.3b.jar` | CF summary: Allows modpack makers to customize vanilla mob spawners behaviour. |
| 85 | [ModelLoader](https://www.curseforge.com/minecraft/mc-mods/modelloader) (277663) | `modelloader-1.1.7.jar` | CF summary: A small library to load .mcx model format |
| 86 | [ModTweaker](https://www.curseforge.com/minecraft/mc-mods/modtweaker) (220954) | `modtweaker-4.0.20.11.jar` | CF summary: ModTweaker is an addon for CraftTweaker, a recipe manipulator utility for Minecraft. It allows you to modify the recipes |
| 87 | [Modular Machinery](https://www.curseforge.com/minecraft/mc-mods/modular-machinery) (270790) | `modularmachinery-1.12.2-1.11.1.jar` | **Pack role:** 76 custom multiblocks: four-tier Assembling Machines, Laboratory, Chemical Plant, Distillation Tower, Nuclear Reactor, ore processing, Astro Hub (`config/modularmachinery/machinery/`) |
| 88 | [Mouse Tweaks](https://www.curseforge.com/minecraft/mc-mods/mouse-tweaks) (60089) | `MouseTweaks-2.10.1-mc1.12.2.jar` | CF summary: Enhances inventory management by adding various functions to the mouse buttons. |
| 89 | [MrCrayfish's Vehicle Mod](https://www.curseforge.com/minecraft/mc-mods/mrcrayfishs-vehicle-mod) (286660) | `vehicle-mod-0.44.1-1.12.2.jar` | **Pack role:** Off Roader, Speedboat, Sports Plane, fuelled with Fuelium (quests `[L14]`–`[L16]`) |
| 90 | [MrTJPCore](https://www.curseforge.com/minecraft/mc-mods/mrtjpcore) (229002) | `MrTJPCore-1.12.2-2.1.4.43-universal.jar` | CF summary: Miscellaneous utilities for all my mods |
| 91 | [MTLib](https://www.curseforge.com/minecraft/mc-mods/mtlib) (253211) | `MTLib-3.0.7.jar` | CF summary: Library files for Minetweaker Addons |
| 92 | [Natura](https://www.curseforge.com/minecraft/mc-mods/natura) (74120) | `natura-1.12.2-4.3.2.69.jar` | CF summary: Natura |
| 93 | [No Recipe Book](https://www.curseforge.com/minecraft/mc-mods/no-recipe-book) (284904) | `noRecipeBook_v1.2.2formc1.12.2.jar` | CF summary: Removes the Recipe Book button from the player inventory and crafting table GUI. |
| 94 | [Not Enough Items 1.8.+](https://www.curseforge.com/minecraft/mc-mods/not-enough-items-1-8) (247694) | `NotEnoughItems-1.12.2-2.4.3.245-universal.jar` | CF summary: Recipe Viewer, Inventory Manager, Item Spawner, Cheats and more |
| 95 | [NuclearCraft: Overhauled](https://www.curseforge.com/minecraft/mc-mods/nuclearcraft-overhauled) (336895) | `NuclearCraft-2o.5.5-1.12.2.jar` | **Pack role:** radiation: item radiation levels, chunk radiation that spreads and decays, block mutation, scrubbers, Rad-X/RadAway (`scripts/x_environment/radiation.zs`, `config/nuclearcraft.cfg`) |
| 96 | [Numina](https://www.curseforge.com/minecraft/mc-mods/numina) (235440) | `Numina-1.12.2-1.0.38.jar` | CF summary: Various utilities |
| 97 | [Obfuscate](https://www.curseforge.com/minecraft/mc-mods/obfuscate) (289380) | `obfuscate-0.4.2-1.12.2.jar` | CF summary: A library that adds in useful events and utilities |
| 98 | [OGDragon +](https://www.curseforge.com/minecraft/mc-mods/ogdragon) (303284) | `ogdragon-1.12.2-0.1.4.jar` | CF summary: The original/hybrid pre 1.9 dragon fight |
| 99 | [OMLib](https://www.curseforge.com/minecraft/mc-mods/omlib) (254334) | `omlib-1.12.2-3.1.5-256.jar` | CF summary: Library for Open Modular * Mods |
| 100 | [Open Modular Turrets](https://www.curseforge.com/minecraft/mc-mods/openmodularturrets) (224663) | `openmodularturrets-1.12.2-3.1.14-382.jar` | **Pack role:** turret bases, gun/grenade/rocket/tesla turrets and addons (Military chapter) |
| 101 | [Ore Reeds](https://www.curseforge.com/minecraft/mc-mods/ore-reeds) (302688) | `ore_reeds-1.12.2-1.1.1.jar` | **Pack role:** ore-growing reeds on irradiated soil (quest `Genetic Manipulation [O33]`) |
| 102 | [Ore Visual Detector](https://www.curseforge.com/minecraft/mc-mods/ore-visual-detector) (568579) | `Ore Visual Detector-1.0.1.jar` | CF summary: Add a tool to detect all ores in the world and display them visual friendly. |
| 103 | [OreLib](https://www.curseforge.com/minecraft/mc-mods/orelib) (307806) | `OreLib-1.12.2-3.6.0.1.jar` | CF summary: Support library for OreCruncher's mods |
| 104 | [Pam's Portal Poof](https://www.curseforge.com/minecraft/mc-mods/pams-portal-poof) (278482) | `Pam's Portal Poof 1.12.Xa.jar` | CF summary: Disables the creation of Nether Portals |
| 105 | [Placebo](https://www.curseforge.com/minecraft/mc-mods/placebo) (283644) | `Placebo-1.12.2-1.6.0.jar` | CF summary: A library mod |
| 106 | [Portal Gun](https://www.curseforge.com/minecraft/mc-mods/portal-gun) (229084) | `PortalGun-1.12.2-7.1.0.jar` | CF summary: This mod adds the Portal Gun, as well as several other portal-related aspects, to Minecraft! |
| 107 | [Portality](https://www.curseforge.com/minecraft/mc-mods/portality) (291493) | `portality-1.12.2-1.2.3-15.jar` | **Pack role:** Teleportation Portals (quest `Teleportation Portals [S41]`) |
| 108 | [Project Red - Core](https://www.curseforge.com/minecraft/mc-mods/project-red-core) (228702) | `ProjectRed-1.12.2-4.9.4.120-Base.jar` | CF summary: Core module for the Project Red series |
| 109 | [Project Red - Exploration](https://www.curseforge.com/minecraft/mc-mods/project-red-exploration) (229049) | `ProjectRed-1.12.2-4.9.4.120-world.jar` | CF summary: Exploration module for the Project Red series |
| 110 | [Project Red - Integration](https://www.curseforge.com/minecraft/mc-mods/project-red-integration) (229045) | `ProjectRed-1.12.2-4.9.4.120-integration.jar` | CF summary: Integration module for the Project Red series |
| 111 | [Project Table](https://www.curseforge.com/minecraft/mc-mods/project-table) (325309) | `projecttable-1.12.2-0.1.10.4-universal.jar` | **Pack role:** Project Table: the hand-crafting bench, where recipes need a Plan (`config/projectTable/`, 186 recipes) |
| 112 | [Quark](https://www.curseforge.com/minecraft/mc-mods/quark) (243121) | `Quark-r1.6-179.jar` | CF summary: A Quark is a very small thing. This mod is a collection of small things that improve the vanilla minecraft experience. |
| 113 | [Random Bone Meal Flowers](https://www.curseforge.com/minecraft/mc-mods/random-bone-meal-flowers) (345572) | `rbmf_1.12-1.0.jar` | CF summary: 🎲 Randomizes the flowers spawned by bonemeal, allowing all (modded) types to spawn everywhere. |
| 114 | [Realistic Terrain Generation](https://www.curseforge.com/minecraft/mc-mods/realistic-terrain-generation) (237989) | `RTG-1.12.2-6.1.0.0-snapshot.1.jar` | **Pack role:** world type; the description asks servers to use `level-type=RTG` |
| 115 | [Realistic Torches](https://www.curseforge.com/minecraft/mc-mods/realistic-torches) (235729) | `RealisticTorches-1.12.2-2.1.2.jar` | CF summary: Makes torches burn out after a configurable amount of time. |
| 116 | [ReAuth](https://www.curseforge.com/minecraft/mc-mods/reauth) (237701) | `ReAuth-1.12-Forge-4.0.6.jar` | CF summary: Fixes the Problem of having to restart your Client when your Session invalidates |
| 117 | [Redstone Flux](https://www.curseforge.com/minecraft/mc-mods/redstone-flux) (270789) | `RedstoneFlux-1.12-2.1.1.1-universal.jar` | CF summary: Redstone Flux API - Energy Transfer in Minecraft. |
| 118 | [Refined Storage](https://www.curseforge.com/minecraft/mc-mods/refined-storage) (243076) | `refinedstorage-1.6.16.jar` | **Pack role:** digital storage and crafting (quests `[L30]`, `[L37]`, `[L38]`) |
| 119 | [Resource Loader](https://www.curseforge.com/minecraft/mc-mods/resource-loader) (226447) | `ResourceLoader-MC1.12.1-1.5.3.jar` | CF summary: A small mod that allows users to add their own resources to minecraft without making a resource pack |
| 120 | [Ruins (Structure Spawning System)](https://www.curseforge.com/minecraft/mc-mods/ruins-structure-spawning-system) (227873) | `Ruins-1.12.2.jar` | CF summary: A structure spawning system |
| 121 | [Scaling Health](https://www.curseforge.com/minecraft/mc-mods/scaling-health) (248027) | `ScalingHealth-1.12.2-1.3.42+147.jar` | **Pack role:** difficulty that rises with play time, raising mob health and damage (`config/scalinghealth/main.cfg`, quest `Mob Spawning`) |
| 122 | [Scannable](https://www.curseforge.com/minecraft/mc-mods/scannable) (266784) | `Scannable-MC1.12.2-1.6.3.26.jar` | **Pack role:** Scanner for ores and mobs (quest `Scanner [S12]`) |
| 123 | [Scape and Run: Parasites](https://www.curseforge.com/minecraft/mc-mods/scape-and-run-parasites) (348025) | `SRParasites-1.12.2v1.8.2.jar` | **Pack role:** parasites spawning on heavily irradiated ground (`config/incontrol/spawn.json`) |
| 124 | [Shadowfacts' Forgelin](https://www.curseforge.com/minecraft/mc-mods/shadowfacts-forgelin) (248453) | `Forgelin-1.8.4.jar` | CF summary: My fork of Emberwalker's Forgelin, with some sprinkles on top. |
| 125 | [Signals](https://www.curseforge.com/minecraft/mc-mods/signals) (245824) | `Signals-1.12.2-1.4.1-30-universal.jar` | **Pack role:** block signals and station markers for rail logistics (`config/projectTable/l21_*.json`) |
| 126 | [Silent Lib (silentlib)](https://www.curseforge.com/minecraft/mc-mods/silent-lib) (242998) | `SilentLib-1.12.2-3.0.14+168.jar` | CF summary: Library to make writing and maintaining mods easier. |
| 127 | [Simple Ore Samples](https://www.curseforge.com/minecraft/mc-mods/simple-ore-samples) (300090) | `SimpleOreSamples-1.12-2.0.jar` | CF summary: A mod that adds surface sample to ores in the chunk beneath |
| 128 | [Simple Storage Network](https://www.curseforge.com/minecraft/mc-mods/simple-storage-network) (268495) | `SimpleStorageNetwork-1.12.2-1.8.3.jar` | CF summary: A simplified port of the original Storage Network |
| 129 | [Single Spot Chest](https://www.curseforge.com/minecraft/mc-mods/single-spot-chest) (355166) | `SingleSpotChest-1.12.2-1.1.jar` | **Pack role:** one-stack buffer to unload belts (quest `Inserters`) |
| 130 | [Smooth Font](https://www.curseforge.com/minecraft/mc-mods/smooth-font) (285742) | `SmoothFont-mc1.12.2-2.1.4.jar` | CF summary: Draws all font smoothly for better readability and can replace to fonts in your computer. |
| 131 | [Solar Flux Reborn](https://www.curseforge.com/minecraft/mc-mods/solar-flux-reborn) (246974) | `SolarFluxReborn-1.12.2-12.4.11.jar` | CF summary: Adding solar panels into Minecraft. A reborn of Solar Flux mod. |
| 132 | [SpawnTableTweaker](https://www.curseforge.com/minecraft/mc-mods/spawntabletweaker) (316840) | `spawntabletweaker-1.0.jar` | CF summary: Simply crafttweaker interface to manipulate Minecraft's spawn npc tables |
| 133 | [Spiders 2.0](https://www.curseforge.com/minecraft/mc-mods/spiders-2-0) (410497) | `spiders-2.0-1.12.2-1.0.2.jar` | CF summary: A mod that enhances the AI of spiders and their ability to climb. |
| 134 | [Spiders Produce Webs](https://www.curseforge.com/minecraft/mc-mods/spiders-produce-webs) (345298) | `spidersproducewebs_1.12.2-1.3.jar` | CF summary: 🕷 Spiders and cave spiders can periodically produce a cobweb/spiderweb when a player is close. |
| 135 | [Streams](https://www.curseforge.com/minecraft/mc-mods/streams) (229769) | `Streams-1.12-0.4.9.jar` | CF summary: This mod introduces real flowing rivers, with a true current, to your Minecraft worlds. Compatible with many terrain gen |
| 136 | [Surge](https://www.curseforge.com/minecraft/mc-mods/surge) (250290) | `Surge-1.12.2-2.0.79.jar` | CF summary: An open source performance enhancement mod. |
| 137 | [SwingThroughGrass](https://www.curseforge.com/minecraft/mc-mods/swingthroughgrass) (264353) | `stg-1.12.2-1.2.3.jar` | CF summary: Kill mobs and players through grass |
| 138 | [Techguns](https://www.curseforge.com/minecraft/mc-mods/techguns) (244201) | `techguns-1.12.2-2.0.2.0_pre3.2.jar` | **Pack role:** guns, armour, infinite ore clusters and their drills (`scripts/3_research/a22_infinity_mining.zs`, `config/cofh/world/x_infinite_ores.json`) |
| 139 | [Thermal Dynamics](https://www.curseforge.com/minecraft/mc-mods/thermal-dynamics) (227443) | `ThermalDynamics-1.12.2-2.5.6.1-universal.jar` | **Pack role:** LV/MV/HV/UHV energy ducts and fluid ducts (Power chapter) |
| 140 | [Thermal Expansion](https://www.curseforge.com/minecraft/mc-mods/thermal-expansion) (69163) | `ThermalExpansion-1.12.2-5.5.7.1-universal.jar` | CF summary: Expanding Minecraft Thermally! A server-friendly and content-rich blend of magic and technology! |
| 141 | [Thermal Foundation](https://www.curseforge.com/minecraft/mc-mods/thermal-foundation) (222880) | `ThermalFoundation-1.12.2-2.6.7.1-universal.jar` | **Pack role:** materials; its Challenge Reward Coin (`thermalfoundation:coin:72`) buys ore rewards (Challenges chapter) |
| 142 | [TickCentral](https://www.curseforge.com/minecraft/mc-mods/tickcentral) (377201) | `TickCentral-3.2.jar` | CF summary: A future proof coremod for control over in-game ticking |
| 143 | [TidyChunk](https://www.curseforge.com/minecraft/mc-mods/tidychunk) (305460) | `TidyChunk-1.12.2-1.0.0.0.jar` | CF summary: Cleans up a chunk after it has been generated |
| 144 | [Toast Control](https://www.curseforge.com/minecraft/mc-mods/toast-control) (271740) | `Toast Control-1.12.2-1.8.1.jar` | CF summary: Control toasts, those popups in the corner! |
| 145 | [Translocators 1.8.+](https://www.curseforge.com/minecraft/mc-mods/translocators-1-8) (247695) | `Translocators-1.12.2-2.5.2.81-universal.jar` | CF summary: A fancy way to move stuff. |
| 146 | [Treecapitator Updated](https://www.curseforge.com/minecraft/mc-mods/treecapitator-updated) (324007) | `[1.12]TreeCapitator-client-1.43.0.jar` | CF summary: This is an updated Treecapitator mod for newer versions of MC (1.8.9 - 1.12.2) |
| 147 | [VanillaFix](https://www.curseforge.com/minecraft/mc-mods/vanillafix) (292785) | `VanillaFix-1.0.10-150.jar` | CF summary: Keep playing after a crash / Increase your FPS by up to 3x |
| 148 | [Water Control Extreme](https://www.curseforge.com/minecraft/mc-mods/water-control-extreme) (277890) | `WaterControlExtreme-1.0.2.jar` | **Pack role:** water finite except in beach, river and ocean biomes (quest `Finding Water`) |
| 149 | [Weaker Spiderwebs](https://www.curseforge.com/minecraft/mc-mods/weaker-spiderwebs) (345293) | `weakerspiderwebs_1.12.2-2.0.jar` | CF summary: 🕸 Breaks spider webs/cobwebs with a configurable delay when walking through. |
| 150 | [Wireless Redstone CBE](https://www.curseforge.com/minecraft/mc-mods/wireless-redstone-cbe) (260824) | `WR-CBE-1.12.2-2.3.2.33-universal.jar` | CF summary: Transmits Redstone signals wirelessly |
| 151 | [XNet](https://www.curseforge.com/minecraft/mc-mods/xnet) (260912) | `xnet-1.12-1.8.2.jar` | CF summary: XNet is a highly optimized networking cable system for items, energy, fluids, information, ... |
| 152 | [YUNG's Better Caves (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/yungs-better-caves) (340583) | `bettercaves-1.12.2-2.0.4.jar` | CF summary: Overhauled cave generation, underground lakes and rivers, lava oceans, and more! |
| 153 | [YUNG's Better Mineshafts (Forge/NeoForge)](https://www.curseforge.com/minecraft/mc-mods/yungs-better-mineshafts-forge) (389665) | `BetterMineshaftsForge-1.12.2-2.2.1.jar` | CF summary: A long-awaited and much-needed abandoned mineshaft overhaul! |

## 3. Factorio mechanics mapping

Quest names are FTB Quests titles; the code in brackets is the research's Plan code (and the
suffix of its `contenttweaker:plan…` item and its `scripts/3_research/` file).

| Factorio mechanic | How Nuclear Edition does it | Our verdict (`docs/factorio-mechanics.md`) and comparison |
| --- | --- | --- |
| **Research and science packs** | Nine packs are ContentTweaker items: Red, Green ("Transport"), Blue ("Chemical"), Military (Black), Orange ("Organic"), Purple ("Productivity"), Yellow ("Advanced"), Nuclear and White ("Space") (`scripts/3_research/000.zs`; Science chapter). Red is an assembler recipe of one copper gear and one iron gear, Green one Inserter, one Bulk Conveyor and a tin plate (`config/modularmachinery/recipes/assembler/as1_redscience.json`, `as1_greenscience.json`), echoing Factorio's red and green packs. A Modular Machinery **Laboratory** turns packs into research items, and a tier's research needs every lower pack too, as in Factorio: Green research takes Red + Green, Blue takes Red + Green + Blue, and White takes Red + Green + Blue + Yellow + Space (`config/modularmachinery/recipes/laboratory_*.json`, 280 ticks at 60–300 RF/t). Military and Productivity combine with other colours into separate research items (quest `Military Science Packs (aka Black)`). A research quest consumes research items and rewards a Plan (see Overview). | `planned`. **Closer to Factorio than the original**: packs stack per tier as Factorio's labs require, and the red and green recipes copy Factorio's. Research is still a lump of items handed in at a quest, with no lab research rate or queue. The White pack comes from asteroids (`laboratory_space_*.json`: one asteroid chunk → one White pack), not from a rocket. |
| **Technology tree** | Hand-authored in FTB Quests: 232 quest files outside the Library and Challenges chapters take research items as their task (count of files naming `contenttweaker:research_`); some entries are marked `[WIP]` with no task or reward (e.g. `Automation 5 [WIP]`, `Plutonium Nuclear Reactor [WIP]`). | `shipped` for us, extracted from Factorio (ADR-0022). NE's tree is invented, and unfinished at 1.01. |
| **Recipe locking on assemblers** | A Plan sits in the machine as a non-consumed input, and the quest `Plans` says this "set[s] the correct crafting mode so machines do not accidently craft the wrong items". The Assembling Machine 1 quest: "Place in the plan and the correct ingredients, and items will be crafted." | `planned` (Held recipe). **New in NE and a real analogue.** Inferred: the Plan narrows which recipe can match, but Modular Machinery still matches whatever set of inputs arrives, so it is a filter rather than a set recipe with input filtering. |
| **Resource patches and finite ore** | CoFH World replaces standard ore generation (`config/cofh/world/config.cfg`: `ReplaceStandardGeneration=true`). Each ore is a huge `spike` generator with `fractal` distribution, `chunk-chance` 64 (e.g. `config/cofh/world/00_0_iron_ore.json`, iron, `vein-diameter` 40, dimension 0 only): quest `Mining` calls them "large outcrops of ores scattered across the landscape". The ore is ordinary blocks, so finite. Separately, **infinite** ore clusters (`techguns:orecluster`) are placed as surface veins (`config/cofh/world/x_infinite_ores.json`) and mined by Techguns' ore drill multiblock (quest `Infinity Ore Mining [A22]`: "The biggest the cluster of connecting blocks, the more ores that are produced"). Ore Visual Detector, Simple Ore Samples and Scannable help find them. | `adapted`. **NE moved to visible surface deposits, like ours** (ADR-0041, ADR-0045), and away from the original's invisible per-chunk Factory0-Resources amounts. The infinite clusters are a separate late-game tier with no Factorio counterpart. |
| **Mining drills** | Burner Drill = QuarryPlus `solidquarry` renamed; it "consumes coal, and will mine to to bedrock within the frame it builds" (quest `Mining`; `scripts/3_research/000.zs`). The Powered Drill is `quarryplus:quarry` (quest `Electric Drill [P01]`). The Industrial Renewal "Advanced Mining Drill" mines only ores and replaces them with cobblestone, 0.5 s at 170 RF/t, with steel and diamond drill heads (quests `Advanced Mining [A12]`, `Diamond Drill Head [A15]`). Later a Laser Drill (`[A34]`) and quarry markers (`[A33]`). | `adapted`. Burner then electric, as in Factorio. A quarry digging a whole frame to bedrock is not a Factorio drill. |
| **Manual mining** | Vanilla mining plus hand tools: a fuel-tank Mining Drill (`[S16]`), Chainsaw (`[S15]`), Treecapitator. | `adapted`. |
| **Smelting** | Vanilla furnaces (Project Table recipe `config/projectTable/000_furnace.json`, gated by `plan000`), IE's Improved Blast Furnace for steel and refined copper, with up to two 100 RF/t preheaters each (quests `Blast Furnaces [P07]`, `Blast Furnace Preheaters [P14]`), an Induction Furnace feeding molten metal to a Casting Machine (`[P15]`), and a four-tier Modular Machinery Electric Furnace that takes modules (`[P45]`; `machinery/electric_furnace*.json`). | `planned`. Same stone → steel → electric ladder as the original, plus a molten-metal path. |
| **Assembling machines and recipe categories** | Four Assembling Machine tiers: 1 takes 3 ingredients at 100 RF/t, speed 0.5; 2 takes 4 at 140 RF/t, 0.75; 3 at 210 RF/t, 1.25, "modules can be used"; 4 at 310 RF/t, 2.0 (quests `Automation [A01]`, `[A10]`, `[A20]`, `[A30]`). A Package Assembler copies packages (`[A14]`), Fabricators make circuits (`[A32]`), and a Manufacturer crafts a finished item from raw materials given the item as a blueprint (`[A35]`). Other machines act as categories: Chemical Plant, Distillation Tower, Hydrocarbon Separator, Electrolyser, Casting Machine, the ore line and more (`config/modularmachinery/machinery/`). Script recipe counts by machine: `astro_hub` 244, `chemical_plant` 99, `casting_machine` 85, `assembler1` 56, `assembler2` 42, `assembler3` 41 (count of `RecipeBuilder.newBuilder(…, "<machine>", …)` in `scripts/`). | `planned`. Ingredient-count tiers and speed modifiers follow Factorio's assembler 1/2/3. The Manufacturer has no Factorio counterpart. |
| **Handcrafting and the crafting queue** | The Project Table: "a search mode, and a one click craft system using resources in your inventory" (quest `Project Table`). Key components need a Plan there. Vanilla 3x3 crafting also works. No queue. | `planned` (Personal Assembler). NE drops the original's scarcity of crafting surfaces, and has one-click crafting from inventory but no queue. |
| **Transport belts** | Industrial Renewal Bulk Conveyors, loaded and unloaded through hoppers; they back-stuff when blocked (quest `Item Transport`). An Item Router sits inline for splitting (quest `Basic Logistics [L01]`). The IE conveyor recipe is removed (`scripts/3_research/000.zs`). | `adapted` (Beltworks). One belt family, no lanes or tiers. |
| **Inserters** | `magneticraft:inserter`, one item at a time with filters (quest `Inserters`); craftable speed and capacity upgrades, capacity to 8 items (quests `Inserter Speed Upgrade [L04]`, `Inserter Capacity Upgrade [L05]`). | `adapted`. Same as the original. |
| **Electric network and transmission** | LV energy ducts and cables first (quest `LV Energy Ducts`; `scripts/3_research/000.zs`: LV wires 1000 RF/t). Ducts tier up to UHV, and the HV and UHV tiers need Neptunium and Plutonium, so radioactive cables need shielding (quests `[R10]`, `[R20]`, `[R40]`). Flexible wiring converts ducts to long-range wire (`[R01]`, `[R13]`, `[R23]`, `[R43]`). **Tesla Towers** "can power any machines within a large radius of each tower", up to 500 RF/t (quest `Wireless Power [R16]`; Magneticraft `tesla_tower`). Energy laser relays come later (`[R44]`, `[R54]`, `[R64]`). | `adapted`. **The Tesla Tower is an area-supply analogue** of a Factorio pole, absent from the original. |
| **Power generation (steam, boilers, engines)** | Basic Boiler (Better Boilers, delivered as a Package) feeding a Magneticraft Small Steam Engine at 400 RF/t (`scripts/3_research/000.zs`, quests `Creating Power - Steam`, `Assembling the Engine`). Steam Turbine at 1200 RF/t (`[R02]`), Advanced Steam Turbine 2000 RF/t (`[R17]`), Advanced Boilers needing purified water (`[R03]`). Solar in sets of nine panels from 126 to 2052 RF/t (`[R09]`–`[R79]`), wind 100 RF/t, geothermal 500 RF/t over a 5x5x3 lava pool, and a Uranium Decay Generator (`[R14]`, `[R27]`, `[R28]`). Batteries and battery banks (`[R11]`, `[R12]`, `[R21]`, `[R22]`). | `planned`. Boiler → engine → turbine, with Factorio's accumulator and solar analogues. Figures are not Factorio's. |
| **Water as a resource** | Water Control Extreme: "Only water in Beach, River or Ocean biomes is infinite. In other biomes it's finite." A pump consumes source blocks within 4 blocks (quest `Finding Water`). Water Bores make water from nothing, 3 mB per face early and 60 mB/s later (quests `A dry situation`, `Water Treatment [P08]`); an Industrial Water Pump over a 5x5x2 pool gives 2000 mB/t (`[P28]`). | `adapted` (ADR-0050: extracted, never created). NE ties infinite water to shoreline biomes, which is close to Factorio's offshore siting, but its bores create water. |
| **Fluid handling** | Industrial Renewal pipes and pump, Thermal fluiducts, hardened fluiducts for hot fluids (`[L17]`), portable tanks, 250-bucket stackable tanks (`[L06]`), steel barrels for oil and waters (`scripts/3_research/l10_tubes.zs` display names). | `planned`. Barrelling is Factorio-like. |
| **Oil processing** | Crude oil generates as spikes of `magneticraft:oil_source` blocks (`config/cofh/world/00_6_crude_oil.json`). A pumpjack package must sit on "a 5x5x3 foundation of Crude Oil blocks"; Gas Wells tap geysers; a Hydrocarbon Separator comes first (quest `Gas and Oil Extraction [P12]`). A Distillation Tower splits hot crude into naphtha, light oil, heavy oil and residues (`[P24]`). Then steam cracking, oil cracking with catalysts, lubricant, plastics, liquid plastics, solid fuel, sulfur, explosives, coolants (`[P31]`, `[P32]`, `[P41]`, `[P42]`, `[P44]`, `[P47]`, `[P48]`, `[P49]`, `[P51]`, `[P52]`, `[P61]`, `[P62]`). | `planned`. A deeper chain than the original, modelled on modded Factorio (inferred from the description's "inspired by Modded Factorio"). Oil is finite source blocks, not Factorio's infinite yield. |
| **Pollution** | **Replaced by radiation.** NuclearCraft: Overhauled's chunk radiation is enabled (`config/nuclearcraft.cfg`: `radiation_enabled=true`, `radiation_spread_rate=0.1`, `radiation_decay_rate=0.001`, `radiation_chunk_limit=-1.0`). 223 `setRadiationLevel` calls make ordinary items radioactive, redstone and even science packs among them (`scripts/x_environment/radiation.zs`, e.g. `<minecraft:redstone>` 9.6e-11 Rad/t). 80 `RadiationBlockMutation` recipes turn grass and dirt into dead grass, coarse dirt, "Dead Earth", wasteland earth and then `contenttweaker:rad5`–`rad9` as radiation rises; 22 purification recipes and a scrubber reverse it (quest `Environmental Radiation Scrubbing [U50]`). Quest `Radiation`: "Over time radiation will cause damage to the environment and the player." The pollution mods (Pollution of the Realms, Polluted Earth, Advanced Chimneys, Pollutant Pump) are all gone. | `planned` (ADR-0055). **A chunk-level contaminant that spreads between chunks, decays and damages terrain is the same shape as Factorio pollution.** It comes from what is stored and processed rather than from machine emission rates. |
| **Enemies and evolution** | No invasions and no enhanced AI (quest `Mob Spawning`: "Unlike the previous Manufactio modpack, there are no invasions and no enhanced AI"). Threat has two drivers. **Time:** Scaling Health raises mob health and damage with play time (`config/scalinghealth/main.cfg`: `"Increase Per Second"=0.00011574`, `"Max Value"=250.0`; inferred ≈10 difficulty per 24 h of play, reaching the cap after about 25 days). **Radiation:** `config/incontrol/spawn.json` lets each mutant family spawn only on a band of irradiated ground. Apoca-Mobs' bloatflies, cockroaches and radscorpions spawn on `rad4`/`rads` and up; feral ghouls (double health and damage) on `rad6`–`rad9`; Scape and Run parasites in four escalating groups on wasteland earth, `rad5`, `rad6` and `rad7` and up; Techguns alien bugs on wasteland and `rad5`+. Each has a per-player cap, e.g. 50 alien bugs. Vanilla zombies, skeletons and creepers are denied wherever they see the sky. | `planned`. **The closest any Manufactio pack comes to Factorio's loop**: the more the factory contaminates the land, the nastier what spawns on it. There are still no nests, attacks or expansion. |
| **Combat: guns, ammo, turrets, walls** | Techguns guns (Military 1–3), Open Modular Turrets bases in tiers with gun, grenade, rocket, chem-thrower and Tesla turrets and addons (quests `Gun Turret [M01]`, `Turret Base (Tier 3) [M19]`, `[M14]`, `[M16]`, `[M28]`, `[M34]`, `[M20]`, `[M30]`). Concrete stops hostile spawns (`spawn.json`: `ore:concrete` deny); electric fences knock back (`[M23]`). | `planned`. Broad coverage, as in the original. |
| **Armor and the equipment grid** | Techguns armour sets from Bandit to Power Armour and Exo Suit; hazmat suits for radiation (`[M03]`–`[M61]`, `[U15]`, `[U25]`). No grid (inferred: no grid mod in the manifest). | `planned`. |
| **Modules and beacons** | Speed, Productivity and Efficiency modules in six tiers each, as Modular Machinery modifiers on Assembling Machine 3/4, the Laboratory and the Electric Furnace (18 `modifiers` in `machinery/assembler3.json`). Speed: −10 % time and +20 % RF/t per tier; Productivity: +5 % chance not to consume inputs and +10 % RF/t per tier; Efficiency: −10 % RF/t per tier (quests `[XS1]`, `[XP1]`, `[XE1]`; `scripts/3_research/a19_modules.zs`). No beacons (no `beacon` item in any research script). | `blocked` for us (#120). Same model as the original with six tiers instead of three. Productivity is still a chance to save inputs, not bonus output. |
| **Trains** | Industrial Renewal rails, cargo, bulk, log, fluid and passenger wagons with loaders (quests `Railways [L11]`, `Fluid Wagons [L12]`); Signals block signals and station markers (`config/projectTable/l21_*.json`, quest `Rail Logistics [L21]`). Industrial Renewal's steam locomotive and Signals' cart engine are hidden and uncraftable (`scripts/1_removals/industrialrenewal.zs`, `signals.zs`); booster rails are craftable (`config/projectTable/l11_rail_boost.json`). | `planned`. Inferred: wagons are moved by powered rails, not locomotives. |
| **Logistic robots** | None: the original's drones are gone. Logistic Pipes' supplier/provider/request pipes stand in (`[L10]`, `[L20]`), plus Refined Storage, Simple Storage Network, XNet and item laser relays (`[L24]`, `[L30]`, `[L35]`). | `excluded` (ADR-0017). |
| **Logistic request and trash** | Not present (inferred). | `excluded`. |
| **Construction robots and blueprints** | No robots. Every multiblock is a Package: craft it into a chest of parts, link a Capsule to the chest and deploy the structure (quest `Creating Power - Steam`). Building, Exchanging and Destruction Gadgets (`[S11]`, `[S21]`, `[S22]`). Funky Locomotion frame machines (`[A23]`). | `excluded`. Same blueprint-paste analogue as the original, now with the materials drawn from a chest. |
| **Circuit network** | Project Red, Gauges and Switches, Wireless Redstone CBE (quest `Flexible Redstone Wire [A03]`). No combinators. | `adapted`. |
| **Rocket silo / launch** | A Launchpad structure whose quest takes 200 `immersiveengineering:stone_decoration:5`, 500 Nitinol plates, 100 motors and more (quest `Launchpad`), a single-use Spaceship capsule fuelled with rocket fuel (quest `Spaceship`), and Advanced Rocketry travel to the gas giant's other moons (quests `Visit Niflheim`, `Visit Vulcan`, `Visit Ardethe`, `Visit Theroc`, `Visit Hellous`). | `planned`. Transport, not a win condition; closer to Space Age travel. |
| **Asteroid mining (Space Age)** | An Observatory finds asteroids and compiles Mission Profiles; a Mining Drone Hub (`astro_hub`, 244 script recipes) launches drones fed on solid fuel, in four tiers sized to tiny, small, medium and large asteroids (quests `Asteroid Mining [C01]`, `[C11]`, `[C21]`, `[C31]`; `scripts/3_research/asteroid_mining/`, one file per resource). Asteroid chunks become White science (`laboratory_space_*.json`). | `blocked` for us. **An abstracted asteroid-mining loop**, with no platform. |
| **Interplanetary travel / space platforms** | Five other bodies in `config/advRocketry/PlanetDefs.xml`; warp travel is a `[WIP]` quest (`Advanced Space Theory [WIP]`). No platforms. | `blocked`. |
| **Nuclear fission** | A Modular Machinery Nuclear Reactor delivered as a Package: water-cooled, 5000 RF/t over 400 ticks, or liquid-lead-cooled, 10000 RF/t over 100 ticks with more Neptunium by-product (quest `Uranium Nuclear Reactor [U22]`; 6 JSON recipes under `config/modularmachinery/recipes/` with `"machine": "nuclear_reactor_1"`). Uranium ore → yellowcake → Isotope Centrifuge → U-235/U-238/U-234 → fuel cells (`[U02]`, `[U12]`); a "Kovarex Enrichment Process" enriching U-238 into U-235 (`[U52]`); fuel cell reprocessing (`[U54]`); Neptunium → Plutonium (`[U32]`); Plutonium reactor `[WIP]`. Geiger counter, shielding plates, Rad-X, RadAway, hazmat (Nuclear chapter). | `adapted`. **Kovarex by name**, and fuel-cell reprocessing like Factorio's. The reactor is a fixed-output recipe, with no heat or neighbour bonus. |
| **Personal transport** | MrCrayfish's Off Roader, Speedboat and Sports Plane on Fuelium from a gas pump (`[L14]`–`[L16]`). | `blocked` for us. |
| **Radar and map exploration** | JourneyMap; Scannable scanner (`[S12]`); Ore Visual Detector. No radar block. | `adapted` (our Radar). |
| **Trees and wood** | Treecapitator; tree, crop, mushroom and chorus farms (Organic chapter). | `adapted`. |
| **Agriculture (Gleba-like)** | An Organic chapter with its own Orange science pack: Plant Masher, algae farms, Industrial Squeezer, bioplastics, and radiation-grown ore reeds (quests `[O01]`–`[O34]`). | `planned` for us (Gleba). An organic tech branch with its own pack, not spoilage. |
| **Recycling (Space Age)** | Fuel-cell reprocessing only (`[U54]`); the original's Recycler machine is not among the 76 machines. | `planned`. |
| **Day and night** | Vanilla, with Realistic Torches (manifest) and powered lanterns and floodlights "provide greater protection against mob spawning" (quest `Optics [S01]`). | `shipped`. |

## 4. Factorio mechanics Nuclear Edition does not implement

Each item was checked against the manifest and scripts; absence is inferred from no mod or script
providing it.

- **A recipe set on the machine with input filtering.** The Plan narrows matching but does not stop
  wrong inputs entering.
- **A crafting queue.** The Project Table crafts one click at a time.
- **Beacons.**
- **Belt lanes, tiers, underground belts and splitters.** One conveyor type and an Item Router.
- **Nests, attacks, expansion and absorption.** Radiation raises what spawns, but nothing attacks
  the factory on its own schedule.
- **Locomotives.** The locomotive items are hidden (`scripts/1_removals/industrialrenewal.zs`).
- **Logistic and construction robots.**
- **Combinators.**
- **An equipment grid.**
- **Factorio's figures.** No file indicates numbers were taken from Factorio's data.
- **Space Age quality, spoilage and space platforms.** Asteroid mining exists but is abstracted
  into missions.

## 5. Completely new: in Nuclear Edition, not in Factorio

| feature | where | worth noting for us? |
| --- | --- | --- |
| Plans: research rewards as copyable items that also select a machine's recipe | `scripts/3_research/*.zs`, quests `Plans`, `Plan Library` | **Yes.** One item is both the unlock and the recipe selector, which is a cheap Held-recipe analogue. |
| Radiation as the contamination model, with mutant mobs tiered to how irradiated the ground is | `scripts/x_environment/radiation.zs`, `config/incontrol/spawn.json` | **Yes**, as a data point for ADR-0055: contamination that raises local threat without nests. |
| Everyday items made radioactive (redstone, pistons, science packs) | `scripts/x_environment/radiation.zs` | No. |
| Time-based mob scaling (Scaling Health) | `config/scalinghealth/main.cfg` | No. It is time, not the factory, that drives it. |
| Infinite ore clusters mined by a sized multiblock drill | `config/cofh/world/x_infinite_ores.json`, quest `[A22]` | Possibly, as a late-game infinite tier. |
| Asteroid missions: Observatory → Mission Profile → drone launch → ore and White science | `scripts/3_research/asteroid_mining/`, quests `[C01]`–`[C31]` | Possibly, as an abstraction of Space Age asteroid mining. |
| GLOOP: a singularity fluid collected and converted into resources | quests `GLOOP Theory`, `GLOOP Reaction Chamber`, `[C41]` | No. |
| Challenges: repeatable deliveries (e.g. 1000 or 5000 of a science pack) paying coins spent on ore rewards | Challenges chapter | No. |
| Four-tier multiblocks upgraded by swapping a core (Core upgrades T2–T4: +33/66/100 % RF/t, −25/50/75 % time) | quests `[P19]`, `[P29]`, `[P39]` | Only as a data point: an upgrade-in-place mechanic, close to our Fast Replace. |
| Manufacturer: supply raw materials plus the finished item as a blueprint, skip the intermediates | quest `Manufacturer [A35]` | No. |
| Ore processing line (Grinder, Sieve, Ore Mill froth flotation, Washer, Electrolyser, Centrifuge, Iridium extraction) | Processing chapter | No. That is modded-Factorio flavour, not base game. |
| Story frame: stranded on a moon of a gas giant near a black hole | quest `Story`, `config/advRocketry/PlanetDefs.xml` | No. |

## 6. Diff vs the original Manufactio (1.35)

Compared by project ID against `Manufactio-1.35.zip`'s `manifest.json` (146 mods): **76 kept, 70
removed, 77 added**. Of the 76 kept, 20 are at a different file.

**Removed (70):** Additional Resources, Advanced Chimneys, Applied Energistics 2, BNBGamingLib, BdLib,
Better Foliage, Better Questing, Better Questing - Quest Book, Better Questing - Standard Expansion,
Better Railroads, BnBGamingCore, Bookshelf, Chicken Chunks 1.8.+, Compacter, ComputerCraft, CoroUtil,
DesirePaths, Drones, Dynamic Surroundings, Dynamic Trees, Entity Culling, Epic Siege Mod, Expandable
Inventory, Extreme Reactors, Factory0-Resources, FindMe, ForgeEndertech, Game Stages, GraveStone Mod,
Guide-API, Hostile Worlds - Invasions, Immersive Petroleum, Item Stages, Just Enough Calculation,
LLibrary, Logistical Automation, LootBags, LunatriusCore, MalisisCore, MalisisDoors, Mekanism,
Mekanism Generators, Metal Chests, MixinBootstrap, Mo' Bends, Mob Grinding Utils, Mob Stages, Mob
Sunscreen, Mowzie's Mobs, MultiBlock Stages, OnlinePictureFrame, OpenBlocks, OpenModsLib, Phosphor
(Forge), PneumaticCraft: Repressurized, Pollutant Pump, Polluted Earth Reborn, Pollution of the
Realms, ProjectE, Quest Utils, RFTools, Serene Seasons, Simple Magnet, SimpleHarvest, Super Sound
Muffler: Revived, Suppergerrie2's Drone Mod, TBone, Topography, ZeroCore 2, Zombie Awareness.

**Added (77):** Abyssal Depths, Actually Additions, Additional Enchanted Miner, AI Improvements,
AmbientSounds 6, Apoca-Mobs, Athenaeum, AttributeFix, BetterFps, Biomes O' Plenty, Born in a Barn,
Bouncy Creepers, bspkrsCore Updated, Collective, Corpse, Derelict, Dropt, Ender Storage 1.8.+,
Ender-Rift, Engineer's Decor, Farseek, Fish's Undead Rising, FTB Library (Legacy), FTB Quests, FTB
Utilities, Funky Locomotion, Gauges and Switches, HammerLib, Impractical Storage, Iron Chests, Item
Filters, IvToolkit, LagGoggles, Light Level Overlay Reloaded, MAGE (Graphical Tweaks), Magma Monsters,
MixinBooter, Mob Spawner Control, Natura, No Recipe Book, NuclearCraft: Overhauled, Numina, OGDragon +,
OMLib, Open Modular Turrets, Ore Reeds, Ore Visual Detector, Pam's Portal Poof, Portality, Project
Table, Random Bone Meal Flowers, Realistic Terrain Generation, Realistic Torches, Refined Storage,
Scaling Health, Scannable, Scape and Run: Parasites, Silent Lib, Simple Ore Samples, Simple Storage
Network, Smooth Font, Solar Flux Reborn, Spiders 2.0, Spiders Produce Webs, Streams, Surge, Thermal
Expansion, TickCentral, TidyChunk, Translocators 1.8.+, Treecapitator Updated, Water Control Extreme,
Weaker Spiderwebs, Wireless Redstone CBE, XNet, YUNG's Better Caves, YUNG's Better Mineshafts.

**Mechanics changed:**

| area | Manufactio 1.35 | Nuclear Edition 1.01 |
| --- | --- | --- |
| Research unlock | Better Questing quest → `/gamestage add`, lifting Item/Recipe/MultiBlock Stages locks | FTB Quests quest → Plan item, used as a non-consumed ingredient |
| Science packs | Renamed Thermal Foundation coins; each pack becomes its own research coin | ContentTweaker items; a tier's research consumes every lower pack too |
| Ore | Invisible per-chunk amounts (Factory0-Resources) found with a scanner | Surface ore spikes (CoFH World) plus infinite ore clusters |
| Drill | Factory0-Resources Burner/Electric Drill with consumable heads | QuarryPlus quarries renamed, Industrial Renewal drill, Techguns ore drill |
| Oil | Per-chunk finite fluid; Immersive Petroleum | Oil source blocks; Modular Machinery pumpjack, separator, distillation tower |
| Pollution | Pollution of the Realms air pollution + Polluted Earth ground | NuclearCraft radiation, with block mutation |
| Enemies | Invasions every 7 days, digging/building AI, mob types unlocked by research | No invasions or enhanced AI; Scaling Health time scaling; mutants on irradiated ground |
| Hand crafting | 2x2 grid plus a few Forestry Worktables; crafting tables uncraftable | Project Table with one-click crafting; crafting tables allowed |
| Belts | IE, Logistical Automation and Magneticraft conveyors | Industrial Renewal Bulk Conveyor |
| Power transfer | IE wires, Thermal fluxducts | Energy ducts to UHV, flexible wires, Tesla Tower area power, laser relays |
| Nuclear | Extreme Reactors | Modular Machinery reactor, isotope centrifuge, Kovarex, reprocessing |
| Late game | Mekanism "nanotechnology", ProjectE EMC | Asteroid missions, GLOOP, Portality; no Mekanism, no ProjectE |
| Storage | AE2 | Refined Storage, Simple Storage Network, XNet, Ender-Rift |
| Robots | Hauler Drone, PneumaticCraft logistics drones | None |
| Water | Vanilla | Finite outside beach/river/ocean biomes (Water Control Extreme) |
| Machines | 26 Modular Machinery definitions | 76, mostly in four tiers |
| Modules | 3 tiers each | 6 tiers each |

## 7. Reception

**CurseForge**

- **Total downloads:** 28,431 (cfwidget `downloads.total`; the project page shows the same number).
  **Comments:** 12 (the project page's Comments tab label). The comment text could not be read
  through WebFetch.
- **Per-file downloads.** cfwidget's exact numbers and the files tab's rounded ones. They disagree
  for 1.01 (9,456 vs 12.1K). Inferred: cfwidget's cache is stale, as it was for the original.

| file | uploaded | downloads (cfwidget) | downloads (files tab) |
| --- | --- | --- | --- |
| Manufactio NE-1.01.zip | 2023-02-20 | 9,456 | 12.1K |
| Manufactio NE-1.0.zip | 2023-01-21 | 879 | 905 |
| Manufactio NE-0.23.zip | 2022-06-06 | 4,194 | 4.2K |
| Manufactio NE-0.22.zip | 2022-05-17 | 664 | 691 |
| Manufactio NE-0.21.zip | 2022-04-24 | 698 | 715 |
| Manufactio NE-0.20.zip | 2022-02-07 | 1,897 | 1.9K |
| Manufactio NE-0.19.zip | 2022-01-02 | 1,049 | 1.0K |
| Manufactio NE-0.18c.zip | 2021-12-11 | 675 | 692 |
| Manufactio NE-0.18b.zip | 2021-12-09 | 95 | 111 |
| Manufactio NE-0.18.zip | 2021-12-08 | 85 | 105 |
| Manufactio NE-0.17.zip | 2021-11-13 | 740 | 760 |
| Manufactio NE-0.16.zip | 2021-10-31 | 472 | 499 |
| Manufactio NE-0.15.zip | 2021-10-15 | 515 | 534 |
| Manufactio NE-0.14.zip | 2021-10-10 | 225 | 244 |
| Manufactio NE-0.13.zip | 2021-09-25 | 538 | 557 |
| Manufactio NE-0.12.zip | 2021-09-14 | 462 | 479 |
| Manufactio NE-0.11.zip | 2021-09-04 | 404 | 420 |
| Manufactio NE-0.10.zip | 2021-08-28 | 337 | 351 |
| Manufactio NE-0.09.zip | 2021-08-20 | 342 | 359 |
| Manufactio NE-0.08.zip | 2021-08-12 | 346 | 365 |

- For scale: the original has 160,585 downloads and Casual Mode 8,732 (see
  `docs/research/manufactio.md` and `docs/research/manufactio-casual-mode.md`).

**YouTube.** Titles and channels come from YouTube's oEmbed endpoint. View counts and publish dates
were read from each watch page's `viewCount`/`publishDate` on 2026-09-27. The first three are
embedded under "Playthroughs:" on the CurseForge description (cfwidget `description`).

| video | channel | published | views | how found |
| --- | --- | --- | --- | --- |
| [Minecraft Manufactio 2 / Nuclear Edition - Ep 1 / Alien world](https://www.youtube.com/watch?v=6XtMQcg2nDY) | Lensmanoz | 2021-12-08 | 6,620 | CF page |
| [Manufactio N: 01 - A new world](https://www.youtube.com/watch?v=phqytvqb2MM) | Anakardian | 2021-08-31 | 3,886 | CF page |
| [Manufactio NE EP1-Here we go again!](https://www.youtube.com/watch?v=YV6zTPaJb0E) | Frogman79 | 2022-03-25 | 2,926 | web search |
| [Manufactio NE - A New Beginning (E01) - Manufactio NE](https://www.youtube.com/watch?v=5pckfiEMoqA) | Jonny & Lawrence | 2021-10-12 | 2,662 | CF page |
| [Manufactio 2 Nuclear Edition Episode 1](https://www.youtube.com/watch?v=w7_K4SY7ZqI) | TheNimbleNinja | 2021-10-11 | 394 | web search |
| [Minecraft Manufactio 2 / Nuclear Edition - Ep 52 / Space Prep](https://www.youtube.com/watch?v=LKF3jsyW1kk) | Lensmanoz | 2022-06-25 | 135 | web search |
| [Minecraft Manufactio 2 / Nuclear Edition - Ep 41 / Nuclear Power](https://www.youtube.com/watch?v=TmdZPFx3jXI) | Lensmanoz | 2022-04-20 | 99 | web search |

(The `|` in the Lensmanoz titles is written `/` here to keep the table intact.)

Other observations:

- Lensmanoz's series runs to at least episode 52; web search also returned episodes 2, 20, 27, 35,
  40 and 51, which were not opened. Episode counts for the other series were not collected.
- Lensmanoz is the CurseForge owner of Single Spot Chest (cfwidget project 355166), a mod in both
  Manufactio packs. Anakardian and Frogman79 also played the original (`docs/research/manufactio.md`),
  and Anakardian is thanked in quest `Credits`.

**Forum and Discord.** No FTB forum thread for the sequel was found; web searches returned only the
original's thread. The description links a Discord (`https://discord.gg/vtgn5aPrXe`), which was not
opened.

**Reddit.** Web searches for the sequel on Reddit returned nothing relevant. No thread was found,
which does not mean none exists.

## 8. Sources

- CurseForge project page: https://www.curseforge.com/minecraft/modpacks/manufactio-2-nuclear-edition
- CurseForge files tab: https://www.curseforge.com/minecraft/modpacks/manufactio-2-nuclear-edition/files/all
- CurseForge comments tab (count only): https://www.curseforge.com/minecraft/modpacks/manufactio-2-nuclear-edition/comments
- cfwidget API (project, files, per-file downloads, description): https://api.cfwidget.com/minecraft/modpacks/manufactio-2-nuclear-edition
- cfwidget API per mod (manifest file-ID and slug resolution): `https://api.cfwidget.com/<projectID>` for each of the 153 project IDs
- Golrith's CurseForge projects: https://www.curseforge.com/members/golrith/projects
- Modpack zip 1.01: https://mediafilez.forgecdn.net/files/4405/41/Manufactio%20NE-1.01.zip. Files read: `manifest.json`,
  `modlist.html`, `overrides/scripts/3_research/{000,a19_modules,a22_infinity_mining,r16_tesla_towers,u22_nuclear_reactor,l11_rails,l13_engines}.zs`,
  `overrides/scripts/3_research/asteroid_mining/`, `overrides/scripts/1_removals/{industrialrenewal,signals}.zs`,
  `overrides/scripts/x_environment/radiation.zs`, `overrides/config/ftbquests/normal/` (all chapters),
  `overrides/config/modularmachinery/{machinery,recipes}/`, `overrides/config/projectTable/`,
  `overrides/config/cofh/world/`, `overrides/config/incontrol/spawn.json`, `overrides/config/scalinghealth/main.cfg`,
  `overrides/config/nuclearcraft.cfg`, `overrides/config/advRocketry/PlanetDefs.xml`
- Original for the diff: `Manufactio-1.35.zip` (https://mediafilez.forgecdn.net/files/3582/169/Manufactio-1.35.zip), `manifest.json`
- YouTube videos: the URLs in section 7. Metadata came from `https://www.youtube.com/oembed?url=…` and the watch pages.
- Our ledger: `/Users/kc00l/curseforge/Instances/PlanetaryFactory/docs/factorio-mechanics.md`
