package com.factoryworks.core.gametest;

import io.github._5thlayer.wireworks.WireworksRegistries;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.factoryworks.core.PFBlocks;
import io.github._5thlayer.wireworks.PoleTier;
import io.github._5thlayer.wireworks.SupplyAreaPoleBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import io.github._5thlayer.beltworks.BlockContent;
import io.github._5thlayer.beltworks.model.BeltTier;

/**
 * Beltworks against the Pack's own content: a small pole's demand probe, and the KubeJS recipe
 * sweep. The belt mechanics are the Mod's own GameTests (#438).
 */
final class BeltworksPackTests {

    private static final BlockPos LOADER = new BlockPos(3, 1, 3);
    private static final BlockPos POLE = new BlockPos(5, 1, 4);
    private static final int TIER_2_LOADER_BUFFER_FE = 200;
    private static final String EXPRESS_BELT_RECIPE = "factoryworks:assembling/express_transport_belt";

    private BeltworksPackTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("loader_demand_probe_leaves_nothing_behind", 100, BeltworksPackTests::probeLeavesNothing);
        tests.test("belts_own_recipes_are_swept", 20, BeltworksPackTests::ownRecipesAreSwept);
    }

    // A small pole with no generator still probes: the loader must hold nothing after it (#348).
    private static void probeLeavesNothing(GameTestHelper helper) {
        helper.setBlock(POLE, WireworksRegistries.pole(PoleTier.SMALL).get());
        helper.setBlock(LOADER, BlockContent.loaderFor(BeltTier.IMPROVED).defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        helper.startSequence().thenIdle(45).thenExecute(() -> {
            long stored = face(helper, LOADER).getAmountAsLong();
            if (stored != 0) {
                helper.fail("the loader kept " + stored + " FE from the demand probe", LOADER);
            }
            long demanded = helper.getBlockEntity(POLE, SupplyAreaPoleBlockEntity.class).demandedFePerTick();
            if (demanded != TIER_2_LOADER_BUFFER_FE) {
                helper.fail("the pole read a demand of " + demanded + " FE/t, expected "
                        + TIER_2_LOADER_BUFFER_FE, POLE);
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
