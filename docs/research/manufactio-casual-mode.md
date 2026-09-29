# Manufactio - Casual Mode: the original without pollution or siege, compared

Researched 2026-09-27 for a comparison with FactoryWorks, as a companion to
`docs/research/manufactio.md` (the original pack), which this file leans on for every mechanic
Casual Mode did not change. Where a claim comes from inside the modpack zip, the citation is a path
inside `Manufactio - CM-1.31.zip` (CurseForge file 4744184, downloaded from
`https://mediafilez.forgecdn.net/files/4744/184/Manufactio%20-%20CM-1.31.zip`). The overrides sit
under `overrides/`. The comparison with the original is against `Manufactio-1.35.zip` (file 3582169),
by `diff -rq` of the two `overrides/` trees and by project ID across the two manifests. Our verdicts
are quoted from `docs/factorio-mechanics.md` as of this date. **Inferred** marks anything not stated
by a source.

## 1. Overview

| field | value | source |
| --- | --- | --- |
| Name | Manufactio - Casual Mode (`"name": "Manufactio - CM"` in the manifest) | https://www.curseforge.com/minecraft/modpacks/manufactio-casual-mode, `manifest.json` |
| Author | Golrith (CurseForge owner); `"author": "golrith"` in the manifest | https://api.cfwidget.com/373441, `manifest.json` |
| CurseForge project ID | 373441 | cfwidget, CurseForge project page |
| Minecraft / loader | 1.12.2, Forge 14.23.5.2847 | `manifest.json` (`forge-14.23.5.2847`) |
| Project created | 2020-04-06 | cfwidget `created_at` |
| Latest version | 1.31 (`Manufactio - CM-1.31.zip`, 2023-09-08) | cfwidget; https://www.curseforge.com/minecraft/modpacks/manufactio-casual-mode/files/all |
| Mods in the manifest | 139 | `manifest.json` (139 `files`) |
| Categories | Quests, Tech | cfwidget |
| License | All Rights Reserved | CurseForge files tab (via WebFetch) |
| Summary | "Inspired by Factorio, this pack brings the Factorio style gameplay to Minecraft but more casually." | cfwidget `summary` |

**Description summary** (CurseForge description via cfwidget). It is "the Manufactio modpack, but
for a more casual playthrough", linking the original. It repeats the original's pitch (science packs
into research points, a rocket to the moons, Mekanism "nanotechnology" and EMC) and then lists what
is different:

- no enhanced mob AI ("X-Ray vision", "Digging", "Building", "Proximity Fuse", "Hordes");
- no pollution;
- mobs are not stage-locked;
- mobs are still blocked from spawning on grass and other blocks;
- it is "24/7 server friendly, no config adjustments required".

It embeds five of the original's playthroughs (Anakardian, MarcusKron, Frogman79, Jim MiningWorm,
Lensmanoz; video IDs in the cfwidget `description`).

**Only two files exist.** 1.30 (2020-04-05) and 1.31 (2023-09-08). 1.31 came out 21 months after the
original's last release (1.35, 2021-12-27). Inferred: 1.31 is built from the original's 1.35 rather
than from its own 1.30. Of the 138 mods the two manifests share, 135 are at the same file, and every
override that differs is a pollution, invasion or mob-stage edit (section 6).

**How the pack is built.** Exactly as the original: CraftTweaker/ModTweaker scripts, Game Stages with
Item and MultiBlock Stages, Better Questing (369 quests in
`overrides/config/betterquesting/DefaultQuests.json`) and Modular Machinery. The original report's
section 1 applies unchanged.

## 2. Full mod list

All 139 entries in `manifest.json`. Each project's title, CurseForge URL and the file name for its
`fileID` come from `https://api.cfwidget.com/<projectID>`; the names match `modlist.html`. The
"Role" column copies the original report's pack role for every mod that has one there. That is
sound because the files it cites are byte-identical in the two zips: `diff -rq` of the two
`overrides/` trees lists none of them. For every other mod, the column quotes the mod's CurseForge
summary.

