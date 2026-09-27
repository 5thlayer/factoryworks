# Manufactio mods to adapt or port (WIP)

Mods from [Manufactio](manufactio.md)'s 1.12.2 manifest worth adapting or porting to the Pack. A
working list: no mod here is decided. Each row names the ledger section in
[`docs/factorio-mechanics.md`](../factorio-mechanics.md) it would serve, and what Manufactio uses it
for.

| Mod | Ledger row | Manufactio's use | Notes |
| --- | --- | --- | --- |
| [Magneticraft](https://www.curseforge.com/minecraft/mc-mods/magneticraft) (224808) | [Inserters](../factorio-mechanics.md#inserters) | `magneticraft:inserter`, with a whitelist/blacklist GUI; stack and speed upgrades as research (quests `L014`, `L023`) | **The inserters.** Also a back-stuffing conveyor. |
| [Signals](https://www.curseforge.com/minecraft/mc-mods/signals) (245824) | [Trains](../factorio-mechanics.md#trains) | "OpenTTD/Factorio style" block and chain signals, station markers, cart engines (quests `L031`, `L041`, `L042`) | Railcraft's Distant Signal reserves no path; this one's chain signal is Factorio's. |
| [Industrial Renewal](https://www.curseforge.com/minecraft/mc-mods/industrial-renewal) (299849) | [Trains](../factorio-mechanics.md#trains) | In the manifest; no quest found naming it | CF summary: "Industrial objects to minecraft". |
| [Suppergerrie2's Drone Mod](https://www.curseforge.com/minecraft/mc-mods/suppergerrie2s-drone-mod) (291410) | [Logistic robots](../factorio-mechanics.md#logistic-robots) (`excluded`) | Hauler Drone (quest `A034`) | Would reopen an `excluded` row (ADR-0017). |
| [Drones](https://www.curseforge.com/minecraft/mc-mods/drones) (263438) | [Enemies and evolution](../factorio-mechanics.md#enemies-and-evolution) | Hostile drone mobs, unlocked by research ("UNLOCKS HOSTILE DRONES", `scripts/MobStages.zs`) | CF summary: "A Mod to add Drones Mobs to the world". |
| [Techguns](https://www.curseforge.com/minecraft/mc-mods/techguns) (244201) | [Combat](../factorio-mechanics.md#combat-guns-ammo-turrets-walls), [Armor](../factorio-mechanics.md#armor-and-the-equipment-grid) | Guns, ammo press, turrets, armour up to Power Armour (Military chapter; quests `M991`, `M991B`) | Combat is #230, armour #231. |
| [MrCrayfish's Vehicle Mod](https://www.curseforge.com/minecraft/mc-mods/mrcrayfishs-vehicle-mod) (286660) | [Personal transport](../factorio-mechanics.md#personal-transport) (`blocked`, #121) | ATV and sports plane (quests `A023`, `A036`) | A car analogue. |
| [Expandable Inventory](https://www.curseforge.com/minecraft/mc-mods/expandable-inventory) (304371) | none yet | "Toolbelt Upgrade 1–7" adds inventory rows (Exotic Military & Tech chapter) | Factorio's Toolbelt technology; the ledger has no row for it. |

Versions, jar names and quest ids are in [manufactio.md](manufactio.md)'s mod list and mechanics
table.
