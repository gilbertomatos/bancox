# arquitetura.md — Estrutura e Padrões Técnicos BancoX

## Stack Tecnológico

### Backend
| Categoria         | Tecnologia        | Versão   | Observação                                      |
|-------------------|-------------------|----------|-------------------------------------------------|
| Linguagem         | Java              | 21 LTS   | records, sealed classes, pattern matching       |
| Build             | Maven             | 3.9.x    | pom.xml na raiz; NÃO usar Gradle                |
| Framework web     | Spring Boot       | 3.3.x    | Spring MVC para controllers REST                |
| Redução boilerplate | Lombok          | 1.18.x   | @Data, @Builder, @RequiredArgsConstructor       |
| Banco de dados    | PostgreSQL        | 16.x     |                                                 |
| Migrações         | Flyway            | 10.x     | scripts em src/main/resources/db/migration      |
| Documentação API  | Swagger / OpenAPI | 3.x      | springdoc-openapi-starter-webmvc-ui             |
| Testes unitários  | JUnit 5 + Mockito | 5.x / 5.x| NÃO usar JUnit 4                               |
| Testes integração | Spring Boot Test  | —        | @SpringBootTest + @Transactional                |
| Logs              | SLF4J + Logback   | —        | JSON estruturado em produção via logstash-logback|
| Contêiner         | Docker + Compose  | —        | docker-compose.yml na raiz                      |

### Frontend (server-side rendering — DA-24)
| Categoria       | Tecnologia                    | Versão | Observação                              |
|-----------------|-------------------------------|--------|-----------------------------------------|
| Template engine | Thymeleaf                     | 3.x    | integrado ao Spring Boot                |
| Sec. integration| thymeleaf-extras-springsecurity6 | —   | sec:authorize, sec:authentication       |
| Estilo          | Bootstrap                     | 5.x    | via CDN — NÃO instalar via npm          |
| Ícones          | Bootstrap Icons               | 1.x    | via CDN                                 |
| Interatividade  | HTMX                          | 2.x    | via CDN — atualizações parciais sem SPA |
| Gráficos        | Chart.js                      | 4.x    | via CDN — usado no extrato              |

> Templates em: src/main/resources/templates/
> Estáticos em: src/main/resources/static/
> Tokens e padrões visuais: ver @.claude/context/design-system.md

---

## Convenções de Código (Java)

- Pacotes: `com.bancox.<camada>` (ex: `com.bancox.service`, `com.bancox.controller`)
- Classes: PascalCase (`ContaService`, `LancamentoRepository`)
- Métodos e variáveis: camelCase (`buscarPorId`, `saldoAtual`)
- Constantes: SNAKE_UPPER (`MAX_LANCAMENTOS_EXTRATO = 50`)
- Usar `@Builder` do Lombok em entidades e DTOs — nunca construtores manuais com muitos parâmetros
- Records Java para DTOs imutáveis de request/response (não usar Lombok em records)
- Exceptions customizadas em `exception/` — nunca lançar `RuntimeException` diretamente

---

## Estrutura de Pacotes

```
src/main/java/com/bancox/
  controller/       # Controllers REST — finos, sem lógica de negócio
  service/          # Lógica de negócio (ContaService, TransferenciaService, ExtratoService)
  repository/       # Interfaces JPA (ContaRepository, LancamentoRepository)
  entity/           # Entidades JPA (Conta, Lancamento, Transferencia)
  dto/              # Records de request e response
  exception/        # Exceptions customizadas + GlobalExceptionHandler
  config/           # Beans de configuração Spring (Swagger, etc.)

src/main/resources/
  application.yml           # configuração principal
  db/migration/             # scripts Flyway (V1__init.sql, V2__add_index.sql)

src/test/java/com/bancox/
  service/          # Testes unitários com Mockito
  controller/       # Testes de integração com @SpringBootTest
  repository/       # Testes de repositório com @DataJpaTest

src/main/resources/
  templates/
    fragments/      # Fragmentos reutilizáveis (header, sidebar, layout)
    auth/           # login.html
    correntista/    # dashboard.html, extrato.html, transferencia.html
    operador/       # gestao-contas.html
  static/
    css/            # CSS customizado (complementa Bootstrap)
    js/             # JS customizado quando necessário
    img/            # Imagens e ícones estáticos
```

---

## Regras de Camada

