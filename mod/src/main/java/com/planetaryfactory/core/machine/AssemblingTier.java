package com.planetaryfactory.core.machine;

/**
 * Factorio's three Assembling Machines, each a block of its own (ADR-0075), with the Oritech paint
 * that tells them apart. Every figure is the tier's {@link MachineSpec} (#489).
 *
 * <p>Pure: the paint is Oritech's {@code ColorVariant} by name, so the test source set can hold the
 * tiers without Oritech on its classpath.
 */
public enum AssemblingTier {
    ONE("assembling_machine", "assembling-machine-1", "ORANGE"),
    TWO("assembling_machine_2", "assembling-machine-2", "DIAMOND"),
    THREE("assembling_machine_3", "assembling-machine-3", "INDUSTRIAL");

    private final String blockName;
    private final String factorioName;
    private final String paint;

    AssemblingTier(String blockName, String factorioName, String paint) {
        this.blockName = blockName;
        this.factorioName = factorioName;
        this.paint = paint;
    }

    public String blockName() {
        return blockName;
    }

    public String partBlockName() {
        return blockName + "_part";
    }

    public MachineSpec spec() {
        return MachineSpecs.get().spec(factorioName);
    }

    /** Oritech's {@code ColorVariant} name, fixed per tier: a repainted tier would claim another's rate. */
    public String paint() {
        return paint;
    }

    /** Whether this tier crafts a recipe of Factorio {@code category}. */
    public boolean crafts(String category) {
        return spec().crafts(category);
    }
}
