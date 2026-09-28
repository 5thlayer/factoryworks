package com.planetaryfactory.core.machine.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.machine.ChassisMachineBlock;
import com.planetaryfactory.core.machine.AssemblingTier;
import com.planetaryfactory.core.compat.emi.HeldRecipeTooltip;
import com.planetaryfactory.core.machine.AssemblingMachineBlockEntity;
import com.planetaryfactory.core.machine.AssemblingMachineMenu;
import com.planetaryfactory.core.machine.AssemblingStatus;
import com.planetaryfactory.core.machine.AssemblingStatusText;
import com.planetaryfactory.core.machine.MachineSpec;
import com.planetaryfactory.core.machine.TankRow;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.neoforge.fluids.FluidStack;
import rearth.oritech.client.renderers.util.RenderHelpers;
import rearth.oritech.util.ColorHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import org.joml.Matrix3x2f;

import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.client.ui.render.LargeItemRenderState;

/**
 * The Assembling Machine's screen (#327): the Held recipe, its five slots, the craft's progress, the
 * machine's energy and its status (#332).
 *
 * <p>No recipe is picked here: the recipe viewer is the only picker, through EMI's Fill Recipe
 * (ADR-0073, #336). The Held recipe heads the screen as its result's icon and name; with EMI loaded
 * the icon's tooltip carries the recipe the way EMI's own recipe-bearing stacks do, and a click
 * opens it. A recipe the team has not researched is marked locked -- the Lock annotation policy is
 * to annotate, never to hide. Between the inputs and the output, a bar and a percentage show how
 * far the craft under way is, as Factorio's machine window does. A tab above the panel carries the
 * machine's icon and name, the header every Oritech machine screen has. Under the slots, a bar shows
 * the stored FE, with the stored and maximum FE and the Held recipe's draw in its tooltip, and one
 * line states the {@link AssemblingStatus}: what is wrong and what fixes it (ADR-0073). On a machine
 * with tanks a row above the energy bar holds one bar per tank, whose tooltip names its fluid.
 *
 * <p>The Held recipe is also drawn in its slots (ADR-0073): each ingredient ghosted in the input slot
 * {@link com.planetaryfactory.core.machine.AssemblingInputSlots} gives it, with the count one craft
 * needs, and the product in the output. A tag ingredient cycles through its members. An input slot
 * holding less than one craft is red, as Factorio's are.
 *
 * <p>Whether this draws correctly is a human check on delivery; no check here claims it. So is the
 * client's input slot refusing a wrong item in the hand (#334): the menu's slot prediction runs in
 * no harness, and the server half is {@code AssemblingMachineTests}'.
 */
public class AssemblingMachineScreen extends AbstractContainerScreen<AssemblingMachineMenu> {

    // Oritech's slot bevel, read off its itemslot.png.
    private static final int SLOT = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int BAR = 0xFF5DA05D;
    // Jade's energy bar sprite, tiled 1:1 so its stripes keep their width.
    private static final Identifier ENERGY_SPRITE = Identifier.fromNamespaceAndPath("jade", "energy_progress");
    private static final int ENERGY_EMPTY_TINT = 0xFF404040;
    // For a tank whose recipe no longer resolves, so its fluid is unknown.
    private static final int FLUID = 0xFF3B6FE0;
    private static final int SPRITE_SIZE = 16;
    private static final int TEXT = 0xFF404040;
    private static final int BAR_TEXT = 0xFFFFFFFF;
    private static final int PROBLEM_TEXT = 0xFFA02020;
    private static final int SHORT = 0xFFB84C4C;
    // Half the slot's own colour over a ghost, so it reads as a placeholder rather than an item.
    private static final int GHOST_VEIL = 0x808B8B8B;
    private static final int SHORT_VEIL = 0x80B84C4C;
    private static final long CYCLE_MILLIS = 1000;

