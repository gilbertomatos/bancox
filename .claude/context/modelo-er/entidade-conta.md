# entidade-conta.md — CONTA

## Campos

| Campo      | Tipo          | Restrição                       |
|------------|---------------|---------------------------------|
| id         | UUID v4       | PK                              |
| cliente_id | UUID v4       | FK → CLIENTE.id, NOT NULL       |
| saldo      | NUMERIC(15,2) | NOT NULL, DEFAULT 0, CHECK >= 0 |
| criado_em  | TIMESTAMPTZ   | NOT NULL, UTC                   |

## Relacionamentos

| Relação      | Entidade      | Cardinalidade | Descrição                              |
|--------------|---------------|---------------|----------------------------------------|
| pertence a   | CLIENTE       | N para 1      | Toda conta tem exatamente um titular   |
| gera         | LANCAMENTO    | 1 para N      | Toda operação gera um lançamento       |
| origem em    | TRANSFERENCIA | 1 para N      | Conta pode ser origem de transferências|
| destino em   | TRANSFERENCIA | 1 para N      | Conta pode ser destino de transferências|

## Regras

- `saldo` nunca pode ser negativo — garantido por CHECK no banco E validação em ContaService
- `saldo` é atualizado somente via services/ — nunca diretamente por repositories/
- `criado_em` preenchido automaticamente no INSERT, nunca atualizado
- Exclusão de CONTA não é permitida se houver LANCAMENTO vinculado
