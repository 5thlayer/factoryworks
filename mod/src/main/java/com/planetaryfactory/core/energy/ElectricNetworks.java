package com.planetaryfactory.core.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Every Electric Network in a level, and the tick that settles them (ADR-0062).
 *
 * <h2>Networks follow the stored wires</h2>
 *
 * <p>A pole reports itself every tick it runs. A pole that did not report -- broken, unloaded, or
 * turned into a column extension -- is dropped at the level tick. Any change to the set of poles,
 * or a wire made or cut ({@link LevelWires}), rebuilds the networks from the wires between the poles
 * standing (ADR-0068, superseding ADR-0062's "no stored topology"). A merge and a split are still
 * the same recomputation.
 *
 * <h2>One settlement per network</h2>
 *
 * <p>A block inside two linked poles' areas is one consumer, not two: the member poles' lists are
 * unioned by position before anything is probed. Room and offer are each measured with an insert or
 * extract inside a transaction that is then aborted, since the transfer API has no "how much"
 * question, and {@link NetworkBalance} decides the flows. Sources are weighted by what they offer
 * this tick; for a generator whose face caps extraction at its rated output, that is its maximum
 * output (#282 owns checking the Steam Engine's face does).
 */
public final class ElectricNetworks {

    private static final Map<Level, ElectricNetworks> BY_LEVEL = new WeakHashMap<>();

    /**
     * An offer large enough to cover any area, and small enough to sum without overflow. It also
     * charges accumulators from nothing, which is what a creative pole is for.
     *
     * <p>Every draw, grant and charge is bounded by a probe of {@code Integer.MAX_VALUE} or by this,
     * which is what makes the {@code int} casts in {@link #insert} and {@link #extract} safe.
     */
    private static final long CREATIVE_OFFER = Integer.MAX_VALUE;

    private final Map<BlockPos, SupplyAreaPoleBlockEntity> poles = new LinkedHashMap<>();
    private final Map<BlockPos, Long> lastReport = new LinkedHashMap<>();
    private List<List<SupplyAreaPoleBlockEntity>> networks = List.of();
    private Map<BlockPos, Long> accumulatorFlows = Map.of();
    private boolean dirty;

    private ElectricNetworks() {
    }

    public static ElectricNetworks of(Level level) {
        return BY_LEVEL.computeIfAbsent(level, l -> new ElectricNetworks());
    }

    void report(SupplyAreaPoleBlockEntity pole) {
        BlockPos pos = pole.getBlockPos();
        if (poles.put(pos, pole) != pole) {
            dirty = true;
        }
        lastReport.put(pos, pole.getLevel().getGameTime());
    }

    /** A wire was made or cut: the networks are rebuilt on the next level tick. */
    void wiresChanged() {
        dirty = true;
    }

    /** The poles linked into the same network as this one, itself included. */
    public List<SupplyAreaPoleBlockEntity> networkOf(SupplyAreaPoleBlockEntity pole) {
        for (List<SupplyAreaPoleBlockEntity> network : networks) {
            if (network.contains(pole)) {
                return network;
            }
        }
        return List.of();
    }

    public boolean drawsFrom(BlockPos generator) {
        for (SupplyAreaPoleBlockEntity pole : poles.values()) {
            if (pole.generators().contains(generator)) {
                return true;
            }
        }
        return false;
    }

    public boolean chargesFrom(BlockPos accumulator) {
        for (SupplyAreaPoleBlockEntity pole : poles.values()) {
            if (pole.accumulators().contains(accumulator)) {
                return true;
            }
        }
        return false;
    }

    /** FE the accumulator took last tick, negative when it gave. */
    public long accumulatorFlow(BlockPos accumulator) {
        return accumulatorFlows.getOrDefault(accumulator, 0L);
    }

    /**
     * Forget an unloaded level. A weak key is not enough: the poles held as values point back at
     * their level, so the key would never be collected.
     */
    public static void onLevelUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            BY_LEVEL.remove(level);
        }
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        ElectricNetworks networks = BY_LEVEL.get(level);
        if (networks != null) {
            networks.tick(level);
        }
    }

    private void tick(Level level) {
        long now = level.getGameTime();
        // A report from this tick or the last one counts: block entities tick inside the level tick,
        // and which side of the clock increment they land on is not this class's to assume.
        var stale = lastReport.entrySet().iterator();
        while (stale.hasNext()) {
            var entry = stale.next();
            SupplyAreaPoleBlockEntity pole = poles.get(entry.getKey());
            if (entry.getValue() < now - 1 || pole.isRemoved()) {
                poles.remove(entry.getKey());
                stale.remove();
                dirty = true;
            }
        }
        if (dirty) {
            rebuild(level);
            dirty = false;
        }
        Map<BlockPos, Long> flows = new LinkedHashMap<>();
        for (List<SupplyAreaPoleBlockEntity> network : networks) {
            settle(level, network, flows);
        }
        accumulatorFlows = flows;
    }

    private void rebuild(Level level) {
        List<SupplyAreaPoleBlockEntity> all = new ArrayList<>(poles.values());
        List<PoleLinks.Pole> shapes = new ArrayList<>(all.size());
        for (SupplyAreaPoleBlockEntity pole : all) {
            shapes.add(pole.shape());
        }
        int[] ids = PoleLinks.networks(shapes, LevelWires.of((net.minecraft.server.level.ServerLevel) level).wires().all());
        List<List<SupplyAreaPoleBlockEntity>> built = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            while (built.size() <= ids[i]) {
                built.add(new ArrayList<>());
            }
            built.get(ids[i]).add(all.get(i));
        }
        networks = built;
    }

    private static void settle(Level level, List<SupplyAreaPoleBlockEntity> network,
            Map<BlockPos, Long> accumulatorFlows) {
        Set<BlockPos> generatorPositions = new LinkedHashSet<>();
        Set<BlockPos> accumulatorPositions = new LinkedHashSet<>();
        Set<BlockPos> consumerPositions = new LinkedHashSet<>();
        int creative = 0;
        for (SupplyAreaPoleBlockEntity pole : network) {
            generatorPositions.addAll(pole.generators());
            accumulatorPositions.addAll(pole.accumulators());
            consumerPositions.addAll(pole.consumers());
            if (pole.isCreative()) {
                creative++;
            }
        }

        List<EnergyHandler> generators = handlers(level, generatorPositions);
        List<BlockPos> accumulatorsAt = new ArrayList<>(accumulatorPositions.size());
        List<EnergyHandler> accumulators = handlers(level, accumulatorPositions, accumulatorsAt);
        List<EnergyHandler> consumers = handlers(level, consumerPositions);

        // The creative poles lead the generator array as generators with no handler.
        long[] generatorOffers = new long[creative + generators.size()];
        for (int i = 0; i < creative; i++) {
            generatorOffers[i] = CREATIVE_OFFER;
        }
        for (int i = 0; i < generators.size(); i++) {
            generatorOffers[creative + i] = probeExtract(generators.get(i));
        }
        long[] accumulatorOffers = new long[accumulators.size()];
        long[] accumulatorRooms = new long[accumulators.size()];
        for (int i = 0; i < accumulators.size(); i++) {
            accumulatorOffers[i] = probeExtract(accumulators.get(i));
            accumulatorRooms[i] = probeInsert(accumulators.get(i));
        }
        long[] demands = new long[consumers.size()];
        long demanded = 0L;
        for (int i = 0; i < consumers.size(); i++) {
            demands[i] = probeInsert(consumers.get(i));
            demanded += demands[i];
        }

        NetworkBalance.Settlement plan = NetworkBalance.settle(
                generatorOffers, accumulatorOffers, accumulatorRooms, demands);

        // Receivers first, sources second, all in one transaction. What is extracted is exactly
        // what was accepted, so a receiver that takes less than its grant costs its sources the
        // difference rather than destroying it. A source that gives less than its probe promised
        // cannot cover the tick, and the whole tick is aborted rather than creating energy.
        long delivered = 0L;
        long produced = 0L;
        long charged = 0L;
        long discharged = 0L;
        long[] accumulatorFlow = new long[accumulators.size()];
        try (Transaction transaction = Transaction.open(null)) {
            for (int i = 0; i < consumers.size(); i++) {
                delivered += insert(consumers.get(i), plan.consumerGrants()[i], transaction);
            }
            for (int i = 0; i < accumulators.size(); i++) {
                accumulatorFlow[i] = insert(accumulators.get(i), plan.accumulatorCharges()[i], transaction);
                charged += accumulatorFlow[i];
            }
            long owed = delivered + charged;
            for (int i = 0; i < creative && owed > 0L; i++) {
                long drawn = Math.min(owed, plan.generatorDraws()[i]);
                produced += drawn;
                owed -= drawn;
            }
            for (int i = 0; i < generators.size() && owed > 0L; i++) {
                long drawn = extract(generators.get(i),
                        Math.min(owed, plan.generatorDraws()[creative + i]), transaction);
                produced += drawn;
                owed -= drawn;
            }
            for (int i = 0; i < accumulators.size() && owed > 0L; i++) {
                long drawn = extract(accumulators.get(i),
                        Math.min(owed, plan.accumulatorDischarges()[i]), transaction);
                accumulatorFlow[i] -= drawn;
                discharged += drawn;
                owed -= drawn;
            }
            if (owed == 0L) {
                transaction.commit();
            } else {
                delivered = produced = charged = discharged = 0L;
                accumulatorFlow = new long[accumulators.size()];
            }
        }

        // Read after the transaction closes, so an aborted tick reads what is really stored.
        long stored = 0L;
        long capacity = 0L;
        for (EnergyHandler accumulator : accumulators) {
            stored += accumulator.getAmountAsLong();
            capacity += accumulator.getCapacityAsLong();
        }
        for (int i = 0; i < accumulators.size(); i++) {
            accumulatorFlows.put(accumulatorsAt.get(i), accumulatorFlow[i]);
        }
        NetworkReading reading = new NetworkReading(produced, delivered, demanded, charged,
                discharged, stored, capacity, accumulators.size(), network.size());
        for (SupplyAreaPoleBlockEntity pole : network) {
            pole.recordNetworkTick(reading);
        }
    }

    private static List<EnergyHandler> handlers(Level level, Set<BlockPos> positions) {
        return handlers(level, positions, new ArrayList<>());
    }

    /** @param at filled with each found handler's position, index for index */
    private static List<EnergyHandler> handlers(Level level, Set<BlockPos> positions, List<BlockPos> at) {
        List<EnergyHandler> found = new ArrayList<>(positions.size());
        for (BlockPos pos : positions) {
            EnergyHandler handler = SupplyAreaPoleBlockEntity.handler(level, pos);
            if (handler != null) {
                found.add(handler);
                at.add(pos);
            }
        }
        return found;
    }

    private static long probeInsert(EnergyHandler handler) {
        try (Transaction probe = Transaction.open(null)) {
            return handler.insert(Integer.MAX_VALUE, probe);
        }
    }

    private static long probeExtract(EnergyHandler handler) {
        try (Transaction probe = Transaction.open(null)) {
            return handler.extract(Integer.MAX_VALUE, probe);
        }
    }

    private static long insert(EnergyHandler handler, long amount, Transaction transaction) {
        return amount <= 0L ? 0L : handler.insert((int) amount, transaction);
    }

    private static long extract(EnergyHandler handler, long amount, Transaction transaction) {
        return amount <= 0L ? 0L : handler.extract((int) amount, transaction);
    }
}
