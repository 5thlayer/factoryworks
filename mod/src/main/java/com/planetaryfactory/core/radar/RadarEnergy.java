package com.planetaryfactory.core.radar;

/**
 * A Radar's buffer and its progress towards the next sector (#368).
 *
 * <p>The buffer holds one tick's draw, which is what a pole fills. Each tick moves what is there,
 * up to the full draw, into progress, so an underfed Radar charts proportionally slower, as
 * Factorio's does, rather than stalling.
 */
public final class RadarEnergy {

    private final RadarSpec spec;
    private long buffered;
    private long progress;

    public RadarEnergy(RadarSpec spec) {
        this.spec = spec;
    }

    public long capacity() {
        return spec.fePerTick();
    }

    public long buffered() {
        return buffered;
    }

    public long progress() {
        return progress;
    }

    /** Takes what fits and reports it. */
    public long insert(long fe) {
        long taken = Math.min(capacity() - buffered, Math.max(0L, fe));
        buffered += taken;
        return taken;
    }

    /** One tick of scanning; the number of sectors that fell due, which is 0 or 1. */
    public int tick() {
        long drawn = Math.min(buffered, spec.fePerTick());
        buffered -= drawn;
        progress += drawn;
        if (progress < spec.fePerSector()) {
            return 0;
        }
        progress -= spec.fePerSector();
        return 1;
    }

    public void setBuffered(long fe) {
        buffered = Math.max(0L, Math.min(capacity(), fe));
    }

    public void setProgress(long fe) {
        progress = Math.max(0L, fe);
    }
}
