package com.factoryworks.core.radar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.factoryworks.core.ore.PatchId;

/**
 * An outfield patch's marker on the map: its resource, its centre, and the units its blocks held
 * when placed (#370, ADR-0079). What is left is the ledger's, and is sent beside it.
 */
public record PatchMarker(String resource, int x, int y, int z, long total) {

    public static final Codec<PatchMarker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("resource").forGetter(PatchMarker::resource),
            Codec.INT.fieldOf("x").forGetter(PatchMarker::x),
            Codec.INT.fieldOf("y").forGetter(PatchMarker::y),
            Codec.INT.fieldOf("z").forGetter(PatchMarker::z),
            Codec.LONG.optionalFieldOf("total", 0L).forGetter(PatchMarker::total))
            .apply(instance, PatchMarker::new));

    public PatchId id(String dimension) {
        return new PatchId(dimension, resource, x, z);
    }
}
