# Assignment 4 - Dynamic Programming and String Algorithms

**Student:** Dyussenbay Damir  
**Group:** SE-2523  
**Course:** Design and Analysis of Algorithms  
**Instructor:** Taubakabyl Nurlybek

## Build and test

Install a **JDK 17 or newer** and **Maven 3.9+**. Ensure `JAVA_HOME` points to that JDK; Java 8 is not sufficient. Run these commands from the directory containing `pom.xml`:

```sh
mvn test
mvn package
java -Xms128m -Xmx512m -jar target/assignment4-1.0.jar
```

`mvn package` also executes the tests and produces an executable JAR. The first Maven build needs internet access for dependencies. The benchmark regenerates `results/results.csv` and `results/environment.properties`. To preserve the submitted measurements, use a separate directory:

```sh
java -Xms128m -Xmx512m -jar target/assignment4-1.0.jar results-rerun
```

The full benchmark is intentionally bounded. Recursive edit distance stops at n=12; increasing this limit on disjoint strings can make execution much slower.

## Rebuild the figures and report

Python 3.10+ is needed only for report generation, not for Java compilation or tests:

```sh
python -m pip install -r scripts/requirements.txt
python scripts/create_report.py
```

This reads the existing `results/results.csv`, checks its structure and deterministic counters, and recreates four PNG figures, `results/summary.csv`, `REPORT.md` and the four-page `REPORT.pdf`. It does not run the Java benchmark or invent timing data. The design card in `EXPERIMENT_DESIGN.md` was saved and committed before the first experiment.

## Contents

- `src/main/java/edu/daa/`: three edit-distance implementations, Naive, KMP, common interfaces, measured counters, fixed-seed input generator and experiment runner.
- `src/test/java/edu/daa/`: JUnit 5 reference, empty-input, overlap, random differential, LPS oracle and counter tests.
- `results/results.csv`: five recorded batches per configuration, one row per batch.
- `results/summary.csv`: medians and min/max times by configuration.
- `results/plots/`: four labeled PNG charts, including logarithmic axes.
- `results/environment.properties`: Java, operating system, seed and benchmark settings.
- `results/test-summary.txt`: actual Maven/JUnit verification summary.
- `EXPERIMENT_DESIGN.md`: original questions, predictions and experimental choices.
- `REPORT.md`, `REPORT.pdf`: report with the design card, results and short answers.
- `.git/`: local history on `main`, final tag `v1.0`.

## Algorithm contracts and complexity

`EditDistance.distance(String a, String b)` returns unit-cost Levenshtein distance. The three versions use the same signature. Table and TwoRows take O(n*m) time. Table keeps (n+1)*(m+1) integers; TwoRows keeps 2*(m+1) integers. Recursive deliberately has no memoization: exponential worst-case time and O(n+m) stack depth, with a linear equal-string path.

`StringSearch.search(String text, String pattern)` returns all zero-based starting positions in increasing order, including overlaps. Empty patterns throw `IllegalArgumentException`; longer-than-text patterns return an empty list. Null arguments throw `NullPointerException`. `KmpSearch.lps(String pattern)` is public; for an empty pattern the LPS helper returns an empty array, while search still rejects empty patterns. Naive has O((n-m+1)*m) worst-case time for m<=n. KMP has O(n+m) time and O(m) LPS storage, plus O(number of matches) result storage for both searches.

Indices and character operations use Java UTF-16 code units. Supplementary Unicode symbols can occupy two units. Objects hold reusable counters and are not thread-safe. No library matching algorithm is used in either implementation; `String.indexOf` appears only in the test oracle.

## What the counters mean

- `recursive_calls`: all recursive invocations, including base cases.
- `dp_cells`: evaluated interior DP states, excluding initialized boundaries.
- `comparisons`: edit-character comparisons or search text-to-pattern comparisons.
- `lps_comparisons`: pattern-to-pattern comparisons during KMP preprocessing.
- `total_comparisons`: the two comparison counters combined; includes preprocessing.
- `aux_cells`: simultaneously allocated algorithm-owned integer cells, counted at allocation. For DP these are table/row cells; for KMP they are LPS cells. This excludes array headers, references, input strings, output lists and recursion stack; zero does **not** mean zero JVM memory.

All counters are incremented inside the algorithms and reset for each invocation. `time_ms` is per-invocation elapsed milliseconds, calculated as `batch_time_ms / iterations`. Two warm-up batches precede five recorded batches. Input generation, reference validation and CSV writing are outside the timed region; algorithm allocation, counter increments, batch-loop overhead and KMP LPS construction are inside it. Each row's counters describe the last invocation, checked against warm-up and other recorded batches. `result` is the edit distance or the Java list hash code of search indices. All search experiment inputs in this run have no occurrences; successful and overlapping matches are covered by tests.

The custom SplitMix64 generator uses seed **20261107**, with a deterministic derived seed per size. Repeating the same command reproduces inputs, answers and counters. Exact wall-clock times are not reproducible and should not be claimed to be. Different input types are compared at the same size; recursive and large DP timings must not be compared as if their sizes were equal.

## Git and submission

The repository is on branch `main` and the submitted version has tag `v1.0`. Commits represent actual steps in creating the project; AI-assisted commits use an AI-assistant identity rather than pretending to be the student.

**GitHub repository:** [Domik-hub/DAA_Assignment4_Damir_Dyussenbay_SE-2523](https://github.com/Domik-hub/DAA_Assignment4_Damir_Dyussenbay_SE-2523)  
**Submission tag:** [v1.0](https://github.com/Domik-hub/DAA_Assignment4_Damir_Dyussenbay_SE-2523/tree/v1.0)

To obtain the exact submitted version:

```sh
git clone --branch v1.0 https://github.com/Domik-hub/DAA_Assignment4_Damir_Dyussenbay_SE-2523.git
cd DAA_Assignment4_Damir_Dyussenbay_SE-2523
mvn test
```

The archive is named `DAA_Assignment4_Damir_Dyussenbay_SE-2523.zip`; upload it to Moodle together with the GitHub repository link above. No Moodle upload has been performed.

## AI-use acknowledgement

OpenAI Codex was used to draft and implement the Java algorithms, tests, experiment harness, plotting script and report, and to run local validation. Timing values and counters in the submitted CSV were obtained by executing this code; they were not fabricated. The student should review the implementations, reproduce the experiment and be able to explain the recurrence, LPS fallback and conclusions before submitting. The assignment PDF supplied the requirements and reference values.
