package com.factoryworks.core.energy;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * How a pole's area becomes three lists: every block resolved to the energy owner it answers to,
 * each owner kept once, and the owner -- not the block -- given a role (#292, ADR-0062).
 *
 * <p>A block is not a machine. Oritech's Steam Engine is three failures to a scan that files by
 * position: its machine-core hull blocks answer the controller's energy face with an untagged
 * block, so they would be consumers; one engine would be offered and drawn once per block in the
 * area; and a slave, which holds no FE, would be reached while its master stands out of range.
 * Resolving first answers all three at once.
 *
 * <p>Pure: positions are a type parameter, so no Minecraft types.
 */
public final class SupplyScan {

    /**
     * Hops from a block to its owner. A hull block to its controller, and a slave to its master, is
     * two; the bound only exists so that a mod answering in a circle cannot hang a server tick.
     */
    private static final int MAX_HOPS = 8;

    public enum Role { CONSUMER, GENERATOR, ACCUMULATOR, NONE }

    public record Roles<P>(List<P> consumers, List<P> generators, List<P> accumulators) {
    }

    private SupplyScan() {
    }

    /**
     * @param owner the owner a position answers to, or {@code null} (or itself) if it answers to
     *              nobody else
     * @param role  an owner's role; {@link Role#NONE} for a position with no reachable face
     */
    public static <P> Roles<P> classify(Iterable<P> positions, Function<P, P> owner,
            Function<P, Role> role) {
        Set<P> owners = new LinkedHashSet<>();
        for (P position : positions) {
            owners.add(resolve(position, owner));
        }
        List<P> consumers = new ArrayList<>();
        List<P> generators = new ArrayList<>();
        List<P> accumulators = new ArrayList<>();
        for (P found : owners) {
            switch (role.apply(found)) {
                case CONSUMER -> consumers.add(found);
                case GENERATOR -> generators.add(found);
                case ACCUMULATOR -> accumulators.add(found);
                case NONE -> { }
            }
        }
        return new Roles<>(consumers, generators, accumulators);
    }

    private static <P> P resolve(P position, Function<P, P> owner) {
        P at = position;
        for (int hop = 0; hop < MAX_HOPS; hop++) {
            P next = owner.apply(at);
            if (next == null || next.equals(at)) {
                return at;
            }
            at = next;
        }
        return at;
    }
}
