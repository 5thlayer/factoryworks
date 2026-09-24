package com.planetaryfactory.core.gametest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.energy.PoleTier;
import com.planetaryfactory.core.energy.SupplyAreaPoleBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import rearth.belts.BlockContent;
import rearth.belts.ItemContent;
import rearth.belts.blocks.BeltTileBlock;
import rearth.belts.blocks.BeltTileBlockEntity;
import rearth.belts.blocks.ChuteBlockEntity;
import rearth.belts.model.BeltTier;

/**
 * A line of belt tiles carrying items between two loaders (#398): chest, loader, tiles, loader,
 * chest, at each tier's Factorio rate, at its slowest piece when the pieces differ, holding eight
 * items a tile when it backs up, and paying the loaders' FE per item.
 *
 * <p>A tile's facing is its direction of travel, so the loaders face the same way the line runs:
 * the loading one from behind the head tile, the unloading one back along the line past the last.
 * Each pulls from, and pushes into, the inventory behind its own facing.
 *
 * <p>The rates and the capacity are typed rather than read off the fork, so the test cannot agree
 * with the fork by construction; {@code tests/factorio/test_logistics_extract.py} derives the same
 * figures from Factorio's belt prototypes.
 */
final class BeltTileTests {

    private static final BlockPos SOURCE = new BlockPos(2, 1, 3);
    private static final BlockPos FROM = new BlockPos(3, 1, 3);
    private static final BlockPos FIRST_TILE = new BlockPos(4, 1, 3);
    // Off the line, with both loaders in a small pole's 5x5 area.
    private static final BlockPos POLE = new BlockPos(5, 1, 4);

    private static final int TILES = 3;

    private static final String EXPRESS_BELT_RECIPE = "planetaryfactory:assembling/express_transport_belt";

    private static final int TIER_1_ITEMS_PER_SECOND = 15;
    private static final int TIER_2_ITEMS_PER_SECOND = 30;
    private static final int TIER_3_ITEMS_PER_SECOND = 45;
    private static final int TIER_4_ITEMS_PER_SECOND = 60;

    private static final int ITEMS = 16;
    // A multiple of four ticks: tiers 1 and 3 deliver a whole number of items only every four.
    private static final int RATE_WINDOW_TICKS = 200;
    private static final int RATE_WARMUP_TICKS = 100;
    // A full chest, so the source outlasts the window at tier 4.
    private static final int RATE_SUPPLY = 27 * 64;

    private static final int LONG_LINE_TILES = 64;
    private static final int LONG_LINE_HOLDS = 512;
    private static final BlockPos LONG_SOURCE = new BlockPos(0, 1, 1);
    private static final BlockPos LONG_FROM = new BlockPos(1, 1, 1);
    private static final BlockPos LONG_FIRST_TILE = LONG_FROM.east();
    private static final int LONG_SUPPLY = 640;
    private static final int LONG_LINE_SETTLED_TICKS = 1000;

    private static final int MERGE_TILES = 3;
    // The second line's chest and loader, which the merge replaces with two tiles.
    private static final BlockPos MIDDLE_SOURCE = FIRST_TILE.east(MERGE_TILES);
    private static final BlockPos MIDDLE_FROM = MIDDLE_SOURCE.east();
    private static final BlockPos MIDDLE_FIRST_TILE = MIDDLE_FROM.east();
    // Not the first or the last tile, so both halves of a split are runs of their own.
    private static final int MERGE_GAP = 1;
    private static final int MERGE_SUPPLY = 27 * 64;
    // Long enough for a three-tile line to back up against its own end.
    private static final int MERGE_FILL_TICKS = 400;
    // A placed or broken tile scans on its next tick and its head on the tick after.
    private static final int MERGE_SETTLE_TICKS = 5;

    private static final int TIER_2_JOULES_PER_ITEM = 6650;
    private static final int TIER_2_DRAIN_WATTS = 400;
    private static final int TIER_2_LOADER_BUFFER_FE = 200;
    private static final int JOULES_PER_FE = 100;
    private static final int UNPOWERED_TICKS = 60;
    private static final int FED_WARMUP_TICKS = 40;
    // One tick: the buffer holds only the largest tick the loader's flow limit allows.
    private static final int DRAW_WINDOW_TICKS = 1;

    // Each tile's height above the platform, so a climb of one block is a foot and a top (#417).
    private static final int[] ONE_BLOCK_CLIMB = {0, 0, 1, 1};
    private static final int[] OVER_A_STEP = {0, 0, 1, 1, 0, 0};
    // A foot, two middles and a top between level tiles (#418).
    private static final int[] THREE_BLOCK_CLIMB = {0, 0, 1, 2, 3, 3};
    private static final int[] THREE_BLOCK_DESCENT = {3, 3, 2, 1, 0, 0};
    private static final List<BeltTileBlock.PitchState> THREE_BLOCK_CLIMB_PITCHES = List.of(
            BeltTileBlock.PitchState.LEVEL, BeltTileBlock.PitchState.FOOT_UP, BeltTileBlock.PitchState.MIDDLE_UP,
            BeltTileBlock.PitchState.MIDDLE_UP, BeltTileBlock.PitchState.TOP_UP, BeltTileBlock.PitchState.LEVEL);
    private static final List<BeltTileBlock.PitchState> THREE_BLOCK_DESCENT_PITCHES = List.of(
            BeltTileBlock.PitchState.LEVEL, BeltTileBlock.PitchState.TOP_DOWN, BeltTileBlock.PitchState.MIDDLE_DOWN,
            BeltTileBlock.PitchState.MIDDLE_DOWN, BeltTileBlock.PitchState.FOOT_DOWN, BeltTileBlock.PitchState.LEVEL);
    private static final int CLIMB_HOLDS = 6 * 8;
    private static final int CLIMB_SUPPLY = 27 * 64;
    private static final int CLIMB_SETTLED_TICKS = 400;

