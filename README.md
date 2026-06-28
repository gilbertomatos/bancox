# BancoX

Sistema bancário com operações de crédito, débito, transferência e extrato.

> **Este projeto foi integralmente gerado com IA.**
> Todo o código — backend, frontend, testes, migrations, configuração de SAST e documentação —
> foi escrito pelo [Claude Code](https://claude.ai/code) (Anthropic) a partir de casos de uso
> e decisões arquiteturais definidos em `.claude/`.

<details>
<summary>Ver o prompt usado para gerar o código</summary>

```
Implemente todos os casos de uso conforme documentado.

Antes de escrever qualquer código, leia:
- .claude/casos-de-uso/UC-XX-nome.md
- .claude/context/modelo-er/entidade-NOME.md (se nova entidade)
- .claude/context/contratos-api.md (novos erros e envelopes)
- .claude/context/decisoes-arquiteturais.md (DAs relevantes)

Implemente nesta ordem:

## 1. Migration Flyway
Criar src/main/resources/db/migration/VN__descricao.sql
- [campos e tabelas conforme entidade-NOME.md]

## 2. Entidades JPA
- Criar/atualizar [NomeEntity]
- Lombok: @Getter + @NoArgsConstructor + @Builder — NÃO @Data (DA-09)

## 3. Repositório
- Criar [NomeRepository] extends JpaRepository
- [métodos de query necessários]

## 4. Exceptions
- Criar [NomeException] para cada erro do UC
- Registrar no GlobalExceptionHandler com HTTP codes de contratos-api.md

## 5. Service
- [NomeService].[método](params)
- Prioridade de validação: [listar na ordem correta]
- Usar @Transactional em operações de escrita

## 6. Controller
- [METHOD] /endpoint
- Anotar com @Operation e @ApiResponse (DA-12)
- Validar entrada com @Valid e Bean Validation

## 7. Testes unitários (JUnit 5 + Mockito)
@ExtendWith(MockitoExtension.class)
Cobrir: fluxo principal + cada exceção do UC + casos-limite

## 8. Teste de integração
@SpringBootTest + @AutoConfigureMockMvc + @Transactional
Cobrir: fluxo completo do HTTP ao banco

## Ao finalizar
- Verificar o DoD em src/test/java/com/bancox/CLAUDE.md
- Executar SAST: `mvn verify` (SpotBugs + Dependency-Check)
- Executar Gitleaks: `gitleaks detect --source . --config .gitleaks.toml`
- Revisar relatórios em target/ antes de considerar concluído
```

</details>

## O que foi gerado pelo Claude Code

Todo o conteúdo deste repositório foi produzido a partir de um único prompt (ver abaixo) aplicado sobre uma base de documentação estruturada em `.claude/`. Nenhuma linha de código foi escrita manualmente.

### Documentação de entrada (`.claude/`) — o harness

| Arquivo | Conteúdo |
|---|---|
| `CLAUDE.md` | Identidade do agente, glossário, regras de negócio, fallbacks globais |
| `context/arquitetura.md` | Stack completa, estrutura de pacotes, princípios SOLID aplicados, armadilhas conhecidas |
| `context/autenticacao-autorizacao.md` | Fluxo JWT cookie, perfis, mapeamento de endpoints, regras OWASP |
| `context/contratos-api.md` | Envelopes de resposta e catálogo completo de erros |
| `context/design-system.md` | Tokens Bootstrap, componentes, mapa de telas, padrões Thymeleaf |
| `context/decisoes-arquiteturais.md` | 18 decisões técnicas documentadas (DA-01 a DA-30) com contexto, motivo e restrições |
| `context/modelo-er/` | 5 entidades com campos, relacionamentos e regras de integridade |
| `casos-de-uso/UC-00…UC-04` | 5 casos de uso de negócio com user story, fluxo, exceções e exemplos |
| `casos-de-uso/UC-UI-01…UC-UI-05` | 5 casos de uso de interface com estados de tela e dados do Model |

### Código gerado — o que saiu

**Backend (56 arquivos Java)**
- 5 entidades JPA · 5 repositories · 7 services · 7 controllers (REST + Thymeleaf)
- 14 exceptions customizadas + `GlobalExceptionHandler`
- `JwtCookieFilter` · `SecurityConfig` · `UserPrincipal` · `LoginRateLimiter` (Bucket4j)
- 4 DTOs de request · 5 DTOs de response · `TipoLancamentoConverter`

**Frontend server-side (9 templates Thymeleaf)**
- `login.html` · `dashboard.html` · `extrato.html`
- `transferencia.html` · `transferencia-confirmacao.html` · `transferencia-comprovante.html`
- `gestao-contas.html` · `layout.html` (fragment) · `alert.html` (fragment)

**Banco de dados (6 migrations Flyway)**
- V1–V5: schema completo com correções incrementais
- V6: seed com dados de demonstração

**Testes (72 testes em 11 classes)**
- Unitários com Mockito: `ContaServiceTest` · `TransferenciaServiceTest` · `ExtratoServiceTest` · `AuthServiceTest` · `ContaGestaoServiceTest` · `LoginRateLimiterTest`
- Integração com `@SpringBootTest`: `ContaControllerTest` · `TransferenciaControllerTest` · `AdminContaControllerTest` · `GlobalExceptionHandlerTest`
- Startup com PostgreSQL real via Testcontainers: `ApplicationStartupTest`

**Qualidade e segurança**
- `pom.xml` com JaCoCo (metas por pacote), SpotBugs + Find Security Bugs, OWASP Dependency-Check
- `spotbugs-exclude.xml` · `dependency-check-suppressions.xml` · `.gitleaks.toml`
- `application-prod.yml` (Swagger desabilitado, graceful shutdown, virtual threads)

### Métricas finais

| Métrica | Valor |
|---|---|
| Testes passando | 72 / 72 |
| Cobertura `service/` | 96,9% |
| Cobertura global | 93,1% |
| Bugs SpotBugs | 0 |
| CVEs CVSS ≥ 7,0 | 0 |
| Leaks Gitleaks | 0 |

---

## Stack

**Backend:** Java 21 · Spring Boot 3.3 · Maven · Lombok · Flyway · PostgreSQL · Swagger  
**Frontend:** Thymeleaf (SSR) · Bootstrap 5 · HTMX · Chart.js (via CDN — DA-24)  
**Segurança:** Spring Security · JWT em cookie httpOnly · BCrypt · Bucket4j (rate limiting)  
**Testes:** JUnit 5 · Mockito · Spring Boot Test · Testcontainers  
**SAST:** SpotBugs + Find Security Bugs · OWASP Dependency-Check · Gitleaks · JaCoCo

## Início rápido

```bash
# Variável de ambiente obrigatória (mínimo 256 bits — DA-14)
export JWT_SECRET="chave-local-de-desenvolvimento-minimo-256-bits-aaaa"

# Subir infraestrutura
docker compose up

# Backend isolado (requer PostgreSQL local ou via Docker)
mvn spring-boot:run

# Swagger UI (apenas em dev — desabilitado em produção por DA-16)
http://localhost:8080/swagger-ui.html
```

## Comandos Maven

```bash
mvn compile          # compilar
mvn test             # testes unitários + integração
mvn verify           # testes + JaCoCo + SpotBugs + OWASP Dependency-Check
mvn clean package    # gerar jar
mvn flyway:info      # status das migrations
```

## Resultados SAST (última verificação: 2026-06-28)

| Ferramenta | Resultado |
|---|---|
| JaCoCo `service/` | 96.9% (meta ≥ 90%) |
| JaCoCo global | 93.1% (meta ≥ 80%) |
| SpotBugs | 0 bugs |
| OWASP Dependency-Check | sem CVEs com CVSS ≥ 7.0 |
| Gitleaks | sem leaks |

## Documentação do projeto

A documentação vive em `.claude/` e é lida automaticamente pelo Claude Code.

| Arquivo | Conteúdo |
|---|---|
| `CLAUDE.md` | Índice e identidade do agente |
| `.claude/context/arquitetura.md` | Stack e convenções |
| `.claude/context/contratos-api.md` | Envelopes e códigos de erro |
| `.claude/context/design-system.md` | Tokens e componentes UI |
| `.claude/context/decisoes-arquiteturais.md` | Porquê das decisões |
| `.claude/context/modelo-er/` | Entidades e relacionamentos |
| `.claude/casos-de-uso/` | Fluxos, exceções e exemplos |

## Dados de demonstração

A migration `V6__seed_demo_data.sql` popula o banco com usuários fictícios prontos para uso:

| Usuário | CPF | Senha | Perfil | Saldo |
|---|---|---|---|---|
| Ana Lima | `11122233344` | `Bancox@123` | CORRENTISTA | R$ 4.550,00 |
| Bruno Costa | `55566677788` | `Bancox@123` | CORRENTISTA | R$ 1.700,00 |
| Carla Souza | `99900011122` | `Bancox@123` | CORRENTISTA | R$ 200,00 (conta **bloqueada**) |
| Demo Operador | `12345678900` | `Operador@1` | OPERADOR | — |

O extrato já contém lançamentos de crédito, débito e uma transferência entre Ana e Bruno.

## Como desenvolver uma nova feature

Consulte `Processo.md` — seção "1. Nova Feature".
