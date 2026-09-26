package com.planetaryfactory.core.energy;

import java.util.List;

import com.planetaryfactory.core.PFBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import rearth.oritech.block.entity.storage.LargeStorageBlockEntity;

/**
 * The accumulator's anchor (#283): Oritech's Large Energy Storage entity under the pack's own type,
 * placed as a footprint on the Steam Engine's pattern (ADR-0077). {@code LargeStorageBlockEntityMixin}
 * reaches it through inheritance.
 *
 * <p>Oritech's storage reads the six-way {@code facing} its own block declares; the anchor carries
 * the horizontal one, so every read of it is answered here.
 */
public class AccumulatorBlockEntity extends LargeStorageBlockEntity {

    public AccumulatorBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        return PFBlockEntities.ACCUMULATOR.get();
    }

    @Override
    public Holder<BlockEntityType<?>> typeHolder() {
        return getType().builtInRegistryHolder();
    }

    @Override
    public Direction getFacing() {
        return getBlockState().getValue(HorizontalDirectionalBlock.FACING);
    }

    @Override
    public Direction getFacingForAddon() {
        return getFacing();
    }

    @Override
    public Property<Direction> getBlockFacingProperty() {
        return HorizontalDirectionalBlock.FACING;
    }

    @Override
    public boolean initMultiblock(BlockState state) {
        return true;
    }

    @Override
    public void rescanMultiblock() {
    }

    @Override
    public List<Vec3i> getCorePositions() {
        return List.of();
    }
}
