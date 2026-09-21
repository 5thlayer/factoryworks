package com.planetaryfactory.core.machine;

import java.util.List;

import com.planetaryfactory.core.PFBlockEntities;

import com.planetaryfactory.core.PFMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.ContainerSlotAssignment;
import rearth.oritech.util.ScreenProvider;

/**
 * The Assembling Machine's anchor (#326, ADR-0071): Oritech's machine base, placed as a footprint
 * and holding a {@link HeldRecipe} the player sets from its screen (#327). It crafts nothing yet.
 *
 * <p>Extends {@link MultiblockMachineEntity} rather than Oritech's {@code AssemblerBlockEntity},
 * whose one constructor hard-codes Oritech's own block entity type -- a subclass of it would be
 * reloaded from disk as Oritech's class. What {@code AssemblerBlockEntity} adds over the base is
 * re-derived here from the 2.0.0-exp6 jar: the slot layout, the screen, the addon slots and the
 * config it reads its energy figures from.
 *
 * <p><b>Two overrides make it a footprint rather than Oritech's multiblock.</b>
 * {@link #getCorePositions} is empty, because the pack places every block of the footprint itself
 * and the player never stacks Machine Cores. {@link #isAssembled} is {@code true}, which is what
 * Oritech's non-multiblock base returns before {@code MultiblockMachineEntity} overrides it with an
 * unguarded read of {@code ASSEMBLED}. The block still declares {@code ASSEMBLED} and is placed with
 * it set, so the paths that read the property -- {@code initMultiblock}, the block's
 * {@code useWithoutItem} and {@code resetMultiblock} -- see an assembled machine and return early.
 * <b>Not</b> the bare empty-core-list route: {@code initMultiblock} over an empty list on an
 * unassembled state divides {@code 0.0f} by zero and sets a NaN core quality.
 *
 * <p><b>Inert, deliberately.</b> {@link #findActiveRecipe} answers empty, so Oritech's
 * {@code serverTick} resets progress and returns before {@code workTick} is reached -- nothing is
 * looked up, nothing is consumed and no energy is drawn. ADR-0071 replaces the craft cycle whole
 * against a Held recipe; until #325's later tickets land, the one thing this machine must not do
 * is run Oritech's own assembler recipes by first match, which is what returning Oritech's type
 * from {@link #getOwnRecipeType} would otherwise do.
 */
public class AssemblingMachineBlockEntity extends MultiblockMachineEntity {

    /** The four input slots, which a change of Held recipe hands back. */
    public static final int INPUTS = 4;

    private static final String HELD_KEY = "held_recipe";

    private HeldRecipe held = HeldRecipe.NONE;

    public AssemblingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.ASSEMBLING_MACHINE.get(), pos, state,
                OritechConfig.processingMachines.assemblerData.energyPerTick.get());
    }

    @Override
    public boolean isAssembled(BlockState state) {
        return true;
    }

    /**
     * Assembled by construction, whatever the blockstate says. The item places the anchor with
     * {@code ASSEMBLED=true}, but a state set some other way -- {@code /setblock}, a debug stick --
     * would otherwise reach Oritech's walk over an empty core list and its {@code 0.0f / 0}.
     */
    @Override
    public boolean initMultiblock(BlockState state) {
        return true;
    }

    /**
     * Never rescanned. Oritech's {@code MachineControllerLifecycle.onLoad} schedules a rescan for
     * the next server tick -- on placement as well as on chunk load -- and a rescan that finds no
     * cores calls {@code resetInvalidMultiblock}, which sets {@code ASSEMBLED} to {@code false}.
     * The block's {@code useWithoutItem} then replays the setup animation on every right-click and
     * never opens the screen. There are no cores to rescan for.
     */
    @Override
    public void rescanMultiblock() {
    }

    @Override
    public List<Vec3i> getCorePositions() {
        return List.of();
    }

    @Override
    protected OritechRecipe findActiveRecipe() {
        return OritechRecipe.EMPTY.get();
    }

    /**
     * Oritech's assembler type, which the base class makes abstract and nothing here reads:
     * {@link #findActiveRecipe} never reaches the lookup that would.
     */
    @Override
    protected RecipeType<OritechRecipe> getOwnRecipeType() {
        return RecipeContent.ASSEMBLER.get();
    }

    @Override
    public long getDefaultCapacity() {
        return OritechConfig.processingMachines.assemblerData.energyCapacity.get();
    }

    @Override
    public long getDefaultInsertRate() {
        return OritechConfig.processingMachines.assemblerData.maxEnergyInsertion.get();
    }

    /** Four inputs at 0..3 and one output at 4 -- Oritech's assembler's, and ADR-0071's four slots. */
    @Override
    public ContainerSlotAssignment getSlotAssignments() {
        return new ContainerSlotAssignment(0, 4, 4, 1);
    }

    @Override
    public List<ScreenProvider.GuiSlot> getGuiSlots() {
        return List.of(
                new ScreenProvider.GuiSlot(0, 38, 26),
                new ScreenProvider.GuiSlot(1, 56, 26),
                new ScreenProvider.GuiSlot(2, 38, 44),
                new ScreenProvider.GuiSlot(3, 56, 44),
                new ScreenProvider.GuiSlot(4, 117, 36, true));
    }

    @Override
    public int getInventorySize() {
        return 5;
    }

    /**
     * The pack's own menu, not Oritech's (#327): Oritech's screen has no hook for the recipe
     * widget. {@link AssemblingMachineBlock} opens it, since the opening packet carries the list.
     */
    @Override
    public MenuType<?> getScreenHandlerType() {
        return PFMenus.ASSEMBLING_MACHINE.get();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return AssemblingMachineMenu.open(containerId, playerInventory, this);
    }

    public HeldRecipe heldRecipe() {
        return held;
    }

    /**
     * Holds {@code next}. A change hands every ingredient already in the input slots back to
     * {@code player} -- changing one's mind destroys nothing; what does not fit drops at their feet. Setting the recipe already held is
     * not a change and moves nothing.
     *
     * <p>The lock is not asked here: an unresearched recipe is held and shown, and refusing to craft
     * it is the craft cycle's.
     */
    public void setHeldRecipe(HeldRecipe next, Player player) {
        if (!held.changesTo(next)) {
            return;
        }
        for (int slot = 0; slot < INPUTS; slot++) {
            ItemStack stack = inventory.getItem(slot).copy();
            if (stack.isEmpty()) {
                continue;
            }
            inventory.set(slot, ItemResource.EMPTY, 0);
            player.getInventory().placeItemBackInInventory(stack);
        }
        held = next;
        setChanged();
    }

    /** Whether {@link #heldRecipe} names a recipe the server has loaded. */
    public boolean heldRecipeResolves() {
        return level instanceof ServerLevel server
                && AssemblingMachineRecipes.resolve(server, held).isPresent();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(HELD_KEY, HeldRecipe.CODEC, held);
    }

    /** The id only. It is resolved when asked, never here, where the recipes may not be loaded. */
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        held = input.read(HELD_KEY, HeldRecipe.CODEC).orElse(HeldRecipe.NONE);
    }

    /** {@link AssemblingMachineFootprint#addonSlots}: beside the row and behind the anchor. */
    @Override
    public List<Vec3i> getAddonSlots() {
        return AssemblingMachineFootprint.addonSlots().stream()
                .map(slot -> new Vec3i(slot.x(), slot.y(), slot.z()))
                .toList();
    }
}
