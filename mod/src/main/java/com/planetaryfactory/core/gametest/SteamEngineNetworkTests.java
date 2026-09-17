package com.planetaryfactory.core.gametest;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.energy.NetworkReading;
import com.planetaryfactory.core.energy.PoleTier;
import com.planetaryfactory.core.energy.SupplyAreaPoleBlockEntity;
import com.planetaryfactory.core.fluid.PFFluids;
import com.planetaryfactory.core.smelting.FurnaceBlockEntity;
import com.planetaryfactory.core.smelting.FurnaceTier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.block.entity.generators.SteamEngineEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.Geometry;

/**
 * Oritech's Steam Engine on the pole network (#292, ADR-0062).
 *
 * <p>{@code SupplyScanTest} holds the rule -- every block stands for its energy owner, kept once.
 * What it cannot see is that the owners are the right ones: that a machine core's face really is the
 * controller's, that a slave really names its master, and that the mixins answering those questions
 * are applied at all. A mixin that failed to apply is a warning in the log and a scan that files by
 * position again.
 *
 * <h2>The layout</h2>
 *
 * <p>A row of three engines A, B, C, assembled with their machine cores. A is fed first, so it scans
 * the row and becomes master of B and C. From then on steam goes in at both ends, A's own face and
 * C's, which Oritech delegates to A's tank. The tank is held at 70 %, which is Oritech's speed 7, the
 * peak {@code SteamEngineSpec} is calibrated at.
 *
 * <p>A small pole stands two blocks past C along the row, so its 5x5 area holds C and C's cores and
 * nothing of A or B. An Electric Furnace beside the pole is the one consumer, emptied every tick so it
 * always asks for more than the row makes. The row then makes 1,350 FE a tick, and the pole must draw
 * all of it through a slave and its hull, from a master it cannot reach, and once.
 */
final class SteamEngineNetworkTests {

    /** Three engines at 450 FE/t apiece (ADR-0060: 900 kW at 100 J per FE). */
    private static final long ROW_FE_PER_TICK = 1_350L;

    private static final BlockPos ORIGIN = new BlockPos(0, 1, 3);

    /** Past one rescan interval (40) plus the tick the network is rebuilt on. */
    private static final int SETTLE = 45;

    /** Ticks the draw is read over once it has settled. */
    private static final int MEASURED = 20;

