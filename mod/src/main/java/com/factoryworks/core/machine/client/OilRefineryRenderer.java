package com.factoryworks.core.machine.client;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.factoryworks.core.machine.OilRefineryBlockEntity;
import com.factoryworks.core.machine.OilRefineryFootprint;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import rearth.oritech.client.renderers.blocks.MachineRenderer;

/**
 * Oritech's Refinery model with its chamber module's model drawn on each chamber layer (ADR-0096).
 * The chambers are part blocks with no block entity, so the anchor draws them.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class OilRefineryRenderer implements BlockEntityRenderer<OilRefineryBlockEntity, OilRefineryRenderer.State> {

    public static final class State extends BlockEntityRenderState {
        BlockEntityRenderState base;
        BlockEntityRenderState chamber;
    }

    private final MachineRenderer base;
    private final MachineRenderer chamber;

    public OilRefineryRenderer(BlockEntityRendererProvider.Context context) {
        base = new MachineRenderer<>(context, "models/refinery", false);
        chamber = new MachineRenderer<>(context, "models/refinery_chamber_module", false);
    }

    @Override
    public State createRenderState() {
        State state = new State();
        state.base = base.createRenderState();
        state.chamber = chamber.createRenderState();
        return state;
    }

    @Override
    public void extractRenderState(OilRefineryBlockEntity refinery, State state, float partialTicks,
            Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(refinery, state, partialTicks, cameraPosition, breakProgress);
        base.extractRenderState(refinery, state.base, partialTicks, cameraPosition, breakProgress);
        chamber.extractRenderState(refinery, state.chamber, partialTicks, cameraPosition, breakProgress);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        base.submit(state.base, poseStack, collector, camera);
        for (int layer = 0; layer < OilRefineryFootprint.CHAMBERS; layer++) {
            poseStack.pushPose();
            poseStack.translate(0, OilRefineryFootprint.BASE_HEIGHT + layer, 0);
            chamber.submit(state.chamber, poseStack, collector, camera);
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(OilRefineryBlockEntity refinery) {
        return new AABB(refinery.getBlockPos()).inflate(2, 0, 2).expandTowards(0, OilRefineryFootprint.BASE_HEIGHT
                + OilRefineryFootprint.CHAMBERS, 0);
    }
}
