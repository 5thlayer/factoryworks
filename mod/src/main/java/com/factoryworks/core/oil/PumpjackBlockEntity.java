package com.factoryworks.core.oil;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.factoryworks.core.PFBlockEntities;
import com.factoryworks.core.energy.LongSnapshotJournal;
import com.factoryworks.core.transfer.GuardedResourceHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * The Pumpjack (ADR-0081): draws power from a pole, and each cycle takes the well under it down by
 * one depletion and puts the well's yield of crude into its tank, which any face drains.
 *
 * <p>A cycle waits for room for its whole yield, so a full tank neither draws work energy nor
 * depletes the well; the drain is paid regardless.
 */
public class PumpjackBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final PumpjackSpec SPEC = PumpjackSpec.fromCorpus();

    private final PumpjackEnergy energy = new PumpjackEnergy(SPEC);
    private final LongSnapshotJournal journal =
            new LongSnapshotJournal(energy::buffered, energy::setBuffered, this::setChanged);
    private final FluidStacksResourceHandler tank = new FluidStacksResourceHandler(1, SPEC.tankMillibuckets()) {
        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            setChanged();
        }
    };
    private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);

    public PumpjackBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.PUMPJACK.get(), pos, state);
    }

    public static PumpjackSpec spec() {
        return SPEC;
    }

    public long progress() {
        return energy.progress();
    }

    public int crude() {
        return tank.getAmountAsInt(0);
    }

    public @Nullable OilWellBlockEntity well() {
        return level != null && level.getBlockEntity(worldPosition.below()) instanceof OilWellBlockEntity well
                ? well : null;
    }

    public void serverTick() {
        OilWellBlockEntity well = well();
        boolean working = well != null && SPEC.tankMillibuckets() - crude() >= well.nextCycle();
        long before = energy.buffered();
        if (energy.tick(working) && well != null) {
            int crude = well.cycle();
            if (crude > 0) {
                try (Transaction tx = Transaction.openRoot()) {
                    tank.insert(0, FluidResource.of(BuiltInRegistries.FLUID.getValue(Identifier.parse(SPEC.fluid()))),
                            crude, tx);
                    tx.commit();
                }
            }
        }
        if (energy.buffered() != before) {
            setChanged();
        }
    }

    /** The FE face a pole fills. Insert-only, and journalled so a pole's aborted probe leaves nothing. */
    public EnergyHandler energySide() {
        return energyHandler;
    }

    private final EnergyHandler energyHandler = new EnergyHandler() {
        @Override
        public long getAmountAsLong() {
            return energy.buffered();
        }

        @Override
        public long getCapacityAsLong() {
            return energy.capacity();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            if (amount <= 0) {
                return 0;
            }
            journal.updateSnapshots(transaction);
            return (int) energy.insert(amount);
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0;
        }
    };

    /** Crude out through any face, and nothing in (ADR-0081). */
    public ResourceHandler<FluidResource> fluidSide() {
        return new GuardedResourceHandler<>(tank) {
            @Override
            public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
                return 0;
            }

            @Override
            public boolean isValid(int index, FluidResource resource) {
                return false;
            }
        };
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animations;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.setBuffered(input.getLongOr("Energy", 0L));
        energy.setProgress(input.getLongOr("Progress", 0L));
        tank.deserialize(input.childOrEmpty("Tank"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Energy", energy.buffered());
        output.putLong("Progress", energy.progress());
        tank.serialize(output.child("Tank"));
    }
}
