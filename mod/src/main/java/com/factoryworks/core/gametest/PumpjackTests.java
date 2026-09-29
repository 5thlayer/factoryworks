package com.factoryworks.core.gametest;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.PFItems;
import com.factoryworks.core.oil.OilWellBlockEntity;
import com.factoryworks.core.oil.PumpjackBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * A pole-fed Pumpjack on a 100% well pumps 10 mB of crude a second and takes 10 off the well each
 * cycle, and a pipe at any of its faces drains it; a starved one does neither (ADR-0081). The figures
 * are typed: a cycle is 900 FE at 45 FE/t, 20 ticks.
 */
final class PumpjackTests {

    private static final BlockPos WELL = new BlockPos(5, 0, 3);
    /** Two blocks past the footprint's edge, where a pole's area reaches its nearest parts. */
    private static final BlockPos POLE = new BlockPos(8, 1, 3);
    /** A part on the top layer's far corner, which no pole touches. */
    private static final BlockPos FAR_PART = new BlockPos(4, 3, 2);

    private static final long FULL_YIELD = 300_000;
    private static final int TICKS = 205;
    private static final int CRUDE_PER_CYCLE = 10;
    private static final long DEPLETION = 10;

    private PumpjackTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("fed_pumpjack_pumps_ten_a_second_and_depletes_its_well", 300, PumpjackTests::fedPumpjackPumps);
        tests.test("starved_pumpjack_pumps_nothing", 300, PumpjackTests::starvedPumpjackPumpsNothing);
    }

    private static void fedPumpjackPumps(GameTestHelper helper) {
        place(helper);
        helper.setBlock(POLE, PFBlocks.CREATIVE_POLE.get());
        helper.startSequence()
                .thenIdle(TICKS)
                .thenExecute(() -> {
                    int crude = pumpjack(helper).crude();
                    long taken = FULL_YIELD - well(helper).amount();
                    long cycles = taken / DEPLETION;
                    // The first cycle yields 10 and takes the well under 100%, so each after yields a hair under.
                    if (taken % DEPLETION != 0 || cycles < TICKS / 20 - 1 || cycles > TICKS / 20
                            || crude > CRUDE_PER_CYCLE * cycles || crude < CRUDE_PER_CYCLE * cycles - 1) {
                        helper.fail("after " + TICKS + " ticks the well gave up " + taken + " and the tank holds "
                                + crude + " mB, not 10 a second", WELL.above());
                        return;
                    }
                    var face = helper.getLevel().getCapability(Capabilities.Fluid.BLOCK,
                            helper.absolutePos(FAR_PART), Direction.UP);
                    if (face == null) {
                        helper.fail("no fluid face on a top-layer part", FAR_PART);
                        return;
                    }
                    try (Transaction tx = Transaction.openRoot()) {
                        int drained = face.extract(FluidResource.of(BuiltInRegistries.FLUID.getValue(
                                Identifier.parse(PumpjackBlockEntity.spec().fluid()))), crude, tx);
                        if (drained != crude) {
                            helper.fail("a pipe at a top-layer part took " + drained + " of " + crude + " mB", FAR_PART);
                        }
                    }
                })
                .thenSucceed();
    }

    private static void starvedPumpjackPumpsNothing(GameTestHelper helper) {
        place(helper);
        helper.startSequence()
                .thenIdle(TICKS)
                .thenExecute(() -> {
                    PumpjackBlockEntity pumpjack = pumpjack(helper);
                    if (pumpjack.crude() != 0 || pumpjack.progress() != 0 || well(helper).amount() != FULL_YIELD) {
                        helper.fail("a Pumpjack with no pole pumped " + pumpjack.crude() + " mB, banked "
                                + pumpjack.progress() + " FE and left the well at " + well(helper).amount(), WELL);
                    }
                })
                .thenSucceed();
    }

    /** A full-yield well, and the Pumpjack placed on it with its own item, the way a player does. */
    private static void place(GameTestHelper helper) {
        helper.setBlock(WELL, PFBlocks.OIL_WELL.get());
        well(helper).start(FULL_YIELD);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PFItems.PUMPJACK.get()));
        BlockPos absolute = helper.absolutePos(WELL);
        helper.useBlock(WELL, player, new BlockHitResult(
                Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false));
    }

    private static OilWellBlockEntity well(GameTestHelper helper) {
        return (OilWellBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(WELL));
    }

    private static PumpjackBlockEntity pumpjack(GameTestHelper helper) {
        return (PumpjackBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(WELL.above()));
    }
}
