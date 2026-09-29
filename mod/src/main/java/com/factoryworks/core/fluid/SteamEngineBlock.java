package com.factoryworks.core.fluid;

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

/** The Steam Engine's anchor block (ADR-0077), holding {@link SteamEngineBlockEntity}. */
public class SteamEngineBlock extends FootprintAnchorBlock {

    /** The peak figures do not read the efficiency curve, so a flat one serves the tooltip. */
    private static final SteamEngineSpec SPEC = SteamEngineSpec.fromCorpus(SteamChainCorpus.get(), speed -> 1.0);

    @Override
    protected List<MachineTooltip.Line> tooltipLines() {
        return MachineTooltip.steamEngine(SPEC.steamPerSecondAtPeak(), SPEC.energyPerTickAtPeak());
    }

    public SteamEngineBlock(Properties properties) {
        super(properties, () -> PFBlocks.STEAM_ENGINE_FOOTPRINT);
    }

    /** Oritech's {@code MachineBlock.newBlockEntity} constructs this class by reflection. */
    @Override
    public Class<? extends BlockEntity> getBlockEntityType() {
        return SteamEngineBlockEntity.class;
    }

    /** No screen: there is nothing on an engine to set, and its Jade line carries every figure (#352). */
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                            BlockHitResult hit) {
        return InteractionResult.PASS;
    }
}
