package com.factoryworks.core.machine;

import java.util.ArrayList;
import java.util.List;

/**
 * Each fluid product's output tank as Factorio sizes it once a recipe is set: the larger of
 * {@code multiplier} crafts' worth and its box, where the first product also takes every box the
 * recipe leaves unused unless the recipe pins its products to boxes (#520).
 *
 * <p>Pure: no Minecraft types.
 */
public final class OutputTankVolume {

    private OutputTankVolume() {
    }

    public static List<Integer> of(List<Integer> boxes, List<Integer> amounts, boolean pinned, int multiplier) {
        List<Integer> volumes = new ArrayList<>(amounts.size());
        for (int index = 0; index < amounts.size(); index++) {
            int box = boxes.get(index);
            if (index == 0 && !pinned) {
                for (int unused = amounts.size(); unused < boxes.size(); unused++) {
                    box += boxes.get(unused);
                }
            }
            volumes.add(Math.max(box, multiplier * amounts.get(index)));
        }
        return volumes;
    }
}
