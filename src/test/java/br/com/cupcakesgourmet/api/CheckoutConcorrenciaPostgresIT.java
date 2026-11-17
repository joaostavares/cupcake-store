package br.com.cupcakesgourmet.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mesmo teste de concorrência, agora no PostgreSQL real (SGBD de produção), além de validar que as migrations
 * Flyway e o mapeamento JPA (ddl-auto=validate) batem com o Postgres.
 * <p>
 * Usa Testcontainers quando há Docker. Sem Docker, pode apontar para um Postgres existente:
 * {@code PG_TEST_URL=jdbc:postgresql://localhost:5432/cupcakes_test PG_TEST_USER=cupcakes PG_TEST_PASSWORD=cupcakes}.
 * Sem nenhum dos dois, o teste é ignorado (skipped).
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIf("postgresDisponivel")
class CheckoutConcorrenciaPostgresIT extends CheckoutConcorrenciaTestBase {

    private static PostgreSQLContainer<?> container;

    @Autowired
    JdbcTemplate jdbc;

    static boolean postgresDisponivel() {
        if (System.getenv("PG_TEST_URL") != null) {
            return true;
        }
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        String url = System.getenv("PG_TEST_URL");
        if (url != null) {
            registry.add("spring.datasource.url", () -> url);
            registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("PG_TEST_USER", "cupcakes"));
            registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("PG_TEST_PASSWORD", "cupcakes"));
            return;
        }
        if (container == null) {
            container = new PostgreSQLContainer<>("postgres:16-alpine");
            container.start();
        }
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
    }

    @Test
    @DisplayName("migrations aplicadas no PostgreSQL e restrições CHECK ativas")
    void schemaNoPostgres() {
        assertThat(jdbc.queryForObject("select version()", String.class)).startsWith("PostgreSQL");
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class))
                .isGreaterThanOrEqualTo(3);
        assertThat(jdbc.queryForObject(
                "select count(*) from pg_constraint where conname in ('ck_pedido_valores','ck_item_subtotal','ck_cupcake_estoque')",
                Integer.class)).isEqualTo(3);
    }
}
