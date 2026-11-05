package dev.example.api;

import dev.example.core.TextAnalysis;
import dev.example.core.TextAnalyzer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analyses")
class AnalysisController {
    @PostMapping
    AnalysisResponse analyze(@Valid @RequestBody AnalysisRequest request) {
        TextAnalysis analysis = TextAnalyzer.analyze(request.text());
        return new AnalysisResponse(analysis.lines(), analysis.words(), analysis.codePoints());
    }
}

record AnalysisRequest(@NotNull String text) {
}

record AnalysisResponse(long lines, long words, long codePoints) {
}
