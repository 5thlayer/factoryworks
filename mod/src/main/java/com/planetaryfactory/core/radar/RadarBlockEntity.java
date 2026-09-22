package com.planetaryfactory.core.radar;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Predicate;

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
 * The Radar (#368, ADR-0079): draws power, pulses its nearby area into its owner's team's chart
 * every 250 kJ, and charts one long-range sector per 10 MJ, unexplored sectors first.
 */
public class RadarBlockEntity extends BlockEntity {

    private static final RadarSpec SPEC = RadarSpec.fromCorpus();
    private static final RadarSweep SWEEP = RadarSweep.of(SPEC.nearReach(), SPEC.reach());

    private final RadarEnergy energy = new RadarEnergy(SPEC);
    private final LongSnapshotJournal journal =
            new LongSnapshotJournal(energy::buffered, energy::setBuffered, this::setChanged);
    /** A pulse's uncharted sectors, charted one a tick so a pulse never generates 196 chunks at once (ADR-0079). */
    private final Queue<Sector> pulse = new ArrayDeque<>();
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

    public int sweepSize() {
        return SWEEP.size();
    }

    /** Where in the long range the next sector falls, which is unexplored ground while any is left. */
    public int nextIndex(ServerLevel serverLevel) {
        return SWEEP.pick(origin(), charted(serverLevel), cursor);
    }

    public Sector nextSector(ServerLevel serverLevel) {
        return SWEEP.sectorAt(origin(), nextIndex(serverLevel));
    }

    private Sector origin() {
        return Sector.ofBlock(worldPosition.getX(), worldPosition.getZ());
    }

    private Predicate<Sector> charted(ServerLevel serverLevel) {
        if (owner == null) {
            return sector -> false;
        }
        RadarChartData data = RadarChartData.get(serverLevel.getServer());
        UUID team = ChartOwners.teamOf(owner);
        String dimension = serverLevel.dimension().identifier().toString();
        return sector -> data.isCharted(team, dimension, sector);
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        long before = energy.progress();
        RadarEnergy.Scans scans = energy.tick();
        if (scans.nearby() && pulse.isEmpty() && owner != null) {
            Predicate<Sector> charted = charted(serverLevel);
            SWEEP.nearby(origin()).stream().filter(charted.negate()).forEach(pulse::add);
        }
        if (!pulse.isEmpty()) {
            chart(serverLevel, pulse.poll());
        }
        if (scans.sector()) {
            int index = nextIndex(serverLevel);
            chart(serverLevel, SWEEP.sectorAt(origin(), index));
            if (index == cursor) {
                cursor = SWEEP.next(cursor);
            }
        }
        if (energy.progress() != before) {
            setChanged();
        }
    }

    /**
     * An unowned Radar, one placed other than by a player, charts for no team and generates nothing.
     * A sector already charted is not loaded again: nothing re-sends it, so a re-scan would only
     * load four chunks for no change on any map (ADR-0079).
     */
    private void chart(ServerLevel serverLevel, Sector sector) {
        if (owner == null || charted(serverLevel).test(sector)) {
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
        energy.setNearbyProgress(input.getLongOr("NearbyProgress", 0L));
        cursor = SWEEP.resume(input.getIntOr("Cursor", 0));
        owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("Energy", energy.buffered());
        output.putLong("Progress", energy.progress());
        output.putLong("NearbyProgress", energy.nearbyProgress());
        output.putInt("Cursor", cursor);
        output.storeNullable("Owner", UUIDUtil.CODEC, owner);
    }
}
