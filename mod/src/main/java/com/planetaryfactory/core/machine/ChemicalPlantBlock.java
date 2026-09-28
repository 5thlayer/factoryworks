package com.planetaryfactory.core.machine;

import java.util.List;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.machine.footprint.Footprint.Local;

import net.minecraft.world.level.block.entity.BlockEntity;

/** The Chemical Plant's anchor (ADR-0096), on Oritech's Centrifuge model. */
public class ChemicalPlantBlock extends ChassisMachineBlock {

    public ChemicalPlantBlock(Properties properties) {
        super(properties, () -> PFBlocks.CHEMICAL_PLANT_FOOTPRINT);
    }

    @Override
    public MachineSpec spec() {
        return MachineSpecs.get().spec("chemical-plant");
    }

    @Override
    public String paint() {
        return "ORANGE";
    }

    @Override
    public List<Local> addonSlots() {
        return ChemicalPlantFootprint.addonSlots();
    }

    @Override
    public Class<? extends BlockEntity> getBlockEntityType() {
        return ChemicalPlantBlockEntity.class;
    }
}
