package com.factoryworks.core.mining.rig.client;

import com.factoryworks.core.FactoryWorksCore;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/** Untextured quads drawn over everything: vanilla ships no such type (#588). */
public final class OverlayRenderTypes {

    private static final RenderPipeline QUADS_NO_DEPTH = RenderPipeline
            .builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(FactoryWorksCore.NAMESPACE, "pipeline/overlay_quads_no_depth"))
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .build();

    private static final RenderType QUADS_NO_DEPTH_TYPE = RenderType.create("factoryworks_overlay_quads_no_depth",
            RenderSetup.builder(QUADS_NO_DEPTH).sortOnUpload().createRenderSetup());

    private OverlayRenderTypes() {
    }

    public static RenderType quadsNoDepth() {
        return QUADS_NO_DEPTH_TYPE;
    }

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(QUADS_NO_DEPTH);
    }
}
