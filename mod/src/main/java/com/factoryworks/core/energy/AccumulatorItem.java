package com.factoryworks.core.energy;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.machine.footprint.FootprintMachine;
import com.factoryworks.core.machine.footprint.MachineTooltip;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.PlansPlacement;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jspecify.annotations.Nullable;

/**
 * Places the whole accumulator (ADR-0069). A plain {@link BlockItem}, not a {@code FootprintItem}:
 * Oritech's Large Energy Storage has a block model and no GeckoLib one to draw in the hand.
 */
public class AccumulatorItem extends BlockItem implements PlansPlacement {

    public AccumulatorItem(Properties properties) {
        super(PFBlocks.ACCUMULATOR.get(), properties);
    }

    @Override
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        return PFBlocks.ACCUMULATOR_FOOTPRINT.plan(context);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        return FootprintMachine.place(this, context) == null ? InteractionResult.FAIL : InteractionResult.SUCCESS;
    }

    /** A plain {@link BlockItem} never reaches the block's {@code addToTooltip}, which Oritech's items call (#515). */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        for (MachineTooltip.Line line : MachineTooltip.accumulator(AccumulatorSpec.capacityFe(),
                AccumulatorSpec.outputFePerTick())) {
            tooltip.accept(Component.translatable(line.key(), line.args().toArray()).withStyle(ChatFormatting.GRAY));
        }
    }
}
