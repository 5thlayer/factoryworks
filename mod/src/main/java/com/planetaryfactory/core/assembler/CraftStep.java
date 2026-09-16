package com.planetaryfactory.core.assembler;

import java.util.List;

/**
 * The crafts of one recipe in a resolved plan: what they eat, what they make, how many there are and
 * how long each one takes.
 *
 * <p>{@code inputs} and {@code outputs} are the whole batch's; {@code durationTicks} is <em>one</em>
 * craft's, and the queue runs and delivers the {@code crafts} one at a time (#289). One record rather
 * than one per craft keeps a large plan small on the player attachment.
 *
 * <p>{@code durationTicks} is Factorio's {@code energy_required x 20}, unmodified -- the Assembler
 * runs at speed 1 and ADR-0029 leaves the durations alone, because what makes hand-crafting slow
 * here is that the queue is serial.
 *
 * <p>The inputs are carried on the step rather than derived from the recipe because the queue never
 * looks a recipe up: a plan is resolved once, paid for once, and then is not re-resolved (ADR-0038).
 */
public record CraftStep(
        String recipe, List<ItemAmount> inputs, List<ItemAmount> outputs, int durationTicks, int crafts) {

    public CraftStep {
        if (recipe == null || recipe.isBlank()) {
            throw new IllegalArgumentException("a craft step needs a recipe id");
        }
        if (durationTicks < 0) {
            throw new IllegalArgumentException("a craft step cannot take " + durationTicks + " ticks");
        }
        if (crafts < 1) {
            throw new IllegalArgumentException("a craft step cannot run " + crafts + " crafts");
        }
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
    }

    /** A single craft. */
    public CraftStep(String recipe, List<ItemAmount> inputs, List<ItemAmount> outputs, int durationTicks) {
        this(recipe, inputs, outputs, durationTicks, 1);
    }

    /**
     * How much of a batch total the craft numbered {@code craft} (zero-based) accounts for.
     *
     * <p>Floors of the running share, so the crafts' amounts differ by at most one and always sum to
     * exactly the total -- a tag ingredient drawn from two items rarely divides evenly.
     */
    public int share(int total, int craft) {
        return (int) ((long) total * (craft + 1) / crafts - (long) total * craft / crafts);
    }

    /** What is left of a batch total once {@code craftsDone} crafts have taken their share. */
    public int remaining(int total, int craftsDone) {
        return total - (int) ((long) total * craftsDone / crafts);
    }
}
