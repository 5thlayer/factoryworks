package com.planetaryfactory.core.ore;

import com.planetaryfactory.core.PlanetaryFactoryCore;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** The outfield patches' ledger, saved with the world on the overworld's data storage (#370, #371). */
public final class PatchLedgerData extends SavedData {

    public static final SavedDataType<PatchLedgerData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(PlanetaryFactoryCore.NAMESPACE, "patch_ledger"),
            PatchLedgerData::new,
            PatchLedger.CODEC.xmap(PatchLedgerData::new, data -> data.ledger));

    private final PatchLedger ledger;

    public PatchLedgerData() {
        this(new PatchLedger());
    }

    private PatchLedgerData(PatchLedger ledger) {
        this.ledger = ledger;
    }

    public static PatchLedgerData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void drew(PatchId patch) {
        ledger.drew(patch);
        setDirty();
    }

    public boolean blockGone(PatchId patch, int unitsLost, int blockCount) {
        boolean exhausted = ledger.blockGone(patch, unitsLost, blockCount);
        setDirty();
        return exhausted;
    }

    public long remaining(PatchId patch, long total) {
        return ledger.remaining(patch, total);
    }
}
