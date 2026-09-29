package com.factoryworks.core.machine;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

/**
 * Which Held-recipe ingredient each input slot takes: the {@code n}th ingredient goes in the
 * {@code n}th slot, and an unused slot takes nothing (ADR-0073). The input filter and the slot
 * ghosts both read this, so they cannot disagree. Generic so it tests without Minecraft.
 */
public final class AssemblingInputSlots {

    public static final int INPUTS = AssemblingTier.ONE.spec().itemInputs();

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

    /** Whether a slot holding {@code held} items cannot cover one craft: the screen draws it red. */
    public static <I> boolean isShort(int slot, List<I> ingredients, int held, ToIntFunction<I> count) {
        return ingredientFor(slot, ingredients).filter(ingredient -> held < count.applyAsInt(ingredient)).isPresent();
    }
}
