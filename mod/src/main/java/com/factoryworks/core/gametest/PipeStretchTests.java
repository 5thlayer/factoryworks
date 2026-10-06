package com.factoryworks.core.gametest;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.Raise;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * The Pack's claim: its pipe Leg builder, which the Pack registers with Groundworks, lays Pipeworks'
 * pipes each open to the next, climbs a rise straight up in place, joins a pipe already beside the
 * leg, plans no arm into a mix of two fluids and refuses a run that would join them whole; and the
 * Pack's Raise reach is 16 (#452, #590, #627, ADR-0110). The gesture, the charge, the detour and the
 * refusals for too few items are Groundworks' own tests.
 *
 * <p>Each test goes through the player's game mode and {@link Raise#press}, as the key's payload
 * presses it.
 */
final class PipeStretchTests {

    private static final BlockPos START = new BlockPos(1, 0, 3);
    private static final BlockPos END = new BlockPos(6, 0, 3);
    private static final Identifier FLUID_PIPE = Identifier.fromNamespaceAndPath("pipeworks", "pipe");

    private PipeStretchTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_flat_pipe_stretch_lays_its_plan_joined_for_one_pipe_a_block", 20, PipeStretchTests::flat);
        tests.test("a_pipe_stretch_raised_3_stacks_3_pipes_at_the_start_then_runs_level", 20, PipeStretchTests::raised);
        tests.test("a_pipe_stretch_joins_a_pipe_already_beside_its_leg", 20, PipeStretchTests::besideAPipe);
        tests.test("a_pipe_stretch_plans_no_arm_to_a_pipe_that_would_mix_two_fluids", 20, PipeStretchTests::mixing);
        tests.test("a_pipe_stretch_from_water_to_lava_is_refused", 20, helper -> endsBeside(helper, Fluids.LAVA, true));
        tests.test("a_pipe_stretch_from_water_to_water_is_accepted", 20, helper -> endsBeside(helper, Fluids.WATER, false));
        tests.test("a_pipe_stretch_from_water_to_an_empty_pipe_is_accepted", 20, helper -> endsBeside(helper, null, false));
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

    /** A single placement opens to a pipe it touches, so a stretch does too, and the old pipe opens back. */
    private static void besideAPipe(GameTestHelper helper) {
        BlockPos beside = new BlockPos(3, 1, 2);
        BlockPos absolute = helper.absolutePos(beside);
        helper.setBlock(beside, LibraryBlocks.pipe().defaultBlockState());
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

    /** A leg pipe between water and lava is closed to both (ADR-0110); beside water alone it opens. */
    private static void mixing(GameTestHelper helper) {
        fill(helper, new BlockPos(3, 1, 2), Fluids.WATER);
        fill(helper, new BlockPos(3, 1, 4), Fluids.LAVA);
        fill(helper, new BlockPos(5, 1, 2), Fluids.WATER);
        ListeningPlayer player = holding(helper, 16);
        click(helper, player, START, true);
        PlacementPlan plan = plan(helper, player, END);
        if (plan == null || plan.isRefused()) {
            helper.fail("the plan was " + plan, END);
        }
        BlockState between = plannedAt(helper, plan, new BlockPos(3, 1, 3));
        if (LibraryBlocks.pipeIsOpen(between, Direction.NORTH) || LibraryBlocks.pipeIsOpen(between, Direction.SOUTH)) {
            helper.fail("the plan opened a pipe between water and lava", new BlockPos(3, 1, 3));
        }
        BlockState beside = plannedAt(helper, plan, new BlockPos(5, 1, 3));
        if (!LibraryBlocks.pipeIsOpen(beside, Direction.NORTH)) {
            helper.fail("the plan did not open a pipe beside one fluid", new BlockPos(5, 1, 3));
        }
        helper.succeed();
    }

    /** Water beside the run's first pipe, {@code other} (or an empty pipe) beside its last (#590). */
    private static void endsBeside(GameTestHelper helper, @Nullable Fluid other, boolean refused) {
        BlockPos far = new BlockPos(6, 1, 2);
        fill(helper, new BlockPos(1, 1, 2), Fluids.WATER);
        if (other == null) {
            helper.setBlock(far, LibraryBlocks.pipe().defaultBlockState());
        } else {
            fill(helper, far, other);
        }
        ListeningPlayer player = holding(helper, 16);
        click(helper, player, START, true);
        PlacementPlan plan = plan(helper, player, END);
        if (plan == null || plan.isRefused() != refused) {
            helper.fail("the plan was " + plan + ", expected refused=" + refused, END);
        }
        if (refused) {
            click(helper, player, END, false);
            for (BlockPos pos : row(1, 6, 1)) {
                if (!helper.getBlockState(pos).isAir()) {
                    helper.fail("a refused stretch laid " + helper.getBlockState(pos), pos);
                }
            }
        }
        helper.succeed();
    }

    private static void fill(GameTestHelper helper, BlockPos pos, Fluid fluid) {
        helper.setBlock(pos, LibraryBlocks.pipe().defaultBlockState());
        var handler = helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, helper.absolutePos(pos), null);
        try (Transaction transaction = Transaction.openRoot()) {
            handler.insert(FluidResource.of(fluid), 50, transaction);
            transaction.commit();
        }
    }

    private static BlockState plannedAt(GameTestHelper helper, PlacementPlan plan, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        return plan.blocks().stream().filter(placed -> placed.pos().equals(absolute)).findFirst()
                .orElseThrow(() -> helper.assertionException("the plan has no pipe at " + pos)).state();
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

    /** Each pipe linked to the next the way its segment draws it, and each end closed to the air beyond it. */
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
        return LibraryBlocks.pipeIsOpen(state, side);
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

    /** A survival player standing north of the start's row, looking east, holding {@code count} pipes. */
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
