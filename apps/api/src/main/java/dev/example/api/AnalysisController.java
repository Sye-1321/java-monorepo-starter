package dev.example.api;

import dev.example.core.TextAnalysis;
import dev.example.core.TextAnalyzer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/analyses")
class AnalysisController {
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
}

record AnalysisRequest(@NotNull String text) {
}

record AnalysisResponse(UUID id, long lines, long words, long codePoints, Instant createdAt) {
    static AnalysisResponse from(StoredAnalysis analysis) {
        return new AnalysisResponse(analysis.id(), analysis.lines(), analysis.words(),
                analysis.codePoints(), analysis.createdAt());
    }
}
