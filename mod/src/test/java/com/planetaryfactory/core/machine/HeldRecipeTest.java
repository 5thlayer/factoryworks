package com.planetaryfactory.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The Held recipe's round trip (#327).
 *
 * <p>A codec that drops the field does not crash: it decodes to "holds nothing", and every machine
 * in a world silently forgets its recipe over a reload. So the round trip is asserted on the value,
 * not on the absence of an error. {@code JsonOps} stands in for NBT; a string field is carried
 * identically by both.
 */
class HeldRecipeTest {

    private static HeldRecipe roundTrip(HeldRecipe held) {
        JsonElement encoded = HeldRecipe.CODEC.encodeStart(JsonOps.INSTANCE, held).getOrThrow();
        return HeldRecipe.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
    }

    @Test
    void aHeldRecipeSurvivesTheRoundTrip() {
        HeldRecipe held = HeldRecipe.of("planetaryfactory:assembling/iron-gear-wheel");

        HeldRecipe back = roundTrip(held);

        assertEquals(held, back);
        assertEquals(Optional.of("planetaryfactory:assembling/iron-gear-wheel"), back.id());
    }

    @Test
    void holdingNothingSurvivesTheRoundTrip() {
        assertEquals(HeldRecipe.NONE, roundTrip(HeldRecipe.NONE));
    }

    /** A machine saved before the field existed loads holding nothing, rather than failing. */
    @Test
    void aSaveWithoutTheFieldLoadsHoldingNothing() {
        assertEquals(HeldRecipe.NONE,
                HeldRecipe.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow());
    }

    @Test
    void onlyADifferentRecipeIsAChange() {
        HeldRecipe gear = HeldRecipe.of("a:gear");

        assertFalse(gear.changesTo(HeldRecipe.of("a:gear")));
        assertTrue(gear.changesTo(HeldRecipe.of("a:belt")));
        assertTrue(gear.changesTo(HeldRecipe.NONE));
        assertTrue(HeldRecipe.NONE.changesTo(gear));
        assertFalse(HeldRecipe.NONE.changesTo(HeldRecipe.NONE));
    }

    /** Lock annotation, not hiding: a locked recipe is still a choice, and marked as one. */
    @Test
    void aLockedRecipeIsListedAndMarkedRatherThanHidden() {
        List<RecipeChoice> choices = RecipeChoice.of(
                List.of("a:gear", "a:circuit"), id -> id.equals("a:circuit"));

        assertEquals(List.of(new RecipeChoice("a:circuit", true), new RecipeChoice("a:gear", false)),
                choices);
    }
}
