package com.planetaryfactory.core.energy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
