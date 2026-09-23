package com.planetaryfactory.core.energy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import com.planetaryfactory.core.placement.PlacementPlan;
import com.planetaryfactory.core.placement.Placements;
import com.planetaryfactory.core.placement.PlansPlacement;
import com.planetaryfactory.core.placement.ReplaceGroups;
import com.planetaryfactory.core.placement.ReplaceHandoff;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The pole in the hand, and the only thing in the pack that says what a pole does.
 *
 * <h2>Why a pole ships its own explanation</h2>
 *
 * <p>Every other way a player learns a machine is missing here. There is no cable to trace, so the
 * shape of the network cannot be read off the world. There is no GUI, so nothing can be inspected.
 * The recipe says nothing about reach. The area itself used to be invisible with it, which left a
 * block whose entire behaviour had to be taken on trust unless it was stated somewhere.
 *
 * <p>The <b>Supply Area Box</b> now shows where the area lands (#158, ADR-0070), so the area is no
 * longer invisible -- but it is shown only while a pole is held or looked at, and it says nothing
 * about wireless reach or about the area being measured at the base. Those are still this tooltip's
 * alone, and the numbers here are what the box is read against.
 *
 * <p>So it is stated here, in three lines, always shown rather than hidden behind Shift. This is
 * not detail a player goes looking for; it is the block's basic contract, and a tooltip nobody
 * opens teaches nobody.
 *
 * <p>The live half -- how many machines are actually in the area, and whether they are being fed --
 * cannot come from an item and belongs to the Jade line instead.
 */
public class SupplyAreaPoleItem extends BlockItem implements PlansPlacement {

    public SupplyAreaPoleItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    private PoleTier tier() {
        return ((SupplyAreaPoleBlock) getBlock()).tier();
    }

    /**
     * The pole's plan (#297, ADR-0069): ordinary placement, except where the aim lands on a pole,
     * where the column rule takes over.
     *
     * <p><b>The extension shows the real result</b>, not the spot vanilla would have chosen. A pole
     * aimed at any segment of a same-tier column previews the segment that would land on
     * <em>top</em>, because that is what {@link SupplyAreaPoleBlock#useItemOn} does -- clicking the
     * base raises a pole past the player's own reach, and a preview drawn beside the base would
     * describe a placement the pack does not perform.
     *
     * <p>A pole of another tier in the same Replace Group plans a Fast Replace of the whole column
     * instead (ADR-0082). The column's other refusals draw at the position the segment was headed
     * for, which is the only place a refusal about a column means anything.
     *
     * <p>A sneak places beside, because a sneaking player's click never reaches the block's
     * {@code useItemOn}.
     */
    @Override
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos aimed = Placements.aimedPos(context);
        BlockState aimedState = level.getBlockState(aimed);
        if (!(aimedState.getBlock() instanceof SupplyAreaPoleBlock) || context.isSecondaryUseActive()) {
            return Placements.vanillaPlan(this, context);
        }
        if (!aimedState.is(getBlock())) {
            if (ReplaceGroups.get().canReplace(id(getBlock()), id(aimedState.getBlock()))) {
                return replacePlan(context, aimed, aimedState.getBlock());
            }
            // Refused rather than placed beside, which would look exactly like the extension the
            // player asked for; drawn at the segment the player was plainly asking for.
            BlockPos top = PoleColumn.topOf(level, aimed);
            BlockPos at = top == null ? aimed.above() : top.above();
            return PlacementPlan.refused(at, getBlock().defaultBlockState(),
                    PlacementPlan.Refusal.OTHER_REPLACE_GROUP);
        }
        BlockPos top = PoleColumn.topOf(level, aimed);
        if (top == null) {
            return Placements.vanillaPlan(this, context);
        }
        BlockPos next = top.above();
        BlockState segment = getBlock().defaultBlockState();
        if (PoleColumn.height(level, aimed) >= PoleColumn.MAX_SEGMENTS) {
            return PlacementPlan.refused(next, segment, PlacementPlan.Refusal.COLUMN_FULL);
        }
        if (!level.getBlockState(next).canBeReplaced()) {
            return PlacementPlan.refused(next, segment, PlacementPlan.Refusal.BLOCKED_TOP);
        }
        return PlacementPlan.accepted(next, segment);
    }

    /** Every segment of the aimed column swapped in place, for the one item a column costs (ADR-0036, ADR-0082). */
    private PlacementPlan replacePlan(BlockPlaceContext context, BlockPos aimed, Block replaced) {
        Level level = context.getLevel();
        BlockPos base = PoleColumn.baseOf(level, aimed);
        int height = PoleColumn.height(level, aimed);
        BlockState segment = getBlock().defaultBlockState();
        List<PlacementPlan.Placed> blocks = new ArrayList<>(height);
        for (int i = 0; i < height; i++) {
            blocks.add(new PlacementPlan.Placed(base.above(i), segment));
        }
        Player player = context.getPlayer();
        boolean fits = player == null
                || ReplaceHandoff.fits(player, context.getHand(), new ItemStack(replaced), List.of());
        return PlacementPlan.replacing(blocks, fits ? null : PlacementPlan.Refusal.NO_ROOM_TO_RETURN);
    }

    private static String id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        PoleTier tier = tier();

        // What it covers. Stated as Factorio states it -- a square of tiles -- plus the vertical
        // band, which is the dimension Factorio has no answer for and a player cannot guess.
        tooltip.accept(Component.translatable("tooltip.planetaryfactory.pole.area",
                tier.supplySize(), tier.supplySize(), tier.verticalRadius())
                .withStyle(ChatFormatting.GRAY));

        // That it is wireless. The load-bearing line: a Minecraft player who is not told this will
        // go looking for the cable, fail to find one, and conclude the pole is broken.
        tooltip.accept(Component.translatable("tooltip.planetaryfactory.pole.wireless")
                .withStyle(ChatFormatting.GRAY));

        // And, for the creative pole, the one thing that makes it not the substation it is wearing
        // the footprint of. A dev tool that looks like a shipped block is one a player can leave in
        // a world and then read a self-sufficient factory off; the pink sprite says it at a
        // distance and this says it in the hand.
        if (getBlock() instanceof CreativeSupplyAreaPoleBlock) {
            tooltip.accept(Component.translatable("tooltip.planetaryfactory.pole.creative")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        // That height does not move the area, and how to add height. One line for both, because
        // they are the same fact from two sides: the column exists so the wire can go up, and the
        // footprint stays on the ground while it does. Saying only the first would replace an
        // invisible bug with an invisible rule.
        tooltip.accept(Component.translatable("tooltip.planetaryfactory.pole.column",
                Component.translatable(getBlock().getDescriptionId()), PoleColumn.MAX_SEGMENTS)
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
