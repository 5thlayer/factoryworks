package com.planetaryfactory.core.ore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

/** What is left in each outfield patch, and when it is gone (#370, #371, ADR-0041). */
class PatchLedgerTest {

    private static final PatchId IRON = new PatchId("minecraft:overworld", "iron", 2008, -1016);
    private static final PatchId COAL = new PatchId("minecraft:overworld", "coal", 2008, -1016);
    private static final int BLOCKS = 2;
    private static final int PER_BLOCK = 3;
    private static final long TOTAL = BLOCKS * PER_BLOCK;

    private final PatchLedger ledger = new PatchLedger();

    @Test
    void anUntouchedPatchHoldsItsTotal() {
        assertEquals(TOTAL, ledger.remaining(IRON, TOTAL));
        assertFalse(ledger.isExhausted(IRON));
    }

    @Test
    void eachDrawTakesOneUnitFromThePatch() {
        ledger.drew(IRON);
        ledger.drew(IRON);

        assertEquals(TOTAL - 2, ledger.remaining(IRON, TOTAL));
        assertEquals(TOTAL, ledger.remaining(COAL, TOTAL), "a patch is its resource and its centre");
    }

    @Test
    void aPatchIsExhaustedWhenItsLastBlockGoesAndNotBefore() {
        for (int unit = 0; unit < PER_BLOCK; unit++) {
            ledger.drew(IRON);
        }
        assertFalse(ledger.blockGone(IRON, 0, BLOCKS), "one block of two is gone");
        for (int unit = 0; unit < PER_BLOCK; unit++) {
            ledger.drew(IRON);
        }

        assertTrue(ledger.blockGone(IRON, 0, BLOCKS));
        assertTrue(ledger.isExhausted(IRON));
        assertEquals(0, ledger.remaining(IRON, TOTAL));
        assertFalse(ledger.blockGone(IRON, 0, BLOCKS), "exhausted once, however often it is asked");
    }

    @Test
    void aBlockBlownUpTakesItsUnitsWithIt() {
        ledger.drew(IRON);
        ledger.blockGone(IRON, PER_BLOCK - 1, BLOCKS);

        assertEquals(TOTAL - PER_BLOCK, ledger.remaining(IRON, TOTAL));
        assertTrue(ledger.blockGone(IRON, PER_BLOCK, BLOCKS),
                "the last block gone ends the patch, however it went");
    }

    @Test
    void theLedgerSurvivesItsCodec() {
        ledger.drew(IRON);
        ledger.blockGone(COAL, PER_BLOCK, 1);

        JsonElement json = PatchLedger.CODEC.encodeStart(JsonOps.INSTANCE, ledger).getOrThrow();
        PatchLedger back = PatchLedger.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        assertEquals(TOTAL - 1, back.remaining(IRON, TOTAL));
        assertTrue(back.isExhausted(COAL));
    }
}
