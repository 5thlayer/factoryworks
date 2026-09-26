package com.planetaryfactory.core.gametest;

import com.planetaryfactory.core.PFBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Oritech's Large Energy Storage as the accumulator (#283, ADR-0062): the mixin's figures reach the
 * placed block, and a pole charges it at them. The figures are typed rather than read off
 * {@code AccumulatorSpec}, so the test cannot agree with the spec by construction.
 */
final class AccumulatorTests {

    private static final Identifier LARGE_STORAGE =
            Identifier.fromNamespaceAndPath("oritech", "large_storage");
    private static final BlockPos CREATIVE = new BlockPos(1, 1, 3);
    private static final BlockPos ACCUMULATOR = new BlockPos(4, 1, 3);

    /** Past one rescan interval (40) plus the tick the network is rebuilt on. */
    private static final int SETTLE = 45;
    private static final int WINDOW = 10;

    private AccumulatorTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("accumulator_holds_5_mj", 20, AccumulatorTests::holdsFiveMegajoules);
        tests.test("accumulator_charges_at_300_kw", 100, AccumulatorTests::chargesAtThreeHundredKw);
    }

    private static void holdsFiveMegajoules(GameTestHelper helper) {
        helper.setBlock(ACCUMULATOR, BuiltInRegistries.BLOCK.getValue(LARGE_STORAGE));
        helper.startSequence()
                .thenExecute(() -> {
                    long capacity = face(helper).getCapacityAsLong();
                    if (capacity != 50_000L) {
                        helper.fail("the accumulator holds " + capacity + " FE, not 50,000",
                                ACCUMULATOR);
                    }
                })
                .thenSucceed();
    }

    /** A creative pole offers without limit, so only the accumulator's own rate holds the charge. */
    private static void chargesAtThreeHundredKw(GameTestHelper helper) {
        helper.setBlock(CREATIVE, PFBlocks.CREATIVE_POLE.get());
        helper.setBlock(ACCUMULATOR, BuiltInRegistries.BLOCK.getValue(LARGE_STORAGE));
        long[] before = new long[1];
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> before[0] = face(helper).getAmountAsLong())
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    long charged = face(helper).getAmountAsLong() - before[0];
                    if (charged != 150L * WINDOW) {
                        helper.fail("the accumulator charged " + charged + " FE in " + WINDOW
                                + " ticks, not 150 FE/t", ACCUMULATOR);
                    }
                    long offered;
                    try (Transaction tx = Transaction.openRoot()) {
                        offered = face(helper).extract(Integer.MAX_VALUE, tx);
                    }
                    if (offered != 150L) {
                        helper.fail("the accumulator gives " + offered + " FE in a tick, not 150",
                                ACCUMULATOR);
                    }
                })
                .thenSucceed();
    }

    private static EnergyHandler face(GameTestHelper helper) {
        EnergyHandler handler = helper.getLevel().getCapability(Capabilities.Energy.BLOCK,
                helper.absolutePos(ACCUMULATOR), null);
        if (handler == null) {
            helper.fail("the accumulator has no energy face", ACCUMULATOR);
        }
        return handler;
    }
}
