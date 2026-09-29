package com.factoryworks.core.radar.ftb;

import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.map.ChunkUpdateTask;
import dev.ftb.mods.ftbchunks.client.map.MapManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Draws a charted chunk onto FTB Chunks' map through its internal walked-chunk task (ADR-0079). The
 * chunk is built standalone and never added to the level. Client only, loaded only when
 * {@code ftbchunks} is.
 */
public final class FtbMapChunks {

    private FtbMapChunks() {
    }

    public static void draw(ResourceKey<Level> dimension, int x, int z, ClientboundLevelChunkPacketData data) {
        ClientLevel level = Minecraft.getInstance().level;
        MapManager manager = MapManager.getInstance().orElse(null);
        // A level of another dimension has a different height, so the sections would not line up.
        if (level == null || manager == null || !level.dimension().equals(dimension)) {
            return;
        }
        ChunkPos pos = new ChunkPos(x, z);
        LevelChunk chunk = new LevelChunk(level, pos);
        chunk.replaceWithPacketData(data.getReadBuffer(), data.getHeightmaps(), blockEntities -> {
        });
        FTBChunksClient.MAP_EXECUTOR.execute(new ChunkUpdateTask(manager, level, chunk, pos));
    }
}
