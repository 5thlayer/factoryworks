package com.factoryworks.core.radar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.factoryworks.core.oil.OilField;
import com.factoryworks.core.oil.OilFieldSource;
import com.factoryworks.core.ore.OreResource;
import com.factoryworks.core.ore.OutfieldDisc;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Which outfield patches a charted sector marks (#370, ADR-0079). */
class SectorPatchesTest {

    private static final String OVERWORLD = "minecraft:overworld";
    private static final SectorPatches.Surface FLAT = (x, z) -> 70;

    private static OutfieldDisc.Source disc(OreResource resource, int x, int z) {
        OutfieldDisc disc = new OutfieldDisc(resource, 1.0, 100, x, z);
        return new OutfieldDisc.Source() {
            @Override
            public OutfieldDisc disc() {
                return disc;
            }

            @Override
            public boolean covers(int cx, int cz) {
                return Math.abs(cx - x) <= 20 && Math.abs(cz - z) <= 20;
            }
        };
    }

    private static PatchMarker marker(OreResource resource, int x, int y, int z) {
        return new PatchMarker(resource.key(), x, y, z, disc(resource, x, z).disc().total());
    }

    /** A starting field is a jigsaw piece, not an outfield disc. */
    private record StartingFieldPiece(OreResource resource, int x, int z) {
    }

    @Test
    void aSectorMarksExactlyTheDiscsCentredInIt() {
        Sector sector = new Sector(10, -3);
        List<Object> pieces = List.of(
                disc(OreResource.IRON, 327, -89),
                disc(OreResource.COAL, 351, -65),
                // Its box reaches into the sector, and its centre lies in the next one east.
                disc(OreResource.COPPER, 360, -80));

        assertEquals(
                List.of(marker(OreResource.IRON, 327, 70, -89), marker(OreResource.COAL, 351, 70, -65)),
                SectorPatches.find(sector, pieces, FLAT));
    }

    @Test
    void aStartingFieldIsNotMarked() {
        List<Object> pieces = List.of(new StartingFieldPiece(OreResource.IRON, 8, 8));

        assertEquals(List.of(), SectorPatches.find(new Sector(0, 0), pieces, FLAT));
    }

    @Test
    void aMarkerStandsOnTheSurfaceAtItsCentre() {
        SectorPatches.Surface surface = (x, z) -> x == 40 && z == 40 ? 83 : 0;

        assertEquals(List.of(marker(OreResource.STONE, 40, 83, 40)),
                SectorPatches.find(new Sector(1, 1), List.of(disc(OreResource.STONE, 40, 40)), surface));
    }

    private static OilFieldSource oilField(int x, int z, List<OilField.Well> wells) {
        return new OilFieldSource() {
            @Override
            public int centreX() {
                return x;
            }

            @Override
            public int centreZ() {
                return z;
            }

            @Override
            public List<OilField.Well> wells() {
                return wells;
            }
        };
    }

    @Test
    void anOilFieldIsMarkedWithItsWellsSummed() {
        List<OilField.Well> wells = List.of(new OilField.Well(40, 40, 300_000), new OilField.Well(44, 40, 450_000));

        assertEquals(List.of(new PatchMarker(SectorPatches.CRUDE_OIL, 40, 70, 40, 750_000)),
                SectorPatches.find(new Sector(1, 1), List.of(oilField(40, 40, wells)), FLAT, (x, z) -> false));
    }

    @Test
    void anOilFieldsWellOnAnOreColumnIsNotSummed() {
        List<OilField.Well> wells = List.of(new OilField.Well(40, 40, 300_000), new OilField.Well(44, 40, 450_000));

        assertEquals(List.of(new PatchMarker(SectorPatches.CRUDE_OIL, 40, 70, 40, 300_000)),
                SectorPatches.find(new Sector(1, 1), List.of(oilField(40, 40, wells)), FLAT, (x, z) -> x == 44));
    }

    @Test
    void aTeamsMarkersAreThoseOfItsChartedSectors() {
        SectorPatches patches = new SectorPatches();
        PatchMarker iron = new PatchMarker("iron", 8, 70, 8, 300);
        PatchMarker coal = new PatchMarker("coal", 40, 70, 8, 300);
        patches.record(OVERWORLD, new Sector(0, 0), List.of(iron));
        patches.record(OVERWORLD, new Sector(1, 0), List.of(coal));
        patches.record("minecraft:the_nether", new Sector(0, 0), List.of(new PatchMarker("stone", 8, 70, 8, 300)));

        assertEquals(Set.of(iron), patches.in(OVERWORLD, Set.of(new Sector(0, 0), new Sector(5, 5))));
        assertEquals(List.of(coal), patches.in(OVERWORLD, new Sector(1, 0)));
    }

    @Test
    void theRecordSurvivesItsCodec() {
        SectorPatches patches = new SectorPatches();
        PatchMarker uranium = new PatchMarker("uranium", -3000, 64, 2100, 300);
        patches.record(OVERWORLD, Sector.ofBlock(-3000, 2100), List.of(uranium));

        JsonElement json = SectorPatches.CODEC.encodeStart(JsonOps.INSTANCE, patches).getOrThrow();
        SectorPatches back = SectorPatches.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        assertEquals(Set.of(uranium), back.in(OVERWORLD, Set.of(Sector.ofBlock(-3000, 2100))));
    }
}
