# entidade-usuario.md — USUARIO

## Campos

| Campo      | Tipo         | Restrição                                      |
|------------|--------------|------------------------------------------------|
| id         | UUID v4      | PK                                             |
| cpf        | VARCHAR(11)  | UNIQUE, NOT NULL                               |
| senha_hash | VARCHAR(60)  | NOT NULL (BCrypt strength 12 — nunca texto puro)|
| perfil     | ENUM         | 'CORRENTISTA', 'OPERADOR', 'ADMIN'             |
| conta_id   | UUID v4      | FK → CONTA.id, NULLABLE (OPERADOR/ADMIN não têm conta) |
| ativo      | BOOLEAN      | NOT NULL, DEFAULT true                         |
| criado_em  | TIMESTAMPTZ  | NOT NULL, UTC                                  |

## Relacionamentos

| Relação    | Entidade | Cardinalidade | Descrição                              |
|------------|----------|---------------|----------------------------------------|
| possui     | CONTA    | 1 para 0..1   | CORRENTISTA tem conta, OPERADOR não    |

## Regras

- `senha_hash` armazenada sempre como BCrypt strength 12 (DS-02)
- `cpf` armazenado apenas com dígitos (sem pontos ou traços)
- `conta_id` preenchido apenas para perfil CORRENTISTA
- `ativo = false` desativa o login sem deletar o registro
- Nunca retornar `senha_hash` em nenhum endpoint
