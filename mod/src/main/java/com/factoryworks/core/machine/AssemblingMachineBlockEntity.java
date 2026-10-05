package com.factoryworks.core.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.factoryworks.core.recipes.AssemblingRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.factoryworks.core.PFMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.ColorableMachine.ColorVariant;
import rearth.oritech.util.ContainerSlotAssignment;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.ScreenProvider;

/**
 * The chassis the Chemical Plant and the Oil Refinery stand on (#326, ADR-0071, ADR-0096): Oritech's
 * machine base, placed as a footprint and holding a {@link HeldRecipe} the player sets from its screen
 * (#327), which it crafts (#328). The Assemblers are Craftworks' (ADR-0118).
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
 * <p><b>The craft cycle is the pack's, replaced whole (#328, ADR-0071).</b> Oritech's is typed to
 * {@code OritechRecipe}, whose inputs are one unit per slot; the pack's recipe carries sized
 * ingredients with Factorio's counts, and the two cannot be adapted into each other.
 * {@link #serverTick} is therefore overridden rather than {@code workTick} alone, because Oritech's
 * {@code serverTick} returns before {@code workTick} whenever {@link #findActiveRecipe} -- which
 * stays empty, so Oritech's own assembler recipes never run here by first match -- finds nothing.
 * The transaction discipline is Oritech's: open a root, extract the tick's energy, increment
 * progress, and on the last tick take the inputs and place the outputs, committing only if all of
 * it succeeded. The rate is {@link AssemblingMachineSpec}'s, with Oritech's addon multipliers on top.
 *
 * <p>Every stall ({@link AssemblingStall}) is answered <b>before</b> the energy is extracted, so a
 * blocked machine draws nothing, starts nothing and voids nothing (ADR-0041). Progress is held
 * across a stall, the way {@code FurnaceCycle} holds it: the inputs are only taken on the last tick,
 * and the energy already paid is the craft's. A change of Held recipe resets it.
 */
public abstract class AssemblingMachineBlockEntity extends MultiblockMachineEntity {

    /** The input slots, which a change of Held recipe hands back. */
    public static final int INPUTS = AssemblingInputSlots.INPUTS;

    /** The one output slot, after the inputs. */
    public static final int OUTPUT = INPUTS;

    /** Input tanks first, then output tanks, laid out for the widest machine; a tank the spec lacks has no room. */
    private static final int FLUID_INPUTS = MachineSpecs.get().maxFluidInputs();

    private static final int FLUID_OUTPUTS = MachineSpecs.get().maxFluidOutputs();

    private static final String HELD_KEY = "held_recipe";

    private static final String TANK_KEY = "tanks";

    private static final Logger LOGGER = LoggerFactory.getLogger(AssemblingMachineBlockEntity.class);

    private HeldRecipe held = HeldRecipe.NONE;

    private AssemblingStall stall = AssemblingStall.NO_RECIPE;

