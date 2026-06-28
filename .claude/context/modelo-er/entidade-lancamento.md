# entidade-lancamento.md — LANCAMENTO

## Campos

| Campo       | Tipo          | Restrição                                    |
|-------------|---------------|----------------------------------------------|
| id          | UUID v4       | PK                                           |
| conta_id    | UUID v4       | FK → CONTA.id, NOT NULL                      |
| tipo        | ENUM          | 'credito', 'debito', 'transferencia'         |
| valor       | NUMERIC(15,2) | NOT NULL, CHECK > 0                          |
| saldo_apos  | NUMERIC(15,2) | NOT NULL, CHECK >= 0                         |
| transfer_id | UUID v4       | FK → TRANSFERENCIA.id, NULLABLE              |
| criado_em   | TIMESTAMPTZ   | NOT NULL, UTC                                |

## Relacionamentos

| Relação    | Entidade      | Cardinalidade | Descrição                                   |
|------------|---------------|---------------|---------------------------------------------|
| pertence a | CONTA         | N para 1      | Todo lançamento pertence a uma conta        |
| origina de | TRANSFERENCIA | N para 1      | Lançamento de transferência tem transfer_id |

## Regras

- `LANCAMENTO` é **imutável** — nunca UPDATE ou DELETE após INSERT
- `transfer_id` preenchido somente quando `tipo = 'transferencia'`
- `transfer_id` é NULL quando `tipo = 'credito'` ou `tipo = 'debito'` direto
- `saldo_apos` representa o saldo da conta imediatamente após este lançamento
- `criado_em` preenchido automaticamente no INSERT, nunca atualizado
- Dois lançamentos com o mesmo `transfer_id` representam os dois lados de uma transferência
