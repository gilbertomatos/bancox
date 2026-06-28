# CLAUDE.md — src/test/

## Regras desta camada

- Todo método público em service/ deve ter teste unitário correspondente
- Todo UC-XX deve ter teste de integração correspondente
- Testes unitários: usar Mockito para mockar repositories, testar apenas lógica do service
- Testes de integração: @SpringBootTest com banco real (H2 ou Testcontainers)
- Testes de repository: @DataJpaTest isolado

## Anotações obrigatórias por tipo

```java
// Unitário
@ExtendWith(MockitoExtension.class)        // NÃO usar @RunWith (JUnit 4)
class ContaServiceTest {
    @Mock ContaRepository contaRepository;
    @InjectMocks ContaService contaService;
}

// Integração
@SpringBootTest
@AutoConfigureMockMvc
@Transactional                             // rollback automático após cada teste
class TransferenciaControllerTest { }

// Repository
@DataJpaTest
class LancamentoRepositoryTest { }
```

## Nomenclatura
```
BOM:  deveRejeitarDebitoQuandoSaldoForInsuficiente()
RUIM: testDebito2()
```

## TDD — Regras quando usar fluxo Test-First (Processo.md seção 1b)

- Escrever testes consultando APENAS o UC correspondente e contratos-api.md
- NÃO consultar código existente ao escrever testes — testes refletem a spec, não a impl
- NÃO modificar testes após o Prompt 2 (implementação) — se falhar, corrigir o código
- Se o teste estiver errado por spec ambígua → corrigir UC-XX.md PRIMEIRO, depois o teste
- Toda assertion deve verificar comportamento observável — nunca estado interno

## Cobertura de Testes (DA-21)

### Metas
- service/     → mínimo 90% de linhas  (meta principal — onde vive a lógica de negócio)
- projeto geral → mínimo 80% de linhas (excluindo classes sem lógica testável)

### Imposição automática
JaCoCo verifica as metas em mvn verify.
Build falha se as metas não forem atingidas.
Relatório HTML gerado em target/site/jacoco/index.html

### Classes excluídas da cobertura (DA-21)
NÃO criar testes para satisfazer métrica nestas classes — são excluídas por design:

- BancoxApplication        → entry point, sem lógica testável
- config/*                 → beans de configuração Spring (SecurityConfig, SwaggerConfig, JwtAuthFilter)
- entity/*                 → entidades JPA, getters/setters gerados pelo Lombok
- dto/*                    → records imutáveis sem lógica
- exception/BancoxException → classe abstrata base sem lógica própria

### O que NÃO fazer (DA-21)
- NÃO criar testes que existem apenas para aumentar o número de cobertura
- NÃO testar getters e setters gerados pelo Lombok
- NÃO testar métodos de configuração de beans Spring
- NÃO fazer assertions vazias (chamar método sem verificar resultado)
- NÃO mockar o que não precisa ser mockado só para aumentar cobertura

## Cobertura obrigatória por UC

| UC    | Fluxo principal | Cada exceção documentada | Caso limite                      |
|-------|-----------------|--------------------------|----------------------------------|
| UC-00 | ✓               | ✓                        | token expirado, refresh inválido |
| UC-01 | ✓               | ✓                        | primeiro depósito (saldo zero)   |
| UC-02 | ✓               | ✓                        | débito exato do saldo disponível |
| UC-03 | ✓               | ✓                        | rollback com falha simulada      |
| UC-04 | ✓               | ✓                        | período > 90 dias truncado       |

## Teste obrigatório para UC-03 — rollback
Simular falha após débito (etapa 4a ok, crédito falha) e verificar:
- Saldo da conta_origem restaurado ao valor original
- Nenhum lançamento persistido em nenhuma das contas
- Resposta HTTP 500 com erro FALHA_TRANSACAO

```java
@Test
void deveReverterTransferenciaQuandoCreditoFalhar() {
    // given: mock de contaDestinoRepository lançando exceção no save
    // when: chamar transferenciaService.transferir(...)
    // then: verificar rollback — saldo origem não alterado, nenhum lançamento salvo
}
```

---

## Definition of Done

Nenhuma tarefa está concluída até que todos os itens abaixo sejam verificados.

### Código
- [ ] Implementação completa e funcional
- [ ] Sem erros de compilação (`mvn compile`)
- [ ] Sem warnings de Lombok ou JPA

### SAST — Análise Estática de Segurança (DA-27)
- [ ] SpotBugs sem bugs de prioridade MEDIUM ou HIGH (`mvn verify`)
- [ ] OWASP Dependency-Check sem CVEs com CVSS >= 7.0 (`mvn verify`)
- [ ] Gitleaks sem segredos detectados (`gitleaks detect --source . --config .gitleaks.toml`)
- [ ] Se falso positivo: adicionar exclusão documentada em spotbugs-exclude.xml ou dependency-check-suppressions.xml
- [ ] Relatórios SAST revisados: target/spotbugs.html e target/dependency-check-report.html

### Testes
- [ ] Testes unitários passando (`mvn test`)
- [ ] Testes de integração passando (`mvn verify`)
- [ ] Meta de cobertura atingida (`mvn verify` sem falha do JaCoCo)
- [ ] Teste de startup passando (`ApplicationStartupTest` — PostgreSQL real + Flyway)
- [ ] Nomenclatura dos testes espelha o fluxo do UC
- [ ] Casos-limite e exceções documentadas no UC cobertos

### Diagnóstico de template (obrigatório em correções de bug)
- [ ] Executei o checklist de diagnóstico do Processo.md seção 0?
- [ ] O bug foi causado por omissão ou erro no template? → corrigir o template ANTES do código
- [ ] Este erro pode se repetir em outros UCs ou entidades? → adicionar em arquitetura.md "Armadilhas conhecidas"

### Documentação — verificar se algum destes foi afetado e atualizar:
- [ ] Mudou comportamento de fluxo?          → atualizar UC-XX correspondente
- [ ] Mudou intenção ou critério de aceite?  → atualizar User Story no UC-XX
- [ ] Mudou entidade ou campo no banco?      → atualizar .claude/context/modelo-er/ + nova migration Flyway
- [ ] Mudou contrato de request/response?    → atualizar .claude/context/contratos-api.md + Swagger
- [ ] Mudou stack, camada ou convenção?      → atualizar .claude/context/arquitetura.md
- [ ] Mudou token, componente ou padrão UI?  → atualizar .claude/context/design-system.md
- [ ] Tomou decisão arquitetural relevante?  → adicionar DA-XX em decisoes-arquiteturais.md
- [ ] Qualquer arquivo em .claude/ foi alterado? → commitar com mensagem docs: descritiva
