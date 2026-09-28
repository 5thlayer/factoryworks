package com.planetaryfactory.core.machine;

import java.util.List;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.machine.footprint.Footprint.Local;

import net.minecraft.world.level.block.entity.BlockEntity;

/** The Oil Refinery's anchor (ADR-0096), on Oritech's Refinery model and its two chambers. */
public class OilRefineryBlock extends ChassisMachineBlock {

    public OilRefineryBlock(Properties properties) {
        super(properties, () -> PFBlocks.OIL_REFINERY_FOOTPRINT);
    }

    @Override
    public MachineSpec spec() {
        return MachineSpecs.get().spec("oil-refinery");
    }

    @Override
    public String paint() {
        return "FLUXITE";
    }

    /** None: Oritech's Refinery takes no addon, and its chambers are the footprint's own layers. */
    @Override
    public List<Local> addonSlots() {
        return List.of();
    }

    @Override
    public Class<? extends BlockEntity> getBlockEntityType() {
        return OilRefineryBlockEntity.class;
    }
}
