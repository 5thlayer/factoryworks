package com.factoryworks.core.fluid;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Where an Offshore Pump may stand (ADR-0050, #256): beside a natural source of a pumpable fluid.
 *
 * <p>The predicate is deliberately the smallest thing that can be written -- <b>one adjacent block
 * whose fluid state is a source of a {@link #PUMPABLE} fluid</b>. It carries no minimum body
 * size and no biome test, both of which ADR-0050 considered and rejected, because water in Factorio is not something the player
 * hunts for and a pump that only works on an ocean would make it so here.
 *
 * <p><b>Why a blockstate is a sufficient check.</b> It is not, on its own: vanilla records nothing
 * that distinguishes a poured source from a worldgen one -- {@code BucketItem} empties as
 * {@code content.defaultFluidState().createLegacyBlock()}, byte-identical to what the generator
 * lays down. What makes this predicate sound is the rule around it. With source formation off
 * ({@link WaterConservation}) and no mod in the pack able to place one, nothing can
 * bring a source into existence, so every source block in the world is one worldgen or a structure
 * placed. Naturalness is guaranteed by construction rather than tracked.
 *
 * <p>That is the reason ADR-0050 deleted an entire chunk-attachment design that marked placed water
 * per block: it <em>failed open</em>, because its correctness rested on an exhaustive list of the
 * ways a source can be placed, and the list was never going to be exhaustive.
 *
 * <p>Free of Minecraft, so the rule can be asserted in an ordinary unit test. The caller is what
 * turns real neighbours into {@link Neighbour} values.
 */
public final class OffshorePumpSiting {

    /**
     * The fluids a pump may draw, by registry id (#256). A whitelist: a body's own pumped fluid
     * (heavy oil on Electra, ammoniacal solution on Gelida) is a row added here when that body is
     * built, and every fluid not named -- Oritech's finite oil above all -- stays unpumpable.
     */
    public static final List<String> PUMPABLE = List.of("minecraft:water", "minecraft:lava");

    /**
     * What one neighbouring block is, as far as siting is concerned. {@link #FLOWING} is not merely
     * "not a source" but the state ADR-0050's deferred outlet block is allowed to create, and it
     * must stay refused here for that block to be a way of moving water rather than a way of making
     * it. A source carries its fluid's id, because the pump emits what it stands against.
     *
     * @param fluid the source's fluid id, or null for flowing and dry
     */
    public record Neighbour(Kind kind, String fluid) {

        public enum Kind { SOURCE, FLOWING, DRY }

        /** A moving fluid: a dug channel, or the deferred outlet block. Never a valid site. */
        public static final Neighbour FLOWING = new Neighbour(Kind.FLOWING, null);
        /** No fluid at all. */
        public static final Neighbour DRY = new Neighbour(Kind.DRY, null);

        /** Still fluid: worldgen's, or a structure's. */
        public static Neighbour source(String fluid) {
            return new Neighbour(Kind.SOURCE, fluid);
        }
    }

    private OffshorePumpSiting() {
    }

    /**
     * The fluid a pump standing against these neighbours would pump, or empty for a refused site.
     * The first pumpable source in the caller's order wins; worldgen never sets two pumpable fluids
     * against one block, so there is no tie to break by design.
     */
    public static Optional<String> site(Collection<Neighbour> neighbours) {
        return neighbours.stream()
                .filter(n -> n.kind() == Neighbour.Kind.SOURCE && PUMPABLE.contains(n.fluid()))
                .map(Neighbour::fluid)
                .findFirst();
    }

    /** Whether a pump may stand against these neighbours. */
    public static boolean accepts(Collection<Neighbour> neighbours) {
        return site(neighbours).isPresent();
    }
}