- `controller/` só chama `service/` — nunca acessa `repository/` diretamente
- `service/` contém toda a lógica de negócio e validações
- `repository/` só declara queries JPA — sem regras de negócio
- `entity/` são entidades JPA puras — sem lógica de negócio
- `exception/GlobalExceptionHandler` mapeia todas as exceptions para os envelopes de contratos-api.md
- Templates Thymeleaf consomem dados via Model do Spring MVC — nunca acessam o banco diretamente

---

## Padrões Obrigatórios

- Toda operação de escrita usa `@Transactional` em `service/`
- Rollback automático em qualquer `RuntimeException` dentro de `@Transactional`
- Logs estruturados com MDC contendo `conta_id` e `transfer_id` quando aplicável
- Testes unitários obrigatórios para todo método público em `service/`
- Testes de integração obrigatórios para cada caso de uso (UC-XX)
- Swagger documenta todos os endpoints — usar `@Operation` e `@ApiResponse` nos controllers
- Migrações Flyway nomeadas: `V{versão}__{descricao_snake}.sql` — nunca alterar migration já aplicada

---

## Princípios SOLID — Aplicação Concreta no BancoX

SOLID não é documentado como lista de princípios abstratos.
Cada princípio está traduzido em regras concretas do projeto abaixo.

### SRP — Single Responsibility (Responsabilidade Única)
Cada classe tem exatamente uma razão para mudar.

```
controller/  → única responsabilidade: orquestrar request/response
              NÃO colocar validação de negócio, cálculo ou acesso a banco

service/     → única responsabilidade: lógica de negócio do UC correspondente
              ContaService cuida de conta
              TransferenciaService cuida de transferência
              ExtratoService cuida de extrato
              NÃO criar um "BancoxService" genérico que faz tudo

repository/  → única responsabilidade: queries de acesso a dados
              NÃO colocar regras de negócio ou validações

exception/   → única responsabilidade: mapear erros para respostas HTTP
              GlobalExceptionHandler é o único ponto de mapeamento
              NÃO tratar exceções em controllers ou services além de lançá-las
```

### OCP — Open/Closed (Aberto para extensão, fechado para modificação)
Estender comportamento sem modificar código existente.

```
Nova operação financeira → criar novo UC-XX.md + novo Service + novo Controller
                           NÃO modificar ContaService para adicionar nova operação

Novo tipo de erro        → criar nova Exception + novo @ExceptionHandler
                           NÃO modificar handlers existentes no GlobalExceptionHandler

Nova regra de validação  → adicionar novo método de validação no Service
                           NÃO alterar o fluxo principal de métodos existentes

Novo perfil de acesso    → adicionar novo role no SecurityConfig
                           NÃO modificar a lógica de ownership existente
```

### LSP — Liskov Substitution (Substituição de Liskov)
Subtipos devem ser substituíveis pelos seus tipos base sem alterar comportamento.

```
Hierarquia de exceptions (obrigatória):
  RuntimeException
    └── BancoxException           ← base de todas as exceptions de negócio
          ├── ContaNaoEncontradaException
          ├── ValorInvalidoException
          ├── SaldoInsuficienteException
          ├── ContasIdenticasException
          └── (demais exceptions de UC)

Regra: GlobalExceptionHandler captura BancoxException como base
       Toda subclasse deve ser tratável pelo handler da superclasse
       NÃO criar exceptions que mudam o contrato de BancoxException
       NÃO lançar RuntimeException diretamente — sempre uma subclasse de BancoxException
```

### ISP — Interface Segregation (Segregação de Interfaces)
Interfaces específicas são melhores que uma interface geral.

```
Repositories: cada um declara apenas os métodos que seus consumers usam

  ContaRepository     → findById, save
  LancamentoRepository → save, findByContaIdAndCriadoEmBetween
  TransferenciaRepository → save
  UsuarioRepository   → findByCpf

  NÃO criar um RepositoryBase<T> com todos os métodos possíveis
  NÃO adicionar métodos em Repository que nenhum Service usa
  NÃO expor métodos de escrita em repositories usados apenas para leitura
```

### DIP — Dependency Inversion (Inversão de Dependência)
Depender de abstrações, não de implementações concretas.

```
Injeção de dependência: sempre via construtor com @RequiredArgsConstructor
  NÃO usar @Autowired em campo (acoplamento forte, dificulta testes)
  NÃO instanciar dependências com new dentro de services ou controllers
  NÃO acessar ApplicationContext diretamente para obter beans

Services dependem de interfaces (JpaRepository é uma interface):
  ContaService      → depende de ContaRepository (interface)
  ExtratoService    → depende de LancamentoRepository (interface)
  Mockito substitui a interface nos testes unitários sem mudar o service
```

