package com.planetaryfactory.core.radar;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.machine.footprint.FootprintMachine;
import com.planetaryfactory.core.machine.footprint.MachineTooltip;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.PlansPlacement;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jspecify.annotations.Nullable;

/** Places the whole Radar (ADR-0069) and makes the placing player its owner, whose team it charts for. */
public class RadarItem extends BlockItem implements PlansPlacement {

    private static final RadarSpec SPEC = RadarSpec.fromCorpus();

    public RadarItem(Properties properties) {
        super(PFBlocks.RADAR.get(), properties);
    }

    @Override
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        return PFBlocks.RADAR_FOOTPRINT.plan(context);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        BlockPos anchor = FootprintMachine.place(this, context);
        if (anchor == null) {
            return InteractionResult.FAIL;
        }
        if (context.getPlayer() != null
                && context.getLevel().getBlockEntity(anchor) instanceof RadarBlockEntity radar) {
            radar.setOwner(context.getPlayer().getUUID());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        for (MachineTooltip.Line line : MachineTooltip.radar(SPEC.fePerTick(), SPEC.nearReach(), SPEC.reach())) {
            tooltip.accept(Component.translatable(line.key(), line.args().toArray()).withStyle(ChatFormatting.GRAY));
        }
    }
}
