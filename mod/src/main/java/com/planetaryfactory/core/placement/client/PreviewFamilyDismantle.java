package com.planetaryfactory.core.placement.client;

import java.util.List;

import com.planetaryfactory.core.dismantle.DismantlePlan;
import com.planetaryfactory.core.dismantle.FamilyDismantle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.jspecify.annotations.Nullable;

/**
 * A held item that dismantles, with a family's start stored (#431): the blocks its Dismantle Plan
 * would take up at the aim, or the stored start alone where the plan is refused. A belt tile aimed
 * at is left to the belt's own preview.
 */
final class PreviewFamilyDismantle {

    private PreviewFamilyDismantle() {
    }

    /** Whether this drew the frame's preview, so nothing else draws one. */
    static boolean draw(SubmitCustomGeometryEvent event, ClientLevel level, ItemStack stack, @Nullable HitResult hit) {
        if (!FamilyDismantle.dismantles(stack)) {
            return false;
        }
        BlockPos start = FamilyDismantle.liveStart(level, stack);
        if (start == null) {
            return false;
        }
        DismantlePlan plan = null;
        if (hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK) {
            if (!FamilyDismantle.claims(level, block.getBlockPos(), stack)) {
                return false;
            }
            plan = FamilyDismantle.plan(level, stack, block.getBlockPos());
        }
        DismantleOutline.draw(event, plan == null || plan.refused() ? List.of(start) : plan.blocks());
        return true;
    }
}
