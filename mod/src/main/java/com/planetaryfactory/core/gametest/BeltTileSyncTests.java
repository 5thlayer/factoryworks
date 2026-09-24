package com.planetaryfactory.core.gametest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import rearth.belts.blocks.BeltTileBlockEntity;
import rearth.belts.model.BeltContents;
import rearth.belts.model.BeltTier;
import rearth.belts.model.TransportLine;

/**
 * A line of tiles costs the server no block updates, saves tile by tile, and stops at a chunk that
 * unloads and joins up again when it loads, losing and duplicating nothing (#395).
 *
 * <p>The harness cannot unload a test's chunk, so the unload runs the same steps a real one does
 * in the same order: each tile of the far chunk saves, the chunk's tiles are let go through the
 * fork's {@code chunkUnloading} (which the fork's {@code ChunkEvent.Unload} listener calls), and
 * the chunk loads again as block entities read from those saves. Whether the client draws the
 * items is a human check on delivery; the copy it draws from is the fork's
 * {@code TileLineSyncTest}.
 */
final class BeltTileSyncTests {

    private static final BlockPos SOURCE = new BlockPos(2, 1, 3);
    private static final BlockPos FROM = new BlockPos(3, 1, 3);
    private static final BlockPos FIRST_TILE = new BlockPos(4, 1, 3);

    private static final int TILES = 5;
    private static final int SETTLE_TICKS = 5;
    private static final int MOVING_TICKS = 30;
    // Longer than an item takes to cross the line, so it is flowing end to end.
    private static final int FLOWING_WARMUP_TICKS = 80;
    private static final int FLOWING_TICKS = 100;
    private static final int FULL_CHEST = 27 * 64;

    // Loaded in 32 ticks at 15 items/s and still short of the dead end at tick 40.
    private static final int SAVED_SUPPLY = 24;
    private static final int SAVED_TILES = 6;
    private static final int SAVED_AFTER_TICKS = 40;

    // Twenty tiles cross at least one chunk boundary wherever the test is placed.
    private static final BlockPos EDGE_SOURCE = new BlockPos(0, 1, 1);
    private static final BlockPos EDGE_FROM = new BlockPos(1, 1, 1);
    private static final BlockPos EDGE_FIRST_TILE = new BlockPos(2, 1, 1);
    private static final int EDGE_TILES = 20;
    private static final int EDGE_SUPPLY = 128;
    // The front reaches the last tile at about 215 ticks, with items loading until about 170.
    private static final int EDGE_FLOWING_TICKS = 220;
    private static final int EDGE_UNLOADED_TICKS = 60;
    // The front on the last tile and still moving: a dense stream into a dead end stops all at once.
    private static final int EDGE_MOVING_TICKS = 205;

    // Tiles from this index on stand a block up, so the line climbs a step (#417); at the line's
    // length there is none.
    private static final int SAVED_STEP = 3;
    private static final int EDGE_STEP = 10;

