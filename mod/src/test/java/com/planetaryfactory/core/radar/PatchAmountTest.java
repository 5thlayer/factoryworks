package com.planetaryfactory.core.radar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** A patch's amount as its map label reads, in Factorio's short form (#370). */
class PatchAmountTest {

    @Test
    void underAThousandIsTheWholeNumber() {
        assertEquals("0", PatchAmount.format(0));
        assertEquals("999", PatchAmount.format(999));
    }

    @Test
    void thousandsAndMillionsKeepOneDecimalBelowAHundred() {
        assertEquals("1.2k", PatchAmount.format(1_234));
        assertEquals("45.6k", PatchAmount.format(45_678));
        assertEquals("456k", PatchAmount.format(456_789));
        assertEquals("1.2M", PatchAmount.format(1_234_567));
        assertEquals("12.3M", PatchAmount.format(12_345_678));
        assertEquals("123M", PatchAmount.format(123_456_789));
    }

    @Test
    void aFigureNeverRoundsUpPastWhatIsThere() {
        assertEquals("9.9k", PatchAmount.format(9_999));
        assertEquals("999k", PatchAmount.format(999_999));
    }
}
