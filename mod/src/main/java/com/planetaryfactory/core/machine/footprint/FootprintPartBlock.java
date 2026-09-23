package com.planetaryfactory.core.machine.footprint;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;
import com.planetaryfactory.core.energy.EnergyOwnerBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
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
 * One block of a {@link FootprintMachine} that is not the anchor.
 *
 * <p>Invisible, since Oritech's renderer draws the whole model from the anchor, and with no block
 * entity: {@link #PART} and {@link #FACING} together name where the anchor is, so nothing is stored
 * that a reload could lose. Never held and never placed on its own.
 */
public class FootprintPartBlock extends Block implements EnergyOwnerBlock {

    /** The most parts one footprint may have; the property is declared before any machine is known. */
    public static final int MAX_PARTS = 26;

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 1, MAX_PARTS);

    private final Supplier<FootprintMachine> machine;
    private final MapCodec<FootprintPartBlock> codec;

    public FootprintPartBlock(Properties properties, Supplier<FootprintMachine> machine) {
        super(properties);
        this.machine = machine;
        this.codec = simpleCodec(props -> new FootprintPartBlock(props, machine));
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(PART, 1));
    }

    public FootprintMachine machine() {
        return machine.get();
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** Its anchor's: a part is not a machine of its own to a pole. */
    @Override
    public BlockPos energyOwner(BlockPos pos, BlockState state) {
        return machine().anchorOf(pos, state);
    }

    /** Answered by the anchor, so a Fast Replace aimed at a part replaces the machine (ADR-0082). */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos anchor = machine().anchorOf(pos, state);
        BlockState anchorState = level.getBlockState(anchor);
        if (!machine().isAnchor(anchorState)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return anchorState.useItemOn(stack, level, player, hand, hit.withPosition(anchor));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        BlockPos anchor = machine().anchorOf(pos, state);
        BlockState anchorState = level.getBlockState(anchor);
        if (!machine().isAnchor(anchorState)) {
            return InteractionResult.PASS;
        }
        return anchorState.useWithoutItem(level, player, hit.withPosition(anchor));
    }

    /** Breaking a part pops the machine's one item and takes the rest of it down. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
        if (FootprintMachine.tearingDown()) {
            return;
        }
        BlockPos anchor = machine().anchorOf(pos, state);
        BlockState anchorState = level.getBlockState(anchor);
        if (machine().isAnchor(anchorState)) {
            popResource(level, pos, new ItemStack(machine().item().get()));
            machine().teardown(level, anchor, anchorState.getValue(FACING), pos);
        }
    }
}
