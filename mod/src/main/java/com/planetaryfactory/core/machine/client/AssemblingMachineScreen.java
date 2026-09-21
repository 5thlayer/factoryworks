package com.planetaryfactory.core.machine.client;

import com.planetaryfactory.core.PFItems;
import com.planetaryfactory.core.compat.emi.HeldRecipeTooltip;
import com.planetaryfactory.core.machine.AssemblingMachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * The Assembling Machine's screen (#327): the Held recipe, its five slots, and the craft's progress.
 *
 * <p>No recipe is picked here: the recipe viewer is the only picker, through EMI's Fill Recipe
 * (ADR-0073, #336). The Held recipe heads the screen as its result's icon and name; with EMI loaded
 * the icon's tooltip carries the recipe the way EMI's own recipe-bearing stacks do, and a click
 * opens it. A recipe the team has not researched is marked locked -- the Lock annotation policy is
 * to annotate, never to hide. Between the inputs and the output, a bar and a percentage show how
 * far the craft under way is, as Factorio's machine window does. A tab above the panel carries the
 * machine's icon and name, the header every Oritech machine screen has.
 *
 * <p>Whether this draws correctly is a human check on delivery; no check here claims it.
 */
public class AssemblingMachineScreen extends AbstractContainerScreen<AssemblingMachineMenu> {

    // Oritech's machine-screen palette, read off its gui_base.png and itemslot.png.
    private static final int PANEL = 0xFFD0D1D4;
    private static final int FRAME = 0xFF1E1E1F;
    private static final int FRAME_LIGHT = 0xFFF1F1F1;
    private static final int FRAME_SHADOW = 0xFF58585A;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int SLOT_DARK = 0xFF373737;
    private static final int SLOT_LIGHT = 0xFFFFFFFF;
    private static final int BAR = 0xFF5DA05D;
    private static final int TEXT = 0xFF404040;
    private static final int BAR_TEXT = 0xFFFFFFFF;
    private static final int LOCKED_TEXT = 0xFFA02020;

    private static final int TAB_X = 36;
    private static final int TAB_SIZE = 26;
    private static final int TITLE_HEIGHT = 16;

    private static final int HELD_X = 8;
    private static final int HELD_Y = 17;
    private static final int BAR_X = 84;
    private static final int BAR_WIDTH = 60;

    private static final boolean EMI = ModList.get().isLoaded("emi");

    public AssemblingMachineScreen(AssemblingMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, AssemblingMachineMenu.INVENTORY_Y + 83);
        inventoryLabelY = AssemblingMachineMenu.INVENTORY_Y - 11;
    }

    private static final Component NAME = Component.translatable("block.planetaryfactory.assembling_machine");
    private static final ItemStack ICON = new ItemStack(PFItems.ASSEMBLING_MACHINE.get());

    /** Oritech's header: the machine's icon in a tab on the panel's top edge, its name beside it. */
    private void extractTab(GuiGraphicsExtractor graphics) {
        int x = leftPos + TAB_X;
        int y = topPos - TAB_SIZE + 4;
        frame(graphics, x, y, TAB_SIZE, TAB_SIZE);
        graphics.item(ICON, x + (TAB_SIZE - 16) / 2, y + (TAB_SIZE - 16) / 2);

        int titleX = x + TAB_SIZE + 2;
        int titleY = y + 2;
        int titleWidth = font.width(NAME) + 12;
        frame(graphics, titleX, titleY, titleWidth, TITLE_HEIGHT);
        graphics.text(font, NAME, titleX + 6, titleY + (TITLE_HEIGHT - 8) / 2, TEXT, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // The world dims behind the panel as it does behind the inventory; skipping super left it bright.
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        frame(graphics, leftPos, topPos, imageWidth, imageHeight);
        extractTab(graphics);
        for (Slot slot : menu.slots) {
            recess(graphics, leftPos + slot.x, topPos + slot.y, 16);
        }
        int x = leftPos + BAR_X;
        int y = topPos + AssemblingMachineMenu.INPUT_Y;
        recess(graphics, x, y, BAR_WIDTH);
        graphics.fill(x, y, x + Math.round(BAR_WIDTH * menu.progress()), y + 16, BAR);
    }

    /** A slot's bevel, as Oritech's itemslot.png draws it: dark above and left, light below and right. */
    private static void recess(GuiGraphicsExtractor graphics, int x, int y, int width) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + 17, SLOT);
        graphics.fill(x - 1, y - 1, x + width, y, SLOT_DARK);
        graphics.fill(x - 1, y, x, y + 16, SLOT_DARK);
        graphics.fill(x, y + 16, x + width + 1, y + 17, SLOT_LIGHT);
        graphics.fill(x + width, y, x + width + 1, y + 16, SLOT_LIGHT);
    }

    /** Oritech's panel: a dark outline, a light inner edge, and a two-pixel shadow along the bottom. */
    private static void frame(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, FRAME);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, FRAME_SHADOW);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 3, FRAME_LIGHT);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 4, PANEL);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        String percent = Math.round(menu.progress() * 100) + "%";
        graphics.text(font, percent, BAR_X + BAR_WIDTH - 2 - font.width(percent),
                AssemblingMachineMenu.INPUT_Y + 4, BAR_TEXT, true);

        AssemblingMachineMenu.Entry held = menu.held();
        if (held == null) {
            String key = menu.holdsUnknown()
                    ? "gui.planetaryfactory.assembling_machine.unknown_recipe"
                    : "gui.planetaryfactory.assembling_machine.no_recipe";
            graphics.text(font, Component.translatable(key), HELD_X, HELD_Y + 4, TEXT, false);
            return;
        }
        graphics.item(held.icon(), HELD_X, HELD_Y);
        graphics.text(font, held.icon().getHoverName(), HELD_X + 20, HELD_Y + 4, TEXT, false);
        if (held.choice().locked()) {
            Component locked = Component.translatable("gui.planetaryfactory.assembling_machine.locked");
            graphics.text(font, locked, imageWidth - 8 - font.width(locked), HELD_Y + 4, LOCKED_TEXT, false);
        }
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
