package com.planetaryfactory.core.fluid;

import com.planetaryfactory.core.PFBlocks;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

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
public class OffshorePumpItem extends BlockItem {

    /** Named by {@code scripts/build-pump-assets.py}, which writes the string beside the block. */
    private static final String NO_SOURCE_KEY = "message.planetaryfactory.offshore_pump.no_source";

    /** Water and lava both adjoin (#256): refused rather than one being picked. */
    private static final String MIXED_SOURCE_KEY = "message.planetaryfactory.offshore_pump.mixed_source";

    public OffshorePumpItem(Item.Properties properties) {
        super(PFBlocks.OFFSHORE_PUMP.get(), properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (!context.canPlace()) {
            return InteractionResult.FAIL;
        }
        OffshorePumpSiting.Verdict verdict = OffshorePumpBlock.siteOf(context.getLevel(), context.getClickedPos());
        if (OffshorePumpBlock.PumpedFluid.of(verdict) == null) {
            // Above the hotbar rather than in chat: it is feedback on a gesture the player just
            // made, not a log line. 26.1 moved the overlay form onto ServerPlayer, which is also
            // the only side worth sending it from.
            if (context.getPlayer() instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.translatable(
                        verdict == OffshorePumpSiting.Verdict.MIXED ? MIXED_SOURCE_KEY : NO_SOURCE_KEY), true);
            }
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }

    /** The site's fluid goes into the state here, the one moment the site is read (#256). */
    @Override
    @Nullable
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        OffshorePumpBlock.PumpedFluid fluid = OffshorePumpBlock.PumpedFluid.of(
                OffshorePumpBlock.siteOf(context.getLevel(), context.getClickedPos()));
        return state == null || fluid == null ? null : state.setValue(OffshorePumpBlock.FLUID, fluid);
    }
}
