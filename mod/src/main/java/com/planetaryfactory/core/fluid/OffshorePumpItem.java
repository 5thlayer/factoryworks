package com.planetaryfactory.core.fluid;

import com.planetaryfactory.core.PFBlocks;
import com.planetaryfactory.core.placement.PlacementPlan;
import com.planetaryfactory.core.placement.Placements;
import com.planetaryfactory.core.placement.PlansPlacement;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

/**
 * Placing a pump (#213, ADR-0050), and refusing to when there is nothing to pump.
 *
 * <p><b>Refused with a message, rather than placed and inert.</b> A pump that accepts any position
 * and then quietly produces nothing does not reach the player as a mistake at the pump: it reaches
 * them as a dead factory three machines downstream, with nothing in any log to say why. That is the
 * same failure the vein-indicator check exists to prevent, and the same reason the rig refuses a
 * footprint that does not fit rather than placing part of one.
 *
 * <p>Nothing is consumed on a refusal, because nothing was placed.
 */
public class OffshorePumpItem extends BlockItem implements PlansPlacement {

    /**
     * Named by {@code scripts/build-pump-assets.py}, which writes the string beside the block. It
     * names no fluid: the pumpable list grows per body, and a message listing it would go stale.
     */
    private static final String NO_SOURCE_KEY = "message.planetaryfactory.offshore_pump.no_source";

    public OffshorePumpItem(Item.Properties properties) {
        super(PFBlocks.OFFSHORE_PUMP.get(), properties);
    }

    /**
     * The pump's plan (#297, ADR-0069): vanilla's, plus the one refusal that is the pump's own.
     *
     * <p>Drawing the dry site red is the preview half of the same argument the message makes -- a
     * pump that reaches the player as a dead factory three machines downstream is the failure, and
     * a red block under the cursor says it before the click rather than after.
     */
    @Override
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        PlacementPlan plan = Placements.vanillaPlan(this, context);
        if (plan == null || plan.isRefused()) {
            return plan;
        }
        PlacementPlan.Placed at = plan.blocks().getFirst();
        if (OffshorePumpBlock.siteOf(context.getLevel(), at.pos()).isEmpty()) {
            return PlacementPlan.refused(at.pos(), at.state(), PlacementPlan.Refusal.NO_FLUID_SOURCE);
        }
        return plan;
    }

    /**
     * Placing <em>executes</em> the plan (ADR-0069), so the site is decided once.
     *
     * <p>It used to be decided twice -- once here and, after the preview landed, once in
     * {@link #plan} -- and at two different positions, because {@code getClickedPos} is the
     * pre-{@code updatePlacementContext} one and the plan's is the post. Wherever those differ the
     * preview and the click would disagree about whether there is water, which is exactly the drift
     * ADR-0069 exists to prevent.
     */
    @Override
    public InteractionResult place(BlockPlaceContext context) {
        PlacementPlan plan = Placements.planFor(this, context);
        if (plan == null || plan.isRefused()) {
            // Above the hotbar rather than in chat: it is feedback on a gesture the player just
            // made, not a log line. 26.1 moved the overlay form onto ServerPlayer, which is also
            // the only side worth sending it from.
            if (plan != null && plan.refusal() == PlacementPlan.Refusal.NO_FLUID_SOURCE
                    && context.getPlayer() instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.translatable(NO_SOURCE_KEY), true);
            }
            return InteractionResult.FAIL;
        }
        BlockPos pos = plan.blocks().getFirst().pos();
        Optional<String> fluid = OffshorePumpBlock.siteOf(context.getLevel(), pos);
        InteractionResult result = super.place(context);
        // The site's fluid is recorded here, the one moment the site is read (#256).
        if (result.consumesAction() && fluid.isPresent()
                && context.getLevel().getBlockEntity(pos) instanceof OffshorePumpBlockEntity pump) {
            pump.setFluid(fluid.get());
        }
        return result;
    }
}
