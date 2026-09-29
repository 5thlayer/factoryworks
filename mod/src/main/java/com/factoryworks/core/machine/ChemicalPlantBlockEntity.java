package com.factoryworks.core.machine;

import java.util.List;

import com.factoryworks.core.PFBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.block.blocks.addons.HeartOfTheMachineAddonBlock;
import rearth.oritech.init.BlockContent;

/**
 * The Chemical Plant (ADR-0096): the Assembling Machine's chassis under a type of its own, so
 * Oritech's Centrifuge model can be its renderer's.
 */
public class ChemicalPlantBlockEntity extends AssemblingMachineBlockEntity {

    public ChemicalPlantBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.CHEMICAL_PLANT.get(), pos, state);
    }

    /**
     * Neither the Fluid addon nor the Heart of the Machine, which can carry fluid processing,
     * connects: the machine already has its tanks (ADR-0096).
     */
    @Override
    public List<AddonBlock> getAllAddons(BlockPos pos) {
        return super.getAllAddons(pos).stream()
                .filter(addon -> addon.state().getBlock() != BlockContent.MACHINE_FLUID_ADDON.get()
                        && !(addon.state().getBlock() instanceof HeartOfTheMachineAddonBlock))
                .toList();
    }
}
