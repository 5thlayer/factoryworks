package com.factoryworks.core.gametest;

import java.util.List;
import java.util.Optional;

import io.github._5thlayer.craftworks.machine.AssemblerBlockEntity;
import io.github._5thlayer.craftworks.machine.AssemblerMenu;
import io.github._5thlayer.craftworks.machine.AssemblerSlots;
import io.github._5thlayer.craftworks.machine.AssemblerTier;
import io.github._5thlayer.craftworks.machine.Assemblers;
import io.github._5thlayer.craftworks.machine.HoldVerdict;
import io.github._5thlayer.groundworks.Footprint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The Pack's claim: a tier 2 Assembler crafts a {@code crafting-with-fluid} recipe with its fluid
 * piped in from a Pipeworks storage tank, and tier 1 cannot hold one (#580, ADR-0109). The pull is
 * Craftworks' Fluid Connection and the segment is Pipeworks', so the Pack moves no fluid here: the
 * test only fills the tank and lays the pipe.
 *
 * <p>The recipe is held before the pipe is placed, since a connection exists only while the Held
 * recipe has a fluid. Concrete (100 mB of water, 10 gray concrete) and the electric engine unit
 * (15 mB of lubricant) are the fixtures; tier 2's speed 0.75 runs a 200-tick recipe in 267 ticks.
 * The figures are typed, for {@code BoilerTests}' reason.
 */
final class AssemblingFluidTests {

    private static final BlockPos ORIGIN = new BlockPos(4, 1, 4);
    private static final Direction FACING = Direction.NORTH;
    /** The block the first Fluid Connection pulls from: two ahead of the origin and one to its left. */
    private static final BlockPos PIPE = ORIGIN.relative(FACING, 2).relative(FACING.getCounterClockWise());
    private static final BlockPos TANK = PIPE.relative(FACING);
    private static final BlockPos POLE = ORIGIN.south(3);

    private static final String PREFIX = "factoryworks:assembling/";
    private static final Identifier CONCRETE = Identifier.parse(PREFIX + "concrete");
    private static final Identifier ENGINE = Identifier.parse(PREFIX + "electric_engine_unit");

    private static final int TICKS_PER_CRAFT = 267;
    private static final int TANK_FILL = 1000;

    private AssemblingFluidTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("lubricant_through_a_pipe_into_a_tier_2_assembler_crafts_the_recipe", 600,
                helper -> craftsFromPipe(helper, ENGINE, "factoryworks:lubricant", "factoryworks:electric_engine_unit", 1));
        tests.test("water_through_a_pipe_into_a_tier_2_assembler_crafts_the_recipe", 600,
                helper -> craftsFromPipe(helper, CONCRETE, "minecraft:water", "minecraft:gray_concrete", 10));
        tests.test("a_tier_1_assembler_cannot_hold_a_fluid_recipe", 20, AssemblingFluidTests::tierOneRefuses);
    }

    private static void craftsFromPipe(GameTestHelper helper, Identifier recipe, String fluid, String product,
            int count) {
        AssemblerBlockEntity machine = place(helper, AssemblerTier.TWO);
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        HoldVerdict verdict = ((AssemblerMenu) machine.createMenu(0, player.getInventory(), player))
                .request(player, recipe);
        if (verdict != HoldVerdict.HELD || !machine.heldRecipe().equals(Optional.of(recipe))) {
            helper.fail("tier 2 answered " + recipe + " " + verdict + " and holds " + machine.heldRecipe(), ORIGIN);
            return;
        }
        helper.setBlock(PIPE, LibraryBlocks.pipe());
        helper.setBlock(TANK, LibraryBlocks.storageTank());
        helper.startSequence()
                .thenExecute(() -> {
                    fillTank(helper, fluid);
                    feed(machine, recipe);
                    helper.setBlock(POLE, LibraryBlocks.creativePole());
                })
                .thenIdle(LibraryBlocks.POLE_RESCAN_INTERVAL + 5 + TICKS_PER_CRAFT + 20)
                .thenExecute(() -> {
                    ItemResource made = machine.inventory().getResource(AssemblerSlots.PRODUCT);
                    int amount = machine.inventory().getAmountAsInt(AssemblerSlots.PRODUCT);
                    if (!made.equals(ItemResource.of(item(product))) || amount != count) {
                        helper.fail("a pipe-fed " + recipe + " made " + amount + " of " + made + ", expected "
                                + count + " of " + product, ORIGIN);
                    }
                })
                .thenSucceed();
    }

    /** The Pack gives tier 1 no `crafting-with-fluid` (ADR-0125), so either fluid recipe is refused. */
    private static void tierOneRefuses(GameTestHelper helper) {
        AssemblerBlockEntity machine = place(helper, AssemblerTier.ONE);
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        AssemblerMenu menu = (AssemblerMenu) machine.createMenu(0, player.getInventory(), player);
        for (Identifier recipe : List.of(CONCRETE, ENGINE)) {
            HoldVerdict verdict = menu.request(player, recipe);
            if (verdict == HoldVerdict.HELD || machine.heldRecipe().isPresent()) {
                helper.fail("tier 1 answered " + recipe + " " + verdict + " and holds " + machine.heldRecipe(),
                        ORIGIN);
                return;
            }
        }
        helper.succeed();
    }

    private static void fillTank(GameTestHelper helper, String fluid) {
        ResourceHandler<FluidResource> tank = helper.getLevel().getCapability(Capabilities.Fluid.BLOCK,
                helper.absolutePos(TANK), null);
        FluidResource resource = FluidResource.of(BuiltInRegistries.FLUID.getValue(Identifier.parse(fluid)));
        try (Transaction transaction = Transaction.openRoot()) {
            int moved = tank == null ? 0 : tank.insert(resource, TANK_FILL, transaction);
            if (moved != TANK_FILL) {
                helper.fail("the storage tank took " + moved + " mB of " + fluid + ", expected " + TANK_FILL, TANK);
                return;
            }
            transaction.commit();
        }
    }

    private static void feed(AssemblerBlockEntity machine, Identifier recipe) {
        if (recipe.equals(CONCRETE)) {
            machine.inventory().set(0, ItemResource.of(item("minecraft:stone_bricks")), 5);
            machine.inventory().set(1, ItemResource.of(item("minecraft:raw_iron")), 1);
        } else {
            machine.inventory().set(0, ItemResource.of(item("factoryworks:engine_unit")), 1);
            machine.inventory().set(1, ItemResource.of(item("factoryworks:electronic_circuit")), 2);
        }
    }

    private static AssemblerBlockEntity place(GameTestHelper helper, AssemblerTier tier) {
        Footprint footprint = Assemblers.footprint(tier);
        List<BlockPos> positions = footprint.positions(helper.absolutePos(ORIGIN), FACING);
        for (int i = 0; i < positions.size(); i++) {
            helper.getLevel().setBlock(positions.get(i), footprint.stateAt(i, FACING), Block.UPDATE_ALL);
        }
        return helper.getBlockEntity(ORIGIN, AssemblerBlockEntity.class);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }
}
