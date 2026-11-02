package dev.example.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {
    @TempDir
    Path tempDir;

    @Test
    void analyzesUtf8File() throws Exception {
        Path file = Files.writeString(tempDir.resolve("sample.txt"), "hello 😀\nworld", StandardCharsets.UTF_8);
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();

        int exitCode = Main.run(new String[] {file.toString()}, stream(out), stream(err));

        assertEquals(0, exitCode);
        assertEquals("Lines: 2\nWords: 3\nCode points: 13\n", normalize(out));
        assertEquals("", err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void rejectsInvalidArgumentCount() {
        var err = new ByteArrayOutputStream();

        int exitCode = Main.run(new String[0], stream(new ByteArrayOutputStream()), stream(err));

        assertEquals(2, exitCode);
        assertTrue(err.toString(StandardCharsets.UTF_8).startsWith("Usage:"));
    }

    @Test
    void reportsReadFailure() {
        var err = new ByteArrayOutputStream();
        Path missing = tempDir.resolve("missing.txt");

        int exitCode = Main.run(new String[] {missing.toString()}, stream(new ByteArrayOutputStream()), stream(err));

        assertEquals(1, exitCode);
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("Could not read"));
    }

    @Test
    void reportsMalformedUtf8() throws Exception {
        Path file = Files.write(tempDir.resolve("malformed.txt"), new byte[] {(byte) 0xc3, (byte) 0x28});
        var err = new ByteArrayOutputStream();

        int exitCode = Main.run(new String[] {file.toString()}, stream(new ByteArrayOutputStream()), stream(err));

        assertEquals(1, exitCode);
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("Could not read"));
    }

    private static PrintStream stream(ByteArrayOutputStream output) {
        return new PrintStream(output, true, StandardCharsets.UTF_8);
    }

    private static String normalize(ByteArrayOutputStream output) {
        return output.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
