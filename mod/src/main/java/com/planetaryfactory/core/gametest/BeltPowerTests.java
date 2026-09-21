package com.planetaryfactory.core.gametest;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.energy.PoleTier;
import com.planetaryfactory.core.energy.SupplyAreaPoleBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import rearth.belts.BlockContent;
import rearth.belts.model.BeltTier;

/**
 * Loaders of tiers 2 to 4 pay FE for every item they move and tier 1 pays nothing (#348). The
 * loaders' face is the fork's, so the pack's source-text checks cannot read it; these tests are
 * where it is held: that a pole finds it, that its demand probe leaves nothing, and that it charges.
 *
 * <p>The figures are typed rather than read off the fork; {@code tests/factorio/test_logistics_extract.py}
 * derives them from the inserter prototypes.
 */
final class BeltPowerTests {

    private static final BlockPos SOURCE = new BlockPos(2, 1, 3);
    private static final BlockPos FROM = new BlockPos(3, 1, 3);
    private static final BlockPos TO = new BlockPos(7, 1, 3);
    private static final BlockPos TARGET = new BlockPos(8, 1, 3);
    // Off the belt's line, with both loaders in a small pole's 5x5 area.
    private static final BlockPos POLE = new BlockPos(5, 1, 4);

    private static final int SUPPLY = 256;

    private static final int TIER_2_JOULES_PER_ITEM = 6650;
    private static final int TIER_2_DRAIN_WATTS = 400;
    private static final int TIER_2_LOADER_BUFFER_FE = 200;
    private static final int JOULES_PER_FE = 100;

    private static final int UNPOWERED_TICKS = 60;
    private static final int FED_WARMUP_TICKS = 40;
    // One tick: the buffer holds only the largest tick the loader's flow limit allows.
    private static final int DRAW_WINDOW_TICKS = 1;

    private BeltPowerTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("tier_1_loader_moves_items_with_no_power", UNPOWERED_TICKS + 20,
                BeltPowerTests::tierOneRunsUnpowered);
        tests.test("tier_2_loader_with_no_fe_moves_nothing", UNPOWERED_TICKS + 20,
                BeltPowerTests::tierTwoStallsUnpowered);
        tests.test("loader_demand_probe_leaves_nothing_behind", 100, BeltPowerTests::probeLeavesNothing);
        tests.test("pole_fed_tier_2_loader_draws_66_5_fe_per_item",
                FED_WARMUP_TICKS + DRAW_WINDOW_TICKS + 20, BeltPowerTests::fedLoaderDrawsPerItem);
    }

    // The control for the stall: the same line with the tier changed moves items.
    private static void tierOneRunsUnpowered(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.CHEST);
        BeltHandoffTests.placeBelt(helper, SOURCE, FROM, TO, SUPPLY, BeltTier.BELT, BeltTier.BELT);
        helper.startSequence().thenIdle(UNPOWERED_TICKS).thenExecute(() -> {
            // No face, so a pole does not count an unpowered loader as a machine.
            if (helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(FROM), null) != null) {
                helper.fail("a tier-1 loader has an energy face", FROM);
            }
            int arrived = BeltHandoffTests.count(BeltHandoffTests.chest(helper, TARGET));
            if (arrived == 0) {
                helper.fail("tier-1 loaders with no pole delivered nothing in " + UNPOWERED_TICKS
                        + " ticks", TARGET);
            }
        }).thenSucceed();
    }

    private static void tierTwoStallsUnpowered(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.CHEST);
        var belt = BeltHandoffTests.placeBelt(helper, SOURCE, FROM, TO, SUPPLY, BeltTier.IMPROVED,
                BeltTier.IMPROVED);
        helper.startSequence().thenIdle(UNPOWERED_TICKS).thenExecute(() -> {
            int left = BeltHandoffTests.count(BeltHandoffTests.chest(helper, SOURCE));
            if (left != SUPPLY || !belt.getBeltEntries().isEmpty()) {
                helper.fail("a tier-2 loader with no FE took " + (SUPPLY - left) + " items and holds "
                        + belt.getBeltEntries().size() + " on its belt", FROM);
            }
        }).thenSucceed();
    }

    // A small pole with no generator still probes: the loader must hold nothing after it.
    private static void probeLeavesNothing(GameTestHelper helper) {
        helper.setBlock(POLE, PFBlocks.pole(PoleTier.SMALL).get());
        helper.setBlock(FROM, BlockContent.loaderFor(BeltTier.IMPROVED).defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        helper.startSequence().thenIdle(45).thenExecute(() -> {
            long stored = face(helper, FROM).getAmountAsLong();
            if (stored != 0) {
                helper.fail("the loader kept " + stored + " FE from the demand probe", FROM);
            }
            long demanded = helper.getBlockEntity(POLE, SupplyAreaPoleBlockEntity.class).demandedFePerTick();
            if (demanded != TIER_2_LOADER_BUFFER_FE) {
                helper.fail("the pole read a demand of " + demanded + " FE/t, expected "
                        + TIER_2_LOADER_BUFFER_FE, POLE);
            }
        }).thenSucceed();
    }

    // Measured with the pole cut off and the buffer filled by hand: fed, the refill's order against
    // the loader's tick decides what a read sees.
    private static void fedLoaderDrawsPerItem(GameTestHelper helper) {
        helper.setBlock(POLE, PFBlocks.CREATIVE_POLE.get());
        helper.setBlock(TARGET, Blocks.CHEST);
        var loader = BeltHandoffTests.placeBelt(helper, SOURCE, FROM, TO, SUPPLY, BeltTier.IMPROVED,
                BeltTier.IMPROVED);
        long[] before = new long[2];
        helper.startSequence()
                .thenIdle(FED_WARMUP_TICKS)
                .thenExecute(() -> {
                    if (BeltHandoffTests.count(BeltHandoffTests.chest(helper, TARGET)) == 0) {
                        helper.fail("pole-fed tier-2 loaders delivered nothing", TARGET);
                    }
                    helper.setBlock(POLE, PFBlocks.pole(PoleTier.SMALL).get());
                    loader.getEnergy().insertFe(Long.MAX_VALUE);
                    before[0] = face(helper, FROM).getAmountAsLong();
                    before[1] = BeltHandoffTests.count(BeltHandoffTests.chest(helper, SOURCE));
                })
                .thenIdle(DRAW_WINDOW_TICKS)
                .thenExecute(() -> {
                    long items = before[1] - BeltHandoffTests.count(BeltHandoffTests.chest(helper, SOURCE));
                    if (items == 0) {
                        helper.fail("the loader moved nothing in the window, so this proves nothing", FROM);
                    }
                    long drawn = before[0] - face(helper, FROM).getAmountAsLong();
                    long joules = items * TIER_2_JOULES_PER_ITEM
                            + (long) DRAW_WINDOW_TICKS * TIER_2_DRAIN_WATTS / 20;
                    // The face reads whole FE, so the window's far end rounds down by under one.
                    if (Math.abs(drawn * JOULES_PER_FE - joules) >= JOULES_PER_FE) {
                        helper.fail("the loader drew " + drawn + " FE for " + items + " items in "
                                + DRAW_WINDOW_TICKS + " ticks, expected " + joules / (double) JOULES_PER_FE,
                                FROM);
                    }
                })
                .thenSucceed();
    }

    private static EnergyHandler face(GameTestHelper helper, BlockPos pos) {
        var handler = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(pos), null);
        if (handler == null) {
            helper.fail("the loader has no energy face", pos);
        }
        return handler;
    }
}
