package com.planetaryfactory.core.assembler;

/**
 * Which of the Crafting Plan's {@code +1}, {@code +5} and {@code all} are live (#287).
 *
 * <p>There is no Start: a press queues that many at once and takes the reservation there and then.
 * A lit button is therefore a promise the inventory covers it, and the one ceiling that can keep it
 * is the resolver's {@code largestAffordable} -- the largest count whose <em>complete</em> plan the
 * inventory covers, chain crafts included. Minecraft-free so the promise is a unit test.
 */
public record CraftButtons(boolean one, boolean five, boolean all, int allCount) {

    public static CraftButtons of(int largestAffordable) {
        int ceiling = Math.max(0, largestAffordable);
        return new CraftButtons(ceiling >= 1, ceiling >= 5, ceiling >= 1, ceiling);
    }
}
