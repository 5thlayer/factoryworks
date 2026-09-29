package com.factoryworks.core.machine;

import java.util.List;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.machine.footprint.Footprint.Local;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockState;

/** An Assembling Machine tier's anchor block (#326, ADR-0071), holding {@link AssemblingMachineBlockEntity}. */
public class AssemblingMachineBlock extends ChassisMachineBlock {

    private final AssemblingTier tier;

    public AssemblingMachineBlock(AssemblingTier tier, Properties properties) {
        super(properties, () -> PFBlocks.assemblingFootprint(tier));
        this.tier = tier;
    }

    public AssemblingTier tier() {
        return tier;
    }

    @Override
    public MachineSpec spec() {
        return tier.spec();
    }

    @Override
    public String paint() {
        return tier.paint();
    }

    @Override
    public List<Local> addonSlots() {
        return AssemblingMachineFootprint.addonSlots();
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
}
