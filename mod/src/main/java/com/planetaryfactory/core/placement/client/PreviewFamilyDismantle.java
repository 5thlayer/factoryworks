package com.planetaryfactory.core.placement.client;

import java.util.List;

import com.planetaryfactory.core.dismantle.DismantlePlan;
import com.planetaryfactory.core.dismantle.FamilyDismantle;
import io.github._5thlayer.placementpreview.client.Outline;
import io.github._5thlayer.placementpreview.client.PlacementPreviewEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A held item that dismantles, with a family's start stored (#431): the blocks its Dismantle Plan
 * would take up at the aim, or the stored start alone where the plan is refused.
 */
final class PreviewFamilyDismantle {

    private PreviewFamilyDismantle() {
    }

    static void onTakeover(PlacementPreviewEvent.Takeover event) {
        if (!FamilyDismantle.dismantles(event.getStack())) {
            return;
        }
        BlockPos start = FamilyDismantle.liveStart(event.getLevel(), event.getStack());
        if (start == null) {
            return;
        }
        DismantlePlan plan = null;
        if (event.getHitResult() instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK) {
            // A belt tile is Beltworks' own Takeover, and no order between the two is promised.
            if (!FamilyDismantle.claims(event.getLevel(), block.getBlockPos(), event.getStack())) {
                return;
            }
            plan = FamilyDismantle.plan(event.getLevel(), event.getStack(), block.getBlockPos());
        }
        Outline.draw(event.getGeometry(), plan == null || plan.refused() ? List.of(start) : plan.blocks());
        event.setCanceled(true);
    }
}