    private BeltTileTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("belt_tiles_hand_items_from_chest_to_chest", 400, BeltTileTests::handsOff);
        rateTest(tests, BeltTier.BELT, TIER_1_ITEMS_PER_SECOND);
        rateTest(tests, BeltTier.IMPROVED, TIER_2_ITEMS_PER_SECOND);
        rateTest(tests, BeltTier.EXPRESS, TIER_3_ITEMS_PER_SECOND);
        rateTest(tests, BeltTier.TURBO, TIER_4_ITEMS_PER_SECOND);
        tests.test("belt_tiles_tier_3_between_tier_1_loaders_deliver_" + TIER_1_ITEMS_PER_SECOND,
                RATE_WARMUP_TICKS + RATE_WINDOW_TICKS + 20,
                helper -> deliversAtRate(helper, BeltTier.BELT, tiers(BeltTier.EXPRESS), TIER_1_ITEMS_PER_SECOND));
        tests.test("belt_tiles_tier_1_between_tier_4_loaders_deliver_" + TIER_1_ITEMS_PER_SECOND,
                RATE_WARMUP_TICKS + RATE_WINDOW_TICKS + 20,
                helper -> deliversAtRate(helper, BeltTier.TURBO, tiers(BeltTier.BELT), TIER_1_ITEMS_PER_SECOND));
        tests.test("belt_tiles_of_mixed_tiers_deliver_" + TIER_1_ITEMS_PER_SECOND,
                RATE_WARMUP_TICKS + RATE_WINDOW_TICKS + 20,
                helper -> deliversAtRate(helper, BeltTier.TURBO,
                        List.of(BeltTier.EXPRESS, BeltTier.BELT, BeltTier.EXPRESS), TIER_1_ITEMS_PER_SECOND));
        tests.test("backed_up_64_tile_line_holds_" + LONG_LINE_HOLDS, LONG_LINE_SETTLED_TICKS + 20,
                PFGameTests.LONG_PLATFORM, BeltTileTests::backedUpLineHolds512);
        tests.test("tier_1_loader_moves_onto_tiles_with_no_power", UNPOWERED_TICKS + 20,
                BeltTileTests::tierOneRunsUnpowered);
        tests.test("tier_2_loader_with_no_fe_puts_nothing_on_tiles", UNPOWERED_TICKS + 20,
                BeltTileTests::tierTwoStallsUnpowered);
        tests.test("pole_fed_tier_2_loader_on_tiles_draws_66_5_fe_per_item",
                FED_WARMUP_TICKS + DRAW_WINDOW_TICKS + 20, BeltTileTests::fedLoaderDrawsPerItem);
        tests.test("loader_demand_probe_leaves_nothing_behind", 100, BeltTileTests::probeLeavesNothing);
        tests.test("belts_own_recipes_are_swept", 20, BeltTileTests::ownRecipesAreSwept);
        tests.test("a_line_built_by_hand_carries_items", 200, BeltTileTests::lineBuiltByHand);
        tests.test("a_tile_placed_between_two_lines_merges_them", MERGE_FILL_TICKS + 20,
                BeltTileTests::placingATileMergesTwoLines);
        tests.test("breaking_a_mid_line_tile_splits_it_and_keeps_both_halves_items",
                MERGE_FILL_TICKS + 20, BeltTileTests::breakingATileSplitsTheLine);
        tests.test("a_tile_placed_past_an_empty_lines_end_joins_it", 200,
                helper -> tilePlacedPastTheEnd(helper, false));
        tests.test("a_tile_placed_past_a_loaded_lines_end_joins_it", 200,
                helper -> tilePlacedPastTheEnd(helper, true));
        tests.test("a_tile_placed_above_ahead_makes_a_foot_and_a_top", 40, BeltTileTests::stepUpByHand);
        tests.test("a_tile_placed_below_ahead_makes_a_descent", 40, BeltTileTests::stepDownByHand);
        tests.test("a_crest_and_a_valley_connect_to_neither", 40, BeltTileTests::crestAndValley);
        tests.test("belt_tiles_over_a_climb_tier_1_deliver_" + TIER_1_ITEMS_PER_SECOND,
                RATE_WARMUP_TICKS + RATE_WINDOW_TICKS + 20,
                helper -> climbDeliversAtRate(helper, BeltTier.BELT, TIER_1_ITEMS_PER_SECOND, ONE_BLOCK_CLIMB));
        tests.test("belt_tiles_over_a_climb_tier_4_deliver_" + TIER_4_ITEMS_PER_SECOND,
                RATE_WARMUP_TICKS + RATE_WINDOW_TICKS + 20,
                helper -> climbDeliversAtRate(helper, BeltTier.TURBO, TIER_4_ITEMS_PER_SECOND, ONE_BLOCK_CLIMB));
        tests.test("a_backed_up_line_up_and_down_a_step_holds_" + CLIMB_HOLDS, CLIMB_SETTLED_TICKS + 20,
                helper -> backedUpClimbHolds(helper, OVER_A_STEP));
        tests.test("a_three_block_staircase_makes_a_foot_two_middles_and_a_top", 40, BeltTileTests::staircaseByHand);
        tests.test("breaking_a_middle_connects_nothing_across_the_gap", 40, BeltTileTests::breakingAMiddle);
        tests.test("belt_tiles_over_a_three_block_climb_tier_1_deliver_" + TIER_1_ITEMS_PER_SECOND,
                RATE_WARMUP_TICKS + RATE_WINDOW_TICKS + 20,
                helper -> climbDeliversAtRate(helper, BeltTier.BELT, TIER_1_ITEMS_PER_SECOND, THREE_BLOCK_CLIMB));
        tests.test("belt_tiles_over_a_three_block_climb_tier_4_deliver_" + TIER_4_ITEMS_PER_SECOND,
                RATE_WARMUP_TICKS + RATE_WINDOW_TICKS + 20,
                helper -> climbDeliversAtRate(helper, BeltTier.TURBO, TIER_4_ITEMS_PER_SECOND, THREE_BLOCK_CLIMB));
        tests.test("a_backed_up_line_over_a_three_block_climb_holds_" + CLIMB_HOLDS, CLIMB_SETTLED_TICKS + 20,
                helper -> backedUpClimbHolds(helper, THREE_BLOCK_CLIMB));
    }

    /**
     * Tiles running east from {@code first}, those from {@code step} on standing a block up on stone,
     * so the tile before the step is a foot and the one after it a top. Returns them in order.
     */
    static List<BlockPos> climb(GameTestHelper helper, BeltTier tier, BlockPos first, int count, int step) {
        int[] heights = new int[count];
        Arrays.fill(heights, step, count, 1);
        return stairs(helper, tier, first, heights);
    }

    /** Tiles running east from {@code first}, each on a column of stone as tall as its height. */
    static List<BlockPos> stairs(GameTestHelper helper, BeltTier tier, BlockPos first, int... heights) {
        List<BlockPos> tiles = new ArrayList<>();
        for (int tile = 0; tile < heights.length; tile++) {
            BlockPos at = stone(helper, first.east(tile), heights[tile]);
            helper.setBlock(at, tile(tier, Direction.EAST));
            tiles.add(at);
        }
        return tiles;
    }

    // Returns the block on top of the column.
    private static BlockPos stone(GameTestHelper helper, BlockPos base, int height) {
        for (int block = 0; block < height; block++) helper.setBlock(base.above(block), Blocks.STONE);
        return base.above(height);
    }

    static BeltTileBlock.PitchState pitch(GameTestHelper helper, BlockPos tile) {
        return helper.getBlockState(tile).getValue(BeltTileBlock.PITCH);
    }

    private static void expectPitch(GameTestHelper helper, BlockPos tile, BeltTileBlock.PitchState expected, String when) {
        BeltTileBlock.PitchState pitch = pitch(helper, tile);
        if (pitch != expected) helper.fail(when + ", the tile is " + pitch + ", expected " + expected, tile);
    }

    private static void expectLine(GameTestHelper helper, BlockPos tile, int tiles, String when) {
        var line = helper.getBlockEntity(tile, BeltTileBlockEntity.class).line();
        int count = line == null ? 0 : line.tileCount();
        if (count != tiles) helper.fail(when + ", the tile is in a line of " + count + ", expected " + tiles, tile);
    }

    // Placed by a player's click on the step's top, read on the same tick: the placed state is the
    // one derived, and the tile it reshapes is a block down and behind, which no neighbour update reaches.
    private static void stepUpByHand(GameTestHelper helper) {
        helper.setBlock(FIRST_TILE, tile(BeltTier.BELT, Direction.EAST));
        helper.setBlock(FIRST_TILE.east(), tile(BeltTier.BELT, Direction.EAST));
        BlockPos step = FIRST_TILE.east(2);
        helper.setBlock(step, Blocks.STONE);
        BlockPos top = step.above();
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        player.setYRot(Direction.EAST.toYRot());
        player.setShiftKeyDown(false);
        use(helper, player, new ItemStack(ItemContent.tileFor(BeltTier.BELT)), step, Direction.UP);

        expectPitch(helper, top, BeltTileBlock.PitchState.TOP_UP, "placed above and ahead");
        expectPitch(helper, FIRST_TILE.east(), BeltTileBlock.PitchState.FOOT_UP, "with a tile placed above and ahead");
        expectPitch(helper, FIRST_TILE, BeltTileBlock.PitchState.LEVEL, "behind a foot");
        helper.destroyBlock(top);
        expectPitch(helper, FIRST_TILE.east(), BeltTileBlock.PitchState.LEVEL, "with its top broken");
        helper.succeed();
    }

    private static void stepDownByHand(GameTestHelper helper) {
        List<BlockPos> upper = List.of(FIRST_TILE.above(), FIRST_TILE.east().above());
        for (BlockPos tile : upper) {
            helper.setBlock(tile.below(), Blocks.STONE);
            helper.setBlock(tile, tile(BeltTier.BELT, Direction.EAST));
        }
        BlockPos foot = FIRST_TILE.east(2);
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        player.setYRot(Direction.EAST.toYRot());
        player.setShiftKeyDown(false);
        use(helper, player, new ItemStack(ItemContent.tileFor(BeltTier.BELT)), foot.below(), Direction.UP);

        expectPitch(helper, foot, BeltTileBlock.PitchState.FOOT_DOWN, "placed below and ahead");
        expectPitch(helper, upper.getLast(), BeltTileBlock.PitchState.TOP_DOWN, "with a tile placed below and ahead");
        helper.destroyBlock(foot);
        expectPitch(helper, upper.getLast(), BeltTileBlock.PitchState.LEVEL, "with its foot broken");
        helper.succeed();
    }

    // Placed with commands, so a tile placed in a state it did not derive waits a tick for its own.
    private static void crestAndValley(GameTestHelper helper) {
        BlockPos crest = FIRST_TILE.east().above();
        helper.setBlock(FIRST_TILE, tile(BeltTier.BELT, Direction.EAST));
        helper.setBlock(crest.below(), Blocks.STONE);
        helper.setBlock(crest, tile(BeltTier.BELT, Direction.EAST));
        helper.setBlock(FIRST_TILE.east(2), tile(BeltTier.BELT, Direction.EAST));

        BlockPos valley = FIRST_TILE.east(4).south(2);
        for (BlockPos high : List.of(valley.west().above(), valley.east().above())) {
            helper.setBlock(high.below(), Blocks.STONE);
            helper.setBlock(high, tile(BeltTier.BELT, Direction.EAST));
        }
        helper.setBlock(valley, tile(BeltTier.BELT, Direction.EAST));

        helper.startSequence().thenIdle(2).thenExecute(() -> {
            for (BlockPos tile : List.of(FIRST_TILE, crest, FIRST_TILE.east(2), valley.west().above(), valley, valley.east().above())) {
                expectPitch(helper, tile, BeltTileBlock.PitchState.LEVEL, "at a crest or a valley");
            }
            for (BlockPos alone : List.of(crest, valley)) {
                var line = helper.getBlockEntity(alone, BeltTileBlockEntity.class).line();
                if (line == null || line.tileCount() != 1) {
                    helper.fail("the tile is in a line of " + (line == null ? "none" : line.tileCount())
                            + " tiles, expected a line of its own", alone);
                }
            }
        }).thenSucceed();
    }

    // A slope is one block of line, so a climb is no bottleneck (#417).
    private static void climbDeliversAtRate(GameTestHelper helper, BeltTier tier, int itemsPerSecond, int[] heights) {
        if (tier != BeltTier.BELT) helper.setBlock(POLE, PFBlocks.CREATIVE_POLE.get());
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(FROM, loader(tier, Direction.EAST));
        List<BlockPos> tiles = stairs(helper, tier, FIRST_TILE, heights);
        int top = heights[heights.length - 1];
        BlockPos to = stone(helper, FIRST_TILE.east(heights.length), top);
        BlockPos target = stone(helper, FIRST_TILE.east(heights.length + 1), top);
        // The far loader is out of the first pole's area.
        if (tier != BeltTier.BELT) helper.setBlock(stone(helper, FIRST_TILE.east(heights.length).south(), top),
                PFBlocks.CREATIVE_POLE.get());
        helper.setBlock(to, loader(tier, Direction.WEST));
        helper.setBlock(target, Blocks.CHEST);
        fill(helper, SOURCE, RATE_SUPPLY);

        int expected = itemsPerSecond * RATE_WINDOW_TICKS / 20;
        int[] before = new int[1];
        helper.startSequence()
                .thenIdle(RATE_WARMUP_TICKS)
                .thenExecute(() -> {
                    expectLine(helper, FIRST_TILE, tiles.size(), "over a climb");
                    before[0] = count(chest(helper, target));
                })
                .thenIdle(RATE_WINDOW_TICKS)
                .thenExecute(() -> {
                    int delivered = count(chest(helper, target)) - before[0];
                    if (delivered != expected) {
                        helper.fail("a tier-" + tier.number() + " line over a climb delivered " + delivered + " items in "
                                + RATE_WINDOW_TICKS + " ticks, expected " + expected, target);
                    }
                })
                .thenSucceed();
    }

    // Nothing past the last tile, so the line backs up against its own end.
    private static void backedUpClimbHolds(GameTestHelper helper, int[] heights) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(FROM, loader(BeltTier.BELT, Direction.EAST));
        stairs(helper, BeltTier.BELT, FIRST_TILE, heights);
        fill(helper, SOURCE, CLIMB_SUPPLY);

        helper.startSequence().thenIdle(CLIMB_SETTLED_TICKS).thenExecute(() -> {
            expectLine(helper, FIRST_TILE, heights.length, "backed up over a climb");
            int held = held(helper, FIRST_TILE);
            if (held != CLIMB_HOLDS) {
                helper.fail("a backed-up line over a climb holds " + held + ", expected " + CLIMB_HOLDS, FIRST_TILE);
            }
        }).thenSucceed();
    }

    // Placed by a player's clicks upstream first and read on the same tick, as stepUpByHand is.
    private static void staircaseByHand(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        player.setYRot(Direction.EAST.toYRot());
        player.setShiftKeyDown(false);
        List<BlockPos> up = stairsByHand(helper, player, FIRST_TILE.north(2), THREE_BLOCK_CLIMB);
        List<BlockPos> down = stairsByHand(helper, player, FIRST_TILE.south(2), THREE_BLOCK_DESCENT);

        for (int tile = 0; tile < up.size(); tile++) {
            expectPitch(helper, up.get(tile), THREE_BLOCK_CLIMB_PITCHES.get(tile), "up a three-block staircase");
            expectPitch(helper, down.get(tile), THREE_BLOCK_DESCENT_PITCHES.get(tile), "down a three-block staircase");
        }
        helper.succeed();
    }

    private static List<BlockPos> stairsByHand(GameTestHelper helper, ServerPlayer player, BlockPos first, int[] heights) {
        List<BlockPos> tiles = new ArrayList<>();
        for (int tile = 0; tile < heights.length; tile++) tiles.add(stone(helper, first.east(tile), heights[tile]));
        for (BlockPos tile : tiles) {
            use(helper, player, new ItemStack(ItemContent.tileFor(BeltTier.BELT)), tile.below(), Direction.UP);
        }
        return tiles;
    }

    // The tiles either side of the gap are two blocks apart, which is no step.
    private static void breakingAMiddle(GameTestHelper helper) {
        List<BlockPos> tiles = stairs(helper, BeltTier.BELT, FIRST_TILE, THREE_BLOCK_CLIMB);
        helper.startSequence()
                .thenIdle(MERGE_SETTLE_TICKS)
                .thenExecute(() -> {
                    expectLine(helper, FIRST_TILE, tiles.size(), "up a three-block climb");
                    helper.destroyBlock(tiles.get(2));
                })
                .thenIdle(MERGE_SETTLE_TICKS)
                .thenExecute(() -> {
                    expectPitch(helper, tiles.get(1), BeltTileBlock.PitchState.LEVEL, "below a broken middle");
                    expectPitch(helper, tiles.get(3), BeltTileBlock.PitchState.FOOT_UP, "above a broken middle");
                    expectPitch(helper, tiles.get(4), BeltTileBlock.PitchState.TOP_UP, "at the top of a broken climb");
                    expectLine(helper, tiles.get(1), 2, "below a broken middle");
                    expectLine(helper, tiles.get(3), 3, "above a broken middle");
                })
                .thenSucceed();
    }

    /** Every block but the chests placed by a sneaking player's clicks, as a player builds a line. */
    private static void lineBuiltByHand(GameTestHelper helper) {
        List<BeltTier> tiles = tiers(BeltTier.BELT);
        BlockPos target = targetOf(tiles);
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(target, Blocks.CHEST);
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        player.setYRot(Direction.EAST.toYRot());
        // Sneaking past the chest's menu for the loaders.
        player.setShiftKeyDown(true);
        use(helper, player, new ItemStack(BlockContent.loaderFor(BeltTier.BELT).asItem()), SOURCE, Direction.EAST);
        player.setShiftKeyDown(false);
        for (int tile = 0; tile < tiles.size(); tile++) {
            use(helper, player, new ItemStack(ItemContent.tileFor(BeltTier.BELT)),
                    FIRST_TILE.east(tile).below(), Direction.UP);
        }
        player.setShiftKeyDown(true);
        use(helper, player, new ItemStack(BlockContent.loaderFor(BeltTier.BELT).asItem()), target, Direction.WEST);
        player.setShiftKeyDown(false);
        fill(helper, SOURCE, ITEMS);
        helper.startSequence().thenIdle(100).thenExecute(() -> {
            int arrived = count(chest(helper, target));
            if (arrived != ITEMS) {
                helper.fail("a line built by hand delivered " + arrived + " of " + ITEMS + "; from "
                        + helper.getBlockState(FROM) + ", into " + helper.getBlockState(target.west()), target);
            }
        }).thenSucceed();
    }

    private static void use(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos on, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos absolute = helper.absolutePos(on);
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false));
    }

    /**
     * Two loaded lines, each fed by a loader of its own, and the two tiles that join them into one
     * run. The second line's head is holding a live line when the merge is scanned, and block
     * entities tick in an order nothing here decides, so this is where items would be stranded.
     */
    private static void placingATileMergesTwoLines(GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(FROM, loader(BeltTier.BELT, Direction.EAST));
        helper.setBlock(MIDDLE_SOURCE, Blocks.CHEST);
        helper.setBlock(MIDDLE_FROM, loader(BeltTier.BELT, Direction.EAST));
        for (int tile = 0; tile < MERGE_TILES; tile++) {
            helper.setBlock(FIRST_TILE.east(tile), tile(BeltTier.BELT, Direction.EAST));
            helper.setBlock(MIDDLE_FIRST_TILE.east(tile), tile(BeltTier.BELT, Direction.EAST));
        }
        fill(helper, SOURCE, MERGE_SUPPLY);
        fill(helper, MIDDLE_SOURCE, MERGE_SUPPLY);

        int[] before = new int[1];
        helper.startSequence().thenIdle(MERGE_FILL_TICKS).thenExecute(() -> {
            int upstream = held(helper, FIRST_TILE);
            int downstream = held(helper, MIDDLE_FIRST_TILE);
            if (upstream != MERGE_TILES * 8 || downstream != MERGE_TILES * 8) {
                helper.fail("the two lines hold " + upstream + " and " + downstream
                        + " before the merge, expected " + MERGE_TILES * 8 + " each", FIRST_TILE);
                return;
            }
            before[0] = upstream + downstream;
            // Emptied first: a replaced chest would spill its own items onto the floor.
            chest(helper, MIDDLE_SOURCE).clearContent();
            helper.setBlock(MIDDLE_SOURCE, tile(BeltTier.BELT, Direction.EAST));
            helper.setBlock(MIDDLE_FROM, tile(BeltTier.BELT, Direction.EAST));
        }).thenIdle(MERGE_SETTLE_TICKS).thenExecute(() -> {
            var line = helper.getBlockEntity(FIRST_TILE, BeltTileBlockEntity.class).line();
            int tiles = 2 * MERGE_TILES + 2;
            if (line == null || line.tileCount() != tiles) {
                helper.fail("the joined runs left a line of "
                        + (line == null ? "none" : line.tileCount()) + " tiles, expected " + tiles,
                        FIRST_TILE);
                return;
            }
            int carried = line.size();
            int onGround = helper.getEntities(EntityType.ITEM).size();
            if (carried < before[0] || onGround != 0) {
                helper.fail("the merged line carries " + carried + " items with " + onGround
                        + " on the ground, expected at least the " + before[0]
                        + " the two lines held", FIRST_TILE);
                return;
            }
            helper.succeed();
        });
    }

    /** Breaking a tile mid-line: the tiles past the break keep what they were carrying. */
    private static void breakingATileSplitsTheLine(GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(FROM, loader(BeltTier.BELT, Direction.EAST));
        for (int tile = 0; tile < MERGE_TILES; tile++) {
            helper.setBlock(FIRST_TILE.east(tile), tile(BeltTier.BELT, Direction.EAST));
        }
        fill(helper, SOURCE, MERGE_SUPPLY);

        int[] before = new int[1];
        helper.startSequence().thenIdle(MERGE_FILL_TICKS).thenExecute(() -> {
            before[0] = held(helper, FIRST_TILE);
            if (before[0] != MERGE_TILES * 8) {
                helper.fail("the line holds " + before[0] + " before the break, expected "
                        + MERGE_TILES * 8, FIRST_TILE);
            }
            helper.destroyBlock(FIRST_TILE.east(MERGE_GAP));
        }).thenIdle(MERGE_SETTLE_TICKS).thenExecute(() -> {
            int upstream = held(helper, FIRST_TILE);
            int downstream = held(helper, FIRST_TILE.east(MERGE_GAP + 1));
            // The broken tile's own eight are its own; every other item stays on its own side.
            if (upstream != MERGE_GAP * 8 || downstream != (MERGE_TILES - MERGE_GAP - 1) * 8) {
                helper.fail("the break left " + upstream + " upstream and " + downstream
                        + " downstream, expected " + MERGE_GAP * 8 + " and "
                        + (MERGE_TILES - MERGE_GAP - 1) * 8, FIRST_TILE);
                return;
            }
            helper.succeed();
        });
    }

    /**
     * A line of three, then a fourth tile placed by hand past its end on a later tick (#392). The
     * old last tile is not the head, so the head learns of the new tile only through it.
     */
    private static void tilePlacedPastTheEnd(GameTestHelper helper, boolean loaded) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(FROM, loader(BeltTier.BELT, Direction.EAST));
        for (int tile = 0; tile < TILES; tile++) {
            helper.setBlock(FIRST_TILE.east(tile), tile(BeltTier.BELT, Direction.EAST));
        }
        if (loaded) fill(helper, SOURCE, ITEMS);
        BlockPos added = FIRST_TILE.east(TILES);
        BlockPos target = added.east(2);

        helper.startSequence().thenIdle(40).thenExecute(() -> {
            if (loaded && held(helper, FIRST_TILE) == 0) {
                helper.fail("the line carries nothing before the new tile, so this proves little", FIRST_TILE);
            }
            ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
            player.setGameMode(GameType.SURVIVAL);
            player.setYRot(Direction.EAST.toYRot());
            player.setShiftKeyDown(false);
            use(helper, player, new ItemStack(ItemContent.tileFor(BeltTier.BELT)), added.below(), Direction.UP);
            helper.setBlock(added.east(), loader(BeltTier.BELT, Direction.WEST));
            helper.setBlock(target, Blocks.CHEST);
            if (!loaded) fill(helper, SOURCE, ITEMS);
        }).thenIdle(MERGE_SETTLE_TICKS).thenExecute(() -> {
            var line = helper.getBlockEntity(FIRST_TILE, BeltTileBlockEntity.class).line();
            if (line == null || line.tileCount() != TILES + 1) {
                helper.fail("a tile placed past a line of " + TILES + " left a line of "
                        + (line == null ? "none" : line.tileCount()) + " tiles, expected " + (TILES + 1), added);
            }
        }).thenIdle(120).thenExecute(() -> {
            int arrived = count(chest(helper, target));
            if (arrived != ITEMS) {
                helper.fail("the extended line delivered " + arrived + " of " + ITEMS, target);
            }
        }).thenSucceed();
    }

    private static void click(GameTestHelper helper, Player player, BlockPos target) {
        BlockPos absolute = helper.absolutePos(target);
        helper.useBlock(target, player, new BlockHitResult(
                Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false));
    }

    private static List<BeltTier> tiers(BeltTier tier) {
        return Collections.nCopies(TILES, tier);
    }

    private static void handsOff(GameTestHelper helper) {
        place(helper, tiers(BeltTier.BELT), BeltTier.BELT, ITEMS);

        BlockPos target = targetOf(tiers(BeltTier.BELT));
        helper.succeedWhen(() -> {
            int arrived = count(chest(helper, target));
            if (arrived != ITEMS) {
                helper.fail("target chest holds " + arrived + " of " + ITEMS + " cobblestone", target);
            }
            int left = count(chest(helper, SOURCE));
            if (left != 0) {
                helper.fail("source chest still holds " + left + " cobblestone", SOURCE);
            }
        });
    }

    private static void rateTest(PFGameTests.Registrar tests, BeltTier tier, int itemsPerSecond) {
        tests.test("belt_tiles_tier_" + tier.number() + "_deliver_" + itemsPerSecond + "_items_per_second",
                RATE_WARMUP_TICKS + RATE_WINDOW_TICKS + 20,
                helper -> deliversAtRate(helper, tier, tiers(tier), itemsPerSecond));
    }

    private static void deliversAtRate(GameTestHelper helper, BeltTier loaders, List<BeltTier> tiles,
            int itemsPerSecond) {
        // Tiers 2 to 4 pay FE per item (#348); the creative pole's area covers the platform.
        if (loaders != BeltTier.BELT) helper.setBlock(POLE, PFBlocks.CREATIVE_POLE.get());
        place(helper, tiles, loaders, RATE_SUPPLY);
        BlockPos target = targetOf(tiles);

        int expected = itemsPerSecond * RATE_WINDOW_TICKS / 20;
        int[] before = new int[1];
        helper.startSequence()
                .thenIdle(RATE_WARMUP_TICKS)
                .thenExecute(() -> before[0] = count(chest(helper, target)))
                .thenIdle(RATE_WINDOW_TICKS)
                .thenExecute(() -> {
                    int delivered = count(chest(helper, target)) - before[0];
                    if (delivered != expected) {
                        helper.fail("a line of " + tiles + " between tier-" + loaders.number()
                                + " loaders delivered " + delivered + " items in " + RATE_WINDOW_TICKS
                                + " ticks, expected " + expected, target);
                    }
                })
                .thenSucceed();
    }

    // Nothing past the last tile, so the line backs up against its own end.
    private static void backedUpLineHolds512(GameTestHelper helper) {
        helper.setBlock(LONG_SOURCE, Blocks.CHEST);
        helper.setBlock(LONG_FROM, loader(BeltTier.BELT, Direction.EAST));
        for (int tile = 0; tile < LONG_LINE_TILES; tile++) {
            helper.setBlock(LONG_FIRST_TILE.east(tile), tile(BeltTier.BELT, Direction.EAST));
        }
        fill(helper, LONG_SOURCE, LONG_SUPPLY);

        // Read once rather than polled: succeedWhen would pass while the line was still filling.
        helper.startSequence().thenIdle(LONG_LINE_SETTLED_TICKS).thenExecute(() -> {
            int held = held(helper, LONG_FIRST_TILE);
            if (held != LONG_LINE_HOLDS) {
                helper.fail("a backed-up " + LONG_LINE_TILES + "-tile line holds " + held
                        + ", expected " + LONG_LINE_HOLDS, LONG_FIRST_TILE);
            }
            int left = count(chest(helper, LONG_SOURCE));
            if (left != LONG_SUPPLY - LONG_LINE_HOLDS) {
                helper.fail("the source chest holds " + left + " after filling the line, expected "
                        + (LONG_SUPPLY - LONG_LINE_HOLDS), LONG_SOURCE);
            }
        }).thenSucceed();
    }

    // The control for the stall: the same line with the loaders' tier changed moves items.
    private static void tierOneRunsUnpowered(GameTestHelper helper) {
        place(helper, tiers(BeltTier.BELT), BeltTier.BELT, RATE_SUPPLY);
        BlockPos target = targetOf(tiers(BeltTier.BELT));
        helper.startSequence().thenIdle(UNPOWERED_TICKS).thenExecute(() -> {
            if (helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(FROM), null) != null) {
                helper.fail("a tier-1 loader has an energy face", FROM);
            }
            int arrived = count(chest(helper, target));
            if (arrived == 0) {
                helper.fail("tier-1 loaders with no pole delivered nothing onto tiles in "
                        + UNPOWERED_TICKS + " ticks", target);
            }
        }).thenSucceed();
    }

    private static void tierTwoStallsUnpowered(GameTestHelper helper) {
        place(helper, tiers(BeltTier.IMPROVED), BeltTier.IMPROVED, RATE_SUPPLY);
        helper.startSequence().thenIdle(UNPOWERED_TICKS).thenExecute(() -> {
            int left = count(chest(helper, SOURCE));
            int onLine = held(helper, FIRST_TILE);
            if (left != RATE_SUPPLY || onLine != 0) {
                helper.fail("a tier-2 loader with no FE took " + (RATE_SUPPLY - left)
                        + " items and put " + onLine + " on the line", FROM);
            }
        }).thenSucceed();
    }

    // Measured with the pole cut off and the buffer filled by hand: fed, the refill's order against
    // the loader's tick decides what a read sees.
    private static void fedLoaderDrawsPerItem(GameTestHelper helper) {
        helper.setBlock(POLE, PFBlocks.CREATIVE_POLE.get());
        place(helper, tiers(BeltTier.IMPROVED), BeltTier.IMPROVED, RATE_SUPPLY);
        BlockPos target = targetOf(tiers(BeltTier.IMPROVED));
        ChuteBlockEntity loader = helper.getBlockEntity(FROM, ChuteBlockEntity.class);
        long[] before = new long[2];
        helper.startSequence()
                .thenIdle(FED_WARMUP_TICKS)
                .thenExecute(() -> {
                    if (count(chest(helper, target)) == 0) {
                        helper.fail("pole-fed tier-2 loaders delivered nothing over tiles", target);
                    }
                    helper.setBlock(POLE, PFBlocks.pole(PoleTier.SMALL).get());
                    loader.getEnergy().insertFe(Long.MAX_VALUE);
                    before[0] = face(helper, FROM).getAmountAsLong();
                    before[1] = count(chest(helper, SOURCE));
                })
                .thenIdle(DRAW_WINDOW_TICKS)
                .thenExecute(() -> {
                    long items = before[1] - count(chest(helper, SOURCE));
                    if (items == 0) {
                        helper.fail("the loader moved nothing in the window, so this proves nothing", FROM);
                    }
                    long drawn = before[0] - face(helper, FROM).getAmountAsLong();
                    long joules = items * TIER_2_JOULES_PER_ITEM
                            + (long) DRAW_WINDOW_TICKS * TIER_2_DRAIN_WATTS / 20;
                    // The face reads whole FE, so the window's far end rounds down by under one.
                    if (Math.abs(drawn * JOULES_PER_FE - joules) >= JOULES_PER_FE) {
                        helper.fail("the loader drew " + drawn + " FE for " + items + " items in "
                                + DRAW_WINDOW_TICKS + " ticks, expected " + joules / (double) JOULES_PER_FE,
                                FROM);
                    }
                })
                .thenSucceed();
    }

    // A small pole with no generator still probes: the loader must hold nothing after it.
    private static void probeLeavesNothing(GameTestHelper helper) {
        helper.setBlock(POLE, PFBlocks.pole(PoleTier.SMALL).get());
        helper.setBlock(FROM, loader(BeltTier.IMPROVED, Direction.EAST));
        helper.startSequence().thenIdle(45).thenExecute(() -> {
            long stored = face(helper, FROM).getAmountAsLong();
            if (stored != 0) {
                helper.fail("the loader kept " + stored + " FE from the demand probe", FROM);
            }
            long demanded = helper.getBlockEntity(POLE, SupplyAreaPoleBlockEntity.class).demandedFePerTick();
            if (demanded != TIER_2_LOADER_BUFFER_FE) {
                helper.fail("the pole read a demand of " + demanded + " FE/t, expected "
                        + TIER_2_LOADER_BUFFER_FE, POLE);
            }
        }).thenSucceed();
    }

    // The pack's express belt recipe is the control: a sweep that removed everything passes too.
    private static void ownRecipesAreSwept(GameTestHelper helper) {
        Set<String> loaded = helper.getLevel().getServer().getRecipeManager().recipeMap().values()
                .stream()
                .map(holder -> holder.id().identifier().toString())
                .collect(Collectors.toSet());
        if (!loaded.contains(EXPRESS_BELT_RECIPE)) {
            helper.fail(EXPRESS_BELT_RECIPE + " is not loaded, so this proves nothing");
            return;
        }
        List<String> survivors = loaded.stream().filter(id -> id.startsWith("belts:")).sorted().toList();
        if (!survivors.isEmpty()) {
            helper.fail("SimpleBelts' own recipes survived the sweep: " + survivors);
            return;
        }
        helper.succeed();
    }

    /** A chest behind an east-facing loader, a line of tiles running east, a west-facing loader and a chest. */
    private static void place(GameTestHelper helper, List<BeltTier> tiles, BeltTier loaders, int items) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(FROM, loader(loaders, Direction.EAST));
        for (int tile = 0; tile < tiles.size(); tile++) {
            helper.setBlock(FIRST_TILE.east(tile), tile(tiles.get(tile), Direction.EAST));
        }
        helper.setBlock(FIRST_TILE.east(tiles.size()), loader(loaders, Direction.WEST));
        helper.setBlock(targetOf(tiles), Blocks.CHEST);
        fill(helper, SOURCE, items);
    }

    private static BlockPos targetOf(List<BeltTier> tiles) {
        return FIRST_TILE.east(tiles.size() + 1);
    }

    static void fill(GameTestHelper helper, BlockPos chest, int items) {
        for (int slot = 0; items > 0; slot++, items -= 64) {
            chest(helper, chest).setItem(slot, new ItemStack(Items.COBBLESTONE, Math.min(items, 64)));
        }
    }

    /** What the line through this tile carries, asked of whichever tile of the run holds it. */
    private static int held(GameTestHelper helper, BlockPos tile) {
        var line = helper.getBlockEntity(tile, BeltTileBlockEntity.class).line();
        return line == null ? 0 : line.size();
    }

    static ChestBlockEntity chest(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, ChestBlockEntity.class);
    }

    static int count(Container container) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            total += container.getItem(slot).getCount();
        }
        return total;
    }

    static BlockState loader(BeltTier tier, Direction facing) {
        return BlockContent.loaderFor(tier).defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    static BlockState tile(BeltTier tier, Direction facing) {
        return BlockContent.tileFor(tier).defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static EnergyHandler face(GameTestHelper helper, BlockPos pos) {
        var handler = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(pos), null);
        if (handler == null) {
            helper.fail("the loader has no energy face", pos);
        }
        return handler;
    }
}