    private SteamEngineNetworkTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("pole_draws_steam_engine_row_through_a_slave", 400,
                SteamEngineNetworkTests::poleDrawsRowThroughASlave);
    }

    private record Layout(Direction facing, BlockPos a, BlockPos b, BlockPos c, BlockPos pole,
                          BlockPos furnace) {
    }

    private static void poleDrawsRowThroughASlave(GameTestHelper helper) {
        Layout at = layout(helper);
        for (BlockPos engine : new BlockPos[] {at.a(), at.b(), at.c()}) {
            assemble(helper, engine, at.facing());
        }
        helper.setBlock(at.pole(), PFBlocks.pole(PoleTier.SMALL).get());
        helper.setBlock(at.furnace(), PFBlocks.furnace(FurnaceTier.ELECTRIC).get());

        long[] produced = {0L};
        helper.onEachTick(() -> {
            topUp(helper, engine(helper, at.a()));
            if (engine(helper, at.c()).inSlaveMode()) {
                topUp(helper, engine(helper, at.c()));
            }
            furnace(helper, at.furnace()).data().set(FurnaceBlockEntity.DATA_ENERGY, 0);
        });
        helper.startSequence()
                .thenWaitUntil(() -> {
                    if (!engine(helper, at.b()).inSlaveMode() || !engine(helper, at.c()).inSlaveMode()) {
                        helper.fail("A never became master of the row", at.a());
                    }
                })
                .thenIdle(SETTLE)
                .thenExecuteFor(MEASURED, () -> {
                    SupplyAreaPoleBlockEntity pole =
                            helper.getBlockEntity(at.pole(), SupplyAreaPoleBlockEntity.class);
                    NetworkReading reading = pole.networkReading();
                    if (pole.machineCount() != 1) {
                        helper.fail("the pole files " + pole.machineCount() + " consumers; only the"
                                + " furnace is one, so an engine or its hull is being fed", at.pole());
                    }
                    if (reading.produced() != ROW_FE_PER_TICK) {
                        helper.fail("the pole drew " + reading.produced() + " FE from a row of three"
                                + " making " + ROW_FE_PER_TICK + " a tick", at.pole());
                    }
                    if (reading.delivered() != reading.produced()) {
                        helper.fail("the furnace received " + reading.delivered() + " of "
                                + reading.produced() + " FE drawn", at.furnace());
                    }
                    produced[0] += reading.produced();
                })
                .thenExecute(() -> {
                    if (produced[0] != ROW_FE_PER_TICK * MEASURED) {
                        helper.fail("drew " + produced[0] + " FE over " + MEASURED + " ticks", at.pole());
                    }
                })
                .thenSucceed();
    }

    /**
     * The row runs along Oritech's local x, which is asked of {@link Geometry} rather than assumed:
     * the facing is the first whose row lies along the platform's long axis, x.
     */
    private static Layout layout(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(ORIGIN);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos step = new BlockPos(
                    Geometry.offsetToWorldPosition(facing, new Vec3i(1, 0, 0), origin)).subtract(origin);
            if (step.getX() != 0) {
                return layout(facing, step);
            }
        }
        throw helper.assertionException(ORIGIN, "no facing runs a Steam Engine row along x");
    }

    private static Layout layout(Direction facing, BlockPos step) {
        // Start at whichever end of the platform leaves room for the pole and furnace past C.
        BlockPos a = step.getX() > 0 ? new BlockPos(2, 1, 3) : new BlockPos(20, 1, 3);
        BlockPos b = a.offset(step);
        BlockPos c = b.offset(step);
        BlockPos pole = c.offset(step).offset(step);
        return new Layout(facing, a, b, c, pole, pole.offset(step));
    }

    private static void assemble(GameTestHelper helper, BlockPos relative, Direction facing) {
        BlockState state = BlockContent.STEAM_ENGINE.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing);
        helper.setBlock(relative, state);
        SteamEngineEntity engine = engine(helper, relative);
        BlockPos absolute = helper.absolutePos(relative);
        for (Vec3i core : engine.getCorePositions()) {
            helper.getLevel().setBlockAndUpdate(absolute.offset(Geometry.rotatePosition(core, facing)),
                    BlockContent.MACHINE_CORE_1.get().defaultBlockState());
        }
        if (!engine.initMultiblock(helper.getBlockState(relative))) {
            helper.fail("the Steam Engine did not assemble", relative);
        }
    }

    /** Holds the tank this face reaches at 70 % of its capacity: Oritech's speed 7. */
    private static void topUp(GameTestHelper helper, SteamEngineEntity engine) {
        ResourceHandler<FluidResource> face = engine.getFluidLookup(null);
        // The Boiler's steam, not Oritech's: a test fed Oritech's own fluid passed while the pack's
        // steam made nothing in game.
        FluidResource steam = FluidResource.of(PFFluids.STEAM_SOURCE.get());
        long target = face.getCapacityAsLong(0, steam) * 7 / 10;
        long missing = target - face.getAmountAsLong(0);
        if (missing <= 0L) {
            return;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            if (face.insert(steam, (int) missing, transaction) != missing) {
                helper.fail("the Steam Engine refused steam", engine.getBlockPos());
            }
            transaction.commit();
        }
    }

    private static SteamEngineEntity engine(GameTestHelper helper, BlockPos relative) {
        return helper.getBlockEntity(relative, SteamEngineEntity.class);
    }

    private static FurnaceBlockEntity furnace(GameTestHelper helper, BlockPos relative) {
        return helper.getBlockEntity(relative, FurnaceBlockEntity.class);
    }
}
