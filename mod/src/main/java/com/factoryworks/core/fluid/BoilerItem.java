package com.factoryworks.core.fluid;

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
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.BlockItem;

/** Places the whole Boiler, or nothing (ADR-0114, ADR-0069). */
public class BoilerItem extends BlockItem implements PlansPlacement {

    public BoilerItem(Properties properties) {
        super(PFBlocks.BOILER.get(), properties);
    }

    @Override
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        return PFBlocks.BOILER_FOOTPRINT.plan(context);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        return FootprintMachine.place(this, context) == null ? InteractionResult.FAIL : InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        for (MachineTooltip.Line line : MachineTooltip.boiler(BoilerBlockEntity.steamPerSecond(), BoilerBlockEntity.joulesPerTick())) {
            tooltip.accept(Component.translatable(line.key(), line.args().toArray()).withStyle(ChatFormatting.GRAY));
        }
    }
}
