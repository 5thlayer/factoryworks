package com.factoryworks.core.mining.rig;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * {@link RigArea} placed in the world. The rig's search and the mining-area overlay both ask here,
 * so what is tinted is what is mined (#195, ADR-0069).
 */
public final class RigMiningArea {

    private RigMiningArea() {
    }

    /** Every position the rig anchored here works, in {@link RigArea}'s fixed order. */
    public static List<BlockPos> positions(BlockPos anchor, RigTier tier, Direction facing) {
        RigCorpus.Row row = RigCorpus.get().rowOf(tier);
        return RigArea.tiles(row.width(), row.height(), row.searchingRadius(),
                        RigDirections.toRigFacing(facing)).stream()
                .map(offset -> anchor.offset(offset.dx(), offset.dy(), offset.dz()))
                .toList();
    }
}
