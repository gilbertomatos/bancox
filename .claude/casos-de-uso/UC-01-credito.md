# UC-01 — Crédito (Depósito)
> Incluído por: UC-03

## User Story
Como correntista autenticado
Quero depositar um valor na minha conta
Para ter saldo disponível para minhas despesas

## Critérios de Aceitação
- [ ] Depósito com valor positivo atualiza o saldo corretamente
- [ ] Lançamento do tipo `credito` registrado com timestamp UTC
- [ ] Resposta inclui saldo_anterior e saldo_atual
- [ ] Valor zero ou negativo rejeitado com VALOR_INVALIDO
- [ ] Conta inexistente rejeitada com CONTA_NAO_ENCONTRADA

## Atores
Correntista → API (POST) → ContaService → ContaRepository + LancamentoRepository

## Fluxo Principal

```
1. POST /contas/{conta_id}/credito
   Body: { "valor": 500.00 }

2. ContaService valida:
   - conta_id existe
   - valor > 0

3. saldo_novo = saldo_atual + valor

4. LancamentoRepository persiste:
   { tipo: "credito", valor, timestamp: UTC_NOW, saldo_apos: saldo_novo, transfer_id: null }

5. ContaRepository atualiza saldo_atual = saldo_novo

6. Retorna HTTP 200 com envelope de sucesso
```

## Exceção — Valor Inválido (valor <= 0)
```
→ HTTP 422 { "erro": "VALOR_INVALIDO",
              "mensagem": "O valor do crédito deve ser maior que zero." }
```

## Exceção — Conta Não Encontrada
```
→ HTTP 404 { "erro": "CONTA_NAO_ENCONTRADA",
              "mensagem": "Conta informada não existe." }
```

## Exemplos

### Caso feliz
```
ENTRADA : POST /contas/abc-123/credito { "valor": 500.00 }
SAÍDA   : 200 { "status": "sucesso", "operacao": "credito",
                "valor": 500.00, "saldo_anterior": 1200.00,
                "saldo_atual": 1700.00, "timestamp": "2026-06-25T10:32:00Z" }
POR QUÊ : conta existe, valor > 0, soma simples
```

### Caso limite — primeiro depósito (saldo zero)
```
ENTRADA : POST /contas/abc-123/credito { "valor": 100.00 }
SAÍDA   : 200 { ..., "saldo_anterior": 0.00, "saldo_atual": 100.00 }
POR QUÊ : saldo zero é válido como ponto de partida
```

### Caso de erro — valor negativo
```
ENTRADA : POST /contas/abc-123/credito { "valor": -50.00 }
SAÍDA   : 422 { "erro": "VALOR_INVALIDO", ... }
ERRO A EVITAR: não escrever nada no banco antes de validar o valor
```

## Critério de Sucesso
Saldo atualizado e lançamento registrado em uma única operação atômica.

## Segurança (ver autenticacao-autorizacao.md)
- Requer: Bearer token válido com perfil CORRENTISTA
- Ownership: conta_id da URL deve pertencer ao correntista autenticado → senão 403 ACESSO_NEGADO
- Prioridade completa: token → perfil → ownership → conta existe → valor válido → execução

