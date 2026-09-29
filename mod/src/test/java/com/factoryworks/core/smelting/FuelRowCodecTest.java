package com.factoryworks.core.smelting;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The generated fuel file's codec (ADR-0047).
 *
 * <p>What a burner burns is datapack JSON written by {@code scripts/factorio-fuel-convert.py}, and
 * this codec is the only thing that reads it. A codec that drops a field does not crash -- it
 * returns a row that quietly lost its target, and a default-deny table then refuses an item the
 * converter decided was fuel, with no error anywhere. That is the failure
 * {@code docs/testing/what-to-check.md} asks a round trip to catch, and the reason the codec sits
 * on {@link FuelRow} rather than on the listener: this source set has no Minecraft on it.
 *
 * <p>The asymmetric half is what earns the test. {@code item} and {@code tag} are two JSON keys
 * standing for one field, chosen by the {@code tag} flag, so encode and decode are written
 * separately and are the two directions most able to disagree.
 */
class FuelRowCodecTest {

    private static FuelRow roundTrip(FuelRow row) {
        JsonElement json = FuelRow.CODEC.encodeStart(JsonOps.INSTANCE, row).getOrThrow();
        return FuelRow.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    @Test
    void anItemRowSurvivesTheRoundTrip() {
        FuelRow coal = new FuelRow("coal", "minecraft:coal", false, 4_000_000L, "chemical");

        assertEquals(coal, roundTrip(coal));
    }

    @Test
    void aTagRowSurvivesTheRoundTrip() {
        // `wood` is the row the tag side exists for: Factorio's wood is every log.
        FuelRow wood = new FuelRow("wood", "minecraft:logs", true, 2_000_000L, "chemical");

        assertEquals(wood, roundTrip(wood));
    }

    @Test
    void anItemRowWritesItemAndNotTag() {
        JsonElement json = FuelRow.CODEC
                .encodeStart(JsonOps.INSTANCE, new FuelRow("coal", "minecraft:coal", false, 1L, "chemical"))
                .getOrThrow();

        assertEquals("minecraft:coal", json.getAsJsonObject().get("item").getAsString());
        assertTrue(json.getAsJsonObject().get("tag") == null
                || json.getAsJsonObject().get("tag").getAsString().isEmpty());
    }

    @Test
    void aTagRowWritesTagAndNotItem() {
        JsonElement json = FuelRow.CODEC
                .encodeStart(JsonOps.INSTANCE, new FuelRow("wood", "minecraft:logs", true, 1L, "chemical"))
                .getOrThrow();

        assertEquals("minecraft:logs", json.getAsJsonObject().get("tag").getAsString());
        assertTrue(json.getAsJsonObject().get("item") == null
                || json.getAsJsonObject().get("item").getAsString().isEmpty());
    }

    /** The converter's own output, read as the game reads it. */
    @Test
    void theConvertersJsonParses() {
        String json = """
                {
                  "factorio_name": "coal",
                  "item": "minecraft:coal",
                  "fuel_value": 4000000,
                  "fuel_category": "chemical"
                }""";

        FuelRow row = FuelRow.CODEC
                .parse(JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(json))
                .getOrThrow();

        assertEquals("minecraft:coal", row.target());
        assertFalse(row.tag(), "no tag key means the target is an item");
        assertEquals(4_000_000L, row.fuelValue());
    }

    /**
     * A row naming neither an item nor a tag does not load pointing at nothing.
     *
     * <p>It throws rather than returning a {@code DataResult} error, because the guard is
     * {@link FuelRow}'s compact constructor and DataFixerUpper calls it directly. Stated as the
     * throw it is: a reader who expects an error result would otherwise write a listener that
     * silently skips the file, and this refusal is meant to be loud.
     */
    @Test
    void aRowWithNoTargetIsRefused() {
        String json = """
                {"factorio_name": "ghost", "fuel_value": 1, "fuel_category": "chemical"}""";

        assertThrows(IllegalArgumentException.class, () -> FuelRow.CODEC
                .parse(JsonOps.INSTANCE, com.google.gson.JsonParser.parseString(json))
                .getOrThrow());
    }
}
