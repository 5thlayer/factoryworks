package com.planetaryfactory.core.machine.client;

import com.planetaryfactory.core.PFBlockEntities;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import rearth.oritech.client.renderers.blocks.MachineRenderer;

/**
 * The Assembling Machine's renderer (#326): Oritech's own, pointed at Oritech's assembler model.
 *
 * <p>{@code "models/assembler"} and {@code false} are the arguments Oritech's {@code ModRenderers}
 * registers its assembler with, read off the 2.0.0-exp6 jar. The model and textures resolve into
 * Oritech's jar, so the pack ships no art for the machine.
 */
public final class AssemblingMachineClient {

    private AssemblingMachineClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(AssemblingMachineClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PFBlockEntities.ASSEMBLING_MACHINE.get(),
                context -> new MachineRenderer<>(context, "models/assembler", false));
    }
}
