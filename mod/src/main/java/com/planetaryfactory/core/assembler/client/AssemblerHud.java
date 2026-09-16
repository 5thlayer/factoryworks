package com.planetaryfactory.core.assembler.client;

import com.planetaryfactory.core.assembler.AssemblerQueueView;
import com.planetaryfactory.core.network.QueueSyncPacket;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.minecraft.network.chat.Component;

/**
 * The queue beside the hotbar, so a plan running while the player is playing is visible without
 * opening anything.
 *
 * <p>The queue keeps going with every screen shut -- that is the point of a queue rather than a
 * crafting grid (ADR-0038) -- and until this existed the only way to see it was to stop playing and
 * open a screen, which is the opposite of what a background queue is for.
 *
 * <p>Read-only, and not because interaction was hard: the cursor is held by the camera while the
 * HUD is up, so a button here would be a button nothing can press. Cancelling is on the inventory
 * screen, where the pointer is, and this hides while that screen is open.
 *
 * <p>It draws {@link AssemblerQueueView}, the same client copy the inventory screen draws, which the server
 * re-syncs four times a second whether or not a screen is open.
 */
final class AssemblerHud implements GuiLayer {

    /**
     * How many plans the corner shows.
     *
     * <p>A cap rather than a scroll: this is glanceable status beside the hotbar, and a queue tall
     * enough to reach the crosshair would be in the way of the game it is reporting on.
     */
    private static final int MAX_ROWS = 5;

    private static final int ROW_HEIGHT = 20;
    private static final int WIDTH = 132;

    /** Vanilla's hotbar: 182 wide, centred, its top 22 pixels off the bottom. */
    private static final int HOTBAR_HALF_WIDTH = 91;

    private static final int HOTBAR_HEIGHT = 22;

    /**
     * Clear of the offhand slot, which vanilla puts 29 pixels beyond the hotbar's left edge.
     *
     * <p>The queue sits left of the hotbar at the hotbar's own height, so the offhand slot is the
     * one thing it can collide with -- and it appears and disappears with what the player is
     * holding, which is the worst kind of collision to leave to chance.
     */
    private static final int CLEAR_OF_OFFHAND = 32;


    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.options.hideGui || client.player == null) return;
        // The inventory screen draws the queue itself, where it can be clicked (#290).
        if (client.screen instanceof InventoryScreen) return;
        List<QueueSyncPacket.Entry> entries = AssemblerQueueView.entries();
        if (entries.isEmpty()) return;

        int shown = Math.min(MAX_ROWS, entries.size());
        // Left of the hotbar, bottom row level with it, stacked upward from there.
        int left = graphics.guiWidth() / 2 - HOTBAR_HALF_WIDTH - CLEAR_OF_OFFHAND - WIDTH;
        int bottom = graphics.guiHeight() - (HOTBAR_HEIGHT - ROW_HEIGHT) / 2 - ROW_HEIGHT;
        int top = bottom - (shown - 1) * ROW_HEIGHT;

        Font font = client.font;
        for (int index = 0; index < shown; index++) {
            QueueSyncPacket.Entry entry = entries.get(index);
            int y = top + index * ROW_HEIGHT;
            QueueRow.draw(graphics, font, entry, index == 0, left, y, 0xFFFFFFFF, true);
        }
        if (entries.size() > shown) {
            Component more = Component.translatable(
                    "planetaryfactory_core.assembler.and_more", entries.size() - shown);
            graphics.text(font, more, left, top - 12, 0xFF999999, true);
        }
    }
}
