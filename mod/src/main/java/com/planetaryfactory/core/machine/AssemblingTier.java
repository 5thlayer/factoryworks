package com.planetaryfactory.core.machine;

import java.util.Set;

/**
 * Factorio's three Assembling Machines, each a block of its own (ADR-0075): {@code crafting_speed},
 * {@code energy_usage} and {@code crafting_categories} from {@code machine.json}, and the Oritech
 * paint that tells them apart.
 *
 * <p>Pure: the paint is Oritech's {@code ColorVariant} by name, so the test source set can hold the
 * figures without Oritech on its classpath.
 */
public enum AssemblingTier {
    ONE("assembling_machine", 0.5, 75_000L, "ORANGE", "crafting", "advanced-crafting"),
    TWO("assembling_machine_2", 0.75, 150_000L, "DIAMOND", "crafting", "advanced-crafting", "crafting-with-fluid"),
    THREE("assembling_machine_3", 1.25, 375_000L, "INDUSTRIAL", "crafting", "advanced-crafting", "crafting-with-fluid");

    private static final String FLUID_CATEGORY = "crafting-with-fluid";

    private final String blockName;
    private final double craftingSpeed;
    private final long watts;
    private final String paint;
    private final Set<String> categories;

    AssemblingTier(String blockName, double craftingSpeed, long watts, String paint, String... categories) {
        this.blockName = blockName;
        this.craftingSpeed = craftingSpeed;
        this.watts = watts;
        this.paint = paint;
        this.categories = Set.of(categories);
    }

    public String blockName() {
        return blockName;
    }

    public String partBlockName() {
        return blockName + "_part";
    }

    public double craftingSpeed() {
        return craftingSpeed;
    }

    public long watts() {
        return watts;
    }

    /** Oritech's {@code ColorVariant} name, fixed per tier: a repainted tier would claim another's rate. */
    public String paint() {
        return paint;
    }

    /** Whether this tier crafts a recipe of Factorio {@code category}. */
    public boolean crafts(String category) {
        return categories.contains(category);
    }

    /** Whether this tier has a fluid input, which is the same as crafting with a fluid. */
    public boolean hasFluidInput() {
        return crafts(FLUID_CATEGORY);
    }
}
