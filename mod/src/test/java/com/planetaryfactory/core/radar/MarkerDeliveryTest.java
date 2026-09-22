package com.planetaryfactory.core.radar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Which patch markers each player's map is sent, and when (#370, ADR-0079). */
class MarkerDeliveryTest {

    private static final UUID TEAM = UUID.fromString("6f1b1e5e-0000-4000-8000-00000000abcd");
    private static final UUID OTHER_TEAM = UUID.fromString("6f1b1e5e-0000-4000-8000-00000000dcba");
    private static final UUID ALICE = UUID.fromString("6f1b1e5e-0000-4000-8000-0000000000a1");
    private static final UUID BOB = UUID.fromString("6f1b1e5e-0000-4000-8000-0000000000b0");
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String NETHER = "minecraft:the_nether";

    private static final Sector HOME = new Sector(0, 0);
    private static final Sector EAST = new Sector(1, 0);
    private static final PatchMarker IRON = new PatchMarker("iron", 8, 70, 8);
    private static final PatchMarker COAL = new PatchMarker("coal", 40, 70, 8);
    private static final PatchMarker COPPER = new PatchMarker("copper", 50, 70, 20);

    private final RadarCharts charts = new RadarCharts();
    private final SectorPatches patches = new SectorPatches();
    private final WalkedPatches walked = new WalkedPatches();
    private final MarkerDelivery delivery = new MarkerDelivery();

    /** What {@code RadarChartData.chart} does with a sector and the patches found in it. */
    private void chart(UUID team, String dimension, Sector sector, PatchMarker... found) {
        if (charts.chart(team, dimension, sector)) {
            patches.record(dimension, sector, List.of(found));
            delivery.charted(team, dimension, List.of(found));
        }
    }

    /** What {@code RadarChartData.walked} does with the patches of a chunk sent to a player. */
    private void walk(UUID player, String dimension, PatchMarker... found) {
        delivery.found(player, dimension, walked.add(player, dimension, List.of(found)));
    }

    private void observe(UUID player, UUID team, String dimension) {
        delivery.observe(player, team, dimension, charts, patches, walked);
    }

    private Set<PatchMarker> drain(UUID player) {
        return new HashSet<>(delivery.take(player));
    }

    @Test
    void aChartedSectorsPatchesReachEachMemberOnce() {
        observe(ALICE, TEAM, OVERWORLD);
        observe(BOB, TEAM, OVERWORLD);

        chart(TEAM, OVERWORLD, HOME, IRON);
        chart(TEAM, OVERWORLD, HOME, IRON);

        assertEquals(Set.of(IRON), drain(ALICE));
        assertEquals(Set.of(IRON), drain(BOB));
        chart(TEAM, OVERWORLD, EAST, COAL);
        assertEquals(Set.of(COAL), drain(ALICE));
    }

    @Test
    void anotherTeamsChartSendsNothing() {
        observe(ALICE, TEAM, OVERWORLD);

        chart(OTHER_TEAM, OVERWORLD, HOME, IRON);

        assertEquals(Set.of(), drain(ALICE));
    }

    @Test
    void aSectorAnotherTeamChartedFirstStillMarksItsPatches() {
        chart(OTHER_TEAM, OVERWORLD, HOME, IRON);
        observe(ALICE, TEAM, OVERWORLD);

        chart(TEAM, OVERWORLD, HOME, IRON);

        assertEquals(Set.of(IRON), drain(ALICE));
    }

    @Test
    void aLoginReceivesExactlyTheMarkersThePlayerLacks() {
        chart(TEAM, OVERWORLD, HOME, IRON);
        chart(TEAM, OVERWORLD, EAST, COAL, COPPER);
        observe(ALICE, TEAM, OVERWORLD);
        assertEquals(Set.of(IRON, COAL, COPPER), drain(ALICE));

        observe(ALICE, TEAM, NETHER);
        observe(ALICE, TEAM, OVERWORLD);
        assertEquals(Set.of(), drain(ALICE), "a round trip through another dimension lacks nothing");

        // The map's markers are the client's memory, gone at logout (ADR-0079).
        delivery.logout(ALICE);
        observe(ALICE, TEAM, OVERWORLD);
        assertEquals(Set.of(IRON, COAL, COPPER), drain(ALICE));
    }

    @Test
    void aPlayerElsewhereReceivesTheMarkersOnArrival() {
        observe(ALICE, TEAM, NETHER);
        chart(TEAM, OVERWORLD, HOME, IRON);
        assertEquals(Set.of(), drain(ALICE));

        observe(ALICE, TEAM, OVERWORLD);

        assertEquals(Set.of(IRON), drain(ALICE));
    }

    @Test
    void joiningATeamSendsItsMarkers() {
        chart(TEAM, OVERWORLD, HOME, IRON);
        observe(BOB, BOB, OVERWORLD);
        assertEquals(Set.of(), drain(BOB));

        observe(BOB, TEAM, OVERWORLD);

        assertEquals(Set.of(IRON), drain(BOB));
    }

    @Test
    void nobodyOnlineIsSentNothing() {
        chart(TEAM, OVERWORLD, HOME, IRON);

        assertEquals(List.of(), delivery.take(ALICE));
    }

    @Test
    void walkingPastAPatchMarksItForThatPlayerOnly() {
        observe(ALICE, TEAM, OVERWORLD);
        observe(BOB, TEAM, OVERWORLD);

        walk(ALICE, OVERWORLD, IRON);

        assertEquals(Set.of(IRON), drain(ALICE));
        assertEquals(Set.of(), drain(BOB), "walked terrain is the walker's own map, not the team's chart");
    }

    @Test
    void aWalkedPatchIsSentAgainAfterALogin() {
        observe(ALICE, TEAM, OVERWORLD);
        walk(ALICE, OVERWORLD, IRON);
        drain(ALICE);
        walk(ALICE, OVERWORLD, IRON);
        assertEquals(Set.of(), drain(ALICE), "the chunk is sent again on each approach");

        delivery.logout(ALICE);
        observe(ALICE, TEAM, OVERWORLD);

        assertEquals(Set.of(IRON), drain(ALICE));
    }

    @Test
    void aPatchBothWalkedAndChartedIsSentOnce() {
        observe(ALICE, TEAM, OVERWORLD);
        walk(ALICE, OVERWORLD, IRON);
        chart(TEAM, OVERWORLD, HOME, IRON);

        assertEquals(List.of(IRON), delivery.take(ALICE));
    }

    @Test
    void aPatchWalkedInAnotherDimensionWaitsForTheReturn() {
        observe(ALICE, TEAM, NETHER);
        walk(ALICE, OVERWORLD, IRON);
        assertEquals(Set.of(), drain(ALICE));

        observe(ALICE, TEAM, OVERWORLD);

        assertEquals(Set.of(IRON), drain(ALICE));
    }
}
