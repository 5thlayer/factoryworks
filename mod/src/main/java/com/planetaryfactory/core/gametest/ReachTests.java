package com.planetaryfactory.core.gametest;

import java.util.ArrayList;
import java.util.List;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.PFItems;
import com.planetaryfactory.core.machine.AssemblingTier;
import com.planetaryfactory.core.machine.footprint.FootprintMachine;
import com.planetaryfactory.core.mining.rig.RigPartBlock;
import com.planetaryfactory.core.mining.rig.RigTier;
import com.planetaryfactory.core.smelting.FurnaceTier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The player's Reach (#413). The figures are typed, not read off {@code Reach}.
 *
 * <p>A break goes through {@code handleBlockBreakAction}, the path on which the server checks
 * reach; {@code destroyBlock} would skip the rule.
 */
final class ReachTests {

    private static final Vec3 FEET = new Vec3(2.5, 1, 3.5);
    /** In the floor, so the eye is 5.73 from a block 6 off, clear of the server's 5.5. */
    private static final BlockPos FLOOR = BlockPos.containing(FEET).below();
    private static final Identifier PICK = Identifier.fromNamespaceAndPath("planetaryfactory",
            "engineers_iron_pick");

    private ReachTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_fresh_player_reaches_blocks_at_16_and_entities_at_3", 20,
                ReachTests::freshPlayer);
        tests.test("stone_6_blocks_off_is_not_broken", 20,
                helper -> refused(helper, Blocks.STONE.defaultBlockState(), 6));
        tests.test("a_building_6_blocks_off_breaks", 20,
                helper -> breaks(helper, PFBlocks.furnace(FurnaceTier.STONE).get().defaultBlockState(), 6));
        tests.test("stone_4_blocks_off_breaks", 20,
                helper -> breaks(helper, Blocks.STONE.defaultBlockState(), 4));
        tests.test("a_drill_part_7_blocks_off_breaks", 20, ReachTests::drillPart);
    }

    private static void freshPlayer(GameTestHelper helper) {
        ListeningPlayer player = new ListeningPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        helper.assertValueEqual(player.blockInteractionRange(), 16.0, "block interaction range");
        helper.assertValueEqual(player.entityInteractionRange(), 3.0, "entity interaction range");
        helper.succeed();
    }

    private static void refused(GameTestHelper helper, BlockState state, int blocksOff) {
        BlockPos target = FLOOR.east(blocksOff);
        helper.setBlock(target, state);
        ListeningPlayer player = player(helper);
        BlockState before = helper.getBlockState(target);
        List<ItemStack> held = inventory(player);

        mine(helper, player, target);

        helper.assertValueEqual(helper.getBlockState(target), before, "the block " + blocksOff + " off");
        List<ItemStack> after = inventory(player);
        for (int slot = 0; slot < held.size(); slot++) {
            if (!ItemStack.matches(held.get(slot), after.get(slot))) {
                helper.fail("slot " + slot + " went from " + held.get(slot) + " to " + after.get(slot));
            }
        }
        helper.succeed();
    }

    private static void breaks(GameTestHelper helper, BlockState state, int blocksOff) {
        BlockPos target = FLOOR.east(blocksOff);
        helper.setBlock(target, state);
        mine(helper, player(helper), target);
        helper.assertBlockPresent(Blocks.AIR, target);
        helper.succeed();
    }

    private static void drillPart(GameTestHelper helper) {
        BlockPos under = FLOOR.east(10);
        var placer = helper.makeMockPlayer(GameType.SURVIVAL);
        placer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PFItems.rig(RigTier.BURNER).get()));
        BlockPos absolute = helper.absolutePos(under);
        helper.useBlock(under, placer, new BlockHitResult(
                Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false));
        BlockPos part = BlockPos.betweenClosedStream(new AABB(absolute).inflate(3))
                .filter(pos -> helper.getLevel().getBlockState(pos).getBlock() instanceof RigPartBlock)
                .map(BlockPos::immutable)
                .findFirst()
                .orElseThrow(() -> helper.assertionException(under, Component.literal("placing the drill put down no part")));
        // Not relativePos, which turns a Rotation.NONE test by 180 degrees.
        BlockPos target = part.subtract(helper.absolutePos(BlockPos.ZERO));
        mine(helper, player(helper), target);
        if (helper.getBlockState(target).getBlock() instanceof RigPartBlock) {
            helper.fail("the drill part still stands", target);
        }
        helper.succeed();
    }

    private static ListeningPlayer player(GameTestHelper helper) {
        ListeningPlayer player = new ListeningPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        Vec3 feet = helper.absoluteVec(FEET);
        player.setPos(feet.x, feet.y, feet.z);
        player.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(BuiltInRegistries.ITEM.getValue(PICK)));
        return player;
    }

    /** Holds the attack as long as the block takes, the way a client sends start and stop. */
    private static void mine(GameTestHelper helper, ListeningPlayer player, BlockPos target) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(target);
        float perTick = level.getBlockState(pos).getDestroyProgress(player, level, pos);
        if (perTick <= 0) helper.fail("the block cannot be mined", target);
        player.gameMode.handleBlockBreakAction(pos, Action.START_DESTROY_BLOCK, Direction.WEST,
                level.getMaxY(), 0);
        for (int tick = 0; tick < Math.ceil(0.7f / perTick); tick++) player.gameMode.tick();
        player.gameMode.handleBlockBreakAction(pos, Action.STOP_DESTROY_BLOCK, Direction.WEST,
                level.getMaxY(), 1);
    }

    private static List<ItemStack> inventory(ListeningPlayer player) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            stacks.add(player.getInventory().getItem(slot).copy());
        }
        return stacks;
    }

    /** A machine's screen stays open as far off as the player reaches it. */
    static final class Screens {

        private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);

        private Screens() {
        }

        static void register(PFGameTests.Registrar tests) {
            tests.test("an_assembling_machine_12_blocks_off_stays_open", 20,
                    helper -> screen(helper, PFBlocks.assemblingFootprint(AssemblingTier.ONE), 12, true));
            tests.test("an_assembling_machine_22_blocks_off_closes", 20,
                    helper -> screen(helper, PFBlocks.assemblingFootprint(AssemblingTier.ONE), 22, false));
            tests.test("a_steam_engine_12_blocks_off_stays_open", 20,
                    helper -> screen(helper, PFBlocks.STEAM_ENGINE_FOOTPRINT, 12, true));
            tests.test("a_steam_engine_22_blocks_off_closes", 20,
                    helper -> screen(helper, PFBlocks.STEAM_ENGINE_FOOTPRINT, 22, false));
        }

        /** The menu is made directly: a fake player opens none. */
        private static void screen(GameTestHelper helper, FootprintMachine footprint, int blocksOff,
                boolean open) {
            footprint.placeAll(helper.getLevel(), helper.absolutePos(ANCHOR), Direction.NORTH);
            ListeningPlayer player = new ListeningPlayer(helper);
            Vec3 feet = helper.absoluteVec(Vec3.atBottomCenterOf(ANCHOR).add(-blocksOff, 0, 0));
            player.setPos(feet.x, feet.y, feet.z);
            MenuProvider machine = (MenuProvider) helper.getLevel().getBlockEntity(helper.absolutePos(ANCHOR));
            AbstractContainerMenu menu = machine.createMenu(0, player.getInventory(), player);
            helper.assertValueEqual(menu.stillValid(player), open, "the screen " + blocksOff + " blocks off is open");
            helper.succeed();
        }
    }
}
