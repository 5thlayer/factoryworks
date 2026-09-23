package com.planetaryfactory.core.gametest;

import java.util.List;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.PFDataComponents;
import com.planetaryfactory.core.placement.HeldTurn;
import com.planetaryfactory.core.placement.PlacementPlan;
import com.planetaryfactory.core.placement.Placements;
import com.planetaryfactory.core.placement.QuarterTurn;
import com.planetaryfactory.core.smelting.FurnaceTier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import rearth.belts.ItemContent;
import rearth.belts.model.BeltTier;

/**
 * Rotate on the held stack (#386, ADR-0083): what a stack turned by the presses places, and where
 * the turn goes as the stack is spent. The presses go through {@link HeldTurn#press}, which is what
 * the key's payload calls; the key itself is a human check on delivery.
 */
final class RotateTests {

    private static final int OFFSETS = 4;

    private RotateTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_turned_stack_places_a_tile_and_a_furnace_turned_from_the_look", 20,
                RotateTests::placedFacesTheTurnedLook);
        tests.test("a_turn_stays_on_the_rest_of_a_stack_and_goes_with_its_last_item", 20,
                RotateTests::turnGoesWithTheLastItem);
        tests.test("rotate_leaves_an_unplaceable_stack_alone", 20, RotateTests::unplaceableIsLeftAlone);
    }

    private static void placedFacesTheTurnedLook(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        List<Direction> looks = List.of(Direction.EAST, Direction.SOUTH);
        int row = 0;
        for (boolean sneaking : List.of(false, true)) {
            player.setShiftKeyDown(sneaking);
            for (Direction look : looks) {
                player.setYRot(look.toYRot());
                for (int offset = 0; offset < OFFSETS; offset++) {
                    Direction turned = look;
                    for (int quarter = 0; quarter < offset; quarter++) {
                        turned = turned.getClockWise();
                    }
                    String gesture = "looking " + look + " with offset " + offset + (sneaking ? " sneaking" : "");
                    BlockPos tile = new BlockPos(1 + 4 * offset, 1, 2 * row);
                    if (!placesFacing(helper, player, ItemContent.tileFor(BeltTier.BELT), offset, tile, turned,
                            "a tile placed " + gesture)) {
                        return;
                    }
                    BlockPos furnace = tile.east(2);
                    if (!placesFacing(helper, player, PFBlocks.furnace(FurnaceTier.STONE).get().asItem(), offset,
                            furnace, turned.getOpposite(), "a furnace placed " + gesture)) {
                        return;
                    }
                }
                row++;
            }
        }
        helper.succeed();
    }

    /** Whether a fresh stack pressed {@code offset} times plans and places this facing at {@code at}. */
    private static boolean placesFacing(GameTestHelper helper, Player player, Item item, int offset,
                                        BlockPos at, Direction expected, String what) {
        ItemStack stack = new ItemStack(item, 2);
        for (int press = 0; press < offset; press++) {
            HeldTurn.press(stack, false);
        }
        if (offset == 0 && stack.has(PFDataComponents.QUARTER_TURN.get())) {
            helper.fail(what + ": an unpressed stack carries a turn", at);
            return false;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = hit(helper, at.below());
        PlacementPlan plan = Placements.planFor(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit);
        helper.useBlock(at.below(), player, hit);
        BlockState placed = helper.getBlockState(at);
        if (!placed.is(((BlockItem) item).getBlock())) {
            helper.fail(what + " placed " + placed, at);
            return false;
        }
        Direction facing = placed.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (facing != expected) {
            helper.fail(what + " faces " + facing + ", expected " + expected, at);
            return false;
        }
        if (plan == null || plan.blocks().size() != 1 || !plan.blocks().getFirst().state().equals(placed)) {
            helper.fail(what + " was planned as " + plan + " and placed as " + placed, at);
            return false;
        }
        return true;
    }

    private static void turnGoesWithTheLastItem(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(Direction.EAST.toYRot());
        ItemStack stack = new ItemStack(ItemContent.tileFor(BeltTier.BELT), 2);
        HeldTurn.press(stack, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        BlockPos first = new BlockPos(2, 1, 2);
        helper.useBlock(first.below(), player, hit(helper, first.below()));
        ItemStack rest = player.getMainHandItem();
        if (rest.getCount() != 1 || !HeldTurn.of(rest).equals(QuarterTurn.of(1))) {
            helper.fail("one of two placed left " + rest.getCount() + " holding " + HeldTurn.of(rest)
                    + ", expected 1 holding a quarter turn", first);
            return;
        }
        BlockPos last = first.east(2);
        helper.useBlock(last.below(), player, hit(helper, last.below()));
        if (!player.getMainHandItem().isEmpty()) {
            helper.fail("the last item placed left " + player.getMainHandItem(), last);
            return;
        }
        helper.succeed();
    }

    private static void unplaceableIsLeftAlone(GameTestHelper helper) {
        ItemStack stick = new ItemStack(Items.STICK);
        if (HeldTurn.press(stick, false) || stick.has(PFDataComponents.QUARTER_TURN.get())) {
            helper.fail("Rotate turned a stick");
            return;
        }
        ItemStack tile = new ItemStack(ItemContent.tileFor(BeltTier.BELT));
        HeldTurn.press(tile, true);
        HeldTurn.press(tile, false);
        if (tile.has(PFDataComponents.QUARTER_TURN.get())) {
            helper.fail("a stack turned back to none still carries the turn, and will not stack with an unturned one");
            return;
        }
        helper.succeed();
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos floor) {
        BlockPos absolute = helper.absolutePos(floor);
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
    }
}
