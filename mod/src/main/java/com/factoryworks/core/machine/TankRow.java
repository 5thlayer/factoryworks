package com.factoryworks.core.machine;

import java.util.ArrayList;
import java.util.List;

/** How a region of the machine screen's tank row is shared among its tanks' bars. */
public final class TankRow {

    public static final int GAP = 3;

    public record Bar(int x, int width) {
    }

    private TankRow() {
    }

    public static List<Bar> split(int x, int width, int tanks) {
        List<Bar> bars = new ArrayList<>(tanks);
        if (tanks <= 0) {
            return bars;
        }
        int each = (width - GAP * (tanks - 1)) / tanks;
        for (int tank = 0; tank < tanks; tank++) {
            int barX = x + tank * (each + GAP);
            bars.add(new Bar(barX, tank == tanks - 1 ? x + width - barX : each));
        }
        return bars;
    }
}
