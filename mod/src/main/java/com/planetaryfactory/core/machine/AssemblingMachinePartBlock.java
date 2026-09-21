package com.planetaryfactory.core.machine;

import com.planetaryfactory.core.energy.EnergyOwnerBlock;

import com.mojang.serialization.MapCodec;
import com.planetaryfactory.core.PFItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * One block of the Assembling Machine that is not the anchor (#326).
 *
 * <p>Invisible: Oritech's renderer draws the whole model from the anchor, and this block is the
 * collision and the break target under it. It has no block entity, unlike the rig's part --
 * {@link #PART} and {@link #FACING} together name where the anchor is, so there is nothing to store
 * and nothing that can be lost over a reload.
 *
 * <p>Never held and never placed on its own: no item, no recipe, an empty loot table.
 */
public class AssemblingMachinePartBlock extends Block implements EnergyOwnerBlock {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty PART =
            IntegerProperty.create("part", 1, AssemblingMachineFootprint.PART_COUNT);

    public static final MapCodec<AssemblingMachinePartBlock> CODEC =
            simpleCodec(AssemblingMachinePartBlock::new);

    public AssemblingMachinePartBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(PART, 1));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** Its anchor's: a hull block is not a machine of its own to a pole (#328). */
    @Override
    public BlockPos energyOwner(BlockPos pos, BlockState state) {
        return anchorOf(pos, state);
    }

    public static BlockPos anchorOf(BlockPos pos, BlockState state) {
        return AssemblingMachineHull.anchorOf(pos, state.getValue(PART), state.getValue(FACING));
    }

    /** Right-clicking any block of the machine is right-clicking the machine. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        BlockPos anchor = anchorOf(pos, state);
        BlockState anchorState = level.getBlockState(anchor);
        if (!(anchorState.getBlock() instanceof AssemblingMachineBlock)) {
            return InteractionResult.PASS;
        }
        return anchorState.useWithoutItem(level, player, hit.withPosition(anchor));
    }

    /** Breaking a part pops the machine's one item and takes the rest of it down. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
        if (AssemblingMachineHull.inProgress()) {
            return;
        }
        BlockPos anchor = anchorOf(pos, state);
        BlockState anchorState = level.getBlockState(anchor);
        if (anchorState.getBlock() instanceof AssemblingMachineBlock) {
            popResource(level, pos, new ItemStack(PFItems.ASSEMBLING_MACHINE.get()));
            AssemblingMachineHull.teardown(level, anchor, anchorState.getValue(FACING), pos);
        }
    }
}
