package com.planetaryfactory.core.machine;

import java.util.ArrayList;
import java.util.List;

import com.planetaryfactory.core.machine.footprint.Footprint;
import com.planetaryfactory.core.machine.footprint.Footprint.Local;

/**
 * Where the Assembling Machine stands (#326): two wide, one deep, two tall -- Oritech's own
 * assembler's controller and three cores, placed as one footprint.
 *
 * <p><b>The model's own extent, not Factorio's 3x3.</b> ADR-0059 says a machine's footprint is its
 * Factorio tile size, and ADR-0071 has the machine reuse Oritech's assembler model -- which is drawn
 * over Oritech's 2x1x2 and cannot fill a 3x3 without being redrawn.
 * #326 took the model's extent, so what the player sees and what the player collides with are the
 * same blocks. The divergence from ADR-0059 is recorded in ADR-0072.
 *
 * <p>It holds the three core positions Oritech's own assembler uses, in {@link Footprint}'s frame.
 *
 * <p>Minecraft-free, for the reason {@code RigGeometry} is: this is arithmetic, and the block glue
 * that rotates it into a {@code BlockPos} lives beside the blocks.
 */
public final class AssemblingMachineFootprint {

    public static final int WIDE = 2;
    public static final int TALL = 2;

    /** The lateral offset of the machine's first column: the anchor's, with the cores' beside it. */
    private static final int FIRST_COLUMN = 0;

    public static final Footprint FOOTPRINT = build();

    private AssemblingMachineFootprint() {
    }

    /**
     * Where the machine's addons go: one beyond each end of the row, and one behind the anchor --
     * Oritech's assembler's own {@code (0,0,-1)}, {@code (0,0,2)} and {@code (1,0,0)}.
     */
    public static List<Local> addonSlots() {
        return List.of(new Local(0, 0, FIRST_COLUMN - 1), new Local(0, 0, FIRST_COLUMN + WIDE),
                new Local(1, 0, 0));
    }

    private static Footprint build() {
        List<Local> parts = new ArrayList<>(WIDE * TALL - 1);
        for (int y = 0; y < TALL; y++) {
            for (int z = FIRST_COLUMN; z < FIRST_COLUMN + WIDE; z++) {
                if (y == 0 && z == 0) {
                    continue;
                }
                parts.add(new Local(0, y, z));
            }
        }
        return Footprint.of(parts.toArray(Local[]::new));
    }
}
