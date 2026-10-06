package com.factoryworks.core.gametest;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.craftworks.machine.ChemicalPlantBlockEntity;
import io.github._5thlayer.craftworks.machine.ChemicalPlantSlots;
import io.github._5thlayer.craftworks.machine.ChemicalPlants;
import io.github._5thlayer.groundworks.Footprint;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * What the Pack owns of the Chemical Plant, which is Craftworks' (ADR-0123): that it takes its fluid
 * from Pipeworks pipes and puts its fluid result into a pipe that leads to a tank. The plant's own
 * behaviour is Craftworks' GameTests, whose calls these reach into until craftworks#37 names a
 * Consumer API. The recipes are the Pack's, typed here for {@code BoilerTests}' reason.
 */
final class ChemicalPlantTests {

    private static final BlockPos ORIGIN = new BlockPos(8, 1, 3);
    private static final Direction FACING = Direction.EAST;
    private static final BlockPos POLE = ORIGIN.south(3);

    private static final Identifier PLASTIC = Identifier.parse("factoryworks:chemistry/plastic_bar");
    private static final Identifier SULFURIC_ACID = Identifier.parse("factoryworks:chemistry/sulfuric_acid");

    private static final int GAS_PER_CRAFT = 20;
    private static final int ACID_PER_CRAFT = 50;
    /** More than the plant's input box holds, so the supply pipes stay full of water. */
    private static final int SUPPLY = 5_000;
    private static final int TICKS_PER_CRAFT = 20;

    private static final int PIPES = 2;

