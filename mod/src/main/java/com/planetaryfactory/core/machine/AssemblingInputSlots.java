package com.planetaryfactory.core.machine;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Which Held-recipe ingredient each input slot takes: the {@code n}th ingredient goes in the
 * {@code n}th slot, and an unused slot takes nothing (ADR-0073). The input filter and the slot
 * ghosts (#334) must both read this, so they cannot disagree. Generic so it tests without Minecraft.
 */
public final class AssemblingInputSlots {

    /** {@code test_recipe_convert.py} reads this and fails a recipe with more ingredients. */
    public static final int INPUTS = 4;

    private AssemblingInputSlots() {
    }

    public static <I> Optional<I> ingredientFor(int slot, List<I> ingredients) {
        if (slot < 0 || slot >= INPUTS || slot >= ingredients.size()) {
            return Optional.empty();
        }
        return Optional.of(ingredients.get(slot));
    }

    public static <I, T> boolean accepts(int slot, List<I> ingredients, T item, BiPredicate<I, T> matches) {
        return ingredientFor(slot, ingredients).filter(ingredient -> matches.test(ingredient, item)).isPresent();
    }
}
