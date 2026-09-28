package com.planetaryfactory.core;

import com.planetaryfactory.core.ore.OreCodecs;
import com.planetaryfactory.core.ore.OreDelta;
import com.planetaryfactory.core.start.StartingCodecs;
import com.planetaryfactory.core.start.StartingGrant;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;

public final class PFAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, PlanetaryFactoryCore.NAMESPACE);

    /**
     * What has already been drawn out of the ore blocks in one chunk (ADR-0041).
     *
     * <p>A chunk attachment rather than a block entity per ore block: a starting field is around
     * 1150 blocks, and the delta is sparse -- an untouched field costs nothing and the entries
     * unload with their chunk. No {@code copyOnDeath}, because a chunk does not die.
     */
    public static final Supplier<AttachmentType<OreDelta>> ORE_DELTA = ATTACHMENTS.register(
            "ore_delta",
            () -> AttachmentType.builder(OreDelta::new)
                    .serialize(OreCodecs.DELTA.fieldOf("delta"))
                    .build());

    /**
     * Whether this player has already been handed the starting kit (#203).
     *
     * <p>{@code copyOnDeath} because dying is not a reason to be handed a second one -- without it
     * the kit costs one death, which is the cheapest thing to do in the opening. Why the flag is
     * persisted at all is on {@link StartingGrant}.
     */
    public static final Supplier<AttachmentType<StartingGrant>> STARTING_GRANT = ATTACHMENTS.register(
            "starting_grant",
            // The cast picks the Supplier overload: StartingGrant's boolean constructor makes the
            // bare method reference ambiguous against builder(Function<IAttachmentHolder, T>).
            () -> AttachmentType.builder((Supplier<StartingGrant>) StartingGrant::new)
                    .serialize(StartingCodecs.GRANT.fieldOf("grant"))
                    .copyOnDeath()
                    .build());

    private PFAttachments() {
    }

    static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
