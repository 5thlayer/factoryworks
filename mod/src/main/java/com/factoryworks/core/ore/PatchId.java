package com.factoryworks.core.ore;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** An outfield patch: its dimension, its resource and its centre column (#370). */
public record PatchId(String dimension, String resource, int x, int z) {

    public static final Codec<PatchId> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("dimension").forGetter(PatchId::dimension),
            Codec.STRING.fieldOf("resource").forGetter(PatchId::resource),
            Codec.INT.fieldOf("x").forGetter(PatchId::x),
            Codec.INT.fieldOf("z").forGetter(PatchId::z))
            .apply(instance, PatchId::new));

    public static PatchId of(String dimension, OutfieldDisc disc) {
        return new PatchId(dimension, disc.resource().key(), disc.centreX(), disc.centreZ());
    }
}
