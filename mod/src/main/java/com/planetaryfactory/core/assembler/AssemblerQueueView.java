package com.planetaryfactory.core.assembler;

import com.planetaryfactory.core.network.QueueSyncPacket;
import java.util.List;

/**
 * The client's copy of the queue: what the last sync said, and nothing more.
 *
 * <p>A read-only picture, deliberately. The client never holds a plan, never computes a
 * reservation and never decides anything -- it draws rows and sends button presses, and every answer
 * comes back over the wire.
 *
 * <p>Free of {@code net.minecraft.client} on purpose, so a dedicated server can load the packet's
 * handler class without reaching for a class that is not there.
 */
public final class AssemblerQueueView {

    private static volatile QueueSyncPacket latest = new QueueSyncPacket(List.of(), false);

    /** One server tick, and how far past a sync the view will extrapolate: one sync interval. */
    private static final double NANOS_PER_TICK = 50_000_000.0;
    private static final int EXTRAPOLATE_TICKS = 5;

    private static volatile long receivedAt = System.nanoTime();

    private AssemblerQueueView() {
    }

    public static void accept(QueueSyncPacket packet) {
        latest = packet;
        receivedAt = System.nanoTime();
    }

    public static List<QueueSyncPacket.Entry> entries() {
        return latest.entries();
    }

    /**
     * The head's progress as of this frame rather than as of the last sync.
     *
     * <p>Syncs arrive four times a second, which draws the clock hand in visible jumps. Between them
     * the view runs the craft forward at its own rate, capped at one sync interval so a stalled server
     * does not run the hand away, and not at all while the queue is paused.
     */
    public static float liveProgress(QueueSyncPacket.Entry entry) {
        if (latest.blocked()) return entry.progress();
        double ticks = Math.min(EXTRAPOLATE_TICKS, (System.nanoTime() - receivedAt) / NANOS_PER_TICK);
        return Math.min(1.0f, entry.progress() + (float) (ticks * entry.progressPerTick()));
    }

    public static boolean blocked() {
        return latest.blocked();
    }
}
