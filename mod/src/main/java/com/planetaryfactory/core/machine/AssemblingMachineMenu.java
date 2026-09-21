package com.planetaryfactory.core.machine;

import java.util.List;

import com.planetaryfactory.core.PFMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
 * <p>The recipe list still travels in the opening packet, as what the screen names the Held recipe
 * from -- the recipes are server truth, and the client has no recipe manager to read them from. It
 * is fixed for the life of the menu, so the Held recipe crosses as one index into it, in a data slot,
 * beside the craft's progress and duration.
 */
public class AssemblingMachineMenu extends AbstractContainerMenu {

    /** One recipe the machine may hold: the recipe, whether the team is locked out of it, and what it makes. */
    public record Entry(RecipeChoice choice, ItemStack icon) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, entry -> entry.choice().id(),
                ByteBufCodecs.BOOL, entry -> entry.choice().locked(),
                ItemStack.OPTIONAL_STREAM_CODEC, Entry::icon,
                (id, locked, icon) -> new Entry(new RecipeChoice(id, locked), icon));

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
    private static final int DATA_COUNT = 3;
    private static final int OUTPUT = 4;
    private static final int MACHINE_SLOTS = 5;

    public static final int INPUT_X = 8;
    public static final int INPUT_Y = 36;
    public static final int OUTPUT_X = 152;
    public static final int INVENTORY_Y = 72;

    private final List<Entry> entries;
    private final ContainerData data;
    private final AssemblingMachineBlockEntity machine;
    private final BlockPos pos;

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

        for (int slot = 0; slot < AssemblingMachineBlockEntity.INPUTS; slot++) {
            addSlot(new ResourceHandlerSlot(slots, slots::set, slot, INPUT_X + slot * 18, INPUT_Y));
        }
        addSlot(new OutputSlot(slots, slots::set, OUTPUT, OUTPUT_X, INPUT_Y));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18,
                        INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, INVENTORY_Y + 58));
        }
        addDataSlots(data);
    }

    /** Server side, over the machine's own inventory and the recipes the server has loaded. */
    public static AssemblingMachineMenu open(int containerId, Inventory playerInventory,
            AssemblingMachineBlockEntity machine) {
        List<Entry> entries = entries((ServerLevel) machine.getLevel());
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_HELD -> heldIndex(entries, machine.heldRecipe());
                    case DATA_PROGRESS -> machine.craftProgress();
                    default -> machine.craftDuration();
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

    public static List<Entry> entries(ServerLevel level) {
        return AssemblingMachineRecipes.choices(level).stream()
                .map(choice -> new Entry(choice, AssemblingMachineRecipes.icon(level, choice.id())))
                .toList();
    }

    /** What the opening packet carries: the position, then the list {@link #open} built. */
    public static void writeOpening(RegistryFriendlyByteBuf buf, AssemblingMachineBlockEntity machine) {
        buf.writeBlockPos(machine.getBlockPos());
        Entry.LIST_CODEC.encode(buf, entries((ServerLevel) machine.getLevel()));
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

    /** Whether the machine holds an id the list does not name. */
    public boolean holdsUnknown() {
        return data.get(DATA_HELD) == UNKNOWN;
    }

    /** How far the craft under way is, from 0 to 1; 0 with no recipe held. */
    public float progress() {
        int duration = data.get(DATA_DURATION);
        return duration <= 0 ? 0f : Math.min(1f, (float) data.get(DATA_PROGRESS) / duration);
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
        HoldVerdict verdict = verdict((ServerLevel) machine.getLevel(), id);
        if (verdict.held()) {
            machine.setHeldRecipe(HeldRecipe.of(id), player);
        } else if (player instanceof ServerPlayer server) {
            // Above the hotbar: feedback on the press just made. 26.1 has the overlay form on ServerPlayer only.
            server.sendSystemMessage(Component.translatable(verdict.messageKey(), id), true);
        }
        return verdict;
    }

    /** What {@link #request} would answer, asked of the server's recipes and research. */
    public static HoldVerdict verdict(ServerLevel level, String id) {
        return HoldVerdict.of(AssemblingMachineRecipes.resolve(level, HeldRecipe.of(id)).isPresent(),
                AssemblingMachineRecipes.isLocked(id));
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
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
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
