package com.planetaryfactory.core.placement.client;

import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;
import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.network.RotatePacket;
import com.planetaryfactory.core.placement.HeldTurn;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/** The keys for Rotate and Reverse Rotate (ADR-0083, ADR-0087). */
public final class RotateKeys {

    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "planetaryfactory"));

    private static final KeyMapping ROTATE = new KeyMapping("key.planetaryfactory.rotate",
            KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
    private static final KeyMapping REVERSE_ROTATE = new KeyMapping("key.planetaryfactory.reverse_rotate",
            KeyConflictContext.IN_GAME, KeyModifier.SHIFT, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    private RotateKeys() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(RotateKeys::onRegisterKeys);
        NeoForge.EVENT_BUS.addListener(RotateKeys::onClientTick);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(ROTATE);
        event.register(REVERSE_ROTATE);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        while (ROTATE.consumeClick()) {
            send(player, false);
        }
        while (REVERSE_ROTATE.consumeClick()) {
            send(player, true);
        }
    }

    private static void send(LocalPlayer player, boolean reverse) {
        if (player == null) {
            return;
        }
        Optional<BlockPos> aimed = Minecraft.getInstance().hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK ? Optional.of(hit.getBlockPos()) : Optional.empty();
        if (HeldTurn.turns(player.getMainHandItem()) || aimed.isPresent()) {
            ClientPacketDistributor.sendToServer(new RotatePacket(reverse, aimed));
        }
    }
}
