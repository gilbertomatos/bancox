package com.bancox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Teste de startup — sobe o contexto Spring completo contra PostgreSQL real.
 *
 * Detecta erros que escapam dos testes com H2:
 *   - schema-validation (CHAR vs VARCHAR, ENUM vs tipo customizado)
 *   - migrations Flyway com SQL inválido para PostgreSQL
 *   - beans mal configurados (@PostConstruct, @EventListener)
 *   - JWT_SECRET ausente ou inválido
 *
 * DA-30: Testcontainers para teste de startup com banco real.
 * H2 não detecta incompatibilidades de tipo específicas do PostgreSQL.
 *
 * Tempo médio: 15-20s (container PostgreSQL + Flyway + contexto Spring).
 * Rodar em: mvn verify (integrado ao CI).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ApplicationStartupTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Sobrescrever driver H2 definido no application.yml de teste
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Habilitar Flyway — desabilitado nos testes H2 (application.yml de teste)
        registry.add("spring.flyway.enabled", () -> "true");

        // Validar schema após Flyway rodar — detecta incompatibilidades de tipo
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        // JWT_SECRET obrigatório — sem valor default em produção (DA-14)
        registry.add("app.jwt.secret",
                () -> "test-secret-minimo-256-bits-para-startup-test-aaaa");
    }

    @Test
    void contextLoads() {
        // Se chegou aqui:
        //   ✓ Flyway aplicou todas as migrations sem erro
        //   ✓ Hibernate validou o schema contra as entidades JPA
        //   ✓ Todos os beans foram criados sem exceção
        //   ✓ JWT_SECRET foi aceito pelo JwtService
    }
}
