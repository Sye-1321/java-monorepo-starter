package dev.example.app;

import dev.example.core.TextAnalysis;
import dev.example.core.TextAnalyzer;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length != 1) {
            err.println("Usage: text-analyzer <file>");
            return 2;
        }

        try {
            String text = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
            TextAnalysis result = TextAnalyzer.analyze(text);
            out.printf("Lines: %d%nWords: %d%nCode points: %d%n",
                    result.lines(), result.words(), result.codePoints());
            return 0;
        } catch (InvalidPathException | IOException e) {
            err.printf("Could not read '%s': %s%n", args[0], e.getMessage());
            return 1;
        }
    }
}
