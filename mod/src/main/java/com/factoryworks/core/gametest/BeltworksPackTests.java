package com.factoryworks.core.gametest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.factoryworks.core.PFBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The Pack's claims about Beltworks: that its loader, run on the Pack's {@code beltworks-server.toml},
 * gives a Wireworks pole's demand probe nothing (no Beltworks test holds it), and that the KubeJS
 * recipe sweep removes every {@code beltworks:} recipe with the Pack's own belt recipe as the control
 * (#348, #627, ADR-0034). The belt mechanics are Beltworks' own GameTests (#438).
 */
final class BeltworksPackTests {

    private static final BlockPos LOADER = new BlockPos(3, 1, 3);
    private static final BlockPos POLE = new BlockPos(5, 1, 4);
    private static final int TIER_2_LOADER_BUFFER_FE = 200;
    private static final Identifier IMPROVED_LOADER = Identifier.fromNamespaceAndPath("beltworks", "improved_loader");
    private static final String EXPRESS_BELT_RECIPE = "factoryworks:assembling/express_transport_belt";

    private BeltworksPackTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("loader_demand_probe_leaves_nothing_behind", 100, BeltworksPackTests::probeLeavesNothing);
        tests.test("belts_own_recipes_are_swept", 20, BeltworksPackTests::ownRecipesAreSwept);
    }

    // A small pole with no generator still probes: the loader must hold nothing after it, and an aborted
    // insert by hand must find the room the probe would (#348).
    private static void probeLeavesNothing(GameTestHelper helper) {
        helper.setBlock(POLE, LibraryBlocks.smallPole());
        Block loader = BuiltInRegistries.BLOCK.getValue(IMPROVED_LOADER);
        helper.setBlock(LOADER, loader.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        helper.startSequence().thenIdle(45).thenExecute(() -> {
            long stored = face(helper, LOADER).getAmountAsLong();
            if (stored != 0) {
                helper.fail("the loader kept " + stored + " FE from the demand probe", LOADER);
            }
            long room;
            try (Transaction transaction = Transaction.openRoot()) {
                room = face(helper, LOADER).insert(Integer.MAX_VALUE, transaction);
            }
            if (room != TIER_2_LOADER_BUFFER_FE) {
                helper.fail("the loader's face reported room for " + room + " FE, expected "
                        + TIER_2_LOADER_BUFFER_FE, LOADER);
            }
            stored = face(helper, LOADER).getAmountAsLong();
            if (stored != 0) {
                helper.fail("the loader kept " + stored + " FE from an aborted insert", LOADER);
            }
        }).thenSucceed();
    }

    // The pack's express belt recipe is the control: a sweep that removed everything passes too (ADR-0034).
    private static void ownRecipesAreSwept(GameTestHelper helper) {
        Set<String> loaded = helper.getLevel().getServer().getRecipeManager().recipeMap().values()
                .stream()
                .map(holder -> holder.id().identifier().toString())
                .collect(Collectors.toSet());
        if (!loaded.contains(EXPRESS_BELT_RECIPE)) {
            helper.fail(EXPRESS_BELT_RECIPE + " is not loaded, so this proves nothing");
            return;
        }
        List<String> survivors = loaded.stream().filter(id -> id.startsWith("beltworks:")).sorted().toList();
        if (!survivors.isEmpty()) {
            helper.fail("Beltworks' own recipes survived the sweep: " + survivors);
            return;
        }
        helper.succeed();
    }

    private static EnergyHandler face(GameTestHelper helper, BlockPos pos) {
        var handler = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(pos), null);
        if (handler == null) {
            helper.fail("the loader has no energy face", pos);
        }
        return handler;
    }
}
