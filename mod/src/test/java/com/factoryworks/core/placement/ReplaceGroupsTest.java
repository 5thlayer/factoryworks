package com.factoryworks.core.placement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplaceGroupsTest {

    private static final String STONE = "factoryworks:stone_furnace";
    private static final String STEEL = "factoryworks:steel_furnace";
    private static final String ELECTRIC = "factoryworks:electric_furnace";
    private static final String SMALL = "wireworks:small_electric_pole";
    private static final String MEDIUM = "wireworks:medium_electric_pole";
    private static final String SUBSTATION = "wireworks:substation_electric_pole";
    private static final String ASSEMBLER_1 = "factoryworks:assembling_machine";
    private static final String ASSEMBLER_3 = "factoryworks:assembling_machine_3";

    private final ReplaceGroups groups = ReplaceGroups.get();

    @Test
    @DisplayName("tiers of one group replace each other, up and down")
    void sameGroupReplaces() {
        assertTrue(groups.canReplace(STEEL, STONE));
        assertTrue(groups.canReplace(STONE, ELECTRIC));
        assertTrue(groups.canReplace(MEDIUM, SMALL));
        assertTrue(groups.canReplace(ASSEMBLER_1, ASSEMBLER_3));
    }

    @Test
    @DisplayName("the substation is alone in its group")
    void substationReplacesNoPole() {
        assertFalse(groups.canReplace(SUBSTATION, SMALL));
        assertFalse(groups.canReplace(MEDIUM, SUBSTATION));
    }

    @Test
    @DisplayName("a block does not replace itself, another group, or a block with no group")
    void otherwiseNothingReplaces() {
        assertFalse(groups.canReplace(STONE, STONE));
        assertFalse(groups.canReplace(STONE, SMALL));
        assertFalse(groups.canReplace(STONE, "minecraft:furnace"));
        assertFalse(groups.canReplace("minecraft:furnace", STONE));
    }
}
