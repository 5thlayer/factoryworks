package com.factoryworks.core.worldgen;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterCensusTest {

    private static final int SEA_LEVEL = 63;
    private static final WaterFixture ROW = WaterFixture.TERRA;

    /** 100 columns: 25 water (4 shelf, 21 deep), all sea; 3 shore; 72 other land. */
    private static WaterCensus healthy() {
        WaterCensus census = new WaterCensus(ROW, SEA_LEVEL);
        for (int i = 0; i < 4; i++) {
            census.column(60, true, false);
        }
        for (int i = 0; i < 21; i++) {
            census.column(7, true, false);
        }
        for (int i = 0; i < 3; i++) {
            census.column(64, false, true);
        }
        for (int i = 0; i < 72; i++) {
            census.column(68, false, false);
        }
        census.startColumn(66);
        return census;
    }

    private static void assertOneFailure(WaterCensus census, String mentioning) {
        List<String> failures = census.failures();
        assertEquals(1, failures.size(), failures.toString());
        assertTrue(failures.get(0).contains(mentioning), failures.get(0));
    }

    @Test
    @DisplayName("a body that meets every band has no failures")
    void healthyPasses() {
        assertEquals(List.of(), healthy().failures());
    }

    @Test
    @DisplayName("water is below sea level, not at it")
    void seaLevelIsDry() {
        WaterCensus census = new WaterCensus(ROW, SEA_LEVEL);
        census.column(SEA_LEVEL, false, false);
        census.column(SEA_LEVEL - 1, true, false);
        assertTrue(census.summary().contains("water 50.0%"), census.summary());
    }

    @Test
    @DisplayName("too little water fails the share")
    void dryBodyFails() {
        WaterCensus census = healthy();
        for (int i = 0; i < 60; i++) {
            census.column(68, false, false);
        }
        assertOneFailure(census, "water covers");
    }

    @Test
    @DisplayName("a sea biome over dry ground fails, as #316 measured")
    void drySeaFails() {
        WaterCensus census = healthy();
        for (int i = 0; i < 4; i++) {
            census.column(66, true, false);
        }
        assertOneFailure(census, "sea columns are water");
    }

    @Test
    @DisplayName("water under a land biome fails")
    void waterUnderLandFails() {
        WaterCensus census = healthy();
        census.column(7, false, false);
        census.column(7, false, false);
        census.column(7, false, false);
        assertOneFailure(census, "water columns are sea");
    }

    @Test
    @DisplayName("a wide shore fails")
    void wideShoreFails() {
        WaterCensus census = healthy();
        for (int i = 0; i < 4; i++) {
            census.column(64, false, true);
        }
        assertOneFailure(census, "shore covers");
    }

    @Test
    @DisplayName("a sea with no shelf fails")
    void noShelfFails() {
        WaterCensus census = new WaterCensus(ROW, SEA_LEVEL);
        for (int i = 0; i < 25; i++) {
            census.column(7, true, false);
        }
        for (int i = 0; i < 75; i++) {
            census.column(68, false, false);
        }
        assertOneFailure(census, "shelf");
    }

    @Test
    @DisplayName("water neither on the shelf nor at the bedrock band fails")
    void midDepthWaterFails() {
        WaterCensus census = healthy();
        census.column(30, true, false);
        assertOneFailure(census, "neither shelf nor bedrock");
    }

    @Test
    @DisplayName("the deep floor's ceiling is inclusive")
    void deepFloorIsInclusive() {
        WaterCensus census = healthy();
        census.column(ROW.deepFloor(), true, false);
        assertEquals(List.of(), census.failures());
    }

    @Test
    @DisplayName("water within the start's reach of spawn fails")
    void wetStartFails() {
        WaterCensus census = healthy();
        census.startColumn(SEA_LEVEL - 1);
        assertOneFailure(census, "spawn");
    }

    @Test
    @DisplayName("an empty census fails rather than passing vacuously")
    void emptyFails() {
        assertTrue(new WaterCensus(ROW, SEA_LEVEL).failures().size() > 0);
    }
}
