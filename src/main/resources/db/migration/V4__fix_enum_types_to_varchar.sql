-- V4__fix_enum_types_to_varchar.sql
-- Correção: tipos ENUM customizados do PostgreSQL → VARCHAR com CHECK constraint
--
-- Motivo: Hibernate mapeia @Enumerated(EnumType.STRING) para VARCHAR por padrão.
-- Tipos customizados do PostgreSQL (tipo_lancamento, perfil_usuario) causam erro:
-- "column is of type tipo_lancamento but expression is of type character varying"
--
-- Solução: substituir CREATE TYPE ... AS ENUM por VARCHAR com CHECK constraint.
-- Comportamento idêntico para a aplicação — sem perda de integridade referencial.
-- Hibernate @Enumerated(EnumType.STRING) funciona nativamente com VARCHAR.
--
-- Referência: DA-29 (mapeamento de enums Java → PostgreSQL)

-- ─── LANCAMENTO.tipo ─────────────────────────────────────────────────────────

-- 1. Garantir que o tipo é VARCHAR(20)
ALTER TABLE lancamento ALTER COLUMN tipo TYPE VARCHAR(20);

-- 2. Recriar CHECK constraint de forma idempotente
--    (V1 pode já tê-la criado inline — DROP IF EXISTS garante segurança)
ALTER TABLE lancamento DROP CONSTRAINT IF EXISTS ck_lancamento_tipo;
ALTER TABLE lancamento
    ADD CONSTRAINT ck_lancamento_tipo
    CHECK (tipo IN ('credito', 'debito', 'transferencia'));

-- 3. Remover tipo customizado (não é mais usado)
DROP TYPE IF EXISTS tipo_lancamento;

-- ─── USUARIO.perfil ───────────────────────────────────────────────────────────

-- 1. Garantir que o tipo é VARCHAR(20)
ALTER TABLE usuario ALTER COLUMN perfil TYPE VARCHAR(20);

-- 2. Recriar CHECK constraint de forma idempotente
--    (V2 pode já tê-la criado inline — DROP IF EXISTS garante segurança)
ALTER TABLE usuario DROP CONSTRAINT IF EXISTS ck_usuario_perfil;
ALTER TABLE usuario
    ADD CONSTRAINT ck_usuario_perfil
    CHECK (perfil IN ('CORRENTISTA', 'OPERADOR', 'ADMIN'));

-- 3. Remover tipo customizado (não é mais usado)
DROP TYPE IF EXISTS perfil_usuario;
