package com.factoryworks.core.gametest;

import io.github._5thlayer.pipeworks.api.FluidPorts;
import io.github._5thlayer.wireworks.EnergyOwner;
import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.fluid.PFFluids;
import com.factoryworks.core.smelting.FurnaceBlockEntity;
import com.factoryworks.core.smelting.FurnaceTier;

import com.factoryworks.core.fluid.SteamEngineBlockEntity;

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
import rearth.oritech.util.Geometry;

/**
 * The Pack's claim: its Steam Engine, which the Pack owns, is a generator a Wireworks pole draws
 * through its parts alone, once, and a Pipeworks port that draws steam from its segment and from
 * nothing else (#292, #352, #627, ADR-0062, ADR-0116). Both Libraries' documented API is all it
 * reads: {@link EnergyOwner} for the parts and {@link FluidPorts} for the segment.
 *
 * <p>{@code SupplyScanTest} holds the rule -- every block stands for its energy owner, kept once.
 * What it cannot see is that the owner is the right one: that a part's face really is its anchor's.
 *
 * <h2>The layout</h2>
 *
 * <p>A row of three engines A, B, C, whose ports touch and so share one steam segment, which the test
 * keeps full. Each draws its own rate from it.
 *
 * <p>A small pole stands beside C on its parts' side, far enough out that its 5x5 area holds C's
 * parts and nothing else of the row, not even C's anchor. An Electric Furnace beside the pole is the
 * one consumer, emptied every tick so it always asks for more than C makes. The pole must draw all of
 * C's 450 FE a tick through a part, and once.
 *
 * <p>The row tests drain each engine directly instead: fed, each makes its own 450 FE a tick; starved,
 * they share what arrives and none passes its rate. The arithmetic is {@code SteamEngineSpecTest}'s;
 * these hold that touching anchors really are one segment each engine draws from.
 */
final class SteamEngineNetworkTests {

    /** One engine's 450 FE/t (ADR-0060: 900 kW at 100 J per FE). */
    private static final long ENGINE_FE_PER_TICK = 450L;

    private static final BlockPos ORIGIN = new BlockPos(0, 1, 3);

    /** The part at {@code (0,0,-1)}: beside the anchor, on the ground, off the row's axis. */
    private static final int LATERAL_PART = 2;

    /** The tick the network is rebuilt on, and a few more for the buffers to fill. */
    private static final int SETTLE = 10;

    /** Ticks the draw is read over once it has settled. */
    private static final int MEASURED = 20;

    /** Supply to the segment each tick of a starved row: under the 4.5 mB three engines burn. */
    private static final int STARVED_SUPPLY = 2;

    /** FE a millibucket of steam is worth: 450 FE/t over 1.5 mB/t. */
    private static final long FE_PER_MB = 300L;

