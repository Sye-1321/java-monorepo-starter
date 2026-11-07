package dev.example.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Testcontainers
class AnalysisApiIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18.4-bookworm");

    @Autowired
    RestTestClient client;

    @Autowired
    JdbcClient jdbcClient;

    @BeforeEach
    void deleteAnalyses() {
        jdbcClient.sql("DELETE FROM analyses").update();
    }

    @Test
    void postPersistsAnalysisAndGetRetrievesIt() {
        AnalysisResponse created = post("Hello world");

        assertThat(created.id()).isNotNull();
        assertThat(created.lines()).isEqualTo(1);
        assertThat(created.words()).isEqualTo(2);
        assertThat(created.codePoints()).isEqualTo(11);
        assertThat(created.createdAt()).isNotNull();

        client.get().uri("/api/v1/analyses/{id}", created.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(AnalysisResponse.class).isEqualTo(created);
    }

    @Test
    void preservesUnicodeAndEmptyTextBehavior() {
        AnalysisResponse unicode = post("Hello 🌍\n你好");
        AnalysisResponse empty = post("");

        assertThat(unicode.lines()).isEqualTo(2);
        assertThat(unicode.words()).isEqualTo(3);
        assertThat(unicode.codePoints()).isEqualTo(10);
        assertThat(empty.lines()).isZero();
        assertThat(empty.words()).isZero();
        assertThat(empty.codePoints()).isZero();
    }

    @Test
    void returnsNotFoundForUnknownAnalysis() {
        client.get().uri("/api/v1/analyses/{id}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void rejectsInvalidRequests() {
        client.post().uri("/api/v1/analyses")
                .body(Map.of())
                .exchange()
                .expectStatus().isBadRequest();
        client.post().uri("/api/v1/analyses")
                .header("Content-Type", "application/json")
                .body("{\"text\":null}")
                .exchange()
                .expectStatus().isBadRequest();
        client.post().uri("/api/v1/analyses")
                .header("Content-Type", "application/json")
                .body("{\"text\":")
                .exchange()
                .expectStatus().isBadRequest();
        client.get().uri("/api/v1/analyses/not-a-uuid")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void flywayAppliesMigrations() {
        Integer migrations = jdbcClient.sql("""
                        SELECT count(*) FROM flyway_schema_history
                        WHERE success AND version IN ('1', '2')
                        """)
                .query(Integer.class)
                .single();

        assertThat(migrations).isEqualTo(2);
    }

    @Test
    void listsAnEmptyResult() {
        AnalysisPageResponse page = list(20, null);

        assertThat(page.items()).isEmpty();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void defaultsToTwentyItems() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        for (int index = 1; index <= 21; index++) {
            insert(id(index), createdAt.minusSeconds(index));
        }

        AnalysisPageResponse page = client.get().uri("/api/v1/analyses")
                .exchange()
                .expectStatus().isOk()
                .expectBody(AnalysisPageResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(page.items()).hasSize(20);
        assertThat(page.nextCursor()).isNotNull();
    }

    @Test
    void listsNewestFirstWithIdAsTieBreaker() {
        Instant newer = Instant.parse("2026-01-02T00:00:00Z");
        Instant same = Instant.parse("2026-01-01T00:00:00Z");
        UUID newest = id(1);
        UUID largerTie = id(3);
        UUID smallerTie = id(2);
        UUID oldest = id(4);
        insert(oldest, same.minusSeconds(1));
        insert(smallerTie, same);
        insert(newest, newer);
        insert(largerTie, same);

        AnalysisPageResponse page = list(20, null);

        assertThat(page.items()).extracting(AnalysisResponse::id)
                .containsExactly(newest, largerTie, smallerTie, oldest);
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void paginatesWithoutDuplicatesOrMissingRecords() {
        Instant createdAt = Instant.parse("2026-01-05T00:00:00Z");
        for (int index = 1; index <= 5; index++) {
            insert(id(index), createdAt);
        }

        List<UUID> actual = new ArrayList<>();
        String cursor = null;
        do {
            AnalysisPageResponse page = list(2, cursor);
            actual.addAll(page.items().stream().map(AnalysisResponse::id).toList());
            cursor = page.nextCursor();
        } while (cursor != null);

        assertThat(actual).containsExactly(id(5), id(4), id(3), id(2), id(1));
    }

    @Test
    void newRecordBetweenPageRequestsDoesNotShiftLaterPages() {
        Instant base = Instant.parse("2026-01-05T00:00:00Z");
        UUID first = id(1);
        UUID second = id(2);
        UUID third = id(3);
        insert(first, base);
        insert(second, base.minusSeconds(1));
        insert(third, base.minusSeconds(2));

        AnalysisPageResponse firstPage = list(2, null);
        insert(id(4), base.plusSeconds(1));
        AnalysisPageResponse secondPage = list(2, firstPage.nextCursor());

        assertThat(firstPage.items()).extracting(AnalysisResponse::id).containsExactly(first, second);
        assertThat(secondPage.items()).extracting(AnalysisResponse::id).containsExactly(third);
        assertThat(secondPage.nextCursor()).isNull();
    }

    @Test
    void rejectsInvalidListingParameters() {
        for (String query : List.of("size=0", "size=101", "size=not-a-number")) {
            client.get().uri("/api/v1/analyses?" + query)
                    .exchange()
                    .expectStatus().isBadRequest();
        }
        for (String cursor : List.of(
                "%%%",
                encoded("malformed"),
                encoded("not-a-timestamp\n" + id(1)),
                encoded("2026-01-01T00:00:00Z\nnot-a-uuid"))) {
            client.get().uri(builder -> builder.path("/api/v1/analyses")
                            .queryParam("cursor", cursor)
                            .build())
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }

    private AnalysisResponse post(String text) {
        EntityExchangeResult<AnalysisResponse> result = client.post().uri("/api/v1/analyses")
                .body(Map.of("text", text))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody(AnalysisResponse.class)
                .returnResult();
        AnalysisResponse response = result.getResponseBody();
        assertThat(response).isNotNull();
        assertThat(result.getResponseHeaders().getLocation())
                .hasToString("/api/v1/analyses/" + response.id());
        return response;
    }

    private AnalysisPageResponse list(int size, String cursor) {
        return client.get().uri(builder -> {
                    builder.path("/api/v1/analyses").queryParam("size", size);
                    if (cursor != null) {
                        builder.queryParam("cursor", cursor);
                    }
                    return builder.build();
                })
                .exchange()
                .expectStatus().isOk()
                .expectBody(AnalysisPageResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private void insert(UUID id, Instant createdAt) {
        jdbcClient.sql("""
                        INSERT INTO analyses (id, lines, words, code_points, created_at)
                        VALUES (:id, 1, 1, 1, :createdAt)
                        """)
                .param("id", id)
                .param("createdAt", createdAt.atOffset(ZoneOffset.UTC))
                .update();
    }

    private static UUID id(int suffix) {
        return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(suffix));
    }

    private static String encoded(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
