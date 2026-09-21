package com.planetaryfactory.core.mining.rig;

import javax.annotation.Nullable;

import com.planetaryfactory.core.PFBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A part's only job: remember where its anchor is (#192). It carries no rules of its own --
 * everything a rig does lives on {@link RigBlockEntity}; this is the pointer the bed-and-door
 * idiom needs to find it from any part, which is what lets breaking any part break the rig.
 */
public class RigPartBlockEntity extends BlockEntity {

    private static final String TAG_ANCHOR_X = "anchor_x";
    private static final String TAG_ANCHOR_Y = "anchor_y";
    private static final String TAG_ANCHOR_Z = "anchor_z";

    @Nullable
    private BlockPos anchorPos;

    public RigPartBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.RIG_PART.get(), pos, state);
    }

    /** Set once, at placement, by the item that placed the whole footprint in one click. */
    public void setAnchorPos(BlockPos anchorPos) {
        this.anchorPos = anchorPos;
        setChanged();
    }

    @Nullable
    public BlockPos anchorPos() {
        return anchorPos;
    }

    /**
     * Breaking a part pops one drill item by hand (its loot table gives nothing) and tears the rest
     * of the rig down (#192, bed-and-door).
     *
     * <p>Here and not in the block's {@code affectNeighborsAfterRemoval}: 26.1 removes the block
     * entity between the two, so the anchor position is gone by then (#310).
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide() || anchorPos == null || RigBreaker.inProgress()) {
            return;
        }
        BlockState anchor = level.getBlockState(anchorPos);
        if (anchor.getBlock() instanceof RigBlock rig) {
            Block.popResource(level, pos, new ItemStack(
                    com.planetaryfactory.core.PFItems.rig(rig.tier()).get()));
            RigBreaker.teardown(level, anchorPos, rig.tier(), anchor.getValue(RigBlock.FACING), pos);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (anchorPos != null) {
            output.putInt(TAG_ANCHOR_X, anchorPos.getX());
            output.putInt(TAG_ANCHOR_Y, anchorPos.getY());
            output.putInt(TAG_ANCHOR_Z, anchorPos.getZ());
        }
    }

    /** The client needs the anchor too: looking at a part draws the rig's mining area (#195). */
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        anchorPos = input.getInt(TAG_ANCHOR_X)
                .map(x -> new BlockPos(x, input.getIntOr(TAG_ANCHOR_Y, 0), input.getIntOr(TAG_ANCHOR_Z, 0)))
                .orElse(null);
    }
}
