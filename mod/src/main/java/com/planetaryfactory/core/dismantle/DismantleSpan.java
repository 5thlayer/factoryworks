package com.planetaryfactory.core.dismantle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The blocks a Dismantle takes up: the shortest path of joined members from the start to the end,
 * both included, start first (ADR-0086). Two equally short paths refuse the end rather than pick one.
 */
public final class DismantleSpan {

    public enum Refusal {
        OUTSIDE_FAMILY,
        NOT_JOINED,
        TIED
    }

    public record Result<N>(List<N> path, @Nullable Refusal refusal) {

        public Result {
            path = List.copyOf(path);
        }

        static <N> Result<N> refused(Refusal refusal) {
            return new Result<>(List.of(), refusal);
        }
    }

    private DismantleSpan() {
    }

    public static <N> Result<N> between(N start, N end, DismantleFamily<N> family) {
        if (!family.member(start) || !family.member(end)) {
            return Result.refused(Refusal.OUTSIDE_FAMILY);
        }
        Map<N, Integer> distance = new HashMap<>();
        // How many shortest paths reach a node, counted no higher than two.
        Map<N, Integer> routes = new HashMap<>();
        Map<N, N> previous = new HashMap<>();
        Deque<N> queue = new ArrayDeque<>();
        distance.put(start, 0);
        routes.put(start, 1);
        queue.add(start);
        while (!queue.isEmpty()) {
            N at = queue.poll();
            int here = distance.get(at);
            Integer reached = distance.get(end);
            if (reached != null && here >= reached) {
                break;
            }
            for (N next : family.neighbours(at)) {
                if (!family.member(next) || !family.joined(at, next)) {
                    continue;
                }
                Integer there = distance.get(next);
                if (there == null) {
                    distance.put(next, here + 1);
                    routes.put(next, routes.get(at));
                    previous.put(next, at);
                    queue.add(next);
                } else if (there == here + 1) {
                    routes.put(next, Math.min(2, routes.get(next) + routes.get(at)));
                }
            }
        }
        if (!distance.containsKey(end)) {
            return Result.refused(Refusal.NOT_JOINED);
        }
        if (routes.get(end) > 1) {
            return Result.refused(Refusal.TIED);
        }
        List<N> path = new ArrayList<>();
        for (N at = end; at != null; at = previous.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
        return new Result<>(path, null);
    }
}
