package com.planetaryfactory.core.fluid;

import com.planetaryfactory.core.transfer.GuardedResourceHandler;

import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import rearth.oritech.block.entity.generators.SteamEngineEntity;

/**
 * The Steam Engine's fluid face: its steam tank alone, insert-only. Oritech's face also exposes the
 * water tank its engine returns into, which ADR-0062 leaves empty for good, so a pipe or Jade would
 * show a tank that never fills.
 */
public class SteamEngineFluidHandler extends GuardedResourceHandler<FluidResource> {

    private final SteamEngineEntity engine;

    /** {@code engine} is the one whose tank this is: a slave's master. */
    public SteamEngineFluidHandler(SteamEngineEntity engine) {
        super(engine.boilerStorage.getInputContainer());
        this.engine = engine;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return engine.boilerAcceptsInput(resource) && super.isValid(index, resource);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (!engine.boilerAcceptsInput(resource)) {
            return 0;
        }
        return super.insert(index, resource, amount, transaction);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return 0;
    }
}
