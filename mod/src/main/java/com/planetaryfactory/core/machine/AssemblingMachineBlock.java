package com.planetaryfactory.core.machine;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.machine.footprint.FootprintAnchorBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The Assembling Machine's anchor block (#326, ADR-0071), holding {@link AssemblingMachineBlockEntity}. */
public class AssemblingMachineBlock extends FootprintAnchorBlock {

    public AssemblingMachineBlock(Properties properties) {
        super(properties, () -> PFBlocks.ASSEMBLING_MACHINE_FOOTPRINT);
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
}
