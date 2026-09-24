package com.planetaryfactory.core.placement.client;

import java.util.ArrayList;
import java.util.List;

import com.planetaryfactory.core.placement.PlacementPlan;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jspecify.annotations.Nullable;
import rearth.belts.items.DismantlePlan;
import rearth.belts.items.Dismantling;

/**
 * A held item that dismantles, with a start stored (#404): the tiles and wedges its Dismantle Plan
 * would take up at the aim, in red, or only the stored start where the plan is refused. Loaded only
 * when the fork is.
 */
final class PreviewDismantle {

    private PreviewDismantle() {
    }

    /** Whether this drew the frame's preview, so the placement preview draws nothing. */
    static boolean draw(SubmitCustomGeometryEvent event, ClientLevel level, ItemStack stack, @Nullable HitResult hit) {
        if (!Dismantling.dismantles(stack)) {
            return false;
        }
        BlockPos start = Dismantling.liveStart(level, stack);
        if (start == null) {
            return false;
        }
        DismantlePlan plan = hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK
                ? Dismantling.plan(level, stack, block.getBlockPos())
                : null;
        List<BlockPos> positions = new ArrayList<>();
        if (plan == null || plan.refused()) {
            positions.add(start);
        } else {
            positions.addAll(plan.tiles());
            positions.addAll(plan.wedges());
        }
        PlacementPreview.drawRefused(event, level, positions.stream()
                .map(pos -> new PlacementPlan.Placed(pos, level.getBlockState(pos)))
                .toList());
        return true;
    }
}
