package com.planetaryfactory.core.ore;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What has been taken out of each outfield patch, for its map marker (#370, #371). The blocks stay
 * the truth of what a block holds (ADR-0041); this is the patch's sum, kept as they change so the
 * map never has to load a disc's chunks to read it. An untouched patch has no entry.
 */
public final class PatchLedger {

    private static final Codec<Entry> ENTRY = RecordCodecBuilder.create(instance -> instance.group(
            PatchId.CODEC.fieldOf("patch").forGetter(entry -> entry.patch),
            Codec.LONG.fieldOf("drawn").forGetter(entry -> entry.drawn),
            Codec.INT.fieldOf("gone").forGetter(entry -> entry.gone),
            Codec.BOOL.fieldOf("exhausted").forGetter(entry -> entry.exhausted))
            .apply(instance, Entry::new));

    public static final Codec<PatchLedger> CODEC = ENTRY.listOf().xmap(PatchLedger::decode, PatchLedger::encode);

    private final Map<PatchId, Entry> entries = new HashMap<>();

    private static final class Entry {
        private final PatchId patch;
        private long drawn;
        private int gone;
        private boolean exhausted;

        private Entry(PatchId patch, long drawn, int gone, boolean exhausted) {
            this.patch = patch;
            this.drawn = drawn;
            this.gone = gone;
            this.exhausted = exhausted;
        }
    }

    /** One unit drawn from one of the patch's blocks. */
    public void drew(PatchId patch) {
        entry(patch).drawn++;
    }

    /**
     * One of the patch's blocks is gone, holding {@code unitsLost} undrawn: none when mined out,
     * the rest when blown up or broken in creative. True once, when this was the last block.
     */
    public boolean blockGone(PatchId patch, int unitsLost, int blockCount) {
        Entry entry = entry(patch);
        if (entry.exhausted) {
            return false;
        }
        entry.drawn += unitsLost;
        entry.gone++;
        entry.exhausted = entry.gone >= blockCount;
        return entry.exhausted;
    }

    public boolean isExhausted(PatchId patch) {
        Entry entry = entries.get(patch);
        return entry != null && entry.exhausted;
    }

    public long remaining(PatchId patch, long total) {
        Entry entry = entries.get(patch);
        if (entry == null) {
            return total;
        }
        return entry.exhausted ? 0 : Math.max(0, total - entry.drawn);
    }

    private Entry entry(PatchId patch) {
        return entries.computeIfAbsent(patch, p -> new Entry(p, 0, 0, false));
    }

    private static PatchLedger decode(List<Entry> decoded) {
        PatchLedger ledger = new PatchLedger();
        decoded.forEach(entry -> ledger.entries.put(entry.patch, entry));
        return ledger;
    }

    private List<Entry> encode() {
        return new ArrayList<>(entries.values());
    }
}
