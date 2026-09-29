package com.factoryworks.core.start.client;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * The quest book's tooltip points at the inventory, on every copy of the book (#496). Matched by id
 * because FTB Quests is not a compile dependency.
 */
public final class QuestBookTooltip {
    static final String BOOK = "ftbquests:book";
    static final String KEY = "tooltip.factoryworks.quest_book";

    private QuestBookTooltip() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(QuestBookTooltip::onTooltip);
    }

    private static void onTooltip(ItemTooltipEvent event) {
        if (!BOOK.equals(BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()).toString())) {
            return;
        }
        event.getToolTip().add(Component.translatable(KEY)
                .withStyle(ChatFormatting.YELLOW));
    }
}
