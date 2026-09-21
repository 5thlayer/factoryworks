package com.planetaryfactory.core.gametest;

import java.util.List;

import com.planetaryfactory.core.PFItems;
import com.planetaryfactory.core.mining.rig.RigBlock;
import com.planetaryfactory.core.mining.rig.RigPartBlock;
import com.planetaryfactory.core.mining.rig.RigTier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * A rig broken by any of its blocks leaves nothing standing and pays exactly one drill item
 * (ADR-0043, #310). The break goes through the player's game mode, the path a mined block takes.
 */
final class RigBreakTests {

    private static final BlockPos FLOOR = new BlockPos(3, 0, 3);
    private static final Identifier PICK = Identifier.fromNamespaceAndPath("planetaryfactory",
            "engineers_iron_pick");

    private RigBreakTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        for (RigTier tier : RigTier.values()) {
            String name = tier.serializedName();
            tests.test("rig_" + name + "_broken_at_its_anchor_leaves_nothing", 20,
                    helper -> breakAndCheck(helper, tier, true));
            tests.test("rig_" + name + "_broken_at_a_part_leaves_nothing", 20,
                    helper -> breakAndCheck(helper, tier, false));
        }
    }

    private static void breakAndCheck(GameTestHelper helper, RigTier tier, boolean atAnchor) {
        List<BlockPos> footprint = place(helper, tier);
        BlockPos target = footprint.stream()
                .filter(pos -> (helper.getLevel().getBlockState(pos).getBlock() instanceof RigBlock)
                        == atAnchor)
                .findFirst()
                .orElseThrow();

        // Not makeMockServerPlayerInLevel: joining the level fires KubeJS's login sync, which
        // refuses the mock connection.
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        Item pick = BuiltInRegistries.ITEM.getValue(PICK);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(pick));
        player.gameMode.destroyBlock(target);

        for (BlockPos pos : footprint) {
            var block = helper.getLevel().getBlockState(pos).getBlock();
            if (block instanceof RigBlock || block instanceof RigPartBlock) {
                helper.fail("breaking the " + (atAnchor ? "anchor" : "part") + " left "
                        + block + " standing", helper.relativePos(pos));
            }
        }

        Item drill = PFItems.rig(tier).get();
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16);
        int drills = helper.getLevel().getEntities(EntityType.ITEM, area, e -> true).stream()
                .map(ItemEntity::getItem)
                .filter(stack -> stack.is(drill))
                .mapToInt(ItemStack::getCount)
                .sum();
        if (drills != 1) {
            helper.fail("breaking the " + (atAnchor ? "anchor" : "part") + " dropped " + drills
                    + " drill items where exactly one is paid", helper.relativePos(target));
        }
        helper.succeed();
    }

    /** Places the rig with its own item, the way a player does, and returns every block it put down. */
    private static List<BlockPos> place(GameTestHelper helper, RigTier tier) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(PFItems.rig(tier).get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos absolute = helper.absolutePos(FLOOR);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
        helper.useBlock(FLOOR, player, hit);

        List<BlockPos> placed = BlockPos.betweenClosedStream(
                        new AABB(absolute).inflate(4))
                .filter(pos -> {
                    var block = helper.getLevel().getBlockState(pos).getBlock();
                    return block instanceof RigBlock || block instanceof RigPartBlock;
                })
                .map(BlockPos::immutable)
                .toList();
        if (placed.size() < 2) {
            helper.fail("placing the rig put down " + placed.size() + " blocks", FLOOR);
        }
        return placed;
    }
}