    // Oritech's header, from OritechWidgetScreen.addTitle: a 28px icon on a panel padded
    // (0 top, 2 right, 3 bottom, 2 left), and a 14px label padded (5, 0, 1, 10) six pixels past it.
    private static final int TITLE_Y = -27;
    private static final int ICON_SIZE = 28;
    private static final int LABEL_HEIGHT = 14;

    private static final int HELD_X = 8;
    private static final int HELD_Y = 17;
    private static final int BAR_X = 84;
    private static final int BAR_WIDTH = 60;
    private static final int ENERGY_X = 8;
    private static final int ENERGY_WIDTH = 160;
    private static final int ENERGY_HEIGHT = 12;
    private static final int BAR_GAP = 8;
    private static final int STATUS_X = 8;
    private static final int STATUS_WIDTH = 160;

    private static final boolean EMI = ModList.get().isLoaded("emi");

    private final Component name;
    private final ItemStack icon;

    public AssemblingMachineScreen(AssemblingMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, menu.inventoryY() + 83);
        inventoryLabelY = menu.inventoryY() - 11;
        Block block = playerInventory.player.level().getBlockState(menu.pos()).getBlock();
        if (!(block instanceof ChassisMachineBlock)) {
            block = PFBlocks.assemblingMachine(AssemblingTier.ONE).get();
        }
        name = block.getName();
        icon = new ItemStack(block.asItem());
    }

    /**
     * Oritech's header, drawn as {@code OritechWidgetScreen.addTitle} lays it out: the machine's
     * icon on a panel tab at the top edge and its name on a panel beside it, right-aligned to the
     * panel's edge when the name is longer than fifteen characters. The icon is Oritech's own large-item render
     * state: a pose-scaled item flickers in 26.1's GUI renderer.
     */
    private void extractTab(GuiGraphicsExtractor graphics) {
        int labelWidth = font.width(name) + 10;
        // Oritech's combined width leaves out the label's six-pixel gap, so its right-aligned
        // header ends six pixels past the panel -- into EMI's column. This one counts the gap.
        int combined = ICON_SIZE + 2 + 6 + labelWidth;
        int x = leftPos + (name.getString().length() > 15
                ? imageWidth - combined
                : (imageWidth - combined) * 65 / 100);
        int y = topPos + TITLE_Y;

        int labelX = x + ICON_SIZE + 2 + 6;
        int labelY = y + 9;
        OritechSurface.PANEL.render(graphics, labelX - 10, labelY - 5, labelWidth + 10, LABEL_HEIGHT + 6);
        graphics.text(font, name, labelX, labelY, TEXT, false);

        OritechSurface.PANEL.render(graphics, x - 2, y, ICON_SIZE + 4, ICON_SIZE + 3);
        graphics.submitPictureInPictureRenderState(new LargeItemRenderState(icon.copy(), x, y,
                x + ICON_SIZE, y + ICON_SIZE, ICON_SIZE, new Matrix3x2f(graphics.pose()),
                graphics.peekScissorStack()));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // The world dims behind the panel as it does behind the inventory; skipping super left it bright.
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        OritechSurface.PANEL.render(graphics, leftPos, topPos, imageWidth, imageHeight);
        extractTab(graphics);
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            recess(graphics, leftPos + slot.x, topPos + slot.y, 16, 16);
            if (isShort(slot)) {
                graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, SHORT);
            }
        }
        int x = leftPos + BAR_X;
        int y = topPos + AssemblingMachineMenu.INPUT_Y;
        recess(graphics, x, y, BAR_WIDTH, 16);
        graphics.fill(x, y, x + Math.round(BAR_WIDTH * menu.progress()), y + 16, BAR);

        int energyX = leftPos + ENERGY_X;
        int energyY = topPos + menu.energyY();
        int width = ENERGY_WIDTH;
        recess(graphics, energyX, energyY, width, ENERGY_HEIGHT);
        drawEnergy(graphics, energyX, energyY, width, Math.round(width * charge()));
        for (FluidBar bar : fluidBars()) {
            recess(graphics, bar.x(), bar.y(), bar.width(), bar.height());
            float fill = bar.capacity() <= 0 ? 0f : Math.min(1f, (float) bar.amount() / bar.capacity());
            int filled = Math.round(bar.width() * fill);
            if (bar.fluid().isPresent()) {
                drawFluid(graphics, bar.fluid().get(), bar.x(), bar.y(), filled, bar.height());
            } else {
                graphics.fill(bar.x(), bar.y(), bar.x() + filled, bar.y() + bar.height(), FLUID);
            }
        }
    }

    private static void drawEnergy(GuiGraphicsExtractor graphics, int x, int y, int width, int filled) {
        energyTiles(graphics, x, y, width, ENERGY_EMPTY_TINT);
        if (filled > 0) {
            energyTiles(graphics, x, y, filled, 0xFFFFFFFF);
        }
    }

    private static void energyTiles(GuiGraphicsExtractor graphics, int x, int y, int width, int tint) {
        graphics.enableScissor(x, y, x + width, y + ENERGY_HEIGHT);
        for (int tileX = x; tileX < x + width; tileX += SPRITE_SIZE) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ENERGY_SPRITE, tileX, y, SPRITE_SIZE, SPRITE_SIZE, tint);
        }
        graphics.disableScissor();
    }

    /** The fluid's own sprite and tint, as Oritech's tanks draw it, so the pack's retint shows (ADR-0067). */
    private static void drawFluid(GuiGraphicsExtractor graphics, Fluid fluid, int x, int y, int width, int height) {
        if (width <= 0) {
            return;
        }
        TextureAtlasSprite sprite = RenderHelpers.getFluidSprite(fluid);
        int tint = fluidTint(fluid);
        graphics.enableScissor(x, y, x + width, y + height);
        for (int tileX = x; tileX < x + width; tileX += SPRITE_SIZE) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, tileX, y, SPRITE_SIZE, SPRITE_SIZE, tint);
        }
        graphics.disableScissor();
    }

    private static int fluidTint(Fluid fluid) {
        return ColorHelper.makeOpaque(ColorHelper.getFluidTint(new FluidStack(fluid, 1)));
    }

    /** A tank's bar on the energy row, in screen coordinates, and the fluid the Held recipe puts in it. */
    public record FluidBar(Optional<Fluid> fluid, int amount, int capacity, int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    /**
     * The tank row: input tanks under the input slots, output tanks under the progress bar and the
     * output, so each tank keeps a readable width however many the machine has (ADR-0096).
     */
    private List<FluidBar> fluidBars() {
        MachineSpec spec = menu.spec(minecraft.player);
        AssemblingMachineMenu.Entry held = menu.held();
        int y = topPos + AssemblingMachineMenu.TANK_Y;
        List<FluidBar> bars = new ArrayList<>();
        List<TankRow.Bar> inputs = TankRow.split(ENERGY_X, BAR_X - BAR_GAP - ENERGY_X, spec.fluidInputs().size());
        for (int tank = 0; tank < inputs.size(); tank++) {
            TankRow.Bar bar = inputs.get(tank);
            bars.add(new FluidBar(held == null ? Optional.empty() : held.inputFluid(tank), menu.inputAmount(tank),
                    spec.fluidInputVolume(tank), leftPos + bar.x(), y, bar.width(), ENERGY_HEIGHT));
        }
        List<TankRow.Bar> outputs = TankRow.split(BAR_X, ENERGY_X + ENERGY_WIDTH - BAR_X, spec.fluidOutputs().size());
        for (int tank = 0; tank < outputs.size(); tank++) {
            TankRow.Bar bar = outputs.get(tank);
            bars.add(new FluidBar(held == null ? Optional.empty() : held.outputFluid(tank), menu.outputAmount(tank),
                    spec.fluidOutputVolume(tank), leftPos + bar.x(), y, bar.width(), ENERGY_HEIGHT));
        }
        return bars;
    }

    /** The fluid of the bar under the mouse, for EMI's and JEI's recipe and usage keys. */
    public Optional<FluidBar> fluidBarAt(double mouseX, double mouseY) {
        return fluidBars().stream()
                .filter(bar -> bar.fluid().isPresent() && bar.contains(mouseX, mouseY))
                .findFirst();
    }

    private float charge() {
        long capacity = menu.capacityFe();
        return capacity <= 0 ? 0f : Math.min(1f, (float) menu.storedFe() / capacity);
    }

    /** A slot's bevel, as Oritech's itemslot.png draws it: dark above and left, light below and right. */
    private static void recess(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, SLOT);
        graphics.fill(x - 1, y - 1, x + width, y, SLOT_DARK);
        graphics.fill(x - 1, y, x, y + height, SLOT_DARK);
        graphics.fill(x, y + height, x + width + 1, y + height + 1, SLOT_LIGHT);
        graphics.fill(x + width, y, x + width + 1, y + height, SLOT_LIGHT);
    }

    private boolean isShort(Slot slot) {
        return slot.index < AssemblingMachineBlockEntity.INPUTS && menu.isShort(slot.index);
    }

    @Override
    protected void extractSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY) {
        ItemStack ghost = ghost(slot);
        if (!ghost.isEmpty()) {
            graphics.fakeItem(ghost, slot.x, slot.y);
            graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, isShort(slot) ? SHORT_VEIL : GHOST_VEIL);
            graphics.itemDecorations(font, ghost, slot.x, slot.y);
        }
        super.extractSlot(graphics, slot, mouseX, mouseY);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (FluidBar bar : fluidBars()) {
            if (bar.contains(mouseX, mouseY)) {
                graphics.setTooltipForNextFrame(font,
                        AssemblingStatusText.tank(bar.fluid(), bar.amount(), bar.capacity()), mouseX, mouseY);
                return;
            }
        }
        if (over(mouseX, mouseY, ENERGY_X, menu.energyY(), ENERGY_WIDTH, ENERGY_HEIGHT)) {
            graphics.setTooltipForNextFrame(font, List.of(
                    Component.translatable("gui.planetaryfactory.assembling_machine.energy",
                            String.format("%,d", menu.storedFe()), String.format("%,d", menu.capacityFe())),
                    Component.translatable("gui.planetaryfactory.assembling_machine.draw",
                            menu.drawTenths() / 10 + "." + menu.drawTenths() % 10)
                            .withStyle(ChatFormatting.GRAY)),
                    Optional.empty(), mouseX, mouseY);
            return;
        }
        if (over(mouseX, mouseY, STATUS_X, menu.statusY(), STATUS_WIDTH, font.lineHeight)) {
            graphics.setTooltipForNextFrame(font, AssemblingStatusText.of(menu.status(), menu.heldFluid()), mouseX, mouseY);
            return;
        }
        ItemStack ghost = hoveredSlot == null || !menu.getCarried().isEmpty() ? ItemStack.EMPTY : ghost(hoveredSlot);
        if (ghost.isEmpty()) {
            super.extractTooltip(graphics, mouseX, mouseY);
            return;
        }
        graphics.setTooltipForNextFrame(font, List.of(ghost.getHoverName(),
                Component.translatable("gui.planetaryfactory.assembling_machine.per_craft", ghost.getCount())
                        .withStyle(ChatFormatting.GRAY)),
                Optional.empty(), mouseX, mouseY);
    }

    private boolean over(double mouseX, double mouseY, int x, int y, int width, int height) {
        double relX = mouseX - leftPos - x;
        double relY = mouseY - topPos - y;
        return relX >= 0 && relX < width && relY >= 0 && relY < height;
    }

    /** What an empty slot shows of the Held recipe, or empty: a real stack hides the ghost. */
    private ItemStack ghost(Slot slot) {
        if (slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        if (slot.index == AssemblingMachineBlockEntity.OUTPUT) {
            AssemblingMachineMenu.Entry held = menu.held();
            return held == null ? ItemStack.EMPTY : held.icon();
        }
        return menu.slotIngredient(slot.index).map(AssemblingMachineScreen::cycled).orElse(ItemStack.EMPTY);
    }

    private static ItemStack cycled(SizedIngredient sized) {
        List<Holder<Item>> members = sized.ingredient().items().toList();
        if (members.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(members.get((int) (Util.getMillis() / CYCLE_MILLIS % members.size())), sized.count());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        String percent = Math.round(menu.progress() * 100) + "%";
        graphics.text(font, percent, BAR_X + BAR_WIDTH - 2 - font.width(percent),
                AssemblingMachineMenu.INPUT_Y + 4, BAR_TEXT, true);

        AssemblingStatus status = menu.status();
        graphics.text(font, fitted(AssemblingStatusText.of(status, menu.heldFluid()), STATUS_WIDTH), STATUS_X,
                menu.statusY(), status.problem() ? PROBLEM_TEXT : TEXT, false);

        int right = imageWidth - 8;
        AssemblingMachineMenu.Entry held = menu.held();
        if (held == null) {
            String key = menu.holdsUnknown()
                    ? "gui.planetaryfactory.assembling_machine.unknown_recipe"
                    : "gui.planetaryfactory.assembling_machine.no_recipe";
            graphics.text(font, fitted(Component.translatable(key), right - HELD_X), HELD_X, HELD_Y + 4,
                    TEXT, false);
            return;
        }
        if (!held.icon().isEmpty()) {
            graphics.item(held.icon(), HELD_X, HELD_Y);
        } else {
            held.outputFluid(0).ifPresent(fluid -> graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                    RenderHelpers.getFluidSprite(fluid), HELD_X, HELD_Y, SPRITE_SIZE, SPRITE_SIZE, fluidTint(fluid)));
        }
        int nameRight = right;
        if (held.choice().locked()) {
            Component locked = Component.translatable("gui.planetaryfactory.assembling_machine.locked");
            nameRight -= font.width(locked) + 4;
            graphics.text(font, locked, right - font.width(locked), HELD_Y + 4, PROBLEM_TEXT, false);
        }
        graphics.text(font, fitted(held.name(), nameRight - HELD_X - 20), HELD_X + 20, HELD_Y + 4,
                TEXT, false);
    }

    /** Cut to {@code width} with an ellipsis; translations and item names vary in length. */
    private String fitted(Component text, int width) {
        String plain = text.getString();
        if (font.width(plain) <= width) {
            return plain;
        }
        return font.plainSubstrByWidth(plain, width - font.width("...")) + "...";
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        AssemblingMachineMenu.Entry held = hoveredHeld(mouseX, mouseY);
        if (held == null) {
            return;
        }
        if (EMI) {
            graphics.nextStratum();
            graphics.tooltip(font, HeldRecipeTooltip.components(held.icon(), held.choice().id()),
                    mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        } else {
            graphics.setTooltipForNextFrame(font, held.icon(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        AssemblingMachineMenu.Entry held = hoveredHeld(event.x(), event.y());
        if (held != null && EMI && HeldRecipeTooltip.display(held.choice().id())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** The Held recipe if the mouse is over its icon, else null. */
    private AssemblingMachineMenu.Entry hoveredHeld(double mouseX, double mouseY) {
        AssemblingMachineMenu.Entry held = menu.held();
        double x = mouseX - leftPos - HELD_X;
        double y = mouseY - topPos - HELD_Y;
        boolean over = x >= 0 && x < 16 && y >= 0 && y < 16;
        return over && held != null && !held.icon().isEmpty() ? held : null;
    }
}
