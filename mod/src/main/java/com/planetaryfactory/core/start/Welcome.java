package com.planetaryfactory.core.start;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * The first-join message: what the pack is, what Terra asks, and the opening's two surprises, for a
 * player who never opens the book (#494). A message, not a script (#100).
 */
public final class Welcome {

    public record Line(String key, ChatFormatting... style) {
        public Component component() {
            return Component.translatable(key).withStyle(style);
        }
    }

    public static final List<Line> LINES = List.of(
            new Line("message.planetaryfactory.welcome.title", ChatFormatting.GOLD, ChatFormatting.BOLD),
            new Line("message.planetaryfactory.welcome.pack", ChatFormatting.GRAY, ChatFormatting.ITALIC),
            new Line("message.planetaryfactory.welcome.terra", ChatFormatting.AQUA),
            new Line("message.planetaryfactory.welcome.crafting", ChatFormatting.YELLOW),
            new Line("message.planetaryfactory.welcome.reach", ChatFormatting.YELLOW));

    private Welcome() {
    }
}
