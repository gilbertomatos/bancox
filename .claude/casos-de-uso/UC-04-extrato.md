# UC-04 — Extrato
> Estende: UC-03 (exibe transfer_id quando lançamento for transferência)

## User Story
Como correntista autenticado
Quero consultar meu extrato
Para entender minha movimentação financeira e conferir lançamentos

## Critérios de Aceitação
- [ ] Lançamentos retornados em ordem cronológica crescente
- [ ] Sem filtro de data: padrão de 30 dias aplicado automaticamente
- [ ] Limite máximo respeitado: 90 dias ou 50 lançamentos
- [ ] Lançamentos de transferência exibem transfer_id preenchido
- [ ] Lançamentos de crédito e débito diretos exibem transfer_id = null
- [ ] Período sem movimentação retorna lista vazia com aviso SEM_LANCAMENTOS
- [ ] data_inicio > data_fim rejeitado com INTERVALO_INVALIDO
- [ ] Conta inexistente rejeitada com CONTA_NAO_ENCONTRADA
- [ ] Saldo atual reflete o momento da consulta

## Atores
Correntista → API (GET) → ExtratoService → LancamentoRepository

## Fluxo Principal

```
1. GET /contas/{conta_id}/extrato?data_inicio=2026-05-01&data_fim=2026-06-25

2. ExtratoService valida:
   - conta_id existe
   - data_inicio <= data_fim

3. Aplicar limites:
   - máximo 90 dias (truncar silenciosamente se exceder)
   - máximo 50 lançamentos (retornar os mais recentes)

4. Buscar lançamentos em ordem cronológica crescente

5. Retorna HTTP 200 com lista + saldo_atual
```

## Alternativo — Sem Filtro de Data
```
Sem query params → aplicar padrão:
  data_inicio = hoje - 30 dias
  data_fim    = hoje
Continuar a partir da etapa 2
```

## Exceção — Intervalo Inválido
```
→ HTTP 422 { "erro": "INTERVALO_INVALIDO",
              "mensagem": "A data de início deve ser anterior à data de fim." }
```

## Exceção — Conta Não Encontrada
```
→ HTTP 404 { "erro": "CONTA_NAO_ENCONTRADA",
              "mensagem": "Conta informada não existe." }
```

## Exceção — Sem Lançamentos no Período
```
→ HTTP 200 { "aviso": "SEM_LANCAMENTOS",
              "mensagem": "Nenhuma movimentação encontrada no período informado.",
              "lancamentos": [], "saldo_atual": 0.00 }
```

## Exemplos

### Caso feliz — lançamentos mistos
```
ENTRADA : GET /contas/abc-123/extrato
SAÍDA   : 200 {
            "status": "sucesso", "operacao": "extrato",
            "periodo": "2026-05-26 a 2026-06-25", "total_lancamentos": 3,
            "lancamentos": [
              { "tipo": "credito",       "valor": 500.00, "saldo_apos": 1700.00,
                "timestamp": "2026-06-25T10:32:00Z", "transfer_id": null },
              { "tipo": "debito",        "valor": 300.00, "saldo_apos": 1400.00,
                "timestamp": "2026-06-25T11:00:00Z", "transfer_id": null },
              { "tipo": "transferencia", "valor": 200.00, "saldo_apos": 1200.00,
                "timestamp": "2026-06-25T12:00:00Z", "transfer_id": "uuid-xyz" }
            ],
            "saldo_atual": 1200.00
          }
POR QUÊ : transfer_id preenchido só em transferência (extensão UC-03)
```

### Caso limite — período truncado em 90 dias
```
ENTRADA : GET /contas/abc-123/extrato?data_inicio=2025-01-01&data_fim=2026-06-25
SAÍDA   : 200 { "periodo": "2026-03-27 a 2026-06-25", ... }
POR QUÊ : truncado silenciosamente, sem erro
```

## Critério de Sucesso
Lista cronológica crescente com saldo_atual correto no momento da consulta.

## Segurança (ver autenticacao-autorizacao.md)
- Requer: Bearer token válido com perfil CORRENTISTA
- Ownership: conta_id da URL deve pertencer ao correntista autenticado → senão 403 ACESSO_NEGADO
- Prioridade completa: token → perfil → ownership → conta existe → execução

