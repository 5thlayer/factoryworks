package com.planetaryfactory.core.radar;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.planetaryfactory.core.network.PFNetwork;
import com.planetaryfactory.core.ore.PatchLedgerData;
import com.planetaryfactory.core.worldgen.OilFieldPiece;
import com.planetaryfactory.core.network.RadarChunkPacket;
import com.planetaryfactory.core.network.RadarMarkersPacket;

import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Sends each online player's map the team's charted sectors it lacks, and the patch markers of the
 * chart and of what the player has walked (ADR-0079).
 */
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
        MarkerDelivery.Amounts amounts = amounts(event.getServer());
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
            List<MarkerDelivery.Update> markers = data.takeMarkers(id, amounts);
            if (!markers.isEmpty()) {
                PFNetwork.sendToPlayer(player, new RadarMarkersPacket(level.dimension(), markers));
            }
        }
    }

    /** A chunk sent to a player is drawn on their map, so its patches are marked for them (#370). */
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        if (!FTB_CHUNKS || event.getChunk().getAllStarts().isEmpty()) {
            return;
        }
        ServerLevel level = event.getLevel();
        ChunkPos pos = event.getPos();
        List<PatchMarker> found = patchesStartedIn(level,
                Sector.ofBlock(pos.getMinBlockX(), pos.getMinBlockZ()), List.of(event.getChunk()));
        if (!found.isEmpty()) {
            RadarChartData.get(level.getServer()).walked(event.getPlayer().getUUID(),
                    level.dimension().identifier().toString(), found);
        }
    }

    /** What is left in each patch, as the ledger holds it. */
    public static MarkerDelivery.Amounts amounts(MinecraftServer server) {
        PatchLedgerData ledger = PatchLedgerData.get(server);
        return (dimension, marker) -> ledger.remaining(marker.id(dimension), marker.total());
    }

    /** A structure's start is kept by the chunk it began in, which for a disc or a field holds its centre (ADR-0079). */
    public static List<PatchMarker> patchesStartedIn(ServerLevel level, Sector sector,
            List<? extends ChunkAccess> chunks) {
        List<StructurePiece> pieces = new ArrayList<>();
        for (ChunkAccess chunk : chunks) {
            chunk.getAllStarts().values().forEach(start -> pieces.addAll(start.getPieces()));
        }
        return SectorPatches.find(sector, pieces, (x, z) -> level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z),
                (x, z) -> OilFieldPiece.oreAt(level.structureManager(), x, z));
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
