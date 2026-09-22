package com.planetaryfactory.core.oil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.planetaryfactory.core.ore.OutfieldShape;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** A crude-oil field: which columns of its mask carry a well, and how much each holds (ADR-0081). */
class OilFieldTest {

    private static final OutfieldShape.Noise FLAT = (x, z) -> 0;
    private static final OutfieldShape.Land LAND = (x, z) -> true;
    private static final int X = 2300;
    private static final int Z = 0;

    private static OilField field() {
        return OilField.of(X, Z, FLAT, LAND);
    }

    @Test
    void aColumnCarriesAWellWithProbabilityOneInNinetySix() {
        // random_penalty passes 1/48 of the tiles, and the value it leaves is itself a probability.
        assertEquals(1.0 / 96, OilField.columnProbability(), 1e-12);
    }

    @Test
    void theFieldIsTheOutfieldShapeOfCrudesLaw() {
        OilField field = field();
        OutfieldShape shape = OutfieldShape.of(OilCorpus.get().law(), 1.0, X, Z, FLAT, LAND);
        assertEquals(shape.blockCount(), field.shape().blockCount());
        assertTrue(field.shape().blockCount() > 1_000, "a field 2,300 out spans about 21 blocks' radius");
    }

    @Test
    void everyDrawnWellIsInTheMask() {
        OilField field = field();
        for (OilField.Well well : field.draw(() -> 0.0)) {
            assertTrue(field.shape().contains(well.x(), well.z()), well.toString());
        }
    }

    @Test
    void wellsStandAtLeastThreeBlocksApart() {
        assertEquals(3, OilField.spacing());
        List<OilField.Well> wells = field().draw(() -> 0.0);
        for (OilField.Well a : wells) {
            for (OilField.Well b : wells) {
                if (a != b) {
                    assertTrue(Math.max(Math.abs(a.x() - b.x()), Math.abs(a.z() - b.z())) >= 3, a + " and " + b);
                }
            }
        }
    }

    @Test
    void aColumnEveryDrawPassesIsSkippedOnlyForAWellWithinReach() {
        OilField field = field();
        List<OilField.Well> wells = field.draw(() -> 0.0);
        for (int x = X - field.shape().reach(); x <= X + field.shape().reach(); x++) {
            for (int z = Z - field.shape().reach(); z <= Z + field.shape().reach(); z++) {
                if (!field.shape().contains(x, z)) {
                    continue;
                }
                int cx = x;
                int cz = z;
                assertTrue(wells.stream().anyMatch(w -> Math.max(Math.abs(w.x() - cx), Math.abs(w.z() - cz)) < 3),
                        "column " + x + ", " + z + " is free and drew no well");
            }
        }
    }

    @Test
    void noColumnDrawsAWellWhenNoRollPasses() {
        assertTrue(field().draw(() -> 0.999).isEmpty());
    }

    @Test
    void aboutOneColumnInNinetySixCarriesAWell() {
        OilField field = field();
        Random random = new Random(377);
        long wells = 0;
        long columns = 0;
        for (int i = 0; i < 400; i++) {
            wells += field.draw(random::nextDouble).size();
            columns += field.shape().blockCount();
        }
        double rate = (double) wells / columns;
        // The spacing turns away a roll beside an earlier well, so the rate sits a little under 1/96.
        assertTrue(rate < 1.0 / 96 && rate > 0.8 / 96, "rate " + rate * 96 + "/96");
    }

    @Test
    void aWellsAmountIsTheConeTimesFortyEightPlusTheFlatRichnessTimesTheDistanceLaw() {
        OilField field = field();
        double height = OilCorpus.get().law().height(1.0, 2300);
        double radius = OilCorpus.get().law().radius(1.0, 2300);
        double richness = (1000 + 2300.0) / 2600;
        assertEquals((long) ((48 * height + 220_000) * richness), field.amountAt(X, Z), 2);

        double r = 5;
        double cone = 1 - r / radius;
        double distance = Math.sqrt((X + 3.0) * (X + 3.0) + 16);
        assertEquals((long) ((48 * height * cone + 220_000) * (1000 + distance) / 2600), field.amountAt(X + 3, Z + 4), 2);
    }

    @Test
    void aColumnPastTheConesEdgeHoldsTheFlatRichnessOnly() {
        OilField field = field();
        double radius = OilCorpus.get().law().radius(1.0, 2300);
        int x = X + (int) Math.ceil(radius) + 1;
        assertEquals((long) (220_000 * (1000 + (double) x) / 2600), field.amountAt(x, Z), 2);
    }

    @Test
    void theRichnessIsFlatWithin1600Blocks() {
        OilField near = OilField.of(1000, 0, FLAT, LAND);
        double height = OilCorpus.get().law().height(1.0, 1000);
        assertEquals((long) (48 * height + 220_000), near.amountAt(1000, 0), 2);
    }

    @Test
    void aFarWellStartsAbove100Percent() {
        assertTrue(field().amountAt(X, Z) > WellYield.fromCorpus().normal());
    }

    @Test
    void nothingIsDealtWithin150BlocksOfOrigin() {
        assertTrue(OilField.of(100, 0, FLAT, LAND).shape().isEmpty());
    }

    @Test
    void aWellOnAnOreColumnIsNotPlaced() {
        OilField field = field();
        List<OilField.Well> wells = field.draw(() -> 0.0);
        OilField.Well taken = wells.getFirst();
        List<OilField.Well> placed = OilField.placeable(wells, (x, z) -> x == taken.x() && z == taken.z());
        assertEquals(wells.size() - 1, placed.size());
        assertFalse(placed.contains(taken));
    }

    @Test
    void theFieldsYieldIsItsWellsSummed() {
        List<OilField.Well> wells = List.of(new OilField.Well(0, 0, 300_000), new OilField.Well(3, 0, 150_000));
        assertEquals(450_000, OilField.total(wells));
    }
}
