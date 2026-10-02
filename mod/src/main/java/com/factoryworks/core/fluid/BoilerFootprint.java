package com.factoryworks.core.fluid;

import org.jspecify.annotations.Nullable;

import com.factoryworks.core.machine.footprint.Footprint;
import com.factoryworks.core.machine.footprint.Footprint.Local;

/**
 * Factorio's 3x2 boiler, one block tall (ADR-0114). The anchor is the front-middle block; local
 * {@code x} runs backward from the front, so the second row is {@code x = 1}.
 */
public final class BoilerFootprint {

    public static final Footprint FOOTPRINT = Footprint.of(
            new Local(0, 0, -1), new Local(0, 0, 1),
            new Local(1, 0, -1), new Local(1, 0, 0), new Local(1, 0, 1));

    /** The back middle block, the only steam port; its index is the part number and the position index. */
    public static final int STEAM_PART = FOOTPRINT.offsets().indexOf(new Local(1, 0, 0));

    /** Which segment a block stands in. */
    public enum Port {
        WATER, STEAM
    }

    /** The front row is water, the back middle steam, and the back corners no port. */
    public static @Nullable Port portAt(Local offset) {
        if (offset.y() != 0) {
            return null;
        }
        if (offset.x() == 0) {
            return Port.WATER;
        }
        return offset.x() == 1 && offset.z() == 0 ? Port.STEAM : null;
    }

    private BoilerFootprint() {
    }
}
