package edu.daa;

import java.util.Objects;

public final class RecursiveDistance implements EditDistance {
    private final Metrics metrics = new Metrics();

    // State: D(i,j) is the distance between prefixes a[0,i) and b[0,j).
    // Transition: equal last chars -> D(i-1,j-1); else 1+min(D(i-1,j),D(i,j-1),D(i-1,j-1)).
    // Base: D(0,j)=j and D(i,0)=i; direct recursion deliberately uses no memoization.
    @Override public int distance(String a, String b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        metrics.reset();
        return solve(a, b, a.length(), b.length());
    }

    private int solve(String a, String b, int i, int j) {
        metrics.recursiveCalls++;
        if (i == 0) return j;
        if (j == 0) return i;
        metrics.comparisons++;
        if (a.charAt(i - 1) == b.charAt(j - 1)) return solve(a, b, i - 1, j - 1);
        int delete = solve(a, b, i - 1, j);
        int insert = solve(a, b, i, j - 1);
        int replace = solve(a, b, i - 1, j - 1);
        return 1 + Math.min(replace, Math.min(delete, insert));
    }

    @Override public Metrics metrics() { return metrics; }
    @Override public String name() { return "Recursive"; }
}
