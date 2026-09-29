package com.factoryworks.core.network;

import com.factoryworks.core.FactoryWorksCore;
import com.factoryworks.core.radar.ChartDeliveries;
import com.factoryworks.core.radar.ftb.FtbMapChunks;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** One chunk of a charted sector, server to client, for FTB Chunks' map to draw as if walked (ADR-0079). */
public record RadarChunkPacket(ResourceKey<Level> dimension, int x, int z, ClientboundLevelChunkPacketData data)
        implements CustomPacketPayload {

    public static final Type<RadarChunkPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(FactoryWorksCore.NAMESPACE, "radar_chunk"));

    private static final StreamCodec<ByteBuf, ResourceKey<Level>> DIMENSION =
            ResourceKey.streamCodec(Registries.DIMENSION);

    public static final StreamCodec<RegistryFriendlyByteBuf, RadarChunkPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                DIMENSION.encode(buf, packet.dimension());
                buf.writeVarInt(packet.x());
                buf.writeVarInt(packet.z());
                packet.data().write(buf);
            },
            buf -> {
                ResourceKey<Level> dimension = DIMENSION.decode(buf);
                int x = buf.readVarInt();
                int z = buf.readVarInt();
                return new RadarChunkPacket(dimension, x, z, new ClientboundLevelChunkPacketData(buf, x, z));
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RadarChunkPacket packet, IPayloadContext context) {
        if (ChartDeliveries.FTB_CHUNKS) {
            context.enqueueWork(() -> FtbMapChunks.draw(packet.dimension(), packet.x(), packet.z(), packet.data()));
        }
    }
}
