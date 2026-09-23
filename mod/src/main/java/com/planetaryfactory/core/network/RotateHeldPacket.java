package com.planetaryfactory.core.network;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.placement.HeldTurn;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Rotate or Reverse Rotate was pressed with the main hand's stack held (ADR-0083). */
public record RotateHeldPacket(boolean reverse) implements CustomPacketPayload {

    public static final Type<RotateHeldPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "rotate_held"));

    public static final StreamCodec<ByteBuf, RotateHeldPacket> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(RotateHeldPacket::new, RotateHeldPacket::reverse);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RotateHeldPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            HeldTurn.press(player.getMainHandItem(), packet.reverse());
        }
    }
}
