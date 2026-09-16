package com.planetaryfactory.core.energy;

/**
 * The pole's books.
 *
 * <p>FE in, FE out. ADR-0060 takes GregTech, Power Grid and Create out and leaves FE as the
 * pack's energy currency, so the FE-to-EU conversion this class used to be named for has no
 * second currency left to meet at this boundary. What remains is a buffer that respects a
 * capacity and does not invent energy.
 *
 * <p><b>EU has left the mod entirely as of #266.</b> {@link
 * com.planetaryfactory.core.smelting.FurnaceTier} states the Electric tier's draw in FE, derived
 * from the same {@code energy_usage} as before at ADR-0060's rate of 100 J to the FE, so both
 * sides of this boundary now count the same thing and nothing converts.
 *
 * <p>How big the books are allowed to get is not this class's business -- it takes a capacity and
 * respects it. {@code SupplyAreaPoleBlockEntity.BUFFER_FE} owns that number and the argument for it.
 *
 * <p>Not final, for one subclass: {@link InfiniteEnergyLedger}, the creative pole's books (#272).
 * Every method it overrides is one whose answer a full-forever buffer changes, and nothing here
 * branches on which of the two it is.
 *
 * <p>Pure: no Minecraft types, so the mod's Minecraft-free test source set can hold it to account.
 */
public class EnergyLedger {

    private final long capacityFe;
    private long storedFe;

    public EnergyLedger(long capacityFe) {
        this.capacityFe = Math.max(0L, capacityFe);
    }

    public long capacityFe() {
        return capacityFe;
    }

    public long storedFe() {
        return storedFe;
    }

    /** FE the pole could hand out right now. */
    public long availableFe() {
        return storedFe;
    }

    /** How much of an offered amount would be taken, without taking it. */
    public long simulateReceiveFe(long fe) {
        if (fe <= 0L) {
            return 0L;
        }
        return Math.min(fe, capacityFe - storedFe);
    }

    /** Takes what fits and reports it. */
    public long receiveFe(long fe) {
        long accepted = simulateReceiveFe(fe);
        storedFe += accepted;
        return accepted;
    }

    /** Hands out up to {@code maxFe}, debiting exactly that. */
    public long drainFe(long maxFe) {
        if (maxFe <= 0L) {
            return 0L;
        }
        long drained = Math.min(maxFe, storedFe);
        storedFe -= drained;
        return drained;
    }

    /** For the block entity's save data. */
    public void setStoredFe(long fe) {
        storedFe = Math.max(0L, Math.min(fe, capacityFe));
    }
}
