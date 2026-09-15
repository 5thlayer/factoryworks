package com.planetaryfactory.core.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** Issue #76's acceptance criteria for the retry memory, as assertions. */
class LockedRecipeRetryTest {

    /** Stands in for a machine recipe: an id, and an object identity distinct from it. */
    private record Recipe(String id) {}

    private static final Function<Recipe, String> ID = Recipe::id;

    private static final Recipe FLUXGATE = new Recipe("oritech:assembler/fluxgate");
    private static final Recipe BATTERY = new Recipe("oritech:assembler/battery");

    @Test
    void remembersARefusedRecipeSoTheMachineKeepsTicking() {
        List<Recipe> remembered = LockedRecipeRetry.remember(null, FLUXGATE, ID);

        assertEquals(List.of(FLUXGATE), remembered, "an empty memory is what unsubscribes the machine");
    }

    @Test
    void keepsWhatWasAlreadyRemembered() {
        List<Recipe> first = LockedRecipeRetry.remember(null, FLUXGATE, ID);

        assertEquals(List.of(FLUXGATE, BATTERY), LockedRecipeRetry.remember(first, BATTERY, ID));
    }

    @Test
    void remembersARecipeOnlyOnce() {
        List<Recipe> once = LockedRecipeRetry.remember(null, FLUXGATE, ID);

        assertEquals(List.of(FLUXGATE), LockedRecipeRetry.remember(once, FLUXGATE, ID));
    }

    /**
     * The re-check path runs the refused recipe through {@code fullModifyRecipe} first, so what
     * comes back each tick is a new object carrying the same id. Identity dedup would grow the list
     * for as long as the machine waits for its research.
     */
    @Test
    void treatsAModifiedCopyAsTheSameRecipe() {
        Recipe modified = new Recipe(FLUXGATE.id());
        assertNotSame(FLUXGATE, modified);

        List<Recipe> remembered =
                LockedRecipeRetry.remember(LockedRecipeRetry.remember(null, FLUXGATE, ID), modified, ID);

        assertEquals(1, remembered.size(), "one waiting recipe is one entry, however many ticks pass");
    }

    /**
     * Oritech iterates this list on the same tick that the pack writes to it -- the iteration calls
     * back into the refusal. Mutating in place would throw on the machine's own tick.
     */
    @Test
    void leavesTheListItWasGivenUntouched() {
        List<Recipe> held = LockedRecipeRetry.remember(null, FLUXGATE, ID);

        List<Recipe> next = LockedRecipeRetry.remember(held, BATTERY, ID);

        assertNotSame(held, next);
        assertEquals(List.of(FLUXGATE), held, "an in-flight iteration must see what it started with");
    }

    /** A no-op remember still hands back a copy, so the same guarantee holds on the dedup path. */
    @Test
    void copiesEvenWhenNothingIsAdded() {
        List<Recipe> held = LockedRecipeRetry.remember(null, FLUXGATE, ID);

        assertNotSame(held, LockedRecipeRetry.remember(held, FLUXGATE, ID));
    }

    /** {@code handleSearchingRecipes} adds to this field itself, so what is left there must take it. */
    @Test
    void handsBackAListOritechCanAddTo() {
        List<Recipe> remembered = LockedRecipeRetry.remember(null, FLUXGATE, ID);

        remembered.add(BATTERY);

        assertTrue(remembered.contains(BATTERY));
    }

    @Test
    void takesOverAListOritechBuilt() {
        List<Recipe> gregtechs = new ArrayList<>(List.of(BATTERY));

        List<Recipe> remembered = LockedRecipeRetry.remember(gregtechs, FLUXGATE, ID);

        assertEquals(List.of(BATTERY, FLUXGATE), remembered);
        assertSame(BATTERY, remembered.get(0));
    }
}
