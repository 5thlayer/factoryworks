package com.factoryworks.core.fluid;

import com.factoryworks.core.PFBlockEntities;
import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.smelting.FuelBuffer;
import com.factoryworks.core.smelting.PFFuel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import io.github._5thlayer.pipeworks.api.FluidPorts;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Boiler running (#224, ADR-0048): solid fuel and water in, low-temperature steam out.
 *
 * <p><b>The third customer of the burner model</b> the Furnace and the Burner Mining Drill already
 * share (ADR-0047). Nothing about fuel is re-derived here: {@link PFFuel} says what an item is
 * worth, {@link FuelBuffer} banks it whole and spends it a tick at a time, and the fuel table is
 * generated datapack JSON rather than Forge's burn table -- so what burns in a Boiler is exactly
 * what burns in a Stone Furnace, by construction.
 *
 * <p>The two things this class must get right, both of which are {@link BoilerCycle}'s to decide
 * and this class's to feed honestly:
 *
 * <ol>
 *   <li><b>The stall.</b> A full steam segment makes no steam, burns no fuel and voids none, and it
 *       resumes the moment a pipe drains it (#224).
 *   <li><b>Water is consumed, never created.</b> Under ADR-0050 every drop comes from an Offshore
 *       Pump.
 * </ol>
 *
 * <p>It draws from the water segment its anchor stands in and fills the steam segment its back
 * middle part stands in (ADR-0114, #593). The rate is read, not chosen -- {@link SteamChainCorpus}
 * into {@link BoilerSpec}.
 */
public class BoilerBlockEntity extends BoilerPortBlockEntity implements Container, MenuProvider {

    public static final int DATA_FUEL = 0;
    public static final int DATA_FUEL_CAPACITY = 1;
    public static final int DATA_WATER = 2;
    public static final int DATA_STEAM = 3;
    public static final int DATA_WATER_CAPACITY = 4;
    public static final int DATA_STEAM_CAPACITY = 5;
    public static final int DATA_COUNT = 6;

    private static final FluidResource WATER = FluidResource.of(Fluids.WATER);

    private static FluidResource steam() {
        return FluidResource.of(PFFluids.STEAM_SOURCE.get());
    }

    /** Every figure below is the prototype's, resolved once at class-init. */
    private static final SteamChainCorpus CORPUS = SteamChainCorpus.get();

    private static final long JOULES_PER_TICK =
            BoilerSpec.joulesPerTick(CORPUS.boilerEnergyConsumption());
    private static final long JOULES_PER_MILLIBUCKET = BoilerSpec.joulesPerMilliBucket(
            CORPUS.boilerTargetTemperature(),
            CORPUS.fluidDefaultTemperature("water"),
            // Steam's, not water's. BoilerSpec is where that trap is written down.
            CORPUS.fluidHeatCapacity("steam"));
    /** ADR-0047's multiplier on the way *in* to the buffer. Factorio's is 1. */
    private static final double EFFECTIVITY = CORPUS.boilerEffectivity();

    public static int steamPerSecond() {
        return BoilerSpec.milliBucketsPerSecond(CORPUS.boilerEnergyConsumption(), JOULES_PER_MILLIBUCKET);
    }

    private static final int MILLIBUCKETS_PER_TICK =
            BoilerSpec.milliBucketsPerTick(JOULES_PER_TICK, JOULES_PER_MILLIBUCKET);

    private final NonNullList<ItemStack> items = NonNullList.withSize(BoilerSlots.SIZE, ItemStack.EMPTY);
    private final FuelBuffer fuel = new FuelBuffer();

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_FUEL -> clampToInt(fuel.storedJoules());
                case DATA_FUEL_CAPACITY -> clampToInt(fuel.gaugeCapacity());
                case DATA_WATER -> amountOf(waterRow(), WATER);
                case DATA_STEAM -> amountOf(steamSegment(), steam());
                case DATA_WATER_CAPACITY -> capacityOf(waterRow(), WATER);
                case DATA_STEAM_CAPACITY -> capacityOf(steamSegment(), steam());
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_FUEL -> fuel.load(value, fuel.lastLitJoules());
                case DATA_FUEL_CAPACITY -> fuel.load(fuel.storedJoules(), value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    /** Saturating, for the reason the furnace's is: a wrapped int draws a full gauge as an empty
     * one, and nobody traces that back to a cast. */
    private static int clampToInt(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value));
    }

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.BOILER.get(), pos, state);
    }

    @Override
    BoilerFootprint.Port port() {
        return BoilerFootprint.Port.WATER;
    }

    public ContainerData data() {
        return data;
    }

    /** What a tick of boiling costs, for the screen's fuel tooltip. */
    public static long joulesPerTick() {
        return JOULES_PER_TICK;
    }

    // -- the operation --------------------------------------------------------------------------

    public void serverTick() {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ResourceHandler<FluidResource> input = waterRow();
        ResourceHandler<FluidResource> output = steamSegment();
        int converted = BoilerCycle.tick(
                amountOf(input, WATER),
                Math.max(0, capacityOf(output, steam()) - amountOf(output, steam())),
                MILLIBUCKETS_PER_TICK,
                JOULES_PER_TICK,
                fuel,
                this::light);
        if (converted <= 0) {
            return;
        }
        try (Transaction tx = Transaction.openRoot()) {
            // Unit for unit: Factorio's boiler is a temperature change, not a reaction.
            if (input.extract(WATER, converted, tx) == converted && output.insert(steam(), converted, tx) == converted) {
                tx.commit();
            }
        }
        setChanged();
    }

    private @Nullable ResourceHandler<FluidResource> waterRow() {
        return FluidPorts.segment(level, worldPosition);
    }

    private @Nullable ResourceHandler<FluidResource> steamSegment() {
        Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        BlockPos steamPort = PFBlocks.BOILER_FOOTPRINT.positions(worldPosition, facing).get(BoilerFootprint.STEAM_PART);
        return FluidPorts.segment(level, steamPort);
    }

    private static int amountOf(@Nullable ResourceHandler<FluidResource> segment, FluidResource fluid) {
        return segment != null && segment.getResource(0).equals(fluid) ? segment.getAmountAsInt(0) : 0;
    }

    private static int capacityOf(@Nullable ResourceHandler<FluidResource> segment, FluidResource fluid) {
        return segment == null ? 0 : segment.getCapacityAsInt(0, fluid);
    }

    /**
     * Consume one fuel item whole and report what it was worth, the rig's own idiom.
     *
     * <p>ADR-0047's rule in full: {@code fuel_value * effectivity} joules into the buffer. The
     * Boiler's effectivity is 1, so the multiplication changes nothing today -- it is here so that
     * the rule is executed rather than assumed, and a prototype that ever stated otherwise would
     * be obeyed instead of silently ignored.
     */
    private long light() {
        ItemStack stack = items.get(BoilerSlots.FUEL);
        long joules = PFFuel.joules(stack);
        if (joules <= 0L) {
            return 0L;
        }
        stack.shrink(1);
        return Math.round(joules * EFFECTIVITY);
    }

    /** Whether the generated fuel table names this stack (ADR-0047). Default-deny. */
    public boolean isFuel(ItemStack stack) {
        return PFFuel.joules(stack) > 0L;
    }

    // -- the container --------------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return BoilerSlots.SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.get(BoilerSlots.FUEL).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == BoilerSlots.FUEL ? items.get(BoilerSlots.FUEL) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == BoilerSlots.insertionSlot(isFuel(stack));
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BoilerMenu(containerId, playerInventory, this, data);
    }

    // -- persistence ----------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        fuel.load(input.getLongOr("FuelJoules", 0L), input.getLongOr("FuelLitJoules", 0L));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putLong("FuelJoules", fuel.storedJoules());
        output.putLong("FuelLitJoules", fuel.lastLitJoules());
    }
}
