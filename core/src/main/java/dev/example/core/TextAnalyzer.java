package dev.example.core;

import java.util.Objects;

public final class TextAnalyzer {
    private TextAnalyzer() {
    }

    /**
     * Counts logical lines as defined by {@link String#lines()}, words as maximal
     * sequences of non-whitespace Unicode code points, and all Unicode code points.
     */
    public static TextAnalysis analyze(String text) {
        Objects.requireNonNull(text, "text");

        long words = 0;
        boolean inWord = false;
        var codePoints = text.codePoints().iterator();
        while (codePoints.hasNext()) {
            boolean whitespace = Character.isWhitespace(codePoints.nextInt());
            if (!whitespace && !inWord) {
                words++;
            }
            inWord = !whitespace;
        }

        return new TextAnalysis(text.lines().count(), words, text.codePointCount(0, text.length()));
    }
}
