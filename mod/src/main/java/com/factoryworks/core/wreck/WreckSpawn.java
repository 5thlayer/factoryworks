package com.factoryworks.core.wreck;

import com.factoryworks.core.PFBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;

/** The world's spawn point is the wreck's floor, exactly (ADR-0107). */
public final class WreckSpawn {

    private WreckSpawn() {
    }

    public static boolean isWreckSpawn(ServerLevel level, BlockPos pos) {
        LevelData.RespawnData spawn = level.getRespawnData();
        return spawn.dimension().equals(level.dimension())
                && spawn.pos().equals(pos)
                && onWreckFloor(level, pos)
                && isClear(level, pos)
                && isClear(level, pos.above());
    }

    public static boolean onWreckFloor(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(PFBlocks.WRECK_HULL.get());
    }

    // A block the player put on the spawn cell hands the search back to vanilla, which steps aside.
    private static boolean isClear(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }
}
