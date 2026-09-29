package com.factoryworks.core.radar;

import com.mojang.serialization.Codec;
import com.factoryworks.core.oil.OilField;
import com.factoryworks.core.oil.OilFieldSource;
import com.factoryworks.core.ore.OutfieldDisc;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The outfield patches and oil fields of every sector any team has charted, per dimension (#370,
 * ADR-0079, ADR-0081). A patch belongs to the sector holding its centre, so it is marked once however
 * many sectors its disc reaches into.
 */
public final class SectorPatches {

    /** An oil field's marker resource: its total is the wells' summed amount, and nothing retires it. */
    public static final String CRUDE_OIL = "crude_oil";

    /** The height a marker stands at, read when the sector is charted and its chunks are loaded. */
    @FunctionalInterface
    public interface Surface {
        int at(int x, int z);
    }

    private static final Codec<Map<String, Map<String, List<PatchMarker>>>> ENCODED =
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, PatchMarker.CODEC.listOf()));

    public static final Codec<SectorPatches> CODEC = ENCODED.xmap(SectorPatches::decode, SectorPatches::encode);

    private final Map<String, Map<Long, List<PatchMarker>>> patches = new HashMap<>();

    /**
     * The markers of the outfield discs among {@code pieces} centred in {@code sector}. Only an
     * outfield disc is marked: a starting field is dealt beside spawn and needs no Radar.
     */
    public static List<PatchMarker> find(Sector sector, Collection<?> pieces, Surface surface) {
        return find(sector, pieces, surface, (x, z) -> false);
    }

    /** As above, and each oil field centred in {@code sector}, less its wells {@code ore} turned away. */
    public static List<PatchMarker> find(Sector sector, Collection<?> pieces, Surface surface, OilField.Ore ore) {
        List<PatchMarker> found = new ArrayList<>();
        for (Object piece : pieces) {
            if (piece instanceof OilFieldSource field
                    && Sector.ofBlock(field.centreX(), field.centreZ()).equals(sector)) {
                found.add(new PatchMarker(CRUDE_OIL, field.centreX(), surface.at(field.centreX(), field.centreZ()),
                        field.centreZ(), OilField.total(OilField.placeable(field.wells(), ore))));
            }
            if (piece instanceof OutfieldDisc.Source source) {
                OutfieldDisc disc = source.disc();
                if (Sector.ofBlock(disc.centreX(), disc.centreZ()).equals(sector)) {
                    found.add(new PatchMarker(disc.resource().key(), disc.centreX(),
                            surface.at(disc.centreX(), disc.centreZ()), disc.centreZ(), disc.total()));
                }
            }
        }
        return found;
    }

    public void record(String dimension, Sector sector, List<PatchMarker> markers) {
        if (markers.isEmpty()) {
            return;
        }
        patches.computeIfAbsent(dimension, d -> new HashMap<>()).put(sector.pack(), List.copyOf(markers));
    }

    public List<PatchMarker> in(String dimension, Sector sector) {
        return patches.getOrDefault(dimension, Map.of()).getOrDefault(sector.pack(), List.of());
    }

    public Set<PatchMarker> in(String dimension, Collection<Sector> sectors) {
        Set<PatchMarker> markers = new HashSet<>();
        for (Sector sector : sectors) {
            markers.addAll(in(dimension, sector));
        }
        return markers;
    }

    // The codec's map keys must be strings, so a sector's packed long is written as its decimal.
    private static SectorPatches decode(Map<String, Map<String, List<PatchMarker>>> encoded) {
        SectorPatches decoded = new SectorPatches();
        encoded.forEach((dimension, sectors) -> sectors.forEach((sector, markers) ->
                decoded.patches.computeIfAbsent(dimension, d -> new HashMap<>())
                        .put(Long.parseLong(sector), List.copyOf(markers))));
        return decoded;
    }

    private Map<String, Map<String, List<PatchMarker>>> encode() {
        Map<String, Map<String, List<PatchMarker>>> encoded = new HashMap<>();
        patches.forEach((dimension, sectors) -> {
            Map<String, List<PatchMarker>> bySector = new HashMap<>();
            sectors.forEach((sector, markers) -> bySector.put(Long.toString(sector), markers));
            encoded.put(dimension, bySector);
        });
        return encoded;
    }
}
