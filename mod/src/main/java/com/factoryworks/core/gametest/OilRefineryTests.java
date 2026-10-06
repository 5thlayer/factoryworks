package com.factoryworks.core.gametest;

import java.util.ArrayList;
import java.util.List;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.oil.OilWellBlockEntity;

import io.github._5thlayer.craftworks.machine.FluidMachineBlockEntity;
import io.github._5thlayer.craftworks.machine.FluidMachines;
import io.github._5thlayer.groundworks.Footprint;
import io.github._5thlayer.pipeworks.api.FluidPorts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * What the Pack owns of the Oil Refinery, which is Craftworks' (ADR-0124): that the Pumpjack's crude
 * reaches it through Pipeworks pipes, and that its three products leave through pipes into three tanks.
 * Its own behaviour is Craftworks' GameTests. The recipe is the Pack's, typed here for
 * {@code BoilerTests}' reason.
 *
 * <p>Its Fluid Connections have no direction, so a pipe that runs dry takes whatever the refinery
 * pushes. The three outputs therefore leave by the first three connections, which a refinery tries
 * first, and the two supplies stand on the last two.
 */
final class OilRefineryTests {

    private static final BlockPos ORIGIN = new BlockPos(12, 1, 3);
    private static final Direction FACING = Direction.SOUTH;
    private static final BlockPos WELL = new BlockPos(5, 0, 3);
    private static final BlockPos POLE = new BlockPos(8, 1, 3);

    private static final Identifier ADVANCED = Identifier.parse("factoryworks:oil_processing/advanced_oil_processing");

    private static final String HEAVY_OIL = "factoryworks:heavy_oil";
    private static final String LIGHT_OIL = "factoryworks:light_oil";
    private static final String PETROLEUM_GAS = "factoryworks:petroleum_gas";
    private static final List<String> PRODUCTS = List.of(HEAVY_OIL, LIGHT_OIL, PETROLEUM_GAS);
    /** One advanced craft's share of each product, in {@link #PRODUCTS} order. */
    private static final List<Integer> PER_CRAFT = List.of(25, 45, 55);

    /** More than the refinery's water box holds, so the supply stays full. */
    private static final int WATER_SUPPLY = 5_000;
    private static final long FULL_YIELD = 300_000;

