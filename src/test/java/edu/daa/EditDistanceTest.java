package edu.daa;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EditDistanceTest {
    private EditDistance[] algorithms() {
        return new EditDistance[]{new RecursiveDistance(), new TableDistance(), new TwoRowDistance()};
    }

    @Test void referenceValuesAndEmptyStrings() {
        for (EditDistance d : algorithms()) {
            assertEquals(3, d.distance("kitten", "sitting"), d.name());
            assertEquals(2, d.distance("flaw", "lawn"), d.name());
            assertEquals(5, d.distance("intention", "execution"), d.name());
            assertEquals(3, d.distance("", "abc"), d.name());
            assertEquals(3, d.distance("abc", ""), d.name());
            assertEquals(0, d.distance("abc", "abc"), d.name());
            assertEquals(0, d.distance("", ""), d.name());
            assertEquals(1, d.distance("A", "B"), d.name());
        }
    }

    @TestFactory List<DynamicTest> oneHundredRandomCrossChecks() {
        InputGenerator g = new InputGenerator(82917);
        List<DynamicTest> tests = new ArrayList<>();
        for (int t = 0; t < 100; t++) {
            String a = g.word(g.nextInt(7), "ABCD");
            String b = g.word(g.nextInt(7), "ABCD");
            tests.add(DynamicTest.dynamicTest("random edit pair " + t, () -> {
                int expected = new RecursiveDistance().distance(a, b);
                for (EditDistance d : algorithms()) {
                    assertEquals(expected, d.distance(a, b), d.name());
                    assertEquals(expected, d.distance(b, a), "Symmetry: " + d.name());
                    assertTrue(expected >= Math.abs(a.length() - b.length()));
                    assertTrue(expected <= Math.max(a.length(), b.length()));
                }
            }));
        }
        return tests;
    }

    @Test void measuredWorkAndReset() {
        RecursiveDistance recursive = new RecursiveDistance();
        assertEquals(1, recursive.distance("A", "B"));
        assertEquals(4, recursive.metrics().recursiveCalls);
        assertEquals(1, recursive.metrics().comparisons);
        recursive.distance("", "");
        assertEquals(1, recursive.metrics().recursiveCalls);
        assertEquals(0, recursive.metrics().comparisons);
        for (EditDistance d : new EditDistance[]{new TableDistance(), new TwoRowDistance()}) {
            d.distance("abc", "xy");
            assertEquals(6, d.metrics().dpCells);
            assertEquals(6, d.metrics().comparisons);
            assertEquals(d instanceof TableDistance ? 12 : 6, d.metrics().auxCells);
            d.distance("", "abc");
            assertEquals(0, d.metrics().dpCells);
        }
    }

    @Test void unequalLengthAndUnicodeCodeUnits() {
        for (EditDistance d : algorithms()) {
            assertEquals(4, d.distance("a", "aaaaa"));
            assertEquals(1, d.distance("Алма", "Алмы"));
            assertEquals(2, d.distance("", "\uD83D\uDE00"));
        }
    }
}
