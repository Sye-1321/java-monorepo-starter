package dev.example.api;

import dev.example.core.TextAnalysis;
import dev.example.core.TextAnalyzer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/analyses")
class AnalysisController {
    private static final int MAX_PAGE_SIZE = 100;

    private final AnalysisRepository repository;

    AnalysisController(AnalysisRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    ResponseEntity<AnalysisResponse> analyze(@Valid @RequestBody AnalysisRequest request) {
        TextAnalysis analysis = TextAnalyzer.analyze(request.text());
        StoredAnalysis stored = repository.save(analysis);
        URI location = URI.create("/api/v1/analyses/" + stored.id());
        return ResponseEntity.created(location).body(AnalysisResponse.from(stored));
    }

    @GetMapping("/{id}")
    AnalysisResponse get(@PathVariable UUID id) {
        return repository.findById(id)
                .map(AnalysisResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    AnalysisPageResponse list(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String cursor) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        AnalysisCursor decodedCursor = cursor == null ? null : decodeCursor(cursor);
        List<StoredAnalysis> results = repository.findAll(decodedCursor, size + 1);
        boolean hasNextPage = results.size() > size;
        List<StoredAnalysis> page = hasNextPage ? results.subList(0, size) : results;
        String nextCursor = hasNextPage ? encodeCursor(page.getLast()) : null;
        return new AnalysisPageResponse(page.stream().map(AnalysisResponse::from).toList(), nextCursor);
    }

    private static String encodeCursor(StoredAnalysis analysis) {
        String value = analysis.createdAt() + "\n" + analysis.id();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static AnalysisCursor decodeCursor(String cursor) {
        try {
            String value = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int separator = value.indexOf('\n');
            if (separator < 1 || separator != value.lastIndexOf('\n')) {
                throw new IllegalArgumentException();
            }
            return new AnalysisCursor(
                    Instant.parse(value.substring(0, separator)),
                    UUID.fromString(value.substring(separator + 1)));
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
}

record AnalysisRequest(@NotNull String text) {
}

record AnalysisResponse(UUID id, long lines, long words, long codePoints, Instant createdAt) {
    static AnalysisResponse from(StoredAnalysis analysis) {
        return new AnalysisResponse(analysis.id(), analysis.lines(), analysis.words(),
                analysis.codePoints(), analysis.createdAt());
    }
}

record AnalysisPageResponse(List<AnalysisResponse> items, String nextCursor) {
}
