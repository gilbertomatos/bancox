# decisoes-arquiteturais.md — Decisões Arquiteturais BancoX
> Registra o PORQUÊ das decisões técnicas e de negócio.
> Consulte antes de propor mudanças. NÃO reverta decisões sem entender o motivo.

---

## Formato de cada decisão

```
# DA-XX: Título
Data      : YYYY-MM
Status    : ativo | substituído por DA-YY | em revisão
Contexto  : situação que gerou a decisão
Decisão   : o que foi decidido
Motivo    : por que esta opção e não outra
Consequências : o que isso implica para o código
NÃO FAZER : o que explicitamente não deve ser feito por causa desta decisão
```

---

## DA-01: Spring Boot como framework web

```
Data      : 2026-06
Status    : ativo
Contexto  : escolha do framework HTTP para o backend Java
Decisão   : usar Spring Boot 3.3.x com Spring MVC
Motivo    : ecossistema maduro para sistemas financeiros; integração nativa com
            JPA, Flyway, Swagger e gerenciamento de transações via @Transactional;
            Java 21 LTS + Spring Boot 3 suportam records e virtual threads
Consequências : controllers anotados com @RestController; injeção via construtor
                (não @Autowired em campo); application.yml como config principal
NÃO FAZER : NÃO usar @Autowired em campo — usar injeção via construtor sempre —
            NÃO misturar Spring MVC com Spring WebFlux
```

---

## DA-02: Flyway para migrações de banco

```
Data      : 2026-06
Status    : ativo
Contexto  : gerenciamento de schema do banco de dados
Decisão   : usar Flyway 10.x com scripts SQL versionados
Motivo    : migrações versionadas e rastreáveis em src/main/resources/db/migration;
            histórico de mudanças auditável; rollback explícito via scripts de undo;
            integração automática com Spring Boot no startup
Consequências : toda mudança de schema gera novo arquivo V{N}__{descricao}.sql;
                nunca alterar migration já aplicada em produção
NÃO FAZER : NÃO editar migration já aplicada — NÃO usar ddl-auto=update em produção
```

---

## DA-03: LANCAMENTO é imutável

```
Data      : 2026-06
Status    : ativo
Contexto  : modelagem do histórico de operações financeiras
Decisão   : registros de LANCAMENTO nunca recebem UPDATE ou DELETE após INSERT
Motivo    : requisito regulatório bancário — auditoria exige histórico imutável;
            qualquer alteração posterior quebraria a rastreabilidade de saldo_apos
            e invalidaria reconciliações financeiras
Consequências : erros de lançamento são corrigidos com novo lançamento de estorno,
                nunca editando o original; entity Lancamento sem setters via Lombok
NÃO FAZER : NÃO adicionar UPDATE/DELETE em LANCAMENTO — @Getter apenas na entidade (sem @Data)
```

---

## DA-04: Transação atômica obrigatória em transferências

```
Data      : 2026-06
Status    : ativo
Contexto  : implementação da operação de transferência entre contas
Decisão   : débito e crédito ocorrem dentro de uma única transação via @Transactional
Motivo    : falha parcial (débito ok, crédito falhou) geraria inconsistência
            financeira irrecuperável — dinheiro desapareceria do sistema
Consequências : TransferenciaService.transferir() anotado com @Transactional;
                qualquer RuntimeException dispara rollback automático do Spring
NÃO FAZER : NÃO separar débito e crédito em métodos @Transactional independentes —
            NÃO usar propagação REQUIRES_NEW sem DA explícita aprovando
```

---

## DA-05: Validação de todas as condições antes de qualquer escrita

```
Data      : 2026-06
Status    : ativo
Contexto  : ordem das operações em service/
Decisão   : validar conta, valor e saldo ANTES de iniciar qualquer escrita no banco
Motivo    : evitar estados parciais onde uma escrita ocorreu mas validação seguinte falhou;
            mais simples de testar com Mockito; erros retornam sem efeitos colaterais
Consequências : prioridade de validação é sempre:
                1. conta existe → 2. valor válido → 3. saldo suficiente → 4. execução
NÃO FAZER : NÃO intercalar validações com escritas —
            NÃO otimizar buscando registro uma só vez se quebrar a ordem de validação
```

---

## DA-06: Saldo não pode ser negativo (sem cheque especial)

