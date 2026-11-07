package dev.example.api;

import dev.example.core.TextAnalysis;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class AnalysisRepository {
    private static final String COLUMNS = "id, lines, words, code_points, created_at";

    private final JdbcClient jdbcClient;

    AnalysisRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    StoredAnalysis save(TextAnalysis analysis) {
        return jdbcClient.sql("""
                        INSERT INTO analyses (id, lines, words, code_points)
                        VALUES (:id, :lines, :words, :codePoints)
                        RETURNING %s
                        """.formatted(COLUMNS))
                .param("id", UUID.randomUUID())
                .param("lines", analysis.lines())
                .param("words", analysis.words())
                .param("codePoints", analysis.codePoints())
                .query(AnalysisRepository::mapRow)
                .single();
    }

    Optional<StoredAnalysis> findById(UUID id) {
        return jdbcClient.sql("SELECT " + COLUMNS + " FROM analyses WHERE id = :id")
                .param("id", id)
                .query(AnalysisRepository::mapRow)
                .optional();
    }

    private static StoredAnalysis mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        return new StoredAnalysis(
                resultSet.getObject("id", UUID.class),
                resultSet.getLong("lines"),
                resultSet.getLong("words"),
                resultSet.getLong("code_points"),
                resultSet.getObject("created_at", OffsetDateTime.class).toInstant());
    }
}

record StoredAnalysis(UUID id, long lines, long words, long codePoints, Instant createdAt) {
}