    private SteamEngineNetworkTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("pole_draws_steam_engine_through_a_part", 400,
                SteamEngineNetworkTests::poleDrawsEngineThroughAPart);
        tests.test("steam_engine_keeps_its_charge_across_a_reload", 60,
                SteamEngineNetworkTests::keepsItsCharge);
        tests.test("steam_engine_has_no_fluid_face_and_draws_steam_alone", 60,
                SteamEngineNetworkTests::drawsSteamAlone);
        tests.test("steam_engine_row_on_a_fed_segment_makes_its_rate_per_engine", 100,
                SteamEngineNetworkTests::fedRow);
        tests.test("steam_engine_row_on_a_starved_segment_splits_it", 100,
                SteamEngineNetworkTests::starvedRow);
    }

    private record Layout(Direction facing, BlockPos a, BlockPos b, BlockPos c, BlockPos pole,
                          BlockPos furnace) {
    }

    private static void poleDrawsEngineThroughAPart(GameTestHelper helper) {
        Layout at = layout(helper);
        for (BlockPos engine : new BlockPos[] {at.a(), at.b(), at.c()}) {
            place(helper, engine, at.facing());
        }
        helper.setBlock(at.pole(), LibraryBlocks.smallPole());
        helper.setBlock(at.furnace(), PFBlocks.furnace(FurnaceTier.ELECTRIC).get());

        long[] delivered = new long[MEASURED];
        int[] tick = {0};
        helper.onEachTick(() -> {
            topUp(helper, at.a());
            var data = furnace(helper, at.furnace()).data();
            if (tick[0] >= SETTLE && tick[0] < SETTLE + MEASURED) {
                delivered[tick[0] - SETTLE] = data.get(FurnaceBlockEntity.DATA_ENERGY);
            }
            data.set(FurnaceBlockEntity.DATA_ENERGY, 0);
            tick[0]++;
        });
        helper.startSequence()
                .thenIdle(SETTLE + MEASURED + 1)
                .thenExecute(() -> {
                    BlockPos anchor = helper.absolutePos(at.c());
                    for (BlockPos block : PFBlocks.STEAM_ENGINE_FOOTPRINT.positions(anchor, at.facing())) {
                        if (!block.equals(anchor) && !anchor.equals(EnergyOwner.of(helper.getLevel(), block))) {
                            helper.fail("a part of the engine answers for "
                                    + EnergyOwner.of(helper.getLevel(), block) + ", not its anchor", at.c());
                        }
                    }
                    for (int i = 0; i < MEASURED; i++) {
                        if (delivered[i] != ENGINE_FE_PER_TICK) {
                            helper.fail("the furnace received " + delivered[i] + " FE on tick " + i
                                    + " of the window from one engine making " + ENGINE_FE_PER_TICK
                                    + " a tick, so the pole drew it through a part wrongly or twice", at.furnace());
                        }
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

    /** The world offset of part {@code index} of an engine facing {@code facing}. */
    private static BlockPos partOffset(Direction facing, int index) {
        return PFBlocks.STEAM_ENGINE_FOOTPRINT.positions(BlockPos.ZERO, facing).get(index);
    }

    private static void place(GameTestHelper helper, BlockPos anchor, Direction facing) {
        PFBlocks.STEAM_ENGINE_FOOTPRINT.placeAll(helper.getLevel(), helper.absolutePos(anchor), facing);
    }

    /**
     * The engine answers no fluid capability on any block; only its anchor stands in a segment. A
     * segment of water makes no power, and a segment of steam does.
     */
    private static void drawsSteamAlone(GameTestHelper helper) {
        BlockPos anchor = new BlockPos(3, 1, 3);
        place(helper, anchor, Direction.NORTH);
        BlockPos absolute = helper.absolutePos(anchor);
        List<BlockPos> blocks = PFBlocks.STEAM_ENGINE_FOOTPRINT.positions(absolute, Direction.NORTH);
        FluidResource steam = FluidResource.of(PFFluids.STEAM_SOURCE.get());
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
                    long held = engine(helper, anchor).energyHandler().getAmountAsLong();
                    if (held != 0L) {
                        helper.fail("the engine made " + held + " FE from a segment of water", anchor);
                    }
                    drain(helper, anchor);
                    insert(helper, anchor, steam, 200);
                })
                .thenIdle(10)
                .thenExecute(() -> {
                    long held = engine(helper, anchor).energyHandler().getAmountAsLong();
                    if (held != ENGINE_FE_PER_TICK) {
                        helper.fail("the engine holds " + held + " FE, not a full buffer of "
                                + ENGINE_FE_PER_TICK, anchor);
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

    /** The charge and the owed fractions are saved, so a reload neither loses nor invents power. */
    private static void keepsItsCharge(GameTestHelper helper) {
        BlockPos anchor = new BlockPos(3, 1, 3);
        place(helper, anchor, Direction.NORTH);
        helper.onEachTick(() -> topUp(helper, anchor));
        helper.startSequence()
                .thenIdle(5)
                .thenExecute(() -> {
                    SteamEngineBlockEntity engine = engine(helper, anchor);
                    CompoundTag saved = engine.saveWithFullMetadata(helper.getLevel().registryAccess());
                    BlockEntity loaded = BlockEntity.loadStatic(engine.getBlockPos(), engine.getBlockState(),
                            saved, helper.getLevel().registryAccess());
                    if (!(loaded instanceof SteamEngineBlockEntity reloaded)) {
                        helper.fail("the saved Steam Engine reloaded as " + loaded, anchor);
                        return;
                    }
                    long held = engine.energyHandler().getAmountAsLong();
                    long kept = reloaded.energyHandler().getAmountAsLong();
                    if (held <= 0L) {
                        helper.fail("the engine held no charge to save", anchor);
                    }
                    if (kept != held) {
                        helper.fail("the engine saved " + held + " FE and reloaded " + kept, anchor);
                    }
                    CompoundTag again = reloaded.saveWithFullMetadata(helper.getLevel().registryAccess());
                    for (String key : new String[] {"CarrySteam", "CarryEnergy"}) {
                        if (!saved.contains(key)) {
                            helper.fail("the engine saved no " + key, anchor);
                        }
                        double before = saved.getDoubleOr(key, Double.NaN);
                        double after = again.getDoubleOr(key, Double.NaN);
                        if (before != after) {
                            helper.fail("the engine saved " + key + " " + before + " and reloaded " + after, anchor);
                        }
                    }
                })
                .thenSucceed();
    }

    /** Each engine of a row on one fed segment makes its own 450 FE/t, drained by the test every tick. */
    private static void fedRow(GameTestHelper helper) {
        Layout at = layout(helper);
        BlockPos[] row = {at.a(), at.b(), at.c()};
        for (BlockPos engine : row) {
            place(helper, engine, at.facing());
        }
        long[] drawn = drawEachTick(helper, row, () -> topUp(helper, at.a()));
        helper.startSequence()
                .thenIdle(SETTLE + MEASURED + 1)
                .thenExecute(() -> {
                    if (!sharesOneSegment(helper, row)) {
                        helper.fail("the row's anchors are not one steam segment", at.a());
                    }
                    for (int i = 0; i < row.length; i++) {
                        if (drawn[i] != ENGINE_FE_PER_TICK * MEASURED) {
                            helper.fail("engine " + i + " made " + drawn[i] + " FE over " + MEASURED
                                    + " ticks, not " + ENGINE_FE_PER_TICK * MEASURED, row[i]);
                        }
                    }
                })
                .thenSucceed();
    }

    /**
     * A row on a segment fed less than it burns shares what arrives: no engine passes its rate, and
     * together they make what the steam is worth, give or take a millibucket and a buffer each.
     */
    private static void starvedRow(GameTestHelper helper) {
        Layout at = layout(helper);
        BlockPos[] row = {at.a(), at.b(), at.c()};
        for (BlockPos engine : row) {
            place(helper, engine, at.facing());
        }
        FluidResource steam = FluidResource.of(PFFluids.STEAM_SOURCE.get());
        long[] drawn = drawEachTick(helper, row, () -> {
            if (FluidPorts.segment(helper.getLevel(), helper.absolutePos(at.a())) != null) {
                insert(helper, at.a(), steam, STARVED_SUPPLY);
            }
        });
        helper.startSequence()
                .thenIdle(SETTLE + MEASURED + 1)
                .thenExecute(() -> {
                    long total = 0L;
                    for (int i = 0; i < row.length; i++) {
                        if (drawn[i] > ENGINE_FE_PER_TICK * MEASURED) {
                            helper.fail("engine " + i + " made " + drawn[i] + " FE over " + MEASURED
                                    + " ticks, past its " + ENGINE_FE_PER_TICK * MEASURED, row[i]);
                        }
                        total += drawn[i];
                    }
                    long worth = STARVED_SUPPLY * FE_PER_MB * MEASURED;
                    long slack = row.length * (ENGINE_FE_PER_TICK + FE_PER_MB);
                    if (Math.abs(total - worth) > slack) {
                        helper.fail("the row made " + total + " FE over " + MEASURED + " ticks from steam worth "
                                + worth, at.a());
                    }
                })
                .thenSucceed();
    }

    /**
     * Runs {@code feed} and empties every engine's buffer each tick; per engine, the FE taken over the
     * {@link #MEASURED} ticks after {@link #SETTLE}.
     */
    private static long[] drawEachTick(GameTestHelper helper, BlockPos[] row, Runnable feed) {
        long[] drawn = new long[row.length];
        int[] tick = {0};
        helper.onEachTick(() -> {
            feed.run();
            boolean measured = tick[0] >= SETTLE && tick[0] < SETTLE + MEASURED;
            for (int i = 0; i < row.length; i++) {
                try (Transaction transaction = Transaction.openRoot()) {
                    int taken = engine(helper, row[i]).energyHandler().extract(Integer.MAX_VALUE, transaction);
                    transaction.commit();
                    if (measured) {
                        drawn[i] += taken;
                    }
                }
            }
            tick[0]++;
        });
        return drawn;
    }

    /** Every anchor's segment holds the whole row's ports, so the row is one segment. */
    private static boolean sharesOneSegment(GameTestHelper helper, BlockPos[] row) {
        FluidResource steam = FluidResource.of(PFFluids.STEAM_SOURCE.get());
        long whole = (long) row.length * SteamEngineBlockEntity.spec().portCapacity();
        for (BlockPos engine : row) {
            ResourceHandler<FluidResource> segment = FluidPorts.segment(helper.getLevel(), helper.absolutePos(engine));
            if (segment == null || segment.getCapacityAsLong(0, steam) != whole) {
                return false;
            }
        }
        return true;
    }

    /** Keeps the row's steam segment full. */
    private static void topUp(GameTestHelper helper, BlockPos anchor) {
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

    private static SteamEngineBlockEntity engine(GameTestHelper helper, BlockPos relative) {
        return helper.getBlockEntity(relative, SteamEngineBlockEntity.class);
    }

    private static FurnaceBlockEntity furnace(GameTestHelper helper, BlockPos relative) {
        return helper.getBlockEntity(relative, FurnaceBlockEntity.class);
    }
}
