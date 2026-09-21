package com.planetaryfactory.core.machine;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.util.Geometry;

/**
 * The Assembling Machine's footprint in the world (#326): where its blocks go, and taking them all
 * down when one is broken -- the rig's bed-and-door idiom ({@code RigBreaker}), for this machine.
 *
 * <p>Positions are {@link AssemblingMachineFootprint}'s offsets rotated by Oritech's own
 * {@link Geometry#rotatePosition}, the call {@code initMultiblock} makes on its core positions with
 * the same facing, which is what keeps the blocks under the model Oritech's renderer draws.
 *
 * <p>Exactly one item drops whichever block was broken: the anchor's own loot table when the
 * anchor is broken, and the part's hand-popped item when a part is -- the part's loot table is
 * empty. The reentrancy guard is what stops the teardown's own removals from starting a second
 * teardown or popping a second item.
 */
public final class AssemblingMachineHull {

    private static final ThreadLocal<Boolean> IN_PROGRESS = ThreadLocal.withInitial(() -> false);

    private AssemblingMachineHull() {
    }

    /** Every position the machine anchored at {@code anchor} occupies, anchor first. */
    public static List<BlockPos> positions(BlockPos anchor, Direction facing) {
        List<AssemblingMachineFootprint.Local> offsets = AssemblingMachineFootprint.offsets();
        List<BlockPos> positions = new ArrayList<>(offsets.size());
        for (AssemblingMachineFootprint.Local offset : offsets) {
            positions.add(anchor.offset(worldOffset(offset, facing)));
        }
        return positions;
    }

    /** Where a part's anchor is, from the part's own position and its blockstate. */
    public static BlockPos anchorOf(BlockPos part, int index, Direction facing) {
        Vec3i offset = worldOffset(AssemblingMachineFootprint.offsetOfPart(index), facing);
        return part.subtract(offset);
    }

    public static boolean inProgress() {
        return Boolean.TRUE.equals(IN_PROGRESS.get());
    }

    /**
     * Removes every block of the machine at {@code anchor} except {@code skip}, the position
     * already being replaced by whatever triggered this.
     *
     * <p>When the anchor is one of the removals -- a part was broken -- its inventory is dropped
     * first. Oritech drops a machine's inventory in {@code MachineBlock.playerWillDestroy}, which
     * runs only for the block the player actually broke, and the pack is not a resource sink.
     */
    public static void teardown(Level level, BlockPos anchor, Direction facing, BlockPos skip) {
        if (inProgress()) {
            return;
        }
        IN_PROGRESS.set(true);
        try {
            for (BlockPos pos : positions(anchor, facing)) {
                if (pos.equals(skip)) {
                    continue;
                }
                var block = level.getBlockState(pos).getBlock();
                if (block instanceof AssemblingMachineBlock) {
                    if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
                        dropInventory(level, pos, machine.getDisplayedInventory());
                    }
                    level.removeBlock(pos, false);
                } else if (block instanceof AssemblingMachinePartBlock) {
                    level.removeBlock(pos, false);
                }
            }
        } finally {
            IN_PROGRESS.set(false);
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

    private static Vec3i worldOffset(AssemblingMachineFootprint.Local offset, Direction facing) {
        return Geometry.rotatePosition(new Vec3i(offset.x(), offset.y(), offset.z()), facing);
    }
}