```
Data      : 2026-06
Status    : ativo
Contexto  : regra de negócio central do BancoX
Decisão   : saldo nunca fica abaixo de zero — operação rejeitada com SALDO_INSUFICIENTE
Motivo    : BancoX não oferece crédito automático nesta versão;
            garantido em duas camadas: CHECK >= 0 no banco + validação em ContaService
Consequências : qualquer débito ou transferência que resultaria em saldo negativo é bloqueado
NÃO FAZER : NÃO remover o CHECK do banco mesmo que a validação em código pareça suficiente —
            NÃO implementar limite de crédito sem uma DA explícita aprovando a mudança
```

---

## DA-07: Erros técnicos não chegam ao cliente

```
Data      : 2026-06
Status    : ativo
Contexto  : tratamento de erros na camada controller/
Decisão   : GlobalExceptionHandler (@RestControllerAdvice) intercepta todas as exceptions
            e mapeia para envelopes padronizados (ver contratos-api.md)
Motivo    : segurança — stack traces expõem estrutura interna e vetores de ataque;
            experiência — mensagens legíveis sem jargão técnico
Consequências : exceptions de negócio em exception/ mapeadas para HTTP no GlobalExceptionHandler;
                SLF4J loga o erro completo com conta_id e transfer_id via MDC
NÃO FAZER : NÃO retornar Exception ou StackTrace diretamente no ResponseBody —
            NÃO logar dados sensíveis (CPF, saldo) nos logs de erro
```

---

## DA-08: Extrato limitado a 90 dias ou 50 lançamentos

```
Data      : 2026-06
Status    : ativo
Contexto  : performance e usabilidade do endpoint de extrato
Decisão   : máximo 90 dias de histórico ou 50 lançamentos por consulta
Motivo    : contas antigas com alto volume gerariam queries lentas sem paginação;
            90 dias cobre 99% dos casos de uso reais de conferência;
            50 lançamentos é o limite de renderização confortável na UI
Consequências : ExtratoService trunca silenciosamente sem erro quando limite excedido;
                constante MAX_LANCAMENTOS_EXTRATO = 50 em ExtratoService
NÃO FAZER : NÃO retornar todos os lançamentos sem limite mesmo que o cliente peça —
            NÃO implementar paginação sem DA explícita definindo a estratégia
```

---

## DA-09: Lombok para redução de boilerplate

```
Data      : 2026-06
Status    : ativo
Contexto  : redução de código repetitivo em entidades e services Java
Decisão   : usar Lombok 1.18.x com anotações seletivas por tipo de classe
Motivo    : elimina getters/setters/construtores manuais; reduz ruído no código;
            @Builder melhora legibilidade na criação de objetos complexos
Consequências :
  Entidades JPA  → @Getter + @NoArgsConstructor + @Builder (NÃO @Data — evita equals/hashCode problemático com JPA)
  Services       → @RequiredArgsConstructor (injeção via construtor)
  DTOs request   → Records Java (sem Lombok — records já são imutáveis)
  DTOs response  → @Builder + @Getter
NÃO FAZER : NÃO usar @Data em entidades JPA — gera equals/hashCode baseado em todos os campos,
            causando problemas com lazy loading e coleções Hibernate —
            NÃO usar @Setter em LancamentoEntity (imutável por DA-03)
```

---

## DA-10: Valores monetários sempre em NUMERIC(15,2) no banco

```
Data      : 2026-06
Status    : ativo
Contexto  : armazenamento de valores financeiros
Decisão   : usar NUMERIC(15,2) no PostgreSQL — nunca FLOAT ou DOUBLE
Motivo    : FLOAT tem imprecisão binária que gera erros em cálculos financeiros
            (ex: 0.1 + 0.2 != 0.3 em ponto flutuante); NUMERIC é exato
Consequências : campos monetários mapeados como BigDecimal em entidades Java;
                usar BigDecimal para todas as operações aritméticas de saldo
NÃO FAZER : NÃO usar double ou float para valores monetários em nenhuma camada —
            NÃO converter para double mesmo que temporariamente em cálculos
```

---

## DA-11: JUnit 5 + Mockito para testes