    /** Each tank's size is asked of the spec and the Held recipe each time. */
    private final FluidStacksResourceHandler tank = new FluidStacksResourceHandler(FLUID_INPUTS + FLUID_OUTPUTS, 0) {
        @Override
        protected int getCapacity(int index, FluidResource resource) {
            return capacity(spec(), index);
        }

        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            setChanged();
        }
    };

    protected AssemblingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, OritechConfig.processingMachines.assemblerData.energyPerTick.get());
    }

    public MachineSpec spec() {
        return chassis().spec();
    }

    private ChassisMachineBlock chassis() {
        return (ChassisMachineBlock) getBlockState().getBlock();
    }

    private int capacity(MachineSpec spec, int index) {
        if (index < FLUID_INPUTS) {
            return spec.fluidInputVolume(index);
        }
        int result = index - FLUID_INPUTS;
        if (result >= spec.fluidOutputs().size() || !(level instanceof ServerLevel server)) {
            return spec.fluidOutputVolume(result);
        }
        return AssemblingMachineRecipes.resolve(server, held, spec)
                .filter(holder -> result < holder.value().fluidResults().size())
                .map(holder -> {
                    OverloadLimit limit = OverloadLimit.get();
                    List<Integer> amounts = holder.value().fluidResults().stream()
                            .map(fluid -> fluid.amount()).toList();
                    return OutputTankVolume.of(spec.fluidOutputBoxes(), amounts,
                            limit.pinnedFluidOutputs().contains(holder.id().identifier().toString()),
                            limit.fluidOutputCrafts()).get(result);
                })
                .orElse(spec.fluidOutputVolume(result));
    }

    /** The block's paint, whatever was saved or assigned. {@code PaintLock} stops a cartridge being spent on it. */
    @Override
    public ColorVariant getCurrentColor() {
        return ColorVariant.valueOf(chassis().paint());
    }

    /** Ignored: see {@link #getCurrentColor}. */
    @Override
    public void assignColor(ColorVariant color) {
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

    /**
     * The pack's craft tick, in place of Oritech's. Redstone still disables the machine, as it does
     * Oritech's; everything else is {@link #craftTick}.
     */
    @Override
    public void serverTick(ServerLevel world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        if (!isAssembled(state) || disabledViaRedstone) {
            return;
        }
        craftTick(world);
    }

    private void craftTick(ServerLevel server) {
        Optional<RecipeHolder<AssemblingRecipe>> resolved = AssemblingMachineRecipes.resolve(server, held, spec());
        stall = stallFor(resolved);
        if (stall.stalled()) {
            return;
        }
        AssemblingRecipe recipe = resolved.get().value();
        int duration = durationTicks(recipe);
        try (Transaction tx = Transaction.openRoot()) {
            long fe = nextTickFe(recipe);
            if (energyStorage.internalExtract(fe, tx) != fe) {
                stall = AssemblingStall.NO_POWER;
                return;
            }
            progress.increment(tx);
            if (progress.get() >= duration) {
                if (!takeInputs(recipe, tx) || !takeFluids(recipe, tx) || !placeOutputs(recipe, tx)) {
                    LOGGER.warn("Assembling Machine at {} passed its checks and could not finish {}",
                            worldPosition.toShortString(), held.id().orElse("?"));
                    return;
                }
                progress.reset(tx);
            }
            tx.commit();
        }
        lastWorkedAt = server.getGameTime();
        setChanged();
        onProgressed();
    }

    /** Asked in {@link AssemblingStall}'s order, and never by spending anything: both probes abort. */
    private AssemblingStall stallFor(Optional<RecipeHolder<AssemblingRecipe>> resolved) {
        if (resolved.isEmpty()) {
            return AssemblingStall.NO_RECIPE;
        }
        AssemblingRecipe recipe = resolved.get().value();
        boolean locked = AssemblingMachineRecipes.isLocked(this, held.id().orElseThrow());
        boolean fed;
        boolean fluidFed;
        boolean itemsFit;
        List<Boolean> tanksFit = new ArrayList<>();
        try (Transaction probe = Transaction.openRoot()) {
            fed = takeInputs(recipe, probe);
        }
        try (Transaction probe = Transaction.openRoot()) {
            fluidFed = takeFluids(recipe, probe);
        }
        try (Transaction probe = Transaction.openRoot()) {
            itemsFit = placeItems(recipe, probe);
        }
        for (int result = 0; result < recipe.fluidResults().size(); result++) {
            try (Transaction probe = Transaction.openRoot()) {
                tanksFit.add(placeFluid(recipe, result, probe));
            }
        }
        return AssemblingStall.of(true, locked, fed, fluidFed, itemsFit, tanksFit);
    }

    private boolean takeInputs(AssemblingRecipe recipe, Transaction tx) {
        ResourceHandler<ItemResource> inputs = inventory.getInputContainer();
        for (SizedIngredient sized : recipe.ingredients()) {
            int owed = sized.count();
            for (int slot = 0; slot < inputs.size() && owed > 0; slot++) {
                ItemResource resource = inputs.getResource(slot);
                if (resource.isEmpty() || !sized.ingredient().test(resource.toStack(1))) {
                    continue;
                }
                owed -= inputs.extract(slot, resource, owed, tx);
            }
            if (owed > 0) {
                return false;
            }
        }
        return true;
    }

    /** The {@code n}th fluid ingredient is in input tank {@code n}, and the {@code n}th result goes in output tank {@code n} (ADR-0096). */
    private boolean takeFluids(AssemblingRecipe recipe, Transaction tx) {
        List<SizedFluidIngredient> ingredients = recipe.fluidIngredients();
        for (int index = 0; index < ingredients.size(); index++) {
            SizedFluidIngredient sized = ingredients.get(index);
            FluidResource held = tank.getResource(index);
            if (held.isEmpty() || !sized.ingredient().test(held.toStack(1))
                    || tank.extract(index, held, sized.amount(), tx) != sized.amount()) {
                return false;
            }
        }
        return true;
    }

    /** Places one craft's whole result, or reports that some output cannot take it. Never a part. */
    private boolean placeOutputs(AssemblingRecipe recipe, Transaction tx) {
        if (!placeItems(recipe, tx)) {
            return false;
        }
        for (int result = 0; result < recipe.fluidResults().size(); result++) {
            if (!placeFluid(recipe, result, tx)) {
                return false;
            }
        }
        return true;
    }

    private boolean placeItems(AssemblingRecipe recipe, Transaction tx) {
        ResourceHandler<ItemResource> outputs = inventory.getOutputContainer();
        for (ItemStackTemplate result : recipe.results()) {
            if (outputs.insert(ItemResource.of(result), result.count(), tx) != result.count()) {
                return false;
            }
        }
        return true;
    }

    private boolean placeFluid(AssemblingRecipe recipe, int result, Transaction tx) {
        FluidStackTemplate fluid = recipe.fluidResults().get(result);
        return tank.insert(FLUID_INPUTS + result, FluidResource.of(fluid), fluid.amount(), tx) == fluid.amount();
    }

    private int durationTicks(AssemblingRecipe recipe) {
        return AssemblingMachineSpec.durationTicks(spec(), recipe.time(), getSpeedMultiplier());
    }

    /** Why the last tick made no progress, for the screen and the GameTests. */
    public AssemblingStall stall() {
        return stall;
    }

    /**
     * What the screen shows, recomputed on every ask (#332). The stall is asked the craft cycle's
     * way, and power as the tick's share against the buffer, without the draw.
     */
    public AssemblingStatus status() {
        if (!(level instanceof ServerLevel server)) {
            return AssemblingStatus.IDLE;
        }
        Optional<RecipeHolder<AssemblingRecipe>> resolved = AssemblingMachineRecipes.resolve(server, held, spec());
        AssemblingStall now = stallFor(resolved);
        boolean powered = now.stalled() || energyStorage.getAmountAsLong() >= resolved
                .map(holder -> nextTickFe(holder.value()))
                .orElse(0L);
        return AssemblingStatus.of(now, powered);
    }

    private long nextTickFe(AssemblingRecipe recipe) {
        int duration = durationTicks(recipe);
        return AssemblingMachineSpec.feForTick(Math.min(progress.get(), duration - 1), duration, craftFe(recipe));
    }

    private long craftFe(AssemblingRecipe recipe) {
        return AssemblingMachineSpec.fePerCraft(spec(), recipe.time(), getEfficiencyMultiplier());
    }

    /** The Held recipe's average draw in tenths of an FE a tick, or 0 with none. Server only. */
    public long drawTenths() {
        if (!(level instanceof ServerLevel server)) {
            return 0L;
        }
        return AssemblingMachineRecipes.resolve(server, held, spec())
                .map(holder -> AssemblingMachineSpec.drawTenths(craftFe(holder.value()), durationTicks(holder.value())))
                .orElse(0L);
    }

    /** Ticks into the craft under way, for the screen's progress bar. */
    public int craftProgress() {
        return (int) progress.get();
    }

    /** The Held recipe's duration at this machine's speed, or 0 with none. Server only. */
    public int craftDuration() {
        if (!(level instanceof ServerLevel server)) {
            return 0;
        }
        return AssemblingMachineRecipes.resolve(server, held, spec())
                .map(holder -> durationTicks(holder.value()))
                .orElse(0);
    }

    /**
     * The Held recipe's duration before Oritech's speed multiplier, for Oritech's
     * {@code getProgress}. Oritech's reads a {@code currentRecipe} this machine never sets. Server
     * only: the client has no recipe manager, and the animation no longer asks (see
     * {@link #getAnimationSpeed}).
     */
    @Override
    public int getRecipeDuration() {
        if (!(level instanceof ServerLevel server)) {
            return 1;
        }
        return AssemblingMachineRecipes.resolve(server, held, spec())
                .map(holder -> AssemblingMachineSpec.durationTicks(spec(), holder.value().time(), 1.0f))
                .orElse(1);
    }

    /**
     * The machine's pace, not the recipe's. Oritech fits one play of the animation to one craft,
     * which needs the recipe's duration on a client that cannot resolve it. Factorio's assembler
     * animates at a fixed rate scaled by its crafting speed whatever it makes, so this does too:
     * one play per 60 ticks, quickened by Oritech's speed addon (a multiplier below 1 is faster).
     */
    @Override
    protected float getAnimationSpeed() {
        return 1.0f / getSpeedMultiplier();
    }

    @Override
    protected OritechRecipe findActiveRecipe() {
        return OritechRecipe.EMPTY.get();
    }

    /**
     * Oritech's assembler type, which the base class makes abstract and nothing here reads:
     * {@link #serverTick} never reaches the lookup that would.
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

    /**
     * Laid out for the widest machine, as the tanks are: the menu and the item face address the
     * output by {@link #OUTPUT}, and a spec with fewer inputs leaves its last slots taking nothing.
     */
    @Override
    public ContainerSlotAssignment getSlotAssignments() {
        return new ContainerSlotAssignment(0, INPUTS, OUTPUT, 1);
    }

    @Override
    public List<ScreenProvider.GuiSlot> getGuiSlots() {
        MachineSpec spec = spec();
        List<ScreenProvider.GuiSlot> slots = new ArrayList<>();
        for (int input = 0; input < spec.itemInputs(); input++) {
            slots.add(new ScreenProvider.GuiSlot(input, 38 + 18 * (input % 2), 26 + 18 * (input / 2)));
        }
        slots.add(new ScreenProvider.GuiSlot(OUTPUT, 117, 36, true));
        return slots;
    }

    @Override
    public int getInventorySize() {
        return INPUTS + 1;
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
     * {@code player}, and what does not fit drops at their feet. The tanks are voided, as Factorio's
     * are: an input tank's face refuses extraction and the next recipe fills other outputs, so kept
     * fluid would be stranded (ADR-0075). Setting the
     * recipe already held is not a change and moves nothing.
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
        voidTanks();
        held = next;
        progress.set(0);
        setChanged();
    }

    private void voidTanks() {
        for (int index = 0; index < tank.size(); index++) {
            tank.set(index, FluidResource.EMPTY, 0);
        }
    }

    /**
     * False off the server, which alone can resolve the Held recipe, and for a recipe the machine
     * has no tank to run.
     */
    public boolean acceptsInput(int slot, ItemResource resource) {
        if (resource.isEmpty() || !(level instanceof ServerLevel server)) {
            return false;
        }
        return AssemblingMachineRecipes.resolve(server, held, spec())
                .map(holder -> AssemblingMachineRecipes.accepts(slot,
                        AssemblingMachineRecipes.slotIngredients(holder.value(), spec()), resource.toStack(1)))
                .orElse(false);
    }

    /**
     * How many more of its ingredient automated insertion may put in {@code slot}: the Overload
     * Limit less what the slot holds (#517). The menu's slots do not ask, so the hand is not held to it.
     */
    public int overloadRoom(int slot) {
        if (!(level instanceof ServerLevel server)) {
            return 0;
        }
        return AssemblingMachineRecipes.resolve(server, held, spec())
                .flatMap(holder -> AssemblingInputSlots.ingredientFor(slot,
                                AssemblingMachineRecipes.slotIngredients(holder.value(), spec()))
                        .map(ingredient -> OverloadLimit.room(ingredient.count(),
                                OverloadLimit.get().crafts(spec().craftingSpeed() / getSpeedMultiplier(),
                                        holder.value().time()),
                                inventory.getItem(slot).getCount())))
                .orElse(0);
    }

    /**
     * The input tank {@code resource} goes to: the one holding the Held recipe's fluid ingredient it
     * matches, or -1 when the recipe names no such fluid or the machine cannot run it (ADR-0075,
     * ADR-0096). Server only.
     */
    public int inputTankFor(FluidResource resource) {
        if (resource.isEmpty() || !(level instanceof ServerLevel server)) {
            return -1;
        }
        return AssemblingMachineRecipes.resolve(server, held, spec())
                .filter(holder -> AssemblingMachineRecipes.fits(holder.value(), spec()))
                .map(holder -> {
                    List<SizedFluidIngredient> ingredients = holder.value().fluidIngredients();
                    for (int index = 0; index < ingredients.size(); index++) {
                        if (ingredients.get(index).ingredient().test(resource.toStack(1))) {
                            return index;
                        }
                    }
                    return -1;
                })
                .orElse(-1);
    }

    /** The Overload Limit left in input tank {@code index}, whatever the crafting speed (#519). Server only. */
    public int fluidOverloadRoom(int index) {
        if (!(level instanceof ServerLevel server)) {
            return 0;
        }
        return AssemblingMachineRecipes.resolve(server, held, spec())
                .map(holder -> holder.value().fluidIngredients())
                .filter(ingredients -> index < ingredients.size())
                .map(ingredients -> OverloadLimit.get().fluidRoom(ingredients.get(index).amount(),
                        tank.getAmountAsInt(index)))
                .orElse(0);
    }

    /** Output tank {@code result}'s volume in mB as the Held recipe sizes it (#520). */
    public int outputTankVolume(int result) {
        return capacity(spec(), FLUID_INPUTS + result);
    }

    /** Output tank {@code result}'s fill in mB. */
    public long outputTankAmount(int result) {
        return tank.getAmountAsLong(FLUID_INPUTS + result);
    }

    public boolean isOutputTank(int index) {
        return index >= FLUID_INPUTS && index - FLUID_INPUTS < spec().fluidOutputs().size();
    }

    /** The fluid the Held recipe takes, which is the only one its tank can hold, or empty. Server only. */
    public Optional<Fluid> heldFluid() {
        if (!(level instanceof ServerLevel server)) {
            return Optional.empty();
        }
        return AssemblingMachineRecipes.resolve(server, held, spec())
                .flatMap(holder -> AssemblingMachineRecipes.firstFluid(holder.value().fluidIngredients()));
    }

    /** The input tanks, then the output tanks; the fluid face guards them. */
    public ResourceHandler<FluidResource> tank() {
        return tank;
    }

    /** Refused: {@code FILL_EVENLY} spreads a per-slot insert past the input filter (ADR-0074). */
    @Override
    public void cycleInputMode() {
    }

    /** Whether {@link #heldRecipe} names a recipe the server has loaded. */
    public boolean heldRecipeResolves() {
        return level instanceof ServerLevel server
                && AssemblingMachineRecipes.resolve(server, held, spec()).isPresent();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(HELD_KEY, HeldRecipe.CODEC, held);
        tank.serialize(output.child(TANK_KEY));
    }

    /** The id only. It is resolved when asked, never here, where the recipes may not be loaded. */
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // Oritech restores its input mode from the save; keep it pinned (ADR-0074).
        inventoryInputMode = InventoryInputMode.FILL_LEFT_TO_RIGHT;
        held = input.read(HELD_KEY, HeldRecipe.CODEC).orElse(HeldRecipe.NONE);
        tank.deserialize(input.childOrEmpty(TANK_KEY));
    }

    @Override
    public List<Vec3i> getAddonSlots() {
        return chassis().addonSlots().stream()
                .map(slot -> new Vec3i(slot.x(), slot.y(), slot.z()))
                .toList();
    }
}
