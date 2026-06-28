# Processo.md — Guia de Processos com Claude Code

> Documento de referência para todos os fluxos de desenvolvimento usando o template BancoX.
> Cada fluxo inclui etapas, arquivos afetados e prompt completo para o agente.

---

## Índice

1. [Nova Feature (US + UC)](#1-nova-feature-us--uc)
1b. [Nova Feature com TDD (Test-First)](#1b-nova-feature-com-tdd-test-first)
2. [Alteração de Feature Existente](#2-alteração-de-feature-existente)
3. [Correção de Bug](#3-correção-de-bug)
4. [Hotfix Emergencial](#4-hotfix-emergencial)

---


---

## 0. DIAGNÓSTICO DE TEMPLATE — Executar antes de qualquer correção de código

> Este fluxo é obrigatório antes de corrigir qualquer bug ou comportamento inesperado.
> Objetivo: descobrir se o problema é no código ou na especificação do template.
> Um bug corrigido apenas no código sem corrigir o template vai se repetir.

### Checklist de diagnóstico

Antes de escrever qualquer linha de correção, responder estas perguntas:

**1. O template descrevia o comportamento correto?**
   - Ler o UC correspondente ao comportamento que falhou
   - Ler as entidades do modelo-er/ envolvidas
   - Ler as DAs relevantes em decisoes-arquiteturais.md
   - Ler a seção "Armadilhas conhecidas" em arquitetura.md

   → Se o template estava INCORRETO ou OMISSO: corrigir o template ANTES do código
   → Se o template estava correto: o bug é de implementação — corrigir o código

**2. O agente estava seguindo o template quando gerou o código errado?**
   → Se SIM: o template estava incompleto — adicionar a regra que estava faltando
   → Se NÃO: corrigir o código e verificar se o template precisa de uma regra mais explícita

**3. Este tipo de erro pode se repetir em outros UCs ou entidades?**
   → Buscar padrões similares no código gerado
   → Se sim: adicionar à seção "Armadilhas conhecidas" em arquitetura.md
   → Se for decisão arquitetural relevante: criar DA-XX

### Ordem de execução obrigatória

```
1. Diagnosticar causa raiz (checklist acima)
2. Atualizar o template SE necessário:
   - arquitetura.md (armadilhas, regras de camada)
   - UC-XX correspondente (se fluxo estava errado)
   - modelo-er/ (se entidade estava errada)
   - decisoes-arquiteturais.md (se decisão nova)
3. Corrigir o código
4. Criar migration de correção SE houve mudança de schema (nunca editar migration existente)
5. Adicionar teste de regressão cobrindo o cenário que falhou
6. Commitar template e código juntos com mensagem docs: + fix:
```

### Exemplos de diagnóstico aplicado

**Erro: CHAR vs VARCHAR no campo CPF**
- Template dizia CHAR(11) → template estava incorreto
- Ação: corrigir entidade-cliente.md + entidade-usuario.md → criar V3 migration → corrigir código
- Prevenção: adicionar em arquitetura.md "Armadilhas conhecidas"

**Erro: CREATE TYPE ENUM vs VARCHAR**
- Template usava tipo_lancamento ENUM → template estava incorreto
- Ação: corrigir entidade-lancamento.md + V1 e V2 → criar V4 migration → adicionar regra em arquitetura.md
- Prevenção: regra explícita em arquitetura.md + @Enumerated(EnumType.STRING) na seção de camadas

**Erro: /favicon.ico bloqueado**
- Template não mencionava recursos estáticos no permitAll() → omissão no template
- Ação: corrigir SecurityConfig.java → adicionar em arquitetura.md "Armadilhas conhecidas"

### Quando o template NÃO precisa ser atualizado

- Erro de digitação ou lógica simples que não tem relação com a spec
- Comportamento que o template proíbe explicitamente mas o agente ignorou
  (neste caso: reforçar a regra com NÃO FAZER mais explícito)

---

## 1. Nova Feature (US + UC)

### Quando usar
Sempre que uma nova funcionalidade for adicionada ao sistema — nova operação,
novo endpoint, novo fluxo de negócio.

### Etapas

```
1. Escrever UC-XX.md com User Story embutida
2. Atualizar modelo-er/ se houver entidade nova ou campo novo
3. Atualizar contratos-api.md com novos erros e envelopes
4. Atualizar CLAUDE.md raiz (referenciar UC-XX na seção 6)
5. Atualizar mapa de relações na seção 5 do CLAUDE.md se houver <<include>> ou <<extend>>
6. Registrar em git log (rastreabilidade via commits)
7. Executar prompt ao agente
8. Verificar DoD em src/tests/CLAUDE.md
```

### Arquivos sempre afetados
- `.claude/casos-de-uso/UC-XX-nome.md` ← criar
- `CLAUDE.md` ← referenciar UC-XX na seção 6
- `.claude/git log (rastreabilidade via commits)` ← registrar

### Arquivos condicionalmente afetados
- `.claude/context/modelo-er/entidade-NOME.md` ← se nova entidade
- `.claude/context/modelo-er/README.md` ← se nova entidade ou nova relação
- `.claude/context/contratos-api.md` ← se novo erro ou novo envelope
- `.claude/context/decisoes-arquiteturais.md` ← se nova decisão técnica relevante

### Template do UC-XX.md

```markdown
# UC-XX — Nome da Feature
> Inclui: UC-YY (se aplicável)
> Estende: UC-ZZ (se aplicável)

## User Story
Como [perfil de usuário]
Quero [ação]
Para [objetivo de negócio]

## Critérios de Aceitação
- [ ] critério 1
- [ ] critério 2
- [ ] casos de erro cobertos

## Atores
[Ator] → API ([METHOD]) → [Service] → [Repository]

## Fluxo Principal
```
1. [METHOD] /endpoint
   Body: { campos }

2. Service valida (nesta ordem):
   - condição 1
   - condição 2

3. [operação]

4. Repository persiste: { campos }

5. Retorna HTTP [código] com envelope de sucesso
```

## Exceção — [Nome]
```
→ HTTP [código] { "erro": "CODIGO", "mensagem": "Mensagem legível." }
```

## Exemplos

### Caso feliz
```
ENTRADA : [METHOD] /endpoint { corpo }
SAÍDA   : [código] { resposta }
POR QUÊ : lógica aplicada
```

### Caso de erro — [nome]
```
ENTRADA : [METHOD] /endpoint { corpo inválido }
SAÍDA   : [código] { "erro": "CODIGO", ... }
ERRO A EVITAR: [o que não fazer]
```

## Critério de Sucesso
[resultado mensurável]

## Impacto nos UCs Existentes (se houver)
[listar UCs afetados e o que muda]
```

### Prompt ao agente — Nova Feature

```
Implemente o UC-XX ([Nome]) conforme documentado.

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

---

## 1b. Nova Feature com TDD (Test-First)

### Quando usar
Variante da seção 1 para UCs com múltiplas validações, regras de estado ou
transações compostas — onde os casos de exceção têm peso igual ao fluxo principal.
Não usar para scaffolding, configuração ou CRUD simples.

### Diferença em relação ao fluxo padrão
No fluxo padrão (seção 1) os testes são escritos junto com o código no mesmo prompt.
No TDD os prompts são separados e obrigatoriamente em sequência:
  Prompt 1 → apenas testes (sem implementação)
  Prompt 2 → apenas código (sem modificar os testes)

Separar os prompts é o que garante que os testes foram escritos contra a spec,
não contra a implementação.

### Etapas

```
1. Escrever UC-XX.md (igual à seção 1)
2. Atualizar modelo-er/, contratos-api.md, CLAUDE.md (igual à seção 1)
3. Executar Prompt 1 → agente escreve apenas os testes
4. Revisar os testes manualmente antes de prosseguir
5. Executar Prompt 2 → agente implementa o código para os testes passarem
6. mvn test → todos os testes devem passar sem modificar nenhum teste
7. Verificar DoD em src/test/CLAUDE.md
```

### Prompt 1 ao agente — Escrever apenas os testes

```
Leia o UC-XX ([Nome]) em .claude/casos-de-uso/UC-XX-nome.md.

Escreva APENAS os testes — não implemente nenhum código de produção.

Consulte somente:
- .claude/casos-de-uso/UC-XX-nome.md
- .claude/context/contratos-api.md (códigos HTTP e erros esperados)
- src/test/java/com/bancox/CLAUDE.md (padrões de teste)

NÃO consulte código existente — os testes devem refletir a spec, não a implementação.

Crie os seguintes testes em [NomeServiceTest] (@ExtendWith(MockitoExtension.class)):

## Testes obrigatórios por seção do UC

### Fluxo principal
- [nome_descritivo_do_cenario_feliz]

### Cada exceção documentada no UC (uma por erro)
- [deve_falhar_quando_condicao_1] → espera [ExceptionNome]
- [deve_falhar_quando_condicao_2] → espera [ExceptionNome]

### Casos-limite
- [descricao_do_caso_limite_1]
- [descricao_do_caso_limite_2]

Convencão de nomenclatura obrigatória:
  deve[ComportamentoEsperado]quando[Condicao]()
  Exemplo: deveRejeitarDebitoQuandoSaldoForInsuficiente()

Após os testes unitários, crie [NomeControllerTest] (@SpringBootTest + @AutoConfigureMockMvc)
cobrindo: fluxo principal HTTP + cada erro com código HTTP correto.

NÃO implemente nada além dos testes.
```

### Revisão manual antes do Prompt 2

Antes de executar o Prompt 2, verificar:
- [ ] Cada critério de aceitação do UC tem ao menos um teste correspondente
- [ ] Cada exceção documentada tem um teste de falha
- [ ] Os casos-limite estão cobertos
- [ ] Nenhum teste está vazio (sem assertions)
- [ ] Nenhum teste assume a implementação (usa apenas interfaces do service)

Se algum teste estiver faltando, adicionar manualmente antes de prosseguir.

### Prompt 2 ao agente — Implementar código para os testes passarem

```
Os testes do UC-XX ([Nome]) já estão escritos em:
- src/test/java/com/bancox/[NomeServiceTest].java
- src/test/java/com/bancox/[NomeControllerTest].java

Implemente o código de produção para fazê-los passar.

Antes de escrever qualquer código, leia:
- .claude/casos-de-uso/UC-XX-nome.md
- .claude/context/modelo-er/entidade-NOME.md
- .claude/context/contratos-api.md
- .claude/context/decisoes-arquiteturais.md

Implemente nesta ordem:

## 1. Migration Flyway
Criar src/main/resources/db/migration/VN__descricao.sql

## 2. Entidades JPA
Lombok: @Getter + @NoArgsConstructor + @Builder — NÃO @Data (DA-09)

## 3. Repositório
[NomeRepository] extends JpaRepository + métodos necessários

## 4. Exceptions
[NomeException] para cada erro + registrar no GlobalExceptionHandler

## 5. Service
[NomeService] com prioridade de validação conforme UC e contratos-api.md

## 6. Controller
[METHOD] /endpoint com @Operation, @ApiResponse e @Valid

## Restrição crítica
NÃO modificar nenhum arquivo em src/test/ — os testes são a spec.
Se um teste falhar por motivo que parece ser erro do teste, reportar antes de alterar.

## Ao finalizar
Executar mvn test — todos os testes devem passar.
Reportar quais testes passaram e quais falharam (se houver).
```

### O que fazer se um teste falhar

```
Teste falhou → analisar o motivo ANTES de qualquer alteração:

1. Falha por bug na implementação?
   → corrigir o código — NÃO modificar o teste

2. Falha porque o teste estava errado (spec ambígua)?
   → corrigir o UC-XX.md PRIMEIRO → atualizar o teste → corrigir o código

3. Falha porque a spec mudou durante a implementação?
   → parar, rever o UC com o time → atualizar spec → repetir o ciclo
```

---

## 2. Alteração de Feature Existente

### Quando usar
Quando uma funcionalidade existente muda de comportamento — novo campo,
nova regra, novo fluxo alternativo, mudança de contrato.

### Etapas

```
1. Analisar impacto: grep -r "UC-XX" .claude/ para encontrar dependências
2. Alterar UC-XX.md (User Story, critérios, fluxos, exemplos)
   - Adicionar nota "Alterado em: YYYY-MM-DD — [o que mudou]"
   - Adicionar seção "## Impacto nos UCs Existentes" se necessário
3. Atualizar entidades afetadas em modelo-er/
4. Atualizar contratos-api.md se mudou erro ou envelope
5. Adicionar DA-XX em decisoes-arquiteturais.md se houve decisão relevante
6. Registrar em git log (rastreabilidade via commits)
7. Executar prompt ao agente
8. Verificar DoD em src/tests/CLAUDE.md
```

### Regra de análise de impacto
Antes de alterar qualquer arquivo, executar:

```bash
grep -r "UC-XX" .claude/ --include="*.md" -l
```

Para cada arquivo encontrado, verificar se a alteração afeta:
- UCs que fazem `<<include>>` do UC alterado → podem herdar o impacto
- UCs que fazem `<<extend>>` do UC alterado → verificar se a condição ainda vale
- Modelo ER referenciado → verificar se campos/tabelas mudam

### Prompt ao agente — Alteração de Feature

```
Implemente a alteração do UC-XX ([Nome]) conforme documentado.

Antes de escrever qualquer código, leia:
- .claude/casos-de-uso/UC-XX-nome.md (versão atualizada — atenção às notas de alteração)
- .claude/context/modelo-er/entidade-NOME.md (campos novos ou alterados)
- .claude/context/contratos-api.md (novos erros ou envelopes)
- .claude/context/decisoes-arquiteturais.md DA-XX (decisão que motivou a alteração)

Implemente nesta ordem:

## 1. Migration Flyway (se mudou schema)
Criar src/main/resources/db/migration/VN__descricao_alteracao.sql
NÃO editar migrations existentes — criar nova migration de alteração (DA-02)

## 2. Entidades JPA (se mudou modelo)
- Atualizar [NomeEntity] com novos campos
- Manter Lombok conforme DA-09

## 3. Repositório (se necessário)
- [novos métodos de query]

## 4. Exceptions (se novos erros)
- Criar [NomeException]
- Registrar no GlobalExceptionHandler

## 5. Service — [NomeService]
- Alterar [método] conforme novo fluxo do UC
- [listar mudanças específicas]
- Verificar todos os métodos que usam [campo/regra alterada]

## 6. Controller (se mudou contrato)
- Atualizar [endpoint] conforme novo envelope

## 7. Testes
- Atualizar testes existentes que cobrem o fluxo alterado
- Adicionar testes para novos fluxos e exceções
- Nomenclatura espelhando o fluxo do UC

## O que NÃO alterar
- [listar explicitamente o que está fora do escopo]

## Ao finalizar
- Verificar o DoD em src/test/java/com/bancox/CLAUDE.md
- Executar SAST: `mvn verify` (SpotBugs + Dependency-Check)
- Executar Gitleaks: `gitleaks detect --source . --config .gitleaks.toml`
- Revisar relatórios em target/ antes de considerar concluído
```

---

## 3. Correção de Bug

### Quando usar
Bug encontrado em desenvolvimento, QA ou produção (sem urgência de hotfix).
O código vai para produção no próximo ciclo normal de deploy.

### Etapas

```
1. Criar .claude/bugs/BUG-XXX-descricao.md com reprodução exata
2. Diagnosticar: verificar se o bug está no código, na doc, ou nos dois
   - Se na doc: corrigir o UC/modelo-er/contratos-api antes do prompt
   - Se só no código: anotar na seção "Causa raiz" do BUG-XXX.md
3. Corrigir a documentação inconsistente (se houver)
4. Adicionar o cenário faltante nos exemplos do UC afetado
5. Registrar em git log (rastreabilidade via commits)
6. Executar prompt ao agente
7. Verificar DoD em src/tests/CLAUDE.md
```

### Template do BUG-XXX.md

```markdown
# BUG-XXX — Descrição curta

## Identificação
Data reportado : YYYY-MM-DD
Severidade     : CRÍTICO | ALTO | MÉDIO | BAIXO
Status         : ABERTO | EM ANDAMENTO | RESOLVIDO
Afeta          : UC-XX, DA-XX (se aplicável)

## Sintoma
[descrição do comportamento incorreto]

## Impacto
[consequência para o usuário ou sistema]

## Causa raiz (hipótese)
[onde está o bug — método, validação, query]

## Reprodução
```
Estado inicial: [condições necessárias]

[METHOD] /endpoint { corpo }

Resultado esperado : [o que deveria acontecer]
Resultado obtido   : [o que acontece]
```

## Arquivos impactados
- [Service/método] — [o que está errado]
- [UC-XX] — [inconsistência na doc, se houver]
- [Teste] — [caso faltante]
```

### Prompt ao agente — Correção de Bug

```
Corrija o BUG-XXX conforme documentado.

Antes de escrever qualquer código, leia:
- .claude/bugs/BUG-XXX-descricao.md (reprodução e causa raiz)
- .claude/casos-de-uso/UC-XX-nome.md (versão já corrigida)
- .claude/context/decisoes-arquiteturais.md DA-XX (regra violada, se aplicável)
- .claude/context/modelo-er/entidade-NOME.md (se envolve modelo)

## O que corrigir

### 1. [Arquivo/Classe]
[descrição exata do que mudar]

Substituir:
  [código incorreto]
Por:
  [código correto]

NÃO alterar mais nada no método — escopo mínimo.

### 2. Teste de regressão obrigatório
Adicionar em [NomeServiceTest] ANTES de qualquer outro teste:

```java
@Test
void [descricaoDoComportamentoCorreto]() {
    // given: [estado que reproduz o bug]
    // when: [ação que causava o bug]
    // then: [comportamento correto esperado]
    verify([repository], never()).save(any()); // se nenhuma escrita deve ocorrer
}
```

### 3. Teste de integração (se necessário)
[descrição do teste de integração cobrindo o cenário]

## O que NÃO alterar
- [listar explicitamente o que está fora do escopo]
- [outros métodos relacionados que NÃO devem ser tocados]

## Verificação final
mvn test -Dtest=[NomeServiceTest]
mvn verify
Confirmar que o cenário do BUG-XXX está coberto e passando.
Verificar DoD em src/tests/CLAUDE.md antes de fechar.
```

---

## 4. Hotfix Emergencial

### Quando usar
Bug crítico em produção que precisa de correção imediata, fora do ciclo normal
de deploy. Tempo é crítico — documentação completa vem depois, no follow-up.

### Princípio fundamental
> O registro vem antes do código, mesmo sob pressão.
> O follow-up é criado no mesmo momento do hotfix, não depois.

### Etapas

```
FASE 1 — HOTFIX (minutos)
1. Criar .claude/bugs/HF-XXX-descricao.md com reprodução e decisão de escopo
2. Criar .claude/bugs/FU-XXX-followup-hfXXX.md listando o que fica para depois
3. Executar prompt mínimo ao agente (3 alterações máximo)
4. Rodar mvn test -Dtest=[ClasseAfetadaTest] antes do deploy
5. Registrar em git log (rastreabilidade via commits) com horário

FASE 2 — FOLLOW-UP (horas depois, em ambiente calmo)
6. Atualizar UC afetado com novo fluxo/exceção
7. Atualizar contratos-api.md com novo código de erro
8. Adicionar testes de integração completos
9. Auditar outros UCs que possam ter o mesmo problema
10. Marcar HF-XXX como RESOLVIDO no arquivo
11. Registrar conclusão no git log (rastreabilidade via commits)
```

### Template do HF-XXX.md

```markdown
# HF-XXX — Descrição curta

## Identificação
Data reportado : YYYY-MM-DD HH:MM
Severidade     : CRÍTICO — impacto em produção
Tipo           : HOTFIX — correção emergencial
Status         : EM ANDAMENTO
Afeta          : UC-XX, UC-YY (se aplicável)

## Sintoma em produção
[descrição do comportamento incorreto em produção]

## Causa raiz
[onde está o bug]

## Reprodução
```
Estado: [condições]

[METHOD] /endpoint { corpo }

Resultado esperado : [comportamento correto]
Resultado obtido   : [comportamento incorreto]
```

## Decisão de hotfix — ESCOPO MÍNIMO
  ✓ [o que será feito agora]
  ✗ NÃO [o que fica para o follow-up]
  ✗ NÃO [o que fica para o follow-up]

## Follow-up obrigatório (após hotfix em produção)
Abrir FU-XXX para:
  - [item 1]
  - [item 2]
```

### Template do FU-XXX.md

```markdown
# FU-XXX — Follow-up do HF-XXX (pós-produção)

## Contexto
HF-XXX foi aplicado em produção em YYYY-MM-DD HH:MM.

## Status do hotfix
- [x] [o que foi feito no hotfix]
- [ ] Documentação atualizada (este follow-up)
- [ ] Testes de integração completos

## O que fazer neste follow-up

### 1. Atualizar [UC-XX]
[o que adicionar/corrigir no UC]

### 2. Atualizar [contratos-api.md / outro arquivo]
[o que adicionar]

### 3. Testes de integração
[testes a adicionar]

### 4. Auditoria
[outros locais a verificar]
```

### Prompt ao agente — Hotfix (FASE 1)

```
HOTFIX EMERGENCIAL — escopo mínimo obrigatório.
Altere APENAS o necessário para corrigir o problema em produção.
Qualquer melhoria fora do escopo vai para o follow-up FU-XXX.

Leia antes de escrever qualquer código:
- .claude/bugs/HF-XXX-descricao.md (reprodução e escopo decidido)
- .claude/casos-de-uso/UC-XX-nome.md (validações atuais)

## O que corrigir — [N] alterações apenas

### 1. [Classe/Método]
[descrição exata e posição no código]

```java
// substituir:
[código incorreto]

// por:
[código correto]
```

### 2. Exception (se necessário)
Criar [NomeException] em exception/

### 3. GlobalExceptionHandler (se necessário)
Adicionar handler:
  [NomeException] → HTTP [código]
  body: { "erro": "CODIGO", "mensagem": "Mensagem legível." }

## Teste mínimo obrigatório (deve passar ANTES do deploy)
```java
@Test
void [descricaoDoComportamentoCorreto]() {
    // given: [estado que reproduz o bug]
    // when + then: [comportamento correto]
    verify([repository], never()).save(any());
}
```

## O que NÃO fazer agora
- NÃO atualizar [UC-XX] (follow-up FU-XXX)
- NÃO atualizar [contratos-api.md] (follow-up FU-XXX)
- NÃO adicionar testes de integração (follow-up FU-XXX)
- NÃO refatorar nenhum outro método

## Verificação antes do deploy
mvn test -Dtest=[ClasseAfetadaTest]
Confirmar que o teste acima passa.
Confirmar que os testes existentes de UC-XX não regrediram.
```

### Prompt ao agente — Follow-up (FASE 2)

```
Execute o follow-up FU-XXX após o hotfix HF-XXX.

Leia antes de escrever qualquer código:
- .claude/bugs/FU-XXX-followup-hfXXX.md (lista completa do que fazer)
- .claude/bugs/HF-XXX-descricao.md (contexto do que foi corrigido)
- .claude/casos-de-uso/UC-XX-nome.md (para atualizar)
- .claude/context/contratos-api.md (para atualizar)

## 1. Atualizar UC-XX-nome.md
[especificar o que adicionar: nova exceção, novo exemplo, nova validação]

## 2. Atualizar contratos-api.md
[especificar novos códigos de erro ou envelopes]

## 3. Testes de integração
[descrever os testes a adicionar]

## 4. Auditoria
Executar:
  grep -r "[campo/método afetado]" src/main/java/
Reportar qualquer outro ponto com o mesmo problema sem corrigir agora.

## Ao finalizar
- Marcar HF-XXX como RESOLVIDO com data e referência ao commit
- Registrar conclusão em .claude/git log (rastreabilidade via commits)
- Verificar DoD em src/tests/CLAUDE.md
```

---

## Resumo comparativo dos fluxos

| | Nova Feature | Alteração | Bug | Hotfix |
|---|---|---|---|---|
| Doc antes do código | Sim | Sim | Sim | Registro mínimo antes, doc completa depois |
| Análise de impacto | Criar UC | `grep -r "UC-XX"` | `grep -r "UC-XX"` | Identificar causa raiz |
| Escopo do prompt | Completo | Completo | Mínimo | Mínimo absoluto |
| Testes | Completos | Completos + regressão | Regressão obrigatória | 1 teste antes do deploy |
| Follow-up | Não | Não | Não | Obrigatório e criado junto |
| CHANGELOG | 1 entrada | 1 entrada | 1 entrada | 2 entradas com horário |
| Arquivo de rastreio | UC-XX.md | UC-XX.md (alterado) | BUG-XXX.md | HF-XXX.md + FU-XXX.md |

---

## 5. SAST — Quando e como executar

### Em todos os fluxos

| Fluxo         | SpotBugs + Dep-Check | Gitleaks | Falha o processo? |
|---------------|----------------------|----------|-------------------|
| Nova feature  | mvn verify           | obrigatório | Sim — corrigir antes do PR |
| Alteração     | mvn verify           | obrigatório | Sim — corrigir antes do PR |
| Bug           | mvn verify           | obrigatório | Sim — corrigir antes do PR |
| Hotfix        | mvn verify           | obrigatório | Não bloqueia — registrar BUG para follow-up |

### Falso positivo — o que fazer

1. Ler o relatório completo em target/spotbugs.html ou target/dependency-check-report.html
2. Confirmar manualmente que é falso positivo para o contexto do BancoX
3. Adicionar exclusão documentada:
   - SpotBugs → spotbugs-exclude.xml (com comment explicando)
   - Dependency-Check → dependency-check-suppressions.xml (com notes + until)
4. Commitar os arquivos de exclusão junto com o código
5. Registrar em git log (rastreabilidade via commits)

### Segredo detectado pelo Gitleaks — o que fazer

1. Se segredo está no código atual → remover, rotacionar o segredo real, commitar
2. Se segredo está no histórico git → usar git filter-repo para reescrever histórico
3. Se é falso positivo → adicionar ao bloco regexes do .gitleaks.toml com comentário
4. NUNCA: commitar com --no-verify para pular a verificação

### Comandos de referência

```bash
# SAST completo (integrado ao Maven)
mvn verify

# Apenas SpotBugs (mais rápido — sem testes)
mvn spotbugs:check

# Apenas Dependency-Check
mvn dependency-check:check

# Gitleaks — código atual
gitleaks detect --source . --config .gitleaks.toml

# Gitleaks — histórico git completo
gitleaks detect --source . --config .gitleaks.toml --log-opts="HEAD~50..HEAD"

# Ver relatório SpotBugs no browser
open target/spotbugs.html

# Ver relatório Dependency-Check no browser
open target/dependency-check-report.html
```
