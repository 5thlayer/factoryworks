package com.planetaryfactory.core.machine;

import java.util.List;
import java.util.function.Supplier;

import com.planetaryfactory.core.machine.footprint.Footprint.Local;
import com.planetaryfactory.core.machine.footprint.FootprintAnchorBlock;
import com.planetaryfactory.core.machine.footprint.FootprintMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The anchor of a machine on the Assembling Machine's chassis (ADR-0071, ADR-0096): the block says
 * which {@link MachineSpec} its {@link AssemblingMachineBlockEntity} runs, the paint it wears and
 * where its addons go.
 */
public abstract class ChassisMachineBlock extends FootprintAnchorBlock {

    protected ChassisMachineBlock(Properties properties, Supplier<FootprintMachine> machine) {
        super(properties, machine);
    }

    public abstract MachineSpec spec();

    /** Oritech's {@code ColorVariant} name. */
    public abstract String paint();

    /** In {@code Footprint}'s frame. */
    public abstract List<Local> addonSlots();

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
