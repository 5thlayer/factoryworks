package com.planetaryfactory.core.assembler;

/**
 * Whether one {@code planetaryfactory:assembling} recipe is in the Personal Assembler's hand set.
 *
 * <p>ADR-0038 gives the Assembler no recipe type of its own: the hand set is the Assembling
 * Machine's recipes whose Factorio category is {@code crafting}, which already excludes the eleven
 * withholds (#88). The converter writes the category onto every recipe it emits (#279), so this is a
 * field test and not a second list to keep in step.
 *
 * <p>Minecraft-free, and counts rather than recipe objects, so the rule is a unit test and
 * {@code RuntimeHandRecipes} only has to count.
 */
public final class HandSetAdmission {

    /** Factorio's first category for a recipe the character can craft. */
    public static final String HAND_CATEGORY = "crafting";

    private HandSetAdmission() {
    }

    /**
     * Why this recipe cannot be planned, or null if it can.
     *
     * <p>A fluid is refused even in the hand category: the corpus has none there, and the Assembler
     * has nowhere to hold one, so a recipe that grew one is a converter change to hear about rather
     * than a plan that pauses forever.
     */
    public static String refusal(String category, int fluidIngredients, int fluidResults, int itemResults) {
        if (!HAND_CATEGORY.equals(category)) return "category " + category + ", not " + HAND_CATEGORY;
        if (fluidIngredients > 0) return "a fluid input, which the Assembler has nowhere to hold";
        if (fluidResults > 0) return "a fluid output";
        if (itemResults == 0) return "no item output";
        return null;
    }
}
