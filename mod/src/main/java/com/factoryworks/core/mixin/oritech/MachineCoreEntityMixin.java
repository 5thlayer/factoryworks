package com.factoryworks.core.mixin.oritech;

import com.factoryworks.core.energy.EnergyOwner;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import rearth.oritech.block.entity.MachineCoreEntity;

/**
 * An Oritech machine core answers to its controller on the pole network (#292, ADR-0062).
 *
 * <p>Read off the 2.0.0-exp6 jar: {@code MACHINE_CORE} is registered for {@code Energy.BLOCK}, and a
 * used core with a cached controller hands out the controller's whole energy storage. Its block is
 * not the controller's, so it carries none of the controller's tags, and a scan that filed it by its
 * own block made every hull block of a Steam Engine a consumer.
 */
@Mixin(MachineCoreEntity.class)
public abstract class MachineCoreEntityMixin implements EnergyOwner {

    @Override
    public BlockPos factoryworks$energyOwner() {
        MachineCoreEntity self = (MachineCoreEntity) (Object) this;
        // The same two conditions under which the core exposes the controller's face at all.
        return self.isEnabled() && self.getCachedController() != null
                ? self.getControllerPos()
                : null;
    }
}