```
Data      : 2026-06
Status    : ativo
Contexto  : escolha de framework de testes para o backend Java
Decisão   : JUnit 5 (Jupiter) com Mockito 5.x — NÃO usar JUnit 4
Motivo    : JUnit 5 tem suporte nativo a injeção de dependências em testes,
            extensões (@ExtendWith), testes parametrizados e nested tests;
            Mockito 5 compatível com Java 21 e Spring Boot 3
Consequências :
  Unitários  → @ExtendWith(MockitoExtension.class) + @Mock + @InjectMocks
  Integração → @SpringBootTest + @AutoConfigureMockMvc + banco real (H2 ou Testcontainers)
  Repository → @DataJpaTest com banco em memória
NÃO FAZER : NÃO usar @RunWith (JUnit 4) — usar @ExtendWith (JUnit 5) —
            NÃO mockar repositories em testes de integração — usar banco real
```

---

## DA-12: Swagger / OpenAPI para documentação da API

```
Data      : 2026-06
Status    : ativo
Contexto  : documentação dos endpoints REST
Decisão   : springdoc-openapi-starter-webmvc-ui gerando Swagger UI em /swagger-ui.html
Motivo    : documentação gerada automaticamente a partir das anotações do código;
            Swagger UI permite testar endpoints diretamente no browser;
            OpenAPI 3 é o padrão da indústria para contratos de API REST
Consequências : todo controller anotado com @Operation(summary=) e @ApiResponse;
                DTOs anotados com @Schema quando necessário para clareza
NÃO FAZER : NÃO desabilitar Swagger em produção sem substituição —
            NÃO deixar endpoints sem @Operation — dificulta uso por outros times
```

---

## DA-13: conta_id extraído do token, nunca do body

```
Data      : 2026-06-25
Status    : ativo
Contexto  : endpoints financeiros recebem conta_id — risco de IDOR (A01)
Decisão   : conta_id sempre extraído do JWT no SecurityContext
            body e path param usados apenas para identificar conta_destino
Motivo    : body pode ser forjado pelo cliente; token é assinado pelo servidor
Consequências : ContaService recebe contaId do token via @AuthenticationPrincipal
               nunca do @RequestBody
NÃO FAZER : NÃO aceitar conta_id no body para identificar conta do correntista —
            NÃO confiar em qualquer ID vindo do cliente para definir autoria
```

---

## DA-14: JWT_SECRET via variável de ambiente sem default

```
Data      : 2026-06-25
Status    : ativo
Contexto  : chave de assinatura hardcoded vaza via repositório (A02)
Decisão   : application.yml referencia ${JWT_SECRET} sem valor default
            startup falha explicitamente se variável não estiver definida
Motivo    : chave comprometida permite forjar tokens para qualquer usuário do sistema
Consequências : CI/CD injeta JWT_SECRET no deploy; dev local usa .env (no .gitignore)
NÃO FAZER : NÃO fornecer valor default para JWT_SECRET —
            NÃO commitar JWT_SECRET em nenhum arquivo versionado
```

---

## DA-15: Rate limiting no /auth/login por CPF

```
Data      : 2026-06-25
Status    : ativo
Contexto  : sem limite, CPF semi-público permite força bruta de senha (A07)
Decisão   : máximo 5 tentativas por CPF por minuto via Bucket4j
Motivo    : CPF é identificador semi-público no Brasil — mais previsível que IP
Consequências : dependência Bucket4j adicionada; LoginRateLimiter como @Component
               retorna HTTP 429 TOO_MANY_REQUESTS após limite atingido
NÃO FAZER : NÃO implementar rate limiting apenas por IP —
            NÃO usar contador em memória sem TTL (vaza entre restarts)
```

---

## DA-16: Swagger desabilitado em produção

```
Data      : 2026-06-25
Status    : ativo
Contexto  : Swagger expõe estrutura completa da API facilitando reconhecimento (A05)
Decisão   : springdoc desabilitado no profile prod via application-prod.yml
Motivo    : documentação é útil em dev/staging; risco desnecessário em produção
Consequências : application-prod.yml com springdoc.api-docs.enabled=false
               e springdoc.swagger-ui.enabled=false
NÃO FAZER : NÃO deixar Swagger ativo em produção —
            NÃO usar o mesmo application.yml para todos os profiles
```

---

## DA-17: BancoxException como base da hierarquia de exceptions

