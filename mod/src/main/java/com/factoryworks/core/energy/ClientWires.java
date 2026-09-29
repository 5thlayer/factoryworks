package com.factoryworks.core.energy;

import java.util.List;

/**
 * The wires a client has been sent, which is what the wire renderer draws (ADR-0068).
 *
 * <p>One set for the one level a client has open, emptied when that level unloads. Holds no
 * Minecraft client types, so the common packet can hand to it.
 */
public final class ClientWires {

    private static WireSet wires = new WireSet();

    private ClientWires() {
    }

    public static void accept(int chunkX, int chunkZ, List<PoleLinks.Wire> sent) {
        wires.replaceTouching(chunkX, chunkZ, sent);
    }

    public static WireSet wires() {
        return wires;
    }

    public static void onLevelUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            wires = new WireSet();
        }
    }
}
