package com.planetaryfactory.core.machine;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import com.planetaryfactory.core.PFMenus;
import com.planetaryfactory.core.recipes.AssemblingRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import net.neoforged.neoforge.transfer.IndexModifier;

/**
 * The Assembling Machine's menu (#327): four inputs, one output, and the Held recipe.
 *
 * <p>The pack's own rather than Oritech's, because Oritech's screen has no hook for an extra
 * widget. A recipe is picked in the recipe viewer, never here (ADR-0073, #336): EMI's Fill Recipe
 * lands on {@link #request}. The screen only shows what is held and how far its craft is. There is
 * no clear: an Assembling Machine without a recipe does nothing, so a recipe is replaced, never
 * removed.
 *
 * <p>The recipe list still travels in the opening packet, as what the screen names and ghosts the
 * Held recipe from and what the client's input slots filter by -- the recipes are server truth, and
 * the client has no recipe manager to read them from. It is fixed for the life of the menu, so the
 * Held recipe crosses as one index into it, in a data slot, beside the craft's progress and duration,
 * the machine's status and its energy.
 */
public class AssemblingMachineMenu extends AbstractContainerMenu {

    /**
     * One recipe the machine may hold: the recipe, whether the team is locked out of it, what it
     * makes, what each input slot takes, in {@link AssemblingInputSlots}' order, and its fluid
     * ingredients, the {@code n}th in input tank {@code n}, and the fluid each output tank fills with,
     * in order (ADR-0096).
     */
    public record Entry(RecipeChoice choice, Component name, ItemStack icon, List<SizedIngredient> slotIngredients,
                        List<SizedFluidIngredient> fluidIngredients, List<Fluid> outputFluids) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, entry -> entry.choice().id(),
                ByteBufCodecs.BOOL, entry -> entry.choice().locked(),
                ComponentSerialization.STREAM_CODEC, Entry::name,
                ItemStack.OPTIONAL_STREAM_CODEC, Entry::icon,
                SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), Entry::slotIngredients,
                SizedFluidIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), Entry::fluidIngredients,
                ByteBufCodecs.registry(Registries.FLUID).apply(ByteBufCodecs.list()), Entry::outputFluids,
                (id, locked, name, icon, ingredients, fluids, output) ->
                        new Entry(new RecipeChoice(id, locked), name, icon, ingredients, fluids, output));

        /** The first fluid this recipe's tank takes, or empty for an item-only recipe. */
        public Optional<Fluid> fluid() {
            return AssemblingMachineRecipes.firstFluid(fluidIngredients);
        }

        public Optional<Fluid> inputFluid(int tank) {
            return tank < fluidIngredients.size()
                    ? AssemblingMachineRecipes.firstFluid(List.of(fluidIngredients.get(tank)))
                    : Optional.empty();
        }

        public Optional<Fluid> outputFluid(int tank) {
            return tank < outputFluids.size() ? Optional.of(outputFluids.get(tank)) : Optional.empty();
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, List<Entry>> LIST_CODEC =
                STREAM_CODEC.apply(ByteBufCodecs.list());
    }

    /** No recipe held. */
    public static final int NONE = -1;

    /** A recipe held that the list does not name -- one a datapack reload took away. */
    public static final int UNKNOWN = -2;

    private static final int DATA_HELD = 0;
    private static final int DATA_PROGRESS = 1;
    private static final int DATA_DURATION = 2;
    private static final int DATA_STATUS = 3;
    private static final int DATA_STORED = 4;
    private static final int DATA_CAPACITY = 6;
    private static final int DATA_DRAW = 8;
    private static final int FLUID_INPUTS = MachineSpecs.get().maxFluidInputs();
    private static final int FLUID_OUTPUTS = MachineSpecs.get().maxFluidOutputs();
    // Only each tank's fill crosses: its size is the spec's, which the client reads off the block.
    private static final int DATA_TANKS = 10;
    private static final int DATA_COUNT = DATA_TANKS + FLUID_INPUTS + FLUID_OUTPUTS;
    private static final int OUTPUT = AssemblingMachineBlockEntity.OUTPUT;
    private static final int MACHINE_SLOTS = 5;

    public static final int INPUT_X = 8;
    public static final int INPUT_Y = 36;
    public static final int OUTPUT_X = 152;
    public static final int TANK_Y = 58;
    private static final int TANK_ROW = 16;
    private static final int ENERGY_Y = 58;
    private static final int STATUS_Y = 74;
    private static final int INVENTORY_Y = 98;

    private final List<Entry> entries;
    private final ContainerData data;
    private final AssemblingMachineBlockEntity machine;
    private final BlockPos pos;
    private final int inputs;
    private final int rowShift;

    /** Client side: the slots stand over a stub the menu's own sync fills. */
    public AssemblingMachineMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, null, buf.readBlockPos(), Entry.LIST_CODEC.decode(buf),
                new ItemStacksResourceHandler(MACHINE_SLOTS), new SimpleContainerData(DATA_COUNT));
    }

    private AssemblingMachineMenu(int containerId, Inventory playerInventory,
            AssemblingMachineBlockEntity machine, BlockPos pos, List<Entry> entries,
            ItemStacksResourceHandler slots, ContainerData data) {
        super(PFMenus.ASSEMBLING_MACHINE.get(), containerId);
        this.machine = machine;
        this.pos = pos;
        this.entries = List.copyOf(entries);
        this.data = data;
        MachineSpec spec = specAt(playerInventory.player, pos);
        this.inputs = spec == null ? AssemblingMachineBlockEntity.INPUTS : spec.itemInputs();
        this.rowShift = spec != null && spec.hasTanks() ? TANK_ROW : 0;

        for (int slot = 0; slot < AssemblingMachineBlockEntity.INPUTS; slot++) {
            addSlot(new InputSlot(slots, slots::set, slot, INPUT_X + slot * 18, INPUT_Y));
        }
        addSlot(new OutputSlot(slots, slots::set, OUTPUT, OUTPUT_X, INPUT_Y));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18,
                        inventoryY() + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, inventoryY() + 58));
        }
        addDataSlots(data);
    }

    public BlockPos pos() {
        return pos;
    }

    private static MachineSpec specAt(Player player, BlockPos pos) {
        return player.level().getBlockState(pos).getBlock() instanceof ChassisMachineBlock block ? block.spec() : null;
    }

    /** Asked of the block each time, since a Fast Replace changes it under an open menu (ADR-0082). */
    public MachineSpec spec(Player player) {
        MachineSpec spec = specAt(player, pos);
        return spec == null ? AssemblingTier.ONE.spec() : spec;
    }

    public int energyY() {
        return ENERGY_Y + rowShift;
    }

    public int statusY() {
        return STATUS_Y + rowShift;
    }

    public int inventoryY() {
        return INVENTORY_Y + rowShift;
    }

    /** Server side, over the machine's own inventory and the recipes the server has loaded. */
    public static AssemblingMachineMenu open(int containerId, Inventory playerInventory,
            AssemblingMachineBlockEntity machine) {
        List<Entry> entries = entries(machine);
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_HELD -> heldIndex(entries, machine.heldRecipe());
                    case DATA_PROGRESS -> machine.craftProgress();
                    case DATA_DURATION -> machine.craftDuration();
                    case DATA_STATUS -> machine.status().ordinal();
                    case DATA_STORED -> DataSlotHalves.low(machine.energyStorage.getAmountAsLong());
                    case DATA_STORED + 1 -> DataSlotHalves.high(machine.energyStorage.getAmountAsLong());
                    case DATA_CAPACITY -> DataSlotHalves.low(machine.energyStorage.getCapacityAsLong());
                    case DATA_CAPACITY + 1 -> DataSlotHalves.high(machine.energyStorage.getCapacityAsLong());
                    case DATA_DRAW -> DataSlotHalves.low(machine.drawTenths());
                    case DATA_DRAW + 1 -> DataSlotHalves.high(machine.drawTenths());
                    default -> index >= DATA_TANKS && index < DATA_COUNT
                            ? (int) machine.tank().getAmountAsLong(index - DATA_TANKS) : 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        return new AssemblingMachineMenu(containerId, playerInventory, machine, machine.getBlockPos(),
                entries, machine.inventory, data);
    }

    public static List<Entry> entries(AssemblingMachineBlockEntity machine) {
        ServerLevel level = (ServerLevel) machine.getLevel();
        MachineSpec spec = machine.spec();
        return AssemblingMachineRecipes.choices(machine).stream()
                .map(choice -> AssemblingMachineRecipes.resolve(level, HeldRecipe.of(choice.id()), spec)
                        .map(holder -> new Entry(choice, AssemblingMachineRecipes.name(holder),
                                holder.value().assemble(null),
                                AssemblingMachineRecipes.slotIngredients(holder.value(), spec),
                                holder.value().fluidIngredients(),
                                holder.value().fluidResults().stream()
                                        .map(result -> FluidResource.of(result).getFluid()).toList()))
                        .orElseGet(() -> new Entry(choice, Component.literal(choice.id()), ItemStack.EMPTY, List.of(),
                                List.of(), List.of())))
                .toList();
    }

    /** What the opening packet carries: the position, then the list {@link #open} built. */
    public static void writeOpening(RegistryFriendlyByteBuf buf, AssemblingMachineBlockEntity machine) {
        buf.writeBlockPos(machine.getBlockPos());
        Entry.LIST_CODEC.encode(buf, entries(machine));
    }

    static int heldIndex(List<Entry> entries, HeldRecipe held) {
        if (held.id().isEmpty()) {
            return NONE;
        }
        String id = held.id().get();
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).choice().id().equals(id)) {
                return index;
            }
        }
        return UNKNOWN;
    }

    public List<Entry> entries() {
        return entries;
    }

    /** The entry the machine holds, or null. */
    public Entry held() {
        int index = data.get(DATA_HELD);
        return index >= 0 && index < entries.size() ? entries.get(index) : null;
    }

    /** What input {@code slot} takes under the Held recipe; empty for an unused slot or no recipe. */
    public Optional<SizedIngredient> slotIngredient(int slot) {
        Entry held = held();
        return held == null ? Optional.empty() : AssemblingInputSlots.ingredientFor(slot, held.slotIngredients());
    }

    /** Whether input {@code slot}'s contents cannot cover one craft of the Held recipe. */
    public boolean isShort(int slot) {
        Entry held = held();
        return held != null && AssemblingInputSlots.isShort(slot, held.slotIngredients(),
                slots.get(slot).getItem().getCount(), SizedIngredient::count);
    }

    /** Whether the machine holds an id the list does not name. */
    public boolean holdsUnknown() {
        return data.get(DATA_HELD) == UNKNOWN;
    }

    /** How far the craft under way is, from 0 to 1; 0 with no recipe held. */
    public float progress() {
        int duration = data.get(DATA_DURATION);
        return duration <= 0 ? 0f : Math.min(1f, (float) data.get(DATA_PROGRESS) / duration);
    }

    public AssemblingStatus status() {
        return AssemblingStatus.fromOrdinal(data.get(DATA_STATUS));
    }

    public long storedFe() {
        return DataSlotHalves.join(data.get(DATA_STORED), data.get(DATA_STORED + 1));
    }

    public long capacityFe() {
        return DataSlotHalves.join(data.get(DATA_CAPACITY), data.get(DATA_CAPACITY + 1));
    }

    public int inputAmount(int tank) {
        return data.get(DATA_TANKS + tank);
    }

    public int outputAmount(int tank) {
        return data.get(DATA_TANKS + FLUID_INPUTS + tank);
    }

    /** The fluid the status names, or empty. */
    public Optional<Fluid> heldFluid() {
        Entry held = held();
        return held == null ? Optional.empty() : held.fluid();
    }

    /** The Held recipe's draw while crafting, in tenths of an FE a tick. */
    public long drawTenths() {
        return DataSlotHalves.join(data.get(DATA_DRAW), data.get(DATA_DRAW + 1));
    }

    /**
     * Holds {@code id} if the machine may, or tells the player why not (#330, ADR-0073). The
     * setter EMI's Fill Recipe lands on, so a refusal is never a
     * gesture that silently did nothing.
     */
    public HoldVerdict request(Player player, String id) {
        if (machine == null) {
            return HoldVerdict.NOT_ASSEMBLING;
        }
        HoldVerdict verdict = verdict(machine, machine.spec(), id);
        if (verdict.held()) {
            machine.setHeldRecipe(HeldRecipe.of(id), player);
        } else if (player instanceof ServerPlayer server) {
            server.sendSystemMessage(Component.translatable(verdict.messageKey(), recipeName(server.level(), id)));
        }
        return verdict;
    }

    /** An id that names no recipe has only itself. */
    private static Component recipeName(ServerLevel level, String id) {
        return AssemblingMachineRecipes.resolveAny(level, HeldRecipe.of(id))
                .map(AssemblingMachineRecipes::name)
                .orElse(Component.literal(id));
    }

    /** What {@link #request} would answer for {@code machine} running {@code spec}, asked of the server's recipes and locks. */
    public static HoldVerdict verdict(AssemblingMachineBlockEntity machine, MachineSpec spec, String id) {
        Optional<RecipeHolder<AssemblingRecipe>> recipe =
                AssemblingMachineRecipes.resolveAny((ServerLevel) machine.getLevel(), HeldRecipe.of(id));
        return HoldVerdict.of(recipe.isPresent(),
                recipe.map(holder -> AssemblingMachineRecipes.ofType(holder.value(), spec)).orElse(false),
                recipe.map(holder -> AssemblingMachineRecipes.fits(holder.value(), spec)).orElse(false),
                AssemblingMachineRecipes.isLocked(machine, id));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, AssemblingMachineBlockEntity.INPUTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (machine != null && machine.isRemoved()) {
            return false;
        }
        // Vanilla's buffer in Container.stillValidBlockEntity, so a screen opened at full Reach stays open (#413).
        return player.isWithinBlockInteractionRange(pos, 4.0);
    }

    /**
     * The server asks the machine; the client asks the Held entry through the same rule, so a wrong
     * item is refused in the hand rather than placed and put back by the sync (ADR-0074).
     */
    private final class InputSlot extends ResourceHandlerSlot {
        InputSlot(ResourceHandler<ItemResource> handler, IndexModifier<ItemResource> modifier,
                int index, int x, int y) {
            super(handler, modifier, index, x, y);
        }

        /** A slot past the machine's own inputs is laid out and never used. */
        @Override
        public boolean isActive() {
            return getSlotIndex() < inputs;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!super.mayPlace(stack)) {
                return false;
            }
            if (machine != null) {
                return machine.acceptsInput(getSlotIndex(), ItemResource.of(stack));
            }
            Entry held = held();
            return held != null && AssemblingMachineRecipes.accepts(getSlotIndex(), held.slotIngredients(), stack);
        }
    }

    /** The output takes nothing from the player. */
    private static final class OutputSlot extends ResourceHandlerSlot {
        OutputSlot(ResourceHandler<ItemResource> handler, IndexModifier<ItemResource> modifier,
                int index, int x, int y) {
            super(handler, modifier, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
