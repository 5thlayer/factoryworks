---
status: superseded by ADR-0127
---

# Terra spawns no vanilla mob on its own

Base Factorio has no wildlife but fish, and no enemy but the biters. Terra had vanilla's: every land
biome spawned spiders, zombies, skeletons, creepers, endermen and witches, most of them sheep, pigs,
chickens and cows, and the overworld ran phantoms, pillager patrols, the wandering trader and village
cats and sieges on top of that.

**Decision (#480).** Two halves.

- **The biomes.** `scripts/build-terra-worldgen.py` gives every Terra biome empty spawner lists and
  sets the noise settings' `disable_mob_generation`, so neither the natural spawner nor chunk
  generation places a mob.
- **The custom spawners.** These run whatever the biomes say, and each is a game rule. A new world
  starts with `spawn_mobs`, `spawn_phantoms`, `spawn_patrols` and `spawn_wandering_traders` off, set
  by `core/worldgen/VanillaSpawning` on NeoForge's `LevelEvent.CreateSpawnPosition`, which fires once,
  when a world is created. `spawn_mobs` is the rule that stops cats and sieges: both count beds as a
  village, so a player's own bunk room would summon them.

The pack's own enemies (ADR-0055) instantiate their mobs from saved records, which no game rule
gates.

**Considered.**

- *The world preset.* A preset names dimensions, not game rules, so it cannot hold this.
- *KubeJS.* The rules would sit in a script that no GameTest reaches, beside a mod that already
  sets the neighbouring water rule (ADR-0015 puts mechanism in the mod).
- *Forcing the rules on every server start*, as `WaterConservation` does (ADR-0050). That rule closes
  an exploit, and a player who reopens it breaks the factory's water arithmetic. Turning a mob back on
  harms only the world of the player who did it, so this is a default and `/gamerule` is left alone.
- *`spawn_mobs` alone.* It stops every custom spawner and the natural spawner at once, but it is one
  command away from vanilla's full roster. The biomes are emptied anyway, and the three named rules
  are set too, so turning `spawn_mobs` back on brings back only cats, sieges and the skeleton horses
  lightning can spawn.

**Consequences.** A world created before this keeps the rules it was created with. Mob drops have no
source on Terra, so the Obtainable index has nothing to derive from them (#455). A body that comes out
of `kubejs/parked/` has its own biomes and needs the same emptying when it does.
