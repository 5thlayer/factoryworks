package com.planetaryfactory.core.smelting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One row of the generated fuel table (ADR-0047, #187): what burns, for how many joules, and in
 * which of Factorio's fuel categories.
 *
 * <p>{@code target} is either an item id or a tag id, told apart by {@code tag} -- the lookup is
 * item-then-tag rather than a flat item map, because {@code wood} maps onto {@code minecraft:logs}
 * and every log is Factorio's wood.
 *
 * <p>{@code factorioName} is carried for diagnosis only; nothing routes on it.
 *
 * <p>Pure: no Minecraft types, so ids are strings and the table is testable without a game --
 * {@link #CODEC} included, which is why the codec lives here rather than on the reload listener
 * that uses it.
 */
public record FuelRow(String factorioName, String target, boolean tag, long fuelValue,
        String fuelCategory) {

    public FuelRow {
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException(factorioName + " has no target");
        }
        if (fuelValue <= 0L) {
            throw new IllegalArgumentException(factorioName + " has no fuel value");
        }
        if (fuelCategory == null || fuelCategory.isEmpty()) {
            throw new IllegalArgumentException(factorioName + " has no fuel category");
        }
    }

    /**
     * One generated file's shape.
     *
     * <p>26.1's reload listener parses with a codec rather than handing over raw Gson, so the field
     * names {@code scripts/factorio-fuel-convert.py} writes are stated once, here, instead of as
     * six GsonHelper calls.
     *
     * <p>{@code item} and {@code tag} are two keys for one field: exactly one is present, and which
     * one it is <em>is</em> the {@code tag} flag. That is the converter's shape rather than a
     * choice made here -- a row is a pointer at an item or at a tag, never at both -- so the
     * asymmetry is encoded rather than smoothed over, and the round trip is what holds it.
     */
    public static final Codec<FuelRow> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("factorio_name").forGetter(FuelRow::factorioName),
            Codec.STRING.optionalFieldOf("item", "").forGetter(row -> row.tag() ? "" : row.target()),
            Codec.STRING.optionalFieldOf("tag", "").forGetter(row -> row.tag() ? row.target() : ""),
            Codec.LONG.fieldOf("fuel_value").forGetter(FuelRow::fuelValue),
            Codec.STRING.fieldOf("fuel_category").forGetter(FuelRow::fuelCategory)
    ).apply(instance, (name, item, tag, value, category) ->
            new FuelRow(name, tag.isEmpty() ? item : tag, !tag.isEmpty(), value, category)));
}
