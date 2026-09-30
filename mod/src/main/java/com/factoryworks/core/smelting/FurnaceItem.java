package com.factoryworks.core.smelting;

import com.factoryworks.core.machine.footprint.MachineTooltip;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.Placements;
import io.github._5thlayer.groundworks.PlansPlacement;
import com.factoryworks.core.placement.PackRefusal;
import com.factoryworks.core.placement.ReplaceGroups;
import com.factoryworks.core.placement.ReplaceHandoff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A furnace in the hand: vanilla's placement, or a Fast Replace when plainly aimed at a furnace of
 * another tier in its Replace Group (ADR-0082). A sneak places beside, as it does against any block
 * with a screen.
 */
public class FurnaceItem extends BlockItem implements PlansPlacement {

    public FurnaceItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    public FurnaceTier tier() {
        return ((FurnaceBlock) getBlock()).tier();
    }

    @Override
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        if (context.isSecondaryUseActive()) {
            return Placements.vanillaPlan(this, context);
        }
        BlockPos aimed = Placements.aimedPos(context);
        BlockState placed = context.getLevel().getBlockState(aimed);
        if (!(placed.getBlock() instanceof FurnaceBlock)
                || !ReplaceGroups.get().canReplace(id(getBlock()), id(placed.getBlock()))
                || !(context.getLevel().getBlockEntity(aimed) instanceof FurnaceBlockEntity furnace)) {
            return Placements.vanillaPlan(this, context);
        }
        BlockState state = getBlock().defaultBlockState()
                .setValue(FurnaceBlock.FACING, placed.getValue(FurnaceBlock.FACING));
        Player player = context.getPlayer();
        // A client furnace holds no items, so the client's plan can promise a replace the server
        // then refuses over the fuel it hands back; the server's plan is the one the click executes.
        boolean fits = player == null || ReplaceHandoff.fits(player, context.getHand(),
                new ItemStack(placed.getBlock()), furnace.handOver(tier()).extras());
        return PlacementPlan.replacing(new PlacementPlan.Placed(aimed, state),
                fits ? null : PackRefusal.NO_ROOM_TO_RETURN);
    }

    private static String id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        for (MachineTooltip.Line line : MachineTooltip.furnace(tier().craftingSpeed(), tier().joulesPerTick(), tier().fePerTick())) {
            tooltip.accept(Component.translatable(line.key(), line.args().toArray()).withStyle(ChatFormatting.GRAY));
        }
    }
}
