# CLAUDE.md — BancoX
> Arquivo âncora. Carregado automaticamente pelo Claude Code a cada sessão.
> Mantenha este arquivo leve — detalhes vivem nos arquivos referenciados.

---

## 1. IDENTIDADE

Você é o agente de desenvolvimento do sistema bancário BancoX.
Seu objetivo é implementar, testar e manter operações financeiras com segurança e precisão.

NÃO implemente lógica de negócio fora das camadas definidas em arquitetura.md.
NÃO permita saldo negativo em nenhuma operação.
NÃO execute operações parciais — toda transação é atômica ou não ocorre.
NÃO exponha stack trace — log interno, mensagem amigável para o cliente.
NÃO armazene senha em texto puro — sempre BCrypt strength 12 (DS-02).
Antes de propor mudanças estruturais, consulte decisoes-arquiteturais.md.

Linguagem: Java 21 | Build: Maven | Idioma de commits e comentários: PT-BR

---

## 2. GLOSSÁRIO

| Termo         | Significado                                                          |
|---------------|----------------------------------------------------------------------|
| credito       | Entrada de valor na conta (saldo sobe)                               |
| debito        | Saída de valor da conta (saldo cai)                                  |
| transferencia | Movimentação atômica entre duas contas do mesmo banco                |
| extrato       | Histórico cronológico de lançamentos de uma conta                    |
| lancamento    | Registro único imutável: tipo, valor, timestamp, saldo_apos          |
| saldo_apos    | Saldo da conta imediatamente após o lançamento ser registrado        |
| transfer_id   | UUID gerado por transferência, presente nos lançamentos de ambas as contas |
| conta_id      | UUID v4 que identifica unicamente uma conta corrente                 |
| access_token  | JWT de curta duração (15 min) para autenticar requisições            |
| refresh_token | Token opaco de longa duração (7 dias) para renovar access_token      |
| ownership     | Regra: CORRENTISTA só acessa sua própria conta                       |

**Regras de negócio:**
- Cada cliente possui exatamente uma conta corrente (`conta_id` UUID v4)
- Saldo nunca pode ser negativo (sem cheque especial)
- Toda operação gera um `lancamento` imutável com `timestamp` UTC
- Extrato retorna máximo 90 dias ou 50 lançamentos (o que vier primeiro)
- Padrão de datas: ISO-8601 UTC em todas as entradas e saídas

---

## 3. REQUISITOS NÃO FUNCIONAIS

| Categoria       | Requisito                                        | Referência   |
|-----------------|--------------------------------------------------|--------------|
| Performance     | Extrato: máx 90 dias ou 50 lançamentos           | DA-08        |
| Performance     | Virtual Threads habilitadas (Java 21 Loom)       | DA-18        |
| Disponibilidade | Graceful Shutdown — drena requisições em 30s     | DA-19        |
| Disponibilidade | Escalabilidade: instância única no MVP           | DA-25        |
| Segurança       | Autenticação JWT em cookie httpOnly              | DA-26        |
| Segurança       | Rate limiting: 5 tentativas de login por CPF/min | DA-15        |
| Segurança       | OWASP Top 10 — regras em autenticacao-autorizacao.md | DA-27   |
| Qualidade       | Cobertura de testes: 90% em service/, 80% global | DA-21        |
| Qualidade       | SAST integrado ao build (SpotBugs + Dep-Check)   | DA-27        |
| Observabilidade | Actuator + Prometheus + logs JSON estruturados   | DA-20, DA-07 |
| Manutenção      | Migrations Flyway imutáveis após aplicação       | DA-02        |
| Manutenção      | Valores monetários sempre BigDecimal             | DA-10        |

---

## 4. CONTEXTO TÉCNICO

@.claude/context/arquitetura.md
@.claude/context/autenticacao-autorizacao.md
@.claude/context/contratos-api.md
@.claude/context/design-system.md
@.claude/context/decisoes-arquiteturais.md

---

## 5. MODELO ENTIDADE-RELACIONAL

@.claude/context/modelo-er/README.md

---

## 6. RELAÇÕES ENTRE CASOS DE USO

```
UC-00 Autenticação        ← pré-requisito de todos os UCs
UC-01 Crédito
UC-02 Débito
UC-03 Transferência
  <<include>> UC-02  (sempre debita conta_origem — não duplicar lógica)
  <<include>> UC-01  (sempre credita conta_destino — não duplicar lógica)
UC-04 Extrato
  <<extend>>  UC-03  (transfer_id preenchido quando lançamento for transferência)
```

- `<<include>>` = obrigatório — UC base não funciona sem o incluído
- `<<extend>>`  = condicional — UC base funciona sozinho, ativado por condição
- Todos os UCs (exceto UC-00) requerem token válido e ownership verificado

---

## 7. CASOS DE USO DE NEGÓCIO
> Cada UC contém: user story, critérios, fluxo, exceções, exemplos e seção de segurança.

@.claude/casos-de-uso/UC-00-autenticacao.md
@.claude/casos-de-uso/UC-01-credito.md
@.claude/casos-de-uso/UC-02-debito.md
@.claude/casos-de-uso/UC-03-transferencia.md
@.claude/casos-de-uso/UC-04-extrato.md

---

## 8. CASOS DE USO DE INTERFACE
> Cada UC-UI contém: user story, critérios, componentes, fluxo de tela e estados.
> Ver design-system.md para tokens, componentes base e mapa de telas.

