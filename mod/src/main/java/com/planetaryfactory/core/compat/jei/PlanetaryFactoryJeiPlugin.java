package com.planetaryfactory.core.compat.jei;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import net.minecraft.resources.Identifier;

/**
 * The pack's JEI plugin. Loaded by JEI's own annotation scan, so nothing in the mod references this
 * class and it never loads when JEI is absent.
 */
@JeiPlugin
public final class PlanetaryFactoryJeiPlugin implements IModPlugin {

    private static final Identifier UID =
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.MOD_ID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    // No registerAdvanced. What this plugin registered was the research-lock decorator against
    // every recipe type JEI knows, and the decorator went with Researchd -- see
    // docs/port/blocked-removals-26.1.2.md. The plugin itself stays registered so #75 has the seam
    // back the moment #260 lands.
}
