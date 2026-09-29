package com.factoryworks.core.machine.footprint;

import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.PlansPlacement;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jspecify.annotations.Nullable;
import rearth.oritech.item.OritechGeoItem;
import rearth.oritech.util.ColorableMachine;

/**
 * One click places a whole {@link FootprintMachine} (ADR-0069), or nothing at all.
 *
 * <p>An {@link OritechGeoItem}, so the item in the hand is the Oritech model GeckoLib draws, with
 * the scale and default colour Oritech's own {@code BlockContent} gives that model, read off the
 * jar.
 *
 * <p>Replaces {@code BlockItem.place} whole, for the reason {@code RigBlockItem} does: the
 * single-block flow has no hook for the other blocks, and a partial machine that ate the item is
 * the failure every footprint refuses.
 */
public class FootprintItem extends OritechGeoItem implements PlansPlacement {

    private final FootprintMachine machine;

    public FootprintItem(Properties properties, FootprintMachine machine, float scale, String model) {
        this(properties, machine, scale, model, ColorableMachine.ColorVariant.ORANGE);
    }

    public FootprintItem(Properties properties, FootprintMachine machine, float scale, String model,
                         ColorableMachine.ColorVariant color) {
        super(machine.anchor().get(), properties, scale, model, color);
        this.machine = machine;
    }

    @Override
    public @Nullable PlacementPlan plan(BlockPlaceContext context) {
        return machine.plan(context);
    }

    /** Replaces {@code BlockItem.place} whole: see {@link FootprintMachine#place}. */
    @Override
    public InteractionResult place(BlockPlaceContext context) {
        return FootprintMachine.place(this, context) == null ? InteractionResult.FAIL : InteractionResult.SUCCESS;
    }
}
