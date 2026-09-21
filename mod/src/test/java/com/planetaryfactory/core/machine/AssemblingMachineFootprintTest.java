package com.planetaryfactory.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.planetaryfactory.core.machine.AssemblingMachineFootprint.Local;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The Assembling Machine's footprint (#326): Oritech's assembler model's own extent, 2 wide, 1 deep
 * and 2 tall, in Oritech's controller-local frame.
 *
 * <p>The frame is the load-bearing part. The model is drawn by Oritech's renderer, rotated by the
 * block's facing, and the footprint is rotated by Oritech's {@code Geometry.rotatePosition} with the
 * same facing -- so the two agree only while the offsets are in the frame Oritech's own
 * {@code getCorePositions()} is written in. That is why the three core positions Oritech's
 * assembler ships are asserted to be inside it: a footprint that grew to the other side would place
 * blocks where the model is not, and the model would stand half inside something else.
 */
class AssemblingMachineFootprintTest {

    @Test
    void itIsTwoWideOneDeepTwoTallWithTheAnchorFirst() {
        List<Local> offsets = AssemblingMachineFootprint.offsets();

        assertEquals(2 * 1 * 2, offsets.size());
        assertEquals(new Local(0, 0, 0), offsets.getFirst(), "the anchor is always first");
        assertEquals(offsets.size(), Set.copyOf(offsets).size(), "no position twice");
        for (Local offset : offsets) {
            assertEquals(0, offset.x(), "one deep: nothing in front of or behind the anchor");
            assertTrue(offset.y() >= 0 && offset.y() < 2, "two tall, standing on the anchor's level");
            assertTrue(offset.z() == 0 || offset.z() == 1, "two wide: the anchor and the cores' side");
        }
    }

    @Test
    void itIsExactlyOritechsControllerAndCores() {
        // AssemblerBlockEntity.getCorePositions() at 2.0.0-exp6, read off the jar, plus the anchor.
        Set<Local> oritech = Set.of(new Local(0, 0, 0),
                new Local(0, 0, 1), new Local(0, 1, 0), new Local(0, 1, 1));

        assertEquals(oritech, Set.copyOf(AssemblingMachineFootprint.offsets()));
    }

    @Test
    void itsAddonSlotsAreOritechsAssemblers() {
        assertEquals(List.of(new Local(0, 0, -1), new Local(0, 0, 2), new Local(1, 0, 0)),
                AssemblingMachineFootprint.addonSlots());
    }

    @Test
    void oritechsAddonSlotsAreOutsideIt() {
        // Addons stand beside the machine; a slot inside the footprint is a part, and no addon
        // could ever be placed on it.
        Set<Local> footprint = Set.copyOf(AssemblingMachineFootprint.offsets());
        for (Local slot : AssemblingMachineFootprint.addonSlots()) {
            assertTrue(!footprint.contains(slot), slot + " is inside the footprint");
        }
    }

    @Test
    void aPartIndexNamesItsOffsetAndBack() {
        List<Local> offsets = AssemblingMachineFootprint.offsets();
        for (int part = 1; part < offsets.size(); part++) {
            assertEquals(offsets.get(part), AssemblingMachineFootprint.offsetOfPart(part));
        }
        assertEquals(offsets.size() - 1, AssemblingMachineFootprint.PART_COUNT);
    }

    @Test
    void theAnchorIsNotAPart() {
        assertThrows(IllegalArgumentException.class, () -> AssemblingMachineFootprint.offsetOfPart(0));
        assertThrows(IllegalArgumentException.class,
                () -> AssemblingMachineFootprint.offsetOfPart(AssemblingMachineFootprint.PART_COUNT + 1));
    }
}
