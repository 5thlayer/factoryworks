package com.factoryworks.core.gametest;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.craftworks.machine.AssemblerBlockEntity;
import io.github._5thlayer.craftworks.machine.AssemblerSlots;
import io.github._5thlayer.craftworks.machine.AssemblerTier;
import io.github._5thlayer.craftworks.machine.Assemblers;
import io.github._5thlayer.craftworks.machine.FluidLayout;
import io.github._5thlayer.groundworks.Footprint;
import io.github._5thlayer.pipeworks.api.FluidPipes;
import io.github._5thlayer.pipeworks.api.FluidPorts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * What the Pack owns of its oil and chemistry recipes, which run on Craftworks' Assembler (ADR-0125): that
 * they take their fluids from Pipeworks pipes and send their fluid results down pipes into tanks. The
 * Assembler's own behaviour is Craftworks' GameTests. The recipes are the Pack's, typed here for
 * {@code BoilerTests}' reason.
 *
 * <p>A Fluid Connection has no direction and pushes into any neighbour that takes the fluid, so a supply
 * stays full and an output pipe starts empty: an emptied supply pipe would take a product. The Assembler
 * pushes through its connections in {@link FluidLayout#ASSEMBLER}'s order, the three it faces first.
 */
final class AssemblerOilChainTests {

    private static final Identifier PLASTIC = Identifier.parse("factoryworks:chemistry/plastic_bar");
    private static final Identifier SULFURIC_ACID = Identifier.parse("factoryworks:chemistry/sulfuric_acid");
    private static final Identifier ADVANCED = Identifier.parse("factoryworks:oil_processing/advanced_oil_processing");

    private static final BlockPos CHEMISTRY = new BlockPos(8, 1, 3);
    private static final Direction CHEMISTRY_FACING = Direction.EAST;
    private static final int GAS_PER_CRAFT = 20;
    private static final int ACID_PER_CRAFT = 50;
    /** Plastic and acid are one second each, and tier 2's speed is 0.75. */
    private static final int TIER_2_TICKS_PER_CRAFT = 27;

    private static final BlockPos REFINING = new BlockPos(11, 1, 3);
    private static final Direction REFINING_FACING = Direction.SOUTH;
    private static final List<String> PRODUCTS = List.of(
            "factoryworks:heavy_oil", "factoryworks:light_oil", "factoryworks:petroleum_gas");
    /** One advanced craft's share of each product, in {@link #PRODUCTS} order. */
    private static final List<Integer> PER_CRAFT = List.of(25, 45, 55);

    /** More than an input box holds, so a supply never runs dry. */
    private static final int SUPPLY = 5_000;

    private AssemblerOilChainTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_tier_2_assembler_makes_plastic_from_petroleum_gas_in_a_pipe", 200,
                AssemblerOilChainTests::makesPlasticFromPipedGas);
        tests.test("a_tier_2_assembler_sends_sulfuric_acid_through_a_pipe_to_a_tank", 200,
                AssemblerOilChainTests::sendsAcidToATank);
        tests.test("a_tier_3_assembler_refines_piped_crude_into_three_tanks", 600,
                AssemblerOilChainTests::refinesPipedCrude);
    }

    /** Gas in a two-pipe segment at one connection, coal in the slot and a pole beside: plastic comes out. */
    private static void makesPlasticFromPipedGas(GameTestHelper helper) {
        AssemblerBlockEntity machine = place(helper, AssemblerTier.TWO, CHEMISTRY, CHEMISTRY_FACING);
        hold(helper, machine, PLASTIC, CHEMISTRY);
        Connection in = connections(helper, CHEMISTRY, CHEMISTRY_FACING).get(1);
        List<BlockPos> pipes = line(helper, in, 2);
        int gas = 2 * 100;

        helper.startSequence()
                .thenExecute(() -> {
                    machine.inventory().set(0, ItemResource.of(item("minecraft:coal")), 8);
                    helper.setBlock(CHEMISTRY.south(3), LibraryBlocks.creativePole());
                })
                .thenIdle(2)
                .thenExecute(() -> fill(helper, pipes.getFirst(), fluid("factoryworks:petroleum_gas"), gas))
                .thenIdle(LibraryBlocks.POLE_RESCAN_INTERVAL + 5 + 3 * TIER_2_TICKS_PER_CRAFT)
                .thenExecute(() -> {
                    int plastic = machine.inventory().getAmountAsInt(AssemblerSlots.PRODUCT);
                    if (plastic < 2 || plastic % 2 != 0
                            || !machine.inventory().getResource(AssemblerSlots.PRODUCT).equals(
                                    ItemResource.of(item("factoryworks:plastic_bar")))) {
                        helper.fail("an Assembler 2 on piped gas made " + plastic + " of "
                                + machine.inventory().getResource(AssemblerSlots.PRODUCT)
                                + ", expected whole crafts of 2 plastic", CHEMISTRY);
                    }
                    int taken = gas - FluidPorts.segment(helper.getLevel(), pipes.getFirst()).getAmountAsInt(0);
                    if (taken < plastic / 2 * GAS_PER_CRAFT) {
                        helper.fail("the pipes gave " + taken + " mB of gas for " + plastic + " plastic", CHEMISTRY);
                    }
                })
                .thenSucceed();
    }

    /** Water from a tank through the connection it faces, sulfur and iron in the slots; the acid leaves behind. */
    private static void sendsAcidToATank(GameTestHelper helper) {
        AssemblerBlockEntity machine = place(helper, AssemblerTier.TWO, CHEMISTRY, CHEMISTRY_FACING);
        hold(helper, machine, SULFURIC_ACID, CHEMISTRY);
        List<Connection> connections = connections(helper, CHEMISTRY, CHEMISTRY_FACING);
        BlockPos supply = tankAfter(helper, line(helper, connections.get(1), 2), connections.get(1).side());
        BlockPos tank = tankAfter(helper, line(helper, connections.get(4), 2), connections.get(4).side());

        helper.startSequence()
                .thenExecute(() -> {
                    machine.inventory().set(0, ItemResource.of(item("factoryworks:sulfur")), 10);
                    machine.inventory().set(1, ItemResource.of(item("factoryworks:iron_plate")), 2);
                    helper.setBlock(CHEMISTRY.south(3), LibraryBlocks.creativePole());
                })
                .thenIdle(2)
                .thenExecute(() -> fill(helper, supply, Fluids.WATER, SUPPLY))
                .thenIdle(LibraryBlocks.POLE_RESCAN_INTERVAL + 5 + 3 * TIER_2_TICKS_PER_CRAFT)
                .thenExecute(() -> {
                    ResourceHandler<FluidResource> stored = FluidPorts.segment(helper.getLevel(), tank);
                    int amount = stored == null ? 0 : stored.getAmountAsInt(0);
                    if (amount < ACID_PER_CRAFT || amount % ACID_PER_CRAFT != 0
                            || !stored.getResource(0).equals(FluidResource.of(fluid("factoryworks:sulfuric_acid")))) {
                        helper.fail("the tank holds " + amount + " mB of " + (stored == null ? "no segment" : stored.getResource(0))
                                + ", expected whole crafts of " + ACID_PER_CRAFT + " mB of sulfuric acid",
                                relative(helper, tank));
                    }
                })
                .thenSucceed();
    }

    /**
     * Crude and water from two tanks behind, and the three products out of the three connections it faces,
     * each down its own pipe into its own tank. The middle output pipe touches the other two, so its sides
     * toward them are closed, as a player closes them, and it rises to its tank.
     */
    private static void refinesPipedCrude(GameTestHelper helper) {
        AssemblerBlockEntity machine = place(helper, AssemblerTier.THREE, REFINING, REFINING_FACING);
        hold(helper, machine, ADVANCED, REFINING);
        List<Connection> connections = connections(helper, REFINING, REFINING_FACING);
        Direction east = Direction.EAST;

        List<BlockPos> first = line(helper, connections.get(0), 1);
        first.add(pipe(helper, first.getLast().relative(east)));
        List<BlockPos> middle = line(helper, connections.get(1), 1);
        middle.add(pipe(helper, middle.getLast().above()));
        List<BlockPos> last = line(helper, connections.get(2), 1);
        last.add(pipe(helper, last.getLast().relative(east.getOpposite())));
        FluidPipes.close(helper.getLevel(), middle.getFirst(), east);
        FluidPipes.close(helper.getLevel(), middle.getFirst(), east.getOpposite());
        List<BlockPos> outputs = List.of(tankAfter(helper, first, east), tankAfter(helper, middle, Direction.UP),
                tankAfter(helper, last, east.getOpposite()));

        BlockPos water = tankAfter(helper, line(helper, connections.get(3), 1), east);
        BlockPos crude = tankAfter(helper, line(helper, connections.get(5), 1), east.getOpposite());

        helper.startSequence()
                .thenExecute(() -> helper.setBlock(REFINING.east(3), LibraryBlocks.creativePole()))
                .thenIdle(2)
                .thenExecute(() -> {
                    fill(helper, water, Fluids.WATER, SUPPLY);
                    fill(helper, crude, fluid("factoryworks:crude_oil"), SUPPLY);
                })
                .thenWaitUntil(() -> assertProducts(helper, outputs))
                .thenSucceed();
    }

    /** The three tanks hold the three products, one each, in whole crafts. */
    private static void assertProducts(GameTestHelper helper, List<BlockPos> outputs) {
        List<String> seen = new ArrayList<>();
        for (BlockPos tank : outputs) {
            ResourceHandler<FluidResource> stored = FluidPorts.segment(helper.getLevel(), tank);
            if (stored == null) {
                helper.fail("the tank is in no segment", relative(helper, tank));
                return;
            }
            FluidResource resource = stored.getResource(0);
            int index = PRODUCTS.indexOf(BuiltInRegistries.FLUID.getKey(resource.getFluid()).toString());
            int amount = stored.getAmountAsInt(0);
            if (index < 0 || amount == 0) {
                throw helper.assertionException(relative(helper, tank), "an output tank holds " + amount + " mB of " + resource);
            }
            if (amount % PER_CRAFT.get(index) != 0) {
                helper.fail("a tank holds " + amount + " mB of " + resource + ", not whole crafts of "
                        + PER_CRAFT.get(index), relative(helper, tank));
            }
            seen.add(PRODUCTS.get(index));
        }
        if (seen.stream().distinct().count() != PRODUCTS.size()) {
            helper.fail("the three tanks hold " + seen + ", expected one each of " + PRODUCTS, REFINING);
        }
    }

    /** A footprint block that is a Fluid Connection, and the way its face points: absolute. */
    private record Connection(BlockPos block, Direction side) {
    }

    /** The six connections, in {@link FluidLayout#ASSEMBLER}'s order. */
    private static List<Connection> connections(GameTestHelper helper, BlockPos origin, Direction facing) {
        BlockPos at = helper.absolutePos(origin);
        List<Connection> found = new ArrayList<>();
        for (FluidLayout.Site site : FluidLayout.ASSEMBLER.connections()) {
            BlockPos block = at.relative(facing, site.ahead()).relative(facing.getClockWise(), site.right());
            Direction side = site.face() == FluidLayout.Face.AHEAD ? facing : facing.getOpposite();
            found.add(new Connection(block, side));
        }
        return found;
    }

    /** {@code count} pipes in a line out of the connection, the first against it, absolute. */
    private static List<BlockPos> line(GameTestHelper helper, Connection connection, int count) {
        List<BlockPos> pipes = new ArrayList<>();
        BlockPos at = connection.block().relative(connection.side());
        for (int i = 0; i < count; i++) {
            pipes.add(pipe(helper, at));
            at = at.relative(connection.side());
        }
        return pipes;
    }

    private static BlockPos pipe(GameTestHelper helper, BlockPos absolute) {
        helper.getLevel().setBlockAndUpdate(absolute, LibraryBlocks.pipe().defaultBlockState());
        return absolute;
    }

    /** A storage tank one past the last pipe, the way {@code toward} points: absolute. */
    private static BlockPos tankAfter(GameTestHelper helper, List<BlockPos> pipes, Direction toward) {
        BlockPos tank = pipes.getLast().relative(toward);
        helper.getLevel().setBlockAndUpdate(tank, LibraryBlocks.storageTank().defaultBlockState());
        return tank;
    }

    private static void fill(GameTestHelper helper, BlockPos node, Fluid fluid, int amount) {
        ResourceHandler<FluidResource> segment = FluidPorts.segment(helper.getLevel(), node);
        if (segment == null) {
            helper.fail("the pipe or tank is in no segment", relative(helper, node));
            return;
        }
        try (Transaction tx = Transaction.openRoot()) {
            segment.insert(0, FluidResource.of(fluid), amount, tx);
            tx.commit();
        }
    }

    private static AssemblerBlockEntity place(GameTestHelper helper, AssemblerTier tier, BlockPos origin, Direction facing) {
        Footprint footprint = Assemblers.footprint(tier);
        List<BlockPos> positions = footprint.positions(helper.absolutePos(origin), facing);
        for (int i = 0; i < positions.size(); i++) {
            helper.getLevel().setBlock(positions.get(i), footprint.stateAt(i, facing), Block.UPDATE_ALL);
        }
        return helper.getBlockEntity(origin, AssemblerBlockEntity.class);
    }

    private static void hold(GameTestHelper helper, AssemblerBlockEntity machine, Identifier recipe, BlockPos origin) {
        machine.setHeldRecipe(recipe, helper.makeMockPlayer(GameType.SURVIVAL));
        if (machine.heldRecipe().isEmpty()) {
            helper.fail(recipe + " was not held, so this proves nothing", origin);
        }
    }

    private static BlockPos relative(GameTestHelper helper, BlockPos absolute) {
        return absolute.subtract(helper.absolutePos(BlockPos.ZERO));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    private static Fluid fluid(String id) {
        return BuiltInRegistries.FLUID.getValue(Identifier.parse(id));
    }
}
