package com.planetaryfactory.core.machine.footprint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.planetaryfactory.core.placement.PlacementPlan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.util.Geometry;

/**
 * An Oritech machine placed whole and broken whole (ADR-0072, ADR-0077): its {@link Footprint}, the
 * anchor block that holds its block entity, the invisible part block standing on every other
 * position, and the one item that places it and that a break pays back.
 *
 * <p>Positions are the footprint rotated by Oritech's own {@link Geometry#rotatePosition}, the call
 * Oritech's renderer draws the model with, which is what keeps the blocks under the model.
 *
 * <p>Exactly one item drops whichever block is broken: the anchor's loot table when the anchor is,
 * the part's hand-popped item when a part is, since a part's loot table is empty. The reentrancy
 * guard is what stops the teardown's own removals from starting a second teardown or popping a
 * second item.
 */
public record FootprintMachine(Footprint footprint, Supplier<? extends FootprintAnchorBlock> anchor,
                               Supplier<? extends FootprintPartBlock> part, Supplier<? extends Item> item) {

    private static final ThreadLocal<Boolean> TEARING_DOWN = ThreadLocal.withInitial(() -> false);

    public FootprintMachine {
        if (footprint.partCount() > FootprintPartBlock.MAX_PARTS) {
            throw new IllegalArgumentException("the part block numbers at most "
                    + FootprintPartBlock.MAX_PARTS + " parts, and this footprint has " + footprint.partCount());
        }
    }

    public List<BlockPos> positions(BlockPos anchor, Direction facing) {
        List<BlockPos> positions = new ArrayList<>(footprint.offsets().size());
        for (Footprint.Local offset : footprint.offsets()) {
            positions.add(anchor.offset(worldOffset(offset, facing)));
        }
        return positions;
    }

    /** Where a part's anchor is, from the part's own position and blockstate. */
    public BlockPos anchorOf(BlockPos part, BlockState state) {
        Vec3i offset = worldOffset(footprint.offsetOfPart(state.getValue(FootprintPartBlock.PART)),
                state.getValue(FootprintPartBlock.FACING));
        return part.subtract(offset);
    }

    public boolean isAnchor(BlockState state) {
        return state.is(anchor.get());
    }

    /**
     * The state each position is placed in. The anchor is placed already assembled, which is what
     * makes Oritech's multiblock paths return early rather than scan for cores the pack never asks
     * the player to place.
     */
    public BlockState stateAt(int index, Direction facing) {
        if (index == 0) {
            return anchor.get().defaultBlockState()
                    .setValue(MultiblockMachine.FACING, facing)
                    .setValue(MultiblockMachine.ASSEMBLED, true);
        }
        return part.get().defaultBlockState()
                .setValue(FootprintPartBlock.FACING, facing)
                .setValue(FootprintPartBlock.PART, index);
    }

    /** Every block of the machine, in its placed state, without a player's click. */
    public void placeAll(Level level, BlockPos anchor, Direction facing) {
        List<BlockPos> positions = positions(anchor, facing);
        for (int i = 0; i < positions.size(); i++) {
            level.setBlock(positions.get(i), stateAt(i, facing), Block.UPDATE_ALL);
        }
    }

    /**
     * The whole footprint at the facing Oritech's {@code MachineBlock} would give the anchor --
     * towards the player -- refusing as one if any position is taken or out of the world.
     */
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        if (!context.canPlace()) {
            return null;
        }
        Level level = context.getLevel();
        Direction facing = context.getHorizontalDirection().getOpposite();
        List<BlockPos> positions = positions(context.getClickedPos(), facing);

        List<PlacementPlan.Placed> blocks = new ArrayList<>(positions.size());
        boolean fits = true;
        for (int i = 0; i < positions.size(); i++) {
            BlockPos pos = positions.get(i);
            BlockState state = stateAt(i, facing);
            blocks.add(new PlacementPlan.Placed(pos, state));
            // isUnobstructed is vanilla's own entity check: without it the footprint closes
            // around a player or a mob standing in it.
            if (!level.isInWorldBounds(pos) || !level.getBlockState(pos).canBeReplaced()
                    || !level.isUnobstructed(state, pos, CollisionContext.empty())) {
                fits = false;
            }
        }
        return fits
                ? PlacementPlan.accepted(blocks)
                : PlacementPlan.refused(blocks, PlacementPlan.Refusal.FOOTPRINT_BLOCKED);
    }

    public static boolean tearingDown() {
        return Boolean.TRUE.equals(TEARING_DOWN.get());
    }

    /**
     * Removes every block of the machine at {@code anchor} except {@code skip}, the position
     * already being replaced by whatever triggered this.
     *
     * <p>When the anchor is one of the removals -- a part was broken -- its inventory is dropped
     * first: Oritech drops it in {@code MachineBlock.playerWillDestroy}, which runs only for the
     * block the player actually broke. A tank's fluid is voided.
     */
    public void teardown(Level level, BlockPos anchor, Direction facing, BlockPos skip) {
        if (tearingDown()) {
            return;
        }
        TEARING_DOWN.set(true);
        try {
            for (BlockPos pos : positions(anchor, facing)) {
                if (pos.equals(skip)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (isAnchor(state)) {
                    if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
                        dropInventory(level, pos, machine.getDisplayedInventory());
                    }
                    level.removeBlock(pos, false);
                } else if (state.is(part.get())) {
                    level.removeBlock(pos, false);
                }
            }
        } finally {
            TEARING_DOWN.set(false);
        }
    }

    private static void dropInventory(Level level, BlockPos pos, ResourceHandler<ItemResource> inventory) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemResource resource = inventory.getResource(slot);
            int amount = inventory.getAmountAsInt(slot);
            if (!resource.isEmpty() && amount > 0) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                        resource.toStack(amount));
            }
        }
    }

    private static Vec3i worldOffset(Footprint.Local offset, Direction facing) {
        return Geometry.rotatePosition(new Vec3i(offset.x(), offset.y(), offset.z()), facing);
    }
}
