package com.factoryworks.core.fluid;

import java.util.OptionalInt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * That the mod reads the tint resource (#277, #619).
 *
 * <p>{@code tests/pack/test_fluid_tints.py} asserts the resource names the five fluids, but
 * it reads JSON. A parser reaching for the wrong key, or dropping the alpha,
 * leaves that check green and every fluid invisible or the wrong colour.
 */
class FluidTintCorpusTest {

    @Test
    @DisplayName("a retinted fluid answers its colour, fully opaque")
    void retintedFluidIsOpaque() {
        OptionalInt argb = FluidTintCorpus.get().tint("factoryworks:heavy_oil");
        assertTrue(argb.isPresent(), "heavy oil is tinted toward Factorio's orange");
        assertEquals(0xFF, argb.getAsInt() >>> 24,
                "a tint with no alpha draws the fluid fully transparent");
    }

    @Test
    @DisplayName("the colour is the resource's hex, not a default")
    void colourIsTheResources() {
        assertEquals(0xFFFFCC1E, FluidTintCorpus.get().tint("factoryworks:sulfuric_acid").getAsInt());
    }

    @Test
    @DisplayName("a fluid with no row has no tint")
    void unknownFluidHasNoTint() {
        assertFalse(FluidTintCorpus.get().tint("factoryworks:crude_oil").isPresent());
    }
}
