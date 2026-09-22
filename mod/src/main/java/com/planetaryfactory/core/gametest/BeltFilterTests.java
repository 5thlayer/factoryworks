package com.planetaryfactory.core.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import rearth.belts.BlockContent;
import rearth.belts.blocks.ChuteBlockEntity;
import rearth.belts.model.BeltTier;

/**
 * A loader's item filter, set by clicking it with the item, holds through the rest of the click.
 * A client that does not know the loader is in use goes on to try the empty off hand, which the
 * server used to read as a reset.
 */
final class BeltFilterTests {

    private static final BlockPos SOURCE = new BlockPos(2, 1, 3);
    private static final BlockPos FROM = new BlockPos(3, 1, 3);
    private static final BlockPos TO = new BlockPos(7, 1, 3);
    private static final BlockPos TARGET = new BlockPos(8, 1, 3);

    private static final int RUN_TICKS = 100;

    private BeltFilterTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_loaders_filter_survives_the_empty_off_hand", RUN_TICKS + 20,
                BeltFilterTests::filterSurvivesEmptyOffHand);
        tests.test("both_loaders_of_a_belt_read_as_used_to_the_client", 20,
                BeltFilterTests::bothEndsReadAsUsedToTheClient);
    }

    // Cobblestone sits in the first slot, so an unfiltered loader takes it first.
    private static void filterSurvivesEmptyOffHand(GameTestHelper helper) {
        placeBelt(helper);
        Container source = chest(helper, SOURCE);
        source.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        source.setItem(1, new ItemStack(Items.DIRT, 32));

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT));
        helper.useBlock(FROM, player, hit(helper, FROM));
        helper.getBlockState(FROM).useItemOn(ItemStack.EMPTY, helper.getLevel(), player,
                InteractionHand.OFF_HAND, hit(helper, FROM));

        helper.startSequence().thenIdle(RUN_TICKS).thenExecute(() -> {
            int dirt = count(chest(helper, TARGET), Items.DIRT);
            int cobblestone = count(chest(helper, TARGET), Items.COBBLESTONE);
            if (dirt == 0) {
                helper.fail("no dirt arrived, so this proves nothing about the filter", TARGET);
            }
            if (cobblestone != 0) {
                helper.fail(cobblestone + " cobblestone passed a loader filtered to dirt", FROM);
            }
        }).thenSucceed();
    }

    // What a client knows of a loader is its update tag; a loader it thinks is free takes no click.
    private static void bothEndsReadAsUsedToTheClient(GameTestHelper helper) {
        placeBelt(helper);
        helper.runAfterDelay(2, () -> {
            for (BlockPos end : List.of(FROM, TO)) {
                if (!asTheClientSeesIt(helper, end).isUsed()) {
                    helper.fail("the client reads this loader as free while it is part of a belt", end);
                }
            }
            helper.succeed();
        });
    }

    private static ChuteBlockEntity asTheClientSeesIt(GameTestHelper helper, BlockPos pos) {
        ChuteBlockEntity server = helper.getBlockEntity(pos, ChuteBlockEntity.class);
        CompoundTag tag = server.getUpdateTag(helper.getLevel().registryAccess());
        tag.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(server.getType()).toString());
        BlockEntity copy = BlockEntity.loadStatic(server.getBlockPos(), server.getBlockState(), tag,
                helper.getLevel().registryAccess());
        return (ChuteBlockEntity) copy;
    }

    private static void placeBelt(GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(TARGET, Blocks.CHEST);
        helper.setBlock(FROM, BlockContent.CHUTE_BLOCK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        helper.setBlock(TO, BlockContent.CHUTE_BLOCK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.WEST));
        BeltHandoffTests.link(helper, FROM, TO, List.of(), BeltTier.BELT);
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        return new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP,
                absolute, false);
    }

    private static ChestBlockEntity chest(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, ChestBlockEntity.class);
    }

    private static int count(Container container, Item item) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
