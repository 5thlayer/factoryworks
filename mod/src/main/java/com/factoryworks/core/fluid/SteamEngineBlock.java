package com.factoryworks.core.fluid;

import java.util.List;
import java.util.function.Consumer;

import com.factoryworks.core.PFBlockEntities;
import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.machine.footprint.FootprintTurn;
import com.factoryworks.core.machine.footprint.MachineTooltip;
import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jspecify.annotations.Nullable;

/** The Steam Engine's anchor block (ADR-0116), holding {@link SteamEngineBlockEntity}. */
public class SteamEngineBlock extends HorizontalDirectionalBlock implements EntityBlock, FootprintTurn, TooltipProvider {

    private static final MapCodec<SteamEngineBlock> CODEC = simpleCodec(SteamEngineBlock::new);

    public SteamEngineBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag,
                             DataComponentGetter components) {
        SteamEngineSpec spec = SteamEngineBlockEntity.spec();
        List<MachineTooltip.Line> lines = MachineTooltip.steamEngine(spec.steamPerSecond(), spec.energyPerTick());
        for (MachineTooltip.Line line : lines) {
            tooltip.accept(Component.translatable(line.key(), line.args().toArray()).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamEngineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                          BlockEntityType<T> type) {
        if (level.isClientSide() || type != PFBlockEntities.STEAM_ENGINE.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> ((SteamEngineBlockEntity) entity).serverTick();
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
        SteamEngineBlockEntity.leave(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        PFBlocks.STEAM_ENGINE_FOOTPRINT.teardown(level, pos, state.getValue(FACING), pos);
    }
}
