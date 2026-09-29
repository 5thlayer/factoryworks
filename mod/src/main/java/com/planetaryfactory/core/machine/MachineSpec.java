package com.planetaryfactory.core.machine;

import java.util.List;
import java.util.Set;

/**
 * One crafting machine's figures, as {@code scripts/build-machine-specs.py} copies them out of the
 * corpus (#489, ADR-0096). Watts are Factorio's; {@link AssemblingMachineSpec} turns them into FE.
 *
 * <p>Pure: no Minecraft types.
 */
public record MachineSpec(
        String name,
        String recipeType,
        Set<String> categories,
        double craftingSpeed,
        long watts,
        long drainWatts,
        String replaceGroup,
        int itemInputs,
        int itemOutputs,
        List<Integer> fluidInputs,
        List<Integer> fluidOutputs,
        List<Integer> fluidOutputBoxes) {

    public MachineSpec {
        categories = Set.copyOf(categories);
        fluidInputs = List.copyOf(fluidInputs);
        fluidOutputs = List.copyOf(fluidOutputs);
        fluidOutputBoxes = List.copyOf(fluidOutputBoxes);
    }

    public boolean crafts(String category) {
        return categories.contains(category);
    }

    /**
     * Whether a recipe needing these counts has a slot or tank for each. A recipe that fits no tank
     * is refused rather than held and idled on (ADR-0075, ADR-0096).
     */
    public boolean fits(int items, int itemResults, int fluids, int fluidResults) {
        return items <= itemInputs && itemResults <= itemOutputs
                && fluids <= fluidInputs.size() && fluidResults <= fluidOutputs.size();
    }

    public boolean hasTanks() {
        return !fluidInputs.isEmpty() || !fluidOutputs.isEmpty();
    }

    /** Input tank {@code index}'s volume in mB, or 0 past the machine's last. */
    public int fluidInputVolume(int index) {
        return index < fluidInputs.size() ? fluidInputs.get(index) : 0;
    }

    /**
     * Output tank {@code index}'s volume in mB, or 0 past the machine's last. A Held recipe resizes
     * it; see {@link OutputTankVolume}.
     */
    public int fluidOutputVolume(int index) {
        return index < fluidOutputs.size() ? fluidOutputs.get(index) : 0;
    }
}
