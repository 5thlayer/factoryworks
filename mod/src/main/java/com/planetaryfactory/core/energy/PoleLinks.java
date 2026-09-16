package com.planetaryfactory.core.energy;

import java.util.Arrays;
import java.util.List;

/**
 * Which poles are one Electric Network (ADR-0062).
 *
 * <p>Two poles link when they are within the shorter of their two wire reaches, measured between
 * block centres in three dimensions, and a network is everything linked transitively. There is no
 * stored topology: the answer is recomputed from the poles standing whenever that set changes, so a
 * break that splits a network is not a separate operation from a place that merges two.
 *
 * <p>Pure: no Minecraft types.
 */
public final class PoleLinks {

    /** A pole's position and tier. */
    public record Pole(int x, int y, int z, PoleTier tier) {
    }

    private PoleLinks() {
    }

    /** Whether two poles are wired to each other. */
    public static boolean linked(Pole a, Pole b) {
        double reach = Math.min(a.tier().wireReach(), b.tier().wireReach());
        long dx = a.x() - b.x();
        long dy = a.y() - b.y();
        long dz = a.z() - b.z();
        return dx * dx + dy * dy + dz * dz <= reach * reach;
    }

    /**
     * A network id per pole, index-aligned with the input, numbered densely from zero in order of
     * first appearance.
     */
    public static int[] networks(List<Pole> poles) {
        int n = poles.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
        // Quadratic in poles, not in blocks. The count is what a player places, and the pairwise
        // test is only run when that set changes.
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (linked(poles.get(i), poles.get(j))) {
                    parent[root(parent, i)] = root(parent, j);
                }
            }
        }
        int[] ids = new int[n];
        int[] idOfRoot = new int[n];
        Arrays.fill(idOfRoot, -1);
        int next = 0;
        for (int i = 0; i < n; i++) {
            int r = root(parent, i);
            if (idOfRoot[r] < 0) {
                idOfRoot[r] = next++;
            }
            ids[i] = idOfRoot[r];
        }
        return ids;
    }

    private static int root(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }
}
