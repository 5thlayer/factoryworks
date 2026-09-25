package com.planetaryfactory.core.dismantle;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import io.github._5thlayer.beltworks.blocks.BeltTileBlock;
import io.github._5thlayer.beltworks.items.Dismantling;

/** A belt tile's click belongs to the fork's own Dismantle until belts are a family (ADR-0086). */
final class BeltClaim {

    private BeltClaim() {
    }

    static boolean claims(Level level, BlockPos pos, ItemStack held) {
        return Dismantling.dismantles(held)
                && level.getBlockState(Dismantling.aimedTile(level, pos)).getBlock() instanceof BeltTileBlock;
    }
}
