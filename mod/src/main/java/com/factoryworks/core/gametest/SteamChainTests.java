package com.factoryworks.core.gametest;

import java.util.List;

import io.github._5thlayer.pipeworks.api.FluidPorts;
import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.fluid.BoilerBlockEntity;
import com.factoryworks.core.fluid.BoilerFootprint;
import com.factoryworks.core.fluid.BoilerSlots;
import com.factoryworks.core.fluid.PFFluids;
import com.factoryworks.core.fluid.SteamEngineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The Pack's claim: its Offshore Pump, Boiler and Steam Engine make power when joined by registered
 * Pipeworks pipes, and the Boiler's water row and steam port stand in different segments (#593, #627,
 * ADR-0114). Pipeworks' own GameTests hold how a segment fills, splits and refuses a mix.
 *
 * <p>Water and steam on Pipeworks (#593, ADR-0114): an Offshore Pump through a Boiler to a Steam
 * Engine, water passing through one Boiler's front row into the next, and a steam pipe refused at
 * the water row.
 *
 * <p>Everything is laid out from the footprints' own positions, not from a facing assumed: the
 * pump chain needs the Boiler's row along the platform's short axis and the pass-through needs it
 * along the long one, so each test asks which facing does that.
 */
final class SteamChainTests {

    /** Ticks for a port to join its segment after placement. */
    private static final int PORTS_JOIN = 2;

    private SteamChainTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("pump_boiler_and_engine_make_power_through_pipes", 400, SteamChainTests::makesPower);
        tests.test("water_passes_through_one_boilers_row_to_a_second", 300, SteamChainTests::passesThrough);
        tests.test("a_steam_pipe_into_the_water_row_waits", 100, SteamChainTests::refusesMixing);
    }

    /** Pump, a pipe, the Boiler's row end; then the steam port, two pipes and the Engine. */
    private static void makesPower(GameTestHelper helper) {
        Direction facing = facingWithBackAlong(Direction.Axis.X);
        List<BlockPos> boiler = boilerAt(helper, facing, anchorX(facing), 3);
        BlockPos back = back(boiler);
        BlockPos lateral = lateral(boiler);
        BlockPos anchor = boiler.getFirst();

        put(helper, anchor.offset(lateral).offset(lateral), LibraryBlocks.pipe());
        put(helper, anchor.offset(lateral).offset(lateral).offset(lateral), PFBlocks.OFFSHORE_PUMP.get());
        put(helper, anchor.offset(back).offset(back), LibraryBlocks.pipe());
        put(helper, anchor.offset(back).offset(back).offset(back), LibraryBlocks.pipe());
        BlockPos engine = anchor.offset(back).offset(back).offset(back).offset(back);
        PFBlocks.STEAM_ENGINE_FOOTPRINT.placeAll(helper.getLevel(), engine, engineFacing());
        ((BoilerBlockEntity) helper.getLevel().getBlockEntity(anchor)).setItem(BoilerSlots.FUEL, new ItemStack(Items.COAL, 8));

        helper.succeedWhen(() -> {
            SteamEngineBlockEntity entity = (SteamEngineBlockEntity) helper.getLevel().getBlockEntity(engine);
            if (entity.energyHandler().getAmountAsLong() <= 0L) {
                helper.fail("the engine made no power from a pump's water and the Boiler's steam", helper.relativePos(engine));
            }
        });
    }

    /** The pump reaches the second Boiler only through the first's front row, and only the second is fuelled. */
    private static void passesThrough(GameTestHelper helper) {
        Direction facing = facingWithBackAlong(Direction.Axis.Z);
        List<BlockPos> first = boilerAt(helper, facing, 7, 3);
        List<BlockPos> second = boilerAt(helper, facing, 10, 3);
        helper.setBlock(new BlockPos(5, 1, 3), LibraryBlocks.pipe());
        helper.setBlock(new BlockPos(4, 1, 3), PFBlocks.OFFSHORE_PUMP.get());
        ((BoilerBlockEntity) helper.getLevel().getBlockEntity(second.getFirst())).setItem(BoilerSlots.FUEL, new ItemStack(Items.COAL, 8));

        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    ResourceHandler<FluidResource> row = FluidPorts.segment(helper.getLevel(), first.getFirst());
                    ResourceHandler<FluidResource> next = FluidPorts.segment(helper.getLevel(), second.getFirst());
                    if (row == null || next == null || row.getAmountAsInt(0) == 0
                            || row.getAmountAsInt(0) != next.getAmountAsInt(0)) {
                        helper.fail("the two Boilers' rows are not one segment holding water", helper.relativePos(second.getFirst()));
                    }
                    ResourceHandler<FluidResource> steam = steamOf(helper, second);
                    if (steam == null || steam.getAmountAsInt(0) <= 0 || !steam.getResource(0).equals(steamResource())) {
                        helper.fail("the second Boiler made no steam from water that passed through the first", helper.relativePos(second.getFirst()));
                    }
                    if (steamOf(helper, first).getAmountAsInt(0) != 0) {
                        helper.fail("the unfuelled Boiler made steam", helper.relativePos(first.getFirst()));
                    }
                })
                .thenSucceed();
    }

    /** A pipe that would join the steam segment to the water row waits outside both, and neither fluid moves. */
    private static void refusesMixing(GameTestHelper helper) {
        Direction facing = facingWithBackAlong(Direction.Axis.X);
        List<BlockPos> boiler = boilerAt(helper, facing, anchorX(facing), 3);
        BlockPos back = back(boiler);
        BlockPos lateral = lateral(boiler);
        BlockPos anchor = boiler.getFirst();
        BlockPos steamPipe = anchor.offset(back).offset(back);
        BlockPos[] around = {
                steamPipe, steamPipe.offset(lateral), steamPipe.offset(lateral).offset(lateral),
                anchor.offset(back).offset(lateral).offset(lateral)};
        for (BlockPos pipe : around) {
            put(helper, pipe, LibraryBlocks.pipe());
        }
        BlockPos joint = anchor.offset(lateral).offset(lateral);

        helper.startSequence()
                .thenIdle(PORTS_JOIN)
                .thenExecute(() -> {
                    insert(FluidPorts.segment(helper.getLevel(), anchor), FluidResource.of(Fluids.WATER), 100);
                    insert(FluidPorts.segment(helper.getLevel(), steamPipe), steamResource(), 50);
                    if (FluidPorts.canJoin(helper.getLevel(), joint, face -> true)) {
                        helper.fail("a pipe between the water row and the steam would be placed", helper.relativePos(joint));
                    }
                    put(helper, joint, LibraryBlocks.pipe());
                })
                .thenIdle(PORTS_JOIN)
                .thenExecute(() -> {
                    if (FluidPorts.segment(helper.getLevel(), joint) != null) {
                        helper.fail("the pipe joined water and steam", helper.relativePos(joint));
                    }
                    ResourceHandler<FluidResource> row = FluidPorts.segment(helper.getLevel(), anchor);
                    ResourceHandler<FluidResource> steam = FluidPorts.segment(helper.getLevel(), steamPipe);
                    if (row.getAmountAsInt(0) != 100 || !row.getResource(0).equals(FluidResource.of(Fluids.WATER))) {
                        helper.fail("the water row holds " + row.getAmountAsInt(0) + " mB of " + row.getResource(0), helper.relativePos(anchor));
                    }
                    if (steam.getAmountAsInt(0) != 50 || !steam.getResource(0).equals(steamResource())) {
                        helper.fail("the steam holds " + steam.getAmountAsInt(0) + " mB of " + steam.getResource(0), helper.relativePos(steamPipe));
                    }
                })
                .thenSucceed();
    }

    /** Placed at an absolute position, as the footprints are. */
    private static void put(GameTestHelper helper, BlockPos absolute, Block block) {
        helper.getLevel().setBlock(absolute, block.defaultBlockState(), Block.UPDATE_ALL);
    }

    /** The footprint's absolute positions, anchor first, placed at the relative column and row. */
    private static List<BlockPos> boilerAt(GameTestHelper helper, Direction facing, int x, int z) {
        BlockPos anchor = helper.absolutePos(new BlockPos(x, 1, z));
        PFBlocks.BOILER_FOOTPRINT.placeAll(helper.getLevel(), anchor, facing);
        return PFBlocks.BOILER_FOOTPRINT.positions(anchor, facing);
    }

    /** A facing whose backward step runs along {@code axis}. */
    private static Direction facingWithBackAlong(Direction.Axis axis) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos step = back(PFBlocks.BOILER_FOOTPRINT.positions(BlockPos.ZERO, facing));
            if (axis == Direction.Axis.X ? step.getX() != 0 : step.getZ() != 0) {
                return facing;
            }
        }
        throw new IllegalStateException("no facing runs a Boiler's back along " + axis);
    }

    /** The step from the anchor to the back middle: the steam port's side. */
    private static BlockPos back(List<BlockPos> boiler) {
        return boiler.get(BoilerFootprint.STEAM_PART).subtract(boiler.getFirst());
    }

    /** The step along the front row from the anchor towards part 2. */
    private static BlockPos lateral(List<BlockPos> boiler) {
        return boiler.get(2).subtract(boiler.getFirst());
    }

    /** Far enough from the platform's end that the back leg and the engine fit. */
    private static int anchorX(Direction facing) {
        return PFBlocks.BOILER_FOOTPRINT.positions(BlockPos.ZERO, facing).get(BoilerFootprint.STEAM_PART).getX() > 0 ? 4 : 18;
    }

    /** An engine facing whose lateral part stands off the row of pipes. */
    private static Direction engineFacing() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            if (PFBlocks.STEAM_ENGINE_FOOTPRINT.positions(BlockPos.ZERO, facing).get(2).getX() == 0) {
                return facing;
            }
        }
        throw new IllegalStateException("no facing puts a Steam Engine's part off the x axis");
    }

    private static ResourceHandler<FluidResource> steamOf(GameTestHelper helper, List<BlockPos> boiler) {
        return FluidPorts.segment(helper.getLevel(), boiler.get(BoilerFootprint.STEAM_PART));
    }

    private static FluidResource steamResource() {
        return FluidResource.of(PFFluids.STEAM_SOURCE.get());
    }

    private static void insert(ResourceHandler<FluidResource> segment, FluidResource fluid, int amount) {
        try (Transaction tx = Transaction.openRoot()) {
            segment.insert(fluid, amount, tx);
            tx.commit();
        }
    }
}
