package com.planetaryfactory.core.placement.client;

import net.neoforged.neoforge.common.NeoForge;

/**
 * The client half of the Placement Preview (#297, ADR-0069).
 *
 * <p>Called only on the client, from {@code PlanetaryFactoryCore}. One listener: the preview has no
 * keybind, no toggle and no state beyond its own cache.
 */
public final class PlacementPreviewClient {

    private PlacementPreviewClient() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PlacementPreview::onSubmitGeometry);
    }
}
