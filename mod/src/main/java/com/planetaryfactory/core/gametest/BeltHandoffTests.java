package com.planetaryfactory.core.gametest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import rearth.belts.BlockContent;
import rearth.belts.blocks.ChuteBlockEntity;

/**
 * The pack's SimpleBelts fork loaded and moving items: chest, loader, belt, loader, chest (#342).
 *
 * <p>A loader pulls from the inventory behind its facing and pushes into the one behind the far
 * loader's, so the two face each other along the row.
 */
final class BeltHandoffTests {

    private static final BlockPos SOURCE = new BlockPos(2, 1, 3);
    private static final BlockPos FROM = new BlockPos(3, 1, 3);
    private static final BlockPos TO = new BlockPos(7, 1, 3);
    private static final BlockPos TARGET = new BlockPos(8, 1, 3);

    private static final int ITEMS = 16;

    private static final int BELT_TIER = 1;

    private BeltHandoffTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("belt_hands_items_from_chest_to_chest", 400, BeltHandoffTests::handsOff);
    }

    private static void handsOff(GameTestHelper helper) {
        helper.setBlock(SOURCE, Blocks.CHEST);
        helper.setBlock(TARGET, Blocks.CHEST);
        helper.setBlock(FROM, loader(Direction.EAST));
        helper.setBlock(TO, loader(Direction.WEST));
        chest(helper, SOURCE).setItem(0, new ItemStack(Items.COBBLESTONE, ITEMS));

        helper.getBlockEntity(FROM, ChuteBlockEntity.class)
                .assignFromBeltItem(helper.absolutePos(TO), List.of(), BELT_TIER);

        helper.succeedWhen(() -> {
            int arrived = count(chest(helper, TARGET));
            if (arrived != ITEMS) {
                helper.fail("target chest holds " + arrived + " of " + ITEMS + " cobblestone",
                        TARGET);
            }
            int left = count(chest(helper, SOURCE));
            if (left != 0) {
                helper.fail("source chest still holds " + left + " cobblestone", SOURCE);
            }
        });
    }

    private static BlockState loader(Direction facing) {
        return BlockContent.CHUTE_BLOCK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static ChestBlockEntity chest(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, ChestBlockEntity.class);
    }

    private static int count(Container container) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            total += container.getItem(slot).getCount();
        }
        return total;
    }
}
