package edu.daa;

import java.util.Objects;

public final class TwoRowDistance implements EditDistance {
    private final Metrics metrics = new Metrics();

    // State: previous[j]=D(i-1,j), current[j]=D(i,j) for the prefix-distance DP.
    // Transition: min(previous[j]+1,current[j-1]+1,previous[j-1]+(a[i-1]!=b[j-1])); swap rows.
    // Base: previous[j]=j for row 0 and current[0]=i for each new row; D(i,0)=i.
    @Override public int distance(String a, String b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        metrics.reset();
        int n = a.length(), m = b.length();
        int[] previous = new int[m + 1];
        metrics.auxCells += previous.length;
        int[] current = new int[m + 1];
        metrics.auxCells += current.length;
        for (int j = 0; j <= m; j++) previous[j] = j;
        for (int i = 1; i <= n; i++) {
            current[0] = i;
            for (int j = 1; j <= m; j++) {
                metrics.dpCells++;
                metrics.comparisons++;
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(previous[j - 1] + cost,
                        Math.min(previous[j] + 1, current[j - 1] + 1));
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[m];
    }

    @Override public Metrics metrics() { return metrics; }
    @Override public String name() { return "TwoRows"; }
}
