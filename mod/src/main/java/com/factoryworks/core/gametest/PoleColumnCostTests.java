package com.factoryworks.core.gametest;

import java.util.List;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.energy.PoleTier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A column is one pole however tall it is (ADR-0036): raising one costs nothing, and breaking one
 * pays back the single item it cost.
 *
 * <p>Neither half means anything without the other, which is why they are one file. Free extension
 * alone is a duplicator -- extend, break the top segment back off, repeat -- and lootless segments
 * alone would charge a pole per segment and hand nothing back.
 *
 * <p>A world is needed for both: the first is a held stack surviving a real use gesture, and the
 * second is a loot table asked during a break. A JVM test can reach neither.
 */
final class PoleColumnCostTests {

    /** Clear of the platform's edges and of the other tests' fixtures. */
    private static final BlockPos BASE = new BlockPos(3, 1, 3);

    /** Long enough for the drops to exist as entities, short enough to keep the test quick. */
    private static final int SETTLE = 5;

    private PoleColumnCostTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("raising_a_pole_consumes_no_item", 20, PoleColumnCostTests::raisingIsFree);
        tests.test("a_broken_column_pays_back_one_pole", 40, PoleColumnCostTests::columnPaysBackOne);
        tests.test("breaking_an_extension_drops_nothing", 40, PoleColumnCostTests::extensionDropsNothing);
    }

    private static void raisingIsFree(GameTestHelper helper) {
        helper.setBlock(BASE, PFBlocks.pole(PoleTier.SMALL).get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack held = new ItemStack(PFBlocks.pole(PoleTier.SMALL).get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);

        if (!helper.getBlockState(BASE).is(PFBlocks.pole(PoleTier.SMALL).get())) {
            helper.fail("the fixture pole is not standing", BASE);
        }
        use(helper, player, held, BASE);
        if (player.getMainHandItem().isEmpty()) {
            helper.fail("the mock player is not holding the pole", BASE);
        }

        if (!helper.getBlockState(BASE.above()).is(PFBlocks.pole(PoleTier.SMALL).get())) {
            helper.fail("clicking a pole with its own tier did not raise the column", BASE.above());
        }
        if (held.getCount() != 2) {
            helper.fail("raising a pole consumed " + (2 - held.getCount()) + " item(s)", BASE);
        }
        helper.succeed();
    }

    private static void columnPaysBackOne(GameTestHelper helper) {
        column(helper, 3);
        // Not the helper's own destroyBlock: it drops nothing, which is the very thing under test.
        helper.getLevel().destroyBlock(helper.absolutePos(BASE), true, null);
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> {
                    if (!helper.getBlockState(BASE.above()).is(Blocks.AIR)) {
                        helper.fail("breaking the base left a segment standing", BASE.above());
                    }
                    expectPoles(helper, 1, "breaking a three-tall column");
                })
                .thenSucceed();
    }

    private static void extensionDropsNothing(GameTestHelper helper) {
        column(helper, 3);
        helper.getLevel().destroyBlock(helper.absolutePos(BASE.above(2)), true, null);
        helper.startSequence()
                .thenIdle(SETTLE)
                .thenExecute(() -> expectPoles(helper, 0, "breaking a column's top segment"))
                .thenSucceed();
    }

    /** Every pole item lying in the test's area, however it got there. */
    private static void expectPoles(GameTestHelper helper, int expected, String what) {
        List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                helper.getBounds().inflate(2.0));
        int poles = 0;
        for (ItemEntity item : items) {
            if (item.getItem().is(PFBlocks.pole(PoleTier.SMALL).get().asItem())) {
                poles += item.getItem().getCount();
            }
        }
        if (poles != expected) {
            helper.fail(what + " dropped " + poles + " pole(s) where " + expected + " was expected", BASE);
        }
    }

    private static void column(GameTestHelper helper, int segments) {
        for (int i = 0; i < segments; i++) {
            helper.setBlock(BASE.above(i), PFBlocks.pole(PoleTier.SMALL).get());
        }
    }

    private static void use(GameTestHelper helper, Player player, ItemStack held, BlockPos target) {
        BlockPos absolute = helper.absolutePos(target);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(absolute).relative(Direction.NORTH, 0.5), Direction.NORTH, absolute, false);
        helper.useBlock(target, player, hit);
    }
}
