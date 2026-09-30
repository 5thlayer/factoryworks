package com.factoryworks.core.fluid;

import com.factoryworks.core.PFBlocks;
import com.factoryworks.core.machine.footprint.MachineTooltip;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.BlockItem;

public class BoilerItem extends BlockItem {

    public BoilerItem(Properties properties) {
        super(PFBlocks.BOILER.get(), properties);
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
