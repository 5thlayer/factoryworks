package com.planetaryfactory.core.dismantle;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import io.github._5thlayer.beltworks.blocks.BeltFamily;
import io.github._5thlayer.groundworks.Dismantles;

/** A belt tile's click belongs to Groundworks' belt family until pipes are one too (#448). */
final class BeltClaim {

    private static final BeltFamily BELT_FAMILY = new BeltFamily();

    private BeltClaim() {
    }

    static boolean claims(Level level, BlockPos pos, ItemStack held) {
        return Dismantles.isTool(held)
                && BELT_FAMILY.claims(level.getBlockState(BELT_FAMILY.names(level, pos)));
    }
}
