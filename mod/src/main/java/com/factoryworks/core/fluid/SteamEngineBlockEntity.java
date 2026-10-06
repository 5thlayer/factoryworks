package com.factoryworks.core.fluid;

import java.util.Optional;

import com.factoryworks.core.PFBlockEntities;
import com.factoryworks.core.energy.LongSnapshotJournal;
import io.github._5thlayer.pipeworks.api.FluidPort;
import io.github._5thlayer.pipeworks.api.FluidPorts;
import io.github._5thlayer.wireworks.ElectricNetworks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The Steam Engine's anchor (ADR-0116): a steam port and an FE buffer.
 *
 * <p>Each engine draws its own {@link SteamEngineSpec} rate from the segment it stands in, so engines
 * whose ports touch share a segment and need no row to chain them. A pole reaches the buffer through
 * the energy face; the rest of the footprint forwards to this block.
 */
public class SteamEngineBlockEntity extends BlockEntity implements FluidPort {

    private static final SteamEngineSpec SPEC = SteamEngineSpec.fromCorpus(SteamChainCorpus.get());

    private long stored;
    private SteamEngineSpec.Carry carry = SteamEngineSpec.Carry.NONE;
    private boolean burning;
    private final LongSnapshotJournal journal = new LongSnapshotJournal(() -> stored, v -> stored = v, this::setChanged);

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.STEAM_ENGINE.get(), pos, state);
    }

    public static SteamEngineSpec spec() {
        return SPEC;
    }

    @Override
    public boolean connectsOn(Direction face) {
        return true;
    }

    @Override
    public long capacity() {
        return SPEC.portCapacity();
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
        long room = SPEC.bufferCapacity() - stored;
        SteamEngineSpec.Request asked = SPEC.request(carry, room);
        FluidResource steam = FluidResource.of(PFFluids.STEAM_SOURCE.get());
        SteamEngineSpec.Tick made;
        try (Transaction transaction = Transaction.openRoot()) {
            int drawn = segment != null && asked.steam() > 0 ? segment.extract(steam, asked.steam(), transaction) : 0;
            made = SPEC.burn(drawn, asked.carry(), room);
            transaction.commit();
        }
        boolean changed = made.energy() > 0L || !made.carry().equals(carry);
        carry = made.carry();
        stored += made.energy();
        burning = made.steam() > 0;
        if (changed) {
            setChanged();
        }
    }

    /** The FE face on every block of the engine. Journalled so a pole's aborted probe takes nothing. */
    public EnergyHandler energyHandler() {
        return energy;
    }

    private final EnergyHandler energy = new EnergyHandler() {
        @Override
        public long getAmountAsLong() {
            return stored;
        }

        @Override
        public long getCapacityAsLong() {
            return SPEC.bufferCapacity();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            int taken = (int) Math.min(Math.max(amount, 0), stored);
            if (taken > 0) {
                journal.updateSnapshots(transaction);
                stored -= taken;
            }
            return taken;
        }
    };

    public Optional<SteamEngineStatus> status() {
        return SteamEngineStatus.of(burning || hasSteam(), ElectricNetworks.of(level).drawsFrom(worldPosition));
    }

    private boolean hasSteam() {
        ResourceHandler<FluidResource> segment = level == null ? null : FluidPorts.segment(level, worldPosition);
        return segment != null && segment.getAmountAsLong(0) > 0
                && segment.getResource(0).equals(FluidResource.of(PFFluids.STEAM_SOURCE.get()));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        stored = input.getLongOr("Energy", 0L);
        carry = new SteamEngineSpec.Carry(input.getDoubleOr("CarrySteam", 0.0), input.getDoubleOr("CarryEnergy", 0.0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Energy", stored);
        output.putDouble("CarrySteam", carry.steam());
        output.putDouble("CarryEnergy", carry.energy());
    }
}
