package com.factoryworks.core.placement.client;

import com.factoryworks.core.mining.rig.client.MiningAreaOverlay;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.client.PlacementPreviewEvent;

import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/** What the pack draws with a held item's Placement Preview: a rig's ore. */
final class PlanOverlays {

    private PlanOverlays() {
    }

    static void onOverlay(PlacementPreviewEvent.Overlay event) {
        SubmitCustomGeometryEvent geometry = event.getGeometry();
        drawMiningArea(geometry, event.getLevel(), event.getPlan());
    }

    /** The ore a held rig would work (#195) and the tile it would eject onto (#535). */
    private static void drawMiningArea(SubmitCustomGeometryEvent event, ClientLevel level, PlacementPlan plan) {
        if (plan.isRefused()) {
            return;
        }
        PlacementPlan.Placed first = plan.blocks().getFirst();
        MiningAreaOverlay.drawFor(event.getSubmitNodeCollector(), event.getPoseStack(), level,
                event.getLevelRenderState().cameraRenderState.pos, first.pos(), first.state());
        MiningAreaOverlay.drawDropPosition(event.getSubmitNodeCollector(), event.getPoseStack(),
                event.getLevelRenderState().cameraRenderState.pos, first.pos(), first.state());
    }
}
