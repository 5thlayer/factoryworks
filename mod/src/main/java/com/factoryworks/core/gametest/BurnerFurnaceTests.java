package com.factoryworks.core.gametest;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.smelting.FurnaceBlockEntity;
import com.factoryworks.core.smelting.FurnaceSlots;
import com.factoryworks.core.smelting.FurnaceTier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** A Stone and a Steel Furnace smelt in a world on coal (#432, ADR-0047, ADR-0041). */
final class BurnerFurnaceTests {

    private static final BlockPos FURNACE = new BlockPos(3, 1, 3);

    // Typed rather than read off FurnaceTier, so the test cannot agree with the enum by construction.
    private static final long JOULES_PER_TICK = 4_500L;
    private static final long COAL_JOULES = 4_000_000L;

    private static final int SMELT_WINDOW = 150;
    private static final int STALL_WINDOW = 40;

    private static final Identifier IRON_PLATE = Identifier.parse("factoryworks:iron_plate");
    private static final Identifier STEEL_PLATE = Identifier.parse("factoryworks:steel_plate");

    private BurnerFurnaceTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        burner(tests, FurnaceTier.STONE, 64, 320);
        burner(tests, FurnaceTier.STEEL, 32, 160);
    }

    private static void burner(PFGameTests.Registrar tests, FurnaceTier tier, int ironTicks, int steelTicks) {
        String name = tier.blockName();
        tests.test(name + "_smelts_iron_on_coal", SMELT_WINDOW + 50, helper -> smeltsIron(helper, tier, ironTicks));
        tests.test(name + "_keeps_the_steel_count", steelTicks + 60, helper -> keepsSteelCount(helper, tier));
        tests.test(name + "_blocked_output_stalls", STALL_WINDOW + 60, helper -> blockedOutputStalls(helper, tier));
    }

    /**
     * Every working tick costs 4,500 J and advances the smelt by one, so the joules spent name the
     * ticks worked, and the plates and progress must be exactly those ticks at the tier's rate.
     * Read as one snapshot, which keeps it independent of where the test's tick falls.
     */
    private static void smeltsIron(GameTestHelper helper, FurnaceTier tier, int ironTicks) {
        helper.setBlock(FURNACE, PFBlocks.furnace(tier).get());
        furnace(helper).setItem(FurnaceSlots.INPUT, new ItemStack(Items.RAW_IRON, 64));
        furnace(helper).setItem(FurnaceSlots.FUEL, new ItemStack(Items.COAL, 1));
        helper.startSequence()
                .thenIdle(SMELT_WINDOW)
                .thenExecute(() -> {
                    FurnaceBlockEntity furnace = furnace(helper);
                    if (!furnace.getItem(FurnaceSlots.FUEL).isEmpty()) {
                        helper.fail("the coal was never lit", FURNACE);
                    }
                    long spent = COAL_JOULES - joules(helper);
                    if (spent % JOULES_PER_TICK != 0L) {
                        helper.fail(spent + " J spent, not a whole number of " + JOULES_PER_TICK
                                + " J ticks", FURNACE);
                    }
                    long worked = spent / JOULES_PER_TICK;
                    ItemStack output = furnace.getItem(FurnaceSlots.OUTPUT);
                    if (!output.isEmpty() && !output.is(item(IRON_PLATE))) {
                        helper.fail("output holds " + output + ", expected iron plate", FURNACE);
                    }
                    long plates = output.getCount();
                    int progress = furnace.data().get(FurnaceBlockEntity.DATA_PROGRESS);
                    if (plates != worked / ironTicks || progress != worked % ironTicks) {
                        helper.fail(worked + " ticks worked at " + ironTicks + " a plate should be "
                                + worked / ironTicks + " plates and " + worked % ironTicks
                                + " ticks in, found " + plates + " and " + progress, FURNACE);
                    }
                    // Two ticks of slack for where the placement and this read fall in the tick.
                    if (worked < SMELT_WINDOW - 2) {
                        helper.fail("worked " + worked + " of " + SMELT_WINDOW + " ticks", FURNACE);
                    }
                    if (furnace.getItem(FurnaceSlots.INPUT).getCount() != 64 - plates) {
                        helper.fail("input holds " + furnace.getItem(FurnaceSlots.INPUT).getCount()
                                + " raw iron after " + plates + " plates", FURNACE);
                    }
                })
                .thenSucceed();
    }

    /**
     * Seven plates in: the first steel plate must leave two behind. A 1:1 smelt never shows one steel
     * plate beside two iron, so it times out rather than passing.
     */
    private static void keepsSteelCount(GameTestHelper helper, FurnaceTier tier) {
        helper.setBlock(FURNACE, PFBlocks.furnace(tier).get());
        furnace(helper).setItem(FurnaceSlots.INPUT, new ItemStack(item(IRON_PLATE), 7));
        furnace(helper).setItem(FurnaceSlots.FUEL, new ItemStack(Items.COAL, 1));
        helper.succeedWhen(() -> {
            ItemStack output = furnace(helper).getItem(FurnaceSlots.OUTPUT);
            ItemStack input = furnace(helper).getItem(FurnaceSlots.INPUT);
            if (!output.is(item(STEEL_PLATE)) || output.getCount() != 1 || input.getCount() != 2) {
                helper.fail("output " + output + " beside input " + input
                        + ", expected 1 steel plate beside 2 iron plates", FURNACE);
            }
        });
    }

    /**
     * A banked buffer and unlit coal behind a full output spend nothing (ADR-0041). Emptying the
     * output afterwards must start the smelt, or the stall was a missing recipe.
     */
    private static void blockedOutputStalls(GameTestHelper helper, FurnaceTier tier) {
        long banked = 100_000L;
        Item plate = item(IRON_PLATE);
        helper.setBlock(FURNACE, PFBlocks.furnace(tier).get());
        FurnaceBlockEntity placed = furnace(helper);
        placed.setItem(FurnaceSlots.INPUT, new ItemStack(Items.RAW_IRON, 8));
        placed.setItem(FurnaceSlots.FUEL, new ItemStack(Items.COAL, 1));
        placed.setItem(FurnaceSlots.OUTPUT, new ItemStack(plate, new ItemStack(plate).getMaxStackSize()));
        placed.data().set(FurnaceBlockEntity.DATA_ENERGY, (int) banked);
        int full = placed.getItem(FurnaceSlots.OUTPUT).getCount();
        helper.startSequence()
                .thenIdle(STALL_WINDOW)
                .thenExecute(() -> {
                    FurnaceBlockEntity furnace = furnace(helper);
                    if (joules(helper) != banked) {
                        helper.fail("stalled furnace burned its buffer from " + banked + " J to "
                                + joules(helper) + " J", FURNACE);
                    }
                    if (furnace.getItem(FurnaceSlots.FUEL).getCount() != 1) {
                        helper.fail("stalled furnace lit its coal", FURNACE);
                    }
                    if (furnace.data().get(FurnaceBlockEntity.DATA_PROGRESS) != 0) {
                        helper.fail("stalled furnace started a smelt", FURNACE);
                    }
                    if (furnace.getItem(FurnaceSlots.INPUT).getCount() != 8
                            || furnace.getItem(FurnaceSlots.OUTPUT).getCount() != full) {
                        helper.fail("stalled furnace moved items: input "
                                + furnace.getItem(FurnaceSlots.INPUT) + ", output "
                                + furnace.getItem(FurnaceSlots.OUTPUT), FURNACE);
                    }
                    furnace.setItem(FurnaceSlots.OUTPUT, ItemStack.EMPTY);
                })
                .thenIdle(5)
                .thenExecute(() -> {
                    if (joules(helper) >= banked) {
                        helper.fail("emptied output did not restart the smelt", FURNACE);
                    }
                })
                .thenSucceed();
    }

    private static FurnaceBlockEntity furnace(GameTestHelper helper) {
        return helper.getBlockEntity(FURNACE, FurnaceBlockEntity.class);
    }

    private static long joules(GameTestHelper helper) {
        return furnace(helper).data().get(FurnaceBlockEntity.DATA_ENERGY);
    }

    private static Item item(Identifier id) {
        return BuiltInRegistries.ITEM.getValue(id);
    }
}