    private BeltTileSyncTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_moving_tile_line_sends_no_block_update", SETTLE_TICKS + MOVING_TICKS + 20,
                BeltTileSyncTests::movingSendsNothing);
        tests.test("a_loading_and_delivering_tile_line_sends_no_block_update",
                FLOWING_WARMUP_TICKS + FLOWING_TICKS + 20, BeltTileSyncTests::flowingSendsNothing);
        tests.test("each_tile_saves_its_own_items_and_a_reloaded_line_holds_them_all", SAVED_AFTER_TICKS + 40,
                helper -> savedTilesRestore(helper, SAVED_TILES));
        tests.test("a_line_with_a_step_reloads_from_its_tiles_holding_every_item", SAVED_AFTER_TICKS + 40,
                helper -> savedTilesRestore(helper, SAVED_STEP));
        tests.test("a_tile_placed_past_a_lines_end_extends_it", 40, BeltTileSyncTests::tilePlacedPastTheEnd);
        tests.test("a_moving_line_marks_every_chunk_it_crosses_for_saving", EDGE_MOVING_TICKS + 20,
                PFGameTests.LONG_PLATFORM, BeltTileSyncTests::everyChunkMarked);
        tests.test("a_line_stops_at_an_unloaded_chunk_and_rejoins_when_it_loads", 900,
                PFGameTests.LONG_PLATFORM, helper -> unloadedChunk(helper, EDGE_TILES));
        tests.test("a_line_with_a_step_rejoins_across_an_unloaded_chunk", 900,
                PFGameTests.LONG_PLATFORM, helper -> unloadedChunk(helper, EDGE_STEP));
    }

    private static void movingSendsNothing(GameTestHelper helper) {
        List<BlockPos> tiles = line(helper, SOURCE, FROM, FIRST_TILE, TILES, false, TILES);
        BeltTileTests.fill(helper, SOURCE, 1);

        double[] before = new double[1];
        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    before[0] = only(helper).position();
                    watch(helper, tiles);
                })
                .thenIdle(MOVING_TICKS)
                .thenExecute(() -> {
                    int updates = stop(helper, tiles);
                    if (only(helper).position() <= before[0]) {
                        helper.fail("the item did not move, so this proves nothing", FIRST_TILE);
                    }
                    if (updates != 0) {
                        helper.fail("a tile line moving one item sent " + updates + " block updates in "
                                + MOVING_TICKS + " ticks, expected none", FIRST_TILE);
                    }
                })
                .thenSucceed();
    }

    private static void flowingSendsNothing(GameTestHelper helper) {
        List<BlockPos> tiles = line(helper, SOURCE, FROM, FIRST_TILE, TILES, true, TILES);
        BlockPos target = FIRST_TILE.east(TILES + 1);
        BeltTileTests.fill(helper, SOURCE, FULL_CHEST);

        int[] arrivedBefore = new int[1];
        helper.startSequence()
                .thenIdle(FLOWING_WARMUP_TICKS)
                .thenExecute(() -> {
                    arrivedBefore[0] = BeltTileTests.count(BeltTileTests.chest(helper, target));
                    watch(helper, tiles);
                })
                .thenIdle(FLOWING_TICKS)
                .thenExecute(() -> {
                    int updates = stop(helper, tiles);
                    if (BeltTileTests.count(BeltTileTests.chest(helper, target)) == arrivedBefore[0]) {
                        helper.fail("nothing was delivered, so this proves nothing", target);
                    }
                    if (updates != 0) {
                        helper.fail("a tile line loading and delivering sent " + updates + " block updates in "
                                + FLOWING_TICKS + " ticks, expected none", FIRST_TILE);
                    }
                })
                .thenSucceed();
    }

    // Through each block entity's own save and load, then put back in the world in place of the
    // ones that saved; whether the save is reached at all is everyChunkMarked's.
    private static void savedTilesRestore(GameTestHelper helper, int step) {
        List<BlockPos> tiles = line(helper, SOURCE, FROM, FIRST_TILE, SAVED_TILES, false, step);
        BeltTileTests.fill(helper, SOURCE, SAVED_SUPPLY);

        helper.startSequence()
                .thenIdle(SAVED_AFTER_TICKS)
                .thenExecute(() -> {
                    if (BeltTileTests.count(BeltTileTests.chest(helper, SOURCE)) != 0) {
                        helper.fail("the source still holds items, so the line is still loading", SOURCE);
                    }
                    Map<BlockPos, CompoundTag> saved = save(helper, tiles);
                    int spread = 0;
                    for (BlockPos tile : tiles) {
                        List<TransportLine.Share<ItemStack>> was = tileAt(helper, tile).held();
                        List<TransportLine.Share<ItemStack>> is = load(helper, tile, saved.get(tile)).held();
                        if (!was.isEmpty()) spread++;
                        if (!same(was, is)) {
                            helper.fail("tile " + tile + " saved " + describe(was) + " and loaded " + describe(is), tile);
                        }
                    }
                    if (spread < 3) {
                        helper.fail("the items sit on " + spread + " tiles, so this proves little", FIRST_TILE);
                    }
                    for (BlockPos tile : tiles) helper.getLevel().setBlockEntity(load(helper, tile, saved.get(tile)));
                })
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    TransportLine<ItemStack> line = tileAt(helper, FIRST_TILE).line();
                    int onGround = helper.getEntities(EntityType.ITEM).size();
                    if (line == null || line.tileCount() != SAVED_TILES || line.size() != SAVED_SUPPLY || onGround != 0) {
                        helper.fail("the reloaded tiles make a line of "
                                + (line == null ? "none" : line.tileCount() + " tiles holding " + line.size())
                                + " with " + onGround + " on the ground, expected " + SAVED_TILES
                                + " tiles holding " + SAVED_SUPPLY, FIRST_TILE);
                    }
                })
                .thenSucceed();
    }

    // Placed a tick after the line formed, so the line has to learn of it.
    private static void tilePlacedPastTheEnd(GameTestHelper helper) {
        line(helper, SOURCE, FROM, FIRST_TILE, 2, false, 2);
        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> helper.setBlock(FIRST_TILE.east(2), BeltTileTests.tile(BeltTier.BELT, Direction.EAST)))
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    TransportLine<ItemStack> line = tileAt(helper, FIRST_TILE).line();
                    if (line == null || line.tileCount() != 3) {
                        helper.fail("a tile placed past a two-tile line's end left a line of "
                                + (line == null ? "none" : line.tileCount()) + " tiles, expected 3", FIRST_TILE);
                    }
                })
                .thenSucceed();
    }

    // A chunk not marked unsaved is skipped by the next save, so a tile would save what it held
    // at the last one and a reload would lose or repeat what moved since.
    // A dead end, since a loader in a chunk marks it itself on every item it takes.
    private static void everyChunkMarked(GameTestHelper helper) {
        List<BlockPos> tiles = line(helper, EDGE_SOURCE, EDGE_FROM, EDGE_FIRST_TILE, EDGE_TILES, false, EDGE_TILES);
        BeltTileTests.fill(helper, EDGE_SOURCE, EDGE_SUPPLY);
        Map<ChunkPos, BlockPos> crossed = new LinkedHashMap<>();
        for (BlockPos tile : tiles) crossed.putIfAbsent(ChunkPos.containing(helper.absolutePos(tile)), tile);

        helper.startSequence()
                .thenIdle(EDGE_MOVING_TICKS - 1)
                .thenExecute(() -> {
                    if (crossed.size() < 2) helper.fail("the line lies in one chunk, so it crosses no edge", EDGE_FIRST_TILE);
                    for (BlockPos tile : crossed.values()) helper.getLevel().getChunkAt(helper.absolutePos(tile)).tryMarkSaved();
                })
                .thenIdle(1)
                .thenExecute(() -> {
                    for (var chunk : crossed.entrySet()) {
                        if (!helper.getLevel().getChunkAt(helper.absolutePos(chunk.getValue())).isUnsaved()) {
                            helper.fail("chunk " + chunk.getKey() + " of a moving line was not marked for saving", chunk.getValue());
                        }
                    }
                })
                .thenSucceed();
    }

    private static void unloadedChunk(GameTestHelper helper, int step) {
        List<BlockPos> tiles = line(helper, EDGE_SOURCE, EDGE_FROM, EDGE_FIRST_TILE, EDGE_TILES, true, step);
        BlockPos target = tiles.getLast().east(2);
        BeltTileTests.fill(helper, EDGE_SOURCE, EDGE_SUPPLY);

        ChunkPos farChunk = ChunkPos.containing(helper.absolutePos(tiles.getLast()));
        List<BlockPos> far = tiles.stream().filter(tile -> ChunkPos.containing(helper.absolutePos(tile)).equals(farChunk)).toList();
        int near = tiles.size() - far.size();

        Map<BlockPos, CompoundTag> saved = new LinkedHashMap<>();
        int[] deliveredAtUnload = new int[1];
        helper.startSequence()
                .thenIdle(EDGE_FLOWING_TICKS)
                .thenExecute(() -> {
                    if (near == 0) helper.fail("the line lies in one chunk, so it crosses no edge", EDGE_FIRST_TILE);
                    int onFar = 0;
                    for (BlockPos tile : far) onFar += tileAt(helper, tile).held().size();
                    if (onFar == 0) helper.fail("no item is on the far chunk's tiles, so this proves little", far.getFirst());
                    conserved(helper, target);
                    deliveredAtUnload[0] = BeltTileTests.count(BeltTileTests.chest(helper, target));
                    saved.putAll(save(helper, far));
                    List<BlockEntity> going = new ArrayList<>();
                    for (BlockPos tile : far) going.add(tileAt(helper, tile));
                    BeltTileBlockEntity.chunkUnloading(going);
                })
                .thenIdle(EDGE_UNLOADED_TICKS)
                .thenExecute(() -> {
                    TransportLine<ItemStack> line = tileAt(helper, EDGE_FIRST_TILE).line();
                    if (line == null || line.tileCount() != near) {
                        helper.fail("with the far chunk unloaded the line runs over "
                                + (line == null ? "none" : line.tileCount()) + " tiles, expected the " + near
                                + " on the loaded side", EDGE_FIRST_TILE);
                    }
                    int delivered = BeltTileTests.count(BeltTileTests.chest(helper, target));
                    if (delivered != deliveredAtUnload[0]) {
                        helper.fail("the target received " + (delivered - deliveredAtUnload[0])
                                + " items through an unloaded chunk", target);
                    }
                    for (BlockPos tile : far) helper.getLevel().setBlockEntity(load(helper, tile, saved.get(tile)));
                })
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> {
                    TransportLine<ItemStack> line = tileAt(helper, EDGE_FIRST_TILE).line();
                    if (line == null || line.tileCount() != EDGE_TILES) {
                        helper.fail("the reloaded chunk left a line of "
                                + (line == null ? "none" : line.tileCount()) + " tiles, expected " + EDGE_TILES,
                                EDGE_FIRST_TILE);
                    }
                    conserved(helper, target);
                })
                .thenWaitUntil(() -> {
                    int arrived = BeltTileTests.count(BeltTileTests.chest(helper, target));
                    if (arrived != EDGE_SUPPLY) {
                        helper.fail("the target holds " + arrived + " of " + EDGE_SUPPLY, target);
                    }
                })
                .thenExecute(() -> {
                    int onGround = helper.getEntities(EntityType.ITEM).size();
                    int onLine = tileAt(helper, EDGE_FIRST_TILE).line().size();
                    if (onGround != 0 || onLine != 0) {
                        helper.fail("with every item delivered, " + onLine + " are still on the line and "
                                + onGround + " on the ground", EDGE_FIRST_TILE);
                    }
                })
                .thenSucceed();
    }

    // Every item is in the source, on the line or in the target: none lost, none made.
    private static void conserved(GameTestHelper helper, BlockPos target) {
        TransportLine<ItemStack> line = tileAt(helper, EDGE_FIRST_TILE).line();
        int total = BeltTileTests.count(BeltTileTests.chest(helper, EDGE_SOURCE))
                + BeltTileTests.count(BeltTileTests.chest(helper, target))
                + (line == null ? 0 : line.size());
        if (total != EDGE_SUPPLY) {
            helper.fail("the source, the line and the target hold " + total + " items, expected " + EDGE_SUPPLY,
                    EDGE_FIRST_TILE);
        }
    }

    /**
     * A chest, an east-facing loader, tiles running east with those from {@code step} on a block up,
     * and, if asked, a loader and a chest past them at the last tile's height.
     */
    private static List<BlockPos> line(GameTestHelper helper, BlockPos source, BlockPos from, BlockPos first, int count,
            boolean delivering, int step) {
        helper.setBlock(source, Blocks.CHEST);
        helper.setBlock(from, BeltTileTests.loader(BeltTier.BELT, Direction.EAST));
        List<BlockPos> tiles = BeltTileTests.climb(helper, BeltTier.BELT, first, count, step);
        if (delivering) {
            BlockPos to = tiles.getLast().east();
            if (step < count) {
                helper.setBlock(to.below(), Blocks.STONE);
                helper.setBlock(to.east().below(), Blocks.STONE);
            }
            helper.setBlock(to, BeltTileTests.loader(BeltTier.BELT, Direction.WEST));
            helper.setBlock(to.east(), Blocks.CHEST);
        }
        return tiles;
    }

    private static Map<BlockPos, CompoundTag> save(GameTestHelper helper, List<BlockPos> tiles) {
        var registries = helper.getLevel().registryAccess();
        Map<BlockPos, CompoundTag> saved = new LinkedHashMap<>();
        for (BlockPos tile : tiles) saved.put(tile, tileAt(helper, tile).saveWithFullMetadata(registries));
        return saved;
    }

    private static BeltTileBlockEntity load(GameTestHelper helper, BlockPos tile, CompoundTag tag) {
        BlockPos absolute = helper.absolutePos(tile);
        return (BeltTileBlockEntity) BlockEntity.loadStatic(absolute, helper.getLevel().getBlockState(absolute), tag,
                helper.getLevel().registryAccess());
    }

    private static BeltTileBlockEntity tileAt(GameTestHelper helper, BlockPos tile) {
        return helper.getBlockEntity(tile, BeltTileBlockEntity.class);
    }

    private static BeltContents.Entry<ItemStack> only(GameTestHelper helper) {
        TransportLine<ItemStack> line = tileAt(helper, FIRST_TILE).line();
        if (line == null || line.size() != 1) {
            helper.fail("the line holds " + (line == null ? "nothing" : line.size() + " items") + ", expected one", FIRST_TILE);
        }
        return line.contents().entries().getFirst();
    }

    // The loaders and every tile: whichever of them the line's items change.
    private static void watch(GameTestHelper helper, List<BlockPos> tiles) {
        for (BlockPos pos : watched(tiles)) BlockUpdateWatch.watch(helper.absolutePos(pos));
    }

    private static int stop(GameTestHelper helper, List<BlockPos> tiles) {
        int updates = 0;
        for (BlockPos pos : watched(tiles)) updates += BlockUpdateWatch.stop(helper.absolutePos(pos));
        return updates;
    }

    private static List<BlockPos> watched(List<BlockPos> tiles) {
        List<BlockPos> watched = new ArrayList<>(tiles);
        watched.add(tiles.getFirst().west());
        watched.add(tiles.getLast().east());
        return watched;
    }

    private static boolean same(List<TransportLine.Share<ItemStack>> was, List<TransportLine.Share<ItemStack>> is) {
        if (was.size() != is.size()) return false;
        for (int i = 0; i < was.size(); i++) {
            if (was.get(i).offset() != is.get(i).offset() || !ItemStack.matches(was.get(i).payload(), is.get(i).payload())) {
                return false;
            }
        }
        return true;
    }

    private static String describe(List<TransportLine.Share<ItemStack>> shares) {
        return shares.stream().map(share -> share.payload() + "@" + share.offset()).toList().toString();
    }
}
