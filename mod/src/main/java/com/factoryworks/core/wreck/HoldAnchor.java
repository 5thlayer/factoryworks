package com.factoryworks.core.wreck;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Which block of a cargo hold owns its inventory (ADR-0107, #548).
 *
 * <p>The hold is ten blocks and the hub template is rotated per world, so a part stores no offset
 * to its anchor: it walks the connected hold blocks and takes the one flagged as the anchor.
 * Minecraft-free so the walk is tested over an abstract graph.
 */
public final class HoldAnchor {

    /** A hold is ten blocks; the bound keeps a malformed mass of hold blocks from being walked. */
    public static final int LIMIT = 16;

    private HoldAnchor() {
    }

    /**
     * The one anchor among the hold blocks connected to {@code from}, or empty when the hold has
     * none, has more than one, or is larger than {@link #LIMIT}.
     */
    public static <P> Optional<P> resolve(P from, Function<P, List<P>> neighbours,
            Predicate<P> isHold, Predicate<P> isAnchor) {
        if (!isHold.test(from)) {
            return Optional.empty();
        }
        Set<P> seen = new HashSet<>();
        Deque<P> queue = new ArrayDeque<>();
        seen.add(from);
        queue.add(from);
        P anchor = null;
        while (!queue.isEmpty()) {
            P at = queue.poll();
            if (isAnchor.test(at)) {
                if (anchor != null) {
                    return Optional.empty();
                }
                anchor = at;
            }
            for (P next : neighbours.apply(at)) {
                if (isHold.test(next) && seen.add(next)) {
                    if (seen.size() > LIMIT) {
                        return Optional.empty();
                    }
                    queue.add(next);
                }
            }
        }
        return Optional.ofNullable(anchor);
    }
}
