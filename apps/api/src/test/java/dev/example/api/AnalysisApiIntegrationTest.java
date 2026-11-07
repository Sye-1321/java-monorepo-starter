package dev.example.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
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
    void flywayAppliesInitialMigration() {
        Integer migrations = jdbcClient.sql("""
                        SELECT count(*) FROM flyway_schema_history
                        WHERE success AND version = '1'
                        """)
                .query(Integer.class)
                .single();

        assertThat(migrations).isEqualTo(1);
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
}
