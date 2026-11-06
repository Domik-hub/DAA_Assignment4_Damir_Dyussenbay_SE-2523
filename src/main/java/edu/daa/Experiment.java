package edu.daa;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Properties;
import java.util.function.IntSupplier;
import java.util.stream.IntStream;

public final class Experiment {
    private static volatile long sink;
    private static final int RUNS = 5;
    private static final int WARMUPS = 2;

    private Experiment() { }

    public static void main(String[] args) throws IOException {
        Path output = Path.of(args.length == 0 ? "results" : args[0]);
        Files.createDirectories(output);
        Properties env = new Properties();
        for (String key : new String[]{"java.version", "java.vm.name", "os.name", "os.version", "os.arch"})
            env.setProperty(key, System.getProperty(key));
        env.setProperty("seed", Long.toString(InputGenerator.SEED));
        env.setProperty("started_utc", Instant.now().toString());
        env.setProperty("available_processors", Integer.toString(Runtime.getRuntime().availableProcessors()));
        env.setProperty("max_heap_bytes", Long.toString(Runtime.getRuntime().maxMemory()));
        env.setProperty("warmup_batches", Integer.toString(WARMUPS));
        env.setProperty("measured_batches", Integer.toString(RUNS));
        try (var writer = Files.newBufferedWriter(output.resolve("environment.properties"), StandardCharsets.UTF_8)) {
            env.store(writer, "Benchmark environment; elapsed time is not deterministic");
        }

        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(output.resolve("results.csv"), StandardCharsets.UTF_8))) {
            csv.println("family,algorithm,input,n,m,run,seed,iterations,batch_time_ms,time_ms,recursive_calls,dp_cells,comparisons,lps_comparisons,total_comparisons,aux_cells,result");
            for (EditDistance algorithm : new EditDistance[]{new RecursiveDistance(), new TableDistance(), new TwoRowDistance()}) {
                boolean recursive = algorithm instanceof RecursiveDistance;
                int[] sizes = recursive ? new int[]{3, 6, 9, 12} : new int[]{128, 256, 512, 1024, 2048};
                for (String input : new String[]{"identical", "random", "disjoint"}) {
                    for (int n : sizes) {
                        String[] pair = InputGenerator.edit(input, n);
                        int expected = new TableDistance().distance(pair[0], pair[1]);
                        int iterations = recursive ? switch (n) { case 3 -> 10000; case 6 -> 100; case 9 -> 2; default -> 1; }
                                : Math.max(1, 1048576 / (n * n));
                        measure(csv, "edit", algorithm.name(), input, n, n, iterations,
                                () -> algorithm.distance(pair[0], pair[1]), algorithm.metrics(), expected);
                    }
                }
            }
            for (String input : new String[]{"random", "early_mismatch", "adversarial"}) {
                for (int n : new int[]{4096, 16384, 65536, 262144}) {
                    String[] pair = InputGenerator.search(input, n, 64);
                    var expected = new NaiveSearch().search(pair[0], pair[1]);
                    for (StringSearch algorithm : new StringSearch[]{new NaiveSearch(), new KmpSearch()}) {
                        if (!expected.equals(algorithm.search(pair[0], pair[1])))
                            throw new AssertionError("Search answers differ");
                        measure(csv, "search", algorithm.name(), input, n, 64, 1048576 / n,
                                () -> algorithm.search(pair[0], pair[1]).hashCode(), algorithm.metrics(), expected.hashCode());
                    }
                }
            }
            if (csv.checkError()) throw new IOException("Could not write complete CSV");
        }
        System.out.println("Saved " + output.resolve("results.csv") + "; consumed checksum=" + sink);
    }

    private static long batch(IntSupplier operation, int iterations) {
        long sum = 0;
        for (int i = 0; i < iterations; i++) sum += operation.getAsInt();
        return sum;
    }

    private static void measure(PrintWriter csv, String family, String name, String input,
                                int n, int m, int iterations, IntSupplier operation,
                                Metrics metrics, int expected) {
        for (int w = 0; w < WARMUPS; w++) sink = batch(operation, iterations);
        Metrics reference = metrics.copy();
        double[] times = new double[RUNS];
        for (int run = 1; run <= RUNS; run++) {
            long start = System.nanoTime();
            long sum = batch(operation, iterations);
            long elapsed = System.nanoTime() - start;
            sink = sum;
            // Validation and CSV writing occur outside the measured interval.
            if (sum != (long) expected * iterations) throw new AssertionError("Incorrect result: " + name);
            if (!metrics.sameAs(reference)) throw new AssertionError("Non-deterministic counters: " + name);
            double batchMs = elapsed / 1_000_000.0;
            double perCallMs = batchMs / iterations;
            if (perCallMs <= 0) throw new AssertionError("Timer resolution too small");
            times[run - 1] = perCallMs;
            csv.printf(Locale.ROOT, "%s,%s,%s,%d,%d,%d,%d,%d,%.9f,%.9f,%d,%d,%d,%d,%d,%d,%d%n",
                    family, name, input, n, m, run, InputGenerator.SEED, iterations, batchMs, perCallMs,
                    metrics.recursiveCalls, metrics.dpCells, metrics.comparisons, metrics.lpsComparisons,
                    metrics.totalComparisons(), metrics.auxCells, expected);
        }
        double median = IntStream.range(0, RUNS).mapToDouble(i -> times[i]).sorted().toArray()[RUNS / 2];
        System.out.printf(Locale.ROOT, "%s %-14s n=%-7d median=%.6f ms%n", name, input, n, median);
        csv.flush();
    }
}
