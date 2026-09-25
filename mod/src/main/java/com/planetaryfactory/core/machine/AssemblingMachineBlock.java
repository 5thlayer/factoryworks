package com.planetaryfactory.core.machine;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.machine.footprint.FootprintAnchorBlock;
import io.github._5thlayer.placementpreview.PlacementPlan;
import io.github._5thlayer.placementpreview.Placements;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** An Assembling Machine tier's anchor block (#326, ADR-0071), holding {@link AssemblingMachineBlockEntity}. */
public class AssemblingMachineBlock extends FootprintAnchorBlock {

    private final AssemblingTier tier;

    public AssemblingMachineBlock(AssemblingTier tier, Properties properties) {
        super(properties, () -> PFBlocks.assemblingFootprint(tier));
        this.tier = tier;
    }

    public AssemblingTier tier() {
        return tier;
    }

    /** Oritech's {@code MachineBlock.newBlockEntity} constructs this class by reflection. */
    @Override
    public Class<? extends BlockEntity> getBlockEntityType() {
        return AssemblingMachineBlockEntity.class;
    }

    /** One block entity type serves every tier, so a Fast Replace keeps it (ADR-0082). */
    @Override
    protected boolean shouldChangedStateKeepBlockEntity(BlockState oldState) {
        return oldState.getBlock() instanceof AssemblingMachineBlock;
    }

    /** A machine of another tier in the same Replace Group swaps this one in place (ADR-0082). */
    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                       InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof AssemblingMachineItem item) {
            PlacementPlan plan = Placements.planFor(item, new BlockPlaceContext(level, player, hand, stack, hit));
            if (plan != null && plan.isReplace()) {
                return item.replace(plan, level, pos, player, hand);
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
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
