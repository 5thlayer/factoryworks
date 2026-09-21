package com.planetaryfactory.core.machine;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * One entry in the Assembling Machine's recipe widget (#327): a recipe id and whether the viewing
 * team is locked out of it.
 *
 * <p>A locked recipe is listed and marked, never left out -- the pack's Lock annotation policy.
 * Whether it crafts is the craft cycle's question, not the widget's.
 */
public record RecipeChoice(String id, boolean locked) {

    /** Every id, in id order so the widget reads the same on every opening. */
    public static List<RecipeChoice> of(List<String> ids, Predicate<String> locked) {
        return ids.stream()
                .sorted(Comparator.naturalOrder())
                .map(id -> new RecipeChoice(id, locked.test(id)))
                .toList();
    }
}
