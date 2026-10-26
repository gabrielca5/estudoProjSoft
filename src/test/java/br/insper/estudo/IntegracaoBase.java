package br.insper.estudo;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base dos testes de INTEGRAÇÃO (Aula 06).
 * Sobe um Postgres DE VERDADE num container Docker (Testcontainers) e aponta o Spring para ele.
 * O container é estático e iniciado uma vez só, reaproveitado por todas as classes de teste.
 * Precisa do Docker rodando na máquina (no GitHub Actions já tem).
 */
public abstract class IntegracaoBase {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:15-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("fila.worker.enabled", () -> "false"); // sem Redis nos testes
    }
}
