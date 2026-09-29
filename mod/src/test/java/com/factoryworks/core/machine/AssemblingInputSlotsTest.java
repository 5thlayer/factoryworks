package com.factoryworks.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.Test;

/** Ingredients are strings, and an item matches one it is a substring of, standing in for a tag. */
class AssemblingInputSlotsTest {

    private static final BiPredicate<String, String> CONTAINS = String::contains;

    private static boolean accepts(int slot, List<String> ingredients, String item) {
        return AssemblingInputSlots.accepts(slot, ingredients, item, CONTAINS);
    }

    @Test
    void theNthIngredientIsTheNthSlots() {
        List<String> recipe = List.of("iron_plate", "copper_cable", "gear");
        for (int slot = 0; slot < recipe.size(); slot++) {
            assertEquals(Optional.of(recipe.get(slot)), AssemblingInputSlots.ingredientFor(slot, recipe));
        }
    }

    @Test
    void aSlotAcceptsItsOwnIngredient() {
        assertTrue(accepts(1, List.of("iron_plate", "copper_cable"), "copper_cable"));
    }

    @Test
    void aSlotRefusesAnotherSlotsIngredient() {
        assertFalse(accepts(0, List.of("iron_plate", "copper_cable"), "copper_cable"));
    }

    @Test
    void aSlotRefusesAnItemTheRecipeDoesNotUse() {
        assertFalse(accepts(0, List.of("iron_plate"), "stone"));
    }

    @Test
    void aTagIngredientAcceptsEveryMember() {
        List<String> recipe = List.of("oak_logs|birch_logs");
        assertTrue(accepts(0, recipe, "oak_logs"));
        assertTrue(accepts(0, recipe, "birch_logs"));
    }

    @Test
    void aSlotTheRecipeDoesNotUseAcceptsNothing() {
        List<String> recipe = List.of("iron_plate", "copper_cable");
        for (int slot = recipe.size(); slot < AssemblingInputSlots.INPUTS; slot++) {
            assertEquals(Optional.empty(), AssemblingInputSlots.ingredientFor(slot, recipe));
            assertFalse(accepts(slot, recipe, "iron_plate"), "slot " + slot);
            assertFalse(accepts(slot, recipe, "copper_cable"), "slot " + slot);
        }
    }

    @Test
    void withNoRecipeEverySlotAcceptsNothing() {
        for (int slot = 0; slot < AssemblingInputSlots.INPUTS; slot++) {
            assertEquals(Optional.empty(), AssemblingInputSlots.ingredientFor(slot, List.of()));
            assertFalse(accepts(slot, List.of(), "iron_plate"), "slot " + slot);
        }
    }

    @Test
    void anIngredientPastTheFourthHasNoSlot() {
        List<String> recipe = List.of("a", "b", "c", "d", "e");
        assertEquals(Optional.empty(), AssemblingInputSlots.ingredientFor(AssemblingInputSlots.INPUTS, recipe));
        assertFalse(accepts(AssemblingInputSlots.INPUTS, recipe, "e"));
    }

    @Test
    void aSlotOutsideTheInputsIsForNothing() {
        assertEquals(Optional.empty(), AssemblingInputSlots.ingredientFor(-1, List.of("a")));
    }

    private static final List<String> TWO_PLATES_ONE_CABLE = List.of("iron_plate:2", "copper_cable:1");

    private static boolean isShort(int slot, int held) {
        return AssemblingInputSlots.isShort(slot, TWO_PLATES_ONE_CABLE, held,
                ingredient -> Integer.parseInt(ingredient.substring(ingredient.indexOf(':') + 1)));
    }

    @Test
    void aSlotHoldingLessThanOneCraftIsShort() {
        assertTrue(isShort(0, 0));
        assertTrue(isShort(0, 1));
    }

    @Test
    void aSlotHoldingOneCraftIsNotShort() {
        assertFalse(isShort(0, 2));
        assertFalse(isShort(1, 1));
        assertFalse(isShort(0, 64));
    }

    @Test
    void aSlotTheRecipeDoesNotUseIsNeverShort() {
        for (int slot = TWO_PLATES_ONE_CABLE.size(); slot < AssemblingInputSlots.INPUTS; slot++) {
            assertFalse(isShort(slot, 0), "slot " + slot);
        }
    }
}
