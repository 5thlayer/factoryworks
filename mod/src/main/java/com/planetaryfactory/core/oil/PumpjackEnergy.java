package com.planetaryfactory.core.oil;

import com.planetaryfactory.core.energy.ForgeEnergy;

/**
 * A Pumpjack's buffer and its progress towards the next cycle (ADR-0081).
 *
 * <p>The drain is paid every tick, working or not, and a drain the buffer cannot pay is not owed.
 * Working moves what is left, up to the full draw, into progress, so an underfed Pumpjack cycles
 * proportionally slower, as the Radar does.
 */
public final class PumpjackEnergy {

    private final PumpjackSpec spec;
    private long buffered;
    private long progress;
    private long drainJoules;

    public PumpjackEnergy(PumpjackSpec spec) {
        this.spec = spec;
    }

    public long capacity() {
        return spec.fePerTick() + Math.ceilDiv(spec.drainJoulesPerTick(), ForgeEnergy.JOULES_PER_FE);
    }

    public long buffered() {
        return buffered;
    }

    public long progress() {
        return progress;
    }

    public long insert(long fe) {
        long taken = Math.min(capacity() - buffered, Math.max(0L, fe));
        buffered += taken;
        return taken;
    }

    /** One tick; whether a cycle fell due on it. */
    public boolean tick(boolean working) {
        drainJoules += spec.drainJoulesPerTick();
        long due = drainJoules / ForgeEnergy.JOULES_PER_FE;
        drainJoules -= due * ForgeEnergy.JOULES_PER_FE;
        buffered -= Math.min(buffered, due);
        if (!working) {
            return false;
        }
        long drawn = Math.min(buffered, spec.fePerTick());
        buffered -= drawn;
        progress += drawn;
        if (progress < spec.fePerCycle()) {
            return false;
        }
        progress -= spec.fePerCycle();
        return true;
    }

    public void setBuffered(long fe) {
        buffered = Math.max(0L, Math.min(capacity(), fe));
    }

    public void setProgress(long fe) {
        progress = Math.max(0L, fe);
    }
}
