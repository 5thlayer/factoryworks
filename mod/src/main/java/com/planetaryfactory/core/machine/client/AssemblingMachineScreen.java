package com.planetaryfactory.core.machine.client;

import java.util.ArrayList;
import java.util.List;

import com.planetaryfactory.core.machine.AssemblingMachineMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The Assembling Machine's screen (#327): what it holds, its five slots, and the recipe widget.
 *
 * <p>The widget is a scrolling grid of every assembling recipe's result. A press sets the Held
 * recipe through vanilla's menu button click; the machine hands back what its inputs held. A
 * recipe the team has not researched is drawn and pressable, shaded and tooltipped as locked --
 * the Lock annotation policy is to annotate, never to hide.
 *
 * <p>Whether this draws correctly is a human check on delivery; no check here claims it.
 */
public class AssemblingMachineScreen extends AbstractContainerScreen<AssemblingMachineMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int SLOT = 0xFF8B8B8B;
    private static final int SLOT_EDGE = 0xFF373737;
    private static final int HELD_CELL = 0xFF5DA05D;
    private static final int LOCKED_SHADE = 0x80A02020;
    private static final int TEXT = 0xFF404040;
    private static final int LOCKED_TEXT = 0xFFA02020;

    private static final int HELD_Y = 17;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 60;
    private static final int COLUMNS = 9;
    private static final int ROWS = 3;
    private static final int CELL = 18;

    private int firstRow;

    public AssemblingMachineScreen(AssemblingMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
        inventoryLabelY = AssemblingMachineMenu.INVENTORY_Y - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        for (Slot slot : menu.slots) {
            recess(graphics, leftPos + slot.x, topPos + slot.y);
        }

        List<AssemblingMachineMenu.Entry> entries = menu.entries();
        AssemblingMachineMenu.Entry held = menu.held();
        for (int cell = 0; cell < COLUMNS * ROWS; cell++) {
            int index = firstRow * COLUMNS + cell;
            int x = leftPos + GRID_X + (cell % COLUMNS) * CELL;
            int y = topPos + GRID_Y + (cell / COLUMNS) * CELL;
            recess(graphics, x, y);
            if (index >= entries.size()) {
                continue;
            }
            AssemblingMachineMenu.Entry entry = entries.get(index);
            if (entry == held) {
                graphics.fill(x, y, x + 16, y + 16, HELD_CELL);
            }
            graphics.item(entry.icon(), x, y);
            if (entry.choice().locked()) {
                graphics.fill(x, y, x + 16, y + 16, LOCKED_SHADE);
            }
        }
    }

    private static void recess(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
        graphics.fill(x, y, x + 16, y + 16, SLOT);
    }

    /** What the machine holds -- drawn whether or not it has anything to make it with. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        AssemblingMachineMenu.Entry held = menu.held();
        if (held == null) {
            String key = menu.holdsUnknown()
                    ? "gui.planetaryfactory.assembling_machine.unknown_recipe"
                    : "gui.planetaryfactory.assembling_machine.no_recipe";
            graphics.text(font, Component.translatable(key),
                    8, HELD_Y + 4, TEXT, false);
            return;
        }
        graphics.item(held.icon(), 8, HELD_Y);
        graphics.text(font, held.icon().getHoverName(), 28, HELD_Y + 4, TEXT, false);
        if (held.choice().locked()) {
            graphics.text(font, Component.translatable("gui.planetaryfactory.assembling_machine.locked"),
                    120, HELD_Y + 4, LOCKED_TEXT, false);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int index = entryAt(mouseX, mouseY);
        if (index < 0) {
            return;
        }
        AssemblingMachineMenu.Entry entry = menu.entries().get(index);
        List<Component> lines = new ArrayList<>();
        lines.add(entry.icon().getHoverName());
        lines.add(Component.literal(entry.choice().id()).withStyle(ChatFormatting.DARK_GRAY));
        if (entry.choice().locked()) {
            lines.add(Component.translatable("gui.planetaryfactory.assembling_machine.locked")
                    .withStyle(ChatFormatting.RED));
        }
        graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int index = entryAt(event.x(), event.y());
        if (index >= 0 && event.button() == 0) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, index);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (overGrid(x, y)) {
            int lastRow = Math.max(0, (menu.entries().size() + COLUMNS - 1) / COLUMNS - ROWS);
            firstRow = Math.clamp(firstRow - (int) Math.signum(scrollY), 0, lastRow);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    private boolean overGrid(double x, double y) {
        double gridX = x - leftPos - GRID_X;
        double gridY = y - topPos - GRID_Y;
        return gridX >= 0 && gridX < COLUMNS * CELL && gridY >= 0 && gridY < ROWS * CELL;
    }

    /** The entry under the mouse, or -1. */
    private int entryAt(double x, double y) {
        if (!overGrid(x, y)) {
            return -1;
        }
        int column = (int) (x - leftPos - GRID_X) / CELL;
        int row = (int) (y - topPos - GRID_Y) / CELL;
        int index = (firstRow + row) * COLUMNS + column;
        return index < menu.entries().size() ? index : -1;
    }
}
