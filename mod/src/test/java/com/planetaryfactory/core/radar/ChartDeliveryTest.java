package com.planetaryfactory.core.radar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Which of a team's charted sectors each player's map is sent, and when (#369, ADR-0079). */
class ChartDeliveryTest {

    private static final UUID TEAM = UUID.fromString("6f1b1e5e-0000-4000-8000-00000000abcd");
    private static final UUID ALICE = UUID.fromString("6f1b1e5e-0000-4000-8000-0000000000a1");
    private static final UUID BOB = UUID.fromString("6f1b1e5e-0000-4000-8000-0000000000b0");
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";
    private static final int ALL = Integer.MAX_VALUE;

    private final RadarCharts charts = new RadarCharts();
    private final ChartDelivery delivery = new ChartDelivery();

    private void chart(UUID team, String dimension, Sector sector) {
        if (charts.chart(team, dimension, sector)) {
            delivery.charted(team, dimension, sector);
        }
    }

    private Set<Sector> drain(UUID player) {
        return new HashSet<>(delivery.take(player, ALL));
    }

    @Test
    void aLoginReceivesExactlyTheSectorsThePlayerLacks() {
        chart(TEAM, OVERWORLD, new Sector(0, 0));
        delivery.observe(ALICE, TEAM, OVERWORLD, charts);
        assertEquals(Set.of(new Sector(0, 0)), drain(ALICE));
        delivery.logout(ALICE);

        chart(TEAM, OVERWORLD, new Sector(1, 0));
        chart(TEAM, OVERWORLD, new Sector(2, 0));
        delivery.observe(ALICE, TEAM, OVERWORLD, charts);

        assertEquals(Set.of(new Sector(1, 0), new Sector(2, 0)), drain(ALICE));
    }

    @Test
    void aSecondLoginSendsNothing() {
        chart(TEAM, OVERWORLD, new Sector(0, 0));
        chart(TEAM, OVERWORLD, new Sector(0, 1));
        delivery.observe(ALICE, TEAM, OVERWORLD, charts);
        drain(ALICE);
        delivery.logout(ALICE);

        delivery.observe(ALICE, TEAM, OVERWORLD, charts);

        assertEquals(Set.of(), drain(ALICE));
    }

    @Test
    void observingAgainWithNothingChangedQueuesNothingTwice() {
        chart(TEAM, OVERWORLD, new Sector(0, 0));
        delivery.observe(ALICE, TEAM, OVERWORLD, charts);
        delivery.observe(ALICE, TEAM, OVERWORLD, charts);

        assertEquals(List.of(new Sector(0, 0)), delivery.take(ALICE, ALL));
        delivery.observe(ALICE, TEAM, OVERWORLD, charts);
        assertEquals(List.of(), delivery.take(ALICE, ALL));
    }

    @Test
    void aPlayerWhoJoinsTheTeamReceivesWhatItChartedBefore() {
        chart(TEAM, OVERWORLD, new Sector(3, 3));
        chart(TEAM, OVERWORLD, new Sector(4, 3));
        delivery.observe(BOB, BOB, OVERWORLD, charts);
        assertEquals(Set.of(), drain(BOB));

        delivery.observe(BOB, TEAM, OVERWORLD, charts);

        assertEquals(Set.of(new Sector(3, 3), new Sector(4, 3)), drain(BOB));
    }

    @Test
    void aSectorAlreadyOnTheMapIsNotSentAgainByTheNextTeam() {
        UUID other = UUID.randomUUID();
        chart(other, OVERWORLD, new Sector(0, 0));
        delivery.observe(BOB, other, OVERWORLD, charts);
        drain(BOB);

        chart(TEAM, OVERWORLD, new Sector(0, 0));
        chart(TEAM, OVERWORLD, new Sector(1, 1));
        delivery.observe(BOB, TEAM, OVERWORLD, charts);

        assertEquals(Set.of(new Sector(1, 1)), drain(BOB));
    }

    @Test
    void theBacklogIsSentAFewSectorsAtATime() {
        for (int x = 0; x < 5; x++) {
            chart(TEAM, OVERWORLD, new Sector(x, 0));
        }
        delivery.observe(ALICE, TEAM, OVERWORLD, charts);

        assertEquals(2, delivery.take(ALICE, 2).size());
        assertEquals(2, delivery.take(ALICE, 2).size());
        assertEquals(1, delivery.take(ALICE, 2).size());
        assertEquals(0, delivery.take(ALICE, 2).size());
    }

    @Test
    void aNewSectorGoesToOnlineMembersInItsDimensionOnly() {
        delivery.observe(ALICE, TEAM, OVERWORLD, charts);
        delivery.observe(BOB, TEAM, NETHER, charts);

        chart(TEAM, OVERWORLD, new Sector(7, 7));

        assertEquals(Set.of(new Sector(7, 7)), drain(ALICE));
        assertEquals(Set.of(), drain(BOB));
        delivery.observe(BOB, TEAM, OVERWORLD, charts);
        assertEquals(Set.of(new Sector(7, 7)), drain(BOB));
    }

    @Test
    void anOfflineMemberGetsANewSectorAtLogin() {
        chart(TEAM, OVERWORLD, new Sector(7, 7));
        assertEquals(Set.of(), drain(ALICE));

        delivery.observe(ALICE, TEAM, OVERWORLD, charts);

        assertEquals(Set.of(new Sector(7, 7)), drain(ALICE));
    }

    @Test
    void leavingADimensionMidBacklogKeepsTheRestOwed() {
        chart(TEAM, OVERWORLD, new Sector(0, 0));
        chart(TEAM, OVERWORLD, new Sector(1, 0));
        chart(TEAM, NETHER, new Sector(5, 5));
        delivery.observe(ALICE, TEAM, OVERWORLD, charts);
        Set<Sector> first = new HashSet<>(delivery.take(ALICE, 1));

        delivery.observe(ALICE, TEAM, NETHER, charts);
        assertEquals(Set.of(new Sector(5, 5)), drain(ALICE));

        delivery.observe(ALICE, TEAM, OVERWORLD, charts);
        Set<Sector> rest = drain(ALICE);
        assertEquals(1, rest.size());
        rest.addAll(first);
        assertEquals(Set.of(new Sector(0, 0), new Sector(1, 0)), rest);
    }

    @Test
    void whatWasDeliveredSurvivesTheRoundTrip() {
        chart(TEAM, OVERWORLD, new Sector(-4, 9));
        chart(TEAM, NETHER, new Sector(0, -1));
        delivery.observe(ALICE, TEAM, OVERWORLD, charts);
        drain(ALICE);
        delivery.observe(ALICE, TEAM, NETHER, charts);
        drain(ALICE);

        JsonElement written = ChartDelivery.CODEC.encodeStart(JsonOps.INSTANCE, delivery).getOrThrow();
        ChartDelivery read = ChartDelivery.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow();

        read.observe(ALICE, TEAM, OVERWORLD, charts);
        assertEquals(List.of(), read.take(ALICE, ALL));
        read.observe(ALICE, TEAM, NETHER, charts);
        assertEquals(List.of(), read.take(ALICE, ALL));
    }
}
