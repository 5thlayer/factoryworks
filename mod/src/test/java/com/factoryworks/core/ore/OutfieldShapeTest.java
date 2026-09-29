package com.factoryworks.core.ore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** An outfield disc's footprint: the spot's cone plus Factorio's ragged edge (ADR-0045, #320). */
class OutfieldShapeTest {

    private static final OutfieldLaw IRON = OutfieldLaw.of(OreResource.IRON);
    private static final OutfieldShape.Noise FLAT = (x, z) -> 0;
    private static final OutfieldShape.Land LAND = (x, z) -> true;

    @Test
    void theLawsRadiusHeightAndAmplitudeReproduceTheCorpusTable() throws IOException {
        JsonObject corpus = new Gson().fromJson(
                Files.readString(Path.of(System.getProperty("pf.repo"), "data/factorio/resource.json")),
                JsonObject.class);
        int rows = 0;
        for (OreResource resource : OreResource.values()) {
            OutfieldLaw law = OutfieldLaw.of(resource);
            double mean = resource.corpus().outfield().meanSpotSize();
            for (JsonElement element : entry(corpus, resource).getAsJsonObject("outfield").getAsJsonArray("law")) {
                JsonObject row = element.getAsJsonObject();
                double distance = row.get("distance").getAsDouble();
                String where = resource.key() + " at " + distance;
                assertEquals(row.get("spot_radius").getAsDouble(), law.radius(mean, distance), 1e-9, where);
                assertEquals(row.get("spot_height").getAsDouble(), law.typicalHeight(distance), 1e-6, where);
                assertEquals(row.get("blob_amplitude").getAsDouble(), law.blobAmplitude(distance), 1e-6, where);
                rows++;
            }
        }
        assertTrue(rows >= 5 * 8, "every resource's table was read: " + rows);
    }

    @Test
    void theRadiusIsCappedAt32() {
        assertEquals(32, IRON.radius(100, 1600), 1e-9);
    }

    @Test
    void theSizeFactorIsDrawnUniformlyBetweenTheSpotSizeBounds() {
        assertEquals(0.25, IRON.sizeFactor(0), 1e-9);
        assertEquals(2.0, IRON.sizeFactor(1), 1e-9);
        assertEquals(1.125, IRON.sizeFactor(0.5), 1e-9);
    }

    @Test
    void aFlatEdgeIsTheConeLessTheOffsetTimesTheAmplitude() {
        double distance = 3000;
        OutfieldShape shape = OutfieldShape.of(IRON, 1.0, 3000, 0, FLAT, LAND);
        double radius = IRON.radius(1.0, distance);
        double height = IRON.height(1.0, distance);
        // cone h(1 - r/R) meets (0 - 1/3) × amplitude
        double edge = radius * (1 - IRON.blobAmplitude(distance) / 3 / height);

        assertFalse(shape.isEmpty());
        assertTrue(shape.contains(3000, 0));
        for (int dx = -40; dx <= 40; dx++) {
            for (int dz = -40; dz <= 40; dz++) {
                double r = Math.sqrt(dx * dx + dz * dz);
                if (Math.abs(r - edge) > 1e-6) {
                    assertEquals(r < edge, shape.contains(3000 + dx, dz), "at " + dx + ", " + dz);
                }
            }
        }
    }

    @Test
    void theBlockCountIsTheColumnsTheShapeContains() {
        OutfieldShape shape = OutfieldShape.of(IRON, 1.0, 3000, 0, FLAT, LAND);
        int counted = 0;
        for (int x = 3000 - shape.reach(); x <= 3000 + shape.reach(); x++) {
            for (int z = -shape.reach(); z <= shape.reach(); z++) {
                counted += shape.contains(x, z) ? 1 : 0;
            }
        }
        assertEquals(counted, shape.blockCount());
        assertEquals(Math.PI * Math.pow(shape.radius() * (1 - IRON.blobAmplitude(3000) / 3 / IRON.height(1.0, 3000)), 2),
                counted, counted * 0.05);
    }

