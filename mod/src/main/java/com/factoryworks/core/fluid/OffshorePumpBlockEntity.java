package com.factoryworks.core.fluid;

import com.factoryworks.core.PFBlockEntities;

import io.github._5thlayer.pipeworks.api.FluidPort;
import io.github._5thlayer.pipeworks.api.FluidPorts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The pump running (#213, ADR-0050): a Pipeworks port that puts 60 mB of the fluid it was sited on
 * (#256) into its segment every tick, as far as the segment has room (ADR-0110).
 *
 * <p>Nothing here checks the water is still there. The site was settled at placement
 * ({@link OffshorePumpItem}), which is Factorio's own bargain and safe under ADR-0050 for the
 * reason {@link OffshorePumpSiting} states.
 *
 * <p>No energy. {@link PumpCorpus#takesPower()} reads Factorio's {@code void} energy source.
 */
public class OffshorePumpBlockEntity extends BlockEntity implements FluidPort {

    private static final int MILLIBUCKETS_PER_TICK =
            OffshorePumpSpec.milliBucketsPerTick(PumpCorpus.get().pumpingSpeed());

    private static final String TAG_FLUID = "Fluid";

    /** What the pump was sited on (#256), recorded once at placement. */
    private String fluid = "minecraft:water";

    public OffshorePumpBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.OFFSHORE_PUMP.get(), pos, state);
    }

    @Override
    public boolean connectsOn(Direction face) {
        return true;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        FluidPorts.join(this);
    }

    static void leave(ServerLevel level, BlockPos pos) {
        FluidPorts.leave(level, pos);
    }

    public void serverTick() {
        ResourceHandler<FluidResource> segment = FluidPorts.segment(level, worldPosition);
        if (segment == null) {
            return;
        }
        try (Transaction tx = Transaction.openRoot()) {
            segment.insert(FluidResource.of(pumped()), MILLIBUCKETS_PER_TICK, tx);
            tx.commit();
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
}
