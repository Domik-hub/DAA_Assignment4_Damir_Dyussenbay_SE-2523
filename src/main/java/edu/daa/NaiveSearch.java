package edu.daa;

import java.util.ArrayList;
import java.util.List;

public final class NaiveSearch implements StringSearch {
    private final Metrics metrics = new Metrics();

    @Override public List<Integer> search(String text, String pattern) {
        StringSearch.validate(text, pattern);
        metrics.reset();
        List<Integer> matches = new ArrayList<>();
        int n = text.length(), m = pattern.length();
        for (int start = 0; start <= n - m; start++) {
            int j = 0;
            while (j < m) {
                metrics.comparisons++;
                if (text.charAt(start + j) != pattern.charAt(j)) break;
                j++;
            }
            if (j == m) matches.add(start);
        }
        return matches;
    }

    @Override public Metrics metrics() { return metrics; }
    @Override public String name() { return "Naive"; }
}
