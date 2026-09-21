package com.planetaryfactory.core.fluid;

/**
 * What the HUD needs from an engine that only the mixin can read: the row is a private field
 * (#352). Implemented by {@code SteamEngineEntityMixin}.
 */
public interface SteamEngineReadout {

    SteamEngineSpec planetaryfactory$readSpec();

    int planetaryfactory$readRowLength();
}
