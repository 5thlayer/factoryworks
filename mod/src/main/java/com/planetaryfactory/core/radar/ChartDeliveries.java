package com.planetaryfactory.core.radar;

import java.util.UUID;

import com.planetaryfactory.core.network.PFNetwork;
import com.planetaryfactory.core.network.RadarChunkPacket;

import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Sends each online player's map the team's charted sectors it lacks (ADR-0079). */
public final class ChartDeliveries {

    /** Nothing is recorded as sent while nothing can draw it (ADR-0079). */
    public static final boolean FTB_CHUNKS = ModList.get().isLoaded("ftbchunks");

    /** A backlog sector is four chunk loads from disk on the server thread, so one load a tick per player. */
    private static final int TICKS_PER_SECTOR = 4;

    /** FTB Chunks starts a client's map from its own login packet, and drops a chunk that beats it. */
    private static final int LOGIN_GRACE_TICKS = 40;

    private ChartDeliveries() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (!FTB_CHUNKS || event.getServer().getTickCount() % TICKS_PER_SECTOR != 0) {
            return;
        }
        RadarChartData data = RadarChartData.get(event.getServer());
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (player.tickCount < LOGIN_GRACE_TICKS) {
                continue;
            }
            UUID id = player.getUUID();
            ServerLevel level = player.level();
            data.observe(id, ChartOwners.teamOf(id), level.dimension().identifier().toString());
            for (Sector sector : data.takeDeliveries(id, 1)) {
                send(player, level, sector);
            }
        }
    }

    public static void onLogout(PlayerLoggedOutEvent event) {
        if (FTB_CHUNKS && event.getEntity() instanceof ServerPlayer player) {
            RadarChartData.get(player.level().getServer()).logout(player.getUUID());
        }
    }

    private static void send(ServerPlayer player, ServerLevel level, Sector sector) {
        for (int dx = 0; dx < Sector.CHUNKS_PER_SIDE; dx++) {
            for (int dz = 0; dz < Sector.CHUNKS_PER_SIDE; dz++) {
                int x = sector.minChunkX() + dx;
                int z = sector.minChunkZ() + dz;
                PFNetwork.sendToPlayer(player, new RadarChunkPacket(level.dimension(), x, z,
                        new ClientboundLevelChunkPacketData(level.getChunk(x, z))));
            }
        }
    }
}
