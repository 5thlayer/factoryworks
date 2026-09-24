package com.planetaryfactory.core.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.planetaryfactory.core.placement.PlacementPlan;
import com.planetaryfactory.core.placement.Placements;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import rearth.belts.BlockContent;
import rearth.belts.ItemContent;
import rearth.belts.blocks.BeltTileBlock;
import rearth.belts.blocks.BeltTileBlockEntity;
import rearth.belts.model.BeltTier;

/**
 * A middle or top over air stands on a wedge (#420, ADR-0085): placed with its tile for nothing,
 * broken with it for one tile, taking a replaceable block's place and refused over a belt or a
 * machine, gone when its tile levels. A belt built by hand crosses another over two wedges.
 *
 * <p>Every tile placed by hand is asked for its plan first, and the world is held to it.
 */
final class BeltWedgeTests {

    private static final BlockPos FIRST = new BlockPos(3, 1, 3);
    // Each tile's height above the platform, climbing east from FIRST through air.
    private static final int[] THREE_BLOCK_CLIMB = {0, 0, 1, 2, 3, 3};
    private static final List<BeltTileBlock.PitchState> THREE_BLOCK_CLIMB_PITCHES = List.of(
            BeltTileBlock.PitchState.LEVEL, BeltTileBlock.PitchState.FOOT_UP, BeltTileBlock.PitchState.MIDDLE_UP,
            BeltTileBlock.PitchState.MIDDLE_UP, BeltTileBlock.PitchState.TOP_UP, BeltTileBlock.PitchState.LEVEL);
    private static final int[] THREE_BLOCK_DESCENT = {3, 3, 2, 1, 0, 0};
    private static final List<BeltTileBlock.PitchState> THREE_BLOCK_DESCENT_PITCHES = List.of(
            BeltTileBlock.PitchState.LEVEL, BeltTileBlock.PitchState.TOP_DOWN, BeltTileBlock.PitchState.MIDDLE_DOWN,
            BeltTileBlock.PitchState.MIDDLE_DOWN, BeltTileBlock.PitchState.FOOT_DOWN, BeltTileBlock.PitchState.LEVEL);
    private static final int[] ONE_BLOCK_CLIMB = {0, 0, 1, 1};
    private static final BlockPos TOP = FIRST.east(2).above();
    private static final BlockPos TOP_WEDGE = TOP.below();

    // A line running south at x = 8, crossed at z = 3 by a line running east a block above it.
    private static final int CROSSED_X = 8;
    private static final BlockPos CROSSED_SOURCE = new BlockPos(CROSSED_X, 1, 0);
    private static final BlockPos CROSSED_TARGET = new BlockPos(CROSSED_X, 1, 6);
    private static final BlockPos CROSSED_TILE = new BlockPos(CROSSED_X, 1, 3);
    private static final BlockPos CROSSING_SOURCE = new BlockPos(3, 1, 3);
    private static final BlockPos CROSSING_FIRST = CROSSING_SOURCE.east(2);
    private static final int[] CROSSING = {0, 0, 1, 1, 1, 0, 0};
    private static final List<BeltTileBlock.PitchState> CROSSING_PITCHES = List.of(
            BeltTileBlock.PitchState.LEVEL, BeltTileBlock.PitchState.FOOT_UP, BeltTileBlock.PitchState.TOP_UP,
            BeltTileBlock.PitchState.LEVEL, BeltTileBlock.PitchState.TOP_DOWN, BeltTileBlock.PitchState.FOOT_DOWN,
            BeltTileBlock.PitchState.LEVEL);
    private static final BlockPos CROSSING_TARGET = CROSSING_FIRST.east(CROSSING.length + 1);

    private static final int ITEMS = 64;
    private static final int DELIVERY_TICKS = 400;
    private static final int TIER_1_ITEMS_PER_SECOND = 15;
    private static final int RATE_WARMUP_TICKS = 100;
    private static final int RATE_WINDOW_TICKS = 200;
    private static final int RATE_SUPPLY = 27 * 64;

