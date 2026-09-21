package com.planetaryfactory.core.gametest;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import rearth.belts.BlockContent;
import rearth.belts.ItemContent;
import rearth.belts.model.BeltTier;

/**
 * A belt laid onto open ground takes each loader it places from the inventory: the belt's own
 * tier, else the highest lower tier held, never a higher one, and nothing is placed or charged
 * when a loader is missing (#354).
 */
final class BeltLoaderCostTests {

    // Ground under the belt's two ends; the loaders stand one block up.
    private static final BlockPos FROM_GROUND = new BlockPos(3, 0, 3);
    private static final BlockPos TO_GROUND = new BlockPos(7, 0, 3);
    private static final BlockPos FROM = FROM_GROUND.above();
    private static final BlockPos TO = TO_GROUND.above();
    private static final int BELTS = 16;

    private BeltLoaderCostTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_belt_with_no_loader_held_is_refused", 20, helper -> refused(helper, BeltTier.BELT, Map.of()));
        tests.test("a_belt_with_one_loader_for_two_ends_is_refused", 20,
                helper -> refused(helper, BeltTier.BELT, Map.of(BeltTier.BELT, 1)));
        tests.test("a_belt_never_takes_a_higher_tier_loader", 20,
                helper -> refused(helper, BeltTier.BELT, Map.of(BeltTier.IMPROVED, 2)));
        tests.test("a_belt_takes_loaders_of_its_own_tier", 20, BeltLoaderCostTests::ownTier);
        tests.test("a_belt_falls_back_to_a_lower_tier_loader", 20, BeltLoaderCostTests::lowerTier);
        tests.test("a_belt_ending_at_a_placed_loader_takes_one", 20, BeltLoaderCostTests::oneEndPlaced);
    }

    private static void refused(GameTestHelper helper, BeltTier belt, Map<BeltTier, Integer> held) {
        Player player = player(helper, belt, held);
        lay(helper, player);

        for (BlockPos end : new BlockPos[] {FROM, TO}) {
            if (!helper.getBlockState(end).isAir()) {
                helper.fail("a belt was refused yet " + helper.getBlockState(end).getBlock() + " stands at its end", end);
            }
        }
        if (count(player, ItemContent.beltFor(belt)) != BELTS) {
            helper.fail("a refused belt charged belt items", FROM);
        }
        for (BeltTier tier : BeltTier.values()) {
            int expected = held.getOrDefault(tier, 0);
            int left = count(player, loaderItem(tier));
            if (left != expected) {
                helper.fail("a refused belt took " + (expected - left) + " tier-" + tier.number() + " loaders", FROM);
            }
        }
        helper.succeed();
    }

    private static void ownTier(GameTestHelper helper) {
        Player player = player(helper, BeltTier.IMPROVED, Map.of(BeltTier.IMPROVED, 2, BeltTier.BELT, 2));
        lay(helper, player);

        expectLoader(helper, FROM, BeltTier.IMPROVED);
        expectLoader(helper, TO, BeltTier.IMPROVED);
        expectHeld(helper, player, BeltTier.IMPROVED, 0);
        expectHeld(helper, player, BeltTier.BELT, 2);
        helper.succeed();
    }

    private static void lowerTier(GameTestHelper helper) {
        Player player = player(helper, BeltTier.EXPRESS,
                Map.of(BeltTier.EXPRESS, 1, BeltTier.BELT, 1, BeltTier.TURBO, 5));
        lay(helper, player);

        expectLoader(helper, FROM, BeltTier.EXPRESS);
        expectLoader(helper, TO, BeltTier.BELT);
        expectHeld(helper, player, BeltTier.EXPRESS, 0);
        expectHeld(helper, player, BeltTier.BELT, 0);
        expectHeld(helper, player, BeltTier.TURBO, 5);
        helper.succeed();
    }

    private static void oneEndPlaced(GameTestHelper helper) {
        helper.setBlock(TO, BlockContent.loaderFor(BeltTier.BELT).defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
        Player player = player(helper, BeltTier.BELT, Map.of(BeltTier.BELT, 1));
        click(helper, player, FROM_GROUND);
        click(helper, player, TO);

        expectLoader(helper, FROM, BeltTier.BELT);
        expectHeld(helper, player, BeltTier.BELT, 0);
        helper.succeed();
    }

    /** A survival player facing west, so the start loader faces east along the belt. */
    private static Player player(GameTestHelper helper, BeltTier belt, Map<BeltTier, Integer> loaders) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setYRot(90);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemContent.beltFor(belt), BELTS));
        loaders.forEach((tier, count) -> player.getInventory().add(new ItemStack(loaderItem(tier), count)));
        return player;
    }

    private static void lay(GameTestHelper helper, Player player) {
        click(helper, player, FROM_GROUND);
        click(helper, player, TO_GROUND);
    }

    private static void click(GameTestHelper helper, Player player, BlockPos target) {
        BlockPos absolute = helper.absolutePos(target);
        helper.useBlock(target, player, new BlockHitResult(
                Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false));
    }

    private static void expectLoader(GameTestHelper helper, BlockPos pos, BeltTier tier) {
        Block found = helper.getBlockState(pos).getBlock();
        if (found != BlockContent.loaderFor(tier)) {
            helper.fail("expected a tier-" + tier.number() + " loader, found " + found, pos);
        }
    }

    private static void expectHeld(GameTestHelper helper, Player player, BeltTier tier, int expected) {
        int left = count(player, loaderItem(tier));
        if (left != expected) {
            helper.fail("the player holds " + left + " tier-" + tier.number() + " loaders, expected " + expected, FROM);
        }
    }

    private static Item loaderItem(BeltTier tier) {
        return BlockContent.loaderFor(tier).asItem();
    }

    private static int count(Player player, Item item) {
        return ContainerHelper.clearOrCountMatchingItems(player.getInventory(), stack -> stack.is(item), 0, true);
    }
}
