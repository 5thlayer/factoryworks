package com.planetaryfactory.core.placement.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jspecify.annotations.Nullable;
import io.github._5thlayer.beltworks.items.DismantlePlan;
import io.github._5thlayer.beltworks.items.Dismantling;

/**
 * A held item that dismantles belts, with a start stored (#404): the tiles and wedges its Dismantle
 * Plan would take up at the aim, or the stored start alone where the plan is refused. Loaded only
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
        DismantleOutline.draw(event, positions);
        return true;
    }
}
