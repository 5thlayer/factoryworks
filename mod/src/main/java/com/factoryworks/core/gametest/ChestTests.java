package com.factoryworks.core.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/** No chest pairs into a double chest, vanilla's included (#542, ADR-0106). */
final class ChestTests {

    private static final BlockPos LEFT = new BlockPos(2, 1, 3);
    private static final BlockPos RIGHT = new BlockPos(3, 1, 3);

    private ChestTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("two_chests_placed_side_by_side_stay_single", 20, ChestTests::placedBesideEachOther);
        tests.test("a_chest_forced_to_pair_falls_back_to_single", 20, ChestTests::forcedPairFallsBack);
    }

    private static void placedBesideEachOther(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CHEST, 2));
        placeOnFloor(helper, player, LEFT);
        placeOnFloor(helper, player, RIGHT);

        for (BlockPos at : new BlockPos[] {LEFT, RIGHT}) {
            BlockState state = helper.getBlockState(at);
            if (!state.is(Blocks.CHEST)) {
                helper.fail("no chest was placed", at);
                return;
            }
            helper.assertValueEqual(state.getValue(ChestBlock.TYPE), ChestType.SINGLE, "chest type at " + at);
            ResourceHandler<ItemResource> items = helper.getLevel()
                    .getCapability(Capabilities.Item.BLOCK, helper.absolutePos(at), null);
            helper.assertValueEqual(items.size(), 27, "slots at " + at);
        }
        helper.assertValueEqual(helper.getBlockState(LEFT).getValue(ChestBlock.FACING),
                helper.getBlockState(RIGHT).getValue(ChestBlock.FACING), "facing of the pair");
        helper.succeed();
    }

    private static void forcedPairFallsBack(GameTestHelper helper) {
        BlockState chest = Blocks.CHEST.defaultBlockState();
        helper.setBlock(RIGHT, chest);
        helper.setBlock(LEFT, chest.setValue(ChestBlock.TYPE, ChestType.LEFT));

        helper.getLevel().neighborChanged(helper.absolutePos(LEFT), Blocks.CHEST, null);
        BlockState settled = helper.getLevel().getBlockState(helper.absolutePos(LEFT));
        BlockState updated = settled.updateShape(helper.getLevel(), helper.getLevel(), helper.absolutePos(LEFT),
                Direction.EAST, helper.absolutePos(RIGHT), helper.getLevel().getBlockState(helper.absolutePos(RIGHT)),
                helper.getLevel().getRandom());
        helper.assertValueEqual(updated.getValue(ChestBlock.TYPE), ChestType.SINGLE, "chest type after updateShape");
        helper.succeed();
    }

    private static void placeOnFloor(GameTestHelper helper, net.minecraft.world.entity.player.Player player,
            BlockPos at) {
        BlockPos floor = at.below();
        BlockPos absolute = helper.absolutePos(floor);
        helper.useBlock(floor, player, new BlockHitResult(
                Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false));
    }
}
