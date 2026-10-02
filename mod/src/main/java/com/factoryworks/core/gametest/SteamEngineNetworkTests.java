package com.factoryworks.core.gametest;

import io.github._5thlayer.pipeworks.api.FluidPorts;
import io.github._5thlayer.wireworks.WireworksRegistries;
import com.factoryworks.core.PFBlocks;
import io.github._5thlayer.wireworks.NetworkReading;
import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.SupplyAreaPoleBlockEntity;
import com.factoryworks.core.fluid.PFFluids;
import com.factoryworks.core.smelting.FurnaceBlockEntity;
import com.factoryworks.core.smelting.FurnaceTier;

import com.factoryworks.core.fluid.SteamEngineBlockEntity;
import com.factoryworks.core.fluid.SteamEngineSpec;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.block.entity.generators.SteamEngineEntity;
import rearth.oritech.util.Geometry;

/**
 * The pack's Steam Engine on the pole network (#292, #352, ADR-0062, ADR-0077).
 *
 * <p>{@code SupplyScanTest} holds the rule -- every block stands for its energy owner, kept once.
 * What it cannot see is that the owners are the right ones: that a part's face really is its
 * anchor's, that a slave really names its master, and that the mixin answering the second is
 * applied at all. A mixin that failed to apply is a warning in the log and a scan that files by
 * position again.
 *
 * <h2>The layout</h2>
 *
 * <p>A row of three engines A, B, C, whose ports touch and so share one steam segment, which the test
 * keeps full. A is the head of the row, so it draws first, scans the row and becomes master of B and
 * C; from then on a slave's port draws into A's tank. Each port holds the tank at 70 %, which is
 * Oritech's speed 7, the peak {@code SteamEngineSpec} is calibrated at.
 *
 * <p>A small pole stands beside C on its parts' side, far enough out that its 5x5 area holds C's
 * parts and nothing else of the row, not even C's anchor. An Electric Furnace beside the pole is the
 * one consumer, emptied every tick so it always asks for more than the row makes. The row then makes
 * 1,350 FE a tick, and the pole must draw all of it through a slave's part, from a master it cannot
 * reach, and once.
 */
final class SteamEngineNetworkTests {

    /** Three engines at 450 FE/t apiece (ADR-0060: 900 kW at 100 J per FE). */
    private static final long ROW_FE_PER_TICK = 1_350L;

    private static final BlockPos ORIGIN = new BlockPos(0, 1, 3);

    /** The part at {@code (0,0,-1)}: beside the anchor, on the ground, off the row's axis. */
    private static final int LATERAL_PART = 2;

    /** Past one rescan interval (40) plus the tick the network is rebuilt on. */
    private static final int SETTLE = 45;

    /** Ticks the draw is read over once it has settled. */
    private static final int MEASURED = 20;

