package com.planetaryfactory.core.energy;

import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The pole block entities a client level has loaded, which is what the wire renderer links (#281).
 *
 * <p>The server's {@link ElectricNetworks} is not synced and does not need to be: a link is geometry
 * alone ({@link PoleLinks#linked}), so the client answers it from the poles it can see. Extensions
 * are kept here and filtered where they are read, because being a base is derived from the block
 * below and can change without the block entity reloading.
 *
 * <p>Holds no Minecraft client types, so the common block entity can report to it.
 */
public final class ClientPoles {

    private static final Map<Level, Set<SupplyAreaPoleBlockEntity>> BY_LEVEL = new WeakHashMap<>();

    private ClientPoles() {
    }

    static void add(SupplyAreaPoleBlockEntity pole) {
        BY_LEVEL.computeIfAbsent(pole.getLevel(), l -> new LinkedHashSet<>()).add(pole);
    }

    static void remove(SupplyAreaPoleBlockEntity pole) {
        Set<SupplyAreaPoleBlockEntity> poles = BY_LEVEL.get(pole.getLevel());
        if (poles != null) {
            poles.remove(pole);
            if (poles.isEmpty()) {
                BY_LEVEL.remove(pole.getLevel());
            }
        }
    }

    /**
     * Forget an unloaded level. A weak key is not enough: the poles held as values point back at
     * their level, and a disconnect need not remove each block entity first.
     */
    public static void onLevelUnload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            BY_LEVEL.remove(level);
        }
    }

    /** The poles loaded in this level, bases and extensions alike. */
    public static Set<SupplyAreaPoleBlockEntity> loadedIn(Level level) {
        Set<SupplyAreaPoleBlockEntity> poles = BY_LEVEL.get(level);
        return poles == null ? Set.of() : Collections.unmodifiableSet(poles);
    }
}
