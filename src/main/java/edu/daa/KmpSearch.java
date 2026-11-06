package edu.daa;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class KmpSearch implements StringSearch {
    private final Metrics metrics = new Metrics();

    /** Longest proper prefix which is also a suffix for every pattern prefix. */
    public int[] lps(String pattern) {
        Objects.requireNonNull(pattern, "pattern");
        metrics.reset();
        return buildLps(pattern);
    }

    private int[] buildLps(String pattern) {
        int[] lps = new int[pattern.length()];
        metrics.auxCells += lps.length;
        int i = 1, length = 0;
        while (i < pattern.length()) {
            metrics.lpsComparisons++;
            if (pattern.charAt(i) == pattern.charAt(length)) {
                lps[i++] = ++length;
            } else if (length > 0) {
                length = lps[length - 1];
            } else {
                lps[i++] = 0;
            }
        }
        return lps;
    }

    @Override public List<Integer> search(String text, String pattern) {
        StringSearch.validate(text, pattern);
        metrics.reset();
        List<Integer> matches = new ArrayList<>();
        if (pattern.length() > text.length()) return matches;
        int[] lps = buildLps(pattern); // Included in both timing and total comparisons.
        int i = 0, j = 0;
        while (i < text.length()) {
            metrics.comparisons++;
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                if (j == pattern.length()) {
                    matches.add(i - j);
                    j = lps[j - 1]; // Keep a matching suffix so overlaps are found.
                }
            } else if (j > 0) {
                j = lps[j - 1]; // Only the pattern moves back; text index i never decreases.
            } else {
                i++;
            }
        }
        return matches;
    }

    @Override public Metrics metrics() { return metrics; }
    @Override public String name() { return "KMP"; }
}
