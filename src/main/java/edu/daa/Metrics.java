package edu.daa;

/** Counters are reset for every invocation. Algorithm objects are not thread-safe. */
public final class Metrics {
    public long recursiveCalls;
    public long dpCells;
    public long comparisons;
    public long lpsComparisons;
    public long auxCells;

    public void reset() {
        recursiveCalls = dpCells = comparisons = lpsComparisons = auxCells = 0;
    }

    public long totalComparisons() { return comparisons + lpsComparisons; }

    public Metrics copy() {
        Metrics m = new Metrics();
        m.recursiveCalls = recursiveCalls;
        m.dpCells = dpCells;
        m.comparisons = comparisons;
        m.lpsComparisons = lpsComparisons;
        m.auxCells = auxCells;
        return m;
    }

    public boolean sameAs(Metrics m) {
        return recursiveCalls == m.recursiveCalls && dpCells == m.dpCells
                && comparisons == m.comparisons && lpsComparisons == m.lpsComparisons
                && auxCells == m.auxCells;
    }
}
