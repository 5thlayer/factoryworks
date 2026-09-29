package com.factoryworks.core.energy;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.machine.footprint.FootprintAnchorBlock;
import com.factoryworks.core.machine.footprint.MachineTooltip;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The accumulator's anchor block (#283), holding {@link AccumulatorBlockEntity}. */
public class AccumulatorBlock extends FootprintAnchorBlock {

    @Override
    protected List<MachineTooltip.Line> tooltipLines() {
        return MachineTooltip.accumulator(AccumulatorSpec.capacityFe(), AccumulatorSpec.outputFePerTick());
    }

    public AccumulatorBlock(Properties properties) {
        super(properties, () -> PFBlocks.ACCUMULATOR_FOOTPRINT);
    }

    /** Oritech's {@code MachineBlock.newBlockEntity} constructs this class by reflection. */
    @Override
    public Class<? extends BlockEntity> getBlockEntityType() {
        return AccumulatorBlockEntity.class;
    }

    /** Oritech draws the Large Energy Storage as a block model, not through GeckoLib. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** No screen: nothing on an accumulator is the player's to set, and its Jade line is the interface. */
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                            BlockHitResult hit) {
        return InteractionResult.PASS;
    }
}
