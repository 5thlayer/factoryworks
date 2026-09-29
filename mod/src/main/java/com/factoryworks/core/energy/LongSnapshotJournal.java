package com.factoryworks.core.energy;

import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;

/**
 * An energy buffer's undo, spelled once.
 *
 * <p>NeoForge's transfer API lets a caller open a transaction, insert, and then abort it, which is
 * how ADR-0036's pole measures a machine's room -- so every energy face in the mod has to be able
 * to put a stored amount back. Each of them holds the same long, so each of them wants the same
 * journal: read it before the mutation, write it back on an abort, and mark the block entity dirty
 * once the outermost transaction commits.
 *
 * <p>Spelled once for the reason {@code core/transfer/GuardedResourceHandler} is: a second copy is
 * a place for one of the faces to quietly stop taking its snapshot, and a face that skips the
 * snapshot does not fail -- it keeps the probe's worth of energy the pole never meant to hand it.
 */
public final class LongSnapshotJournal extends SnapshotJournal<Long> {

    private final LongSupplier read;
    private final LongConsumer write;
    private final Runnable onChange;

    public LongSnapshotJournal(LongSupplier read, LongConsumer write, Runnable onChange) {
        this.read = read;
        this.write = write;
        this.onChange = onChange;
    }

    @Override
    protected Long createSnapshot() {
        return read.getAsLong();
    }

    @Override
    protected void revertToSnapshot(Long snapshot) {
        write.accept(snapshot);
    }

    @Override
    protected void onRootCommit(Long originalState) {
        if (read.getAsLong() != originalState) {
            onChange.run();
        }
    }
}
