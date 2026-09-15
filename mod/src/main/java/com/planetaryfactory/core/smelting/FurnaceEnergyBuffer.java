package com.planetaryfactory.core.smelting;

/**
 * The Electric Furnace's FE buffer (#155, #266), which is the whole of what the supply-area pole
 * sees.
 *
 * <p>ADR-0036's pole water-fills across the machines in its area, reading each one's room and
 * paying it. Since ADR-0060 what moves is Forge Energy and nothing else: GregTech's
 * {@code IEnergyContainer} left with the mod, and the face the pole talks to is NeoForge's own
 * {@link net.neoforged.neoforge.transfer.energy.EnergyHandler}, held by the block entity so the
 * arithmetic here stays Minecraft-free.
 *
 * <p><b>Insertion is not restricted to the pole any more, and does not need to be.</b> The
 * ambiguity the old EU buffer refused a cable for -- two routes in, and no rule for which drains
 * first -- does not arise when both routes carry the same currency into the same buffer. What the
 * buffer still refuses is extraction: energy delivered to a furnace is spent there, never pulled
 * back out into the grid.
 *
 * <p>Pure: no Minecraft types.
 */
public final class FurnaceEnergyBuffer {

    private final long capacityFe;
    private long storedFe;

    public FurnaceEnergyBuffer(long capacityFe) {
        this.capacityFe = Math.max(0L, capacityFe);
    }

    public long getEnergyStored() {
        return storedFe;
    }

    public long getEnergyCapacity() {
        return capacityFe;
    }

    /** Room, which is what an insert is clamped to. */
    public long getEnergyCanBeInserted() {
        return capacityFe - storedFe;
    }

    /** Takes what fits and reports it, which is what the pole debits itself by. */
    public long addEnergy(long fe) {
        return changeEnergy(fe);
    }

    /** Moves the buffer either way, clamped, and reports what actually moved. */
    public long changeEnergy(long delta) {
        long before = storedFe;
        storedFe = Math.max(0L, Math.min(capacityFe, storedFe + delta));
        return storedFe - before;
    }

    /**
     * Pays for one tick of operation, all or nothing.
     *
     * <p>A partial tick is not a tick: spending 7 FE towards a 90 FE tick would make a
     * half-supplied furnace consume power and never finish.
     */
    public boolean drawTick(long fePerTick) {
        if (fePerTick <= 0L) {
            return true;
        }
        if (storedFe < fePerTick) {
            return false;
        }
        storedFe -= fePerTick;
        return true;
    }

    /** For the block entity's save data and for a transaction that was aborted. */
    public void setStoredFe(long fe) {
        storedFe = Math.max(0L, Math.min(capacityFe, fe));
    }
}
