package com.planetaryfactory.core.fluid.client;

import com.planetaryfactory.core.fluid.PFFluidTypes;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * The client half of Terra's two steam fluids: what texture and tint a tank or a pipe renders them
 * with (#223, ADR-0048).
 *
 * <p><b>26.1 moved this seam and the class is currently empty of effect.</b>
 * {@code IClientFluidTypeExtensions} no longer carries {@code getStillTexture},
 * {@code getFlowingTexture} or {@code getTintColor} -- a fluid's appearance comes from the model
 * system now, and what is left on the extension is the underwater overlay and the fog. So the two
 * tints below are recorded rather than applied, and both steam fluids currently render as whatever
 * the model system defaults them to.
 *
 * <p>The intent is unchanged and is the thing to restore: both fluids reuse vanilla's own water
 * still/flow textures, tinted rather than redrawn -- the same reuse-by-reference the Offshore Pump
 * makes of vanilla's block textures. Steam keeps close to water's own pale tint; Superheated Steam
 * is tinted toward the orange end, so the two read as visibly different fluids in a tank without
 * either needing a hand-drawn texture. Re-expressing that as fluid models is #223's to finish, and
 * it is the one thing on this class a compile cannot check: a fluid with no appearance ships as
 * invisible or as the missing-texture checker, and nothing fails.
 */
public final class SteamFluidClient {

    private static final Identifier WATER_STILL =
            Identifier.withDefaultNamespace("block/water_still");
    private static final Identifier WATER_FLOW =
            Identifier.withDefaultNamespace("block/water_flow");

    /** A pale, slightly translucent grey-blue -- steam rather than water, but visibly kin to it. */
    private static final int STEAM_TINT = 0xB0DCE6EC;

    /** An orange-red tint on the same water texture, reading as hot rather than merely wet. */
    private static final int SUPERHEATED_STEAM_TINT = 0xC0FF8A3D;

    private SteamFluidClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SteamFluidClient::registerExtensions);
    }

    private static void registerExtensions(RegisterClientExtensionsEvent event) {
        // Nothing to register: every hook this class used is gone from the extension in 26.1.
        // The listener stays so the seam keeps its name and #223 has somewhere to land.
    }
}
