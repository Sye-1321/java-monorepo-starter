package dev.example.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TextAnalyzerTest {
    @Test
    void analyzesEmptyText() {
        assertEquals(new TextAnalysis(0, 0, 0), TextAnalyzer.analyze(""));
    }

    @Test
    void countsLogicalLinesAndWords() {
        assertEquals(new TextAnalysis(3, 4, 20), TextAnalyzer.analyze("one two\r\nthree\rfour\n"));
    }

    @Test
    void countsSupplementaryCharactersAsSingleCodePoints() {
        assertEquals(new TextAnalysis(1, 2, 3), TextAnalyzer.analyze("A 😀"));
    }

    @Test
    void usesUnicodeWhitespaceForWordBoundaries() {
        assertEquals(new TextAnalysis(1, 3, 5), TextAnalyzer.analyze("a\tb\u2003c"));
    }

    @Test
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> TextAnalyzer.analyze(null));
    }
}
