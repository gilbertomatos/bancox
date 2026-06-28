# contratos-api.md — Contratos de API BancoX
> Envelopes de resposta e catálogo de erros.
> Consultar ao implementar GlobalExceptionHandler, controllers e templates.

---

## Envelopes de Sucesso

| Operação              | HTTP | Campos obrigatórios                                                    |
|-----------------------|------|------------------------------------------------------------------------|
| credito / debito      | 200  | status, operacao, valor, saldo_anterior, saldo_atual, timestamp        |
| transferencia         | 200  | status, operacao, valor, saldo_anterior, saldo_atual, timestamp        |
| extrato               | 200  | status, operacao, periodo, total_lancamentos, lancamentos[], saldo_atual|
| bloqueio / desbloqueio| 200  | status, operacao, conta_id, novo_status, timestamp                     |
| login / refresh       | 200  | status, access_token, refresh_token, expires_in, perfil                |
| logout                | 204  | (sem corpo)                                                            |
| agendamento (débito)  | 202  | status="agendado", operacao, valor, data_agendamento, saldo_reservado, agendamento_id |

Campo `lancamentos[]` no extrato: tipo, valor, saldo_apos, timestamp, transfer_id (null se não for transferência)

---

## Catálogo de Erros

| Código                       | HTTP | Quando usar                                                  |
|------------------------------|------|--------------------------------------------------------------|
| VALOR_INVALIDO               | 422  | valor <= 0 em qualquer operação                              |
| CONTA_NAO_ENCONTRADA         | 404  | conta_id não existe                                          |
| CONTA_DESTINO_NAO_ENCONTRADA | 404  | conta_destino não existe em transferência                    |
| SALDO_INSUFICIENTE           | 422  | saldo < valor em débito ou transferência                     |
| CONTAS_IDENTICAS             | 422  | conta_origem == conta_destino                                |
| INTERVALO_INVALIDO           | 422  | data_inicio > data_fim no extrato                            |
| CONTA_BLOQUEADA              | 422  | operação financeira em conta com status BLOQUEADA            |
| CONTA_JA_BLOQUEADA           | 422  | tentativa de bloquear conta já bloqueada                     |
| CONTA_JA_ATIVA               | 422  | tentativa de desbloquear conta já ativa                      |
| MOTIVO_INVALIDO              | 422  | motivo ausente ou com mais de 255 caracteres                 |
| DATA_AGENDAMENTO_INVALIDA    | 422  | data_agendamento <= hoje                                     |
| CONTA_DESTINO_BLOQUEADA      | 422  | conta_destino com status BLOQUEADA em transferência          |
| TOKEN_AUSENTE                | 401  | cookie JWT não presente                                      |
| TOKEN_INVALIDO               | 401  | JWT malformado ou assinatura inválida                        |
| TOKEN_EXPIRADO               | 401  | JWT expirado                                                 |
| CREDENCIAIS_INVALIDAS        | 401  | CPF ou senha incorretos                                      |
| ACESSO_NEGADO                | 403  | perfil insuficiente ou ownership violado                     |
| OPERACAO_INVALIDA            | 400  | operação não reconhecida                                     |
| CAMPO_FALTANTE               | 400  | campo obrigatório ausente (incluir nome do campo)            |
| FALHA_TRANSACAO              | 500  | falha parcial em transferência — após rollback               |
| ERRO_INTERNO                 | 500  | qualquer erro não mapeado                                    |

## Avisos (HTTP 200 com corpo diferente)

| Código          | Quando usar                                    |
|-----------------|------------------------------------------------|
| SEM_LANCAMENTOS | Extrato sem movimentação no período informado  |

---

## Formato padrão dos envelopes

```
Sucesso:  { "status": "sucesso",  "operacao": "...", ...campos específicos }
Erro:     { "status": "erro",     "erro": "CODIGO",  "mensagem": "texto legível" }
Agendado: { "status": "agendado", "operacao": "...", ...campos específicos }
```

NÃO expor stack trace — apenas mensagem legível (DA-07).
NÃO exibir código de erro na UI — usar campo "mensagem".
