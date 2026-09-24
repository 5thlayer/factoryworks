package com.planetaryfactory.core.gametest;

import java.util.Map;

import com.planetaryfactory.core.placement.PlacementPlan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import rearth.belts.BlockContent;
import rearth.belts.ItemContent;
import rearth.belts.blocks.BeltTileBlock;
import rearth.belts.blocks.SplitterBlock;
import rearth.belts.model.BeltTier;

/**
 * A slope's edges (#419, ADR-0085): a slope never turns and meets no loader or splitter, so a tile
 * placed by hand that would make one is refused with its reason and changes nothing. Each tile is
 * asked for its plan first, and the world and the stack are held to it.
 */
final class BeltSlopeEdgeTests {

    private static final BlockPos FIRST = new BlockPos(3, 1, 3);
    private static final BlockPos ABOVE_AHEAD = FIRST.east().above();

    private BeltSlopeEdgeTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_tile_fed_from_its_side_a_block_lower_is_refused", 40, helper -> {
            BeltWedgeTests.byHand(helper, BeltWedgeTests.player(helper), FIRST, Direction.EAST, 0);
            refused(helper, ABOVE_AHEAD, Direction.NORTH, PlacementPlan.Refusal.SLOPE_TURNS);
        });
        tests.test("a_tile_a_block_lower_facing_a_tiles_side_is_refused", 40, helper -> {
            BeltWedgeTests.byHand(helper, BeltWedgeTests.player(helper), ABOVE_AHEAD, Direction.NORTH, 0);
            refused(helper, FIRST, Direction.EAST, PlacementPlan.Refusal.SLOPE_TURNS);
        });
        tests.test("a_tile_that_would_slope_a_corner_is_refused", 40, BeltSlopeEdgeTests::slopingACorner);
        tests.test("a_tile_that_would_slope_a_tile_out_of_a_loader_is_refused", 40, helper -> {
            helper.setBlock(FIRST.west(), BeltTileTests.loader(BeltTier.BELT, Direction.EAST));
            BeltWedgeTests.byHand(helper, BeltWedgeTests.player(helper), FIRST, Direction.EAST, 0);
            refused(helper, ABOVE_AHEAD, Direction.EAST, PlacementPlan.Refusal.SLOPE_MEETS_LOADER);
        });
        tests.test("a_tile_that_would_slope_a_tile_into_a_loader_is_refused", 40, helper -> {
            helper.setBlock(ABOVE_AHEAD.east(), BeltTileTests.loader(BeltTier.BELT, Direction.WEST));
            BeltWedgeTests.byHand(helper, BeltWedgeTests.player(helper), ABOVE_AHEAD, Direction.EAST, 0);
            refused(helper, FIRST, Direction.EAST, PlacementPlan.Refusal.SLOPE_MEETS_LOADER);
        });
        tests.test("a_tile_that_would_slope_a_tile_into_a_splitter_half_is_refused", 40, helper -> {
            BlockPos left = ABOVE_AHEAD.east();
            helper.setBlock(left, half(SplitterBlock.Side.LEFT));
            helper.setBlock(left.relative(Direction.EAST.getClockWise()), half(SplitterBlock.Side.RIGHT));
            BeltWedgeTests.byHand(helper, BeltWedgeTests.player(helper), ABOVE_AHEAD, Direction.EAST, 0);
            refused(helper, FIRST, Direction.EAST, PlacementPlan.Refusal.SLOPE_MEETS_LOADER);
        });
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

    private static BlockState half(SplitterBlock.Side side) {
        return BlockContent.splitterFor(BeltTier.BELT).defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)
                .setValue(SplitterBlock.SIDE, side);
    }
}
