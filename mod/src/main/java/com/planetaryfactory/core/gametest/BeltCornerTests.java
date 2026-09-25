package com.planetaryfactory.core.gametest;

import com.planetaryfactory.core.PFBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import io.github._5thlayer.beltworks.blocks.BeltTileBlock;
import io.github._5thlayer.beltworks.blocks.BeltTileBlockEntity;
import io.github._5thlayer.beltworks.model.BeltTier;

/**
 * One-block corners by Factorio's rule (#391): an L of tiles, up a column and along a row, is one
 * transport line through its corner, delivers at its tier's rate, loses nothing, and holds eight a
 * tile when it backs up. A tile fed from its side with a tile behind it stays straight.
 *
 * <p>Tiles are placed downstream first, since a tile's shape is set when what feeds it is placed.
 * The rates are typed, as in {@link BeltTileTests}.
 */
final class BeltCornerTests {

    // A chest south of a north-facing loader, a column north, the corner, a row east, a loader, a chest.
    private static final BlockPos SOURCE = new BlockPos(2, 1, 6);
    private static final BlockPos FROM = new BlockPos(2, 1, 5);
    private static final BlockPos CORNER = new BlockPos(2, 1, 2);
    private static final int COLUMN = 2;
    private static final int ROW = 3;
    private static final int TILES = COLUMN + 1 + ROW;
    private static final BlockPos TO = CORNER.east(ROW + 1);
    private static final BlockPos TARGET = TO.east();
    private static final BlockPos POLE = new BlockPos(4, 1, 4);

    private static final int TIER_1_ITEMS_PER_SECOND = 15;
    private static final int TIER_4_ITEMS_PER_SECOND = 60;
    private static final int ITEMS = 32;
    private static final int RATE_WINDOW_TICKS = 200;
    private static final int RATE_WARMUP_TICKS = 100;
    private static final int RATE_SUPPLY = 27 * 64;
    private static final int SETTLED_TICKS = 400;

    // A straight row east with a column feeding the side of its middle tile.
    private static final BlockPos ROW_SOURCE = new BlockPos(2, 1, 2);
    private static final BlockPos ROW_FROM = ROW_SOURCE.east();
    private static final BlockPos ROW_FIRST = ROW_FROM.east();
    private static final BlockPos SIDE_LOADED = ROW_FIRST.east();
    private static final BlockPos ROW_TO = ROW_FIRST.east(3);
    private static final BlockPos ROW_TARGET = ROW_TO.east();

