package com.planetaryfactory.core.machine;

import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import rearth.oritech.item.other.ColorCartridgeItem;

/**
 * An Oritech paint cartridge does nothing to an Assembling Machine (ADR-0075). The machine already
 * ignores the colour it is assigned; without this the cartridge would still be spent on it.
 */
public final class PaintLock {

    private PaintLock() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getItemStack().getItem() instanceof ColorCartridgeItem
                && event.getLevel().getBlockEntity(event.getPos()) instanceof AssemblingMachineBlockEntity) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }
}
