package com.planetaryfactory.core.placement.client;

import net.neoforged.neoforge.common.NeoForge;

/** What the pack adds to placementpreview's Placement Preview (ADR-0069). Client only. */
public final class PlacementPreviewClient {

    private PlacementPreviewClient() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PreviewFamilyDismantle::onTakeover);
        NeoForge.EVENT_BUS.addListener(PlanOverlays::onOverlay);
    }
}