---

## Regras por Camada (referência rápida)

### controller/
- Fino: recebe request → chama service → retorna response
- NÃO colocar lógica de negócio aqui
- NÃO capturar Exception — deixar para GlobalExceptionHandler
- NÃO aceitar conta_id no body — extrair do token JWT (DA-13)
- Anotar com @Operation e @ApiResponse (Swagger)
- Validar entrada com @Valid antes de chamar service

### service/
- Toda lógica de negócio vive aqui
- Validar TODAS as condições ANTES de qualquer escrita (DA-05)
- @Transactional em toda operação de escrita múltipla
- @RequiredArgsConstructor para injeção — NÃO @Autowired em campo
- BigDecimal para valores monetários — NÃO double ou float (DA-10)
- Enums: @Enumerated(EnumType.STRING) — NÃO EnumType.ORDINAL (DA-29)

### exception/
- GlobalExceptionHandler é o único ponto de mapeamento de exceptions para HTTP
- Toda exception de negócio estende BancoxException (DA-17)
- Nova exception: criar classe + adicionar @ExceptionHandler (NÃO modificar existentes)

### templates/ (Thymeleaf)
- Templates exibem dados do Model — SEM lógica de negócio
- NÃO exibir mensagens técnicas — usar ${erro} e ${sucesso} do Model
- CSRF desabilitado — SameSite=Strict no cookie JWT protege (DA-26)
- Formatos: #numbers.formatCurrency(valor) | #temporals.format(data, 'dd/MM/yyyy HH:mm')

---

## Armadilhas conhecidas (bugs recorrentes)

### CHAR vs VARCHAR em campos CPF
Hibernate mapeia String → VARCHAR. Usar VARCHAR(11), não CHAR(11).
Migração de correção: V3__fix_cpf_char_to_varchar.sql

### CREATE TYPE ENUM vs VARCHAR
Hibernate @Enumerated(EnumType.STRING) persiste como VARCHAR.
NÃO usar CREATE TYPE ... AS ENUM no PostgreSQL — usar VARCHAR + CHECK constraint.
Padrão: tipo VARCHAR(20) NOT NULL, CONSTRAINT ck_tipo CHECK (tipo IN ('a','b','c'))
Migração de correção: V4__fix_enum_types_to_varchar.sql

### Migração de correção que tenta recriar constraint já existente
Migrations de "fix" (V3, V4, V5...) tentam ADD CONSTRAINT de algo que V1/V2 já criou inline.
Resultado: "constraint already exists" → Flyway aborta e ApplicationStartupTest falha.
Solução: toda migration de correção deve usar `DROP CONSTRAINT IF EXISTS` antes do `ADD CONSTRAINT`.
Padrão idempotente:
```sql
ALTER TABLE t DROP CONSTRAINT IF EXISTS ck_nome;
ALTER TABLE t ADD CONSTRAINT ck_nome CHECK (...);
```

### favicon.ico bloqueado pelo Spring Security
Adicionar /favicon.ico, /error, /static/**, /css/**, /js/**, /img/** ao permitAll().

### SRI (integrity) em CDNs
NÃO usar atributo integrity — hash pode divergir com atualizações silenciosas do CDN.
Usar crossorigin="anonymous" sem integrity (DA-28).

### Hibernate 6.6.x — ordem de flush com FK declarada como @Column UUID
Hibernate 6.6.x (Spring Boot 3.5.x) mudou a ordem de flush de entidades sem relacionamento JPA explícito.
Se `LancamentoEntity.transferId` é `@Column UUID` (sem `@ManyToOne`), o Hibernate não detecta a dependência
com `TransferenciaEntity` e pode inserir os lançamentos antes da transferência, violando a FK.
Solução: usar `saveAndFlush()` para forçar o INSERT da entidade pai antes das entidades que a referenciam.
```java
// CORRETO — garante INSERT imediato antes dos lançamentos que referenciam transfer_id por FK
transferenciaRepository.saveAndFlush(transferencia);

// ERRADO — Hibernate 6.6.x pode reordenar o flush e inserir lançamentos antes
transferenciaRepository.save(transferencia);
```
Afeta também os testes unitários com Mockito: stub deve usar `saveAndFlush`, não `save`.
