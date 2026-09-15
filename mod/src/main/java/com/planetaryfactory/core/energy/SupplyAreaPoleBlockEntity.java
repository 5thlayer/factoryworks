package com.planetaryfactory.core.energy;

import com.planetaryfactory.core.PFBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The pole's tick: rescan the area now and then, push FE into everything found, every tick.
 *
 * <h2>FE in, FE out</h2>
 *
 * <p>ADR-0060 leaves the pack with one energy currency. GregTech is gone, so there is no EU to
 * convert to and no {@code IEnergyContainer} to insert into: a receiver is anything answering
 * NeoForge's own {@link Capabilities.Energy#BLOCK}, and the pole hands it FE.
 *
 * <p>Insertion runs inside a {@link Transaction} because that is the transfer API's contract --
 * what {@code insert} reports is only committed when the transaction is. The pole opens one per
 * tick and commits it, so a receiver that accepts less than it asked for costs the pole exactly
 * what the receiver took and nothing more.
 *
 * <h2>Why the receiver list is cached</h2>
 *
 * <p>A substation's area is 18x18x5 = 1620 blocks. A capability lookup per block per tick is not
 * affordable, and it is also pointless: machines do not appear and vanish every tick. The area is
 * rescanned on {@link #RESCAN_INTERVAL} and energy is pushed to the cached list in between, so a
 * newly placed machine waits at most two seconds to be picked up and the steady state costs one
 * lookup per actual receiver.
 */
public class SupplyAreaPoleBlockEntity extends BlockEntity {

    /** Ticks between rescans of the supply area. Two seconds. */
    private static final int RESCAN_INTERVAL = 40;

    /**
     * The FE buffer, sized at one tick of a busy area and derived rather than picked.
     *
     * <p>The figure is inherited from the EU ladder it was derived on: thirty-two machines at
     * 32 EU/t each, at the four-FE-to-one-EU ratio GregTech's converters used, came to 4,096 FE for
     * a single tick. The ratio is gone but the sizing argument is not -- it is still one tick of a
     * packed area -- and #266 is where the number is re-derived against whatever Oritech's
     * machines actually draw.
     *
     * <p>Sizing it that way is what keeps it clear of the machine-side storage ADR-0036 forbids.
     * That prohibition exists so sag and blown fuses reach the machines instead of being absorbed,
     * and a buffer holding one tick cannot absorb anything a player could perceive -- the moment
     * the grid gives less, the very next tick gives less. The buffer exists at all only because a
     * capability push and a block tick do not happen at the same instant.
     */
    private static final long BUFFER_FE = 4_096L;

    private final EnergyLedger ledger = new EnergyLedger(BUFFER_FE);
    private final PoleEnergyStorage feSide = new PoleEnergyStorage(this);

    private List<BlockPos> receivers = List.of();
    private int sinceRescan = RESCAN_INTERVAL;

    /**
     * What the last tick actually did, for the Jade line and for nothing else.
     *
     * <p>These are not persisted and not synced by the block entity: Jade asks the server for them
     * when a player looks at the pole, and a pole nobody is looking at has no reason to send them.
     * They exist because the two failure modes a player cannot otherwise tell apart -- a machine
     * outside the area, and a machine inside it that is not being fed -- are distinguished exactly
     * by the machine count and the delivered-against-demanded pair.
     */
    private int lastMachineCount;
    private long lastDeliveredFe;
    private long lastDemandedFe;

    public SupplyAreaPoleBlockEntity(BlockPos pos, BlockState state) {
        super(PFBlockEntities.SUPPLY_AREA_POLE.get(), pos, state);
    }

    public EnergyLedger ledger() {
        return ledger;
    }

    /** The FE face the grid mod's bridge block feeds. There is no separate boundary block. */
    public PoleEnergyStorage feSide() {
        return feSide;
    }

    public PoleTier tier() {
        // The block entity type is registered against the four pole blocks and nothing else, so
        // this cannot fail. Saying so loudly beats defaulting to SMALL, which would answer a
        // question wrongly rather than reveal that the registration had come apart.
        if (getBlockState().getBlock() instanceof SupplyAreaPoleBlock pole) {
            return pole.tier();
        }
        throw new IllegalStateException(
                "supply-area pole block entity on " + getBlockState().getBlock()
                        + " at " + getBlockPos() + ", which is not a pole");
    }

    void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        if (++sinceRescan >= RESCAN_INTERVAL) {
            sinceRescan = 0;
            receivers = scan(level);
        }
        if (receivers.isEmpty()) {
            lastMachineCount = 0;
            lastDeliveredFe = 0L;
            lastDemandedFe = 0L;
            return;
        }
        // Deliberately not short-circuited on an empty ledger. A pole with no energy still has to
        // measure what its area is asking for, because "0 of 120 EU/t" is the reading that tells a
        // player the machines are in range and the grid is not feeding them -- which is the whole
        // point of the Jade line. The cost is the capability lookups distribute() already does.
        distribute(level);
    }

    /** How many machines answered the last scan. Jade reads this; nothing else does. */
    public int machineCount() {
        return lastMachineCount;
    }

    /** FE actually handed out on the last tick. */
    public long deliveredFePerTick() {
        return lastDeliveredFe;
    }

    /** FE the area asked for on the last tick, whether or not it was there to give. */
    public long demandedFePerTick() {
        return lastDemandedFe;
    }

    /** Every position in the area that currently answers with an FE handler. */
    private List<BlockPos> scan(Level level) {
        List<BlockPos> found = new ArrayList<>();
        BlockPos origin = getBlockPos();
        SupplyArea.forEachOffset(tier(), (dx, dy, dz) -> {
            BlockPos pos = origin.offset(dx, dy, dz);
            if (pos.equals(origin) || !level.isLoaded(pos)) {
                return;
            }
            if (handler(level, pos) != null) {
                found.add(pos.immutable());
            }
        });
        return found;
    }

    /** A machine that answered this tick, and the room it has. */
    private record Receiver(EnergyHandler handler, int demand) {
    }

    private void distribute(Level level) {
        List<Receiver> hungry = new ArrayList<>(receivers.size());
        try (Transaction probe = Transaction.open(null)) {
            for (BlockPos pos : receivers) {
                EnergyHandler handler = handler(level, pos);
                if (handler == null) {
                    continue;
                }
                // The transfer API has no "how much room is there" question, so room is a simulated
                // insert: an insert inside a transaction that is then aborted. Asking for the
                // capacity instead would read a full machine as hungry for everything it holds.
                int demand = handler.insert(Integer.MAX_VALUE, probe);
                if (demand > 0) {
                    hungry.add(new Receiver(handler, demand));
                }
            }
        }
        // The count is every machine the scan found, not just the hungry ones. A machine sitting
        // full has no demand this tick, and dropping it from the count would report "0 machines"
        // to a player standing in front of a working factory -- the precise misreading this line
        // exists to prevent.
        lastMachineCount = receivers.size();
        if (hungry.isEmpty()) {
            lastDeliveredFe = 0L;
            lastDemandedFe = 0L;
            return;
        }

        long[] demands = hungry.stream().mapToLong(Receiver::demand).toArray();
        long[] grants = EnergyShare.waterFill(ledger.availableFe(), demands);

        long wanted = 0L;
        for (long demand : demands) {
            wanted += demand;
        }
        long spent = 0L;
        try (Transaction transaction = Transaction.open(null)) {
            for (int i = 0; i < grants.length; i++) {
                if (grants[i] > 0L) {
                    spent += hungry.get(i).handler().insert((int) grants[i], transaction);
                }
            }
            transaction.commit();
        }
        lastDemandedFe = wanted;
        lastDeliveredFe = spent;
        if (spent > 0L) {
            ledger.drainFe(spent);
            setChanged();
        }
    }

    private static EnergyHandler handler(Level level, BlockPos pos) {
        // A pole supplies wirelessly, so it has no natural side to ask through. Most machines
        // answer on a null context; the faces are a fallback for anything that insists on one.
        EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, pos, null);
        if (handler != null) {
            return handler;
        }
        for (Direction side : Direction.values()) {
            handler = level.getCapability(Capabilities.Energy.BLOCK, pos, side);
            if (handler != null) {
                return handler;
            }
        }
        return null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ledger.setStoredFe(input.getLongOr("StoredFe", 0L));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("StoredFe", ledger.storedFe());
    }
}
