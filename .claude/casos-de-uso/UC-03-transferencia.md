# UC-03 — Transferência entre Contas
> Inclui: UC-02 (débito na origem), UC-01 (crédito no destino)

## User Story
Como correntista autenticado
Quero transferir um valor para outra conta do banco
Para pagar alguém de forma imediata e rastreável

## Critérios de Aceitação
- [ ] Transferência válida debita origem e credita destino atomicamente
- [ ] Ambos os lançamentos compartilham o mesmo `transfer_id`
- [ ] Saldo da conta_origem nunca fica negativo
- [ ] Falha em qualquer etapa aciona rollback total
- [ ] Contas iguais rejeitadas com CONTAS_IDENTICAS
- [ ] Conta destino inexistente rejeitada com CONTA_DESTINO_NAO_ENCONTRADA
- [ ] Saldo insuficiente rejeitado com SALDO_INSUFICIENTE
- [ ] Valor inválido rejeitado com VALOR_INVALIDO

## Atores
Correntista → API (POST) → TransferenciaService → ContaService (x2) + LancamentoRepository (x2)

## Fluxo Principal

```
1. POST /transferencias
   Body: { "conta_origem": "abc-123", "conta_destino": "xyz-456", "valor": 200.00 }

2. TransferenciaService valida (tudo antes de qualquer escrita):
   - conta_origem existe
   - conta_destino existe
   - conta_origem != conta_destino
   - valor > 0
   - saldo_origem >= valor

3. Gerar transfer_id (UUID v4)

4. Iniciar transação atômica:
   4a. [inclui UC-02] Debitar conta_origem
       Lançamento: { tipo: "transferencia", valor, saldo_apos, transfer_id }
   4b. [inclui UC-01] Creditar conta_destino
       Lançamento: { tipo: "transferencia", valor, saldo_apos, transfer_id }

5. Commit

6. Retorna HTTP 200 com saldo_apos da conta_origem
```

## Exceção — Contas Idênticas
```
→ HTTP 422 { "erro": "CONTAS_IDENTICAS",
              "mensagem": "Conta de origem e destino não podem ser a mesma." }
```

## Exceção — Conta Destino Não Encontrada
```
→ HTTP 404 { "erro": "CONTA_DESTINO_NAO_ENCONTRADA",
              "mensagem": "Conta de destino não encontrada." }
```

## Exceção — Saldo Insuficiente
```
→ HTTP 422 { "erro": "SALDO_INSUFICIENTE",
              "mensagem": "Saldo insuficiente na conta de origem." }
```

## Exceção — Falha na Transação Atômica
```
Qualquer erro nas etapas 4a ou 4b → ROLLBACK automático
→ HTTP 500 { "erro": "FALHA_TRANSACAO",
              "mensagem": "Transferência cancelada. Nenhum valor foi movimentado." }
Ver exemplo de rollback abaixo
```

## Exemplos

### Caso feliz
```
ENTRADA : POST /transferencias
          { "conta_origem": "abc-123", "conta_destino": "xyz-456", "valor": 200.00 }
SAÍDA   : 200 { "status": "sucesso", "operacao": "transferencia",
                "valor": 200.00, "saldo_anterior": 1400.00,
                "saldo_atual": 1200.00, "timestamp": "2026-06-25T12:00:00Z" }
```

### Caso de erro — contas idênticas
```
ENTRADA : POST /transferencias
          { "conta_origem": "abc-123", "conta_destino": "abc-123", "valor": 100.00 }
SAÍDA   : 422 { "erro": "CONTAS_IDENTICAS", ... }
ERRO A EVITAR: nunca executar nenhuma etapa antes de validar todas as condições
```

## Critério de Sucesso
Débito e crédito ocorrem atomicamente com o mesmo transfer_id.
Ou os dois ocorrem ou nenhum ocorre.

### Exemplo de rollback — falha parcial na transação atômica
```
ESTADO INICIAL:
  abc-123: saldo = 1000.00
  xyz-456: saldo = 200.00

POST /transferencias { "conta_origem": "abc-123", "conta_destino": "xyz-456", "valor": 400.00 }

Etapa 4a → débito executado em abc-123
  abc-123.saldo = 600.00  (em memória, dentro da transação)

Etapa 4b → falha (timeout, constraint violation, erro de rede)
  ROLLBACK automático disparado

ESTADO APÓS ROLLBACK:
  abc-123: saldo = 1000.00  (restaurado)
  xyz-456: saldo = 200.00   (inalterado)
  LANCAMENTO: nenhum registro persistido
  TRANSFERENCIA: nenhum registro persistido

SAÍDA: HTTP 500 { "erro": "FALHA_TRANSACAO",
                  "mensagem": "Transferência cancelada. Nenhum valor foi movimentado." }
```

O agente deve garantir:
- Ambas as contas no estado original após rollback
- Nenhum lançamento persistido em nenhuma das contas
- Log interno registra transfer_id + motivo da falha (auditoria)
- Mensagem ao cliente não expõe detalhes técnicos

## Segurança (ver autenticacao-autorizacao.md)
- Requer: Bearer token válido com perfil CORRENTISTA
- Ownership: conta_origem deve pertencer ao correntista autenticado → senão 403 ACESSO_NEGADO
- conta_destino pode pertencer a qualquer correntista (sem restrição de ownership)
- Prioridade completa: token → perfil → ownership (origem) → contas existem → contas distintas → valor válido → saldo suficiente → execução

