package com.planetaryfactory.core.fluid;

import javax.annotation.Nullable;

import com.planetaryfactory.core.PFBlockEntities;
import com.planetaryfactory.core.smelting.FuelBuffer;
import com.planetaryfactory.core.smelting.PFFuel;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import com.planetaryfactory.core.transfer.GuardedResourceHandler;
import net.neoforged.neoforge.transfer.CombinedResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.transaction.Transaction;
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
 *   <li><b>The stall.</b> A full steam tank makes no steam, burns no fuel and voids none, and it
 *       resumes the moment a pipe drains it. #224 names this as the behaviour it is watching for:
 *       a boiler quietly eating coal into a full tank is a leak with no symptom at all.
 *   <li><b>Water is consumed, never created.</b> Under ADR-0050 every drop comes from an Offshore
 *       Pump. The input tank is fillable from outside and by nothing else.
 * </ol>
 *
 * <p>The rate is read, not chosen -- {@link SteamChainCorpus} into {@link BoilerSpec} -- and the
 * tanks' capacities are the prototype's own fluid boxes, at Factorio's 1:1 unit rule.
 */
public class BoilerBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int DATA_FUEL = 0;
    public static final int DATA_FUEL_CAPACITY = 1;
    public static final int DATA_WATER = 2;
    public static final int DATA_STEAM = 3;
    public static final int DATA_COUNT = 4;

    /** Every figure below is the prototype's, resolved once at class-init. */
    private static final SteamChainCorpus CORPUS = SteamChainCorpus.get();

    public static final int WATER_CAPACITY = CORPUS.boilerFluidBoxVolume(BoilerSpec.INPUT);
    public static final int STEAM_CAPACITY = CORPUS.boilerFluidBoxVolume(BoilerSpec.OUTPUT);

    private static final long JOULES_PER_TICK =
            BoilerSpec.joulesPerTick(CORPUS.boilerEnergyConsumption());
    private static final long JOULES_PER_MILLIBUCKET = BoilerSpec.joulesPerMilliBucket(
            CORPUS.boilerTargetTemperature(),
            CORPUS.fluidDefaultTemperature("water"),
            // Steam's, not water's. BoilerSpec is where that trap is written down.
            CORPUS.fluidHeatCapacity("steam"));
    /** ADR-0047's multiplier on the way *in* to the buffer. Factorio's is 1. */
    private static final double EFFECTIVITY = CORPUS.boilerEffectivity();

    private static final int MILLIBUCKETS_PER_TICK =
            BoilerSpec.milliBucketsPerTick(JOULES_PER_TICK, JOULES_PER_MILLIBUCKET);

    /**
     * The two tanks' indices behind {@link #fluidHandler()}'s combined face, in the order they are
     * combined. Named because the face's whole rule is which index does what, and {@code 0} and
     * {@code 1} do not say which is which.
     */
    private static final int WATER_TANK = 0;
    private static final int STEAM_TANK = 1;

    private final NonNullList<ItemStack> items = NonNullList.withSize(BoilerSlots.SIZE, ItemStack.EMPTY);
    private final FuelBuffer fuel = new FuelBuffer();

    private final FluidStacksResourceHandler water = new FluidStacksResourceHandler(1, WATER_CAPACITY) {
        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.is(Fluids.WATER);
        }
        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            setChanged();
        }
    };

    private final FluidStacksResourceHandler steam = new FluidStacksResourceHandler(1, STEAM_CAPACITY) {
        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            setChanged();
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_FUEL -> clampToInt(fuel.storedJoules());
                case DATA_FUEL_CAPACITY -> clampToInt(fuel.gaugeCapacity());
                case DATA_WATER -> water.getAmountAsInt(0);
                case DATA_STEAM -> steam.getAmountAsInt(0);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_FUEL -> fuel.load(value, fuel.lastLitJoules());
                case DATA_FUEL_CAPACITY -> fuel.load(fuel.storedJoules(), value);
                case DATA_WATER -> water.set(0, FluidResource.of(Fluids.WATER), value);
                case DATA_STEAM -> steam.set(0, FluidResource.of(PFFluids.STEAM_SOURCE.get()), value);
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
        int converted = BoilerCycle.tick(
                water.getAmountAsInt(0),
                steam.getCapacityAsInt(0, net.neoforged.neoforge.transfer.fluid.FluidResource.EMPTY) - steam.getAmountAsInt(0),
                MILLIBUCKETS_PER_TICK,
                JOULES_PER_TICK,
                fuel,
                this::light);
        setLit(converted > 0);
        if (converted <= 0) {
            return;
        }
        try (Transaction tx = Transaction.openRoot()) {
            water.extract(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), converted, tx);
            // Unit for unit: Factorio's boiler is a temperature change, not a reaction.
            steam.insert(net.neoforged.neoforge.transfer.fluid.FluidResource.of(PFFluids.STEAM_SOURCE.get()), converted, tx);
            tx.commit();
        }
        setChanged();
    }

    private void setLit(boolean lit) {
        if (getBlockState().getValue(BoilerBlock.LIT) == lit) {
            return;
        }
        level.setBlock(getBlockPos(), getBlockState().setValue(BoilerBlock.LIT, lit), Block.UPDATE_ALL);
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

    // -- the faces ------------------------------------------------------------------------------

    /**
     * Water in, steam out, on every side.
     *
     * <p>Two tanks behind one handler, and the direction never decides which: the <em>fluid</em>
     * does. A pipe pushing water reaches tank 0 and nothing else; a pipe pulling reaches the steam
     * and can never drain the water back out, which would otherwise let a player launder water
     * through a machine that is supposed to be consuming it.
     *
     * <p>Both rules are stated per tank <em>and</em> reached by the slot-less overloads, which is
     * what {@link GuardedResourceHandler} is for -- see its javadoc for why a plain
     * {@code DelegatingResourceHandler} would let a pipe around both of them.
     */
    public ResourceHandler<FluidResource> fluidHandler() {
        return new GuardedResourceHandler<>(new CombinedResourceHandler<>(water, steam)) {
            @Override
            public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
                if (index == WATER_TANK) {
                    return super.insert(index, resource, amount, transaction);
                }
                return 0;
            }

            @Override
            public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
                if (index == STEAM_TANK) {
                    return super.extract(index, resource, amount, transaction);
                }
                return 0;
            }
        };
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
        // Both tanks persist. A Boiler that came back empty over a logout would have destroyed
        // water an Offshore Pump had to lift, and steam a whole fuel item paid for.
        water.deserialize(input.childOrEmpty("Water"));
        steam.deserialize(input.childOrEmpty("Steam"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putLong("FuelJoules", fuel.storedJoules());
        output.putLong("FuelLitJoules", fuel.lastLitJoules());
        water.serialize(output.child("Water"));
        steam.serialize(output.child("Steam"));
    }
}
