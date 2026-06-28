# UC-UI-03 — Transferência com Confirmação
> Template: correntista/transferencia.html
> Rotas: GET /transferencia, POST /transferencia, POST /transferencia/confirmar
> UC de negócio: UC-03

## User Story
Como correntista autenticado
Quero transferir com uma etapa de confirmação antes de enviar
Para evitar transferências acidentais

## Critérios de Aceitação
- [ ] Etapa 1: formulário com conta_destino e valor
- [ ] Etapa 2: tela de confirmação com resumo (th:text com dados da sessão)
- [ ] Etapa 3: comprovante com transfer_id
- [ ] Botão "Voltar" retorna à etapa anterior sem perder dados
- [ ] Saldo após calculado pelo controller antes de exibir confirmação
- [ ] Erro retorna à etapa 1 com mensagem legível via fragment alert

## Fluxo entre Controllers

```
GET  /transferencia          → exibir etapa 1 (formulário)
POST /transferencia          → validar + armazenar em sessão → exibir etapa 2 (confirmação)
POST /transferencia/confirmar → executar + redirect → etapa 3 (comprovante) ou etapa 1 (erro)
```

## Dados disponíveis por etapa

**Etapa 1 — Formulário**

| Variável   | Tipo       | Conteúdo              |
|------------|------------|-----------------------|
| saldoAtual | BigDecimal | saldo disponível      |

**Etapa 2 — Confirmação**

| Variável      | Tipo       | Conteúdo                             |
|---------------|------------|--------------------------------------|
| contaDestino  | String     | conta de destino informada           |
| valor         | BigDecimal | valor a transferir                   |
| saldoAnterior | BigDecimal | saldo atual antes da transferência   |
| saldoApos     | BigDecimal | saldo projetado após a transferência |

**Etapa 3 — Comprovante**

| Variável   | Tipo     | Conteúdo                        |
|------------|----------|---------------------------------|
| transferId | UUID     | identificador da transferência  |
| valor      | BigDecimal | valor transferido              |
| timestamp  | Instant  | data e hora da operação         |
