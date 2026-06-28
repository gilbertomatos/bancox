# entidade-transferencia.md — TRANSFERENCIA

## Campos

| Campo         | Tipo          | Restrição                                          |
|---------------|---------------|----------------------------------------------------|
| id            | UUID v4       | PK                                                 |
| conta_origem  | UUID v4       | FK → CONTA.id, NOT NULL                            |
| conta_destino | UUID v4       | FK → CONTA.id, NOT NULL                            |
| valor         | NUMERIC(15,2) | NOT NULL, CHECK > 0                                |
| criado_em     | TIMESTAMPTZ   | NOT NULL, UTC                                      |

## Relacionamentos

| Relação       | Entidade   | Cardinalidade | Descrição                                      |
|---------------|------------|---------------|------------------------------------------------|
| origina       | LANCAMENTO | 1 para 2      | Gera exatamente dois lançamentos (origem + destino) |
| debitada de   | CONTA      | N para 1      | Conta que perde o valor                        |
| creditada em  | CONTA      | N para 1      | Conta que recebe o valor                       |

## Regras

- `conta_origem != conta_destino` — CHECK no banco
- `TRANSFERENCIA` é criada somente após os dois lançamentos serem persistidos com sucesso
- Em caso de rollback, o registro de TRANSFERENCIA também é revertido
- `id` desta entidade é o `transfer_id` nos dois LANCAMENTOS gerados
- `criado_em` preenchido automaticamente no INSERT, nunca atualizado
- Exclusão de TRANSFERENCIA não é permitida após commit
