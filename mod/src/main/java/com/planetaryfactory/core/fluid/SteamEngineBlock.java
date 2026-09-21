package com.planetaryfactory.core.fluid;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.machine.footprint.FootprintAnchorBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.client.init.ParticleContent;

/** The Steam Engine's anchor block (ADR-0077), holding {@link SteamEngineBlockEntity}. */
public class SteamEngineBlock extends FootprintAnchorBlock {

    public SteamEngineBlock(Properties properties) {
        super(properties, () -> PFBlocks.STEAM_ENGINE_FOOTPRINT);
    }

    /** Oritech's {@code MachineBlock.newBlockEntity} constructs this class by reflection. */
    @Override
    public Class<? extends BlockEntity> getBlockEntityType() {
        return SteamEngineBlockEntity.class;
    }

    /**
     * A slave points at its master instead of opening its own screen, as Oritech's engine does.
     * Oritech's block asks by block entity type, which the pack's engine does not have.
     */
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                            BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SteamEngineBlockEntity engine
                && engine.inSlaveMode()) {
            player.sendSystemMessage(Component.translatable("message.oritech.steamengine.controller_link"));
            ParticleContent.HighlightBlock(level, Vec3.atLowerCornerOf(engine.master.getBlockPos()));
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
