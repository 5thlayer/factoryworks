package com.planetaryfactory.core.energy;

import net.minecraft.core.BlockPos;

/**
 * A block entity whose energy belongs to another block's (#292, ADR-0062).
 *
 * <p>Implemented by mixin on foreign block entities -- Oritech's machine core and Steam Engine --
 * so the pole can ask the question without importing the mod that answers it.
 * {@link SupplyScan} follows the answer until it stops, so a hull block of a slave engine reaches
 * the row's master in two hops.
 */
public interface EnergyOwner {

    /** The block whose energy this one's face stands for, or {@code null} if it is its own. */
    BlockPos planetaryfactory$energyOwner();
}
