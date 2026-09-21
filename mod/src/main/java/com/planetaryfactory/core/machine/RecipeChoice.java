package com.planetaryfactory.core.machine;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * One recipe an Assembling Machine may hold (#327): a recipe id and whether the viewing team is
 * locked out of it. The screen names the Held recipe from these; it no longer lists them (#336).
 *
 * <p>A locked recipe is kept and marked, never left out -- the pack's Lock annotation policy.
 * Whether it crafts is the craft cycle's question, not this record's.
 */
public record RecipeChoice(String id, boolean locked) {

    /** Every id, in id order so every opening sends the same order. */
    public static List<RecipeChoice> of(List<String> ids, Predicate<String> locked) {
        return ids.stream()
                .sorted(Comparator.naturalOrder())
                .map(id -> new RecipeChoice(id, locked.test(id)))
                .toList();
    }
}
