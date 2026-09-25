package com.planetaryfactory.core.gametest;

import java.util.Map;

import com.planetaryfactory.core.placement.PlacementPlan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import io.github._5thlayer.beltworks.ItemContent;
import io.github._5thlayer.beltworks.blocks.BeltTileBlock;
import io.github._5thlayer.beltworks.model.BeltTier;

/**
 * A slope's edges (#419, ADR-0085): a slope never turns, so a tile placed by hand that would turn a
 * corner into one is refused with its reason and changes nothing, while a tile across another a
 * block up is a crossing and places level. Each tile is asked for its plan first, and the world and
 * the stack are held to it.
 */
final class BeltSlopeEdgeTests {

    private static final BlockPos FIRST = new BlockPos(3, 1, 3);
    private static final BlockPos ABOVE_AHEAD = FIRST.east().above();

    private BeltSlopeEdgeTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_tile_across_a_line_a_block_up_is_placed_level", 40, helper -> {
            ServerPlayer player = BeltWedgeTests.player(helper);
            BeltWedgeTests.byHand(helper, player, FIRST, Direction.EAST, 0);
            BeltWedgeTests.byHand(helper, player, ABOVE_AHEAD, Direction.NORTH, 0);
            level(helper, FIRST);
            level(helper, ABOVE_AHEAD);
            helper.succeed();
        });
        tests.test("a_tile_ending_under_a_line_across_it_is_placed_level", 40, helper -> {
            ServerPlayer player = BeltWedgeTests.player(helper);
            BeltWedgeTests.byHand(helper, player, ABOVE_AHEAD, Direction.NORTH, 0);
            BeltWedgeTests.byHand(helper, player, FIRST, Direction.EAST, 0);
            level(helper, FIRST);
            level(helper, ABOVE_AHEAD);
            helper.succeed();
        });
        tests.test("a_tile_that_would_slope_a_corner_is_refused", 40, BeltSlopeEdgeTests::slopingACorner);
    }

    private static void slopingACorner(GameTestHelper helper) {
        ServerPlayer player = BeltWedgeTests.player(helper);
        BeltWedgeTests.byHand(helper, player, FIRST.north(), Direction.SOUTH, 0);
        BeltWedgeTests.byHand(helper, player, FIRST, Direction.EAST, 0);
        if (helper.getBlockState(FIRST).getValue(BeltTileBlock.CORNER) == BeltTileBlock.Shape.STRAIGHT) {
            helper.fail("a tile fed only from its side is straight, so this proves little", FIRST);
        }
        refused(helper, ABOVE_AHEAD, Direction.EAST, PlacementPlan.Refusal.SLOPE_TURNS);
    }

    /** A tile placed by hand at {@code at} facing {@code facing} is planned refused for {@code expected}, and changes nothing. */
    private static void refused(GameTestHelper helper, BlockPos at, Direction facing, PlacementPlan.Refusal expected) {
        ServerPlayer player = BeltWedgeTests.player(helper);
        player.setYRot(facing.toYRot());
        Map<BlockPos, BlockState> before = BeltWedgeTests.around(helper, at);
        ItemStack stack = new ItemStack(ItemContent.tileFor(BeltTier.BELT));
        PlacementPlan plan = BeltWedgeTests.planOf(helper, player, stack, at);
        if (plan == null || plan.refusal() != expected) {
            helper.fail("a tile facing " + facing + " is planned " + (plan == null ? "as nothing" : "with " + plan.refusal())
                    + ", expected refused for " + expected, at);
        }
        BeltWedgeTests.use(helper, player, stack, at);
        if (!BeltWedgeTests.around(helper, at).equals(before)) helper.fail("a refused tile changed the world", at);
        if (stack.getCount() != 1) helper.fail("a refused tile was spent", at);
        helper.succeed();
    }

    private static void level(GameTestHelper helper, BlockPos tile) {
        BlockState state = helper.getBlockState(tile);
        if (state.getValue(BeltTileBlock.PITCH) != BeltTileBlock.PitchState.LEVEL
                || state.getValue(BeltTileBlock.CORNER) != BeltTileBlock.Shape.STRAIGHT) {
            helper.fail("a tile crossing another a block up stands " + state + ", expected level and straight", tile);
        }
    }
}