    private ChemicalPlantTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("a_chemical_plant_makes_plastic_from_petroleum_gas_in_a_pipe", 160,
                ChemicalPlantTests::makesPlasticFromPipedGas);
        tests.test("a_chemical_plant_sends_sulfuric_acid_through_a_pipe_to_a_tank", 160,
                ChemicalPlantTests::sendsAcidToATank);
    }

    /** Gas in a two-pipe segment at one connection, coal in the slot and a pole beside: plastic comes out. */
    private static void makesPlasticFromPipedGas(GameTestHelper helper) {
        ChemicalPlantBlockEntity plant = place(helper);
        hold(helper, plant, PLASTIC);
        Connection in = connections(helper).stream().filter(c -> c.side() == FACING).findFirst().orElseThrow();
        List<BlockPos> pipes = pipesFrom(helper, in);
        int gas = PIPES * 100;

        helper.startSequence()
                .thenExecute(() -> {
                    plant.inventory().set(0, ItemResource.of(item("minecraft:coal")), 8);
                    helper.setBlock(POLE, LibraryBlocks.creativePole());
                })
                .thenIdle(PIPES)
                .thenExecute(() -> fill(helper, pipes.getFirst(), FluidResource.of(fluid("factoryworks:petroleum_gas")), gas))
                .thenIdle(ChassisFixture.RESCAN_INTERVAL + 5 + 3 * TICKS_PER_CRAFT)
                .thenExecute(() -> {
                    int plastic = plant.inventory().getAmountAsInt(ChemicalPlantSlots.PRODUCT);
                    if (plastic < 2 || plastic % 2 != 0) {
                        helper.fail("a Chemical Plant on piped gas made " + plastic + " plastic, expected whole crafts of 2",
                                ORIGIN);
                    }
                    if (!plant.inventory().getResource(ChemicalPlantSlots.PRODUCT).equals(
                            ItemResource.of(item("factoryworks:plastic_bar")))) {
                        helper.fail("a Chemical Plant's product is "
                                + plant.inventory().getResource(ChemicalPlantSlots.PRODUCT), ORIGIN);
                    }
                    int taken = gas - segmentAmount(helper, pipes.getFirst());
                    if (taken < plastic / 2 * GAS_PER_CRAFT) {
                        helper.fail("the pipes gave " + taken + " mB of gas for " + plastic + " plastic", ORIGIN);
                    }
                })
                .thenSucceed();
    }

    /**
     * Water from a tank through one connection, sulfur and iron in the slots, and the acid leaves by the
     * opposite edge. The supply is a tank so its pipes never run dry: a connection has no direction, and
     * an emptied pipe takes the acid.
     */
    private static void sendsAcidToATank(GameTestHelper helper) {
        ChemicalPlantBlockEntity plant = place(helper);
        hold(helper, plant, SULFURIC_ACID);
        List<Connection> connections = connections(helper);
        Connection in = connections.stream().filter(c -> c.side() == FACING).findFirst().orElseThrow();
        Connection out = connections.stream().filter(c -> c.side() == FACING.getOpposite()).findFirst().orElseThrow();
        List<BlockPos> water = pipesFrom(helper, in);
        List<BlockPos> acid = pipesFrom(helper, out);
        BlockPos tank = acid.getLast().relative(out.side());
        BlockPos supply = water.getLast().relative(in.side());
        helper.getLevel().setBlockAndUpdate(tank, LibraryBlocks.storageTank().defaultBlockState());
        helper.getLevel().setBlockAndUpdate(supply, LibraryBlocks.storageTank().defaultBlockState());

        helper.startSequence()
                .thenExecute(() -> {
                    plant.inventory().set(0, ItemResource.of(item("factoryworks:sulfur")), 10);
                    plant.inventory().set(1, ItemResource.of(item("factoryworks:iron_plate")), 2);
                    helper.setBlock(POLE, LibraryBlocks.creativePole());
                })
                .thenIdle(PIPES)
                .thenExecute(() -> fill(helper, supply, FluidResource.of(Fluids.WATER), SUPPLY))
                .thenIdle(ChassisFixture.RESCAN_INTERVAL + 5 + 3 * TICKS_PER_CRAFT)
                .thenExecute(() -> {
                    ResourceHandler<FluidResource> stored = FluidPorts.segment(helper.getLevel(), tank);
                    if (stored == null) {
                        helper.fail("the tank is in no segment", relative(helper, tank));
                        return;
                    }
                    int amount = stored.getAmountAsInt(0);
                    if (amount < ACID_PER_CRAFT || amount % ACID_PER_CRAFT != 0
                            || !stored.getResource(0).equals(FluidResource.of(fluid("factoryworks:sulfuric_acid")))) {
                        helper.fail("the tank holds " + amount + " mB of " + stored.getResource(0)
                                + ", expected whole crafts of " + ACID_PER_CRAFT + " mB of sulfuric acid",
                                relative(helper, tank));
                    }
                })
                .thenSucceed();
    }

    /** A footprint block that is a Fluid Connection, and the way its face points: absolute. */
    private record Connection(BlockPos block, Direction side) {

        BlockPos beyond() {
            return block.relative(side);
        }
    }

    /**
     * The connections the held recipe gives the plant, found as a pipe finds them: by asking each side of
     * each block for a fluid face. Four, or the recipe names no fluid.
     */
    private static List<Connection> connections(GameTestHelper helper) {
        List<Connection> found = new ArrayList<>();
        Footprint footprint = ChemicalPlants.footprint();
        for (BlockPos block : footprint.positions(helper.absolutePos(ORIGIN), FACING)) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, block, side) != null) {
                    found.add(new Connection(block, side));
                }
            }
        }
        if (found.size() != 4) {
            helper.fail("the plant has " + found.size() + " fluid connections, expected 4", ORIGIN);
        }
        return found;
    }

    /** {@link #PIPES} pipes in a line out of the connection, the first against it, absolute. */
    private static List<BlockPos> pipesFrom(GameTestHelper helper, Connection connection) {
        List<BlockPos> pipes = new ArrayList<>();
        BlockPos at = connection.beyond();
        for (int i = 0; i < PIPES; i++) {
            helper.getLevel().setBlockAndUpdate(at, LibraryBlocks.pipe().defaultBlockState());
            pipes.add(at);
            at = at.relative(connection.side());
        }
        return pipes;
    }

    private static BlockPos relative(GameTestHelper helper, BlockPos absolute) {
        return absolute.subtract(helper.absolutePos(BlockPos.ZERO));
    }

    private static void fill(GameTestHelper helper, BlockPos pipe, FluidResource fluid, int amount) {
        ResourceHandler<FluidResource> segment = FluidPorts.segment(helper.getLevel(), pipe);
        if (segment == null) {
            helper.fail("the pipe is in no segment", relative(helper, pipe));
            return;
        }
        try (Transaction tx = Transaction.openRoot()) {
            segment.insert(0, fluid, amount, tx);
            tx.commit();
        }
    }

    private static int segmentAmount(GameTestHelper helper, BlockPos pipe) {
        return FluidPorts.segment(helper.getLevel(), pipe).getAmountAsInt(0);
    }

    private static ChemicalPlantBlockEntity place(GameTestHelper helper) {
        Footprint footprint = ChemicalPlants.footprint();
        List<BlockPos> positions = footprint.positions(helper.absolutePos(ORIGIN), FACING);
        for (int i = 0; i < positions.size(); i++) {
            helper.getLevel().setBlock(positions.get(i), footprint.stateAt(i, FACING), Block.UPDATE_ALL);
        }
        return helper.getBlockEntity(ORIGIN, ChemicalPlantBlockEntity.class);
    }

    private static void hold(GameTestHelper helper, ChemicalPlantBlockEntity plant, Identifier recipe) {
        plant.setHeldRecipe(recipe, helper.makeMockPlayer(GameType.SURVIVAL));
        if (plant.heldRecipe().isEmpty()) {
            helper.fail(recipe + " was not held, so this proves nothing", ORIGIN);
        }
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    private static Fluid fluid(String id) {
        return BuiltInRegistries.FLUID.getValue(Identifier.parse(id));
    }
}
