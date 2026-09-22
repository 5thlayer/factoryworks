package com.planetaryfactory.core.radar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The outfield patches each player's map has had drawn by walking (#370, ADR-0079). */
class WalkedPatchesTest {

    private static final UUID ALICE = UUID.fromString("6f1b1e5e-0000-4000-8000-0000000000a1");
    private static final String OVERWORLD = "minecraft:overworld";
    private static final PatchMarker IRON = new PatchMarker("iron", 8, 70, 8, 1_000);
    private static final PatchMarker COAL = new PatchMarker("coal", 40, 70, 8, 2_000);

    @Test
    void addingAnswersOnlyWhatIsNew() {
        WalkedPatches walked = new WalkedPatches();

        assertEquals(List.of(IRON), walked.add(ALICE, OVERWORLD, List.of(IRON)));
        assertEquals(List.of(COAL), walked.add(ALICE, OVERWORLD, List.of(IRON, COAL)));
        assertEquals(Set.of(IRON, COAL), walked.of(ALICE, OVERWORLD));
        assertEquals(Set.of(), walked.of(ALICE, "minecraft:the_nether"));
    }

    @Test
    void theRecordSurvivesItsCodec() {
        WalkedPatches walked = new WalkedPatches();
        walked.add(ALICE, OVERWORLD, List.of(IRON, COAL));

        JsonElement json = WalkedPatches.CODEC.encodeStart(JsonOps.INSTANCE, walked).getOrThrow();
        WalkedPatches back = WalkedPatches.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        assertEquals(Set.of(IRON, COAL), back.of(ALICE, OVERWORLD));
    }
}
