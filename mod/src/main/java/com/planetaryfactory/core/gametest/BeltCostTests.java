package com.planetaryfactory.core.gametest;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import rearth.belts.BlockContent;
import rearth.belts.ItemContent;
import rearth.belts.blocks.ChuteBlockEntity;
import rearth.belts.model.BeltTier;

/**
 * A belt costs one belt item per block, and breaking either of its loaders pays that back, with
 * the belt's items, into the breaker's inventory (#346). Placing and breaking go through the belt
 * item's use and the player's game mode, the paths a player takes.
 *
 * <p>The cost is typed rather than read off the fork: a belt from the west face of one loader to
 * the east face of a loader four blocks on is five blocks long.
 */
final class BeltCostTests {

    private static final BlockPos SOURCE = new BlockPos(2, 1, 3);
    private static final BlockPos FROM = new BlockPos(3, 1, 3);
    private static final BlockPos TO = new BlockPos(7, 1, 3);
    private static final int COST = 5;

    // Nothing behind the far loader, so the belt backs up and holds items when it is broken.
    private static final int SUPPLY = 64;
    private static final int FILL_TICKS = 60;

    private BeltCostTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("placing_a_belt_charges_one_item_per_block", 20, BeltCostTests::chargesPerBlock);
        tests.test("a_belt_the_player_cannot_afford_is_refused", 20, BeltCostTests::refusesWhenShort);
        tests.test("breaking_a_belts_start_refunds_it_to_the_breaker", FILL_TICKS + 20,
                helper -> breakingRefunds(helper, FROM, TO));
        tests.test("breaking_a_belts_end_refunds_it_to_the_breaker", FILL_TICKS + 20,
                helper -> breakingRefunds(helper, TO, FROM));
    }

    private static void chargesPerBlock(GameTestHelper helper) {
        loaders(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ChuteBlockEntity belt = placeWithItem(helper, player, COST + 2);

        if (!helper.absolutePos(TO).equals(belt.getTarget())) {
            helper.fail("a player holding " + (COST + 2) + " belt items could not place a "
                    + COST + "-block belt", FROM);
        }
        int left = belts(player);
        if (left != 2) {
            helper.fail("placing a " + COST + "-block belt left " + left + " of " + (COST + 2)
                    + " belt items, expected 2", FROM);
        }
        helper.succeed();
    }

    private static void refusesWhenShort(GameTestHelper helper) {
        loaders(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ChuteBlockEntity belt = placeWithItem(helper, player, COST - 1);

        if (belt.getTarget() != null) {
            helper.fail("a " + COST + "-block belt was placed by a player holding " + (COST - 1)
                    + " belt items", FROM);
        }
        int left = belts(player);
        if (left != COST - 1) {
            helper.fail("a refused belt charged " + (COST - 1 - left) + " belt items", FROM);
        }
        helper.succeed();
    }

    private static void breakingRefunds(GameTestHelper helper, BlockPos broken, BlockPos other) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.getBlockEntity(SOURCE, ChestBlockEntity.class)
                .setItem(0, new ItemStack(Items.COBBLESTONE, SUPPLY));
        loaders(helper);
        ChuteBlockEntity belt = placeWithItem(helper, helper.makeMockPlayer(GameType.SURVIVAL), COST);

        // A player of its own: the shared fake player's inventory is every test's at once.
        ServerPlayer breaker = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "pf_belt_breaker"));
        breaker.setGameMode(GameType.SURVIVAL);

        helper.startSequence().thenIdle(FILL_TICKS).thenExecute(() -> {
            int onBelt = belt.getBeltEntries().size();
            if (onBelt == 0) {
                helper.fail("the belt carries nothing, so this proves nothing about its items", FROM);
            }
            breaker.gameMode.destroyBlock(helper.absolutePos(broken));

            int refunded = count(breaker, ItemContent.beltFor(BeltTier.BELT));
            if (refunded != COST) {
                helper.fail("breaking the loader refunded " + refunded + " belt items into the inventory, expected "
                        + COST, broken);
            }
            int carried = count(breaker, Items.COBBLESTONE);
            if (carried != onBelt) {
                helper.fail("breaking the loader put " + carried + " of the belt's " + onBelt
                        + " items into the inventory", broken);
            }
            int lying = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(2.0))
                    .stream()
                    .map(ItemEntity::getItem)
                    .filter(stack -> stack.is(Items.COBBLESTONE) || stack.is(ItemContent.beltFor(BeltTier.BELT)))
                    .mapToInt(ItemStack::getCount)
                    .sum();
            if (lying != 0) {
                helper.fail(lying + " of the belt's items or refund lie on the ground", broken);
            }
            if (helper.getBlockState(broken).is(BlockContent.CHUTE_BLOCK.get())) {
                helper.fail("the broken loader is still standing", broken);
            }
            if (!helper.getBlockState(other).is(BlockContent.CHUTE_BLOCK.get())) {
                helper.fail("breaking one loader removed the other", other);
            }
            ChuteBlockEntity survivor = helper.getBlockEntity(other, ChuteBlockEntity.class);
            if (survivor.isUsed()) {
                helper.fail("the surviving loader still reads as part of a belt", other);
            }
        }).thenSucceed();
    }

    /** An east-facing loader and a west-facing one, the belt's two ends, placed and unlinked. */
    private static void loaders(GameTestHelper helper) {
        helper.setBlock(FROM, BlockContent.CHUTE_BLOCK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        helper.setBlock(TO, BlockContent.CHUTE_BLOCK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
    }

    /** Clicks the start loader then the end one with a stack of tier-1 belt items. */
    private static ChuteBlockEntity placeWithItem(GameTestHelper helper, Player player, int held) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemContent.beltFor(BeltTier.BELT), held));
        click(helper, player, FROM);
        click(helper, player, TO);
        return helper.getBlockEntity(FROM, ChuteBlockEntity.class);
    }

    private static void click(GameTestHelper helper, Player player, BlockPos target) {
        BlockPos absolute = helper.absolutePos(target);
        helper.useBlock(target, player, new BlockHitResult(
                Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false));
    }

    private static int belts(Player player) {
        return count(player, ItemContent.beltFor(BeltTier.BELT));
    }

    private static int count(Player player, Item item) {
        return ContainerHelper.clearOrCountMatchingItems(player.getInventory(), stack -> stack.is(item), 0, true);
    }
}
