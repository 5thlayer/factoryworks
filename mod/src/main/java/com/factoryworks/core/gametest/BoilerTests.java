package com.factoryworks.core.gametest;

import java.util.List;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.PFItems;
import com.factoryworks.core.fluid.BoilerBlockEntity;
import com.factoryworks.core.fluid.BoilerFootprint;
import com.factoryworks.core.fluid.BoilerSlots;
import com.factoryworks.core.fluid.PFFluids;

import io.github._5thlayer.pipeworks.api.FluidPorts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The Pack's claim: its Boiler, which the Pack owns, boils water into steam, and its ports stand in
 * the Pipeworks segments ADR-0114 says, through the API Pipeworks documents for a port's block entity,
 * {@link FluidPorts} (#274, #593, #627). What a segment does with a fluid is Pipeworks' own GameTests.
 *
 * <p>What a placed Boiler does in a world, and nothing that can be asked without one (#274, #224).
 *
 * <p>The Boiler's arithmetic is checked on a plain JVM: {@code BoilerSpecTest} owns the rate and
 * {@code BoilerCycleTest} the stall. What is left is that a tick with everything present produces,
 * that the footprint's ports stand in the segments ADR-0114 says, and that the fuel face reaches
 * the anchor from every part.
 *
 * <p>One Boiler on the platform's stone floor, anchor at relative y 1. Its water row and steam
 * port are Pipeworks segments, filled and read through {@link FluidPorts}.
 */
final class BoilerTests {

    /** The Boiler. Centre of the platform, clear of every edge. */
    private static final BlockPos BOILER = new BlockPos(3, 1, 3);

    /** The floor the footprint tests click, with room for the 3x2 around the anchor above it. */
    private static final BlockPos FLOOR = new BlockPos(3, 0, 3);

    /**
     * What a whole tick converts, in millibuckets: Factorio's 60 a second over Minecraft's twenty
     * ticks.
     *
     * <p>Stated as a literal rather than read off {@code BoilerSpec}, for the reason
     * {@link EnergyFaceTests}' furnace demand is: reading it off the spec would make this test
     * agree with the spec by construction. {@code BoilerSpecTest} owns the derivation, and what is
     * checked here is that the number survives the trip through a running block entity.
     */
    private static final int MILLIBUCKETS_PER_TICK = 3;

    private static final Direction FACING = Direction.NORTH;

    /** Ticks for the ports to join their segments after placement. */
    private static final int PORTS_JOIN = 2;

    /** How many ticks of boiling the window is measured over. */
    private static final int WINDOW = 10;

    private BoilerTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("boiler_boils", 100, BoilerTests::boils);
        tests.test("boiler_ports_stand_in_two_segments", 20, BoilerTests::portsStandInTwoSegments);
        tests.test("boiler_item_face_takes_fuel_only", 100, BoilerTests::itemFaceTakesFuelOnly);
        tests.test("boiler_has_no_fluid_face", 20, BoilerTests::hasNoFluidFace);
        tests.test("boiler_item_places_the_whole_footprint", 20, BoilerTests::placedWhole);
        tests.test("boiler_item_refused_when_one_position_is_blocked", 20, BoilerTests::refusedWhenBlocked);
        tests.test("boiler_parts_reach_the_anchors_faces", 20, BoilerTests::partsReachTheAnchor);
    }

    /**
     * Water in, fuel in, room in the steam segment: it boils, and the water falls to match.
     *
     * <p>Unit for unit is asserted as well as the rate, because Factorio's boiler is a temperature
     * change rather than a reaction: a tick that made steam without spending the same water would
     * be creating it, which is the other half of ADR-0050's rule.
     */
    private static void boils(GameTestHelper helper) {
        place(helper);
        int[] water = new int[1];
        int[] steam = new int[1];
        helper.startSequence()
                .thenIdle(PORTS_JOIN)
                .thenExecute(() -> {
                    boiler(helper).setItem(BoilerSlots.FUEL, new ItemStack(Items.COAL, 8));
                    fill(waterRow(helper), water(), 600);
                })
                // One tick to let it light, so the window below is whole ticks of boiling rather
                // than the tick that paid for the first of them.
                .thenIdle(1)
                .thenExecute(() -> {
                    water[0] = amount(waterRow(helper));
                    steam[0] = amount(steamPort(helper));
                })
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    int made = amount(steamPort(helper)) - steam[0];
                    if (made != MILLIBUCKETS_PER_TICK * WINDOW) {
                        helper.fail("boiler made " + made + " mB of steam over " + WINDOW
                                + " ticks, expected " + (MILLIBUCKETS_PER_TICK * WINDOW), BOILER);
                    }
                    int spent = water[0] - amount(waterRow(helper));
                    if (spent != made) {
                        helper.fail("boiler spent " + spent + " mB of water making " + made
                                + " mB of steam", BOILER);
                    }
                    if (amount(steamPort(helper)) <= 0) {
                        helper.fail("nothing to measure: the steam segment is still empty", BOILER);
                    }
                })
                .thenSucceed();
    }

    /**
     * The three front blocks are one water segment of three 200 mB ports, and the back middle is a
     * segment of its own: water put in the row reaches every front block and not the steam port,
     * which is what stops a pipe at the back laundering water through a machine that consumes it.
     * The back corners are in none.
     */
    private static void portsStandInTwoSegments(GameTestHelper helper) {
        place(helper);
        helper.startSequence()
                .thenIdle(PORTS_JOIN)
                .thenExecute(() -> {
                    List<BlockPos> blocks = PFBlocks.BOILER_FOOTPRINT.positions(helper.absolutePos(BOILER), FACING);
                    ResourceHandler<FluidResource> row = waterRow(helper);
                    ResourceHandler<FluidResource> steam = steamPort(helper);
                    if (row == null || steam == null) {
                        helper.fail("the row or the steam port is in no segment", BOILER);
                        return;
                    }
                    fill(row, water(), 100);
                    for (int part : new int[] {1, 2}) {
                        ResourceHandler<FluidResource> end = FluidPorts.segment(helper.getLevel(), blocks.get(part));
                        if (end == null || end.getAmountAsInt(0) != 100) {
                            helper.fail("front part " + part + " is not in the row's segment", helper.relativePos(blocks.get(part)));
                        }
                    }
                    if (amount(steam) != 0 || steam.getCapacityAsInt(0, steam()) != 200) {
                        helper.fail("the steam port holds " + amount(steam) + " of " + steam.getCapacityAsInt(0, steam())
                                + " mB: it shares the row's segment or is not 200 mB", BOILER);
                    }
                    if (row.getCapacityAsInt(0, water()) != 600) {
                        helper.fail("the water row holds " + row.getCapacityAsInt(0, water()) + " mB, not three ports of 200", BOILER);
                    }
                    for (int corner : new int[] {3, 5}) {
                        if (FluidPorts.segment(helper.getLevel(), blocks.get(corner)) != null) {
                            helper.fail("back corner " + corner + " is in a segment", helper.relativePos(blocks.get(corner)));
                        }
                    }
                })
                .thenSucceed();
    }

    /** Water and steam leave through Pipeworks only: no block of the footprint answers a fluid capability. */
    private static void hasNoFluidFace(GameTestHelper helper) {
        place(helper);
        for (BlockPos at : PFBlocks.BOILER_FOOTPRINT.positions(helper.absolutePos(BOILER), FACING)) {
            if (helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, at, null) != null) {
                helper.fail("a Boiler block answers a fluid capability", helper.relativePos(at));
            }
        }
        helper.succeed();
    }

    /**
     * Fuel in, and nothing at all back out.
     *
     * <p>The Boiler's only item is the one it is burning, and a funnel that could take it back
     * would be pulling the coal out from under the machine mid-tick.
     */
    private static void itemFaceTakesFuelOnly(GameTestHelper helper) {
        place(helper);
        ItemResource coal = ItemResource.of(new ItemStack(Items.COAL));
        ResourceHandler<ItemResource> face = Faces.item(helper, BOILER);
        if (face == null) {
            helper.fail("the Boiler has no item capability: it is inert", BOILER);
            return;
        }

        Faces.expectMoved(helper, BOILER, "coal into the item face", 1, face, (f, tx) -> f.insert(coal, 1, tx));
        if (boiler(helper).getItem(BoilerSlots.FUEL).getCount() != 1) {
            helper.fail("the inserted coal did not reach the fuel slot", BOILER);
        }
        int given = Faces.simulate(face, (f, tx) -> f.extract(coal, 1, tx));
        if (given != 0) {
            helper.fail("a funnel pulled " + given + " coal back out of the Boiler", BOILER);
        }
        helper.succeed();
    }

    /** The footprint stands whole from one click, one item spent (#592). */
    private static void placedWhole(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PFItems.BOILER.get()));
        click(helper, player);

        BlockState anchor = helper.getLevel().getBlockState(helper.absolutePos(FLOOR.above()));
        if (!PFBlocks.BOILER_FOOTPRINT.isAnchor(anchor)) {
            helper.fail("the click put no Boiler anchor on the floor", FLOOR.above());
            return;
        }
        List<BlockPos> positions = PFBlocks.BOILER_FOOTPRINT.positions(helper.absolutePos(FLOOR.above()),
                anchor.getValue(HorizontalDirectionalBlock.FACING));
        if (positions.size() != 6 || positions.stream().map(BlockPos::getY).distinct().count() != 1) {
            helper.fail("the footprint is " + positions.size() + " blocks on "
                    + positions.stream().map(BlockPos::getY).distinct().count() + " layers, not 6 on 1", FLOOR.above());
        }
        for (int i = 1; i < positions.size(); i++) {
            if (!helper.getLevel().getBlockState(positions.get(i)).is(PFBlocks.BOILER_PART.get())) {
                helper.fail("position " + i + " of the footprint is not a Boiler part",
                        helper.relativePos(positions.get(i)));
            }
        }
        if (!player.getMainHandItem().isEmpty()) {
            helper.fail("placing the Boiler did not spend its item", FLOOR.above());
        }
        helper.succeed();
    }

    /** One taken position refuses the whole machine and spends nothing (ADR-0069). */
    private static void refusedWhenBlocked(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PFItems.BOILER.get()));
        BlockPos anchor = helper.absolutePos(FLOOR.above());
        Direction facing = player.getDirection().getOpposite();
        BlockPos corner = PFBlocks.BOILER_FOOTPRINT.positions(anchor, facing).getLast();
        helper.getLevel().setBlockAndUpdate(corner, Blocks.STONE.defaultBlockState());
        click(helper, player);

        for (BlockPos pos : PFBlocks.BOILER_FOOTPRINT.positions(anchor, facing)) {
            BlockState state = helper.getLevel().getBlockState(pos);
            if (state.is(PFBlocks.BOILER.get()) || state.is(PFBlocks.BOILER_PART.get())) {
                helper.fail("a blocked footprint still placed " + state.getBlock(), helper.relativePos(pos));
            }
        }
        if (player.getMainHandItem().getCount() != 1) {
            helper.fail("a refused Boiler spent its item", FLOOR.above());
        }
        helper.succeed();
    }

    /** A hopper on any part finds the anchor's fuel slot (#592). */
    private static void partsReachTheAnchor(GameTestHelper helper) {
        BlockPos anchor = helper.absolutePos(FLOOR.above());
        PFBlocks.BOILER_FOOTPRINT.placeAll(helper.getLevel(), anchor, Direction.NORTH);

        ItemResource coal = ItemResource.of(new ItemStack(Items.COAL));
        List<BlockPos> positions = PFBlocks.BOILER_FOOTPRINT.positions(anchor, Direction.NORTH);
        for (int i = 1; i < positions.size(); i++) {
            BlockPos absolute = positions.get(i);
            BlockPos part = helper.relativePos(absolute);
            ResourceHandler<ItemResource> item =
                    helper.getLevel().getCapability(Capabilities.Item.BLOCK, absolute, null);
            if (item == null) {
                helper.fail("part " + i + " has no item face", part);
                return;
            }
            if (Faces.simulate(item, (f, tx) -> f.insert(coal, 1, tx)) <= 0) {
                helper.fail("part " + i + " did not reach the anchor's fuel slot", part);
            }
        }
        helper.succeed();
    }

    private static void click(GameTestHelper helper, Player player) {
        BlockPos floor = helper.absolutePos(FLOOR);
        helper.useBlock(FLOOR, player, new BlockHitResult(
                Vec3.atCenterOf(floor).relative(Direction.UP, 0.5), Direction.UP, floor, false));
    }

    // -- the plumbing ---------------------------------------------------------------------------

    private static void place(GameTestHelper helper) {
        PFBlocks.BOILER_FOOTPRINT.placeAll(helper.getLevel(), helper.absolutePos(BOILER), FACING);
    }

    private static BoilerBlockEntity boiler(GameTestHelper helper) {
        return helper.getBlockEntity(BOILER, BoilerBlockEntity.class);
    }

    private static ResourceHandler<FluidResource> waterRow(GameTestHelper helper) {
        return FluidPorts.segment(helper.getLevel(), helper.absolutePos(BOILER));
    }

    private static ResourceHandler<FluidResource> steamPort(GameTestHelper helper) {
        return FluidPorts.segment(helper.getLevel(),
                PFBlocks.BOILER_FOOTPRINT.positions(helper.absolutePos(BOILER), FACING).get(BoilerFootprint.STEAM_PART));
    }

    private static int amount(ResourceHandler<FluidResource> segment) {
        return segment == null ? 0 : segment.getAmountAsInt(0);
    }

    private static void fill(ResourceHandler<FluidResource> segment, FluidResource fluid, int amount) {
        try (Transaction tx = Transaction.openRoot()) {
            segment.insert(fluid, amount, tx);
            tx.commit();
        }
    }

    private static FluidResource water() {
        return FluidResource.of(Fluids.WATER);
    }

    private static FluidResource steam() {
        return FluidResource.of(PFFluids.STEAM_SOURCE.get());
    }
}
