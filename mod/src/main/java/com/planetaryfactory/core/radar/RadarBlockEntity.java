package com.planetaryfactory.core.radar;

import java.util.UUID;

import com.planetaryfactory.core.PFBlockEntities;
import com.planetaryfactory.core.energy.LongSnapshotJournal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * The Radar (#368, ADR-0079): draws power, and charts one sector of its reach into its owner's
 * team's chart per 10 MJ, nearest first, pass after pass.
 */
public class RadarBlockEntity extends BlockEntity {

    private static final RadarSpec SPEC = RadarSpec.fromCorpus();
    private static final RadarSweep SWEEP = RadarSweep.of(SPEC.reach());

    private final RadarEnergy energy = new RadarEnergy(SPEC);
    private final LongSnapshotJournal journal =
            new LongSnapshotJournal(energy::buffered, energy::setBuffered, this::setChanged);
    private int cursor;
    private @Nullable UUID owner;

    public RadarBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.RADAR.get(), pos, state);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public @Nullable UUID owner() {
        return owner;
    }

    public long progress() {
        return energy.progress();
    }

    public static RadarSpec spec() {
        return SPEC;
    }

    public int cursor() {
        return cursor;
    }

    public int sweepSize() {
        return SWEEP.size();
    }

    public Sector nextSector() {
        return SWEEP.sectorAt(Sector.ofBlock(worldPosition.getX(), worldPosition.getZ()), cursor);
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        long before = energy.progress();
        if (energy.tick() > 0) {
            chart(serverLevel, nextSector());
            cursor = SWEEP.next(cursor);
        }
        if (energy.progress() != before) {
            setChanged();
        }
    }

    /** An unowned Radar, one placed other than by a player, charts for no team and generates nothing. */
    private void chart(ServerLevel serverLevel, Sector sector) {
        if (owner == null) {
            return;
        }
        for (int dx = 0; dx < Sector.CHUNKS_PER_SIDE; dx++) {
            for (int dz = 0; dz < Sector.CHUNKS_PER_SIDE; dz++) {
                serverLevel.getChunk(sector.minChunkX() + dx, sector.minChunkZ() + dz, ChunkStatus.FULL, true);
            }
        }
        RadarChartData.get(serverLevel.getServer()).chart(ChartOwners.teamOf(owner),
                serverLevel.dimension().identifier().toString(), sector);
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
        cursor = SWEEP.resume(input.getIntOr("Cursor", 0));
        owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Energy", energy.buffered());
        output.putLong("Progress", energy.progress());
        output.putInt("Cursor", cursor);
        output.storeNullable("Owner", UUIDUtil.CODEC, owner);
    }
}
