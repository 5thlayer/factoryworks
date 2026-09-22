package com.planetaryfactory.core.radar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** A team's chart: its charted sectors per dimension, and its round trip through a save (#368). */
class RadarChartsTest {

    private static final UUID TEAM = UUID.fromString("6f1b1e5e-0000-4000-8000-00000000abcd");
    private static final UUID OTHER = UUID.fromString("6f1b1e5e-0000-4000-8000-00000000beef");
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";

    @Test
    void aSectorChartedTwiceIsNewOnlyOnce() {
        RadarCharts charts = new RadarCharts();
        assertTrue(charts.chart(TEAM, OVERWORLD, new Sector(2, 3)));
        assertFalse(charts.chart(TEAM, OVERWORLD, new Sector(2, 3)));
        assertEquals(Set.of(new Sector(2, 3)), charts.sectors(TEAM, OVERWORLD));
    }

    @Test
    void chartsAreKeptPerTeamAndPerDimension() {
        RadarCharts charts = new RadarCharts();
        charts.chart(TEAM, OVERWORLD, new Sector(0, 0));
        assertTrue(charts.chart(TEAM, NETHER, new Sector(0, 0)));
        assertTrue(charts.chart(OTHER, OVERWORLD, new Sector(0, 0)));
        assertEquals(Set.of(), charts.sectors(OTHER, NETHER));
        assertEquals(Set.of(), charts.sectors(UUID.randomUUID(), OVERWORLD));
    }

    @Test
    void theChartsSurviveTheRoundTrip() {
        RadarCharts charts = new RadarCharts();
        charts.chart(TEAM, OVERWORLD, new Sector(-4, 9));
        charts.chart(TEAM, OVERWORLD, new Sector(1, 1));
        charts.chart(OTHER, NETHER, new Sector(0, -1));

        JsonElement written = RadarCharts.CODEC.encodeStart(JsonOps.INSTANCE, charts).getOrThrow();
        RadarCharts read = RadarCharts.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow();

        assertEquals(Set.of(new Sector(-4, 9), new Sector(1, 1)), read.sectors(TEAM, OVERWORLD));
        assertEquals(Set.of(new Sector(0, -1)), read.sectors(OTHER, NETHER));
    }
}
