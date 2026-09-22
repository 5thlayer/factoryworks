package com.planetaryfactory.core.gametest;

import java.util.List;
import java.util.Map;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.machine.footprint.FootprintMachine;

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
 * A footprint machine broken at any of its blocks leaves none standing and pays exactly one item
 * (ADR-0072, ADR-0077), through the player's game mode, the path a mined block takes.
 */
final class FootprintBreakTests {

    private static final BlockPos FLOOR = new BlockPos(3, 0, 3);
    private static final Identifier PICK = Identifier.fromNamespaceAndPath("planetaryfactory",
            "engineers_iron_pick");

    private FootprintBreakTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        Map<String, FootprintMachine> machines = Map.of(
                "assembling_machine", PFBlocks.ASSEMBLING_MACHINE_FOOTPRINT,
                "steam_engine", PFBlocks.STEAM_ENGINE_FOOTPRINT,
                "radar", PFBlocks.RADAR_FOOTPRINT,
                "pumpjack", PFBlocks.PUMPJACK_FOOTPRINT);
        machines.forEach((name, machine) -> {
            tests.test(name + "_broken_at_its_anchor_leaves_nothing", 20,
                    helper -> breakAndCheck(helper, machine, 0));
            for (int part = 1; part <= machine.footprint().partCount(); part++) {
                int index = part;
                tests.test(name + "_broken_at_part_" + part + "_leaves_nothing", 20,
                        helper -> breakAndCheck(helper, machine, index));
            }
        });
    }

    private static void breakAndCheck(GameTestHelper helper, FootprintMachine machine, int index) {
        List<BlockPos> footprint = place(helper, machine);
        BlockPos target = footprint.get(index);

        // Not makeMockServerPlayerInLevel: joining the level fires KubeJS's login sync, which
        // refuses the mock connection.
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.getValue(PICK)));
        player.gameMode.destroyBlock(target);

        for (BlockPos pos : footprint) {
            var state = helper.getLevel().getBlockState(pos);
            if (machine.isAnchor(state) || state.is(machine.part().get())) {
                helper.fail("breaking block " + index + " left " + state.getBlock() + " standing",
                        helper.relativePos(pos));
            }
        }

        Item item = machine.item().get();
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(16);
        int dropped = helper.getLevel().getEntities(EntityType.ITEM, area, e -> true).stream()
                .map(ItemEntity::getItem)
                .filter(stack -> stack.is(item))
                .mapToInt(ItemStack::getCount)
                .sum();
        if (dropped != 1) {
            helper.fail("breaking block " + index + " dropped " + dropped + " items where exactly one"
                    + " is paid", helper.relativePos(target));
        }
        helper.succeed();
    }

    /** Places the machine with its own item, the way a player does; every block it put down, anchor first. */
    private static List<BlockPos> place(GameTestHelper helper, FootprintMachine machine) {
        if (machine == PFBlocks.PUMPJACK_FOOTPRINT) {
            helper.setBlock(FLOOR, PFBlocks.OIL_WELL.get());
        }
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(machine.item().get()));
        BlockPos absolute = helper.absolutePos(FLOOR);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
        helper.useBlock(FLOOR, player, hit);

        BlockPos anchor = absolute.above();
        var state = helper.getLevel().getBlockState(anchor);
        if (!machine.isAnchor(state)) {
            helper.fail("placing the machine put no anchor on the floor", FLOOR.above());
        }
        return machine.positions(anchor, state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING));
    }
}