@.claude/casos-de-uso/UC-UI-04-login.md
@.claude/casos-de-uso/UC-UI-01-dashboard.md
@.claude/casos-de-uso/UC-UI-02-extrato.md
@.claude/casos-de-uso/UC-UI-03-transferencia.md
@.claude/casos-de-uso/UC-UI-05-gestao-contas.md

---

## 9. COMANDOS DO PROJETO

```bash
# Backend
mvn spring-boot:run          # servidor de desenvolvimento
mvn test                     # testes unitários
mvn verify                   # testes unitários + integração
mvn compile                  # compilar sem rodar testes
mvn clean package            # build do jar

# Swagger UI (com servidor rodando)
http://localhost:8080/swagger-ui.html

# Flyway
mvn flyway:info              # status das migrations
mvn flyway:migrate           # aplicar migrations pendentes

# Infra
docker compose up            # sobe PostgreSQL + backend + frontend
```

---

## 10. FALLBACKS GLOBAIS

```
NUNCA assuma valores ausentes.
NUNCA execute operação parcialmente.
NUNCA retorne saldo negativo.
NUNCA exponha stack trace — usar GlobalExceptionHandler.
NUNCA exiba código técnico de erro na UI — use a mensagem legível da API.
NUNCA armazene senha em texto puro — sempre BCrypt (DS-02).
NUNCA reverta uma decisão arquitetural sem consultar decisoes-arquiteturais.md.
NUNCA considere uma tarefa concluída sem verificar o DoD em src/tests/CLAUDE.md.
NUNCA use double ou float para valores monetários — sempre BigDecimal.
NUNCA edite uma migration Flyway já aplicada — criar nova migration de correção.
NUNCA revele se foi CPF ou senha que errou no login — mensagem genérica.
NUNCA construa queries SQL/JPQL por concatenação de strings — sempre parâmetros nomeados.
NUNCA logue CPF, saldo, valor de transação ou tokens de autenticação.
NUNCA aceite conta_id no body de operações financeiras — extrair do token JWT (DA-13).
NUNCA use JWT_SECRET hardcoded no código — sempre via variável de ambiente (DA-14).
NUNCA ler JWT do header Authorization em endpoints Thymeleaf — apenas do cookie (DA-26).
NUNCA habilitar CSRF — SameSite=Strict no cookie JWT já protege (DA-26).
NUNCA criar HttpSession — autenticação é stateless via JWT cookie (DA-26).
NUNCA instalar Bootstrap ou HTMX via npm — sempre via CDN (DA-24).
NUNCA exibir código técnico de erro na UI — usar mensagem legível da API.
NUNCA expor /actuator/env ou /actuator/shutdown em nenhum ambiente (DA-20).
NUNCA reduzir timeout-per-shutdown-phase abaixo de 30s sem medir tempo das operações (DA-19).
SEMPRE registre alterações em .claude/ no arquivo .claude/CHANGELOG-DOC.md.
```

---

## 11. PRIORIDADE DE VALIDAÇÃO COMPLETA

Aplicável a todos os UCs (exceto UC-00):

```
1. Token presente e válido              → senão: 401 TOKEN_INVALIDO/TOKEN_EXPIRADO
2. Perfil autorizado para o endpoint   → senão: 403 ACESSO_NEGADO
3. Ownership da conta (CORRENTISTA)    → senão: 403 ACESSO_NEGADO
4. Conta existe                        → senão: 404 CONTA_NAO_ENCONTRADA
5. Conta ATIVA                         → senão: 422 CONTA_BLOQUEADA
6. Valor válido                        → senão: 422 VALOR_INVALIDO
7. Saldo suficiente                    → senão: 422 SALDO_INSUFICIENTE
8. Execução
```

Etapas 1-3: transparentes via Spring Security + JwtAuthFilter
Etapas 4-8: nos services, conforme cada UC

---

## 12. RASTREABILIDADE DE DOCUMENTAÇÃO

Toda mudança em `.claude/` deve ser commitada com mensagem descritiva.
O `git log` é o changelog — não há arquivo separado para manter.

Convenção de commit para mudanças de documentação:
```
docs: atualiza UC-02 com suporte a débito agendado (DA-13)
docs: adiciona UC-06 para pagamento de boleto
docs: corrige tipo cpf VARCHAR(11) em entidade-cliente (bug)
docs: adiciona DA-30 sobre estratégia de cache
```

---

## 13. COMO CRESCER ESTE PROJETO

| Quando                          | O que fazer                                               |
|---------------------------------|-----------------------------------------------------------|
| Nova feature                    | Criar UC-XX.md (com US embutida) e referenciar aqui      |
| Nova relação entre UCs          | Atualizar seção 5 deste arquivo                          |
| Relação complexa com fluxo      | Criar .claude/relations/ com arquivo dedicado            |
| Nova entidade no modelo ER      | Criar .claude/context/modelo-er/entidade-NOME.md + migration Flyway |
| Novo componente de UI           | Documentar em .claude/context/design-system.md           |
| Design system > 10 componentes  | Migrar para .claude/context/design-system/ com README.md |
| Nova decisão técnica            | Adicionar DA-XX em decisoes-arquiteturais.md             |
| Nova decisão de segurança       | Adicionar DS-XX em autenticacao-autorizacao.md                          |
| Decisão anterior substituída    | Marcar como "substituído" — nunca deletar                |
| Exemplos transversais complexos | Criar .claude/examples/ e referenciar aqui               |
| Nova camada técnica             | Adicionar subseção em arquitetura.md → Regras por Camada |
| Nova tela de UI                 | Criar UC-UI-XX.md e referenciar na seção 7               |
| Novo componente de UI           | Documentar em .claude/context/design-system.md           |