```
Data      : 2026-06-25
Status    : ativo
Contexto  : exceptions espalhadas sem hierarquia dificultam o GlobalExceptionHandler
            e violam LSP — subclasses com contratos incompatíveis com a superclasse
Decisão   : toda exception de negócio estende BancoxException extends RuntimeException
Motivo    : GlobalExceptionHandler captura BancoxException como fallback de negócio;
            toda subclasse é substituível sem quebrar o handler;
            facilita testes — verificar lançamento da superclasse quando suficiente
Consequências : criar BancoxException antes de qualquer UC;
                todo @ExceptionHandler novo adicionado sem modificar handlers existentes
NÃO FAZER : NÃO lançar RuntimeException diretamente —
            NÃO criar exceptions que não estendem BancoxException —
            NÃO modificar o contrato de BancoxException após criada
```

---

## DA-18: Virtual Threads habilitadas (Java 21 + Spring Boot 3.2+)

```
Data      : 2026-06-25
Status    : ativo
Contexto  : operações JPA são bloqueantes por natureza; thread pool limitado reduz
            throughput em picos de transferências e consultas de extrato
Decisão   : spring.threads.virtual.enabled=true no application.yml
Motivo    : Virtual Threads (Project Loom) aumentam throughput em I/O bloqueante
            sem mudar nenhuma linha de código de negócio; Java 21 LTS suporta nativamente
Consequências : ThreadLocal ainda funciona mas MDC deve ser propagado explicitamente
                em operações assíncronas; desabilitado nos testes (comportamento mais previsível)
NÃO FAZER : NÃO desabilitar em produção para "simplificar" —
            NÃO assumir que ThreadLocal se propaga automaticamente em contextos assíncronos
```

---

## DA-19: Graceful Shutdown obrigatório

```
Data      : 2026-06-25
Status    : ativo
Contexto  : SIGTERM em deploy ou scale-down pode interromper transferências em andamento
            — exatamente o cenário que DA-04 (atomicidade) protege em código,
            mas que a infraestrutura pode quebrar na hora do deploy
Decisão   : server.shutdown=graceful + lifecycle.timeout-per-shutdown-phase=30s
Motivo    : ao receber SIGTERM, Spring para de aceitar novas requisições e aguarda
            as em andamento terminarem antes de encerrar o processo;
            /actuator/health reporta OUT_OF_SERVICE durante o dreno —
            load balancers param de rotear tráfego automaticamente
Consequências : deploy pode levar até 30s a mais para completar;
                timeout de 30s deve ser maior que o tempo máximo de qualquer operação
NÃO FAZER : NÃO reduzir timeout abaixo de 30s sem medir tempo real das operações —
            NÃO usar server.shutdown=immediate em produção
```

---

## DA-20: Actuator — exposição mínima pública, demais endpoints protegidos

```
Data      : 2026-06-25
Status    : ativo
Contexto  : Actuator sem proteção expõe /env (variáveis de ambiente incluindo JWT_SECRET)
            e /shutdown (derruba o serviço remotamente) — OWASP A05
Decisão   : health e info públicos; demais endpoints requerem perfil ADMIN
Motivo    : health é necessário para load balancer e liveness probe do Kubernetes;
            info é útil para pipelines de CI/CD verificarem versão deployada;
            metrics e prometheus são necessários para observabilidade mas não públicos
Consequências : SecurityConfig protege /actuator/** com hasRole("ADMIN");
                health e info explicitamente liberados antes da regra geral;
                em produção: metrics, prometheus, flyway e threaddump também expostos mas protegidos
NÃO FAZER : NÃO adicionar endpoint ao include sem avaliar o que ele expõe —
            NÃO expor /actuator/env ou /actuator/shutdown em nenhum ambiente —
            NÃO remover a proteção ADMIN dos endpoints adicionais
```

---

## DA-21: Metas de cobertura de testes — 90% em service/, 80% global

```
Data      : 2026-06-25
Status    : ativo
Contexto  : cobertura de 87% de classes e 92% de linhas sem exclusões corretas
            inclui classes sem lógica testável (config, entity, dto, main)
            forçar 100% nessas classes gera testes que existem para satisfazer
            métricas, não para proteger comportamento
Decisão   : meta de 90% de linhas em service/ e 80% global via JaCoCo
            com exclusões explícitas de classes sem lógica de negócio
Motivo    : service/ é onde toda lógica de negócio vive — 90% é exigente e real
            global 80% reflete cobertura real excluindo ruído de configuração
            exclusões documentadas aqui evitam que sejam removidas por engano
Consequências : mvn verify falha se metas não forem atingidas
                relatório HTML em target/site/jacoco/index.html
                build de CI falha antes do deploy se cobertura regredir

Classes excluídas e porquê:
  BancoxApplication     → entry point com main() — sem lógica testável
  config/*              → beans de configuração Spring — sem lógica de negócio
  entity/*              → entidades JPA — getters/setters gerados pelo Lombok
  dto/*                 → records imutáveis — sem lógica
  BancoxException       → classe abstrata base — sem lógica própria

NÃO FAZER : NÃO criar testes artificiais para atingir métricas
            NÃO remover exclusões achando que são desleixo — são decisão deliberada
            NÃO aumentar meta para 100% sem revisar as exclusões
            NÃO desabilitar JaCoCo no CI para "fazer o build passar mais rápido"
```

