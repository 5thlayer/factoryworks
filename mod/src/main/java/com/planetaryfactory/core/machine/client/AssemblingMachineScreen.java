package com.planetaryfactory.core.machine.client;

import com.planetaryfactory.core.compat.emi.HeldRecipeTooltip;
import com.planetaryfactory.core.machine.AssemblingMachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.fml.ModList;

/**
 * The Assembling Machine's screen (#327): the Held recipe, its five slots, and the craft's progress.
 *
 * <p>No recipe is picked here: the recipe viewer is the only picker, through EMI's Fill Recipe
 * (ADR-0073, #336). The Held recipe heads the screen as its result's icon and name; with EMI loaded
 * the icon's tooltip carries the recipe the way EMI's own recipe-bearing stacks do, and a click
 * opens it. A recipe the team has not researched is marked locked -- the Lock annotation policy is
 * to annotate, never to hide. Between the inputs and the output, a bar and a percentage show how
 * far the craft under way is, as Factorio's machine window does.
 *
 * <p>Whether this draws correctly is a human check on delivery; no check here claims it.
 */
public class AssemblingMachineScreen extends AbstractContainerScreen<AssemblingMachineMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int SLOT_EDGE = 0xFF373737;
    private static final int BAR = 0xFF5DA05D;
    private static final int TEXT = 0xFF404040;
    private static final int BAR_TEXT = 0xFFFFFFFF;
    private static final int LOCKED_TEXT = 0xFFA02020;

    private static final int HELD_X = 8;
    private static final int HELD_Y = 17;
    private static final int BAR_X = 84;
    private static final int BAR_WIDTH = 60;

    private static final boolean EMI = ModList.get().isLoaded("emi");

    public AssemblingMachineScreen(AssemblingMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, AssemblingMachineMenu.INVENTORY_Y + 83);
        inventoryLabelY = AssemblingMachineMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        for (Slot slot : menu.slots) {
            recess(graphics, leftPos + slot.x, topPos + slot.y, 16);
        }
        int x = leftPos + BAR_X;
        int y = topPos + AssemblingMachineMenu.INPUT_Y;
        recess(graphics, x, y, BAR_WIDTH);
        graphics.fill(x, y, x + Math.round(BAR_WIDTH * menu.progress()), y + 16, BAR);
    }

    private static void recess(GuiGraphicsExtractor graphics, int x, int y, int width) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + 17, SLOT_EDGE);
        graphics.fill(x, y, x + width, y + 16, SLOT);
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
