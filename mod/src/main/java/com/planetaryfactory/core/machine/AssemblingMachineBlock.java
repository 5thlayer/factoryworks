package com.planetaryfactory.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.block.base.block.MultiblockMachine;

/**
 * The Assembling Machine's anchor block (#326, ADR-0071), holding {@link AssemblingMachineBlockEntity}.
 *
 * <p>Extends Oritech's {@link MultiblockMachine} so the {@code ASSEMBLED} property is declared:
 * Oritech's block and controller code reads it unguarded, and a state without it would throw. The
 * pack's item places this block with {@code ASSEMBLED} already {@code true}, which is what makes
 * the multiblock paths return early -- see {@link AssemblingMachineBlockEntity}.
 *
 * <p>Nothing here places the block: {@link AssemblingMachineItem} puts the whole footprint down in
 * one click, and this class only tears it down again when the anchor goes.
 */
public class AssemblingMachineBlock extends MultiblockMachine {

    public AssemblingMachineBlock(Properties properties) {
        super(properties);
    }

    /** Oritech's {@code MachineBlock.newBlockEntity} constructs this class by reflection. */
    @Override
    public Class<? extends BlockEntity> getBlockEntityType() {
        return AssemblingMachineBlockEntity.class;
    }

    /**
     * Opens the pack's menu rather than Oritech's (#327): the opening packet carries the recipe
     * widget's list, which Oritech's {@code openMenu(provider, pos)} has no room for. An anchor that
     * somehow lost {@code ASSEMBLED} goes to Oritech's own path, which is what repairs it.
     */
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                            BlockHitResult hit) {
        if (!state.getValue(ASSEMBLED)) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof AssemblingMachineBlockEntity machine) {
            player.openMenu(machine, buf -> AssemblingMachineMenu.writeOpening(buf, machine));
        }
        return InteractionResult.CONSUME;
    }

    /**
     * The anchor going takes its parts with it. The anchor's own item comes from its loot table,
     * and its inventory from Oritech's {@code playerWillDestroy} -- or from
     * {@link AssemblingMachineHull#teardown} when a part was what the player broke.
     */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        AssemblingMachineHull.teardown(level, pos, state.getValue(FACING), pos);
    }
}
