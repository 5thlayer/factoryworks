package com.planetaryfactory.core.research;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The index is what stands in for {@code isRecipeBlocked} when there is no team to ask about
 * (issue #74). Strings stand in for {@code Identifier} and {@code ResourceKey<Research>};
 * the index never looks inside either, so nothing here is weakened by the substitution.
 */
class RecipeResearchIndexTest {

    private static RecipeResearchIndex<String, String> index() {
        return RecipeResearchIndex.<String, String>builder()
                .add("steam_power", List.of("oritech:powered_furnace", "oritech:foundry"))
                .add("electricity", List.of("oritech:assembler"))
                .build();
    }

    @Test
    void reportsRecipesSomeResearchUnlocks() {
        assertTrue(index().isUnlockedByResearch("oritech:powered_furnace"));
        assertTrue(index().isUnlockedByResearch("oritech:assembler"));
    }

    @Test
    void doesNotReportARecipeNoResearchMentions() {
        assertFalse(index().isUnlockedByResearch("minecraft:stick"));
    }

    @Test
    void anEmptyIndexUnlocksNothing() {
        assertFalse(RecipeResearchIndex.<String, String>empty().isUnlockedByResearch("oritech:assembler"));
        assertEquals(0, RecipeResearchIndex.empty().size());
    }

    @Test
    void namesTheResearchThatUnlocksARecipe() {
        assertEquals(Set.of("steam_power"), index().researchesUnlocking("oritech:foundry"));
    }

    @Test
    void namesEveryResearchWhenTwoUnlockTheSameRecipe() {
        RecipeResearchIndex<String, String> shared = RecipeResearchIndex.<String, String>builder()
                .add("steam_power", List.of("oritech:centrifuge"))
                .add("alloys", List.of("oritech:centrifuge"))
                .build();

        assertEquals(Set.of("steam_power", "alloys"), shared.researchesUnlocking("oritech:centrifuge"));
        assertEquals(1, shared.size(), "one recipe id, however many researches name it");
    }

    @Test
    void namesTheResearchOnceWhenItRepeatsARecipe() {
        RecipeResearchIndex<String, String> repeated = RecipeResearchIndex.<String, String>builder()
                .add("steam_power", List.of("oritech:foundry", "oritech:foundry"))
                .build();

        assertEquals(Set.of("steam_power"), repeated.researchesUnlocking("oritech:foundry"));
    }

    @Test
    void yieldsNoResearchesForAnUnlockedRecipe() {
        assertEquals(Set.of(), index().researchesUnlocking("minecraft:stick"));
    }

    @Test
    void keepsRegistryOrderSoAnAnnotationIsStable() {
        RecipeResearchIndex<String, String> shared = RecipeResearchIndex.<String, String>builder()
                .add("steam_power", List.of("oritech:centrifuge"))
                .add("alloys", List.of("oritech:centrifuge"))
                .build();

        assertEquals(List.of("steam_power", "alloys"), List.copyOf(shared.researchesUnlocking("oritech:centrifuge")));
    }

    @Test
    void isImmutableOnceBuilt() {
        RecipeResearchIndex<String, String> built = index();

        assertThrows(
                UnsupportedOperationException.class,
                () -> built.researchesUnlocking("oritech:assembler").add("smuggled_in"));
    }

    @Test
    void ignoresLaterBuilderWrites() {
        RecipeResearchIndex.Builder<String, String> builder =
                RecipeResearchIndex.<String, String>builder().add("steam_power", List.of("oritech:foundry"));
        RecipeResearchIndex<String, String> built = builder.build();

        builder.add("electricity", List.of("oritech:assembler"));

        assertFalse(built.isUnlockedByResearch("oritech:assembler"), "a built index is a snapshot");
        assertEquals(1, built.size());
    }
}
