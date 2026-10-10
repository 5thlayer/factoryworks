# Ore amount checks

ADR-0041 makes an ore block carry an **amount** and a break gesture draw one unit from it. That
claim is four separate things that can each be wrong on their own, so it is four checks rather than
one, and none of them launches the game.

## The numbers are hand-owned

`amounts.json` — the patch totals, stage ratios, distance and density laws, the outfield edge and
crude's figures — is hand-owned data (#600). Nothing re-derives it from a corpus, so a rebalance
edits the file and the Java and asset checks below follow.

`tests/factorio/test_resource_extract.py` keeps the half that still reads the corpus.

The same check reads `PickTier.java` and asserts the pack's `MINING_TIME` is half Factorio's
`mining_time` and that the two tier speeds are the extracted ones. That is what caught the pack
reading `character-mining-speed` as an addend: Factorio applies the modifier as `base * (1 +
modifier)`, so steel is 1.0 rather than 1.5. The value was already right; its prose was not.

## The mechanism pays out what the block holds

`mod/src/test/java/com/factoryworks/core/ore/` under `./gradlew :factoryworks_core:test`.

`OreDeltaTest` is the one that matters: a block pays out **exactly** its amount over exactly that
many gestures, the last unit is paid rather than swallowed by the break that removes the block, and
an exhausted position **retires** its entry. Retirement is not tidiness — a delta left behind is
inherited by whatever is placed at that position next, which reaches a player as a fresh patch that
pays out half, with nothing in any log. Note the consequence the tests encode: because retiring is
required, a draw against a retired position is indistinguishable from a draw against a fresh block,
so "an exhausted block pays nothing more" is not a statement this class can make. `OreMining` never
asks it, because by then the block is stone.

`OreFieldTest` is the derivation the design turns on — patch total ÷ blocks actually placed — and
`OreStageTest` the eight sprite rungs against a remaining fraction. `OreCodecsTest` is the
attachment's round trip, which ADR-0038 asks for by name: a codec that drops the map does not crash,
it hands back a chunk whose every ore block silently refilled. `OreCorpusTest` asserts the classpath
slice the mod loads at class-init is the one in the repository — it has to be a resource rather
than a datapack file, because the stage count sizes a blockstate property before any world exists.

`OutfieldAmountTest` is the outfield's own arithmetic (#319, ADR-0045): a disc's blocks share one
amount, its total over the block count its structure piece recorded, read at the disc **centre's**
distance from world origin. It asserts the spot stops growing at 1600 blocks while richness rises
with no cap, that uranium derives from its own `base_density` rather than borrowing another field's
amount. The starting fields keep the census; two arithmetics is Factorio's own shape.

`OutfieldShapeTest` is the disc's footprint (#320): the radius caps at 32, and a disc covers the columns where
the cone plus `(octaves - 1/3) × amplitude` is above zero, each octave read at its own scale and
weighted by its own weight, and never off the land. Its saved mask reads back the same columns. Its
block count is the columns it covers, and a disc inside 150 blocks,
or one whose radius rounds below one block, covers none. Whether a disc places is
`OutfieldDiscTests`, in the GameTest run.

`MiningSpeedTest` carries the arithmetic across the two halves: a field's cost is its **amount**
times the tier's seconds, not its block count. That is the whole of what the amendment changed, and
it is the one number that was silently a function of how many blocks the generator laid down.

## The blocks exist and drop the right thing

`tests/pack/test_ore_assets.py`, which also runs `scripts/build-ore-textures.py --check` and
`scripts/build-ore-assets.py --check`.

Five ore blocks × eight stages is 54 generated files, and every one of them is a hop a player falls
through: a blockstate naming a model that is not there is a purple cube, a loot table naming an item
the alphabet does not have is a block that drops nothing. The check walks blockstate → model →
texture for all forty, asserts the lang keys, and cross-reads the drop table out of
`OreResource.java` so the Java and the JSON cannot disagree.

It also asserts every ore block is in **`c:ores`**, which is not cosmetic: GregTech's `MinerLogic`
scans that tag to decide what a drill may take, so a pack ore block outside it is invisible to
`gtceu:lv_miner` — the failure ADR-0041 gates its whole worldgen half on.

## One consequence worth knowing about

The ore blocks' loot tables are **empty on purpose** — a table that paid out would let a player
blow up a thousand-unit block for one free item, which is ADR-0041's rejected "vandalise a patch
for one ore" wearing TNT. `OreMining.drop` is the only payout, and it is metered.

GregTech's `MinerLogic` produces its output *through the loot table*, so until #105 builds the rigs
a `gtceu:lv_miner` pointed at a pack ore block grinds it to stone and pays nothing. That is not a
regression the empty table introduced so much as one it made honest: the same miner previously
destroyed the whole block for a single item, because it calls `destroyBlock` rather than drawing a
unit. Both readings are wrong, and only #105 — which owns the drills and is what ADR-0041 unblocks
— can make a drill draw one unit at a time. `c:ores` membership is kept precisely so that rig has a
block to see.

## What still needs a world

Whether the stages render, whether the Jade line agrees with what the block actually pays, whether
`gtceu:lv_miner` mines a pack ore block, and whether rung-0 pacing feels right. All four are
one launch, and none of them is a static check.