---

## DA-22: JWT em cookie httpOnly — stateless com Thymeleaf (substituída por DA-26)

```
Data      : 2026-06-25
Status    : substituída por DA-26
Ver       : DA-26 para a decisão atual de autenticação
```


---

## DA-25: Escalabilidade horizontal — resolvida via JWT cookie (DA-26)

```
Data      : 2026-06-25
Status    : resolvida — JWT stateless elimina o problema de sessão em memória
Ver       : DA-26 para detalhes da solução de autenticação

O que ainda precisa de atenção ao escalar:
  1. Rate limiting distribuído (Bucket4j):
     Substituir Bucket4j in-memory por Bucket4j com backend Redis
     Sem Redis: cada instância tem contador próprio (5 tentativas × N instâncias)
     Com Redis: contador único compartilhado entre todas as instâncias

  3. Flyway (já seguro):
     Flyway com PostgreSQL usa lock distribuído nativo
     Apenas uma instância executa migrations mesmo com N instâncias subindo juntas
     Nenhuma mudança necessária

  4. Logs distribuídos (operacional):
     Sem agregação: logs de cada instância ficam isolados — diagnóstico difícil
     Adicionar ELK Stack, Grafana Loki ou serviço de log centralizado
     Nenhuma mudança no código — apenas infraestrutura

  5. Actuator + Load Balancer:
     Load balancer deve checar /actuator/health de cada instância individualmente
     Graceful shutdown (DA-19) garante que instância em scale-down drena requisições
     Nenhuma mudança no código — apenas configuração do load balancer

NÃO FAZER : NÃO subir segunda instância sem implementar Spring Session + Redis
            NÃO usar sticky sessions no load balancer como solução permanente
            (sticky sessions criam ponto único de falha e distribuição desigual de carga)
```

---

## DA-26: JWT em cookie httpOnly com Thymeleaf — stateless sem sessão server-side

```
Data      : 2026-06-25
Status    : ativo — substitui DA-22
Contexto  : Thymeleaf + HTMX envia requisições via forms HTML e hx-post
            browser não injeta header Authorization automaticamente sem JavaScript
            sessão server-side (anterior) não escalava horizontalmente (DA-25)
Decisão   : JWT gerado no login e armazenado em cookie httpOnly
            cookie enviado automaticamente pelo browser em toda requisição
            JwtCookieFilter extrai e valida JWT do cookie — sem sessão server-side
Motivo    :
  - Cookie httpOnly inacessível via JavaScript → elimina XSS como vetor de roubo
  - SameSite=Strict protege contra CSRF cross-origin → CSRF desabilitado no Spring Security
  - JWT stateless → escala horizontalmente sem Redis de sessão
  - Browser envia cookie automaticamente → sem JavaScript de gerenciamento de token
  - Time Java implementa apenas JwtCookieFilter + JwtService — curva baixa
Consequências :
  - CSRF desabilitado no SecurityConfig (SameSite=Strict substitui)
  - SessionCreationPolicy.STATELESS — sem HttpSession
  - Cookie: bancox_token; HttpOnly; Secure (prod); SameSite=Strict; Max-Age=900
  - Logout apaga o cookie (Max-Age=0) — sem blacklist necessária para MVP
  - Renovação: AuthController renova cookie silenciosamente quando JWT expira em < 2 min
  - JWT_SECRET deve ser idêntico em todas as instâncias (variável de ambiente — DA-14)
NÃO FAZER : NÃO ler token do header Authorization — apenas do cookie
            NÃO habilitar CSRF — SameSite=Strict já protege
            NÃO armazenar JWT em localStorage ou sessionStorage
            NÃO criar HttpSession — SessionCreationPolicy deve ser STATELESS
            NÃO usar cookie sem Secure em produção (DA-05)
```

