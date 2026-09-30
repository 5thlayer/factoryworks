package com.factoryworks.core.mining.rig;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The turn rule of #537: 10 operations a block, round-robin, equal share whatever the amount. */
class RigRotationTest {

    /** Runs {@code ops} operations over blocks holding {@code amounts}, returning the block of each. */
    private static List<Integer> run(RigRotation rotation, int[] amounts, int ops) {
        List<Integer> worked = new ArrayList<>();
        for (int i = 0; i < ops; i++) {
            int block = rotation.current(amounts.length, b -> amounts[b] > 0);
            if (block < 0) {
                break;
            }
            worked.add(block);
            amounts[block]--;
            rotation.completed();
        }
        return worked;
    }

    private static long count(List<Integer> worked, int block) {
        return worked.stream().filter(b -> b == block).count();
    }

    @Test
    void twoBlocksAlternateTenAndTen() {
        List<Integer> worked = run(new RigRotation(), new int[] {1000, 1000}, 40);
        assertEquals(List.of(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
                worked.subList(0, 20));
        assertEquals(0, worked.get(20));
        assertEquals(1, worked.get(30));
    }

    @Test
    void fourBlocksTakeTurnsInOrderAndWrap() {
        List<Integer> worked = run(new RigRotation(), new int[] {1000, 1000, 1000, 1000}, 50);
        for (int turn = 0; turn < 5; turn++) {
            for (int op = 0; op < 10; op++) {
                assertEquals(turn % 4, worked.get(turn * 10 + op));
            }
        }
    }

    @Test
    void aBlockGetsTheSameShareWhateverItHolds() {
        List<Integer> worked = run(new RigRotation(), new int[] {5000, 30}, 60);
        assertEquals(30, count(worked, 0));
        assertEquals(30, count(worked, 1));
    }

    @Test
    void aBlockThatEmptiesMidTurnHandsOverAtOnce() {
        List<Integer> worked = run(new RigRotation(), new int[] {3, 1000}, 13);
        assertEquals(List.of(0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), worked);
    }

    @Test
    void skipsBlocksWithNoOreAndWrapsPastTheEnd() {
        int[] amounts = {1000, 0, 1000, 0};
        List<Integer> worked = run(new RigRotation(), amounts, 30);
        assertEquals(Set.of(0, 2), Set.copyOf(worked));
        assertEquals(2, worked.get(10));
        assertEquals(0, worked.get(20));
    }

    @Test
    void aLoneBlockKeepsGoingThroughFreshTurns() {
        List<Integer> worked = run(new RigRotation(), new int[] {0, 1000}, 25);
        assertEquals(25, count(worked, 1));
    }

    @Test
    void nothingLeftAnswersNoBlock() {
        assertEquals(-1, new RigRotation().current(3, b -> false));
        assertEquals(-1, new RigRotation().current(0, b -> true));
    }

    @Test
    void askingAgainWithoutWorkingAnswersTheSameBlock() {
        RigRotation rotation = new RigRotation();
        assertEquals(0, rotation.current(2, b -> true));
        assertEquals(0, rotation.current(2, b -> true));
        assertEquals(0, rotation.spent());
    }

    @Test
    void theTurnResumesFromWhatWasSaved() {
        int[] amounts = {1000, 1000, 1000};
        RigRotation before = new RigRotation();
        run(before, amounts, 14);

        RigRotation after = new RigRotation();
        after.load(before.index(), before.spent());
        assertEquals(1, before.index());
        assertEquals(4, after.spent());
        assertEquals(List.of(1, 1, 1, 1, 1, 1, 2), run(after, amounts, 7));
    }
}