    private BeltCornerTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("an_l_of_tiles_turns_at_its_corner_and_delivers_every_item", 300, BeltCornerTests::lDeliversEverything);
        tests.test("an_l_of_tier_1_tiles_delivers_" + TIER_1_ITEMS_PER_SECOND,
                RATE_WARMUP_TICKS + RATE_WINDOW_TICKS + 20,
                helper -> lDeliversAtRate(helper, BeltTier.BELT, TIER_1_ITEMS_PER_SECOND));
        tests.test("an_l_of_tier_4_tiles_delivers_" + TIER_4_ITEMS_PER_SECOND,
                RATE_WARMUP_TICKS + RATE_WINDOW_TICKS + 20,
                helper -> lDeliversAtRate(helper, BeltTier.TURBO, TIER_4_ITEMS_PER_SECOND));
        tests.test("a_loader_beside_a_line_head_turns_it_and_loads_it", 300, BeltCornerTests::loaderBesideHead);
        tests.test("a_backed_up_l_holds_8_a_tile", SETTLED_TICKS + 20, BeltCornerTests::backedUpLHolds);
        tests.test("a_side_loaded_tile_stays_straight_and_its_line_carries_" + TIER_1_ITEMS_PER_SECOND,
                RATE_WARMUP_TICKS + RATE_WINDOW_TICKS + 20, BeltCornerTests::sideLoadStaysStraight);
    }

    private static void lDeliversEverything(GameTestHelper helper) {
        placeL(helper, BeltTier.BELT, true);
        BeltTileTests.fill(helper, SOURCE, ITEMS);
        helper.startSequence().thenIdle(20).thenExecute(() -> {
            var shape = helper.getBlockState(CORNER).getValue(BeltTileBlock.CORNER);
            if (shape != BeltTileBlock.Shape.FROM_RIGHT) {
                helper.fail("the tile the column feeds from its right is " + shape + ", expected a corner", CORNER);
            }
            var line = helper.getBlockEntity(FROM.north(), BeltTileBlockEntity.class).line();
            if (line == null || line.tileCount() != TILES) {
                helper.fail("the L's first tile is on a line of " + (line == null ? 0 : line.tileCount())
                        + " tiles, expected one line of " + TILES, FROM.north());
            }
        }).thenIdle(260).thenExecute(() -> {
            int arrived = BeltTileTests.count(BeltTileTests.chest(helper, TARGET));
            int left = BeltTileTests.count(BeltTileTests.chest(helper, SOURCE));
            if (arrived != ITEMS || left != 0) {
                helper.fail("an L delivered " + arrived + " of " + ITEMS + " and left " + left + " behind", TARGET);
            }
        }).thenSucceed();
    }

    private static void lDeliversAtRate(GameTestHelper helper, BeltTier tier, int itemsPerSecond) {
        if (tier != BeltTier.BELT) helper.setBlock(POLE, PFBlocks.CREATIVE_POLE.get());
        placeL(helper, tier, true);
        BeltTileTests.fill(helper, SOURCE, RATE_SUPPLY);
        int expected = itemsPerSecond * RATE_WINDOW_TICKS / 20;
        int[] before = new int[1];
        helper.startSequence()
                .thenIdle(RATE_WARMUP_TICKS)
                .thenExecute(() -> before[0] = BeltTileTests.count(BeltTileTests.chest(helper, TARGET)))
                .thenIdle(RATE_WINDOW_TICKS)
                .thenExecute(() -> {
                    int delivered = BeltTileTests.count(BeltTileTests.chest(helper, TARGET)) - before[0];
                    if (delivered != expected) {
                        helper.fail("a tier-" + tier.number() + " L delivered " + delivered + " in "
                                + RATE_WINDOW_TICKS + " ticks, expected " + expected, TARGET);
                    }
                })
                .thenSucceed();
    }

    // The loader stands north of the head and faces south into it, so the head turns east.
    private static void loaderBesideHead(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.CHEST);
        helper.setBlock(TO, BeltTileTests.loader(BeltTier.BELT, Direction.WEST));
        for (int tile = ROW; tile >= 0; tile--) helper.setBlock(CORNER.east(tile), BeltTileTests.tile(BeltTier.BELT, Direction.EAST));
        helper.setBlock(CORNER.north(), BeltTileTests.loader(BeltTier.BELT, Direction.SOUTH));
        helper.setBlock(CORNER.north(2), Blocks.CHEST);
        BeltTileTests.fill(helper, CORNER.north(2), ITEMS);
        helper.startSequence().thenIdle(20).thenExecute(() -> {
            var shape = helper.getBlockState(CORNER).getValue(BeltTileBlock.CORNER);
            if (shape != BeltTileBlock.Shape.FROM_LEFT) {
                helper.fail("the head a loader feeds from its left is " + shape + ", expected a corner", CORNER);
            }
        }).thenIdle(260).thenExecute(() -> {
            int arrived = BeltTileTests.count(BeltTileTests.chest(helper, TARGET));
            if (arrived != ITEMS) {
                helper.fail("a head loaded from its side delivered " + arrived + " of " + ITEMS, TARGET);
            }
        }).thenSucceed();
    }

    // Nothing past the row's last tile, so the L backs up against its own end.
    private static void backedUpLHolds(GameTestHelper helper) {
        placeL(helper, BeltTier.BELT, false);
        BeltTileTests.fill(helper, SOURCE, RATE_SUPPLY);
        helper.startSequence().thenIdle(SETTLED_TICKS).thenExecute(() -> {
            var line = helper.getBlockEntity(CORNER, BeltTileBlockEntity.class).line();
            int held = line == null ? 0 : line.size();
            if (held != TILES * 8) {
                helper.fail("a backed-up L of " + TILES + " tiles holds " + held + ", expected " + TILES * 8, CORNER);
            }
        }).thenSucceed();
    }

    // The side-loading column has no loader, so the row's rate is the row's own.
    private static void sideLoadStaysStraight(GameTestHelper helper) {
        helper.setBlock(ROW_TARGET, Blocks.CHEST);
        helper.setBlock(ROW_TO, BeltTileTests.loader(BeltTier.BELT, Direction.WEST));
        for (int tile = 2; tile >= 0; tile--) {
            helper.setBlock(ROW_FIRST.east(tile), BeltTileTests.tile(BeltTier.BELT, Direction.EAST));
        }
        helper.setBlock(ROW_FROM, BeltTileTests.loader(BeltTier.BELT, Direction.EAST));
        helper.setBlock(ROW_SOURCE, Blocks.CHEST);
        helper.setBlock(SIDE_LOADED.south(), BeltTileTests.tile(BeltTier.BELT, Direction.NORTH));
        helper.setBlock(SIDE_LOADED.south(2), BeltTileTests.tile(BeltTier.BELT, Direction.NORTH));
        BeltTileTests.fill(helper, ROW_SOURCE, RATE_SUPPLY);
        int expected = TIER_1_ITEMS_PER_SECOND * RATE_WINDOW_TICKS / 20;
        int[] before = new int[1];
        helper.startSequence()
                .thenIdle(RATE_WARMUP_TICKS)
                .thenExecute(() -> {
                    var shape = helper.getBlockState(SIDE_LOADED).getValue(BeltTileBlock.CORNER);
                    if (shape != BeltTileBlock.Shape.STRAIGHT) {
                        helper.fail("a side-loaded tile with a tile behind it is " + shape, SIDE_LOADED);
                    }
                    before[0] = BeltTileTests.count(BeltTileTests.chest(helper, ROW_TARGET));
                })
                .thenIdle(RATE_WINDOW_TICKS)
                .thenExecute(() -> {
                    int delivered = BeltTileTests.count(BeltTileTests.chest(helper, ROW_TARGET)) - before[0];
                    if (delivered != expected) {
                        helper.fail("the side-loaded row delivered " + delivered + ", expected " + expected, ROW_TARGET);
                    }
                })
                .thenSucceed();
    }

    private static void placeL(GameTestHelper helper, BeltTier tier, boolean delivers) {
        if (delivers) {
            helper.setBlock(TARGET, Blocks.CHEST);
            helper.setBlock(TO, BeltTileTests.loader(tier, Direction.WEST));
        }
        for (int tile = ROW; tile >= 1; tile--) helper.setBlock(CORNER.east(tile), BeltTileTests.tile(tier, Direction.EAST));
        helper.setBlock(CORNER, BeltTileTests.tile(tier, Direction.EAST));
        for (int tile = 1; tile <= COLUMN; tile++) helper.setBlock(CORNER.south(tile), BeltTileTests.tile(tier, Direction.NORTH));
        helper.setBlock(FROM, BeltTileTests.loader(tier, Direction.NORTH));
        helper.setBlock(SOURCE, Blocks.CHEST);
    }
}
