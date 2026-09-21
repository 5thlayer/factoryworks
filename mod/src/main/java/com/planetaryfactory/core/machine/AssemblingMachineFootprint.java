package com.planetaryfactory.core.machine;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the Assembling Machine stands (#326): four wide, one deep, two tall.
 *
 * <p><b>The model's own extent, not Factorio's 3x3.</b> ADR-0059 says a machine's footprint is its
 * Factorio tile size, and ADR-0071 has the machine reuse Oritech's assembler model -- which spans
 * one block to one side of the anchor and two to the other, one deep and two and a half tall, and
 * cannot fill a 3x3 without being redrawn.
 * #326 took the model's extent, so what the player sees and what the player collides with are the
 * same blocks. The divergence from ADR-0059 is recorded in ADR-0072.
 *
 * <p><b>The offsets are in Oritech's controller-local frame</b>: {@code x} forward, {@code y} up,
 * {@code z} lateral, turned into world positions by Oritech's {@code Geometry.rotatePosition} with
 * the block's facing. That is the frame {@code AssemblerBlockEntity.getCorePositions()} is written
 * in, and the renderer rotates the model by the same facing, so the footprint lands where the model
 * is drawn only while it stays in that frame. The three core positions Oritech's own assembler uses
 * are inside it, which {@code AssemblingMachineFootprintTest} asserts.
 *
 * <p>Minecraft-free, for the reason {@code RigGeometry} is: this is arithmetic, and the block glue
 * that rotates it into a {@code BlockPos} lives beside the blocks.
 */
public final class AssemblingMachineFootprint {

    public static final int WIDE = 4;
    public static final int TALL = 2;

    /**
     * The lateral offset of the machine's first column: one to the side opposite Oritech's cores,
     * so the anchor is the second column of four and the cores' side carries the two beyond it.
     */
    private static final int FIRST_COLUMN = -1;

    private static final List<Local> OFFSETS = build();

    /** Every block but the anchor. A part's blockstate names which one it is, 1 to this. */
    public static final int PART_COUNT = OFFSETS.size() - 1;

    private AssemblingMachineFootprint() {
    }

    /** Every position the machine occupies, anchor first. */
    public static List<Local> offsets() {
        return OFFSETS;
    }

    /** The offset of part {@code part}, which is its index into {@link #offsets()}. */
    public static Local offsetOfPart(int part) {
        if (part < 1 || part > PART_COUNT) {
            throw new IllegalArgumentException("a part is numbered 1 to " + PART_COUNT + ", got " + part);
        }
        return OFFSETS.get(part);
    }

    /**
     * Where the machine's addons go: one beyond each end of the row, and one behind the anchor.
     * Oritech's assembler has them at lateral -1 and 2, which this footprint occupies, so the side
     * slots move one further out; behind the anchor is Oritech's own {@code (1, 0, 0)}.
     */
    public static List<Local> addonSlots() {
        return List.of(new Local(0, 0, FIRST_COLUMN - 1), new Local(0, 0, FIRST_COLUMN + WIDE),
                new Local(1, 0, 0));
    }

    private static List<Local> build() {
        List<Local> offsets = new ArrayList<>(WIDE * TALL);
        offsets.add(new Local(0, 0, 0));
        for (int y = 0; y < TALL; y++) {
            for (int z = FIRST_COLUMN; z < FIRST_COLUMN + WIDE; z++) {
                if (y == 0 && z == 0) {
                    continue;
                }
                offsets.add(new Local(0, y, z));
            }
        }
        return List.copyOf(offsets);
    }

    /** One position relative to the anchor, in Oritech's controller-local frame. */
    public record Local(int x, int y, int z) {
    }
}
