package com.planetaryfactory.core.gametest;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.fluid.BoilerBlockEntity;
import com.planetaryfactory.core.fluid.BoilerSlots;
import com.planetaryfactory.core.fluid.PFFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

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
        ResourceHandler<FluidResource> face = fluidFace(helper);

        int waterIn = simulateInsert(face, water);
        if (waterIn <= 0) {
            helper.fail("the fluid face refused water on the way in", BOILER);
        }
        int steamIn = simulateInsert(face, steam);
        if (steamIn != 0) {
            helper.fail("the fluid face took " + steamIn + " mB of steam on the way in", BOILER);
        }
        int steamOut = simulateExtract(face, steam);
        if (steamOut <= 0) {
            helper.fail("the fluid face refused steam on the way out", BOILER);
        }
        int waterOut = simulateExtract(face, water);
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
        ResourceHandler<ItemResource> face = itemFace(helper);

        try (Transaction tx = Transaction.openRoot()) {
            int taken = face.insert(coal, 1, tx);
            if (taken != 1) {
                helper.fail("the item face took " + taken + " coal, expected 1", BOILER);
            }
            tx.commit();
        }
        if (boiler(helper).getItem(BoilerSlots.FUEL).getCount() != 1) {
            helper.fail("the inserted coal did not reach the fuel slot", BOILER);
        }
        int given = simulateExtract(face, coal);
        if (given != 0) {
            helper.fail("a funnel pulled " + given + " coal back out of the Boiler", BOILER);
        }
        helper.succeed();
    }

    // -- the plumbing ---------------------------------------------------------------------------

    private static <T extends Resource> int simulateInsert(
            ResourceHandler<T> face, T resource) {
        try (Transaction tx = Transaction.openRoot()) {
            return face.insert(resource, 1, tx);
        }
    }

    private static <T extends Resource> int simulateExtract(
            ResourceHandler<T> face, T resource) {
        try (Transaction tx = Transaction.openRoot()) {
            return face.extract(resource, 1, tx);
        }
    }

    /** The fluid face as a pipe finds it: through the capability, not off the block entity. */
    private static ResourceHandler<FluidResource> fluidFace(GameTestHelper helper) {
        ResourceHandler<FluidResource> face = helper.getLevel().getCapability(
                Capabilities.Fluid.BLOCK, helper.absolutePos(BOILER), null);
        if (face == null) {
            helper.fail("the Boiler has no fluid capability: it is inert", BOILER);
        }
        return face;
    }

    private static ResourceHandler<ItemResource> itemFace(GameTestHelper helper) {
        ResourceHandler<ItemResource> face = helper.getLevel().getCapability(
                Capabilities.Item.BLOCK, helper.absolutePos(BOILER), null);
        if (face == null) {
            helper.fail("the Boiler has no item capability: it is inert", BOILER);
        }
        return face;
    }

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
