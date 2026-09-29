package com.factoryworks.core.placement.client;

import com.factoryworks.core.energy.SupplyAreaPoleBlock;
import com.factoryworks.core.energy.client.SupplyAreaBox;
import com.factoryworks.core.mining.rig.client.MiningAreaOverlay;
import io.github._5thlayer.groundworks.PlacementPlan;
import io.github._5thlayer.groundworks.client.PlacementPreviewEvent;

import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/** What the pack draws with a held item's Placement Preview: a pole's area and wires, a rig's ore. */
final class PlanOverlays {

    private PlanOverlays() {
    }

    static void onOverlay(PlacementPreviewEvent.Overlay event) {
        SubmitCustomGeometryEvent geometry = event.getGeometry();
        drawSupplyArea(geometry, event.getLevel(), event.getPlan());
        drawMiningArea(geometry, event.getLevel(), event.getPlan());
        PreviewWires.draw(geometry.getSubmitNodeCollector(), geometry.getPoseStack(), event.getLevel(),
                geometry.getLevelRenderState().cameraRenderState.pos, event.getPlan());
    }

    /**
     * The Supply Area Box a held pole would stand in (#158, ADR-0070). An extension draws none, since
     * the column it joins already draws that box; only the same pole below makes one, as a small pole
     * on a medium column's snow layer stands apart from it.
     */
    private static void drawSupplyArea(SubmitCustomGeometryEvent event, ClientLevel level, PlacementPlan plan) {
        if (plan.isRefused()) {
            return;
        }
        for (PlacementPlan.Placed placed : plan.blocks()) {
            if (!(placed.state().getBlock() instanceof SupplyAreaPoleBlock pole)) {
                continue;
            }
            if (level.getBlockState(placed.pos().below()).is(placed.state().getBlock())) {
                return;
            }
            SupplyAreaBox.drawAt(event.getSubmitNodeCollector(), event.getPoseStack(), level,
                    event.getLevelRenderState().cameraRenderState.pos, placed.pos(), pole.tier());
            return;
        }
    }

    /** The ore a held rig would work (#195). */
    private static void drawMiningArea(SubmitCustomGeometryEvent event, ClientLevel level, PlacementPlan plan) {
        if (plan.isRefused()) {
            return;
        }
        PlacementPlan.Placed first = plan.blocks().getFirst();
        MiningAreaOverlay.drawFor(event.getSubmitNodeCollector(), event.getPoseStack(), level,
                event.getLevelRenderState().cameraRenderState.pos, first.pos(), first.state());
    }
}
