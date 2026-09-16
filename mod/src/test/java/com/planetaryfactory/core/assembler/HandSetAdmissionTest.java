package com.planetaryfactory.core.assembler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Which {@code planetaryfactory:assembling} recipes the Personal Assembler plans (#279, ADR-0038).
 *
 * <p>The hand set is a predicate over the Assembling Machine's type, not a type of its own, so the
 * predicate is the whole definition of what a player can plan. A refusal is a reason rather than a
 * boolean because {@code RuntimeHandRecipes} logs it by name.
 */
class HandSetAdmissionTest {

    @Test
    void anItemOnlyCraftingRecipeIsAdmitted() {
        assertNull(HandSetAdmission.refusal("crafting", 0, 0, 1));
    }

    @Test
    void theOtherAssemblingCategoriesAreNotHandCraftable() {
        assertNotNull(HandSetAdmission.refusal("crafting-with-fluid", 0, 0, 1));
        assertNotNull(HandSetAdmission.refusal("advanced-crafting", 0, 0, 1));
    }

    @Test
    void aFluidOnEitherSideIsRefusedEvenInTheHandCategory() {
        assertNotNull(HandSetAdmission.refusal("crafting", 1, 0, 1));
        assertNotNull(HandSetAdmission.refusal("crafting", 0, 1, 1));
    }

    @Test
    void aRecipeWithNoItemResultIsRefused() {
        assertNotNull(HandSetAdmission.refusal("crafting", 0, 0, 0));
    }

    @Test
    void theCategoryIsMatchedExactly() {
        assertNotNull(HandSetAdmission.refusal("Crafting", 0, 0, 1));
        assertNotNull(HandSetAdmission.refusal("", 0, 0, 1));
    }
}
