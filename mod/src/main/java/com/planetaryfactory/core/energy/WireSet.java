package com.planetaryfactory.core.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Every wire in a level (ADR-0068), stored once each, as a pair of pole bases.
 *
 * <p>Pure: DataFixerUpper's codec, no Minecraft types.
 */
public final class WireSet {

    private static final Codec<PoleLinks.Pos> POS = Codec.INT.listOf(3, 3).xmap(
            l -> new PoleLinks.Pos(l.get(0), l.get(1), l.get(2)),
            p -> List.of(p.x(), p.y(), p.z()));

    private static final Codec<PoleLinks.Wire> WIRE = RecordCodecBuilder.create(i -> i.group(
            POS.fieldOf("a").forGetter(PoleLinks.Wire::a),
            POS.fieldOf("b").forGetter(PoleLinks.Wire::b)
    ).apply(i, PoleLinks.Wire::new));

    public static final Codec<WireSet> CODEC = WIRE.listOf().xmap(
            list -> {
                WireSet set = new WireSet();
                list.forEach(w -> set.add(w.a(), w.b()));
                return set;
            },
            set -> List.copyOf(set.wires));

    private final Set<PoleLinks.Wire> wires = new LinkedHashSet<>();

    public void add(PoleLinks.Pos a, PoleLinks.Pos b) {
        wires.add(pair(a, b));
    }

    public void remove(PoleLinks.Pos a, PoleLinks.Pos b) {
        wires.remove(pair(a, b));
    }

    public boolean contains(PoleLinks.Pos a, PoleLinks.Pos b) {
        return wires.contains(pair(a, b));
    }

    /** Cuts every wire with an end at {@code pole}, which is what breaking it does. */
    public void removeAllOf(PoleLinks.Pos pole) {
        wires.removeIf(w -> w.a().equals(pole) || w.b().equals(pole));
    }

    /** Every wire with an end in chunk ({@code chunkX}, {@code chunkZ}): what a client watching it is sent. */
    public List<PoleLinks.Wire> touching(int chunkX, int chunkZ) {
        return wires.stream().filter(w -> inChunk(w, chunkX, chunkZ)).toList();
    }

    /**
     * A client's copy of a chunk's wires, replaced whole when the chunk is sent. A wire cut while no
     * end was watched is dropped here, when either end is watched again.
     */
    public void replaceTouching(int chunkX, int chunkZ, Collection<PoleLinks.Wire> sent) {
        wires.removeIf(w -> inChunk(w, chunkX, chunkZ));
        sent.forEach(w -> add(w.a(), w.b()));
    }

    private static boolean inChunk(PoleLinks.Wire w, int chunkX, int chunkZ) {
        return inChunk(w.a(), chunkX, chunkZ) || inChunk(w.b(), chunkX, chunkZ);
    }

    private static boolean inChunk(PoleLinks.Pos p, int chunkX, int chunkZ) {
        return (p.x() >> 4) == chunkX && (p.z() >> 4) == chunkZ;
    }

    /** One wire per unordered pair: the end that sorts first by position is always {@code a}. */
    private static PoleLinks.Wire pair(PoleLinks.Pos a, PoleLinks.Pos b) {
        return sortsFirst(a, b) ? new PoleLinks.Wire(a, b) : new PoleLinks.Wire(b, a);
    }

    private static boolean sortsFirst(PoleLinks.Pos a, PoleLinks.Pos b) {
        if (a.x() != b.x()) {
            return a.x() < b.x();
        }
        if (a.y() != b.y()) {
            return a.y() < b.y();
        }
        return a.z() <= b.z();
    }

    public Set<PoleLinks.Wire> all() {
        return Collections.unmodifiableSet(wires);
    }
}
