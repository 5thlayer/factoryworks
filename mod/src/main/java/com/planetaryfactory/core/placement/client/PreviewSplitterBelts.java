package com.planetaryfactory.core.placement.client;

import com.planetaryfactory.core.placement.PlacementPlan;

import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import rearth.belts.blocks.SplitterBlock;
import rearth.belts.client.renderers.ChuteBeltRenderer;

/**
 * A planned splitter half's belt surface, which the fork draws with a block entity renderer rather
 * than the block model the preview draws (#355). Loaded only when the fork is.
 */
final class PreviewSplitterBelts {

    private PreviewSplitterBelts() {
    }

    static void draw(SubmitCustomGeometryEvent event, PlacementPlan plan, int tint) {
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        for (PlacementPlan.Placed placed : plan.blocks()) {
            if (placed.state().getBlock() instanceof SplitterBlock splitter) {
                ChuteBeltRenderer.submitPlannedSplitter(event.getPoseStack(), event.getSubmitNodeCollector(), camera,
                        placed.pos(), placed.state().getValue(SplitterBlock.FACING), splitter.tier(), tint);
            }
        }
    }
}
