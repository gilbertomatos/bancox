# UC-02 — Débito (Saque ou Pagamento)
> Incluído por: UC-03

## User Story
Como correntista autenticado
Quero realizar um débito (saque ou pagamento)
Para utilizar meu saldo disponível sem ir a uma agência

## Critérios de Aceitação
- [ ] Débito com saldo suficiente atualiza o saldo corretamente
- [ ] Lançamento do tipo `debito` registrado com timestamp UTC
- [ ] Saldo nunca fica negativo após débito
- [ ] Saldo insuficiente rejeitado com SALDO_INSUFICIENTE
- [ ] Valor zero ou negativo rejeitado com VALOR_INVALIDO
- [ ] Conta inexistente rejeitada com CONTA_NAO_ENCONTRADA

## Atores
Correntista → API (POST) → ContaService → ContaRepository + LancamentoRepository

## Fluxo Principal

```
1. POST /contas/{conta_id}/debito
   Body: { "valor": 300.00 }

2. ContaService valida (nesta ordem):
   - conta_id existe
   - valor > 0
   - saldo_atual >= valor

3. saldo_novo = saldo_atual - valor

4. LancamentoRepository persiste:
   { tipo: "debito", valor, timestamp: UTC_NOW, saldo_apos: saldo_novo, transfer_id: null }

5. ContaRepository atualiza saldo_atual = saldo_novo

6. Retorna HTTP 200 com envelope de sucesso
```

## Exceção — Saldo Insuficiente
```
→ HTTP 422 { "erro": "SALDO_INSUFICIENTE",
              "mensagem": "Saldo disponível insuficiente para esta operação." }
```

## Exceção — Valor Inválido (valor <= 0)
```
→ HTTP 422 { "erro": "VALOR_INVALIDO",
              "mensagem": "O valor do débito deve ser maior que zero." }
```

## Exceção — Conta Não Encontrada
```
→ HTTP 404 { "erro": "CONTA_NAO_ENCONTRADA",
              "mensagem": "Conta informada não existe." }
```

## Exemplos

### Caso feliz
```
ENTRADA : POST /contas/abc-123/debito { "valor": 300.00 }  (saldo: 1700.00)
SAÍDA   : 200 { "status": "sucesso", "operacao": "debito",
                "valor": 300.00, "saldo_anterior": 1700.00,
                "saldo_atual": 1400.00, "timestamp": "2026-06-25T11:00:00Z" }
POR QUÊ : saldo 1700.00 >= 300.00, operação válida
```

### Caso limite — débito exato do saldo disponível
```
ENTRADA : POST /contas/abc-123/debito { "valor": 1400.00 }  (saldo: 1400.00)
SAÍDA   : 200 { ..., "saldo_anterior": 1400.00, "saldo_atual": 0.00 }
POR QUÊ : saldo == valor é permitido, resultado é zero (não negativo)
```

### Caso de erro — saldo insuficiente
```
ENTRADA : POST /contas/abc-123/debito { "valor": 1400.01 }  (saldo: 1400.00)
SAÍDA   : 422 { "erro": "SALDO_INSUFICIENTE", ... }
ERRO A EVITAR: nunca executar escrita antes de validar saldo
```

## Critério de Sucesso
Saldo reduzido sem ficar negativo e lançamento registrado em uma única operação atômica.

## Segurança (ver autenticacao-autorizacao.md)
- Requer: Bearer token válido com perfil CORRENTISTA
- Ownership: conta_id da URL deve pertencer ao correntista autenticado → senão 403 ACESSO_NEGADO
- Prioridade completa: token → perfil → ownership → conta existe → status ATIVA → valor válido → saldo suficiente → execução

