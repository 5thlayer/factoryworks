package com.planetaryfactory.core.fluid;

/**
 * What the HUD needs from an engine that only the mixin can read: Oritech's speed is a private
 * method, and the row a private field (#352). Implemented by {@code SteamEngineEntityMixin}.
 */
public interface SteamEngineReadout {

    SteamEngineSpec planetaryfactory$readSpec();

    /** The speed the fill gives this tick, capped where the burn caps it. */
    double planetaryfactory$readSpeed();

    int planetaryfactory$readRowLength();
}