    private OilRefineryTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("an_oil_refinery_refines_a_pumpjacks_crude_from_a_pipe_into_three_tanks", 900,
                OilRefineryTests::refinesPipedCrude);
    }

    /**
     * Crude from the Pumpjack down a line of pipes, water from a tank, a pole between them: after two
     * crafts' time each of heavy oil, light oil and petroleum gas has reached a tank of its own.
     */
    private static void refinesPipedCrude(GameTestHelper helper) {
        FluidMachineBlockEntity refinery = placeRefinery(helper);
        hold(helper, refinery);
        List<Connection> connections = connections(helper);

        Tanks tanks = new Tanks(
                List.of(lay(helper, connections.get(0), List.of(), new BlockPos(14, 1, 6)),
                        lay(helper, connections.get(1), List.of(), new BlockPos(10, 1, 6)),
                        lay(helper, connections.get(2), List.of(), new BlockPos(15, 1, 0))),
                lay(helper, connections.get(3), List.of(new BlockPos(12, 2, 0)), new BlockPos(12, 3, 0)),
                crudeLine(helper, connections.get(4)));
        placePumpjack(helper);

        helper.startSequence()
                .thenExecute(() -> helper.setBlock(POLE, LibraryBlocks.creativePole()))
                .thenIdle(2)
                .thenExecute(() -> fill(helper, tanks.water(), FluidResource.of(Fluids.WATER), WATER_SUPPLY))
                .thenWaitUntil(() -> assertProducts(helper, tanks))
                .thenSucceed();
    }

    /** The three tanks hold the three products, one each, in whole crafts; the crude line holds only crude. */
    private static void assertProducts(GameTestHelper helper, Tanks tanks) {
        List<String> seen = new ArrayList<>();
        for (BlockPos tank : tanks.outputs()) {
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
            helper.fail("the three tanks hold " + seen + ", expected one each of " + PRODUCTS, ORIGIN);
        }
        ResourceHandler<FluidResource> crude = FluidPorts.segment(helper.getLevel(), tanks.crude());
        if (crude == null || (crude.getAmountAsInt(0) > 0 && !crude.getResource(0).equals(
                FluidResource.of(fluid("factoryworks:crude_oil"))))) {
            helper.fail("the crude line holds " + (crude == null ? "no segment" : crude.getResource(0)), relative(helper, tanks.crude()));
        }
    }

    /** The three output tanks, the water supply tank, and the pipe at the crude connection. */
    private record Tanks(List<BlockPos> outputs, BlockPos water, BlockPos crude) {
    }

    /** A footprint block that is a Fluid Connection, and the way its face points: absolute. */
    private record Connection(BlockPos block, Direction side) {

        BlockPos beyond() {
            return block.relative(side);
        }
    }

    /**
     * The refinery's five connections in the order it tries them, typed from the ticket: two on the edge it
     * faces, then three on the other. Each is checked to answer with a fluid face.
     */
    private static List<Connection> connections(GameTestHelper helper) {
        Direction back = FACING.getOpposite();
        Direction right = FACING.getClockWise();
        BlockPos origin = helper.absolutePos(ORIGIN);
        List<Connection> found = new ArrayList<>();
        for (int along : new int[] {-1, 1}) {
            found.add(new Connection(origin.relative(FACING, 2).relative(right, along), FACING));
        }
        for (int along : new int[] {-2, 0, 2}) {
            found.add(new Connection(origin.relative(back, 2).relative(right, along), back));
        }
        for (Connection connection : found) {
            if (helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, connection.block(), connection.side()) == null) {
                helper.fail("no fluid face at " + relative(helper, connection.block()) + " facing " + connection.side(), ORIGIN);
            }
        }
        return found;
    }

    /** A pipe against the connection, then {@code more} pipes, then a storage tank: the tank's position. */
    private static BlockPos lay(GameTestHelper helper, Connection connection, List<BlockPos> more, BlockPos tank) {
        helper.getLevel().setBlockAndUpdate(connection.beyond(), LibraryBlocks.pipe().defaultBlockState());
        for (BlockPos pipe : more) {
            helper.setBlock(pipe, LibraryBlocks.pipe());
        }
        helper.setBlock(tank, LibraryBlocks.storageTank());
        return helper.absolutePos(tank);
    }

    /** Pipes from the crude connection west along the wall to the Pumpjack's north-east part. */
    private static BlockPos crudeLine(GameTestHelper helper, Connection connection) {
        BlockPos first = connection.beyond();
        helper.getLevel().setBlockAndUpdate(first, LibraryBlocks.pipe().defaultBlockState());
        for (int x = 9; x >= 6; x--) {
            helper.setBlock(new BlockPos(x, 1, 0), LibraryBlocks.pipe());
        }
        helper.setBlock(new BlockPos(6, 1, 1), LibraryBlocks.pipe());
        return first;
    }

    private static void placePumpjack(GameTestHelper helper) {
        helper.setBlock(WELL, PFBlocks.OIL_WELL.get());
        ((OilWellBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(WELL))).start(FULL_YIELD);
        PFBlocks.PUMPJACK_FOOTPRINT.placeAll(helper.getLevel(), helper.absolutePos(WELL.above()), Direction.NORTH);
    }

    private static FluidMachineBlockEntity placeRefinery(GameTestHelper helper) {
        Footprint footprint = FluidMachines.OIL_REFINERY.footprint();
        List<BlockPos> positions = footprint.positions(helper.absolutePos(ORIGIN), FACING);
        for (int i = 0; i < positions.size(); i++) {
            helper.getLevel().setBlock(positions.get(i), footprint.stateAt(i, FACING), Block.UPDATE_ALL);
        }
        return helper.getBlockEntity(ORIGIN, FluidMachineBlockEntity.class);
    }

    private static void hold(GameTestHelper helper, FluidMachineBlockEntity refinery) {
        refinery.setHeldRecipe(ADVANCED, helper.makeMockPlayer(GameType.SURVIVAL));
        if (refinery.heldRecipe().isEmpty()) {
            helper.fail(ADVANCED + " was not held, so this proves nothing", ORIGIN);
        }
    }

    private static BlockPos relative(GameTestHelper helper, BlockPos absolute) {
        return absolute.subtract(helper.absolutePos(BlockPos.ZERO));
    }

    private static void fill(GameTestHelper helper, BlockPos tank, FluidResource fluid, int amount) {
        ResourceHandler<FluidResource> segment = FluidPorts.segment(helper.getLevel(), tank);
        if (segment == null) {
            helper.fail("the tank is in no segment", relative(helper, tank));
            return;
        }
        try (Transaction tx = Transaction.openRoot()) {
            segment.insert(0, fluid, amount, tx);
            tx.commit();
        }
    }

    private static Fluid fluid(String id) {
        return BuiltInRegistries.FLUID.getValue(Identifier.parse(id));
    }
}
