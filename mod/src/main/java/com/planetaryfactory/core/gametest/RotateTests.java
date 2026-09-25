package com.planetaryfactory.core.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.PFDataComponents;
import com.planetaryfactory.core.placement.HeldTurn;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import com.planetaryfactory.core.placement.QuarterTurn;
import com.planetaryfactory.core.placement.RotatePress;
import com.planetaryfactory.core.smelting.FurnaceTier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import io.github._5thlayer.beltworks.BlockContent;
import io.github._5thlayer.beltworks.ComponentContent;
import io.github._5thlayer.beltworks.ItemContent;
import io.github._5thlayer.beltworks.blocks.BeltTileBlock;
import io.github._5thlayer.beltworks.blocks.SplitterBlock;
import io.github._5thlayer.beltworks.items.StretchPlan;
import io.github._5thlayer.beltworks.model.BeltTier;

/**
 * Rotate on the held stack (#386, ADR-0083): what a stack turned by the presses places, and where
 * the turn goes as the stack is spent. And on the aimed block (#405, ADR-0087): turned in place, or
 * refused with nothing changed. The presses go through {@link HeldTurn#press} and
 * {@link RotatePress#press}, which is what the key's payload calls; the key itself is a human check
 * on delivery.
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
        tests.test("rotate_turns_an_aimed_tile_and_loader_a_quarter_each_press_both_ways", 20,
                RotateTests::aimedTurnsInPlace);
        tests.test("rotate_refuses_a_splitter_half_with_nothing_changed", 20, RotateTests::splitterRefused);
        tests.test("rotate_refuses_a_foot_a_top_and_its_wedge_with_nothing_changed", 20, RotateTests::slopeRefused);
        tests.test("rotate_refuses_a_turn_that_would_slope_a_corner", 20, RotateTests::cornerSlopeRefused);
        tests.test("rotate_refuses_a_turn_whose_wedge_would_stand_on_a_loader", 20, RotateTests::wedgeBlockedRefused);
        tests.test("rotate_with_a_placeable_held_turns_the_stack_not_the_aimed_block", 20,
                RotateTests::heldBeforeAimed);
        tests.test("rotate_with_a_block_that_has_no_facing_held_turns_the_aimed_block", 20,
                RotateTests::unrotatableHeldTurnsAimed);
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
                    boolean tileTurned = sneaking
                            ? startsFacing(helper, player, offset, tile, turned, "a stretch started " + gesture)
                            : placesFacing(helper, player, ItemContent.tileFor(BeltTier.BELT), offset, tile, turned,
                                    "a tile placed " + gesture);
                    if (!tileTurned) {
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

    /** Whether a sneak-click with a fresh tile stack pressed {@code offset} times stores the stretch's start facing this way (#393). */
    private static boolean startsFacing(GameTestHelper helper, Player player, int offset, BlockPos at,
                                        Direction expected, String what) {
        ItemStack stack = new ItemStack(ItemContent.tileFor(BeltTier.BELT), 2);
        for (int press = 0; press < offset; press++) {
            HeldTurn.press(stack, false);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.useBlock(at.below(), player, hit(helper, at.below()));
        Direction stored = stack.get(ComponentContent.BELT_DIR.get());
        if (stored != expected || !helper.getBlockState(at).isAir()) {
            helper.fail(what + " stored " + stored + " and left " + helper.getBlockState(at) + ", expected "
                    + expected + " and nothing placed", at);
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

    private static void aimedTurnsInPlace(GameTestHelper helper) {
        BlockPos tile = new BlockPos(2, 1, 2);
        BlockPos loader = new BlockPos(5, 1, 2);
        helper.setBlock(tile, tileState(BeltTier.BELT, Direction.EAST));
        helper.setBlock(loader, loaderState(BeltTier.BELT, Direction.EAST));
        ListeningPlayer player = standingAt(helper, new BlockPos(3, 1, 4));
        for (BlockPos aimed : List.of(tile, loader)) {
            BlockEntity entity = helper.getBlockEntity(aimed, BlockEntity.class);
            for (boolean reverse : List.of(false, true)) {
                Direction expected = Direction.EAST;
                for (int press = 0; press < OFFSETS; press++) {
                    expected = reverse ? expected.getCounterClockWise() : expected.getClockWise();
                    RotatePress.press(player, helper.absolutePos(aimed), reverse);
                    Direction facing = helper.getBlockState(aimed).getValue(BlockStateProperties.HORIZONTAL_FACING);
                    if (facing != expected) {
                        helper.fail((reverse ? "Reverse Rotate" : "Rotate") + " press " + (press + 1) + " left "
                                + helper.getBlockState(aimed) + " facing " + facing + ", expected " + expected, aimed);
                        return;
                    }
                }
            }
            if (helper.getBlockEntity(aimed, BlockEntity.class) != entity) {
                helper.fail("turning " + helper.getBlockState(aimed) + " replaced its block entity", aimed);
                return;
            }
        }
        if (!player.heard.isEmpty()) {
            helper.fail("a turn that went through named a refusal: " + player.heard);
            return;
        }
        helper.succeed();
    }

    private static void splitterRefused(GameTestHelper helper) {
        BlockPos left = new BlockPos(3, 1, 2);
        BlockPos right = left.relative(Direction.EAST.getClockWise());
        helper.setBlock(left, splitterHalf(SplitterBlock.Side.LEFT));
        helper.setBlock(right, splitterHalf(SplitterBlock.Side.RIGHT));
        refusedUnchanged(helper, List.of(left, right), List.of(left, right), "message.planetaryfactory.rotate.splitter");
    }

    private static BlockState splitterHalf(SplitterBlock.Side side) {
        return BlockContent.splitterFor(BeltTier.BELT).defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST)
                .setValue(SplitterBlock.SIDE, side);
    }

    // Laid by hand through air, so the top stands on a wedge.
    private static void slopeRefused(GameTestHelper helper) {
        List<BlockPos> tiles = byHand(helper, builder(helper), new BlockPos(3, 1, 3),
                Direction.EAST, 0, 0, 1, 1);
        BlockPos foot = tiles.get(1);
        BlockPos top = tiles.get(2);
        if (pitch(helper, foot) != BeltTileBlock.PitchState.FOOT_UP
                || pitch(helper, top) != BeltTileBlock.PitchState.TOP_UP
                || !helper.getBlockState(top.below()).is(BlockContent.BELT_WEDGE.get())) {
            helper.fail("the climb stands as " + pitch(helper, foot) + " and "
                    + pitch(helper, top) + " over " + helper.getBlockState(top.below())
                    + ", expected a foot and a top on a wedge", foot);
            return;
        }
        refusedUnchanged(helper, List.of(foot, top, top.below()), List.copyOf(around(helper, foot).keySet()),
                "message.planetaryfactory.rotate.slope");
    }

    // A level tile across a corner's line a block up, turned to continue it, would slope the corner (#419).
    private static void cornerSlopeRefused(GameTestHelper helper) {
        BlockPos corner = new BlockPos(3, 1, 3);
        BlockPos across = corner.east().above();
        ServerPlayer player = builder(helper);
        byHand(helper, player, corner.north(), Direction.SOUTH, 0);
        byHand(helper, player, corner, Direction.EAST, 0);
        byHand(helper, player, across, Direction.NORTH, 0);
        if (helper.getBlockState(corner).getValue(BeltTileBlock.CORNER) == BeltTileBlock.Shape.STRAIGHT) {
            helper.fail("the tile fed from its side stands " + helper.getBlockState(corner) + ", expected a corner", corner);
            return;
        }
        refitRefused(helper, corner, across, StretchPlan.Reason.SLOPE_TURNS);
    }

    // A level tile above a loader, turned to continue the line below it, would be a top standing on the loader (#420).
    private static void wedgeBlockedRefused(GameTestHelper helper) {
        BlockPos first = new BlockPos(3, 1, 3);
        BlockPos loader = first.east(2);
        BlockPos above = loader.above();
        helper.setBlock(loader, loaderState(BeltTier.BELT, Direction.NORTH));
        ServerPlayer player = builder(helper);
        byHand(helper, player, first, Direction.EAST, 0, 0);
        byHand(helper, player, above, Direction.NORTH, 0);
        refitRefused(helper, first, above, StretchPlan.Reason.WEDGE_BLOCKED);
    }

    /** Rotate once at {@code turned} is refused for {@code reason} and changes nothing around {@code centre}. */
    private static void refitRefused(GameTestHelper helper, BlockPos centre, BlockPos turned, StretchPlan.Reason reason) {
        if (pitch(helper, turned) != BeltTileBlock.PitchState.LEVEL) {
            helper.fail("the tile to turn stands " + helper.getBlockState(turned) + ", expected level", turned);
            return;
        }
        Map<BlockPos, BlockState> before = around(helper, centre);
        ListeningPlayer turner = standingAt(helper, centre.south(2));
        RotatePress.press(turner, helper.absolutePos(turned), false);
        if (!around(helper, centre).equals(before)) {
            helper.fail("a turn refused for " + reason + " changed the world; the turned tile stands "
                    + helper.getBlockState(turned), turned);
            return;
        }
        if (!turner.heard.equals(List.of(reason.messageKey()))) {
            helper.fail("a turn refused for " + reason + " named " + turner.heard, turned);
            return;
        }
        helper.succeed();
    }

    /** Presses both ways at each of {@code aimed}, holding every block of {@code watched} to its state before. */
    private static void refusedUnchanged(GameTestHelper helper, List<BlockPos> aimed, List<BlockPos> watched,
                                         String reason) {
        Map<BlockPos, BlockState> before = new HashMap<>();
        watched.forEach(pos -> before.put(pos, helper.getBlockState(pos)));
        for (BlockPos pos : aimed) {
            for (boolean reverse : List.of(false, true)) {
                ListeningPlayer player = standingAt(helper, pos.north(2));
                RotatePress.press(player, helper.absolutePos(pos), reverse);
                for (BlockPos kept : watched) {
                    if (!helper.getBlockState(kept).equals(before.get(kept))) {
                        helper.fail("a refused turn changed " + before.get(kept) + " to " + helper.getBlockState(kept), kept);
                        return;
                    }
                }
                if (!player.heard.equals(List.of(reason))) {
                    helper.fail("a refused turn named " + player.heard + ", expected " + reason, pos);
                    return;
                }
            }
        }
        helper.succeed();
    }

    private static void heldBeforeAimed(GameTestHelper helper) {
        BlockPos tile = new BlockPos(2, 1, 2);
        BlockState placed = tileState(BeltTier.BELT, Direction.EAST);
        helper.setBlock(tile, placed);
        ListeningPlayer player = standingAt(helper, tile.north(2));
        ItemStack stack = new ItemStack(ItemContent.tileFor(BeltTier.BELT), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        RotatePress.press(player, helper.absolutePos(tile), false);
        if (!HeldTurn.of(player.getMainHandItem()).equals(QuarterTurn.of(1))) {
            helper.fail("a press with a tile held left it turned " + HeldTurn.of(player.getMainHandItem())
                    + ", expected a quarter", tile);
            return;
        }
        if (!helper.getBlockState(tile).equals(placed)) {
            helper.fail("a press with a tile held turned the aimed tile to " + helper.getBlockState(tile), tile);
            return;
        }
        helper.succeed();
    }

    // Stone places a block with nothing to face, so it is not rotatable, as in Factorio.
    private static void unrotatableHeldTurnsAimed(GameTestHelper helper) {
        BlockPos tile = new BlockPos(2, 1, 2);
        helper.setBlock(tile, tileState(BeltTier.BELT, Direction.EAST));
        ListeningPlayer player = standingAt(helper, tile.north(2));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 2));
        RotatePress.press(player, helper.absolutePos(tile), false);
        if (player.getMainHandItem().has(PFDataComponents.QUARTER_TURN.get())) {
            helper.fail("a press with stone held turned the stone", tile);
            return;
        }
        Direction facing = helper.getBlockState(tile).getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (facing != Direction.SOUTH) {
            helper.fail("a press with stone held left the aimed tile facing " + facing + ", expected SOUTH", tile);
            return;
        }
        helper.succeed();
    }

    private static ListeningPlayer standingAt(GameTestHelper helper, BlockPos at) {
        ListeningPlayer player = new ListeningPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        Vec3 feet = Vec3.atBottomCenterOf(helper.absolutePos(at));
        player.setPos(feet.x, feet.y, feet.z);
        return player;
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos floor) {
        BlockPos absolute = helper.absolutePos(floor);
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
    }

    private static BlockState tileState(BeltTier tier, Direction facing) {
        return BlockContent.tileFor(tier).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static BlockState loaderState(BeltTier tier, Direction facing) {
        return BlockContent.loaderFor(tier).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static BeltTileBlock.PitchState pitch(GameTestHelper helper, BlockPos tile) {
        return helper.getBlockState(tile).getValue(BeltTileBlock.PITCH);
    }

    private static ServerPlayer builder(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(false);
        return player;
    }

    private static List<BlockPos> byHand(GameTestHelper helper, ServerPlayer player, BlockPos first, Direction facing,
                                         int... heights) {
        player.setYRot(facing.toYRot());
        List<BlockPos> tiles = new ArrayList<>();
        for (int tile = 0; tile < heights.length; tile++) {
            BlockPos at = first.relative(facing, tile).above(heights[tile]);
            ItemStack stack = new ItemStack(ItemContent.tileFor(BeltTier.BELT));
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            // Aimed at the spot itself, which is air, so the tile goes there whatever stands around it.
            BlockPos absolute = helper.absolutePos(at);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
            player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
            if (!(helper.getBlockState(at).getBlock() instanceof BeltTileBlock)) {
                helper.fail("tile " + tile + " was not placed; " + helper.getBlockState(at) + " stands there", at);
            }
            tiles.add(at);
        }
        return tiles;
    }

    private static Map<BlockPos, BlockState> around(GameTestHelper helper, BlockPos centre) {
        Map<BlockPos, BlockState> states = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-2, -2, -2), centre.offset(2, 2, 2))) {
            states.put(pos.immutable(), helper.getBlockState(pos));
        }
        return states;
    }
}