    @Test
    void theEdgeFollowsTheOctavesWeightsAndScales() {
        // Each octave sampled at its own scale: a noise that is 1 only where it is read at the
        // coarsest scale's coordinates pushes the edge out by that octave's weight alone.
        OreCorpus.Octave coarse = OreCorpus.get().edge().octaves().getLast();
        OutfieldShape.Noise coarseOnly = (x, z) -> Math.abs(x - 3000 * coarse.inputScale()) < 1 ? 1 : 0;
        OutfieldShape flat = OutfieldShape.of(IRON, 1.0, 3000, 0, FLAT, LAND);
        OutfieldShape raised = OutfieldShape.of(IRON, 1.0, 3000, 0, coarseOnly, LAND);

        double distance = 3000;
        double pushed = IRON.radius(1.0, distance) * (1 + (coarse.outputScale() - 1.0 / 3)
                * IRON.blobAmplitude(distance) / IRON.height(1.0, distance));
        int onAxis = (int) Math.floor(pushed - 1e-6);
        assertTrue(raised.contains(3000, onAxis), "the coarse octave's weight moves the edge to " + pushed);
        assertFalse(flat.contains(3000, onAxis));
        assertTrue(raised.blockCount() > flat.blockCount());
    }

    @Test
    void noColumnOffTheLandHoldsOre() {
        OutfieldShape everywhere = OutfieldShape.of(IRON, 1.0, 3000, 0, FLAT, LAND);
        OutfieldShape westOnly = OutfieldShape.of(IRON, 1.0, 3000, 0, FLAT, (x, z) -> x < 3000);

        assertTrue(everywhere.contains(3005, 0));
        assertFalse(westOnly.contains(3005, 0));
        assertTrue(westOnly.contains(2995, 0));
        assertTrue(westOnly.blockCount() < everywhere.blockCount());
    }

    @Test
    void theSavedMaskReadsBackTheSameColumns() {
        OutfieldShape.Noise ragged = (x, z) -> Math.sin(x * 7) * Math.cos(z * 5);
        OutfieldShape shape = OutfieldShape.of(IRON, 1.0, 3000, 0, ragged, LAND);
        OutfieldShape read = OutfieldShape.of(3000, 0, shape.reach(), shape.mask());

        assertEquals(shape.blockCount(), read.blockCount());
        for (int x = 3000 - shape.reach() - 1; x <= 3000 + shape.reach() + 1; x++) {
            for (int z = -shape.reach() - 1; z <= shape.reach() + 1; z++) {
                assertEquals(shape.contains(x, z), read.contains(x, z), "at " + x + ", " + z);
            }
        }
    }

    @Test
    void nothingGeneratesInsideThePlacementRadius() {
        for (double factor : new double[] {0.25, 1, 2}) {
            assertTrue(OutfieldShape.of(IRON, factor, 150, 0, FLAT, LAND).isEmpty());
            assertTrue(OutfieldShape.of(IRON, factor, 0, 0, FLAT, LAND).isEmpty());
            assertTrue(OutfieldShape.of(IRON, factor, 100, 100, FLAT, LAND).isEmpty());
        }
    }

    @Test
    void aDiscIsSmallUntil450() {
        assertTrue(OutfieldShape.of(IRON, 0.25, 150, 1, FLAT, LAND).isEmpty(),
                "a radius that rounds below one block does not generate");
        assertTrue(IRON.radius(1, 300) < IRON.radius(1, 450));
        assertTrue(IRON.radius(1, 450) < IRON.radius(1, 1600));
        assertEquals(IRON.radius(1, 1600), IRON.radius(1, 5000), 1e-9);
    }

    private static JsonObject entry(JsonObject corpus, OreResource resource) {
        for (JsonElement element : corpus.getAsJsonArray("resources")) {
            if (element.getAsJsonObject().get("name").getAsString().equals(resource.corpus().factorioName())) {
                return element.getAsJsonObject();
            }
        }
        throw new AssertionError(resource + " is not in the corpus");
    }
}
