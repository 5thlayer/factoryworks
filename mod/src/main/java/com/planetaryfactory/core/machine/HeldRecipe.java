package com.planetaryfactory.core.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;

/**
 * The one {@code planetaryfactory:assembling} recipe an Assembling Machine is told to make (#327,
 * ADR-0071), or none.
 *
 * <p>An <b>id</b>, not a recipe: ids are stable (ADR-0063) and are what research unlocks key on.
 * It is resolved against the recipe manager when something asks, never on load -- the block
 * entity's load hook can run before the manager is populated, and a recipe resolved there would
 * be resolved against nothing.
 *
 * <p>Minecraft-free, like {@code AssemblerCodecs}, so the round trip is a unit test: a codec that
 * drops the field does not crash, it silently empties every machine over a reload.
 */
public record HeldRecipe(Optional<String> id) {

    public static final HeldRecipe NONE = new HeldRecipe(Optional.empty());

    public static final Codec<HeldRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.optionalFieldOf("recipe").forGetter(HeldRecipe::id))
            .apply(instance, HeldRecipe::new));

    public HeldRecipe {
        Objects.requireNonNull(id, "id");
    }

    public static HeldRecipe of(String id) {
        return new HeldRecipe(Optional.of(id));
    }

    public boolean isSet() {
        return id.isPresent();
    }

    /** Whether setting {@code next} is a change -- the moment ingredients go back to the player. */
    public boolean changesTo(HeldRecipe next) {
        return !equals(next);
    }
}
