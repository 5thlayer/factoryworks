package com.planetaryfactory.core.radar;

/**
 * A Radar's buffer and its progress towards the next long-range sector and the next nearby pulse
 * (#368, ADR-0079).
 *
 * <p>The buffer holds one tick's draw, which is what a pole fills. Each tick moves what is there,
 * up to the full draw, into both counters, so an underfed Radar charts proportionally slower, as
 * Factorio's does, rather than stalling. Both count the same energy: Factorio's long-range sector
 * still takes 33.3 s with the nearby area pulsing every 0.83 s.
 */
public final class RadarEnergy {

    public record Scans(boolean nearby, boolean sector) {
    }

    private final RadarSpec spec;
    private long buffered;
    private long progress;
    private long nearbyProgress;

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

    public long nearbyProgress() {
        return nearbyProgress;
    }

    /** Takes what fits and reports it. */
    public long insert(long fe) {
        long taken = Math.min(capacity() - buffered, Math.max(0L, fe));
        buffered += taken;
        return taken;
    }

    /** One tick of scanning, and which scans fell due on it. */
    public Scans tick() {
        long drawn = Math.min(buffered, spec.fePerTick());
        buffered -= drawn;
        progress += drawn;
        nearbyProgress += drawn;
        boolean nearby = nearbyProgress >= spec.fePerNearbyScan();
        if (nearby) {
            nearbyProgress -= spec.fePerNearbyScan();
        }
        boolean sector = progress >= spec.fePerSector();
        if (sector) {
            progress -= spec.fePerSector();
        }
        return new Scans(nearby, sector);
    }

    public void setBuffered(long fe) {
        buffered = Math.max(0L, Math.min(capacity(), fe));
    }

    public void setProgress(long fe) {
        progress = Math.max(0L, fe);
    }

    public void setNearbyProgress(long fe) {
        nearbyProgress = Math.max(0L, fe);
    }
}
