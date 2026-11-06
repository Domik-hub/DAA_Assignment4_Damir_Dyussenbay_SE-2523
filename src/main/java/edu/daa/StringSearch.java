package edu.daa;

import java.util.List;

public interface StringSearch {
    List<Integer> search(String text, String pattern);
    Metrics metrics();
    String name();

    static void validate(String text, String pattern) {
        java.util.Objects.requireNonNull(text, "text");
        java.util.Objects.requireNonNull(pattern, "pattern");
        if (pattern.isEmpty()) throw new IllegalArgumentException("Pattern must not be empty");
    }
}
