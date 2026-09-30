package com.factoryworks.core.energy;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.machine.footprint.FootprintAnchorBlock;
import com.factoryworks.core.machine.footprint.MachineTooltip;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The Solar Panel's anchor block (#529), holding {@link SolarPanelBlockEntity}. */
public class SolarPanelBlock extends FootprintAnchorBlock {

    public SolarPanelBlock(Properties properties) {
        super(properties, () -> PFBlocks.SOLAR_PANEL_FOOTPRINT);
    }

    @Override
    protected List<MachineTooltip.Line> tooltipLines() {
        return MachineTooltip.solarPanel(SolarPanelSpec.get().peakFePerTick());
    }

    /** Oritech's {@code MachineBlock.newBlockEntity} constructs this class by reflection. */
    @Override
    public Class<? extends BlockEntity> getBlockEntityType() {
        return SolarPanelBlockEntity.class;
    }

    /** No screen: Oritech's opens a menu the panel's entity does not provide. */
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                            BlockHitResult hit) {
        return InteractionResult.PASS;
    }
}
