package com.factoryworks.core.energy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A supply area's blocks sorted into roles by the energy owner they answer to (#292, ADR-0062).
 *
 * <p>Positions are strings here: the rule is only about which block stands for which, and a
 * {@code BlockPos} would drag Minecraft onto a classpath that deliberately has none.
 */
class SupplyScanTest {

    private static final Map<String, SupplyScan.Role> ROLES = Map.of(
            "furnace", SupplyScan.Role.CONSUMER,
            "master", SupplyScan.Role.GENERATOR,
            "slave", SupplyScan.Role.GENERATOR,
            "lone", SupplyScan.Role.GENERATOR,
            "battery", SupplyScan.Role.ACCUMULATOR,
            // A hull block answers the controller's face, and its own block carries no tag.
            "core", SupplyScan.Role.CONSUMER);

    private static final Map<String, String> OWNERS = Map.of(
            "core", "slave",
            "slave", "master");

    private static SupplyScan.Roles<String> scan(String... positions) {
        return SupplyScan.classify(List.of(positions), OWNERS::get,
                p -> ROLES.getOrDefault(p, SupplyScan.Role.NONE));
    }

    @Test
    void aBlockAnsweringToNobodyIsItsOwnOwner() {
        SupplyScan.Roles<String> roles = scan("furnace", "lone", "battery");
        assertEquals(List.of("furnace"), roles.consumers());
        assertEquals(List.of("lone"), roles.generators());
        assertEquals(List.of("battery"), roles.accumulators());
    }

    @Test
    void aHullBlockOfATaggedMachineIsNeverAConsumer() {
        SupplyScan.Roles<String> roles = scan("core");
        assertEquals(List.of(), roles.consumers());
        assertEquals(List.of("master"), roles.generators());
    }

    @Test
    void aSlaveResolvesToItsMasterEvenWhenTheMasterIsOutOfTheArea() {
        assertEquals(List.of("master"), scan("slave").generators());
    }

    @Test
    void anOwnerIsKeptOnceHoweverManyOfItsBlocksAreInTheArea() {
        SupplyScan.Roles<String> roles = scan("core", "slave", "master", "furnace", "furnace");
        assertEquals(List.of("master"), roles.generators());
        assertEquals(List.of("furnace"), roles.consumers());
    }

    @Test
    void aBlockWithNoFaceIsDropped() {
        SupplyScan.Roles<String> roles = scan("stone");
        assertEquals(List.of(), roles.consumers());
        assertEquals(List.of(), roles.generators());
        assertEquals(List.of(), roles.accumulators());
    }

    @Test
    void anOwnerCycleTerminatesRatherThanHangingTheTick() {
        // No mod should hand one back, and a server tick is the wrong place to find out it did.
        SupplyScan.Roles<String> roles = SupplyScan.classify(List.of("a"),
                p -> p.equals("a") ? "b" : "a", p -> SupplyScan.Role.CONSUMER);
        assertEquals(1, roles.consumers().size());
    }
}
