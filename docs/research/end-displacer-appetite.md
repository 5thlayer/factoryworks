# How many dense blocks an End Displacer eats

Research for [#695](https://github.com/5thlayer/factoryworks/issues/695), on the map
[#693](https://github.com/5thlayer/factoryworks/issues/693) "Carry matter between dimensions".

**Source**: [5thlayer/voidworks](https://github.com/5thlayer/voidworks) at `main` commit `3a581a3`
(mod version 0.1.2). Paths below are relative to that repository. **Every number is provisional**:
the server config says so of all its values (`src/main/java/io/github/_5thlayer/voidworks/VoidworksConfig.java:14`),
and the spec lists Zone sizes, the Density curve, Voidstone and Well capacity and every Void Dragon
number as open (`docs/spec/void-energy.md:508-518`). A pack can change any of them in
`voidworks-server.toml`.

Below, `Config` is `src/main/java/io/github/_5thlayer/voidworks/VoidworksConfig.java`, `Displacer`
is `src/main/java/io/github/_5thlayer/voidworks/displacer/DisplacerBlockEntity.java`, and `Spec` is
`docs/spec/void-energy.md`. A second is 20 ticks, at a full 20 TPS.

## Answer

One End Displacer harvests **one fed block per second, whatever its Density**, and **64 blocks in
its life**, which then lasts about 64 seconds. Zone upgrades, the only thing that would lengthen a
life, are not built. So "per hour at full work" means one Displacer *slot* kept busy by a chain of
about 56 Displacers built one after another, each eating one stack.

| Density | Blocks (tag `#voidworks:density/N`) | Motes per block | Blocks per hour | Motes per hour | Blocks per life | Motes per life |
|---|---|---|---|---|---|---|
| 1 | copper, raw copper, coal blocks | 1 | 3,600 | 3,600 | 64 | 64 |
| 2 | iron, raw iron, redstone, lapis blocks | 4 | 3,600 | 14,400 | 64 | 256 |
| 3 | gold, raw gold, emerald blocks | 16 | 3,600 | 57,600 | 64 | 1,024 |
| 4 | diamond, netherite blocks | 64 | 3,600 | 230,400 | 64 | 4,096 |

The motes are grade 4 over the End island and grade 5 over the open void.

One Void Dragon carrying 8,192 motes between an End Well and an Overworld Well right beside it
moves about **983,000 motes per hour**. That is about 4.3 Displacer slots fed Density 4. With the
Wells 1,000 blocks apart it moves about 481,000, or about 2.1 slots. In blocks, one dragon's work
matches about **15,400 Density-4 blocks an hour, roughly 4.3 a second**, going the other way.

**Throughput target for the item link: 3,600 blocks an hour (one a second) per busy End Displacer
slot. It may arrive in bursts of one 64-block stack per Displacer life.**

## The numbers

### Harvest rate and life

- **Harvest interval: 20 ticks.** `harvestInterval` defaults to 20 (`Config:72-74`). The
  Displacer harvests once each time the interval passes (`Displacer:182-185`), and each harvest
  takes exactly one fed block (`Displacer:232`, `extract(INPUT, fed, 1, …)`). The rate doesn't
  depend on Density. Density only sets how many motes a harvest gives (`Displacer:228`).
- **Zone: 64.** `zone` defaults to 64 positions per life (`Config:54-56`). A new Displacer takes
  its Zone from the config on its first tick (`Displacer:173-175`). Once it has harvested that
  many, it becomes Voidstone (`Displacer:194-196`, `Spec:105-106`, `Spec:125-129`).
- **Zone upgrades are not built.** The spec says upgrades paid in motes raise a Zone
  (`Spec:105-106`, `Spec:129-130`), and `setZone` exists for one (`Displacer:102-106`). But only
  the first tick and the game tests call it: `gametest/DimensionSite.java:95` and
  `gametest/DisplacerTests.java:284,330`. No 5thlayer/voidworks issue tracks Zone upgrades yet.
  Their cost and size are still open (`Spec:508`).
- **Life: about 64 seconds** of harvesting, plus the time it spends Dormant soaking its cost before
  the first harvest (`Displacer:182`, `ready()`, `EndDisplacerBlockEntity.java:57-59`).
- **Input: one slot**, which takes only blocks of Density above 0 (`Displacer:64-65`, `:80`). Its
  capacity is the handler's default, which is the item's stack size of 64. That is inferred: the
  code sets no limit of its own (`inventory/ItemFace.java:65-66`). **A whole life fits in one
  stack.**
- **What it reaches**: the End Displacer harvests only air, and only ahead of its front face
  (`displacer/EndDisplacerBlockEntity.java:42-49`). It gives nothing back to its output
  (`:52-54`).

### Density and motes

- **Density tags**: `src/main/resources/data/voidworks/tags/item/density/1.json:3-16` (copper,
  raw copper, coal), `2.json:3-21` (iron, raw iron, redstone, lapis), `3.json:3-16` (gold, raw
  gold, emerald) and `4.json:3-11` (diamond, netherite). Each tag also takes the matching
  `#c:storage_blocks/*` tag. A block's Density is the highest tag that names it, and 0 if none
  does (`displacer/Density.java:33-44`). The spec has the same table (`Spec:139-144`).
- **Motes per block**: `densityMotes` defaults to `[1, 4, 16, 64]` (`Config:99-102`), read by
  `motes(density)` (`Config:266-269`).
- **Grade**: one above the Void Pressure where the block lands (`Displacer:227`). The End's
  pressure is 3 (`src/main/resources/data/voidworks/data_maps/dimension/void_pressure.json:11-13`),
  and one step higher over the open void (`Spec:71-72`, `Spec:75-76`). That makes grade 4 over the
  island and grade 5 over the open void.

### Mote storage: why Density 4 needs a drain

- **Voidstone holds 16 motes** (`voidstoneCapacity`, `Config:57-61`). That is one Density-3
  harvest.
- A harvest goes ahead only if the network it joins has room for all of its motes
  (`Displacer:280-281`). That room is one new Voidstone plus whatever spare room the touching
  networks have (`network/VoidNetworks.java:257-265`). The motes land at once in the nearest
  stores in the network with room, with no wait for conduction (`network/VoidNetworks.java:267-277`).
- So a **Density-4 harvest leaves 48 motes more than its own Voidstone holds**. Fed Density 4, a
  Displacer on a fresh network stalls at its first harvest. The game test `fullNetworkStalls`
  asserts this (`gametest/DisplacerTests.java:192-199`). Density 3 exactly fills the Voidstone it
  makes, and Densities 1 and 2 leave room to spare.
- A Displacer slot fed Density 4 therefore has to send **48 motes a second (172,800 an hour) out
  of its network**. A **Void Well holds 65,536** (`wellCapacity`, `Config:62-64`). That is about
  23 minutes of one busy Density-4 slot, or about 21 Displacer lives (64 × 48 = 3,072 surplus
  motes per life). After that, a dragon or some other spender has to empty the network or the
  harvest stalls. **Feeding the End Displacer Density 4 depends on the dragon carrying motes out,
  as well as on the item link carrying blocks in.**

### Void Dragon carry

- **Load: 8,192 motes** (`dragonLoad`, `Config:132-134`; `Spec:516`).
- **Stop: 100 ticks**, 5 s, to load or to unload (`dragonStopTicks`, `Config:135-137`;
  `dragon/Routing.java:230,247`).
- **Dive**: 200 ticks more between dimensions (`dragonDiveTicks`, `Config:138-141`), plus one
  second per 64 Overworld blocks of ground distance (`dragonDiveBlocksPerSecond`, `Config:142-145`).
  The End counts its own blocks at scale 1 and the Nether at 8. Every dive takes at least 40 ticks
  (`dragon/Dive.java:27-46`).
- **Harm**: a diving dragon loses one point of health every 200 ticks (`dragonWeakenTicks`,
  `Config:146-151`) for each pressure step the worse end of the dive sits below the End: 1 for the
  Overworld, 2 for the Nether (`dragon/Harm.java:29-32`). It heals 16 motes per point before its
  next dive (`dragonHealMotes`, `Config:152-155`), out of 100 health
  (`dragon/VoidDragonRecord.java:34`). Its speed is its share of health, down to a quarter
  (`dragon/VoidDragonRecord.java:68-71`). A 10-second End–Overworld dive costs about 1 point,
  which means 16 motes and 1% of its speed. That is negligible here.

**Round trip, End Well to Overworld Well**, the shortest case with the Wells at the same x/z: load
5 s, dive 10 s, unload 5 s, dive 10 s back. That is 30 s, or 120 round trips an hour, which moves
**983,040 motes an hour** one way. With the Wells 1,000 blocks apart, each dive is
200 + ⌈1000 × 20 / 64⌉ = 513 ticks, so a round trip takes 1,226 ticks (61.3 s). That is about 58.7
trips an hour, or **about 481,000 motes an hour**. Trips to the Nether weaken the dragon twice as
fast, and one Nether block counts eight.

| Comparison | Motes per hour |
|---|---|
| One End Displacer slot, Density 4 | 230,400 |
| One Void Dragon, Wells beside each other | 983,040 (~4.3 slots) |
| One Void Dragon, Wells 1,000 blocks apart | ~481,000 (~2.1 slots) |
| Density-4 surplus to drain per slot (above what its Voidstone holds) | 172,800 |

### What a life costs, and what End feeding buys

- **The End Displacer's Dormant cost is 6,400 void energy**
  (`src/main/resources/data/voidworks/data_maps/block/dormant_cost.json:6-8`). A mote one grade
  above the local pressure releases 100 (`energy/VoidEnergy.java:25,67`), so the cost comes to 64
  grade-4 motes or 32 grade-5 motes, paid in the End. To keep one slot busy for an hour takes 56.25
  Displacers, which is 360,000 void energy, or 3,600 grade-4 motes. That is 1.6% of a Density-4
  hour, and exactly the whole of a Density-1 hour over the island. **Fed Density 1 over the island,
  an End Displacer only pays back its own Dormant cost.** Over the open void it pays it back twice.
- **Its recipe** is End stone bricks, popped chorus fruit and end rods around a sticky piston
  (`src/main/resources/data/voidworks/recipe/end_void_displacer.json`). Everything but the piston's
  slimeball is End-made, so the Displacers need little transport of their own.
- **The gain per block** is what makes a link pay. Void energy is `100 × 2^(grade − pressure − 1)`
  (`energy/VoidEnergy.java:56-67`). Spent in the Nether (pressure 1), an Overworld grade-3 mote
  releases 200 and an End open-void grade-5 mote releases 800. A diamond block fed in the Overworld
  is worth 64 × 200 = 12,800 there, and fed over the End's open void 64 × 800 = 51,200. That is a
  gain of **38,400 void energy per Density-4 block** and **600 per Density-1 block**, which is the
  ceiling on any per-block void cost for the link.

## For later tickets

- **Late-tier direct link ([#699](https://github.com/5thlayer/factoryworks/issues/699))**: size
  it per busy End Displacer slot at one block a second. Delivery in whole stacks is fine, since a
  life is one 64-block stack and the input slot holds exactly that. If Zone upgrades are built,
  each life gets longer but the rate doesn't change: Zone changes blocks per life, never blocks
  per second. Whatever comes after the default numbers, `harvestInterval` and `zone` are the two
  config values the target scales with.
- **Void cost ([#700](https://github.com/5thlayer/factoryworks/issues/700))**: one block gains
  between 600 (Density 1) and 38,400 (Density 4) void energy, counted at Nether spending, by being
  fed over the End's open void and not in the Overworld. A flat per-item cost would make
  low-Density blocks a loss, so a cost per Density, or per mote of yield, keeps every Density
  worth carrying. Density 1 over the island already only pays back the Displacer's own Dormant cost.
- **The dragon is part of the route.** Density 4 stalls without a drain of 48 motes per second per
  slot, so the item link and the dragon's return flow have to be sized together: about four busy
  Density-4 slots per dragon at most, fewer the farther apart the Wells.
