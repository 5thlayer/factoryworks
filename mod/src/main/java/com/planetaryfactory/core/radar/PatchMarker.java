package com.planetaryfactory.core.radar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** An outfield patch's marker on the map: its resource and centre, and no amount (#370, ADR-0079). */
public record PatchMarker(String resource, int x, int y, int z) {

    public static final Codec<PatchMarker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("resource").forGetter(PatchMarker::resource),
            Codec.INT.fieldOf("x").forGetter(PatchMarker::x),
            Codec.INT.fieldOf("y").forGetter(PatchMarker::y),
            Codec.INT.fieldOf("z").forGetter(PatchMarker::z))
            .apply(instance, PatchMarker::new));
}
