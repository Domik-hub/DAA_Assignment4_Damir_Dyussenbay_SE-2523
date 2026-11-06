package edu.daa;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StringSearchTest {
    private StringSearch[] algorithms() { return new StringSearch[]{new NaiveSearch(), new KmpSearch()}; }

    @Test void referenceLpsArrays() {
        KmpSearch kmp = new KmpSearch();
        assertArrayEquals(new int[]{0, 1, 2, 0, 1, 2, 3, 3}, kmp.lps("AAACAAAA"));
        assertArrayEquals(new int[]{0, 0, 1, 2, 0, 1, 2, 3, 4}, kmp.lps("ABABCABAB"));
        assertArrayEquals(new int[]{}, kmp.lps(""));
        assertArrayEquals(new int[]{0}, kmp.lps("A"));
    }

    @Test void referenceSearchAndOverlap() {
        for (StringSearch s : algorithms()) {
            assertEquals(List.of(10), s.search("ABABDABACDABABCABAB", "ABABCABAB"));
            assertEquals(List.of(0, 1, 2, 3), s.search("AAAAA", "AA"));
            assertEquals(List.of(0, 2, 4), s.search("ABABABA", "ABA"));
            assertEquals(List.of(0), s.search("ABC", "ABC"));
            assertEquals(List.of(2), s.search("ABC", "C"));
            assertEquals(List.of(), s.search("ABC", "Z"));
        }
    }

    @Test void requiredEdgeCases() {
        for (StringSearch s : algorithms()) {
            assertThrows(IllegalArgumentException.class, () -> s.search("abc", ""));
            assertThrows(IllegalArgumentException.class, () -> s.search("", ""));
            assertEquals(List.of(), s.search("abc", "abcd"));
            assertEquals(List.of(), s.search("", "a"));
        }
    }

    private static List<Integer> oracle(String text, String pattern) {
        List<Integer> hits = new ArrayList<>();
        for (int i = text.indexOf(pattern); i >= 0; i = text.indexOf(pattern, i + 1)) hits.add(i);
        return hits;
    }

    @TestFactory List<DynamicTest> oneHundredRandomSearchCrossChecks() {
        InputGenerator g = new InputGenerator(1234567);
        List<DynamicTest> tests = new ArrayList<>();
        for (int t = 0; t < 100; t++) {
            String text = g.word(g.nextInt(101), "ABCD");
            String candidate;
            if (t % 2 == 0 && !text.isEmpty()) {
                int start = g.nextInt(text.length());
                candidate = text.substring(start, start + 1 + g.nextInt(Math.min(10, text.length() - start)));
            } else candidate = g.word(1 + g.nextInt(12), "ABCD");
            String pattern = candidate;
            tests.add(DynamicTest.dynamicTest("random search " + t, () -> {
                List<Integer> expected = oracle(text, pattern);
                for (StringSearch s : algorithms()) assertEquals(expected, s.search(text, pattern), s.name());
            }));
        }
        return tests;
    }

    @TestFactory List<DynamicTest> oneHundredRandomLpsOracleChecks() {
        InputGenerator g = new InputGenerator(475);
        List<DynamicTest> tests = new ArrayList<>();
        for (int t = 0; t < 100; t++) {
            String p = g.word(g.nextInt(51), "ABC");
            tests.add(DynamicTest.dynamicTest("random LPS " + t, () -> {
                int[] expected = new int[p.length()];
                for (int end = 1; end <= p.length(); end++)
                    for (int len = 1; len < end; len++)
                        if (p.substring(0, len).equals(p.substring(end - len, end))) expected[end - 1] = len;
                assertArrayEquals(expected, new KmpSearch().lps(p));
            }));
        }
        return tests;
    }

    @Test void adverseInputCountersAndReset() {
        NaiveSearch naive = new NaiveSearch();
        KmpSearch kmp = new KmpSearch();
        assertEquals(List.of(), naive.search("AAAAAAAA", "AAAB"));
        assertEquals(20, naive.metrics().comparisons);
        assertEquals(List.of(), kmp.search("AAAAAAAA", "AAAB"));
        assertEquals(13, kmp.metrics().comparisons);
        assertEquals(5, kmp.metrics().lpsComparisons);
        assertEquals(4, kmp.metrics().auxCells);
        kmp.search("A", "A");
        assertEquals(1, kmp.metrics().comparisons);
        assertEquals(0, kmp.metrics().lpsComparisons);
    }

    @Test void reproducibleInputs() {
        assertArrayEquals(InputGenerator.edit("random", 40), InputGenerator.edit("random", 40));
        assertArrayEquals(InputGenerator.search("random", 100, 12), InputGenerator.search("random", 100, 12));
    }
}
