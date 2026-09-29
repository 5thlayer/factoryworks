package com.factoryworks.core.fluid;

import com.factoryworks.core.PFBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import com.factoryworks.core.transfer.GuardedResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The pump running (#213, ADR-0050): 60 mB of the fluid it was sited on (#256) into a small buffer every tick, for whatever
 * pipe cares to take it.
 *
 * <p><b>The buffer holds nothing between ticks that matters.</b> It exists because a fluid
 * capability needs somewhere to hand fluid out of, not as storage -- it is one tick's production
 * over, so a pump left unconnected does not silently accumulate a tank's worth of water to dump the
 * moment a pipe arrives. Water is unlimited at the source anyway; what a buffer would add is a
 * burst, and a burst is a thing a player would have to plan around.
 *
 * <p><b>Nothing here checks the water is still there.</b> The site was settled at placement
 * ({@link OffshorePumpItem}) and is not re-tested, which is Factorio's own bargain and safe under
 * ADR-0050 for the reason {@link OffshorePumpSiting} states.
 *
 * <p>No energy. {@link PumpCorpus#takesPower()} reads Factorio's {@code void} energy source, so the
 * absence of a power connection is a read fact rather than something nobody wired up.
 */
public class OffshorePumpBlockEntity extends BlockEntity {

    /**
     * One tick's production. Not a tank: a pump that banks water while unconnected would deliver a
     * burst on connection, which is a behaviour a player would have to learn.
     */
    private final FluidStacksResourceHandler buffer = new FluidStacksResourceHandler(1, OffshorePumpSpec.milliBucketsPerTick(PumpCorpus.get().pumpingSpeed())) {
        @Override
        protected void onContentsChanged(int index, net.neoforged.neoforge.fluids.FluidStack previousContents) {
            setChanged();
        }
    };

    private static final String TAG_FLUID = "Fluid";

    /**
     * What the pump was sited on (#256), recorded once at placement and saved with the block. Water
     * until told otherwise, which is what every pump placed before #256 was pumping.
     */
    private String fluid = "minecraft:water";

    public OffshorePumpBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.OFFSHORE_PUMP.get(), pos, state);
    }

    /**
     * Refill to the tick's worth. Filling rather than setting, so a pipe that took only part of
     * last tick's water is topped up rather than handed a second full measure -- the rate is
     * 60 mB a tick delivered, not 60 mB a tick offered.
     */
    public void serverTick() {
        int room = buffer.getCapacityAsInt(0, FluidResource.EMPTY) - buffer.getAmountAsInt(0);
        if (room > 0) {
            try (Transaction tx = Transaction.openRoot()) {
                buffer.insert(FluidResource.of(pumped()), room, tx);
                tx.commit();
            }
        }
    }

    /** Records the site's fluid. {@link OffshorePumpItem} calls this once, right after placing. */
    public void setFluid(String fluid) {
        this.fluid = fluid;
        setChanged();
    }

    /** The recorded fluid, or nothing at all if its mod has left the pack since. */
    private Fluid pumped() {
        return BuiltInRegistries.FLUID.getValue(Identifier.parse(fluid));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString(TAG_FLUID, fluid);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fluid = input.getStringOr(TAG_FLUID, "minecraft:water");
    }

    /**
     * Extract-only. A pump is an origin, and letting something push fluid back into it would make
     * it a pipe junction that happens to make water.
     *
     * <p>{@link GuardedResourceHandler} rather than a plain {@code DelegatingResourceHandler}
     * because the refusal has to hold for the slot-less {@code insert} too, which the plain one
     * forwards straight to the buffer.
     */
    public ResourceHandler<FluidResource> fluidHandler() {
        return new GuardedResourceHandler<>(buffer) {
            @Override
            public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
                return 0;
            }

            /** Nothing can be pushed in, so nothing can be valid to push in either. */
            @Override
            public boolean isValid(int index, FluidResource resource) {
                return false;
            }
        };
    }
}
