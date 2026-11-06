package edu.daa;

import java.util.Objects;

public final class TableDistance implements EditDistance {
    private final Metrics metrics = new Metrics();

    // State: dp[i][j] is the distance between prefixes a[0,i) and b[0,j).
    // Transition: min(dp[i-1][j]+1, dp[i][j-1]+1, dp[i-1][j-1]+(a[i-1]!=b[j-1])).
    // Base: dp[0][j]=j and dp[i][0]=i; fill rows in increasing i and j.
    @Override public int distance(String a, String b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        metrics.reset();
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][];
        for (int i = 0; i <= n; i++) {
            dp[i] = new int[m + 1];
            metrics.auxCells += dp[i].length; // Count each actual row allocation.
            dp[i][0] = i;
        }
        for (int j = 0; j <= m; j++) dp[0][j] = j;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                metrics.dpCells++;
                metrics.comparisons++;
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(dp[i - 1][j - 1] + cost,
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1));
            }
        }
        return dp[n][m];
    }

    @Override public Metrics metrics() { return metrics; }
    @Override public String name() { return "Table"; }
}
