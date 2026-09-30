package com.factoryworks.core.oil;

import com.factoryworks.core.machine.footprint.MachineTooltip;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.machine.footprint.FootprintItem;
import com.factoryworks.core.placement.PackRefusal;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jspecify.annotations.Nullable;

/** Places the whole Pumpjack, and only with its anchor over an oil well (ADR-0081). */
public class PumpjackItem extends FootprintItem {

    private static final String NO_WELL_KEY = "message.factoryworks.pumpjack.no_well";

    public PumpjackItem(Properties properties) {
        super(properties, PFBlocks.PUMPJACK_FOOTPRINT, 1.0f, "pump");
    }

    @Override
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        PlacementPlan plan = super.plan(context);
        if (plan == null || plan.isRefused()) {
            return plan;
        }
        if (!context.getLevel().getBlockState(plan.blocks().getFirst().pos().below()).is(PFBlocks.OIL_WELL.get())) {
            return PlacementPlan.refused(plan.blocks(), PackRefusal.NOT_ON_WELL);
        }
        return plan;
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        PlacementPlan plan = Placements.planFor(this, context);
        if (plan != null && plan.refusal() == PackRefusal.NOT_ON_WELL
                && context.getPlayer() instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.translatable(NO_WELL_KEY), true);
        }
        return super.place(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        for (MachineTooltip.Line line : MachineTooltip.pumpjack(PumpjackBlockEntity.spec().fePerTick())) {
            tooltip.accept(Component.translatable(line.key(), line.args().toArray()).withStyle(ChatFormatting.GRAY));
        }
    }
}