| # | Mod (CurseForge project ID) | File in manifest | Role |
| --- | --- | --- | --- |
| 1 | [Additional Resources](https://www.curseforge.com/minecraft/mc-mods/additional-resources) (225124) | `additionalresources-1.9.4-0.2.0.28+47cd0bd_signed.jar` | CF summary: Add loose resources to the game without needing the user to install a texture pack themselves. |
| 2 | [Advanced Rocketry](https://www.curseforge.com/minecraft/mc-mods/advanced-rocketry) (236542) | `AdvancedRocketry-1.12.2-1.7.0-232-universal.jar` | **Pack role:** rocket to the moon, space stations, warp (Space and Beyond chapter) |
| 3 | [AppleSkin](https://www.curseforge.com/minecraft/mc-mods/appleskin) (248787) | `AppleSkin-mc1.12-1.0.14.jar` | CF summary: Adds some useful information about food/hunger to the HUD |
| 4 | [Applied Energistics 2](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2) (223794) | `appliedenergistics2-rv6-stable-7.jar` | **Pack role:** digital storage and autocrafting (L032, L044) |
| 5 | [ArchitectureCraft - TridentMC Version](https://www.curseforge.com/minecraft/mc-mods/architecturecraft-tridev) (277631) | `architecturecraft-1.12-3.98.jar` | CF summary: Distinguished architectural features for your Minecraft buildings. Ported to 1.12 |
| 6 | [Aroma1997Core](https://www.curseforge.com/minecraft/mc-mods/aroma1997core) (223735) | `Aroma1997Core-1.12.2-2.0.0.2.b167.jar` | CF summary: This is a mod required by most of my other mods, so you should install it, if you want to run any other of my mods. |
| 7 | [AromaBackup](https://www.curseforge.com/minecraft/mc-mods/aromabackup) (225658) | `AromaBackup-1.12.2-3.0.0.0.b135.jar` | CF summary: IT DOES BACKUPS!!! (Works in SSP and SMP) |
| 8 | [AutoRegLib](https://www.curseforge.com/minecraft/mc-mods/autoreglib) (250363) | `AutoRegLib-1.3-32.jar` | CF summary: A library to ease menial tasks in mod development. |
| 9 | [B.A.S.E](https://www.curseforge.com/minecraft/mc-mods/base) (246996) | `base-1.12.2-3.13.0.jar` | CF summary: Main Library for The Acronym Coders Mod Projects |
| 10 | [Baubles](https://www.curseforge.com/minecraft/mc-mods/baubles) (227083) | `Baubles-1.12-1.5.2.jar` | CF summary: An addon module and API for Thaumcraft |
| 11 | [BdLib](https://www.curseforge.com/minecraft/mc-mods/bdlib) (70496) | `bdlib-1.14.3.12-mc1.12.2.jar` | CF summary: A library of generic code for my other mods |
| 12 | [Better Boilers](https://www.curseforge.com/minecraft/mc-mods/better-boilers) (284383) | `BetterBoilers-1.2.jar` | **Pack role:** multiblock boiler for steam power |
| 13 | [Better Foliage](https://www.curseforge.com/minecraft/mc-mods/better-foliage) (228529) | `BetterFoliage-MC1.12-2.3.3.jar` | CF summary: Makes the vegetation in your world more natural and awesome |
| 14 | [Better Questing](https://www.curseforge.com/minecraft/mc-mods/better-questing) (238856) | `BetterQuesting-3.5.299.jar` | **Pack role:** research tree: each research is a quest consuming research points and granting a game stage (`config/betterquesting/DefaultQuests.json`) |
| 15 | [Better Questing - Quest Book](https://www.curseforge.com/minecraft/mc-mods/better-questing-quest-book) (242106) | `questbook-3.1.1-1.12.jar` | CF summary: Adds a quest book to Funwayguy's Better Questing Mod |
| 16 | [Better Questing - Standard Expansion](https://www.curseforge.com/minecraft/mc-mods/better-questing-standard-expansion) (238857) | `StandardExpansion-3.4.159.jar` | CF summary: Standard tasks, rewards, importers and themes for BetterQuesting |
| 17 | [Better Railroads](https://www.curseforge.com/minecraft/mc-mods/better-railroads) (285047) | `Better-Railroads-1.0.0.jar` | CF summary: A mod for better utilizing rails and carts |
| 18 | [BnBGamingCore](https://www.curseforge.com/minecraft/mc-mods/bnbgamingcore) (274341) | `BNBGamingCore-1.12.2-0.12.0.jar` | CF summary: Core Mod containing all ASM for BnBGaming Mods |
| 19 | [BNBGamingLib](https://www.curseforge.com/minecraft/mc-mods/bnbgaminglib) (229587) | `BNBGamingLib-1.12.2-2.17.6.jar` | CF summary: Library for Blood N Bones Gaming mods |
| 20 | [Bookshelf](https://www.curseforge.com/minecraft/mc-mods/bookshelf) (228525) | `Bookshelf-1.12.2-2.3.590.jar` | CF summary: An open source library for other mods! |
| 21 | [Building Gadgets](https://www.curseforge.com/minecraft/mc-mods/building-gadgets) (298187) | `BuildingGadgets-2.8.4.jar` | CF summary: A collection of Gadgets to make building large structures a little bit easier! |
| 22 | [Capsule](https://www.curseforge.com/minecraft/mc-mods/capsule) (235338) | `Capsule-1.12.2-3.4.76.jar` | **Pack role:** machine "packages", deployable capsules and blueprints (`scripts/Capsule.zs`) |
| 23 | [CB Multipart](https://www.curseforge.com/minecraft/mc-mods/cb-multipart) (258426) | `ForgeMultipart-1.12.2-2.6.2.83-universal.jar` | CF summary: An opensource library for having multiple things in the one block space |
| 24 | [Chicken Chunks 1.8.+](https://www.curseforge.com/minecraft/mc-mods/chicken-chunks-1-8) (243883) | `ChickenChunks-1.12.2-2.4.2.74-universal.jar` | CF summary: Chunk load all the chunks with the power of chicken! |
| 25 | [Chisel](https://www.curseforge.com/minecraft/mc-mods/chisel) (235279) | `Chisel-MC1.12.2-1.0.2.45.jar` | CF summary: A builder's best friend. |
| 26 | [Clumps](https://www.curseforge.com/minecraft/mc-mods/clumps) (256717) | `Clumps-3.1.2.jar` | CF summary: Clumps XP orbs together to reduce lag |
| 27 | [CodeChicken Lib 1.8.+](https://www.curseforge.com/minecraft/mc-mods/codechicken-lib-1-8) (242818) | `CodeChickenLib-1.12.2-3.2.3.358-universal.jar` | CF summary: Contains various tools to make modding easier. |
| 28 | [CoFH Core](https://www.curseforge.com/minecraft/mc-mods/cofh-core) (69162) | `CoFHCore-1.12.2-4.6.6.1-universal.jar` | CF summary: Contains Core Functionality for all Team CoFH mods. Also does some really cool stuff on its own! |
| 29 | [CoFH World](https://www.curseforge.com/minecraft/mc-mods/cofh-world) (271384) | `CoFHWorld-1.12.2-1.4.0.1-universal.jar` | CF summary: Customizable and powerful world generation! |
| 30 | [Compacter](https://www.curseforge.com/minecraft/mc-mods/compacter) (231549) | `compacter-1.3.0.3-mc1.12.2.jar` | CF summary: Rapidly compacts items that can be crafted in 2x2 or 3x3 pattern |
| 31 | [ComputerCraft](https://www.curseforge.com/minecraft/mc-mods/computercraft) (67504) | `ComputerCraft1.80pr1.jar` | CF summary: Computers, Programming and Robotics in Minecraft |
| 32 | [ConnectedTexturesMod](https://www.curseforge.com/minecraft/mc-mods/ctm) (267602) | `CTM-MC1.12.2-1.0.2.31.jar` | CF summary: A resource pack extension library |
| 33 | [ContentTweaker](https://www.curseforge.com/minecraft/mc-mods/contenttweaker) (237065) | `ContentTweaker-1.12.2-4.9.1.jar` | **Pack role:** research items, packages, filter blocks (`scripts/contenttweaker/`) |
| 34 | [Controlling](https://www.curseforge.com/minecraft/mc-mods/controlling) (250398) | `Controlling-3.0.10.jar` | CF summary: Adds a search bar to the Key-Bindings menu |
| 35 | [CoroUtil](https://www.curseforge.com/minecraft/mc-mods/coroutil) (237749) | `coroutil-1.12.1-1.2.37.jar` | CF summary: Library Mod for Weather/Tropicraft/ZombieAwareness/etc |
| 36 | [CraftTweaker](https://www.curseforge.com/minecraft/mc-mods/crafttweaker) (239197) | `CraftTweaker2-1.12-4.1.20.586.jar` | **Pack role:** all recipe/stage scripts (`overrides/scripts/*.zs`) |
| 37 | [CreativeCore](https://www.curseforge.com/minecraft/mc-mods/creativecore) (257814) | `CreativeCore_v1.10.16_mc1.12.2.jar` | CF summary: a core mod |
| 38 | [Custom Main Menu](https://www.curseforge.com/minecraft/mc-mods/custom-main-menu) (226406) | `CustomMainMenu-MC1.12.2-2.0.9.1.jar` | CF summary: Allows you to modify the main menu using a simple json file |
| 39 | [DesirePaths](https://www.curseforge.com/minecraft/mc-mods/desirepaths) (307301) | `desirepaths-1.12.1-1.2.8.jar` | CF summary: Wears down grass as its walked on to create naturally formed dirt paths |
| 40 | [Diet Hoppers](https://www.curseforge.com/minecraft/mc-mods/diet-hoppers) (278385) | `diethopper-1.1.jar` | CF summary: Tightens the selection box of the hopper to allow you to access visible blocks behind them. |
| 41 | [Drones](https://www.curseforge.com/minecraft/mc-mods/drones) (263438) | `Drones-0.3.0.jar` | CF summary: A Mod to add Drones Mobs to the world |
| 42 | [Dynamic Surroundings](https://www.curseforge.com/minecraft/mc-mods/dynamic-surroundings) (238891) | `DynamicSurroundings-1.12.2-3.6.1.0.jar` | CF summary: Alters the fabric of Minecraft experience by weaving a tapestry of sound and visual effects |
| 43 | [Dynamic Trees](https://www.curseforge.com/minecraft/mc-mods/dynamictrees) (252818) | `DynamicTrees-1.12.2-0.9.20.jar` | **Pack role:** trees grow, die and reseed (Introduction quest) |
| 44 | [Entity Culling](https://www.curseforge.com/minecraft/mc-mods/entity-culling) (409087) | `EntityCulling-1.12.2-4.2.5.jar` | CF summary: Skip rendering of hidden entities |
| 45 | [Expandable Inventory](https://www.curseforge.com/minecraft/mc-mods/expandable-inventory) (304371) | `ExpandableInventory-1.12.2-1.4.0.jar` | **Pack role:** "Toolbelt Upgrade" inventory rows (Exotic Military & Tech chapter) |
| 46 | [Extreme Reactors](https://www.curseforge.com/minecraft/mc-mods/extreme-reactors) (250277) | `ExtremeReactors-1.12.2-0.4.5.50.jar` | **Pack role:** Nuclear Reactor (R033) |
| 47 | [Factory0-Resources](https://www.curseforge.com/minecraft/mc-mods/factory0-resources) (299713) | `Factory0-Resources-1.12.2-1.0.3.1.jar` | **Pack role:** Burner/Electric Drill multiblock mining finite per-chunk ore amounts; oil pump (`config/F0Resources/*.json`, `config/f0-resources.cfg`) |
| 48 | [Fast Leaf Decay](https://www.curseforge.com/minecraft/mc-mods/fast-leaf-decay) (230976) | `FastLeafDecay-v14.jar` | CF summary: Makes leaves decay faster when a tree is cut down. |
| 49 | [FindMe](https://www.curseforge.com/minecraft/mc-mods/findme) (291936) | `findme-1.12.2-1.1.0-8.jar` | CF summary: Search for an item in nearby inventories |
| 50 | [Foam​Fix](https://www.curseforge.com/minecraft/mc-mods/foamfix-optimization-mod) (278494) | `foamfix-0.10.10-1.12.2.jar` | CF summary: Simple, targetted optimizations for a popular block game |
| 51 | [Forestry](https://www.curseforge.com/minecraft/mc-mods/forestry) (59751) | `forestry_1.12.2-5.8.2.422.jar` | CF summary: Bringing bees, butterflies and more trees. as well as eco-friendly energy production and mail to Minecraft. |
| 52 | [ForgeEndertech](https://www.curseforge.com/minecraft/mc-mods/forgeendertech) (244844) | `ForgeEndertech-1.12.2-4.5.2.0-build.0459.jar` | CF summary: Core library powering Endertech mods |
| 53 | [Game Stages](https://www.curseforge.com/minecraft/mc-mods/game-stages) (268655) | `GameStages-1.12.2-2.0.123.jar` | **Pack role:** research unlock state (stage IDs such as A002, L011) |
| 54 | [GraveStone Mod](https://www.curseforge.com/minecraft/mc-mods/gravestone-mod) (238551) | `gravestone-1.10.3.jar` | CF summary: Places a gravestone with your inventory items inside when you die |
| 55 | [Guide-API](https://www.curseforge.com/minecraft/mc-mods/guide-api) (228832) | `Guide-API-1.12-2.1.8-63.jar` | CF summary: Simple library mod for in-game guide creation |
| 56 | [HammerLib](https://www.curseforge.com/minecraft/mc-mods/hammer-lib) (247401) | `HammerCore-1.12.2-2.0.5.7.jar` | CF summary: Library used by all of DragonForge team's mods. |
| 57 | [Hooked](https://www.curseforge.com/minecraft/mc-mods/hooked) (297209) | `hooked-1.0.3.jar` | CF summary: Grappling hooks done simple, natural, and useful. |
| 58 | [Hwyla](https://www.curseforge.com/minecraft/mc-mods/hwyla) (253449) | `Hwyla-1.8.26-B41_1.12.2.jar` | CF summary: Hwyla (Here's What You're Looking At) is a UI improvement mod aimed at providing block information directly in-game. It  |
| 59 | [Hydrophobia](https://www.curseforge.com/minecraft/mc-mods/hydrophobia) (273084) | `hydrophobia-1.0.4c.jar` | CF summary: Don't let it rain on your parade... |
| 60 | [iChunUtil](https://www.curseforge.com/minecraft/mc-mods/ichunutil) (229060) | `iChunUtil-1.12.2-7.2.2.jar` | CF summary: Shared library used by iChun's mods |
| 61 | [Immersive Engineering](https://www.curseforge.com/minecraft/mc-mods/immersive-engineering) (231951) | `ImmersiveEngineering-0.12-98.jar` | **Pack role:** conveyors, LV/MV/HV wire network, blast furnace, excavator, science-pack blueprints (`scripts/SciencePacks.zs`) |
| 62 | [Immersive Petroleum](https://www.curseforge.com/minecraft/mc-mods/immersive-petroleum) (268250) | `immersivepetroleum-1.12.2-1.1.9.jar` | **Pack role:** pumpjack, distillation tower (P033, P038) |
| 63 | [Immersive Posts](https://www.curseforge.com/minecraft/mc-mods/immersiveposts) (314645) | `ImmersivePosts-0.0.5.jar` | CF summary: Extendable treated-wood, aluminium, steel posts and more for ImmersiveEngineering. |
| 64 | [In Control!](https://www.curseforge.com/minecraft/mc-mods/in-control) (257356) | `incontrol-1.12-3.9.17.jar` | **Pack role:** no hostile spawns on grass/dirt/sand/concrete or in light (`config/incontrol/spawn.json`) |
| 65 | [Industrial Renewal](https://www.curseforge.com/minecraft/mc-mods/industrial-renewal) (299849) | `IndustrialRenewal_1.12.2-0.17.6.jar` | CF summary: Industrial objects to minecraft |
| 66 | [Insomniac](https://www.curseforge.com/minecraft/mc-mods/insomniac) (270193) | `insomniac-1.6.jar` | CF summary: Disables sleeping, but you can configure if beds still sets your spawnpoint or not. |
| 67 | [Inventory Tweaks [1.12 only]](https://www.curseforge.com/minecraft/mc-mods/inventory-tweaks) (223094) | `InventoryTweaks-1.64+dev.151.jar` | CF summary: Tweaks to inventory handling for ease of use, including sorting and automatic replacement of broken tools or exhausted s |
| 68 | [Item Stages](https://www.curseforge.com/minecraft/mc-mods/item-stages) (280316) | `ItemStages-1.12.2-2.0.49.jar` | **Pack role:** hides/locks items until their research stage (`scripts/research.zs`) |
| 69 | [JourneyMap](https://www.curseforge.com/minecraft/mc-mods/journeymap) (32274) | `journeymap-1.12.2-5.7.1.jar` | CF summary: Real-time map used for mapping in-game or your browser as you explore. |
| 70 | [Just Enough Calculation](https://www.curseforge.com/minecraft/mc-mods/just-enough-calculation) (242223) | `JustEnoughCalculation-1.12.2-3.2.7.jar` | CF summary: A simple mod help you calculate the cost for recipes |
| 71 | [Just Enough Items (JEI)](https://www.curseforge.com/minecraft/mc-mods/jei) (238222) | `jei_1.12.2-4.16.1.302.jar` | CF summary: View Items and Recipes |
| 72 | [LibrarianLib](https://www.curseforge.com/minecraft/mc-mods/librarianlib) (252910) | `librarianlib-1.12.2-4.22.jar` | CF summary: An extensive collection of tools, utilities, and frameworks. |
| 73 | [LibVulpes](https://www.curseforge.com/minecraft/mc-mods/libvulpes) (236541) | `LibVulpes-1.12.2-0.4.2-75-universal.jar` | CF summary: Common Library required for my mods |
| 74 | [LLibrary](https://www.curseforge.com/minecraft/mc-mods/llibrary) (243298) | `llibrary-1.7.19-1.12.2.jar` | CF summary: The lightweight Minecraft modding library |
| 75 | [Logistical Automation](https://www.curseforge.com/minecraft/mc-mods/logistical-automation) (283037) | `logisticalautomation-0.1.0.jar` | **Pack role:** faster conveyors, splitter, junction (`scripts/research.zs`) |
| 76 | [Logistics Pipes](https://www.curseforge.com/minecraft/mc-mods/logistics-pipes) (232838) | `logisticspipes-0.10.3.114.jar` | CF summary: Logistics Pipes is an extensive overhaul of the Buildcraft pipe system. It allows for better distribution of items via p |
| 77 | [LootBags](https://www.curseforge.com/minecraft/mc-mods/lootbags) (225946) | `LootBags-1.12.2-2.5.8.5.jar` | CF summary: Adds bags of loot to mob and dungeon drops |
| 78 | [LootTableTweaker](https://www.curseforge.com/minecraft/mc-mods/loottabletweaker) (261339) | `LootTableTweaker-1.12.2-1.1.14.jar` | CF summary: LootTable support for CraftTweaker / MineTweaker3 |
| 79 | [LunatriusCore](https://www.curseforge.com/minecraft/mc-mods/lunatriuscore) (225605) | `LunatriusCore-1.12.2-1.2.0.42-universal.jar` | CF summary: A (small) compilation of utility classes used in Lunatrius' mods. |
| 80 | [Magneticraft](https://www.curseforge.com/minecraft/mc-mods/magneticraft) (224808) | `Magneticraft_1.12-2.8.3-dev.jar` | **Pack role:** the Inserter (`magneticraft:inserter`) and a back-stuffing conveyor |
| 81 | [MalisisCore](https://www.curseforge.com/minecraft/mc-mods/malisiscore) (223896) | `malisiscore-1.12.2-6.5.1.jar` | CF summary: Framework over Forge. |
| 82 | [MalisisDoors](https://www.curseforge.com/minecraft/mc-mods/malisisdoors) (223891) | `malisisdoors-1.12.2-7.3.0.jar` | CF summary: New animations for doors. You can build you own doors with millions of possibilities! |
| 83 | [Mantle](https://www.curseforge.com/minecraft/mc-mods/mantle) (74924) | `Mantle-1.12-1.3.3.55.jar` | CF summary: Shared code for Forge mods |
| 84 | [McJtyLib](https://www.curseforge.com/minecraft/mc-mods/mcjtylib) (233105) | `mcjtylib-1.12-3.5.4.jar` | CF summary: Companion mod for all mods by McJty (RFTools, XNet, Deep Resonance, Not Enough Wands, SignTastic, ...)) |
| 85 | [Mekanism](https://www.curseforge.com/minecraft/mc-mods/mekanism) (268560) | `Mekanism-1.12.2-9.8.3.390.jar` | **Pack role:** "Nanotechnology" single-block machines, ore x3-x5, fusion (Nanotechnology chapter) |
| 86 | [Mekanism Generators](https://www.curseforge.com/minecraft/mc-mods/mekanism-generators) (268566) | `MekanismGenerators-1.12.2-9.8.3.390.jar` | CF summary: Advanced energy generation for Mekanism. |
| 87 | [Metal Chests](https://www.curseforge.com/minecraft/mc-mods/metalchests) (290145) | `MetalChests-v6.1.0+mc1.12.2.jar` | CF summary: The better alternative to Iron Chests |
| 88 | [MixinBootstrap](https://www.curseforge.com/minecraft/mc-mods/mixinbootstrap) (357178) | `MixinBootstrap-1.0.5.jar` | CF summary: MixinBootstrap is a temporary way of booting Mixin in a MinecraftForge production environment. |
| 89 | [Mo' Bends](https://www.curseforge.com/minecraft/mc-mods/mo-bends) (231347) | `MoBends_1.12.2-1.2.1-19.12.21.jar` | CF summary: Changes the look of players and mobs to act more realistic and epic. |
| 90 | [Mob Grinding Utils](https://www.curseforge.com/minecraft/mc-mods/mob-grinding-utils) (254241) | `MobGrindingUtils-0.3.13.jar` | CF summary: A Mod that adds fun ways to make mob farms |
| 91 | [Mob Sunscreen](https://www.curseforge.com/minecraft/mc-mods/mob-sunscreen) (298408) | `mobsunscreen-1.12.2-2.1.3.jar` | CF summary: Stop Zombies and Skeletons from Burning in the Sun! |
| 92 | [ModelLoader](https://www.curseforge.com/minecraft/mc-mods/modelloader) (277663) | `modelloader-1.1.7.jar` | CF summary: A small library to load .mcx model format |
| 93 | [ModTweaker](https://www.curseforge.com/minecraft/mc-mods/modtweaker) (220954) | `modtweaker-4.0.18.jar` | CF summary: ModTweaker is an addon for CraftTweaker, a recipe manipulator utility for Minecraft. It allows you to modify the recipes |
| 94 | [Modular Machinery](https://www.curseforge.com/minecraft/mc-mods/modular-machinery) (270790) | `modularmachinery-1.12.2-1.11.1.jar` | **Pack role:** every custom multiblock: Assemblers, Laboratory, Metal Former, Electric Furnace, Oil Refinery, Chemical Plant, Steam Generator, Recycler; modules as structure modifiers (`config/modularmachinery/`) |
| 95 | [Mouse Tweaks](https://www.curseforge.com/minecraft/mc-mods/mouse-tweaks) (60089) | `MouseTweaks-2.10-mc1.12.2.jar` | CF summary: Enhances inventory management by adding various functions to the mouse buttons. |
| 96 | [Mowzie's Mobs](https://www.curseforge.com/minecraft/mc-mods/mowzies-mobs) (250498) | `mowziesmobs-1.5.8.jar` | CF summary: Powerful overworld enemies and more! |
| 97 | [MrCrayfish's Vehicle Mod](https://www.curseforge.com/minecraft/mc-mods/mrcrayfishs-vehicle-mod) (286660) | `vehicle-mod-0.38.1-1.12.2.jar` | CF summary: Adds in vehicles that you can drive! |
| 98 | [MrTJPCore](https://www.curseforge.com/minecraft/mc-mods/mrtjpcore) (229002) | `MrTJPCore-1.12.2-2.1.4.43-universal.jar` | CF summary: Miscellaneous utilities for all my mods |
| 99 | [MTLib](https://www.curseforge.com/minecraft/mc-mods/mtlib) (253211) | `MTLib-3.0.6.jar` | CF summary: Library files for Minetweaker Addons |
| 100 | [MultiBlock Stages](https://www.curseforge.com/minecraft/mc-mods/multiblock-stages) (284139) | `multiblockstages-1.2.0.jar` | **Pack role:** locks IE multiblocks behind research (`scripts/research.zs`) |
| 101 | [Not Enough Items 1.8.+](https://www.curseforge.com/minecraft/mc-mods/not-enough-items-1-8) (247694) | `NotEnoughItems-1.12.2-2.4.3.245-universal.jar` | CF summary: Recipe Viewer, Inventory Manager, Item Spawner, Cheats and more |
| 102 | [Obfuscate](https://www.curseforge.com/minecraft/mc-mods/obfuscate) (289380) | `obfuscate-0.2.6-1.12.2.jar` | CF summary: A library that adds in useful events and utilities |
| 103 | [OnlinePictureFrame](https://www.curseforge.com/minecraft/mc-mods/onlinepictureframe) (257815) | `OnlinePicFrame_v1.4.40_mc1.12.2.jar` | CF summary: add pictures directly from the internet to the game |
| 104 | [OpenBlocks](https://www.curseforge.com/minecraft/mc-mods/openblocks) (228816) | `OpenBlocks-1.12.2-1.8.1.jar` | CF summary: Random collection of blocks. Some of which aren't blocks at all. |
| 105 | [OpenModsLib](https://www.curseforge.com/minecraft/mc-mods/openmodslib) (228815) | `OpenModsLib-1.12.2-0.12.2.jar` | CF summary: Common base used by OpenBlocks and OpenPeripheral |
| 106 | [OreLib](https://www.curseforge.com/minecraft/mc-mods/orelib) (307806) | `OreLib-1.12.2-3.6.0.1.jar` | CF summary: Support library for OreCruncher's mods |
| 107 | [Phosphor (Forge)](https://www.curseforge.com/minecraft/mc-mods/phosphor-forge) (318255) | `phosphor-1.12.2-0.2.6+build50-universal.jar` | CF summary: Performance improvements for Minecraft's lighting engine |
| 108 | [Placebo](https://www.curseforge.com/minecraft/mc-mods/placebo) (283644) | `Placebo-1.12.2-1.6.0.jar` | CF summary: A library mod |
| 109 | [PneumaticCraft: Repressurized](https://www.curseforge.com/minecraft/mc-mods/pneumaticcraft-repressurized) (281849) | `pneumaticcraft-repressurized-1.12.2-0.11.15-398.jar` | **Pack role:** Logistics Drones with frames (L024) (inferred from quest text) |
| 110 | [Portal Gun](https://www.curseforge.com/minecraft/mc-mods/portal-gun) (229084) | `PortalGun-1.12.2-7.1.0.jar` | CF summary: This mod adds the Portal Gun, as well as several other portal-related aspects, to Minecraft! |
| 111 | [Project Red - Core](https://www.curseforge.com/minecraft/mc-mods/project-red-core) (228702) | `ProjectRed-1.12.2-4.9.4.120-Base.jar` | CF summary: Core module for the Project Red series |
| 112 | [Project Red - Exploration](https://www.curseforge.com/minecraft/mc-mods/project-red-exploration) (229049) | `ProjectRed-1.12.2-4.9.4.120-world.jar` | CF summary: Exploration module for the Project Red series |
| 113 | [Project Red - Integration](https://www.curseforge.com/minecraft/mc-mods/project-red-integration) (229045) | `ProjectRed-1.12.2-4.9.4.120-integration.jar` | CF summary: Integration module for the Project Red series |
| 114 | [ProjectE](https://www.curseforge.com/minecraft/mc-mods/projecte) (226410) | `ProjectE-1.12.2-PE1.4.1.jar` | **Pack role:** EMC crafting late game (S003, S004) |
| 115 | [Quark](https://www.curseforge.com/minecraft/mc-mods/quark) (243121) | `Quark-r1.6-179.jar` | CF summary: A Quark is a very small thing. This mod is a collection of small things that improve the vanilla minecraft experience. |
| 116 | [Quest Utils](https://www.curseforge.com/minecraft/mc-mods/quest-utils) (291674) | `questutils-0.4.0.jar` | CF summary: A mod that helps modpacks based on objectives and/or quests |
| 117 | [ReAuth](https://www.curseforge.com/minecraft/mc-mods/reauth) (237701) | `reauth-3.6.0.jar` | CF summary: Fixes the Problem of having to restart your Client when your Session invalidates |
| 118 | [Redstone Flux](https://www.curseforge.com/minecraft/mc-mods/redstone-flux) (270789) | `RedstoneFlux-1.12-2.1.1.1-universal.jar` | CF summary: Redstone Flux API - Energy Transfer in Minecraft. |
| 119 | [Resource Loader](https://www.curseforge.com/minecraft/mc-mods/resource-loader) (226447) | `ResourceLoader-MC1.12.1-1.5.3.jar` | CF summary: A small mod that allows users to add their own resources to minecraft without making a resource pack |
| 120 | [RFTools](https://www.curseforge.com/minecraft/mc-mods/rftools) (224641) | `rftools-1.12-7.73.jar` | CF summary: RFTools, blocks and items to help with Redflux (dimension builder, crafter, monitor, scanner, ...) |
| 121 | [Ruins (Structure Spawning System)](https://www.curseforge.com/minecraft/mc-mods/ruins-structure-spawning-system) (227873) | `Ruins-1.12.2.jar` | CF summary: A structure spawning system |
| 122 | [Serene Seasons](https://www.curseforge.com/minecraft/mc-mods/serene-seasons) (291874) | `SereneSeasons-1.12.2-1.2.18-universal.jar` | **Pack role:** seasons gate crop growth (Introduction quest) |
| 123 | [Shadowfacts' Forgelin](https://www.curseforge.com/minecraft/mc-mods/shadowfacts-forgelin) (248453) | `Forgelin-1.8.4.jar` | CF summary: My fork of Emberwalker's Forgelin, with some sprinkles on top. |
| 124 | [Signals](https://www.curseforge.com/minecraft/mc-mods/signals) (245824) | `Signals-1.12.2-1.4.1-30-universal.jar` | **Pack role:** Factorio-style block/chain rail signals and station markers (L031, L041) |
| 125 | [Simple Magnet](https://www.curseforge.com/minecraft/mc-mods/simple-magnet) (245060) | `simplemagnet-1.12.2-1.3.12.jar` | CF summary: A simple item magnet mod |
| 126 | [SimpleHarvest](https://www.curseforge.com/minecraft/mc-mods/simpleharvest) (240783) | `Harvest-1.12-1.2.8-25.jar` | CF summary: Right click crop harvesting |
| 127 | [Single Spot Chest](https://www.curseforge.com/minecraft/mc-mods/single-spot-chest) (355166) | `SingleSpotChest-1.12.2-1.1.jar` | CF summary: A chest type block with a single slot for items |
| 128 | [SpawnTableTweaker](https://www.curseforge.com/minecraft/mc-mods/spawntabletweaker) (316840) | `spawntabletweaker-1.0.jar` | CF summary: Simply crafttweaker interface to manipulate Minecraft's spawn npc tables |
| 129 | [Super Sound Muffler: Revived](https://www.curseforge.com/minecraft/mc-mods/super-sound-muffler-revived) (363856) | `supersoundmuffler-revived_1.12.2_1.0.2.10.jar` | CF summary: Revival of the 1.12 Super Sound Muffler mod! |
| 130 | [Suppergerrie2's Drone Mod](https://www.curseforge.com/minecraft/mc-mods/suppergerrie2s-drone-mod) (291410) | `sdrones-1.3.1.jar` | **Pack role:** Hauler Drone (A034) |
| 131 | [SwingThroughGrass](https://www.curseforge.com/minecraft/mc-mods/swingthroughgrass) (264353) | `stg-1.12.2-1.2.3.jar` | CF summary: Kill mobs and players through grass |
| 132 | [TBone](https://www.curseforge.com/minecraft/mc-mods/tbone) (323527) | `TBone-v1.6.8+mc1.12.2.jar` | CF summary: T145's shared code library |
| 133 | [Techguns](https://www.curseforge.com/minecraft/mc-mods/techguns) (244201) | `techguns-1.12.2-2.0.1.2_1.jar` | **Pack role:** guns, armour, turrets (Military chapter) |
| 134 | [Thermal Dynamics](https://www.curseforge.com/minecraft/mc-mods/thermal-dynamics) (227443) | `ThermalDynamics-1.12.2-2.5.6.1-universal.jar` | CF summary: Thermal gets Dynamic! Adds ducts - transportation for Redstone Flux, Fluids, and Items! |
| 135 | [Thermal Foundation](https://www.curseforge.com/minecraft/mc-mods/thermal-foundation) (222880) | `ThermalFoundation-1.12.2-2.6.7.1-universal.jar` | **Pack role:** its coin items are renamed into science packs and research points (`scripts/SciencePacks.zs`) |
| 136 | [Toast Control](https://www.curseforge.com/minecraft/mc-mods/toast-control) (271740) | `Toast Control-1.12.2-1.8.1.jar` | CF summary: Control toasts, those popups in the corner! |
| 137 | [Topography](https://www.curseforge.com/minecraft/mc-mods/topography) (297878) | `Topography-1.12.2-1.10.1.jar` | **Pack role:** world presets incl. void worlds (`config/topography`, `scripts/VoidWorlds.zs`) |
| 138 | [VanillaFix](https://www.curseforge.com/minecraft/mc-mods/vanillafix) (292785) | `VanillaFix-1.0.10-150.jar` | CF summary: Keep playing after a crash / Increase your FPS by up to 3x |
| 139 | [ZeroCore 2](https://www.curseforge.com/minecraft/mc-mods/zerocore) (247921) | `zerocore-1.12-0.1.2.3.jar` | CF summary: Utility mod and multiblock API |

## 3. Factorio mechanics mapping

Casual Mode changes three rows of the original's mapping and leaves the rest alone. `diff -rq`
between the two `overrides/` trees reports no difference in `config/F0Resources/`,
`config/f0-resources.cfg`, `config/modularmachinery/`, `config/incontrol/`, `scripts/SciencePacks.zs`,
`scripts/Capsule.zs`, `scripts/Tier1.zs`, `scripts/Tier3.zs` or `scripts/MM.zs`. Every row not
listed as changed below is therefore identical to `docs/research/manufactio.md` section 3.

| Factorio mechanic | Casual Mode | Our verdict (`docs/factorio-mechanics.md`) and comparison |
| --- | --- | --- |
| Research and science packs | Unchanged: renamed Thermal Foundation coins, a Laboratory multiblock, Better Questing quests granting Game Stages (`scripts/SciencePacks.zs`, `config/modularmachinery/`) | `planned` |
| Technology tree | Unchanged except that 7 quests are removed (section 6) | `shipped` for us |
| Resource patches and finite ore | Unchanged: Factory0-Resources per-chunk amounts, `reduceOreInTheChunk=true` (`config/f0-resources.cfg`) | `adapted` |
| Mining drills | Unchanged: Burner and Electric Drill with drill heads | `adapted` |
| Smelting | Unchanged | `planned` |
| Assembling machines and recipe categories | Unchanged: Basic, Standard and Advanced Assemblers, no recipe locking | `planned` |
| Handcrafting and the crafting queue | Unchanged: scarce crafting surfaces, no queue | `planned` |
| Transport belts | Unchanged | `adapted` |
| Inserters | Unchanged | `adapted` |
| Electric network and transmission | Unchanged | `adapted` |
| Power generation | Unchanged | `planned` |
| Nuclear fission | Unchanged: Extreme Reactors | `adapted` |
| Fluid handling | Unchanged | `planned` |
| Oil processing | Unchanged, except that the Chemical Plant recipe cleaning `polluted_water` is commented out (`scripts/Tier4.zs`) | `planned` |
| **Pollution** | **Removed.** Pollution of the Realms, Polluted Earth Reborn, Advanced Chimneys and Pollutant Pump are not in the manifest. Their scripts are commented out: filter and respirator recipes (`scripts/Tier2.zs`, `scripts/_DisabledRecipes.zs`) and their research stages (`scripts/research.zs` stages C000, C001, C005; `scripts/recipestages.zs`). | `planned` (ADR-0055). Casual Mode keeps **no** pollution analogue. Quest texts still print per-machine figures such as "Burner Drill: Pollution: 4.4 (Carbon) 0.4 (Sulfur)" (quest `Mining`), which is stale text (inferred: the quests were not rewritten). |
| **Enemies and evolution** | **Removed as a progression mechanic.** Hostile Worlds - Invasions, Epic Siege Mod and Zombie Awareness are not in the manifest. Mob Stages is still installed, but every `mods.MobStages.addStage` line in `scripts/MobStages.zs` is commented out, so no mob waits on research. In Control's spawn rules are unchanged (`config/incontrol/spawn.json`, identical to the original's), so hostiles still cannot spawn on grass, dirt, sand, gravel, concrete or in light. Quest texts still announce "## UNLOCKS CREEPERS ##" and the like (e.g. quest `Advanced Material Processing [P011]`), which is stale. | `planned` (ADR-0055). Casual Mode has no evolution at all: threat is vanilla night spawning on the blocks In Control allows. |
| Combat: guns, ammo, turrets, walls | Unchanged (Techguns). Inferred: concrete walls matter less without digging mobs. | `planned` |
| Armor and the equipment grid | Unchanged | `planned` |
| Capsules | Unchanged | `planned` |
| Modules and beacons | Unchanged: speed, productivity and efficiency modifiers on Modular Machinery machines, no beacons | `blocked` for us (#120) |
| Trains | Unchanged | `planned` |
| Logistic robots | Unchanged | `excluded` |
| Logistic request and trash | Unchanged: not present | `excluded` |
| Construction robots and blueprints | Unchanged: Capsule packages | `excluded` |
| Circuit network | Unchanged | `adapted` |
| Rocket silo / launch | Unchanged: Advanced Rocketry to the moon | `planned` |
| Interplanetary travel | Unchanged | `blocked` |
| Personal transport | Unchanged | `blocked` |
| Radar and map exploration | Unchanged | `adapted` |
| Trees and wood | Unchanged: Dynamic Trees | `adapted` |
| **Repair and entity damage** | The original's auto-repair after an invasion goes with the Invasions mod. `config/HW_Invasions/` is still shipped, but no mod reads it (inferred). Turret stand-down repair (Techguns) remains. | `blocked` for us. |
| Recycling (Space Age) | Unchanged | `planned` |
| Day and night | Unchanged: torches uncraftable, lanterns must be researched and powered | `shipped` |

## 4. Factorio mechanics Casual Mode does not implement

Everything listed in `docs/research/manufactio.md` section 4, plus two more:

- **Pollution of any kind.** The original's pollution cloud, emitters and filters are removed.
- **Any evolution.** The original's research-gated mob types and periodic invasions are removed;
  Factorio's pollution-driven evolution was already absent.

## 5. Completely new: in Casual Mode, not in Factorio

Nothing new. Casual Mode is a subtraction from the original: it adds no mod but HammerLib (a library,
section 6) and no script, quest or machine. The original's list in `docs/research/manufactio.md`
section 5 applies, minus the research-gated mob types and the invasions.

The zip also ships an `overrides/difficulty options/` folder. It holds alternative
`Polluted Earth.cfg`, `epicsiegemod.cfg` and Zombie Awareness configs, a pollution readme pointing
at `config/adpother/adpother.cfg`, two alternative `F0Resources/ores.json` ("beta ores", "more
spread out ores"), and AE2 channel/no-channel script sets. Inferred: it was carried over from the
original, since three of those mods are not in Casual Mode's manifest.

## 6. Diff vs the original Manufactio (1.35)

**Mods.** By project ID, 138 of the original's 146 mods are kept, 8 removed and 1 added. 135 of the
138 kept are at the same file.

- **Removed (8):** Advanced Chimneys (244830), Epic Siege Mod (229449), Hostile Worlds - Invasions
  (257244), Mob Stages (278359)*, Pollutant Pump (305122), Polluted Earth Reborn (358722), Pollution
  of the Realms (269973), Zombie Awareness (237754).
- **Added (1):** HammerLib (247401), `HammerCore-1.12.2-2.0.5.7.jar`. Inferred: a dependency, since
  no script names it.
- **Different file (3):** Better Foliage 2.3.1 → 2.3.3, Mo' Bends 1.0.0-beta-20.06.20 →
  1.2.1-19.12.21, Just Enough Calculation 3.2.4 → 3.2.7.
- **Forge:** 14.23.5.2860 in the original, 14.23.5.2847 here (`manifest.json`).

\* Mob Stages is absent by project ID but its script, `scripts/MobStages.zs`, is still shipped with
its stage lines commented out. Inferred: CraftTweaker skips the `mods.MobStages` calls because they
are commented, so the missing mod causes no error.

**Scripts and configs** (`diff -rq` of the two `overrides/` trees, 41 differences):

| file | change |
| --- | --- |
| `scripts/MobStages.zs` | every `addStage`/`toggleSpawner` line commented out |
| `scripts/research.zs`, `scripts/recipestages.zs` | Item Stages and tooltips for pollution items (stages C000, C001, C005) commented out |
| `scripts/Tier2.zs` | respirator and filter recipes commented out |
| `scripts/Tier4.zs` | polluted-water cleaning and unloading recipes commented out |
| `scripts/_DisabledRecipes.zs`, `scripts/_DisabledTech.zs` | removals of pollution items commented out, as the items no longer exist |
| `scripts/AdvancedRocketry.zs`, `scripts/Recycler.zs` | differ (not read in detail) |
| `config/adpother/`, `config/adchimneys/`, `config/capsule/loot` | only in the original |
| `config/betterquesting/DefaultQuests.json` | 7 quests removed: `Environmental Cleaning [C001]`, `Advanced Coke Oven [P016]`, `Mob Spawning and Pollution`, `Invasions`, `Advanced Filtering [C011]`, `Basic Filtering [C000]`, `Respirator [C005]`. No remaining quest's name, text, task or reward changed. |
| `config/AppliedEnergistics2/`, `config/capsule/blueprint_whitelist.json`, textures, splash | minor |

**Mechanics changed.** Pollution: removed. Invasions and mob AI: removed. Research-gated mob types:
disabled. Everything that makes the factory a factory (research, finite ore, drills, assemblers,
modules, packages, oil, trains) is unchanged.

## 7. Reception

**CurseForge**

- **Total downloads:** 8,732 (cfwidget `downloads.total`; the project page, via WebFetch, shows
  8,732). **Comments:** 11 (project page). The comment text was not read.

| file | uploaded | downloads (cfwidget) | downloads (files tab) |
| --- | --- | --- | --- |
| Manufactio - CM-1.31.zip | 2023-09-08 | 1,298 | 2.0K |
| Manufactio - CM-1.30.zip | 2020-04-05 | 6,682 | 6.7K |

Inferred: cfwidget's number for 1.31 is stale, as it was for the other two packs.

- For scale: the original has 160,585 downloads and Nuclear Edition 28,431
  (`docs/research/manufactio.md`, `docs/research/manufactio-2-nuclear-edition.md`).

**YouTube.** Only one Casual Mode video was found. Title and channel come from YouTube's oEmbed
endpoint; views and date from the watch page on 2026-09-27.

| video | channel | published | views | how found |
| --- | --- | --- | --- | --- |
| [Manufactio (Casual Mode) Episode 8 .... If I have to restart one more time!](https://www.youtube.com/watch?v=kEgN0SdAYGs) | Henry & Noobie | 2020-11-22 | 60 | web search |

The five videos embedded on the CurseForge page are playthroughs of the original, listed in
`docs/research/manufactio.md` section 6.

**Forum and Reddit.** The description points feedback at the original's FTB forum thread
(https://forum.feed-the-beast.com/threads/manufactio-minecraft-factorio-hybrid.283615/). No thread
about Casual Mode itself was found on the FTB forum or on Reddit. Web search turned up only
server-hosting and launcher listings (ScalaCube, moddedminecraftservers.com, klauncher.gg, Modpack
Index), none with reception data.

## 8. Sources

- CurseForge project page: https://www.curseforge.com/minecraft/modpacks/manufactio-casual-mode
- CurseForge files tab: https://www.curseforge.com/minecraft/modpacks/manufactio-casual-mode/files/all
- cfwidget API (project, files, per-file downloads, description): https://api.cfwidget.com/373441
  (the slug path `https://api.cfwidget.com/minecraft/modpacks/manufactio-casual-mode` returned 404)
- cfwidget API per mod: `https://api.cfwidget.com/<projectID>` for each of the 139 project IDs
- Golrith's CurseForge projects: https://www.curseforge.com/members/golrith/projects
- Modpack zip 1.31: https://mediafilez.forgecdn.net/files/4744/184/Manufactio%20-%20CM-1.31.zip. Files read: `manifest.json`,
  `modlist.html`, `overrides/scripts/{MobStages,research,recipestages,Tier2,Tier4,_DisabledRecipes,_DisabledTech}.zs`,
  `overrides/config/betterquesting/DefaultQuests.json`, `overrides/difficulty options/`, and a `diff -rq` of all of `overrides/`
- Original for the diff: `Manufactio-1.35.zip` (https://mediafilez.forgecdn.net/files/3582/169/Manufactio-1.35.zip)
- Companion reports: `docs/research/manufactio.md`, `docs/research/manufactio-2-nuclear-edition.md`
- YouTube: https://www.youtube.com/watch?v=kEgN0SdAYGs, metadata from `https://www.youtube.com/oembed?url=…` and the watch page
- Our ledger: `/Users/kc00l/curseforge/Instances/PlanetaryFactory/docs/factorio-mechanics.md`
