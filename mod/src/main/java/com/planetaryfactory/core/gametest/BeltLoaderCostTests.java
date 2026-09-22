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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import rearth.belts.BlockContent;
import rearth.belts.ItemContent;
import rearth.belts.model.BeltTier;

/**
 * A belt laid onto open ground takes each loader it places from the inventory: the belt's own
 * tier, else the nearest higher tier held, else the nearest lower one, and nothing is placed or
 * charged when a loader is missing (#354). An open end becomes a loader only against an
 * inventory, so each end here has a chest beyond it; with none, it becomes a support (#366).
 */
final class BeltLoaderCostTests {

    // Ground under the belt's two ends; the loaders stand one block up.
    private static final BlockPos FROM_GROUND = new BlockPos(3, 0, 3);
    private static final BlockPos TO_GROUND = new BlockPos(7, 0, 3);
    private static final BlockPos FROM = FROM_GROUND.above();
    private static final BlockPos TO = TO_GROUND.above();
    private static final BlockPos BEHIND_FROM = FROM.west();
    private static final BlockPos BEYOND_TO = TO.east();
    private static final int BELTS = 16;

    private BeltLoaderCostTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_belt_with_no_loader_held_is_refused", 20, helper -> refused(helper, BeltTier.BELT, Map.of()));
        tests.test("a_belt_with_one_loader_for_two_ends_is_refused", 20,
                helper -> refused(helper, BeltTier.BELT, Map.of(BeltTier.BELT, 1)));
        tests.test("a_belt_takes_loaders_of_its_own_tier", 20, BeltLoaderCostTests::ownTier);
        tests.test("a_belt_takes_a_higher_tier_loader_before_a_lower_one", 20, BeltLoaderCostTests::higherTier);
        tests.test("a_belt_falls_back_to_a_lower_tier_loader", 20, BeltLoaderCostTests::lowerTier);
        tests.test("a_belt_ending_at_a_placed_loader_takes_one", 20, BeltLoaderCostTests::oneEndPlaced);
        tests.test("a_belt_with_no_inventory_at_its_ends_ends_on_supports", 20, BeltLoaderCostTests::supportsWithoutInventory);
        tests.test("an_end_clicked_facing_its_chest_takes_a_loader", 20, BeltLoaderCostTests::endFacingChest);
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

    private static void higherTier(GameTestHelper helper) {
        Player player = player(helper, BeltTier.IMPROVED,
                Map.of(BeltTier.IMPROVED, 1, BeltTier.BELT, 1, BeltTier.EXPRESS, 1, BeltTier.TURBO, 1));
        lay(helper, player);

        expectLoader(helper, FROM, BeltTier.IMPROVED);
        expectLoader(helper, TO, BeltTier.EXPRESS);
        expectHeld(helper, player, BeltTier.BELT, 1);
        expectHeld(helper, player, BeltTier.TURBO, 1);
        helper.succeed();
    }

    private static void lowerTier(GameTestHelper helper) {
        Player player = player(helper, BeltTier.EXPRESS, Map.of(BeltTier.EXPRESS, 1, BeltTier.BELT, 1));
        lay(helper, player);

        expectLoader(helper, FROM, BeltTier.EXPRESS);
        expectLoader(helper, TO, BeltTier.BELT);
        expectHeld(helper, player, BeltTier.EXPRESS, 0);
        expectHeld(helper, player, BeltTier.BELT, 0);
        helper.succeed();
    }

    private static void supportsWithoutInventory(GameTestHelper helper) {
        Player player = player(helper, BeltTier.BELT, Map.of(BeltTier.BELT, 2));
        click(helper, player, FROM_GROUND);
        click(helper, player, TO_GROUND);

        for (BlockPos end : new BlockPos[] {FROM, TO}) {
            if (!helper.getBlockState(end).is(BlockContent.CONVEYOR_SUPPORT_BLOCK.get())) {
                helper.fail("an end with no inventory beyond it is " + helper.getBlockState(end).getBlock() + ", not a support", end);
            }
        }
        expectHeld(helper, player, BeltTier.BELT, 2);
        helper.succeed();
    }

    // Facing the chest, as the player does at the start, rather than back along the belt (#366).
    private static void endFacingChest(GameTestHelper helper) {
        helper.setBlock(BEHIND_FROM, Blocks.CHEST);
        helper.setBlock(BEYOND_TO, Blocks.CHEST);
        Player player = player(helper, BeltTier.BELT, Map.of(BeltTier.BELT, 2));
        click(helper, player, FROM_GROUND);
        player.setYRot(-90);
        click(helper, player, TO_GROUND);

        expectLoader(helper, FROM, BeltTier.BELT);
        expectLoader(helper, TO, BeltTier.BELT);
        Direction facing = helper.getBlockState(TO).getValue(HorizontalDirectionalBlock.FACING);
        if (facing != Direction.WEST) {
            helper.fail("the end loader faces " + facing + ", so its chest is not beyond it", TO);
        }
        helper.succeed();
    }

    private static void oneEndPlaced(GameTestHelper helper) {
        helper.setBlock(TO, BlockContent.loaderFor(BeltTier.BELT).defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
        helper.setBlock(BEHIND_FROM, Blocks.CHEST);
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
        helper.setBlock(BEHIND_FROM, Blocks.CHEST);
        helper.setBlock(BEYOND_TO, Blocks.CHEST);
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
