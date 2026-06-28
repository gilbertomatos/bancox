# entidade-cliente.md — CLIENTE

## Campos

| Campo     | Tipo         | Restrição         |
|-----------|--------------|-------------------|
| id        | UUID v4      | PK                |
| nome      | VARCHAR(100) | NOT NULL          |
| cpf       | VARCHAR(11)  | UNIQUE, NOT NULL  |
| criado_em | TIMESTAMPTZ  | NOT NULL, UTC     |

## Relacionamentos

| Relação  | Entidade | Cardinalidade | Descrição                          |
|----------|----------|---------------|------------------------------------|
| possui   | CONTA    | 1 para N      | Um cliente pode ter várias contas  |

## Regras

- `cpf` deve ser armazenado apenas com dígitos (sem pontos ou traços)
- `criado_em` preenchido automaticamente no INSERT, nunca atualizado
- Exclusão de CLIENTE não é permitida se houver CONTA vinculada
