package com.planetaryfactory.core.fluid;

import java.util.OptionalInt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * That the mod reads the tint resource {@code scripts/build-fluid-tints.py} writes (#277).
 *
 * <p>{@code tests/pack/test_fluid_tints.py} asserts the resource renders each borrowed fluid in
 * Factorio's colour, but it reads JSON. A parser reaching for the wrong key, or dropping the alpha,
 * leaves that check green and every retinted fluid invisible or in Oritech's colour.
 */
class FluidTintCorpusTest {

    @Test
    @DisplayName("a retinted fluid answers its colour, fully opaque")
    void retintedFluidIsOpaque() {
        OptionalInt argb = FluidTintCorpus.get().tint("oritech:still_heavy_oil");
        assertTrue(argb.isPresent(), "heavy oil is retinted from near-black to Factorio's orange");
        assertEquals(0xFF, argb.getAsInt() >>> 24,
                "a tint with no alpha draws the fluid fully transparent");
    }

    @Test
    @DisplayName("the colour is the resource's hex, not a default")
    void colourIsTheResources() {
        assertEquals(0xFFFFCC1E, FluidTintCorpus.get().tint("oritech:still_sulfuric_acid").getAsInt());
    }

    @Test
    @DisplayName("a fluid Oritech already colours right keeps Oritech's tint")
    void keptFluidHasNoTint() {
        assertFalse(FluidTintCorpus.get().tint("oritech:still_oil").isPresent());
    }
}
