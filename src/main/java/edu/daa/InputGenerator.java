package edu.daa;

/** Custom SplitMix64 generator: overflow is intentional; no external data needed. */
public final class InputGenerator {
    public static final long SEED = 20261107L;
    private long state;

    public InputGenerator(long seed) { state = seed; }

    public long nextLong() {
        long z = (state += 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        // Unsigned rejection avoids modulo bias for arbitrary alphabet sizes.
        long threshold = Long.remainderUnsigned(-((long) bound), bound);
        long value;
        do { value = nextLong(); } while (Long.compareUnsigned(value, threshold) < 0);
        return (int) Long.remainderUnsigned(value, bound);
    }

    public String word(int length, String alphabet) {
        if (alphabet.isEmpty()) throw new IllegalArgumentException("alphabet must not be empty");
        StringBuilder b = new StringBuilder(length);
        for (int i = 0; i < length; i++) b.append(alphabet.charAt(nextInt(alphabet.length())));
        return b.toString();
    }

    public static String[] edit(String type, int n) {
        InputGenerator g = new InputGenerator(SEED + 31L * n);
        return switch (type) {
            case "identical" -> { String a = g.word(n, "ABCD"); yield new String[]{a, a}; }
            case "random" -> new String[]{g.word(n, "ABCD"), g.word(n, "ABCD")};
            case "disjoint" -> new String[]{"A".repeat(n), "B".repeat(n)};
            default -> throw new IllegalArgumentException(type);
        };
    }

    public static String[] search(String type, int n, int m) {
        InputGenerator g = new InputGenerator(SEED + 31L * n + m);
        return switch (type) {
            case "random" -> new String[]{g.word(n, "ABCD"), g.word(m, "ABCD")};
            case "early_mismatch" -> new String[]{"A".repeat(n), "B".repeat(m)};
            case "adversarial" -> new String[]{"A".repeat(n), "A".repeat(m - 1) + "B"};
            default -> throw new IllegalArgumentException(type);
        };
    }
}
