package com.factoryworks.core.oil;

import com.factoryworks.core.PFBlockEntities;
import com.factoryworks.core.energy.LongSnapshotJournal;

import io.github._5thlayer.pipeworks.api.FluidPorts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * The Pumpjack (ADR-0081): draws power from a pole, and each cycle takes the well under it down by
 * one depletion and puts the well's yield of crude into the Pipeworks segment it stands in, which
 * a pipe at any face of the footprint joins (ADR-0110).
 *
 * <p>A cycle waits for room for its whole yield, so a full segment neither draws work energy nor
 * depletes the well; the drain is paid regardless.
 */
public class PumpjackBlockEntity extends PumpjackPortBlockEntity {

    private static final PumpjackSpec SPEC = PumpjackSpec.fromCorpus();

    private final PumpjackEnergy energy = new PumpjackEnergy(SPEC);
    private final LongSnapshotJournal journal =
            new LongSnapshotJournal(energy::buffered, energy::setBuffered, this::setChanged);

    public PumpjackBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.PUMPJACK.get(), pos, state);
    }

    public static PumpjackSpec spec() {
        return SPEC;
    }

    public long progress() {
        return energy.progress();
    }

    /** The crude in the segment the Pumpjack stands in. */
    public int crude() {
        ResourceHandler<FluidResource> segment = segment();
        return segment == null ? 0 : segment.getAmountAsInt(0);
    }

    /** The Pumpjack's own fluid box, a part of every segment it stands in. */
    @Override
    public long capacity() {
        return SPEC.portCapacityMillibuckets();
    }

    private @Nullable ResourceHandler<FluidResource> segment() {
        return level == null ? null : FluidPorts.segment(level, worldPosition);
    }

    private FluidResource crudeResource() {
        return FluidResource.of(BuiltInRegistries.FLUID.getValue(Identifier.parse(SPEC.fluid())));
    }

    private boolean roomFor(int amount) {
        ResourceHandler<FluidResource> segment = segment();
        if (segment == null) {
            return false;
        }
        try (Transaction tx = Transaction.openRoot()) {
            return segment.insert(crudeResource(), amount, tx) == amount;
        }
    }

    public @Nullable OilWellBlockEntity well() {
        return level != null && level.getBlockEntity(worldPosition.below()) instanceof OilWellBlockEntity well
                ? well : null;
    }

    public void serverTick() {
        OilWellBlockEntity well = well();
        boolean working = well != null && roomFor(well.nextCycle());
        long before = energy.buffered();
        if (energy.tick(working) && well != null) {
            int crude = well.cycle();
            ResourceHandler<FluidResource> segment = segment();
            if (crude > 0 && segment != null) {
                try (Transaction tx = Transaction.openRoot()) {
                    segment.insert(crudeResource(), crude, tx);
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

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.setBuffered(input.getLongOr("Energy", 0L));
        energy.setProgress(input.getLongOr("Progress", 0L));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Energy", energy.buffered());
        output.putLong("Progress", energy.progress());
    }
}
