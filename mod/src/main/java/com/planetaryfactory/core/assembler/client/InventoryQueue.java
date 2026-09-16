package com.planetaryfactory.core.assembler.client;

import com.planetaryfactory.core.assembler.AssemblerQueueView;
import com.planetaryfactory.core.assembler.CancelClick;
import com.planetaryfactory.core.network.PlanCancelPacket;
import com.planetaryfactory.core.network.QueueSyncPacket;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The Personal Assembler's queue on the inventory screen, in the area the 2x2 grid left blank (#290).
 *
 * <p>The inventory screen is the Assembler: EMI's Fill Recipe queues from it, and this is where the
 * queue is watched and cancelled. A click on either icon of a row cancels that row's final item the
 * way Factorio's hand-craft queue does -- see {@link CancelClick}. What does not fit shows as a
 * "+N more" line, as Factorio does; a side panel is the follow-up if two rows prove too few.
 *
 * <p>Drawn on {@code Render.Post} rather than the container's foreground, so it lands over
 * {@code InventoryGridBlank}'s grey whichever listener runs first, and in screen coordinates.
 */
public final class InventoryQueue {

    /** The blanked area's left edge and the first row's top, in the screen's own coordinates. */
    private static final int LEFT = 98;
    private static final int TOP = 7;
    private static final int ROW_HEIGHT = 18;
    private static final int ROWS = 2;
    private static final int TEXT = 0xFF404040;

    private InventoryQueue() {
    }

    public static void onRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        List<QueueSyncPacket.Entry> entries = AssemblerQueueView.entries();
        if (entries.isEmpty()) return;
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int shown = Math.min(ROWS, entries.size());
        for (int index = 0; index < shown; index++) {
            QueueSyncPacket.Entry entry = entries.get(index);
            int y = rowTop(screen, index);
            QueueRow.draw(graphics, font, entry, index == 0, screen.getGuiLeft() + LEFT, y, TEXT, false);
        }
        if (entries.size() > shown) {
            graphics.text(font, Component.translatable("planetaryfactory_core.assembler.and_more", entries.size() - shown),
                    screen.getGuiLeft() + LEFT, rowTop(screen, shown) + 1, TEXT, false);
        }
    }

    /**
     * The hovered icon's tooltip. Set before the screen draws, because the screen spends its deferred
     * tooltip at the end of its own render, and one set on {@code Render.Post} is never shown.
     */
    public static void onTooltip(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        List<QueueSyncPacket.Entry> entries = AssemblerQueueView.entries();
        Font font = Minecraft.getInstance().font;
        for (int index = 0; index < Math.min(ROWS, entries.size()); index++) {
            QueueSyncPacket.Entry entry = entries.get(index);
            if (!QueueRow.layout(font, entry, screen.getGuiLeft() + LEFT)
                    .hit(event.getMouseX(), event.getMouseY(), rowTop(screen, index))) continue;
            event.getGuiGraphics().setComponentTooltipForNextFrame(font, List.of(
                    PlanItems.name(entry.rootItem()),
                    Component.translatable("planetaryfactory_core.assembler.cancel.left"),
                    Component.translatable("planetaryfactory_core.assembler.cancel.right"),
                    Component.translatable("planetaryfactory_core.assembler.cancel.shift")),
                    event.getMouseX(), event.getMouseY());
            return;
        }
    }

    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) return;
        List<QueueSyncPacket.Entry> entries = AssemblerQueueView.entries();
        Font font = Minecraft.getInstance().font;
        for (int index = 0; index < Math.min(ROWS, entries.size()); index++) {
            QueueSyncPacket.Entry entry = entries.get(index);
            int y = rowTop(screen, index);
            if (!QueueRow.layout(font, entry, screen.getGuiLeft() + LEFT).hit(event.getMouseX(), event.getMouseY(), y)) continue;
            int crafts = CancelClick.crafts(event.getButton(), event.getMouseButtonEvent().hasShiftDown());
            if (crafts == 0) return;
            ClientPacketDistributor.sendToServer(new PlanCancelPacket(entry.planId(), crafts));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            event.setCanceled(true);
            return;
        }
    }

    private static int rowTop(InventoryScreen screen, int index) {
        return screen.getGuiTop() + TOP + index * ROW_HEIGHT;
    }
}