    private SteamEngineNetworkTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("pole_draws_steam_engine_row_through_a_slaves_part", 400,
                SteamEngineNetworkTests::poleDrawsRowThroughASlavesPart);
        tests.test("steam_engine_row_chains_across_a_part", 200,
                SteamEngineNetworkTests::rowChainsAcrossAPart);
        tests.test("steam_engine_reloads_as_the_packs_engine", 20,
                SteamEngineNetworkTests::reloadsAsThePacksEngine);
        tests.test("steam_engine_has_no_fluid_face_and_draws_steam_alone", 60,
                SteamEngineNetworkTests::drawsSteamAlone);
    }

    private record Layout(Direction facing, BlockPos a, BlockPos b, BlockPos c, BlockPos pole,
                          BlockPos furnace) {
    }

    private static void poleDrawsRowThroughASlavesPart(GameTestHelper helper) {
        Layout at = layout(helper);
        for (BlockPos engine : new BlockPos[] {at.a(), at.b(), at.c()}) {
            place(helper, engine, at.facing());
        }
        helper.setBlock(at.pole(), WireworksRegistries.pole(PoleTier.SMALL).get());
        helper.setBlock(at.furnace(), PFBlocks.furnace(FurnaceTier.ELECTRIC).get());

        long[] produced = {0L};
        helper.onEachTick(() -> {
            topUp(helper, at.a());
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
                                + " furnace is one, so an engine or its part is being fed", at.pole());
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
        // Three out on C's lateral part's side: the area's near edge is that part, not C's anchor.
        BlockPos lateral = partOffset(facing, LATERAL_PART);
        BlockPos pole = c.offset(step).offset(step).offset(lateral.multiply(3));
        return new Layout(facing, a, b, c, pole, pole.offset(step));
    }

    /** A and B in a row, and C turned a quarter so its lateral part, not its anchor, is the block the scan meets next. */
    private static void rowChainsAcrossAPart(GameTestHelper helper) {
        Layout at = layout(helper);
        BlockPos step = at.b().subtract(at.a());
        Direction turned = null;
        for (Direction candidate : Direction.Plane.HORIZONTAL) {
            if (partOffset(candidate, LATERAL_PART).equals(BlockPos.ZERO.subtract(step))) {
                turned = candidate;
            }
        }
        if (turned == null) {
            throw helper.assertionException(at.a(), "no facing puts a Steam Engine's part on the row");
        }
        BlockPos cPart = at.b().offset(step);
        BlockPos cAnchor = cPart.subtract(partOffset(turned, LATERAL_PART));
        place(helper, at.a(), at.facing());
        place(helper, at.b(), at.facing());
        place(helper, cAnchor, turned);

        helper.onEachTick(() -> topUp(helper, at.a()));
        helper.succeedWhen(() -> {
            SteamEngineEntity c = engine(helper, cAnchor);
            if (!c.inSlaveMode() || c.master != engine(helper, at.a())) {
                helper.fail("the scan did not chain C through the part standing in the row", cPart);
            }
        });
    }

    /** The world offset of part {@code index} of an engine facing {@code facing}. */
    private static BlockPos partOffset(Direction facing, int index) {
        return PFBlocks.STEAM_ENGINE_FOOTPRINT.positions(BlockPos.ZERO, facing).get(index);
    }

    private static void place(GameTestHelper helper, BlockPos anchor, Direction facing) {
        PFBlocks.STEAM_ENGINE_FOOTPRINT.placeAll(helper.getLevel(), helper.absolutePos(anchor), facing);
    }

    /**
     * The engine answers no fluid capability on any block; only its anchor stands in a segment. A
     * segment of water feeds the tank nothing, and a segment of steam holds it at the peak fill
     * and no more.
     */
    private static void drawsSteamAlone(GameTestHelper helper) {
        BlockPos anchor = new BlockPos(3, 1, 3);
        place(helper, anchor, Direction.NORTH);
        BlockPos absolute = helper.absolutePos(anchor);
        List<BlockPos> blocks = PFBlocks.STEAM_ENGINE_FOOTPRINT.positions(absolute, Direction.NORTH);
        FluidResource steam = FluidResource.of(PFFluids.STEAM_SOURCE.get());
        long peak = SteamEngineSpec.peakFill(
                engine(helper, anchor).boilerStorage.getInputContainer().getCapacityAsLong(0, steam));
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    for (int i = 0; i < blocks.size(); i++) {
                        BlockPos at = blocks.get(i);
                        BlockPos relative = helper.relativePos(at);
                        if (helper.getLevel().getCapability(
                                net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, at, null) != null) {
                            helper.fail("a Steam Engine block answers a fluid capability", relative);
                        }
                        boolean inSegment = FluidPorts.segment(helper.getLevel(), at) != null;
                        if (inSegment != (i == 0)) {
                            helper.fail(i == 0 ? "the anchor is in no segment" : "a part is in a segment", relative);
                        }
                    }
                })
                .thenExecute(() -> insert(helper, anchor, FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 100))
                .thenIdle(10)
                .thenExecute(() -> {
                    long held = engine(helper, anchor).boilerStorage.getInputContainer().getAmountAsLong(0);
                    if (held != 0L) {
                        helper.fail("the engine drew " + held + " mB from a segment of water", anchor);
                    }
                    drain(helper, anchor);
                    insert(helper, anchor, steam, 200);
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    long held = engine(helper, anchor).boilerStorage.getInputContainer().getAmountAsLong(0);
                    if (held > peak || held < peak - 3) {
                        helper.fail("the engine's tank holds " + held + " mB, not the peak fill of " + peak, anchor);
                    }
                })
                .thenSucceed();
    }

    private static void insert(GameTestHelper helper, BlockPos anchor, FluidResource fluid, int amount) {
        ResourceHandler<FluidResource> segment = FluidPorts.segment(helper.getLevel(), helper.absolutePos(anchor));
        try (Transaction transaction = Transaction.openRoot()) {
            segment.insert(fluid, amount, transaction);
            transaction.commit();
        }
    }

    private static void drain(GameTestHelper helper, BlockPos anchor) {
        ResourceHandler<FluidResource> segment = FluidPorts.segment(helper.getLevel(), helper.absolutePos(anchor));
        try (Transaction transaction = Transaction.openRoot()) {
            segment.extract(segment.getResource(0), segment.getAmountAsInt(0), transaction);
            transaction.commit();
        }
    }

    /**
     * The engine's type is answered by an override rather than Oritech's constructor (ADR-0077), so a
     * save that wrote Oritech's id would reload the engine as Oritech's class.
     */
    private static void reloadsAsThePacksEngine(GameTestHelper helper) {
        BlockPos anchor = new BlockPos(3, 1, 3);
        place(helper, anchor, Direction.NORTH);
        SteamEngineEntity engine = engine(helper, anchor);
        CompoundTag saved = engine.saveWithFullMetadata(helper.getLevel().registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(engine.getBlockPos(), engine.getBlockState(), saved,
                helper.getLevel().registryAccess());
        if (!(loaded instanceof SteamEngineBlockEntity)) {
            helper.fail("the saved Steam Engine reloaded as " + loaded, anchor);
        }
        helper.succeed();
    }

    /**
     * Keeps the row's steam segment full, so each port holds the tank at 70 % of its capacity:
     * Oritech's speed 7.
     */
    private static void topUp(GameTestHelper helper, BlockPos anchor) {
        // The Boiler's steam, not Oritech's: a test fed Oritech's own fluid passed while the pack's
        // steam made nothing in game.
        FluidResource steam = FluidResource.of(PFFluids.STEAM_SOURCE.get());
        ResourceHandler<FluidResource> segment = FluidPorts.segment(helper.getLevel(), helper.absolutePos(anchor));
        if (segment == null) {
            return;
        }
        long room = segment.getCapacityAsLong(0, steam) - segment.getAmountAsLong(0);
        if (room <= 0L) {
            return;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            segment.insert(steam, (int) room, transaction);
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