---

## DA-27: SAST — Análise Estática de Segurança integrada ao build

```
Data      : 2026-06-25
Status    : ativo
Contexto  : OWASP Top 10 documentado em autenticacao-autorizacao.md precisa de verificação automática;
            segredos hardcoded e dependências vulneráveis são riscos que revisão humana
            pode não detectar consistentemente
Decisão   : três ferramentas integradas ao mvn verify (DA-27):
            1. SpotBugs + Find Security Bugs → OWASP Top 10, padrões inseguros Java
            2. OWASP Dependency-Check        → CVEs em dependências (CVSS >= 7 falha o build)
            3. Gitleaks                      → segredos hardcoded no código e histórico git

Ferramentas escolhidas e motivo:
  SpotBugs + Find Security Bugs:
    - Gratuito e open source
    - Find Security Bugs tem regras específicas para Spring, JPA, Thymeleaf
    - Detecta SQL injection via concatenação, XSS via th:utext sem sanitização,
      autenticação insegura, criptografia fraca
    - Integração Maven nativa — roda em mvn verify

  OWASP Dependency-Check:
    - Padrão da indústria para SCA (Software Composition Analysis)
    - Consulta NVD (National Vulnerability Database) automaticamente
    - Threshold: CVSS >= 7.0 (ALTO ou CRÍTICO) falha o build
    - CVSS 4.0-6.9 (MÉDIO): reportado mas não falha

  Gitleaks:
    - Detecta segredos hardcoded: JWT_SECRET, senhas, tokens de API
    - Varre histórico git — detecta segredos que foram commitados e removidos
    - Regras customizadas para padrões específicos do BancoX (.gitleaks.toml)

Threshold de falha no build:
  SpotBugs: qualquer bug de prioridade MEDIUM ou HIGH → falha
  Dependency-Check: CVSS >= 7.0 → falha; < 7.0 → apenas reporta
  Gitleaks: qualquer segredo detectado → falha (exit code 1)

Arquivos de configuração:
  spotbugs-exclude.xml               → exclusões de falsos positivos SpotBugs
  dependency-check-suppressions.xml  → supressões de CVEs não aplicáveis
  .gitleaks.toml                     → regras customizadas e allowlist

Consequências:
  mvn verify executa na ordem: compile → test → JaCoCo → SpotBugs → Dependency-Check
  Gitleaks roda separado no CI (não integrado ao Maven — CLI direto)
  Relatórios gerados em: target/spotbugs.html, target/dependency-check-report.html

NÃO FAZER : NÃO excluir regra SpotBugs sem revisão manual e comentário no XML
            NÃO suprimir CVE sem ler o advisory completo e definir data de expiração
            NÃO desabilitar Gitleaks no CI para "fazer o build passar mais rápido"
            NÃO aumentar CVSS threshold acima de 7.0 sem aprovação explícita
            NÃO commitar exclusões em massa — revisar uma a uma
```



---

## DA-30: Testcontainers para teste de startup com PostgreSQL real

```
Data      : 2026-06-25
Status    : ativo
Contexto  : erros de startup (schema-validation, migrations inválidas, beans mal
            configurados) só apareciam ao subir a aplicação localmente —
            testes com H2 e Flyway desabilitado não os detectavam
Decisão   : ApplicationStartupTest com @Testcontainers + PostgreSQLContainer
            sobe contexto Spring completo contra PostgreSQL real com Flyway ativo
            e ddl-auto: validate
Motivo    : H2 não replica comportamento do PostgreSQL para tipos customizados
            e constraints CHECK; Flyway desabilitado nos testes H2 não valida
            as migrations reais; um único teste cobre toda a camada de configuração
Consequências : testcontainers-bom 1.19.8 + junit-jupiter + postgresql no pom.xml;
                ApplicationStartupTest.java em src/test/java/com/bancox/;
                Docker deve estar disponível no CI; ~15-20s de overhead no mvn verify

Erros detectados que H2 não detecta:
  - schema-validation: CHAR vs VARCHAR, ENUM vs tipo customizado
  - Migration SQL com sintaxe inválida para PostgreSQL
  - Bean mal configurado no startup
  - JWT_SECRET ausente

NÃO FAZER : NÃO substituir H2 por Testcontainers em todos os testes —
            NÃO desabilitar ApplicationStartupTest para acelerar o CI
```
