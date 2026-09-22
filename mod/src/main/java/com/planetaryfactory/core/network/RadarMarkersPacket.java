package com.planetaryfactory.core.network;

import java.util.List;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import com.planetaryfactory.core.radar.PatchMarker;
import com.planetaryfactory.core.radar.client.RadarMapClient;

import io.netty.buffer.ByteBuf;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Outfield patch markers for a player's map, server to client (#370, ADR-0079). */
public record RadarMarkersPacket(ResourceKey<Level> dimension, List<PatchMarker> markers)
        implements CustomPacketPayload {

    public static final Type<RadarMarkersPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "radar_markers"));

    private static final StreamCodec<ByteBuf, PatchMarker> MARKER = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PatchMarker::resource,
            ByteBufCodecs.VAR_INT, PatchMarker::x,
            ByteBufCodecs.VAR_INT, PatchMarker::y,
            ByteBufCodecs.VAR_INT, PatchMarker::z,
            PatchMarker::new);

    public static final StreamCodec<ByteBuf, RadarMarkersPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), RadarMarkersPacket::dimension,
            MARKER.apply(ByteBufCodecs.list()), RadarMarkersPacket::markers,
            RadarMarkersPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RadarMarkersPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> RadarMapClient.receive(packet.dimension(), packet.markers()));
    }
}
