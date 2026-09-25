package com.planetaryfactory.core.gametest;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.groundworks.Groundworks;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Raise;
import io.github._5thlayer.groundworks.Refusal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import rearth.oritech.block.blocks.pipes.AbstractPipeBlock;

/**
 * Oritech's fluid pipes laid by Groundworks' Stretch (#452), through the player's game mode and
 * {@link Raise#press}, as the key's payload presses it.
 */
final class PipeStretchTests {

    private static final BlockPos START = new BlockPos(1, 0, 3);
    private static final BlockPos END = new BlockPos(6, 0, 3);
    private static final Identifier FLUID_PIPE = Identifier.fromNamespaceAndPath("oritech", "fluid_pipe");

    private PipeStretchTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_flat_pipe_stretch_lays_its_plan_joined_for_one_pipe_a_block", 20, PipeStretchTests::flat);
        tests.test("a_pipe_stretch_raised_3_stacks_3_pipes_at_the_start_then_runs_level", 20, PipeStretchTests::raised);
        tests.test("a_pipe_stretch_goes_round_a_block_on_its_leg", 20, PipeStretchTests::detour);
        tests.test("a_pipe_stretch_with_too_few_pipes_is_refused_whole", 20, PipeStretchTests::tooFew);
        tests.test("a_pipe_stretch_joins_a_pipe_already_beside_its_leg", 20, PipeStretchTests::besideAPipe);
        tests.test("a_pipe_s_raise_reaches_as_far_as_the_pack_s_reach", 20, PipeStretchTests::reach);
    }

    private static void flat(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 16);
        click(helper, player, START, true);
        List<BlockPos> laid = layAsPlanned(helper, player, END);
        expectPositions(helper, laid, row(1, 6, 1));
        expectJoined(helper, laid);
        expectHeld(helper, player, 10);
        helper.succeed();
    }

    private static void raised(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 16);
        click(helper, player, START, true);
        for (int press = 0; press < 3; press++) {
            Raise.press(player, false);
        }
        List<BlockPos> laid = layAsPlanned(helper, player, END);
        List<BlockPos> expected = new ArrayList<>(List.of(new BlockPos(1, 1, 3), new BlockPos(1, 2, 3), new BlockPos(1, 3, 3)));
        expected.addAll(row(1, 6, 4));
        expectPositions(helper, laid, expected);
        expectJoined(helper, laid);
        expectHeld(helper, player, 7);
        helper.succeed();
    }

    /** The player stands north of the line, so the detour takes the north side. */
    private static void detour(GameTestHelper helper) {
        BlockPos stone = new BlockPos(4, 1, 3);
        helper.setBlock(stone, Blocks.STONE);
        ListeningPlayer player = holding(helper, 16);
        click(helper, player, START, true);
        List<BlockPos> laid = layAsPlanned(helper, player, END);
        if (!helper.getBlockState(stone).is(Blocks.STONE)) {
            helper.fail("the stretch replaced the stone with " + helper.getBlockState(stone), stone);
        }
        if (!laid.getFirst().equals(helper.absolutePos(START.above())) || !laid.getLast().equals(helper.absolutePos(END.above()))) {
            helper.fail("the detour ran from " + laid.getFirst() + " to " + laid.getLast() + ", not from the start to the end");
        }
        for (BlockPos pos : laid) {
            if (pos.getY() != helper.absolutePos(START).getY() + 1 || pos.getZ() > helper.absolutePos(START).getZ()) {
                helper.fail("the detour left its height or went round the far side", pos);
            }
        }
        expectJoined(helper, laid);
        expectHeld(helper, player, 16 - laid.size());
        helper.succeed();
    }

    /** A single placement opens to a pipe it touches, so a stretch does too, and the old pipe opens back. */
    private static void besideAPipe(GameTestHelper helper) {
        BlockPos beside = new BlockPos(3, 1, 2);
        Block block = BuiltInRegistries.BLOCK.getValue(FLUID_PIPE);
        BlockPos absolute = helper.absolutePos(beside);
        helper.getLevel().setBlockAndUpdate(absolute,
                ((AbstractPipeBlock) block).addConnectionStates(block.defaultBlockState(), helper.getLevel(), absolute, true));
        ListeningPlayer player = holding(helper, 16);
        click(helper, player, START, true);
        List<BlockPos> laid = layAsPlanned(helper, player, END);
        expectPositions(helper, laid, row(1, 6, 1));
        BlockPos leg = helper.absolutePos(new BlockPos(3, 1, 3));
        if (!open(helper, leg, Direction.NORTH) || !open(helper, absolute, Direction.SOUTH)) {
            helper.fail("the stretch did not join the pipe beside it", beside);
        }
        helper.succeed();
    }

    private static void tooFew(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 4);
        click(helper, player, START, true);
        PlacementPlan plan = plan(helper, player, END);
        if (plan == null || plan.refusal() != Refusal.Stretch.NOT_ENOUGH_ITEMS) {
            helper.fail("the plan was " + plan + ", not refused for too few pipes", END);
        }
        click(helper, player, END, false);
        for (BlockPos pos : row(1, 6, 1)) {
            if (!helper.getBlockState(pos).isAir()) {
                helper.fail("a refused stretch laid " + helper.getBlockState(pos), pos);
            }
        }
        expectHeld(helper, player, 4);
        if (!player.getMainHandItem().has(Groundworks.STRETCH.get())) {
            helper.fail("a refused stretch dropped the stretch being drawn");
        }
        helper.succeed();
    }

    // Vanilla's reach would stop the raise at 4 (#413).
    private static void reach(GameTestHelper helper) {
        ListeningPlayer player = holding(helper, 16);
        for (int press = 0; press < 17; press++) {
            Raise.press(player, false);
        }
        int height = Raise.heightOf(player, player.getMainHandItem()).blocks();
        if (height != 16) {
            helper.fail("seventeen presses of Raise held a pipe at " + height + ", not the Pack's reach of 16");
        }
        helper.succeed();
    }

    /**
     * Lays the stretch to an end aimed at {@code floor}'s top and answers the plan's positions in its
     * order, failing unless the click laid exactly its states.
     */
    private static List<BlockPos> layAsPlanned(GameTestHelper helper, ListeningPlayer player, BlockPos floor) {
        PlacementPlan plan = plan(helper, player, floor);
        if (plan == null || plan.isRefused()) {
            helper.fail("the plan was " + plan, floor);
        }
        click(helper, player, floor, false);
        List<BlockPos> laid = new ArrayList<>();
        for (PlacementPlan.Placed placed : plan.blocks()) {
            BlockState there = helper.getLevel().getBlockState(placed.pos());
            if (!there.equals(placed.state())) {
                helper.fail("the plan put " + placed.state() + " at " + placed.pos() + ", the click laid " + there);
            }
            laid.add(placed.pos());
        }
        return laid;
    }

    private static void expectPositions(GameTestHelper helper, List<BlockPos> laid, List<BlockPos> expected) {
        List<BlockPos> wanted = expected.stream().map(helper::absolutePos).toList();
        if (!laid.equals(wanted)) {
            helper.fail("the stretch laid " + laid + ", expected " + wanted);
        }
    }

    /** Each pipe open to the next the way Oritech's network reads it, and each end closed to the air beyond it. */
    private static void expectJoined(GameTestHelper helper, List<BlockPos> laid) {
        for (int i = 0; i + 1 < laid.size(); i++) {
            BlockPos a = laid.get(i);
            BlockPos b = laid.get(i + 1);
            Direction side = Direction.getApproximateNearest(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ());
            if (!a.relative(side).equals(b) || !open(helper, a, side) || !open(helper, b, side.getOpposite())) {
                helper.fail("the pipes at " + a + " and " + b + " are not joined", helper.relativePos(a));
            }
        }
        for (BlockPos pos : laid) {
            for (Direction side : Direction.values()) {
                if (open(helper, pos, side) && !laid.contains(pos.relative(side))) {
                    helper.fail("the pipe at " + pos + " is open " + side + " to " + helper.getLevel().getBlockState(pos.relative(side)),
                            helper.relativePos(pos));
                }
            }
        }
    }

    private static boolean open(GameTestHelper helper, BlockPos pos, Direction side) {
        BlockState state = helper.getLevel().getBlockState(pos);
        return state.getBlock() instanceof AbstractPipeBlock pipe
                && pipe.isConnectingInDirection(state, side, pos, helper.getLevel(), false);
    }

    private static void expectHeld(GameTestHelper helper, ListeningPlayer player, int held) {
        int count = player.getMainHandItem().getCount();
        if (count != held) {
            helper.fail("the player holds " + count + " pipes, expected " + held);
        }
    }

    private static List<BlockPos> row(int fromX, int toX, int y) {
        List<BlockPos> row = new ArrayList<>();
        for (int x = fromX; x <= toX; x++) {
            row.add(new BlockPos(x, y, START.getZ()));
        }
        return row;
    }

    /** A survival player standing north of the start's row, looking east, holding {@code count} fluid pipes. */
    private static ListeningPlayer holding(GameTestHelper helper, int count) {
        ListeningPlayer player = new ListeningPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(0, 1, 0))));
        player.setYRot(Direction.EAST.toYRot());
        player.setYHeadRot(Direction.EAST.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.getValue(FLUID_PIPE), count));
        return player;
    }

    private static @Nullable PlacementPlan plan(GameTestHelper helper, ListeningPlayer player, BlockPos floor) {
        return Placements.planFor(player.level(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(), onTop(helper, floor));
    }

    private static void click(GameTestHelper helper, ListeningPlayer player, BlockPos floor, boolean sneaking) {
        player.setShiftKeyDown(sneaking);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, onTop(helper, floor));
        player.setShiftKeyDown(false);
    }

    private static BlockHitResult onTop(GameTestHelper helper, BlockPos floor) {
        BlockPos absolute = helper.absolutePos(floor);
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
    }
}
