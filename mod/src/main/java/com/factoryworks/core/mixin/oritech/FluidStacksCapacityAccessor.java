package com.factoryworks.core.mixin.oritech;

import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets the Steam Engine resize its steam tank to its row (#282, ADR-0062).
 *
 * <p>NeoForge takes the capacity once, in the constructor, and Oritech sizes every generator's
 * tank from one config value. The engine's tank is Factorio's steam box per engine in the row, and
 * a row's length is only known once {@code setupMaster} has scanned it, so the field is set on
 * every master tick. It is written on one instance and read by nothing else.
 */
@Mixin(FluidStacksResourceHandler.class)
public interface FluidStacksCapacityAccessor {

    @Accessor("capacity")
    void factoryworks$setCapacity(int capacity);
}
