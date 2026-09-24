package com.planetaryfactory.core.dismantle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DismantleSpanTest {

    private record At(int x, int z) {
    }

    /** A flat grid of family members, face-adjacent in four directions, joined by {@code join}. */
    private static final class Grid implements DismantleFamily<At> {

        private final Set<At> members = new HashSet<>();
        private BiPredicate<At, At> join = (a, b) -> true;

        Grid row(int z, int fromX, int toX) {
            for (int x = fromX; x <= toX; x++) members.add(new At(x, z));
            return this;
        }

        Grid column(int x, int fromZ, int toZ) {
            for (int z = fromZ; z <= toZ; z++) members.add(new At(x, z));
            return this;
        }

        Grid joinedBy(BiPredicate<At, At> join) {
            this.join = join;
            return this;
        }

        @Override
        public boolean member(At at) {
            return members.contains(at);
        }

        @Override
        public List<At> neighbours(At at) {
            return List.of(new At(at.x + 1, at.z), new At(at.x - 1, at.z), new At(at.x, at.z + 1), new At(at.x, at.z - 1));
        }

        @Override
        public boolean joined(At a, At b) {
            return join.test(a, b);
        }
    }

    private static List<At> along(int z, int fromX, int toX) {
        List<At> path = new ArrayList<>();
        int step = toX >= fromX ? 1 : -1;
        for (int x = fromX; x != toX + step; x += step) path.add(new At(x, z));
        return path;
    }

    @Test
    void aStraightRunIsTakenStartFirstEitherWay() {
        Grid grid = new Grid().row(0, 0, 5);
        assertEquals(along(0, 1, 4), DismantleSpan.between(new At(1, 0), new At(4, 0), grid).path());
        assertEquals(along(0, 4, 1), DismantleSpan.between(new At(4, 0), new At(1, 0), grid).path());
    }

    @Test
    void aSpanOnItsOwnStartIsThatBlock() {
        DismantleSpan.Result<At> span = DismantleSpan.between(new At(2, 0), new At(2, 0), new Grid().row(0, 0, 3));
        assertEquals(List.of(new At(2, 0)), span.path());
        assertNull(span.refusal());
    }

    @Test
    void aSpanFollowsABend() {
        Grid grid = new Grid().row(0, 0, 3).column(3, 0, 3);
        List<At> expected = new ArrayList<>(along(0, 0, 3));
        for (int z = 1; z <= 3; z++) expected.add(new At(3, z));
        assertEquals(expected, DismantleSpan.between(new At(0, 0), new At(3, 3), grid).path());
    }

    @Test
    void aTeeTakesOnlyTheBranchBetweenTheEnds() {
        Grid grid = new Grid().row(0, 0, 6).column(3, 1, 4);
        assertEquals(along(0, 0, 6), DismantleSpan.between(new At(0, 0), new At(6, 0), grid).path());
    }

    @Test
    void theShortestOfTwoRoutesIsTaken() {
        // A ring five wide and three deep: along the top is 3 steps, round the bottom 7.
        Grid grid = new Grid().row(0, 0, 4).row(2, 0, 4).column(0, 0, 2).column(4, 0, 2);
        assertEquals(along(0, 1, 4), DismantleSpan.between(new At(1, 0), new At(4, 0), grid).path());
    }

    @Test
    void oppositePointsOfARingAreRefusedAsTied() {
        Grid ring = new Grid().row(0, 0, 2).row(2, 0, 2).column(0, 0, 2).column(2, 0, 2);
        DismantleSpan.Result<At> span = DismantleSpan.between(new At(0, 0), new At(2, 2), ring);
        assertEquals(DismantleSpan.Refusal.TIED, span.refusal());
        assertTrue(span.path().isEmpty());
    }

    @Test
    void anEndOutsideTheFamilyIsRefused() {
        DismantleSpan.Result<At> span = DismantleSpan.between(new At(0, 0), new At(5, 0), new Grid().row(0, 0, 4));
        assertEquals(DismantleSpan.Refusal.OUTSIDE_FAMILY, span.refusal());
        assertTrue(span.path().isEmpty());
    }

    @Test
    void anEndInTheFamilyButNotJoinedToTheStartIsRefused() {
        Grid grid = new Grid().row(0, 0, 2).row(0, 4, 6);
        assertEquals(DismantleSpan.Refusal.NOT_JOINED, DismantleSpan.between(new At(0, 0), new At(5, 0), grid).refusal());
    }

    @Test
    void touchingMembersAreJoinedByDefault() {
        DismantleFamily<At> touching = new DismantleFamily<>() {
            @Override
            public boolean member(At at) {
                return at.z == 0 && at.x >= 0 && at.x <= 3;
            }

            @Override
            public List<At> neighbours(At at) {
                return List.of(new At(at.x + 1, at.z), new At(at.x - 1, at.z));
            }
        };
        assertEquals(along(0, 0, 3), DismantleSpan.between(new At(0, 0), new At(3, 0), touching).path());
    }

    @Test
    void aJoinRuleSeparatesTouchingMembers() {
        At cut = new At(2, 0);
        Grid grid = new Grid().row(0, 0, 4).joinedBy((a, b) -> !(a.equals(cut) && b.x == 3 || b.equals(cut) && a.x == 3));
        assertEquals(DismantleSpan.Refusal.NOT_JOINED, DismantleSpan.between(new At(0, 0), new At(4, 0), grid).refusal());
        assertEquals(along(0, 0, 2), DismantleSpan.between(new At(0, 0), cut, grid).path());
    }

    @Test
    void aJoinRuleCanBreakATie() {
        Grid ring = new Grid().row(0, 0, 2).row(2, 0, 2).column(0, 0, 2).column(2, 0, 2)
                .joinedBy((a, b) -> !(a.equals(new At(1, 2)) || b.equals(new At(1, 2))));
        List<At> expected = List.of(new At(0, 0), new At(1, 0), new At(2, 0), new At(2, 1), new At(2, 2));
        assertEquals(expected, DismantleSpan.between(new At(0, 0), new At(2, 2), ring).path());
    }
}
