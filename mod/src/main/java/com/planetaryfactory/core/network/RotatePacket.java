package com.planetaryfactory.core.network;

import java.util.Optional;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.placement.RotatePress;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Rotate or Reverse Rotate was pressed, with the block under the crosshair if any (ADR-0083, ADR-0087). */
public record RotatePacket(boolean reverse, Optional<BlockPos> aimed) implements CustomPacketPayload {

    public static final Type<RotatePacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "rotate"));

    public static final StreamCodec<ByteBuf, RotatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, RotatePacket::reverse,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs::optional), RotatePacket::aimed,
            RotatePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RotatePacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            RotatePress.press(player, packet.aimed().orElse(null), packet.reverse());
        }
    }
}
