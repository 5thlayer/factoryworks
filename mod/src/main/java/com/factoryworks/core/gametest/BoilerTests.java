package com.factoryworks.core.gametest;

import java.util.List;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.PFItems;
import com.factoryworks.core.fluid.BoilerBlockEntity;
import com.factoryworks.core.fluid.BoilerSlots;
import com.factoryworks.core.fluid.PFFluids;

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

/**
 * What a placed Boiler does in a world, and nothing that can be asked without one (#274, #224).
 *
 * <h2>Why these three and no others</h2>
 *
 * <p>The Boiler's arithmetic is already checked on a plain JVM -- {@code BoilerSpecTest} owns the
 * rate and {@code BoilerCycleTest} the stall, and {@code tests/pack/test_boiler_assets.py}
 * re-derives the 60 mB/s from the corpus a second time. The two static checks
 * ({@code tests/pack/test_capability_registration.py}, {@code tests/pack/test_transfer_guards.py})
 * assert the two faces are registered and are built on {@code GuardedResourceHandler}, so both of
 * the transfer API's overloads reach them. All of that reads source text or runs without a level.
 * What is left over is that a pipe reaches the faces at all, and that a tick with everything
 * present produces.
 *
 * <p>The middle test is the one nothing static can make. The fluid face refuses <em>each direction
 * on a different tank</em>: insertion reaches the water tank and nothing else, extraction reaches
 * the steam tank and nothing else. A static check sees that a guard is present; it cannot see that
 * the two indices are the right way round. Swapped, the Boiler accepts steam it cannot use and
 * lets a pipe drain its water back out -- which would launder water through a machine that is
 * supposed to be consuming it, against ADR-0050's rule that water is extracted and never created.
 *
 * <h2>The layout</h2>
 *
 * <p>One Boiler on the platform's stone floor at relative y 0, so it stands at y 1. Nothing else:
 * the Boiler needs no neighbour to boil, and every fluid here arrives through its own face or its
 * own {@code ContainerData}, which is how its screen reads the same tanks.
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

    /** How many ticks of boiling the window is measured over. */
    private static final int WINDOW = 10;

    private BoilerTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        tests.test("boiler_boils", 100, BoilerTests::boils);
        tests.test("boiler_fluid_face_is_per_tank", 100, BoilerTests::fluidFaceIsPerTank);
        tests.test("boiler_item_face_takes_fuel_only", 100, BoilerTests::itemFaceTakesFuelOnly);
        tests.test("boiler_item_places_the_whole_footprint", 20, BoilerTests::placedWhole);
        tests.test("boiler_item_refused_when_one_position_is_blocked", 20, BoilerTests::refusedWhenBlocked);
        tests.test("boiler_parts_reach_the_anchors_faces", 20, BoilerTests::partsReachTheAnchor);
    }

    /**
     * Water in, fuel in, room in the steam tank: it boils, and the water falls to match.
     *
     * <p>Unit for unit is asserted as well as the rate, because Factorio's boiler is a temperature
     * change rather than a reaction: a tick that made steam without spending the same water would
     * be creating it, which is the other half of ADR-0050's rule.
     */
    private static void boils(GameTestHelper helper) {
        helper.setBlock(BOILER, PFBlocks.BOILER.get());
        int[] water = new int[1];
        int[] steam = new int[1];
        helper.startSequence()
                .thenExecute(() -> {
                    BoilerBlockEntity boiler = boiler(helper);
                    boiler.setItem(BoilerSlots.FUEL, new ItemStack(Items.COAL, 8));
                    setWater(helper, BoilerBlockEntity.WATER_CAPACITY);
                    setSteam(helper, 0);
                })
                // One tick to let it light, so the window below is whole ticks of boiling rather
                // than the tick that paid for the first of them.
                .thenIdle(1)
                .thenExecute(() -> {
                    water[0] = water(helper);
                    steam[0] = steam(helper);
                })
                .thenIdle(WINDOW)
                .thenExecute(() -> {
                    int made = steam(helper) - steam[0];
                    if (made != MILLIBUCKETS_PER_TICK * WINDOW) {
                        helper.fail("boiler made " + made + " mB of steam over " + WINDOW
                                + " ticks, expected " + (MILLIBUCKETS_PER_TICK * WINDOW), BOILER);
                    }
                    int spent = water[0] - water(helper);
                    if (spent != made) {
                        helper.fail("boiler spent " + spent + " mB of water making " + made
                                + " mB of steam", BOILER);
                    }
                    if (steam(helper) <= 0) {
                        helper.fail("nothing to measure: the steam tank is still empty", BOILER);
                    }
                })
                .thenSucceed();
    }

    /**
     * All four combinations, through the capability a pipe would find.
     *
     * <p>The slot-less overloads are the ones asked, deliberately: they are what a pipe calls, and
     * they are the pair a plain {@code DelegatingResourceHandler} would forward past both refusals
     * (#265). Each is asked inside a transaction that is then aborted, so no assertion here leaves
     * the tanks changed for the next one.
     */
    private static void fluidFaceIsPerTank(GameTestHelper helper) {
        helper.setBlock(BOILER, PFBlocks.BOILER.get());
        FluidResource water = FluidResource.of(Fluids.WATER);
        FluidResource steam = FluidResource.of(PFFluids.STEAM_SOURCE.get());
        // Both tanks part full, so every one of the four questions has something to say yes to and
        // room to say it in: a refusal that is really an empty tank would pass vacuously.
        setWater(helper, BoilerBlockEntity.WATER_CAPACITY / 2);
        setSteam(helper, BoilerBlockEntity.STEAM_CAPACITY / 2);
        ResourceHandler<FluidResource> face = Faces.fluid(helper, BOILER);
        if (face == null) {
            helper.fail("the Boiler has no fluid capability: it is inert", BOILER);
            return;
        }

        int waterIn = Faces.simulate(face, (f, tx) -> f.insert(water, 1, tx));
        if (waterIn <= 0) {
            helper.fail("the fluid face refused water on the way in", BOILER);
        }
        int steamIn = Faces.simulate(face, (f, tx) -> f.insert(steam, 1, tx));
        if (steamIn != 0) {
            helper.fail("the fluid face took " + steamIn + " mB of steam on the way in", BOILER);
        }
        int steamOut = Faces.simulate(face, (f, tx) -> f.extract(steam, 1, tx));
        if (steamOut <= 0) {
            helper.fail("the fluid face refused steam on the way out", BOILER);
        }
        int waterOut = Faces.simulate(face, (f, tx) -> f.extract(water, 1, tx));
        if (waterOut != 0) {
            helper.fail("a pipe drained " + waterOut + " mB of water back out of the Boiler",
                    BOILER);
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
        helper.setBlock(BOILER, PFBlocks.BOILER.get());
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

    /** A pipe or hopper on any part finds the anchor's tanks and fuel slot (#592). */
    private static void partsReachTheAnchor(GameTestHelper helper) {
        BlockPos anchor = helper.absolutePos(FLOOR.above());
        PFBlocks.BOILER_FOOTPRINT.placeAll(helper.getLevel(), anchor, Direction.NORTH);
        BoilerBlockEntity boiler = (BoilerBlockEntity) helper.getLevel().getBlockEntity(anchor);
        boiler.data().set(BoilerBlockEntity.DATA_STEAM, BoilerBlockEntity.STEAM_CAPACITY / 2);

        FluidResource water = FluidResource.of(Fluids.WATER);
        FluidResource steam = FluidResource.of(PFFluids.STEAM_SOURCE.get());
        ItemResource coal = ItemResource.of(new ItemStack(Items.COAL));
        List<BlockPos> positions = PFBlocks.BOILER_FOOTPRINT.positions(anchor, Direction.NORTH);
        for (int i = 1; i < positions.size(); i++) {
            BlockPos absolute = positions.get(i);
            BlockPos part = helper.relativePos(absolute);
            ResourceHandler<FluidResource> fluid =
                    helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, absolute, null);
            ResourceHandler<ItemResource> item =
                    helper.getLevel().getCapability(Capabilities.Item.BLOCK, absolute, null);
            if (fluid == null || item == null) {
                helper.fail("part " + i + " has no " + (fluid == null ? "fluid" : "item") + " face", part);
                return;
            }
            if (Faces.simulate(fluid, (f, tx) -> f.insert(water, 1, tx)) <= 0) {
                helper.fail("part " + i + " did not reach the anchor's water tank", part);
            }
            if (Faces.simulate(fluid, (f, tx) -> f.extract(steam, 1, tx)) <= 0) {
                helper.fail("part " + i + " did not reach the anchor's steam tank", part);
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

    private static BoilerBlockEntity boiler(GameTestHelper helper) {
        return helper.getBlockEntity(BOILER, BoilerBlockEntity.class);
    }

    /** The tanks, read and written the way the Boiler's own screen reads them. */
    private static int water(GameTestHelper helper) {
        return boiler(helper).data().get(BoilerBlockEntity.DATA_WATER);
    }

    private static int steam(GameTestHelper helper) {
        return boiler(helper).data().get(BoilerBlockEntity.DATA_STEAM);
    }

    private static void setWater(GameTestHelper helper, int millibuckets) {
        boiler(helper).data().set(BoilerBlockEntity.DATA_WATER, millibuckets);
    }

    private static void setSteam(GameTestHelper helper, int millibuckets) {
        boiler(helper).data().set(BoilerBlockEntity.DATA_STEAM, millibuckets);
    }
}
