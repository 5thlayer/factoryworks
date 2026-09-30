package com.factoryworks.core.showcase;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /factoryworks showcase <scene>}: builds a showcase scene with its north-west corner under
 * the player's feet (#538).
 */
public final class ShowcaseCommand {

    private ShowcaseCommand() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("factoryworks")
                .then(Commands.literal("showcase")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("scene", StringArgumentType.word())
                                .suggests((context, builder) ->
                                        SharedSuggestionProvider.suggest(ShowcaseScenes.SCENES.keySet(), builder))
                                .executes(context -> {
                                    String scene = StringArgumentType.getString(context, "scene");
                                    var source = context.getSource();
                                    if (!ShowcaseScenes.SCENES.containsKey(scene)) {
                                        source.sendFailure(Component.literal("No showcase scene " + scene));
                                        return 0;
                                    }
                                    BlockPos origin = BlockPos.containing(source.getPosition()).below();
                                    ShowcaseScenes.build(scene, source.getLevel(), origin);
                                    source.sendSuccess(() -> Component.literal("Built " + scene + " at " + origin.toShortString()), true);
                                    return 1;
                                }))));
    }
}
