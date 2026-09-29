# Manufactio: a Factorio-inspired 1.12.2 pack, compared

Researched 2026-09-27 for a comparison with FactoryWorks. Where a claim comes from inside the
modpack zip, the citation is a path inside `Manufactio-1.35.zip` (CurseForge file 3582169, downloaded
from `https://mediafilez.forgecdn.net/files/3582/169/Manufactio-1.35.zip`). The pack's overrides sit
under `overrides/`, so `overrides/scripts/research.zs` is the [CraftTweaker](https://www.curseforge.com/minecraft/mc-mods/crafttweaker) script of that name. Our
verdicts are quoted from `docs/factorio-mechanics.md` as of this date. **Inferred** marks anything
not stated by a source.

## 1. Overview

| field | value | source |
| --- | --- | --- |
| Name | Manufactio | `manifest.json` (`"name": "Manufactio"`) |
| Author | Golrith (CurseForge owner); `"author": "golrith"` in the manifest | https://api.cfwidget.com/minecraft/modpacks/manufactio, `manifest.json` |
| CurseForge project ID | 298850 | cfwidget, https://www.curseforge.com/minecraft/modpacks/manufactio |
| Minecraft / loader | 1.12.2, Forge 14.23.5.2860 | `manifest.json` (`forge-14.23.5.2860`) |
| Project created | 2018-07-22 | cfwidget `created_at` |
| First public post | FTB forum thread by Golrith, 2 July 2018 | https://forum.feed-the-beast.com/threads/manufactio-minecraft-factorio-hybrid.283615/ |
| Latest version | 1.35 (`Manufactio-1.35.zip`, 2021-12-27) | cfwidget; https://www.curseforge.com/minecraft/modpacks/manufactio/files/all |
| Mods in the manifest | 146 | `manifest.json` (146 `files`), `modlist.html` (146 entries) |
| Categories | Tech, Quests | cfwidget |
| License | All Rights Reserved | CurseForge project page |
| Related packs by the same author | Manufactio - Casual Mode; Manufactio 2 - Nuclear Edition (project 500287, created 2021-07-05, 1.12.2, latest `Manufactio NE-1.01.zip` 2023-02-20) | CurseForge project page; https://api.cfwidget.com/minecraft/modpacks/manufactio-2-nuclear-edition |

**Description summary** (CurseForge description via cfwidget): the pack brings Factorio-style play to
Minecraft. You gather resources to build science packs, which become research points that unlock
technology. You start with few tools and defend the factory against mobs that grow more dangerous.
Later stages add a rocket to the planet's moons, [Mekanism](https://www.curseforge.com/minecraft/mc-mods/mekanism) "nanotechnology" and [ProjectE](https://www.curseforge.com/minecraft/mc-mods/projecte) EMC. The
description also mentions an optional 32x resource pack. The "enhanced AI" mobs and the pollution
system are the difficulty levers, and a `Difficulty Options` folder offers easier configs. For
servers, the author asks owners to raise [Polluted Earth](https://www.curseforge.com/minecraft/mc-mods/polluted-earth-reborn)'s day-based timers, because pollution's
effects depend on elapsed time.

The earliest file the CurseForge files tab still lists is 1.30 (2020-03-29). Only 8 files are listed,
so versions before 1.30 are not public there
(https://www.curseforge.com/minecraft/modpacks/manufactio/files/all).

**How the pack is built.** Everything Factorio-like is stitched together from other mods plus
scripts. There is no Manufactio mod of its own. The four load-bearing pieces are:

- [CraftTweaker](https://www.curseforge.com/minecraft/mc-mods/crafttweaker)/ModTweaker scripts (`overrides/scripts/*.zs`, 33 files, including `research.zs`,
  `recipestages.zs`, `SciencePacks.zs` and `Tier1.zs`–`Tier4.zs`).
- [Game Stages](https://www.curseforge.com/minecraft/mc-mods/game-stages) with [Item](https://www.curseforge.com/minecraft/mc-mods/item-stages)/[Mob](https://www.curseforge.com/minecraft/mc-mods/mob-stages)/[MultiBlock Stages](https://www.curseforge.com/minecraft/mc-mods/multiblock-stages), which lock things until they are researched.
- [Better Questing](https://www.curseforge.com/minecraft/mc-mods/better-questing), which holds the research tree (`overrides/config/betterquesting/DefaultQuests.json`,
  376 quests).
- [Modular Machinery](https://www.curseforge.com/minecraft/mc-mods/modular-machinery), which provides the custom multiblocks (`overrides/config/modularmachinery/machinery/`,
  26 machine definitions).

## 2. Full mod list

All 146 entries in `manifest.json`. `manifest.json` gives only `projectID`/`fileID`, so each
file name was looked up from its project's file list at `https://api.cfwidget.com/<projectID>`.
The names match `modlist.html`. For mods the pack's own files use in a specific way, the "Role"
column gives that use and cites the file. For every other mod, the column quotes the mod's own
CurseForge summary (from cfwidget) and makes no claim about how the pack uses it.

| # | Mod (CurseForge project ID) | File in manifest | Role |
| --- | --- | --- | --- |
| 1 | [Additional Resources](https://www.curseforge.com/minecraft/mc-mods/additional-resources) (225124) | `additionalresources-1.9.4-0.2.0.28+47cd0bd_signed.jar` | CF summary: Add loose resources to the game without needing the user to install a texture pack themselves. |
| 2 | [Advanced Chimneys](https://www.curseforge.com/minecraft/mc-mods/advanced-chimneys) (244830) | `AdChimneys-1.12.2-3.5.14.0-build.0505.jar` | **Pack role:** route and filter emissions (ContentTweaker filter blocks) |
| 3 | [Advanced Rocketry](https://www.curseforge.com/minecraft/mc-mods/advanced-rocketry) (236542) | `AdvancedRocketry-1.12.2-1.7.0-232-universal.jar` | **Pack role:** rocket to the moon, space stations, warp (Space and Beyond chapter) |
| 4 | [AppleSkin](https://www.curseforge.com/minecraft/mc-mods/appleskin) (248787) | `AppleSkin-mc1.12-1.0.14.jar` | CF summary: Adds some useful information about food/hunger to the HUD |
| 5 | [Applied Energistics 2](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2) (223794) | `appliedenergistics2-rv6-stable-7.jar` | **Pack role:** digital storage and autocrafting (L032, L044) |
| 6 | [ArchitectureCraft - TridentMC Version](https://www.curseforge.com/minecraft/mc-mods/architecturecraft-tridev) (277631) | `architecturecraft-1.12-3.98.jar` | CF summary: Distinguished architectural features for your Minecraft buildings. Ported to 1.12 |
| 7 | [Aroma1997Core](https://www.curseforge.com/minecraft/mc-mods/aroma1997core) (223735) | `Aroma1997Core-1.12.2-2.0.0.2.b167.jar` | CF summary: This is a mod required by most of my other mods, so you should install it, if you want to run any other of my |
| 8 | [AromaBackup](https://www.curseforge.com/minecraft/mc-mods/aromabackup) (225658) | `AromaBackup-1.12.2-3.0.0.0.b135.jar` | CF summary: IT DOES BACKUPS!!! (Works in SSP and SMP) |
| 9 | [AutoRegLib](https://www.curseforge.com/minecraft/mc-mods/autoreglib) (250363) | `AutoRegLib-1.3-32.jar` | CF summary: A library to ease menial tasks in mod development. |
| 10 | [B.A.S.E](https://www.curseforge.com/minecraft/mc-mods/base) (246996) | `base-1.12.2-3.13.0.jar` | CF summary: Main Library for The Acronym Coders Mod Projects |
| 11 | [Baubles](https://www.curseforge.com/minecraft/mc-mods/baubles) (227083) | `Baubles-1.12-1.5.2.jar` | CF summary: An addon module and API for Thaumcraft |
| 12 | [BdLib](https://www.curseforge.com/minecraft/mc-mods/bdlib) (70496) | `bdlib-1.14.3.12-mc1.12.2.jar` | CF summary: A library of generic code for my other mods |
| 13 | [Better Boilers](https://www.curseforge.com/minecraft/mc-mods/better-boilers) (284383) | `BetterBoilers-1.2.jar` | **Pack role:** multiblock boiler for steam power |
| 14 | [Better Foliage](https://www.curseforge.com/minecraft/mc-mods/better-foliage) (228529) | `BetterFoliage-MC1.12-2.3.1.jar` | CF summary: Makes the vegetation in your world more natural and awesome |
| 15 | [Better Questing](https://www.curseforge.com/minecraft/mc-mods/better-questing) (238856) | `BetterQuesting-3.5.299.jar` | **Pack role:** research tree: each research is a quest consuming research points and granting a game stage (`config/betterquesting/DefaultQuests.json`) |
| 16 | [Better Questing - Quest Book](https://www.curseforge.com/minecraft/mc-mods/better-questing-quest-book) (242106) | `questbook-3.1.1-1.12.jar` | CF summary: Adds a quest book to Funwayguy's Better Questing Mod |
| 17 | [Better Questing - Standard Expansion](https://www.curseforge.com/minecraft/mc-mods/better-questing-standard-expansion) (238857) | `StandardExpansion-3.4.159.jar` | CF summary: Standard tasks, rewards, importers and themes for BetterQuesting |
| 18 | [Better Railroads](https://www.curseforge.com/minecraft/mc-mods/better-railroads) (285047) | `Better-Railroads-1.0.0.jar` | CF summary: A mod for better utilizing rails and carts |
| 19 | [BnBGamingCore](https://www.curseforge.com/minecraft/mc-mods/bnbgamingcore) (274341) | `BNBGamingCore-1.12.2-0.12.0.jar` | CF summary: Core Mod containing all ASM for BnBGaming Mods |
| 20 | [BNBGamingLib](https://www.curseforge.com/minecraft/mc-mods/bnbgaminglib) (229587) | `BNBGamingLib-1.12.2-2.17.6.jar` | CF summary: Library for Blood N Bones Gaming mods |
| 21 | [Bookshelf](https://www.curseforge.com/minecraft/mc-mods/bookshelf) (228525) | `Bookshelf-1.12.2-2.3.590.jar` | CF summary: An open source library for other mods! |
| 22 | [Building Gadgets](https://www.curseforge.com/minecraft/mc-mods/building-gadgets) (298187) | `BuildingGadgets-2.8.4.jar` | CF summary: A collection of Gadgets to make building large structures a little bit easier! |
| 23 | [Capsule](https://www.curseforge.com/minecraft/mc-mods/capsule) (235338) | `Capsule-1.12.2-3.4.76.jar` | **Pack role:** machine "packages", deployable capsules and blueprints (`scripts/Capsule.zs`) |
| 24 | [CB Multipart](https://www.curseforge.com/minecraft/mc-mods/cb-multipart) (258426) | `ForgeMultipart-1.12.2-2.6.2.83-universal.jar` | CF summary: An opensource library for having multiple things in the one block space |
| 25 | [Chicken Chunks 1.8.+](https://www.curseforge.com/minecraft/mc-mods/chicken-chunks-1-8) (243883) | `ChickenChunks-1.12.2-2.4.2.74-universal.jar` | CF summary: Chunk load all the chunks with the power of chicken! |
| 26 | [Chisel](https://www.curseforge.com/minecraft/mc-mods/chisel) (235279) | `Chisel-MC1.12.2-1.0.2.45.jar` | CF summary: A builder's best friend. |
| 27 | [Clumps](https://www.curseforge.com/minecraft/mc-mods/clumps) (256717) | `Clumps-3.1.2.jar` | CF summary: Clumps XP orbs together to reduce lag |
| 28 | [CodeChicken Lib 1.8.+](https://www.curseforge.com/minecraft/mc-mods/codechicken-lib-1-8) (242818) | `CodeChickenLib-1.12.2-3.2.3.358-universal.jar` | CF summary: Contains various tools to make modding easier. |
| 29 | [CoFH Core](https://www.curseforge.com/minecraft/mc-mods/cofh-core) (69162) | `CoFHCore-1.12.2-4.6.6.1-universal.jar` | CF summary: Contains Core Functionality for all Team CoFH mods. Also does some really cool stuff on its own! |
| 30 | [CoFH World](https://www.curseforge.com/minecraft/mc-mods/cofh-world) (271384) | `CoFHWorld-1.12.2-1.4.0.1-universal.jar` | CF summary: Customizable and powerful world generation! |
| 31 | [Compacter](https://www.curseforge.com/minecraft/mc-mods/compacter) (231549) | `compacter-1.3.0.3-mc1.12.2.jar` | CF summary: Rapidly compacts items that can be crafted in 2x2 or 3x3 pattern |
| 32 | [ComputerCraft](https://www.curseforge.com/minecraft/mc-mods/computercraft) (67504) | `ComputerCraft1.80pr1.jar` | CF summary: Computers, Programming and Robotics in Minecraft |
| 33 | [ConnectedTexturesMod](https://www.curseforge.com/minecraft/mc-mods/ctm) (267602) | `CTM-MC1.12.2-1.0.2.31.jar` | CF summary: A resource pack extension library |
| 34 | [ContentTweaker](https://www.curseforge.com/minecraft/mc-mods/contenttweaker) (237065) | `ContentTweaker-1.12.2-4.9.1.jar` | **Pack role:** research items, packages, filter blocks (`scripts/contenttweaker/`) |
| 35 | [Controlling](https://www.curseforge.com/minecraft/mc-mods/controlling) (250398) | `Controlling-3.0.10.jar` | CF summary: Adds a search bar to the Key-Bindings menu |
| 36 | [CoroUtil](https://www.curseforge.com/minecraft/mc-mods/coroutil) (237749) | `coroutil-1.12.1-1.2.37.jar` | CF summary: Library Mod for Weather/Tropicraft/ZombieAwareness/etc |
| 37 | [CraftTweaker](https://www.curseforge.com/minecraft/mc-mods/crafttweaker) (239197) | `CraftTweaker2-1.12-4.1.20.586.jar` | **Pack role:** all recipe/stage scripts (`overrides/scripts/*.zs`) |
| 38 | [CreativeCore](https://www.curseforge.com/minecraft/mc-mods/creativecore) (257814) | `CreativeCore_v1.10.16_mc1.12.2.jar` | CF summary: a core mod |
| 39 | [Custom Main Menu](https://www.curseforge.com/minecraft/mc-mods/custom-main-menu) (226406) | `CustomMainMenu-MC1.12.2-2.0.9.1.jar` | CF summary: Allows you to modify the main menu using a simple json file |
| 40 | [DesirePaths](https://www.curseforge.com/minecraft/mc-mods/desirepaths) (307301) | `desirepaths-1.12.1-1.2.8.jar` | CF summary: Wears down grass as its walked on to create naturally formed dirt paths |
| 41 | [Diet Hoppers](https://www.curseforge.com/minecraft/mc-mods/diet-hoppers) (278385) | `diethopper-1.1.jar` | CF summary: Tightens the selection box of the hopper to allow you to access visible blocks behind them. |
| 42 | [Drones](https://www.curseforge.com/minecraft/mc-mods/drones) (263438) | `Drones-0.3.0.jar` | CF summary: A Mod to add Drones Mobs to the world |
| 43 | [Dynamic Surroundings](https://www.curseforge.com/minecraft/mc-mods/dynamic-surroundings) (238891) | `DynamicSurroundings-1.12.2-3.6.1.0.jar` | CF summary: Alters the fabric of Minecraft experience by weaving a tapestry of sound and visual effects |
| 44 | [Dynamic Trees](https://www.curseforge.com/minecraft/mc-mods/dynamictrees) (252818) | `DynamicTrees-1.12.2-0.9.20.jar` | **Pack role:** trees grow, die and reseed (Introduction quest) |
| 45 | [Entity Culling](https://www.curseforge.com/minecraft/mc-mods/entity-culling) (409087) | `EntityCulling-1.12.2-4.2.5.jar` | CF summary: Skip rendering of hidden entities |
| 46 | [Epic Siege Mod](https://www.curseforge.com/minecraft/mc-mods/epic-siege-mod) (229449) | `EpicSiegeMod-13.167.jar` | **Pack role:** "enhanced AI" digging/building mobs (CF description) |
| 47 | [Expandable Inventory](https://www.curseforge.com/minecraft/mc-mods/expandable-inventory) (304371) | `ExpandableInventory-1.12.2-1.4.0.jar` | **Pack role:** "Toolbelt Upgrade" inventory rows (Exotic Military & Tech chapter) |
| 48 | [Extreme Reactors](https://www.curseforge.com/minecraft/mc-mods/extreme-reactors) (250277) | `ExtremeReactors-1.12.2-0.4.5.50.jar` | **Pack role:** Nuclear Reactor (R033) |
| 49 | [Factory0-Resources](https://www.curseforge.com/minecraft/mc-mods/factory0-resources) (299713) | `Factory0-Resources-1.12.2-1.0.3.1.jar` | **Pack role:** Burner/Electric Drill multiblock mining finite per-chunk ore amounts; oil pump (`config/F0Resources/*.json`, `config/f0-resources.cfg`) |
| 50 | [Fast Leaf Decay](https://www.curseforge.com/minecraft/mc-mods/fast-leaf-decay) (230976) | `FastLeafDecay-v14.jar` | CF summary: Makes leaves decay faster when a tree is cut down. |
| 51 | [FindMe](https://www.curseforge.com/minecraft/mc-mods/findme) (291936) | `findme-1.12.2-1.1.0-8.jar` | CF summary: Search for an item in nearby inventories |
| 52 | [FoamFix](https://www.curseforge.com/minecraft/mc-mods/foamfix-optimization-mod) (278494) | `foamfix-0.10.10-1.12.2.jar` | CF summary: Simple, targetted optimizations for a popular block game |
| 53 | [Forestry](https://www.curseforge.com/minecraft/mc-mods/forestry) (59751) | `forestry_1.12.2-5.8.2.422.jar` | CF summary: Bringing bees, butterflies and more trees. as well as eco-friendly energy production and mail to Minecraft. |
| 54 | [ForgeEndertech](https://www.curseforge.com/minecraft/mc-mods/forgeendertech) (244844) | `ForgeEndertech-1.12.2-4.5.2.0-build.0459.jar` | CF summary: Core library powering Endertech mods |
| 55 | [Game Stages](https://www.curseforge.com/minecraft/mc-mods/game-stages) (268655) | `GameStages-1.12.2-2.0.123.jar` | **Pack role:** research unlock state (stage IDs such as A002, L011) |
| 56 | [GraveStone Mod](https://www.curseforge.com/minecraft/mc-mods/gravestone-mod) (238551) | `gravestone-1.10.3.jar` | CF summary: Places a gravestone with your inventory items inside when you die |
| 57 | [Guide-API](https://www.curseforge.com/minecraft/mc-mods/guide-api) (228832) | `Guide-API-1.12-2.1.8-63.jar` | CF summary: Simple library mod for in-game guide creation |
| 58 | [Hooked](https://www.curseforge.com/minecraft/mc-mods/hooked) (297209) | `hooked-1.0.3.jar` | CF summary: Grappling hooks done simple, natural, and useful. |
| 59 | [Hostile Worlds - Invasions](https://www.curseforge.com/minecraft/mc-mods/hostile-worlds-invasions) (257244) | `hostileworlds_invasions-1.12.1-1.1.13.jar` | **Pack role:** invasion every 7 days per player (`config/HW_Invasions/InvasionConfig.cfg`) |
| 60 | [Hwyla](https://www.curseforge.com/minecraft/mc-mods/hwyla) (253449) | `Hwyla-1.8.26-B41_1.12.2.jar` | CF summary: Hwyla (Here's What You're Looking At) is a UI improvement mod aimed at providing block information directly in |
| 61 | [Hydrophobia](https://www.curseforge.com/minecraft/mc-mods/hydrophobia) (273084) | `hydrophobia-1.0.4c.jar` | CF summary: Don't let it rain on your parade... |
| 62 | [iChunUtil](https://www.curseforge.com/minecraft/mc-mods/ichunutil) (229060) | `iChunUtil-1.12.2-7.2.2.jar` | CF summary: Shared library used by iChun's mods |
| 63 | [Immersive Engineering](https://www.curseforge.com/minecraft/mc-mods/immersive-engineering) (231951) | `ImmersiveEngineering-0.12-98.jar` | **Pack role:** conveyors, LV/MV/HV wire network, blast furnace, excavator, science-pack blueprints (`scripts/SciencePacks.zs`) |
| 64 | [Immersive Petroleum](https://www.curseforge.com/minecraft/mc-mods/immersive-petroleum) (268250) | `immersivepetroleum-1.12.2-1.1.9.jar` | **Pack role:** pumpjack, distillation tower (P033, P038) |
| 65 | [Immersive Posts](https://www.curseforge.com/minecraft/mc-mods/immersiveposts) (314645) | `ImmersivePosts-0.0.5.jar` | CF summary: Extendable treated-wood, aluminium, steel posts and more for ImmersiveEngineering. |
| 66 | [In Control!](https://www.curseforge.com/minecraft/mc-mods/in-control) (257356) | `incontrol-1.12-3.9.17.jar` | **Pack role:** no hostile spawns on grass/dirt/sand/concrete or in light (`config/incontrol/spawn.json`) |
| 67 | [Industrial Renewal](https://www.curseforge.com/minecraft/mc-mods/industrial-renewal) (299849) | `IndustrialRenewal_1.12.2-0.17.6.jar` | CF summary: Industrial objects to minecraft |
| 68 | [Insomniac](https://www.curseforge.com/minecraft/mc-mods/insomniac) (270193) | `insomniac-1.6.jar` | CF summary: Disables sleeping, but you can configure if beds still sets your spawnpoint or not. |
| 69 | [Inventory Tweaks \[1.12 only\]](https://www.curseforge.com/minecraft/mc-mods/inventory-tweaks) (223094) | `InventoryTweaks-1.64+dev.151.jar` | CF summary: Tweaks to inventory handling for ease of use, including sorting and automatic replacement of broken tools or e |
| 70 | [Item Stages](https://www.curseforge.com/minecraft/mc-mods/item-stages) (280316) | `ItemStages-1.12.2-2.0.49.jar` | **Pack role:** hides/locks items until their research stage (`scripts/research.zs`) |
| 71 | [JourneyMap](https://www.curseforge.com/minecraft/mc-mods/journeymap) (32274) | `journeymap-1.12.2-5.7.1.jar` | CF summary: Real-time map used for mapping in-game or your browser as you explore. |
| 72 | [Just Enough Calculation](https://www.curseforge.com/minecraft/mc-mods/just-enough-calculation) (242223) | `JustEnoughCalculation-1.12.2-3.2.4.jar` | CF summary: A simple mod help you calculate the cost for recipes |
| 73 | [Just Enough Items (JEI)](https://www.curseforge.com/minecraft/mc-mods/jei) (238222) | `jei_1.12.2-4.16.1.302.jar` | CF summary: View Items and Recipes |
| 74 | [LibrarianLib](https://www.curseforge.com/minecraft/mc-mods/librarianlib) (252910) | `librarianlib-1.12.2-4.22.jar` | CF summary: An extensive collection of tools, utilities, and frameworks. |
| 75 | [LibVulpes](https://www.curseforge.com/minecraft/mc-mods/libvulpes) (236541) | `LibVulpes-1.12.2-0.4.2-75-universal.jar` | CF summary: Common Library required for my mods |
| 76 | [LLibrary](https://www.curseforge.com/minecraft/mc-mods/llibrary) (243298) | `llibrary-1.7.19-1.12.2.jar` | CF summary: The lightweight Minecraft modding library |
| 77 | [Logistical Automation](https://www.curseforge.com/minecraft/mc-mods/logistical-automation) (283037) | `logisticalautomation-0.1.0.jar` | **Pack role:** faster conveyors, splitter, junction (`scripts/research.zs`) |
| 78 | [Logistics Pipes](https://www.curseforge.com/minecraft/mc-mods/logistics-pipes) (232838) | `logisticspipes-0.10.3.114.jar` | CF summary: Logistics Pipes is an extensive overhaul of the Buildcraft pipe system. It allows for better distribution of i |
| 79 | [LootBags](https://www.curseforge.com/minecraft/mc-mods/lootbags) (225946) | `LootBags-1.12.2-2.5.8.5.jar` | CF summary: Adds bags of loot to mob and dungeon drops |
| 80 | [LootTableTweaker](https://www.curseforge.com/minecraft/mc-mods/loottabletweaker) (261339) | `LootTableTweaker-1.12.2-1.1.14.jar` | CF summary: LootTable support for CraftTweaker / MineTweaker3 |
| 81 | [LunatriusCore](https://www.curseforge.com/minecraft/mc-mods/lunatriuscore) (225605) | `LunatriusCore-1.12.2-1.2.0.42-universal.jar` | CF summary: A (small) compilation of utility classes used in Lunatrius' mods. |
| 82 | [Magneticraft](https://www.curseforge.com/minecraft/mc-mods/magneticraft) (224808) | `Magneticraft_1.12-2.8.3-dev.jar` | **Pack role:** the Inserter (`magneticraft:inserter`) and a back-stuffing conveyor |
| 83 | [MalisisCore](https://www.curseforge.com/minecraft/mc-mods/malisiscore) (223896) | `malisiscore-1.12.2-6.5.1.jar` | CF summary: Framework over Forge. |
| 84 | [MalisisDoors](https://www.curseforge.com/minecraft/mc-mods/malisisdoors) (223891) | `malisisdoors-1.12.2-7.3.0.jar` | CF summary: New animations for doors. You can build you own doors with millions of possibilities! |
| 85 | [Mantle](https://www.curseforge.com/minecraft/mc-mods/mantle) (74924) | `Mantle-1.12-1.3.3.55.jar` | CF summary: Shared code for Forge mods |
| 86 | [McJtyLib](https://www.curseforge.com/minecraft/mc-mods/mcjtylib) (233105) | `mcjtylib-1.12-3.5.4.jar` | CF summary: Companion mod for all mods by McJty (RFTools, XNet, Deep Resonance, Not Enough Wands, SignTastic, ...)) |
| 87 | [Mekanism](https://www.curseforge.com/minecraft/mc-mods/mekanism) (268560) | `Mekanism-1.12.2-9.8.3.390.jar` | **Pack role:** "Nanotechnology" single-block machines, ore x3-x5, fusion (Nanotechnology chapter) |
| 88 | [Mekanism Generators](https://www.curseforge.com/minecraft/mc-mods/mekanism-generators) (268566) | `MekanismGenerators-1.12.2-9.8.3.390.jar` | CF summary: Advanced energy generation for Mekanism. |
| 89 | [Metal Chests](https://www.curseforge.com/minecraft/mc-mods/metalchests) (290145) | `MetalChests-v6.1.0+mc1.12.2.jar` | CF summary: The better alternative to Iron Chests |
| 90 | [MixinBootstrap](https://www.curseforge.com/minecraft/mc-mods/mixinbootstrap) (357178) | `MixinBootstrap-1.0.5.jar` | CF summary: MixinBootstrap is a temporary way of booting Mixin in a MinecraftForge production environment. |
| 91 | [Mo' Bends](https://www.curseforge.com/minecraft/mc-mods/mo-bends) (231347) | `MoBends_1.12.2-1.0.0-beta-20.06.20.jar` | CF summary: Changes the look of players and mobs to act more realistic and epic. |
| 92 | [Mob Grinding Utils](https://www.curseforge.com/minecraft/mc-mods/mob-grinding-utils) (254241) | `MobGrindingUtils-0.3.13.jar` | CF summary: A Mod that adds fun ways to make mob farms |
| 93 | [Mob Stages](https://www.curseforge.com/minecraft/mc-mods/mob-stages) (278359) | `MobStages-1.12.2-2.0.8.jar` | **Pack role:** mob types unlocked by research (`scripts/MobStages.zs`) |
| 94 | [Mob Sunscreen](https://www.curseforge.com/minecraft/mc-mods/mob-sunscreen) (298408) | `mobsunscreen-1.12.2-2.1.3.jar` | CF summary: Stop Zombies and Skeletons from Burning in the Sun! |
| 95 | [ModelLoader](https://www.curseforge.com/minecraft/mc-mods/modelloader) (277663) | `modelloader-1.1.7.jar` | CF summary: A small library to load .mcx model format |
| 96 | [ModTweaker](https://www.curseforge.com/minecraft/mc-mods/modtweaker) (220954) | `modtweaker-4.0.18.jar` | CF summary: ModTweaker is an addon for CraftTweaker, a recipe manipulator utility for Minecraft. It allows you to modify t |
| 97 | [Modular Machinery](https://www.curseforge.com/minecraft/mc-mods/modular-machinery) (270790) | `modularmachinery-1.12.2-1.11.1.jar` | **Pack role:** every custom multiblock: Assemblers, Laboratory, Metal Former, Electric Furnace, Oil Refinery, Chemical Plant, Steam Generator, Recycler; modules as structure modifiers (`config/modularmachinery/`) |
| 98 | [Mouse Tweaks](https://www.curseforge.com/minecraft/mc-mods/mouse-tweaks) (60089) | `MouseTweaks-2.10-mc1.12.2.jar` | CF summary: Enhances inventory management by adding various functions to the mouse buttons. |
| 99 | [Mowzie's Mobs](https://www.curseforge.com/minecraft/mc-mods/mowzies-mobs) (250498) | `mowziesmobs-1.5.8.jar` | CF summary: Powerful overworld enemies and more! |
| 100 | [MrCrayfish's Vehicle Mod](https://www.curseforge.com/minecraft/mc-mods/mrcrayfishs-vehicle-mod) (286660) | `vehicle-mod-0.38.1-1.12.2.jar` | CF summary: Adds in vehicles that you can drive! |
| 101 | [MrTJPCore](https://www.curseforge.com/minecraft/mc-mods/mrtjpcore) (229002) | `MrTJPCore-1.12.2-2.1.4.43-universal.jar` | CF summary: Miscellaneous utilities for all my mods |
| 102 | [MTLib](https://www.curseforge.com/minecraft/mc-mods/mtlib) (253211) | `MTLib-3.0.6.jar` | CF summary: Library files for Minetweaker Addons |
| 103 | [MultiBlock Stages](https://www.curseforge.com/minecraft/mc-mods/multiblock-stages) (284139) | `multiblockstages-1.2.0.jar` | **Pack role:** locks IE multiblocks behind research (`scripts/research.zs`) |
| 104 | [Not Enough Items 1.8.+](https://www.curseforge.com/minecraft/mc-mods/not-enough-items-1-8) (247694) | `NotEnoughItems-1.12.2-2.4.3.245-universal.jar` | CF summary: Recipe Viewer, Inventory Manager, Item Spawner, Cheats and more |
| 105 | [Obfuscate](https://www.curseforge.com/minecraft/mc-mods/obfuscate) (289380) | `obfuscate-0.2.6-1.12.2.jar` | CF summary: A library that adds in useful events and utilities |
| 106 | [OnlinePictureFrame](https://www.curseforge.com/minecraft/mc-mods/onlinepictureframe) (257815) | `OnlinePicFrame_v1.4.40_mc1.12.2.jar` | CF summary: add pictures directly from the internet to the game |
| 107 | [OpenBlocks](https://www.curseforge.com/minecraft/mc-mods/openblocks) (228816) | `OpenBlocks-1.12.2-1.8.1.jar` | CF summary: Random collection of blocks. Some of which aren't blocks at all. |
| 108 | [OpenModsLib](https://www.curseforge.com/minecraft/mc-mods/openmodslib) (228815) | `OpenModsLib-1.12.2-0.12.2.jar` | CF summary: Common base used by OpenBlocks and OpenPeripheral |
| 109 | [OreLib](https://www.curseforge.com/minecraft/mc-mods/orelib) (307806) | `OreLib-1.12.2-3.6.0.1.jar` | CF summary: Support library for OreCruncher's mods |
| 110 | [Phosphor (Forge)](https://www.curseforge.com/minecraft/mc-mods/phosphor-forge) (318255) | `phosphor-1.12.2-0.2.6+build50-universal.jar` | CF summary: Performance improvements for Minecraft's lighting engine |
| 111 | [Placebo](https://www.curseforge.com/minecraft/mc-mods/placebo) (283644) | `Placebo-1.12.2-1.6.0.jar` | CF summary: A library mod |
| 112 | [PneumaticCraft: Repressurized](https://www.curseforge.com/minecraft/mc-mods/pneumaticcraft-repressurized) (281849) | `pneumaticcraft-repressurized-1.12.2-0.11.15-398.jar` | **Pack role:** Logistics Drones with frames (L024) (inferred from quest text) |
| 113 | [Pollutant Pump](https://www.curseforge.com/minecraft/mc-mods/pollutant-pump) (305122) | `pollutantpump-1.12.2-1.2.0.jar` | **Pack role:** removes pollution, researched (C001) |
| 114 | [Polluted Earth Reborn](https://www.curseforge.com/minecraft/mc-mods/polluted-earth-reborn) (358722) | `polluted_earth-1.0.jar` | **Pack role:** polluted ground where mobs spawn (`config/Polluted Earth.cfg`) |
| 115 | [Pollution of the Realms](https://www.curseforge.com/minecraft/mc-mods/pollution-of-the-realms) (269973) | `AdPother-1.12.2-1.2.6.0-build.0510.jar` | **Pack role:** air pollution (carbon, sulfur, dust) emitted by 126 configured machines (`config/adpother/`) |
| 116 | [Portal Gun](https://www.curseforge.com/minecraft/mc-mods/portal-gun) (229084) | `PortalGun-1.12.2-7.1.0.jar` | CF summary: This mod adds the Portal Gun, as well as several other portal-related aspects, to Minecraft! |
| 117 | [Project Red - Core](https://www.curseforge.com/minecraft/mc-mods/project-red-core) (228702) | `ProjectRed-1.12.2-4.9.4.120-Base.jar` | CF summary: Core module for the Project Red series |
| 118 | [Project Red - Exploration](https://www.curseforge.com/minecraft/mc-mods/project-red-exploration) (229049) | `ProjectRed-1.12.2-4.9.4.120-world.jar` | CF summary: Exploration module for the Project Red series |
| 119 | [Project Red - Integration](https://www.curseforge.com/minecraft/mc-mods/project-red-integration) (229045) | `ProjectRed-1.12.2-4.9.4.120-integration.jar` | CF summary: Integration module for the Project Red series |
| 120 | [ProjectE](https://www.curseforge.com/minecraft/mc-mods/projecte) (226410) | `ProjectE-1.12.2-PE1.4.1.jar` | **Pack role:** EMC crafting late game (S003, S004) |
| 121 | [Quark](https://www.curseforge.com/minecraft/mc-mods/quark) (243121) | `Quark-r1.6-179.jar` | CF summary: A Quark is a very small thing. This mod is a collection of small things that improve the vanilla minecraft exp |
| 122 | [Quest Utils](https://www.curseforge.com/minecraft/mc-mods/quest-utils) (291674) | `questutils-0.4.0.jar` | CF summary: A mod that helps modpacks based on objectives and/or quests |
| 123 | [ReAuth](https://www.curseforge.com/minecraft/mc-mods/reauth) (237701) | `reauth-3.6.0.jar` | CF summary: Fixes the Problem of having to restart your Client when your Session invalidates |
| 124 | [Redstone Flux](https://www.curseforge.com/minecraft/mc-mods/redstone-flux) (270789) | `RedstoneFlux-1.12-2.1.1.1-universal.jar` | CF summary: Redstone Flux API - Energy Transfer in Minecraft. |
| 125 | [Resource Loader](https://www.curseforge.com/minecraft/mc-mods/resource-loader) (226447) | `ResourceLoader-MC1.12.1-1.5.3.jar` | CF summary: A small mod that allows users to add their own resources to minecraft without making a resource pack |
| 126 | [RFTools](https://www.curseforge.com/minecraft/mc-mods/rftools) (224641) | `rftools-1.12-7.73.jar` | CF summary: RFTools, blocks and items to help with Redflux (dimension builder, crafter, monitor, scanner, ...) |
| 127 | [Ruins (Structure Spawning System)](https://www.curseforge.com/minecraft/mc-mods/ruins-structure-spawning-system) (227873) | `Ruins-1.12.2.jar` | CF summary: A structure spawning system |
| 128 | [Serene Seasons](https://www.curseforge.com/minecraft/mc-mods/serene-seasons) (291874) | `SereneSeasons-1.12.2-1.2.18-universal.jar` | **Pack role:** seasons gate crop growth (Introduction quest) |
| 129 | [Shadowfacts' Forgelin](https://www.curseforge.com/minecraft/mc-mods/shadowfacts-forgelin) (248453) | `Forgelin-1.8.4.jar` | CF summary: My fork of Emberwalker's Forgelin, with some sprinkles on top. |
| 130 | [Signals](https://www.curseforge.com/minecraft/mc-mods/signals) (245824) | `Signals-1.12.2-1.4.1-30-universal.jar` | **Pack role:** Factorio-style block/chain rail signals and station markers (L031, L041) |
| 131 | [Simple Magnet](https://www.curseforge.com/minecraft/mc-mods/simple-magnet) (245060) | `simplemagnet-1.12.2-1.3.12.jar` | CF summary: A simple item magnet mod |
| 132 | [SimpleHarvest](https://www.curseforge.com/minecraft/mc-mods/simpleharvest) (240783) | `Harvest-1.12-1.2.8-25.jar` | CF summary: Right click crop harvesting |
| 133 | [Single Spot Chest](https://www.curseforge.com/minecraft/mc-mods/single-spot-chest) (355166) | `SingleSpotChest-1.12.2-1.1.jar` | CF summary: A chest type block with a single slot for items |
| 134 | [SpawnTableTweaker](https://www.curseforge.com/minecraft/mc-mods/spawntabletweaker) (316840) | `spawntabletweaker-1.0.jar` | CF summary: Simply crafttweaker interface to manipulate Minecraft's spawn npc tables |
| 135 | [Super Sound Muffler: Revived](https://www.curseforge.com/minecraft/mc-mods/super-sound-muffler-revived) (363856) | `supersoundmuffler-revived_1.12.2_1.0.2.10.jar` | CF summary: Revival of the 1.12 Super Sound Muffler mod! |
| 136 | [Suppergerrie2's Drone Mod](https://www.curseforge.com/minecraft/mc-mods/suppergerrie2s-drone-mod) (291410) | `sdrones-1.3.1.jar` | **Pack role:** Hauler Drone (A034) |
| 137 | [SwingThroughGrass](https://www.curseforge.com/minecraft/mc-mods/swingthroughgrass) (264353) | `stg-1.12.2-1.2.3.jar` | CF summary: Kill mobs and players through grass |
| 138 | [TBone](https://www.curseforge.com/minecraft/mc-mods/tbone) (323527) | `TBone-v1.6.8+mc1.12.2.jar` | CF summary: T145's shared code library |
| 139 | [Techguns](https://www.curseforge.com/minecraft/mc-mods/techguns) (244201) | `techguns-1.12.2-2.0.1.2_1.jar` | **Pack role:** guns, armour, turrets (Military chapter) |
| 140 | [Thermal Dynamics](https://www.curseforge.com/minecraft/mc-mods/thermal-dynamics) (227443) | `ThermalDynamics-1.12.2-2.5.6.1-universal.jar` | CF summary: Thermal gets Dynamic! Adds ducts - transportation for Redstone Flux, Fluids, and Items! |
| 141 | [Thermal Foundation](https://www.curseforge.com/minecraft/mc-mods/thermal-foundation) (222880) | `ThermalFoundation-1.12.2-2.6.7.1-universal.jar` | **Pack role:** its coin items are renamed into science packs and research points (`scripts/SciencePacks.zs`) |
| 142 | [Toast Control](https://www.curseforge.com/minecraft/mc-mods/toast-control) (271740) | `Toast Control-1.12.2-1.8.1.jar` | CF summary: Control toasts, those popups in the corner! |
| 143 | [Topography](https://www.curseforge.com/minecraft/mc-mods/topography) (297878) | `Topography-1.12.2-1.10.1.jar` | **Pack role:** world presets incl. void worlds (`config/topography`, `scripts/VoidWorlds.zs`) |
| 144 | [VanillaFix](https://www.curseforge.com/minecraft/mc-mods/vanillafix) (292785) | `VanillaFix-1.0.10-150.jar` | CF summary: Keep playing after a crash / Increase your FPS by up to 3x |
| 145 | [ZeroCore 2](https://www.curseforge.com/minecraft/mc-mods/zerocore) (247921) | `zerocore-1.12-0.1.2.3.jar` | CF summary: Utility mod and multiblock API |
| 146 | [Zombie Awareness](https://www.curseforge.com/minecraft/mc-mods/zombie-awareness) (237754) | `zombieawareness-1.12.1-1.11.16.jar` | CF summary: - Smarter more aware zombies (and skeletons), they track you down via blood scent, sound, and light source awa |

## 3. Factorio mechanics mapping

"Quest `X`" means the [Better Questing](https://www.curseforge.com/minecraft/mc-mods/better-questing) entry of that name in
`overrides/config/betterquesting/DefaultQuests.json`. The code in brackets is the Game Stage the
quest grants. The quest descriptions are the author's own words about how each mechanic works.

| Factorio mechanic | How Manufactio does it | Our verdict (`docs/factorio-mechanics.md`) and comparison |
| --- | --- | --- |
| **Research and science packs** | Science packs are [Thermal Foundation](https://www.curseforge.com/minecraft/mc-mods/thermal-foundation) coins renamed with [CraftTweaker](https://www.curseforge.com/minecraft/mc-mods/crafttweaker): Red, Green, Blue, Military, High Tech, Production, Anomaly, Challenge (`overrides/scripts/SciencePacks.zs`). Most are made with [IE](https://www.curseforge.com/minecraft/mc-mods/immersive-engineering) Engineer's Workbench blueprints (`mods.immersiveengineering.Blueprint.addRecipe("Science Packs", …)`). The Green pack's inputs are an Inserter and a conveyor, echoing Factorio's green pack. A [Modular Machinery](https://www.curseforge.com/minecraft/mc-mods/modular-machinery) **Laboratory** (a 3x3x3 multiblock) turns each pack into a matching "Research" coin. One such recipe is 133 ticks at 280 RF/t (`overrides/config/modularmachinery/recipes/laboratory2_*.json`, `machinery/laboratory.json`, `laboratory2.json`). A research is then a Better Questing *retrieval* quest that consumes N research coins. For example, quest `Basic Electronics [A002]` takes 30, and `Logistics 2 [L011]` takes 100 + 100. Its reward runs `/gamestage add @p <ID>`, which lifts the [ItemStages](https://www.curseforge.com/minecraft/mc-mods/item-stages)/RecipeStages/[MultiBlockStages](https://www.curseforge.com/minecraft/mc-mods/multiblock-stages) locks in `overrides/scripts/research.zs` and `recipestages.zs`. 221 quests carry a stage reward. | `planned`. Both packs turn packs into research consumed by a lab. In Manufactio a research costs a lump of research items handed in at a quest, with no research rate or queue in a lab. Inferred: research time is only how long the lab takes to make the coins. |
| **Technology tree** | Hand-authored in Better Questing. The chapters include Factory Basics, Automation, Logistics, Processing, Power, Military, Science, Exotic Military & Tech, Space and Beyond, Nanotechnology and Challenges (`DefaultQuests.json` quest lines). The Introduction tells players to explore every chapter rather than finish them one at a time. | `shipped` for us, extracted from Factorio's own tree (ADR-0022). Manufactio's tree is invented and is not derived from Factorio. |
| **Resource patches and finite ore** | [Factory0-Resources](https://www.curseforge.com/minecraft/mc-mods/factory0-resources) gives each chunk an abstract, noise-shaped ore amount per resource (`overrides/config/F0Resources/ores.json`). The configured maxima include coal 100000, iron 100000, gold 5000, redstone 50000, diamond 250 and emerald 100, and the resources are confined to dimension 0. `reduceOreInTheChunk=true` and `oreReducedBy=1` (`overrides/config/f0-resources.cfg`) mean each mined unit comes off the chunk's total, so patches are finite. Quest `Mining`: "To locate ores, right click in a chunk with your scanner". Vanilla/Thermal ore still generates underground (quest `Ores`; `overrides/config/cofh/world/`). Late game adds IE's Excavator on deep deposits (quest `Ore Excavation [P031]`). | `adapted`. Close in spirit: a per-location finite amount drained by a drill. The difference is that Manufactio's "patch" is an invisible per-chunk number found with a scanner. Our amounts are ore blocks on the surface (ADR-0041, ADR-0045). |
| **Mining drills** | Factory0-Resources **Burner Drill**, a 2x2x2 multiblock that burns fuel. It takes a Drill Head (iron, steel, diamond, titanium or uranium), and each head has a tier and speed (`overrides/config/F0Resources/drills.json`). It outputs to a set location (quest `Mining`). The electric version is quest `Powered Mining [P004]` (3x3x1, runs on RF). The drill is crafted as one "Package" block that unpacks into its parts. | `adapted`. Burner then electric, as in Factorio. The drill-head consumable has no Factorio counterpart. |
| **Smelting** | The quests tell players to start with "Stone Furnaces" (quest `Intense Smelting`). Inferred: this means the vanilla furnace, since no script renames one. Next come IE's Improved Blast Furnace, whose fuels and ore-to-ingot recipes are rewritten in `overrides/scripts/Tier1.zs`, and the Arc Furnace. Then a Modular Machinery **Electric Furnace** (quest `Electric Furnace [P034]`; `machinery/electric_furnace.json`). Steel comes from the IE blast furnace (quest `Steel Processing [P001]`). | `shipped` (the three furnace tiers are pack blocks; the burner tiers' GameTest is #432). Same stone, then steel, then electric ladder. Manufactio borrows other mods' furnaces for the middle rung. |
| **Assembling machines and recipe categories** | Three Modular Machinery tiers, each limited by ingredient count much as early Factorio assemblers were. **Basic Assembler** (2x2x2) takes 2 ingredients (quest `Basic Automation`). **Standard Assembler** takes 3 solids + 1 fluid, or 4 solids, at 350 RF/t (quest `Automation 2 [A014]`). **Advanced Assembler** runs faster at 490 RF/t (quest `Automation 4 [A022]`). There are 47 recipes for the Basic Assembler (`recipes/ab/`), 107 + 5 fluid-variant recipes for the Standard (`recipes/as/`) and 4 + 1 for the Advanced (`recipes/aa/`), counted by each JSON's `machine` field. Separate machines act as recipe categories: Metal Former, Chemical Plant, Oil Refinery, Recycler and Hell Forge. Vanilla crafting tables cannot be crafted (`recipes.removeShaped(<minecraft:crafting_table>)` in `Tier1.zs`). | `shipped`: three pack-authored tiers on Oritech's base, tier 3's recipe waiting on `speed-module` (#120). **Recipe locking:** Modular Machinery matches whatever inputs arrive, and no file sets a recipe on a machine (inferred from the machine JSONs, which declare no recipe selection). Manufactio therefore does not reproduce Factorio's set-a-recipe assembler, which we do (Held recipe). |
| **Handcrafting and the crafting queue** | Early crafting is limited to the player's 2x2 grid, a single [Forestry](https://www.curseforge.com/minecraft/mc-mods/forestry) Worktable (one bonus Worktable per research tier), and the Metal Former (quest `Crafting`). No crafting-queue mod is in the manifest. | `shipped` for us (the Personal Assembler and its Crafting Plan). Manufactio **does not** reproduce the queue. Instead it makes hand-crafting deliberately scarce. |
| **Transport belts** | IE conveyors, [Logistical Automation](https://www.curseforge.com/minecraft/mc-mods/logistical-automation) belts (faster; splitter, junction, alternator) and a [Magneticraft](https://www.curseforge.com/minecraft/mc-mods/magneticraft) conveyor that "allow[s] backstuffing" (quests `Logistics 1 [L001]`, `Logistics 2 [L011]`, `Logistics 3 [L022]`; stages in `research.zs`). The description warns that many items on belts hurt FPS and suggests [Thermal Dynamics](https://www.curseforge.com/minecraft/mc-mods/thermal-dynamics) ducts instead. | `adapted` (Beltworks). Manufactio's belts have no Factorio lanes or tiers. It offers three belt mods side by side. |
| **Inserters** | `magneticraft:inserter`, with a whitelist/blacklist GUI (quest `Basic Logistics`). Stack and speed upgrades are researches (`Inserter Stack Upgrade [L023]`, `Inserter Speed Upgrade [L014]`). | `adapted`. Close analogue, including Factorio's inserter capacity bonus as a research. |
| **Electric network and transmission** | IE wires: LV wires carry 4096 RF/t and their connectors 512 RF/t, MV wires 16384 RF/t. HV and transformers come with research (quests `Power Transmission`, `Power Distribution [R001]`, `[R012]`). Thermal fluxducts come later (R040A–D). | `adapted`. Wires between connectors are a pole-and-wire analogue, but there are no supply areas. Inferred from IE's behaviour: power passes only through the connected block. |
| **Power generation (steam, boilers, engines)** | A coal generator gives 60 RF/t (quest `Powering up`). A [Better Boilers](https://www.curseforge.com/minecraft/mc-mods/better-boilers) multiblock boiler feeds either a Modular Machinery **Steam Generator** at 1000 RF/t (quest `Steam Generator`; `machinery/steam_gen.json`) or IE's Diesel Generator running on steam (quest `Improved Steam Power`). Solar, wind, water, bio and lava generators also exist, plus a steam turbine at 4096 RF/t (Power chapter). | `adapted`: the pack's Boiler and Steam Engine at Factorio's 60 mB/s and 450 FE/t, the accumulator at 5 MJ / 300 kW, solar at Oritech's own output (#7), the Steam Turbine still to come (#135). Manufactio has the boiler-to-engine chain too, but its figures are not Factorio's. |
| **Nuclear fission** | [Extreme Reactors](https://www.curseforge.com/minecraft/mc-mods/extreme-reactors) (quest `Nuclear Power [R033]`), plus reprocessing and enrichment (`[R042]`). [Mekanism](https://www.curseforge.com/minecraft/mc-mods/mekanism) fusion is quest `[R050]`. | `adapted`. A different reactor model. |
| **Fluid handling** | IE pipes and tanks, Thermal fluiducts, Mekanism tanks and Modular Machinery fluid hatches (quests `Fluid Handling [L002]`–`[L002D]`). | `shipped`: Oritech's pipe and tank, the pack's barrel and Offshore Pump; the in-line pump is #293. Both packs use generic mod pipes with no Factorio throughput model. |
| **Oil processing** | Oil is a per-chunk finite fluid in Factory0-Resources (`overrides/config/F0Resources/fluids.json`: `fluidMaximum` 1000000, `drainRateMin` 0.2, `drainRateMax` 2.0). Inferred: the minimum drain rate plays the part of Factorio's minimum yield. It is found with a Dowsing Rod (quest `Oil Processing [P013]`). Refining uses a Modular Machinery Oil Refinery (`recipes/or_oil1-3.json`) and later IE's Distillation Tower and Pumpjack via [Immersive Petroleum](https://www.curseforge.com/minecraft/mc-mods/immersive-petroleum) (quests `[P038]`, `[P033]`). A Chemical Plant (`machinery/chemical_plant.json`) makes plastic, sulfur, solid fuel 1–3 and cracking (`recipes/cp_*.json`). | `planned`. A notably full chain, including cracking and solid fuel from three sources. |
| **Pollution** | [Pollution of the Realms](https://www.curseforge.com/minecraft/mc-mods/pollution-of-the-realms) (modid `adpother`) tracks carbon, sulfur and dust in the air. It has 126 emitter configs, the pack's Burner Drill among them at carbon 3.0 and sulfur 0.3 (`overrides/config/adpother/Emitters/f0_resources$burner_drill.cfg`). Pollution drifts with wind and is absorbed by blocks (`config/adpother/Pollutants/*`). Countermeasures are a respirator, filters on chimneys ([Advanced Chimneys](https://www.curseforge.com/minecraft/mc-mods/advanced-chimneys), [ContentTweaker](https://www.curseforge.com/minecraft/mc-mods/contenttweaker) filter blocks in `scripts/contenttweaker/blocks.zs`) and the [Pollutant Pump](https://www.curseforge.com/minecraft/mc-mods/pollutant-pump) (quests `[C000]`, `[C001]`, `[C011]`). [Polluted Earth](https://www.curseforge.com/minecraft/mc-mods/polluted-earth-reborn) turns grass into ground mobs can spawn on (`config/Polluted Earth.cfg`, quest `Mob Spawning and Pollution`). | `planned`. Manufactio has a working per-chunk pollution cloud that spreads and is absorbed. Pollution does not drive attacks (see next row). |
| **Enemies and evolution** | Invasions come every 7 days per player (`overrides/config/HW_Invasions/InvasionConfig.cfg`: `invadeEveryXDays=7`, `invasionCountingPerPlayer=true`), and damaged blocks repair after an invasion. Mobs can dig and place blocks ([Epic Siege Mod](https://www.curseforge.com/minecraft/mc-mods/epic-siege-mod), [Zombie Awareness](https://www.curseforge.com/minecraft/mc-mods/zombie-awareness)). Hostiles cannot spawn on grass, dirt, sand, gravel, concrete or in light (`config/incontrol/spawn.json`). The "evolution" is research-gated: each mob type unlocks with a research, for example creepers at P011 and endermen at P027 (`overrides/scripts/MobStages.zs`; quest texts "UNLOCKS CREEPERS"). There are no nests. | `planned` (ADR-0055: nests absorb pollution and pay for attacks). Manufactio ties threat to **time and research**, not to pollution absorbed by nests. |
| **Combat: guns, ammo, turrets, walls** | [Techguns](https://www.curseforge.com/minecraft/mc-mods/techguns) weapons, ammo press and turrets (quests `Turrets [M991B]`, `Sentry Turrets [M991]`). Concrete serves as the wall because "Concrete is resistant to digging mobs" (quest `Concrete [M992]`). | `planned`. Covered broadly, with modern-guns flavour. |
| **Armor and the equipment grid** | Techguns armour tiers up to Power Armour and an Exo Suit (Military chapter). There is no modular equipment grid (inferred: no grid mod is in the manifest). | `planned`. Armour only, no grid. |
| **Capsules** | No Factorio-style throwables beyond Techguns grenades. The mod named "[Capsule](https://www.curseforge.com/minecraft/mc-mods/capsule)" is a build tool (see Construction below). | `planned`. |
| **Modules and beacons** | **Modules exist.** They are Modular Machinery *modifiers*: a coloured concrete-powder block placed in a machine's module slot changes its stats. Speed tiers give +20/30/50 % speed at +50/60/70 % RF/t. Productivity tiers give a +4/6/10 % chance not to consume inputs, at −15 % speed and +40/60/80 % RF/t. Efficiency tiers cut RF/t by 30/40/50 % (`overrides/config/modularmachinery/machinery/assembler_standard.json` `modifiers`; quests `Speed Modules [A042]`–`[A044C]`, `Module Upgrades`). **No beacons**: [Advanced Rocketry](https://www.curseforge.com/minecraft/mc-mods/advanced-rocketry)'s `beacon` is a waypoint block, and its recipe is removed (`scripts/_DisabledRecipes.zs`). | `blocked` for us (#120). **This is the one Factorio mechanic Manufactio implements that we have not argued.** It works as a speed/productivity/efficiency trade-off on a block in a structure, with productivity modelled as a chance not to consume inputs rather than as bonus output. |
| **Trains** | Minecarts, plus [Signals](https://www.curseforge.com/minecraft/mc-mods/signals) ("OpenTTD/Factorio style signaling": block and chain signals, station markers, cart engines) and [Better Railroads](https://www.curseforge.com/minecraft/mc-mods/better-railroads) steel rails (quests `Rail Automation [L031]`, `Rail Signals [L041]`, `Cart Engine [L042]`, `Extra Rails [L021]`). [Industrial Renewal](https://www.curseforge.com/minecraft/mc-mods/industrial-renewal) is also in the manifest. | `adapted` for us: Railcraft Reborn's locomotive, wagons and block and distant signals on vanilla rail, with schedules and stations on #435. Manufactio's signal semantics are Factorio's, but its vehicles are carts, not locomotives with wagons. |
| **Logistic robots** | [Suppergerrie2's Hauler Drone](https://www.curseforge.com/minecraft/mc-mods/suppergerrie2s-drone-mod) (quest `Robotics [A034]`) and Logistics Drones that interact through frames placed on inventories (quest `Logistics Drones [L024]`; inferred to be [PneumaticCraft](https://www.curseforge.com/minecraft/mc-mods/pneumaticcraft-repressurized)'s logistics drones and frames). The Mekanism Robit (`[A035]`) is also available. | `excluded` (ADR-0017). Manufactio includes a requester/provider-frame analogue. |
| **Logistic request and trash** | Not present (inferred: no personal-logistics mod). [AE2](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2) and [Logistics Pipes](https://www.curseforge.com/minecraft/mc-mods/logistics-pipes) provide network requests instead (`[L044]`, `[L033]`). | `excluded`. |
| **Construction robots and blueprints** | Not robots. Instead: Capsule captures a region and redeploys it, and "can also be upgraded into blueprints" (quest `Capsules`). Every multiblock is crafted as a Package that becomes a single-use deployable capsule (quest `Basic Automation`; `scripts/Capsule.zs`). There are also [RFTools](https://www.curseforge.com/minecraft/mc-mods/rftools) Scanner/Builder templates (`[N012]`) and [Building Gadgets](https://www.curseforge.com/minecraft/mc-mods/building-gadgets) (`[N011]`). | `excluded`. Manufactio has a working blueprint-paste analogue. |
| **Circuit network** | Redstone, [Project Red](https://www.curseforge.com/minecraft/mc-mods/project-red-integration) wiring (`Redstone Wire [A013]`), RFTools screens and wireless redstone (`[A015]`, `[A026]`), [ComputerCraft](https://www.curseforge.com/minecraft/mc-mods/computercraft) (`[C020]`). | `adapted`. Generic redstone, with no combinators. |
| **Rocket silo / launch** | Advanced Rocketry: build a rocket on a launch pad, fuel it and fly to the moon for Osmium and "Unusual Ore" (quests `To the moon! [S001]`, `Assembling the Rocket`, `Rocket Fuel`). The moon's ore feeds the Anomaly science pack. | `planned`. Manufactio treats the rocket as transport rather than as a win condition. It is closer to Space Age's travel than to base-game "launch a satellite". |
| **Interplanetary travel / space platforms** | Advanced Rocketry space stations, warp drive to other stars, orbital laser drill, railgun and space elevator (quests `[S012]`, `[S022]`, `[S041]`, `[S051]`, `[S052]`). | `blocked` for us. Manufactio has this through AR, with no Factorio platform mechanics. |
| **Personal transport** | [MrCrayfish's vehicles](https://www.curseforge.com/minecraft/mc-mods/mrcrayfishs-vehicle-mod): ATV and sports plane (quests `Engines [A023]`, `Flight [A036]`). Jumppack and jetpack (`[M904]`, `[M905]`). | `blocked` for us. Manufactio has a car analogue. |
| **Radar and map exploration** | [JourneyMap](https://www.curseforge.com/minecraft/mc-mods/journeymap). The Seismic Reader (`<mekanism:seismicreader>`, `scripts/Tier1.zs`) shows ore density per chunk (quest `Seismic Reader`). There is no radar block. | `adapted` (our Radar). |
| **Trees and wood** | [Dynamic Trees](https://www.curseforge.com/minecraft/mc-mods/dynamictrees): trees "grow and die over time, and will self seed" (quest `Trees & Seasons`). | `adapted`. |
| **Repair and entity damage** | Blocks damaged by an invasion repair automatically (`convertExplodedBlocksToRepairingBlocksDuringInvasions=true`, `InvasionConfig.cfg`). Turrets "auto-repair" in stand-down mode (quest `Turrets [M991B]`). | `blocked` for us. |
| **Recycling (Space Age)** | A Modular Machinery Recycler (quest `Material Recycling [P030]`; `scripts/Recycler.zs`, `recipes/rc/`). | `planned`. |
| **Toolbelt (inventory research)** | [Expandable Inventory](https://www.curseforge.com/minecraft/mc-mods/expandable-inventory) "Toolbelt Upgrade 1–7" adds inventory rows (Exotic Military & Tech chapter). | No row of its own in our ledger. Inferred: it corresponds to Factorio's Toolbelt technology. |
| **Day and night** | Vanilla, with torches removed: "Torches cannot be crafted… You need to research, build and power Lanterns" (quest `Making Light of a Dark situation`; `recipes.removeShaped(<minecraft:torch>)` in `Tier1.zs`). | `shipped`. Manufactio makes darkness a pressure, since hostiles spawn only in low light (`incontrol/spawn.json`). |

## 4. Factorio mechanics Manufactio does not implement

Each item below was checked against the manifest and the scripts. Absence is inferred from no mod
or script providing it.

- **Recipe-locked assemblers.** [Modular Machinery](https://www.curseforge.com/minecraft/mc-mods/modular-machinery) matches whatever inputs arrive.
- **Handcrafting queue.** Crafting is deliberately constrained instead.
- **Beacons.**
- **Belt lanes, belt tiers as throughput, underground belts and splitter semantics.** Logistical
  Automation has a splitter block, but nothing indicates two-lane belts.
- **Pollution-driven attacks.** There are no nests, no absorption by spawners and no expansion.
  Evolution is research-gated and time-gated instead.
- **Locomotives and cargo wagons as trains.** Minecarts with Factorio-style signals stand in.
- **Electric-pole supply areas.** Power goes only through wire connections (inferred from IE).
- **A combinator-based circuit network.**
- **A modular equipment grid.**
- **Factorio's personal logistics (request/trash slots).**
- **Factorio's actual numbers.** Recipes are rewritten "to be similar to Factorio, with more
  streamlined resource requirements" (quest `Introduction`), but no file indicates figures were
  taken from Factorio's data.
- **Space Age mechanics:** quality, spoilage, planet-specific mechanics, asteroid mining and space
  platforms as Factorio defines them.

## 5. Completely new: in Manufactio, not in Factorio

| feature | where | worth noting for us? |
| --- | --- | --- |
| Research-gated mob types ("UNLOCKS CREEPERS", "UNLOCKS HOSTILE DRONES") | `scripts/MobStages.zs`, quest texts | Possibly. It ties threat to progress. We chose pollution-absorption instead (ADR-0055). |
| Periodic invasions with a skip cost (sacrifice at an altar, 3 skips max) and auto-repair of damaged blocks | `config/HW_Invasions/InvasionConfig.cfg`, quest `Invasions` | No, as Factorio has its own model. |
| Multiblock "Packages" that unpack into a deployable capsule, so machines are autocraftable as one item | quests `Mining`, `Basic Automation`; `scripts/Capsule.zs` | **Yes.** It solves "a footprint machine is one item" in a similar way to our footprint seam (ADR-0077). |
| Drill heads as consumable, tiered drill parts | `config/F0Resources/drills.json` | No. |
| Torches removed; light must be researched and powered | `Tier1.zs`, quest `Making Light of a Dark situation` | Only as a design data point. |
| Seasons and rain affecting crops and polluted ground ([Serene Seasons](https://www.curseforge.com/minecraft/mc-mods/serene-seasons), [Hydrophobia](https://www.curseforge.com/minecraft/mc-mods/hydrophobia), [Forestry](https://www.curseforge.com/minecraft/mc-mods/forestry) Rainmaker `[C002]`) | quests `Trees & Seasons`, `Environmental Control [C002]` | No. |
| Challenge Science Pack giving creative items | `scripts/SciencePacks.zs`, chapter `Challenges` | No. |
| [ProjectE](https://www.curseforge.com/minecraft/mc-mods/projecte) EMC, a duplicator machine, portal gun, [Techguns](https://www.curseforge.com/minecraft/mc-mods/techguns) sci-fi weapons | quests `EMC Crafting [S003]`, `Duplicator [P029]`; manifest | No. These are off-theme for Factorio fidelity. |
| Void-world starts with starter quests handing out coal, hardened stone and lava | `scripts/VoidWorlds.zs`, `Introduction` quests | No. |
| Vanilla crafting tables uncraftable and found only in villages | `Tier1.zs`, quest `Crafting Tables` | No. |

## 6. Reception

**CurseForge**

- **Total downloads:** 160,585 (cfwidget `downloads.total`, and the CurseForge project page on the
  same day). **Comments:** 277. **Author followers:** 14. The page shows no project follower or like
  count (https://www.curseforge.com/minecraft/modpacks/manufactio, as read through WebFetch).
- **Per-file downloads.** The table gives cfwidget's exact numbers and the files tab's rounded ones.
  The two differ for 1.35 (20,850 vs 25.2K). Inferred: cfwidget's cache is stale.

| file | uploaded | downloads (cfwidget) | downloads (files tab) |
| --- | --- | --- | --- |
| Manufactio-1.35.zip | 2021-12-27 | 20,850 | 25.2K |
| Manufactio-1.34.zip | 2020-11-05 | 36,772 | 36.8K |
| Manufactio-1.33b.zip | 2020-08-09 | 9,516 | 9.5K |
| Manufactio-1.33.zip | 2020-08-01 | 1,867 | 1.8K |
| Manufactio-1.32b.zip | 2020-06-03 | 11,231 | 11.2K |
| Manufactio-1.32.zip | 2020-05-31 | 1,227 | 1.2K |
| Manufactio-1.31.zip | 2020-04-29 | 10,169 | 10.1K |
| Manufactio-1.30.zip | 2020-03-29 | 5,418 | 5.4K |

- **Sibling packs:** Manufactio 2 - Nuclear Edition has 28,431 downloads (cfwidget) or 28.4K (project
  page). Manufactio - Casual Mode has 8.7K (project page).

**Forum.** The FTB forum thread "Manufactio - Minecraft/Factorio hybrid" was started by Golrith on
2 July 2018 and runs to 18 pages
(https://forum.feed-the-beast.com/threads/manufactio-minecraft-factorio-hybrid.283615/). The
author's early posts describe:

- science packs as Thermal Foundation coins turned into research points in a lab;
- a mob-free start, with mobs unlocked as the tech tree advances;
- RF-powered lighting in place of torches;
- metal-chest minecarts for long-distance hauling.

**Twitch.** The CurseForge description lists direwolf20, hypnotizd, generikb, jessassin and soaryn
as "Recent/Current Twitch Streamers". No VOD or date was found, so this rests on the description
alone.

**YouTube.** Titles and channels come from YouTube's oEmbed endpoint and the watch pages. View counts
and publish dates were read from each watch page's `viewCount`/`publishDate` on 2026-09-27.

| video | channel | published | views | how found |
| --- | --- | --- | --- | --- |
| [Il ModPack PERFETTO? - ManuFactio](https://www.youtube.com/watch?v=b-N7RRE8Ags) (Italian) | MarcusKron | 2019-01-22 | 115,669 | embedded on the CF page |
| [Manufactio (Modded Minecraft) - Day 1](https://www.youtube.com/watch?v=eiqEln_JnB4) | HypnotizdLIVE | 2020-05-17 | 54,152 | web search |
| [Manufactio / "A First Look" / Part 01 [Modded Minecraft 1.12.2]](https://www.youtube.com/watch?v=I6Jx5mIw4oA) | SeriousCreeper | 2019-02-04 | 40,526 | web search |
| [Manufactio: 01 - A new beginning](https://www.youtube.com/watch?v=zRXgT2IcdE4) | Anakardian | 2019-02-11 | 23,042 | CF page |
| [Minecraft Manufactio Ep 1 - Factorio in Minecraft](https://www.youtube.com/watch?v=et54MM9qrTA) | Lensmanoz | 2019-05-24 | 17,880 | CF page |
| [Manufactio E01 - Factorio... in Minecraft?!](https://www.youtube.com/watch?v=OOEOdw4yvDo) | grayduster | 2020-04-27 | 13,371 | web search |
| [Zombie Hordes Galore (E01) - Getting started! - Manufactio](https://www.youtube.com/watch?v=CWjyNJ44r2U) | Jonny & Lawrence | 2020-06-17 | 12,666 | CF page |
| [Manufactio Starting Over! EP1](https://www.youtube.com/watch?v=hYi4cMOsclI) | Frogman79 | 2019-10-11 | 12,302 | web search |
| [Manufactio World Walkthrough](https://www.youtube.com/watch?v=VdTspwp7ZaU) | Frogman79 | 2019-02-14 | 10,177 | CF page ("Impressive Factory build") |
| [Lets Play Manufactio EP 1 - Starting Tips - Packaged Burner Drill and Pollution Guide - Diamonds!](https://www.youtube.com/watch?v=qxGu2pO-Urw) | Let's Play with D_Dae | 2020-09-06 | 9,509 | web search |
| [Jim in Manufactio Minecraft E02 - Laboratory Reboot](https://www.youtube.com/watch?v=g2hVCPQqy2c) | Jim MiningWorm | 2019-03-12 | 8,383 | CF page |
| [Manufactio Episode 1 Getting Started](https://www.youtube.com/watch?v=Z4r4v3b-ACE) | Anakardian | 2018-12-10 | 6,357 | web search |
| [Let's Play Manufactio Ep. 1 - MODDED MINECRAFT](https://www.youtube.com/watch?v=VnEeMUp9XWQ) | James | 2020-04-21 | 157 | web search |

Other observations:

- Nuclear Edition series also exist: [Manufactio NE EP1-Here we go again!](https://www.youtube.com/watch?v=YV6zTPaJb0E)
  by Frogman79 (2022-03-25, 2,926 views) and
  [Manufactio 2 Nuclear Edition Episode 1](https://www.youtube.com/watch?v=w7_K4SY7ZqI) by TheNimbleNinja
  (2021-10-11, 394 views).
- SeriousCreeper, who played the pack, is also the CurseForge author of [Polluted Earth Reborn](https://www.curseforge.com/minecraft/mc-mods/polluted-earth-reborn), one of
  the pack's mods (`modlist.html`). Inferred: the "Frogman Processor" research `[P044]` is named
  after the YouTuber Frogman79. Nothing states it.
- **Episode counts per series were not collected**, because only the listed videos were checked.

**Reddit.** Web searches for Manufactio on reddit.com returned nothing relevant, and Reddit's JSON
search endpoint returned a non-JSON response. No Reddit thread was found, which does not mean none
exists.

## 7. Sources

- CurseForge project page: https://www.curseforge.com/minecraft/modpacks/manufactio
- CurseForge files tab: https://www.curseforge.com/minecraft/modpacks/manufactio/files/all
- cfwidget API (project, files, per-file downloads): https://api.cfwidget.com/minecraft/modpacks/manufactio
- cfwidget API per mod (manifest file-ID resolution): `https://api.cfwidget.com/<projectID>` for each of the 146 project IDs
- cfwidget, Manufactio 2 - Nuclear Edition: https://api.cfwidget.com/minecraft/modpacks/manufactio-2-nuclear-edition
- Modpack zip 1.35: https://mediafilez.forgecdn.net/files/3582/169/Manufactio-1.35.zip. Files read: `manifest.json`,
  `modlist.html`, `overrides/scripts/{research,recipestages,SciencePacks,MobStages,Tier1,MM,Capsule,VoidWorlds,_DisabledRecipes}.zs`,
  `overrides/scripts/contenttweaker/{items,blocks}.zs`, `overrides/config/betterquesting/DefaultQuests.json`,
  `overrides/config/modularmachinery/{machinery,recipes}/`, `overrides/config/F0Resources/{ores,drills,fluids}.json`,
  `overrides/config/f0-resources.cfg`, `overrides/config/adpother/{Emitters,Pollutants}/`,
  `overrides/config/Polluted Earth.cfg`, `overrides/config/HW_Invasions/InvasionConfig.cfg`,
  `overrides/config/incontrol/spawn.json`, `overrides/config/cofh/world/`
- FTB forum thread: https://forum.feed-the-beast.com/threads/manufactio-minecraft-factorio-hybrid.283615/
- YouTube videos: the URLs in section 6. Metadata came from `https://www.youtube.com/oembed?url=…` and the watch pages.
- Web search results: modpackindex (https://www.modpackindex.com/modpack/5515/manufactio) and 9minecraft
  (https://www.9minecraft.net/manufactio-modpack/). Neither was used for any figure above.
- Our ledger: `/Users/kc00l/curseforge/Instances/PlanetaryFactory/docs/factorio-mechanics.md`
