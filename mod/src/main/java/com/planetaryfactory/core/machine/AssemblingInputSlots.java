package com.planetaryfactory.core.machine;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Which of the Held recipe's ingredients each input slot is for (#329, ADR-0073).
 *
 * <p>The {@code n}th ingredient is the {@code n}th slot's, and a slot past the last ingredient is for
 * nothing -- Modern Industrialization's lock-empty rule, which is what stops a belt filling a
 * two-ingredient machine's spare slots with an item it cannot use. No Held recipe is no ingredients,
 * so a machine that makes nothing takes nothing.
 *
 * <p>One rule with two readers: the input face filters by it, and #334's ghosts are drawn from it,
 * so the slot that accepts an item and the slot that shows it cannot disagree. Generic over the
 * ingredient and the item, so it is a unit test without Minecraft.
 */
public final class AssemblingInputSlots {

    /** The four input slots, ADR-0071's four. No emitted assembling recipe has more ingredients. */
    public static final int INPUTS = 4;

    private AssemblingInputSlots() {
    }

    /** The ingredient {@code slot} is for, or empty when the recipe does not use it. */
    public static <I> Optional<I> ingredientFor(int slot, List<I> ingredients) {
        if (slot < 0 || slot >= INPUTS || slot >= ingredients.size()) {
            return Optional.empty();
        }
        return Optional.of(ingredients.get(slot));
    }

    /** Whether {@code slot} takes {@code item}: only when it is the slot's own ingredient. */
    public static <I, T> boolean accepts(int slot, List<I> ingredients, T item, BiPredicate<I, T> matches) {
        return ingredientFor(slot, ingredients).filter(ingredient -> matches.test(ingredient, item)).isPresent();
    }
}