    private BeltWedgeTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_climb_through_air_stands_on_a_wedge_under_each_middle_and_the_top", 40,
                helper -> throughAir(helper, THREE_BLOCK_CLIMB, THREE_BLOCK_CLIMB_PITCHES, 2, Direction.EAST));
        tests.test("a_descent_through_air_stands_on_a_wedge_under_the_top_and_each_middle", 40,
                helper -> throughAir(helper, THREE_BLOCK_DESCENT, THREE_BLOCK_DESCENT_PITCHES, 1, Direction.WEST));
        tests.test("breaking_a_sloped_tile_leaves_no_wedge_and_drops_one_tile", 40, helper -> breaking(helper, TOP));
        tests.test("breaking_a_wedge_leaves_no_tile_and_drops_one_tile", 40, helper -> breaking(helper, TOP_WEDGE));
        tests.test("a_wedge_takes_the_place_of_grass", 40, BeltWedgeTests::replacesGrass);
        tests.test("a_slope_whose_wedge_would_stand_on_a_loader_is_refused", 40,
                helper -> refused(helper, BeltTileTests.loader(BeltTier.BELT, Direction.NORTH)));
        tests.test("a_slope_whose_wedge_would_stand_on_a_tile_is_refused", 40,
                helper -> refused(helper, BeltTileTests.tile(BeltTier.BELT, Direction.SOUTH)));
        tests.test("a_slope_levelled_loses_its_wedge_and_stays_put", 40, BeltWedgeTests::levelledLosesItsWedge);
        tests.test("a_crossing_built_by_hand_delivers_both_lines_every_item", DELIVERY_TICKS + 20,
                BeltWedgeTests::crossingDeliversEveryItem);
        tests.test("a_crossing_built_by_hand_carries_" + TIER_1_ITEMS_PER_SECOND + "_items_s_on_each_line",
                RATE_WARMUP_TICKS + RATE_WINDOW_TICKS + 20, BeltWedgeTests::crossingCarriesItsRate);
    }

    // Tiles from firstWedged on, three in all, stand on wedges rising the way uphill faces.
    private static void throughAir(GameTestHelper helper, int[] heights, List<BeltTileBlock.PitchState> pitches,
                                   int firstWedged, Direction uphill) {
        List<BlockPos> tiles = byHand(helper, player(helper), FIRST, Direction.EAST, heights);
        for (int tile = 0; tile < tiles.size(); tile++) {
            expectPitch(helper, tiles.get(tile), pitches.get(tile), "along a slope through air");
            boolean wedged = tile >= firstWedged && tile < firstWedged + 3;
            BlockState below = helper.getBlockState(tiles.get(tile).below());
            if (below.is(BlockContent.BELT_WEDGE.get()) != wedged) {
                helper.fail("under tile " + tile + " of a slope through air stands " + below + ", expected "
                        + (wedged ? "a wedge" : "no wedge"), tiles.get(tile).below());
            }
            if (wedged && below.getValue(HorizontalDirectionalBlock.FACING) != uphill) {
                helper.fail("a wedge under a slope rising " + uphill + " rises " + below.getValue(HorizontalDirectionalBlock.FACING),
                        tiles.get(tile).below());
            }
        }
        helper.succeed();
    }

    private static void breaking(GameTestHelper helper, BlockPos target) {
        byHand(helper, player(helper), FIRST, Direction.EAST, ONE_BLOCK_CLIMB);
        expectWedge(helper, TOP_WEDGE, "under a top over air");
        ServerPlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.gameMode.destroyBlock(helper.absolutePos(target));

        for (BlockPos pos : List.of(TOP, TOP_WEDGE)) {
            if (!helper.getBlockState(pos).isAir()) {
                helper.fail("breaking " + target + " left " + helper.getBlockState(pos) + " standing", pos);
            }
        }
        int dropped = dropped(helper, ItemContent.tileFor(BeltTier.BELT));
        if (dropped != 1) helper.fail("breaking " + target + " dropped " + dropped + " tiles, expected one", target);
        helper.succeed();
    }

    private static void replacesGrass(GameTestHelper helper) {
        helper.setBlock(TOP_WEDGE.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(TOP_WEDGE, Blocks.SHORT_GRASS);
        byHand(helper, player(helper), FIRST, Direction.EAST, ONE_BLOCK_CLIMB);
        expectWedge(helper, TOP_WEDGE, "where grass grew under a top");
        helper.succeed();
    }

    private static void refused(GameTestHelper helper, BlockState occupant) {
        helper.setBlock(TOP_WEDGE, occupant);
        ServerPlayer player = player(helper);
        byHand(helper, player, FIRST, Direction.EAST, 0, 0);
        Map<BlockPos, BlockState> before = around(helper, TOP);

        ItemStack stack = new ItemStack(ItemContent.tileFor(BeltTier.BELT));
        PlacementPlan plan = planOf(helper, player, stack, TOP);
        if (plan == null || plan.refusal() != PlacementPlan.Refusal.WEDGE_BLOCKED) {
            helper.fail("a top over " + occupant + " is planned " + (plan == null ? "as nothing" : "with " + plan.refusal())
                    + ", expected refused for its wedge", TOP);
        }
        use(helper, player, stack, TOP);

        if (!around(helper, TOP).equals(before)) helper.fail("a refused top over " + occupant + " changed the world", TOP);
        if (stack.getCount() != 1) helper.fail("a refused top over " + occupant + " spent its tile", TOP);
        helper.succeed();
    }

    private static void levelledLosesItsWedge(GameTestHelper helper) {
        List<BlockPos> tiles = byHand(helper, player(helper), FIRST, Direction.EAST, ONE_BLOCK_CLIMB);
        expectWedge(helper, TOP_WEDGE, "under a top over air");
        helper.destroyBlock(tiles.get(1));
        expectPitch(helper, TOP, BeltTileBlock.PitchState.LEVEL, "with its foot broken");
        if (!helper.getBlockState(TOP_WEDGE).isAir()) {
            helper.fail("a levelled top left " + helper.getBlockState(TOP_WEDGE) + " under it", TOP_WEDGE);
        }
        helper.succeed();
    }

    /** The crossed line placed with commands, downstream first; the crossing by hand between its own loaders. */
    private static List<BlockPos> crossing(GameTestHelper helper) {
        helper.setBlock(CROSSED_TARGET, Blocks.CHEST);
        helper.setBlock(CROSSED_TARGET.north(), BeltTileTests.loader(BeltTier.BELT, Direction.NORTH));
        for (int z = 4; z >= 2; z--) helper.setBlock(new BlockPos(CROSSED_X, 1, z), BeltTileTests.tile(BeltTier.BELT, Direction.SOUTH));
        helper.setBlock(CROSSED_SOURCE.south(), BeltTileTests.loader(BeltTier.BELT, Direction.SOUTH));
        helper.setBlock(CROSSED_SOURCE, Blocks.CHEST);

        helper.setBlock(CROSSING_SOURCE, Blocks.CHEST);
        helper.setBlock(CROSSING_SOURCE.east(), BeltTileTests.loader(BeltTier.BELT, Direction.EAST));
        List<BlockPos> tiles = byHand(helper, player(helper), CROSSING_FIRST, Direction.EAST, CROSSING);
        helper.setBlock(CROSSING_TARGET.west(), BeltTileTests.loader(BeltTier.BELT, Direction.WEST));
        helper.setBlock(CROSSING_TARGET, Blocks.CHEST);

        for (int tile = 0; tile < tiles.size(); tile++) {
            expectPitch(helper, tiles.get(tile), CROSSING_PITCHES.get(tile), "across a line");
        }
        expectWedge(helper, tiles.get(2).below(), "under the crossing's climb");
        expectWedge(helper, tiles.get(4).below(), "under the crossing's descent");
        return tiles;
    }

    private static void crossingDeliversEveryItem(GameTestHelper helper) {
        List<BlockPos> tiles = crossing(helper);
        fill(helper, CROSSING_SOURCE, Items.COBBLESTONE, ITEMS);
        fill(helper, CROSSED_SOURCE, Items.IRON_INGOT, ITEMS);
        helper.startSequence().thenIdle(DELIVERY_TICKS).thenExecute(() -> {
            expectLine(helper, tiles.getFirst(), CROSSING.length, "across a line");
            expectLine(helper, CROSSED_TILE, 3, "under a crossing");
            int over = count(helper, CROSSING_TARGET, Items.COBBLESTONE);
            int under = count(helper, CROSSED_TARGET, Items.IRON_INGOT);
            int mixed = count(helper, CROSSING_TARGET, Items.IRON_INGOT) + count(helper, CROSSED_TARGET, Items.COBBLESTONE);
            int onGround = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(2.0)).size();
            if (over != ITEMS || under != ITEMS || mixed != 0 || onGround != 0) {
                helper.fail("a crossing delivered " + over + " of " + ITEMS + " over and " + under + " of " + ITEMS
                        + " under, " + mixed + " into the other line's chest and " + onGround + " on the ground", CROSSED_TILE);
            }
        }).thenSucceed();
    }

    private static void crossingCarriesItsRate(GameTestHelper helper) {
        crossing(helper);
        fill(helper, CROSSING_SOURCE, Items.COBBLESTONE, RATE_SUPPLY);
        fill(helper, CROSSED_SOURCE, Items.IRON_INGOT, RATE_SUPPLY);
        int expected = TIER_1_ITEMS_PER_SECOND * RATE_WINDOW_TICKS / 20;
        int[] before = new int[2];
        helper.startSequence()
                .thenIdle(RATE_WARMUP_TICKS)
                .thenExecute(() -> {
                    before[0] = count(helper, CROSSING_TARGET, Items.COBBLESTONE);
                    before[1] = count(helper, CROSSED_TARGET, Items.IRON_INGOT);
                })
                .thenIdle(RATE_WINDOW_TICKS)
                .thenExecute(() -> {
                    int over = count(helper, CROSSING_TARGET, Items.COBBLESTONE) - before[0];
                    int under = count(helper, CROSSED_TARGET, Items.IRON_INGOT) - before[1];
                    if (over != expected || under != expected) {
                        helper.fail("in " + RATE_WINDOW_TICKS + " ticks a crossing carried " + over + " over and " + under
                                + " under, expected " + expected + " each", CROSSED_TILE);
                    }
                })
                .thenSucceed();
    }

    /** Tiles placed by hand upstream first, from {@code first} along {@code facing}, each at its height, through air. */
    private static List<BlockPos> byHand(GameTestHelper helper, ServerPlayer player, BlockPos first, Direction facing, int... heights) {
        player.setYRot(facing.toYRot());
        List<BlockPos> tiles = new ArrayList<>();
        for (int tile = 0; tile < heights.length; tile++) {
            BlockPos at = first.relative(facing, tile).above(heights[tile]);
            ItemStack stack = new ItemStack(ItemContent.tileFor(BeltTier.BELT));
            PlacementPlan plan = planOf(helper, player, stack, at);
            if (plan == null || plan.isRefused()) {
                helper.fail("tile " + tile + " is planned " + (plan == null ? "as nothing" : "refused, " + plan.refusal()), at);
            }
            use(helper, player, stack, at);
            for (PlacementPlan.Placed placed : plan.blocks()) {
                BlockState there = helper.getLevel().getBlockState(placed.pos());
                if (!there.equals(placed.state())) {
                    helper.fail("tile " + tile + "'s plan named " + placed.state() + ", and " + there + " stands there",
                            helper.relativePos(placed.pos()));
                }
            }
            tiles.add(at);
        }
        return tiles;
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(false);
        return player;
    }

    // Aimed at the spot itself, which is air, so the tile goes there whatever stands around it.
    private static BlockHitResult hit(GameTestHelper helper, BlockPos at) {
        BlockPos absolute = helper.absolutePos(at);
        return new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
    }

    private static PlacementPlan planOf(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos at) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return Placements.planFor(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit(helper, at));
    }

    private static void use(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos at) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit(helper, at));
    }

    private static Map<BlockPos, BlockState> around(GameTestHelper helper, BlockPos centre) {
        Map<BlockPos, BlockState> states = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-2, -2, -2), centre.offset(2, 2, 2))) {
            states.put(pos.immutable(), helper.getBlockState(pos));
        }
        return states;
    }

    private static void expectPitch(GameTestHelper helper, BlockPos tile, BeltTileBlock.PitchState expected, String when) {
        BlockState state = helper.getBlockState(tile);
        if (!(state.getBlock() instanceof BeltTileBlock) || state.getValue(BeltTileBlock.PITCH) != expected) {
            helper.fail(when + ", " + state + " stands there, expected a tile " + expected, tile);
        }
    }

    private static void expectWedge(GameTestHelper helper, BlockPos pos, String where) {
        if (!helper.getBlockState(pos).is(BlockContent.BELT_WEDGE.get())) {
            helper.fail(where + " stands " + helper.getBlockState(pos) + ", expected a wedge", pos);
        }
    }

    private static void expectLine(GameTestHelper helper, BlockPos tile, int tiles, String when) {
        var line = helper.getBlockEntity(tile, BeltTileBlockEntity.class).line();
        int count = line == null ? 0 : line.tileCount();
        if (count != tiles) helper.fail(when + ", the tile is in a line of " + count + ", expected " + tiles, tile);
    }

    private static int dropped(GameTestHelper helper, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(2.0)).stream()
                .map(ItemEntity::getItem)
                .filter(stack -> stack.is(item))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static void fill(GameTestHelper helper, BlockPos chest, Item item, int items) {
        for (int slot = 0; items > 0; slot++, items -= 64) {
            BeltTileTests.chest(helper, chest).setItem(slot, new ItemStack(item, Math.min(items, 64)));
        }
    }

    private static int count(GameTestHelper helper, BlockPos chest, Item item) {
        var container = BeltTileTests.chest(helper, chest);
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }
}
